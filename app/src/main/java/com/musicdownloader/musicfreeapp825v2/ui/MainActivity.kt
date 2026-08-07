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

package com.musicdownloader.musicfreeapp825v2.ui

import android.app.NotificationManager
import android.app.SearchManager
import android.app.assist.AssistContent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.provider.MediaStore
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.session.DefaultMediaNotificationProvider
import coil3.imageLoader
import com.google.android.ads.nativetemplates.NativeUtils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.musicdownloader.musicfreeapp825v2.R
import com.musicdownloader.musicfreeapp825v2.databinding.BottomSheetBackBinding
import com.musicdownloader.musicfreeapp825v2.logic.clone
import com.musicdownloader.musicfreeapp825v2.logic.dpToPx
import com.musicdownloader.musicfreeapp825v2.logic.enableEdgeToEdgeProperly
import com.musicdownloader.musicfreeapp825v2.logic.getBooleanStrict
import com.musicdownloader.musicfreeapp825v2.logic.hasAudioPermission
import com.musicdownloader.musicfreeapp825v2.logic.hasNotificationPermission
import com.musicdownloader.musicfreeapp825v2.logic.hasScopedStorageV2
import com.musicdownloader.musicfreeapp825v2.logic.hasScopedStorageWithMediaTypes
import com.musicdownloader.musicfreeapp825v2.logic.musicDownloaderApplication
import com.musicdownloader.musicfreeapp825v2.logic.needsMissingOnDestroyCallWorkarounds
import com.musicdownloader.musicfreeapp825v2.logic.padOutOfSystemBars
import com.musicdownloader.musicfreeapp825v2.logic.postAtFrontOfQueueAsync
import com.musicdownloader.musicfreeapp825v2.logic.supportsNotificationPermission
import com.musicdownloader.musicfreeapp825v2.logic.ui.BaseActivity
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.InterstitialAdsUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.KeyAdMob
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.KeyTopOn
import com.musicdownloader.musicfreeapp825v2.logic.utils.ads.SelfRenderViewUtil
import com.musicdownloader.musicfreeapp825v2.logic.utils.RateAppUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.firebase.FirebaseEventUtils
import com.musicdownloader.musicfreeapp825v2.logic.utils.online.DownloadController
import com.musicdownloader.musicfreeapp825v2.ui.adapters.PlaylistAdapter
import com.musicdownloader.musicfreeapp825v2.ui.components.PlayerBottomSheet
import com.musicdownloader.musicfreeapp825v2.ui.components.RateAppBottomSheet
import com.musicdownloader.musicfreeapp825v2.ui.components.SatisfiedBottomSheet
import com.musicdownloader.musicfreeapp825v2.ui.fragments.BaseFragment
import com.musicdownloader.musicfreeapp825v2.ui.fragments.DownloadsFragment
import com.musicdownloader.musicfreeapp825v2.ui.fragments.GeneralSubFragment
import com.musicdownloader.musicfreeapp825v2.ui.fragments.SearchFragment
import com.musicdownloader.musicfreeapp825v2.ui.fragments.ViewPagerFragment
import com.thinkup.banner.api.TUBannerListener
import com.thinkup.banner.api.TUBannerView
import com.thinkup.core.api.TUAdConst
import com.thinkup.core.api.TUAdInfo
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativeDislikeListener
import com.thinkup.nativead.api.TUNativeEventListener
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.TUNativePrepareInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.nift4.mediastorecompat.MediaStoreCompat
import uk.akane.libphonograph.dynamicitem.Favorite
import uk.akane.libphonograph.manipulator.ItemManipulator
import uk.akane.libphonograph.manipulator.PlaylistSerializer
import uk.akane.libphonograph.manipulator.PlaylistSerializer.Entry
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * MainActivity:
 *   Core of gramophone, one and the only activity
 * used across the application.
 *
 * @author AkaneTan, nift4
 */
class MainActivity : BaseActivity() {

    companion object {
        private const val PERMISSION_READ_MEDIA_AUDIO = 100
        private const val PERMISSION_POST_NOTIFICATIONS = 101
        const val PLAYBACK_AUTO_START_FOR_FGS = "AutoStartFgs"
        const val PLAYBACK_AUTO_PLAY_ID = "AutoStartId"
        const val PLAYBACK_AUTO_PLAY_POSITION = "AutoStartPos"
        const val FAVORITE_ENTRY = "FavoriteEntry"
        const val FAVORITE_STATE = "FavoriteState"
    }

    // Import our viewModels.
    val controllerViewModel: MediaControllerViewModel by viewModels()
    val startingActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val handler = Handler(Looper.getMainLooper())
    private val reportFullyDrawnRunnable = Runnable { if (!ready) reportFullyDrawn() }
    private var ready = false
    lateinit var playerBottomSheet: PlayerBottomSheet
        private set
    lateinit var bottomNavigationView: BottomNavigationView
        private set

    // Shows the download interstitial when a download finishes (mirrors the sample's AbsBaseActivity).
    // The giga service broadcasts DownloadsFragment.ACTION_UPDATE after the file is media-scanned.
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val configEntity = musicDownloaderApplication.configEntity
            if (configEntity.isAds_download) {
                InterstitialAdsUtils.showAdsGoToSearchScreen(
                    this@MainActivity, configEntity,
                    object : InterstitialAdsUtils.Listener {
                        override fun onNotShowAds() { maybeShowRateApp() }
                        override fun onAdDismissedFullScreenContent() {}
                    }
                )
            } else {
                maybeShowRateApp()
            }
        }
    }

    /**
     * After a download completes (and no interstitial took its place), offer the rate prompt.
     * [RateAppUtils] handles all the count/interval/already-rated gating internally.
     */
    private fun maybeShowRateApp() {
        RateAppUtils.updateShowRateAppCount(this, object : RateAppUtils.Listener {
            override fun showRateAppFirstTime() {
                if (isFinishing || supportFragmentManager.isStateSaved) return
                SatisfiedBottomSheet().show(supportFragmentManager, SatisfiedBottomSheet.TAG)
            }

            override fun showRateApp() {
                if (isFinishing || supportFragmentManager.isStateSaved) return
                RateAppBottomSheet.newInstance(true)
                    .show(supportFragmentManager, RateAppBottomSheet.TAG)
            }
        })
    }

    /** The bottom bar = navigation + home banner ad stacked vertically (banner under the nav). */
    lateinit var bottomBar: View
        private set
    private var homeBannerAdView: AdView? = null
    private var homeBannerView: TUBannerView? = null

    /**
     * The full on-screen height of the bottom bar (navigation + banner), INCLUDING its bottom
     * system-inset padding. Content and the mini player reserve exactly this much space so they sit
     * flush on top of the bar. Falls back to the intrinsic nav dimen before the bar has been laid out.
     */
    val bottomNavHeight: Int
        get() = if (::bottomBar.isInitialized && bottomBar.height > 0)
            bottomBar.height
        else resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
    private lateinit var intentSenderDelete: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var addToPlaylistIntentSender: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var markIsFavoriteStatusIntentSender: ActivityResultLauncher<IntentSenderRequest>
    private var pendingPlaylistRequest: Bundle? = null
    private var pendingDeleteRequest: Bundle? = null
    private var pendingMarkIsFavoriteRequest: Bundle? = null

    fun updateLibrary(smartScanFirst: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                      then: (() -> Unit)? = null) {
        // If library load takes more than 2s, exit splash to avoid ANR
        if (!ready) handler.postDelayed(reportFullyDrawnRunnable, 2000)
        CoroutineScope(Dispatchers.Default).launch {
            if (smartScanFirst)
                MediaStoreCompat.smartScan(this@MainActivity.musicDownloaderApplication)
            this@MainActivity.musicDownloaderApplication.reader.refresh()
            withContext(Dispatchers.Main) {
                onLibraryLoaded()
                then?.let { it() }
            }
        }
    }

    /**
     * onCreate - core of MainActivity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("MainActivity", "onCreate($intent)")
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(controllerViewModel)
        enableEdgeToEdgeProperly()
        if (savedInstanceState?.containsKey("AddToPlaylistPendingRequest") == true) {
            pendingPlaylistRequest = savedInstanceState.getBundle("AddToPlaylistPendingRequest")
        }
        if (savedInstanceState?.containsKey("DeletePendingRequest") == true) {
            pendingDeleteRequest = savedInstanceState.getBundle("DeletePendingRequest")
        }
        if (savedInstanceState?.containsKey("pendingMarkIsFavoriteRequest") == true) {
            pendingMarkIsFavoriteRequest = savedInstanceState.getBundle("pendingMarkIsFavoriteRequest")
        }
        intentSenderDelete =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                val req = pendingDeleteRequest
                    ?: throw IllegalStateException("pending delete request is null")
                pendingDeleteRequest = null
                CoroutineScope(Dispatchers.Default).launch {
                    ItemManipulator.continueDeleteFromPendingIntent(this@MainActivity, it.resultCode, it.data, req)
                }
            }
        addToPlaylistIntentSender =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                val req = pendingPlaylistRequest
                    ?: throw IllegalStateException("pending playlist add request is null")
                pendingPlaylistRequest = null
                CoroutineScope(Dispatchers.Default).launch {
                    doAddToPlaylist(it.resultCode, req)
                }
            }
        markIsFavoriteStatusIntentSender =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                val req = pendingMarkIsFavoriteRequest
                    ?: throw IllegalStateException("pending favorite request is null")
                pendingMarkIsFavoriteRequest = null
                CoroutineScope(Dispatchers.Default).launch {
                    doMarkIsFavoriteStatus(it.resultCode, req)
                }
            }
        // TODO: should Activity.setMediaController() or Activity.setVolumeControlStream() be
        //  called? latter will probably not do particularly much, and former will
        //  forward events to our session no matter whether it makes sense or not to currently
        //  handle volume there... but it's still better than not getting the key events I guess?

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)
                if (fm.fragments.lastOrNull() != f) return
                // this won't be called in case we show()/hide() so
                // we handle that case in BaseFragment
                if (f is BaseFragment && f.wantsPlayer != null) {
                    playerBottomSheet.visible = f.wantsPlayer
                }
                updateBottomNavVisibility(f)
            }
        }, false)

        // Set content Views.
        setContentView(R.layout.activity_main)
        // Bind the giga download service so online downloads can start from the search screen.
        DownloadController.bind(this)
        playerBottomSheet = findViewById(R.id.player_layout)
        bottomNavigationView = findViewById(R.id.bottom_nav)
        bottomBar = findViewById(R.id.bottom_bar)
        // Load the home banner ad into the bottom bar (mirrors the sample's home banner).
        loadHomeBanner()
        // Ask for confirmation before leaving the app (mirrors MSDownloader10's exit bottom sheet).
        // Only active at the navigation root: while there is a sub-fragment on the back stack this
        // callback is disabled so the FragmentManager pops it (keeping predictive back), and the
        // player sheet's own callback still takes precedence when it is expanded.
        val exitCallback = object : OnBackPressedCallback(supportFragmentManager.backStackEntryCount == 0) {
            override fun handleOnBackPressed() {
                showExit()
            }
        }
        onBackPressedDispatcher.addCallback(this, exitCallback)
        supportFragmentManager.addOnBackStackChangedListener {
            exitCallback.isEnabled = supportFragmentManager.backStackEntryCount == 0
        }
        // Pad the whole bottom bar above the system navigation bar / gesture area (the banner is the
        // lowest child, so the inset lives on the bar), but don't consume the insets so the content
        // and the player bottom sheet still see them.
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar) { v, insets ->
            val bottom = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            ).bottom
            v.updatePadding(bottom = bottom)
            insets
        }
        // When the bar's height changes (system inset appears/disappears, or the banner loads/hides)
        // re-dispatch insets so the content and mini player re-reserve the correct amount of space.
        bottomBar.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) {
                ViewCompat.getRootWindowInsets(window.decorView)?.let {
                    ViewCompat.dispatchApplyWindowInsets(window.decorView, it.clone())
                }
            }
        }

        // Check all permissions.
        if (!hasAudioPermission()) {
            // Ask if was denied. Append POST_NOTIFICATIONS (Android 13+) as an optional permission
            // so it's requested together with the media permission at first launch — mirrors the
            // sample, which bundles it as an "optional permission" onto its media request. The media
            // (read) permission stays first so grantResults[0] in onRequestPermissionsResult is it.
            val perms = (
                if (hasScopedStorageWithMediaTypes())
                    arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
                else if (hasScopedStorageV2())
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                else
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
            ).toMutableList()
            if (supportsNotificationPermission() && !hasNotificationPermission()) {
                perms += android.Manifest.permission.POST_NOTIFICATIONS
            }
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), PERMISSION_READ_MEDIA_AUDIO)
        } else {
            // Audio already granted (e.g. returning user): the bundled request above never runs, so
            // ask for the (optional) notification permission on its own if it's still missing.
            if (supportsNotificationPermission() && !hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_POST_NOTIFICATIONS,
                )
            }
            // If all permissions are granted, we can update library now.
            if (!this@MainActivity.reader.hadFirstRefresh) {
                updateLibrary()
            } else onLibraryLoaded() // <-- when recreating activity due to rotation
        }
        // ViewPagerFragment will call reportFullyDrawn itself, for every other fragment we'll handle it
        // (this will happen on activity recreation with non-empty fragment backstack)
        if (supportFragmentManager.findFragmentById(R.id.fragment_viewpager) !is ViewPagerFragment)
            handler.post { maybeReportFullyDrawn() }
    }

    @OptIn(FlowPreview::class, InternalCoroutinesApi::class)
    fun addToPlaylistDialog(item: MediaItem) {
        val song = Entry.ofMediaItem(item)
        if (song == null) {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.edit_playlist_failed, "song == null"),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        lifecycleScope.launch(Dispatchers.Default) {
            val job = async(start = CoroutineStart.UNDISPATCHED) {
                reader.playlistListFlow.first().filter { it.title != null }
            }
            val maybeValue = withTimeoutOrNull(300.milliseconds) {
                job.await()
            }
            val playlists = maybeValue ?: run {
                launch(Dispatchers.Main) {
                    withContext(NonCancellable) {
                        val progressBar = ProgressBar(this@MainActivity)
                        val padding = 20.dpToPx(this@MainActivity)
                        progressBar.isIndeterminate = true
                        progressBar.setPadding(0, padding / 2, 0, padding)
                        val d = MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle(R.string.loading_playlists)
                            .setView(progressBar)
                            .setCancelable(false)
                            .show()
                        job.invokeOnCompletion {
                            launch(Dispatchers.Main, start = CoroutineStart.ATOMIC) {
                                withContext(NonCancellable) {
                                    d.dismiss()
                                }
                            }
                        }
                    }
                }
                job.await()
            }
            launch(Dispatchers.Main) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.add_to_playlist)
                    .setIcon(R.drawable.ic_playlist_play)
                    .setItems((playlists.map {
                        if (it is Favorite) getString(R.string.playlist_favourite) else
                            it.title ?: it.path?.absolutePath ?: it.id.toString()
                    } + getString(R.string.create_playlist)).toTypedArray())
                    { _, item ->
                        if (playlists.size == item) {
                            PlaylistAdapter.playlistNameDialog(this@MainActivity,
                                R.string.create_playlist, "",
                                { ItemManipulator.getDefaultPlaylistFile(it) }) { name ->
                                addToPlaylist(null, name, listOf(song))
                            }
                            return@setItems
                        }
                        val pl = playlists[item]
                        addToPlaylist(
                            ContentUris.withAppendedId(
                                @Suppress("deprecation") MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                pl.id!!
                            ), null, listOf(song)
                        )
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
        }
    }

    fun addToPlaylist(uri: Uri?, name: File?, songs: List<Entry>) {
        val data = Bundle().apply {
            putParcelableArrayList("Songs", ArrayList(songs))
            putParcelable("Uri", uri)
            putString("Name", name?.path)
        }
        CoroutineScope(Dispatchers.Default).launch {
            val token = if (uri != null) {
                MediaStoreCompat.needRequestBytesWrite(this@MainActivity, uri)
            } else {
                MediaStoreCompat.needRequestCreate(this@MainActivity,
                    name!!.path)
            }
            if (token != null) {
                pendingPlaylistRequest = data
                val pendingIntent = MediaStoreCompat.createWriteRequest(this@MainActivity,
                    listOf(token))
                addToPlaylistIntentSender.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } else {
                doAddToPlaylist(RESULT_OK, data)
            }
        }
    }

    fun markIsFavoriteStatus(songs: List<Entry>, favorite: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            val uri = musicDownloaderApplication.reader.playlistListFlow.map { it.find { p -> p is
                    Favorite } }.first()?.id?.let {
                    ContentUris.withAppendedId(@Suppress("deprecation")
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, it)
                }
            val data = Bundle().apply {
                putParcelableArrayList("Songs", ArrayList(songs))
                putParcelable("Uri", uri)
                putBoolean("Favorite", favorite)
            }
            val token = if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStoreCompat.needRequestAdoption(this@MainActivity, uri)
            } else if (uri != null) {
                MediaStoreCompat.needRequestBytesWrite(this@MainActivity, uri)
            } else {
                MediaStoreCompat.needRequestCreate(this@MainActivity,
                    ItemManipulator.getDefaultPlaylistFile(
                        ItemManipulator.FAVORITES).path)
            }
            if (token != null) {
                pendingMarkIsFavoriteRequest = data
                val pendingIntent = MediaStoreCompat.createWriteRequest(this@MainActivity,
                    listOf(token))
                markIsFavoriteStatusIntentSender.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } else {
                doMarkIsFavoriteStatus(RESULT_OK, data)
            }
        }
    }

    fun runIntentForDelete(intent: IntentSender, bundle: Bundle) {
        try {
            intentSenderDelete.launch(IntentSenderRequest.Builder(intent).build())
            pendingDeleteRequest = bundle
        } catch (e: ActivityNotFoundException) {
            Log.e("MainActivity", "error launching intent", e)
            CoroutineScope(Dispatchers.Default).launch {
                ItemManipulator.continueDeleteFromPendingIntent(
                    this@MainActivity, RESULT_FIRST_USER,
                    null, bundle
                )
            }
        }
    }

    private suspend fun doMarkIsFavoriteStatus(resultCode: Int, data: Bundle) {
        if (resultCode == RESULT_OK) {
            var uriIn = BundleCompat.getParcelable(data, "Uri", Uri::class.java)
            if (uriIn != null) {
                uriIn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    MediaStoreCompat.adoptFile(this@MainActivity, uriIn)
                } else uriIn
            }
            val songs = BundleCompat.getParcelableArrayList(data, "Songs",
                Entry::class.java)!!
            val favorite = data.getBoolean("Favorite")
            try {
                val uri = uriIn ?: ItemManipulator.createPlaylist(this,
                    ItemManipulator.getDefaultPlaylistFile(ItemManipulator.FAVORITES))
                val readback = if (uriIn != null) ItemManipulator.readbackPlaylist(this,
                    uri) else PlaylistSerializer.Playlist.create()
                val newSongs = readback.copy(entries = if (favorite) {
                    readback.entries + songs
                } else {
                    readback.entries.filter { songs.find { candidate -> candidate.fuzzyEquals(it) } == null }
                })
                ItemManipulator.setPlaylistContent(this, uri, newSongs,
                    uriIn == null)
            } catch (e: Exception) {
                Log.e("MainActivity", Log.getThrowableString(e)!!)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(
                            R.string.edit_favorites_failed,
                            e.javaClass.name + ": " + e.message
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.edit_favorites_failed, "$resultCode"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun doAddToPlaylist(resultCode: Int, data: Bundle) {
        val uriIn = BundleCompat.getParcelable(data, "Uri", Uri::class.java)
        val errorString = if (uriIn != null) R.string.edit_playlist_failed else
            R.string.create_failed_playlist
        if (resultCode == RESULT_OK) {
            val name = if (uriIn == null) data.getString("Name")!! else null
            val songs = BundleCompat.getParcelableArrayList(data, "Songs",
                Entry::class.java)!!
            try {
                val readback = if (uriIn != null) ItemManipulator.readbackPlaylist(this,
                    uriIn) else PlaylistSerializer.Playlist.create()
                val uri = uriIn ?: ItemManipulator.createPlaylist(this,
                    File(name!!))
                ItemManipulator.setPlaylistContent(this, uri, readback
                    .copy(entries = readback.entries + songs), uriIn == null)
            } catch (e: Exception) {
                Log.e("MainActivity", Log.getThrowableString(e)!!)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(
                            errorString,
                            e.javaClass.name + ": " + e.message
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    getString(errorString, "$resultCode"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (pendingPlaylistRequest != null) {
            outState.putBundle("AddToPlaylistPendingRequest", pendingPlaylistRequest)
        }
        if (pendingDeleteRequest != null) {
            outState.putBundle("DeletePendingRequest", pendingDeleteRequest)
        }
        if (pendingMarkIsFavoriteRequest != null) {
            outState.putBundle("pendingMarkIsFavoriteRequest", pendingMarkIsFavoriteRequest)
        }
    }

    override fun onNewIntent(intent: Intent) {
        Log.i("MainActivity", "onNewIntent($intent)")
        super.onNewIntent(intent)
        if (ready) {
            doPlayFromIntent(intent)
        }
    }

    private fun doPlayFromIntent(intent: Intent) {
        Log.i("MainActivity", "doPlayFromIntent($intent)")
        val autoPlayId = intent.extras?.getString(PLAYBACK_AUTO_PLAY_ID) ?: (if
                (intent.action == "com.musicdownloader.musicfreeapp825v2.action.PLAY_MEDIA_FROM_SUGGESTION")
                intent.data?.let {
                    try {
                        ContentUris.parseId(it)
                        "MediaStore:${it.lastPathSegment}"
                    } catch (_: NumberFormatException) {
                        null
                    } } else null)
        var willAutoPlayLater = false
        autoPlayId?.let { id ->
            willAutoPlayLater = true
            val pos =
                intent.extras?.getLong(PLAYBACK_AUTO_PLAY_POSITION, C.TIME_UNSET) ?: C.TIME_UNSET
            controllerViewModel.addControllerCallback(lifecycle) { controller, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.Default) {
                        val col = reader.idMapFlow.firstOrNull()
                        val item = id.toLongOrNull()?.let { col?.let { it2 -> it2[it] } }
                        if (item == null) {
                            Log.e(
                                "MainActivity",
                                "can't find file with ID $id in library with ${col?.size} items"
                            )
                        }
                        item
                    }.let { mediaItem ->
                        if (mediaItem != null) {
                            controller.setMediaItem(mediaItem, pos)
                            controller.prepare()
                            controller.play()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.cannot_find_file,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                dispose()
            }
        }
        IntentCompat.getParcelableExtra(intent, FAVORITE_ENTRY, Entry::class.java)
            ?.let {
                val state = intent.getBooleanExtra(FAVORITE_STATE, false)
                markIsFavoriteStatus(listOf(it), state)
            }
        if (intent.action == Intent.ACTION_VIEW) {
            val id = intent.getStringExtra("playlist")?.toLongOrNull()
            if (id != null) {
                startFragment(GeneralSubFragment()) {
                    putString("Id", id.toString())
                    putInt("Item", R.id.playlist)
                }
            }
        }
        if (intent.action == Intent.ACTION_SEARCH ||
            intent.action == "com.google.android.gms.actions.SEARCH_ACTION") {
            startFragment(SearchFragment()) {
                putString("query", intent.getStringExtra(SearchManager.QUERY))
            }
        }
        if (intent.action == MediaStore.INTENT_ACTION_MEDIA_SEARCH
            || intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
            // https://developer.android.com/media/implement/assistant#declare_legacy_support_for_voice_actions
            // https://android-developers.googleblog.com/2010/09/supporting-new-music-voice-action.html
            // https://developer.android.com/guide/components/intents-common#PlaySearch
            var focus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS)
                ?: ContentResolver.ANY_CURSOR_ITEM_TYPE
            // Validate all extras before sending them to service.
            if (focus != ContentResolver.ANY_CURSOR_ITEM_TYPE &&
                focus != MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE &&
                focus != MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE &&
                focus != MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE &&
                focus != MediaStore.Audio.Media.ENTRY_CONTENT_TYPE &&
                focus != (@Suppress("deprecation") MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)) {
                Log.w("MainActivity", "unsupported focus " +
                        intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS))
                focus = ContentResolver.ANY_CURSOR_ITEM_TYPE
            }
            val mainQuery: String?
            val subQueries = Bundle()
            subQueries.putString(MediaStore.EXTRA_MEDIA_FOCUS, focus)
            when (focus) {
                MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                    mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)
                        ?: intent.getStringExtra(SearchManager.QUERY)
                }
                MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                    mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)
                        ?: intent.getStringExtra(SearchManager.QUERY)
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                    }
                }
                MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                    mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM)
                        ?: intent.getStringExtra(SearchManager.QUERY)
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_ARTIST, it)
                    }
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                    }
                }
                MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                    mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE)
                        ?: intent.getStringExtra(SearchManager.QUERY)
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_ALBUM, it)
                    }
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_ARTIST, it)
                    }
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                    }
                }
                @Suppress("deprecation") MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> {
                    mainQuery = @Suppress("deprecation")
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_PLAYLIST)
                        ?: intent.getStringExtra(SearchManager.QUERY)
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_TITLE, it)
                    }
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_ALBUM, it)
                    }
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_ARTIST, it)
                    }
                    intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                        subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                    }
                }
                else -> mainQuery = intent.getStringExtra(SearchManager.QUERY)
            }
            if (intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                if (mainQuery != null) {
                    willAutoPlayLater = true
                    controllerViewModel.addControllerCallback(lifecycle) { controller, _ ->
                        controller.setMediaItem(
                            MediaItem.Builder()
                                .setRequestMetadata(
                                    MediaItem.RequestMetadata.Builder()
                                        .setSearchQuery(mainQuery) // may be empty
                                        .setExtras(subQueries)
                                        .build()
                                )
                                .build()
                        )
                        controller.prepare()
                        controller.play()
                        dispose()
                    }
                }
            } else {
                startFragment(SearchFragment()) {
                    putString("query", mainQuery)
                    // TODO: support sub queries or at least focus to use a different type of
                    //  search fragment.
                }
            }
        }
        if (intent.action == "com.musicdownloader.musicfreeapp825v2.action.SHUFFLE") {
            ShortcutManagerCompat.reportShortcutUsed(this@MainActivity, "shuffle_all")
            val query = intent.getStringExtra("item_name") ?: ""
            willAutoPlayLater = true
            controllerViewModel.addControllerCallback(lifecycle) { controller, _ ->
                controller.shuffleModeEnabled = true
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setRequestMetadata(
                            MediaItem.RequestMetadata.Builder()
                                .setSearchQuery(query) // empty = every song
                                .build()
                        )
                        .build()
                )
                controller.prepare()
                controller.play()
                dispose()
            }
        }
        val autoPlay = intent.extras?.getBoolean(PLAYBACK_AUTO_START_FOR_FGS, false) == true
                || intent.extras?.getBoolean(IntentCompat.EXTRA_START_PLAYBACK, false) == true
                || prefs.getBooleanStrict("autoplay", false)
        if (autoPlay && !willAutoPlayLater) {
            controllerViewModel.addControllerCallback(lifecycle) { controller, _ ->
                controller.prepare()
                controller.play()
                dispose()
            }
        }
    }

    override fun onSearchRequested(): Boolean {
        startFragment(SearchFragment())
        return true
    }

    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return onSearchRequested()
    }

    // https://twitter.com/Piwai/status/1529510076196630528
    override fun reportFullyDrawn() {
        handler.removeCallbacks(reportFullyDrawnRunnable)
        if (ready) throw IllegalStateException("ready is already true")
        ready = true
        Choreographer.getInstance().postFrameCallback {
            handler.postAtFrontOfQueueAsync {
                try {
                    super.reportFullyDrawn()
                } catch (e: SecurityException) {
                    // samsung SM-G570M on SDK 26: Permission Denial: broadcast from android asks to run as user
                    // -1 but is calling from user 0; this requires android.permission.INTERACT_ACROSS_USERS_FULL
                    // or android.permission.INTERACT_ACROSS_USERS
                    Log.w("MainActivity", "reportFullyDrawn failed", e)
                }
            }
        }
    }

    @RequiresApi(23)
    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)

        val instance = getPlayer()
        if (instance != null && outContent != null) {
            /* TODO implement schema.org MusicRecording creation here
            https://developer.android.com/training/articles/assistant
           outContent.structuredData = JSONObject()
                .put("@type", "MusicRecording")
                .put("@id", "https://example.com/music/recording")
                .put("name", "Song Title")
                .toString() */
            try {
                val item = instance.currentMediaItem
                val uri = item?.requestMetadata?.mediaUri
                    ?: item?.localConfiguration?.uri
                val strict = StrictMode.getThreadPolicy()
                try {
                    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
                    if (uri != null) {
                        outContent.clipData = ClipData.newUri(contentResolver,
                            item?.mediaMetadata?.title ?: "", uri)
                    }
                } finally {
                    StrictMode.setThreadPolicy(strict)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "unable to generate clip data", e)
            }
        }
    }

    fun onLibraryLoaded() {
        Log.i("MainActivity", "onLibraryLoaded()")
        doPlayFromIntent(intent)
    }

    fun maybeReportFullyDrawn() {
        if (!ready) reportFullyDrawn()
    }

    /**
     * onRequestPermissionResult:
     *   Update library after permission is granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_READ_MEDIA_AUDIO) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                updateLibrary()
            } else {
                maybeReportFullyDrawn() // TODO: is this still needed?
                Toast.makeText(this, getString(R.string.grant_audio), Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData("package:$packageName".toUri())
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Loads the home banner (AdMob adaptive / TopOn) into the bottom bar's ad container, shown only
     * after an ad loads. Mirrors the sample's home banner (gated by remote config). The banner rides
     * with the nav: it hides on sub-screens and when the player expands (the whole bar slides down).
     */
    private fun loadHomeBanner() {
        val container = findViewById<FrameLayout>(R.id.container_ads_home)
        val configEntity = musicDownloaderApplication.configEntity
        if (!configEntity.isAds_bannerHome) {
            container.visibility = View.GONE
            return
        }
        if (configEntity.isToponads) {
            val banner = TUBannerView(this)
            banner.setPlacementId(KeyTopOn.BANNER)
            banner.layoutParams = FrameLayout.LayoutParams(
                resources.displayMetrics.widthPixels, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            container.addView(banner)
            banner.setBannerAdListener(object : TUBannerListener {
                override fun onBannerLoaded() {
                    container.visibility = View.VISIBLE
                }

                override fun onBannerFailed(adError: com.thinkup.core.api.AdError?) {
                    Log.e("MainActivity", "home banner onBannerFailed: $adError")
                }

                override fun onBannerClicked(adInfo: TUAdInfo?) {}
                override fun onBannerShow(adInfo: TUAdInfo?) {}
                override fun onBannerClose(adInfo: TUAdInfo?) {}
                override fun onBannerAutoRefreshed(adInfo: TUAdInfo?) {}
                override fun onBannerAutoRefreshFail(adError: com.thinkup.core.api.AdError?) {}
            })
            banner.loadAd()
            homeBannerView = banner
        } else {
            val av = AdView(this)
            container.addView(av)
            av.adUnitId = KeyAdMob.BANNER
            av.setAdSize(adaptiveBannerSize())
            av.adListener = object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("MainActivity", "home banner onAdFailedToLoad: $adError")
                }

                override fun onAdLoaded() {
                    container.visibility = View.VISIBLE
                }
            }
            av.loadAd(AdRequest.Builder().build())
            homeBannerAdView = av
        }
    }

    /** Adaptive anchored banner size for the current width/orientation (as in the sample). */
    private fun adaptiveBannerSize(): AdSize {
        val metrics = resources.displayMetrics
        val adWidth = (metrics.widthPixels / metrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    /**
     * showExit:
     *   Confirm before leaving the app via a bottom sheet, ported from MSDownloader10.
     * "Cancel" dismisses; "Exit" sends the task to the background (leaving playback running,
     * same as the reference app's moveTaskToBack).
     */
    private fun showExit() {
        val bottomSheet = BottomSheetDialog(this, R.style.ThemeOverlay_App_BottomSheetDialog)
        val view = BottomSheetBackBinding.inflate(layoutInflater)
        bottomSheet.setContentView(view.root)
        view.root.padOutOfSystemBars()
        view.buttonCancel.setOnClickListener {
            bottomSheet.dismiss()
        }
        view.buttonOk.setOnClickListener {
            moveTaskToBack(true)
            bottomSheet.dismiss()
        }
        // Native exit ad, gated by remote config (mirrors the sample app's showExit()).
        val configEntity = musicDownloaderApplication.configEntity
        if (configEntity.isAds_nativeExitApp) {
            if (configEntity.isToponads) {
                var atNatives: TUNative? = null
                atNatives = TUNative(
                    this, KeyTopOn.NATIVE_EXIT,
                    object : TUNativeNetworkListener {
                        override fun onNativeAdLoaded() {
                            try {
                                setAdsNativeExit(atNatives!!, view)
                            } catch (e: Exception) {
                                FirebaseEventUtils.getInstances().recordException(e)
                            }
                        }

                        override fun onNativeAdLoadFail(p0: com.thinkup.core.api.AdError?) {
                            Log.e("MainActivity", "native exit onNativeAdLoadFail: $p0")
                        }
                    }
                )
                val padding = 10.dpToPx(this)
                val adViewWidth = resources.displayMetrics.widthPixels - 2 * padding
                val adViewHeight = 340.dpToPx(this) - 2 * padding
                val localMap: MutableMap<String, Any> = HashMap()
                localMap[TUAdConst.KEY.AD_WIDTH] = adViewWidth
                localMap[TUAdConst.KEY.AD_HEIGHT] = adViewHeight
                atNatives.setLocalExtra(localMap)
                atNatives.makeAdRequest()
            } else {
                val adLoader = AdLoader.Builder(this, KeyAdMob.NATIVE_EXIT)
                    .forNativeAd { ad: NativeAd ->
                        val templateView = NativeUtils.getNativeAd(this, ad, isDarkTheme())
                        view.cardContainer.removeAllViews()
                        view.cardContainer.addView(templateView)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.e("MainActivity", "native exit onAdFailedToLoad: $adError")
                        }
                    })
                    .withNativeAdOptions(NativeAdOptions.Builder().build())
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            }
        } else {
            view.cardContainer.visibility = View.GONE
        }
        bottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.show()
    }

    /** Renders a TopOn native ad into the exit sheet (self-render), ported from the sample. */
    private fun setAdsNativeExit(atNatives: TUNative, viewBinding: BottomSheetBackBinding) {
        val context = this
        val padding = 10.dpToPx(context)
        val adViewWidth = context.resources.displayMetrics.widthPixels - 2 * padding
        val anyThinkNativeAdView = TUNativeAdView(context)
        val nativeAd = atNatives.nativeAd
        anyThinkNativeAdView.removeAllViews()
        if (anyThinkNativeAdView.parent == null) {
            viewBinding.cardContainer.removeAllViews()
            viewBinding.cardContainer.addView(
                anyThinkNativeAdView,
                FrameLayout.LayoutParams(
                    adViewWidth,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        nativeAd?.setNativeEventListener(object : TUNativeEventListener {
            override fun onAdImpressed(tuNativeAdView: TUNativeAdView?, tuAdInfo: TUAdInfo?) {}
            override fun onAdClicked(tuNativeAdView: TUNativeAdView?, tuAdInfo: TUAdInfo?) {}
            override fun onAdVideoStart(tuNativeAdView: TUNativeAdView?) {}
            override fun onAdVideoEnd(tuNativeAdView: TUNativeAdView?) {}
            override fun onAdVideoProgress(tuNativeAdView: TUNativeAdView?, i: Int) {}
        })
        nativeAd?.setDislikeCallbackListener(object : TUNativeDislikeListener() {
            override fun onAdCloseButtonClick(tuNativeAdView: TUNativeAdView?, tuAdInfo: TUAdInfo?) {
                if (tuNativeAdView?.parent != null) {
                    (tuNativeAdView.parent as ViewGroup).removeView(tuNativeAdView)
                }
            }
        })
        val nativePrepareInfo = TUNativePrepareInfo()
        nativeAd?.renderAdContainer(anyThinkNativeAdView, null)
        val selfRenderView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_self, null, false)
        SelfRenderViewUtil.bindSelfRenderView(
            context, nativeAd?.adMaterial!!, selfRenderView, nativePrepareInfo, false
        )
        nativeAd.renderAdContainer(anyThinkNativeAdView, selfRenderView)
        nativeAd.prepare(anyThinkNativeAdView, nativePrepareInfo)
    }

    private fun isDarkTheme(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    /**
     * startFragment:
     *   Used by child fragments / drawer to start
     * a fragment inside MainActivity's fragment
     * scope.
     *
     * @param frag: Target fragment.
     */
    fun startFragment(frag: Fragment, args: (Bundle.() -> Unit)? = null) {
        supportFragmentManager.commit {
            addToBackStack(System.currentTimeMillis().toString())
            hide(supportFragmentManager.fragments.last())
            add(R.id.container, frag.apply { args?.let { arguments = Bundle().apply(it) } })
        }
    }

    /** The bottom navigation belongs to the main screen only; sub-screens hide it (nav slides down). */
    fun updateBottomNavVisibility(f: Fragment) {
        // Only the fragments swapped into the main container drive this — ignore dialogs / pager pages.
        if (f.id != R.id.container) return
        if (::playerBottomSheet.isInitialized) {
            playerBottomSheet.setSubScreenNavHidden(f !is ViewPagerFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this, downloadCompleteReceiver,
            IntentFilter(DownloadsFragment.ACTION_UPDATE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(downloadCompleteReceiver) }
    }

    override fun onDestroy() {
        homeBannerAdView?.destroy()
        homeBannerAdView = null
        homeBannerView?.destroy()
        homeBannerView = null
        // https://github.com/androidx/media/issues/805
        if (needsMissingOnDestroyCallWorkarounds()
            && (getPlayer()?.playWhenReady != true || getPlayer()?.mediaItemCount == 0)
        ) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
        }
        super.onDestroy()
        // we don't ever want covers to be the cause of service being killed by too high mem usage
        // (this is placed after super.onDestroy() to make sure all ImageViews are dead)
        imageLoader.memoryCache?.clear()
    }

    /**
     * getPlayer:
     *   Returns a media controller.
     */
    fun getPlayer() = controllerViewModel.get()

    inline val reader
        get() = musicDownloaderApplication.reader
}
