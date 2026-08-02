# SAOClub hoạt động như thế nào

Tài liệu này giải thích kiến trúc và luồng dữ liệu thật của dự án, viết ra từ việc đọc trực tiếp mã nguồn (không phải mô tả lý thuyết) — kèm đường dẫn file cụ thể để bạn tự tra cứu tiếp khi cần. Mục tiêu: đọc xong hiểu được khi bấm 1 nút trên giao diện thì dữ liệu chạy qua đâu, chạm vào bảng nào.

## 1. Bức tranh tổng thể

```
Trình duyệt (Vue 3 SPA)  <-- REST API (JSON) -->  Spring Boot  <-- JDBC -->  SQL Server
     :5173                                            :8080
```

Ba thành phần này chạy độc lập, đóng gói bằng `docker-compose.yml` thành 3 container (`sqlserver`, `backend`, `frontend`). Frontend là **một trang duy nhất** (SPA) — khi bạn "chuyển trang" trong ứng dụng, trình duyệt không tải lại HTML mới, Vue Router chỉ đổi component nào đang hiển thị (dùng hash routing: `/#/admin`, `/#/account`...). Mọi dữ liệu hiển thị đều lấy về bằng cách gọi REST API tới backend.

Backend theo đúng 4 tầng đã mô tả trong báo cáo, và đây là dữ liệu thật đi qua từng tầng khi có 1 request tới:

```
HTTP request
  → Controller   (nhận request, không có logic nghiệp vụ)
  → Service      (toàn bộ logic: tính tiền, kiểm tra tồn kho, transaction...)
  → Repository   (Spring Data JPA — sinh câu SQL tự động hoặc câu tùy chỉnh)
  → Entity       (lớp Java ánh xạ 1-1 với 1 bảng trong SQL Server)
```

Ví dụ thật: `controller/DonHangController.java` → `service/DonHangService.java` → `repository/DonHangRepository.java` → `entity/DonHang.java` → bảng `don_hang`. Quy ước đặt tên giữ nguyên xuyên suốt cả 4 tầng + cả tên bảng, nên cứ biết tên nghiệp vụ (ví dụ "phiếu bảo hành") là lần ra được cả 4 file liên quan (`PhieuBaoHanh*`).

★ **Điểm hay để ý:** Controller ở đây **không có logic** — chỉ nhận request, gọi đúng 1 hàm Service, trả JSON. Toàn bộ quyết định (được phép hay không, tính tiền thế nào, chuyển trạng thái được không) nằm ở Service. Muốn hiểu 1 tính năng làm gì, đọc thẳng file `*Service.java`, không cần đọc Controller trước.

## 2. Chạy dự án ở máy local

```bash
docker compose up -d
```

Lệnh trên bật cả 3 container. SQL Server dùng lại volume Docker (`sqlserver-data`) nên dữ liệu mẫu không mất giữa các lần chạy. Truy cập:

- Trang khách hàng: <http://localhost:5173>
- Swagger/OpenAPI: hiện đang lỗi do springdoc-openapi 2.6.0 chưa tương thích Spring Boot 4.0.6 (`NoSuchMethodError: ControllerAdviceBean.<init>`) — không ảnh hưởng các API thật, chỉ trang tài liệu tự sinh bị hỏng.

Tài khoản mẫu có sẵn (mật khẩu `123456` cho tất cả):

| Vai trò | Username | Route riêng sau khi đăng nhập |
|---|---|---|
| Khách hàng | `khachhang` | `/#/account` |
| Nhân viên bán hàng | `nhanvienan` | `/#/staff` |
| Quản kho | `nhanviencuong` | `/#/kho` |
| Quản lý | `admin` | `/#/admin` |

## 3. Đăng nhập, JWT và phân quyền — 2 lớp độc lập

**Bước 1 — Đăng nhập:** `POST /api/auth/login` (`AuthController.java`) kiểm tra mật khẩu (đã băm BCrypt trong cột `mat_khau_hash`), nếu đúng thì `JwtUtil` ký ra một token JWT chứa `username` + `role`, trả về cho trình duyệt. Frontend lưu token này vào `sessionStorage` (khoá `saophone_session`) — **mất khi đóng tab trình duyệt**, không phải `localStorage`.

**Bước 2 — Mọi request sau đó:** frontend tự gắn header `Authorization: Bearer <token>`. Ở backend, `JwtAuthFilter.java` chạy trước mọi controller, đọc header này, xác minh chữ ký token, rồi nạp danh tính người dùng vào `SecurityContext` của Spring Security cho request đó.

> Riêng kênh cập nhật thời gian thực (`/api/don-hang/events`) dùng `EventSource` của trình duyệt — API này **không cho gắn header tuỳ ý**, nên `JwtAuthFilter` có một nhánh riêng: nếu URL kết thúc bằng `/don-hang/events`, nó đọc token từ cookie tên `sse_token` thay vì header. Đây là lý do bạn sẽ thấy 1 cookie lạ chỉ dùng cho mỗi endpoint này.

**Phân quyền có ở 2 lớp độc lập, cố tình trùng nhau:**

1. **Lớp giao diện** — `router/index.js`: mỗi route (`/admin`, `/staff`, `/kho`, `/account`) khai báo `meta.roles`. `router.beforeEach()` đọc session đã lưu, role không khớp thì đá về `/`. **Đây là 4 trang component hoàn toàn khác nhau** (`AdminPage.vue`, `StaffPage.vue`, `WarehouseManagementPage.vue`, `AccountPage.vue`) — không phải 1 trang admin ẩn/hiện nút theo quyền.
2. **Lớp máy chủ** — mỗi Controller khai `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN',...)")` ở đầu class. Lớp này mới là lớp an toàn thật: cho dù ai đó sửa code frontend hoặc gọi thẳng API bằng Postman với token của khách hàng, backend vẫn tự trả `403 Forbidden`. Trong đợt kiểm thử, nhóm đã xác nhận điều này bằng cách gọi thẳng API `/api/nhan-vien` với token khách hàng.

Ngoài ra `RateLimitingFilter.java` giới hạn **5 lần gọi/phút/IP** riêng cho `/api/auth/login` và `/api/khach-hang/tim-theo-sdt` — chặn dò mật khẩu và dò số điện thoại khách hàng đã có tài khoản.

## 4. Mô hình dữ liệu cốt lõi: Sản phẩm → Biến thể → Serial

Đây là khác biệt lớn nhất so với web bán hàng thông thường, và gần như mọi luồng nghiệp vụ phía dưới đều xoay quanh 3 tầng này:

```
san_pham (1)  "Dell Inspiron 15"
   └── bien_the_san_pham (N)  "i5/8GB/512GB — Bạc", "i7/16GB/512GB — Xanh"...
          └── chi_tiet_san_pham (N)  từng máy vật lý, 1 dòng = 1 số serial thật
```

- `san_pham`: thông tin chung (tên, mô tả, thương hiệu, danh mục).
- `bien_the_san_pham`: 1 cấu hình bán được, có giá riêng, tham chiếu tới 4 danh mục đã chuẩn hoá (`dm_cpu`, `dm_ram`, `dm_gpu`, `dm_o_cung`) thay vì cho gõ tay — lý do: nếu cho gõ tự do, 3 nhân viên có thể gõ "i5 12500H" / "Core i5-12500H" / "Intel i5 12500H" cho cùng 1 con chip, bộ lọc phía khách hàng sẽ bỏ sót.
- `chi_tiet_san_pham`: **từng chiếc máy có thật**, có `so_serial` và `trang_thai` (`trong_kho` / `da_ban` / lỗi...). Bảng `ton_kho` chỉ lưu số đếm tổng hợp theo biến thể, luôn đồng bộ với số dòng `chi_tiet_san_pham` đang ở trạng thái `trong_kho`.

Chính vì có tầng thứ 3 này, khi khách mang máy đi bảo hành, hệ thống tra theo đúng 1 số serial ra ngay: bán trong đơn nào, ngày nào, còn hạn bảo hành hay không — không cần khách xuất trình phiếu giấy.

## 5. Các luồng nghiệp vụ thật, đi từng bước

### 5.1. Duyệt và lọc sản phẩm (khách hàng, không cần đăng nhập)

Trang chủ gọi `GET /api/san-pham/hien-thi` kèm query param (từ khoá, hãng, khoảng giá, CPU, RAM...) → `SanPhamController` → `SanPhamService` build câu truy vấn động → SQL Server lọc ngay ở tầng DB (không tải hết rồi lọc bằng JS) → trả về danh sách đã phân trang.

### 5.2. Giỏ hàng — hoàn toàn ở phía trình duyệt, không gọi backend

Đây là điểm dễ hiểu nhầm nhất: **giỏ hàng không có bảng riêng trong CSDL và không có API riêng**. Biến `cart` là một `ref([])` khai báo ngay trong `App.vue`, được "phát" xuống mọi trang con qua cơ chế `provide/inject` của Vue (không dùng Pinia cho phần này — Pinia chỉ dùng cho dữ liệu bên admin như danh sách sản phẩm/đơn hàng/kho). Mỗi lần giỏ hàng thay đổi, nó tự lưu vào `localStorage` với khoá riêng theo từng tài khoản (`saophone_cart_<id_khách_hàng>`, hoặc `saophone_cart_guest` nếu chưa đăng nhập) — đây là lý do giỏ hàng của bạn A không lẫn với giỏ của bạn B trên cùng một trình duyệt, và vẫn còn nguyên nếu bạn tải lại trang.

Giỏ hàng chỉ **thật sự chạm tới backend** ở bước đặt hàng (mục 5.3).

### 5.3. Đặt hàng trực tuyến (khách hàng)

1. Khách bấm "Thanh toán" → `CheckoutModal.vue` gửi `POST /api/don-hang` kèm toàn bộ dòng trong giỏ, địa chỉ giao, mã giảm giá (nếu có).
2. `DonHangService.create()` (`service/DonHangService.java:103`) — toàn bộ nằm trong 1 `@Transactional`:
   - Ép `khachHangId` về đúng chủ tài khoản đang đăng nhập, **bỏ qua** giá trị client gửi lên (chặn 1 khách tự xưng là khách khác — lỗi IDOR đã được vá).
   - Nếu có mã giảm giá: **tự tính lại số tiền giảm ở server**, không tin số tiền client gửi — kiểm tra còn hạn, còn lượt, đủ điều kiện đơn tối thiểu. Không cho dùng đồng thời mã khuyến mãi công khai + voucher cá nhân.
   - Lưu đơn, sinh mã đơn hàng (trigger DB), gọi `sseService.notifyNewOrder()` để báo real-time.
3. Đơn mới luôn ở trạng thái `pending` ("Chờ xác nhận") — **serial vật lý chưa được gán ở bước này.**

### 5.4. Nhân viên xác nhận đơn + chọn serial

Đây là bước nối nghiệp vụ bán hàng với nghiệp vụ kho, nằm trong `DonHangService.xacNhanDonHang()` (`service/DonHangService.java:411`):

- Chỉ áp dụng cho đơn **online** (`kenhBan == "online"`) và đơn đang ở `pending`.
- Với mỗi dòng sản phẩm, nhân viên chọn đúng số serial còn `trong_kho`. Backend khoá từng dòng serial bằng `PESSIMISTIC_WRITE` (`findByIdForUpdate`) trước khi kiểm tra trạng thái — nếu không khoá, 2 nhân viên mở 2 tab cùng lúc có thể cùng chọn trúng 1 serial cho 2 đơn khác nhau (race condition thật đã từng xảy ra, ghi chú ngay trong code).
- Serial được chọn chuyển `trang_thai = "da_ban"`, ghi vào bảng nối `chi_tiet_don_hang_serial`. Đơn chuyển sang `confirmed`.

Từ đây trở đi, đơn đi tiếp theo đúng 1 chiều: `confirmed → processing → shipping → out_for_delivery → awaiting_confirmation → delivered`, có thể `cancelled` ở bất kỳ bước nào trước khi giao. Chuỗi chuyển trạng thái hợp lệ được khai cứng trong `CHUYEN_TRANG_THAI_DON_HANG` (một `Map` ở đầu `DonHangService.java:179`) — gọi sai thứ tự (ví dụ nhảy thẳng từ `pending` sang `delivered`) bị chặn ngay ở backend, không chỉ ẩn trên giao diện.

Bước "Đã nhận hàng" (`awaiting_confirmation → delivered`) tách riêng thành API khác (`xacNhanDaNhanHang()`), mở cho cả khách hàng tự bấm — để phân biệt rõ "nhân viên báo đã giao" và "khách xác nhận thật sự đã nhận", tránh một đơn tự động coi là hoàn tất khi chỉ mới có 1 phía xác nhận.

**Hủy đơn:** nếu đơn chuyển sang `cancelled`, `releaseSerialsToStock()` trả toàn bộ serial đã giữ về `trong_kho`, và `giaiPhongKhuyenMaiVoucher()` hoàn lại lượt dùng mã khuyến mãi/voucher — tránh tồn kho bị "kẹt" hoặc khách mất oan 1 voucher chỉ vì đơn bị huỷ.

### 5.5. Bán hàng tại quầy (POS) — khác gì đơn online?

`kenhBan = "in_store"`. Khác biệt chính:

- Serial được chọn **ngay lúc tạo dòng đơn** tại quầy, không qua bước "xác nhận" riêng như đơn online (vì khách đang đứng chờ, không có độ trễ giữa đặt và xử lý).
- Trạng thái đơn nhảy thẳng `confirmed → delivered` ngay sau khi thanh toán xong (bỏ qua toàn bộ `processing/shipping/out_for_delivery` — những bước đó chỉ có ý nghĩa với giao hàng, vô nghĩa khi khách nhận máy tại chỗ). Đây là **ngoại lệ duy nhất** được phép trong hàm `kiemTraChuyenTrangThai()`.
- Điểm tích lũy cộng ngay lập tức, không phải đợi tới lúc "giao hàng thành công" như đơn online.

### 5.6. Nhập kho

Quản kho tạo phiếu nhập (`phieu_nhap_kho` + các dòng `chi_tiet_phieu_nhap`), kèm nhập danh sách số serial của lô hàng. Khi xác nhận phiếu: mỗi serial sinh 1 dòng `chi_tiet_san_pham` mới (`trong_kho`), `ton_kho` cộng tương ứng, và ghi 1 dòng vào `lich_su_ton_kho` (số lượng trước/sau, người thực hiện) — bảng này là **nhật ký duy nhất** để truy vết mọi biến động tồn kho (nhập, bán, trả, điều chỉnh kiểm kê), không chỉ riêng cho nhập kho.

### 5.7. Trả hàng và bảo hành — dựa hoàn toàn vào serial

Cả 2 nghiệp vụ này không tra theo "đơn hàng nào mua sản phẩm gì" một cách chung chung, mà tra theo **đúng chiếc máy** qua bảng nối `chi_tiet_don_hang_serial` đã tạo ở bước 5.4:

- **Trả hàng:** lập `phieu_tra_hang`, xác nhận thì serial được trả chuyển lại `trong_kho`, `ton_kho` cộng lại, tính số tiền hoàn.
- **Bảo hành:** nhập 1 số serial → tra ngược ra đơn hàng gốc, ngày bán, hạn bảo hành còn lại (tính từ `bao_hanh_thang` lưu trên biến thể) → lập `phieu_bao_hanh`.

### 5.8. Tích điểm và vòng quay may mắn

Khi đơn chuyển `delivered` (hoặc ngay khi POS hoàn tất), hệ thống cộng điểm theo quy tắc ở `cai_dat_he_thong`, ghi 1 dòng vào `lich_su_tang_diem` kèm lý do. Vòng quay may mắn (`VongQuayController.quay()`) trừ điểm theo cấu hình, random ra phần thưởng, ghi `lich_su_quay`; nếu trúng voucher thì sinh 1 dòng `phieu_giam_gia_ca_nhan` mới, dùng được ngay ở lần đặt hàng tiếp theo.

### 5.9. Cập nhật thời gian thực (không cần F5)

`SseService.java` giữ 1 danh sách các kết nối `SseEmitter` đang mở (mỗi tab trình duyệt đang mở = 1 kết nối). Khi có đơn mới hoặc đổi trạng thái, `DonHangService` gọi `sseService.notifyNewOrder()` / `notifyOrderUpdate()` — 2 hàm này chạy `@Async` (bất đồng bộ, trên thread riêng), rồi `broadcast()` gửi event tới **toàn bộ** kết nối đang mở kèm `orderId` liên quan; phía frontend tự lọc xem `orderId` đó có phải đơn mình đang xem không rồi mới tải lại.

★ **Vì sao bắt buộc `@Async`:** nếu chạy đồng bộ ngay trong request tạo đơn, một kết nối SSE bị treo (tab đóng không sạch) sẽ chặn luôn request tạo đơn của khách hàng đang chờ phản hồi — dù đơn đã lưu thành công trong DB. Tách `@Async` để việc "báo tin" không bao giờ làm chậm việc "lưu dữ liệu".

## 6. Vài nguyên tắc lặp lại xuyên suốt (đáng nhớ hơn từng luồng riêng lẻ)

- **Không tin dữ liệu client gửi cho bất cứ thứ gì tính được lại ở server** — giá, số tiền giảm, ai là chủ đơn... luôn tính/kiểm tra lại ở Service, vì client có thể bỏ qua giao diện và gọi thẳng API.
- **Mọi thao tác ảnh hưởng nhiều bảng nằm trong 1 `@Transactional`** — lỗi giữa chừng thì toàn bộ hoàn tác, không có chuyện "đơn đã lưu nhưng kho quên trừ".
- **Trạng thái đơn hàng chỉ đi 1 chiều theo bảng chuyển trạng thái khai cứng** — không phải trường tự do, để không ai (kể cả nhân viên thao tác nhầm) đẩy đơn lùi lại hoặc nhảy cóc bước.
- **Xoá mềm cho dữ liệu đã phát sinh nghiệp vụ** (nhân viên, khách hàng, sản phẩm) — chuyển `trang_thai`/`da_xoa`, không xoá thật, để lịch sử đơn hàng cũ không bị vỡ.
- **Mỗi thay đổi tồn kho đều có nhật ký** (`lich_su_ton_kho`) — không có nghiệp vụ nào được phép âm thầm cộng/trừ tồn kho mà không ghi log.

## 7. Muốn tự tìm hiểu tiếp một tính năng cụ thể thì bắt đầu từ đâu

1. Xác định tên nghiệp vụ bằng tiếng Việt không dấu, gạch dưới — ví dụ "phiếu bảo hành" → `phieu_bao_hanh`.
2. Backend: mở `controller/PhieuBaoHanhController.java` xem có những API nào (method + URL) → nhảy sang `service/PhieuBaoHanhService.java` để đọc logic thật.
3. Frontend: `grep -ri "phieuBaoHanh\|PhieuBaoHanhService" src/` để tìm component/service gọi API đó.
4. Muốn biết cấu trúc bảng: mở `Database/QLBanMayTinh.sql`, tìm `CREATE TABLE phieu_bao_hanh`.
5. Muốn biết đặc tả nghiệp vụ gốc (trước khi viết code): tìm trong `docs/superpowers/specs/` — mỗi tính năng lớn đều có 1 file spec + 1 file plan viết trước khi code, giải thích lý do phía sau quyết định thiết kế.
