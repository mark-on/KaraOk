/*
 * Copyright (c) 2026 Auxio Project
 * LyricsViewModel.kt is part of Auxio.
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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.oxycblt.musikr.Song

sealed interface LyricsLoadState {
    data object Idle : LyricsLoadState

    data object Loading : LyricsLoadState

    data class Ready(val lyrics: LyricsDocument) : LyricsLoadState

    data object Unavailable : LyricsLoadState
}

@HiltViewModel
class LyricsViewModel
@Inject
constructor(
    private val repository: LyricsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val isOpen: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_OPEN, false)

    val state: StateFlow<LyricsLoadState>
        field = MutableStateFlow<LyricsLoadState>(LyricsLoadState.Idle)

    private var currentSong: Song? = null
    private var loadJob: Job? = null

    fun open(song: Song) {
        currentSong = song
        savedStateHandle[KEY_OPEN] = true
        load(song)
    }

    fun close() {
        savedStateHandle[KEY_OPEN] = false
    }

    fun onSongChanged(song: Song?) {
        if (song == currentSong) return
        currentSong = song
        if (isOpen.value && song != null) load(song)
    }

    private fun load(song: Song) {
        loadJob?.cancel()
        state.value = LyricsLoadState.Loading
        loadJob = viewModelScope.launch {
            state.value =
                repository.get(song)?.let(LyricsLoadState::Ready) ?: LyricsLoadState.Unavailable
        }
    }

    private companion object {
        private const val KEY_OPEN = "lyrics_open"
    }
}
