package com.travelgo.app.data.place

import android.util.Log
import org.json.JSONObject

/** Ảnh và đoạn mô tả lấy từ Wikipedia cho một địa điểm. */
data class WikiInfo(val imageUrl: String, val description: String)

/**
 * Lấy ảnh + mô tả từ Wikipedia theo thẻ `wikipedia` ("vi:Cầu Rồng") hoặc `wikidata` ("Q1234") của OSM.
 * Gom nhiều điểm vào 1 lần gọi (tối đa 20 bài/lần) để đỡ tốn thời gian.
 * Lỗi ở đây không làm hỏng danh sách: điểm nào không lấy được thì để trống ảnh.
 */
internal object WikipediaClient {
    private const val TAG = "TravelGo"
    private const val BATCH = 20

    /** [refs]: id địa điểm -> thẻ OSM của nó. Trả về id -> WikiInfo. */
    suspend fun fetch(refs: Map<String, Map<String, String>>): Map<String, WikiInfo> {
        if (refs.isEmpty()) return emptyMap()
        val articleOf = mutableMapOf<String, Pair<String, String>>() // id -> (lang, title)
        val wikidataOnly = mutableMapOf<String, String>()                // id -> Qxxx

        refs.forEach { (id, tags) ->
            val wp = tags["wikipedia"]?.let(::parseArticle)
            when {
                wp != null -> articleOf[id] = wp
                tags["wikidata"] != null -> wikidataOnly[id] = tags.getValue("wikidata")
            }
        }
        runCatching { articleOf += resolveWikidata(wikidataOnly) }
            .onFailure { Log.w(TAG, "Wikidata lỗi", it) }

        val result = mutableMapOf<String, WikiInfo>()
        articleOf.entries.groupBy({ it.value.first }, { it.key to it.value.second }).forEach { (lang, items) ->
            items.chunked(BATCH).forEach { chunk ->
                runCatching {
                    val pages = queryPages(lang, chunk.map { it.second })
                    chunk.forEach { (id, title) -> pages[title]?.let { result[id] = it } }
                }.onFailure { Log.w(TAG, "Wikipedia $lang lỗi", it) }
            }
        }
        return result
    }

    /** "vi:Cầu Rồng" -> ("vi", "Cầu Rồng"). */
    private fun parseArticle(value: String): Pair<String, String>? {
        val i = value.indexOf(':')
        if (i <= 0 || i == value.lastIndex) return null
        return value.substring(0, i) to value.substring(i + 1)
    }

    /** Qxxx -> bài viết tiếng Việt (ưu tiên) hoặc tiếng Anh. */
    private suspend fun resolveWikidata(ids: Map<String, String>): Map<String, Pair<String, String>> {
        val out = mutableMapOf<String, Pair<String, String>>()
        ids.entries.chunked(50).forEach { chunk ->
            val q = chunk.joinToString("|") { it.value }
            val json = JSONObject(
                OsmHttp.get(
                    "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&props=sitelinks" +
                        "&sitefilter=viwiki%7Cenwiki&ids=${OsmHttp.encode(q)}",
                ),
            )
            val entities = json.optJSONObject("entities") ?: return@forEach
            chunk.forEach { (placeId, qid) ->
                val links = entities.optJSONObject(qid)?.optJSONObject("sitelinks") ?: return@forEach
                val vi = links.optJSONObject("viwiki")?.optString("title")
                val en = links.optJSONObject("enwiki")?.optString("title")
                when {
                    !vi.isNullOrBlank() -> out[placeId] = "vi" to vi
                    !en.isNullOrBlank() -> out[placeId] = "en" to en
                }
            }
        }
        return out
    }

    /** Gọi 1 lần cho tối đa 20 bài. Trả về: tiêu đề gốc -> WikiInfo. */
    private suspend fun queryPages(lang: String, titles: List<String>): Map<String, WikiInfo> {
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&format=json&formatversion=2" +
            "&prop=pageimages%7Cextracts&piprop=thumbnail&pithumbsize=500" +
            "&exintro=1&explaintext=1&exsentences=3&exlimit=max&redirects=1" +
            "&titles=${OsmHttp.encode(titles.joinToString("|"))}"
        val query = JSONObject(OsmHttp.get(url)).optJSONObject("query") ?: return emptyMap()

        // Wikipedia có thể đổi tên tiêu đề (chuẩn hoá, chuyển hướng): lần ngược về tiêu đề gốc.
        val renamed = mutableMapOf<String, String>()
        listOf("normalized", "redirects").forEach { key ->
            val arr = query.optJSONArray(key) ?: return@forEach
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                renamed[o.getString("from")] = o.getString("to")
            }
        }
        fun finalTitle(t: String): String {
            var cur = t
            repeat(3) { cur = renamed[cur] ?: return cur }
            return cur
        }

        val byTitle = mutableMapOf<String, WikiInfo>()
        val pages = query.optJSONArray("pages") ?: return emptyMap()
        for (i in 0 until pages.length()) {
            val p = pages.getJSONObject(i)
            if (p.optBoolean("missing")) continue
            val image = p.optJSONObject("thumbnail")?.optString("source").orEmpty()
            val text = p.optString("extract").trim()
            byTitle[p.getString("title")] = WikiInfo(image, text)
        }
        return titles.mapNotNull { t -> byTitle[finalTitle(t)]?.let { t to it } }.toMap()
    }

    /** Ảnh ghi thẳng trong thẻ OSM (`image`, `wikimedia_commons`), nếu có. */
    fun imageFromTags(tags: Map<String, String>): String {
        val commons = listOfNotNull(tags["wikimedia_commons"], tags["image"])
            .firstOrNull { it.startsWith("File:") || it.contains("commons.wikimedia.org/wiki/File:") }
        if (commons != null) {
            val file = commons.substringAfter("File:")
            return "https://commons.wikimedia.org/wiki/Special:FilePath/${OsmHttp.encode(file).replace("+", "%20")}?width=500"
        }
        val direct = tags["image"]
        return if (direct != null && direct.startsWith("https://") &&
            direct.substringBefore('?').lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") }
        ) direct else ""
    }
}
