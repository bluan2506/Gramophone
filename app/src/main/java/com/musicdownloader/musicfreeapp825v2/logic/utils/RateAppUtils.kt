package com.musicdownloader.musicfreeapp825v2.logic.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.InterstitialAdsUtils

/**
 * Gating logic for the rate-app bottom sheet flow, ported from MSDownloader07's `RateAppUtils`.
 *
 * A rate prompt is only offered when the user has never interacted with it, after at least
 * [START_SHOW_RATE_APP] eligible triggers, and never while an interstitial is on screen. The very
 * first eligible time shows [Listener.showRateAppFirstTime] (the "Are you satisfied?" filter),
 * later times show [Listener.showRateApp] directly, spaced by [SHOW_RATE_APP_INTERVAL].
 */
object RateAppUtils {

    interface Listener {
        fun showRateAppFirstTime()
        fun showRateApp()
    }

    private const val TAG = "RateAppUtils"

    private const val KEY_SHOW_COUNT = "SHOW_RATE_APP_COUNT"
    private const val KEY_LAST_TIME = "LAST_SHOW_RATE_APP_TIME"
    private const val KEY_INTERACTED = "HAS_INTERACT_RATE_APP"

    private const val START_SHOW_RATE_APP = 4

    private const val SHOW_RATE_APP_INTERVAL = 48L * 60 * 60 * 1000

    private fun prefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun updateShowRateAppCount(context: Context, listener: Listener) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_INTERACTED, false)) {
            return
        }

        val oldCount = prefs.getInt(KEY_SHOW_COUNT, 0)
        val newCount = oldCount + 1
        Log.i(TAG, "showRateAppCount: $oldCount -> $newCount")
        prefs.edit { putInt(KEY_SHOW_COUNT, newCount) }

        canShowRateApp(prefs, listener)
    }

    private fun canShowRateApp(prefs: SharedPreferences, listener: Listener) {
        if (InterstitialAdsUtils.isShowAdsGoToSearchScreen) {
            return
        }

        val count = prefs.getInt(KEY_SHOW_COUNT, 0)
        val current = System.currentTimeMillis()
        val lastShowTime = prefs.getLong(KEY_LAST_TIME, 0)
        if (count >= START_SHOW_RATE_APP) {
            if (lastShowTime == 0L) {
                prefs.edit { putLong(KEY_LAST_TIME, current) }
                listener.showRateAppFirstTime()
            } else if (current - lastShowTime > SHOW_RATE_APP_INTERVAL) {
                prefs.edit { putLong(KEY_LAST_TIME, current) }
                listener.showRateApp()
            }
        }
    }

    /** Marks that the user acted on the prompt (rated or gave feedback) so it never shows again. */
    fun interactRateApp(context: Context) {
        prefs(context).edit { putBoolean(KEY_INTERACTED, true) }
    }
}