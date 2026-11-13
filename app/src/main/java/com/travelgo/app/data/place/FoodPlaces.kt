package com.travelgo.app.data.place

import java.text.Normalizer

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

    /** Thẻ OSM là quán ăn uống (kể cả quán đã đóng cửa). */
    fun isFoodAmenity(tags: Map<String, String>): Boolean =
        tags["amenity"].let { it in FOOD_AMENITIES || it in DRINK_AMENITIES }

    /**
     * Loại quán: [QUAN_AN], [QUAN_NUOC], hoặc null nếu không phải quán ăn uống / đã đóng cửa.
     * Tên quán ("Trà sữa…", "… Coffee") được ưu tiên hơn thẻ amenity, vì trên OSM nhiều tiệm trà sữa,
     * cà phê ở Việt Nam bị gắn nhầm là restaurant.
     */
    fun categoryOf(tags: Map<String, String>): String? {
        val amenity = tags["amenity"] ?: return null
        if (amenity !in FOOD_AMENITIES && amenity !in DRINK_AMENITIES) return null
        if (isClosed(tags)) return null
        val cuisines = cuisines(tags)
        return when {
            amenity in DRINK_AMENITIES -> QUAN_NUOC
            drinkKind(tags["name"].orEmpty()) != null -> QUAN_NUOC
            // Ví dụ tiệm trà sữa gắn amenity=fast_food + cuisine=bubble_tea: vẫn là quán nước.
            cuisines.isNotEmpty() && cuisines.all { it in DRINK_CUISINES } -> QUAN_NUOC
            else -> QUAN_AN
        }
    }

    /**
     * Dấu hiệu quán đã đóng cửa mà OSM có ghi: giờ mở cửa "closed", có ngày kết thúc (end_date),
     * hoặc tên kèm chữ "đã đóng cửa", "ngừng kinh doanh", "closed". Quán đóng hẳn thường đã được đổi thẻ
     * thành disused:amenity nên không có trong dữ liệu. Quán đóng mà chưa ai sửa trên OSM thì không bắt được;
     * người dùng chạm vào quán để xem trạng thái trên Google Maps.
     */
    fun isClosed(tags: Map<String, String>): Boolean {
        val hours = tags["opening_hours"].orEmpty().trim().lowercase()
        if (hours == "closed" || hours == "off") return true
        if (!tags["end_date"].isNullOrBlank()) return true
        return nameHas(tags["name"].orEmpty(), CLOSED_HINTS)
    }

    private val CLOSED_HINTS = listOf("đã đóng cửa", "ngừng kinh doanh", "ngừng hoạt động", "tạm ngừng", "closed", "permanently closed")

    /**
     * "Tiệm trà sữa" / "Quán cà phê" nếu tên quán cho thấy vậy, null nếu không.
     * Tên có cả món ăn ("Phở & Coffee", "Nhà hàng - Cà phê") thì vẫn là quán ăn.
     */
    private fun drinkKind(name: String): String? = when {
        nameHas(name, FOOD_WORDS) || DISH_HINTS.any { (dish, words) -> dish in MEAL_DISHES && nameHas(name, words) } -> null
        nameHas(name, TEA_HINTS) -> "Tiệm trà sữa"
        nameHas(name, COFFEE_HINTS) -> "Quán cà phê"
        else -> null
    }

    private val TEA_HINTS = listOf("trà sữa", "milk tea", "milktea", "boba", "trà chanh", "trà trái cây")
    private val COFFEE_HINTS = listOf("cà phê", "cafe", "café", "coffee", "caphe", "cf")
    private val FOOD_WORDS = listOf(
        "nhà hàng", "quán ăn", "ẩm thực", "restaurant", "food", "foods", "kitchen", "bistro", "buffet", "pasta", "sushi",
        "bếp", "món", "điểm tâm", "chay", "vegetarian", "vegan",
    )

    /** Viết thường, bỏ dấu (đ → d), để so khớp tên quán gõ không dấu. */
    fun plain(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()

    /**
     * [text] có chứa một trong [words] như một từ riêng. Từ khoá viết có dấu: "phở" khác "phố", "trà" khác "cá tra".
     * Riêng tên gõ không dấu ("Pho Hung") thì so theo bản bỏ dấu.
     */
    private fun nameHas(text: String, words: List<String>): Boolean {
        val accented = Normalizer.normalize(text, Normalizer.Form.NFC).lowercase()
        val noMarks = plain(text).takeIf { it == accented }
        return words.any { w -> hasWord(accented, w) || (noMarks != null && hasWord(noMarks, plain(w))) }
    }

    /** [word] xuất hiện như một từ riêng trong [text], không dính vào từ khác. */
    private fun hasWord(text: String, word: String): Boolean {
        var from = 0
        while (true) {
            val i = text.indexOf(word, from)
            if (i < 0) return false
            val before = i == 0 || !text[i - 1].isLetterOrDigit()
            val after = i + word.length >= text.length || !text[i + word.length].isLetterOrDigit()
            if (before && after) return true
            from = i + 1
        }
    }

    /** Nhóm món để vẽ hình minh hoạ trên thẻ quán chưa có ảnh. */
    enum class Dish { TRA_SUA, CA_PHE, BANH_NGOT, KEM, PIZZA, BURGER, GA_RAN, CHAY, LAU_NUONG, HAI_SAN, BANH_MI, PHO_BUN, COM, NUOC, QUAN_AN }

    private val MEAL_DISHES = setOf(
        Dish.PIZZA, Dish.BURGER, Dish.GA_RAN, Dish.LAU_NUONG, Dish.HAI_SAN, Dish.BANH_MI, Dish.PHO_BUN, Dish.COM,
    )

    private val DISH_HINTS: List<Pair<Dish, List<String>>> = listOf(
        Dish.TRA_SUA to TEA_HINTS + listOf("trà", "tea"),
        Dish.CA_PHE to COFFEE_HINTS,
        Dish.BANH_NGOT to listOf("bánh ngọt", "bánh kem", "tiệm bánh", "bakery", "cake", "dessert", "tráng miệng", "chè"),
        Dish.KEM to listOf("kem", "ice cream", "gelato"),
        Dish.PIZZA to listOf("pizza", "món ý"),
        Dish.BURGER to listOf("burger", "mcdonald's", "mcdonalds"),
        Dish.GA_RAN to listOf("gà rán", "kfc", "lotteria", "jollibee", "popeyes", "texas chicken", "fried chicken"),
        Dish.CHAY to listOf("chay", "vegetarian", "vegan"),
        Dish.LAU_NUONG to listOf("lẩu", "nướng", "bbq", "hotpot", "nhậu", "bia"),
        Dish.HAI_SAN to listOf("hải sản", "seafood", "ốc"),
        Dish.BANH_MI to listOf("bánh mì", "bánh bao", "sandwich"),
        Dish.PHO_BUN to listOf("phở", "bún", "mì", "miến", "hủ tiếu", "bánh canh", "noodle", "ramen", "udon"),
        Dish.COM to listOf("cơm", "rice"),
    )

    /** Đoán nhóm món từ tên quán và nhãn (đã dịch từ cuisine). Không đoán được thì theo loại quán. */
    fun dishOf(name: String, tagline: String, category: String): Dish {
        DISH_HINTS.firstOrNull { (_, words) -> nameHas(name, words) || nameHas(tagline, words) }?.let { return it.first }
        return if (category == QUAN_NUOC) Dish.NUOC else Dish.QUAN_AN
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
        val byName = drinkKind(tags["name"].orEmpty())
        val base = when {
            byName != null -> byName
            tags["amenity"] == "cafe" -> "Quán cà phê"
            tags["amenity"] == "ice_cream" -> "Quán kem"
            tags["amenity"] == "fast_food" -> "Đồ ăn nhanh"
            tags["amenity"] == "food_court" -> "Khu ẩm thực"
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
