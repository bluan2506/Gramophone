package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.musicdownloader.musicfreeapp825v2.R

/**
 * Shown when a song can't be downloaded for copyright reasons (allow_download == false, or the logo
 * classifier flags the thumbnail). Ported from the MSDownloader reference.
 */
object CopyrightRestrictionsDialog {

    fun show(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.copyright_restrictions_title)
            .setMessage(R.string.copyright_restrictions_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
