/*
 * Copyright (c) 2026 Auxio Project
 * LyricsCache.kt is part of Auxio.
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

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LyricsCache {
    suspend fun read(key: String): String?

    suspend fun write(key: String, lyrics: String)
}

class DiskLyricsCache(private val directory: File) : LyricsCache {
    override suspend fun read(key: String): String? =
        withContext(Dispatchers.IO) {
            runCatching { fileFor(key).takeIf(File::isFile)?.readText() }.getOrNull()
        }

    override suspend fun write(key: String, lyrics: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                directory.mkdirs()
                val destination = fileFor(key)
                val temporary = File(directory, "${destination.name}.tmp")
                temporary.writeText(lyrics)
                if (!temporary.renameTo(destination)) {
                    destination.writeText(lyrics)
                    temporary.delete()
                }
            }
            Unit
        }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        return File(directory, "$name.lrc")
    }
}

fun interface LyricsSource {
    suspend fun load(): String?
}

class LyricsResolver(private val cache: LyricsCache) {
    suspend fun resolve(
        cacheKey: String,
        primarySources: List<LyricsSource>,
        remoteSource: LyricsSource,
    ): LyricsDocument? {
        var embeddedFailure: EmbeddedLyricsReadException? = null
        for (source in primarySources) {
            val raw = try {
                source.loadSafely()
            } catch (error: EmbeddedLyricsReadException) {
                embeddedFailure = error
                null
            }
            val parsed = raw?.let(LrcParser::parse)
            if (parsed != null) return parsed
        }

        cache.read(cacheKey)?.let(LrcParser::parse)?.let {
            return it
        }
        val downloaded = remoteSource.loadSafely() ?: run {
            embeddedFailure?.let { throw it }
            return null
        }
        val parsed = LrcParser.parse(downloaded) ?: run {
            embeddedFailure?.let { throw it }
            return null
        }
        cache.write(cacheKey, downloaded)
        return parsed
    }

    private suspend fun LyricsSource.loadSafely(): String? =
        try {
            load()
        } catch (error: CancellationException) {
            throw error
        } catch (error: EmbeddedLyricsReadException) {
            throw error
        } catch (_: Exception) {
            null
        }
}

class EmbeddedLyricsReadException(cause: Throwable) :
    IOException("Embedded lyrics could not be read", cause)
