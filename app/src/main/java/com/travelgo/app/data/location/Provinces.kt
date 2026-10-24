package com.travelgo.app.data.location

import com.travelgo.app.data.model.GeoPoint
import java.text.Normalizer

/** Một tỉnh/thành để người dùng tự chọn khi không bật GPS. Toạ độ là trung tâm hành chính (gần đúng). */
data class Province(val name: String, val center: GeoPoint)

/** 34 tỉnh, thành phố sau sắp xếp năm 2025. Các thành phố du lịch lớn xếp lên đầu. */
val vietnamProvinces: List<Province> = listOf(
    Province("Hà Nội", GeoPoint(21.0285, 105.8542)),
    Province("TP. Hồ Chí Minh", GeoPoint(10.7769, 106.7009)),
    Province("Đà Nẵng", GeoPoint(16.0544, 108.2022)),
    Province("Huế", GeoPoint(16.4637, 107.5909)),
    Province("Khánh Hòa", GeoPoint(12.2388, 109.1967)),
    Province("Lâm Đồng", GeoPoint(11.9404, 108.4583)),
    Province("Quảng Ninh", GeoPoint(20.9590, 107.0448)),
    Province("Hải Phòng", GeoPoint(20.8449, 106.6881)),
    Province("Cần Thơ", GeoPoint(10.0452, 105.7469)),
    Province("An Giang", GeoPoint(10.0125, 105.0809)),
    Province("Bắc Ninh", GeoPoint(21.2731, 106.1946)),
    Province("Cà Mau", GeoPoint(9.1769, 105.1524)),
    Province("Cao Bằng", GeoPoint(22.6657, 106.2577)),
    Province("Đắk Lắk", GeoPoint(12.6667, 108.0500)),
    Province("Điện Biên", GeoPoint(21.3860, 103.0230)),
    Province("Đồng Nai", GeoPoint(10.9574, 106.8427)),
    Province("Đồng Tháp", GeoPoint(10.3600, 106.3600)),
    Province("Gia Lai", GeoPoint(13.7820, 109.2196)),
    Province("Hà Tĩnh", GeoPoint(18.3428, 105.9057)),
    Province("Hưng Yên", GeoPoint(20.4463, 106.3366)),
    Province("Lai Châu", GeoPoint(22.3964, 103.4582)),
    Province("Lạng Sơn", GeoPoint(21.8537, 106.7615)),
    Province("Lào Cai", GeoPoint(21.7229, 104.9113)),
    Province("Nghệ An", GeoPoint(18.6796, 105.6813)),
    Province("Ninh Bình", GeoPoint(20.2506, 105.9745)),
    Province("Phú Thọ", GeoPoint(21.3227, 105.4020)),
    Province("Quảng Ngãi", GeoPoint(15.1214, 108.8044)),
    Province("Quảng Trị", GeoPoint(17.4689, 106.6223)),
    Province("Sơn La", GeoPoint(21.3270, 103.9141)),
    Province("Tây Ninh", GeoPoint(10.5359, 106.4137)),
    Province("Thái Nguyên", GeoPoint(21.5942, 105.8482)),
    Province("Thanh Hóa", GeoPoint(19.8067, 105.7852)),
    Province("Tuyên Quang", GeoPoint(21.8236, 105.2140)),
    Province("Vĩnh Long", GeoPoint(10.2537, 105.9722)),
)

/** Bỏ dấu để tìm kiếm: "Đà Nẵng" → "da nang". */
fun String.noAccent(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase()
