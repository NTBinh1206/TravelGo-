package com.travelgo.app.data.place

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dựng nội dung và đọc câu trả lời của API ghi chú OpenStreetMap (mẫu theo định dạng JSON của API v0.6). */
class OsmNotesTest {

    private val cafe = Place("n4516891589", "Gà Rán 28", "quan_an", "", 9.17713, 105.14927, 0.0, 0, "")

    @Test fun elementUrl_mapsOsmIds_andRejectsOthers() {
        assertEquals("https://www.openstreetmap.org/node/4516891589", OsmNotes.elementUrl("n4516891589"))
        assertEquals("https://www.openstreetmap.org/way/39514793", OsmNotes.elementUrl("w39514793"))
        assertEquals("https://www.openstreetmap.org/relation/7", OsmNotes.elementUrl("r7"))
        assertNull(OsmNotes.elementUrl("c1"))   // thành phố nổi bật do nhóm tự thêm
        assertNull(OsmNotes.elementUrl("n"))
        assertFalse(OsmNotes.canReport(cafe.copy(id = "fake-1")))
    }

    @Test fun noteText_hasName_count_link_andHashtag() {
        val text = OsmNotes.noteText(cafe, 2)
        assertTrue("Gà Rán 28" in text)
        assertTrue("2 người dùng" in text && "2 users" in text)
        assertTrue("https://www.openstreetmap.org/node/4516891589" in text)
        assertTrue(text.endsWith(OsmNotes.HASHTAG))
        val form = OsmNotes.createForm(cafe, 2)
        assertEquals("9.1771300", form["lat"])
        assertEquals("105.1492700", form["lon"])
    }

    @Test fun searchUrl_usesLeftBottomRightTop_andOpenNotesOnly() {
        assertEquals(
            "https://api.openstreetmap.org/api/0.6/notes.json?bbox=105.148770,9.176630,105.149770,9.177630&closed=0&limit=50",
            OsmNotes.searchUrl(cafe),
        )
    }

    @Test fun findExisting_matchesSameElementOnly() {
        val json = """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","geometry":{"type":"Point","coordinates":[105.1493,9.1771]},
               "properties":{"id":11,"status":"open","comments":[
                 {"action":"opened","text":"Quán khác https://www.openstreetmap.org/node/45168915890 #TravelGo"}]}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[105.1493,9.1771]},
               "properties":{"id":12,"status":"closed","comments":[
                 {"action":"opened","text":"Đã xử lý https://www.openstreetmap.org/node/4516891589"}]}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[105.1493,9.1771]},
               "properties":{"id":13,"status":"open","comments":[
                 {"action":"opened","text":"Có thể đã đóng"},
                 {"action":"commented","text":"xem https://www.openstreetmap.org/node/4516891589\n#TravelGo"}]}}
            ]}
        """.trimIndent()
        assertEquals(13L, OsmNotes.findExisting(json, cafe))
        assertNull(OsmNotes.findExisting("""{"type":"FeatureCollection","features":[]}""", cafe))
    }

    @Test fun parseCreatedId_readsFeatureProperties() {
        val json = """
            {"type":"Feature","geometry":{"type":"Point","coordinates":[105.1492700,9.1771300]},
             "properties":{"id":4823011,"url":"https://api.openstreetmap.org/api/0.6/notes/4823011.json",
                           "status":"open","comments":[{"action":"opened","text":"…"}]}}
        """.trimIndent()
        assertEquals(4823011L, OsmNotes.parseCreatedId(json))
        assertNull(OsmNotes.parseCreatedId("""{"type":"Feature","properties":{}}"""))
    }
}
