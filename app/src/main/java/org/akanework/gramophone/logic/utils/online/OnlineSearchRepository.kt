package org.akanework.gramophone.logic.utils.online

import android.app.Activity
import com.music.searchapi.ApiServices
import com.music.searchapi.`object`.VideoEntity
import com.videoapps.lib.SearchCallback
import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Wraps the searchapi lib (`com.music.searchapi.ApiServices`) plus the Google autocomplete endpoint.
 * Ported from the MSDownloader `SearchOnlineRepositoryImpl` (suggestions there go through Retrofit;
 * here a plain HttpURLConnection to the same endpoint avoids adding a dependency).
 */
object OnlineSearchRepository {

    /** Firefox-style autocomplete: returns `["query", ["s1","s2",...]]`. We return the suggestions. */
    fun getSuggestion(key: String, countryCode: String): List<String> {
        var conn: HttpURLConnection? = null
        return try {
            val q = URLEncoder.encode(key, "UTF-8")
            val gl = URLEncoder.encode(countryCode, "UTF-8")
            val url = URL(
                "https://suggestqueries.google.com/complete/search?client=firefox&gl=$gl&q=$q"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONArray(body)
            val suggestions = root.getJSONArray(1)
            (0 until suggestions.length()).map { suggestions.getString(it) }
        } catch (e: Exception) {
            FirebaseEventUtils.getInstances().recordException(e)
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    fun getResult(
        key: String,
        activity: Activity,
        onResult: (videoEntities: List<VideoEntity>?, nextPage: Any?) -> Unit,
    ) {
        ApiServices.search(activity, key, object : SearchCallback {
            override fun onSuccess(videoEntities: ArrayList<VideoEntity?>, nextPage: Any?) {
                onResult(videoEntities.filterNotNull(), nextPage)
            }

            override fun onError(e: Exception?) {
                FirebaseEventUtils.getInstances().recordException(e)
                onResult(null, null)
            }

            override fun onRecordException(e: Exception) {
                FirebaseEventUtils.getInstances().recordException(e)
                onResult(null, null)
            }
        })
    }

    fun getMoreResult(
        key: String,
        activity: Activity,
        nextPage: Any,
        onResult: (videoEntities: List<VideoEntity>, nextPage: Any?) -> Unit,
    ) {
        ApiServices.searchMore(activity, key, nextPage, object : SearchCallback {
            override fun onSuccess(videoEntities: ArrayList<VideoEntity?>, nextPage: Any?) {
                onResult(videoEntities.filterNotNull(), nextPage)
            }

            override fun onError(e: Exception?) {
                FirebaseEventUtils.getInstances().recordException(e)
                onResult(emptyList(), null)
            }

            override fun onRecordException(e: Exception) {
                FirebaseEventUtils.getInstances().recordException(e)
                onResult(emptyList(), null)
            }
        })
    }
}
