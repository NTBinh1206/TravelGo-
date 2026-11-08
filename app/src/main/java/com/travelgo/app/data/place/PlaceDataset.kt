package com.travelgo.app.data.place

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Normalizer

/**
 * Bộ dữ liệu điểm du lịch toàn Việt Nam làm sẵn (hướng B), nằm ở `assets/places_vn.tsv`.
 * Lấy một lần từ OpenStreetMap (Overpass) rồi xử lý bằng tools/build_places_vn.py:
 * loại (5 loại), nhãn ngắn và điểm "nổi bật" đã tính sẵn, app chỉ việc đọc.
 * "Gần bạn", "Khám phá", "Tìm kiếm" chạy ngay, không cần mạng. Cách tạo lại file: tools/README_du_lieu.md.
 */
internal class PlaceDataset(private val context: Context) {

    /** Một dòng trong file: điểm du lịch (isTown = false) hoặc thành phố/thị xã (isTown = true). */
    class Entry(
        val id: String,
        val name: String,
        val category: String,
        val lat: Double,
        val lng: Double,
        val score: Double,
        val tagline: String,
        val address: String,
        val imageUrl: String,
        /** Bài Wikipedia dạng "vi:Tên bài", hoặc mã Wikidata "Q123", rỗng nếu không có. */
        val wikipedia: String,
        val isTown: Boolean,
        /** Tên viết thường, bỏ dấu, dùng để tìm không dấu. */
        val plainName: String,
    )

    private val lock = Mutex()
    @Volatile private var entries: List<Entry>? = null
    @Volatile private var index: Map<String, Entry>? = null

    /** Đọc file một lần (lần đầu mất khoảng nửa giây), các lần sau dùng lại. */
    suspend fun all(): List<Entry> {
        entries?.let { return it }
        return lock.withLock {
            entries ?: withContext(Dispatchers.IO) { load() }.also { list ->
                entries = list
                index = list.associateBy { it.id }
            }
        }
    }

    suspend fun byId(id: String): Entry? {
        all()
        return index?.get(id)
    }

    /**
     * Cột (cách nhau bằng tab):
     * id, name, category, lat, lng, score, tagline, address, imageUrl, wikipedia, isTown
     */
    private fun load(): List<Entry> {
        val started = System.currentTimeMillis()
        val out = ArrayList<Entry>(8_000)
        context.assets.open(FILE).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(1).forEach { line ->
                val c = line.split('\t')
                if (c.size < 11) return@forEach
                val lat = c[3].toDoubleOrNull() ?: return@forEach
                val lng = c[4].toDoubleOrNull() ?: return@forEach
                out += Entry(
                    id = c[0],
                    name = c[1],
                    category = c[2],
                    lat = lat,
                    lng = lng,
                    score = c[5].toDoubleOrNull() ?: 3.0,
                    tagline = c[6],
                    address = c[7],
                    imageUrl = c[8],
                    wikipedia = c[9],
                    isTown = c[10] == "1",
                    plainName = plain(c[1]),
                )
            }
        }
        Log.d("TravelGo", "Bộ dữ liệu: ${out.size} điểm, đọc trong ${System.currentTimeMillis() - started} ms")
        return out
    }

    companion object {
        const val FILE = "places_vn.tsv"

        /** Viết thường, bỏ dấu, "đ" -> "d": "Đà Lạt" -> "da lat". */
        fun plain(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .replace('đ', 'd').replace('Đ', 'D')
                .lowercase()
                .trim()
    }
}
