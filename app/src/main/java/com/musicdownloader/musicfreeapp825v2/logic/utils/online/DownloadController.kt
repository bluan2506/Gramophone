package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ailib.classifier.LogoCheckResult
import com.ailib.classifier.LogoClassifier
import com.applogevent.logeventlib.LogEventLibs
import com.aws.config.msserverconfig.Config_V1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.Keys
import us.shandian.giga.get.DownloadManager
import us.shandian.giga.service.DownloadManagerService

object DownloadController {

    private var appContext: Context? = null
    private var binder: DownloadManagerService.DMBinder? = null
    private var manager: DownloadManager? = null
    private var classifier: LogoClassifier? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as? DownloadManagerService.DMBinder
            manager = binder?.downloadManager
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            manager = null
        }
    }

    fun bind(context: Context) {
        appContext = context.applicationContext
        try {
            val intent = Intent(appContext, DownloadManagerService::class.java)
            appContext!!.startService(intent)
            appContext!!.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            FirebaseEventUtils.getInstances().recordException(e)
        }
    }

    fun getManager(): DownloadManager? = manager

    fun startMission(
        context: Context,
        url: String?,
        name: String?,
        id: String?,
        source: String?,
        referer: String?,
    ) {
        val mgr = manager
        val bnd = binder
        if (mgr == null || bnd == null) return

        CoroutineScope(Dispatchers.IO).launch {
            // Block the download if the thumbnail contains a copyrighted logo (fail-open on error).
            val logoResult = isLogoBlocked(context, id, source)
            if (logoResult.hasLogo) {
                when (logoResult.reason) {
                    LogoCheckResult.WARNER -> {} // Warner ("W") logo model
                    LogoCheckResult.VEVO -> {}   // Vevo logo template match
                }
                withContext(Dispatchers.Main) {
                    CopyrightRestrictionsDialog.show(context)
                }
                return@launch
            }

            // mgr.startMission shows a Toast internally -> MUST run on Main.
            withContext(Dispatchers.Main) {
                LogEventLibs.logDownloadMusic(id, source, name)
                appContext?.let { FirebaseEventUtils.getInstances().logEvent(it, Keys.DOWNLOAD_START) }

                val res = mgr.startMission(url, name, id, source, referer)
                bnd.onMissionAdded(mgr.getMission(res))
            }
        }
    }

    private fun classifier(): LogoClassifier? =
        classifier ?: try {
            LogoClassifier(appContext!!).also { classifier = it }
        } catch (e: Exception) {
            null
        }

    // check(videoId, configJson) downloads the thumbnail + runs TFLite/template matching -> MUST run
    // off the main thread. Returns a [LogoCheckResult] (hasLogo + reason: WARNER/VEVO/NONE). Any error
    // (no network, broken thumbnail, closed interpreter...) fails open so the download still proceeds.
    private suspend fun isLogoBlocked(
        context: Context,
        videoId: String?,
        source: String?,
    ): LogoCheckResult {
        // Only check the logo for the "un" search source; skip all other sources.
        if (source != "un") return LogoCheckResult.NOT_FOUND
        if (videoId.isNullOrEmpty()) return LogoCheckResult.NOT_FOUND
        val c = classifier() ?: return LogoCheckResult.NOT_FOUND
        return try {
            withContext(Dispatchers.IO) {
                val configJson = Config_V1.getServerConfig(context).toString()
                c.check(videoId, configJson)
            }
        } catch (e: Exception) {
            LogoCheckResult.NOT_FOUND // fail-open
        }
    }
}
