package com.musicdownloader.musicfreeapp825v2.logic.utils.ads

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.musicdownloader.musicfreeapp825v2.R
import com.thinkup.nativead.api.TUNativeImageView
import com.thinkup.nativead.api.TUNativeMaterial
import com.thinkup.nativead.api.TUNativePrepareExInfo
import com.thinkup.nativead.api.TUNativePrepareInfo

object SelfRenderViewUtil {
    fun bindSelfRenderView(
        context: Context,
        adMaterial: TUNativeMaterial,
        selfRenderView: View,
        nativePrepareInfo: TUNativePrepareInfo?,
        closeVisible: Boolean = false
    ) {
        var nativePrepareInfo = nativePrepareInfo
        val padding: Int = dip2px(context, 5f)
        selfRenderView.setPadding(padding, padding, padding, padding)
        val titleView = selfRenderView.findViewById<TextView>(R.id.native_ad_title)
        val descView = selfRenderView.findViewById<TextView>(R.id.native_ad_desc)
        val ctaView = selfRenderView.findViewById<TextView>(R.id.native_ad_install_btn)
        val adFromView = selfRenderView.findViewById<TextView>(R.id.native_ad_from)
        val iconArea = selfRenderView.findViewById<FrameLayout>(R.id.native_ad_image)
        val contentArea =
            selfRenderView.findViewById<FrameLayout>(R.id.native_ad_content_image_area)
        val logoView = selfRenderView.findViewById<TUNativeImageView>(R.id.native_ad_logo)
        val closeView = selfRenderView.findViewById<View>(R.id.native_ad_close)
        val domainView =
            selfRenderView.findViewById<TextView>(R.id.native_ad_domain) //(v6.1.20+) Yandex domain
        val warningView =
            selfRenderView.findViewById<TextView>(R.id.native_ad_warning) //(v6.1.20+) Yandex warning
        val adLogoContainer =
            selfRenderView.findViewById<FrameLayout>(R.id.native_ad_logo_container) //v6.1.52
        closeView.isVisible = closeVisible
        // bind view
        if (nativePrepareInfo == null) {
            nativePrepareInfo = TUNativePrepareInfo()
        }
        val clickViewList: MutableList<View> = ArrayList() //click views
        val title = adMaterial.title
        // title
        if (!TextUtils.isEmpty(title)) {
            titleView.text = title
            nativePrepareInfo.titleView = titleView //bind title
            clickViewList.add(titleView)
            titleView.visibility = View.VISIBLE
        } else {
            titleView.visibility = View.GONE
        }
        val descriptionText = adMaterial.descriptionText
        if (!TextUtils.isEmpty(descriptionText)) {
            // desc
            descView.text = descriptionText
            nativePrepareInfo.descView = descView //bind desc
            clickViewList.add(descView)
            descView.visibility = View.VISIBLE
        } else {
            descView.visibility = View.GONE
        }

        // icon
        val adIconView = adMaterial.adIconView
        val iconImageUrl = adMaterial.iconImageUrl
        iconArea.removeAllViews()
        val iconView = TUNativeImageView(context)
        if (adIconView != null) {
            iconArea.addView(adIconView)
            nativePrepareInfo.iconView = adIconView //bind icon
            clickViewList.add(adIconView)
            iconArea.visibility = View.VISIBLE
        } else if (!TextUtils.isEmpty(iconImageUrl)) {
            iconArea.addView(iconView)
            iconView.setImage(iconImageUrl)
            nativePrepareInfo.iconView = iconView //bind icon
            clickViewList.add(iconView)
            iconArea.visibility = View.VISIBLE
        } else {
            iconArea.visibility = View.INVISIBLE
        }

        // cta button
        val callToActionText = adMaterial.callToActionText
        if (!TextUtils.isEmpty(callToActionText)) {
            ctaView.text = callToActionText
            nativePrepareInfo.ctaView = ctaView //bind cta button
            clickViewList.add(ctaView)
            ctaView.visibility = View.VISIBLE
        } else {
            ctaView.visibility = View.GONE
        }

        // AppDownloadButton(Only Huawei Ads support)
//        val lastView = (selfRenderView as ViewGroup).getChildAt(selfRenderView.childCount - 1)
        // Remove AppDownloadButton since last time added
//        if (lastView is AppDownloadButton) {
//            selfRenderView.removeView(lastView)
//        }
//        val appDownloadButton = adMaterial.appDownloadButton
//        if (appDownloadButton != null) {
//            if (appDownloadButton is AppDownloadButton) {
//                (appDownloadButton as AppDownloadButton).setTextSize(
//                    SelfRenderViewUtil.dip2px(
//                        context,
//                        12f
//                    )
//                )
//            }
//            val ctaParams = ctaView.layoutParams
//            selfRenderView.addView(appDownloadButton, ctaParams)
//            appDownloadButton.visibility = View.VISIBLE
//            ctaView.visibility = View.INVISIBLE
//        }

        // media view
        val mediaView = adMaterial.getAdMediaView(contentArea)
        val mainImageHeight = adMaterial.mainImageHeight
        val mainImageWidth = adMaterial.mainImageWidth
        val realMainImageWidth: Int =
            context.resources.displayMetrics.widthPixels - dip2px(context, 10f)
        var realMainHeight = 0
        val mainImageParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        if (mainImageWidth > 0 && mainImageHeight > 0) {
            realMainHeight = realMainImageWidth * mainImageHeight / mainImageWidth
            mainImageParam.width = realMainImageWidth
            mainImageParam.height = realMainHeight
        } else {
            mainImageParam.width = FrameLayout.LayoutParams.MATCH_PARENT
            mainImageParam.height = realMainImageWidth * 600 / 1024
        }
        contentArea.removeAllViews()
        if (mediaView != null) {
            if (mediaView.parent != null) {
                (mediaView.parent as ViewGroup).removeView(mediaView)
            }
            mainImageParam.gravity = Gravity.CENTER
            mediaView.layoutParams = mainImageParam
            contentArea.addView(mediaView, mainImageParam)
            clickViewList.add(mediaView)
            contentArea.visibility = View.VISIBLE
        } else if (!TextUtils.isEmpty(adMaterial.mainImageUrl)) {
            val imageView = TUNativeImageView(context)
            imageView.setImage(adMaterial.mainImageUrl)
            imageView.layoutParams = mainImageParam
            contentArea.addView(imageView, mainImageParam)
            nativePrepareInfo.mainImageView = imageView //bind main image
            clickViewList.add(imageView)
            contentArea.visibility = View.VISIBLE
        } else {
            contentArea.removeAllViews()
            contentArea.visibility = View.GONE
        }


        //Ad Logo
        val adLogoView = adMaterial.adLogoView
        if (adLogoView != null) {
            adLogoContainer.visibility = View.VISIBLE
            adLogoContainer.removeAllViews()
            adLogoContainer.addView(adLogoView)
        } else {
            adLogoContainer.visibility = View.GONE
            val adChoiceIconUrl = adMaterial.adChoiceIconUrl
            val adLogoBitmap = adMaterial.adLogo
            if (!TextUtils.isEmpty(adChoiceIconUrl)) {
                logoView.setImage(adChoiceIconUrl)
                nativePrepareInfo.adLogoView = logoView //bind ad choice
                logoView.visibility = View.VISIBLE
            } else if (adLogoBitmap != null) {
                logoView.setImageBitmap(adLogoBitmap)
                logoView.visibility = View.VISIBLE
            } else {
                logoView.setImageBitmap(null)
                logoView.visibility = View.GONE
            }
        }
        val adFrom = adMaterial.adFrom

        // ad from
        if (!TextUtils.isEmpty(adFrom)) {
            adFromView.text = adFrom
            adFromView.visibility = View.VISIBLE
        } else {
            adFromView.visibility = View.GONE
        }
        nativePrepareInfo.adFromView = adFromView //bind ad from
        val layoutParams: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            dip2px(context, 40f),
            dip2px(context, 10f)
        ) //ad choice
        layoutParams.gravity = Gravity.BOTTOM or Gravity.RIGHT
        nativePrepareInfo.choiceViewLayoutParams = layoutParams //bind layout params for ad choice
        nativePrepareInfo.closeView = closeView //bind close button
        val domain = adMaterial.domain //(v6.1.20+) Yandex domain
        if (!TextUtils.isEmpty(domain)) {
            domainView.visibility = View.VISIBLE
            domainView.text = domain
            clickViewList.add(domainView)
            nativePrepareInfo.domainView = domainView
        } else {
            domainView.visibility = View.GONE
        }
        val warning = adMaterial.warning //(v6.1.20+) Yandex warning
        if (!TextUtils.isEmpty(warning)) {
            warningView.visibility = View.VISIBLE
            warningView.text = warning
            clickViewList.add(warningView)
            nativePrepareInfo.warningView = warningView
        } else {
            warningView.visibility = View.GONE
        }
        nativePrepareInfo.clickViewList = clickViewList //bind click view list
        if (nativePrepareInfo is TUNativePrepareExInfo) {
            val creativeClickViewList: MutableList<View> = ArrayList() //click views
            creativeClickViewList.add(ctaView)
            nativePrepareInfo.creativeClickViewList = creativeClickViewList //bind custom view list
        }
    }

    //    private static View initializePlayer(Context context, String url) {
    //        VideoView videoView = new VideoView(context);
    //        videoView.setVideoURI(Uri.parse(url));
    //        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
    //            @Override
    //            public void onPrepared(MediaPlayer mediaPlayer) {
    //            }
    //        });
    //        videoView.start();
    //
    //        return videoView;
    //    }
    fun dip2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }
}