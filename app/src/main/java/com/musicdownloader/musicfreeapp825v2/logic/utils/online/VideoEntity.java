package com.musicdownloader.musicfreeapp825v2.logic.utils.online;

/**
 * Created by Quang Phúc on 12/8/26
 */
public class VideoEntity {
    public static final String MsType = "un";
    private boolean redirect;
    private int duration;
    private String durationString;
    private String licence;
    private boolean downloadable;
    private String image_link;
    private String stream_link;
    private String artist_name;
    private String videoType;
    private String videoTile;
    private String videoId;
    private int taskId;
    private String filePath;
    private long totalLength;
    private boolean isPlayed = false;
    public boolean allow_download = true;
    public String not_allow_download_reason = "";
    private String ccmixter_referrer;

    public VideoEntity() {
    }

    public boolean isPlayed() {
        return this.isPlayed;
    }

    public String getDurationString() {
        return this.durationString;
    }

    public void setDurationString(String var1) {
        this.durationString = var1;
    }

    public void setPlayed(boolean var1) {
        this.isPlayed = var1;
    }

    public long getTotalLength() {
        return this.totalLength;
    }

    public void setTotalLength(long var1) {
        this.totalLength = var1;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String var1) {
        this.filePath = var1;
    }

    public int getTaskId() {
        return this.taskId;
    }

    public void setTaskId(int var1) {
        this.taskId = var1;
    }

    public boolean isRedirect() {
        return this.redirect;
    }

    public void setRedirect(boolean var1) {
        this.redirect = var1;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int var1) {
        this.duration = var1;
    }

    public String getLicence() {
        return this.licence;
    }

    public void setLicence(String var1) {
        this.licence = var1;
    }

    public boolean isDownloadable() {
        return this.downloadable;
    }

    public void setDownloadable(boolean var1) {
        this.downloadable = var1;
    }

    public String getImage_link() {
        return this.image_link;
    }

    public void setImage_link(String var1) {
        this.image_link = var1;
    }

    public String getStream_link() {
        return this.stream_link;
    }

    public void setStream_link(String var1) {
        this.stream_link = var1;
    }

    public String getArtist_name() {
        return this.artist_name;
    }

    public void setArtist_name(String var1) {
        this.artist_name = var1;
    }

    public String getVideoType() {
        return this.videoType;
    }

    public void setVideoType(String var1) {
        this.videoType = var1;
    }

    public String getVideoTile() {
        return this.videoTile;
    }

    public void setVideoTile(String var1) {
        this.videoTile = var1;
    }

    public String getVideoId() {
        return this.videoId;
    }

    public void setVideoId(String var1) {
        this.videoId = var1;
    }

    public String getCcmixterReferrer() {
        return this.ccmixter_referrer;
    }

    public void setCcmixterReferrer(String var1) {
        this.ccmixter_referrer = var1;
    }
}
