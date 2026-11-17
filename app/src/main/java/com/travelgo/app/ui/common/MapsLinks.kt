package com.travelgo.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
