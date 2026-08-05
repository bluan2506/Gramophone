package org.akanework.gramophone.logic.utils.online

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.akanework.gramophone.R

object VpnProxyDialog {

    fun show(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.turn_off_vpn_proxy)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
