package com.travelgo.app.data.place

import android.content.Context
import android.util.Log
import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Quán ăn, quán nước toàn Việt Nam làm sẵn, nằm ở `assets/food_vn.tsv` (tạo bằng tools/build_food_vn.py).
 * Có file này thì mục Ăn uống hiện ngay, không phải chờ máy chủ Overpass (có lúc 15–60 giây hoặc lỗi).
 * Chưa có file, hoặc quanh vị trí không có quán nào trong file, thì OsmPlaceRepository mới hỏi Overpass.
 *
 * File chỉ được đọc lần đầu người dùng cần đến quán ăn, không đọc lúc mở app.
 */
internal class FoodDataset(private val context: Context) {

    /** Một quán: [tags] gồm name, amenity, cuisine để FoodPlaces tính loại quán, nhãn món và lọc quán đã đóng. */
    class Entry(
        val id: String,
        val name: String,
        val tags: Map<String, String>,
        val address: String,
        val lat: Double,
        val lng: Double,
    )

    private val lock = Mutex()
    @Volatile private var entries: List<Entry>? = null
    @Volatile private var index: Map<String, Entry>? = null

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

    /** Các quán trong bán kính [radiusKm] quanh [center], chưa sắp xếp. */
    suspend fun near(center: GeoPoint, radiusKm: Double): List<Entry> = withContext(Dispatchers.Default) {
        // Lọc nhanh theo khung vĩ độ / kinh độ trước, rồi mới tính khoảng cách thật.
        val dLat = radiusKm / 111.0
        val dLng = radiusKm / (111.0 * Math.cos(Math.toRadians(center.lat)).coerceAtLeast(0.1))
        all().filter { e ->
            e.lat in (center.lat - dLat)..(center.lat + dLat) &&
                e.lng in (center.lng - dLng)..(center.lng + dLng) &&
                center.distanceKmTo(GeoPoint(e.lat, e.lng)) <= radiusKm
        }
    }

    private fun load(): List<Entry> {
        val started = System.currentTimeMillis()
        val list = try {
            context.assets.open(FILE).bufferedReader(Charsets.UTF_8).useLines { parse(it) }
        } catch (e: FileNotFoundException) {
            Log.i(TAG, "Chưa có $FILE, mục Ăn uống sẽ hỏi Overpass")
            emptyList()
        }
        Log.d(TAG, "Đọc $FILE: ${list.size} quán trong ${System.currentTimeMillis() - started} ms")
        return list
    }

    companion object {
        const val FILE = "food_vn.tsv"
        private const val TAG = "FoodDataset"

        /** Cột (cách nhau bằng tab): id name amenity cuisine address lat lng. Dòng đầu là tiêu đề. */
        fun parse(lines: Sequence<String>): List<Entry> {
            val out = ArrayList<Entry>(50_000)
            lines.drop(1).forEach { line ->
                val c = line.split('\t')
                if (c.size < 7) return@forEach
                val lat = c[5].toDoubleOrNull() ?: return@forEach
                val lng = c[6].toDoubleOrNull() ?: return@forEach
                if (c[0].isBlank() || c[1].isBlank()) return@forEach
                val tags = buildMap {
                    put("name", c[1])
                    put("amenity", c[2])
                    if (c[3].isNotBlank()) put("cuisine", c[3])
                }
                out += Entry(id = c[0], name = c[1], tags = tags, address = c[4], lat = lat, lng = lng)
            }
            return out
        }
    }
}
