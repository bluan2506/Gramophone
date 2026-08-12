package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.app.Activity
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.OnlineSearchRepository.getMoreResultFMA
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.OnlineSearchRepository.getResultFMA
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

//    fun getResult(
//        key: String,
//        activity: Activity,
//        onResult: (videoEntities: List<VideoEntity>?, nextPage: Any?) -> Unit,
//    ) {
//        ApiServices.search(activity, key, object : SearchCallback {
//            override fun onSuccess(videoEntities: ArrayList<VideoEntity?>, nextPage: Any?) {
//                onResult(videoEntities.filterNotNull(), nextPage)
//            }
//
//            override fun onError(e: Exception?) {
//                FirebaseEventUtils.getInstances().recordException(e)
//                onResult(null, null)
//            }
//
//            override fun onRecordException(e: Exception) {
//                FirebaseEventUtils.getInstances().recordException(e)
//                onResult(null, null)
//            }
//        })
//    }

    /**
     * Same contract as [getResult], but the results come from Free Music Archive (scraped with
     * jsoup by [FreeMusicArchiveApi]) instead of the searchapi lib. The `nextPage` handed back is
     * the next FMA results-page URL — feed it to [getMoreResultFMA], not to [getMoreResult].
     */
    fun getResultFMA(
        key: String,
        activity: Activity,
        onResult: (videoEntities: List<VideoEntity>?, nextPage: Any?) -> Unit,
    ) {
        FreeMusicArchiveApi.search(activity, key, object : SearchCallback {
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

    /** Load-more counterpart of [getResultFMA]; [nextPage] is the token it returned. */
    fun getMoreResultFMA(
        key: String,
        activity: Activity,
        nextPage: Any,
        onResult: (videoEntities: List<VideoEntity>, nextPage: Any?) -> Unit,
    ) {
        FreeMusicArchiveApi.searchMore(activity, key, nextPage, object : SearchCallback {
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

    /**
     * Same contract as [getResultFMA], but the results come from ccMixter (its public JSON query
     * API, via [CcMixterApi]). The `nextPage` handed back is the offset of the next page — feed it
     * to [getMoreResultCcMixter], not to [getMoreResultFMA].
     */
    fun getResultCcMixter(
        key: String,
        activity: Activity,
        onResult: (videoEntities: List<VideoEntity>?, nextPage: Any?) -> Unit,
    ) {
        CcMixterApi.search(activity, key, object : SearchCallback {
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

    /** Load-more counterpart of [getResultCcMixter]; [nextPage] is the token it returned. */
    fun getMoreResultCcMixter(
        key: String,
        activity: Activity,
        nextPage: Any,
        onResult: (videoEntities: List<VideoEntity>, nextPage: Any?) -> Unit,
    ) {
        CcMixterApi.searchMore(activity, key, nextPage, object : SearchCallback {
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

//    fun getMoreResult(
//        key: String,
//        activity: Activity,
//        nextPage: Any,
//        onResult: (videoEntities: List<VideoEntity>, nextPage: Any?) -> Unit,
//    ) {
//        ApiServices.searchMore(activity, key, nextPage, object : SearchCallback {
//            override fun onSuccess(videoEntities: ArrayList<VideoEntity?>, nextPage: Any?) {
//                onResult(videoEntities.filterNotNull(), nextPage)
//            }
//
//            override fun onError(e: Exception?) {
//                FirebaseEventUtils.getInstances().recordException(e)
//                onResult(emptyList(), null)
//            }
//
//            override fun onRecordException(e: Exception) {
//                FirebaseEventUtils.getInstances().recordException(e)
//                onResult(emptyList(), null)
//            }
//        })
//    }
}
