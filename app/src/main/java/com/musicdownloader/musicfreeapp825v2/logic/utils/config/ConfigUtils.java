package com.musicdownloader.musicfreeapp825v2.logic.utils.config;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Created by Quang Phúc on 15/10/24.
 */
public class ConfigUtils {

    private static final String TAG = "MyAppConfigUtils";

    private static ConfigEntity configApp;

    @NonNull
    public static ConfigEntity configApp(Context context) {
//        try {
//            JSONObject jsonObject = Config_V1.getServerConfig(context);
//            if (jsonObject == null || jsonObject.length() == 0) {
//                return new ConfigEntity();
//            }
//            configApp = new ConfigEntity();
//            if (jsonObject.has(Sercurity.decrypt(more_app))) {
//                configApp.setMore_app(jsonObject.getString(Sercurity.decrypt(more_app)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(more_url))) {
//                configApp.setMore_url(jsonObject.getString(Sercurity.decrypt(more_url)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(more_title))) {
//                configApp.setMore_title(jsonObject.getString(Sercurity.decrypt(more_title)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(more_icon))) {
//                configApp.setMore_icon(jsonObject.getString(Sercurity.decrypt(more_icon)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(more_text))) {
//                configApp.setMore_text(jsonObject.getString(Sercurity.decrypt(more_text)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(more_ok))) {
//                configApp.setMore_ok(jsonObject.getString(Sercurity.decrypt(more_ok)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(more_cancel))) {
//                configApp.setMore_cancel(jsonObject.getString(Sercurity.decrypt(more_cancel)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update))) {
//                configApp.setRequest_update(jsonObject.getString(Sercurity.decrypt(request_update)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update_url))) {
//                configApp.setRequest_update_url(jsonObject.getString(Sercurity.decrypt(request_update_url)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update_force_exit_app))) {
//                configApp.setRequest_update_force_exit_app(jsonObject.getString(Sercurity.decrypt(request_update_force_exit_app)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update_text))) {
//                configApp.setRequest_update_text(jsonObject.getString(Sercurity.decrypt(request_update_text)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update_text_update))) {
//                configApp.setRequest_update_text_update(jsonObject.getString(Sercurity.decrypt(request_update_text_update)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update_text_close))) {
//                configApp.setRequest_update_text_close(jsonObject.getString(Sercurity.decrypt(request_update_text_close)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(request_update_title))) {
//                configApp.setRequest_update_title(jsonObject.getString(Sercurity.decrypt(request_update_title)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(onNotiVPN))) {
//                configApp.setOnNotiVPN(jsonObject.getBoolean(Sercurity.decrypt(onNotiVPN)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(onNotiVM))) {
//                configApp.setOnNotiVM(jsonObject.getBoolean(Sercurity.decrypt(onNotiVM)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(AutoGoHomeAfter30s))) {
//                configApp.setAutoGoHomeAfter30s(jsonObject.getBoolean(Sercurity.decrypt(AutoGoHomeAfter30s)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(AutoGoHomeAfter30s_value))) {
//                configApp.setAutoGoHomeAfter30s_value(jsonObject.getInt(Sercurity.decrypt(AutoGoHomeAfter30s_value)));
//            }
//
//            if (jsonObject.has(Sercurity.decrypt(ads_welcome))) {
//                configApp.setAds_welcome(jsonObject.getBoolean(Sercurity.decrypt(ads_welcome)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_download))) {
//                configApp.setAds_download(jsonObject.getBoolean(Sercurity.decrypt(ads_download)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_gotoSearchScreen))) {
//                configApp.setAds_gotoSearchScreen(jsonObject.getBoolean(Sercurity.decrypt(ads_gotoSearchScreen)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_gotoSettingScreen))) {
//                configApp.setAds_gotoSettingScreen(jsonObject.getBoolean(Sercurity.decrypt(ads_gotoSettingScreen)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_nativeSearchScreen))) {
//                configApp.setAds_nativeSearchScreen(jsonObject.getBoolean(Sercurity.decrypt(ads_nativeSearchScreen)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_nativePlayerScreen))) {
//                configApp.setAds_nativePlayerScreen(jsonObject.getBoolean(Sercurity.decrypt(ads_nativePlayerScreen)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_nativeExitApp))) {
//                configApp.setAds_nativeExitApp(jsonObject.getBoolean(Sercurity.decrypt(ads_nativeExitApp)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_bannerHome))) {
//                configApp.setAds_bannerHome(jsonObject.getBoolean(Sercurity.decrypt(ads_bannerHome)));
//            }
//            if (jsonObject.has(Sercurity.decrypt(ads_bannerSetting))) {
//                configApp.setAds_bannerSetting(jsonObject.getBoolean(Sercurity.decrypt(ads_bannerSetting)));
//            }
//
//            if (jsonObject.has(Sercurity.decrypt(timeIntervalShowAdsFull))) {
//                configApp.setTimeShowAds(jsonObject.getLong(Sercurity.decrypt(timeIntervalShowAdsFull)));
//            }
//
//            if (jsonObject.has(Sercurity.decrypt(showInAppUpdate))) {
//                configApp.setShowInAppUpdate(jsonObject.getInt(Sercurity.decrypt(showInAppUpdate)));
//            }
//
//            if (jsonObject.has(Sercurity.decrypt(toponads))) {
//                configApp.setToponads(jsonObject.getBoolean(Sercurity.decrypt(toponads)));
//            }
//
//            if (configApp.isToponads()) {
//                Log.d(TAG, "TopOn");
//            } else {
//                Log.d(TAG, "AdMob");
//            }
//
//            return configApp;
//        } catch (Exception e) {
//            FirebaseEventUtils.getInstances().recordException(e);
//        }
        return new ConfigEntity();
    }

//    /**
//     *
//     */
//    public static final String SERVER_URL = "";

    /**
     *
     */
    public static final String SERVER_URL_CHECK_INTERNET = "";

//    /**
//     *
//     */
//    public static final String SERVER_URL_FILE_BLACKLIST = "";
//
//    /**
//     *
//     */
//    public static final String SERVER_URL_CODE_LUA = "";
//
//    /**
//     *
//     */
//    public static final String SERVER_URL_LOG_ERROR =  "";

    /**
     *
     */
    public static final String SERVER_URL_ANALYTICS = "";

//    /**
//     *
//     */
//    public static final String LOG_ADS_REVENUE_URL = "";

    //    private static final String more_app = "";
    private static final String more_app = "6JF456vDhm+Z0iUvUNQ1sA==";
    //    private static final String more_url = "more_url";
    private static final String more_url = "in9cmhpd4ciZW6r50e3IqA==";
    //    private static final String more_title = "more_title";
    private static final String more_title = "JuTZPpbuWHR48440a+NquA==";
    //    private static final String more_icon = "more_icon";
    private static final String more_icon = "B/943k/cnpBE50xKcuIg8w==";
    //    private static final String more_text = "more_text";
    private static final String more_text = "jXEp82ou+3q+pyQvwt44zQ==";
    //    private static final String more_ok = "more_ok";
    private static final String more_ok = "aceYwNUF7Ig=";
    //    private static final String more_cancel = "more_cancel";
    private static final String more_cancel = "H/XfW98abQCWernsBXnUAA==";
    //    private static final String request_update = "request_update";
    private static final String request_update = "qI3hmODWEVfzN5vEPls1iQ==";
    //    private static final String request_update_url = "request_update_url";
    private static final String request_update_url = "qI3hmODWEVe7nUxFGl058zeR4UNsC6pr";
    //    private static final String request_update_force_exit_app = "request_update_force_exit_app";
    private static final String request_update_force_exit_app = "qI3hmODWEVfPEfAMvt8aTQTo/Px3YUG9o8958lq3VPk=";
    //    private static final String request_update_text = "request_update_text";
    private static final String request_update_text = "qI3hmODWEVePnjGAtpigd2FH9ZGy2K1H";
    //    private static final String request_update_text_update = "request_update_text_update";
    private static final String request_update_text_update = "qI3hmODWEVePnjGAtpigd+jITyd1J2Vk5UWO8mnzvU8=";
    //    private static final String request_update_text_close = "request_update_text_close";
    private static final String request_update_text_close = "qI3hmODWEVePnjGAtpigd6DFWn59ee7kkJGOhKbmeHM=";
    //    private static final String request_update_title = "request_update_title";
    private static final String request_update_title = "qI3hmODWEVePnjGAtpigd36AA2m7vQk8";
    //	private static final String onNotiVPN = "onNotiVPN";
    private static final String onNotiVPN = "iSainSDAoIWp/2KIiadR0w==";
    //	private static final String onNotiVM = "onNotiVM";
    private static final String onNotiVM = "SSvZB3NuFrvonyYh6eRvCA==";
    //	private static final String AutoGoHomeAfter30s = "AutoGoHomeAfter30s";
    private static final String AutoGoHomeAfter30s = "Yn608YRBMOUpU+sHe7XI9EckYOgCzVCk";
    //	private static final String AutoGoHomeAfter30s_value = "AutoGoHomeAfter30s_value";
    private static final String AutoGoHomeAfter30s_value = "Yn608YRBMOUpU+sHe7XI9NJI66WOv26PwNyji6D9O+M=";

    /**
     * ads_welcome
     */
    private static final String ads_welcome = "W2c3/tGuOgK+51T9U62iZg==";

    /**
     * ads_download
     */
    private static final String ads_download = "BfGnow0hyl1lVvEIaVW4Eg==";

    /**
     * ads_gotoSearchScreen
     */
    private static final String ads_gotoSearchScreen = "GYljOaFX6P6b10SMGjpQBfmAvfBZH70H";

    /**
     * ads_gotoSettingScreen
     */
    private static final String ads_gotoSettingScreen = "GYljOaFX6P5mxhOASq+pcbvNWgoDLLg4";

    /**
     * ads_nativeSearchScreen
     */
    private static final String ads_nativeSearchScreen = "hemyOp5EEqHOPoXDXONl0GUIKSJhbJFn";

    /**
     * ads_nativePlayerScreen
     */
    private static final String ads_nativePlayerScreen = "hemyOp5EEqHRHBjbSCFJ+nbQyg+9y1Ce";

    /**
     * ads_nativeExitApp
     */
    private static final String ads_nativeExitApp = "hemyOp5EEqExabwDQnMiMG1pguYyTE19";

    /**
     * ads_nativeListLocalSongs
     */
    private static final String ads_nativeListLocalSongs = "hemyOp5EEqGAVuAh/E+pI4E4h8kcUe9NI5HfirDJNVI=";

    /**
     * ads_nativeListDownloadedSongs
     */
    private static final String ads_nativeListDownloadedSongs = "hemyOp5EEqFuJ7mB0qkxq9bYKbybbG4CdW5kNCqGGoA=";

    /**
     * ads_bannerHome
     */
    private static final String ads_bannerHome = "XDp/oV8MWsQdRh5H2dxtHw==";

    /**
     * ads_bannerSetting
     */
    private static final String ads_bannerSetting = "XDp/oV8MWsSE88o8ZXl7ymjE902UV2M7";

    /**
     * toponads
     */
    private static final String toponads = "7dLRh7mD06CFYZ0mStkf5A==";

    /**
     * TimeIntervalShowAdsFull
     */
    private static final String timeIntervalShowAdsFull = "ajhvenQi2pxql52QhofMFwQwoE59xkfH";

    /**
     * showInappUpdate
     */
    private static final String showInAppUpdate = "EzFRftdBc8quK7b0kH7ieA==";
}
