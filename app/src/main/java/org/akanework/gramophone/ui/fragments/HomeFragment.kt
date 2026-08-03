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

package org.akanework.gramophone.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.getTimer
import org.akanework.gramophone.logic.setTimer
import org.akanework.gramophone.ui.fragments.settings.MainSettingsActivity

/**
 * HomeFragment:
 *   The landing page of the bottom navigation, modelled after the MSDownloader home: a search bar
 * on top and a 2x2 grid of quick actions (Downloaded, Sleep timer, Rate app, Settings).
 */
class HomeFragment : BaseFragment(null) {

    private var sleepTimerSubtitle: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        rootView.enableEdgeToEdgePaddingListener()

        rootView.findViewById<View>(R.id.home_search).setOnClickListener {
            mainActivity.startFragment(SearchFragment())
        }
        rootView.findViewById<View>(R.id.card_downloaded).setOnClickListener {
            // No "downloaded" concept in a local player: jump to the full song library instead.
            mainActivity.bottomNavigationView.selectedItemId = R.id.songs
        }
        rootView.findViewById<View>(R.id.card_sleep_timer).setOnClickListener {
            openSleepTimer()
        }
        rootView.findViewById<View>(R.id.card_rate_app).setOnClickListener {
            rateApp()
        }
        rootView.findViewById<View>(R.id.card_settings).setOnClickListener {
            mainActivity.startActivity(Intent(mainActivity, MainSettingsActivity::class.java))
        }

        sleepTimerSubtitle = rootView.findViewById(R.id.card_sleep_timer_subtitle)
        val downloadedSubtitle = rootView.findViewById<TextView>(R.id.card_downloaded_subtitle)
        val reader = mainActivity.reader
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                reader.songListFlow.collect { songs ->
                    downloadedSubtitle.text = getString(R.string.home_songs_count, songs.size)
                }
            }
        }

        // Home is the default landing page, so let it release the splash screen.
        rootView.post { mainActivity.maybeReportFullyDrawn() }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        updateSleepTimerSubtitle()
    }

    override fun onDestroyView() {
        sleepTimerSubtitle = null
        super.onDestroyView()
    }

    private fun updateSleepTimerSubtitle() {
        val timer = mainActivity.getPlayer()?.getTimer()
        val active = timer != null && ((timer.first ?: 0) > 0 || timer.second)
        sleepTimerSubtitle?.setText(if (active) R.string.home_on else R.string.home_off)
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

    private fun rateApp() {
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
