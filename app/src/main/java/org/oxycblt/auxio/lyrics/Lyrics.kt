/*
 * Copyright (c) 2026 Auxio Project
 * Lyrics.kt is part of Auxio.
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

data class LyricsLine(val timestampMs: Long?, val text: String)

data class LyricsDocument(val lines: List<LyricsLine>, val synchronized: Boolean) {
    fun activeLineAt(positionMs: Long): Int {
        if (!synchronized || lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high).ushr(1)
            val timestamp = lines[middle].timestampMs ?: Long.MAX_VALUE
            if (timestamp <= positionMs) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return result
    }

    val plainText: String
        get() = lines.joinToString("\n") { it.text }
}

object LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val metadata = Regex("^\\[(ar|al|ti|au|by|re|ve|length):.*]$", RegexOption.IGNORE_CASE)
    private val offset = Regex("^\\[offset:([+-]?\\d+)]$", RegexOption.IGNORE_CASE)

    fun parse(raw: String): LyricsDocument? {
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isBlank()) return null

        var offsetMs = 0L
        normalized.lineSequence().forEach { line ->
            offset.matchEntire(line.trim())?.groupValues?.get(1)?.toLongOrNull()?.let {
                offsetMs = it
            }
        }

        val synced = mutableListOf<LyricsLine>()
        val plain = mutableListOf<LyricsLine>()
        normalized.lineSequence().forEach { sourceLine ->
            val line = sourceLine.trim()
            if (line.isEmpty() || metadata.matches(line) || offset.matches(line)) return@forEach
            val matches = timestamp.findAll(line).toList()
            val text = timestamp.replace(line, "").trim()
            if (matches.isEmpty()) {
                plain += LyricsLine(null, line)
            } else {
                matches.forEach matchLoop@{ match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@matchLoop
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@matchLoop
                    if (seconds > 59) return@matchLoop
                    val fraction = match.groupValues[3]
                    val millis =
                        when (fraction.length) {
                            0 -> 0L
                            1 -> fraction.toLong() * 100
                            2 -> fraction.toLong() * 10
                            else -> fraction.take(3).toLong()
                        }
                    val position =
                        (minutes * 60_000 + seconds * 1000 + millis + offsetMs).coerceAtLeast(0)
                    synced += LyricsLine(position, text)
                }
            }
        }

        if (synced.isNotEmpty()) {
            return LyricsDocument(synced.sortedBy { it.timestampMs }, synchronized = true)
        }
        return plain.takeIf { it.isNotEmpty() }?.let { LyricsDocument(it, synchronized = false) }
    }
}
