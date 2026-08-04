package org.akanework.gramophone.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import com.music.searchapi.`object`.VideoEntity
import org.akanework.gramophone.R
import org.akanework.gramophone.databinding.ItemOnlineSongBinding
import org.akanework.gramophone.ui.components.NowPlayingDrawable

/**
 * List adapter for online search results ([VideoEntity]), ported from the MSDownloader
 * `MusicOnlineAdapter` (without the interleaved native ads).
 *
 * Tapping a row calls [onClickItem] (stream); tapping the download icon calls [onClickDownload].
 * The currently playing row shows the same animated equalizer ([NowPlayingDrawable]) as the offline
 * song lists, driven by the media3 controller.
 */
class OnlineSongAdapter(
    private val onClickItem: (VideoEntity) -> Unit,
    private val onClickDownload: (VideoEntity) -> Unit,
) : RecyclerView.Adapter<OnlineSongAdapter.ViewHolder>() {

    private val items = mutableListOf<VideoEntity>()
    private var currentMediaId: String? = null
    private var currentIsPlaying: Boolean? = null

    fun setData(list: List<VideoEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /** Appends deduplicated (by videoId) items for pagination; returns how many were added. */
    fun addData(list: List<VideoEntity>): Int {
        val existing = items.mapNotNull { it.videoId }.toHashSet()
        val fresh = list.filter { it.videoId == null || existing.add(it.videoId) }
        if (fresh.isEmpty()) return 0
        val start = items.size
        items.addAll(fresh)
        notifyItemRangeInserted(start, fresh.size)
        return fresh.size
    }

    /** [mediaId] is the media3 mediaId of the current item ("online:<videoId>"), or null. */
    fun updateCurrentPlaying(mediaId: String?, isPlaying: Boolean?) {
        val old = currentMediaId
        currentMediaId = mediaId
        currentIsPlaying = isPlaying
        items.forEachIndexed { index, item ->
            val key = "online:${item.videoId}"
            if (key == old || key == mediaId) notifyItemChanged(index)
        }
    }

    fun isEmpty() = items.isEmpty()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnlineSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textName.text = item.videoTile
        holder.binding.textArtist.text = buildSubtitle(item)
        holder.binding.imageMusic.load(item.image_link) {
            crossfade(true)
            error(R.drawable.ic_default_cover)
        }

        val isCurrent = item.videoId != null && currentMediaId == "online:${item.videoId}"
        if (isCurrent) {
            holder.binding.nowPlaying.setImageDrawable(
                NowPlayingDrawable(holder.binding.root.context).also {
                    it.setTint(Color.WHITE)
                    it.level = if (currentIsPlaying == true) 1 else 0
                }
            )
            holder.binding.nowPlaying.visibility = View.VISIBLE
        } else {
            holder.binding.nowPlaying.setImageDrawable(null)
            holder.binding.nowPlaying.visibility = View.GONE
        }

        holder.binding.imageDownload.visibility =
            if (item.allow_download) View.VISIBLE else View.GONE
        holder.binding.llItem.setOnClickListener { onClickItem(item) }
        holder.binding.imageDownload.setOnClickListener { onClickDownload(item) }
    }

    private fun buildSubtitle(item: VideoEntity): String {
        val artist = item.artist_name?.takeIf { it.isNotBlank() }
        val duration = formatDuration(item.duration)
        return when {
            artist != null && duration != null -> "$artist · $duration"
            artist != null -> artist
            duration != null -> duration
            else -> ""
        }
    }

    private fun formatDuration(seconds: Int): String? {
        if (seconds <= 0) return null
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    class ViewHolder(val binding: ItemOnlineSongBinding) :
        RecyclerView.ViewHolder(binding.root)
}
