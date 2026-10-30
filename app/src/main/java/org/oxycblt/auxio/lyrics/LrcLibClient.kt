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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

data class LyricsQuery(
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Long,
)

class LrcLibClient {
    suspend fun get(query: LyricsQuery): String? =
        withContext(Dispatchers.IO) {
            val exactParameters =
                buildMap {
                    put("track_name", query.trackName)
                    put("artist_name", query.artistName)
                    if (query.albumName.isNotBlank()) put("album_name", query.albumName)
                    if (query.durationSeconds in 1..3600) {
                        put("duration", query.durationSeconds.toString())
                    }
                }
            when (val exact = request("/api/get", exactParameters)) {
                is ApiResponse.Success -> return@withContext exact.body.recordLyrics()
                ApiResponse.Failed -> return@withContext null
                ApiResponse.NotFound -> Unit
            }

            // Exact lookup is intentionally strict about metadata and duration. A structured
            // search recovers legitimate matches with a different album tag while still applying
            // conservative title, artist, and duration checks locally.
            delay(REQUEST_SPACING_MS)
            val searchParameters =
                mapOf("track_name" to query.trackName, "artist_name" to query.artistName)
            when (val search = request("/api/search", searchParameters)) {
                is ApiResponse.Success -> search.body.bestRecordLyrics(query)
                ApiResponse.Failed, ApiResponse.NotFound -> null
            }
        }

    private suspend fun request(path: String, parameters: Map<String, String>): ApiResponse {
        val encoded =
            parameters.entries.joinToString("&") { (key, value) ->
                "$key=${value.urlEncoded()}"
            }
        val url = URI("https://lrclib.net$path?$encoded").toURL()
        repeat(MAX_ATTEMPTS) { attempt ->
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty(
                    "User-Agent",
                    "KaraOk/1.0 (https://github.com/mark-on/KaraOk)",
                )
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK ->
                        return ApiResponse.Success(
                            connection.inputStream.bufferedReader().use { it.readText() }
                        )
                    HttpURLConnection.HTTP_NOT_FOUND -> return ApiResponse.NotFound
                    HTTP_TOO_MANY_REQUESTS,
                    in 500..599 -> {
                        if (attempt == MAX_ATTEMPTS - 1) return ApiResponse.Failed
                        val retryAfterMs =
                            connection.getHeaderField("Retry-After")
                                ?.toLongOrNull()
                                ?.times(1000L)
                        delay(retryAfterMs ?: RETRY_DELAYS_MS[attempt])
                    }
                    else -> return ApiResponse.Failed
                }
            } catch (error: java.io.IOException) {
                if (attempt == MAX_ATTEMPTS - 1) return ApiResponse.Failed
                delay(RETRY_DELAYS_MS[attempt])
            } finally {
                connection.disconnect()
            }
        }
        return ApiResponse.Failed
    }

    private fun String.urlEncoded() = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun JSONObject.optionalString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)

    private fun String.recordLyrics(): String? =
        JSONObject(this).let { it.optionalString("syncedLyrics") ?: it.optionalString("plainLyrics") }

    private fun String.bestRecordLyrics(query: LyricsQuery): String? {
        val records = JSONArray(this)
        val expectedTrack = query.trackName.normalized()
        val expectedArtist = query.artistName.normalized()
        return (0 until records.length())
            .mapNotNull { records.optJSONObject(it) }
            .filter { record ->
                val track = record.optString("trackName").normalized()
                val artist = record.optString("artistName").normalized()
                val trackMatches = track == expectedTrack
                val artistMatches =
                    artist == expectedArtist ||
                        artist.contains(expectedArtist) ||
                        expectedArtist.contains(artist)
                val durationMatches =
                    query.durationSeconds !in 1..3600 ||
                        abs(record.optLong("duration") - query.durationSeconds) <= 5
                trackMatches && artistMatches && durationMatches
            }
            .maxByOrNull { record ->
                var score = 0
                if (record.optString("albumName").normalized() == query.albumName.normalized()) {
                    score += 2
                }
                if (record.optionalString("syncedLyrics") != null) score += 1
                score
            }
            ?.let { it.optionalString("syncedLyrics") ?: it.optionalString("plainLyrics") }
    }

    private fun String.normalized() =
        lowercase().replace(NON_ALPHANUMERIC, " ").trim().replace(MULTIPLE_SPACES, " ")

    private sealed interface ApiResponse {
        data class Success(val body: String) : ApiResponse

        data object NotFound : ApiResponse

        data object Failed : ApiResponse
    }

    private companion object {
        private const val TIMEOUT_MS = 8000
        private const val MAX_ATTEMPTS = 3
        private const val REQUEST_SPACING_MS = 300L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val RETRY_DELAYS_MS = longArrayOf(750L, 1500L)
        private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
        private val MULTIPLE_SPACES = Regex("\\s+")
    }
}
