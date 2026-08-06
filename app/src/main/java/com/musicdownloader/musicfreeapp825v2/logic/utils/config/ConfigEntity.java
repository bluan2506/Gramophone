package com.musicdownloader.musicfreeapp825v2.logic.utils.config;

/**
 * Created by Quang Phúc on 15/10/24.
 */
public class ConfigEntity {

    private String more_app = "";
    private String more_url = "";
    private String more_title = "";
    private String more_icon = "";
    private String more_text = "";
    private String more_ok = "";
    private String more_cancel = "";
    private String request_update = "";
    private String request_update_url = "";
    private String request_update_force_exit_app = "";
    private String request_update_text = "";
    private String request_update_text_update = "";
    private String request_update_text_close = "";
    private String request_update_title = "";
    private boolean allow_open_file_with_other_app = false;

    private boolean onNotiVPN = false;
    private boolean onNotiVM = false;
    private boolean AutoGoHomeAfter30s = true;
    private int AutoGoHomeAfter30s_value = 35; // seconds

    private boolean ads_welcome = true;
    private boolean ads_download = true;
    private boolean ads_gotoSearchScreen = true;
    private boolean ads_gotoSettingScreen = true;
    private boolean ads_nativeSearchScreen = true;
    private boolean ads_nativePlayerScreen = true;
    private boolean ads_nativeExitApp = true;
    private boolean ads_bannerHome = true;
    private boolean ads_bannerSetting = true;

    private boolean toponads = false;

    private long timeShowAds = 45;

    /**
     * 0: Không hiện In App Update
     * 1: Hiện In App Update IMMEDIATE
     * 2: Hiện In App Update FLEXIBLE
     */
    private int showInAppUpdate = 0;

    public String getMore_app() {
        return more_app;
    }

    public String getMore_url() {
        return more_url;
    }

    public String getMore_title() {
        return more_title;
    }

    public String getMore_icon() {
        return more_icon;
    }

    public String getMore_text() {
        return more_text;
    }

    public String getMore_ok() {
        return more_ok;
    }

    public String getMore_cancel() {
        return more_cancel;
    }

    public String getRequest_update() {
        return request_update;
    }

    public String getRequest_update_url() {
        return request_update_url;
    }

    public String getRequest_update_force_exit_app() {
        return request_update_force_exit_app;
    }

    public String getRequest_update_text() {
        return request_update_text;
    }

    public String getRequest_update_text_update() {
        return request_update_text_update;
    }

    public String getRequest_update_text_close() {
        return request_update_text_close;
    }

    public String getRequest_update_title() {
        return request_update_title;
    }

    public boolean isAllow_open_file_with_other_app() {
        return allow_open_file_with_other_app;
    }

    public long getTimeShowAds() {
        return timeShowAds;
    }

    public void setTimeShowAds(long timeShowAds) {
        this.timeShowAds = timeShowAds;
    }

    public void setMore_app(String more_app) {
        this.more_app = more_app;
    }

    public void setMore_url(String more_url) {
        this.more_url = more_url;
    }

    public void setMore_title(String more_title) {
        this.more_title = more_title;
    }

    public void setMore_icon(String more_icon) {
        this.more_icon = more_icon;
    }

    public void setMore_text(String more_text) {
        this.more_text = more_text;
    }

    public void setMore_ok(String more_ok) {
        this.more_ok = more_ok;
    }

    public void setMore_cancel(String more_cancel) {
        this.more_cancel = more_cancel;
    }

    public void setRequest_update(String request_update) {
        this.request_update = request_update;
    }

    public void setRequest_update_url(String request_update_url) {
        this.request_update_url = request_update_url;
    }

    public void setRequest_update_force_exit_app(String request_update_force_exit_app) {
        this.request_update_force_exit_app = request_update_force_exit_app;
    }

    public void setRequest_update_text(String request_update_text) {
        this.request_update_text = request_update_text;
    }

    public void setRequest_update_text_update(String request_update_text_update) {
        this.request_update_text_update = request_update_text_update;
    }

    public void setRequest_update_text_close(String request_update_text_close) {
        this.request_update_text_close = request_update_text_close;
    }

    public void setRequest_update_title(String request_update_title) {
        this.request_update_title = request_update_title;
    }

    public void setAllow_open_file_with_other_app(boolean allow_open_file_with_other_app) {
        this.allow_open_file_with_other_app = allow_open_file_with_other_app;
    }

    public boolean isOnNotiVPN() {
        return onNotiVPN;
    }

    public void setOnNotiVPN(boolean onNotiVPN) {
        this.onNotiVPN = onNotiVPN;
    }

    public boolean isOnNotiVM() {
        return onNotiVM;
    }

    public void setOnNotiVM(boolean onNotiVM) {
        this.onNotiVM = onNotiVM;
    }

    public boolean isAutoGoHomeAfter30s() {
        return AutoGoHomeAfter30s;
    }

    public void setAutoGoHomeAfter30s(boolean autoGoHomeAfter30s) {
        AutoGoHomeAfter30s = autoGoHomeAfter30s;
    }

    public int getAutoGoHomeAfter30s_value() {
        return AutoGoHomeAfter30s_value;
    }

    public void setAutoGoHomeAfter30s_value(int autoGoHomeAfter30s_value) {
        AutoGoHomeAfter30s_value = autoGoHomeAfter30s_value;
    }

    public boolean isAds_welcome() {
        return ads_welcome;
    }

    public void setAds_welcome(boolean ads_welcome) {
        this.ads_welcome = ads_welcome;
    }

    public boolean isAds_download() {
        return ads_download;
    }

    public void setAds_download(boolean ads_download) {
        this.ads_download = ads_download;
    }

    public boolean isAds_gotoSearchScreen() {
        return ads_gotoSearchScreen;
    }

    public void setAds_gotoSearchScreen(boolean ads_gotoSearchScreen) {
        this.ads_gotoSearchScreen = ads_gotoSearchScreen;
    }

    public boolean isAds_gotoSettingScreen() {
        return ads_gotoSettingScreen;
    }

    public void setAds_gotoSettingScreen(boolean ads_gotoSettingScreen) {
        this.ads_gotoSettingScreen = ads_gotoSettingScreen;
    }

    public boolean isAds_nativeSearchScreen() {
        return ads_nativeSearchScreen;
    }

    public void setAds_nativeSearchScreen(boolean ads_nativeSearchScreen) {
        this.ads_nativeSearchScreen = ads_nativeSearchScreen;
    }

    public boolean isAds_nativePlayerScreen() {
        return ads_nativePlayerScreen;
    }

    public void setAds_nativePlayerScreen(boolean ads_nativePlayerScreen) {
        this.ads_nativePlayerScreen = ads_nativePlayerScreen;
    }

    public boolean isAds_nativeExitApp() {
        return ads_nativeExitApp;
    }

    public void setAds_nativeExitApp(boolean ads_nativeExitApp) {
        this.ads_nativeExitApp = ads_nativeExitApp;
    }

    public boolean isAds_bannerHome() {
        return ads_bannerHome;
    }

    public void setAds_bannerHome(boolean ads_bannerHome) {
        this.ads_bannerHome = ads_bannerHome;
    }

    public boolean isAds_bannerSetting() {
        return ads_bannerSetting;
    }

    public void setAds_bannerSetting(boolean ads_bannerSetting) {
        this.ads_bannerSetting = ads_bannerSetting;
    }

    public boolean isToponads() {
        return toponads;
    }

    public void setToponads(boolean toponads) {
        this.toponads = toponads;
    }

    public int getShowInAppUpdate() {
        return showInAppUpdate;
    }

    public void setShowInAppUpdate(int showInAppUpdate) {
        this.showInAppUpdate = showInAppUpdate;
    }
}
