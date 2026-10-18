package org.oxycblt.musikr.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddedLyricsTagsTest {
    @Test
    fun preservesRepeatedId3LyricsFramesAndTheirLineBreaks() {
        val tags = NativeTagMap()
        tags.addID("USLT", "First line\nSecond line")
        tags.addID("USLT", "[ar:Metadata only]")
        tags.addID("USLT", "   ")
        tags.addID("USLT", "First line\nSecond line")
        assertEquals(
            listOf("First line\nSecond line", "[ar:Metadata only]"),
            EmbeddedLyricsTags.candidates(tags.getObject(), emptyMap(), emptyMap()),
        )
    }

    @Test
    fun synchronizedId3HasPriorityAndPlainTextRemainsAvailable() {
        assertEquals(
            listOf("[00:01.00]Timed", "Plain"),
            EmbeddedLyricsTags.candidates(
                mapOf("USLT" to listOf("Plain"), "SYLT" to listOf("[00:01.00]Timed")),
                emptyMap(), emptyMap(),
            ),
        )
    }

    @Test
    fun handlesFlacAliasesCaseBomAndEmptyFrames() {
        assertEquals(
            listOf("First\nSecond", "Alternate"),
            EmbeddedLyricsTags.candidates(
                emptyMap(),
                mapOf("lyrics" to listOf("\uFEFFFirst\nSecond\u0000", " "),
                    "UNSYNCED LYRICS" to listOf("Alternate")),
                emptyMap(),
            ),
        )
    }

    @Test
    fun supportsMp4StandardAndCustomLyrics() {
        assertEquals(
            listOf("Standard", "Custom"),
            EmbeddedLyricsTags.candidates(
                emptyMap(), emptyMap(),
                mapOf("©lyr" to listOf("Standard"),
                    "----:com.apple.iTunes:LYRICS" to listOf("Custom")),
            ),
        )
    }

    @Test
    fun doesNotChangeOrdinaryMetadataReplacement() {
        val tags = NativeTagMap()
        tags.addID("TIT2", "Old title")
        tags.addID("TIT2", "New title")
        assertEquals(listOf("New title"), tags.getObject()["TIT2"])
    }
}
