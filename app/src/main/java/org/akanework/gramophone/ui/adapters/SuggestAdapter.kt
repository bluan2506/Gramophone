package org.akanework.gramophone.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.akanework.gramophone.databinding.ItemOnlineSuggestBinding

/**
 * Autocomplete suggestion list for online search, ported from the MSDownloader `SuggestAdapter`.
 *
 * Tapping a row runs the search ([onClickSuggest]); tapping the up-left arrow fills the search box
 * with the suggestion without searching ([onClickFill]).
 */
class SuggestAdapter(
    private val onClickSuggest: (String) -> Unit,
    private val onClickFill: (String) -> Unit,
) : RecyclerView.Adapter<SuggestAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    fun setData(list: List<String>) {
        items.clear()
        items.addAll(list)
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
        val text = items[position]
        holder.binding.textSuggest.text = text
        holder.binding.root.setOnClickListener { onClickSuggest(text) }
        holder.binding.imgUp.setOnClickListener { onClickFill(text) }
    }

    class ViewHolder(val binding: ItemOnlineSuggestBinding) :
        RecyclerView.ViewHolder(binding.root)
}
