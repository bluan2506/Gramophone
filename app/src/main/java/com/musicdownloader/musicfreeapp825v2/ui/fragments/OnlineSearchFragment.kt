package com.musicdownloader.musicfreeapp825v2.ui.fragments

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applogevent.logeventlib.LogEventLibs
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.appbar.AppBarLayout
import com.music.searchapi.ApiServices
import com.music.searchapi.`object`.VideoEntity
import com.thinkup.nativead.api.TUNativeAdView
import com.videoapps.lib.GetMusicLinkCallback
import com.videoapps.lib.`object`.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.logic.MusicDownloaderApplication
import com.musicdownloader.musicfreeapp825v2.databinding.DialogOnlineLoadingBinding
import com.musicdownloader.musicfreeapp825v2.databinding.FragmentOnlineSearchBinding
import com.musicdownloader.musicfreeapp825v2.logic.enableEdgeToEdgePaddingListener
import com.musicdownloader.musicfreeapp825v2.logic.utils.NetworkUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.Keys
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.CopyrightRestrictionsDialog
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.DownloadController
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.VpnProxyDialog
import com.musicdownloader.musicfreeapp825v2.ui.MediaControllerViewModel
import com.musicdownloader.musicfreeapp825v2.ui.adapters.OnlineSongAdapter
import com.musicdownloader.musicfreeapp825v2.ui.adapters.SuggestAdapter
import org.json.JSONObject
import us.shandian.giga.util.Utility
import java.net.HttpURLConnection
import java.net.URL

/**
 * OnlineSearchFragment:
 *   Online music search, ported from the MSDownloader `SearchOnlineFragment`. Suggestions as you
 * type, a results list with load-more, and per-result stream (tap the row) or download (tap the
 * download icon). Playback goes through Gramophone's media3 controller; downloads go through the
 * giga [DownloadController].
 */
class OnlineSearchFragment : BaseFragment(true) {

    companion object {
        // Broadcast from the download service when a link 403s mid-download, so we drop the stale
        // stream_link and the next tap re-resolves it. Ported from the reference.
        const val ACTION_LINK_EXPIRED = "com.musicdownloader.musicfreeapp825v2.action.ONLINE_LINK_EXPIRED"
        const val KEY_VIDEO_ID = "video_id"

        private const val FORBIDDEN_CHECK_TIMEOUT_MS = 2_000

        // How many matching past searches to surface at the top of the online suggestion list.
        private const val HISTORY_IN_SUGGEST_LIMIT = 3
    }

    private var _binding: FragmentOnlineSearchBinding? = null
    private val binding get() = _binding!!

    // Activity-scoped so the search results (and pagination state) survive this fragment being
    // popped and re-opened — mirrors the sample's shared LibraryViewModel (`by sharedViewModel()`).
    private val viewModel: OnlineSearchViewModel by activityViewModels()
    private val controllerViewModel: MediaControllerViewModel by activityViewModels()

    private lateinit var songAdapter: OnlineSongAdapter
    private lateinit var suggestAdapter: SuggestAdapter

    private val appConfig by lazy {
        (mainActivity.application as MusicDownloaderApplication).configEntity
    }

    // A single reused native-ad placeholder so load-more re-emissions don't spawn a new ad each time.
    private var nativeAdPlaceholder: Any? = null

    // When a download link expires, clear that videoId's cached stream_link so the next tap re-resolves.
    private val linkExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val videoId = intent?.getStringExtra(KEY_VIDEO_ID) ?: return
            currentResults().forEach {
                if (it.videoId == videoId) it.stream_link = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnlineSearchBinding.inflate(inflater, container, false)
        val appBarLayout = binding.root.findViewById<AppBarLayout>(R.id.appbarlayout)
        appBarLayout.enableEdgeToEdgePaddingListener()

        songAdapter = OnlineSongAdapter(
            activity = mainActivity,
            onClickItem = { onClickSongItem(it) },
            onClickDownload = { onClickDownload(it) },
        )
        suggestAdapter = SuggestAdapter(
            onClickSuggest = { onClickSuggest(it) },
            onClickFill = { fillSearchBox(it) },
            onClickRemove = { removeHistory(it) },
        )

        binding.recyclerViewResult.apply {
            enableEdgeToEdgePaddingListener(ime = true)
            layoutManager = LinearLayoutManager(activity)
            adapter = songAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val lm = rv.layoutManager as? LinearLayoutManager ?: return
                    val visible = lm.childCount
                    val total = lm.itemCount
                    val past = lm.findFirstVisibleItemPosition()
                    if (past + visible >= total && total > 0) {
                        viewModel.getMoreSearchResult(binding.searchView.text.toString().trim(), mainActivity)
                    }
                    binding.imgUp.visibility = if (rv.canScrollVertically(-1)) View.VISIBLE else View.GONE
                }
            })
        }
        binding.recyclerViewSuggest.apply {
            enableEdgeToEdgePaddingListener(ime = true)
            layoutManager = LinearLayoutManager(activity)
            adapter = suggestAdapter
        }

        binding.returnButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        binding.clearText.setOnClickListener {
            binding.searchView.setText("")
            binding.searchView.requestFocus()
            showKeyboard()
        }

        binding.searchView.addTextChangedListener { raw ->
            val text = raw?.toString() ?: ""
            // Empty box shows the search icon (tapping it does nothing); with text it becomes an X
            // that clears the field — mirrors the sample.
            binding.clearText.setImageResource(if (text.isBlank()) R.drawable.ic_search else R.drawable.ic_close)
            if (text.isBlank()) {
                viewModel.clearSuggestions()
                // Empty box: fall back to the recent-search list (or the retained results if none).
                showHistoryOrResults()
            } else {
                viewModel.searchSuggest(text, countryCode())
                // Suggestions replace results while the user is typing a new query.
                showSuggestions(true)
                // Show matching past searches at the top right away (before the online call returns).
                renderTypingSuggestions()
            }
        }
        binding.searchView.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onClickSuggest(v.text?.toString()?.trim() ?: "")
                true
            } else false
        }

        binding.imgUp.setOnClickListener {
            binding.recyclerViewResult.smoothScrollToPosition(0)
        }

        // Auto-open the keyboard on entering the search screen, focused on the search box.
        binding.searchView.requestFocus()
        showKeyboard()

        // Drive the "now playing" equalizer from the media3 controller, like the offline song lists.
        controllerViewModel.addRecreationalPlayerListener(
            viewLifecycleOwner.lifecycle,
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updatePlayingState()
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = updatePlayingState()
                override fun onPlaybackStateChanged(playbackState: Int) = updatePlayingState()
            }
        ) { updatePlayingState() }

        observeViewModel()

        viewModel.loadHistory(requireContext())
        // Returning to a screen whose (activity-scoped) ViewModel still holds a previous search:
        // switch back into results mode so the retained list shows again instead of the empty
        // default. The results collector re-emits the retained list and repopulates the adapter.
        // Otherwise (fresh screen) show the recent-search history under the empty query box.
        if (viewModel.searched.value) {
            showSuggestions(false)
        } else {
            showHistoryOrResults()
        }
        return binding.root
    }

    private fun updatePlayingState() {
        val c = controllerViewModel.get()
        val mediaId = c?.currentMediaItem?.mediaId
        val playing = c != null && c.playWhenReady &&
            c.playbackState != Player.STATE_ENDED && c.playbackState != Player.STATE_IDLE
        if (::songAdapter.isInitialized) songAdapter.updateCurrentPlaying(mediaId, playing)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Online suggestions only apply while typing; when the box is empty the
                    // suggestion list is owned by the recent-search history instead. When typing,
                    // matching past searches are merged in at the top (renderTypingSuggestions).
                    viewModel.suggestions.collect {
                        if (binding.searchView.text?.isNotBlank() == true) renderTypingSuggestions()
                    }
                }
                launch {
                    // Keep the visible history in sync (e.g. after removing an entry): re-merge into
                    // the typing suggestions, or refresh the empty-box history list.
                    viewModel.history.collect { list ->
                        if (binding.recyclerViewSuggest.visibility != View.VISIBLE) return@collect
                        if (binding.searchView.text?.isNotBlank() == true) {
                            renderTypingSuggestions()
                        } else {
                            if (list.isEmpty()) showSuggestions(false) else suggestAdapter.setHistory(list)
                        }
                    }
                }
                launch {
                    viewModel.results.collect { list ->
                        songAdapter.setData(withNativeAd(list))
                        updateEmptyState()
                    }
                }
                launch {
                    // Also drive the empty state from loading: an empty search resets results to
                    // emptyList() up front, so the empty result never re-emits (StateFlow drops the
                    // duplicate) — but loading always transitions true->false, so evaluate here too.
                    viewModel.loading.collect {
                        binding.progressLoading.visibility = if (it) View.VISIBLE else View.GONE
                        updateEmptyState()
                    }
                }
                launch {
                    viewModel.loadingMore.collect { binding.progressLoadMore.visibility = if (it) View.VISIBLE else View.GONE }
                }
            }
        }
    }

    private fun currentResults(): List<VideoEntity> = viewModel.results.value

    /**
     * The "no results" text shows only after a finished search that returned nothing, and only while
     * the results list (not the suggestion list) is on screen. It stays hidden while a search is
     * loading and before any search has run.
     */
    private fun updateEmptyState() {
        val b = _binding ?: return
        val show = viewModel.searched.value &&
            !viewModel.loading.value &&
            viewModel.results.value.isEmpty() &&
            b.recyclerViewResult.visibility == View.VISIBLE
        b.emptyView.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Interleaves one native-ad placeholder at index 2 when there are more than two results and the
     * native-search ad is enabled (mirrors the sample's `SearchOnlineFragment`). AdMob vs TopOn is
     * chosen by [ConfigEntity.isToponads]; the adapter dispatches the placeholder type to the right
     * holder. The placeholder instance is cached so re-emissions (load-more) don't reload the ad.
     */
    private fun withNativeAd(list: List<VideoEntity>): List<Any> {
        if (list.size <= 2 || !appConfig.isAds_nativeSearchScreen || context == null) return list
        if (nativeAdPlaceholder == null) {
            nativeAdPlaceholder = if (appConfig.isToponads) {
                TUNativeAdView(requireContext())
            } else {
                NativeAdView(requireContext())
            }
        }
        return buildList {
            addAll(list)
            add(2, nativeAdPlaceholder!!)
        }
    }

    /** Toggle between the suggestion list (typing) and the results list (after a search). */
    private fun showSuggestions(show: Boolean) {
        binding.recyclerViewSuggest.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewResult.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            binding.emptyView.visibility = View.GONE
            binding.imgUp.visibility = View.GONE
        } else {
            // Back in results mode — re-evaluate whether the "no results" text should show.
            updateEmptyState()
        }
    }

    private fun fillSearchBox(text: String) {
        binding.searchView.setText(text)
        binding.searchView.setSelection(text.length)
    }

    private fun onClickSuggest(query: String) {
        if (query.isEmpty()) return
        fillSearchBox(query)
        closeKeyboard()
        showSuggestions(false)
        viewModel.addToHistory(requireContext(), query)
        FirebaseEventUtils.getInstances().logEvent(requireContext(), Keys.USER_SEARCH_ONLINE)
        LogEventLibs.logSearchOnline(query, countryCode())
        viewModel.getSearchResult(query, mainActivity)
    }

    /**
     * Empty query box: show the recent-search history as tappable suggestions if there is any,
     * otherwise fall back to the (possibly retained) results area.
     */
    private fun showHistoryOrResults() {
        val history = viewModel.history.value
        if (history.isNotEmpty()) {
            suggestAdapter.setHistory(history)
            showSuggestions(true)
        } else {
            showSuggestions(false)
        }
    }

    private fun removeHistory(query: String) {
        viewModel.removeFromHistory(requireContext(), query)
    }

    /**
     * Typing mode: show past searches that match the current query at the top (clock rows, tap to
     * search / ✕ to remove), followed by the live online suggestions with those duplicates dropped.
     */
    private fun renderTypingSuggestions() {
        val query = binding.searchView.text?.toString()?.trim().orEmpty()
        if (query.isEmpty()) return
        val matchedHistory = viewModel.history.value
            .filter { it.contains(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
            .take(HISTORY_IN_SUGGEST_LIMIT)
        val online = viewModel.suggestions.value
            .filter { s -> matchedHistory.none { it.equals(s, ignoreCase = true) } }
        suggestAdapter.setMixed(matchedHistory, online)
    }

    // --- Stream ---

    private fun onClickSongItem(entity: VideoEntity) {
        if (!isAdded) return
        val loadingDialog = showLoading()

        CoroutineScope(Dispatchers.IO).launch {
            if (context == null) return@launch
            val id = entity.videoId
            val source = entity.videoType

            if (!NetworkUtils.checkInternet(requireContext())) {
                withContext(Dispatchers.Main) {
                    toast(getString(R.string.no_internet))
                    loadingDialog.dismissSafely()
                }
                return@launch
            }

            if (entity.stream_link.isNullOrBlank()) {
                resolveLink(entity, id, source, loadingDialog, forDownload = false) { finalLink ->
                    playResolved(entity, finalLink, loadingDialog)
                }
            } else {
                // Already have a direct stream link.
                LogEventLibs.logGetLinkSuccess(id, source)
                context?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.GET_LINK_SUCCESS) }
                LogEventLibs.logPlayOnline(id, source, entity.videoTile)
                context?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.USER_PLAY_ONLINE) }
                withContext(Dispatchers.Main) {
                    playResolved(entity, entity.stream_link ?: "", loadingDialog)
                }
            }
        }
    }

    private fun playResolved(entity: VideoEntity, finalLink: String, loadingDialog: Dialog) {
        if (!isAdded) return
        loadingDialog.dismissSafely()
        if (finalLink.isBlank()) {
            toast(getString(R.string.online_search_error))
            return
        }
        val controller = mainActivity.getPlayer()
        if (controller == null) {
            toast(getString(R.string.online_search_error))
            return
        }
        val item = MediaItem.Builder()
            .setUri(finalLink)
            .setMediaId("online:${entity.videoId}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(entity.videoTile)
                    .setArtist(entity.artist_name)
                    .setArtworkUri(entity.image_link?.takeIf { it.isNotBlank() }?.toUri())
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
    }

    // --- Download ---

    private fun onClickDownload(entity: VideoEntity) {
        if (!isAdded) return
        FirebaseEventUtils.getInstances().logEvent(requireContext(), Keys.USER_CLICK_DOWNLOAD)
        val loadingDialog = showLoading()

        CoroutineScope(Dispatchers.IO).launch {
            if (context == null) return@launch
            val id = entity.videoId
            val source = entity.videoType

            if (!NetworkUtils.checkInternet(requireContext())) {
                withContext(Dispatchers.Main) {
                    toast(getString(R.string.no_internet))
                    loadingDialog.dismissSafely()
                }
                return@launch
            }

            if (entity.stream_link.isNullOrBlank()) {
                resolveLink(entity, id, source, loadingDialog, forDownload = true) { finalLink ->
                    // Copyright restricted -> don't download.
                    if (!entity.allow_download) {
                        LogEventLibs.logDownloadMusicRestricted(id, source, entity.videoTile)
                        context?.let { FirebaseEventUtils.getInstances().logEventDownloadMusicRestricted(it) }
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismissSafely()
                            context?.let { CopyrightRestrictionsDialog.show(it) }
                        }
                    } else {
                        startDownload(entity, finalLink, loadingDialog)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    // Copyright restricted -> don't download.
                    if (!entity.allow_download) {
                        LogEventLibs.logDownloadMusicRestricted(id, source, entity.videoTile)
                        context?.let { FirebaseEventUtils.getInstances().logEventDownloadMusicRestricted(it) }
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismissSafely()
                            context?.let { CopyrightRestrictionsDialog.show(it) }
                        }
                    } else {
                        startDownload(entity, entity.stream_link ?: "", loadingDialog)
                    }

                }
            }
        }
    }

    private fun startDownload(entity: VideoEntity, finalLink: String, loadingDialog: Dialog) {
        if (!isAdded) return
        loadingDialog.dismissSafely()
        if (finalLink.isBlank()) {
            toast(getString(R.string.online_search_error))
            return
        }
        entity.stream_link = finalLink
        DownloadController.startMission(
            requireContext(),
            finalLink,
            Utility.formatMp3FileName(entity.videoTile),
            entity.videoId,
            entity.videoType,
            entity.ccmixterReferrer,
        )
        // The download interstitial is fired on download *completion* (ACTION_UPDATE broadcast),
        // handled in MainActivity — mirroring the sample's AbsBaseActivity receiver.
    }

    // --- getLink resolution (ported from the reference) ---
    //
    private fun resolveLink(
        entity: VideoEntity,
        id: String?,
        source: String?,
        loadingDialog: Dialog,
        forDownload: Boolean,
        onResolved: suspend (String) -> Unit,
    ) {
        val request = VideoEntity().apply {
            videoId = id
            videoType = source
        }
        CoroutineScope(Dispatchers.Main).launch {
            ApiServices.getLink(requireActivity(), request, object : GetMusicLinkCallback {
                override fun onSuccess(
                    link: String?,
                    stream: Stream?,
                    allowDownload: Boolean,
                    notAllowDownloadReason: String?,
                ) {
                    if (link == null || source == "un") return
                    logGetLinkSuccess(id, source)
                    CoroutineScope(Dispatchers.IO).launch {
                        val finalLink = finalizeLink(link, stream, id, source)
                        entity.stream_link = finalLink
                        entity.allow_download = allowDownload
                        entity.not_allow_download_reason = notAllowDownloadReason
                        if (!forDownload) logPlayOnline(id, source, entity.videoTile)
                        withContext(Dispatchers.Main) { onResolved(finalLink) }
                    }
                }

                override fun onSuccess_V2(
                    jsonData: String?,
                    stream: Stream?,
                    allowDownload: Boolean,
                    notAllowDownloadReason: String?,
                ) {
                    if (jsonData == null || source == "ccmixter") return
                    logGetLinkSuccess(id, source)
                    CoroutineScope(Dispatchers.IO).launch {
                        val finalLink = finalizeLink(getBestLink(jsonData), stream, id, source)
                        entity.stream_link = finalLink
                        entity.allow_download = allowDownload
                        entity.not_allow_download_reason = notAllowDownloadReason
                        if (!forDownload) logPlayOnline(id, source, entity.videoTile)
                        withContext(Dispatchers.Main) { onResolved(finalLink) }
                    }
                }

                override fun onError(e: Exception?) = onGetLinkFailed(e, id, source, loadingDialog)
                override fun onRecordException(e: Exception) = onGetLinkFailed(e, id, source, loadingDialog)
            })
        }
    }

    private fun finalizeLink(link: String, stream: Stream?, id: String?, source: String?): String {
        val is403 = isLink403(link)
        val finalLink = if (is403) (stream?.url ?: link) else link
        if (is403) {
            context?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.GET_LINK_SUCCESS_VIDEO_360) }
        }
        return finalLink
    }

    private fun logGetLinkSuccess(id: String?, source: String?) {
        LogEventLibs.logGetLinkSuccess(id, source)
        context?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.GET_LINK_SUCCESS) }
    }

    private fun logPlayOnline(id: String?, source: String?, title: String?) {
        LogEventLibs.logPlayOnline(id, source, title)
        context?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.USER_PLAY_ONLINE) }
    }

    private fun onGetLinkFailed(e: Exception?, id: String?, source: String?, loadingDialog: Dialog) {
        FirebaseEventUtils.getInstances().recordException(e)
        LogEventLibs.logGetLinkFailed(id, source)
        context?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.GET_LINK_FAILED) }
        activity?.runOnUiThread {
            if (isAdded) {
                loadingDialog.dismissSafely()
                val ctx = context
                if (source == "un" && ctx != null && NetworkUtils.isVpnOrProxyActive(ctx)) {
                    VpnProxyDialog.show(ctx)
                }
                toast(getString(R.string.online_search_error))
            }
        }
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

    // --- helpers ---

    private fun showLoading(): Dialog {
        closeKeyboard()
        val dialog = Dialog(requireContext())
        val loadingBinding = DialogOnlineLoadingBinding.inflate(layoutInflater)
        dialog.setContentView(loadingBinding.root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            // Full-screen window + the card centred by the layout itself = reliably dead-centre,
            // regardless of status bar / soft keyboard. FLAG_NOT_FOCUSABLE stops the IME resizing it.
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
        dialog.show()
        return dialog
    }

    private fun Dialog.dismissSafely() {
        if (isShowing) runCatching { dismiss() }
    }

    private fun toast(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }

    private fun countryCode(): String =
        resources.configuration.locales[0].country

    private fun showKeyboard() {
        val editText = _binding?.searchView ?: return
        editText.requestFocus()
        // Post (with a small delay) so the IME reliably shows even right after the go-to-search
        // interstitial dismisses — showSoftInput straight away is often swallowed while the window
        // is still regaining focus from the ad activity.
        editText.postDelayed({
            if (!isAdded) return@postDelayed
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun closeKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            ContextCompat.registerReceiver(
                it, linkExpiredReceiver, IntentFilter(ACTION_LINK_EXPIRED),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { context?.unregisterReceiver(linkExpiredReceiver) }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden && !mainActivity.playerBottomSheet.visibleAndExpanded) {
            binding.searchView.requestFocus()
            showKeyboard()
        }
    }

    override fun onPause() {
        if (!isHidden) closeKeyboard()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
