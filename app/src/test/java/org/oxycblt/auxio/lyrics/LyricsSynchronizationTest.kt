/*
 * Copyright (c) 2026 Auxio Project
 * LyricsSynchronizationTest.kt is part of Auxio.
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
import org.junit.Test

class LyricsSynchronizationTest {
    @Test
    fun followsPlaybackSeekingAndDocumentChanges() {
        val first = requireNotNull(LrcParser.parse("[00:01]One\n[00:05]Two\n[00:10]Three"))
        assertEquals(-1, first.activeLineAt(500))
        assertEquals(0, first.activeLineAt(1000))
        assertEquals(2, first.activeLineAt(12000))
        assertEquals(0, first.activeLineAt(2500))

        val nextTrack = requireNotNull(LrcParser.parse("[00:00]New track\n[00:03]Next line"))
        assertEquals(0, nextTrack.activeLineAt(2500))
        assertEquals(1, nextTrack.activeLineAt(3500))
    }
}
