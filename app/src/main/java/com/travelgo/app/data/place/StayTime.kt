package com.travelgo.app.data.place


import java.text.Normalizer

private fun String.plain(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase()

/** Lớp 1: đoán theo từ khóa trong tên (viết không dấu, chữ thường). */
private val keywordStay = listOf(
    listOf("vinwonders", "safari", "vui choi", "cong vien nuoc") to 360,
    listOf("resort", "khu du lich") to 240,
    listOf("dao", "vinh") to 180,
    listOf("bao tang") to 90,
    listOf("thac", "hang", "suoi") to 90,
    listOf("cho") to 60,
    listOf("chua", "den", "dinh", "nha tho", "thanh duong") to 40,
    listOf("cau", "quang truong") to 30,
)

/** Lớp 2: theo loại chung khi tên không khớp từ khóa nào. */
private fun byCategory(category: String): Int = when (category) {
    "bien" -> 90
    "nui" -> 120
    "van_hoa" -> 60
    "thanh_pho" -> 45
    "thien_nhien" -> 120
    else -> 60
}

/**
 * Thời gian tham quan ước lượng (phút)
 * không phải thời gian đi đường. Places API không có thông tin này nên ta tự gán.
 */
fun estimateStayMinutes(name: String, category: String): Int {
    val text = " ${name.plain()} "
    keywordStay.firstOrNull { (words, _) -> words.any { text.contains(" $it ") } }
        ?.let { return it.second }
    return byCategory(category)
}