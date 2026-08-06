package com.musicdownloader.musicfreeapp825v2.logic.utils

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.logic.utils.config.ConfigUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils

/**
 * Config-driven "update app" dialog, ported from the MSDownloader reference's UpdateUtils.
 *
 * When remote config sets `request_update == "1"`, prompts the user to update. The Update button
 * opens `request_update_url`. `request_update_force_exit_app == "1"` makes it a forced update: no
 * dismiss button, not cancelable, and the app closes after sending the user to the store.
 */
object UpdateUtils {

    fun showDialogUpdate(activity: Activity) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            FirebaseEventUtils.getInstances().recordException(throwable)
        }
        // Read the cached config off the main thread (it parses JSON from disk).
        CoroutineScope(Dispatchers.IO + SupervisorJob() + handler).launch {
            val config = ConfigUtils.configApp(activity)
            if (config.request_update != "1") return@launch
            withContext(Dispatchers.Main) {
                if (activity.isFinishing) return@withContext
                val forceExit = config.request_update_force_exit_app == "1"
                val builder = MaterialAlertDialogBuilder(activity)
                    .setTitle(config.request_update_title)
                    .setMessage(config.request_update_text)
                    .setCancelable(!forceExit)
                    .setPositiveButton(config.request_update_text_update) { dialog, _ ->
                        runCatching {
                            activity.startActivity(
                                Intent(Intent.ACTION_VIEW, config.request_update_url.toUri())
                            )
                        }
                        if (forceExit) activity.finish() else dialog.dismiss()
                    }
                // A forced update has no "close" button, so the only way out is to update.
                if (!forceExit) {
                    builder.setNegativeButton(config.request_update_text_close, null)
                }
                builder.show()
            }
        }
    }
}
