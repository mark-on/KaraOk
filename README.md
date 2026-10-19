<p align="center">
  <img src="app/src/main/res/drawable-nodpi/karaok_logo.png" width="150" alt="KaraOk dinosaur logo">
</p>

<h1 align="center"><b>KaraOk</b></h1>
<h4 align="center">Auxio, with lyrics. Minimal, local, and synchronized.</h4>

<p align="center">
  <a href="https://github.com/mark-on/KaraOk/releases/latest">
    <img alt="Latest release" src="https://img.shields.io/github/v/release/mark-on/KaraOk?color=C28AFA&style=flat">
  </a>
  <a href="https://github.com/mark-on/KaraOk/releases">
    <img alt="Downloads" src="https://img.shields.io/github/downloads/mark-on/KaraOk/total.svg?color=60DEC7&style=flat">
  </a>
  <a href="https://www.gnu.org/licenses/gpl-3.0">
    <img alt="GPLv3" src="https://img.shields.io/badge/license-GPL%20v3-2B6DBE.svg?style=flat">
  </a>
  <img alt="Android 7+" src="https://img.shields.io/badge/Android-7.0%2B-1450A8?style=flat">
</p>

<p align="center">
  <a href="https://github.com/mark-on/KaraOk/releases/latest"><b>Download</b></a>
  ·
  <a href="https://github.com/OxygenCobalt/Auxio"><b>Original Auxio project</b></a>
  ·
  <a href="LICENSE"><b>License</b></a>
</p>

## What is KaraOk?

**KaraOk is a fork of [Auxio](https://github.com/OxygenCobalt/Auxio) that adds lyrics directly to the Now Playing screen.**

It preserves Auxio's Material You foundations, local playback, performance, and minimalist approach. KaraOk adds its own violet-and-mint theme and dinosaur launcher artwork. Tapping the album artwork or its lyrics icon opens a clean lyrics overlay: the player remains visible in the background, lyrics scroll above it, and playback continues uninterrupted.

KaraOk uses a separate application ID (`com.markon.karaok`), so it can be installed alongside Auxio.

## Lyrics

KaraOk looks for lyrics in this order:

1. lyrics embedded in the audio file's metadata;
2. a local `.lrc` file associated with the track;
3. previously downloaded lyrics in the app's persistent local storage;
4. the [LRCLIB](https://lrclib.net) online service, when requested manually or allowed by the automatic-search setting.

Supported features include:

- synchronized LRC lyrics with active-line highlighting;
- automatic scrolling based on the current playback position;
- temporary auto-scroll suspension while scrolling manually;
- unsynchronized lyrics in a clean, scrollable view;
- updates after pausing, seeking, skipping, or changing tracks;
- persistent local storage for downloaded lyrics;
- optional automatic searching for the current and next track, with a Wi-Fi-only setting;
- a manual Search online action when lyrics are missing;
- graceful handling of offline use, missing lyrics, and malformed LRC files.

> KaraOk currently **reads** embedded lyrics but does not modify audio-file metadata. Automatic online searching is off by default. Downloaded lyrics remain in the app's private files until app data is cleared.

## Screenshots

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot0.png" width="245" alt="Music library">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot1.png" width="245" alt="Albums">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot2.png" width="245" alt="Now Playing">
</p>
<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot3.png" width="245" alt="Queue">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot4.png" width="245" alt="Search">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot5.png" width="245" alt="Settings">
</p>

<p align="center"><i>These are inherited Auxio screenshots, not captures of the current KaraOk redesign. Updated screenshots will follow device testing.</i></p>

## Core features

- playback powered by Media3 ExoPlayer;
- an edge-to-edge Material You interface;
- a fully local music library;
- embedded artwork and lyrics support;
- local LRC files and LRCLIB retrieval;
- ReplayGain for MP3, FLAC, OGG, Opus, and MP4;
- gapless playback;
- playlists, search, widgets, and Android Auto;
- SD-card-compatible folder management;
- no account and no tracking.

## Installation

Download the APK from the [Releases](https://github.com/mark-on/KaraOk/releases) page. Android may ask you to allow installation from external sources.

Early KaraOk builds are published as **debug-signed prereleases** intended for testing and development. A future stable release will require a dedicated signing key that is kept safely over time.

## Building

KaraOk can be built directly in Android Studio on Windows, Linux, or macOS. The project requires Java 21, Android SDK 36, NDK `28.2.13676358`, and CMake `3.22.1`; no WSL or Unix shell is required for a normal build.

```bash
git clone --recurse-submodules https://github.com/mark-on/KaraOk.git
cd KaraOk
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The APK is generated in `app/build/outputs/apk/debug/`.

See [BUILDING.md](BUILDING.md) for the Android Studio setup and troubleshooting steps.

## Permissions

- audio-file access, to index and play music;
- foreground service and wake lock, for background playback;
- notifications, for media controls;
- internet access, used only for optional lyrics retrieval from LRCLIB;
- network-state access, used to honor the Wi-Fi-only automatic-download setting.

No additional permission is requested to find local LRC files.

## Credits and license

KaraOk is based on **Auxio**, created and maintained by [OxygenCobalt](https://github.com/OxygenCobalt), and retains the original project's copyright notices. Thanks to the contributors behind Auxio, Media3, TagLib, FFmpeg, and LRCLIB.

KaraOk is free software distributed under the [GNU General Public License v3](LICENSE). You may use, study, modify, and redistribute it in accordance with the license and the original attributions.
