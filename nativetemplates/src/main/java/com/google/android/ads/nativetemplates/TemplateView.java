// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.ads.nativetemplates;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Base class for a template view. *
 */
public class TemplateView extends LinearLayout {

    private int templateType;
    private NativeTemplateStyle style;
    private NativeAd nativeAd;
    private NativeAdView nativeAdView;

    private TextView primaryView;
    private TextView secondaryView;
    private RatingBar ratingBar;
    private TextView tertiaryView;
    private ShapeableImageView iconView;
    private MediaView mediaView;
    private TextView callToActionView;
    private ConstraintLayout background;

    private static final String MEDIUM_TEMPLATE = "medium_template";
    private static final String SMALL_TEMPLATE = "small_template";

    public TemplateView(Context context) {
        super(context);
    }

    public TemplateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public TemplateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public TemplateView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    public void setStyles(NativeTemplateStyle style) {
        this.style = style;
        this.applyStyles();
    }

    public NativeAdView getNativeAdView() {
        return nativeAdView;
    }

    private void applyStyles() {
        if (style.getPrimaryTextColor() != 0 && primaryView != null) {
            primaryView.setTextColor(style.getPrimaryTextColor());
        }

        if (style.getSecondaryTextColor() != 0 && tertiaryView != null) {
            tertiaryView.setTextColor(style.getSecondaryTextColor());
        }

        if (style.getBackgroundResources() != 0 && background != null) {
            background.setBackgroundResource(style.getBackgroundResources());
        }

        if (style.getCallToActionBackgroundResource() != 0 && callToActionView != null) {
            callToActionView.setBackgroundResource(style.getCallToActionBackgroundResource());
        }

        if (style.getIconBackgroundResource() != 0 && iconView != null) {
            iconView.setBackgroundResource(style.getIconBackgroundResource());
        }

        if (style.getTemplateViewBackgroundResource() != 0) {
            setBackgroundResource(style.getTemplateViewBackgroundResource());
        }

        invalidate();
        requestLayout();
    }

    private boolean adHasOnlyStore(NativeAd nativeAd) {
        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser);
    }

    public void setNativeAd(NativeAd nativeAd) {
        this.nativeAd = nativeAd;

        String store = nativeAd.getStore();
        String advertiser = nativeAd.getAdvertiser();
        String headline = nativeAd.getHeadline();
        String body = nativeAd.getBody() == null ? "" : nativeAd.getBody();
        String cta = nativeAd.getCallToAction();
        Double starRating = nativeAd.getStarRating();
        NativeAd.Image icon = nativeAd.getIcon();

        String secondaryText;

        nativeAdView.setCallToActionView(callToActionView);
        if (primaryView == null) {
            nativeAdView.setHeadlineView(callToActionView);
        } else {
            nativeAdView.setHeadlineView(primaryView);
        }
        nativeAdView.setMediaView(mediaView);

        if (mediaView != null) {
            mediaView.setImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        if (secondaryView != null) {
            secondaryView.setVisibility(VISIBLE);
        }
        if (adHasOnlyStore(nativeAd)) {
            if (secondaryView != null) {
                nativeAdView.setStoreView(secondaryView);
            }
            secondaryText = store;
        } else if (!TextUtils.isEmpty(advertiser)) {
            if (secondaryView != null) {
                nativeAdView.setAdvertiserView(secondaryView);
            }
            secondaryText = advertiser;
        } else {
            secondaryText = "";
        }

        if (primaryView != null) {
            primaryView.setText(headline);
        }
        callToActionView.setText(cta);

        //  Set the secondary view to be the star rating if available.
        if (starRating != null && starRating > 0) {
            if (secondaryView != null) {
                secondaryView.setVisibility(GONE);
            }
            if (ratingBar != null) {
                ratingBar.setVisibility(VISIBLE);
                ratingBar.setRating(starRating.floatValue());
                nativeAdView.setStarRatingView(ratingBar);
            }
        } else {
            if (secondaryView != null) {
                secondaryView.setText(secondaryText);
                secondaryView.setVisibility(VISIBLE);
            }
            if (ratingBar != null) {
                ratingBar.setVisibility(GONE);
            }
        }

        if (iconView != null) {
            if (icon != null) {
                iconView.setVisibility(VISIBLE);
                iconView.setImageDrawable(icon.getDrawable());
            } else {
                iconView.setVisibility(View.INVISIBLE);
            }
        }

        if (tertiaryView != null) {
            tertiaryView.setText(body);
            nativeAdView.setBodyView(tertiaryView);
        }

        nativeAdView.setNativeAd(nativeAd);
    }

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * <a href="https://developers.google.com/admob/android/native-unified#destroy_ad">https://developers.google.com/admob/android/native-unified#destroy_ad</a>
     */
    public void destroyNativeAd() {
        nativeAd.destroy();
    }

    private void initView(Context context, AttributeSet attributeSet) {
//        TypedArray attributes = context.getTheme().obtainStyledAttributes(
//            attributeSet,
//            R.styleable.TemplateView,
//            0,
//            0
//        );
//
//        try {
//            templateType = attributes.getResourceId(
//                R.styleable.TemplateView_gnt_template_type,
//                R.layout.gnt_medium_template_view
//            );
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            attributes.recycle();
//        }
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
//            Context.LAYOUT_INFLATER_SERVICE
//        );
//        inflater.inflate(templateType, this);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        nativeAdView = findViewById(R.id.native_ad_view);
        primaryView = findViewById(R.id.primary);
//        secondaryView = findViewById(R.id.secondary);
        tertiaryView = findViewById(R.id.body);

        ratingBar = findViewById(R.id.rating_bar);
        if (ratingBar != null) {
            ratingBar.setEnabled(false);
        }

        callToActionView = findViewById(R.id.cta);
        iconView = findViewById(R.id.icon);
        mediaView = findViewById(R.id.media_view);
        background = findViewById(R.id.background);
    }
}
