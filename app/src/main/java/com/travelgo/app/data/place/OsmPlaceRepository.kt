package com.travelgo.app.data.place

import android.content.Context
import android.util.Log
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Dữ liệu địa điểm thật, thay cho FakePlaceRepository (Người 2).
 * - Gần bạn / Khám phá: bộ dữ liệu OSM toàn Việt Nam làm sẵn trong app (PlaceDataset, assets/places_vn.tsv),
 *   chạy ngay, không cần mạng.
 * - Tìm kiếm: tìm ngay trong bộ dữ liệu (không dấu). Ít kết quả quá thì hỏi thêm Nominatim (OSM, cần mạng).
 * - Ảnh: điểm có bài Wikipedia / mã Wikidata thì hỏi Wikipedia (cần mạng; mạng chậm thì hiện thẻ trước, ảnh có ở lần sau).
 * - Lưu tạm mọi điểm đã trả về theo id, để màn Lộ trình (08) gọi getPlacesByIds() lấy lại được.
 */
class OsmPlaceRepository(
    context: Context,
    private val location: LocationProvider,
) : PlaceRepository {

    private val dataset = PlaceDataset(context.applicationContext)
    private val byId = ConcurrentHashMap<String, Place>()
    private val wikiCache = ConcurrentHashMap<String, WikiInfo>()
    private val nominatimCache = ConcurrentHashMap<String, CacheEntry>()

    private val nominatimLock = Mutex()
    private var lastNominatimCall = 0L

    // Tải ảnh Wikipedia chạy nền: màn hình không phải chờ, ảnh về thì lưu lại cho lần sau.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class CacheEntry(val time: Long, val raw: List<RawPlace>)

    // ---------------- Gần bạn ----------------

    override suspend fun getNearbyPlaces(center: GeoPoint, radiusKm: Int): List<Place> {
        val raw = dataset.all().asSequence()
            .filter { !it.isTown }
            .filter { it.distanceTo(center) <= radiusKm }
            .map { it.toRaw() }
            .toList()
            .distinctNearby()
            .sortedByDescending { it.rank() }
        // Bỏ các điểm quá nhỏ (điểm nổi bật thấp, thường là dữ liệu OSM gắn nhầm) khi quanh đó đủ điểm đáng đi.
        val worthy = raw.filter { it.rank() >= MIN_WORTHY_SCORE }
        val places = buildPlaces((if (worthy.size >= MIN_WORTHY_COUNT) worthy else raw).take(MAX_NEARBY), nearMe = true)
        Log.d(TAG, "Gần bạn: ${places.size} điểm quanh $center (bán kính $radiusKm km)")
        return places
    }

    // ---------------- Tìm kiếm ----------------

    override suspend fun searchPlaces(query: String): List<Place> {
        val q = PlaceDataset.plain(query)
        if (q.length < 2) return emptyList()
        val me = location.currentLocation.value ?: GeoPoint.DEFAULT
        val words = q.split(' ').filter { it.isNotBlank() }

        // 1. Tìm ngay trong bộ dữ liệu: tên chứa đủ mọi từ đã gõ (không dấu).
        val local = dataset.all().asSequence()
            .filter { e -> words.all { e.plainName.contains(it) } }
            .sortedWith(
                compareByDescending<PlaceDataset.Entry> { it.plainName == q }   // trùng hẳn tên
                    .thenByDescending { it.plainName.startsWith(q) }          // bắt đầu bằng từ khoá
                    .thenByDescending { it.isTown }                           // thành phố lên trước
                    .thenBy { it.distanceTo(me) / 50 + (5 - it.score) },             // gần + nổi bật
            )
            .take(MAX_SEARCH)
            .map { it.toRaw() }
            .toList()

        // 2. Ít kết quả quá thì hỏi thêm Nominatim. Mất mạng mà vẫn có kết quả trong máy thì bỏ qua lỗi.
        val online = if (local.size >= MIN_LOCAL_RESULTS) {
            emptyList()
        } else {
            try {
                searchNominatim(query.trim(), me)
            } catch (e: Exception) {
                if (local.isEmpty()) throw e
                Log.w(TAG, "Nominatim lỗi, chỉ dùng kết quả trong máy", e)
                emptyList()
            }
        }
        val localIds = local.mapTo(HashSet()) { it.id }
        val places = buildPlaces(local + online.filter { it.id !in localIds })
        Log.d(TAG, "Tìm \"$query\": ${local.size} trong máy + ${online.size} từ Nominatim")
        return places
    }

    private suspend fun searchNominatim(q: String, me: GeoPoint): List<RawPlace> {
        val key = q.lowercase()
        nominatimCache[key]?.takeIf { it.isFresh() }?.let { return it.raw }
        // viewbox quanh người dùng (~1 độ ≈ 110 km), bounded=0: chỉ ưu tiên, không giới hạn.
        val viewbox = String.format(
            Locale.US, "%.4f,%.4f,%.4f,%.4f", me.lng - 1, me.lat + 1, me.lng + 1, me.lat - 1,
        )
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2" +
            "&q=${OsmHttp.encode(q)}&countrycodes=vn&accept-language=vi" +
            "&addressdetails=1&extratags=1&namedetails=1&limit=$MAX_SEARCH" +
            "&viewbox=$viewbox&bounded=0"
        return parseNominatim(callNominatim(url)).also { nominatimCache[key] = CacheEntry(now(), it) }
    }

    // ---------------- Theo id (màn Lộ trình) ----------------

    override suspend fun getPlacesByIds(ids: List<String>): List<Place> {
        // Ví dụ app bị tắt rồi mở lại thẳng màn 08: lấy lại từ bộ dữ liệu, không có thì hỏi OSM.
        val missing = ids.filter { !byId.containsKey(it) }
        if (missing.isNotEmpty()) {
            val fromDataset = missing.mapNotNull { dataset.byId(it)?.toRaw() }
            if (fromDataset.isNotEmpty()) buildPlaces(fromDataset)
            val stillMissing = missing.filter { id ->
                !byId.containsKey(id) && id.length > 1 && id[0] in "nwr" && id.drop(1).all(Char::isDigit)
            }
            if (stillMissing.isNotEmpty()) {
                runCatching { buildPlaces(parseOverpass(postOverpass(lookupQuery(stillMissing)))) }
                    .onFailure { Log.w(TAG, "Không tải lại được $stillMissing", it) }
            }
        }
        return ids.mapNotNull { byId[it] }
    }

    // ---------------- Trang chủ ----------------

    override suspend fun getFeaturedDestinations(): List<Place> = featured.also { list ->
        list.forEach { byId[it.id] = it }
    }

    /** Đoạn mô tả từ Wikipedia của một điểm (null nếu chưa có). Dùng cho màn chi tiết sau này. */
    fun descriptionOf(id: String): String? = wikiCache[id]?.description?.takeIf { it.isNotBlank() }

    // =====================================================================

    /**
     * Một địa điểm thô, chưa thành Place.
     * Điểm từ bộ dữ liệu có sẵn loại/nhãn/điểm/ảnh; điểm từ Nominatim thì tính từ thẻ OSM ([tags]).
     */
    private data class RawPlace(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double,
        val tags: Map<String, String>,
        val address: String,
        val category: String? = null,
        val tagline: String? = null,
        val score: Double? = null,
        val image: String = "",
    )

    private fun RawPlace.rank(): Double = score ?: OsmCategory.score(tags)

    /**
     * [nearMe] = true: các điểm quanh vị trí người dùng, thiếu địa chỉ thì ghi tạm tên tỉnh đang chọn.
     * Kết quả tìm kiếm (có thể ở tỉnh khác) thì để trống.
     */
    private suspend fun buildPlaces(raw: List<RawPlace>, nearMe: Boolean = false): List<Place> {
        loadWikipedia(raw)
        val fallbackAddress = if (nearMe) location.placeName.value.orEmpty() else ""

        return raw.map { r ->
            val category = r.category ?: OsmCategory.of(r.tags, r.name)
            val info = wikiCache[r.id]
            Place(
                id = r.id,
                name = r.name,
                category = category,
                address = r.address.ifBlank { fallbackAddress },
                lat = r.lat,
                lng = r.lng,
                rating = r.rank(),
                stayMinutes = estimateStayMinutes(r.name, category),
                imageUrl = r.image.ifBlank { info?.imageUrl?.takeIf { it.isNotBlank() } ?: WikipediaClient.imageFromTags(r.tags) },
                tagline = r.tagline ?: OsmCategory.tagline(r.tags, category),
            ).also { byId[it.id] = it }
        }
    }

    /**
     * Hỏi Wikipedia ảnh + mô tả cho các điểm chưa có, chờ tối đa [WIKI_WAIT_MS].
     * Quá thời gian thì vẫn trả danh sách (chưa có ảnh); việc tải tiếp chạy nền và lưu vào wikiCache.
     */
    private suspend fun loadWikipedia(raw: List<RawPlace>) {
        val need = raw.filter {
            it.image.isBlank() && !wikiCache.containsKey(it.id) &&
                (it.tags.containsKey("wikipedia") || it.tags.containsKey("wikidata"))
        }.associate { it.id to it.tags }
        if (need.isEmpty()) return
        val job = scope.async {
            runCatching { WikipediaClient.fetch(need) }
                .onFailure { Log.w(TAG, "Wikipedia lỗi", it) }
                .getOrDefault(emptyMap())
                .also { found ->
                    wikiCache.putAll(found)
                    Log.d(TAG, "Wikipedia: ${found.size}/${need.size} điểm, có ảnh ${found.values.count { it.imageUrl.isNotBlank() }}")
                }
        }
        withTimeoutOrNull(WIKI_WAIT_MS) { job.await() }
    }

    // ---------------- Overpass ----------------

    /** Lấy lại các điểm theo id: "n123" = node, "w123" = way, "r123" = relation. */
    private fun lookupQuery(ids: List<String>): String {
        fun part(prefix: Char, type: String): String {
            val nums = ids.filter { it.firstOrNull() == prefix }.mapNotNull { it.drop(1).toLongOrNull() }
            return if (nums.isEmpty()) "" else "$type(id:${nums.joinToString(",")});"
        }
        return "[out:json][timeout:25];(${part('n', "node")}${part('w', "way")}${part('r', "relation")});out center tags;"
    }

    /** Chỉ dùng khi lấy lại điểm theo id mà bộ dữ liệu không có. Lỗi thì thử máy chủ dự phòng. */
    private suspend fun postOverpass(query: String): String {
        var last: Exception? = null
        for (server in OVERPASS_SERVERS) {
            try {
                return OsmHttp.postForm(server, mapOf("data" to query), readTimeoutMs = 30_000)
            } catch (e: Exception) {
                Log.w(TAG, "Overpass $server lỗi", e)
                last = e
            }
        }
        throw last ?: java.io.IOException("Overpass lỗi")
    }

    private fun parseOverpass(json: String): List<RawPlace> {
        val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<RawPlace>(elements.length())
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val tags = e.optJSONObject("tags")?.toStringMap() ?: continue
            val name = tags["name:vi"] ?: tags["name"] ?: continue
            val center = e.optJSONObject("center")
            val lat = center?.optDouble("lat") ?: e.optDouble("lat")
            val lng = center?.optDouble("lon") ?: e.optDouble("lon")
            if (lat.isNaN() || lng.isNaN()) continue
            val prefix = when (e.optString("type")) { "node" -> "n"; "way" -> "w"; else -> "r" }
            out += RawPlace("$prefix${e.getLong("id")}", name, lat, lng, tags, addressFromTags(tags))
        }
        return out
    }

    private fun addressFromTags(tags: Map<String, String>): String =
        listOfNotNull(
            tags["addr:district"] ?: tags["addr:subdistrict"],
            tags["addr:city"] ?: tags["addr:province"],
        ).distinct().joinToString(", ")

    private fun PlaceDataset.Entry.toRaw() = RawPlace(
        id = id,
        name = name,
        lat = lat,
        lng = lng,
        // Cột wikipedia: "vi:Tên bài" (bài Wikipedia) hoặc "Q123" (chỉ có mã Wikidata). Dùng để lấy ảnh khi thiếu.
        tags = when {
            wikipedia.isBlank() -> emptyMap()
            wikipedia.matches(Regex("Q\\d+")) -> mapOf("wikidata" to wikipedia)
            else -> mapOf("wikipedia" to wikipedia)
        },
        address = address,
        category = category,
        tagline = tagline,
        score = score,
        image = imageUrl,
    )

    private fun PlaceDataset.Entry.distanceTo(p: GeoPoint) = GeoPoint(lat, lng).distanceKmTo(p)

    /** Cùng tên và cách nhau dưới 300 m (ví dụ 1 bãi biển vừa là điểm vừa là vùng): giữ 1. */
    private fun List<RawPlace>.distinctNearby(): List<RawPlace> {
        val kept = mutableListOf<RawPlace>()
        for (p in sortedByDescending { it.rank() }) {
            val dup = kept.any {
                it.name.equals(p.name, ignoreCase = true) &&
                    GeoPoint(it.lat, it.lng).distanceKmTo(GeoPoint(p.lat, p.lng)) < 0.3
            }
            if (!dup) kept += p
        }
        return kept
    }

    // ---------------- Nominatim ----------------

    /** Nominatim chỉ cho gọi tối đa 1 lần mỗi giây: chờ đủ khoảng cách rồi mới gọi. */
    private suspend fun callNominatim(url: String): String = nominatimLock.withLock {
        val wait = lastNominatimCall + NOMINATIM_GAP_MS - now()
        if (wait > 0) delay(wait)
        try {
            OsmHttp.get(url)
        } finally {
            lastNominatimCall = now()
        }
    }

    private fun parseNominatim(json: String): List<RawPlace> {
        val arr = JSONArray(json)
        val out = ArrayList<RawPlace>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val prefix = when (o.optString("osm_type")) { "node" -> "n"; "way" -> "w"; "relation" -> "r"; else -> continue }
            val names = o.optJSONObject("namedetails")?.toStringMap().orEmpty()
            val name = names["name:vi"] ?: names["name"] ?: o.optString("name").takeIf { it.isNotBlank() } ?: continue
            // Gộp extratags với "category=type" của Nominatim thành bộ thẻ giống Overpass.
            val tags = o.optJSONObject("extratags")?.toStringMap().orEmpty().toMutableMap()
            val cat = o.optString("category")
            val type = o.optString("type")
            if (cat.isNotBlank() && type.isNotBlank()) tags.putIfAbsent(cat, type)
            tags.putIfAbsent("name", name)
            val lat = o.optString("lat").toDoubleOrNull() ?: continue
            val lng = o.optString("lon").toDoubleOrNull() ?: continue
            val addr = o.optJSONObject("address")?.toStringMap().orEmpty()
            val address = listOfNotNull(
                addr["city_district"] ?: addr["suburb"] ?: addr["county"],
                addr["city"] ?: addr["town"] ?: addr["state"],
            ).filter { it != name }.distinct().joinToString(", ")
            out += RawPlace("$prefix${o.getLong("osm_id")}", name, lat, lng, tags, address)
        }
        return out
    }

    // ---------------- Tiện ích ----------------

    private fun CacheEntry.isFresh() = now() - time < CACHE_TTL_MS
    private fun now() = System.currentTimeMillis()

    private fun JSONObject.toStringMap(): Map<String, String> {
        val map = HashMap<String, String>(length())
        keys().forEach { k -> map[k] = optString(k) }
        return map
    }

    private companion object {
        const val TAG = "TravelGo"
        const val MAX_NEARBY = 60
        const val MAX_SEARCH = 20
        const val MIN_WORTHY_SCORE = 3.0   // dưới mức này coi là "điểm quá nhỏ"
        const val MIN_WORTHY_COUNT = 20    // quanh đó có ít nhất ngần này điểm đáng đi thì mới bỏ điểm nhỏ
        const val MIN_LOCAL_RESULTS = 5   // ít hơn số này thì tìm thêm trên Nominatim
        const val WIKI_WAIT_MS = 4_000L   // chờ ảnh Wikipedia tối đa 4 giây
        const val CACHE_TTL_MS = 30 * 60 * 1000L     // giữ kết quả 30 phút
        const val NOMINATIM_GAP_MS = 1100L
        val OVERPASS_SERVERS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
        )

        private fun res(name: String) = "android.resource://com.travelgo.app/drawable/$name"

        /** Thành phố nổi bật cho Trang chủ (cố định, ảnh có sẵn trong app). */
        val featured = listOf(
            Place("c1", "Đà Nẵng", "thanh_pho", "Miền Trung", 16.0544, 108.2022, 4.8, 0, res("img_da_nang")),
            Place("c2", "Nha Trang", "bien", "Khánh Hòa", 12.2388, 109.1967, 4.7, 0, res("img_nha_trang")),
            Place("c3", "Hội An", "van_hoa", "Quảng Nam", 15.8801, 108.3380, 4.9, 0, res("img_hoi_an")),
        )
    }
}
