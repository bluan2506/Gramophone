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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.FragmentDownloadsBinding
import com.musicdownloader.musicfreeapp825v2.logic.enableEdgeToEdgePaddingListener
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.DownloadStorage
import com.musicdownloader.musicfreeapp825v2.ui.adapters.DownloadAdapter
import com.musicdownloader.musicfreeapp825v2.ui.adapters.DownloadAdapter.DownloadItem
import us.shandian.giga.util.Utility
import java.io.File

/**
 * DownloadsFragment:
 *   The "Downloaded" screen, ported from the MSDownloader `DownloadFragment`. Lists the completed
 * downloads in the download folder; tapping a file plays it through the media3 controller. Refreshes
 * when a download finishes (via the [ACTION_UPDATE] broadcast from the download service).
 */
class DownloadsFragment : BaseFragment(true) {

    companion object {
        const val ACTION_UPDATE = "com.musicdownloader.musicfreeapp825v2.action.DOWNLOAD_UPDATE"

        val AUDIO_EXTENSIONS =
            setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
    }

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DownloadAdapter

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        binding.root.findViewById<AppBarLayout>(R.id.appbarlayout).enableEdgeToEdgePaddingListener()

        adapter = DownloadAdapter(onClick = { playFile(it) })
        binding.recyclerview.apply {
            enableEdgeToEdgePaddingListener()
            layoutManager = LinearLayoutManager(activity)
            adapter = this@DownloadsFragment.adapter
        }
        binding.returnButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        return binding.root
    }

    // Enumerating the download folder + reading file sizes is disk I/O -> run it off the main thread
    // (StrictMode flags main-thread disk reads), then post the completed list back to the adapter.
    private fun refresh() {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val dir = DownloadStorage.getPathDownload()
            val items = dir.listFiles()?.filter {
                it.isFile && it.name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS
            }?.sortedByDescending { it.lastModified() }
                ?.map { DownloadItem(it.name, it, Utility.formatBytes(it.length())) }
                ?: emptyList()

            withContext(Dispatchers.Main) {
                if (!isAdded || _binding == null) return@withContext
                adapter.setData(items)
                binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun playFile(file: File) {
        val controller = mainActivity.getPlayer() ?: return
        val item = MediaItem.Builder()
            .setUri(file.toUri())
            .setMediaId("download:${file.absolutePath}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(file.nameWithoutExtension)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            ContextCompat.registerReceiver(
                it, updateReceiver, IntentFilter(ACTION_UPDATE),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { context?.unregisterReceiver(updateReceiver) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
