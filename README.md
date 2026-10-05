# 💻 SAOClub - Hệ Thống Quản Lý Bán Máy Tính & Bán Lẻ Đa Kênh

Hệ thống quản lý bán lẻ đa kênh (Omnichannel Retail & E-commerce) chuyên biệt cho ngành hàng máy tính, laptop và thiết bị công nghệ. Dự án tích hợp đầy đủ quy trình bán hàng tại quầy (POS), đặt hàng trực tuyến, quản lý kho chi tiết theo **Số Serial/IMEI**, tra cứu bảo hành, đổi trả, vòng quay may mắn (gamification), và trợ lý trí tuệ nhân tạo (AI Chatbot) vận hành bởi **Ollama (Llama 3.2)**.

---

## 📑 Mục lục

- [1. Tính năng nổi bật](#1-tính-năng-nổi-bật)
- [2. Công nghệ sử dụng (Tech Stack)](#2-công-nghệ-sử-dụng-tech-stack)
- [3. Yêu cầu môi trường (Prerequisites)](#3-yêu-cầu-môi-trường-prerequisites)
- [4. Hướng dẫn cài đặt & Khởi chạy](#4-hướng-dẫn-cài-đặt--khởi-chạy)
  - [Cách 1: Khởi chạy bằng Docker Compose (Khuyến nghị - Nhanh nhất)](#cách-1-khởi-chạy-bằng-docker-compose-khuyến-nghị---nhanh-nhất)
  - [Cách 2: Khởi chạy thủ công trên máy (Local Development)](#cách-2-khởi-chạy-thủ-công-trên-máy-local-development)
  - [Cấu hình Firebase Authentication (firebase-service-account.json)](#cấu-hình-firebase-authentication-firebase-service-accountjson)
- [5. Tài khoản đăng nhập mặc định](#5-tài-khoản-đăng-nhập-mặc-định)
- [6. Cấu trúc thư mục dự án](#6-cấu-trúc-thư-mục-dự-án)
- [7. Kiểm thử & Đóng gói](#7-kiểm-thử--đóng-gói)
- [8. Xử lý sự cố thường gặp (Troubleshooting)](#8-xử-lý-sự-cố-thường-gặp-troubleshooting)

---

## 1. Tính năng nổi bật

### 🛍️ Dành cho Khách hàng (E-Commerce)
- **Danh mục & Chi tiết sản phẩm**: Tìm kiếm, lọc theo CPU, RAM, GPU, Ổ cứng, Thương hiệu; xem thông số kỹ thuật chi tiết; so sánh giữa các dòng máy.
- **Giỏ hàng & Đặt hàng**: Hỗ trợ khách đăng nhập và **khách vãng lai** (không cần tài khoản); chọn địa chỉ giao hàng trực quan qua bản đồ Leaflet; áp dụng mã giảm giá và voucher cá nhân.
- **Thanh toán & Theo dõi**: Hỗ trợ tiền mặt (COD), Chuyển khoản ngân hàng qua VietQR (tự sinh mã QR động), Visa/Mastercard. Timeline theo dõi đơn hàng trực quan 7 trạng thái.
- **Trợ lý AI tư vấn**: AI Chatbot tích hợp Ollama hiểu dữ liệu sản phẩm của cửa hàng, tự động nhận diện từ khóa cần hỗ trợ để chuyển sang nhân viên tư vấn thật qua WebSocket.
- **Bảo hành & Hậu mãi**: Tra cứu hạn bảo hành chính xác theo số Serial/IMEI; gửi yêu cầu bảo hành hoặc yêu cầu đổi trả online.
- **Vòng quay may mắn & Tích điểm**: Tích lũy điểm khi mua hàng, quay thưởng trúng voucher giảm giá với cơ chế chống gian lận điểm (DB-level Lock).

### ⚙️ Dành cho Quản trị & Nhân viên (Admin & POS)
- **Bán hàng tại quầy (POS)**: Lên đơn nhanh, chọn đích danh từng số Serial của máy tính bán ra, in hóa đơn & tem mã vạch.
- **Khóa giữ Serial (Serial Locking)**: Cơ chế khóa bi quan (`PESSIMISTIC_WRITE`) có thời hạn 5 phút khi nhân viên giữ máy tại quầy, phát SSE thời gian thực thông báo cho toàn bộ nhân viên khác để chống bán trùng.
- **Quản lý kho hàng**: Tạo phiếu nhập kho, sinh số serial tự động theo quy tắc, quản lý trạng thái máy (`trong_kho`, `da_ban`, `giu_hang`, `loi`, `dang_bao_hanh`).
- **Quản lý đổi trả & Bảo hành**: Tiếp nhận máy lỗi, lập phiếu bảo hành, theo dõi tiến độ xử lý và lịch sử trả hàng.
- **Báo cáo & Thống kê**: Doanh thu theo ngày/tháng/năm, top sản phẩm bán chạy, cảnh báo tồn kho sắp hết, bản đồ nhiệt (Calendar Heatmap).
- **Admin AI Analytics**: Trợ lý AI dành riêng cho Admin phân tích dữ liệu kinh doanh bằng ngôn ngữ tự nhiên.
- **Cầu nối quét mã vạch qua điện thoại (`usb-camera-proxy`)**: Biến camera điện thoại Android thành máy quét barcode chuyên dụng qua cáp USB (ADB) bắn thẳng serial lên giao diện web.

---

## 2. Công nghệ sử dụng (Tech Stack)

| Tầng | Công nghệ |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.4.4, Spring Data JPA (Hibernate 6), Spring Security 6, JJWT 0.12.6, Spring WebSocket (STOMP), Server-Sent Events (SSE), WebFlux `WebClient`, HikariCP |
| **Frontend** | Vue 3 (Composition API `<script setup>`), Vite 8, Pinia 2.3, Vue Router 4, Bootstrap 5.3, Lucide Icons, FontAwesome 7, Leaflet, SheetJS (`xlsx`), `@zxing/library` |
| **Database** | Microsoft SQL Server 2022 (hỗ trợ Views, Triggers, Check Constraints) |
| **Trí tuệ nhân tạo** | Ollama Local LLM (`llama3.2`) |
| **Hardware Proxy** | Node.js, WebSocket (`ws`), ADB Bridge (`adb reverse`) |
| **DevOps & CI/CD** | Docker, Docker Compose, GitHub Actions (`ci.yml`, `deploy.yml`) |

---

## 3. Yêu cầu môi trường (Prerequisites)

Để chạy dự án, máy tính cần cài đặt:

1. **Git**: Phiên bản 2.30+
2. **Java Development Kit (JDK)**: **JDK 17** (hoặc JDK 21)
3. **Node.js**: Phiên bản **20.19.0+** hoặc **>= 22.12.0** (kèm `npm`)
4. **Apache Maven**: Phiên bản 3.9+ (hoặc dùng `mvn` có sẵn trong IDE)
5. **Cơ sở dữ liệu**: **Microsoft SQL Server 2022** (bản Developer/Express hoặc chạy qua Docker)
6. *(Tùy chọn cho Docker)*: **Docker Desktop** (bật WSL 2)
7. *(Tùy chọn cho AI)*: **Ollama** cài trên máy hoặc chạy trong Docker

---

## 4. Hướng dẫn cài đặt & Khởi chạy

### Cách 1: Khởi chạy bằng Docker Compose (Khuyến nghị - Nhanh nhất)

Cách này sẽ tự động khởi động **SQL Server 2022**, **Ollama AI**, **Backend Spring Boot** và **Frontend Vue**:

1. **Clone dự án về máy**:
   ```bash
   git clone --depth 1 https://github.com/LeeSyaoran/SAOClub.git
   cd SAOClub
   ```
   *(Khuyến nghị dùng cờ `--depth 1` để tải nhanh và tránh đứt kết nối mạng giữa chừng do dung lượng repo lớn)*.

2. **Thiết lập file cấu hình môi trường**:
   Tạo file `.env` từ file mẫu `.env.example`:
   ```bash
   cp .env.example .env
   ```
   *(Trên Windows PowerShell: `Copy-Item .env.example .env`)*

   Mở file `.env` và tùy chỉnh mật khẩu nếu cần:
   ```properties
   DB_HOST=sqlserver
   DB_PORT=1433
   DB_NAME=QLBanMayTinh
   DB_USER=sa
   DB_PASSWORD=SaoClub@2024
   JWT_SECRET=4f8a1c9d3e7b2f6a0d5c8e1b4a7f2d9c6e3b0a8f5d2c7e4b1a9f6d3c0e7b4a1f
   JWT_EXPIRATION=86400000
   ```

3. **Khởi động toàn bộ hệ thống**:
   ```bash
   docker compose up -d --build
   ```
   *Hoặc chạy script PowerShell tự động:*
   ```powershell
   .\docker-clean-restart.ps1
   ```

4. **Truy cập ứng dụng**:
   - **Frontend (Giao diện Web)**: [http://localhost:5173](http://localhost:5173)
   - **Backend API**: [http://localhost:8080](http://localhost:8080)
   - **Ollama AI**: [http://localhost:11434](http://localhost:11434)

---

### Cách 2: Khởi chạy thủ công trên máy (Local Development)

#### Bước 1: Khởi tạo Cơ sở dữ liệu (SQL Server)
1. Mở **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**.
2. Kết nối tới SQL Server cục bộ của bạn (`localhost,1433`).
3. Mở và thực thi toàn bộ file script tại:
   ```
   Database/QLBanMayTinh.sql
   ```
   *(Script sẽ tự động tạo database `QLBanMayTinh`, tạo các bảng, quan hệ, triggers, views và nạp sẵn dữ liệu mẫu đầy đủ).*

#### Bước 2: Cấu hình Backend
1. Tạo file `.env` tại thư mục gốc `SAOClub/.env` (hoặc kiểm tra file `BackEnd/src/main/resources/application.properties`).
2. Đảm bảo thông tin kết nối đúng với SQL Server của bạn:
   ```properties
   DB_HOST=localhost
   DB_PORT=1433
   DB_NAME=QLBanMayTinh
   DB_USER=sa
   DB_PASSWORD=Mật_khẩu_SQL_Server_của_bạn
   JWT_SECRET=4f8a1c9d3e7b2f6a0d5c8e1b4a7f2d9c6e3b0a8f5d2c7e4b1a9f6d3c0e7b4a1f
   ```

#### Bước 3: Khởi chạy Backend (Spring Boot)
Mở một cửa sổ Terminal mới:
```bash
cd BackEnd
mvn clean spring-boot:run
```
Khi thấy dòng log `Started BackEndApplication in ... seconds` trên cổng `8080` là backend đã sẵn sàng.

#### Bước 4: Khởi chạy Frontend (Vue 3)
Mở một cửa sổ Terminal khác:
```bash
cd FrontEnd/QLBanMayTinh
npm install
npm run dev
```
Trình duyệt sẽ tự động chạy tại: **[http://localhost:5173](http://localhost:5173)**.

#### Bước 5: (Tùy chọn) Chạy mô hình AI Ollama
Nếu bạn muốn sử dụng tính năng AI Chatbot:
1. Tải và cài đặt [Ollama](https://ollama.ai/).
2. Chạy model Llama 3.2:
   ```bash
   ollama run llama3.2
   ```
Backend sẽ tự động kết nối tới Ollama tại cổng mặc định `11434`.

#### Bước 6: (Tùy chọn) Quét mã vạch bằng điện thoại (USB Barcode Scanner)
1. Cài đặt **Android Platform Tools (ADB)** vào máy tính và thêm vào `PATH`.
2. Bật chế độ **Gỡ lỗi USB (USB Debugging)** trên điện thoại Android và cắm cáp USB vào máy tính.
3. Khởi chạy proxy:
   ```bash
   cd FrontEnd/usb-camera-proxy
   npm install
   node proxy-adb.js
   ```
4. Mở trình duyệt Chrome trên điện thoại truy cập: `http://localhost:8484/phone` để bắt đầu quét mã vạch trực tiếp vào web.

---

### Cấu hình Firebase Authentication (firebase-service-account.json)

Tính năng **Đăng nhập nhanh bằng Google / Facebook** sử dụng **Firebase Admin SDK** ở Backend (`FirebaseConfig.java`, `AuthService.java`) để xác thực an toàn `idToken` gửi từ Frontend. 

Để Backend khởi động và xác thực Firebase thành công, bạn cần cung cấp file chứng chỉ quản trị **`firebase-service-account.json`**.

#### Trường hợp A: Sử dụng Project Firebase có sẵn (`saoclub-b9b96`)
Nếu tài khoản Google của bạn đang có quyền truy cập vào Firebase project của dự án:
1. Truy cập [Firebase Console](https://console.firebase.google.com/) và đăng nhập tài khoản Google quản trị.
2. Chọn project: **`saoclub-b9b96`**.
3. Nhấp vào biểu tượng bánh răng **⚙️ (Project settings - Cài đặt dự án)** ở góc trên thanh menu bên trái.
4. Chọn tab **Service accounts (Tài khoản dịch vụ)**.
5. Tại mục **Firebase Admin SDK**, nhấn nút **Generate new private key (Tạo khóa riêng mới)** -> Xác nhận **Generate key**.
6. Đổi tên file `.json` vừa tải về thành:
   ```text
   firebase-service-account.json
   ```
7. Đặt file vào đường dẫn:
   ```text
   BackEnd/src/main/resources/firebase-service-account.json
   ```
   *(Hoặc lưu file ở bất kỳ đâu trên máy tính và khai báo đường dẫn tuyệt đối trong file `.env`: `FIREBASE_SERVICE_ACCOUNT=file:C:/secrets/firebase-service-account.json`)*.

#### Trường hợp B: Tạo mới một Project Firebase độc lập (Tự triển khai riêng)
Nếu bạn muốn sử dụng tài khoản Firebase riêng của mình:
1. **Tạo Project mới:**
   - Truy cập [Firebase Console](https://console.firebase.google.com/) -> Bấm **Add project (Thêm dự án)**.
   - Đặt tên dự án (ví dụ: `saoclub-shop`) và hoàn thành các bước tạo.
2. **Bật Authentication:**
   - Vào menu bên trái: **Build** -> **Authentication** -> Bấm **Get started**.
   - Tại tab **Sign-in method**, bật nhà cung cấp **Google** (chọn email hỗ trợ) -> Bấm **Save**.
   - Tại tab **Settings** -> **Authorized domains**, đảm bảo đã có domain `localhost` và `127.0.0.1`.
3. **Lấy Private Key cho Backend:**
   - Vào **Project settings (⚙️)** -> tab **Service accounts** -> Bấm **Generate new private key**.
   - Đổi tên file tải về thành `firebase-service-account.json` và lưu vào thư mục `BackEnd/src/main/resources/`.
4. **Cập nhật cấu hình sang Frontend:**
   - Trong **Project settings (⚙️)** -> tab **General** -> Kéo xuống phần **Your apps** -> Nhấp vào biểu tượng Web `</>` để tạo Web App.
   - Sao chép các thông số trong đoạn mã `firebaseConfig` nhận được:
     ```javascript
     const firebaseConfig = {
       apiKey: "...",
       authDomain: "...",
       projectId: "...",
       storageBucket: "...",
       messagingSenderId: "...",
       appId: "..."
     };
     ```
   - Mở file `FrontEnd/QLBanMayTinh/src/firebase.js` và thay thế cụm `firebaseConfig` tương ứng với thông tin project mới của bạn.

> [!WARNING]
> **Quy định An toàn Bảo mật:** File `firebase-service-account.json` chứa Private Key mang quyền Admin tối cao của project Firebase. File này đã được cấu hình trong `.gitignore` để ngăn chặn việc rò rỉ lên GitHub. Tuyệt đối không xóa dòng `firebase-service-account.json` trong `.gitignore` hoặc chia sẻ công khai file này.

---

## 5. Tài khoản đăng nhập mặc định

Hệ thống đã nạp sẵn các tài khoản demo sau trong cơ sở dữ liệu:

| Vai trò (Role) | Username | Mật khẩu mặc định | Quyền hạn chính |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `123456` | Toàn quyền hệ thống: Quản lý nhân viên, doanh thu, cài đặt, POS, bảo hành, AI Analytics |
| **Nhân viên bán hàng** | `nhanvienan` | `123456` | Bán hàng POS tại quầy, quản lý đơn hàng, xác nhận thanh toán, chat hỗ trợ khách hàng |
| **Nhân viên hỗ trợ** | `nhanvienbao` | `123456` | Quản lý đơn hàng, phiếu bảo hành, tiếp nhận đổi trả |
| **Quản lý kho** | `nhanviencuong` | `123456` | Quản lý kho, nhập kho linh kiện/sản phẩm, theo dõi số Serial/IMEI, xuất kho |
| **Khách hàng thân thiết** | `khachhang` | `123456` | Đặt hàng, tra cứu lịch sử mua, vòng quay may mắn, xem voucher cá nhân |

*(Ngoài ra bạn có thể bấm **Đăng ký** tài khoản mới hoặc đăng nhập nhanh bằng **Google OAuth/Firebase** trên giao diện).*

---

## 6. Cấu trúc thư mục dự án

```
SAOClub/
├── .github/
│   └── workflows/              # GitHub Actions CI/CD (ci.yml, deploy.yml)
├── BackEnd/                    # Backend Spring Boot 3.4
│   ├── src/main/java/com/example/backend/
│   │   ├── config/             # Cấu hình WebSocket, CORS, DatabaseInitializer, Firebase
│   │   ├── controller/         # REST Controllers (Sản phẩm, Đơn hàng, POS, AI, Bảo hành...)
│   │   ├── entity/             # JPA Entities ánh xạ database
│   │   ├── repository/         # Spring Data JPA Repositories (có Pessimistic Lock)
│   │   ├── security/           # JWT Filter, OrderAccessGuard, RateLimiting
│   │   └── service/            # Nghiệp vụ lõi (DonHang, Kho, Serial, Chat, AI...)
│   ├── src/main/resources/     # application.properties & profiles
│   └── pom.xml                 # Maven dependencies
├── Database/
│   ├── QLBanMayTinh.sql        # Script tạo toàn bộ database & dữ liệu mẫu
│   ├── ERD_SAOCLUB.drawio      # Sơ đồ quan hệ thực thể (ERD)
│   ├── backup-db.ps1           # Script PowerShell tự động backup cơ sở dữ liệu
│   └── backups/                # Thư mục lưu các bản sao lưu .bak
├── FrontEnd/
│   ├── QLBanMayTinh/           # Ứng dụng chính Vue 3 + Vite
│   │   ├── src/
│   │   │   ├── components/     # Components chia theo module (admin, checkout, pos...)
│   │   │   ├── pages/          # Các trang (CustomerPage, AdminPage, AccountPage...)
│   │   │   ├── services/       # Lớp gọi API backend
│   │   │   ├── stores/         # State management với Pinia
│   │   │   └── i18n/           # Đa ngôn ngữ (Tiếng Việt, Tiếng Anh)
│   │   └── package.json
│   └── usb-camera-proxy/       # Node.js proxy kết nối camera điện thoại quét barcode
├── docker-compose.yml          # Đóng gói SQL Server, Ollama, Backend & Frontend
├── docker-clean-restart.ps1    # Script khởi động lại môi trường Docker sạch
├── .env.example                # File mẫu biến môi trường
└── README.md                   # Tài liệu hướng dẫn dự án
```

---

## 7. Kiểm thử & Đóng gói

### Kiểm thử Backend (Unit & Security Tests)
```bash
cd BackEnd
mvn clean test -Dtest="!BackEndApplicationTests"
```
*(Bao gồm hơn 130 bài kiểm thử Mockito kiểm tra logic đơn hàng, kho serial, phân quyền `@OrderAccessGuard`, và chống brute-force `RateLimitingFilter`).*

### Kiểm thử Frontend & Lint
```bash
cd FrontEnd/QLBanMayTinh
npm run test        # Chạy Vitest
npm run lint        # Kiểm tra chuẩn mã nguồn ESLint
```

### Đóng gói Production
- **Backend (File JAR)**:
  ```bash
  cd BackEnd
  mvn clean package -DskipTests
  # File JAR tạo ra tại: BackEnd/target/BackEnd-0.0.1-SNAPSHOT.jar
  ```
- **Frontend (Tệp tĩnh)**:
  ```bash
  cd FrontEnd/QLBanMayTinh
  npm run build
  # Tệp tĩnh tạo ra tại thư mục dist/
  ```

---

## 8. Xử lý sự cố thường gặp (Troubleshooting)

1. **Lỗi `Cannot open database "QLBanMayTinh"` khi chạy Backend**:
   - Kiểm tra xem bạn đã chạy file `Database/QLBanMayTinh.sql` trong SSMS chưa.
   - Kiểm tra mật khẩu `DB_PASSWORD` trong file `.env` xem có khớp với tài khoản `sa` của SQL Server không.

2. **Lỗi `The certificate of the secure sockets layer (SSL)... is not trusted`**:
   - File cấu hình đã bao gồm tham số `encrypt=true;trustServerCertificate=true;`. Đảm bảo connection string trong `application.properties` không bị sửa xóa tham số này.

3. **Lỗi cổng bị trùng (Port 8080 hoặc 5173 đã được sử dụng)**:
   - Đổi port Backend trong `BackEnd/src/main/resources/application.properties` bằng cách thêm: `server.port=8081`.
   - Đổi port Frontend trong `FrontEnd/QLBanMayTinh/vite.config.js` hoặc truyền cờ: `npm run dev -- --port 3000`.

4. **AI Chatbot báo lỗi hoặc không trả lời**:
   - Kiểm tra xem Ollama đã chạy chưa (`ollama list`).
   - Đảm bảo model `llama3.2` đã được tải: `ollama pull llama3.2`.
   - Nếu không có GPU, model vẫn chạy tốt trên CPU (chỉ phản hồi chậm hơn một chút).

5. **Lỗi `Failed to initialize Firebase Admin SDK` khi khởi động Backend**:
   - Kiểm tra xem file `firebase-service-account.json` đã có trong thư mục `BackEnd/src/main/resources/` chưa.
   - Nếu bạn lưu file ở ngoài source code, kiểm tra lại giá trị biến `FIREBASE_SERVICE_ACCOUNT` trong `.env` xem đường dẫn tệp có chính xác không (ví dụ: `FIREBASE_SERVICE_ACCOUNT=file:C:/secrets/firebase-service-account.json`).

6. **Lỗi `RPC failed; curl 18 transfer closed... fatal: early EOF` khi pull/clone dự án**:
   - **Nguyên nhân**: Repository chứa tài nguyên và lịch sử commit lớn (trên 12.800 objects), khi đường truyền mạng chập chờn hoặc bộ đệm Git mặc định nhỏ sẽ dẫn tới lỗi ngắt kết nối (`early EOF`).
   - **Cách 1 (Khuyên dùng - Nhanh nhất 100% thành công)**: Clone nông (`--depth 1`) chỉ lấy commit mới nhất:
     ```bash
     git clone --depth 1 https://github.com/LeeSyaoran/SAOClub.git
     ```
     *(Nếu sau này cần toàn bộ lịch sử commit, chạy: `cd SAOClub && git fetch --unshallow`)*.
   - **Cách 2 (Cấu hình tăng buffer cho Git)**: Chạy các lệnh sau rồi clone lại:
     ```bash
     git config --global http.postBuffer 524288000
     git config --global http.maxRequestBuffer 100M
     git config --global core.compression 0
     git config --global http.lowSpeedLimit 0
     git config --global http.lowSpeedTime 999999
     git clone https://github.com/LeeSyaoran/SAOClub.git
     ```
   - **Cách 3 (Tải trực tiếp mã nguồn)**: Truy cập repository trên GitHub: [https://github.com/LeeSyaoran/SAOClub](https://github.com/LeeSyaoran/SAOClub) -> Bấm nút **<> Code** -> Chọn **Download ZIP** rồi giải nén.

---

⭐ **Chúc bạn trải nghiệm và vận hành hệ thống SAOClub thành công!** Nếu có bất kỳ câu hỏi nào, vui lòng mở Issue hoặc liên hệ nhóm phát triển.
