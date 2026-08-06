# Thay emoji-làm-icon bằng lucide-vue-next

## Bối cảnh

Audit giao diện (2026-08-07) so với video demo tham khảo (LAPXPERT) và với chính `DESIGN.md` của dự án phát hiện admin + storefront dùng emoji Unicode làm icon chức năng ở khắp nơi (231 lượt / 33 file `.vue`, cộng 3 hàm icon dùng chung trong `utils/orderStatus.js`). Đây là tín hiệu "thô sơ" rõ nhất: emoji render khác nhau tùy OS/trình duyệt, không khớp tinh thần "Neon Block Arcade" (hình khối, geometric) mà `DESIGN.md` đã định.

Đây là sub-project #1 trong chuỗi polish giao diện (2 việc tiếp theo: token màu dashboard theo Two-Accent Rule, và 2 anti-pattern cục bộ ở WarrantyPanel/AccountPage — mỗi việc sẽ có spec riêng).

## Phạm vi

**Trong phạm vi:** mọi emoji đóng vai trò icon chức năng — icon trong chip số liệu, nút, badge trạng thái, header bảng, nav, timeline đơn hàng, icon phương thức thanh toán — ở cả admin lẫn storefront (toàn bộ 33 file đã audit).

**Ngoài phạm vi:** emoji trang trí bên trong câu chữ đã dịch (vd toast "🎉 Đặt hàng thành công!", copy mô tả trong 5 file `i18n/locales/*.js`). Đó là lựa chọn giọng văn (voice/copy), không phải icon system — không đụng vào các file locale trong sub-project này.

**Quy tắc phân loại khi gặp trường hợp mơ hồ:** nếu emoji đứng một mình làm dấu hiệu thị giác cho một khái niệm (trạng thái, danh mục, hành động) → icon. Nếu emoji nằm giữa câu văn, có thể xoá mà câu vẫn trọn nghĩa → giữ nguyên.

## Thư viện & kỹ thuật

- **Thư viện:** `lucide-vue-next` (thêm vào `dependencies` trong `package.json`). Lý do: component Vue 3 gốc, tree-shakeable (chỉ bundle icon thực dùng), phong cách line-icon hình học hợp với "block-based", không cần thêm build-time tooling.
- **Cách dùng:** import trực tiếp icon cần thiết tại từng file (`import { Laptop, Package } from 'lucide-vue-next'`), dùng thẳng trong template (`<Laptop :size="20" />`). Không tạo wrapper component — lucide đã có `:size`/`color`/`class` đồng nhất, bọc thêm là abstraction thừa.
- **Màu:** mặc định `currentColor` (hành vi gốc của lucide) — icon tự ăn theo màu chữ/token màu ngữ cảnh đã có sẵn (accent theo trạng thái, token theo dark/light theme), không viết lại logic màu.
- **Size:** suy từ `font-size` hiện tại của emoji tại vị trí đó (vd khung `font-size:1.3rem` → `:size="20"`; badge nhỏ `font-size:0.68-0.72rem` → `:size="14"`). Không bịa một scale cứng mới ngoài quy ước này.
- **`utils/orderStatus.js`:** 3 hàm `orderStatusIcon`, `paymentStatusIcon`, `paymentMethodIcon` hiện trả về chuỗi emoji, được nội suy trực tiếp vào template (`{{ fn(s) }}`) ở nhiều nơi. Đổi để trả về icon component thay vì string; mọi nơi gọi đổi từ nội suy chuỗi sang `<component :is="fn(s)" :size="14" />`. Đây là điểm chạm kỹ thuật thật (đổi kiểu trả về của hàm dùng chung), cần rà hết call site khi đổi.

## Thứ tự triển khai (dùng cho kế hoạch chi tiết)

1. `utils/orderStatus.js` + mọi nơi gọi 3 hàm icon — nền tảng, ảnh hưởng cả admin lẫn storefront, làm trước để tránh phải sửa lại chỗ khác 2 lần.
2. Admin: `AdminDashboard`, `AdminPage`, `InventoryPanel`, `OrdersTable`, `PosPanel`, `WarrantyPanel`, `AdminReports`, `AdminSettings`, `DmCategoryTable`, `ProductDetailModal`.
3. Storefront: `AccountPage`, `CustomerPage`, `ProductDetail`, `ProductCard`, `CheckoutModal`, `CartItem`, `NavBar`, `RegisterForm`, `LoginForm`, `OrderStatusTimeline`, `OrderTrackingLog`, `ProductCompareModal`.
4. Common/shared còn lại: `ToastHost`, `EmptyState`, `ErrorBoundary`, `ConfirmDialog`, `BarChart`, `RevenueBarChart`, `NotFoundPage`, `App.vue`, `LuckyWheelPanel`, `StaffPage`, `WarehouseManagementPage`.

## Kiểm tra

Không có logic nghiệp vụ mới (đổi thị giác thuần), nên không cần test case mới. Kiểm tra bằng mắt sau mỗi nhóm file: `npm run dev`, mở panel tương ứng, so icon với emoji cũ (đúng khái niệm, đúng size, đúng màu theo cả dark/light theme), `npm run lint` để bắt import thừa/thiếu.

## Việc không làm trong spec này

- Không đổi emoji trang trí trong 5 file `i18n/locales/*.js`.
- Không tạo wrapper/abstraction cho icon ngoài import trực tiếp.
- Không đụng vào token màu (`theme.css`) hay 2 anti-pattern WarrantyPanel/AccountPage — đó là sub-project #2 và #3 riêng.
