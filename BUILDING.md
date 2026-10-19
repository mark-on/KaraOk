# Building KaraOk

KaraOk is configured to build directly from Android Studio on Windows, Linux, and macOS. A normal app build does not require WSL, Git Bash, or manually running a native build script.

## Requirements

- Android Studio with support for Android Gradle Plugin 9.1.0
- the Embedded JDK (JBR) 21 selected as the Gradle JDK
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0 or newer
- Android NDK `28.2.13676358`
- CMake `3.22.1`

Install the Android components from **Tools > SDK Manager > SDK Tools**. Enable **Show Package Details** to select the exact NDK and CMake versions.

## Android Studio

1. Open the repository root, the folder containing `settings.gradle`.
2. In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, select **Embedded JDK 21**.
3. Select **File > Sync Project with Gradle Files**.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Android Studio creates the machine-specific `local.properties` file automatically. It must point `sdk.dir` to the Android SDK and must not be committed.

## Command line (optional)

The Gradle wrapper uses the same configuration as Android Studio:

```bat
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

On Linux or macOS, use `./gradlew` instead.

## Native libraries

TagLib is compiled by the Android Gradle Plugin and CMake as part of `:musikr`; the previous Bash preprocessing step is no longer used. The optional Media3 FFmpeg decoder is stored as four prebuilt Android libraries in `prebuilt/native/ffmpeg`, because the official Media3 FFmpeg build system does not support Windows. These are the same FLAC/ALAC decoder binaries shipped in KaraOk `4.1.5-karaok.4`.

The normal APK still includes both `libtagJNI.so` and `libffmpegJNI.so` for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.

## Troubleshooting

- If Android Studio reports an unsupported AGP version, verify that the project uses AGP `9.1.0` and Gradle `9.3.1`; do not accept an automatic upgrade.
- If Java 8 is selected, switch the Gradle JDK to Android Studio's Embedded JDK 21.
- If CMake or the NDK is missing, install the exact versions listed above and sync again.
- Do not run **Clean Project** merely to fix sync errors. Normal incremental rebuilds are much faster because TagLib does not need to be recompiled for every change.
