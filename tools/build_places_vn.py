"""Tạo app/src/main/assets/places_vn.tsv từ file Overpass (xem tools/README_du_lieu.md).

Cách dùng:  python tools/build_places_vn.py osm_vn.tsv app/src/main/assets/places_vn.tsv

- Xếp mỗi điểm vào 1 trong 5 loại, nhãn ngắn, điểm "nổi bật" (cùng quy tắc với data/place/OsmCategory.kt).
- Bỏ điểm trùng (cùng tên, cách nhau < 300 m), bỏ dòng lỗi.
- Thành phố / thị xã / thị trấn (place=city|town) đánh dấu isTown=1: chỉ dùng cho Tìm kiếm.
"""
import csv
import math
import sys
import unicodedata
from urllib.parse import quote


def plain(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = "".join(ch for ch in s if unicodedata.category(ch) != "Mn")
    return s.replace("đ", "d").replace("Đ", "D").lower()


NAME_RULES = [
    (["bien", "bai tam", "vinh", "dao", "mui"], "bien"),
    (["nui", "deo", "hang", "dong", "son"], "nui"),
    (["chua", "den", "dinh", "mieu", "nha tho", "lang", "thanh co", "bao tang", "di tich", "pho co", "thap"], "van_hoa"),
    (["ho", "thac", "suoi", "rung", "vuon quoc gia", "khu bao ton", "song"], "thien_nhien"),
]


def category(t: dict, name: str) -> str:
    natural, tourism, leisure, place = t.get("natural"), t.get("tourism"), t.get("leisure"), t.get("place")
    if natural in {"beach", "bay", "cape", "coastline"} or leisure == "beach_resort" or place in {"island", "islet"}:
        return "bien"
    if natural in {"peak", "volcano", "ridge", "hill", "cave_entrance", "cliff", "saddle", "valley"}:
        return "nui"
    if (t.get("historic") or t.get("amenity") == "place_of_worship" or tourism in {"museum", "gallery"}
            or t.get("heritage") or t.get("building") in {"temple", "pagoda", "church", "cathedral", "shrine"}):
        return "van_hoa"
    if (natural in {"waterfall", "water", "wood", "spring", "hot_spring", "wetland", "grassland"}
            or t.get("waterway") == "waterfall" or leisure in {"nature_reserve", "park", "garden"}
            or t.get("boundary") in {"national_park", "protected_area"} or tourism == "viewpoint"):
        return "thien_nhien"
    if place in {"city", "town", "suburb", "quarter", "square"}:
        return "thanh_pho"
    text = f" {plain(name)} "
    for words, cat in NAME_RULES:
        if any(f" {w} " in text for w in words):
            return cat
    return "thanh_pho"


def tagline(t: dict, cat: str) -> str:
    n = t.get("natural")
    if n == "beach": return "Bãi biển"
    if n in {"peak", "volcano"}: return "Núi"
    if n == "cave_entrance": return "Hang động"
    if n == "waterfall" or t.get("waterway") == "waterfall": return "Thác nước"
    if n == "bay": return "Vịnh"
    if n == "cape": return "Mũi"
    if t.get("place") in {"island", "islet"}: return "Đảo"
    if t.get("tourism") == "museum": return "Bảo tàng"
    if t.get("tourism") == "gallery": return "Phòng tranh"
    if t.get("tourism") == "viewpoint": return "Điểm ngắm cảnh"
    if t.get("tourism") == "theme_park" or t.get("leisure") == "water_park": return "Khu vui chơi"
    if t.get("tourism") in {"zoo", "aquarium"}: return "Vườn thú · Thủy cung"
    if t.get("amenity") == "place_of_worship":
        return {"buddhist": "Chùa", "christian": "Nhà thờ"}.get(t.get("religion"), "Đền · Miếu")
    if t.get("historic"): return "Di tích lịch sử"
    if t.get("leisure") in {"park", "garden"}: return "Công viên"
    if t.get("leisure") == "nature_reserve" or t.get("boundary") == "national_park": return "Khu bảo tồn"
    if t.get("place") == "city": return "Thành phố"
    if t.get("place") == "town": return "Thị xã · Thị trấn"
    return {"bien": "Biển", "nui": "Núi", "van_hoa": "Văn hóa", "thien_nhien": "Thiên nhiên"}.get(cat, "Điểm tham quan")


def score(t: dict) -> float:
    s = 2.5
    if t.get("wikipedia") or t.get("wikidata"): s += 1.0
    if t.get("tourism") in {"attraction", "theme_park", "zoo", "aquarium", "museum"}: s += 0.5
    # Cảnh quan thiên nhiên (bãi biển, thác, hang, vịnh, đảo…) là điểm đáng đi dù ít khi có bài Wikipedia.
    if (t.get("natural") in {"beach", "waterfall", "cave_entrance", "bay", "cape", "hot_spring", "peak", "volcano"}
            or t.get("waterway") == "waterfall" or t.get("place") in {"island", "islet"}
            or t.get("leisure") == "nature_reserve" or t.get("boundary") == "national_park"):
        s += 0.5
    if t.get("heritage") or t.get("historic"): s += 0.5
    if t.get("image") or t.get("wikimedia_commons"): s += 0.3
    if t.get("name:en"): s += 0.2
    return round(min(s, 5.0), 1)


def image(t: dict) -> str:
    for v in (t.get("wikimedia_commons", ""), t.get("image", "")):
        if v.startswith("File:") or "commons.wikimedia.org/wiki/File:" in v:
            f = v.split("File:", 1)[1]
            return f"https://commons.wikimedia.org/wiki/Special:FilePath/{quote(f)}?width=500"
    v = t.get("image", "")
    if v.startswith("https://") and v.split("?")[0].lower().endswith((".jpg", ".jpeg", ".png")):
        return v
    return ""


def km(a, b):
    dlat = math.radians(b[0] - a[0])
    dlng = math.radians(b[1] - a[1])
    h = math.sin(dlat / 2) ** 2 + math.cos(math.radians(a[0])) * math.cos(math.radians(b[0])) * math.sin(dlng / 2) ** 2
    return 12742 * math.asin(math.sqrt(h))


def clean(v: str) -> str:
    return " ".join(v.replace("\t", " ").split())


def main(src: str, dst: str) -> None:
    with open(src, encoding="utf-8", newline="") as f:
        rows = list(csv.reader(f, delimiter="\t", quoting=csv.QUOTE_NONE))
    header, items, bad = rows[0], [], 0
    for r in rows[1:]:
        if len(r) != len(header):
            bad += 1
            continue
        t = {h: clean(v) for h, v in zip(header, r) if v.strip()}
        name = t.get("name:vi") or t.get("name")
        try:
            lat, lng = float(t["@lat"]), float(t["@lon"])
        except (KeyError, ValueError):
            bad += 1
            continue
        if not name:
            bad += 1
            continue
        prefix = {"node": "n", "way": "w", "relation": "r"}.get(t.get("@type"))
        if not prefix:
            bad += 1
            continue
        cat = category(t, name)
        town = t.get("place") in {"city", "town"}
        address = ", ".join(dict.fromkeys(x for x in (t.get("addr:district"), t.get("addr:city") or t.get("addr:province")) if x))
        wiki = t.get("wikipedia") or t.get("wikidata") or ""
        items.append([prefix + t["@id"], name, cat, lat, lng, score(t), tagline(t, cat), address, image(t), wiki, town])

    # Bỏ trùng: cùng tên (không dấu) và cách nhau < 300 m -> giữ điểm có điểm số cao hơn.
    items.sort(key=lambda x: -x[5])
    kept, by_name, dup = [], {}, 0
    for it in items:
        key = plain(it[1])
        if any(km((it[3], it[4]), (o[3], o[4])) < 0.3 for o in by_name.get(key, [])):
            dup += 1
            continue
        by_name.setdefault(key, []).append(it)
        kept.append(it)

    kept.sort(key=lambda x: (x[10], -x[5], x[1]))
    with open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write("id\tname\tcategory\tlat\tlng\tscore\ttagline\taddress\timageUrl\twikipedia\tisTown\n")
        for i in kept:
            f.write("\t".join([i[0], i[1], i[2], f"{i[3]:.6f}", f"{i[4]:.6f}", str(i[5]), i[6], i[7], i[8], i[9], "1" if i[10] else "0"]) + "\n")

    from collections import Counter
    c = Counter("town" if i[10] else i[2] for i in kept)
    print(f"Ghi {len(kept)} điểm (bỏ {bad} dòng lỗi, {dup} điểm trùng) -> {dst}")
    print(dict(c))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
