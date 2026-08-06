package com.musicdownloader.musicfreeapp825v2.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.musicdownloader.musicfreeapp825v2.databinding.ItemDownloadBinding
import java.io.File

/**
 * Lists completed downloads. Tapping a row plays that file. Sizes are precomputed off the main
 * thread ([DownloadItem.sizeText]) to avoid disk I/O in onBind (StrictMode).
 */
class DownloadAdapter(
    private val onClick: (File) -> Unit,
) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

    data class DownloadItem(val name: String, val file: File, val sizeText: String)

    private val items = mutableListOf<DownloadItem>()

    fun setData(list: List<DownloadItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
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
    }

    class ViewHolder(val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root)
}
