/*
 * Copyright (c) 2026 Auxio Project
 * LyricsRepository.kt is part of Auxio.
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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.metadata.EmbeddedLyrics

@Singleton
class LyricsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val localLyrics = LocalLyricsReader(context)
    private val remoteLyrics = LrcLibClient()
    private val resolver = LyricsResolver(DiskLyricsCache(context.cacheDir.resolve("lyrics")))

    suspend fun get(song: Song): LyricsDocument? {
        val query =
            LyricsQuery(
                trackName = song.name.resolve(context),
                artistName = song.artists.resolveNames(context),
                albumName = song.album.name.resolve(context),
                durationSeconds = (song.durationMs / 1000).coerceAtLeast(0),
            )
        val cacheKey =
            listOf(
                    query.trackName,
                    query.artistName,
                    query.albumName,
                    query.durationSeconds.toString(),
                )
                .joinToString("\u0000")
        return resolver.resolve(
            cacheKey = cacheKey,
            primarySources =
                listOf(
                    LyricsSource { EmbeddedLyrics.read(context, song) },
                    LyricsSource { localLyrics.read(song) },
                ),
            remoteSource = LyricsSource { remoteLyrics.get(query) },
        )
    }
}
