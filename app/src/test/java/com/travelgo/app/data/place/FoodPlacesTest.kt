package com.travelgo.app.data.place

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phân loại quán ăn / quán nước và nhãn hiển thị từ thẻ OpenStreetMap. */
class FoodPlacesTest {

    @Test fun categoryOf_sortsRestaurantsAndCafes() {
        assertEquals(FoodPlaces.QUAN_AN, FoodPlaces.categoryOf(mapOf("amenity" to "restaurant")))
        assertEquals(FoodPlaces.QUAN_AN, FoodPlaces.categoryOf(mapOf("amenity" to "fast_food", "cuisine" to "burger")))
        assertEquals(FoodPlaces.QUAN_NUOC, FoodPlaces.categoryOf(mapOf("amenity" to "cafe")))
        // Tiệm trà sữa hay bị gắn fast_food: vẫn tính là quán nước.
        assertEquals(FoodPlaces.QUAN_NUOC, FoodPlaces.categoryOf(mapOf("amenity" to "fast_food", "cuisine" to "bubble_tea")))
        assertNull(FoodPlaces.categoryOf(mapOf("amenity" to "bank")))
        assertNull(FoodPlaces.categoryOf(mapOf("tourism" to "museum")))
    }

    @Test fun tagline_translatesCuisine_andSkipsRepeats() {
        assertEquals("Quán ăn · Phở", FoodPlaces.tagline(mapOf("amenity" to "restaurant", "cuisine" to "pho")))
        assertEquals("Quán ăn · Món Việt, Hải sản",
            FoodPlaces.tagline(mapOf("amenity" to "restaurant", "cuisine" to "vietnamese;seafood;unknown")))
        assertEquals("Quán cà phê", FoodPlaces.tagline(mapOf("amenity" to "cafe", "cuisine" to "coffee_shop")))
        assertEquals("Đồ ăn nhanh", FoodPlaces.tagline(mapOf("amenity" to "fast_food")))
    }

    @Test fun categoryOf_trustsShopNameOverWrongAmenity() {
        // Tiệm trà sữa, cà phê bị gắn nhầm restaurant trên OSM: theo tên quán thì là quán nước.
        assertEquals(FoodPlaces.QUAN_NUOC,
            FoodPlaces.categoryOf(mapOf("amenity" to "restaurant", "name" to "Trà sữa Healthy Lick N Bite")))
        assertEquals(FoodPlaces.QUAN_NUOC, FoodPlaces.categoryOf(mapOf("amenity" to "restaurant", "name" to "Cộng Coffee")))
        assertEquals("Tiệm trà sữa", FoodPlaces.tagline(mapOf("amenity" to "restaurant", "name" to "TRÀ SỮA BOBA")))
        // "Cafe" nằm trong từ khác thì không tính.
        assertEquals(FoodPlaces.QUAN_AN, FoodPlaces.categoryOf(mapOf("amenity" to "restaurant", "name" to "Cafeteria Lan")))
        assertEquals(FoodPlaces.QUAN_AN, FoodPlaces.categoryOf(mapOf("amenity" to "restaurant", "name" to "Phở Hùng")))
    }

    @Test fun closedShops_areDropped() {
        assertTrue(FoodPlaces.isClosed(mapOf("amenity" to "cafe", "name" to "Cafe Mộc (đã đóng cửa)")))
        assertTrue(FoodPlaces.isClosed(mapOf("amenity" to "restaurant", "name" to "Bếp Nhà - Ngừng kinh doanh")))
        assertTrue(FoodPlaces.isClosed(mapOf("amenity" to "restaurant", "name" to "Quán A", "opening_hours" to "closed")))
        assertTrue(FoodPlaces.isClosed(mapOf("amenity" to "restaurant", "name" to "Quán B", "end_date" to "2024")))
        assertNull(FoodPlaces.categoryOf(mapOf("amenity" to "cafe", "name" to "Cafe Mộc (đã đóng cửa)")))
        assertFalse(FoodPlaces.isClosed(mapOf("amenity" to "restaurant", "name" to "Bánh canh cua Đồng Khởi")))
        assertFalse(FoodPlaces.isClosed(mapOf("amenity" to "cafe", "name" to "Cafe Cửa Đông")))
    }

    @Test fun dishOf_guessesFromNameAndTagline() {
        assertEquals(FoodPlaces.Dish.TRA_SUA, FoodPlaces.dishOf("Trà sữa Phúc Long", "", FoodPlaces.QUAN_NUOC))
        assertEquals(FoodPlaces.Dish.CA_PHE, FoodPlaces.dishOf("Highlands Coffee", "Quán cà phê", FoodPlaces.QUAN_NUOC))
        assertEquals(FoodPlaces.Dish.PHO_BUN, FoodPlaces.dishOf("Phở Thìn", "Quán ăn", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.PHO_BUN, FoodPlaces.dishOf("Bún chả Hương Liên", "Quán ăn", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.HAI_SAN, FoodPlaces.dishOf("Bé Mặn", "Quán ăn · Hải sản", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.LAU_NUONG, FoodPlaces.dishOf("Lẩu dê 404", "Quán ăn", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.GA_RAN, FoodPlaces.dishOf("KFC Lê Lợi", "Đồ ăn nhanh", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.KEM, FoodPlaces.dishOf("Bạch Đằng", "Quán kem", FoodPlaces.QUAN_NUOC))
        // Không đoán được thì theo loại quán.
        assertEquals(FoodPlaces.Dish.QUAN_AN, FoodPlaces.dishOf("Nhà hàng Ngon", "Quán ăn", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.NUOC, FoodPlaces.dishOf("Góc Phố", "", FoodPlaces.QUAN_NUOC))
        // "Phố" không phải "phở"; tên gõ không dấu vẫn nhận ra.
        assertEquals(FoodPlaces.Dish.QUAN_AN, FoodPlaces.dishOf("Nhà hàng Phố Biển", "Quán ăn", FoodPlaces.QUAN_AN))
        assertEquals(FoodPlaces.Dish.PHO_BUN, FoodPlaces.dishOf("Pho Hung", "Quán ăn", FoodPlaces.QUAN_AN))
    }

    @Test fun address_usesStreetThenDistrict() {
        assertEquals("12 Lê Lợi, Hải Châu",
            FoodPlaces.address(mapOf("addr:housenumber" to "12", "addr:street" to "Lê Lợi", "addr:district" to "Hải Châu")))
        assertEquals("", FoodPlaces.address(emptyMap()))
    }

    @Test fun overpassQuery_asksAroundTheGivenPoint() {
        val q = FoodPlaces.overpassQuery(16.05, 108.2)
        assertTrue(q.contains("around:${FoodPlaces.RADIUS_M},16.05,108.2"))
        assertTrue(q.contains("restaurant|fast_food|food_court|cafe|ice_cream"))
    }
}
