package us.shandian.giga.get;

import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

public class DownloadRunnable implements Runnable {
    private static final String TAG = "MyAppDownloadRunnable";

    private final DownloadMission mMission;
    private final int mId;

    public DownloadRunnable(DownloadMission mission, int id) {
        mMission = mission;
        mId = id;
    }

    @Override
    public void run() {
        boolean retry = mMission.recovered;
        long position = mMission.getPosition(mId);

        while (mMission.errCode == -1 && mMission.running && position < mMission.blocks) {

            if (Thread.currentThread().isInterrupted()) {
                mMission.pause();
                return;
            }

            // Wait for an unblocked position
            while (!retry && position < mMission.blocks && mMission.isBlockPreserved(position)) {
                position++;
            }

            retry = false;

            if (position >= mMission.blocks) {
                break;
            }

            mMission.preserveBlock(position);
            mMission.setPosition(mId, position);

            long start = position * DownloadManager.BLOCK_SIZE;
            long end = start + DownloadManager.BLOCK_SIZE - 1;

            if (end >= mMission.length) {
                end = mMission.length - 1;
            }

            HttpURLConnection conn;

            int total = 0;

            try {
                URL url = new URL(mMission.url);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
                if (mMission.referer != null && !mMission.referer.isBlank()) {
                    conn.setRequestProperty("Referer", mMission.referer);
                }

                // Link expired mid-download (403) -> report error to re-fetch data for this id.
                if (conn.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                    mMission.errCode = DownloadMission.ERROR_FORBIDDEN;
                    notifyError(DownloadMission.ERROR_FORBIDDEN);
                    break;
                }

                // A server may be ignoring the range request
                if (conn.getResponseCode() != 206) {
                    mMission.errCode = DownloadMission.ERROR_SERVER_UNSUPPORTED;
                    notifyError(DownloadMission.ERROR_SERVER_UNSUPPORTED);
                    break;
                }

                RandomAccessFile f = new RandomAccessFile(mMission.location + "/" + mMission.name, "rw");
                f.seek(start);
                BufferedInputStream ipt = new BufferedInputStream(conn.getInputStream());
                byte[] buf = new byte[512];

                while (start < end && mMission.running) {
                    int len = ipt.read(buf, 0, 512);

                    if (len == -1) {
                        break;
                    } else {
                        start += len;
                        total += len;
                        f.write(buf, 0, len);
                        notifyProgress(len);
                    }
                }

                f.close();
                ipt.close();

                // TODO We should save progress for each thread
            } catch (ConnectException | UnknownHostException e) {
                FirebaseEventUtils.getInstances().recordException(e);
                retry = false;
                notifyError(DownloadMission.ERROR_UNKNOWN_HOST);
            } catch (IOException e) {
                FirebaseEventUtils.getInstances().recordException(e);
                retry = false;
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("No space left on device")) {
                        notifyError(DownloadMission.ERROR_NO_SPACE);
                    } else if (e.getMessage().contains("No address associated with hostname")
                        || e.getMessage().contains("connection abort")
                    ) {
                        notifyError(DownloadMission.ERROR_UNKNOWN_HOST);
                    }
                }
            } catch (Exception e) {
                FirebaseEventUtils.getInstances().recordException(e);
                // TODO Retry count limit & notify error
                retry = true;

                notifyProgress(-total);
            }
        }

        if (mMission.errCode == -1 && mMission.running) {
            notifyFinished();
        }
    }

    private void notifyProgress(final long len) {
        synchronized (mMission) {
            mMission.notifyProgress(len);
        }
    }

    private void notifyError(final int err) {
        synchronized (mMission) {
            mMission.notifyError(err);
            mMission.pause();
        }
    }

    private void notifyFinished() {
        synchronized (mMission) {
            mMission.notifyFinished();
        }
    }
}
