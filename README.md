# CamperTe

App personale che mostra solo gli annunci di camper usati adatti ai miei genitori, presi da Subito.it, CamperOnLine.it e AutoScout24.it. Nessun server: il telefono legge direttamente i siti. La specifica completa è in [SPEC.md](SPEC.md).

L'app si usa solo su Android e si installa passando l'APK, senza Play Store. La versione iPhone serve solo per provare l'app dal Mac.

## Generare l'APK da installare

```sh
./gradlew :androidApp:assembleRelease
```

L'APK firmato è in `androidApp/build/outputs/apk/release/androidApp-release.apk`.

Se il nome del file finisce con `-unsigned`, Gradle non ha trovato la chiave di firma: controlla `local.properties` (vedi sotto).

## Installarlo sul telefono

1. Mandare l'APK sul telefono, ad esempio via WhatsApp, email o cavo USB.
2. Aprirlo. La prima volta Android chiede di permettere l'installazione di app all'app usata per aprirlo (WhatsApp, Gmail, File…): attivare "Consenti da questa fonte".
3. Se compare l'avviso di Play Protect ("app non riconosciuta"), toccare "Installa comunque". L'avviso compare perché l'app non viene dal Play Store.

Con il telefono collegato via USB e il debug attivo si può installare anche da qui:

```sh
adb install -r androidApp/build/outputs/apk/release/androidApp-release.apk
```

## Aggiornare l'app

Generare un nuovo APK e installarlo sopra quello vecchio, come la prima volta. Filtri salvati e annunci già visti restano.

Prima di generarlo, aumentare `versionCode` (e se si vuole `versionName`) in `androidApp/build.gradle.kts`.

L'aggiornamento funziona solo se l'APK è firmato **con la stessa chiave**. Con una chiave diversa Android rifiuta l'installazione e l'app va disinstallata, perdendo filtri e memoria degli annunci.

## La chiave di firma

- Il file è `~/Keystores/camperte-release.jks`.
- Le password sono in `local.properties`, alle voci `camperte.release.*`.
- Né la chiave né `local.properties` vanno su GitHub (sono in `.gitignore`).

**Fare un backup** del file `.jks` e delle password, ad esempio nel gestore di password o in una chiavetta: se si perdono, non si può più aggiornare l'app già installata.

In alternativa a `local.properties` si possono usare le variabili d'ambiente:

```
CAMPERTE_RELEASE_STORE_FILE
CAMPERTE_RELEASE_STORE_PASSWORD
CAMPERTE_RELEASE_KEY_ALIAS
CAMPERTE_RELEASE_KEY_PASSWORD
```

## Cambiare i criteri iniziali

I valori con cui parte l'app (prezzo, anno, km, tipologie, regioni, parole escluse e parole bonus) sono nel JSON `CONFIG_JSON` in `shared/src/commonMain/kotlin/camperte/Config.kt`.

Prezzo, anno, km, tipologie e distanza si cambiano anche dall'app, con il pulsante "Filtri". Il pulsante "Ripristina i valori iniziali" riporta ai valori di `Config.kt`.

Dopo aver modificato `Config.kt`, generare e installare un nuovo APK.

## Sviluppo

- Moduli: `shared` (logica comune in Kotlin Multiplatform), `androidApp` (Jetpack Compose), `iosApp` (SwiftUI, si apre con Xcode).
- Test: `./gradlew :shared:allTests :androidApp:testDebugUnitTest`.
- La build di debug ha come pacchetto `com.fabiodonello.camperte.debug` e come nome "CamperTe Debug", quindi convive sul telefono con la versione release.
