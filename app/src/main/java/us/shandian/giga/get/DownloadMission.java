package us.shandian.giga.get;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import us.shandian.giga.util.Utility;

public class DownloadMission {
    private static final String TAG = "MyAppDownloadMission";

    public interface MissionListener {
        HashMap<MissionListener, Handler> handlerStore = new HashMap<>();

        void onProgressUpdate(long done, long total);

        void onFinish();

        void onError(DownloadMission downloadMission, int errCode);
    }

    public static final int ERROR_SERVER_UNSUPPORTED = 206;
    /**
     * Download link returned HTTP 403 (stream link expired) -> need to re-fetch data for this id.
     */
    public static final int ERROR_FORBIDDEN = 403;
    public static final int ERROR_UNKNOWN = 233;
    public static final int ERROR_CONNECT = 234;
    public static final int ERROR_NO_SPACE = 235;
    public static final int ERROR_SOCKET = 236;
    public static final int ERROR_UNKNOWN_HOST = 237;

    public String name = "";
    public String url = "";
    public String location = "";
    public long blocks = 0;
    public long length = 0;
    public long done = 0;
    public int threadCount = 4;
    public int finishCount = 0;
    public ArrayList<Long> threadPositions = new ArrayList<>();
    public HashMap<Long, Boolean> blockState = new HashMap<>();
    public boolean running = false;
    public boolean finished = false;
    public boolean fallback = false;
    public int errCode = -1;
    public long timestamp = 0;

    /**
     * Music id returned by the get-link lib, used for Firebase event logging.
     */
    public String id;

    /**
     * Youtube, Cmixter, Netease
     */
    public String source;

    /**
     * referer for Ccmixter
     */
    public String referer;

    public boolean hasShowMergingNotification = false;

    public transient boolean recovered = false;

    private final transient ArrayList<WeakReference<MissionListener>> mListeners = new ArrayList<>();
    private transient boolean mWritingToFile = false;

    public boolean isBlockPreserved(long block) {
        if (blockState == null) {
            return false;
        }
        return blockState.getOrDefault(block, false);
    }

    public void preserveBlock(long block) {
        synchronized (blockState) {
            blockState.put(block, true);
        }
    }

    public void setPosition(int id, long position) {
        threadPositions.set(id, position);
    }

    public long getPosition(int id) {
        return threadPositions.get(id);
    }

    public synchronized void notifyProgress(long deltaLen) {
        if (!running) return;

        if (recovered) {
            recovered = false;
        }

        done += deltaLen;

        if (done > length) {
            done = length;
        }

        if (done != length) {
            writeThisToFile();
        }

        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            if (listener != null) {
                listener.onProgressUpdate(done, length);
            }
        }
    }

    public synchronized void notifyFinished() {
        if (errCode > 0) {
            return;
        }

        finishCount++;

        Log.i(TAG, "finishCount: " + finishCount + ", threadCount: " + threadCount);
        if (finishCount == threadCount) {
            onFinish();
        }
    }

    private void onFinish() {
        if (errCode > 0) return;

        running = false;
        finished = true;

        deleteThisFromFile();

        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            if (listener != null) {
                listener.onFinish();
            }
        }
    }

    public synchronized void notifyError(int err) {
        errCode = err;

        writeThisToFile();

        for (WeakReference<MissionListener> ref : mListeners) {
            final MissionListener listener = ref.get();
            listener.onError(this, errCode);
        }
    }

    public synchronized void addListener(MissionListener listener) {
        Handler handler = new Handler(Looper.getMainLooper());
        MissionListener.handlerStore.put(listener, handler);
        mListeners.add(new WeakReference<>(listener));
    }

    public synchronized void removeListener(MissionListener listener) {
        mListeners.removeIf(weakRef -> listener != null && listener == weakRef.get());
    }

    public void start() {
        if (!running && !finished) {
            running = true;

            if (!fallback) {
                for (int i = 0; i < threadCount; i++) {
                    if (threadPositions.size() <= i && !recovered) {
                        threadPositions.add((long) i);
                    }
                    new Thread(new DownloadRunnable(this, i)).start();
                }
            } else {
                // In fallback mode, resuming is not supported.
                threadCount = 1;
                done = 0;
                blocks = 0;
                new Thread(new DownloadRunnableFallback(this)).start();
            }
        }
    }

    public void pause() {
        if (running) {
            running = false;
            recovered = true;
        }
    }

    public void delete() {
        deleteThisFromFile();
        new File(location + "/" + name).delete();
    }

    public void writeThisToFile() {
        if (!mWritingToFile) {
            mWritingToFile = true;
            new Thread() {
                @Override
                public void run() {
                    try {
                        doWriteThisToFile();
                    } catch (Exception e) {
                        FirebaseEventUtils.getInstances().recordException(e);
                    }
                    mWritingToFile = false;
                }
            }.start();
        }
    }

    private void doWriteThisToFile() {
        synchronized (blockState) {
            Utility.writeToFile(location + "/" + name + ".giga", new Gson().toJson(this));
        }
    }

    private void deleteThisFromFile() {
        new File(location + "/" + name + ".giga").delete();
    }
}
