package com.musicdownloader.musicfreeapp825v2.ui.adapters

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import com.google.android.ads.nativetemplates.NativeUtils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.music.searchapi.`object`.VideoEntity
import com.thinkup.core.api.TUAdConst
import com.thinkup.core.api.TUAdInfo
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativeDislikeListener
import com.thinkup.nativead.api.TUNativeEventListener
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.TUNativePrepareInfo
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.ItemNativeAdsSearchBinding
import com.musicdownloader.musicfreeapp825v2.databinding.ItemNativeSeftTopOnBinding
import com.musicdownloader.musicfreeapp825v2.databinding.ItemOnlineSongBinding
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.KeyAdMob
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.KeyTopOn
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.SelfRenderViewUtil
import com.musicdownloader.musicfreeapp825v2.ui.components.NowPlayingDrawable

/**
 * List adapter for online search results ([VideoEntity]), ported from the MSDownloader
 * `MusicOnlineAdapter` — including the interleaved native ad (AdMob or TopOn).
 *
 * The backing list is `List<Any>`: [VideoEntity] rows plus, optionally, one native-ad placeholder
 * ([NativeAdView] for AdMob or [TUNativeAdView] for TopOn) inserted by the fragment at index 2.
 *
 * Tapping a row calls [onClickItem] (stream); tapping the download icon calls [onClickDownload].
 * The currently playing row shows the same animated equalizer ([NowPlayingDrawable]) as the offline
 * song lists, driven by the media3 controller.
 */
class OnlineSongAdapter(
    private val activity: Activity,
    private val onClickItem: (VideoEntity) -> Unit,
    private val onClickDownload: (VideoEntity) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "OnlineSongAdapter"
        private const val TYPE_MUSIC = 1
        private const val TYPE_NATIVE_ADS = 2
        private const val TYPE_NATIVE_TOP_ON = 3
    }

    private val items = mutableListOf<Any>()
    private var currentMediaId: String? = null
    private var currentIsPlaying: Boolean? = null

    fun setData(list: List<Any>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /** [mediaId] is the media3 mediaId of the current item ("online:<videoId>"), or null. */
    fun updateCurrentPlaying(mediaId: String?, isPlaying: Boolean?) {
        val old = currentMediaId
        currentMediaId = mediaId
        currentIsPlaying = isPlaying
        items.forEachIndexed { index, item ->
            if (item !is VideoEntity) return@forEachIndexed
            val key = "online:${item.videoId}"
            if (key == old || key == mediaId) notifyItemChanged(index)
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        if (item is NativeAdView) return TYPE_NATIVE_ADS
        if (item is TUNativeAdView) return TYPE_NATIVE_TOP_ON
        return TYPE_MUSIC
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_NATIVE_TOP_ON ->
                NativeAdsTopOnViewHolder(ItemNativeSeftTopOnBinding.inflate(inflater, parent, false))

            TYPE_NATIVE_ADS ->
                NativeAdsViewHolder(ItemNativeAdsSearchBinding.inflate(inflater, parent, false))

            else ->
                MusicViewHolder(ItemOnlineSongBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MusicViewHolder -> holder.bind(items[position] as VideoEntity)
            is NativeAdsViewHolder -> holder.setAdsNative()
            is NativeAdsTopOnViewHolder -> holder.setAdsNative()
        }
    }

    private fun buildSubtitle(item: VideoEntity): String {
        val artist = item.artist_name?.takeIf { it.isNotBlank() }
        val duration = formatDuration(item.duration)
        return when {
            artist != null && duration != null -> "$artist · $duration"
            artist != null -> artist
            duration != null -> duration
            else -> ""
        }
    }

    private fun formatDuration(seconds: Int): String? {
        if (seconds <= 0) return null
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun isDarkTheme(): Boolean =
        (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    inner class MusicViewHolder(val binding: ItemOnlineSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoEntity) {
            binding.textName.text = item.videoTile
            binding.textArtist.text = buildSubtitle(item)
            binding.imageMusic.load(item.image_link) {
                crossfade(true)
                error(R.drawable.ic_default_cover)
            }

            val isCurrent = item.videoId != null && currentMediaId == "online:${item.videoId}"
            if (isCurrent) {
                binding.nowPlaying.setImageDrawable(
                    NowPlayingDrawable(binding.root.context).also {
                        it.setTint(Color.WHITE)
                        it.level = if (currentIsPlaying == true) 1 else 0
                    }
                )
                binding.nowPlaying.visibility = View.VISIBLE
            } else {
                binding.nowPlaying.setImageDrawable(null)
                binding.nowPlaying.visibility = View.GONE
            }

            binding.imageDownload.visibility =
                if (item.allow_download) View.VISIBLE else View.GONE
            binding.llItem.setOnClickListener { onClickItem(item) }
            binding.imageDownload.setOnClickListener { onClickDownload(item) }
        }
    }

    /** AdMob native ad rendered via the nativetemplates [NativeUtils.getNativeAd]. */
    inner class NativeAdsViewHolder(private val binding: ItemNativeAdsSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var loaded = false

        fun setAdsNative() {
            if (loaded) return
            loaded = true
            val context = binding.root.context
            val adLoader = AdLoader.Builder(context, KeyAdMob.NATIVE_SEARCH_SCREEN)
                .forNativeAd { ad: NativeAd ->
                    val templateView = NativeUtils.getNativeAd(activity, ad, isDarkTheme())
                    binding.llRoot.removeAllViews()
                    binding.llRoot.addView(templateView)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "native search onAdFailedToLoad: $adError")
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /** TopOn native ad, self-rendered via [SelfRenderViewUtil] (ported from the sample). */
    inner class NativeAdsTopOnViewHolder(private val binding: ItemNativeSeftTopOnBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var loaded = false

        fun setAdsNative() {
            if (loaded) return
            loaded = true
            val context = binding.root.context
            var adViewWidth = 0
            var atNatives: TUNative? = null
            atNatives = TUNative(
                context, KeyTopOn.NATIVE_SEARCH_SCREEN,
                object : TUNativeNetworkListener {
                    override fun onNativeAdLoaded() {
                        val anyThinkNativeAdView = TUNativeAdView(context)
                        val nativeAd = atNatives?.nativeAd
                        anyThinkNativeAdView.removeAllViews()
                        if (anyThinkNativeAdView.parent == null) {
                            binding.root.removeAllViews()
                            binding.root.addView(
                                anyThinkNativeAdView,
                                FrameLayout.LayoutParams(
                                    adViewWidth,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    Gravity.CENTER
                                )
                            )
                        }
                        nativeAd?.setNativeEventListener(object : TUNativeEventListener {
                            override fun onAdImpressed(v: TUNativeAdView?, i: TUAdInfo?) {}
                            override fun onAdClicked(v: TUNativeAdView?, i: TUAdInfo?) {}
                            override fun onAdVideoStart(v: TUNativeAdView?) {}
                            override fun onAdVideoEnd(v: TUNativeAdView?) {}
                            override fun onAdVideoProgress(v: TUNativeAdView?, i: Int) {}
                        })
                        nativeAd?.setDislikeCallbackListener(object : TUNativeDislikeListener() {
                            override fun onAdCloseButtonClick(v: TUNativeAdView?, i: TUAdInfo?) {
                                if (v?.parent != null) (v.parent as ViewGroup).removeView(v)
                            }
                        })
                        var nativePrepareInfo: TUNativePrepareInfo? = null
                        if (nativeAd?.isNativeExpress == true) {
                            nativeAd.renderAdContainer(anyThinkNativeAdView, null)
                        } else {
                            nativePrepareInfo = TUNativePrepareInfo()
                            val selfRenderView = LayoutInflater.from(context)
                                .inflate(R.layout.layout_native_self, null, false)
                            if (nativeAd?.adMaterial != null) {
                                SelfRenderViewUtil.bindSelfRenderView(
                                    context, nativeAd.adMaterial, selfRenderView,
                                    nativePrepareInfo, false
                                )
                            }
                            nativeAd?.renderAdContainer(anyThinkNativeAdView, selfRenderView)
                        }
                        nativeAd?.prepare(anyThinkNativeAdView, nativePrepareInfo)
                    }

                    override fun onNativeAdLoadFail(adError: com.thinkup.core.api.AdError?) {
                        Log.e(TAG, "native search onNativeAdLoadFail: $adError")
                    }
                }
            )
            val padding = SelfRenderViewUtil.dip2px(context, 10f)
            adViewWidth = context.resources.displayMetrics.widthPixels - 2 * padding
            val adViewHeight = SelfRenderViewUtil.dip2px(context, 340f) - 2 * padding
            val localMap: MutableMap<String, Any> = HashMap()
            localMap[TUAdConst.KEY.AD_WIDTH] = adViewWidth
            localMap[TUAdConst.KEY.AD_HEIGHT] = adViewHeight
            atNatives.setLocalExtra(localMap)
            atNatives.makeAdRequest()
        }
    }
}
