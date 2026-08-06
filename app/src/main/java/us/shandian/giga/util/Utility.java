package us.shandian.giga.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;

import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

public class Utility {

    public enum FileType {
        APP,
        VIDEO,
        EXCEL,
        WORD,
        POWERPOINT,
        MUSIC,
        ARCHIVE,
        UNKNOWN
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return String.format("%d B", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f kB", (float) bytes / 1024);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", (float) bytes / 1024 / 1024);
        } else {
            return String.format("%.2f GB", (float) bytes / 1024 / 1024 / 1024);
        }
    }

    public static String formatSpeed(float speed) {
        if (speed < 1024) {
            return String.format("%.2f B/s", speed);
        } else if (speed < 1024 * 1024) {
            return String.format("%.2f kB/s", speed / 1024);
        } else if (speed < 1024 * 1024 * 1024) {
            return String.format("%.2f MB/s", speed / 1024 / 1024);
        } else {
            return String.format("%.2f GB/s", speed / 1024 / 1024 / 1024);
        }
    }

    public static void writeToFile(String fileName, String content) {
        try {
            writeToFile(fileName, content.getBytes("UTF-8"));
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
        }
    }

    public static void writeToFile(String fileName, byte[] content) {
        File f = new File(fileName);

        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (Exception e) {
                FirebaseEventUtils.getInstances().recordException(e);
            }
        }

        try {
            FileOutputStream opt = new FileOutputStream(f, false);
            opt.write(content, 0, content.length);
            opt.close();
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
        }
    }

    public static String readFromFile(String file) {
        try {
            File f = new File(file);

            if (!f.exists() || !f.canRead()) {
                return null;
            }

            BufferedInputStream ipt = new BufferedInputStream(new FileInputStream(f));

            byte[] buf = new byte[512];
            StringBuilder sb = new StringBuilder();

            while (ipt.available() > 0) {
                int len = ipt.read(buf, 0, 512);
                sb.append(new String(buf, 0, len, "UTF-8"));
            }

            ipt.close();
            return sb.toString();
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
            return null;
        }
    }

    public static <T> T findViewById(View v, int id) {
        return (T) v.findViewById(id);
    }

    public static <T> T findViewById(Activity activity, int id) {
        return (T) activity.findViewById(id);
    }

    public static String getFileExt(String url) {
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf("."));
            if (ext.contains("%")) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.contains("/")) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }

    public static FileType getFileType(String file) {
        if (file.endsWith(".apk")) {
            return FileType.APP;
        } else if (file.endsWith(".mp3") || file.endsWith(".wav") || file.endsWith(".flac")) {
            return FileType.MUSIC;
        } else if (file.endsWith(".mp4") || file.endsWith(".mpeg") || file.endsWith(".rm") || file.endsWith(".rmvb")
            || file.endsWith(".flv") || file.endsWith(".webp")) {
            return FileType.VIDEO;
        } else if (file.endsWith(".doc") || file.endsWith(".docx")) {
            return FileType.WORD;
        } else if (file.endsWith(".xls") || file.endsWith(".xlsx")) {
            return FileType.EXCEL;
        } else if (file.endsWith(".ppt") || file.endsWith(".pptx")) {
            return FileType.POWERPOINT;
        } else if (file.endsWith(".zip") || file.endsWith(".rar") || file.endsWith(".7z") || file.endsWith(".gz")
            || file.endsWith("tar") || file.endsWith(".bz")) {
            return FileType.ARCHIVE;
        } else {
            return FileType.UNKNOWN;
        }
    }

    public static boolean isDirectoryAvailble(String path) {
        File dir = new File(path);
        return dir.exists() && dir.isDirectory();
    }

    public static void copyToClipboard(Context context, String str) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("text", str));
    }

    public static String checksum(String path, String algorithm) {
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
            throw new RuntimeException(e);
        }

        FileInputStream i = null;

        try {
            i = new FileInputStream(path);
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
            throw new RuntimeException(e);
        }

        byte[] buf = new byte[1024];
        int len = 0;

        try {
            while ((len = i.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
        } catch (Exception e) {
            FirebaseEventUtils.getInstances().recordException(e);
        }

        byte[] digest = md.digest();

        // HEX
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();

    }

    public static String formatMp3FileName(String fileName) {
        String mp3FileName = fileName;
        mp3FileName = mp3FileName.replace("/", "\\");
        mp3FileName = mp3FileName.replace(":", "-");
        mp3FileName = mp3FileName.replaceAll("[\\\\/:*?\"<>|]", "");
        mp3FileName = mp3FileName.replace("#", "");
        mp3FileName = mp3FileName.replace("Kism", "Kis");
        mp3FileName = mp3FileName.replace("kism", "kis");
        mp3FileName = mp3FileName.replace("KISM", "kis");
        mp3FileName += "_" + System.currentTimeMillis() + ".mp3";
        return mp3FileName;
    }
}
