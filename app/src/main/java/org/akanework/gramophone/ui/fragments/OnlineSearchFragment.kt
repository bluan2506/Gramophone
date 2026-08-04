package org.akanework.gramophone.ui.fragments

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applogevent.logeventlib.LogEventLibs
import com.google.android.material.appbar.AppBarLayout
import com.music.searchapi.ApiServices
import com.music.searchapi.`object`.VideoEntity
import com.videoapps.lib.GetMusicLinkCallback
import com.videoapps.lib.`object`.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.databinding.DialogOnlineLoadingBinding
import org.akanework.gramophone.databinding.FragmentOnlineSearchBinding
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.utils.NetworkUtils
import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils
import org.akanework.gramophone.logic.utils.firebase.Keys
import org.akanework.gramophone.logic.utils.online.CopyrightRestrictionsDialog
import org.akanework.gramophone.logic.utils.online.DownloadController
import org.akanework.gramophone.ui.MediaControllerViewModel
import org.akanework.gramophone.ui.adapters.OnlineSongAdapter
import org.akanework.gramophone.ui.adapters.SuggestAdapter
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
        const val ACTION_LINK_EXPIRED = "org.akanework.gramophone.action.ONLINE_LINK_EXPIRED"
        const val KEY_VIDEO_ID = "video_id"

        private const val FORBIDDEN_CHECK_TIMEOUT_MS = 2_000
    }

    private var _binding: FragmentOnlineSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnlineSearchViewModel by viewModels()
    private val controllerViewModel: MediaControllerViewModel by activityViewModels()

    private lateinit var songAdapter: OnlineSongAdapter
    private lateinit var suggestAdapter: SuggestAdapter

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
            onClickItem = { onClickSongItem(it) },
            onClickDownload = { onClickDownload(it) },
        )
        suggestAdapter = SuggestAdapter(
            onClickSuggest = { onClickSuggest(it) },
            onClickFill = { fillSearchBox(it) },
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
            if (text.isBlank()) {
                viewModel.clearSuggestions()
                showSuggestions(false)
            } else {
                viewModel.searchSuggest(text, countryCode())
                // Suggestions replace results while the user is typing a new query.
                showSuggestions(true)
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
                    viewModel.suggestions.collect { suggestAdapter.setData(it) }
                }
                launch {
                    viewModel.results.collect { list ->
                        songAdapter.setData(list)
                        if (viewModel.searched.value && !viewModel.loading.value) {
                            binding.emptyView.visibility =
                                if (list.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.loading.collect { binding.progressLoading.visibility = if (it) View.VISIBLE else View.GONE }
                }
                launch {
                    viewModel.loadingMore.collect { binding.progressLoadMore.visibility = if (it) View.VISIBLE else View.GONE }
                }
            }
        }
    }

    private fun currentResults(): List<VideoEntity> = viewModel.results.value

    /** Toggle between the suggestion list (typing) and the results list (after a search). */
    private fun showSuggestions(show: Boolean) {
        binding.recyclerViewSuggest.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewResult.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            binding.emptyView.visibility = View.GONE
            binding.imgUp.visibility = View.GONE
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
        FirebaseEventUtils.getInstances().logEvent(requireContext(), Keys.USER_SEARCH_ONLINE)
        LogEventLibs.logSearchOnline(query, countryCode())
        viewModel.getSearchResult(query, mainActivity)
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

            if (entity.stream_link.isNullOrBlank()
                && entity.allow_download
                && entity.not_allow_download_reason.isNullOrBlank()
            ) {
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

            // Copyright restricted -> don't download.
            if (!entity.allow_download || !entity.not_allow_download_reason.isNullOrBlank()) {
                LogEventLibs.logDownloadMusicRestricted(id, source, entity.videoTile)
                context?.let { FirebaseEventUtils.getInstances().logEventDownloadMusicRestricted(it) }
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissSafely()
                    context?.let { CopyrightRestrictionsDialog.show(it) }
                }
                return@launch
            }

            if (entity.stream_link.isNullOrBlank()) {
                resolveLink(entity, id, source, loadingDialog, forDownload = true) { finalLink ->
                    startDownload(entity, finalLink, loadingDialog)
                }
            } else {
                withContext(Dispatchers.Main) {
                    startDownload(entity, entity.stream_link ?: "", loadingDialog)
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
    }

    // --- getLink resolution (ported from the reference) ---
    //
    // Non-YouTube sources deliver the URL via onSuccess (first String); YouTube (videoType == "un")
    // via onSuccess_V2 (JSON parsed by getBestLink). A 403 audio link falls back to stream.url.
    // getLink must be invoked on the main thread; the 403 pre-check runs on IO.
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
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchView, InputMethodManager.SHOW_IMPLICIT)
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
                ContextCompat.RECEIVER_NOT_EXPORTED
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
