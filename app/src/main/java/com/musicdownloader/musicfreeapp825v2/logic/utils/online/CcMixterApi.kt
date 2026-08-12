package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * ccMixter search, served by the site's public JSON query API (`/api/query?f=json&search=...`) —
 * no scraping, unlike [FreeMusicArchiveApi].
 *
 * Drop-in replacement for `com.music.searchapi.ApiServices.search` / `searchMore`: same argument
 * lists, same [SearchCallback], same [VideoEntity] results — so a call site only has to swap the
 * object name:
 *
 * ```
 * ApiServices.search(activity, key, callback)          ->  CcMixterApi.search(activity, key, callback)
 * ApiServices.searchMore(activity, key, next, callback) -> CcMixterApi.searchMore(activity, key, next, callback)
 * ```
 *
 * `context` is accepted only for signature compatibility and is never used.
 *
 * Both calls do the HTTP work on a background thread and deliver the callback on the main thread,
 * exactly like the lib does. The `nextPage` token handed to [SearchCallback.onSuccess] is the
 * offset of the next page ([Int], or null on the last page); feed it straight back into
 * [searchMore]. A results-page URL ([String]) is accepted there too.
 *
 * Each result already carries a playable `stream_link` (the mp3 under `/content/`), so
 * `ApiServices.getLink` is not needed for these entities — `OnlineSearchFragment` plays and
 * downloads them directly. `image_link` is left null because the query API ships no artwork at all;
 * call [loadArtwork] lazily if covers are wanted.
 *
 * **Hot-link protection:** ccMixter answers `403` for the mp3s under `/content/` unless the request
 * carries a `Referer` from the site — see [STREAM_REFERER]. Both paths are wired for it:
 * downloading, because `DownloadController.startMission` forwards [VideoEntity.ccmixterReferrer]
 * (the track page URL stashed below) and `DownloadManagerImpl`/`DownloadRunnable` send it as the
 * `Referer` header; and streaming, because `MusicDownloaderPlaybackService` resolves every
 * [needsStreamReferer] URI through a `ResolvingDataSource` that adds the header.
 *
 * JSON this parses (`https://ccmixter.org/api/query?f=json&search=<q>&limit=<n>&offset=<n>`):
 * a flat array of uploads, each with upload_id/upload_name/user_real_name/file_page_url plus a
 * `files` array — the playable mix is its lowest-ordered `audio` entry, carrying `download_url` and
 * a `file_format_info.ps` duration.
 */
object CcMixterApi {

    /** Written into [VideoEntity.videoType]; kept out of "un" so the logo/copyright check is skipped. */
    const val SOURCE = "ccmixter"

    private const val HOST_NAME = "ccmixter.org"

    /** Any ccmixter.org referer unlocks the mp3s; a foreign one (or none) still gets a 403. */
    const val STREAM_REFERER = "https://$HOST_NAME/"

    private const val HOST = "https://$HOST_NAME"
    private const val QUERY_URL = "$HOST/api/query"
    private const val PAGE_SIZE = 20
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0.0.0 Safari/537.36"
    private const val TIMEOUT_MS = 20_000

    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "CcMixterSearch").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    /** `mm:ss` or `h:mm:ss`, as reported in `file_format_info.ps`. */
    private val DURATION_REGEX = Regex("""^(\d{1,2}):([0-5]\d)(?::([0-5]\d))?$""")

    /** `offset=<n>` inside a results-page URL — for the [String] form of the `nextPage` token. */
    private val OFFSET_REGEX = Regex("""[?&]offset=(\d+)""")

    // --- public API (mirrors ApiServices) ---

    @JvmStatic
    fun search(context: Context?, key: String?, callback: SearchCallback) {
        val query = key?.trim().orEmpty()
        if (query.isEmpty()) {
            mainHandler.post { callback.onSuccess(ArrayList(), null) }
            return
        }
        load(query, 0, callback)
    }

    @JvmStatic
    fun searchMore(context: Context?, key: String?, nextPage: Any?, callback: SearchCallback) {
        val query = key?.trim().orEmpty()
        val offset = resolveOffset(nextPage)
        if (query.isEmpty() || offset == null) {
            // No token left -> report "no more results" rather than an error; the view model then
            // flips hasMore off and stops asking.
            mainHandler.post { callback.onSuccess(ArrayList(), null) }
            return
        }
        load(query, offset, callback)
    }

    /**
     * Fills [entity]'s `image_link` from the track page — one extra request, so it is *not* part of
     * [search]. ccMixter has no per-track cover; what the page shows (and what this picks up) is the
     * artist's avatar.
     *
     * Blocking: call from a background thread. Returns true when the entity was changed. The track
     * page URL is the one [search] stashed in [VideoEntity.ccmixterReferrer].
     */
    @JvmStatic
    fun loadArtwork(entity: VideoEntity?): Boolean {
        val trackUrl = entity?.ccmixterReferrer?.takeIf { it.startsWith("http") } ?: return false
        return try {
            val image = Jsoup.connect(trackUrl)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get()
                .selectFirst("img[src*=/content/][style*=float]")
                ?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
                ?: return false
            // Images are not hot-link protected, so this one loads without a Referer.
            entity.image_link = image
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * True when [uri] points at ccMixter and so has to be fetched with a `Referer:`
     * [STREAM_REFERER] — i.e. the player is about to stream one of the entities [search] returned.
     * False for every other host (and for local `file://`/`content://` playback), so nothing else
     * ever sees the header.
     */
    @JvmStatic
    fun needsStreamReferer(uri: Uri?): Boolean {
        val host = uri?.host ?: return false
        return host.equals(HOST_NAME, ignoreCase = true) ||
            host.endsWith(".$HOST_NAME", ignoreCase = true)
    }

    // --- internals ---

    private fun load(query: String, offset: Int, callback: SearchCallback) {
        executor.execute {
            try {
                val items = parse(JSONArray(fetch(buildUrl(query, offset))))
                // The API reports no total; a short page is the last one.
                val next = if (items.size < PAGE_SIZE) null else offset + PAGE_SIZE
                mainHandler.post { callback.onSuccess(items, next) }
            } catch (e: Exception) {
                mainHandler.post { callback.onError(e) }
            }
        }
    }

    private fun fetch(url: String): String {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("ccMixter query failed: HTTP $code")
            // Descriptions occasionally carry a stray non-UTF-8 byte; the reader substitutes it
            // instead of throwing, which keeps the rest of the page parseable.
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn?.disconnect()
        }
    }

    private fun parse(array: JSONArray): ArrayList<VideoEntity> {
        val out = ArrayList<VideoEntity>()
        for (i in 0 until array.length()) {
            parseUpload(array.optJSONObject(i))?.let { out.add(it) }
        }
        return out
    }

    private fun parseUpload(upload: JSONObject?): VideoEntity? {
        if (upload == null) return null

        val title = upload.text("upload_name")
        if (title.isEmpty()) return null

        val file = pickAudioFile(upload.optJSONArray("files")) ?: return null
        val stream = file.string("download_url")
        if (!stream.startsWith("http")) return null

        val id = upload.string("upload_id")
        val user = upload.string("user_name")
        val artist = upload.text("user_real_name").ifEmpty { user }
        val duration = (file.optJSONObject("file_format_info")).string("ps")
        // Referer source for the download, and the page loadArtwork() reads.
        val trackUrl = upload.string("file_page_url")
            .ifEmpty { if (user.isEmpty() || id.isEmpty()) HOST else "$HOST/files/$user/$id" }

        return VideoEntity().apply {
            videoId = id.ifEmpty { stream }
            videoTile = title
            videoType = SOURCE
            artist_name = artist
            stream_link = stream
            durationString = duration
            this.duration = parseDurationSeconds(duration)
            licence = upload.text("license_name")
            // Everything on ccMixter is Creative Commons licensed and downloadable by design.
            allow_download = true
            isDownloadable = true
            // Used as the Referer by DownloadController.startMission, and by loadArtwork().
            ccmixterReferrer = trackUrl
        }
    }

    /**
     * An upload bundles the mix with its stems, samples and the occasional zip; the mix is the
     * lowest-ordered audio file (`file_order` 0 in practice, but the array order is not guaranteed).
     */
    private fun pickAudioFile(files: JSONArray?): JSONObject? {
        if (files == null) return null
        var best: JSONObject? = null
        var bestOrder = Int.MAX_VALUE
        for (i in 0 until files.length()) {
            val file = files.optJSONObject(i) ?: continue
            val info = file.optJSONObject("file_format_info")
            val isAudio = info.string("media-type") == "audio" ||
                info.string("mime_type").startsWith("audio/")
            if (!isAudio) continue
            val order = file.optInt("file_order", i)
            if (order < bestOrder) {
                best = file
                bestOrder = order
            }
        }
        return best
    }

    /** `optString` on a possibly-absent blob, with JSON's literal "null" treated as absent. */
    private fun JSONObject?.string(name: String): String =
        this?.optString(name).orEmpty().trim().takeIf { it != "null" }.orEmpty()

    /** [string] for fields the API returns HTML-escaped (titles, artist names, licence names). */
    private fun JSONObject?.text(name: String): String =
        Parser.unescapeEntities(string(name), false).trim()

    private fun parseDurationSeconds(text: String): Int {
        val groups = DURATION_REGEX.matchEntire(text)?.groupValues ?: return 0
        val a = groups[1].toInt()
        val b = groups[2].toInt()
        val c = groups[3]
        return if (c.isEmpty()) a * 60 + b else a * 3600 + b * 60 + c.toInt()
    }

    private fun buildUrl(query: String, offset: Int): String {
        val q = URLEncoder.encode(query, "UTF-8")
        return "$QUERY_URL?f=json&search=$q&limit=$PAGE_SIZE&offset=${offset.coerceAtLeast(0)}"
    }

    /** [nextPage] is whatever came back from [SearchCallback.onSuccess]: an offset or a page URL. */
    private fun resolveOffset(nextPage: Any?): Int? = when (nextPage) {
        is Number -> nextPage.toInt().takeIf { it >= 0 }
        is String ->
            if (nextPage.startsWith("http")) OFFSET_REGEX.find(nextPage)?.groupValues?.get(1)?.toIntOrNull()
            else nextPage.trim().toIntOrNull()?.takeIf { it >= 0 }

        else -> null
    }
}
