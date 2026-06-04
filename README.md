# tGhauctionRM - Online Auction System

## 1. Giới thiệu dự án

`tGhauctionRM` là hệ thống đấu giá trực tuyến được xây dựng cho bài tập lớn môn **Lập trình nâng cao**.

Hệ thống cho phép nhiều người dùng tham gia đấu giá sản phẩm trong thời gian thực. Người bán có thể đăng và quản lý sản phẩm đấu giá, người mua có thể theo dõi phiên đấu giá và đặt giá, còn quản trị viên có thể quản lý dữ liệu hệ thống.

Dự án tập trung vào các nội dung chính:

* Lập trình hướng đối tượng trong Java.
* Kiến trúc Client–Server.
* Giao diện người dùng bằng JavaFX.
* Giao tiếp mạng bằng Socket.
* Xử lý nhiều client đồng thời.
* Cập nhật giá đấu theo thời gian thực.
* Lưu trữ dữ liệu bằng file/serialization.
* Kiểm thử logic quan trọng bằng JUnit.

## 2. Phạm vi hệ thống

Hệ thống mô phỏng một nền tảng đấu giá trực tuyến cơ bản, gồm các nhóm chức năng:

* Quản lý người dùng:
    * Đăng ký tài khoản.
    * Đăng nhập.
    * Phân quyền người dùng: Bidder, Seller, Admin.

* Quản lý sản phẩm đấu giá:
    * Thêm sản phẩm.
    * Sửa thông tin sản phẩm.
    * Xóa sản phẩm.
    * Xem danh sách sản phẩm/phiên đấu giá.

* Tham gia đấu giá:
    * Người dùng đặt giá cao hơn giá hiện tại.
    * Kiểm tra tính hợp lệ của giá đấu.
    * Cập nhật người đang dẫn đầu.
    * Cập nhật giá mới cho các client đang theo dõi phiên đấu giá.

* Kết thúc phiên đấu giá:
    * Tự động đóng phiên khi hết thời gian.
    * Xác định người thắng cuộc.
    * Chuyển trạng thái phiên đấu giá.

* Xử lý lỗi:
    * Đặt giá thấp hơn hoặc bằng giá hiện tại.
    * Đấu giá khi phiên đã đóng.
    * Lỗi đăng nhập/đăng ký.
    * Lỗi kết nối giữa client và server.
    * Lỗi đọc/ghi dữ liệu.

## 3. Công nghệ sử dụng

* Ngôn ngữ lập trình: Java 21
* GUI: JavaFX
* Giao tiếp mạng: Java Socket
* Kiến trúc: Client–Server
* Mô hình thiết kế giao diện: MVC
* Build tool: Maven
* Kiểm thử: JUnit 5
* CI/CD: GitHub Actions, Qodana
* Lưu trữ dữ liệu: BSON/JSON-based Serialization
* Quản lý mã nguồn: Git, GitHub

## 4. Yêu cầu môi trường

Cần cài đặt:

* JDK 21 hoặc cao hơn
* Maven 3.8 hoặc cao hơn
* Git
* JavaFX SDK (nếu chạy trực tiếp bằng IDE không dùng Maven)
* IDE khuyến nghị:
    * IntelliJ IDEA
    * Eclipse

Kiểm tra phiên bản Java và Maven:

```bash
java -version
mvn -version
```

## 5. Cấu trúc thư mục

Cấu trúc dự án đa mô-đun (multi-module) tổ chức như sau:

```text
tGhauctionRM/
├── pom.xml
├── README.md
├── client/
│   ├── pom.xml
│   └── src/main/java/com/app/client/
│       ├── controller/
│       ├── model/
│       ├── network/
│       └── ClientMain.java
├── server/
│   ├── pom.xml
│   └── src/main/java/com/app/server/
│       ├── dao/
│       ├── network/
│       ├── service/
│       └── ServerMain.java
├── shared/
│   ├── pom.xml
│   └── src/main/java/com/app/shared/
│       ├── exception/
│       ├── model/
│       └── network/
└── .github/workflows/
    ├── maven.yml
    └── qodana_code_quality.yml
```

Trong đó:
* `client/`: module chứa giao diện JavaFX và xử lý kết nối phía client.
* `server/`: module chứa code server, xử lý request từ nhiều client, thao tác file hệ thống Database.
* `shared/`: module chứa các model dùng chung, request/response payload, custom format exception.

## 6. Hướng dẫn cài đặt

Clone repository:

```bash
git clone https://github.com/4symptote/tGhauctionRM.git
cd tGhauctionRM
```

Cài đặt dependencies và build toàn bộ project (đã bao gồm chạy test):

```bash
mvn clean install
```

Chạy test riêng biệt bằng lệnh:

```bash
mvn test
```

# ▶️ Khởi Chạy Hệ Thống

## Bước 1: Khởi động Server

### Windows

```bash
java -jar executables/server-1.0-SNAPSHOT.jar
```

---

## Bước 2: Khởi động Client

### Windows

```bash
java -jar executables/client-1.0-SNAPSHOT.jar
```

Có thể chạy nhiều Client đồng thời để mô phỏng nhiều người dùng.

---


# 🔒 Đảm Bảo Tính Toàn Vẹn Dữ Liệu

Động cơ đấu giá được thiết kế để xử lý nhiều yêu cầu đặt giá cùng lúc.

Các cơ chế được sử dụng:

* Thread-safe Locking
* Đồng bộ hóa giao dịch
* Kiểm soát Escrow
* Ngăn chặn race condition
* Bảo toàn trạng thái phiên đấu giá

Nhờ đó hệ thống đảm bảo:

✅ Không mất lượt đấu giá

✅ Không xảy ra double-spending

✅ Dữ liệu nhất quán

✅ Kết quả đấu giá chính xác
## 9. Các chức năng đã hoàn thành

### 9.1. Chức năng bắt buộc

* [x] Đăng ký tài khoản.
* [x] Đăng nhập tài khoản.
* [x] Phân quyền người dùng: Bidder, Seller, Admin.
* [x] Thêm sản phẩm đấu giá.
* [x] Sửa sản phẩm đấu giá.
* [x] Xóa sản phẩm đấu giá.
* [x] Hiển thị danh sách phiên đấu giá.
* [x] Hiển thị chi tiết sản phẩm.
* [x] Đặt giá đấu.
* [x] Kiểm tra giá đấu hợp lệ.
* [x] Cập nhật người dẫn đầu phiên đấu giá.
* [x] Tự động kết thúc phiên đấu giá khi hết thời gian.
* [x] Xác định người thắng cuộc.
* [x] Xử lý lỗi đặt giá thấp hơn giá hiện tại.
* [x] Xử lý lỗi đấu giá khi phiên đã đóng.
* [x] Xử lý lỗi kết nối client-server.
* [x] Giao diện JavaFX.
* [x] Kiến trúc Client–Server.
* [x] MVC phía client (View FXML - Controller).
* [x] Cấu trúc DAO/Service/Handler phía server.
* [x] Xử lý nhiều client đồng thời.
* [x] Realtime update khi có bid mới.
* [x] Unit test cho logic quan trọng.
* [x] GitHub Actions chạy test tự động.

### 9.2. Chức năng nâng cao

* [x] Theo dõi phiên bằng Socket (Observer).
* [x] Auto-Bidding (Hệ thống tự động thay người dùng nâng giá).
* [x] Anti-sniping: tự động gia hạn phiên nếu có bid ở những giây cuối.
* [x] Giao dịch nạp tiền, thu hồi tiền.

## 10. Design Pattern áp dụng

* **Singleton:** Dùng cho AuctionManager, Connection/Data Manager để đảm bảo toàn hệ thống chỉ có một instance lưu trữ trạng thái phiên ở server.
* **Factory Method:** Khởi tạo các phân lớp Item (`ArtCreator`, `ElectronicsCreator`, `VehicleCreator`), giúp tách biệt logic sinh object.
* **Observer:** Cơ chế lắng nghe từ phía Client đối với Event/Response trả về qua Socket để cập nhật AutoBid hoặc thay đổi giá ngay lập tức.

## 11. Xử lý đồng thời

Server hỗ trợ nhiều client kết nối cùng lúc bằng Multithreading (`ClientHandler`).
Kỹ thuật an toàn cho giao dịch đấu giá `Concurrent bidding`:
* Sử dụng bộ nhớ lock `ReentrantLock` theo mỗi ID sản phẩm/phiên.
* Tránh Lost update & Race condition khi nhiều người cùng đặt giá, đảm bảo dòng chảy logic luôn nhất quán theo thứ tự vào hàng đợi.

## 12. Kiểm thử

Chạy toàn bộ test suite bằng Maven:

```bash
mvn test
```

## 13. CI/CD

Đã cấu hình CI/CD Pipelines bằng GitHub Actions:
* **Java CI with Maven**: Tự động build source, setup JDK 21, xác nhận bộ unit tests pass trước khi cho phép pull-request/merge code (`.github/workflows/maven.yml`).
* **Qodana Code Quality**: Quét tự động dự án Java bằng công cụ phân tích tĩnh của JetBrains nhằm phát hiện code smells (`.github/workflows/qodana_code_quality.yml`).

## 14. File báo cáo và video demo app
https://drive.google.com/file/d/1uVhwe_RH1aAhKyJubLeIgMlwhcqw7hiA/view
https://docs.google.com/document/d/1DfNwAb0x_qbnwHuo-92Ob3U9lHC0UJhh/edit