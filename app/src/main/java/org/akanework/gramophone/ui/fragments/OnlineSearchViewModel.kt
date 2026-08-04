package org.akanework.gramophone.ui.fragments

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.searchapi.`object`.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.akanework.gramophone.logic.utils.online.OnlineSearchRepository

/**
 * Drives online search state, ported from the MSDownloader `LibraryViewModel` search methods:
 * suggestions-as-you-type, a full search, and paginated load-more. The pagination token from the
 * lib is kept opaque in [nextPage] and fed straight back into [getMoreSearchResult].
 */
class OnlineSearchViewModel : ViewModel() {

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _results = MutableStateFlow<List<VideoEntity>>(emptyList())
    val results = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()

    private val _searched = MutableStateFlow(false)
    val searched = _searched.asStateFlow()

    private val current = mutableListOf<VideoEntity>()
    private var nextPage: Any? = null
    private var hasMore = true
    private var isLoadMore = false
    private var suggestJob: Job? = null

    fun searchSuggest(text: String, countryCode: String) {
        suggestJob?.cancel()
        val query = text.trim()
        if (query.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }
        suggestJob = viewModelScope.launch(Dispatchers.IO) {
            val list = OnlineSearchRepository.getSuggestion(query, countryCode)
            _suggestions.value = list
        }
    }

    fun clearSuggestions() {
        suggestJob?.cancel()
        _suggestions.value = emptyList()
    }

    fun getSearchResult(query: String, activity: Activity) {
        nextPage = null
        hasMore = true
        isLoadMore = false
        current.clear()
        _results.value = emptyList()
        _suggestions.value = emptyList()
        _searched.value = true
        _loading.value = true

        // Run the lib call off the main thread (it reads cached config from disk); mirrors the
        // reference LibraryViewModel which searches on Dispatchers.IO.
        viewModelScope.launch(Dispatchers.IO) {
            OnlineSearchRepository.getResult(query, activity) { list, next ->
                viewModelScope.launch {
                    nextPage = next
                    _loading.value = false
                    if (list != null) {
                        current.addAll(list)
                        _results.value = current.toList()
                    } else {
                        _results.value = emptyList()
                    }
                }
            }
        }
    }

    fun getMoreSearchResult(query: String, activity: Activity) {
        val token = nextPage
        if (isLoadMore || !hasMore || token == null || query.isEmpty()) return
        isLoadMore = true
        _loadingMore.value = true

        viewModelScope.launch(Dispatchers.IO) {
          OnlineSearchRepository.getMoreResult(query, activity, token) { list, next ->
            viewModelScope.launch {
                nextPage = next
                val existing = current.mapNotNull { it.videoId }.toHashSet()
                val fresh = list.filter { it.videoId == null || existing.add(it.videoId) }
                if (fresh.isEmpty() || next == null) hasMore = false
                if (fresh.isNotEmpty()) {
                    current.addAll(fresh)
                    _results.value = current.toList()
                }
                isLoadMore = false
                _loadingMore.value = false
            }
          }
        }
    }
}
