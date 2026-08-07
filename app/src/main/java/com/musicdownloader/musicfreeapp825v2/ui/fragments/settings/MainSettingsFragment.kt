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

package com.musicdownloader.musicfreeapp825v2.ui.fragments.settings

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.Preference
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.ui.fragments.BasePreferenceFragment
import com.musicdownloader.musicfreeapp825v2.ui.fragments.BaseSettingsActivity

class MainSettingsActivity : BaseSettingsActivity(
    R.string.home_menu_settings,
    { MainSettingsFragment() })

class MainSettingsFragment : BasePreferenceFragment() {

    companion object {
        private const val PRIVACY_POLICY_URL = "https://www.google.com/"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_top, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "appearance" -> {
                startActivity(AppearanceSettingsActivity::class.java)
            }

            "behavior" -> {
                startActivity(BehaviorSettingsActivity::class.java)
            }

            "about" -> {
                startActivity(AboutSettingsActivity::class.java)
            }

            "player" -> {
                startActivity(PlayerSettingsActivity::class.java)
            }

            "audio" -> {
                startActivity(AudioSettingsActivity::class.java)
            }

            "experimental" -> {
                startActivity(ExperimentalSettingsActivity::class.java)
            }

            "privacy" -> {
                val intent = Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    FirebaseEventUtils.getInstances().recordException(e)
                }
            }

            "feedback" -> {
                startActivity(FeedbackActivity::class.java)
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

}
