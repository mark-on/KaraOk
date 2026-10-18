package org.oxycblt.musikr.metadata

import java.util.Locale

/** Keep every lyrics frame: an empty or malformed language variant must not hide another one. */
object EmbeddedLyricsTags {
    fun candidates(
        id3v2: Map<String, List<String>>,
        xiph: Map<String, List<String>>,
        mp4: Map<String, List<String>>,
    ): List<String> {
        fun Map<String, List<String>>.valuesFor(vararg names: String): List<String> {
            val normalized = entries.groupBy { it.key.uppercase(Locale.ROOT) }
            return names.flatMap { name -> normalized[name].orEmpty().flatMap { it.value } }
        }
        return (
            id3v2.valuesFor("SYLT", "USLT", "TXXX:LYRICS", "TXXX:UNSYNCEDLYRICS",
                "TXXX:UNSYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCHRONIZEDLYRICS") +
                xiph.valuesFor("LYRICS", "SYNCEDLYRICS", "SYNCHRONIZEDLYRICS",
                    "UNSYNCEDLYRICS", "UNSYNCED LYRICS") +
                mp4.valuesFor("©LYR", "----:COM.APPLE.ITUNES:LYRICS",
                    "----:COM.APPLE.ITUNES:UNSYNCEDLYRICS")
            ).map { it.trim('\uFEFF', '\u0000', ' ', '\r', '\n', '\t') }
            .filter(String::isNotBlank)
            .distinct()
    }
}
