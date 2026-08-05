package com.google.android.ads.nativetemplates;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.ads.nativead.NativeAd;

/**
 * Created by Quang Phúc on 22/7/24.
 */
public class NativeUtils {

    private static final String TAG = "MyAppNativeUtils";

    public static TemplateView getNativeAd(Activity activity, NativeAd nativeAd, boolean isDark) {
        TemplateView templateView;
        if (nativeAd.getMediaContent() == null) {
            templateView = (TemplateView) activity.getLayoutInflater().inflate(
                R.layout.gnt_square_template_view,
                null
            );
            if (isDark) {
                NativeTemplateStyle styles = new NativeTemplateStyle.Builder()
                    .setCallToActionBackgroundResource(R.drawable.gnt_primary_dark)
                    .setTemplateViewBackgroundResource(R.drawable.gnt_border_4_dark)
                    .build();
                templateView.setStyles(styles);
            }
        } else {
            float aspectRatio = nativeAd.getMediaContent().getAspectRatio();
            Log.i(TAG, "aspectRatio: " + aspectRatio);
            if (aspectRatio > 1.1f) {
                if (nativeAd.getMediaContent().hasVideoContent()) {
                    templateView = (TemplateView) activity.getLayoutInflater().inflate(
                        R.layout.gnt_horizontal_video_template_view,
                        null
                    );
                    if (isDark) {
                        NativeTemplateStyle styles = new NativeTemplateStyle.Builder()
                            .setBackgroundResource(R.color.gnt_bg_native_dark)
                            .setCallToActionBackgroundResource(R.drawable.gnt_primary_dark)
                            .setTemplateViewBackgroundResource(R.drawable.gnt_border_4_dark)
                            .build();
                        templateView.setStyles(styles);
                    }
                } else {
                    templateView = (TemplateView) activity.getLayoutInflater().inflate(
                        R.layout.gnt_horizontal_image_template_view,
                        null
                    );
                    if (isDark) {
                        NativeTemplateStyle styles = new NativeTemplateStyle.Builder()
                            .setPrimaryTextColor(activity.getResources().getColor(
                                R.color.gnt_text_primary_dark,
                                null
                            ))
                            .setSecondaryTextColor(activity.getResources().getColor(
                                R.color.gnt_text_secondary_dark,
                                null
                            ))
                            .setCallToActionBackgroundResource(R.drawable.gnt_primary_dark)
                            .setIconBackgroundResource(R.color.gnt_bg_native_dark)
                            .setTemplateViewBackgroundResource(R.drawable.gnt_border_4_dark)
                            .build();
                        templateView.setStyles(styles);
                    }
                }
            } else if (aspectRatio >= 0.9f) {
                templateView = (TemplateView) activity.getLayoutInflater().inflate(
                    R.layout.gnt_square_template_view,
                    null
                );
                if (isDark) {
                    NativeTemplateStyle styles = new NativeTemplateStyle.Builder()
                        .setCallToActionBackgroundResource(R.drawable.gnt_primary_dark)
                        .setTemplateViewBackgroundResource(R.drawable.gnt_border_4_dark)
                        .build();
                    templateView.setStyles(styles);
                }
            } else {
                templateView = (TemplateView) activity.getLayoutInflater().inflate(
                    R.layout.gnt_vertical_template_view,
                    null
                );
                if (isDark) {
                    NativeTemplateStyle styles = new NativeTemplateStyle.Builder()
                        .setCallToActionBackgroundResource(R.drawable.gnt_primary_dark)
                        .setTemplateViewBackgroundResource(R.drawable.gnt_border_4_dark)
                        .build();
                    templateView.setStyles(styles);
                }
            }
        }

        templateView.setNativeAd(nativeAd);
        return templateView;
    }
}
