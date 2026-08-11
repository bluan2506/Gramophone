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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.FragmentHomeBinding
import com.musicdownloader.musicfreeapp825v2.logic.MusicDownloaderApplication
import com.musicdownloader.musicfreeapp825v2.logic.enableEdgeToEdgePaddingListener
import com.musicdownloader.musicfreeapp825v2.logic.utils.UpdateUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.InterstitialAdsUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.DownloadStorage
import com.musicdownloader.musicfreeapp825v2.ui.fragments.settings.MainSettingsActivity

/**
 * HomeFragment:
 *   The landing page of the bottom navigation, modelled after the MSDownloader home: the app mark
 * and name, a search bar that hands off to the online search screen, a cloud of "hot song"
 * keywords that open that screen pre-searched, and two tiles for Downloaded and Settings.
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

        // The search bar opens the online search screen (gated by the interstitial); the button
        // inside it is only a visual affordance, so it does the same thing.
        binding.homeSearch.setOnClickListener {
            openOnlineSearch()
        }
        binding.homeSearchButton.setOnClickListener {
            openOnlineSearch()
        }
        binding.cardDownloaded.setOnClickListener {
            openDownloaded()
        }
        binding.cardSettings.setOnClickListener {
            openSettings()
        }

        populateHotSongs()

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

    /** Fills the "Hot songs" cloud; tapping a keyword opens the search screen already searching it. */
    private fun populateHotSongs() {
        val group = binding.hotSongs
        val inflater = LayoutInflater.from(group.context)
        resources.getStringArray(R.array.home_hot_songs).forEach { title ->
            val chip = inflater.inflate(R.layout.item_hot_song_chip, group, false) as Chip
            chip.text = title
            chip.setOnClickListener { openOnlineSearch(title) }
            group.addView(chip)
        }
    }

    /** @param query when set, the search screen opens with this keyword already searched. */
    private fun openOnlineSearch(query: String? = null) {
        val args: (Bundle.() -> Unit)? = if (query.isNullOrBlank()) null else {
            { putString(OnlineSearchFragment.KEY_QUERY, query) }
        }
        val open = { mainActivity.startFragment(OnlineSearchFragment(), args) }
        // Show the "go to search" interstitial (if enabled by remote config), then navigate.
        if (appConfig.isAds_gotoSearchScreen) {
            InterstitialAdsUtils.showAdsGoToSearchScreen(
                mainActivity, appConfig,
                object : InterstitialAdsUtils.Listener {
                    override fun onNotShowAds() {
                        open()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        open()
                    }
                }
            )
        } else {
            open()
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

}
