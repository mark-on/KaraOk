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
import android.os.ParcelFileDescriptor
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.fs.AddedMs
import org.oxycblt.musikr.fs.File

/** Reads lyrics tags from a song without rebuilding the music library. */
object EmbeddedLyrics {
    suspend fun readCandidates(context: Context, song: Song): List<String> =
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
            val descriptor = context.contentResolver.openFileDescriptor(song.uri, "r")
                ?: throw IOException("Audio provider returned no file descriptor")
            // One owner for the descriptor, including when TagLib throws.
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
                when (val result = TagLibJNI.open(file, stream)) {
                    is MetadataResult.Success -> result.metadata?.let {
                        EmbeddedLyricsTags.candidates(it.id3v2, it.xiph, it.mp4)
                    }.orEmpty()
                    MetadataResult.ProviderFailed -> throw IOException("Could not read audio metadata")
                    MetadataResult.NoMetadata, MetadataResult.NotAudio ->
                        throw IOException("Audio metadata could not be decoded")
                }
            }
        }
}
