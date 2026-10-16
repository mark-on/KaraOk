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
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import org.oxycblt.auxio.R

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
    private val plainLyrics = TextView(context)
    private val plainScroll = ScrollView(context)
    private val syncedLyrics = RecyclerView(context)
    private val syncedAdapter = SyncedLyricsAdapter()
    private val close = AppCompatImageButton(context)

    private var host: ViewGroup? = null
    private var activeLine = -1
    private var autoScrollAfterMs = 0L

    var onClose: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        visibility = View.GONE
        background =
            android.graphics.drawable.ColorDrawable(ColorUtils.setAlphaComponent(surfaceColor, 214))

        progress.visibility = View.GONE
        addView(progress, centeredParams(WRAP_CONTENT, WRAP_CONTENT))

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
        addView(status, centeredParams(MATCH_PARENT, WRAP_CONTENT))

        plainLyrics.apply {
            gravity = Gravity.CENTER_HORIZONTAL
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(onSurfaceColor)
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )
            setLineSpacing(0f, 1.2f)
            setPadding(dp(28), dp(72), dp(28), dp(72))
        }
        plainScroll.apply {
            isFillViewport = true
            clipToPadding = false
            addView(plainLyrics, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            visibility = View.GONE
        }
        addView(plainScroll, LayoutParams(MATCH_PARENT, MATCH_PARENT))

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
                            autoScrollAfterMs = SystemClock.uptimeMillis() + MANUAL_SCROLL_PAUSE_MS
                        }
                    }
                }
            )
            addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val inset = ((bottom - top) * 0.38f).toInt()
                setPadding(dp(24), inset, dp(24), inset)
            }
        }
        addView(syncedLyrics, LayoutParams(MATCH_PARENT, MATCH_PARENT))

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
        addView(
            close,
            LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(8)
                marginEnd = dp(8)
            },
        )

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (close.layoutParams as LayoutParams).apply {
                topMargin = bars.top + dp(8)
                marginEnd = bars.right + dp(8)
                close.layoutParams = this
            }
            insets
        }
    }

    fun attachTo(host: ViewGroup) {
        this.host = host
        host.addView(this, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        ViewCompat.requestApplyInsets(this)
    }

    fun setOpen(open: Boolean) {
        if (open && visibility != View.VISIBLE) {
            applyBlur(true)
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
            requestFocus()
        } else if (!open && visibility == View.VISIBLE) {
            animate()
                .alpha(0f)
                .setDuration(FADE_DURATION_MS)
                .withEndAction {
                    visibility = View.GONE
                    applyBlur(false)
                }
                .start()
        }
    }

    fun release() {
        animate().cancel()
        applyBlur(false)
        visibility = View.GONE
        host = null
    }

    fun render(state: LyricsLoadState) {
        progress.visibility = if (state is LyricsLoadState.Loading) View.VISIBLE else View.GONE
        status.visibility = if (state is LyricsLoadState.Unavailable) View.VISIBLE else View.GONE
        plainScroll.visibility = View.GONE
        syncedLyrics.visibility = View.GONE
        if (state is LyricsLoadState.Unavailable) status.setText(R.string.lbl_lyrics_unavailable)
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
                syncedLyrics.smoothScrollToPosition(next)
            }
        }
    }

    private fun applyBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val effect =
            if (enabled)
                RenderEffect.createBlurEffect(
                    dp(12).toFloat(),
                    dp(12).toFloat(),
                    Shader.TileMode.CLAMP,
                )
            else null
        host?.childrenExcept(this)?.forEach { it.setRenderEffect(effect) }
    }

    private fun ViewGroup.childrenExcept(excluded: View): List<View> =
        (0 until childCount).map(::getChildAt).filter { it !== excluded }

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
            textView.alpha = if (active) 1f else 0.72f
            textView.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private companion object {
        private const val FADE_DURATION_MS = 180L
        private const val MANUAL_SCROLL_PAUSE_MS = 4000L
    }
}
