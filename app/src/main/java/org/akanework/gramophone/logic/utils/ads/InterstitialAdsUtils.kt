package org.akanework.gramophone.logic.utils.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.Window
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.akanework.gramophone.databinding.DialogLoadingBinding
import org.akanework.gramophone.logic.lockOrientation
import com.thinkup.core.api.TUAdInfo
import com.thinkup.interstitial.api.TUInterstitial
import com.thinkup.interstitial.api.TUInterstitialListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.utils.config.ConfigEntity
import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils

object InterstitialAdsUtils {

    interface Listener {

        fun onNotShowAds()

        fun onAdDismissedFullScreenContent()
    }

    private const val TAG = "MyAppInterstitialAdsUtils"

    private const val SHOW_PROGRESS_DIALOG_DELAY = 1200L

    /**
     * Builds the "Ads loading..." dialog centered on screen with a transparent window (so only the
     * rounded box shows, floating in the middle) and no title bar. Non-cancelable.
     */
    private fun createLoadingDialog(activity: Activity): Dialog {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogLoadingBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }
        return dialog
    }

    private const val SHOW_INTERSTITIAL_ADS_INTERVAL = 40000

    private var lastTimeShowAdsFull = 0L
    private var mInterstitialAdGoToSearchScreen: InterstitialAd? = null
    private var mATInterstitialAdGoToSearchScreen: TUInterstitial? = null
    var isShowAdsGoToSearchScreen = false

    var isShowAdsWelcome = false

    fun init() {
        isShowAdsGoToSearchScreen = false
        isShowAdsWelcome = false
    }

    fun getAdsWelcome(activity: Activity, configEntity: ConfigEntity, callBack: () -> Unit) {
        Log.d(TAG, "getAdsWelcome")

        val handlerException = CoroutineExceptionHandler { _, throwable ->
            FirebaseEventUtils.getInstances().recordException(throwable)
        }
        CoroutineScope(Dispatchers.IO + SupervisorJob() + handlerException).launch {
            if (configEntity.isAds_welcome) {
                if (configEntity.isToponads) {
                    showTopOnWelcome(activity, callBack)
                } else {
                    showAdMobWelcome(activity, callBack)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callBack()
                }
            }
        }
    }

    private suspend fun showAdMobWelcome(activity: Activity, callBack: () -> Unit) {
        Log.d(TAG, "showAdMobWelcome")

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeShowAdsFull < SHOW_INTERSTITIAL_ADS_INTERVAL) {
            withContext(Dispatchers.Main) {
                callBack()
            }
            Log.d(TAG, "return interval")
            return
        }
        withContext(Dispatchers.Main) {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                activity,
                KeyAdMob.INTERSTITIAL_WELCOME,
                adRequest,
                object : InterstitialAdLoadCallback() {

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "onAdFailedToLoad: $adError")
                        callBack()
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "onAdLoaded")

                        if (activity.isFinishing) {
                            Log.d(TAG, "return isFinishing")
                            return
                        }

                        interstitialAd.setOnPaidEventListener { adValue ->
//                            LogAdsRevenueUtils.logAdRevenueEvent(
//                                activity,
//                                adValue,
//                                interstitialAd.responseInfo,
//                                LogAdsRevenueUtils.AdFormat.INTERSTITIAL,
//                                LogAdsRevenueUtils.AdPosition.INTERSTITIAL_WELCOME
//                            )
                        }

                        interstitialAd.fullScreenContentCallback =
                            object : FullScreenContentCallback() {

                                override fun onAdClicked() {
                                    Log.d(TAG, "onAdClicked")
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    Log.d(TAG, "onAdDismissedFullScreenContent")
                                    lastTimeShowAdsFull = System.currentTimeMillis()
                                    isShowAdsWelcome = false
                                    callBack()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    Log.e(TAG, "onAdFailedToShowFullScreenContent: $adError")
                                    isShowAdsWelcome = false
                                    callBack()
                                }

                                override fun onAdImpression() {
                                    Log.d(TAG, "onAdImpression")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    Log.d(TAG, "onAdShowedFullScreenContent")
                                    isShowAdsWelcome = true
                                }
                            }
                        interstitialAd.show(activity)
                    }
                }
            )
        }
    }

    private suspend fun showTopOnWelcome(activity: Activity, callBack: () -> Unit) {
        Log.d(TAG, "showTopOnWelcome")

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeShowAdsFull < SHOW_INTERSTITIAL_ADS_INTERVAL) {
            withContext(Dispatchers.Main) {
                callBack()
            }
            Log.d(TAG, "return interval")
            return
        }
        withContext(Dispatchers.Main) {
            val mATInterstitial = TUInterstitial(activity, KeyTopOn.INTERSTITIAL_WELCOME)
            mATInterstitial.setAdListener(object : TUInterstitialListener {

                override fun onInterstitialAdLoaded() {
                    Log.d(TAG, "onInterstitialAdLoaded")
                    if (!activity.isFinishing) {
                        mATInterstitial.show(activity)
                    }
                }

                override fun onInterstitialAdLoadFail(adError: com.thinkup.core.api.AdError?) {
                    Log.e(TAG, "onInterstitialAdLoadFail: $adError")
                    callBack()
                }

                override fun onInterstitialAdClicked(tuAdInfo: TUAdInfo?) {
                    Log.d(TAG, "onInterstitialAdClicked")
                }

                override fun onInterstitialAdShow(tuAdInfo: TUAdInfo?) {
                    Log.d(TAG, "onInterstitialAdShow")
                    isShowAdsWelcome = true
                }

                override fun onInterstitialAdClose(tuAdInfo: TUAdInfo?) {
                    Log.d(TAG, "onInterstitialAdClose")
                    lastTimeShowAdsFull = System.currentTimeMillis()
                    isShowAdsWelcome = false
                    callBack()
                }

                override fun onInterstitialAdVideoStart(tuAdInfo: TUAdInfo?) {
                    Log.d(TAG, "onInterstitialAdVideoStart")
                }

                override fun onInterstitialAdVideoEnd(tuAdInfo: TUAdInfo?) {
                    Log.d(TAG, "onInterstitialAdVideoEnd")
                }

                override fun onInterstitialAdVideoError(adError: com.thinkup.core.api.AdError?) {
                    Log.e(TAG, "onInterstitialAdVideoError: $adError")
                    isShowAdsWelcome = false
                    callBack()
                }
            })
            mATInterstitial.load()
        }
    }

    fun loadAdsGoToSearchScreen(activity: Activity, configEntity: ConfigEntity) {
        if (configEntity.isToponads) {
            loadTopOnGoToSearchScreen(activity)
        } else {
            loadAdMobGoToSearchScreen(activity)
        }
    }

    private fun loadAdMobGoToSearchScreen(activity: Activity) {
        Log.d(TAG, "loadAdMobGoToSearchScreen")

        if (mInterstitialAdGoToSearchScreen != null) {
            Log.d(TAG, "mInterstitialAdGoToSearchScreen == null")
            return
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            KeyAdMob.INTERSTITIAL_GO_TO_SEARCH_SCREEN,
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "onAdFailedToLoad: $adError")
                    mInterstitialAdGoToSearchScreen = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "onAdLoaded")
                    mInterstitialAdGoToSearchScreen = interstitialAd
                }
            }
        )
    }

    private fun loadTopOnGoToSearchScreen(activity: Activity) {
        Log.d(TAG, "loadTopOnGoToSearchScreen")

        if (mATInterstitialAdGoToSearchScreen != null) {
            Log.d(TAG, "mATInterstitialAdGoToSearchScreen == null")
            return
        }

        val mATInterstitial = TUInterstitial(activity, KeyTopOn.INTERSTITIAL_GO_TO_SEARCH_SCREEN)
        mATInterstitial.setAdListener(object : TUInterstitialListener {

            override fun onInterstitialAdLoaded() {
                Log.d(TAG, "onInterstitialAdLoaded")
                mATInterstitialAdGoToSearchScreen = mATInterstitial
            }

            override fun onInterstitialAdLoadFail(adError: com.thinkup.core.api.AdError?) {
                Log.e(TAG, "onInterstitialAdLoadFail: $adError")
                mATInterstitialAdGoToSearchScreen = null
            }

            override fun onInterstitialAdClicked(tuAdInfo: TUAdInfo?) {
                Log.d(TAG, "onInterstitialAdClicked")
            }

            override fun onInterstitialAdShow(tuAdInfo: TUAdInfo?) {
                Log.d(TAG, "onInterstitialAdShow")
            }

            override fun onInterstitialAdClose(tuAdInfo: TUAdInfo?) {
                Log.d(TAG, "onInterstitialAdClose")
            }

            override fun onInterstitialAdVideoStart(tuAdInfo: TUAdInfo?) {
                Log.d(TAG, "onInterstitialAdVideoStart")
            }

            override fun onInterstitialAdVideoEnd(tuAdInfo: TUAdInfo?) {
                Log.d(TAG, "onInterstitialAdVideoEnd")
            }

            override fun onInterstitialAdVideoError(adError: com.thinkup.core.api.AdError?) {
                Log.e(TAG, "onInterstitialAdVideoError: $adError")
            }
        })
        mATInterstitial.load()
    }

    fun showAdsGoToSearchScreen(
        activity: Activity,
        configEntity: ConfigEntity,
        listener: Listener
    ) {
        Log.d(TAG, "getAdsGoToSearchScreen")

        activity.lockOrientation()

        val handlerException = CoroutineExceptionHandler { _, throwable ->
            FirebaseEventUtils.getInstances().recordException(throwable)
        }
        CoroutineScope(Dispatchers.IO + SupervisorJob() + handlerException).launch {
            if (configEntity.isToponads) {
                showTopOnGoToSearchScreen(activity, listener)
            } else {
                showAdMobGoToSearchScreen(activity, listener)
            }
        }
    }

    private suspend fun showAdMobGoToSearchScreen(activity: Activity, listener: Listener) {
        Log.d(TAG, "showAdMobGoToSearchScreen")

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeShowAdsFull < SHOW_INTERSTITIAL_ADS_INTERVAL) {
            withContext(Dispatchers.Main) {
                listener.onNotShowAds()
            }
            Log.d(TAG, "return interval")
            return
        }
        withContext(Dispatchers.Main) {
            if (mInterstitialAdGoToSearchScreen == null) {
                loadAdMobGoToSearchScreen(activity)
                listener.onNotShowAds()
            } else {
                if (activity.isFinishing
//                    || isShowAdsGoToSearchScreen
                ) {
                    return@withContext
                }

                val progressDialog = createLoadingDialog(activity)
                progressDialog.show()

                withContext(Dispatchers.IO) {
                    delay(SHOW_PROGRESS_DIALOG_DELAY)
                    withContext(Dispatchers.Main) {
                        if (!activity.isFinishing) {
                            progressDialog.dismiss()

                            mInterstitialAdGoToSearchScreen?.setOnPaidEventListener { adValue ->
//                                LogAdsRevenueUtils.logAdRevenueEvent(
//                                    activity,
//                                    adValue,
//                                    mInterstitialAdGoToSearchScreen?.responseInfo,
//                                    LogAdsRevenueUtils.AdFormat.INTERSTITIAL,
//                                    LogAdsRevenueUtils.AdPosition.INTERSTITIAL_HOME_TO_OTHER_SCREEN
//                                )
                            }

                            mInterstitialAdGoToSearchScreen?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {

                                    override fun onAdClicked() {
                                        Log.d(TAG, "onAdClicked")
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "onAdDismissedFullScreenContent")
                                        lastTimeShowAdsFull = System.currentTimeMillis()
                                        mInterstitialAdGoToSearchScreen = null
                                        isShowAdsGoToSearchScreen = false
                                        loadAdMobGoToSearchScreen(activity)
                                        listener.onAdDismissedFullScreenContent()
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        Log.e(TAG, "onAdFailedToShowFullScreenContent: $adError")
                                        mInterstitialAdGoToSearchScreen = null
                                        isShowAdsGoToSearchScreen = false
                                        loadAdMobGoToSearchScreen(activity)
                                        listener.onNotShowAds()
                                    }

                                    override fun onAdImpression() {
                                        Log.d(TAG, "onAdImpression")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        Log.d(TAG, "onAdShowedFullScreenContent")
                                        isShowAdsGoToSearchScreen = true
                                    }
                                }
                            mInterstitialAdGoToSearchScreen?.show(activity)
                        }
                    }
                }
            }
        }
    }

    private suspend fun showTopOnGoToSearchScreen(activity: Activity, listener: Listener) {
        Log.d(TAG, "showTopOnInterstitialAds")

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeShowAdsFull < SHOW_INTERSTITIAL_ADS_INTERVAL) {
            withContext(Dispatchers.Main) {
                listener.onNotShowAds()
            }
            Log.d(TAG, "return interval")
            return
        }
        withContext(Dispatchers.Main) {
            if (mATInterstitialAdGoToSearchScreen == null) {
                loadTopOnGoToSearchScreen(activity)
                listener.onNotShowAds()
            } else {
                if (activity.isFinishing
//                    || isShowAdsGoToSearchScreen
                ) {
                    return@withContext
                }

                val progressDialog = createLoadingDialog(activity)
                progressDialog.show()

                withContext(Dispatchers.IO) {
                    delay(SHOW_PROGRESS_DIALOG_DELAY)
                    withContext(Dispatchers.Main) {
                        if (!activity.isFinishing) {
                            progressDialog.dismiss()

                            mATInterstitialAdGoToSearchScreen?.setAdListener(
                                object : TUInterstitialListener {

                                    override fun onInterstitialAdLoaded() {
                                        Log.d(TAG, "onInterstitialAdLoaded")
                                    }

                                    override fun onInterstitialAdLoadFail(adError: com.thinkup.core.api.AdError?) {
                                        Log.e(TAG, "onInterstitialAdLoadFail: $adError")
                                    }

                                    override fun onInterstitialAdClicked(tuAdInfo: TUAdInfo?) {
                                        Log.d(TAG, "onInterstitialAdClicked")
                                    }

                                    override fun onInterstitialAdShow(tuAdInfo: TUAdInfo?) {
                                        Log.d(TAG, "onInterstitialAdShow")
                                        isShowAdsGoToSearchScreen = true
                                    }

                                    override fun onInterstitialAdClose(tuAdInfo: TUAdInfo?) {
                                        Log.d(TAG, "onInterstitialAdClose")
                                        lastTimeShowAdsFull = System.currentTimeMillis()
                                        mATInterstitialAdGoToSearchScreen = null
                                        isShowAdsGoToSearchScreen = false
                                        loadTopOnGoToSearchScreen(activity)
                                        listener.onAdDismissedFullScreenContent()
                                    }

                                    override fun onInterstitialAdVideoStart(tuAdInfo: TUAdInfo?) {
                                        Log.d(TAG, "onInterstitialAdVideoStart")
                                    }

                                    override fun onInterstitialAdVideoEnd(tuAdInfo: TUAdInfo?) {
                                        Log.d(TAG, "onInterstitialAdVideoEnd")
                                    }

                                    override fun onInterstitialAdVideoError(adError: com.thinkup.core.api.AdError?) {
                                        Log.e(TAG, "onInterstitialAdVideoError: $adError")
                                        mATInterstitialAdGoToSearchScreen = null
                                        isShowAdsGoToSearchScreen = false
                                        loadTopOnGoToSearchScreen(activity)
                                        listener.onNotShowAds()
                                    }
                                }
                            )
                            mATInterstitialAdGoToSearchScreen?.show(activity)
                        }
                    }
                }
            }
        }
    }
}
