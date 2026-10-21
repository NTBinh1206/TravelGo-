# TravelGo - Ứng dụng gợi ý điểm du lịch

Ứng dụng Android lấy vị trí người dùng, gợi ý điểm du lịch xung quanh và sắp xếp lộ trình tối ưu qua tối đa 5 điểm.

- Ngôn ngữ: Kotlin, Jetpack Compose, Material 3
- Điều hướng: Navigation Compose
- Backend (sẽ thêm): Firebase Auth, Firestore, Google Places API, Maps Compose

## Chạy thử

1. Mở thư mục này bằng Android Studio (File > Open).
2. Chờ Gradle Sync xong, chọn máy ảo hoặc điện thoại, bấm Run.
3. Bản khung dùng dữ liệu giả: bấm nút nào ở màn đăng nhập cũng vào được app.

## Cấu trúc thư mục

```
app/src/main/java/com/travelgo/app/
├── MainActivity.kt
├── navigation/        Routes, NavHost (Người 1)
├── ui/theme/          Màu, font, bo góc theo Figma (Người 1)
├── components/        PlaceCard, PrimaryButton, BottomTabBar, ScreenHeader (dùng chung)
├── di/AppContainer.kt Nơi tạo repository, location provider dùng chung
├── data/model/        GeoPoint
├── data/place/        Place, PlaceRepository (Người 2)
├── data/location/     LocationProvider (Người 1)
├── domain/route/      RouteMode, RouteResult, findBestRoute (Người 3)
└── ui/
    ├── auth/          01 Đăng nhập, 02 Xin quyền vị trí (Người 1)
    ├── profile/       05 Cá nhân (Người 1)
    ├── home/          03 Trang chủ (Người 2)
    ├── explore/       04 Khám phá, 06 Tìm kiếm, 07 Gần bạn (Người 2)
    └── route/         08 Lộ trình thông minh (Người 3)
```

Tìm `TODO(Người 1)`, `TODO(Người 2)`, `TODO(Người 3)` trong code để thấy việc của từng người.

## Quy ước chung

- Không tự ý đổi `Place`, `LocationProvider`, `RouteMode`, `RouteResult`, `findBestRoute`. Cần đổi thì báo nhóm trước.
- File dùng chung (`ui/theme`, `navigation`, `components`, `di`) báo nhóm trước khi sửa.
- Mỗi người làm trên nhánh riêng: `feature/auth`, `feature/explore`, `feature/route`, gộp vào `main` qua Pull Request.
- Không đẩy `google-services.json`, API key lên GitHub (để trong `local.properties`).
