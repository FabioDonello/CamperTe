# CamperTe – Specifica per Claude Code

Oct 29, 2026 · @Fabio

## Obiettivo e principi

CamperTe è un'app mobile personale, per Android e iPhone, che all'apertura mostra una sola schermata con i soli annunci di camper usati che rispettano i criteri dei miei genitori. Nient'altro.

Principi da rispettare in ogni scelta di codice:

- **Semplicità prima di tutto.** Una sola schermata, pochi file, poche dipendenze. Se una funzionalità non serve a "vedere gli annunci giusti", non si fa.
- **Logica condivisa, interfaccia nativa.** Tutto ciò che non è grafica è scritto una volta sola in Kotlin; ogni piattaforma ha la sua interfaccia nativa, sottile e senza logica.
- **Nessun backend, nessun cloud.** Niente server, niente database online, niente account, niente login. L'app scarica gli annunci direttamente dal telefono.
- **Dati solo sul telefono.** I criteri stanno in un file JSON incluso nell'app; la memoria degli annunci già visti sta nella memoria locale del dispositivo.
- **Pensata per utenti non tecnici.** I miei genitori toccano l'icona e guardano. Nessuna impostazione da toccare.
- **Lingua italiana** in tutta l'interfaccia e nei messaggi.

## Architettura

L'app è costruita con **Kotlin Multiplatform (KMP)**: un modulo `shared` contiene scaricamento, lettura, filtro e memoria degli annunci; sopra ci sono due interfacce native, **Jetpack Compose** su Android e **SwiftUI** su iOS.

Perché funziona senza backend: un'app nativa, a differenza di una pagina web nel browser, non ha restrizioni CORS e può leggere direttamente le pagine di ricerca dei siti di annunci.

Regola di confine: le interfacce native non contengono logica. Chiamano una sola funzione condivisa, `CamperRepository.loadListings()`, e mostrano il risultato.

**Stack (ultime versioni stabili)**

- Modulo `shared` (commonMain): Kotlin, coroutine, `Ktor Client` (motore OkHttp su Android, Darwin su iOS), `kotlinx.serialization` per il JSON, `multiplatform-settings` per la memoria locale (SharedPreferences / NSUserDefaults)
- Android: Jetpack Compose con Material 3, `Coil` per le foto
- iOS: SwiftUI, `AsyncImage` per le foto, `SKIE` per usare le funzioni `suspend` di Kotlin come `async` in Swift
- Test: `kotlin.test` in `commonTest`
- Nessuna libreria di navigazione (c'è una sola schermata), nessuna dependency injection: il repository si crea a mano

**Struttura dei file**

```
camperte/
├── settings.gradle.kts
├── shared/
│   └── src/
│       ├── commonMain/kotlin/camperte/
│       │   ├── Config.kt            # criteri di ricerca (JSON)
│       │   ├── Listing.kt           # modello comune
│       │   ├── Filter.kt            # applica i criteri, funzioni pure
│       │   ├── SeenListings.kt      # memoria degli annunci visti
│       │   ├── CamperRepository.kt  # unico punto di ingresso per le UI
│       │   └── sources/
│       │       ├── Source.kt        # interfaccia comune + elenco fonti attive
│       │       └── SubitoSource.kt  # un file per ogni sito di annunci
│       ├── androidMain/             # motore HTTP Android
│       ├── iosMain/                 # motore HTTP iOS
│       └── commonTest/kotlin/FilterTest.kt
├── androidApp/
│   └── src/main/kotlin/.../
│       ├── MainActivity.kt
│       ├── ListingsScreen.kt
│       └── ListingCard.kt
└── iosApp/
    └── iosApp/
        ├── CamperTeApp.swift
        ├── ListingsView.swift
        └── ListingCardView.swift
```

## Configurazione dei criteri

Tutti i gusti dei miei genitori stanno in un testo JSON dentro Config.kt (modulo shared), che qui chiamiamo `config.json`. Si modifica una volta sola, a mano, e il resto del codice non va mai toccato. I valori qui sotto sono esempi da sostituire.

```json
{
  "min_price": 15000,
  "max_price": 35000,
  "min_year": 2008,
  "max_mileage_km": 150000,
  "camper_types": ["mansardato", "semintegrale"],
  "min_berths": 4,
  "regions": ["Lombardia", "Emilia-Romagna", "Veneto"],
  "required_words": [],
  "excluded_words": ["incidentato", "da riparare", "noleggio", "cerco"],
  "bonus_words": ["letto fisso", "garage", "pannello solare", "tagliandato"]
}
```

Significato dei campi:

| Campo | Effetto |
| --- | --- |
| `min_price`, `max_price` | Fascia di prezzo in euro; fuori fascia l'annuncio viene scartato |
| `min_year`, `max_mileage_km` | Anno di immatricolazione minimo e chilometri massimi |
| `camper_types` | Tipi di camper accettati; se il tipo non è riconoscibile l'annuncio resta, senza penalità |
| `min_berths` | Posti letto minimi, se l'annuncio li indica |
| `regions` | Zone di ricerca; lista vuota = tutta Italia |
| `required_words` | Parole che devono comparire nel titolo o nella descrizione |
| `excluded_words` | Se compare una di queste, l'annuncio viene scartato |
| `bonus_words` | Non scartano nulla: alzano l'annuncio in classifica e vengono evidenziate |

Regola importante: un dato mancante nell'annuncio (es. km non indicati) non deve mai causarne lo scarto. Meglio mostrare un annuncio in più che perderne uno buono.

Il JSON è una costante `CONFIG_JSON` letta con `kotlinx.serialization` (con `@SerialName` per i nomi con trattino basso), così resta un solo file per entrambe le piattaforme. Per cambiare i criteri lo modifico io e reinstallo l'app: un nuovo APK su Android, una nuova build da Xcode o TestFlight su iPhone. I miei genitori non vedono mai impostazioni.

## Flusso di funzionamento

Ogni apertura dell'app, e ogni trascinamento verso il basso della lista, chiama `CamperRepository.loadListings()`, che esegue nel modulo condiviso gli stessi quattro passi e restituisce un unico risultato.

1. **Scarica gli annunci** da ogni fonte attiva, restringendo già la ricerca a camper, fascia di prezzo e regioni quando il sito lo permette. Le fonti vengono interrogate in parallelo con `async` dentro `supervisorScope`, così l'errore di una non ferma le altre. Ogni annuncio diventa una `data class Listing`:
   - `id` (univoco, es. `subito-123456`), `source`, `title`, `price`, `year`, `mileageKm`, `camperType`, `berths`, `location`, `region`, `shortDescription`, `photoUrl`, `url` (i campi sconosciuti valgono `null`)
2. **Filtra** con le regole di `config.json` e calcola un punteggio semplice: +1 per ogni parola bonus trovata. `Filter.kt` contiene solo funzioni pure, senza rete né memoria, così è facile da testare.
3. **Confronta con la memoria locale** (`multiplatform-settings`, chiave `seen_listings`, id e data in JSON) per marcare come "Nuovo" gli annunci mai mostrati prima, poi la aggiorna. Gli annunci più vecchi di 90 giorni vengono tolti.
4. **Restituisce un `SearchResult`** con la lista ordinata, il numero di nuovi, l'ora dell'aggiornamento e gli avvisi sulle fonti fallite. Le UI lo mostrano così com'è; mentre attendono, una rotellina con la scritta "Sto cercando i camper per voi...".

Se una fonte non risponde o il sito blocca la richiesta, il risultato contiene gli annunci delle altre fonti e un avviso ("Subito non è raggiungibile al momento"). Senza internet le UI mostrano "Nessuna connessione. Riprovate tra poco." con un pulsante "Riprova". L'app non deve mai mostrare errori tecnici né chiudersi: `loadListings()` non lancia eccezioni verso le UI.

## Fonti degli annunci

Si parte con una sola fonte, Subito.it, e solo quando funziona bene se ne aggiungono altre. Ogni fonte è un file in `shared/.../sources/` che implementa una sola interfaccia:

```kotlin
interface Source {
    val name: String
    // Returns listings in the common format; throws on failure.
    suspend fun search(config: Config): List<Listing>
}
```

Così aggiungere AutoScout24 o un concessionario locale significa solo creare un nuovo file e aggiungerlo alla lista `activeSources` in `Source.kt`. Le fonti vivono solo in `commonMain`: nessun codice di rete specifico per piattaforma oltre al motore Ktor.

Indicazioni per Claude Code:

- Prima di scrivere il parser, ispeziona la pagina di ricerca reale: se il sito include i dati degli annunci in JSON dentro la pagina (ad esempio in un tag `<script>`), estrai quelli invece di leggere l'HTML, perché è molto più stabile ed evita librerie di parsing.
- Uso rispettoso: una sola ricerca per apertura (massimo 2-3 pagine di risultati), una pausa di almeno 2 secondi tra le richieste allo stesso sito, uno User-Agent da browser mobile normale, nessun download delle singole pagine di dettaglio.
- È un uso strettamente personale: verificare che resti compatibile con i termini d'uso del sito.
- I siti cambiano struttura e possono bloccare le richieste automatiche: il parser deve fallire in modo pulito (avviso in app) e tutta la logica specifica del sito deve restare in quel solo file.

## La schermata degli annunci

L'app ha una sola schermata, realizzata due volte in modo nativo e identico nei contenuti: `LazyColumn` in Compose e `List` (o `ScrollView` + `LazyVStack`) in SwiftUI. È una lista verticale di schede grandi, una per annuncio, leggibile senza occhiali da vicino.

**In cima:** titolo "I camper per voi", ora dell'ultimo aggiornamento, numero di annunci e quanti sono nuovi (es. "12 camper, 3 nuovi"). Sotto, eventuali avvisi sulle fonti non raggiungibili.

**Ogni scheda mostra:**

- Foto a tutta larghezza; se manca, un riquadro grigio con scritto "Foto non disponibile"
- Etichetta verde "NUOVO" se mai vista prima
- Prezzo in grande, formato italiano (€ 28.500)
- Titolo dell'annuncio
- Riga di dati: anno · km · tipologia · posti letto · località (i dati mancanti si omettono, senza scrivere "n/d")
- Le parole bonus trovate, come piccole etichette (es. "letto fisso", "garage")
- Un pulsante grande "Vedi annuncio" che apre il link originale nel browser o nell'app del sito

**Ordinamento:** prima i nuovi, poi per punteggio bonus decrescente, poi per prezzo crescente.

**Aggiornamento:** trascinando la lista verso il basso si ripete la ricerca (`PullToRefreshBox` in Compose, `.refreshable` in SwiftUI). Nessun altro gesto o pulsante.

**Design:** testo base di almeno 18 sp/pt, prezzo 28 sp/pt, pulsanti alti almeno 48 dp/pt, forte contrasto, colori calmi, rispetto delle aree sicure (notch, barra di sistema). Ogni piattaforma segue il proprio stile: Material 3 su Android, aspetto iOS standard su iPhone. Il testo deve ingrandirsi con il carattere grande del telefono (scala dei font su Android, Dynamic Type su iOS). Funziona in tema chiaro e scuro. Nessun menu, nessuna navigazione, nessun filtro da toccare.

**Nessun risultato:** messaggio gentile "Oggi nessun camper nuovo corrisponde ai vostri criteri. Riprovate domani!"

## Installazione e cosa è escluso

I miei genitori trovano l'icona CamperTe sul telefono, la toccano e vedono gli annunci. Tutta la parte tecnica la gestisco io.

- **Strumenti:** Android Studio con il plugin Kotlin Multiplatform; per la parte iOS serve obbligatoriamente un Mac con Xcode.
- **Android:** genero un APK firmato con Gradle (`./gradlew :androidApp:assembleRelease`) e lo installo direttamente sul telefono, senza Play Store.
- **iPhone:** con un Apple ID gratuito installo da Xcode, ma l'app smette di aprirsi dopo 7 giorni. Per un'installazione che non scada serve un account Apple Developer (99 $ l'anno) e la distribuzione via TestFlight o ad hoc.
- **Aggiornamenti:** per cambiare criteri o correggere qualcosa reinstallo l'app (nuovo APK, nuova build iOS).
- Un file `README.md` breve spiega, per me, questi passaggi e come modificare i criteri in `Config.kt`.

**Fuori dal perimetro (da non costruire):**

- Server, API proprie, database, account, sincronizzazione tra dispositivi
- Notifiche push e ricerche in background
- Valutazione del prezzo di mercato, voti, preferiti, checklist di ispezione
- Schermate di impostazioni, filtri interattivi, navigazione tra più schermate
- Versione web e pubblicazione sugli store pubblici; Compose Multiplatform su iOS (l'interfaccia iPhone resta SwiftUI)

Queste idee potranno arrivare in una seconda versione, solo se la prima viene davvero usata.

## Criteri di accettazione e prompt di partenza

La prima versione è finita quando tutte queste condizioni sono vere:

- [ ] Aprendo l'app su Android e su iPhone compaiono annunci reali da Subito.it, identici nelle due piattaforme
- [ ] Ogni annuncio mostrato rispetta prezzo, anno, km, regioni e parole escluse di `config.json`
- [ ] Alla riapertura, solo gli annunci comparsi nel frattempo hanno l'etichetta "NUOVO"
- [ ] Trascinando la lista verso il basso la ricerca si ripete
- [ ] In modalità aereo l'app mostra un messaggio comprensibile e il pulsante "Riprova", senza chiudersi
- [ ] La logica di filtro ha test con `kotlin.test` in `commonTest` su annunci di esempio (dati mancanti inclusi), eseguiti con `./gradlew :shared:allTests`
- [ ] Le UI native non contengono logica di filtro, ordinamento o rete
- [ ] Il codice totale resta sotto le 1.000 righe circa

**Prompt da incollare in Claude Code** (insieme a questo documento salvato come `SPEC.md` nella cartella del progetto):

```
Leggi SPEC.md: è la specifica completa dell'app mobile CamperTe.
Costruiscila passo per passo con Kotlin Multiplatform e UI native
(Jetpack Compose su Android, SwiftUI su iOS), con la massima semplicità.
1. Crea il progetto KMP (shared, androidApp, iosApp) e Config.kt di esempio.
2. Scrivi Listing.kt e Filter.kt con i test in commonTest, usando annunci finti.
3. Scrivi CamperRepository con una fonte finta, poi la UI Compose e verificala su Android.
4. Scrivi la UI SwiftUI con SKIE e verificala nel simulatore iOS.
5. Solo dopo, ispeziona la ricerca reale di Subito.it e scrivi SubitoSource.kt.
6. Aggiungi SeenListings.kt e la configurazione per l'APK firmato.
Fermati dopo ogni passo e mostrami cosa hai fatto prima di continuare.
Non aggiungere nulla che sia nella sezione "Fuori dal perimetro".
```
