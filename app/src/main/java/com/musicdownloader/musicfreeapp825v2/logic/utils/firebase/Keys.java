package com.musicdownloader.musicfreeapp825v2.logic.utils.firebase;

/**
 * Created by Quang Phúc on 15/10/24.
 */
public class Keys {

    // UX
    public static final String USER_CLICK_RATE_APP = "USER_RATE_APP";
    public static final String USER_SHARE_APP = "USER_SHARE_APP";
    public static final String USER_SEND_FEEDBACK = "USER_SEND_FEEDBACK";
    public static final String USER_GOTO_EQUALIZER = "USER_GOTO_EQUALIZER";
    public static final String USER_CONNECT_TO_ANDROID_AUTO = "USER_CONNECT_TO_ANDROID_AUTO";
    public static final String USER_CONNECT_TO_ANDROID_AUTO_2 = "USER_CONNECT_TO_ANDROID_AUTO_2";

    public static final String DOWNLOAD_MUSIC_RESTRICTED = "DOWNLOAD_MUSIC_RESTRICTED";

    public static final String SHOW_DIALOG_TURN_OFF_VPN_OR_PROXY = "SHOW_DIALOG_TURN_OFF_VPN_OR_PROXY";

    // server config
    public static final String GET_CONFIG_SUCCESS_AND_USE_ADMOB = "GET_CONFIG_OK_ADMOB";
    public static final String GET_CONFIG_SUCCESS_AND_USE_TOP_ON = "GET_CONFIG_OK_TOP_ON";
    public static final String GET_CONFIG_FAILED = "GET_CONFIG_FAILED";

    // ---------------- Navigation (append screen/tab name) ----------------
    public static final String USER_NAVIGATE_TO_ = "USER_NAVIGATE_TO_";
    public static final String USER_OPEN_TAB_HOME = "USER_OPEN_TAB_HOME";
    public static final String USER_OPEN_TAB_TRACK = "USER_OPEN_TAB_TRACK";
    public static final String USER_OPEN_TAB_ARTIST = "USER_OPEN_TAB_ARTIST";
    public static final String USER_OPEN_TAB_ALBUM = "USER_OPEN_TAB_ALBUM";
    public static final String USER_OPEN_TAB_PLAYLIST = "USER_OPEN_TAB_PLAYLIST";
    public static final String USER_OPEN_TAB_DOWNLOAD = "USER_OPEN_TAB_DOWNLOAD";
    public static final String USER_OPEN_TAB_SEARCH_ONLINE = "USER_OPEN_TAB_SEARCH_ONLINE";
    public static final String USER_OPEN_TAB_SETTING = "USER_OPEN_TAB_SETTING";

    // ---------------- Search offline (SearchFragment) ----------------
    public static final String USER_SEARCH_OFFLINE = "USER_SEARCH_OFFLINE";

    // ---------------- Search online (SearchOnlineFragment) ----------------
    public static final String USER_SEARCH_ONLINE = "USER_SEARCH_ONLINE";
    public static final String USER_CLICK_SEARCH_ONLINE_SUGGESTION = "USER_CLICK_SEARCH_ONLINE_SUGGESTION";
    public static final String USER_CLICK_SEARCH_ONLINE_HISTORY = "USER_CLICK_SEARCH_ONLINE_HISTORY";
    public static final String SEARCH_ONLINE_SUCCESS = "SEARCH_ONLINE_SUCCESS";
    public static final String SEARCH_ONLINE_ERROR = "SEARCH_ONLINE_ERROR";

    // ---------------- Get link / play online ----------------
    public static final String GET_LINK_SUCCESS = "GET_LINK_SUCCESS";
    // Link audio bị 403 -> phải fallback sang link video 360p (stream.url) để phát/tải.
    public static final String GET_LINK_SUCCESS_VIDEO_360 = "GET_LINK_SUCCESS_VIDEO_360";
    public static final String GET_LINK_FAILED = "GET_LINK_FAILED";
    public static final String USER_PLAY_ONLINE = "USER_PLAY_ONLINE";
    public static final String USER_PLAY_ONLINE_ERROR = "USER_PLAY_ONLINE_ERROR";

    // ---------------- Download ----------------
    public static final String USER_CLICK_DOWNLOAD = "USER_CLICK_DOWNLOAD";
    public static final String DOWNLOAD_START = "DOWNLOAD_START";
    public static final String DOWNLOAD_SUCCESS = "DOWNLOAD_SUCCESS";
    // Tải về là file video 360p (fallback 403) -> đã tách audio ra .m4a.
    public static final String DOWNLOAD_VIDEO_360 = "DOWNLOAD_VIDEO_360";
    public static final String DOWNLOAD_FAILED = "DOWNLOAD_FAILED";
    public static final String USER_PAUSE_DOWNLOAD = "USER_PAUSE_DOWNLOAD";
    public static final String USER_RESUME_DOWNLOAD = "USER_RESUME_DOWNLOAD";
    public static final String USER_CANCEL_DOWNLOAD = "USER_CANCEL_DOWNLOAD";
    public static final String USER_RETRY_DOWNLOAD = "USER_RETRY_DOWNLOAD";

    // ---------------- Player / mini player ----------------
    public static final String USER_PLAY_OFFLINE = "USER_PLAY_OFFLINE";
    public static final String USER_PLAY_OFFLINE_ERROR = "USER_PLAY_OFFLINE_ERROR";
    public static final String USER_CLICK_PLAYER_PLAY = "USER_CLICK_PLAYER_PLAY";
    public static final String USER_CLICK_PLAYER_PAUSE = "USER_CLICK_PLAYER_PAUSE";
    public static final String USER_CLICK_PLAYER_NEXT = "USER_CLICK_PLAYER_NEXT";
    public static final String USER_CLICK_PLAYER_PREVIOUS = "USER_CLICK_PLAYER_PREVIOUS";
    public static final String USER_CLICK_PLAYER_SHUFFLE_ = "USER_CLICK_PLAYER_SHUFFLE_";   // + ON / OFF
    public static final String USER_CLICK_PLAYER_REPEAT_ = "USER_CLICK_PLAYER_REPEAT_";     // + ALL / ONE / OFF
    public static final String USER_CLICK_PLAYER_FAVORITE = "USER_CLICK_PLAYER_FAVORITE";
    public static final String USER_CLICK_PLAYER_QUEUE = "USER_CLICK_PLAYER_QUEUE";
    public static final String USER_CLICK_MINI_PLAYER = "USER_CLICK_MINI_PLAYER";

    // ---------------- Sleep timer ----------------
    public static final String USER_SET_SLEEP_TIMER_ = "USER_SET_SLEEP_TIMER_";             // + minutes
    public static final String USER_CANCEL_SLEEP_TIMER = "USER_CANCEL_SLEEP_TIMER";

    // ---------------- Queue ----------------
    public static final String USER_CLICK_QUEUE_ITEM = "USER_CLICK_QUEUE_ITEM";
    public static final String USER_REMOVE_QUEUE_ITEM = "USER_REMOVE_QUEUE_ITEM";
    public static final String USER_REORDER_QUEUE = "USER_REORDER_QUEUE";
    public static final String USER_CLEAR_QUEUE = "USER_CLEAR_QUEUE";

    // ---------------- Library ----------------
    public static final String USER_OPEN_ALBUM_VIEW = "USER_OPEN_ALBUM_VIEW";
    public static final String USER_OPEN_ARTIST_VIEW = "USER_OPEN_ARTIST_VIEW";
    public static final String USER_OPEN_PLAYLIST_VIEW = "USER_OPEN_PLAYLIST_VIEW";
    public static final String USER_CLICK_PLAY_ALL = "USER_CLICK_PLAY_ALL";
    public static final String USER_CLICK_SHUFFLE_ALL = "USER_CLICK_SHUFFLE_ALL";

    // ---------------- Playlist ----------------
    public static final String USER_CREATE_PLAYLIST = "USER_CREATE_PLAYLIST";
    public static final String USER_RENAME_PLAYLIST = "USER_RENAME_PLAYLIST";
    public static final String USER_DELETE_PLAYLIST = "USER_DELETE_PLAYLIST";
    public static final String USER_ADD_SONG_TO_PLAYLIST = "USER_ADD_SONG_TO_PLAYLIST";
    public static final String USER_REMOVE_SONG_FROM_PLAYLIST = "USER_REMOVE_SONG_FROM_PLAYLIST";
    public static final String USER_ADD_FAVORITE = "USER_ADD_FAVORITE";
    public static final String USER_REMOVE_FAVORITE = "USER_REMOVE_FAVORITE";

    // ---------------- Bottom sheet more (song) ----------------
    public static final String USER_CLICK_SONG_BOTTOM_SHEET_PLAY_NEXT = "USER_CLICK_SONG_BOTTOM_SHEET_PLAY_NEXT";
    public static final String USER_CLICK_SONG_BOTTOM_SHEET_ADD_TO_QUEUE = "USER_CLICK_SONG_BOTTOM_SHEET_ADD_TO_QUEUE";
    public static final String USER_CLICK_SONG_BOTTOM_SHEET_ADD_TO_PLAYLIST = "USER_CLICK_SONG_BOTTOM_SHEET_ADD_TO_PLAYLIST";
    public static final String USER_CLICK_SONG_BOTTOM_SHEET_SHARE = "USER_CLICK_SONG_BOTTOM_SHEET_SHARE";
    public static final String USER_CLICK_SONG_BOTTOM_SHEET_INFO = "USER_CLICK_SONG_BOTTOM_SHEET_INFO";
    public static final String USER_CLICK_SONG_BOTTOM_SHEET_DELETE = "USER_CLICK_SONG_BOTTOM_SHEET_DELETE";

    // ---------------- Setting ----------------
    public static final String USER_CLICK_SETTING_THEME = "USER_CLICK_SETTING_THEME";
    public static final String USER_CLICK_SETTING_LANGUAGE = "USER_CLICK_SETTING_LANGUAGE";
    public static final String USER_CHANGE_THEME_ = "USER_CHANGE_THEME_";                   // + theme name
    public static final String USER_CHANGE_LANGUAGE_ = "USER_CHANGE_LANGUAGE_";             // + language code
}
