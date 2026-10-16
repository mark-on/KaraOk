/*
 * Copyright (c) 2026 Auxio Project
 * LrcLibClient.kt is part of Auxio.
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

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LyricsQuery(
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Long,
)

class LrcLibClient {
    suspend fun get(query: LyricsQuery): String? =
        withContext(Dispatchers.IO) {
            val parameters =
                mapOf(
                        "track_name" to query.trackName,
                        "artist_name" to query.artistName,
                        "album_name" to query.albumName,
                        "duration" to query.durationSeconds.toString(),
                    )
                    .entries
                    .joinToString("&") { (key, value) -> "$key=${value.urlEncoded()}" }
            val connection =
                URI("https://lrclib.net/api/get?$parameters").toURL().openConnection()
                    as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty(
                    "User-Agent",
                    "KaraOk/1.0 (https://github.com/mark-on/KaraOk)",
                )
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val json =
                            JSONObject(
                                connection.inputStream.bufferedReader().use { it.readText() }
                            )
                        json.optionalString("syncedLyrics") ?: json.optionalString("plainLyrics")
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> null
                    else -> null
                }
            } finally {
                connection.disconnect()
            }
        }

    private fun String.urlEncoded() = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)

    private companion object {
        private const val TIMEOUT_MS = 8000
    }
}
