/*
 * Copyright (c) 2026 Auxio Project
 * LocalLyricsReader.kt is part of Auxio.
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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Song

class LocalLyricsReader(private val context: Context) {
    suspend fun read(song: Song): String? =
        withContext(Dispatchers.IO) {
            val audioName = song.path.name ?: return@withContext null
            val lrcName = "${audioName.substringBeforeLast('.', audioName)}.lrc"
            readDocumentSibling(song.uri, lrcName)
                ?: readMediaStoreSibling(song.uri, lrcName)
                ?: readLegacyFile(song, lrcName)
        }

    private fun readDocumentSibling(songUri: Uri, lrcName: String): String? {
        if (!DocumentsContract.isDocumentUri(context, songUri)) return null
        return runCatching {
                val documentId = DocumentsContract.getDocumentId(songUri)
                val separator = documentId.lastIndexOf('/')
                if (separator < 0) return@runCatching null
                val siblingId = documentId.substring(0, separator + 1) + lrcName
                val sibling =
                    if (DocumentsContract.isTreeUri(songUri)) {
                        DocumentsContract.buildDocumentUriUsingTree(songUri, siblingId)
                    } else {
                        DocumentsContract.buildDocumentUri(songUri.authority, siblingId)
                    }
                context.contentResolver.openInputStream(sibling)?.use { stream ->
                    stream.bufferedReader().use { it.readTextLimited() }
                }
            }
            .getOrNull()
    }

    private fun readMediaStoreSibling(songUri: Uri, lrcName: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val projection =
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.VOLUME_NAME)
        val location =
            runCatching {
                    resolver.query(songUri, projection, null, null, null)?.use { cursor ->
                        if (!cursor.moveToFirst()) return@use null
                        cursor.getString(0) to cursor.getString(1)
                    }
                }
                .getOrNull() ?: return null
        val collection = MediaStore.Files.getContentUri(location.second)
        val candidate =
            runCatching {
                    resolver
                        .query(
                            collection,
                            arrayOf(MediaStore.MediaColumns._ID),
                            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                            arrayOf(location.first, lrcName),
                            null,
                        )
                        ?.use { cursor ->
                            if (!cursor.moveToFirst()) return@use null
                            Uri.withAppendedPath(collection, cursor.getLong(0).toString())
                        }
                }
                .getOrNull() ?: return null
        return resolver.readText(candidate)
    }

    private fun readLegacyFile(song: Song, lrcName: String): String? {
        val volume = song.path.volume.components ?: return null
        val directory =
            (volume.components + song.path.directory.components.components).joinToString(
                File.separator
            )
        val file = File(File.separator + directory, lrcName)
        return runCatching {
                file.takeIf(File::isFile)?.bufferedReader()?.use { it.readTextLimited() }
            }
            .getOrNull()
    }

    private fun ContentResolver.readText(uri: Uri): String? =
        runCatching { openInputStream(uri)?.bufferedReader()?.use { it.readTextLimited() } }
            .getOrNull()

    private fun java.io.BufferedReader.readTextLimited(): String {
        val output = StringBuilder()
        val buffer = CharArray(8192)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            output.append(buffer, 0, read)
            if (output.length > MAX_LYRICS_CHARS) {
                throw IllegalArgumentException("Lyrics file is too large")
            }
        }
        return output.toString()
    }

    private companion object {
        private const val MAX_LYRICS_CHARS = 1_000_000
    }
}
