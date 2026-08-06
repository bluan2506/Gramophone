package com.musicdownloader.musicfreeapp825v2.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.ItemOnlineSuggestBinding

/**
 * Autocomplete suggestion list for online search, ported from the MSDownloader `SuggestAdapter` and
 * extended (Gramophone-only) to also render recent-search history rows.
 *
 * Two row kinds share one layout:
 *  - Online suggestion ([setData]): search icon + up-left arrow that fills the box ([onClickFill]).
 *  - Search history ([setHistory]): clock icon + an X that removes the entry ([onClickRemove]).
 *
 * Tapping the row runs the search ([onClickSuggest]) for both kinds.
 */
class SuggestAdapter(
    private val onClickSuggest: (String) -> Unit,
    private val onClickFill: (String) -> Unit,
    private val onClickRemove: (String) -> Unit = {},
) : RecyclerView.Adapter<SuggestAdapter.ViewHolder>() {

    // isHistory drives the leading icon (clock vs search). removable drives the trailing button:
    // the ✕ remove-from-history action shows ONLY in the empty-box history list; everywhere else
    // (typing, incl. matched past searches) the row shows the up-left arrow that fills the box.
    private data class Item(val text: String, val isHistory: Boolean, val removable: Boolean)

    private val items = mutableListOf<Item>()

    /** Online autocomplete suggestions (typing). */
    fun setData(list: List<String>) {
        items.clear()
        list.forEach { items.add(Item(it, isHistory = false, removable = false)) }
        notifyDataSetChanged()
    }

    /** Recent-search history (shown while the query box is empty) — removable via the ✕ button. */
    fun setHistory(list: List<String>) {
        items.clear()
        list.forEach { items.add(Item(it, isHistory = true, removable = true)) }
        notifyDataSetChanged()
    }

    /** While typing: matching history rows first (clock + fill arrow), then the online suggestions. */
    fun setMixed(history: List<String>, suggestions: List<String>) {
        items.clear()
        history.forEach { items.add(Item(it, isHistory = true, removable = false)) }
        suggestions.forEach { items.add(Item(it, isHistory = false, removable = false)) }
        notifyDataSetChanged()
    }

    fun isEmpty() = items.isEmpty()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnlineSuggestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textSuggest.text = item.text
        holder.binding.root.setOnClickListener { onClickSuggest(item.text) }
        // Leading icon marks whether the row is a past search (clock) or an online suggestion.
        holder.binding.imgSearch.setImageResource(
            if (item.isHistory) R.drawable.ic_schedule else R.drawable.ic_search
        )
        if (item.removable) {
            // Empty-box history list only: ✕ removes the entry.
            holder.binding.imgUp.setImageResource(R.drawable.ic_close)
            holder.binding.imgUp.rotation = 0f
            holder.binding.imgUp.setOnClickListener { onClickRemove(item.text) }
        } else {
            // Typing (incl. matched past searches) and online suggestions: up-left arrow fills the box.
            holder.binding.imgUp.setImageResource(R.drawable.baseline_arrow_upward_24)
            holder.binding.imgUp.rotation = 45f
            holder.binding.imgUp.setOnClickListener { onClickFill(item.text) }
        }
    }

    class ViewHolder(val binding: ItemOnlineSuggestBinding) :
        RecyclerView.ViewHolder(binding.root)
}
