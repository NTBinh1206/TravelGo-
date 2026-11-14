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

## Quán ăn, quán nước (`food_vn.tsv`)

Mục "Ăn uống" đọc `app/src/main/assets/food_vn.tsv` (`data/place/FoodDataset.kt`), không phải chờ Overpass.
File chỉ phủ khoảng 8–13 km quanh trung tâm mỗi tỉnh (đúng chỗ app đặt vị trí khi người dùng chọn tỉnh).
Chưa có file này, hoặc quanh vị trí không có quán nào trong file, app mới hỏi Overpass trực tiếp.

| File | Là gì |
|---|---|
| `tools/food_vn_query.overpassql` | Câu truy vấn quán ăn, quán nước (`{{bbox}}` được thay bằng ô quanh từng tỉnh) |
| `tools/build_food_vn.py` | Tải quán quanh trung tâm 34 tỉnh thành + Hội An, Vũng Tàu, Phan Thiết; gộp, bỏ trùng, ghi `food_vn.tsv` |
| `app/src/main/assets/food_vn.tsv` | File app đọc |

Cột của `food_vn.tsv`: `id name amenity cuisine address lat lng`. Loại quán (quán ăn / quán nước) và
nhãn món ("Quán ăn · Phở") do app tự tính từ `amenity` + `cuisine` (`data/place/FoodPlaces.kt`).

Tạo / cập nhật (ở thư mục gốc project, cần Python 3.7+, không cần cài thêm gì):

```
python tools/build_food_vn.py
```

Máy chủ lỗi hay mạng rớt giữa chừng thì chạy lại lệnh trên: vùng nào đã tải xong (lưu trong `tools/food_raw_parts/`) được bỏ qua.
Muốn tải lại từ đầu thì xoá thư mục `tools/food_raw_parts/`.

## Báo quán đã đóng cửa (Cloud Firestore)

Dữ liệu OpenStreetMap có thể chưa cập nhật quán đã đóng. Trong app, chạm vào một quán → **Báo quán đã đóng cửa**.
Quán ẩn ngay với người báo, và ẩn với mọi người khi có 2 người báo (`ClosedReportRepository.HIDE_FOR_EVERYONE`).

- Dữ liệu nằm ở bộ sưu tập `closed_reports` trên Firestore (gói Spark miễn phí), mỗi quán một tài liệu,
  tên tài liệu là id quán (`n123`, `w456`…), có trường `reporters` là danh sách uid người đã báo.
- Luật bảo vệ: file `firestore.rules` ở thư mục gốc. Dán nội dung file vào Firebase Console → Firestore Database → Rules → Publish.
  Luật chỉ cho người đã đăng nhập đọc, mỗi người chỉ thêm được chính mình vào danh sách một lần, không ai xoá được từ app.
- Quán mở lại: xoá tài liệu của quán đó trên Firebase Console.
