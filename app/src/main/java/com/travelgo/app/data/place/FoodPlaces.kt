package com.travelgo.app.data.place

/**
 * Quán ăn, quán nước lấy từ OpenStreetMap (thẻ amenity=restaurant, cafe…).
 *
 * Lưu ý: OpenStreetMap chỉ cho biết ở đâu có quán và quán bán gì, KHÔNG có điểm đánh giá.
 * Vì vậy ứng dụng không tự gọi quán nào là "ngon": người dùng chạm vào quán để xem đánh giá
 * trên Google Maps.
 *
 * Phần này không dùng Android nên kiểm thử được bằng unit test thường (FoodPlacesTest).
 */
object FoodPlaces {
    /** Hai loại mới, dùng trong Place.category. */
    const val QUAN_AN = "quan_an"
    const val QUAN_NUOC = "quan_nuoc"
    val CATEGORIES = setOf(QUAN_AN, QUAN_NUOC)

    /** Tìm quán trong bán kính này quanh vị trí (mét). Quán ăn dày đặc nên không cần xa như điểm du lịch. */
    const val RADIUS_M = 2_000

    /** Giữ tối đa ngần này quán gần nhất. */
    const val MAX_RESULTS = 60

    private val FOOD_AMENITIES = setOf("restaurant", "fast_food", "food_court")
    private val DRINK_AMENITIES = setOf("cafe", "ice_cream")
    private val DRINK_CUISINES = setOf("coffee_shop", "bubble_tea", "tea", "juice", "smoothie", "ice_cream")

    fun isFood(category: String): Boolean = category in CATEGORIES

    /** Loại quán: [QUAN_AN], [QUAN_NUOC], hoặc null nếu không phải quán ăn uống. */
    fun categoryOf(tags: Map<String, String>): String? {
        val amenity = tags["amenity"] ?: return null
        val cuisines = cuisines(tags)
        return when {
            amenity in DRINK_AMENITIES -> QUAN_NUOC
            // Ví dụ tiệm trà sữa gắn amenity=fast_food + cuisine=bubble_tea: vẫn là quán nước.
            amenity in FOOD_AMENITIES && cuisines.isNotEmpty() && cuisines.all { it in DRINK_CUISINES } -> QUAN_NUOC
            amenity in FOOD_AMENITIES -> QUAN_AN
            else -> null
        }
    }

    /** Thẻ cuisine của OSM có thể ghi nhiều món: "vietnamese;noodle". */
    fun cuisines(tags: Map<String, String>): List<String> =
        tags["cuisine"].orEmpty()
            .split(';', ',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    private val CUISINE_VI = mapOf(
        "vietnamese" to "Món Việt", "pho" to "Phở", "noodle" to "Bún, phở, mì", "seafood" to "Hải sản",
        "rice" to "Cơm", "bbq" to "Đồ nướng", "barbecue" to "Đồ nướng", "hotpot" to "Lẩu",
        "vegetarian" to "Món chay", "vegan" to "Món chay", "sandwich" to "Bánh mì", "chicken" to "Gà rán",
        "fried_chicken" to "Gà rán", "burger" to "Burger", "pizza" to "Pizza", "japanese" to "Món Nhật",
        "sushi" to "Sushi", "korean" to "Món Hàn", "chinese" to "Món Hoa", "thai" to "Món Thái",
        "indian" to "Món Ấn", "italian" to "Món Ý", "french" to "Món Pháp", "steak_house" to "Bò bít tết",
        "coffee_shop" to "Cà phê", "bubble_tea" to "Trà sữa", "tea" to "Trà", "juice" to "Nước ép",
        "smoothie" to "Sinh tố", "ice_cream" to "Kem", "cake" to "Bánh ngọt", "dessert" to "Tráng miệng",
        "breakfast" to "Ăn sáng",
    )

    /** Mô tả ngắn hiện trên thẻ, ví dụ "Quán ăn · Phở", "Quán cà phê", "Đồ ăn nhanh · Gà rán". */
    fun tagline(tags: Map<String, String>): String {
        val base = when (tags["amenity"]) {
            "cafe" -> "Quán cà phê"
            "ice_cream" -> "Quán kem"
            "fast_food" -> "Đồ ăn nhanh"
            "food_court" -> "Khu ẩm thực"
            else -> "Quán ăn"
        }
        val dishes = cuisines(tags)
            .mapNotNull { CUISINE_VI[it] }
            .distinct()
            .filterNot { base.contains(it, ignoreCase = true) } // "Quán cà phê · Cà phê" thì bỏ phần lặp
            .take(2)
        return if (dishes.isEmpty()) base else "$base · ${dishes.joinToString(", ")}"
    }

    /** Địa chỉ từ thẻ OSM: "12 Lê Lợi, Hải Châu". Thiếu thì trả chuỗi rỗng. */
    fun address(tags: Map<String, String>): String {
        val street = listOfNotNull(tags["addr:housenumber"], tags["addr:street"]).joinToString(" ")
        val area = tags["addr:district"] ?: tags["addr:subdistrict"] ?: tags["addr:city"]
        return listOfNotNull(street.ifBlank { null }, area).distinct().joinToString(", ")
    }

    /** Ngồi quán ước lượng (phút), dùng khi quán được đưa vào lộ trình. */
    fun stayMinutes(category: String): Int = if (category == QUAN_NUOC) 30 else 45

    /** Câu hỏi Overpass: mọi quán có tên trong bán kính [radiusM] mét quanh (lat, lng). */
    fun overpassQuery(lat: Double, lng: Double, radiusM: Int = RADIUS_M): String =
        "[out:json][timeout:25];" +
            "nwr[\"amenity\"~\"^(restaurant|fast_food|food_court|cafe|ice_cream)\$\"][\"name\"]" +
            "(around:$radiusM,$lat,$lng);" +
            "out center tags 1000;"
}
