package org.akanework.gramophone.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.aws.config.msserverconfig.Config_V1
import com.aws.config.msserverconfig.GetConfigCallback
import com.google.android.gms.ads.MobileAds
import com.music.searchapi.ApiServices
import com.music.searchapi.callback.ExceptionCallback
import com.music.searchapi.callback.InitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.databinding.ActivitySplashBinding
import org.akanework.gramophone.logic.ui.BaseActivity
import org.akanework.gramophone.logic.utils.NetworkUtils
import org.akanework.gramophone.logic.utils.ads.InterstitialAdsUtils
import org.akanework.gramophone.logic.utils.config.ConfigUtils
import org.akanework.gramophone.logic.utils.config.Sercurity
import org.akanework.gramophone.logic.utils.firebase.FirebaseEventUtils

class SplashActivity : BaseActivity() {

    companion object {
        private const val TAG = "MyAppSplashActivity"
    }

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }

    private val configEntity by lazy {
        ConfigUtils.configApp(this)
    }

    var alreadyShowAds: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@SplashActivity) {

            }
        }

        InterstitialAdsUtils.init()
        NetworkUtils.init()

        Log.d(TAG + " serverurl", Sercurity.encrypt("https://8c3ihksoa5.execute-api.us-east-1.amazonaws.com/config"))
        Log.d(TAG + " checkinternet", Sercurity.encrypt("https://8c3ihksoa5.execute-api.us-east-1.amazonaws.com/internet"))
        Log.d(TAG + " blacklist", Sercurity.encrypt("https://8c3ihksoa5.execute-api.us-east-1.amazonaws.com/bla8"))
        Log.d(TAG + " codelua", Sercurity.encrypt("https://8c3ihksoa5.execute-api.us-east-1.amazonaws.com/luzc8"))
        Log.d(TAG + " logerrror", Sercurity.encrypt("https://8c3ihksoa5.execute-api.us-east-1.amazonaws.com/logerror"))
        Log.d(TAG + " analytics", Sercurity.encrypt("https://8c3ihksoa5.execute-api.us-east-1.amazonaws.com/analytics"))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val callback = object : GetConfigCallback {

                    override fun onFinished() {
                        // dùng cho thư viện cũ.

                        // nếu dùng kiểu thư viện mới thì không cần xử lý hàm này.
                    }

                    override fun onNoInternet_UsingCacheConfig() {
                        // khi mở app lên mà không có internet thì vẫn có config cũ đã lưu trong cache
                        // thư viện trả ra config cũ để phía App xử dụng nếu cần.
                        InterstitialAdsUtils.getAdsWelcome(this@SplashActivity, configEntity) {
                            startMainActivity()
                        }
                    }

                    override fun onFinished_InitLibSearch() {
                        val serverUrlCodeLua = ConfigUtils.SERVER_URL_CODE_LUA
                        val serverUrlLogError = ConfigUtils.SERVER_URL_LOG_ERROR
                        val serverUrlFileBlacklist = ConfigUtils.SERVER_URL_FILE_BLACKLIST

                        ApiServices.init(
                            this@SplashActivity,
                            serverUrlCodeLua,
                            serverUrlLogError,
                            serverUrlFileBlacklist,
                            object : InitCallback {

                                override fun onFinished(isFinished: Boolean, message: String?) {
                                    Log.i(
                                        TAG,
                                        "onFinished, isFinished = $isFinished, message = $message"
                                    )
                                }
                            },
                            object : ExceptionCallback {

                                override fun onException(e: Exception) {
                                    FirebaseEventUtils.getInstances().recordException(e)
                                }
                            }
                        )
                        InterstitialAdsUtils.getAdsWelcome(this@SplashActivity, configEntity) {
                            startMainActivity()
                        }
                    }
                }

                val serverUrl = ConfigUtils.SERVER_URL
                Config_V1.setServerUrl(serverUrl)
                Config_V1.init(application, callback) { e ->
                    FirebaseEventUtils.getInstances().recordException(e)
                }
            } catch (e: Exception) {
                FirebaseEventUtils.getInstances().recordException(e)
                startMainActivity()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            delay(12000)
            withContext(Dispatchers.Main) {
                if (!InterstitialAdsUtils.isShowAdsWelcome && !isFinishing) {
                    Log.d(TAG, "startMainActivity 12000")
                    startMainActivity()
                } else {
                    Log.d(TAG, "Not startMainActivity 12000")
                }
            }
        }
    }

    fun startMainActivity() {
        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
