package org.akanework.gramophone.logic.utils.online

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ailib.classifier.LogoClassifier
import com.applogevent.logeventlib.LogEventLibs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils
import org.akanework.gramophone.logic.utils.firebase.Keys
import us.shandian.giga.get.DownloadManager
import us.shandian.giga.service.DownloadManagerService

/**
 * Binds the giga [DownloadManagerService] and starts download missions.
 *
 * Ported from the MSDownloader reference's `AbsMusicServiceActivity.startMission`: before a YouTube
 * download it runs the TFLite [LogoClassifier] on the thumbnail and blocks (fail-open) if the logo
 * probability is high. Bound once from [MainActivity][org.akanework.gramophone.ui.MainActivity] with
 * the application context, so it lives for the whole app.
 */
object DownloadController {

    // Probability that the thumbnail contains a logo above which a download is blocked (80%).
    private const val LOGO_BLOCK_THRESHOLD = 0.8f

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
            // Block the download if the thumbnail contains a logo with high probability (fail-open).
            if (isLogoBlocked(id, source)) {
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

    // classify(videoId) hits the network (downloads the thumbnail) + runs TFLite -> MUST run off the
    // main thread. Only checked when the search source is "un" (YouTube); any error => fail-open.
    private suspend fun isLogoBlocked(videoId: String?, source: String?): Boolean {
        if (source != "un") return false
        if (videoId.isNullOrEmpty()) return false
        val c = classifier() ?: return false
        return try {
            withContext(Dispatchers.IO) { c.classify(videoId) } >= LOGO_BLOCK_THRESHOLD
        } catch (e: Exception) {
            false // fail-open
        }
    }
}
