# Thiết kế: Trang Cài đặt (admin)

## Bối cảnh

Trang "Cài đặt" hiện tại (`AdminPage.vue:3640-3660`) chỉ là 1 card tĩnh hiển thị thông tin hệ thống hard-code (tên hệ thống, phiên bản, backend API, database, trạng thái) — không lưu được gì, không có backend đứng sau.

Khảo sát cho thấy:
- Chưa có bảng/entity/controller nào liên quan cấu hình hệ thống trong backend.
- Theme (sáng/tối) và ngôn ngữ đã hoạt động, nhưng chỉ lưu `localStorage` phía trình duyệt (`stores/theme.js`, `i18n/index.js`), không có "mặc định hệ thống".
- Chưa có chức năng đổi mật khẩu ở bất kỳ đâu trong app (kể cả trang tài khoản khách hàng).
- Cột `ton_kho.ton_kho_toi_thieu` đã có sẵn cho từng biến thể, nhưng chưa có UI chỉnh hàng loạt.
- `formatPrice` (định dạng tiền VNĐ) bị lặp định nghĩa ở 9 file: `AdminPage.vue`, `RevenueBarChart.vue`, `CheckoutModal.vue`, `App.vue`, `AccountPage.vue`, `ProductDetail.vue`, `ProductCard.vue`, `CartItem.vue`, `CartSummary.vue`.
- Endpoint upload ảnh dùng chung `/api/upload/image` đã có sẵn (`UploadController.java`), đang được `AdminPage.vue:1339` gọi trực tiếp qua `fetch` cho ảnh sản phẩm — tái dùng được cho logo cửa hàng, không cần thêm endpoint upload mới.
- Toàn bộ vai trò (admin/nhân viên/quản kho/khách hàng) đăng nhập qua chung 1 bảng `tai_khoan` (`entity/TaiKhoan.java`, cột `mat_khau_hash`) — 1 endpoint đổi mật khẩu dùng được cho mọi vai trò, xác định người dùng hiện tại qua `SecurityContextHolder` (JWT chỉ mang `username`, không mang id số).

## Phạm vi

Xây trang Cài đặt thành 5 card:

1. **Đổi mật khẩu** — tự đổi mật khẩu tài khoản đang đăng nhập, bắt buộc nhập mật khẩu hiện tại.
2. **Thông tin cửa hàng** — tên, địa chỉ, SĐT, email, mã số thuế, logo (upload ảnh).
3. **Ngưỡng cảnh báo tồn kho** — nhập 1 số, bấm nút để ghi đè `ton_kho_toi_thieu` lên **toàn bộ** biến thể đang có trong kho (không phải giá trị mặc định cho biến thể mới).
4. **Giao diện & ngôn ngữ** — mirror control sáng/tối + đổi ngôn ngữ đã có ở topbar, cộng thêm 2 tùy chọn mới: ngôn ngữ mặc định cho người dùng chưa từng chọn ngôn ngữ, và định dạng số (kiểu Việt `1.234.567` / kiểu Anh `1,234,567`) — áp dụng thật cho toàn app bằng cách gộp 9 file `formatPrice` về dùng chung 1 hàm.
5. **Thông tin hệ thống** — giữ nguyên card tĩnh hiện có, không đổi.

## Backend

### Bảng mới `cai_dat_he_thong` (singleton — luôn đúng 1 dòng, `cai_dat_id = 1`)

Thêm vào cuối `Database/QLBanMayTinh.sql`, idempotent theo đúng quy ước dự án (user luôn chạy lại toàn bộ file):

```sql
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='cai_dat_he_thong' AND xtype='U')
CREATE TABLE cai_dat_he_thong (
    cai_dat_id INT PRIMARY KEY,
    ten_cua_hang NVARCHAR(200) NOT NULL DEFAULT N'SAOPhone',
    dia_chi NVARCHAR(300) NOT NULL DEFAULT '',
    so_dien_thoai NVARCHAR(20) NOT NULL DEFAULT '',
    email NVARCHAR(100) NOT NULL DEFAULT '',
    ma_so_thue NVARCHAR(20) NOT NULL DEFAULT '',
    logo_url NVARCHAR(300) NULL,
    nguong_ton_kho_mac_dinh INT NOT NULL DEFAULT 5,
    ngon_ngu_mac_dinh VARCHAR(5) NOT NULL DEFAULT 'vi',
    dinh_dang_so VARCHAR(5) NOT NULL DEFAULT 'vi'
);

IF NOT EXISTS (SELECT * FROM cai_dat_he_thong WHERE cai_dat_id = 1)
INSERT INTO cai_dat_he_thong (cai_dat_id) VALUES (1);
```

### Entity / Repository / Service / Controller

- `entity/CaiDatHeThong.java` — `@Entity @Table(name="cai_dat_he_thong") @Getter @Setter`, `@Id private Integer caiDatId;` (không `@GeneratedValue` — luôn ghi cứng 1).
- `repository/CaiDatHeThongRepository.java extends JpaRepository<CaiDatHeThong, Integer>`.
- `request/CaiDatHeThongRequest.java` — 8 field ghi được (không gồm `caiDatId`).
- `request/DoiMatKhauRequest.java` — `matKhauCu`, `matKhauMoi` (`@NotBlank`, `matKhauMoi` `@Size(min=6)`).
- `response/CaiDatHeThongResponse.java` — map 1:1 entity.
- `service/CaiDatHeThongService.java`:
  - `get()` — `repository.findById(1).orElseThrow(...)` (luôn tồn tại vì đã seed ở SQL).
  - `update(CaiDatHeThongRequest req)` — load dòng id=1, set field, save.
  - `apDungNguongTonKhoChoTatCa(int nguong)` — lưu `nguongTonKhoMacDinh` mới vào `cai_dat_he_thong`, đồng thời gọi `tonKhoRepository.capNhatNguongChoTatCa(nguong)` (bulk update, `@Modifying @Transactional`).
- `controller/CaiDatController.java` — `@RequestMapping("/api/cai-dat")`:
  - `GET ""` → `CaiDatHeThongResponse`.
  - `PUT ""` → nhận `CaiDatHeThongRequest`, trả response đã cập nhật.
  - `POST "/ap-dung-nguong-ton-kho"` → nhận `{nguong: int}`, trả `{soBienTheDaCapNhat: int}`.
- `TonKhoRepository` — thêm:
  ```java
  @Modifying
  @Transactional
  @Query("UPDATE TonKho t SET t.tonKhoToiThieu = :nguong")
  int capNhatNguongChoTatCa(@Param("nguong") int nguong);
  ```
- `AuthController` — thêm `POST /api/auth/doi-mat-khau`:
  ```java
  @PostMapping("/doi-mat-khau")
  public ResponseEntity<?> doiMatKhau(@Valid @RequestBody DoiMatKhauRequest req) {
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      TaiKhoan tk = taiKhoanRepository.findByUsername(username)
              .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));
      if (!passwordEncoder.matches(req.getMatKhauCu(), tk.getMatKhauHash())) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Mật khẩu hiện tại không đúng"));
      }
      tk.setMatKhauHash(passwordEncoder.encode(req.getMatKhauMoi()));
      taiKhoanRepository.save(tk);
      return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
  }
  ```
  (Cần `@Autowired TaiKhoanRepository`, `PasswordEncoder` — cùng bean đã dùng ở `login()`.)

## Frontend

### Service mới `Service/CaiDatService.js`

`api.js` đã có sẵn `get`/`post`/`put` — nhưng lưu ý **`post`/`put` trả `Promise<Response>` chưa parse** (khác `get`, đã parse sẵn JSON), theo đúng comment cảnh báo ở đầu file (`api.js:13-16`). Các hàm ghi phải tự `.json()` và tự kiểm tra `res.ok`:

```js
import { get, put, post } from './api.js';

const parseOrThrow = async (res) => {
  if (!res.ok) throw new Error((await res.text().catch(() => '')) || `HTTP ${res.status}`);
  return res.json();
};

export const getCaiDat = () => get('/api/cai-dat');
export const updateCaiDat = (data) => put('/api/cai-dat', data).then(parseOrThrow);
export const apDungNguongTonKho = (nguong) => post('/api/cai-dat/ap-dung-nguong-ton-kho', { nguong }).then(parseOrThrow);
export const doiMatKhau = (matKhauCu, matKhauMoi) => post('/api/auth/doi-mat-khau', { matKhauCu, matKhauMoi }).then(parseOrThrow);
```

### Store mới `stores/settings.js`

Cùng pattern `stores/theme.js` — nhưng khác ở chỗ cần tải từ API (không chỉ đọc `localStorage`):

```js
import { reactive } from "vue";
import { getCaiDat } from "../Service/CaiDatService.js";

export const SettingsStore = reactive({ dinhDangSo: "vi", ngonNguMacDinh: "vi", loaded: false });

export const loadSettings = async () => {
  try {
    const data = await getCaiDat();
    SettingsStore.dinhDangSo = data.dinhDangSo || "vi";
    SettingsStore.ngonNguMacDinh = data.ngonNguMacDinh || "vi";
  } catch { /* giữ mặc định 'vi' nếu tải lỗi — không chặn app */ }
  SettingsStore.loaded = true;
};
```

Gọi `loadSettings()` 1 lần ở `App.vue` `onMounted` (không chặn render — mặc định `'vi'` dùng ngay, cập nhật lại khi tải xong).

### `utils/formatPrice.js` (mới) — gộp 9 file

```js
import { SettingsStore } from "../stores/settings.js";

export const formatPrice = (v) =>
  new Intl.NumberFormat(SettingsStore.dinhDangSo === "en" ? "en-US" : "vi-VN",
    { style: "currency", currency: "VND" }).format(v || 0);
```

9 file thay `const formatPrice = (v) => new Intl.NumberFormat(...)` bằng `import { formatPrice } from '@/utils/formatPrice'` (hoặc đường dẫn tương đối đúng theo vị trí file — dự án đã có alias `@` trong `vite.config.js`). Không đổi chữ ký hàm, không đổi call site nào khác — chỉ đổi nguồn định nghĩa.

### Ngôn ngữ mặc định khi đăng nhập

`i18n/index.js` — chỗ khởi tạo locale hiện đọc `localStorage` trước, mặc định `'vi'` nếu chưa có. Sau khi `loadSettings()` chạy xong ở `App.vue`, nếu `localStorage` **chưa từng có** key `saophone_locale` (người dùng chưa từng tự chọn), set locale theo `SettingsStore.ngonNguMacDinh`. Người đã từng đổi ngôn ngữ (đã có key) không bị ghi đè.

### UI trang Cài đặt (`AdminPage.vue`, section `settings`)

4 card mới chèn trước card "Thông tin hệ thống" hiện có, mỗi card có nút Lưu/Áp dụng riêng (không gộp 1 form lớn):

1. **Đổi mật khẩu** — 3 input password (hiện tại/mới/xác nhận), validate khớp ở FE trước khi gọi API, hiện lỗi rõ dưới field khi sai mật khẩu cũ.
2. **Thông tin cửa hàng** — form input thường (tên/địa chỉ/SĐT/email/mã số thuế) + ô chọn ảnh logo dùng lại đúng flow `fetch('/api/upload/image', ...)` đang có ở `AdminPage.vue:1339`, preview logo ngay sau khi chọn, nút Lưu gọi `updateCaiDat`.
3. **Ngưỡng tồn kho** — 1 input number + nút "Áp dụng cho tất cả biến thể", bấm mở modal xác nhận (hiện số biến thể sẽ bị ảnh hưởng, lấy từ `inventory.value.length` đã có sẵn ở client) trước khi gọi `apDungNguongTonKho`.
4. **Giao diện & ngôn ngữ** — toggle sáng/tối (`toggleTheme` đã có), dropdown ngôn ngữ hiện tại (5 lựa chọn đã có trong `i18n`), dropdown "ngôn ngữ mặc định khi đăng nhập", dropdown "định dạng số" kèm preview mẫu số ngay cạnh (`1.234.567 đ` / `1,234,567 đ`).

### i18n

Thêm khối `admin.settings.*` mới cho 5 ngôn ngữ (nhãn form, thông báo lỗi, nút bấm) — theo đúng quy ước dự án đã làm ở các tính năng trước (đủ cả 5 file `vi/en/zh/ko/ja`).

## Xử lý lỗi / trường hợp biên

- Đổi mật khẩu sai mật khẩu cũ → HTTP 400 kèm message, FE hiện lỗi tại chỗ, không đăng xuất.
- Mật khẩu mới trùng mật khẩu cũ → cho phép (không cấm, không phải yêu cầu bảo mật đặt ra ở đây).
- `loadSettings()` lỗi mạng lúc khởi động → dùng mặc định `'vi'`/`'vi'`, không chặn app, không hiện lỗi làm phiền người dùng cuối (chỉ ảnh hưởng admin khi vào lại trang Cài đặt mới thấy tải lại được).
- Áp dụng ngưỡng tồn kho cho 0 biến thể (kho trống) → vẫn cho bấm, trả `soBienTheDaCapNhat: 0`, không lỗi.
- Upload logo thất bại (mạng, file quá lớn) → hiện lỗi ngay dưới ô chọn ảnh, không chặn phần còn lại của form (các field khác vẫn lưu được nếu bấm Lưu mà không đổi logo).
- Toàn bộ endpoint mới thuộc phạm vi admin — dùng chung `SecurityConfig` hiện tại, không cần rule bảo mật mới (riêng `POST /api/auth/doi-mat-khau` áp dụng cho MỌI vai trò đã đăng nhập, không giới hạn admin, vì đặt trong `AuthController` cùng chỗ với login).

## Kiểm thử

- Test tầng service (Mockito) cho `CaiDatHeThongService`: `apDungNguongTonKhoChoTatCa` gọi đúng repository method với đúng tham số.
- Test cho endpoint đổi mật khẩu: sai mật khẩu cũ → trả lỗi, không update DB; đúng mật khẩu cũ → `matKhauHash` được encode lại (khác chuỗi cũ) và lưu.
- Không cần test JPQL `UPDATE` mới (pattern `@Modifying` đơn giản, tự tin theo pattern annotation Spring Data chuẩn).

## Ngoài phạm vi

- Không làm multi-currency thật (vẫn luôn là VNĐ) — "định dạng số" chỉ đổi cách nhóm chữ số (dấu chấm/dấu phẩy), không đổi đơn vị tiền tệ.
- Không thêm 2FA, quản lý phiên đăng nhập, hay các tính năng bảo mật nâng cao khác ngoài đổi mật khẩu.
- Không tự động dùng "ngưỡng tồn kho mặc định" khi tạo biến thể mới (đã chốt ở vòng brainstorm: chỉ là hành động áp dụng hàng loạt thủ công, không phải giá trị default cho form tạo mới).
- Không thêm chức năng xuất/nhập cấu hình (backup/restore settings).
