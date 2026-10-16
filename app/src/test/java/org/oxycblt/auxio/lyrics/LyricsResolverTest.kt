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
import org.junit.Test

class LyricsResolverTest {
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
}
