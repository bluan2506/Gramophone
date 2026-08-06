package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.musicdownloader.musicfreeapp825v2.R

object VpnProxyDialog {

    fun show(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.turn_off_vpn_proxy)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
