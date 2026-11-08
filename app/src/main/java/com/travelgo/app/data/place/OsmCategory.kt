package com.travelgo.app.data.place

import java.text.Normalizer
import kotlin.math.round

/**
 * Xếp một địa điểm OSM vào đúng 1 trong 5 loại của app:
 * bien, nui, van_hoa, thanh_pho, thien_nhien.
 * Thứ tự: thẻ OSM rõ ràng -> từ khoá trong tên -> mặc định "thanh_pho".
 */
internal object OsmCategory {

    fun of(tags: Map<String, String>, name: String): String {
        val natural = tags["natural"]
        val tourism = tags["tourism"]
        val leisure = tags["leisure"]
        val place = tags["place"]

        // 1. Thẻ OSM nói rõ loại
        when {
            natural in setOf("beach", "bay", "cape", "coastline") || leisure == "beach_resort" ||
                place in setOf("island", "islet") -> return "bien"
            natural in setOf("peak", "volcano", "ridge", "hill", "cave_entrance", "cliff", "saddle", "valley") ||
                tags["mountain_pass"] == "yes" -> return "nui"
            tags.containsKey("historic") || tags["amenity"] == "place_of_worship" ||
                tourism in setOf("museum", "gallery") || tags.containsKey("heritage") ||
                tags["building"] in setOf("temple", "pagoda", "church", "cathedral", "shrine") -> return "van_hoa"
            natural in setOf("waterfall", "water", "wood", "spring", "hot_spring", "wetland", "grassland") ||
                tags["waterway"] == "waterfall" || leisure in setOf("nature_reserve", "park", "garden") ||
                tags["boundary"] in setOf("national_park", "protected_area") || tourism == "viewpoint" -> return "thien_nhien"
            place in setOf("city", "town", "suburb", "quarter", "square") -> return "thanh_pho"
        }

        // 2. Đoán theo tên (với các điểm chỉ ghi tourism=attraction)
        val text = " ${plain(name)} "
        nameRules.firstOrNull { (words, _) -> words.any { text.contains(" $it ") } }?.let { return it.second }

        // 3. Còn lại: công trình, cầu, khu vui chơi... trong thành phố
        return "thanh_pho"
    }

    /** Nhãn ngắn hiển thị dưới tên, ví dụ "Bãi biển", "Chùa". */
    fun tagline(tags: Map<String, String>, category: String): String {
        val natural = tags["natural"]
        return when {
            natural == "beach" -> "Bãi biển"
            natural == "peak" || natural == "volcano" -> "Núi"
            natural == "cave_entrance" -> "Hang động"
            natural == "waterfall" || tags["waterway"] == "waterfall" -> "Thác nước"
            natural == "bay" -> "Vịnh"
            tags["place"] in setOf("island", "islet") -> "Đảo"
            tags["tourism"] == "museum" -> "Bảo tàng"
            tags["tourism"] == "viewpoint" -> "Điểm ngắm cảnh"
            tags["tourism"] == "theme_park" || tags["leisure"] == "water_park" -> "Khu vui chơi"
            tags["tourism"] == "zoo" || tags["tourism"] == "aquarium" -> "Vườn thú · Thủy cung"
            tags["amenity"] == "place_of_worship" -> worshipLabel(tags)
            tags.containsKey("historic") -> "Di tích lịch sử"
            tags["leisure"] == "park" || tags["leisure"] == "garden" -> "Công viên"
            tags["leisure"] == "nature_reserve" || tags["boundary"] == "national_park" -> "Khu bảo tồn"
            tags["place"] in setOf("city", "town") -> "Thành phố"
            else -> when (category) {
                "bien" -> "Biển"
                "nui" -> "Núi"
                "van_hoa" -> "Văn hóa"
                "thien_nhien" -> "Thiên nhiên"
                else -> "Điểm tham quan"
            }
        }
    }

    /**
     * Điểm "nổi bật" thang 0–5, thay cho số sao (OSM không có đánh giá).
     * Điểm có bài Wikipedia, là di sản, có ảnh... được cộng thêm.
     */
    fun score(tags: Map<String, String>): Double {
        var s = 2.5
        if (tags.containsKey("wikipedia") || tags.containsKey("wikidata")) s += 1.0
        if (tags["tourism"] in setOf("attraction", "theme_park", "zoo", "aquarium", "museum")) s += 0.5
        // Cảnh quan thiên nhiên là điểm đáng đi dù ít khi có bài Wikipedia (giống tools/build_places_vn.py).
        if (tags["natural"] in setOf("beach", "waterfall", "cave_entrance", "bay", "cape", "hot_spring", "peak", "volcano") ||
            tags["waterway"] == "waterfall" || tags["place"] in setOf("island", "islet") ||
            tags["leisure"] == "nature_reserve" || tags["boundary"] == "national_park"
        ) s += 0.5
        if (tags.containsKey("heritage") || tags.containsKey("historic")) s += 0.5
        if (tags.containsKey("image") || tags.containsKey("wikimedia_commons")) s += 0.3
        if (tags.containsKey("name:en")) s += 0.2
        return round(s.coerceAtMost(5.0) * 10) / 10
    }

    private fun worshipLabel(tags: Map<String, String>): String = when (tags["religion"]) {
        "buddhist" -> "Chùa"
        "christian" -> "Nhà thờ"
        else -> "Đền · Miếu"
    }

    private val nameRules = listOf(
        listOf("bien", "bai tam", "vinh", "dao", "mui") to "bien",
        listOf("nui", "deo", "hang", "dong", "son") to "nui",
        listOf("chua", "den", "dinh", "mieu", "nha tho", "lang", "thanh co", "bao tang", "di tich", "pho co", "thap") to "van_hoa",
        listOf("ho", "thac", "suoi", "rung", "vuon quoc gia", "khu bao ton", "song") to "thien_nhien",
    )

    private fun plain(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
}
