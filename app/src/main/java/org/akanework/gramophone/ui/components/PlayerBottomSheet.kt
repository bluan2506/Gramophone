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

package org.akanework.gramophone.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.session.MediaController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.motion.MaterialBottomContainerBackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.akanework.gramophone.BuildConfig
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.clone
import org.akanework.gramophone.logic.fadOutAnimation
import org.akanework.gramophone.logic.ui.MyBottomSheetBehavior
import org.akanework.gramophone.ui.MainActivity


class PlayerBottomSheet private constructor(
    context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes),
    Player.Listener, DefaultLifecycleObserver {
    constructor(context: Context, attributeSet: AttributeSet?)
            : this(context, attributeSet, 0, 0)

    companion object {
        private const val TAG = "PlayerBottomSheet"
        private const val SUB_SCREEN_ANIM_MS = 350L
    }

    private var standardBottomSheetBehavior: MyBottomSheetBehavior<FrameLayout>? = null

    @SuppressLint("RestrictedApi")
    private var lyricsBackHelper: MaterialBottomContainerBackHelper? = null
    private var bottomSheetBackCallback: OnBackPressedCallback? = null
    val fullPlayer: FullBottomSheet
    private val previewPlayer: View

    private val activity
        get() = context as MainActivity
    private val lifecycleOwner: LifecycleOwner
        get() = activity
    private val handler = Handler(Looper.getMainLooper())
    private val instance: MediaController?
        get() = activity.getPlayer()
    private val fragmentContainer: View by lazy { activity.findViewById(R.id.container)!! }
    private val lyricsFrame: View by lazy { activity.findViewById(R.id.lyric_framelayout)!! }
    private var lastActuallyVisible: Boolean? = null
    private var lastMeasuredHeight: Int? = null

    // Sub-screen state: on a non-main screen the nav and the collapsed mini-player slide down together
    // (glued) by the nav's height, so the mini-player ends up flush at the very bottom (this app hides
    // the system nav bar). Only translationY moves — no padding reflow.
    private var subScreenAnimating = false
    var subScreenNavHidden = false
        private set

    var visible = false
        set(value) {
            if (field != value) {
                field = value
                // TODO: this should be animated with predictive back once we switch to a better
                //  back stack.
                standardBottomSheetBehavior?.state =
                    if ((instance?.mediaItemCount ?: 0) > 0 && value) {
                        if (standardBottomSheetBehavior?.state
                            != BottomSheetBehavior.STATE_EXPANDED
                        )
                            BottomSheetBehavior.STATE_COLLAPSED
                        else BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        BottomSheetBehavior.STATE_HIDDEN
                    }
            }
        }
    val actuallyVisible: Boolean
        get() = standardBottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN
    val visibleAndExpanded: Boolean
        get() = standardBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED

    init {
        inflate(context, R.layout.bottom_sheet, this)
        previewPlayer = findViewById(R.id.preview_player)!!
        fullPlayer = findViewById(R.id.full_player)!!

        setOnClickListener { open() }

        activity.controllerViewModel.addRecreationalPlayerListener(activity.lifecycle, this) {
            onMediaItemTransition(
                instance?.currentMediaItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            )
        }
    }

    fun open() {
        if (standardBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
            standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private val bottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(
            bottomSheet: View,
            newState: Int,
        ) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    fullPlayer.visibilityDueToBottomSheet = GONE
                    previewPlayer.visibility = VISIBLE
                    fragmentContainer.visibility = VISIBLE
                    lyricsFrame.visibility = GONE
                    previewPlayer.alpha = 1f
                    fullPlayer.alpha = 0f
                    lyricsFrame.alpha = 0f
                    bottomSheetBackCallback!!.isEnabled = false
                }

                BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                    fullPlayer.visibilityDueToBottomSheet = VISIBLE
                    previewPlayer.visibility = VISIBLE
                    fragmentContainer.visibility = VISIBLE
                    lyricsFrame.visibility = VISIBLE
                }

                BottomSheetBehavior.STATE_EXPANDED -> {
                    previewPlayer.visibility = GONE
                    fullPlayer.visibilityDueToBottomSheet = VISIBLE
                    // TODO: uncomment fragmentContainer.visibility = INVISIBLE
                    lyricsFrame.visibility = VISIBLE
                    previewPlayer.alpha = 0f
                    fullPlayer.alpha = 1f
                    lyricsFrame.alpha = 1f
                    bottomSheetBackCallback!!.isEnabled = true
                }

                 BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    previewPlayer.visibility = GONE
                    fullPlayer.visibilityDueToBottomSheet = VISIBLE
                    fragmentContainer.visibility = VISIBLE
                    lyricsFrame.visibility = VISIBLE
                    previewPlayer.alpha = 0f
                    fullPlayer.alpha = 1f
                    lyricsFrame.alpha = 1f
                    bottomSheetBackCallback!!.isEnabled = true
                }

                BottomSheetBehavior.STATE_HIDDEN -> {
                    previewPlayer.visibility = GONE
                    fullPlayer.visibilityDueToBottomSheet = GONE
                    fragmentContainer.visibility = VISIBLE
                    lyricsFrame.visibility = GONE
                    previewPlayer.alpha = 0f
                    fullPlayer.alpha = 0f
                    lyricsFrame.alpha = 0f
                    bottomSheetBackCallback!!.isEnabled = false
                }
            }
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED,
                BottomSheetBehavior.STATE_HALF_EXPANDED -> updateBottomNavForOffset(1f)

                BottomSheetBehavior.STATE_COLLAPSED,
                BottomSheetBehavior.STATE_HIDDEN -> updateBottomNavForOffset(0f)
            }
            dispatchBottomSheetInsets()
        }

        override fun onSlide(
            bottomSheet: View,
            slideOffset: Float,
        ) {
            updateBottomNavForOffset(slideOffset)
            if (slideOffset < 0) {
                // hidden state
                previewPlayer.alpha = 1 - (-1 * slideOffset)
                fullPlayer.alpha = 0f
                lyricsFrame.alpha = 0f
                return
            }
            previewPlayer.alpha = 1 - (slideOffset)
            fullPlayer.alpha = slideOffset
            lyricsFrame.alpha = slideOffset
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnLayout { // wait for CoordinatorLayout to finish to allow getting behaviour
            standardBottomSheetBehavior = MyBottomSheetBehavior.from(this)
            fullPlayer.minimize = {
                standardBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            @SuppressLint("RestrictedApi")
            lyricsBackHelper =
                MaterialBottomContainerBackHelper(fullPlayer.bottomSheetFullLyricView)
            bottomSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) {
                    if (fullPlayer.bottomSheetFullLyricView.isVisible) {
                        @SuppressLint("RestrictedApi")
                        lyricsBackHelper!!.startBackProgress(backEvent)
                    } else {
                        standardBottomSheetBehavior!!.startBackProgress(backEvent)
                    }
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    if (fullPlayer.bottomSheetFullLyricView.isVisible) {
                        @SuppressLint("RestrictedApi")
                        lyricsBackHelper!!.updateBackProgress(backEvent)
                    } else {
                        standardBottomSheetBehavior!!.updateBackProgress(backEvent)
                    }
                }

                override fun handleOnBackPressed() {
                    if (fullPlayer.bottomSheetFullLyricView.isVisible) {
                        @SuppressLint("RestrictedApi")
                        val backEvent = lyricsBackHelper!!.onHandleBackInvoked()
                        if (backEvent == null || backEvent.progress == 0f
                            || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ) {
                            fullPlayer.bottomSheetFullLyricView.fadOutAnimation(FullBottomSheet.LYRIC_FADE_TRANSITION_SEC)
                            return
                        }
                        @SuppressLint("RestrictedApi")
                        lyricsBackHelper!!.finishBackProgressPersistent(
                            backEvent,
                            object : AnimatorListenerAdapter() {
                                override fun onAnimationStart(animation: Animator) {
                                    fullPlayer.bottomSheetFullLyricView.fadOutAnimation(FullBottomSheet.LYRIC_FADE_TRANSITION_SEC)
                                }
                            })
                    } else {
                        standardBottomSheetBehavior!!.handleBackInvoked()
                    }
                }

                override fun handleOnBackCancelled() {
                    if (fullPlayer.bottomSheetFullLyricView.isVisible) {
                        @SuppressLint("RestrictedApi")
                        lyricsBackHelper!!.cancelBackProgress()
                    } else {
                        standardBottomSheetBehavior!!.cancelBackProgress()
                    }
                }
            }
            /*
            lyricSheetBackCallback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackPressed() {
                    bottomSheetFullLyricRecyclerView.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                    bottomSheetFullLyricGradientViewUp.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                    bottomSheetFullLyricGradientViewDown.fadOutAnimation(LYRIC_FADE_TRANSITION_SEC)
                    bottomSheetLyricButton.isChecked = false
                    activity.onBackPressedDispatcher.addCallback(activity, bottomSheetBackCallback!!)
                    bottomSheetBackCallback!!.isEnabled = true
                }
            }

             */
            activity.onBackPressedDispatcher.addCallback(activity, bottomSheetBackCallback!!)
            standardBottomSheetBehavior!!.addBottomSheetCallback(bottomSheetCallback)
            // this is required after onRestoreSavedInstanceState() in BottomSheetBehaviour
            bottomSheetCallback.onStateChanged(this, standardBottomSheetBehavior!!.state)
            lifecycleOwner.lifecycle.addObserver(this)
            updatePeekHeight()
            dispatchBottomSheetInsets()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lastActuallyVisible = null
        lastMeasuredHeight = null
        fullPlayer.minimize = null
        lifecycleOwner.lifecycle.removeObserver(this)
        standardBottomSheetBehavior?.removeBottomSheetCallback(bottomSheetCallback)
        bottomSheetBackCallback?.remove()
        standardBottomSheetBehavior = null
        onStop(lifecycleOwner)
    }

    /**
     * Slides the persistent bottom navigation out of the way as the player expands. [offset] is the
     * bottom sheet slide offset: 0 = collapsed (nav fully shown), 1 = expanded (nav hidden below).
     */
    private fun updateBottomNavForOffset(offset: Float) {
        // While the sub-screen enter/exit animation runs, it owns the nav position — don't let a
        // peek-triggered re-settle snap the nav to its end state and cut the animation short.
        if (subScreenAnimating) return
        // Slide the whole bottom bar (nav + home banner) so the banner hides with the nav.
        val bar = activity.bottomBar
        // On a sub-screen the bar stays fully hidden regardless of the player slide.
        val progress = maxOf(offset.coerceIn(0f, 1f), if (subScreenNavHidden) 1f else 0f)
        val height = bar.height.takeIf { it > 0 } ?: activity.bottomNavHeight
        bar.translationY = height.toFloat() * progress
        bar.alpha = 1f - progress
    }

    /**
     * Called by MainActivity when the top container fragment changes. On a sub-screen the bottom nav
     * slides down and the collapsed mini-player slides down flush to the very bottom; back on the main
     * screen both slide back up. The nav and the mini-player animate together (same duration).
     */
    fun setSubScreenNavHidden(hidden: Boolean) {
        if (subScreenNavHidden == hidden) return
        subScreenNavHidden = hidden
        subScreenAnimating = true

        // Slide the whole bottom bar (nav + home banner) so the banner hides with the nav.
        val bar = activity.bottomBar
        val barHeight = (bar.height.takeIf { it > 0 } ?: activity.bottomNavHeight).toFloat()

        // Shrink/grow the collapsed peek to its target (flush on a sub-screen) + re-pad content. The
        // mini-player content is vertically centred so this does NOT reflow it — it only removes the
        // reserved strip so there's no grey block above the mini-player. subScreenAnimating blocks the
        // nav-snap this re-settle would otherwise trigger.
        val oldPeek = previewPlayer.measuredHeight
        previewPlayer.setPadding(
            previewPlayer.paddingLeft, 0, previewPlayer.paddingRight,
            if (hidden) 0 else activity.bottomNavHeight
        )
        updatePeekHeight()
        dispatchBottomSheetInsets()
        val newPeek = previewPlayer.measuredHeight

        // Slide the bar and the mini-player down/up together by the same amount (glued). The peek
        // already snapped the mini-player to the new spot, so start it from the old spot and animate.
        bar.animate().cancel()
        bar.animate()
            .translationY(if (hidden) barHeight else 0f)
            .alpha(if (hidden) 0f else 1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(SUB_SCREEN_ANIM_MS)
            .withEndAction { subScreenAnimating = false }
            .start()

        previewPlayer.animate().cancel()
        previewPlayer.translationY = (newPeek - oldPeek).toFloat()
        previewPlayer.animate()
            .translationY(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(SUB_SCREEN_ANIM_MS)
            .start()
    }

    private fun updatePeekHeight() {
        previewPlayer.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.UNSPECIFIED
        )
        standardBottomSheetBehavior?.setPeekHeight(previewPlayer.measuredHeight, false)
    }

    fun getBottomPadding() = if (lastActuallyVisible == true) lastMeasuredHeight ?: 0 else 0

    private fun dispatchBottomSheetInsets() {
        if (lastMeasuredHeight == previewPlayer.measuredHeight &&
            lastActuallyVisible == actuallyVisible
        ) return
        if (BuildConfig.DEBUG) Log.i(TAG, "dispatching bottom sheet insets")
        lastMeasuredHeight = previewPlayer.measuredHeight
        lastActuallyVisible = actuallyVisible
        // This dispatches the last known insets again to force regeneration of
        // FragmentContainerView's insets which will in turn call generateBottomSheetInsets().
        val i = ViewCompat.getRootWindowInsets(activity.window.decorView)
        if (i != null) {
            ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, i.clone())
        } else Log.e(TAG, "getRootWindowInsets returned null, this should NEVER happen")
    }

    override fun dispatchApplyWindowInsets(platformInsets: WindowInsets): WindowInsets {
        val insets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
        val myInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )
        // We here have to set up inset padding manually as the bottom sheet won't know what
        // View is behind the status bar, paddingTopSystemWindowInsets just allows it to go
        // behind it, which differs from the other padding*SystemWindowInsets. We can't use the
        // other padding*SystemWindowInsets to apply systemBars() because previewPlayer and
        // fullPlayer should extend into system bars AND display cutout. previewPlayer can't use
        // fitsSystemWindows because it doesn't want top padding from status bar.
        // We have to do it manually, duh.
        // Lift the collapsed mini player so it floats directly above the bottom navigation bar
        // instead of overlapping it. bottomNavHeight already includes the system inset, and the
        // nav bar draws on top of the empty strip left behind.
        previewPlayer.setPadding(
            myInsets.left, 0, myInsets.right,
            if (subScreenNavHidden) 0 else activity.bottomNavHeight
        )
        // Let fullPlayer handle insets itself (and discard result as it's irrelevant to hierarchy)
        ViewCompat.dispatchApplyWindowInsets(fullPlayer, insets.clone())
        // Same for lyrics view
        ViewCompat.dispatchApplyWindowInsets(fullPlayer.bottomSheetFullLyricView, insets.clone())
        // Now make sure BottomSheetBehaviour has the correct View height set.
        if (isLaidOut && !isLayoutRequested) {
            updatePeekHeight()
        } else {
            doOnNextLayout {
                updatePeekHeight()
                dispatchBottomSheetInsets()
            }
        }
        val i = insets.getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )
        return WindowInsetsCompat.Builder(insets)
            .setInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout(), Insets.of(0, myInsets.top, 0, 0)
            )
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout(), Insets.of(0, i.top, 0, 0)
            )
            .build()
            .toWindowInsets()!!
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        val shouldBeHidden = (instance?.mediaItemCount ?: 0) <= 0 || !visible
        CoroutineScope(Dispatchers.Main).launch {
            while (standardBottomSheetBehavior?.state == BottomSheetBehavior.STATE_SETTLING) {
                delay(100)
            }
            doOnLayout {
                val state = standardBottomSheetBehavior?.state
                val newState = if (!shouldBeHidden) {
                    if (state != BottomSheetBehavior.STATE_EXPANDED
                        && state != BottomSheetBehavior.STATE_COLLAPSED) {
                        BottomSheetBehavior.STATE_COLLAPSED
                    } else null
                } else {
                    BottomSheetBehavior.STATE_HIDDEN
                }
                // if we are destroyed after onMediaItemTransition but before this runs,
                // standardBottomSheetBehavior will be null
                if (newState != null) {
                    standardBottomSheetBehavior?.state = newState
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        fullPlayer.onStop()
    }
}