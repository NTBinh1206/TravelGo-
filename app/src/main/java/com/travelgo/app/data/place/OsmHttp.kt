package com.travelgo.app.data.place

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Gọi HTTP đơn giản, không cần thêm thư viện.
 * OSM, Nominatim và Wikipedia đều yêu cầu gửi User-Agent ghi rõ tên ứng dụng.
 * Lỗi mạng hoặc máy chủ trả lỗi thì ném IOException để màn hình hiện "Lỗi mạng, Thử lại".
 */
internal object OsmHttp {
    private const val USER_AGENT = "TravelGo/1.0 (Android student project)"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun get(url: String): String = request(url, body = null, READ_TIMEOUT_MS)

    suspend fun postForm(url: String, fields: Map<String, String>, readTimeoutMs: Int = READ_TIMEOUT_MS): String =
        request(url, body = fields.entries.joinToString("&") { (k, v) -> "$k=${encode(v)}" }, readTimeoutMs)

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private suspend fun request(url: String, body: String?, readTimeoutMs: Int): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept-Language", "vi,en")
            if (body != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code từ ${URL(url).host}")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
