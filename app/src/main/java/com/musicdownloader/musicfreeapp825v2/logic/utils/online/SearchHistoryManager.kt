package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.Context
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import org.json.JSONArray

/**
 * Persists the user's recent online-search queries (most-recent-first) in SharedPreferences.
 *
 * Not present in the MSDownloader sample — added for Gramophone: the online search screen shows
 * these as tappable suggestions while the query box is empty. Entries are de-duplicated
 * case-insensitively (a repeated search just moves back to the top) and capped at [MAX].
 */
object SearchHistoryManager {

    private const val PREFS = "online_search_history"
    private const val KEY = "queries"
    private const val MAX = 12

    fun get(context: Context): List<String> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            FirebaseEventUtils.getInstances().recordException(e)
            emptyList()
        }
    }

    /** Add [query] to the top; an existing (case-insensitive) match moves up; capped at [MAX]. */
    fun add(context: Context, query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return get(context)
        val list = get(context).toMutableList()
        list.removeAll { it.equals(q, ignoreCase = true) }
        list.add(0, q)
        while (list.size > MAX) list.removeAt(list.size - 1)
        save(context, list)
        return list
    }

    fun remove(context: Context, query: String): List<String> {
        val list = get(context).toMutableList()
        list.removeAll { it.equals(query, ignoreCase = true) }
        save(context, list)
        return list
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY).apply()
    }

    private fun save(context: Context, list: List<String>) {
        prefs(context).edit().putString(KEY, JSONArray(list).toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
