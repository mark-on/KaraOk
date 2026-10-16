<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="150" alt="KaraOk icon">
</p>

<h1 align="center"><b>KaraOk</b></h1>
<h4 align="center">Auxio, with lyrics. Minimal, local, and synchronized.</h4>

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
  <a href="https://github.com/OxygenCobalt/Auxio"><b>Original Auxio project</b></a>
  ·
  <a href="LICENSE"><b>License</b></a>
</p>

## What is KaraOk?

**KaraOk is a fork of [Auxio](https://github.com/OxygenCobalt/Auxio) that adds lyrics directly to the Now Playing screen.**

It preserves Auxio's Material You design, local playback, performance, and minimalist approach. Tapping the album artwork opens a clean lyrics overlay: the player remains visible in the background, lyrics scroll above it, and playback continues uninterrupted.

KaraOk uses a separate application ID (`com.markon.karaok`), so it can be installed alongside Auxio.

## Lyrics

KaraOk looks for lyrics in this order:

1. lyrics embedded in the audio file's metadata;
2. a local `.lrc` file associated with the track;
3. the [LRCLIB](https://lrclib.net) online service.

Supported features include:

- synchronized LRC lyrics with active-line highlighting;
- automatic scrolling based on the current playback position;
- temporary auto-scroll suspension while scrolling manually;
- unsynchronized lyrics in a clean, scrollable view;
- updates after pausing, seeking, skipping, or changing tracks;
- local caching for downloaded lyrics;
- graceful handling of offline use, missing lyrics, and malformed LRC files.

> KaraOk currently **reads** embedded lyrics but does not modify audio-file metadata. Lyrics retrieved from LRCLIB are stored in the app's local cache.

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

<p align="center"><i>The base interface and screenshots come from Auxio; KaraOk intentionally preserves the same visual language.</i></p>

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

The project requires Java 21, Android SDK 37, NDK `28.2.13676358`, CMake `3.22.1`, and a Unix environment. WSL2 is recommended on Windows.

```bash
git clone --recurse-submodules https://github.com/mark-on/KaraOk.git
cd KaraOk
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The APK is generated in `app/build/outputs/apk/debug/`.

## Permissions

- audio-file access, to index and play music;
- foreground service and wake lock, for background playback;
- notifications, for media controls;
- internet access, used only for optional lyrics retrieval from LRCLIB.

No additional permission is requested to find local LRC files.

## Credits and license

KaraOk is based on **Auxio**, created and maintained by [OxygenCobalt](https://github.com/OxygenCobalt), and retains the original project's copyright notices. Thanks to the contributors behind Auxio, Media3, TagLib, FFmpeg, and LRCLIB.

KaraOk is free software distributed under the [GNU General Public License v3](LICENSE). You may use, study, modify, and redistribute it in accordance with the license and the original attributions.
