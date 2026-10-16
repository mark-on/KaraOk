package org.oxycblt.auxio.lyrics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.oxycblt.auxio.R

/** Small, on-demand preference reader shared by the player and lyrics repository. */
class LyricsPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val enabled: Boolean
        get() = preferences.getBoolean(context.getString(R.string.set_key_lyrics_enabled), true)

    val autoSearch: Boolean
        get() = preferences.getBoolean(context.getString(R.string.set_key_lyrics_auto), false)

    val preloadNext: Boolean
        get() = preferences.getBoolean(context.getString(R.string.set_key_lyrics_preload_next), true)

    val showKara: Boolean
        get() = preferences.getBoolean(context.getString(R.string.set_key_lyrics_show_kara), true)

    fun automaticDownloadAllowed(): Boolean {
        if (!enabled || !autoSearch) return false
        if (!preferences.getBoolean(context.getString(R.string.set_key_lyrics_wifi_only), true)) {
            return true
        }
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(network)?.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) == true
    }
}
