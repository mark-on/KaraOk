# FFmpeg Android binaries

This directory contains `libffmpegJNI.so` for the four Android ABIs supported by KaraOk. They were extracted from the verified KaraOk `4.1.5-karaok.4` build and correspond to the Media3 1.11.0 FFmpeg bridge configured with the FLAC and ALAC decoders.

Media3's FFmpeg extension is not distributed as a ready-to-use Google Maven artifact and its upstream build task explicitly requires a Unix host. Keeping the verified Android binaries here allows Android Studio to build KaraOk directly on Windows without removing FFmpeg playback support.

The corresponding Java bridge remains source-built in the `ffmpeg` module. The matching Media3 extension and FFmpeg source remain available in the repository's pinned `media` submodule. Checksums for the distributed binaries are recorded in `SHA256SUMS`.
