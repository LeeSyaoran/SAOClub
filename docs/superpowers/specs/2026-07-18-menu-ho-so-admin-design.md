# Thiết kế: Menu hồ sơ admin (ô "Quản trị viên" ở chân sidebar)

## Bối cảnh

Ô "Quản trị viên" ở chân sidebar (`AdminPage.vue`, khối "Footer sidebar") hiện chỉ là `<div>` tĩnh hiển thị avatar (chữ cái đầu tên), `userDisplayName`, `userDisplayRole` — không bấm được. Ngay dưới nó là nút "Đăng xuất" riêng.

Tên/SĐT/email của người đang đăng nhập đã có sẵn trong `AuthStore.user` (từ `LoginResponse`, gồm `id, hoTen, username, soDienThoai, email, role, token`) nhưng chỉ `hoTen`/`role` đang được dùng hiển thị — chưa có cách nào để tự sửa các trường này.

Người dùng trang admin (`AdminPage.vue`) luôn là tài khoản vai trò `admin`/`nhan_vien`/`quan_kho` — tức luôn có 1 dòng `NhanVien` đứng sau `TaiKhoan` (đã xác nhận qua khảo sát: `TaiKhoan` chỉ liên kết `nhan_vien` HOẶC `khach_hang`, trang `AdminPage.vue` chỉ hiện khi `auth.isAdmin`, và 3 vai trò staff đều dùng `nhan_vien`). Khách hàng đã có sẵn trang tự sửa hồ sơ riêng (`AccountPage.vue:135-183`) — không thuộc phạm vi thay đổi này.

`NhanVienService.update()` đã có sẵn nhưng nhận `NhanVienRequest` đầy đủ (tên/SĐT/email/**chức vụ**/**lương**/**trạng thái**/mật khẩu) và chỉ gọi được qua `NhanVienController` (`@PreAuthorize("hasRole('ADMIN')")`, không phân biệt "sửa hồ sơ của chính mình" hay "admin sửa nhân viên khác") — dùng lại trực tiếp cho "tự sửa hồ sơ" rủi ro vô tình cho tự đổi cả chức vụ/lương nếu request thiếu field. Cần 1 đường ống hẹp hơn, chỉ đụng đúng 3 trường tên/SĐT/email.

## Phạm vi

1. Ô "Quản trị viên" trở thành nút bấm mở **dropdown menu** ngay phía trên nó (mở lên vì đang ở cuối sidebar), gồm 3 mục:
   - **Chỉnh sửa hồ sơ** — mở modal sửa Họ tên/SĐT/Email.
   - **Đổi mật khẩu** — mở modal đổi mật khẩu (dùng lại đúng API đã có ở Cài đặt, không xây lại logic).
   - **Cài đặt** — điều hướng sang tab Cài đặt đầy đủ.
2. Backend: 1 endpoint mới `PUT /api/cai-dat/ho-so`, tự phục vụ (mọi vai trò, xác định người gọi qua `SecurityContextHolder`), chỉ sửa `hoTen`/`soDienThoai`/`email`.
3. Sau khi lưu hồ sơ thành công, `AuthStore` cập nhật ngay (giống `AccountPage.vue`) — tên trên sidebar đổi liền, không cần đăng nhập lại.

## Backend

### DTO mới

`request/HoSoRequest.java` — theo đúng style `@NoArgsConstructor @Getter @Setter` + Bean Validation, giống `DoiMatKhauRequest.java`:
```java
package com.example.backend.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class HoSoRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String soDienThoai;

    @Email(message = "Email không hợp lệ")
    private String email;
}
```

`response/HoSoResponse.java` — `@AllArgsConstructor @Getter @Setter`, giống `CaiDatHeThongResponse.java`:
```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class HoSoResponse {
    private String hoTen;
    private String soDienThoai;
    private String email;
}
```

### `AuthService` — thêm `capNhatHoSo`

Cùng file đã có `doiMatKhau` (đúng chỗ — logic thuộc "tài khoản của tôi", đã autowire sẵn `TaiKhoanRepository`):
```java
public HoSoResponse capNhatHoSo(String username, HoSoRequest req) {
    TaiKhoan tk = taiKhoanRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
    NhanVien nv = tk.getNhanVien();
    if (nv == null) {
        throw new IllegalStateException("Tài khoản này không có hồ sơ nhân viên để chỉnh sửa");
    }
    nv.setHoTen(req.getHoTen());
    nv.setSoDienThoai(req.getSoDienThoai());
    nv.setEmail(req.getEmail());
    nhanVienRepository.save(nv);
    return new HoSoResponse(nv.getHoTen(), nv.getSoDienThoai(), nv.getEmail());
}
```
Cần thêm `@Autowired private NhanVienRepository nhanVienRepository;` vào `AuthService`. `so_dien_thoai`/`email` có `@Column(unique = true)` trên `NhanVien` — trùng số của nhân viên khác sẽ ném `DataIntegrityViolationException`, đã có `GlobalExceptionHandler.handlerEntityNotFound` bắt sẵn (trả 400 kèm thông báo "Dữ liệu không hợp lệ..."), không cần bắt thêm ở đây.

### `CaiDatController` — thêm endpoint

Đặt cạnh `doiMatKhau` (cùng lý do: `/api/cai-dat/**` không nằm trong `permitAll()`, tự phục vụ mọi vai trò):
```java
@PutMapping("/ho-so")
public HoSoResponse capNhatHoSo(@Valid @RequestBody HoSoRequest req) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return authService.capNhatHoSo(username, req);
}
```
Không thêm `@PreAuthorize` — giống `doiMatKhau`, CỐ Ý mở cho mọi vai trò tự sửa hồ sơ của chính mình.

## Frontend

### Service

Thêm vào `CaiDatService.js`:
```js
export const capNhatHoSo = (data) => put('/api/cai-dat/ho-so', data).then(parseOrThrow);
```

### Dropdown menu ở chân sidebar

Thay khối "Footer sidebar" hiện tại (`<div>` tĩnh bọc avatar+tên+role, rồi nút "Đăng xuất" riêng bên dưới) bằng: bọc avatar+tên+role trong `<div class="position-relative">`, bấm vào toggle `showUserMenu`, panel dropdown `v-if="showUserMenu"` với `position:absolute; bottom:100%` (mở lên trên, vì đang ở cuối sidebar) chứa 3 mục menu — theo đúng pattern dropdown đã có ở `NavBar.vue` (`position-relative` + `v-if` panel + đóng khi `@focusout`/phím Esc). Nút "Đăng xuất" giữ nguyên vị trí hiện tại, không đưa vào trong dropdown (đã có sẵn, không đổi).

3 mục trong dropdown:
- "Chỉnh sửa hồ sơ" → đóng dropdown, mở `showEditProfileModal = true`.
- "Đổi mật khẩu" → đóng dropdown, mở `showChangePasswordModal = true`.
- "Cài đặt" → đóng dropdown, `navigate('settings')`.

### Modal "Chỉnh sửa hồ sơ"

Dùng component `Modal.vue` có sẵn (`v-model="showEditProfileModal"`). Form 3 input (Họ tên/SĐT/Email), giá trị khởi tạo từ `AuthStore.user` lúc mở modal. Lưu → gọi `CaiDatService.capNhatHoSo(...)` → thành công thì:
```js
setSession({ ...AuthStore.user, hoTen: res.hoTen, soDienThoai: res.soDienThoai, email: res.email });
```
(theo đúng pattern `AccountPage.vue:179` đã dùng) — sidebar cập nhật tên ngay, đóng modal. Lỗi (vd trùng SĐT/email) hiện thông báo lỗi inline trong modal, theo đúng pattern các form khác trong `AdminPage.vue` (`formError`).

Cần thêm import `setSession` vào `AdminPage.vue` (hiện chỉ import `AuthStore, clearSession`).

### Modal "Đổi mật khẩu"

Dùng lại `Modal.vue` + gọi thẳng `CaiDatService.doiMatKhau(...)` đã có sẵn từ Task 3/7 — **không viết lại logic**, chỉ là 1 modal nhỏ mới (3 input mật khẩu hiện tại/mới/xác nhận + nút Lưu) làm nơi gọi nhanh, tách biệt khỏi form đổi mật khẩu đã có trong trang Cài đặt (2 nơi cùng gọi 1 API, không phải 2 API khác nhau).

### i18n

Thêm key mới dưới `admin.sidebar.*` hoặc khối mới `admin.profileMenu.*` cho cả 5 ngôn ngữ: nhãn 3 mục menu, tiêu đề 2 modal, nhãn 3 field hồ sơ, thông báo lưu thành công/thất bại.

## Xử lý lỗi / trường hợp biên

- SĐT hoặc email trùng với nhân viên khác → 400 (đã có `GlobalExceptionHandler` bắt sẵn `DataIntegrityViolationException`), hiện lỗi inline trong modal, không đóng modal.
- Tài khoản gọi API không có `NhanVien` liên kết (về lý thuyết không xảy ra vì `AdminPage.vue` chỉ vào được khi `auth.isAdmin`, nhưng vẫn chặn ở service — `IllegalStateException`, `GlobalExceptionHandler.handlerBusinessErrors` đã bắt sẵn, trả 400) → không lộ lỗi 500.
- Đóng dropdown khi bấm ra ngoài hoặc phím Esc (đúng pattern `NavBar.vue`).
- 2 modal không mở đồng thời (mở modal này thì đóng dropdown, không có xung đột z-index vì dropdown đã đóng trước khi modal mở).

## Kiểm thử

- Test service: `AuthService.capNhatHoSo` — cập nhật đúng 3 trường trên `NhanVien`, không đụng `chucVu`/lương/trạng thái/mật khẩu (mock `NhanVien` có sẵn giá trị các trường đó, assert sau khi gọi vẫn giữ nguyên). Test trường hợp tài khoản không có `NhanVien` → ném `IllegalStateException`.
- Không cần test JPQL/DB constraint mới (không có, dùng `save()` thường qua JPA, đã có sẵn `@Column(unique=true)`).

## Ngoài phạm vi

- Không thêm upload avatar (chưa có cột lưu, không được yêu cầu — avatar vẫn là chữ cái đầu tên tự sinh).
- Không đổi luồng tự sửa hồ sơ của khách hàng (`AccountPage.vue`) — đã có sẵn, hoạt động độc lập.
- Không thêm bước xác nhận email/SĐT mới (đổi là có hiệu lực ngay, giống cách `NhanVienService.update()` hiện tại đang làm khi admin sửa nhân viên khác).
