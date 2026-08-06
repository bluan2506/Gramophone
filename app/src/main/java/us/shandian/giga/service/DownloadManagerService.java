package us.shandian.giga.service;


import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.applogevent.logeventlib.LogEventLibs;
import com.music.searchapi.ApiServices;

import com.musicdownloader.musicfreeapp825v2.BuildConfig;
import com.musicdownloader.musicfreeapp825v2.R;
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils;
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.Keys;
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.AudioMuxer;
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.DownloadStorage;
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.SearchApiExecutor;
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.ToastUtils;
import com.musicdownloader.musicfreeapp825v2.ui.fragments.DownloadsFragment;
import com.musicdownloader.musicfreeapp825v2.ui.fragments.OnlineSearchFragment;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import us.shandian.giga.get.DownloadManager;
import us.shandian.giga.get.DownloadManagerImpl;
import us.shandian.giga.get.DownloadMission;

public class DownloadManagerService extends Service implements DownloadMission.MissionListener {

    private static final String TAG = "MyAppDownloadManagerService";

    private static final String DOWNLOADING_CHANNEL_ID = "DOWNLOADING";
    private static final String DOWNLOADING_CHANNEL_NAME = "Downloading";

    private NotificationManagerCompat notificationManager;

    private DMBinder mBinder;
    private DownloadManager mManager;

    private long mLastTimeStamp = System.currentTimeMillis();

    /**
     * hash codes of the files being downloaded, used to show notifications
     */
    private final HashSet<Integer> downloadingNotificationId = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();


        mBinder = new DMBinder();
        if (mManager == null) {
            String path = DownloadStorage.getPathDownload().getPath();
            mManager = new DownloadManagerImpl(this, path);
        }

        notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat notificationChannel = new NotificationChannelCompat.Builder(
                DOWNLOADING_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(DOWNLOADING_CHANNEL_NAME).build();
            if (notificationManager.getNotificationChannel(DOWNLOADING_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Destroying");
        }

        for (int i = 0; i < mManager.getCount(); i++) {
            mManager.pauseMission(i);
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void showNotification() {
        for (int i = 0; i < mManager.getCount(); i++) {
            DownloadMission downloadMission = mManager.getMission(i);
            int notificationId = (int) downloadMission.timestamp;

            // Show the "downloading" notification
            if (downloadMission.running) {
                double progress = downloadMission.done * 1D / downloadMission.length * 100D;
                String contentText;

                if (progress >= 100D) {
                    if (downloadMission.hasShowMergingNotification) {
                        continue;
                    } else {
                        contentText = getString(R.string.merging);
                        downloadMission.hasShowMergingNotification = true;
                    }
                } else {
                    if (downloadMission.errCode == DownloadMission.ERROR_NO_SPACE) {
                        contentText = getString(R.string.no_space_left_on_device);
                    } else if (downloadMission.errCode == DownloadMission.ERROR_UNKNOWN_HOST) {
                        contentText = getString(R.string.no_internet);
                    } else {
                        contentText = String.format("%.1f%%", progress);
                    }
                }

                NotificationCompat.Builder progressNotificationBuilder = createProgressNotificationBuilder()
                    .setContentTitle(downloadMission.name)
                    .setContentText(contentText)
                    .setProgress(100, (int) progress, false)
                    .setSmallIcon(R.drawable.ic_music_downloader_monochrome);

                try {
                    notificationManager.notify(notificationId, progressNotificationBuilder.build());
                } catch (Exception e) {
                    FirebaseEventUtils.getInstances().recordException(e);
                }
                downloadingNotificationId.add(notificationId);
            }
            // Show the "download complete" notification
            else if (downloadMission.finished
                && downloadingNotificationId.contains(notificationId)
            ) {
                // The downloaded file MAY be a 360p video (fallback when the audio link 403'd) ->
                // extract the audio track into an .m4a and delete the original video. A normal audio
                // file is skipped, left untouched. Runs on a background thread (showNotification is
                // already off the main thread here).
                String downloadedRawPath = downloadMission.location
                    + File.separator
                    + downloadMission.name;
                String extractedPath = AudioMuxer.extractAudioIfHasVideo(downloadedRawPath);
                if (!extractedPath.equals(downloadedRawPath)) {
                    // Rename the mission -> toast/notification/media-scan point at the new .m4a.
                    downloadMission.name = new File(extractedPath).getName();
                    FirebaseEventUtils.getInstances().logEvent(this, Keys.DOWNLOAD_VIDEO_360);
                }

                LogEventLibs.logDownloadCompleteMusic(
                    downloadMission.id,
                    downloadMission.source,
                    downloadMission.name
                );
                FirebaseEventUtils.getInstances().logEvent(this, Keys.DOWNLOAD_SUCCESS);

                String text = getString(R.string.download_successfully)
                    + " "
                    + downloadMission.name;
                new Handler(Looper.getMainLooper()).post(
                    () -> ToastUtils.showToast(this, text)
                );

                // Notify the freshly downloaded file into the media store
                String downloadSuccessPath = downloadMission.location
                    + File.separator
                    + downloadMission.name;
                sendBroadcastDownloadedFile(downloadSuccessPath);

                NotificationCompat.Builder progressNotificationBuilder = createProgressNotificationBuilder()
                    .setContentTitle(downloadMission.name)
                    .setContentText(getString(R.string.download_successfully))
                    .setSmallIcon(R.drawable.ic_music_downloader_monochrome);
                notificationManager.cancel(notificationId);
                try {
                    notificationManager.notify(notificationId, progressNotificationBuilder.build());
                } catch (Exception e) {
                    FirebaseEventUtils.getInstances().recordException(e);
                }

                downloadingNotificationId.remove(notificationId);
            }
        }
    }

    private NotificationCompat.Builder createProgressNotificationBuilder() {
        NotificationCompat.Builder progressNotificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progressNotificationBuilder = new NotificationCompat.Builder(
                this,
                DOWNLOADING_CHANNEL_ID
            );
        } else {
            progressNotificationBuilder = new NotificationCompat.Builder(this);
        }
        return progressNotificationBuilder;
    }

    @Override
    public void onProgressUpdate(long done, long total) {
        long now = System.currentTimeMillis();
        long delta = now - mLastTimeStamp;

        if (delta > 2000) {
            postUpdateMessage();
            mLastTimeStamp = now;
        }
    }

    @Override
    public void onFinish() {
        postUpdateMessage();
    }

    @Override
    public void onError(DownloadMission downloadMission, int errCode) {
        LogEventLibs.logDownloadMusicError(
            downloadMission.id,
            downloadMission.source,
            String.valueOf(errCode)
        );
        FirebaseEventUtils.getInstances().logEvent(this, Keys.DOWNLOAD_FAILED);

        int resId;
        if (errCode == DownloadMission.ERROR_FORBIDDEN) {
            // Download link expired (403): ask the lib to re-fetch data for this videoId, and tell
            // the search screen to drop the stale link so the next download tap getLinks a fresh one.
            resId = R.string.song_link_expired_download;
            String videoId = downloadMission.id;
            if (videoId != null && !videoId.isEmpty()) {
                SearchApiExecutor.execute(
                    () -> ApiServices.getBodDataAgain(this, videoId)
                );
                Intent expiredIntent = new Intent(OnlineSearchFragment.ACTION_LINK_EXPIRED);
                expiredIntent.putExtra(OnlineSearchFragment.KEY_VIDEO_ID, videoId);
                expiredIntent.setPackage(getPackageName());
                sendBroadcast(expiredIntent);
            }
        } else if (errCode == DownloadMission.ERROR_NO_SPACE) {
            resId = R.string.no_space_left_on_device;
        } else if (errCode == DownloadMission.ERROR_UNKNOWN_HOST) {
            resId = R.string.no_internet;
        } else {
            resId = R.string.download_error;
        }

        String text = getString(resId);
        new Handler(Looper.getMainLooper()).post(
            () -> ToastUtils.showToast(this, text)
        );

        postUpdateMessage();
    }

    private void postUpdateMessage() {
        String name = Thread.currentThread().getName();
        if (name.equals("main")) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(this::showNotification);
        } else {
            showNotification();
        }
    }

    private void sendBroadcastDownloadedFile(String downloadedPath) {
        Log.i(TAG, "sendBroadcastDownloadedFile: " + downloadedPath);

        MediaScannerConnection.scanFile(
            this,
            new String[]{downloadedPath}, new String[]{"audio/*"},
            new MediaScannerConnection.MediaScannerConnectionClient() {

                @Override
                public void onMediaScannerConnected() {

                }

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    new Handler(Looper.getMainLooper()).postDelayed(
                        () -> {
                            Intent updateIntent = new Intent(DownloadsFragment.ACTION_UPDATE);
                            updateIntent.setPackage(getPackageName());
                            sendBroadcast(updateIntent);
                        },
                        1000
                    );
                }
            }
        );
    }

    // Wrapper of DownloadManager
    public class DMBinder extends Binder {
        public DownloadManager getDownloadManager() {
            return mManager;
        }

        public void onMissionAdded(DownloadMission mission) {
            mission.addListener(DownloadManagerService.this);
            postUpdateMessage();
        }

        public void onMissionRemoved(DownloadMission mission) {
            mission.removeListener(DownloadManagerService.this);
            postUpdateMessage();
        }
    }
}
