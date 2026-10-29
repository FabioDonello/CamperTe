# CamperTe – Specifica per Claude Code

Oct 29, 2026 · @Fabio

## Obiettivo e principi

CamperTe è un'app mobile personale che all'apertura mostra i soli annunci di camper usati che rispettano i criteri dei miei genitori. Nient'altro.

I miei genitori usano solo Android: l'app si installa passando l'APK, senza store. Esiste anche la versione iPhone, con la stessa logica, ma serve solo per i miei test dal Mac.

Principi da rispettare in ogni scelta di codice:

- **Semplicità prima di tutto.** La lista degli annunci più una schermata Filtri, pochi file, poche dipendenze. Se una funzionalità non serve a "vedere gli annunci giusti", non si fa.
- **Logica condivisa, interfaccia nativa.** Tutto ciò che non è grafica è scritto una volta sola in Kotlin; ogni piattaforma ha la sua interfaccia nativa, sottile e senza logica.
- **Nessun backend, nessun cloud.** Niente server, niente database online, niente account, niente login. L'app scarica gli annunci direttamente dal telefono.
- **Dati solo sul telefono.** I criteri iniziali stanno in un file JSON incluso nell'app; i filtri scelti e la memoria degli annunci già visti stanno nella memoria locale del dispositivo. La posizione del telefono serve solo a calcolare le distanze e non esce dal telefono.
- **Pensata per utenti non tecnici.** I miei genitori toccano l'icona e guardano. L'unica cosa da toccare è la schermata Filtri, con pochi controlli grandi: prezzo, distanza, anno e km, tipologie.
- **Lingua italiana** in tutta l'interfaccia e nei messaggi.

## Architettura

L'app è costruita con **Kotlin Multiplatform (KMP)**: un modulo `shared` contiene scaricamento, lettura, filtro e memoria degli annunci; sopra ci sono due interfacce native, **Jetpack Compose** su Android e **SwiftUI** su iOS.

Perché funziona senza backend: un'app nativa, a differenza di una pagina web nel browser, non ha restrizioni CORS e può leggere direttamente le pagine di ricerca dei siti di annunci.

Regola di confine: le interfacce native non contengono logica. Usano solo `CamperRepository`: `loadListings(position)` per gli annunci, `currentFilters()`, `saveFilters()` e `filtersSummary()` per i filtri. Mostrano il risultato così com'è.

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
│       │   ├── Config.kt            # criteri iniziali (JSON)
│       │   ├── Listing.kt           # modello comune e testi mostrati
│       │   ├── Filter.kt            # applica i criteri, funzioni pure
│       │   ├── SearchFilters.kt     # filtri scelti dall'utente
│       │   ├── FiltersStore.kt      # memoria dei filtri
│       │   ├── SeenListings.kt      # memoria degli annunci visti
│       │   ├── GeoPoint.kt          # distanza tra due punti
│       │   ├── ItalianRegions.kt    # regioni vicine a un punto
│       │   ├── SearchResult.kt      # risultato per le UI
│       │   ├── CamperRepository.kt  # unico punto di ingresso per le UI
│       │   └── sources/
│       │       ├── Source.kt        # interfaccia comune + elenco fonti attive
│       │       ├── HttpClient.kt    # client Ktor comune
│       │       ├── PageText.kt      # utilità di lettura delle pagine
│       │       ├── SubitoSource.kt  # un file per ogni sito di annunci
│       │       ├── CamperOnLineSource.kt
│       │       └── AutoScout24Source.kt
│       └── commonTest/kotlin/camperte/   # test di filtri, repository e parser
├── androidApp/
│   └── src/main/kotlin/camperte/android/
│       ├── MainActivity.kt
│       ├── ListingsViewModel.kt
│       ├── LocationProvider.kt
│       ├── ListingsScreen.kt
│       ├── ListingCard.kt
│       ├── FiltersScreen.kt
│       └── Theme.kt
└── iosApp/
    └── iosApp/
        ├── CamperTeApp.swift
        ├── ListingsModel.swift
        ├── LocationProvider.swift
        ├── ListingsView.swift
        ├── ListingCardView.swift
        ├── FiltersView.swift
        └── Typography.swift
```

## Configurazione dei criteri

I criteri iniziali stanno in un testo JSON dentro Config.kt (modulo shared), che qui chiamiamo `config.json`. Prezzo, anno, km e tipologie si possono poi cambiare dalla schermata Filtri; regioni, parole escluse e parole bonus si cambiano solo qui.

```json
{
  "min_price": 15000,
  "max_price": 35000,
  "min_year": 2008,
  "max_mileage_km": 150000,
  "camper_types": ["mansardato", "semintegrale", "furgonato"],
  "min_berths": 4,
  "regions": ["Lombardia", "Emilia-Romagna", "Veneto"],
  "required_words": [],
  "excluded_words": ["incidentato", "da riparare", "noleggio", "cerco", "caravan", "roulotte"],
  "bonus_words": ["letto fisso", "garage", "pannello solare", "tagliandato"]
}
```

Significato dei campi:

| Campo | Effetto |
| --- | --- |
| `min_price`, `max_price` | Fascia di prezzo in euro; fuori fascia l'annuncio viene scartato |
| `min_year`, `max_mileage_km` | Anno di immatricolazione minimo e chilometri massimi |
| `camper_types` | Tipi di camper accettati (mansardato, semintegrale, furgonato/camperizzato, motorhome); se il tipo non è riconoscibile l'annuncio resta, senza penalità |
| `min_berths` | Posti letto minimi, se l'annuncio li indica |
| `regions` | Regioni di ricerca quando non si usa la distanza dal telefono; lista vuota = tutta Italia |
| `required_words` | Parole che devono comparire nel titolo o nella descrizione |
| `excluded_words` | Se compare una di queste, l'annuncio viene scartato |
| `bonus_words` | Non scartano nulla: alzano l'annuncio in classifica e vengono evidenziate |

Regola importante: un dato mancante nell'annuncio (es. km non indicati) non deve mai causarne lo scarto. Meglio mostrare un annuncio in più che perderne uno buono.

Il JSON è una costante `CONFIG_JSON` letta con `kotlinx.serialization` (con `@SerialName` per i nomi con trattino basso), così resta un solo file per entrambe le piattaforme. Per cambiare i criteri iniziali lo modifico io e installo un nuovo APK.

## Flusso di funzionamento

Ogni apertura dell'app, ogni ritorno nell'app, ogni trascinamento verso il basso della lista e ogni modifica dei filtri chiama `CamperRepository.loadListings(position)`, con la posizione del telefono se è disponibile. Il repository esegue nel modulo condiviso gli stessi quattro passi e restituisce un unico risultato.

1. **Scarica gli annunci** da ogni fonte attiva, restringendo già la ricerca con i filtri scelti (prezzo, anno, km, tipologie, regioni) quando il sito lo permette. Le fonti vengono interrogate in parallelo con `async` dentro `supervisorScope`, così l'errore di una non ferma le altre. Ogni annuncio diventa una `data class Listing`:
   - `id` (univoco, es. `subito-123456`), `source`, `title`, `price`, `year`, `mileageKm`, `camperType`, `berths`, `location`, `region`, `shortDescription`, `photoUrls` (tutte le foto disponibili nella lista), `url`, `position` (coordinate, se il sito le dà). I campi sconosciuti valgono `null`.
2. **Filtra** con i filtri scelti e le regole di `config.json`, calcola la distanza dal telefono e un punteggio semplice: +1 per ogni parola bonus trovata. Con la ricerca per distanza, un annuncio con coordinate viene tenuto se è entro i km scelti; uno senza coordinate se è in una regione vicina. `Filter.kt` contiene solo funzioni pure, senza rete né memoria, così è facile da testare.
3. **Confronta con la memoria locale** (`multiplatform-settings`, chiave `seen_listings`, per ogni id la prima e l'ultima volta in cui è stato mostrato) e marca come "Nuovo" gli annunci mostrati per la prima volta da meno di 12 ore. Le 12 ore servono perché l'app ricarica la lista tornando dal browser: senza, le etichette sparirebbero dopo aver aperto un solo annuncio. Gli annunci non mostrati da più di 90 giorni vengono tolti dalla memoria.
4. **Restituisce un `SearchResult`** con la lista ordinata, il numero di nuovi, l'ora dell'aggiornamento e gli avvisi sulle fonti fallite. Le UI lo mostrano così com'è; mentre attendono, una rotellina con la scritta "Sto cercando i camper per voi...".

Se una fonte non risponde o il sito blocca la richiesta, il risultato contiene gli annunci delle altre fonti e un avviso ("Subito non è raggiungibile al momento"). Senza internet le UI mostrano "Nessuna connessione. Riprovate tra poco." con un pulsante "Riprova". L'app non deve mai mostrare errori tecnici né chiudersi: `loadListings()` non lancia eccezioni verso le UI.

## Fonti degli annunci

Ogni fonte è un file in `shared/.../sources/` che implementa una sola interfaccia:

```kotlin
interface Source {
    val name: String
    // Returns listings in the common format; throws on failure.
    suspend fun search(config: Config): List<Listing>
}
```

Così aggiungere un sito significa solo creare un nuovo file e aggiungerlo alla lista `activeSources` in `Source.kt`. Le fonti vivono solo in `commonMain`: nessun codice di rete specifico per piattaforma oltre al motore Ktor.

Fonti attive:

| Fonte | Come si legge | Note |
| --- | --- | --- |
| Subito.it | JSON `__NEXT_DATA__` della pagina di ricerca, una pagina per regione | Ha coordinate e tutte le foto. Il `robots.txt` vieta l'accesso automatico: scelta mia consapevole, per uso personale. |
| CamperOnLine.it | HTML dei risultati, un modulo di ricerca per tipologia | Consentito dal `robots.txt`. Una sola foto per annuncio, niente coordinate, regioni filtrate nell'app. |
| AutoScout24.it | JSON `__NEXT_DATA__` di `/lst-caravan`, una pagina per tipologia | Le condizioni generali (art. 5.1) vietano l'accesso con app di terzi: scelta mia consapevole, per uso personale. Niente coordinate; la regione si ricava dal CAP. Pochi camper usati in Italia. |

Indicazioni per Claude Code:

- Prima di scrivere il parser, ispeziona la pagina di ricerca reale: se il sito include i dati degli annunci in JSON dentro la pagina (ad esempio in un tag `<script>`), estrai quelli invece di leggere l'HTML, perché è molto più stabile ed evita librerie di parsing.
- Uso rispettoso: una sola ricerca per apertura (massimo 2-3 pagine di risultati), una pausa di almeno 2 secondi tra le richieste allo stesso sito, uno User-Agent da browser mobile normale, nessun download delle singole pagine di dettaglio.
- È un uso strettamente personale: verificare che resti compatibile con i termini d'uso del sito.
- I siti cambiano struttura e possono bloccare le richieste automatiche: il parser deve fallire in modo pulito (avviso in app) e tutta la logica specifica del sito deve restare in quel solo file.

## La schermata degli annunci

La lista è realizzata due volte in modo nativo e identico nei contenuti: `LazyColumn` in Compose e `ScrollView` + `LazyVStack` in SwiftUI. È una lista verticale di schede grandi, una per annuncio, leggibile senza occhiali da vicino.

**In cima:** titolo "I camper per voi" con il pulsante blu "Filtri", il riassunto dei filtri attivi (toccandolo si aprono i Filtri), poi numero di annunci, quanti sono nuovi e ora dell'ultimo aggiornamento (es. "12 camper · 3 nuovi · alle 9:41"). Sotto, eventuali avvisi sulle fonti non raggiungibili.

**Ogni scheda mostra:**

- Foto a tutta larghezza, da scorrere di lato se sono più di una, con il contatore "📷 1 / N" e i pallini; se manca, un riquadro grigio con scritto "Foto non disponibile"
- Etichetta verde "NUOVO" se l'annuncio è comparso per la prima volta da meno di 12 ore
- Prezzo in grande, formato italiano (€ 28.500), e la distanza dal telefono se nota ("a 42 km")
- Titolo dell'annuncio
- Riga di dati: anno · km · tipologia · posti letto · località (i dati mancanti si omettono, senza scrivere "n/d")
- Le parole bonus trovate, come piccole etichette (es. "letto fisso", "garage")
- Un pulsante grande "Vedi annuncio" che apre il link originale nel browser o nell'app del sito

**Ordinamento:** prima i nuovi, poi per punteggio bonus decrescente, poi per prezzo crescente.

**Aggiornamento:** trascinando la lista verso il basso si ripete la ricerca (`PullToRefreshBox` in Compose, `.refreshable` in SwiftUI). La ricerca si ripete anche tornando nell'app e chiudendo i Filtri, se sono cambiati.

**Design:** testo base di almeno 18 sp/pt, prezzo 28 sp/pt, pulsanti alti almeno 48 dp/pt, forte contrasto, rispetto delle aree sicure (notch, barra di sistema). Sfondo chiaro e verde salvia; le azioni importanti ("Vedi annuncio", "Filtri", "Mostra i camper") sono pulsanti pieni blu intenso, mai colori tenui. Ogni piattaforma segue il proprio stile: Material 3 su Android, aspetto iOS standard su iPhone. Il testo deve ingrandirsi con il carattere grande del telefono (scala dei font su Android, Dynamic Type su iOS). Funziona in tema chiaro e scuro. Nessun menu oltre al pulsante Filtri.

**Nessun risultato:** messaggio gentile "Oggi nessun camper nuovo corrisponde ai vostri criteri. Riprovate domani!"

## La schermata Filtri

Si apre dal pulsante "Filtri" o toccando il riassunto. Ha pochi controlli grandi, ognuno con pulsanti − e + da 52 dp/pt:

- **Prezzo:** da / a, a passi di 1.000 €
- **Distanza da qui:** 50, 100, 150 o 300 km dalla posizione del telefono, oppure "Tutta Italia". La prima volta l'app spiega perché chiede la posizione; se viene negata, avvisa e usa le regioni di `config.json`
- **Anno e chilometri:** immatricolato dal (a passi di un anno), chilometri al massimo (a passi di 10.000)
- **Tipologie:** mansardato, semintegrale, furgonato, motorhome

"Mostra i camper" o la freccia indietro chiudono i Filtri, li salvano e ripetono la ricerca se sono cambiati. "Ripristina i valori iniziali" torna ai valori di `config.json`. La logica dei filtri (passi, limiti, riassunto) sta in `SearchFilters.kt`; le UI la chiamano soltanto.

## Installazione e cosa è escluso

I miei genitori trovano l'icona CamperTe sul telefono, la toccano e vedono gli annunci. Tutta la parte tecnica la gestisco io.

- **Strumenti:** Android Studio con il plugin Kotlin Multiplatform; per provare la parte iOS serve un Mac con Xcode.
- **Android:** genero un APK firmato con la mia chiave (`./gradlew :androidApp:assembleRelease`, chiave fuori dal repository, credenziali in `local.properties`) e lo installo direttamente sul telefono, senza Play Store.
- **iPhone:** nessuna distribuzione. La uso solo io, installandola da Xcode.
- **Aggiornamenti:** per cambiare criteri o correggere qualcosa installo un nuovo APK, firmato con la stessa chiave, sopra quello vecchio.
- Un file `README.md` breve spiega, per me, questi passaggi, il backup della chiave e come modificare i criteri in `Config.kt`.

**Fuori dal perimetro (da non costruire):**

- Server, API proprie, database, account, sincronizzazione tra dispositivi
- Notifiche push e ricerche in background
- Valutazione del prezzo di mercato, voti, preferiti, checklist di ispezione
- Schermate di impostazioni oltre ai Filtri, navigazione tra più schermate
- Versione web, pubblicazione sugli store pubblici, distribuzione iOS (TestFlight, ad hoc); Compose Multiplatform su iOS (l'interfaccia iPhone resta SwiftUI)

Queste idee potranno arrivare in una seconda versione, solo se la prima viene davvero usata.

## Criteri di accettazione e prompt di partenza

La prima versione è finita quando tutte queste condizioni sono vere:

- [ ] Aprendo l'app su Android e su iPhone compaiono annunci reali da Subito.it, CamperOnLine.it e AutoScout24.it, identici nelle due piattaforme
- [ ] Ogni annuncio mostrato rispetta i filtri scelti (prezzo, anno, km, tipologie, distanza o regioni) e le parole escluse di `config.json`
- [ ] Alla riapertura, dopo più di 12 ore, solo gli annunci comparsi nel frattempo hanno l'etichetta "NUOVO"
- [ ] Trascinando la lista verso il basso la ricerca si ripete
- [ ] In modalità aereo l'app mostra un messaggio comprensibile e il pulsante "Riprova", senza chiudersi
- [ ] La logica di filtro ha test con `kotlin.test` in `commonTest` su annunci di esempio (dati mancanti inclusi), eseguiti con `./gradlew :shared:allTests`
- [ ] Le UI native non contengono logica di filtro, ordinamento o rete

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
