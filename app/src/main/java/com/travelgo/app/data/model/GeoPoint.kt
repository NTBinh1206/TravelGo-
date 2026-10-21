package com.travelgo.app.data.model

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Một toạ độ trên bản đồ. Dùng chung cho vị trí người dùng và địa điểm. */
data class GeoPoint(val lat: Double, val lng: Double) {

    /** Khoảng cách đường chim bay (km), công thức Haversine. */
    fun distanceKmTo(other: GeoPoint): Double {
        val r = 6371.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLng = Math.toRadians(other.lng - lng)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }

    companion object {
        /** Vị trí mặc định khi chưa lấy được GPS (trung tâm Đà Nẵng). */
        val DEFAULT = GeoPoint(16.0544, 108.2022)
    }
}

private val viLocale: Locale = Locale.forLanguageTag("vi")

/** 2.1 -> "2,1 km"; 24.3 -> "24 km" */
fun formatKm(km: Double): String =
    if (km >= 10) String.format(viLocale, "%.0f km", km) else String.format(viLocale, "%.1f km", km)
