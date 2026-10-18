/*
 * Copyright (c) 2026 Auxio Project
 * LyricsResolverTest.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oxycblt.auxio.lyrics

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.oxycblt.musikr.metadata.EmbeddedLyricsTags

class LyricsResolverTest {
    private fun emptyCache() = object : LyricsCache {
        override suspend fun read(key: String): String? = null
        override suspend fun write(key: String, lyrics: String) = Unit
    }

    @Test
    fun malformedEmbeddedFrameDoesNotHideAnotherFrame() = runBlocking {
        val candidates = EmbeddedLyricsTags.candidates(
            mapOf("SYLT" to listOf("[ar:No lines]"), "USLT" to listOf("Embedded\nSecond line")),
            emptyMap(), emptyMap(),
        )
        val document = LyricsResolver(emptyCache()).resolve(
            "song", listOf(LyricsSource { candidates.firstOrNull { LrcParser.parse(it) != null } }),
            LyricsSource { error("Must not contact the network") },
        )
        assertEquals("Embedded\nSecond line", requireNotNull(document).plainText)
    }

    @Test
    fun reportsEmbeddedReadFailureInsteadOfClaimingNoLyrics() = runBlocking {
        val failure = EmbeddedLyricsReadException(java.io.IOException("Unavailable file"))
        try {
            LyricsResolver(emptyCache()).resolve(
                "song", listOf(LyricsSource { throw failure }), LyricsSource { null },
            )
            fail("Expected the embedded read failure")
        } catch (actual: EmbeddedLyricsReadException) {
            assertSame(failure, actual)
        }
    }

    @Test
    fun localSidecarRemainsUsableAfterEmbeddedReadFailure() = runBlocking {
        val document = LyricsResolver(emptyCache()).resolve(
            "song", listOf(
                LyricsSource { throw EmbeddedLyricsReadException(java.io.IOException()) },
                LyricsSource { "[00:01.00]Local line" },
            ), LyricsSource { error("Must not contact the network") },
        )
        assertEquals("Local line", requireNotNull(document).lines.single().text)
    }

    @Test
    fun malformedRemoteResponseDoesNotHideEmbeddedReadFailure() = runBlocking {
        val failure = EmbeddedLyricsReadException(java.io.IOException())
        try {
            LyricsResolver(emptyCache()).resolve(
                "song", listOf(LyricsSource { throw failure }), LyricsSource { "[ar:No lines]" },
            )
            fail("Expected the embedded read failure")
        } catch (actual: EmbeddedLyricsReadException) {
            assertSame(failure, actual)
        }
    }

    @Test
    fun usesSourcesInPriorityOrder() = runBlocking {
        val cacheDirectory = Files.createTempDirectory("lyrics-cache").toFile()
        try {
            var remoteCalls = 0
            val resolver = LyricsResolver(DiskLyricsCache(cacheDirectory))
            val lyrics =
                resolver.resolve(
                    "song",
                    listOf(LyricsSource { null }, LyricsSource { "Local lyrics" }),
                    LyricsSource {
                        remoteCalls++
                        "Remote lyrics"
                    },
                )
            assertEquals("Local lyrics", requireNotNull(lyrics).plainText)
            assertEquals(0, remoteCalls)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun cachesRemoteLyricsAndSurvivesLaterNetworkFailure() = runBlocking {
        val cacheDirectory = Files.createTempDirectory("lyrics-cache").toFile()
        try {
            val resolver = LyricsResolver(DiskLyricsCache(cacheDirectory))
            val downloaded =
                resolver.resolve("song", emptyList(), LyricsSource { "[00:01.00]Cached" })
            assertEquals("Cached", requireNotNull(downloaded).lines.single().text)

            val cached = resolver.resolve("song", emptyList(), LyricsSource { error("offline") })
            assertEquals("Cached", requireNotNull(cached).lines.single().text)
            assertNull(
                resolver.resolve("other-song", emptyList(), LyricsSource { error("offline") })
            )
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun disabledAutomaticSearchStillUsesSavedLyricsWithoutCallingNetwork() = runBlocking {
        val cacheDirectory = Files.createTempDirectory("lyrics-cache").toFile()
        try {
            val resolver = LyricsResolver(DiskLyricsCache(cacheDirectory))
            val disabledNetwork = LyricsSource { null }
            assertNull(resolver.resolve("missing", emptyList(), disabledNetwork))

            resolver.resolve("saved", emptyList(), LyricsSource { "[00:01]Saved line" })
            val saved = resolver.resolve("saved", emptyList(), disabledNetwork)
            assertEquals("Saved line", requireNotNull(saved).lines.single().text)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }
}
