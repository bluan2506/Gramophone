package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.FreeMusicArchiveApi.loadArtwork
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.FreeMusicArchiveApi.search
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.FreeMusicArchiveApi.searchMore
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Free Music Archive search, scraped from the public search page with jsoup.
 *
 * Drop-in replacement for `com.music.searchapi.ApiServices.search` / `searchMore`: same argument
 * lists, same [SearchCallback], same [VideoEntity] results — so a call site only has to swap the
 * object name:
 *
 * ```
 * ApiServices.search(activity, key, callback)          ->  FreeMusicArchiveApi.search(activity, key, callback)
 * ApiServices.searchMore(activity, key, next, callback) -> FreeMusicArchiveApi.searchMore(activity, key, next, callback)
 * ```
 *
 * `context` is accepted only for signature compatibility and is never used.
 *
 * Both calls do the HTTP work on a background thread and deliver the callback on the main thread,
 * exactly like the lib does. The `nextPage` token handed to [SearchCallback.onSuccess] is the
 * absolute URL of the next results page (or `null` on the last page); feed it straight back into
 * [searchMore]. A page number ([Int]) is accepted there too.
 *
 * Each result already carries a playable `stream_link` (FMA's `/track/<handle>/stream/`, a 302 to
 * the raw mp3), so `ApiServices.getLink` is not needed for these entities — `OnlineSearchFragment`
 * plays and downloads them directly. `image_link` is left null because the search page ships no
 * artwork at all; call [loadArtwork] lazily if covers are wanted.
 *
 * Page markup this parses (`https://freemusicarchive.org/search/?quicksearch=<q>&pageSize=<n>&page=<p>`):
 * one `div.play-item` per track, carrying a `data-track-info` JSON blob with id/title/artistName/
 * playbackUrl, plus the duration in a sibling span and `a.pagination-next` for the next page.
 */
object FreeMusicArchiveApi {

    /** Written into [VideoEntity.videoType]; kept out of "un" so the logo/copyright check is skipped. */
    const val SOURCE = "fma"

    private const val HOST = "https://freemusicarchive.org"
    private const val SEARCH_URL = "$HOST/search/"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0.0.0 Safari/537.36"
    private const val TIMEOUT_MS = 20_000

    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "FmaSearch").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    /** `mm:ss` or `h:mm:ss`, as printed in the duration column. */
    private val DURATION_REGEX = Regex("""^(\d{1,2}):([0-5]\d)(?::([0-5]\d))?$""")

    /** The item's `tid-<id>` css class — fallback when `data-track-info` is missing. */
    private val TRACK_ID_REGEX = Regex("""\btid-(\d+)\b""")

    // --- public API (mirrors ApiServices) ---

    @JvmStatic
    fun search(context: Context?, key: String?, callback: SearchCallback) {
        val query = key?.trim().orEmpty()
        if (query.isEmpty()) {
            mainHandler.post { callback.onSuccess(ArrayList(), null) }
            return
        }
        load(buildUrl(query, 1), callback)
    }

    @JvmStatic
    fun searchMore(context: Context?, key: String?, nextPage: Any?, callback: SearchCallback) {
        val url = resolveNextPageUrl(key?.trim().orEmpty(), nextPage)
        if (url == null) {
            // No token left -> report "no more results" rather than an error; the view model then
            // flips hasMore off and stops asking.
            mainHandler.post { callback.onSuccess(ArrayList(), null) }
            return
        }
        load(url, callback)
    }

    /**
     * Fills [entity]'s `image_link` (album cover) and upgrades `stream_link` to the direct mp3 by
     * reading the track page — one extra request, so it is *not* part of [search].
     *
     * Blocking: call from a background thread. Returns true when the entity was changed. The track
     * page URL is the one [search] stashed in [VideoEntity.ccmixterReferrer].
     */
    @JvmStatic
    fun loadArtwork(entity: VideoEntity?): Boolean {
        val trackUrl = entity?.ccmixterReferrer?.takeIf { it.startsWith("http") } ?: return false
        return try {
            val doc = connect(trackUrl)
            var changed = false
            doc.selectFirst("img[src*='/image/?file=']")?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
                ?.let { entity.image_link = it; changed = true }
            // The track page's data-track-info adds fileUrl: the mp3 without the /stream/ redirect.
            trackInfo(doc.selectFirst("[data-track-info]"))
                ?.optString("fileUrl")?.takeIf { it.startsWith("http") }
                ?.let { entity.stream_link = it; changed = true }
            changed
        } catch (e: Exception) {
            false
        }
    }

    // --- internals ---

    private fun load(url: String, callback: SearchCallback) {
        executor.execute {
            try {
                val doc = connect(url)
                val items = parse(doc)
                val next = doc.selectFirst("a.pagination-next")
                    ?.absUrl("href")
                    ?.takeIf { it.isNotBlank() }
                mainHandler.post { callback.onSuccess(items, next) }
            } catch (e: Exception) {
                mainHandler.post { callback.onError(e) }
            }
        }
    }

    private fun connect(url: String): Document =
        Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(TIMEOUT_MS)
            .maxBodySize(0) // a pageSize=200 page is ~1 MB, over jsoup's 2 MB-ish default margin
            .followRedirects(true)
            .get()

    private fun parse(doc: Document): ArrayList<VideoEntity> {
        val out = ArrayList<VideoEntity>()
        for (element in doc.select("div.play-item")) {
            parseItem(element)?.let { out.add(it) }
        }
        return out
    }

    private fun parseItem(element: Element): VideoEntity? {
        val info = trackInfo(element)

        // Prefer the JSON blob; fall back to the visible markup if FMA ever drops the attribute.
        val trackLink = element.selectFirst("span.ptxt-track a")
        val title = info.string("title").ifEmpty { trackLink?.text().orEmpty().trim() }
        if (title.isEmpty()) return null

        val trackUrl = info.string("url").ifEmpty { trackLink?.absUrl("href").orEmpty() }
        val handle = info.string("handle")
            .ifEmpty { trackUrl.trimEnd('/').substringAfterLast('/', "") }
        val stream = info.string("playbackUrl")
            .ifEmpty { if (handle.isEmpty()) "" else "$HOST/track/$handle/stream/" }
        if (stream.isEmpty()) return null

        val id = info.string("id")
            .ifEmpty { TRACK_ID_REGEX.find(element.className())?.groupValues?.get(1).orEmpty() }
            .ifEmpty { handle }
        val artist = info.string("artistName")
            .ifEmpty { element.selectFirst("span.ptxt-artist a")?.text().orEmpty().trim() }
        val duration = findDurationText(element)

        return VideoEntity().apply {
            videoId = id
            videoTile = title
            videoType = SOURCE
            artist_name = artist
            stream_link = stream
            durationString = duration
            this.duration = parseDurationSeconds(duration)
            // Everything on FMA is freely downloadable, and the mp3 behind stream_link is the file
            // the site's own download button serves.
            allow_download = true
            isDownloadable = true
            // Used as the Referer by DownloadController.startMission, and by loadArtwork().
            ccmixterReferrer = trackUrl
        }
    }

    private fun trackInfo(element: Element?): JSONObject? {
        val raw = element?.attr("data-track-info")?.takeIf { it.isNotBlank() } ?: return null
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            null
        }
    }

    /** `optString` on a possibly-absent blob; also coerces the numeric `id` the track page uses. */
    private fun JSONObject?.string(name: String): String =
        this?.optString(name).orEmpty().trim().takeIf { it != "null" }.orEmpty()

    private fun findDurationText(element: Element): String {
        for (span in element.select("span")) {
            val text = span.ownText().trim()
            if (DURATION_REGEX.matches(text)) return text
        }
        return ""
    }

    private fun parseDurationSeconds(text: String): Int {
        val groups = DURATION_REGEX.matchEntire(text)?.groupValues ?: return 0
        val a = groups[1].toInt()
        val b = groups[2].toInt()
        val c = groups[3]
        return if (c.isEmpty()) a * 60 + b else a * 3600 + b * 60 + c.toInt()
    }

    private fun buildUrl(query: String, page: Int): String {
        val q = URLEncoder.encode(query, "UTF-8")
        return "$SEARCH_URL?quicksearch=$q&page=${page.coerceAtLeast(1)}"
    }

    /** [nextPage] is whatever came back from [SearchCallback.onSuccess]: a page URL or a page number. */
    private fun resolveNextPageUrl(query: String, nextPage: Any?): String? = when (nextPage) {
        is String ->
            if (nextPage.startsWith("http")) nextPage
            else nextPage.trim().toIntOrNull()?.let { page -> buildUrl(query, page).takeIf { query.isNotEmpty() } }

        is Number -> if (query.isEmpty()) null else buildUrl(query, nextPage.toInt())
        else -> null
    }
}
