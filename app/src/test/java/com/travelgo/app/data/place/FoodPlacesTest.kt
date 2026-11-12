package com.travelgo.app.data.place

import org.junit.Assert.assertEquals
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
