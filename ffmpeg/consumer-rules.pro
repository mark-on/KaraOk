# Keep native entry points callable after release minification.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Called from libffmpegJNI.so.
-keep,includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
    private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}
