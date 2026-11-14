package com.travelgo.app.data.net

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** Máy chủ trả mã lỗi HTTP (4xx, 5xx). */
class HttpException(val code: Int) : IOException("HTTP $code")

/**
 * Gọi HTTP dùng chung (OkHttp, chạy chặn: gọi trong Dispatchers.IO).
 * Giống OsrmRouter: nếu bắt tay TLS lỗi với bộ mã hoá mặc định của app thì thử lại bằng TLS gốc của Android.
 */
object Http {
    private const val TAG = "Http"
    private const val AGENT = "TravelGo/1.0 (Android student project)"
    private const val TIMEOUT_S = 10L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    private val platformTls: OkHttpClient? by lazy {
        runCatching {
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as KeyStore?)
            val trust = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
            val context = SSLContext.getInstance("TLS", "AndroidOpenSSL").apply { init(null, arrayOf(trust), null) }
            client.newBuilder().sslSocketFactory(context.socketFactory, trust).build()
        }.onFailure { Log.w(TAG, "Không tạo được TLS gốc: ${it.message}") }.getOrNull()
    }

    fun get(url: String): String = execute(Request.Builder().url(url).header("User-Agent", AGENT).build())

    fun post(url: String, body: RequestBody): String =
        execute(Request.Builder().url(url).post(body).header("User-Agent", AGENT).build())

    private fun execute(request: Request): String = try {
        call(client, request)
    } catch (e: SSLException) {
        val fallback = platformTls ?: throw e
        Log.i(TAG, "Bắt tay TLS lỗi (${e.message}), thử lại bằng TLS gốc của Android")
        call(fallback, request)
    }

    private fun call(client: OkHttpClient, request: Request): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpException(response.code)
            response.body?.string() ?: throw IOException("Không có nội dung")
        }
}
