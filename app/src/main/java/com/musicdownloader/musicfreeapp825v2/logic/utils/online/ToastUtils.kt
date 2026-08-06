package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Small Toast helper used by the online download engine. Ported from the MSDownloader reference.
 */
object ToastUtils {

    @JvmStatic
    fun showToast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun showToast(context: Context, @StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }
}
