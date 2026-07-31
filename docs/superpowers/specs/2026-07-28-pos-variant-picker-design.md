# Tối ưu hiển thị biến thể ở màn Bán hàng tại quầy (POS) — Design Spec

## Bối cảnh

`PosPanel.vue` (màn "Bán hàng" trong Admin/Staff, component POS tại quầy) hiện lấy sản
phẩm thẳng từ `ProductsStore.items` — nguồn dữ liệu từ API `/api/san-pham/hien-thi`, vốn
là danh sách phẳng **1 dòng/biến thể** (mỗi tổ hợp cấu hình + màu là 1 dòng riêng, cùng
`sanPhamId` nhưng khác `bienTheId`). Vì `posProducts` không gộp theo sản phẩm, 1 sản phẩm
có nhiều cấu hình/màu sẽ hiện thành nhiều card riêng biệt, cùng tên, khác giá — gây khó
tìm và dễ chọn nhầm khi nhân viên bán hàng tại quầy.

Bên khách hàng (`App.vue` + `ProductCard.vue` + `ProductDetail.vue`) đã giải quyết đúng
vấn đề này: lưới sản phẩm gộp 1 card/sản phẩm (`filteredProducts`), bấm vào card mở
`ProductDetail.vue` — nơi có bộ chọn "Phiên bản (cấu hình)" và "Màu sắc" dạng nút bấm,
thu hẹp dần về đúng 1 `bienTheId`.

Yêu cầu: đưa cách hiển thị/gộp biến thể của POS về giống mẫu này, **không đổi luồng
nghiệp vụ đang có** (cổng xác định khách hàng, modal chọn serial, giỏ hàng, giữ đơn,
khuyến mãi, tạo đơn — backend không đổi gì).

## Kiến trúc

Thuần thay đổi frontend, không đổi API/backend. Gồm 3 phần:

1. Tách hàm gộp sản phẩm dùng chung giữa `App.vue` và `PosPanel.vue`.
2. Đổi lưới sản phẩm trong `PosPanel.vue` sang hiển thị theo sản phẩm đã gộp.
3. Thêm 1 modal mới (inline trong `PosPanel.vue`, theo đúng convention hiện tại của file
   này — 2 modal khác của POS cũng nằm inline, không tách file riêng) để chọn cấu
   hình/màu trước khi vào modal "Chọn serial" sẵn có.

## 1. Hàm gộp sản phẩm dùng chung

File mới: `FrontEnd/QLBanMayTinh/src/utils/productGrouping.js`

Tách đúng logic dedupe đang nằm inline trong `App.vue`'s `filteredProducts` (ưu tiên biến
thể `trangThai === 'active'`, tie-break giá thấp nhất) thành 2 hàm thuần (pure function),
nhận vào 1 mảng biến thể phẳng:

- `groupBySanPham(items)` → trả về mảng đã gộp, 1 phần tử/`sanPhamId`, chọn biến thể đại
  diện theo đúng quy tắc hiện tại của `App.vue`.
- `variantCountBySanPham(items)` → `Map<sanPhamId, number>` — số biến thể của từng sản
  phẩm trong `items` (dùng để quyết định hiện tiền tố "Từ" trên card).

`App.vue` đổi `filteredProducts` và `variantCountMap` sang gọi 2 hàm này thay vì tự viết
lại (hành vi giữ nguyên y hệt — chỉ là refactor rút gọn, có test xác nhận không đổi output).

`PosPanel.vue` gọi cùng 2 hàm này trên tập đã lọc `trangThai === 'active'` +
từ khoá tìm kiếm hiện có (`posProducts` hiện tại) — vì POS chỉ nên hiện cấu hình còn hàng
(hành vi lọc `active` giữ nguyên, không đổi).

## 2. Lưới sản phẩm POS

- `posProducts` (đang là danh sách phẳng đã lọc theo `active` + tìm kiếm) đổi tên/vai trò
  thành nguồn "pool" cho việc gộp — giữ nguyên logic lọc hiện tại.
- Thêm `posProductGroups = computed(() => groupBySanPham(posProducts.value))` — dùng để
  render lưới card thay cho `posProducts`.
- Thêm `posVariantCountMap = computed(() => variantCountBySanPham(posProducts.value))`.
- Card sản phẩm: giữ nguyên style/kích thước hiện tại (không đổi sang style to như
  `ProductCard.vue` bên khách — POS cần mật độ cao, nhiều card/màn hình). Chỉ đổi:
  - Lặp qua `posProductGroups` thay vì `posProducts`.
  - Thêm tiền tố "Từ " trước giá khi `posVariantCountMap.get(p.sanPhamId) > 1` (tái dùng
    key `home.fromPrice`).
  - Nút "Thêm vào giỏ" gọi `posOpenVariantPicker(p)` (hàm mới) thay vì thẳng
    `posOpenSerialPicker(p)`.

## 3. Modal chọn cấu hình/màu (mới, inline trong PosPanel.vue)

Mở **luôn**, kể cả sản phẩm chỉ có 1 biến thể (quyết định có chủ đích: nhất quán UX cho
mọi trường hợp, chấp nhận thêm 1 click cho trường hợp phổ biến nhất — khác với hành vi
"bỏ qua khi chỉ có 1 lựa chọn" đang dùng bên khách hàng ở `handleQuickAdd`).

State mới:
```js
const showVariantPicker = ref(false);
const variantPickerBase = ref(null);      // sản phẩm đại diện vừa bấm (từ posProductGroups)
const variantPickerActiveConfigKey = ref('');
const variantPickerActiveColor = ref('');
```

Hàm mới:
- `posOpenVariantPicker(p)` — thay thế điểm gọi cũ từ nút "Thêm vào giỏ". Giữ nguyên đúng
  guard đang có ở đầu `posOpenSerialPicker` hiện tại (chặn nếu `posStage !== 'selling'`,
  tự `posStartInvoice()` + báo lỗi `needCustomerFirst` — copy nguyên, không đổi). Sau đó
  set `variantPickerBase.value = p`, khởi tạo `activeConfigKey`/`activeColor` theo `p`,
  mở `showVariantPicker.value = true`.
- `variantPickerVariants = computed(...)` — lọc `posProducts.value` (pool đã lọc active)
  theo `sanPhamId === variantPickerBase.value?.sanPhamId`.
- `variantPickerConfigs`, `variantPickerColorsForConfig`, `variantPickerActiveVariant` —
  copy nguyên logic `configKey` / `configs` / `colorsForConfig` / `activeVariant` từ
  `ProductDetail.vue`, đổi tên biến cho khớp namespace POS, chạy trên
  `variantPickerVariants` thay vì `props.products`.
- `posConfirmVariant()` — đóng `showVariantPicker`, gọi
  `posOpenSerialPicker(variantPickerActiveVariant.value)` **y hệt hàm hiện có, không sửa
  gì bên trong nó**.

UI modal: kích thước ~480px giống modal "Chọn serial" đang có (dùng chung style
`position-fixed` + `rounded-4` + `border-secondary` của 2 modal kia trong cùng file để
đồng bộ). Nội dung:
- Header: tên sản phẩm (`variantPickerBase.tenSanPham`) + SKU, nút đóng.
- Khối "Phiên bản (cấu hình)" nếu `variantPickerConfigs.length > 1` — nút bấm, style/label
  copy từ `ProductDetail.vue` (dòng 1: CPU hoặc RAM; dòng 2: RAM + ổ cứng).
- Khối "Màu sắc" nếu có màu — nút bấm có chấm màu (`colorDot`, copy nguyên bảng màu từ
  `ProductDetail.vue`) + giá của màu đó.
- Nút "Tiếp tục chọn serial" (key mới `admin.pos.continueToSerial`) — gọi
  `posConfirmVariant()`.

## Phạm vi KHÔNG đổi (explicit)

- `posStage` (start → phone → selling) và toàn bộ cổng xác định khách hàng.
- `posOpenSerialPicker`, `posSelectSerial`, `setSerialTrangThai`, modal "Chọn serial".
- Giỏ hàng POS (`posCart`, `posRemove`, `posReset`), giữ đơn (`posHoldOrder`,
  `posResumeHeld`, `posDeleteHeld`).
- Mã khuyến mãi, tính tổng tiền, `posPlaceOrder`.
- Mọi API/backend — 0 thay đổi.

## i18n

Tái dùng key có sẵn: `home.fromPrice`, `productDetail.versions`, `productDetail.colorHeading`
(hoặc tương đương trong namespace `admin.pos` nếu muốn tách riêng ngữ cảnh admin — quyết
định cụ thể để lúc viết plan, ưu tiên tái dùng trước). Key mới cần thêm (5 locale):
- `admin.pos.chooseVariantTitle` (nếu cần tiêu đề riêng, có thể dùng thẳng tên sản phẩm)
- `admin.pos.continueToSerial`

## Testing

Không có test tự động hiện có cho `PosPanel.vue` (component thuần UI, không phải service
layer) — theo đúng convention hiện tại của dự án, phần này verify bằng cách chạy thực tế
qua Playwright (mở POS, kiểm tra 1 sản phẩm nhiều biến thể chỉ còn 1 card, bấm vào mở
đúng modal, chọn cấu hình/màu xong vào đúng modal chọn serial hiện có, hoàn tất tạo đơn
bình thường). `productGrouping.js` (hàm thuần, không phụ thuộc Vue) có thể thêm 1 test
đơn vị nhỏ nếu dự án có sẵn hạ tầng test frontend — kiểm tra khi viết plan.
