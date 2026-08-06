package com.musicdownloader.musicfreeapp825v2.logic.utils.online

import java.util.concurrent.Executors

/**
 * Serial queue for background calls into the searchapi lib (currently
 * [com.music.searchapi.ApiServices.getBodDataAgain] when a link 403s).
 *
 * The lib only runs one function at a time — overlapping getBodDataAgain calls from several threads
 * corrupt it. Every call site funnels through here so later calls queue behind earlier ones, off the
 * main thread. Ported from the MSDownloader reference.
 */
object SearchApiExecutor {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "SearchApiExecutor")
    }

    @JvmStatic
    fun execute(task: Runnable) {
        executor.execute(task)
    }
}
