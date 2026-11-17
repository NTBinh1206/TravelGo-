package com.travelgo.app.data.place

import org.json.JSONObject
import java.util.Locale

/** Kết quả gửi ghi chú báo quán đóng cửa lên OpenStreetMap. */
sealed interface NoteOutcome {
    val noteId: Long

    /** Đã tạo ghi chú mới. */
    data class Created(override val noteId: Long) : NoteOutcome

    /** Quanh quán đã có ghi chú đang mở nói về chính quán này, nên không gửi thêm. */
    data class AlreadyOpen(override val noteId: Long) : NoteOutcome
}

/**
 * Gửi ghi chú (note) công khai lên OpenStreetMap báo một quán có thể đã đóng cửa, để người vẽ bản đồ
 * kiểm tra rồi sửa dữ liệu gốc. Lần sau chạy lại tools/build_food_vn.py, quán đã được sửa trên OSM
 * sẽ không còn trong food_vn.tsv.
 *
 * Ghi chú gửi ẩn danh (không cần tài khoản OSM), chỉ gồm tên quán, toạ độ và đường dẫn tới đối tượng
 * trên OSM; không có thông tin gì về người dùng.
 *
 * Bản thật: [OsmNotesClient]. Phần dựng nội dung, đọc JSON nằm ở [OsmNotes] để unit test.
 */
interface OsmNoteSender {
    suspend fun reportClosed(place: Place, reporters: Int): Result<NoteOutcome>
}

/** Phần xử lý thuần của API ghi chú OSM (API v0.6, định dạng JSON). */
object OsmNotes {
    const val API = "https://api.openstreetmap.org/api/0.6/notes.json"

    /** Gắn vào cuối ghi chú để người vẽ bản đồ biết nguồn báo. */
    const val HASHTAG = "#TravelGo"

    /** Nửa cạnh ô tìm ghi chú có sẵn quanh quán (độ), khoảng 55 m. */
    private const val HALF_BOX_DEG = 0.0005

    /** "n123" → https://www.openstreetmap.org/node/123 (w = way, r = relation); id khác của OSM thì null. */
    fun elementUrl(placeId: String): String? {
        val type = when (placeId.firstOrNull()) {
            'n' -> "node"
            'w' -> "way"
            'r' -> "relation"
            else -> return null
        }
        val number = placeId.drop(1)
        if (number.isEmpty() || !number.all(Char::isDigit)) return null
        return "https://www.openstreetmap.org/$type/$number"
    }

    /** Chỉ quán lấy từ OSM (có mã n/w/r) mới gửi ghi chú được. */
    fun canReport(place: Place): Boolean = elementUrl(place.id) != null

    fun noteText(place: Place, reporters: Int): String = buildString {
        append("Quán \"").append(place.name).append("\" có thể đã đóng cửa: ")
        append(reporters).append(" người dùng ứng dụng TravelGo đã báo. Nhờ mọi người kiểm tra lại tại chỗ.\n")
        append("This place may be permanently closed: reported by ").append(reporters)
        append(" users of the TravelGo app. Please verify on the ground.\n")
        append(elementUrl(place.id).orEmpty()).append('\n')
        append(HASHTAG)
    }

    /** Tìm các ghi chú đang mở trong ô nhỏ quanh quán. bbox theo thứ tự: trái, dưới, phải, trên. */
    fun searchUrl(place: Place): String {
        val box = listOf(
            place.lng - HALF_BOX_DEG, place.lat - HALF_BOX_DEG,
            place.lng + HALF_BOX_DEG, place.lat + HALF_BOX_DEG,
        ).joinToString(",") { String.format(Locale.US, "%.6f", it) }
        return "$API?bbox=$box&closed=0&limit=50"
    }

    /** Các trường gửi kèm khi tạo ghi chú (POST, dạng form). */
    fun createForm(place: Place, reporters: Int): Map<String, String> = mapOf(
        "lat" to String.format(Locale.US, "%.7f", place.lat),
        "lon" to String.format(Locale.US, "%.7f", place.lng),
        "text" to noteText(place, reporters),
    )

    /** Id ghi chú đang mở có nhắc tới đúng đối tượng OSM của quán (theo đường dẫn), nếu có. */
    fun findExisting(json: String, place: Place): Long? {
        val url = elementUrl(place.id) ?: return null
        val sameElement = Regex(Regex.escape(url) + "(?!\\d)") // node/12 không khớp node/123
        val features = JSONObject(json).optJSONArray("features") ?: return null
        for (i in 0 until features.length()) {
            val props = features.optJSONObject(i)?.optJSONObject("properties") ?: continue
            if (props.optString("status") != "open") continue
            val comments = props.optJSONArray("comments") ?: continue
            for (j in 0 until comments.length()) {
                val text = comments.optJSONObject(j)?.optString("text").orEmpty()
                if (sameElement.containsMatchIn(text)) return props.optLong("id")
            }
        }
        return null
    }

    /** Id của ghi chú vừa tạo (máy chủ trả về một Feature GeoJSON). */
    fun parseCreatedId(json: String): Long? =
        JSONObject(json).optJSONObject("properties")?.optLong("id", -1L)?.takeIf { it > 0 }
}
