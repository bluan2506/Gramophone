package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Where online downloads are stored — ported from the MSDownloader reference (`Downloads/MusicDownload`).
 */
object DownloadStorage {

    @JvmStatic
    fun getPathDownload(): File {
        val rootFile: File = if (Build.VERSION.SDK_INT >= 30) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStorageDirectory()
        }
        return File("$rootFile/MusicDownload")
    }
}
