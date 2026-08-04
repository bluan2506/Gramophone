package org.akanework.gramophone.logic.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.utils.config.ConfigUtils
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Quang Phúc on 15/10/24.
 */
object NetworkUtils {

    private const val TAG = "MyAppNetworkUtils"

    fun init() {
        // Không còn state toàn cục cần reset.
    }

    fun checkInternet(context: Context, onFinish: (isConnected: Boolean) -> Unit) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            val isConnected = isConnected(context)
            withContext(Dispatchers.Main) {
                onFinish(isConnected)
            }
        }
    }

    fun checkInternet(context: Context): Boolean {
        return try {
            isConnected(context)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: run {
            Log.i(TAG, "isConnected: false (no active network)")
            return false
        }
        val caps = cm.getNetworkCapabilities(network) ?: run {
            Log.i(TAG, "isConnected: false (no capabilities)")
            return false
        }

        // Không có khả năng internet -> chắc chắn không có mạng.
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            Log.i(TAG, "isConnected: false (no INTERNET capability)")
            return false
        }

        // Android đã tự xác thực mạng thực sự ra được internet -> tin luôn,
        // không cần phụ thuộc vào một server cụ thể nào.
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            Log.i(TAG, "isConnected: true (validated)")
            return true
        }

        // Mạng vừa kết nối nhưng chưa được validate -> thử probe HTTP như phương án dự phòng.
        Log.d(TAG, "network not validated yet, fallback to http probe")
        return isConnectedByCheckHttp()
    }

    private fun isConnectedByCheckHttp(): Boolean {
        Log.d(TAG, "isConnectedByCheckHttp")

        var httpUrlConnection: HttpURLConnection? = null
        try {
            val serverUrlCheckInternet = ConfigUtils.SERVER_URL_CHECK_INTERNET
            val url = URL(serverUrlCheckInternet)
            httpUrlConnection = url.openConnection() as HttpURLConnection
            httpUrlConnection.connectTimeout = 6000 // milliseconds
            httpUrlConnection.readTimeout = 6000
            httpUrlConnection.connect()

            Log.i(TAG, "isConnected: true")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            httpUrlConnection?.disconnect()
        }

        Log.i(TAG, "isConnected: false")
        return false
    }

    private fun isVpnActive(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val allNetworks = connectivityManager.allNetworks
        for (network in allNetworks) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities != null
                && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ) {
                return true
            }
        }
        return false
    }

    private fun isProxyActive(): Boolean {
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")?.toIntOrNull() ?: -1
        return !proxyHost.isNullOrEmpty() && proxyPort != -1
    }

    fun isVpnOrProxyActive(context: Context): Boolean {
        return isVpnActive(context) || isProxyActive()
    }
}
