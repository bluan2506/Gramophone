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

package org.akanework.gramophone.ui.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.AdapterFragment
import org.akanework.gramophone.ui.fragments.HomeFragment

/**
 * Fixed ViewPager2 adapter backing the bottom navigation. Unlike [ViewPager2Adapter] the set of
 * pages is not user-configurable: it always hosts Home, Songs, Albums, Artists and Playlists in
 * this order, matching [R.menu.bottom_nav_menu].
 */
class MainPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() = PAGES.size

    override fun createFragment(position: Int): Fragment {
        val id = PAGES[position]
        return if (id == R.id.home) HomeFragment()
        else AdapterFragment().apply {
            arguments = Bundle().apply { putInt("ID", id) }
        }
    }

    override fun getItemId(position: Int) = PAGES[position].toLong()

    override fun containsItem(itemId: Long) = PAGES.any { it.toLong() == itemId }

    companion object {
        /** Page order; each entry is both the ViewPager2 item id and the bottom nav menu item id. */
        val PAGES = listOf(R.id.home, R.id.songs, R.id.albums, R.id.artists, R.id.playlists)
    }
}
