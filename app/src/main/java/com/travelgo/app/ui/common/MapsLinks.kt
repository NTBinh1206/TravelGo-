package com.travelgo.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place

/** Câu tìm trên Google Maps: tên quán kèm địa chỉ (nếu có) để ra đúng quán. */
fun googleMapsQuery(place: Place): String =
    listOf(place.name, place.address).filter { it.isNotBlank() }.joinToString(", ")

/**
 * Mở Google Maps tìm đúng quán để người dùng xem ảnh, điểm sao và bình luận.
 * Không cần khoá API vì chỉ mở liên kết; máy không có Google Maps thì mở bằng trình duyệt.
 */
fun openPlaceInGoogleMaps(context: Context, place: Place) {
    val url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(googleMapsQuery(place))
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Máy chưa có Google Maps hoặc trình duyệt.", Toast.LENGTH_LONG).show()
    }
}

/**
 * Mở Google Maps chỉ đường tới [place] (màn Chi tiết địa điểm).
 * [from] là vị trí app đang dùng (GPS hoặc tỉnh người dùng tự chọn), để điểm xuất phát khớp với
 * khoảng cách "Cách bạn" trong app; null thì Google Maps tự lấy GPS của máy.
 * Dùng toạ độ thay cho tên để không lẫn với nơi trùng tên.
 */
fun openDirectionsInGoogleMaps(context: Context, place: Place, from: GeoPoint? = null) {
    val origin = from?.let { "&origin=${it.lat},${it.lng}" }.orEmpty()
    val url = "https://www.google.com/maps/dir/?api=1$origin&destination=${place.lat},${place.lng}&travelmode=driving"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Máy chưa có Google Maps hoặc trình duyệt để dẫn đường.", Toast.LENGTH_LONG).show()
    }
}
