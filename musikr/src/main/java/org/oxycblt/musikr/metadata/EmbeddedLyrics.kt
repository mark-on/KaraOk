/*
 * Copyright (c) 2026 Auxio Project
 * EmbeddedLyrics.kt is part of Auxio.
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

package org.oxycblt.musikr.metadata

import android.content.Context
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.fs.AddedMs
import org.oxycblt.musikr.fs.File

/** Reads lyrics tags from a song without rebuilding the music library. */
object EmbeddedLyrics {
    suspend fun read(context: Context, song: Song): String? =
        withContext(Dispatchers.IO) {
            val file =
                File(
                    uri = song.uri,
                    path = song.path,
                    addedMs =
                        object : AddedMs {
                            override suspend fun resolve() = song.addedMs
                        },
                    modifiedMs = song.modifiedMs,
                    mimeType = "audio/*",
                    size = song.size,
                    parent = null,
                )
            context.contentResolver.openFileDescriptor(song.uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { stream ->
                    when (val result = TagLibJNI.open(file, stream)) {
                        is MetadataResult.Success -> result.metadata?.lyrics()
                        else -> null
                    }
                }
            }
        }

    private fun Metadata.lyrics(): String? =
        sequenceOf(
                id3v2["SYLT"],
                id3v2["USLT"],
                id3v2["TXXX:LYRICS"],
                id3v2["TXXX:UNSYNCEDLYRICS"],
                xiph["LYRICS"],
                xiph["UNSYNCEDLYRICS"],
                xiph["UNSYNCED LYRICS"],
                mp4["©lyr"],
                mp4["----:COM.APPLE.ITUNES:LYRICS"],
            )
            .filterNotNull()
            .flatten()
            .firstOrNull { it.isNotBlank() }
}
