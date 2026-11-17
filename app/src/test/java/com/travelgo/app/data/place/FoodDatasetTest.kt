package com.travelgo.app.data.place

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Đọc file food_vn.tsv do tools/build_food_vn.py tạo ra. */
class FoodDatasetTest {

    private val sample = """
        id	name	amenity	cuisine	address	lat	lng
        n1	Phở mẫu	restaurant	vietnamese;pho	12 Lê Lợi, Quận 1	10.77690	106.70090
        w2	Cà phê mẫu	cafe		Quận 3	10.78000	106.69000
        n3	Thiếu toạ độ	cafe				
        	Thiếu id	cafe			10.7	106.7
    """.trimIndent().lineSequence()

    @Test fun parse_readsRows_andSkipsBrokenOnes() {
        val list = FoodDataset.parse(sample)
        assertEquals(listOf("n1", "w2"), list.map { it.id })
        assertEquals("12 Lê Lợi, Quận 1", list[0].address)
        assertEquals(10.7769, list[0].lat, 1e-6)
    }

    @Test fun parse_givesTagsThatFoodPlacesUnderstands() {
        val (pho, cafe) = FoodDataset.parse(sample)
        assertEquals(FoodPlaces.QUAN_AN, FoodPlaces.categoryOf(pho.tags))
        assertEquals("Quán ăn · Món Việt, Phở", FoodPlaces.tagline(pho.tags))
        assertEquals(FoodPlaces.QUAN_NUOC, FoodPlaces.categoryOf(cafe.tags))
        assertFalse(cafe.tags.containsKey("cuisine"))
    }
}
