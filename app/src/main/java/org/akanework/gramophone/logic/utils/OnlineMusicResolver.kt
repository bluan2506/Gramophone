package org.akanework.gramophone.logic.utils

import android.app.Activity
import com.music.searchapi.ApiServices
import com.music.searchapi.`object`.VideoEntity
import com.videoapps.lib.GetMusicLinkCallback
import com.videoapps.lib.`object`.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves a [VideoEntity] into a direct, playable/downloadable URL.
 *
 * Ported 1:1 from the MSDownloader reference flow:
 *  - non-YouTube sources deliver the URL through [GetMusicLinkCallback.onSuccess] (first String arg);
 *  - YouTube (`videoType == "un"`) delivers a JSON blob through [GetMusicLinkCallback.onSuccess_V2],
 *    from which [getBestLink] picks the higher-bitrate audio stream;
 *  - an audio link that returns HTTP 403 (expired googlevideo URL) falls back to `stream.url`
 *    (a 360p video URL the player extracts audio from).
 *
 * [onResult] is always invoked exactly once, on the main thread, with the final URL or null on failure.
 */
object OnlineMusicResolver {

    private const val FORBIDDEN_CHECK_TIMEOUT_MS = 2_000

    /** Must be called on the main thread (ApiServices.getLink requirement). */
    fun resolve(activity: Activity, entity: VideoEntity, onResult: (String?) -> Unit) {
        // Already have a direct stream link -> nothing to resolve.
        if (!entity.stream_link.isNullOrBlank()) {
            onResult(entity.stream_link)
            return
        }

        val source = entity.videoType
        val request = VideoEntity().apply {
            videoId = entity.videoId
            videoType = entity.videoType
        }

        ApiServices.getLink(activity, request, object : GetMusicLinkCallback {
            override fun onSuccess(
                link: String?,
                stream: Stream?,
                allowDownload: Boolean,
                notAllowDownloadReason: String?
            ) {
                // Non-YouTube sources only; YouTube is handled by onSuccess_V2.
                if (link == null || source == "un") return
                finish(link, stream)
            }

            override fun onSuccess_V2(
                jsonData: String?,
                stream: Stream?,
                allowDownload: Boolean,
                notAllowDownloadReason: String?
            ) {
                // YouTube only; ccmixter is handled by onSuccess.
                if (jsonData == null || source == "ccmixter") return
                finish(getBestLink(jsonData), stream)
            }

            override fun onError(e: Exception?) {
                e?.let { FirebaseEventUtils.getInstances().recordException(it) }
                postMain { onResult(null) }
            }

            override fun onRecordException(e: Exception) {
                FirebaseEventUtils.getInstances().recordException(e)
                postMain { onResult(null) }
            }

            // The 403 pre-check must run off the main thread (it opens a synchronous connection).
            private fun finish(link: String, stream: Stream?) {
                CoroutineScope(Dispatchers.IO).launch {
                    val finalLink = if (isLink403(link)) (stream?.url ?: link) else link
                    withContext(Dispatchers.Main) { onResult(finalLink) }
                }
            }
        })
    }

    private fun postMain(block: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch { block() }
    }

    private fun getBestLink(sJson: String?): String {
        if (sJson == null) return ""
        return try {
            val json = JSONObject(sJson)
            val bestM4a = json.getJSONObject("best_m4a")
            val bestM4aLink = bestM4a.getString("link")
            val bestM4aBitrate = bestM4a.getInt("nittate")
            val bestWebm = json.getJSONObject("best_webm")
            val bestWebmLink = bestWebm.getString("link")
            val bestWebmBitrate = bestWebm.getInt("nittate")
            if (bestM4aBitrate > bestWebmBitrate) bestM4aLink else bestWebmLink
        } catch (e: Exception) {
            FirebaseEventUtils.getInstances().recordException(e)
            ""
        }
    }

    private fun isLink403(link: String?): Boolean {
        if (link.isNullOrEmpty()) return false
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(link).openConnection() as HttpURLConnection).apply {
                // GET + Range "bytes=0-" mirrors the real request ExoPlayer/the downloader sends;
                // HEAD or a 1-byte range reports the wrong status for googlevideo URLs.
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-")
                connectTimeout = FORBIDDEN_CHECK_TIMEOUT_MS
                readTimeout = FORBIDDEN_CHECK_TIMEOUT_MS
            }
            conn.responseCode == 403
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
