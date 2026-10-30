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
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface LyricsCache {
    suspend fun read(key: String): String?

    suspend fun write(key: String, lyrics: String)
}

class DiskLyricsCache(private val directory: File) : LyricsCache {
    private val writeMutex = Mutex()

    override suspend fun read(key: String): String? =
        withContext(Dispatchers.IO) {
            runCatching { fileFor(key).takeIf(File::isFile)?.readText() }.getOrNull()
        }

    override suspend fun write(key: String, lyrics: String) =
        withContext(Dispatchers.IO + NonCancellable) {
            writeMutex.withLock {
                if (!directory.isDirectory && !directory.mkdirs()) {
                    throw IOException("Unable to create lyrics directory: $directory")
                }
                val destination = fileFor(key)
                val temporary = File.createTempFile("${destination.name}.", ".tmp", directory)
                try {
                    temporary.writeAndSync(lyrics)
                    if (destination.exists() && !destination.delete()) {
                        throw IOException("Unable to replace cached lyrics: $destination")
                    }
                    if (!temporary.renameTo(destination)) {
                        destination.writeAndSync(lyrics)
                    }
                    if (!destination.isFile || destination.length() == 0L) {
                        throw IOException("Cached lyrics were not persisted: $destination")
                    }
                } finally {
                    temporary.delete()
                }
            }
        }

    private fun File.writeAndSync(text: String) {
        FileOutputStream(this).use { output ->
            output.write(text.toByteArray(StandardCharsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
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
        fallbackCacheKeys: List<String> = emptyList(),
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

        (listOf(cacheKey) + fallbackCacheKeys).distinct().forEach { key ->
            cache.read(key)?.let(LrcParser::parse)?.let {
                return it
            }
        }
        val downloaded = remoteSource.loadSafely() ?: run {
            embeddedFailure?.let { throw it }
            return null
        }
        val parsed = LrcParser.parse(downloaded) ?: run {
            embeddedFailure?.let { throw it }
            return null
        }
        // Once a remote response has been received, finish its small disk write even if the
        // screen or prefetch job is cancelled. A visible result must also survive the next launch.
        withContext(NonCancellable) { cache.write(cacheKey, downloaded) }
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
