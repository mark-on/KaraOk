# KaraOK — kit brand Android

Questo kit riproduce in grafica vettoriale il logo selezionato: **coniglietto bianco inclinato che fa l'occhiolino, nota musicale bianca, fondo viola**. Il logo è stato ricalcato in curve SVG dalla versione PNG approvata, senza includere raster negli SVG.

## File principali

- `01_logo_svg/karaok_logo_full.svg`: logo completo con fondo viola.
- `01_logo_svg/karaok_mascot_white_transparent.svg`: mascotte + nota, sfondo trasparente.
- `02_png_high_res/karaok_icon_1024.png`, `_2048.png`, `_4096.png`: logo quadrato PNG.
- `03_splash_screen/karaok_splash_fullscreen.svg`: mockup schermata di avvio con logo e scritta KaraOK.
- `03_splash_screen/karaok_splash_preview_1080x1920.png` e `_1440x3200.png`: rendering per schermi verticali.
- `03_splash_screen/karaok_android12_splash_icon_transparent_1152.png`: icona centrata per splash di sistema (raster opzionale).
- `04_notification_icons/`: sagome bianche con trasparenza, senza fondale colorato.
- `android/adaptive_layers_png/`: livelli PNG a 432px esportati separatamente.
- `android/app/src/main/res/`: risorse Android native XML e PNG suddivise per densità.

## Integrazione Android Studio, Kotlin, Java, Compose

1. Copiare il contenuto di `android/app/src/main/res/` dentro `app/src/main/res/` del progetto Android. Se un file omonimo è già presente, verificare il contenuto prima di sovrascriverlo.
2. Controllare nel manifest dell'applicazione che siano impostati `android:icon="@mipmap/ic_launcher"` e `android:roundIcon="@mipmap/ic_launcher_round"`.
3. Android 8+ usa le icone adattive in `mipmap-anydpi-v26/`, con livello bianco trasparente e sfondo viola. Android 13+ può usare il livello monocromatico per le icone a tema. Le PNG `mipmap-*` sono un fallback per i dispositivi precedenti.
4. Per l'icona piccola delle notifiche utilizzare `R.drawable.ic_stat_karaok`: bianca su trasparenza, senza sfondo viola. Per la notifica colorata usare `NotificationCompat.Builder(...).setSmallIcon(R.drawable.ic_stat_karaok)` e, facoltativamente, `setColor(...)`; il colore è gestito dal sistema.
5. Per la splash di sistema: aggiungere la libreria AndroidX `core-splashscreen` compatibile con il progetto, definire un tema `Theme.SplashScreen` con `windowSplashScreenBackground=@color/karaok_purple`, `windowSplashScreenAnimatedIcon=@drawable/ic_splash_karaok` e `postSplashScreenTheme` impostato sul **tema già presente** nell'app. Applicare il tema di avvio alla launcher Activity e chiamare `installSplashScreen()` prima di `super.onCreate()`.
6. La schermata di sistema di Android 12+ utilizza un fondo tinta unita e l'icona centrale; **non** visualizza automaticamente il mockup PNG a schermo intero. Per usare esattamente il preview con gradiente e scritta, creare una schermata iniziale dell'app (Activity/Composable/Fragment) dopo la splash di sistema, se desiderato. Evitare tempi di attesa artificiali.

### Esempio di tema SplashScreen (da adattare al progetto)

```xml
<style name="Theme.KaraOK.Starting" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/karaok_purple</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_karaok</item>
    <item name="postSplashScreenTheme">@style/TEMA_ESISTENTE_DELL_APP</item>
</style>
```

Kotlin con AndroidX SplashScreen:

```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    // ...
}
```

## Risoluzioni incluse

Launcher vecchio: mdpi 48px, hdpi 72px, xhdpi 96px, xxhdpi 144px, xxxhdpi 192px.

Notifica: mdpi 24px, hdpi 36px, xhdpi 48px, xxhdpi 72px, xxxhdpi 96px.

Adaptive icon: canvas standard 108dp con grafica nella zona centrale sicura; vettori nativi XML + esportazione PNG 432x432.

## Indicazioni per Codex

Non sostituire l'icona bianca delle notifiche con un PNG colorato. Gli SVG sono i sorgenti modificabili; i file `ic_launcher_foreground.xml`, `ic_splash_karaok.xml` e `ic_stat_karaok.xml` sono equivalenti per Android VectorDrawable. Se la app usa Flutter, React Native o Expo, ricavare gli asset launcher e splash dalle PNG/SVG sorgente e seguire la configurazione del framework, anziché copiare le risorse XML senza adattarle.

## Colori

- Viola principale: `#7B0EFC`
- Viola chiaro: `#8515FF`
- Viola scuro: `#7510F3`
- Bianco: `#FFFFFF`

Le versioni SVG sono una vettorializzazione fedele della PNG selezionata, non il file vettoriale originale (che non era disponibile). Controllare l'anteprima finale sul dispositivo e ridimensionare soltanto se richiesto dalla UI.

Riferimenti ufficiali Android:
- Adaptive icons: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
- Splash Screen: https://developer.android.com/develop/ui/views/launch/splash-screen/
