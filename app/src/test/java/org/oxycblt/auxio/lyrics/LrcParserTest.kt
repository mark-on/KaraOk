/*
 * Copyright (c) 2026 Auxio Project
 * LrcParserTest.kt is part of Auxio.
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesTimestampsOffsetsAndRepeatedLines() {
        val lyrics =
            requireNotNull(
                LrcParser.parse(
                    """
                    [ar:Artist]
                    [offset:+250]
                    [00:01.50][00:03.500]Hello
                    [01:02]World
                    """
                        .trimIndent()
                )
            )
        assertTrue(lyrics.synchronized)
        assertEquals(listOf(1750L, 3750L, 62250L), lyrics.lines.map { it.timestampMs })
        assertEquals(listOf("Hello", "Hello", "World"), lyrics.lines.map { it.text })
    }

    @Test
    fun parsesPlainLyrics() {
        val lyrics = requireNotNull(LrcParser.parse("First line\n\nSecond line"))
        assertFalse(lyrics.synchronized)
        assertEquals("First line\nSecond line", lyrics.plainText)
    }

    @Test
    fun rejectsBlankInputAndMalformedSeconds() {
        assertEquals(null, LrcParser.parse("  \n "))
        assertEquals(null, LrcParser.parse("[00:99.00]Still readable"))
    }
}
