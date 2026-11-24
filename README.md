# TravelGo - Ứng dụng gợi ý điểm du lịch và lộ trình thông minh

Đồ án môn Lập trình thiết bị di động (UTH). Ứng dụng Android lấy vị trí người dùng, gợi ý điểm du lịch và quán ăn uống xung quanh, rồi sắp xếp lộ trình tối ưu qua tối đa 5 điểm.

**Tải bản cài (APK):** [Releases](https://github.com/NTBinh1206/TravelGo-/releases/latest) → tải file `.apk` ở mục Assets → mở trên điện thoại Android 7.0 trở lên, cho phép "Cài ứng dụng không rõ nguồn gốc".

## Chức năng

- Đăng nhập bằng Google, Facebook hoặc Khách (ẩn danh); xem thông tin ở màn Cá nhân.
- Lấy vị trí GPS, đổi ra tên tỉnh; chọn tay 1 trong 34 tỉnh thành nếu từ chối quyền hoặc GPS tắt.
- Trang chủ, Khám phá, Gần bạn, Tìm kiếm: 2.937 điểm du lịch toàn Việt Nam (OpenStreetMap + Wikipedia), lọc theo loại, tìm không dấu.
- Ăn uống quanh bạn: 13.652 quán ăn, quán nước trong bán kính 2 km; chạm quán để mở Google Maps xem ảnh, đánh giá.
- Báo quán đã đóng cửa (Cloud Firestore): ẩn với người báo; đủ tài khoản Google/Facebook báo (từ 2, quán nhiều người xem cần tới 5) thì quán được gắn nhãn "Có thể đã đóng cửa"; nút "Quán vẫn mở cửa"; tuỳ chọn gửi ghi chú lên OpenStreetMap.
- Chống báo sai: phải đứng cách quán dưới 200 m (GPS thật, không nhận vị trí giả), tài khoản dùng app từ 1 ngày, tối đa 5 lần báo hoặc xác nhận trong 24 giờ. Tuổi tài khoản và số lượt do luật Firestore kiểm tra trên máy chủ.
- Lộ trình thông minh: tối đa 5 điểm, 3 chế độ (ngắn nhất, khứ hồi, ưu tiên điểm hay), quãng đường và thời gian theo đường bộ (OSRM), bản đồ osmdroid, mở Google Maps để dẫn đường.

## Công nghệ

Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM (ViewModel + StateFlow), Firebase Authentication, Cloud Firestore, Credential Manager, Facebook Login, Fused Location Provider, OkHttp, Coil, osmdroid, OSRM, OpenStreetMap, Wikipedia.

## Chạy từ mã nguồn

1. Mở thư mục này bằng Android Studio, chờ Gradle Sync xong.
2. Tải `google-services.json` từ Firebase Console (dự án của nhóm) và đặt vào `app/`. File này không được đẩy lên GitHub.
3. Muốn đăng nhập Google trên máy mình: thêm SHA-1 của khoá debug (`./gradlew signingReport`) vào Firebase Console.
4. Luật Firestore nằm ở `firestore.rules`: dán vào Firebase Console → Firestore Database → Rules → Publish.
5. Chọn máy ảo hoặc điện thoại, bấm Run. Unit test: chuột phải thư mục `app/src/test` → Run Tests.

## Cấu trúc thư mục

```
app/src/main/java/com/travelgo/app/
├── MainActivity.kt
├── navigation/        Routes, NavHost
├── ui/theme/          Màu, font theo Figma
├── components/        PlaceCard, BottomTabBar, nút dùng chung
├── di/AppContainer.kt Tạo Repository dùng chung cho ViewModel
├── data/auth/         AuthRepository (Google, Facebook, Khách)
├── data/location/     LocationProvider (GPS, tên tỉnh)
├── data/place/        Dữ liệu địa điểm, quán ăn, báo quán đóng cửa, ghi chú OSM
├── data/route/        OSRM (quãng đường đường bộ)
├── domain/route/      Thuật toán tìm lộ trình
└── ui/                auth, home, explore, food, profile, route (mỗi màn một ViewModel)
tools/                 Script tạo bộ dữ liệu từ OpenStreetMap
TravelGo/              Ảnh thiết kế Figma 8 màn hình
```

## Phân công

- Nguyễn Thế Bình: khung ứng dụng, đăng nhập, vị trí, chuyển sang MVVM, ăn uống, báo quán đóng cửa, OSRM.
- Nguyễn Thị Ngọc Tuyến: dữ liệu địa điểm, Trang chủ, Khám phá, Tìm kiếm, Gần bạn.
- Phùng Trung Tín: thuật toán và màn Lộ trình thông minh.

## Còn hạn chế

- Mới chạy thử trên máy ảo; chưa có màn chi tiết địa điểm, chưa lưu yêu thích.
- Facebook đang ở chế độ phát triển: chỉ tài khoản được thêm làm người thử nghiệm mới đăng nhập được.
- Một số mạng ở Việt Nam chặn openstreetmap.org, khi đó không gửi được ghi chú OSM (báo cáo trong Firestore vẫn được lưu).

## Quy ước

- Mỗi người làm trên nhánh riêng, gộp vào `main` qua Pull Request; `main` chỉ chứa bản ổn định.
- Không đẩy `google-services.json`, `local.properties`, khoá ký ứng dụng lên GitHub.
