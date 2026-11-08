# Bộ dữ liệu địa điểm (hướng B)

App không gọi Overpass mỗi lần mở nữa (máy chủ Overpass hay quá tải / chặn từ Việt Nam).
Điểm du lịch toàn Việt Nam được tải **một lần** từ OpenStreetMap rồi đóng vào app:

| File | Là gì |
|---|---|
| `tools/osm_vn_query.overpassql` | Câu truy vấn Overpass lấy điểm du lịch + thành phố/thị xã toàn VN |
| `tools/osm_vn_raw.tsv` | Kết quả thô tải về (cột cách nhau bằng tab) |
| `tools/build_places_vn.py` | Xử lý: xếp 5 loại, nhãn ngắn, điểm "nổi bật", bỏ trùng |
| `app/src/main/assets/places_vn.tsv` | File app đọc (`data/place/PlaceDataset.kt`) |

Cột của `places_vn.tsv`: `id name category lat lng score tagline address imageUrl wikipedia isTown`
- `category`: bien, nui, van_hoa, thanh_pho, thien_nhien
- `score`: điểm "nổi bật" 0–5 (OSM không có số sao). Có bài Wikipedia, là di tích, có ảnh… thì cao hơn.
- `wikipedia`: "vi:Tên bài" hoặc mã Wikidata "Q123", app dùng để lấy ảnh khi có mạng.
- `isTown = 1`: thành phố / thị xã / thị trấn, chỉ dùng cho Tìm kiếm (gõ "da lat" ra Đà Lạt).

## Cập nhật lại dữ liệu

1. Chạy câu truy vấn trong `tools/osm_vn_query.overpassql` (dán vào https://overpass-turbo.eu rồi Export,
   hoặc gửi tới `https://overpass-api.de/api/interpreter`). Lưu kết quả thành `tools/osm_vn_raw.tsv`.
2. `python tools/build_places_vn.py tools/osm_vn_raw.tsv app/src/main/assets/places_vn.tsv`
3. Build lại app.

Dữ liệu © OpenStreetMap contributors (giấy phép ODbL). Ảnh và mô tả: Wikipedia / Wikimedia Commons.
