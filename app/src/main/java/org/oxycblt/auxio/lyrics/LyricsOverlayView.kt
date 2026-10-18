/*
 * Copyright (c) 2026 Auxio Project
 * LyricsOverlayView.kt is part of Auxio.
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
import android.graphics.Color
import android.graphics.Typeface
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import org.oxycblt.auxio.R
import org.oxycblt.auxio.image.CoverView
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.auxio.playback.msToDs
import org.oxycblt.auxio.playback.ui.StyledSeekBar
import org.oxycblt.musikr.Song

class LyricsOverlayView(context: Context) : FrameLayout(context) {
    private val surfaceColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK)
    private val onSurfaceColor =
        MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnSurface,
            Color.WHITE,
        )
    private val secondaryColor =
        MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            onSurfaceColor,
        )
    private val accentColor =
        MaterialColors.getColor(
            this,
            androidx.appcompat.R.attr.colorPrimary,
            onSurfaceColor,
        )

    private val progress = ProgressBar(context)
    private val status = TextView(context)
    private val statusScroll = ScrollView(context)
    private val plainLyrics = TextView(context)
    private val plainScroll = ScrollView(context)
    private val syncedLyrics = RecyclerView(context)
    private val syncedAdapter = SyncedLyricsAdapter()
    private val close = AppCompatImageButton(context)
    private val searchOnline = MaterialButton(context)
    private val retry = MaterialButton(context)
    private val songCover = CoverView(context)
    private val seekBar = StyledSeekBar(context)
    private val playPause = AppCompatImageButton(context)
    private val footer = LinearLayout(context)
    private val column = LinearLayout(context)
    private val content = FrameLayout(context)
    private val songTitle = TextView(context)
    private val songArtist = TextView(context)
    private val header = LinearLayout(context)

    private var activeLine = -1
    private var autoScrollAfterMs = 0L

    var onClose: (() -> Unit)? = null
    var onSearchOnline: (() -> Unit)? = null
    var onRetry: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onTogglePlaying: (() -> Unit)? = null
    var onQueue: (() -> Unit)? = null
    var onSeek: ((Long) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        visibility = View.GONE
        setBackgroundColor(surfaceColor)
        column.orientation = LinearLayout.VERTICAL
        addView(column, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val toolbar = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(action(R.drawable.ic_back_24, R.string.desc_close_lyrics) { onClose?.invoke() })
        toolbar.addView(TextView(context).apply {
            setText(R.string.set_lyrics)
            setTextColor(onSurfaceColor)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        column.addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        progress.visibility = View.GONE
        content.addView(progress, centeredParams(WRAP_CONTENT, WRAP_CONTENT))

        status.apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(secondaryColor)
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )
            setPadding(dp(32), dp(24), dp(32), dp(24))
            visibility = View.GONE
        }
        val emptyState = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
        }
        emptyState.addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        statusScroll.apply {
            isFillViewport = true
            addView(emptyState, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            visibility = View.GONE
        }
        content.addView(statusScroll, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        searchOnline.apply {
            setText(R.string.lbl_lyrics_search_online)
            setOnClickListener { onSearchOnline?.invoke() }
            visibility = View.GONE
        }
        emptyState.addView(searchOnline)
        retry.apply {
            setText(R.string.lbl_lyrics_retry)
            setOnClickListener { onRetry?.invoke() }
            visibility = View.GONE
        }
        emptyState.addView(retry)

        plainLyrics.apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(onSurfaceColor)
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
            )
            setLineSpacing(0f, 1.35f)
            setPadding(dp(28), dp(32), dp(28), dp(32))
        }
        plainScroll.apply {
            isFillViewport = true
            clipToPadding = false
            addView(plainLyrics, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            visibility = View.GONE
        }
        content.addView(plainScroll, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        syncedLyrics.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = syncedAdapter
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            visibility = View.GONE
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            autoScrollAfterMs = Long.MAX_VALUE
                        } else if (newState == RecyclerView.SCROLL_STATE_IDLE && autoScrollAfterMs == Long.MAX_VALUE) {
                            autoScrollAfterMs = SystemClock.uptimeMillis() + MANUAL_SCROLL_PAUSE_MS
                        }
                    }
                }
            )
            addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val inset = ((bottom - top) * 0.38f).toInt()
                if (paddingTop != inset) setPadding(dp(24), inset, dp(24), inset)
            }
        }
        content.addView(syncedLyrics, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        header.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            songTitle.apply {
                gravity = Gravity.START
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setTextColor(onSurfaceColor)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            songArtist.apply {
                gravity = Gravity.START
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                setTextColor(secondaryColor)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            addView(songTitle, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(songArtist, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        val trackHeader = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(8), dp(8), dp(8))
        }
        trackHeader.addView(songCover, LinearLayout.LayoutParams(dp(48), dp(48)))
        trackHeader.addView(header, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        column.addView(trackHeader, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        column.addView(content, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        close.apply {
            setImageResource(R.drawable.ic_close_24)
            imageTintList = android.content.res.ColorStateList.valueOf(onSurfaceColor)
            background =
                context
                    .obtainStyledAttributes(
                        intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                    )
                    .let {
                        try {
                            it.getDrawable(0)
                        } finally {
                            it.recycle()
                        }
                    }
            contentDescription = context.getString(R.string.desc_close_lyrics)
            setOnClickListener { onClose?.invoke() }
        }
        toolbar.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        footer.orientation = LinearLayout.VERTICAL
        seekBar.setWaveEnabled(false)
        seekBar.listener = object : StyledSeekBar.Listener {
            override fun onSeekConfirmed(positionDs: Long) {
                autoScrollAfterMs = 0L
                onSeek?.invoke(positionDs)
            }
        }
        footer.addView(seekBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val controls = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, 0, 0, dp(12))
        }
        controls.addView(action(R.drawable.ic_down_24, R.string.desc_close_lyrics) { onClose?.invoke() })
        controls.addView(action(R.drawable.ic_skip_prev_24, R.string.desc_skip_prev) { onPrevious?.invoke() })
        configureAction(playPause, R.drawable.ic_play_24, R.string.desc_play_pause) { onTogglePlaying?.invoke() }
        playPause.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(accentColor)
        }
        playPause.imageTintList = android.content.res.ColorStateList.valueOf(
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, surfaceColor)
        )
        controls.addView(playPause)
        controls.addView(action(R.drawable.ic_skip_next_24, R.string.desc_skip_next) { onNext?.invoke() })
        controls.addView(action(R.drawable.ic_playlist_24, R.string.lbl_queue) { onQueue?.invoke() })
        footer.addView(controls, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        column.addView(footer, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // The playback host already applies the bottom navigation-bar inset.
            column.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }
    }

    private fun action(icon: Int, description: Int, callback: () -> Unit) =
        AppCompatImageButton(context).also { configureAction(it, icon, description, callback) }

    private fun configureAction(button: AppCompatImageButton, icon: Int, description: Int, callback: () -> Unit) {
        button.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        button.setImageResource(icon)
        button.imageTintList = android.content.res.ColorStateList.valueOf(onSurfaceColor)
        button.setBackgroundColor(Color.TRANSPARENT)
        button.contentDescription = context.getString(description)
        button.setOnClickListener { callback() }
    }

    fun setTrack(song: Song) {
        songTitle.text = song.name.resolve(context)
        songArtist.text = song.artists.resolveNames(context)
        songCover.bind(song)
        seekBar.durationDs = song.durationMs.msToDs()
        autoScrollAfterMs = 0L
    }

    fun setPlaying(playing: Boolean) {
        playPause.setImageResource(if (playing) R.drawable.ic_pause_24 else R.drawable.ic_play_24)
    }

    fun attachTo(host: ViewGroup) {
        host.addView(this, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        ViewCompat.requestApplyInsets(this)
    }

    fun setOpen(open: Boolean) {
        animate().cancel()
        if (open && visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(FADE_DURATION_MS).withEndAction(null).start()
            requestFocus()
        } else if (!open && visibility == View.VISIBLE) {
            animate()
                .alpha(0f)
                .setDuration(FADE_DURATION_MS)
                .withEndAction {
                    visibility = View.GONE
                }
                .start()
        } else if (open) {
            animate().alpha(1f).setDuration(FADE_DURATION_MS).withEndAction(null).start()
        }
    }

    fun release() {
        animate().cancel()
        seekBar.listener = null
        syncedLyrics.adapter = null
        visibility = View.GONE
    }

    fun render(state: LyricsLoadState) {
        progress.visibility = if (state is LyricsLoadState.Loading) View.VISIBLE else View.GONE
        val readError = state is LyricsLoadState.ReadError
        status.visibility = if (state is LyricsLoadState.Unavailable || readError) View.VISIBLE else View.GONE
        statusScroll.visibility = status.visibility
        searchOnline.visibility =
            status.visibility
        retry.visibility = if (readError) View.VISIBLE else View.GONE
        plainScroll.visibility = View.GONE
        syncedLyrics.visibility = View.GONE
        if (state is LyricsLoadState.Unavailable) status.setText(R.string.lbl_lyrics_unavailable)
        if (readError) status.setText(R.string.lbl_lyrics_read_error)
        activeLine = -1
        autoScrollAfterMs = 0L
        syncedAdapter.submit(emptyList())
        plainLyrics.text = ""
        if (state is LyricsLoadState.Ready) {
            activeLine = -1
            if (state.lyrics.synchronized) {
                syncedAdapter.submit(state.lyrics.lines)
                syncedLyrics.visibility = View.VISIBLE
            } else {
                plainLyrics.text = state.lyrics.plainText
                plainScroll.scrollTo(0, 0)
                plainScroll.visibility = View.VISIBLE
            }
        }
    }

    fun updatePosition(document: LyricsDocument?, positionMs: Long) {
        seekBar.positionDs = positionMs.msToDs()
        if (document?.synchronized != true || syncedLyrics.visibility != View.VISIBLE) return
        val next = document.activeLineAt(positionMs)
        if (next == activeLine) return
        val previous = activeLine
        activeLine = next
        syncedAdapter.activeLine = next
        if (previous >= 0) syncedAdapter.notifyItemChanged(previous)
        if (next >= 0) {
            syncedAdapter.notifyItemChanged(next)
            if (SystemClock.uptimeMillis() >= autoScrollAfterMs) {
                syncedLyrics.layoutManager?.startSmoothScroll(object : LinearSmoothScroller(context) {
                    override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int) =
                        (boxStart + boxEnd - viewStart - viewEnd) / 2
                }.apply { targetPosition = next })
            }
        }
    }

    private fun centeredParams(width: Int, height: Int) =
        LayoutParams(width, height, Gravity.CENTER)

    private fun dp(value: Int) =
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                resources.displayMetrics,
            )
            .toInt()

    private inner class SyncedLyricsAdapter : RecyclerView.Adapter<SyncedLineHolder>() {
        private var lines: List<LyricsLine> = emptyList()
        var activeLine = -1

        fun submit(lines: List<LyricsLine>) {
            this.lines = lines
            activeLine = -1
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncedLineHolder {
            val view =
                TextView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    gravity = Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextAppearance(
                        com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
                    )
                    setLineSpacing(0f, 1.12f)
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }
            return SyncedLineHolder(view)
        }

        override fun onBindViewHolder(holder: SyncedLineHolder, position: Int) {
            holder.bind(lines[position], position == activeLine)
        }

        override fun getItemCount() = lines.size
    }

    private inner class SyncedLineHolder(private val textView: TextView) :
        RecyclerView.ViewHolder(textView) {
        fun bind(line: LyricsLine, active: Boolean) {
            textView.text = line.text.ifBlank { "•" }
            textView.setTextColor(if (active) accentColor else secondaryColor)
            // colorOnSurfaceVariant already provides the inactive hierarchy at readable contrast.
            textView.alpha = 1f
            textView.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private companion object {
        private const val FADE_DURATION_MS = 180L
        private const val MANUAL_SCROLL_PAUSE_MS = 4000L
    }
}
