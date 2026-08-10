package com.musicdownloader.musicfreeapp825v2.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.musicdownloader.musicfreeapp825v2.databinding.ItemDownloadBinding
import com.musicdownloader.musicfreeapp825v2.ui.components.NowPlayingDrawable
import java.io.File

/**
 * Lists completed downloads. Tapping a row plays that file. Sizes are precomputed off the main
 * thread ([DownloadItem.sizeText]) to avoid disk I/O in onBind (StrictMode).
 *
 * The currently playing row shows the same animated equalizer ([NowPlayingDrawable]) as the online
 * search and offline song lists, driven by the media3 controller.
 */
class DownloadAdapter(
    private val onClick: (File) -> Unit,
) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

    companion object {
        /** media3 mediaId prefix used for played download files (see DownloadsFragment.playFile). */
        const val MEDIA_ID_PREFIX = "download:"
    }

    data class DownloadItem(val name: String, val file: File, val sizeText: String)

    private val items = mutableListOf<DownloadItem>()
    private var currentMediaId: String? = null
    private var currentIsPlaying: Boolean? = null

    fun setData(list: List<DownloadItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /** [mediaId] is the media3 mediaId of the current item ("download:<absolute path>"), or null. */
    fun updateCurrentPlaying(mediaId: String?, isPlaying: Boolean?) {
        if (currentMediaId == mediaId && currentIsPlaying == isPlaying) return
        val old = currentMediaId
        currentMediaId = mediaId
        currentIsPlaying = isPlaying
        items.forEachIndexed { index, item ->
            val key = MEDIA_ID_PREFIX + item.file.absolutePath
            if (key == old || key == mediaId) notifyItemChanged(index)
        }
    }

    fun isEmpty() = items.isEmpty()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textName.text = item.name
        holder.binding.textSubtitle.text = item.sizeText
        holder.binding.llItem.setOnClickListener { onClick(item.file) }

        val isCurrent = currentMediaId == MEDIA_ID_PREFIX + item.file.absolutePath
        if (isCurrent) {
            // No explicit tint: the ImageView's app:tint (colorOnSurface) colours the drawable,
            // exactly as in the offline song lists.
            holder.binding.nowPlaying.setImageDrawable(
                NowPlayingDrawable(holder.binding.root.context).also {
                    it.level = if (currentIsPlaying == true) 1 else 0
                }
            )
            holder.binding.nowPlaying.visibility = View.VISIBLE
        } else {
            holder.binding.nowPlaying.setImageDrawable(null)
            holder.binding.nowPlaying.visibility = View.GONE
        }
    }

    class ViewHolder(val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root)
}
