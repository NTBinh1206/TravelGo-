"""Tạo app/src/main/assets/food_vn.tsv: quán ăn, quán nước toàn Việt Nam từ OpenStreetMap.

Cách dùng (chạy ở thư mục gốc project):
    python tools/build_food_vn.py                       # tự tải từ Overpass rồi tạo file
    python tools/build_food_vn.py --raw tools/food_vn_raw.tsv   # đã có file thô (vd. tải bằng overpass-turbo.eu)

- Chỉ dùng thư viện có sẵn của Python 3 (không cần pip install).
- Tải theo từng dải vĩ độ (Bắc → Nam) để mỗi lần hỏi máy chủ không quá lớn; dải nào xong được lưu lại
  trong tools/food_raw_parts/, chạy lại sẽ bỏ qua dải đã có (mạng rớt giữa chừng không phải tải lại từ đầu).
- Cột của food_vn.tsv: id name amenity cuisine address lat lng
  Loại quán, nhãn món do app tự tính từ amenity + cuisine (data/place/FoodPlaces.kt), không tính ở đây.
"""
import argparse
import csv
import gzip
import io
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
QUERY_FILE = os.path.join(HERE, "food_vn_query.overpassql")
PARTS_DIR = os.path.join(HERE, "food_raw_parts")
RAW_OUT = os.path.join(HERE, "food_vn_raw.tsv")
ASSET_OUT = os.path.join(HERE, "..", "app", "src", "main", "assets", "food_vn.tsv")

SERVERS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
]

# Việt Nam nằm trong khoảng vĩ độ 8.2–23.5, kinh độ 102.1–109.7. Cắt thành các dải 1 độ vĩ.
LAT_BANDS = [(lat, lat + 1.0) for lat in range(8, 24)]
LNG_MIN, LNG_MAX = 102.0, 110.0


def fetch(query: str) -> str:
    data = urllib.parse.urlencode({"data": query}).encode()
    last = None
    for attempt in range(3):
        for server in SERVERS:
            try:
                req = urllib.request.Request(server, data=data, headers={"User-Agent": "TravelGo-build-food/1.0"})
                with urllib.request.urlopen(req, timeout=700) as r:
                    text = r.read().decode("utf-8")
                if "runtime error" in text[:2000] or text.lstrip().startswith("<"):
                    raise RuntimeError(text[:300])
                return text
            except Exception as e:  # noqa: BLE001 - thử máy chủ khác
                last = e
                print(f"    lỗi ở {server.split('/')[2]}: {str(e)[:120]}", flush=True)
                time.sleep(5)
        time.sleep(20 * (attempt + 1))
    raise RuntimeError(f"Không tải được sau nhiều lần thử: {last}")


def download() -> str:
    template = open(QUERY_FILE, encoding="utf-8").read()
    os.makedirs(PARTS_DIR, exist_ok=True)
    header, rows = None, []
    for i, (s, n) in enumerate(LAT_BANDS, 1):
        part = os.path.join(PARTS_DIR, f"band_{s:02d}.tsv")
        if not os.path.exists(part):
            print(f"[{i}/{len(LAT_BANDS)}] Tải dải vĩ độ {s}–{n}…", flush=True)
            started = time.time()
            text = fetch(template.replace("{{bbox}}", f"({s},{LNG_MIN},{n},{LNG_MAX})"))
            with open(part, "w", encoding="utf-8", newline="") as f:
                f.write(text)
            print(f"    xong {text.count(chr(10)) - 1} quán trong {time.time() - started:.0f} giây", flush=True)
            time.sleep(3)  # lịch sự với máy chủ dùng chung
        lines = open(part, encoding="utf-8").read().splitlines()
        if lines:
            header = header or lines[0]
            rows += lines[1:]
    raw = "\n".join([header or ""] + rows) + "\n"
    with open(RAW_OUT, "w", encoding="utf-8", newline="") as f:
        f.write(raw)
    print(f"Đã lưu file thô: {RAW_OUT}")
    return raw


def address(r: dict) -> str:
    """Cùng quy tắc với FoodPlaces.address() trong app."""
    street = " ".join(x for x in [r.get("addr:housenumber", ""), r.get("addr:street", "")] if x).strip()
    area = r.get("addr:district") or r.get("addr:subdistrict") or r.get("addr:city") or ""
    parts = [p for p in [street, area] if p]
    out = []
    for p in parts:
        if p not in out:
            out.append(p)
    return ", ".join(out)


def clean(s: str) -> str:
    return " ".join((s or "").replace("\t", " ").split())


def build(raw: str) -> None:
    reader = csv.DictReader(io.StringIO(raw), delimiter="\t", quoting=csv.QUOTE_NONE)
    seen, out = set(), []
    for r in reader:
        kind = (r.get("@type") or "")[:1]
        if kind not in ("n", "w", "r"):
            continue
        pid = f"{kind}{r.get('@id')}"
        name = clean(r.get("name:vi") or r.get("name"))
        try:
            lat, lng = float(r["@lat"]), float(r["@lon"])
        except (KeyError, TypeError, ValueError):
            continue
        if not name or pid in seen:
            continue
        seen.add(pid)
        out.append([pid, name, clean(r.get("amenity")), clean(r.get("cuisine")).lower(), clean(address(r)),
                    f"{lat:.5f}", f"{lng:.5f}"])

    out.sort(key=lambda row: (float(row[5]), float(row[6])))
    os.makedirs(os.path.dirname(ASSET_OUT), exist_ok=True)
    with open(ASSET_OUT, "w", encoding="utf-8", newline="") as f:
        f.write("id\tname\tamenity\tcuisine\taddress\tlat\tlng\n")
        for row in out:
            f.write("\t".join(row) + "\n")

    size = os.path.getsize(ASSET_OUT)
    zipped = len(gzip.compress(open(ASSET_OUT, "rb").read()))
    print()
    print(f"Xong: {len(out):,} quán -> {os.path.normpath(ASSET_OUT)}")
    print(f"Dung lượng file: {size / 1e6:.1f} MB, nén trong APK khoảng {zipped / 1e6:.1f} MB")
    by = {}
    for row in out:
        by[row[2]] = by.get(row[2], 0) + 1
    print("Theo loại:", ", ".join(f"{k} {v:,}" for k, v in sorted(by.items(), key=lambda kv: -kv[1])))


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--raw", help="file thô đã tải sẵn (bỏ qua bước tải)")
    args = ap.parse_args()
    if sys.version_info < (3, 7):
        sys.exit("Can Python 3.7 tro len.")
    try:  # cửa sổ lệnh Windows cũ không in được tiếng Việt thì thay ký tự lỗi, không dừng chương trình
        sys.stdout.reconfigure(errors="replace")
    except AttributeError:
        pass
    raw = open(args.raw, encoding="utf-8").read() if args.raw else download()
    build(raw)


if __name__ == "__main__":
    main()
