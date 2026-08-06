/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.musicdownloader.musicfreeapp825v2.ui.fragments

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.FragmentHomeBinding
import com.musicdownloader.musicfreeapp825v2.logic.MusicDownloaderApplication
import com.musicdownloader.musicfreeapp825v2.logic.enableEdgeToEdgePaddingListener
import com.musicdownloader.musicfreeapp825v2.logic.getTimer
import com.musicdownloader.musicfreeapp825v2.logic.setTimer
import com.musicdownloader.musicfreeapp825v2.logic.utils.UpdateUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.InterstitialAdsUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.DownloadStorage
import com.musicdownloader.musicfreeapp825v2.ui.fragments.settings.MainSettingsActivity

/**
 * HomeFragment:
 *   The landing page of the bottom navigation, modelled after the MSDownloader home: a search bar
 * on top and a 2x2 grid of quick actions (Downloaded, Sleep timer, Rate app, Settings).
 */
class HomeFragment : BaseFragment(null) {

    companion object {
        // Show the config-driven update dialog only once per app session.
        private var updateDialogShown = false
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val appConfig by lazy {
        (mainActivity.application as MusicDownloaderApplication).configEntity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.root.enableEdgeToEdgePaddingListener()

        // Preload the "go to search" interstitial so it is ready when the search bar is tapped.
        InterstitialAdsUtils.loadAdsGoToSearchScreen(mainActivity, appConfig)

        // The search bar opens the online search screen (gated by the interstitial).
        binding.homeSearch.setOnClickListener {
            openOnlineSearch()
        }
        binding.cardDownloaded.setOnClickListener {
            openDownloaded()
        }
        binding.cardSleepTimer.setOnClickListener {
            openSleepTimer()
        }
        binding.cardRateApp.setOnClickListener {
            rateApp()
        }
        binding.cardSettings.setOnClickListener {
            openSettings()
        }

        // Show how many songs are in the download folder (not the whole music library).
        updateDownloadedCount()

        // Prompt to update the app (config-driven), once per session on landing on home.
        if (!updateDialogShown) {
            updateDialogShown = true
            UpdateUtils.showDialogUpdate(mainActivity)
        }

        // Home is the default landing page, so let it release the splash screen.
        binding.root.post { mainActivity.maybeReportFullyDrawn() }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateSleepTimerSubtitle()
        updateDownloadedCount()
    }

    override fun onStart() {
        super.onStart()
        // Refresh the downloaded count live when a download completes while home is visible.
        context?.let {
            ContextCompat.registerReceiver(
                it, downloadUpdateReceiver, IntentFilter(DownloadsFragment.ACTION_UPDATE),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { context?.unregisterReceiver(downloadUpdateReceiver) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private val downloadUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateDownloadedCount()
        }
    }

    /** Counts audio files in the download folder off the main thread, then updates the subtitle. */
    private fun updateDownloadedCount() {
        viewLifecycleOwner.lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                DownloadStorage.getPathDownload().listFiles()?.count {
                    it.isFile &&
                        it.name.substringAfterLast('.', "").lowercase() in DownloadsFragment.AUDIO_EXTENSIONS
                } ?: 0
            }
            _binding?.cardDownloadedSubtitle?.text = getString(R.string.home_songs_count, count)
        }
    }

    private fun openOnlineSearch() {
        // Show the "go to search" interstitial (if enabled by remote config), then navigate.
        if (appConfig.isAds_gotoSearchScreen) {
            InterstitialAdsUtils.showAdsGoToSearchScreen(
                mainActivity, appConfig,
                object : InterstitialAdsUtils.Listener {
                    override fun onNotShowAds() {
                        mainActivity.startFragment(OnlineSearchFragment())
                    }

                    override fun onAdDismissedFullScreenContent() {
                        mainActivity.startFragment(OnlineSearchFragment())
                    }
                }
            )
        } else {
            mainActivity.startFragment(OnlineSearchFragment())
        }
    }

    /** Home -> Downloaded, gated by the shared go-to-search interstitial (as in the sample). */
    private fun openDownloaded() {
        if (appConfig.isAds_gotoSearchScreen) {
            InterstitialAdsUtils.showAdsGoToSearchScreen(
                mainActivity, appConfig,
                object : InterstitialAdsUtils.Listener {
                    override fun onNotShowAds() {
                        mainActivity.startFragment(DownloadsFragment())
                    }

                    override fun onAdDismissedFullScreenContent() {
                        mainActivity.startFragment(DownloadsFragment())
                    }
                }
            )
        } else {
            mainActivity.startFragment(DownloadsFragment())
        }
    }

    /** Home -> Settings, gated by the go-to-setting interstitial (as in the sample). */
    private fun openSettings() {
        if (appConfig.isAds_gotoSettingScreen) {
            InterstitialAdsUtils.showAdsGoToSearchScreen(
                mainActivity, appConfig,
                object : InterstitialAdsUtils.Listener {
                    override fun onNotShowAds() {
                        mainActivity.startActivity(Intent(mainActivity, MainSettingsActivity::class.java))
                    }

                    override fun onAdDismissedFullScreenContent() {
                        mainActivity.startActivity(Intent(mainActivity, MainSettingsActivity::class.java))
                    }
                }
            )
        } else {
            mainActivity.startActivity(Intent(mainActivity, MainSettingsActivity::class.java))
        }
    }

    private fun updateSleepTimerSubtitle() {
        val timer = mainActivity.getPlayer()?.getTimer()
        val active = timer != null && ((timer.first ?: 0) > 0 || timer.second)
        _binding?.cardSleepTimerSubtitle?.setText(if (active) R.string.home_on else R.string.home_off)
    }

    private fun openSleepTimer() {
        val controller = mainActivity.getPlayer() ?: return
        val options = listOf(
            getString(R.string.home_off) to { controller.setTimer(0, false) },
            getString(R.string.home_minutes, 15) to { controller.setTimer(15 * 60 * 1000, false) },
            getString(R.string.home_minutes, 30) to { controller.setTimer(30 * 60 * 1000, false) },
            getString(R.string.home_minutes, 45) to { controller.setTimer(45 * 60 * 1000, false) },
            getString(R.string.home_minutes, 60) to { controller.setTimer(60 * 60 * 1000, false) },
            getString(R.string.home_end_of_song) to { controller.setTimer(0, true) },
        )
        MaterialAlertDialogBuilder(mainActivity)
            .setTitle(R.string.home_sleep_timer)
            .setItems(options.map { it.first }.toTypedArray()) { _, which ->
                options[which].second()
                updateSleepTimerSubtitle()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Rate app via the Google Play In-App Review flow, ported from the sample's Utils.rateApp:
    // request + launch the review flow, then open the Play Store listing; if the flow can't be
    // requested, fall back straight to the store.
    private fun rateApp() {
        val activity = mainActivity
        FirebaseEventUtils.getInstances().logEventUserClickRateApp(activity)
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result).addOnCompleteListener {
                    openPlayStore()
                }
            } else {
                openPlayStore()
            }
        }
    }

    private fun openPlayStore() {
        if (!isAdded) return
        val pkg = mainActivity.packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$pkg".toUri()
                )
            )
        }
    }
}
