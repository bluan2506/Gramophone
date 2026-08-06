/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.musicdownloader.musicfreeapp825v2.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.applogevent.logeventlib.LogEventLibs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.thinkup.core.api.TUSDK
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.InterstitialAdsUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.KeyTopOn
import com.musicdownloader.musicfreeapp825v2.logic.utils.config.ConfigUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.ui.SplashActivity
import java.util.Date
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.core.content.edit
import androidx.fragment.app.strictmode.FragmentStrictMode
import androidx.media3.common.util.Log
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.preference.PreferenceManager
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.map.AndroidUriMapper
import coil3.request.NullRequestDataException
import coil3.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.musicdownloader.musicfreeapp825v2.BuildConfig
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.logic.ui.BugHandlerActivity
import com.musicdownloader.musicfreeapp825v2.logic.utils.CoilArtPipeline
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.KeyAdMob
import com.musicdownloader.musicfreeapp825v2.ui.LyricWidgetProvider
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.lsposed.hiddenapibypass.LSPass
import org.nift4.gramophone.hificore.UacManager
import uk.akane.libphonograph.reader.FlowReader
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

class MusicDownloaderApplication : Application(), SingletonImageLoader.Factory,
    Thread.UncaughtExceptionHandler, SharedPreferences.OnSharedPreferenceChangeListener,
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "MusicDownloaderApplication"
    }

    private var currentActivity: Activity? = null
    private lateinit var appOpenAdManager: AppOpenAdManager
    val configEntity by lazy { ConfigUtils.configApp(this) }

    init {
        @SuppressLint("DefaultUncaughtExceptionDelegation")
        Thread.setDefaultUncaughtExceptionHandler(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.MODEL != "robolectric") {
            HiddenApiBypass.setHiddenApiExemptions("")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LSPass.setHiddenApiExemptions("")
        }
        if (BuildConfig.DEBUG) {
            System.setProperty("kotlinx.coroutines.debug", "on")
            @OptIn(ExperimentalComposeRuntimeApi::class)
            Composer.setDiagnosticStackTraceEnabled(true)
        }
    }

    val minSongLengthSecondsFlow = MutableSharedFlow<Long>(replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val blackListSetFlow = MutableSharedFlow<Set<String>>(replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val whiteListSetFlow = MutableSharedFlow<Set<String>>(replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val shouldUseEnhancedCoverReadingFlow = if (hasScopedStorageWithMediaTypes()) null else
        MutableSharedFlow<Boolean?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recentlyAddedFilterSecondFlow = MutableStateFlow(1_209_600L)
    val extraDisallowedFolders = setOf(
        Environment.DIRECTORY_RINGTONES,
        Environment.DIRECTORY_NOTIFICATIONS,
        Environment.DIRECTORY_ALARMS,
        Environment.DIRECTORY_PODCASTS,
        "Android/media",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Environment.DIRECTORY_AUDIOBOOKS else "Audiobooks",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Environment.DIRECTORY_RECORDINGS else "Recordings"
    )
    lateinit var reader: FlowReader
        private set
    lateinit var uacManager: UacManager
        private set

    override fun onCreate() {
        super<Application>.onCreate()
        // disk read and write on first launch, but unavoidable as threads would race setDefaultNightMode
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (BuildConfig.DEBUG && !isColorOS()) {
            // Use StrictMode to find antipattern issues
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectAll()
                    .let {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                            Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM
                        ) {
                            it.permitExplicitGc() // platform bug, now fixed
                        } else it
                    }
                    .let {
                        if (Debug.isDebuggerConnected() || isAlpsBoostFwkPresent())
                            it.permitDiskReads()
                        else it
                    }
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectAll()
                    // detectAll does in fact not detect everything :)
                    .let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            it.detectImplicitDirectBoot()
                        } else it
                    }
                    .penaltyLog()
                    .let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            it.penaltyDeathOnFileUriExposure()
                        } else it
                    }
                    .build()
            )
            FragmentStrictMode.defaultPolicy = FragmentStrictMode.Policy.Builder()
                .detectFragmentReuse()
                .detectFragmentTagUsage()
                .detectRetainInstanceUsage()
                .detectSetUserVisibleHint()
                //.detectTargetFragmentUsage() TODO onDisplayPreferenceDialog()
                .detectWrongFragmentContainer()
                .detectWrongNestedHierarchy()
                .penaltyDeath()
                .build()
        }
        android.util.Log.d(TAG, "MusicDownloaderApplication.onCreate()")
        org.nift4.mediastorecompat.Log.setLogger(object : org.nift4.mediastorecompat.Log.Logger {
            override fun d(
                tag: String,
                message: String,
                throwable: Throwable?
            ) {
                Log.d(tag, message, throwable)
            }

            override fun i(
                tag: String,
                message: String,
                throwable: Throwable?
            ) {
                Log.i(tag, message, throwable)
            }

            override fun w(
                tag: String,
                message: String,
                throwable: Throwable?
            ) {
                Log.w(tag, message, throwable)
            }

            override fun e(
                tag: String,
                message: String,
                throwable: Throwable?
            ) {
                Log.e(tag, message, throwable)
            }
        })
        if (!android.util.Log.isLoggable(TAG, android.util.Log.INFO)) {
            Log.setLogger(object : Log.Logger {
                override fun d(
                    tag: String,
                    message: String,
                    throwable: Throwable?
                ) {
                    android.util.Log.e(tag, "[DEBUG] $message", throwable)
                }

                override fun i(
                    tag: String,
                    message: String,
                    throwable: Throwable?
                ) {
                    android.util.Log.e(tag, "[INFO] $message", throwable)
                }

                override fun w(
                    tag: String,
                    message: String,
                    throwable: Throwable?
                ) {
                    android.util.Log.e(tag, "[WARN] $message", throwable)
                }

                override fun e(
                    tag: String,
                    message: String,
                    throwable: Throwable?
                ) {
                    android.util.Log.e(tag, "[ERROR] $message", throwable)
                }
            })
        }
        uacManager = UacManager(this)
        reader = FlowReader(
            this,
            if (BuildConfig.DISABLE_MEDIA_STORE_FILTER) MutableStateFlow(0) else
                minSongLengthSecondsFlow,
            blackListSetFlow,
            whiteListSetFlow,
            if (hasScopedStorageWithMediaTypes()) MutableStateFlow(null) else
                shouldUseEnhancedCoverReadingFlow!!,
            recentlyAddedFilterSecondFlow
        )
        // Set application theme when launching.
        when (prefs.getString("theme_mode", "0")) {
            "0" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }

            "1" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            "2" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        // --- Ads / analytics / lifecycle (mirrors the sample app's Application) ---
        registerActivityLifecycleCallbacks(this)
        try {
            val analyticsUrl = ConfigUtils.SERVER_URL_ANALYTICS
            LogEventLibs.init(this, analyticsUrl)
        } catch (e: Exception) {
            FirebaseEventUtils.getInstances().recordException(e)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        try {
            Log.i(TAG, "TopOn SDK version: " + TUSDK.getSDKVersionName())
            TUSDK.init(this, KeyTopOn.APP_ID, KeyTopOn.APP_KEY)
        } catch (e: Exception) {
            FirebaseEventUtils.getInstances().recordException(e)
        }
        // This is a separate thread to avoid disk read on main thread and improve startup time
        CoroutineScope(Dispatchers.Default).launch {
            if (prefs.getBoolean("needToAdd_isMusicBlacklist", true)) {
                prefs.edit(true) {
                    putBoolean("needToAdd_isMusicBlacklist", false)
                    if (prefs.contains("folderFilter")) {
                        putStringSet(
                            "folderFilter", (prefs.getStringSet(
                                "folderFilter", setOf()
                            ) ?: setOf()) + extraDisallowedFolders
                        )
                    }
                    if (prefs.getInt("mediastore_filter", 0) == 60) {
                        putInt("mediastore_filter",
                            resources.getInteger(R.integer.filter_default_sec))
                    }
                }
            }
            onSharedPreferenceChanged(prefs, null) // reload all values
            prefs.registerOnSharedPreferenceChangeListener(this@MusicDownloaderApplication)

            // https://github.com/androidx/media/issues/805
            if (needsMissingOnDestroyCallWorkarounds()) {
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
            }

            LyricWidgetProvider.update(this@MusicDownloaderApplication)

            delay(10000.milliseconds) // Wait until we are idle with useless IO
            withContext(Dispatchers.IO) {
                // Clean up old logs
                val selfLogDir = File(cacheDir, "SelfLog")
                selfLogDir.listFiles()?.forEach(File::delete)
            }
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        runBlocking {
            if (key == null || key == "mediastore_filter") {
                minSongLengthSecondsFlow.emit(
                    prefs.getInt(
                        "mediastore_filter",
                        resources.getInteger(R.integer.filter_default_sec)
                    ).toLong()
                )
            }
            if (key == null || key == "folderFilter") {
                blackListSetFlow.emit(prefs.getStringSet("folderFilter",
                    extraDisallowedFolders) ?: extraDisallowedFolders)
            }
            if (key == null || key == "folderAllow") {
                whiteListSetFlow.emit(prefs.getStringSet("folderAllow", setOf()) ?: setOf())
            }
            if ((key == null || key == "album_covers") && !hasScopedStorageWithMediaTypes()) {
                shouldUseEnhancedCoverReadingFlow!!.emit(prefs.getBoolean("album_covers", true))
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCache(null)
            .components {
                add(CoilArtPipeline.ThumbnailKeyer())
                add(CoilArtPipeline.AlbumThumbnailKeyer())
                add(CoilArtPipeline.AudioCoverKeyer())
                add(AndroidUriMapper())
                add(CoilArtPipeline.ThumbnailMapper())
                add(CoilArtPipeline.AudioCoverMapper())
                add(CoilArtPipeline.AlbumThumbnailMapper())
                add(CoilArtPipeline.ThumbnailFetcherFactory())
                add(CoilArtPipeline.AlbumThumbnailFetcherFactory())
                add(CoilArtPipeline.SongCoverFetcherFactory())
                // Load online thumbnails (http/https URLs) for the online search screen.
                add(OkHttpNetworkFetcherFactory())
            }
            .run {
                if (!BuildConfig.DEBUG) this else
                    logger(object : Logger {
                        override var minLevel = Logger.Level.Verbose
                        override fun log(
                            tag: String,
                            level: Logger.Level,
                            message: String?,
                            throwable: Throwable?
                        ) {
                            if (level < minLevel) return
                            val println = { it: String ->
                                when (level) {
                                    Logger.Level.Verbose -> Log.d(tag, it)
                                    Logger.Level.Debug -> Log.d(tag, it)
                                    Logger.Level.Info -> Log.i(tag, it)
                                    Logger.Level.Warn -> Log.w(tag, it)
                                    Logger.Level.Error -> Log.e(tag, it)
                                }
                            }
                            if (message != null) {
                                println(message)
                            }
                            // Let's keep the log readable and ignore normal events' stack traces.
                            if (throwable != null && throwable !is NullRequestDataException
                                && throwable !is CoilArtPipeline.NoAlbumArtException
                                && (throwable !is IOException
                                        || throwable.message != "No album art found"
                                        && throwable.message != "No embedded album art found"
                                        && throwable.message != "No thumbnails in Downloads directories"
                                        && throwable.message != "No thumbnails in top-level directories")
                            ) {
                                println(Log.getThrowableString(throwable)!!)
                            }
                        }
                    })
            }
            .build()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // TODO convert to notification that opens BugHandlerActivity on click, and let JVM
        //  go through the normal exception process (to get stats from play). disadvantage: we can't
        //  cheat the statistic that way
        val exceptionMessage = Log.getThrowableString(e)
        val threadName = Thread.currentThread().name
        Log.e(TAG, "Error on thread $threadName:\n $exceptionMessage")
        val intent = Intent(this, BugHandlerActivity::class.java)
        intent.putExtra("exception_message", exceptionMessage)
        intent.putExtra("thread", threadName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        exitProcess(10)
    }

    private fun isAlpsBoostFwkPresent(): Boolean {
        try {
            Class.forName("com.mediatek.boostfwk.BoostFwkManagerImpl")
            return true
        } catch (_: Throwable) {
            return false
        }
    }

    private fun isColorOS(): Boolean {
        val props = listOf(
            "ro.build.version.opporom",
            "ro.oplus.os.version"
        )
        return props.any {
            !getSystemProperty(it).isNullOrBlank()
        }
    }

    // --- App-open ad on returning to foreground (mirrors the sample app) ---

    override fun onStart(owner: LifecycleOwner) {
        val activity = currentActivity ?: return
        if (activity !is SplashActivity && !configEntity.isToponads
            && !InterstitialAdsUtils.isShowAdsGoToSearchScreen
        ) {
            appOpenAdManager.showAdIfAvailable(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (!appOpenAdManager.isShowingAd) currentActivity = activity
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    /** AdMob App Open ad manager, adapted from the sample app (revenue logging omitted). */
    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false
        private var loadTime = 0L

        fun loadAd(context: Context) {
            if (isLoadingAd || isAdAvailable()) return
            isLoadingAd = true
            AppOpenAd.load(context, KeyAdMob.OPEN_APP, AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        Log.e(TAG, "app open onAdFailedToLoad: $loadAdError")
                    }
                })
        }

        /** App-open ads expire after four hours. */
        private fun isAdAvailable() =
            appOpenAd != null && Date().time - loadTime < 4 * 3600_000L

        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(activity, object : OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    // Empty because the user will go back to the activity that showed the ad.
                }
            })
        }

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(TAG, "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }

            Log.d(TAG, "Will show ad.")

            appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    if (activity is SplashActivity) {
                        activity.startMainActivity()
                    } else {
                        onShowAdCompleteListener.onShowAdComplete()
                    }
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    Log.e(TAG, "app open onAdFailedToShow: $adError")
                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    if (activity is SplashActivity) {
                        activity.alreadyShowAds = true
                    }
                }
            }
            isShowingAd = true
            appOpenAd!!.show(activity)
        }
    }
}
