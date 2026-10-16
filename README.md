<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="150" alt="KaraOk icon">
</p>

<h1 align="center"><b>KaraOk</b></h1>
<h4 align="center">Auxio, con i testi. Minimalista, locale e sincronizzato.</h4>

<p align="center">
  <a href="https://github.com/mark-on/KaraOk/releases/latest">
    <img alt="Latest release" src="https://img.shields.io/github/v/release/mark-on/KaraOk?color=64B5F6&style=flat">
  </a>
  <a href="https://github.com/mark-on/KaraOk/releases">
    <img alt="Downloads" src="https://img.shields.io/github/downloads/mark-on/KaraOk/total.svg?color=4B95DE&style=flat">
  </a>
  <a href="https://www.gnu.org/licenses/gpl-3.0">
    <img alt="GPLv3" src="https://img.shields.io/badge/license-GPL%20v3-2B6DBE.svg?style=flat">
  </a>
  <img alt="Android 7+" src="https://img.shields.io/badge/Android-7.0%2B-1450A8?style=flat">
</p>

<p align="center">
  <a href="https://github.com/mark-on/KaraOk/releases/latest"><b>Download</b></a>
  ·
  <a href="https://github.com/OxygenCobalt/Auxio"><b>Progetto originale Auxio</b></a>
  ·
  <a href="LICENSE"><b>Licenza</b></a>
</p>

## Cos'è KaraOk?

**KaraOk è una fork di [Auxio](https://github.com/OxygenCobalt/Auxio) che aggiunge i testi direttamente alla schermata “In riproduzione”.**

Mantiene il design Material You, la riproduzione locale, le prestazioni e l'approccio essenziale di Auxio. Toccando la copertina dell'album si apre un overlay minimale: l'interfaccia rimane sullo sfondo, i testi scorrono sopra di essa e la musica continua senza interruzioni.

KaraOk usa un application ID separato (`com.markon.karaok`), quindi può essere installato insieme ad Auxio.

## Testi

KaraOk cerca i testi in questo ordine:

1. metadati incorporati nel file audio;
2. file `.lrc` locale associato al brano;
3. servizio online [LRCLIB](https://lrclib.net).

Sono supportati:

- testi sincronizzati LRC, con evidenziazione della riga corrente;
- scorrimento automatico basato sulla posizione di riproduzione;
- pausa dell'auto-scroll durante lo scorrimento manuale;
- testi non sincronizzati in una vista pulita e scorrevole;
- aggiornamento dopo pausa, ricerca, salto o cambio traccia;
- cache locale dei testi scaricati;
- gestione silenziosa di rete assente, testi mancanti o LRC non validi.

> KaraOk attualmente **legge** i testi incorporati, ma non modifica i metadati dei file audio. I testi recuperati da LRCLIB vengono conservati nella cache dell'app.

## Schermate

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot0.png" width="245" alt="Libreria musicale">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot1.png" width="245" alt="Album">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot2.png" width="245" alt="In riproduzione">
</p>
<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot3.png" width="245" alt="Coda">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot4.png" width="245" alt="Ricerca">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot5.png" width="245" alt="Impostazioni">
</p>

<p align="center"><i>L'interfaccia di base e le schermate derivano da Auxio; KaraOk conserva intenzionalmente lo stesso linguaggio visivo.</i></p>

## Funzionalità principali

- riproduzione basata su Media3 ExoPlayer;
- interfaccia Material You edge-to-edge;
- libreria musicale completamente locale;
- supporto per copertine e testi incorporati;
- file LRC locali e recupero da LRCLIB;
- ReplayGain per MP3, FLAC, OGG, Opus e MP4;
- riproduzione gapless;
- playlist, ricerca, widget e Android Auto;
- gestione delle cartelle compatibile con schede SD;
- nessun account e nessun tracciamento.

## Installazione

Scarica l'APK dalla pagina [Releases](https://github.com/mark-on/KaraOk/releases). Android potrebbe chiedere di autorizzare l'installazione da sorgenti esterne.

Le prime versioni di KaraOk sono pubblicate come **prerelease firmate in debug**, pensate per test e sviluppo. Una futura versione stabile richiederà una chiave di firma dedicata e mantenuta nel tempo.

## Compilazione

Il progetto richiede Java 21, Android SDK 37, NDK `28.2.13676358`, CMake `3.22.1` e un ambiente Unix. Su Windows è consigliato WSL2.

```bash
git clone --recurse-submodules https://github.com/mark-on/KaraOk.git
cd KaraOk
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

L'APK viene generato in `app/build/outputs/apk/debug/`.

## Permessi

- accesso ai file audio, per indicizzare e riprodurre la musica;
- servizio in primo piano e wake lock, per la riproduzione in background;
- notifiche, per i controlli multimediali;
- internet, usato esclusivamente per il recupero opzionale dei testi da LRCLIB.

Non vengono richiesti permessi aggiuntivi per cercare file LRC locali.

## Crediti e licenza

KaraOk deriva da **Auxio**, creato e mantenuto da [OxygenCobalt](https://github.com/OxygenCobalt), e conserva gli avvisi di copyright del progetto originale. Grazie agli autori di Auxio, Media3, TagLib, FFmpeg e LRCLIB.

KaraOk è software libero distribuito secondo la [GNU General Public License v3](LICENSE). Puoi usarlo, studiarlo, modificarlo e ridistribuirlo nel rispetto della licenza e delle attribuzioni originali.
