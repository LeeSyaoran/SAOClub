# Thay emoji-làm-icon bằng @lucide/vue Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thay toàn bộ emoji đang đóng vai trò icon chức năng (chip số liệu, nút, badge trạng thái, header bảng, nav, timeline) trong admin + storefront bằng icon component từ `@lucide/vue`, giữ nguyên emoji trang trí trong copy đã dịch.

**Architecture:** Import trực tiếp named icon component từ `@lucide/vue` tại từng file `.vue` cần dùng — không có wrapper component. Icon dùng `currentColor` (mặc định lucide) để ăn theo màu/token hiện có. `utils/orderStatus.js` đổi 3 hàm `orderStatusIcon`/`paymentStatusIcon`/`paymentMethodIcon` từ trả về chuỗi emoji sang trả về icon component, mọi nơi gọi đổi từ `{{ fn(s) }}` sang `<component :is="fn(s)" />`.

**Tech Stack:** Vue 3 (`<script setup>`), `@lucide/vue`, Vite, Vitest.

## Global Constraints

- Chỉ đổi emoji đóng vai trò **icon chức năng**. Emoji trang trí trong chuỗi dịch (`i18n/locales/*.js`, toast copy dạng câu văn) — **không đụng vào**.
- Icon color: mặc định `currentColor`, không set màu cứng trừ khi emoji gốc đã có màu cứng riêng (giữ nguyên màu đó qua prop `color` hoặc CSS `color` trên phần tử cha).
- Icon size: suy từ `font-size` hiện có tại chỗ đó (quy đổi gần đúng rem/px hiện tại → `:size` tương ứng, xem bảng size ở mỗi task).
- Không tạo wrapper component cho icon.
- Không sửa file `i18n/locales/*.js` trong plan này.
- Không cần viết test case mới cho các đổi thị giác thuần (theo spec) — **ngoại lệ**: Task 1 sửa `utils/orderStatus.js` đổi kiểu trả về của hàm, phải cập nhật test hiện có (`__tests__/utils/orderStatus.test.ts`) để không vỡ suite.
- `RevenueBarChart.vue` dòng 29 (`★` bên trong `<text>` SVG) **loại khỏi phạm vi** — đây là annotation dữ liệu trong biểu đồ (không phải icon chrome UI), không thể thay bằng Vue component bên trong SVG `<text>` mà không viết lại cấu trúc chart; giữ nguyên.

---

## Master Icon Glossary

Bảng ánh xạ emoji → icon `@lucide/vue` dùng xuyên suốt plan này. Cùng một khái niệm luôn dùng cùng một icon ở mọi file (nhất quán thị giác — mục tiêu chính của việc đổi này).

| Emoji | Icon | Khái niệm | Emoji | Icon | Khái niệm |
|---|---|---|---|---|---|
| ⏳ | `Clock` | pending/chờ | 💵 | `Banknote` | tiền mặt/giá |
| ✅ | `CheckCircle2` | thành công/xác nhận | 📱 | `Smartphone` | vnpay/điện thoại |
| 📦 | `Package` | đóng gói/tồn kho | 🏦 | `Landmark` | chuyển khoản |
| 🚚 | `Truck` | vận chuyển/ship | 💳 | `CreditCard` | thẻ tín dụng |
| 🛵 | `Bike` | đang giao | 📊 | `BarChart3` | dashboard/số liệu |
| 📬 | `Inbox` | chờ xác nhận/hộp thư | 💻 | `Laptop` | sản phẩm/laptop |
| 🎉 | `PartyPopper` | đã giao/thắng | 🧾 | `Receipt` | đơn hàng/hóa đơn |
| ❌ | `XCircle` | đã hủy (trạng thái) | 👥 | `Users` | khách hàng (số nhiều) |
| ↩️ | `Undo2` | trả hàng/hoàn tiền | 👤🧑 | `User` | khách hàng/tài khoản (số ít) |
| 💰 | `Wallet` | doanh thu/số dư | 📅 | `Calendar` | ngày tháng |
| 🍩 | `PieChart` | biểu đồ donut | 🔥 | `Flame` | bán chạy |
| 🐌 | `Turtle` | bán chậm | 🩺 | `Activity` | KPI health |
| 📈 | `TrendingUp` | xu hướng/báo cáo | 🗃️ | `Archive` | sản phẩm gần đây |
| 🖥️ | `Monitor` | tên sản phẩm (cột) | 🏷️ | `Tag` | thương hiệu/khuyến mãi |
| 🗂️📂 | `FolderOpen` | danh mục/import file | 🔖 | `Bookmark` | trạng thái (cột) |
| ⚠️ | `AlertTriangle` | cảnh báo/sắp hết hàng | 🚫 | `Ban` | hết hàng |
| 🔍🔎 | `Search` | xem chi tiết/tìm kiếm | ✏️ | `Pencil` | sửa |
| 🖨️ | `Printer` | in | 📥 | `Download` | xuất excel |
| ➕ | `Plus` | thêm mới | ✔️✓ | `Check` | duyệt/xác nhận (nút) |
| ✖️✕ | `X` | hủy/đóng/xóa dòng (nút) | 🗑️ | `Trash2` | xóa |
| 🏢 | `Building2` | nhà cung cấp | 📝 | `FileText` | ghi chú |
| 👁️👁 | `Eye` | xem | 🙈 | `EyeOff` | ẩn mật khẩu |
| ℹ️ | `Info` | thông tin | 🔄 | `RefreshCw` | đổi serial/mua lại/trade-in |
| 🛡️ | `Shield` | bảo hành | 🏆 | `Trophy` | khách hàng nổi bật |
| 🔑 | `KeyRound` | đổi mật khẩu | 🏪 | `Store` | thông tin cửa hàng |
| 🖼️ | `Image` | ảnh/logo | 🎨 | `Palette` | giao diện/màu sắc |
| 🔢 | `Hash` | serial | 📷 | `Camera` | upload ảnh |
| ❤️🤍 | `Heart` | yêu thích (đặc/rỗng qua `fill`) | ✨🎡 | `Sparkles` | vòng quay may mắn |
| ← | `ArrowLeft` | quay lại | 🛍️ | `ShoppingBag` | tiếp tục mua sắm |
| 🕘 | `History` | lịch sử trống | 📍 | `MapPin` | địa chỉ/đổi thành phố |
| ✉️ | `Mail` | email | 💾 (nút lưu) | `Save` | lưu hồ sơ |
| 💾 (tab RAM) | `MemoryStick` | RAM | 🎮 | `Gamepad2` | gaming/GPU |
| ⚡ | `Zap` | đồ họa (category) | 🍎 | `Apple` | macbook (category) |
| 🔧 | `Wrench` | linh kiện (category) | 🛒 | `ShoppingCart` | giỏ hàng/POS |
| ☰ | `Menu` | mở sidebar/menu | 🌙 | `Moon` | dark mode |
| ☀️ | `Sun` | light mode | 🔔 | `Bell` | thông báo |
| 📜 | `ScrollText` | lịch sử kho | 🧠 | `Cpu` | CPU |
| 💽 | `HardDrive` | ổ cứng | 🎁 | `Gift` | phần thưởng/điểm |
| 🧑‍💼 | `Briefcase` | nhân viên | ⭐☆ | `Star` | đánh giá (đặc/rỗng qua `fill`) |
| 📤 | `Send` | đã gửi hàng (timeline) | 🔻 | `Triangle` | con trỏ vòng quay |
| 🍀 | `Clover` | trượt vòng quay | 🔑 (đã có ở trên) | | |
| 📵 | `ImageOff` | ảnh QR lỗi | ✓ (badge tin cậy "genuine") | `BadgeCheck` | hàng chính hãng |
| ⚙️ | `Settings` | cài đặt | | | |

**Quy tắc phân biệt ✅/✔️/✓ và ❌/✖️/✕:** khi là **trạng thái** (badge trạng thái đơn/phiếu, tab trạng thái) → `CheckCircle2`/`XCircle` (icon dạng khoanh tròn, tĩnh). Khi là **hành động trên nút** (duyệt, xác nhận, đóng, hủy nhập liệu) → `Check`/`X` (icon trần, phản hồi click). `⏳` khi biểu thị trạng thái đơn/thanh toán → `Clock`; khi biểu thị hành động "đang lưu..." → `Loader2` (Task 13, AccountPage nút lưu hồ sơ — cần thêm class xoay CSS nếu muốn hiệu ứng spin, không bắt buộc).

## Standard Task Recipe

Mọi task dưới đây dùng chung trình tự này (không lặp lại chi tiết ở từng task để giữ DRY):

1. Với mỗi icon mới dùng lần đầu trong file, thêm vào dòng `import { ... } from '@lucide/vue';` trong `<script setup>` (gộp vào import statement hiện có nếu file đã có, tạo mới nếu chưa).
2. Áp dụng từng Edit (old_string → new_string) liệt kê trong task bằng Edit tool.
3. Verify: chạy `npm run dev` (nếu chưa chạy), mở trang/panel liên quan, so icon mới với emoji cũ ở **cả dark và light theme** (toggle theme trên topbar) — đúng khái niệm, size hợp lý, màu ăn theo `currentColor`.
4. `npm run lint` trong `FrontEnd/QLBanMayTinh` — sửa nếu có import thừa/thiếu.
5. Commit: `git add <đúng file(s) của task>` rồi `git commit -m "refactor(icons): thay emoji bằng lucide ở <tên file/nhóm>"`.

---

## Task 0: Cài @lucide/vue

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/package.json`

- [ ] **Bước 1:** Chạy trong `FrontEnd/QLBanMayTinh`:
```bash
npm install @lucide/vue
```
- [ ] **Bước 2:** Xác nhận `"@lucide/vue"` xuất hiện trong `dependencies` của `package.json` và `package-lock.json` được cập nhật.
- [ ] **Bước 3:** Commit:
```bash
git add package.json package-lock.json
git commit -m "chore: thêm @lucide/vue cho icon system"
```

---

## Task 1: `utils/orderStatus.js` + test + 8 call site (nền tảng)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/utils/orderStatus.js`
- Modify: `FrontEnd/QLBanMayTinh/src/__tests__/utils/orderStatus.test.ts`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue` (2 chỗ)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue` (1 chỗ)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue` (5 chỗ)
- Modify: `FrontEnd/QLBanMayTinh/src/components/order/OrderTrackingLog.vue` (1 chỗ)

**Interfaces:**
- Produces: `orderStatusIcon(s)`, `paymentStatusIcon(s)`, `paymentMethodIcon(m)` — trước trả về `string` (emoji), sau đổi trả về **icon component** (import trực tiếp từ `@lucide/vue`, không phải string). Mọi task khác gọi các hàm này qua `<component :is="fn(...)" />`, không còn `{{ fn(...) }}`.

- [ ] **Bước 1: Đổi `orderStatus.js` — 3 hàm icon trả về component thay vì string**

Import ở đầu file (dòng 1, giữ nguyên import `t` hiện có):
```js
import { t } from "../i18n/index.js";
import {
  Clock, CheckCircle2, Package, Truck, Bike, Inbox, PartyPopper, XCircle, Undo2,
  Wallet, Banknote, Smartphone, Landmark, CreditCard, Circle,
} from "@lucide/vue";
```

Thay khối `orderStatusIcon` (dòng 19-31 hiện tại):
```js
// old_string
// Icon theo trạng thái đơn hàng — dùng thay cho chấm tròn chung chung ở badge trạng thái
export const orderStatusIcon = (s) => {
  if (s === 'pending')    return '⏳';
  if (s === 'confirmed')  return '✅';
  if (s === 'processing') return '📦';
  if (s === 'shipping')   return '🚚';
  if (s === 'out_for_delivery') return '🛵';
  if (s === 'awaiting_confirmation') return '📬';
  if (s === 'delivered')  return '🎉';
  if (s === 'cancelled')  return '❌';
  if (s === 'returned')   return '↩️';
  return '●';
};
```
```js
// new_string
// Icon theo trạng thái đơn hàng — dùng thay cho chấm tròn chung chung ở badge trạng thái
export const orderStatusIcon = (s) => {
  if (s === 'pending')    return Clock;
  if (s === 'confirmed')  return CheckCircle2;
  if (s === 'processing') return Package;
  if (s === 'shipping')   return Truck;
  if (s === 'out_for_delivery') return Bike;
  if (s === 'awaiting_confirmation') return Inbox;
  if (s === 'delivered')  return PartyPopper;
  if (s === 'cancelled')  return XCircle;
  if (s === 'returned')   return Undo2;
  return Circle;
};
```

Thay khối `paymentStatusIcon` (dòng 46-52 hiện tại):
```js
// old_string
export const paymentStatusIcon = (s) => {
  if (s === 'unpaid')   return '⏳';
  if (s === 'partial')  return '💰';
  if (s === 'paid')     return '✅';
  if (s === 'refunded') return '↩️';
  return '●';
};
```
```js
// new_string
export const paymentStatusIcon = (s) => {
  if (s === 'unpaid')   return Clock;
  if (s === 'partial')  return Wallet;
  if (s === 'paid')     return CheckCircle2;
  if (s === 'refunded') return Undo2;
  return Circle;
};
```

Thay khối `paymentMethodIcon` (dòng 61-67 hiện tại):
```js
// old_string
export const paymentMethodIcon = (m) => {
  if (m === 'tien_mat')     return '💵';
  if (m === 'vnpay')        return '📱';
  if (m === 'chuyen_khoan') return '🏦';
  if (m === 'the_tin_dung') return '💳';
  return '💰';
};
```
```js
// new_string
export const paymentMethodIcon = (m) => {
  if (m === 'tien_mat')     return Banknote;
  if (m === 'vnpay')        return Smartphone;
  if (m === 'chuyen_khoan') return Landmark;
  if (m === 'the_tin_dung') return CreditCard;
  return Wallet;
};
```

- [ ] **Bước 2: Cập nhật test hiện có để khớp kiểu trả về mới**

File `__tests__/utils/orderStatus.test.ts`, thay khối `describe('orderStatusIcon', ...)` (dòng 42-60):
```ts
// old_string
describe('orderStatusIcon', () => {
  it('should return correct icon for each status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon('pending')).toBe('⏳');
    expect(orderStatusIcon('confirmed')).toBe('✅');
    expect(orderStatusIcon('processing')).toBe('📦');
    expect(orderStatusIcon('shipping')).toBe('🚚');
    expect(orderStatusIcon('out_for_delivery')).toBe('🛵');
    expect(orderStatusIcon('awaiting_confirmation')).toBe('📬');
    expect(orderStatusIcon('delivered')).toBe('🎉');
    expect(orderStatusIcon('cancelled')).toBe('❌');
    expect(orderStatusIcon('returned')).toBe('↩️');
  });

  it('should return default icon for unknown status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon('unknown')).toBe('●');
  });
});
```
```ts
// new_string
describe('orderStatusIcon', () => {
  it('should return a distinct icon component for each status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    const statuses = ['pending', 'confirmed', 'processing', 'shipping', 'out_for_delivery', 'awaiting_confirmation', 'delivered', 'cancelled', 'returned'];
    const icons = statuses.map(orderStatusIcon);
    icons.forEach((icon) => expect(icon).toBeTruthy());
    expect(new Set(icons).size).toBe(statuses.length);
  });

  it('should return default icon component for unknown status', async () => {
    const { orderStatusIcon } = await import('../../utils/orderStatus.js');
    expect(orderStatusIcon('unknown')).toBeTruthy();
  });
});
```

Thay khối `describe('paymentStatusIcon', ...)` (dòng 82-90):
```ts
// old_string
describe('paymentStatusIcon', () => {
  it('should return correct icon for each status', async () => {
    const { paymentStatusIcon } = await import('../../utils/orderStatus.js');
    expect(paymentStatusIcon('unpaid')).toBe('⏳');
    expect(paymentStatusIcon('partial')).toBe('💰');
    expect(paymentStatusIcon('paid')).toBe('✅');
    expect(paymentStatusIcon('refunded')).toBe('↩️');
  });
});
```
```ts
// new_string
describe('paymentStatusIcon', () => {
  it('should return a distinct icon component for each status', async () => {
    const { paymentStatusIcon } = await import('../../utils/orderStatus.js');
    const statuses = ['unpaid', 'partial', 'paid', 'refunded'];
    const icons = statuses.map(paymentStatusIcon);
    icons.forEach((icon) => expect(icon).toBeTruthy());
    expect(new Set(icons).size).toBe(statuses.length);
  });
});
```

Thay khối `describe('paymentMethodIcon', ...)` (dòng 107-120):
```ts
// old_string
describe('paymentMethodIcon', () => {
  it('should return correct icon for each method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon('tien_mat')).toBe('💵');
    expect(paymentMethodIcon('vnpay')).toBe('📱');
    expect(paymentMethodIcon('chuyen_khoan')).toBe('🏦');
    expect(paymentMethodIcon('the_tin_dung')).toBe('💳');
  });

  it('should return default icon for unknown method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon('unknown')).toBe('💰');
  });
});
```
```ts
// new_string
describe('paymentMethodIcon', () => {
  it('should return a distinct icon component for each method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    const methods = ['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung'];
    const icons = methods.map(paymentMethodIcon);
    icons.forEach((icon) => expect(icon).toBeTruthy());
    expect(new Set(icons).size).toBe(methods.length);
  });

  it('should return default icon component for unknown method', async () => {
    const { paymentMethodIcon } = await import('../../utils/orderStatus.js');
    expect(paymentMethodIcon('unknown')).toBeTruthy();
  });
});
```

- [ ] **Bước 3: Chạy test, xác nhận pass**
```bash
npm run test -- orderStatus
```
Expected: tất cả test trong `orderStatus.test.ts` PASS.

- [ ] **Bước 4: Cập nhật 8 call site — đổi `{{ fn(...) }}` sang `<component :is="fn(...)" />`**

`pages/AccountPage.vue` dòng 457:
```html
<!-- old_string -->
                <span style="font-size:11px;">{{ orderStatusIcon(o.trangThaiDonHang) }}</span>
```
```html
<!-- new_string -->
                <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="12" />
```

`pages/AccountPage.vue` dòng 552:
```html
<!-- old_string -->
                  <span style="font-size:11px;">{{ orderStatusIcon(o.trangThaiDonHang) }}</span>
```
```html
<!-- new_string -->
                  <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="12" />
```

`components/admin/PosPanel.vue` dòng 717:
```html
<!-- old_string -->
            <span style="font-size:1.1rem;">{{ paymentMethodIcon(m) }}</span>
```
```html
<!-- new_string -->
            <component :is="paymentMethodIcon(m)" :size="18" />
```

`components/admin/OrdersTable.vue` dòng 719-721 (badge trạng thái đơn):
```html
<!-- old_string -->
              <span class="badge" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                {{ orderStatusIcon(o.trangThaiDonHang) }} {{ orderStatusLabel(o.trangThaiDonHang) }}
              </span>
```
```html
<!-- new_string -->
              <span class="badge d-inline-flex align-items-center gap-1" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                <component :is="orderStatusIcon(o.trangThaiDonHang)" :size="13" /> {{ orderStatusLabel(o.trangThaiDonHang) }}
              </span>
```

`components/admin/OrdersTable.vue` dòng 724-726 (badge trạng thái thanh toán):
```html
<!-- old_string -->
              <span v-if="o.trangThaiThanhToan" class="badge" :style="{ background: paymentStatusColor(o.trangThaiThanhToan).bg, color: paymentStatusColor(o.trangThaiThanhToan).text }">
                {{ paymentStatusIcon(o.trangThaiThanhToan) }} {{ paymentStatusLabel(o.trangThaiThanhToan) }}
              </span>
```
```html
<!-- new_string -->
              <span v-if="o.trangThaiThanhToan" class="badge d-inline-flex align-items-center gap-1" :style="{ background: paymentStatusColor(o.trangThaiThanhToan).bg, color: paymentStatusColor(o.trangThaiThanhToan).text }">
                <component :is="paymentStatusIcon(o.trangThaiThanhToan)" :size="13" /> {{ paymentStatusLabel(o.trangThaiThanhToan) }}
              </span>
```

`components/admin/OrdersTable.vue` dòng 969-971 (modal chi tiết — trạng thái đơn):
```html
<!-- old_string -->
          <span class="badge" :style="{ background: orderStatusColor(orderDetailData.trangThaiDonHang).bg, color: orderStatusColor(orderDetailData.trangThaiDonHang).text }">
            {{ orderStatusIcon(orderDetailData.trangThaiDonHang) }} {{ orderStatusLabel(orderDetailData.trangThaiDonHang) }}
          </span>
```
```html
<!-- new_string -->
          <span class="badge d-inline-flex align-items-center gap-1" :style="{ background: orderStatusColor(orderDetailData.trangThaiDonHang).bg, color: orderStatusColor(orderDetailData.trangThaiDonHang).text }">
            <component :is="orderStatusIcon(orderDetailData.trangThaiDonHang)" :size="13" /> {{ orderStatusLabel(orderDetailData.trangThaiDonHang) }}
          </span>
```

`components/admin/OrdersTable.vue` dòng 975-977 (modal chi tiết — trạng thái thanh toán):
```html
<!-- old_string -->
          <span class="badge" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
            {{ paymentStatusIcon(orderDetailData.trangThaiThanhToan) }} {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
          </span>
```
```html
<!-- new_string -->
          <span class="badge d-inline-flex align-items-center gap-1" :style="{ background: paymentStatusColor(orderDetailData.trangThaiThanhToan).bg, color: paymentStatusColor(orderDetailData.trangThaiThanhToan).text }">
            <component :is="paymentStatusIcon(orderDetailData.trangThaiThanhToan)" :size="13" /> {{ orderDetailData.trangThaiThanhToan ? paymentStatusLabel(orderDetailData.trangThaiThanhToan) : '—' }}
          </span>
```

`components/admin/OrdersTable.vue` dòng 981-985 (danh sách phương thức thanh toán):
```html
<!-- old_string -->
          <span style="color:var(--text-primary);">
            <template v-for="(p, idx) in orderDetailPayments" :key="p.thanhToanId">
              {{ paymentMethodIcon(p.phuongThucThanhToan) }} {{ paymentMethodLabel(p.phuongThucThanhToan) }}<span v-if="idx < orderDetailPayments.length - 1">, </span>
            </template>
          </span>
```
```html
<!-- new_string -->
          <span style="color:var(--text-primary);">
            <template v-for="(p, idx) in orderDetailPayments" :key="p.thanhToanId">
              <component :is="paymentMethodIcon(p.phuongThucThanhToan)" :size="14" style="vertical-align:-2px;" /> {{ paymentMethodLabel(p.phuongThucThanhToan) }}<span v-if="idx < orderDetailPayments.length - 1">, </span>
            </template>
          </span>
```

`components/order/OrderTrackingLog.vue` dòng 13:
```html
<!-- old_string -->
        <span :class="idx === 0 ? 'fw-bold' : ''" :style="idx === 0 ? 'font-size:0.82rem; color:var(--accent-fg);' : 'font-size:0.82rem; color:var(--text-secondary); opacity:0.6;'">{{ orderStatusIcon(entry.trangThaiMoi) }} {{ orderStatusLabel(entry.trangThaiMoi) }}</span>
```
```html
<!-- new_string -->
        <span :class="idx === 0 ? 'fw-bold' : ''" :style="idx === 0 ? 'font-size:0.82rem; color:var(--accent-fg); display:inline-flex; align-items:center; gap:4px;' : 'font-size:0.82rem; color:var(--text-secondary); opacity:0.6; display:inline-flex; align-items:center; gap:4px;'"><component :is="orderStatusIcon(entry.trangThaiMoi)" :size="13" /> {{ orderStatusLabel(entry.trangThaiMoi) }}</span>
```

- [ ] **Bước 5: Verify + lint** — theo Standard Task Recipe bước 3-4. Chú ý kiểm badge trạng thái đơn/thanh toán ở: trang tài khoản khách (lịch sử đơn), POS (chọn phương thức thanh toán), bảng đơn hàng admin, modal chi tiết đơn, log theo dõi đơn hàng.

- [ ] **Bước 6: Commit**
```bash
git add src/utils/orderStatus.js src/__tests__/utils/orderStatus.test.ts \
  src/pages/AccountPage.vue src/components/admin/PosPanel.vue \
  src/components/admin/OrdersTable.vue src/components/order/OrderTrackingLog.vue
git commit -m "refactor(icons): orderStatus.js trả về icon component thay vì emoji"
```

---

## Task 2: `pages/AdminPage.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

- [ ] **Import** (thêm vào `<script setup>`):
```js
import {
  BarChart3, Laptop, Receipt, Users, User, Package, Undo2, Star, Tag, Gift,
  Briefcase, ShoppingCart, TrendingUp, Settings, X, Menu, Moon, Sun, Bell,
  Shield, Hash, Truck, ScrollText, Cpu, MemoryStick, Gamepad2, HardDrive,
} from '@lucide/vue';
```

- [ ] **Edit — `PAGE_META` (dòng 81-94):** đổi giá trị `icon:` từ chuỗi emoji sang tham chiếu component. Vì đây là object dùng trong `computed`/render (không phải trực tiếp trong template), giữ nguyên cấu trúc, chỉ đổi giá trị:
```js
// old_string
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub", icon: "📊" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  "customer-detail": { titleKey: "admin.pageMeta.customerDetail.title", subKey: "admin.pageMeta.customerDetail.sub", icon: "👤" },
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  reviews: { titleKey: "admin.pageMeta.reviews.title", subKey: "admin.pageMeta.reviews.sub", icon: "⭐" },
  promotions: { titleKey: "admin.pageMeta.promotions.title", subKey: "admin.pageMeta.promotions.sub", icon: "🏷️" },
  "doi-thuong": { titleKey: "admin.pageMeta.doiThuong.title", subKey: "admin.pageMeta.doiThuong.sub", icon: "🎁" },
  staff: { titleKey: "admin.pageMeta.staff.title", subKey: "admin.pageMeta.staff.sub", icon: "🧑‍💼" },
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  reports: { titleKey: "admin.pageMeta.reports.title", subKey: "admin.pageMeta.reports.sub", icon: "📈" },
  settings: { titleKey: "admin.pageMeta.settings.title", subKey: "admin.pageMeta.settings.sub", icon: "⚙️" },
```
```js
// new_string
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub", icon: BarChart3 },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: Laptop },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: Receipt },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: Users },
  "customer-detail": { titleKey: "admin.pageMeta.customerDetail.title", subKey: "admin.pageMeta.customerDetail.sub", icon: User },
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: Package },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: Undo2 },
  reviews: { titleKey: "admin.pageMeta.reviews.title", subKey: "admin.pageMeta.reviews.sub", icon: Star },
  promotions: { titleKey: "admin.pageMeta.promotions.title", subKey: "admin.pageMeta.promotions.sub", icon: Tag },
  "doi-thuong": { titleKey: "admin.pageMeta.doiThuong.title", subKey: "admin.pageMeta.doiThuong.sub", icon: Gift },
  staff: { titleKey: "admin.pageMeta.staff.title", subKey: "admin.pageMeta.staff.sub", icon: Briefcase },
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: ShoppingCart },
  reports: { titleKey: "admin.pageMeta.reports.title", subKey: "admin.pageMeta.reports.sub", icon: TrendingUp },
  settings: { titleKey: "admin.pageMeta.settings.title", subKey: "admin.pageMeta.settings.sub", icon: Settings },
```

- [ ] **Edit — fallback icon (dòng 100):**
```js
// old_string
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "📊");
```
```js
// new_string
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? BarChart3);
```

- [ ] **Tìm chỗ render `topbarIcon` trong template** (interpolation `{{ topbarIcon }}` — tương tự dòng 124 ở StaffPage.vue/WarehouseManagementPage.vue, xác nhận vị trí tương ứng trong AdminPage.vue bằng cách grep `topbarIcon` trong file) và đổi từ interpolation sang component:
```html
<!-- old_string (dạng chung, xác nhận đúng dòng thật trước khi edit) -->
{{ topbarIcon }} {{ topbarTitle }}
```
```html
<!-- new_string -->
<component :is="topbarIcon" :size="20" /> {{ topbarTitle }}
```

- [ ] **Edit — sidebar toggle (dòng 1064):**
```html
<!-- old_string -->
                  @click="sidebarOpen = !sidebarOpen">{{ sidebarOpen ? '✕' : '☰' }}</button>
```
```html
<!-- new_string -->
                  @click="sidebarOpen = !sidebarOpen"><component :is="sidebarOpen ? X : Menu" :size="20" /></button>
```

- [ ] **Edit — theme toggle (dòng 1076):**
```html
<!-- old_string -->
            {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
```
```html
<!-- new_string -->
            <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="18" />
```

- [ ] **Edit — notification bell (dòng 1079, HTML entity):**
```html
<!-- old_string -->
               style="width:34px;height:34px;background:var(--bg-hover);cursor:pointer;">&#128276;</div>
```
```html
<!-- new_string -->
               style="width:34px;height:34px;background:var(--bg-hover);cursor:pointer;display:flex;align-items:center;justify-content:center;"><Bell :size="18" /></div>
```

- [ ] **Edit — nav tabs Inventory (dòng 1144, 1147, 1150, 1153, 1156, 1159, 1162, 1165, 1168):**
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='kho'}" @click="inventoryMainTab='kho'">📦 {{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='kho'}" @click="inventoryMainTab='kho'"><Package :size="15" /> {{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'"><Shield :size="15" /> {{ t('admin.inventory.tabWarranty') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='serial'}" @click="inventoryMainTab='serial'">🔢 {{ t('admin.inventory.tabSerial') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='serial'}" @click="inventoryMainTab='serial'"><Hash :size="15" /> {{ t('admin.inventory.tabSerial') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='suppliers'}" @click="inventoryMainTab='suppliers'">🚚 {{ t('admin.sidebar.suppliers') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='suppliers'}" @click="inventoryMainTab='suppliers'"><Truck :size="15" /> {{ t('admin.sidebar.suppliers') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='lich-su'}" @click="inventoryMainTab='lich-su'">📜 {{ t('admin.sidebar.inventoryHistory') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='lich-su'}" @click="inventoryMainTab='lich-su'"><ScrollText :size="15" /> {{ t('admin.sidebar.inventoryHistory') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='cpu'}" @click="inventoryMainTab='cpu'">🧠 {{ t('admin.productsTabs.cpu') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='cpu'}" @click="inventoryMainTab='cpu'"><Cpu :size="15" /> {{ t('admin.productsTabs.cpu') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='ram'}" @click="inventoryMainTab='ram'">💾 {{ t('admin.productsTabs.ram') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='ram'}" @click="inventoryMainTab='ram'"><MemoryStick :size="15" /> {{ t('admin.productsTabs.ram') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='gpu'}" @click="inventoryMainTab='gpu'">🎮 {{ t('admin.productsTabs.gpu') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='gpu'}" @click="inventoryMainTab='gpu'"><Gamepad2 :size="15" /> {{ t('admin.productsTabs.gpu') }}</button>
```
```html
<!-- old_string -->
              <button class="nav-link" :class="{active: inventoryMainTab==='o-cung'}" @click="inventoryMainTab='o-cung'">💽 {{ t('admin.productsTabs.oCung') }}</button>
```
```html
<!-- new_string -->
              <button class="nav-link d-inline-flex align-items-center gap-1" :class="{active: inventoryMainTab==='o-cung'}" @click="inventoryMainTab='o-cung'"><HardDrive :size="15" /> {{ t('admin.productsTabs.oCung') }}</button>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/pages/AdminPage.vue`.

---

## Task 3: `components/admin/AdminDashboard.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/AdminDashboard.vue`

- [ ] **Import:**
```js
import {
  Laptop, Receipt, Users, Wallet, Calendar, AlertTriangle, PieChart, Flame,
  Turtle, Activity, TrendingUp, Archive, Monitor, Tag, FolderOpen, Banknote, Bookmark,
} from '@lucide/vue';
```

- [ ] **Edit — stat chips (dòng 66, 78, 90, 102, 121):**
```html
<!-- old_string -->
                   style="width:44px;height:44px;background:rgba(96,165,250,0.15);font-size:1.3rem;">💻</div>
```
```html
<!-- new_string -->
                   style="width:44px;height:44px;background:rgba(96,165,250,0.15);"><Laptop :size="20" color="#60a5fa" /></div>
```
```html
<!-- old_string -->
                   style="width:44px;height:44px;background:rgba(167,139,250,0.15);font-size:1.3rem;">🧾</div>
```
```html
<!-- new_string -->
                   style="width:44px;height:44px;background:rgba(167,139,250,0.15);"><Receipt :size="20" color="#a78bfa" /></div>
```
```html
<!-- old_string -->
                   style="width:44px;height:44px;background:rgba(52,211,153,0.15);font-size:1.3rem;">👥</div>
```
```html
<!-- new_string -->
                   style="width:44px;height:44px;background:rgba(52,211,153,0.15);"><Users :size="20" color="#34d399" /></div>
```
```html
<!-- old_string -->
                   style="width:44px;height:44px;background:rgba(244,63,94,0.15);font-size:1.3rem;">💰</div>
```
```html
<!-- new_string -->
                   style="width:44px;height:44px;background:rgba(244,63,94,0.15);"><Wallet :size="20" color="var(--accent-fg)" /></div>
```
```html
<!-- old_string -->
                   style="width:44px;height:44px;background:rgba(250,204,21,0.15);font-size:1.3rem;">📅</div>
```
```html
<!-- new_string -->
                   style="width:44px;height:44px;background:rgba(250,204,21,0.15);"><Calendar :size="20" color="#facc15" /></div>
```

**Ghi chú P0 audit:** 5 stat chip trên vẫn giữ 5 màu nền khác nhau như hiện tại ở bước này (đổi icon trước, đúng phạm vi Task này). Việc gom về hệ 2-accent theo "Two-Accent Rule" thuộc **sub-project #2** (token màu dashboard) — không đổi màu nền chip ở đây, chỉ đổi emoji→icon.

- [ ] **Edit — low stock alert (dòng 133):**
```html
<!-- old_string -->
        <span class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
              style="width:22px;height:22px;background:rgba(248,113,113,0.25);font-size:0.85rem;">⚠️</span>
```
```html
<!-- new_string -->
        <span class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
              style="width:22px;height:22px;background:rgba(248,113,113,0.25);"><AlertTriangle :size="13" color="#f87171" /></span>
```

- [ ] **Edit — section headers (dòng 142, 174, 209, 217, 226, 246, 251):**
```html
<!-- old_string -->
                <div class="fw-semibold small text-secondary">🍩 {{ t('admin.dashboard.ordersByStatusChart') }}</div>
```
```html
<!-- new_string -->
                <div class="fw-semibold small text-secondary d-flex align-items-center gap-1"><PieChart :size="14" /> {{ t('admin.dashboard.ordersByStatusChart') }}</div>
```
```html
<!-- old_string -->
                <div class="fw-semibold small text-secondary">📅 {{ t('admin.dashboard.ordersByWeekChart') }}</div>
```
```html
<!-- new_string -->
                <div class="fw-semibold small text-secondary d-flex align-items-center gap-1"><Calendar :size="14" /> {{ t('admin.dashboard.ordersByWeekChart') }}</div>
```
```html
<!-- old_string -->
              <div class="fw-semibold small text-secondary mb-3">🔥 {{ t('admin.dashboard.topSellingChart') }}</div>
```
```html
<!-- new_string -->
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Flame :size="14" /> {{ t('admin.dashboard.topSellingChart') }}</div>
```
```html
<!-- old_string -->
              <div class="fw-semibold small text-secondary mb-3">🐌 {{ t('admin.dashboard.slowSellingChart') }}</div>
```
```html
<!-- new_string -->
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Turtle :size="14" /> {{ t('admin.dashboard.slowSellingChart') }}</div>
```
```html
<!-- old_string -->
          <div class="fw-semibold small text-secondary mb-3">🩺 {{ t('admin.dashboard.kpiHealth') }}</div>
```
```html
<!-- new_string -->
          <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Activity :size="14" /> {{ t('admin.dashboard.kpiHealth') }}</div>
```
```html
<!-- old_string -->
          <div class="fw-semibold small text-secondary mb-3">📈 {{ t('admin.dashboard.revenueTrendChart') }}</div>
```
```html
<!-- new_string -->
          <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><TrendingUp :size="14" /> {{ t('admin.dashboard.revenueTrendChart') }}</div>
```
```html
<!-- old_string -->
      <div class="small fw-semibold text-secondary mb-2">🗃️ {{ t('admin.dashboard.recentProducts') }}</div>
```
```html
<!-- new_string -->
      <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1"><Archive :size="14" /> {{ t('admin.dashboard.recentProducts') }}</div>
```

- [ ] **Edit — table headers (dòng 255):**
```html
<!-- old_string -->
            <th></th><th>🖥️ {{ t('admin.dashboard.colName') }}</th><th>🏷️ {{ t('admin.dashboard.colBrand') }}</th><th>🗂️ {{ t('admin.dashboard.colCategory') }}</th><th>💵 {{ t('admin.dashboard.colPrice') }}</th><th>🔖 {{ t('admin.dashboard.colStatus') }}</th>
```
```html
<!-- new_string -->
            <th></th><th><Monitor :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colName') }}</th><th><Tag :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colBrand') }}</th><th><FolderOpen :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colCategory') }}</th><th><Banknote :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colPrice') }}</th><th><Bookmark :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colStatus') }}</th>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/AdminDashboard.vue`.

---

## Task 4: `components/admin/InventoryPanel.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue`

- [ ] **Import:**
```js
import {
  CheckCircle2, XCircle, Clock, Package, ClipboardList, BarChart3, AlertTriangle,
  Ban, Laptop, Search, Pencil, Printer, Download, Plus, Check, X, Trash2,
  Building2, User, Calendar, FileText, FolderOpen,
} from '@lucide/vue';
```

- [ ] **Edit — `phieuNhapStatusIcon` helper (dòng 273):** hàm này trả về emoji dùng ở nhiều chỗ trong badge; đổi theo cùng pattern Task 1 (trả component, gọi qua `<component :is>`):
```js
// old_string
const phieuNhapStatusIcon = (s) => (s === 'hoan_thanh' ? '✅' : s === 'huy' ? '❌' : '⏳');
```
```js
// new_string
const phieuNhapStatusIcon = (s) => (s === 'hoan_thanh' ? CheckCircle2 : s === 'huy' ? XCircle : Clock);
```
Tìm mọi nơi gọi `phieuNhapStatusIcon(...)` trong template file này (interpolation `{{ }}`) và đổi sang `<component :is="phieuNhapStatusIcon(...)" :size="13" />` — xác nhận số lượng call site bằng grep `phieuNhapStatusIcon(` trong file trước khi sửa.

- [ ] **Edit — nav tabs (dòng 607, 609):**
```html
<!-- old_string -->
              @click="khoTab='ton-kho'">📦 {{ t('admin.inventory.tabStock') }}</button>
```
```html
<!-- new_string -->
              @click="khoTab='ton-kho'"><Package :size="15" style="vertical-align:-2px;" /> {{ t('admin.inventory.tabStock') }}</button>
```
```html
<!-- old_string -->
              @click="khoTab='phieu-nhap'; ensurePhieuNhapData()">📋 {{ t('admin.inventory.tabReceipts') }}</button>
```
```html
<!-- new_string -->
              @click="khoTab='phieu-nhap'; ensurePhieuNhapData()"><ClipboardList :size="15" style="vertical-align:-2px;" /> {{ t('admin.inventory.tabReceipts') }}</button>
```

- [ ] **Edit — stat chips (dòng 618, 630, 642, 654, 770, 794, 806):**
```html
<!-- old_string -->
                 style="width:44px;height:44px;background:rgba(96,165,250,0.15);font-size:1.3rem;">📦</div>
```
```html
<!-- new_string -->
                 style="width:44px;height:44px;background:rgba(96,165,250,0.15);"><Package :size="20" color="#60a5fa" /></div>
```
```html
<!-- old_string -->
                 style="width:44px;height:44px;background:rgba(52,211,153,0.15);font-size:1.3rem;">📊</div>
```
```html
<!-- new_string -->
                 style="width:44px;height:44px;background:rgba(52,211,153,0.15);"><BarChart3 :size="20" color="#34d399" /></div>
```
```html
<!-- old_string -->
                 style="width:44px;height:44px;background:rgba(250,204,21,0.15);font-size:1.3rem;">⚠️</div>
```
```html
<!-- new_string -->
                 style="width:44px;height:44px;background:rgba(250,204,21,0.15);"><AlertTriangle :size="20" color="#facc15" /></div>
```
```html
<!-- old_string -->
                 style="width:44px;height:44px;background:rgba(244,63,94,0.15);font-size:1.3rem;">🚫</div>
```
```html
<!-- new_string (chip tổng out-of-stock, dòng 654) -->
                 style="width:44px;height:44px;background:rgba(244,63,94,0.15);"><Ban :size="20" color="var(--accent-fg)" /></div>
```
```html
<!-- old_string (chip tổng phiếu nhập, dòng 770) -->
                 style="width:44px;height:44px;background:rgba(167,139,250,0.15);font-size:1.3rem;">📋</div>
```
```html
<!-- new_string -->
                 style="width:44px;height:44px;background:rgba(167,139,250,0.15);"><ClipboardList :size="20" color="#a78bfa" /></div>
```
```html
<!-- old_string (chip hoàn thành, dòng 794) -->
                 style="width:44px;height:44px;background:rgba(34,197,94,0.15);font-size:1.3rem;">✅</div>
```
```html
<!-- new_string -->
                 style="width:44px;height:44px;background:rgba(34,197,94,0.15);"><CheckCircle2 :size="20" color="#22c55e" /></div>
```
```html
<!-- old_string (chip huỷ, dòng 806) -->
                 style="width:44px;height:44px;background:rgba(244,63,94,0.15);font-size:1.3rem;">❌</div>
```
```html
<!-- new_string -->
                 style="width:44px;height:44px;background:rgba(244,63,94,0.15);"><XCircle :size="20" color="var(--accent-fg)" /></div>
```

**Chú ý:** dòng 654 và dòng 806 có cùng `old_string` style số (`rgba(244,63,94,0.15)`) nhưng icon con khác nhau (🚫 vs ❌) — dùng thêm 1-2 dòng context phía trên/dưới (đã liệt kê riêng ở trên theo đúng thứ tự xuất hiện) để Edit tool chọn đúng chỗ; không chạy 2 edit này như tìm-thay-tất-cả.

- [ ] **Edit — summary badges out-of-stock/low-stock (dòng 666, 667, 706, 707, 708):**
```html
<!-- old_string -->
      <span v-if="outOfStockItems.length" class="badge" style="background:rgba(244,63,94,0.15);color:#f87171;">🚫 {{ outOfStockItems.length }} {{ t('admin.inventory.outOfStock') }}</span>
```
```html
<!-- new_string -->
      <span v-if="outOfStockItems.length" class="badge d-inline-flex align-items-center gap-1" style="background:rgba(244,63,94,0.15);color:#f87171;"><Ban :size="12" /> {{ outOfStockItems.length }} {{ t('admin.inventory.outOfStock') }}</span>
```
```html
<!-- old_string -->
      <span v-if="lowStockItems.length" class="badge" style="background:rgba(250,204,21,0.15);color:#facc15;">⚠️ {{ lowStockItems.length }} {{ t('admin.inventory.lowStock') }}</span>
```
```html
<!-- new_string -->
      <span v-if="lowStockItems.length" class="badge d-inline-flex align-items-center gap-1" style="background:rgba(250,204,21,0.15);color:#facc15;"><AlertTriangle :size="12" /> {{ lowStockItems.length }} {{ t('admin.inventory.lowStock') }}</span>
```
```html
<!-- old_string -->
            <span v-if="group.outCount" class="badge" style="font-size:0.7rem;background:rgba(244,63,94,0.15);color:#f87171;">🚫 {{ group.outCount }} {{ t('admin.inventory.outOfStock') }}</span>
```
```html
<!-- new_string -->
            <span v-if="group.outCount" class="badge d-inline-flex align-items-center gap-1" style="font-size:0.7rem;background:rgba(244,63,94,0.15);color:#f87171;"><Ban :size="11" /> {{ group.outCount }} {{ t('admin.inventory.outOfStock') }}</span>
```
```html
<!-- old_string -->
            <span v-else-if="group.lowCount" class="badge" style="font-size:0.7rem;background:rgba(250,204,21,0.15);color:#facc15;">⚠️ {{ group.lowCount }} {{ t('admin.inventory.lowStock') }}</span>
```
```html
<!-- new_string -->
            <span v-else-if="group.lowCount" class="badge d-inline-flex align-items-center gap-1" style="font-size:0.7rem;background:rgba(250,204,21,0.15);color:#facc15;"><AlertTriangle :size="11" /> {{ group.lowCount }} {{ t('admin.inventory.lowStock') }}</span>
```
```html
<!-- old_string -->
            <span v-else class="badge" style="font-size:0.7rem;background:rgba(34,197,94,0.15);color:#22c55e;">✅ {{ t('admin.inventory.ok') }}</span>
```
```html
<!-- new_string -->
            <span v-else class="badge d-inline-flex align-items-center gap-1" style="font-size:0.7rem;background:rgba(34,197,94,0.15);color:#22c55e;"><CheckCircle2 :size="11" /> {{ t('admin.inventory.ok') }}</span>
```

- [ ] **Edit — fallback thumbnail (dòng 698):**
```html
<!-- old_string -->
          <div v-else style="width:44px;height:36px;background:var(--bg-input);border-radius:4px;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:1rem;">💻</div>
```
```html
<!-- new_string -->
          <div v-else style="width:44px;height:36px;background:var(--bg-input);border-radius:4px;flex-shrink:0;display:flex;align-items:center;justify-content:center;"><Laptop :size="16" color="var(--text-muted)" /></div>
```

- [ ] **Edit — action buttons (dòng 746, 749, 826, 827, 828, 859, 861, 862, 863, 864, 927, 1049, 1056, 1146):**
```html
<!-- old_string -->
                            @click.stop="openStockDetail(item)">🔍 {{ t('admin.inventory.detail') }}</button>
```
```html
<!-- new_string -->
                            @click.stop="openStockDetail(item)"><Search :size="13" style="vertical-align:-2px;" /> {{ t('admin.inventory.detail') }}</button>
```
```html
<!-- old_string -->
                            @click.stop="openEditStock(item)">✏️ {{ t('admin.inventory.update') }}</button>
```
```html
<!-- new_string -->
                            @click.stop="openEditStock(item)"><Pencil :size="13" style="vertical-align:-2px;" /> {{ t('admin.inventory.update') }}</button>
```
```html
<!-- old_string -->
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapList">🖨️ {{ t('admin.phieuNhap.printPdf') }}</button>
```
```html
<!-- new_string -->
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapList"><Printer :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.printPdf') }}</button>
```
```html
<!-- old_string -->
        <button class="btn btn-sm btn-outline-success" @click="exportPhieuNhapExcel">📥 {{ t('admin.phieuNhap.exportExcel') }}</button>
```
```html
<!-- new_string -->
        <button class="btn btn-sm btn-outline-success" @click="exportPhieuNhapExcel"><Download :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.exportExcel') }}</button>
```
```html
<!-- old_string -->
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPhieuNhap">➕ {{ t('admin.phieuNhap.add') }}</button>
```
```html
<!-- new_string -->
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPhieuNhap"><Plus :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.add') }}</button>
```
```html
<!-- old_string -->
                <button class="btn btn-sm btn-outline-info" style="font-size:0.72rem;padding:2px 8px;" @click="openPhieuNhapDetail(p)">🔍 {{ t('admin.phieuNhap.viewDetail') }}</button>
```
```html
<!-- new_string -->
                <button class="btn btn-sm btn-outline-info" style="font-size:0.72rem;padding:2px 8px;" @click="openPhieuNhapDetail(p)"><Search :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.viewDetail') }}</button>
```
```html
<!-- old_string -->
                  <button class="btn btn-sm btn-outline-success" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'hoan_thanh')">✔️ {{ t('admin.phieuNhap.approve') }}</button>
```
```html
<!-- new_string -->
                  <button class="btn btn-sm btn-outline-success" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'hoan_thanh')"><Check :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.approve') }}</button>
```
```html
<!-- old_string -->
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'huy')">✖️ {{ t('admin.phieuNhap.cancel') }}</button>
```
```html
<!-- new_string -->
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'huy')"><X :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.cancel') }}</button>
```
```html
<!-- old_string -->
                  <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openEditPhieuNhap(p)">✏️ {{ t('admin.phieuNhap.editAction') }}</button>
```
```html
<!-- new_string -->
                  <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openEditPhieuNhap(p)"><Pencil :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.editAction') }}</button>
```
```html
<!-- old_string -->
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="deletePhieuNhap(p.phieuNhapId)">🗑️ {{ t('admin.phieuNhap.deleteAction') }}</button>
```
```html
<!-- new_string -->
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="deletePhieuNhap(p.phieuNhapId)"><Trash2 :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.deleteAction') }}</button>
```
```html
<!-- old_string -->
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;flex:0 0 34px;" :aria-label="t('common.remove')" @click="removePhieuNhapItemRow(idx)">✕</button>
```
```html
<!-- new_string -->
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;flex:0 0 34px;" :aria-label="t('common.remove')" @click="removePhieuNhapItemRow(idx)"><X :size="14" /></button>
```
```html
<!-- old_string -->
                📂 {{ t('admin.stockModal.importFromFile') }}
```
```html
<!-- new_string -->
                <FolderOpen :size="14" style="vertical-align:-2px;" /> {{ t('admin.stockModal.importFromFile') }}
```
```html
<!-- old_string -->
                <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeStockSerialRow(idx)">✕</button>
```
```html
<!-- new_string -->
                <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeStockSerialRow(idx)"><X :size="14" /></button>
```
```html
<!-- old_string -->
                <button v-if="s.trangThai==='trong_kho'" class="btn btn-sm btn-outline-danger" style="padding:1px 7px;font-size:0.72rem;" :title="t('admin.stockDetailModal.deleteSerial')" :aria-label="t('admin.stockDetailModal.deleteSerial')" @click="removeStockSerial(s.chiTietId)">✕</button>
```
```html
<!-- new_string -->
                <button v-if="s.trangThai==='trong_kho'" class="btn btn-sm btn-outline-danger" style="padding:1px 7px;font-size:0.72rem;" :title="t('admin.stockDetailModal.deleteSerial')" :aria-label="t('admin.stockDetailModal.deleteSerial')" @click="removeStockSerial(s.chiTietId)"><X :size="11" /></button>
```

- [ ] **Edit — section header + info chips + detail modal (dòng 905, 951, 967, 970, 973, 978, 1020):**
```html
<!-- old_string -->
        <div class="fw-semibold small text-secondary mb-2">📦 {{ t('admin.phieuNhapModal.itemsLabel') }}</div>
```
```html
<!-- new_string -->
        <div class="fw-semibold small text-secondary mb-2 d-flex align-items-center gap-1"><Package :size="14" /> {{ t('admin.phieuNhapModal.itemsLabel') }}</div>
```
```html
<!-- old_string -->
               style="width:40px;height:40px;background:rgba(167,139,250,0.15);font-size:1.2rem;">📋</div>
```
```html
<!-- new_string -->
               style="width:40px;height:40px;background:rgba(167,139,250,0.15);"><ClipboardList :size="18" color="#a78bfa" /></div>
```
```html
<!-- old_string -->
            🏢 <span class="text-secondary">{{ t('admin.phieuNhap.colSupplier') }}:</span> <span class="text-light fw-semibold">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }}</span>
```
```html
<!-- new_string -->
            <Building2 :size="13" style="vertical-align:-2px;" /> <span class="text-secondary">{{ t('admin.phieuNhap.colSupplier') }}:</span> <span class="text-light fw-semibold">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }}</span>
```
```html
<!-- old_string -->
            👤 <span class="text-secondary">{{ t('admin.phieuNhap.colStaff') }}:</span> <span class="text-light fw-semibold">{{ staffName(phieuNhapDetailData.nhanVienId) }}</span>
```
```html
<!-- new_string -->
            <User :size="13" style="vertical-align:-2px;" /> <span class="text-secondary">{{ t('admin.phieuNhap.colStaff') }}:</span> <span class="text-light fw-semibold">{{ staffName(phieuNhapDetailData.nhanVienId) }}</span>
```
```html
<!-- old_string -->
            📅 <span class="text-secondary">{{ t('admin.phieuNhap.colDate') }}:</span> <span class="text-light fw-semibold">{{ formatDate(phieuNhapDetailData.ngayNhap) }}</span>
```
```html
<!-- new_string -->
            <Calendar :size="13" style="vertical-align:-2px;" /> <span class="text-secondary">{{ t('admin.phieuNhap.colDate') }}:</span> <span class="text-light fw-semibold">{{ formatDate(phieuNhapDetailData.ngayNhap) }}</span>
```
```html
<!-- old_string -->
          <div v-if="phieuNhapDetailData.ghiChu" class="w-100 text-secondary small fst-italic" style="padding-left:2px;">📝 {{ phieuNhapDetailData.ghiChu }}</div>
```
```html
<!-- new_string -->
          <div v-if="phieuNhapDetailData.ghiChu" class="w-100 text-secondary small fst-italic d-flex align-items-center gap-1" style="padding-left:2px;"><FileText :size="12" /> {{ phieuNhapDetailData.ghiChu }}</div>
```
```html
<!-- old_string -->
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapDetail(phieuNhapDetailData)">🖨️ {{ t('admin.phieuNhap.printPdf') }}</button>
```
```html
<!-- new_string -->
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapDetail(phieuNhapDetailData)"><Printer :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.printPdf') }}</button>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/InventoryPanel.vue`.

---

## Task 5: `components/admin/OrdersTable.vue` (phần còn lại, ngoài Task 1)

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue`

- [ ] **Import (gộp với import đã thêm ở Task 1 nếu file này có import khác từ Task 1 — kiểm tra trước khi thêm dòng riêng):**
```js
import { CheckCircle2, Package, Truck, Bike, Inbox, Laptop, User, Eye, Printer } from '@lucide/vue';
```

- [ ] **Edit — `NEXT_ORDER_STATUS_LABEL` map (dòng 423-427):**
```js
// old_string
  pending:          { icon: '✅', key: 'admin.orders.nextConfirm' },
  confirmed:        { icon: '📦', key: 'admin.orders.nextPack' },
  processing:       { icon: '🚚', key: 'admin.orders.nextShip' },
  shipping:         { icon: '🛵', key: 'admin.orders.nextOutForDelivery' },
  out_for_delivery: { icon: '📬', key: 'admin.orders.nextDelivered' },
```
```js
// new_string
  pending:          { icon: CheckCircle2, key: 'admin.orders.nextConfirm' },
  confirmed:        { icon: Package, key: 'admin.orders.nextPack' },
  processing:       { icon: Truck, key: 'admin.orders.nextShip' },
  shipping:         { icon: Bike, key: 'admin.orders.nextOutForDelivery' },
  out_for_delivery: { icon: Inbox, key: 'admin.orders.nextDelivered' },
```
Tìm chỗ render `{{ NEXT_ORDER_STATUS_LABEL[o.trangThaiDonHang].icon }}` trong template (khoảng dòng 739 theo audit trước đó) và đổi sang `<component :is="NEXT_ORDER_STATUS_LABEL[o.trangThaiDonHang].icon" :size="14" />`.

- [ ] **Edit — delivery indicator (dòng 732):**
```html
<!-- old_string -->
                ✅ {{ t('admin.orderStatusModal.actualDeliveryLabel') }}: {{ formatDateTime(o.ngayGiaoThucTe) }}
```
```html
<!-- new_string -->
                <CheckCircle2 :size="13" style="vertical-align:-2px;" /> {{ t('admin.orderStatusModal.actualDeliveryLabel') }}: {{ formatDateTime(o.ngayGiaoThucTe) }}
```

- [ ] **Edit — fallback thumbnails (dòng 774, 923, 1021):**
```html
<!-- old_string -->
              <span v-else style="font-size:4rem;">💻</span>
```
```html
<!-- new_string -->
              <span v-else><Laptop :size="64" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
                  <span v-else style="font-size:1.2rem;flex-shrink:0;">💻</span>
```
```html
<!-- new_string -->
                  <span v-else style="flex-shrink:0;"><Laptop :size="19" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
                  <span v-else style="font-size:1.8rem;">💻</span>
```
```html
<!-- new_string -->
                  <span v-else><Laptop :size="29" color="var(--text-muted)" /></span>
```

- [ ] **Edit — customer prefix + action buttons (dòng 888, 1062, 1079):**
```html
<!-- old_string -->
            👤 {{ customerName(orderDetailData?.khachHangId) }}
```
```html
<!-- new_string -->
            <User :size="14" style="vertical-align:-2px;" /> {{ customerName(orderDetailData?.khachHangId) }}
```
```html
<!-- old_string -->
        <button class="btn btn-sm btn-outline-warning" @click="openHoaDon(orderDetailData)">👁️ {{ t('admin.orderDetailModal.viewInvoice') }}</button>
```
```html
<!-- new_string -->
        <button class="btn btn-sm btn-outline-warning" @click="openHoaDon(orderDetailData)"><Eye :size="14" style="vertical-align:-2px;" /> {{ t('admin.orderDetailModal.viewInvoice') }}</button>
```
```html
<!-- old_string -->
        <button class="btn btn-sm btn-outline-warning" @click="printHoaDon">🖨️ {{ t('admin.orderDetailModal.printInvoice') }}</button>
```
```html
<!-- new_string -->
        <button class="btn btn-sm btn-outline-warning" @click="printHoaDon"><Printer :size="14" style="vertical-align:-2px;" /> {{ t('admin.orderDetailModal.printInvoice') }}</button>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/OrdersTable.vue`.

---

## Task 6: `components/admin/PosPanel.vue` (phần còn lại, ngoài Task 1)

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

- [ ] **Import:**
```js
import { Laptop, ShoppingCart, Receipt, Info, RefreshCw, X, Check } from '@lucide/vue';
```

- [ ] **Edit (dòng 580, 599, 609, 648, 658, 661, 663, 675, 677, 726):**
```html
<!-- old_string -->
              <span v-else style="font-size:1.8rem;">💻</span>
```
```html
<!-- new_string -->
              <span v-else><Laptop :size="29" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
        <span>🛒 {{ t('admin.pos.cart') }} <span class="text-secondary fw-normal small">{{ posCart.length }} {{ t('admin.pos.cartCountSuffix') }}</span></span>
```
```html
<!-- new_string -->
        <span class="d-inline-flex align-items-center gap-1"><ShoppingCart :size="16" /> {{ t('admin.pos.cart') }} <span class="text-secondary fw-normal small">{{ posCart.length }} {{ t('admin.pos.cartCountSuffix') }}</span></span>
```
```html
<!-- old_string -->
          <div style="font-size:2.4rem;">🧾</div>
```
```html
<!-- new_string -->
          <div><Receipt :size="38" color="var(--text-muted)" /></div>
```
```html
<!-- old_string -->
              <span v-else style="font-size:1rem;">💻</span>
```
```html
<!-- new_string -->
              <span v-else><Laptop :size="16" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
                    :aria-label="t('admin.products.detail')" @click="openPosDetail(g)">ℹ️</button>
```
```html
<!-- new_string -->
                    :aria-label="t('admin.products.detail')" @click="openPosDetail(g)"><Info :size="14" /></button>
```
```html
<!-- old_string -->
                      :aria-label="t('admin.pos.swapSerial')" @click="posOpenSerialPicker(g.items[0], g.items[0].chiTietId)">🔄</button>
```
```html
<!-- new_string -->
                      :aria-label="t('admin.pos.swapSerial')" @click="posOpenSerialPicker(g.items[0], g.items[0].chiTietId)"><RefreshCw :size="14" /></button>
```
```html
<!-- old_string -->
                      :aria-label="t('common.remove')" @click="posRemove(g.items[0].chiTietId)">✕</button>
```
```html
<!-- new_string -->
                      :aria-label="t('common.remove')" @click="posRemove(g.items[0].chiTietId)"><X :size="14" /></button>
```
```html
<!-- old_string -->
                      :aria-label="t('admin.pos.swapSerial')" @click="posOpenSerialPicker(item, item.chiTietId)">🔄</button>
```
```html
<!-- new_string -->
                      :aria-label="t('admin.pos.swapSerial')" @click="posOpenSerialPicker(item, item.chiTietId)"><RefreshCw :size="14" /></button>
```
```html
<!-- old_string -->
                      :aria-label="t('common.remove')" @click="posRemove(item.chiTietId)">✕</button>
```
```html
<!-- new_string -->
                      :aria-label="t('common.remove')" @click="posRemove(item.chiTietId)"><X :size="14" /></button>
```
```html
<!-- old_string -->
          <span>✓ {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}</span>
```
```html
<!-- new_string -->
          <span class="d-inline-flex align-items-center gap-1"><Check :size="13" /> {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}</span>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/PosPanel.vue`.

---

## Task 7: `components/admin/WarrantyPanel.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue`

- [ ] **Import:**
```js
import { Calendar, Shield } from '@lucide/vue';
```

- [ ] **Edit (dòng 184, 223):**
```html
<!-- old_string -->
    <span class="badge" style="background:rgba(148,163,184,0.15);color:#94a3b8;font-size:0.72rem;">📅 {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
```
```html
<!-- new_string -->
    <span class="badge d-inline-flex align-items-center gap-1" style="background:rgba(148,163,184,0.15);color:#94a3b8;font-size:0.72rem;"><Calendar :size="11" /> {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
```
```html
<!-- old_string -->
          <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openCreateFromWarranty(w)">🛡️ {{ t('admin.warranty.createClaim') }}</button>
```
```html
<!-- new_string -->
          <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openCreateFromWarranty(w)"><Shield :size="12" style="vertical-align:-2px;" /> {{ t('admin.warranty.createClaim') }}</button>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/WarrantyPanel.vue`.

---

## Task 8: `components/admin/AdminReports.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/AdminReports.vue`

- [ ] **Import:**
```js
import { TrendingUp, PieChart, Flame, Trophy } from '@lucide/vue';
```

- [ ] **Edit (dòng 97, 101, 114, 126):**
```html
<!-- old_string -->
    <div class="small fw-semibold text-secondary mb-2">📈 {{ t('admin.reports.revenueChartTitle') }}</div>
```
```html
<!-- new_string -->
    <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1"><TrendingUp :size="14" /> {{ t('admin.reports.revenueChartTitle') }}</div>
```
```html
<!-- old_string -->
    <div class="small fw-semibold text-secondary mb-2">🍩 {{ t('admin.reports.ordersByStatus') }}</div>
```
```html
<!-- new_string -->
    <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1"><PieChart :size="14" /> {{ t('admin.reports.ordersByStatus') }}</div>
```
```html
<!-- old_string -->
    <div class="small fw-semibold text-secondary mb-2">🔥 {{ t('admin.reports.topProducts') }}</div>
```
```html
<!-- new_string -->
    <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1"><Flame :size="14" /> {{ t('admin.reports.topProducts') }}</div>
```
```html
<!-- old_string -->
    <div class="small fw-semibold text-secondary mb-2 mt-4">🏆 {{ t('admin.reports.customersTitle') }}</div>
```
```html
<!-- new_string -->
    <div class="small fw-semibold text-secondary mb-2 mt-4 d-flex align-items-center gap-1"><Trophy :size="14" /> {{ t('admin.reports.customersTitle') }}</div>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/AdminReports.vue`.

---

## Task 9: `components/admin/AdminSettings.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/AdminSettings.vue`

- [ ] **Import:**
```js
import { KeyRound, Store, Image, Package, Palette, Moon, Sun } from '@lucide/vue';
```

- [ ] **Edit (dòng 42, 67, 71, 110, 125, 129):**
```html
<!-- old_string -->
            <div class="fw-bold mb-3">🔑 {{ t('admin.settings.changePasswordTitle') }}</div>
```
```html
<!-- new_string -->
            <div class="fw-bold mb-3 d-flex align-items-center gap-1"><KeyRound :size="16" /> {{ t('admin.settings.changePasswordTitle') }}</div>
```
```html
<!-- old_string -->
            <div class="fw-bold mb-3">🏪 {{ t('admin.settings.storeInfoTitle') }}</div>
```
```html
<!-- new_string -->
            <div class="fw-bold mb-3 d-flex align-items-center gap-1"><Store :size="16" /> {{ t('admin.settings.storeInfoTitle') }}</div>
```
```html
<!-- old_string -->
                <span v-else style="font-size:1.3rem;">🖼️</span>
```
```html
<!-- new_string -->
                <span v-else><Image :size="20" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
            <div class="fw-bold mb-3">📦 {{ t('admin.settings.lowStockThresholdTitle') }}</div>
```
```html
<!-- new_string -->
            <div class="fw-bold mb-3 d-flex align-items-center gap-1"><Package :size="16" /> {{ t('admin.settings.lowStockThresholdTitle') }}</div>
```
```html
<!-- old_string -->
            <div class="fw-bold mb-3">🎨 {{ t('admin.settings.appearanceTitle') }}</div>
```
```html
<!-- new_string -->
            <div class="fw-bold mb-3 d-flex align-items-center gap-1"><Palette :size="16" /> {{ t('admin.settings.appearanceTitle') }}</div>
```
```html
<!-- old_string -->
                {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
```
```html
<!-- new_string -->
                <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="16" />
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/AdminSettings.vue`.

---

## Task 10: `components/admin/DmCategoryTable.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue`

- [ ] **Import:**
```js
import { Hash, FolderOpen, X } from '@lucide/vue';
```

- [ ] **Edit (dòng 189, 216, 223):**
```html
<!-- old_string -->
              <button class="btn btn-sm btn-outline-info" style="font-size:0.78rem;padding:2px 8px;" @click="openSerials(item)">🔢 {{ t('admin.dmCategory.viewSerials', { count: stockCountOf(item) }) }}</button>
```
```html
<!-- new_string -->
              <button class="btn btn-sm btn-outline-info" style="font-size:0.78rem;padding:2px 8px;" @click="openSerials(item)"><Hash :size="12" style="vertical-align:-2px;" /> {{ t('admin.dmCategory.viewSerials', { count: stockCountOf(item) }) }}</button>
```
```html
<!-- old_string -->
            📂 {{ t('admin.stockModal.importFromFile') }}
```
```html
<!-- new_string -->
            <FolderOpen :size="14" style="vertical-align:-2px;" /> {{ t('admin.stockModal.importFromFile') }}
```
```html
<!-- old_string -->
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeSerialRow(idx)">✕</button>
```
```html
<!-- new_string -->
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeSerialRow(idx)"><X :size="14" /></button>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/admin/DmCategoryTable.vue`.

---

## Task 11: `components/admin/ProductDetailModal.vue` + `components/admin/BienTheTable.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/ProductDetailModal.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/BienTheTable.vue`

- [ ] **`ProductDetailModal.vue` — Import + Edit (dòng 62):**
```js
import { Laptop } from '@lucide/vue';
```
```html
<!-- old_string -->
              <span v-else style="font-size:2rem;width:72px;text-align:center;">💻</span>
```
```html
<!-- new_string -->
              <span v-else style="width:72px;text-align:center;"><Laptop :size="32" color="var(--text-muted)" /></span>
```

- [ ] **`BienTheTable.vue` — Import + Edit (dòng 553, HTML entity):**
```js
import { Camera } from '@lucide/vue';
```
```html
<!-- old_string -->
                    <span style="font-size:1.4rem;">&#128247;</span>
```
```html
<!-- new_string -->
                    <span><Camera :size="22" color="var(--text-muted)" /></span>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/admin/ProductDetailModal.vue src/components/admin/BienTheTable.vue`.

---

## Task 12: `pages/StaffPage.vue` + `pages/WarehouseManagementPage.vue`

Hai file dùng chung một khuôn shell (page-meta icon map, sidebar toggle, theme toggle) — bundle vào 1 task.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue`

- [ ] **`StaffPage.vue` — Import:**
```js
import { ShoppingCart, Receipt, Users, Undo2, Laptop, X, Menu, Moon, Sun } from '@lucide/vue';
```

- [ ] **`StaffPage.vue` — Edit `PAGE_META` (dòng 35-39):**
```js
// old_string
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
```
```js
// new_string
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: ShoppingCart },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: Receipt },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: Users },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: Undo2 },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: Laptop },
```

- [ ] **`StaffPage.vue` — Edit fallback (dòng 43):**
```js
// old_string
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "🛒");
```
```js
// new_string
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? ShoppingCart);
```

- [ ] **`StaffPage.vue` — Edit sidebar toggle (dòng 122):**
```html
<!-- old_string -->
                  @click="sidebarOpen = !sidebarOpen">{{ sidebarOpen ? '✕' : '☰' }}</button>
```
```html
<!-- new_string -->
                  @click="sidebarOpen = !sidebarOpen"><component :is="sidebarOpen ? X : Menu" :size="20" /></button>
```

- [ ] **`StaffPage.vue` — Edit topbar icon render (dòng 124):**
```html
<!-- old_string -->
            <div class="fw-bold" style="font-size:1.05rem;">{{ topbarIcon }} {{ topbarTitle }}</div>
```
```html
<!-- new_string -->
            <div class="fw-bold d-flex align-items-center gap-1" style="font-size:1.05rem;"><component :is="topbarIcon" :size="18" /> {{ topbarTitle }}</div>
```

- [ ] **`StaffPage.vue` — Edit theme toggle (dòng 133):**
```html
<!-- old_string -->
          {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
```
```html
<!-- new_string -->
          <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="18" />
```

- [ ] **`WarehouseManagementPage.vue` — Import:**
```js
import {
  Package, Truck, ScrollText, Undo2, Shield, Hash, Cpu, MemoryStick,
  Gamepad2, HardDrive, X, Menu, Moon, Sun,
} from '@lucide/vue';
```

- [ ] **`WarehouseManagementPage.vue` — Edit `PAGE_META` (dòng 34-43):**
```js
// old_string
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  warrantyClaims: { titleKey: "admin.pageMeta.warrantyClaims.title", subKey: "admin.pageMeta.warrantyClaims.sub", icon: "🛡️" },
  serial: { titleKey: "admin.pageMeta.serial.title", subKey: "admin.pageMeta.serial.sub", icon: "🔢" },
  cpu: { titleKey: "admin.pageMeta.cpu.title", subKey: "admin.pageMeta.cpu.sub", icon: "🧠" },
  ram: { titleKey: "admin.pageMeta.ram.title", subKey: "admin.pageMeta.ram.sub", icon: "💾" },
  gpu: { titleKey: "admin.pageMeta.gpu.title", subKey: "admin.pageMeta.gpu.sub", icon: "🎮" },
  oCung: { titleKey: "admin.pageMeta.oCung.title", subKey: "admin.pageMeta.oCung.sub", icon: "💽" },
```
```js
// new_string
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: Package },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: Truck },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: ScrollText },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: Undo2 },
  warrantyClaims: { titleKey: "admin.pageMeta.warrantyClaims.title", subKey: "admin.pageMeta.warrantyClaims.sub", icon: Shield },
  serial: { titleKey: "admin.pageMeta.serial.title", subKey: "admin.pageMeta.serial.sub", icon: Hash },
  cpu: { titleKey: "admin.pageMeta.cpu.title", subKey: "admin.pageMeta.cpu.sub", icon: Cpu },
  ram: { titleKey: "admin.pageMeta.ram.title", subKey: "admin.pageMeta.ram.sub", icon: MemoryStick },
  gpu: { titleKey: "admin.pageMeta.gpu.title", subKey: "admin.pageMeta.gpu.sub", icon: Gamepad2 },
  oCung: { titleKey: "admin.pageMeta.oCung.title", subKey: "admin.pageMeta.oCung.sub", icon: HardDrive },
```

- [ ] **`WarehouseManagementPage.vue` — Edit fallback (dòng 47):**
```js
// old_string
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "📦");
```
```js
// new_string
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? Package);
```

- [ ] **`WarehouseManagementPage.vue` — Edit sidebar toggle (dòng 126):**
```html
<!-- old_string -->
                  @click="sidebarOpen = !sidebarOpen">{{ sidebarOpen ? '✕' : '☰' }}</button>
```
```html
<!-- new_string -->
                  @click="sidebarOpen = !sidebarOpen"><component :is="sidebarOpen ? X : Menu" :size="20" /></button>
```

- [ ] **`WarehouseManagementPage.vue` — Edit topbar icon render (dòng 128):**
```html
<!-- old_string -->
            <div class="fw-bold" style="font-size:1.05rem;">{{ topbarIcon }} {{ topbarTitle }}</div>
```
```html
<!-- new_string -->
            <div class="fw-bold d-flex align-items-center gap-1" style="font-size:1.05rem;"><component :is="topbarIcon" :size="18" /> {{ topbarTitle }}</div>
```

- [ ] **`WarehouseManagementPage.vue` — Edit theme toggle (dòng 137):**
```html
<!-- old_string -->
          {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
```
```html
<!-- new_string -->
          <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="18" />
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/pages/StaffPage.vue src/pages/WarehouseManagementPage.vue`.

---

## Task 13: `pages/AccountPage.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`

(Task 1 đã xử lý dòng 457 và 552 của file này — không lặp lại ở đây.)

- [ ] **Import:**
```js
import {
  Clock, Truck, CheckCircle2, XCircle, Heart, Sparkles, Settings, ArrowLeft,
  User, Gift, Wallet, Package, ShoppingBag, Receipt, Laptop, History, RefreshCw,
  Undo2, AlertTriangle, Loader2, Smartphone, Mail, MapPin, Save,
} from '@lucide/vue';
```

- [ ] **Edit — tab config (dòng 74-80):**
```js
// old_string
  { id: "pending",   icon: "🕐", label: t("account.tabPending") },
  { id: "shipping",  icon: "🚚", label: t("account.tabShipping") },
  { id: "completed", icon: "✅", label: t("account.tabCompleted") },
  { id: "cancelled", icon: "❌", label: t("account.tabCancelled") },
  { id: "wishlist",  icon: "❤️", label: t("account.tabWishlist") },
  { id: "wheel",     icon: "🎡", label: t("account.tabWheel") },
  { id: "settings",  icon: "⚙️", label: t("account.tabSettings") },
```
```js
// new_string
  { id: "pending",   icon: Clock, label: t("account.tabPending") },
  { id: "shipping",  icon: Truck, label: t("account.tabShipping") },
  { id: "completed", icon: CheckCircle2, label: t("account.tabCompleted") },
  { id: "cancelled", icon: XCircle, label: t("account.tabCancelled") },
  { id: "wishlist",  icon: Heart, label: t("account.tabWishlist") },
  { id: "wheel",     icon: Sparkles, label: t("account.tabWheel") },
  { id: "settings",  icon: Settings, label: t("account.tabSettings") },
```
Tìm chỗ render tab (`{{ tab.icon }}` trong `v-for` render các nút tab) và đổi sang `<component :is="tab.icon" :size="16" />`.

- [ ] **Edit (dòng 369, 382, 389, 394, 431, 435, 449, 468, 469, 478, 499, 523, 538, 545, 560, 566, 585, 620, 623, 637, 650, 656, 670, 717, 730, 738, 744, 752, 758, 759, 764):**
```html
<!-- old_string -->
          ← {{ t('common.backHome') }}
```
```html
<!-- new_string -->
          <ArrowLeft :size="14" style="vertical-align:-2px;" /> {{ t('common.backHome') }}
```
```html
<!-- old_string -->
              <span>👤 {{ t('account.myAccount') }}</span>
```
```html
<!-- new_string -->
              <span class="d-inline-flex align-items-center gap-1"><User :size="14" /> {{ t('account.myAccount') }}</span>
```
```html
<!-- old_string -->
          🎁 {{ t('account.points', { points: profile.diemTichLuy ?? 0 }) }}
```
```html
<!-- new_string -->
          <Gift :size="13" style="vertical-align:-2px;" /> {{ t('account.points', { points: profile.diemTichLuy ?? 0 }) }}
```
```html
<!-- old_string -->
          💰 {{ t('account.walletBalance', { amount: formatPrice(profile.soDuVi ?? 0) }) }}
```
```html
<!-- new_string -->
          <Wallet :size="13" style="vertical-align:-2px;" /> {{ t('account.walletBalance', { amount: formatPrice(profile.soDuVi ?? 0) }) }}
```
```html
<!-- old_string -->
          <div style="font-size:2.6rem; opacity:0.35;">📦</div>
```
```html
<!-- new_string -->
          <div style="opacity:0.35;"><Package :size="42" /></div>
```
```html
<!-- old_string -->
            🛍️ {{ t('common.continueShopping') }}
```
```html
<!-- new_string -->
            <ShoppingBag :size="14" style="vertical-align:-2px;" /> {{ t('common.continueShopping') }}
```
```html
<!-- old_string -->
                <span style="font-size:1.1rem;">🧾</span>
```
```html
<!-- new_string -->
                <span><Receipt :size="18" /></span>
```
```html
<!-- old_string -->
              <span v-if="o.ngayGiaoDuKien">📦 {{ t('account.expectedDelivery') }}: {{ formatDate(o.ngayGiaoDuKien) }}</span>
```
```html
<!-- new_string -->
              <span v-if="o.ngayGiaoDuKien" class="d-inline-flex align-items-center gap-1"><Package :size="13" /> {{ t('account.expectedDelivery') }}: {{ formatDate(o.ngayGiaoDuKien) }}</span>
```
```html
<!-- old_string -->
              <span v-if="o.ngayGiaoThucTe">✅ {{ t('account.actualDelivery') }}: {{ formatDate(o.ngayGiaoThucTe) }}</span>
```
```html
<!-- new_string -->
              <span v-if="o.ngayGiaoThucTe" class="d-inline-flex align-items-center gap-1"><CheckCircle2 :size="13" /> {{ t('account.actualDelivery') }}: {{ formatDate(o.ngayGiaoThucTe) }}</span>
```
```html
<!-- old_string -->
                ✅ {{ t('account.confirmReceived') }}
```
```html
<!-- new_string -->
                <CheckCircle2 :size="14" style="vertical-align:-2px;" /> {{ t('account.confirmReceived') }}
```
```html
<!-- old_string -->
                  <span v-else style="font-size:1.2rem;">💻</span>
```
```html
<!-- new_string -->
                  <span v-else><Laptop :size="19" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
          <div style="font-size:2.6rem; opacity:0.35;">🕘</div>
```
```html
<!-- new_string -->
          <div style="opacity:0.35;"><History :size="42" /></div>
```
```html
<!-- old_string -->
                <span style="font-size:1.3rem; opacity:0.6;">🧾</span>
```
```html
<!-- new_string -->
                <span style="opacity:0.6;"><Receipt :size="21" /></span>
```
```html
<!-- old_string -->
                    ✅ {{ t('account.actualDelivery') }}: {{ formatDate(o.ngayGiaoThucTe) }}
```
```html
<!-- new_string -->
                    <CheckCircle2 :size="13" style="vertical-align:-2px;" /> {{ t('account.actualDelivery') }}: {{ formatDate(o.ngayGiaoThucTe) }}
```
```html
<!-- old_string -->
                  🔁 {{ t('account.buyAgain') }}
```
```html
<!-- new_string -->
                  <RefreshCw :size="14" style="vertical-align:-2px;" /> {{ t('account.buyAgain') }}
```
```html
<!-- old_string -->
                  ↩️ {{ t('account.requestReturn') }}
```
```html
<!-- new_string -->
                  <Undo2 :size="14" style="vertical-align:-2px;" /> {{ t('account.requestReturn') }}
```
```html
<!-- old_string -->
                  <span v-else style="font-size:1rem;">💻</span>
```
```html
<!-- new_string -->
                  <span v-else><Laptop :size="16" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
          <div style="font-size:2.6rem; opacity:0.35;">🤍</div>
```
```html
<!-- new_string -->
          <div style="opacity:0.35;"><Heart :size="42" /></div>
```
```html
<!-- old_string -->
            🛍️ {{ t('wishlist.browse') }}
```
```html
<!-- new_string -->
            <ShoppingBag :size="14" style="vertical-align:-2px;" /> {{ t('wishlist.browse') }}
```
```html
<!-- old_string -->
              <span v-else style="font-size:1.2rem;">💻</span>
```
```html
<!-- new_string (item wishlist — dùng thêm context dòng lân cận nếu Edit báo trùng với dòng 499/585 khi thao tác thật, vì cùng nội dung xuất hiện 3 lần trong file; thêm 2-3 dòng bao quanh lấy từ Read trước khi Edit) -->
              <span v-else><Laptop :size="19" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
              🛒
```
```html
<!-- new_string -->
              <ShoppingCart :size="15" />
```
```html
<!-- old_string -->
              ❤️
```
```html
<!-- new_string -->
              <Heart :size="15" fill="currentColor" />
```
```html
<!-- old_string -->
            <span style="font-size:1.3rem;">🎁</span>
```
```html
<!-- new_string -->
            <span><Gift :size="21" /></span>
```
```html
<!-- old_string -->
            <span style="font-size:1.3rem;">👤</span>
```
```html
<!-- new_string -->
            <span><User :size="21" /></span>
```
```html
<!-- old_string -->
              <label class="form-label small fw-semibold" style="color:var(--text-secondary);">🧑 {{ t('account.settings.fullName') }}</label>
```
```html
<!-- new_string -->
              <label class="form-label small fw-semibold d-flex align-items-center gap-1" style="color:var(--text-secondary);"><User :size="13" /> {{ t('account.settings.fullName') }}</label>
```
```html
<!-- old_string -->
                <label class="form-label small fw-semibold" style="color:var(--text-secondary);">📱 {{ t('account.settings.phone') }}</label>
```
```html
<!-- new_string -->
                <label class="form-label small fw-semibold d-flex align-items-center gap-1" style="color:var(--text-secondary);"><Smartphone :size="13" /> {{ t('account.settings.phone') }}</label>
```
```html
<!-- old_string -->
                <label class="form-label small fw-semibold" style="color:var(--text-secondary);">✉️ {{ t('account.settings.email') }}</label>
```
```html
<!-- new_string -->
                <label class="form-label small fw-semibold d-flex align-items-center gap-1" style="color:var(--text-secondary);"><Mail :size="13" /> {{ t('account.settings.email') }}</label>
```
```html
<!-- old_string -->
              <label class="form-label small fw-semibold" style="color:var(--text-secondary);">📍 {{ t('account.settings.address') }}</label>
```
```html
<!-- new_string -->
              <label class="form-label small fw-semibold d-flex align-items-center gap-1" style="color:var(--text-secondary);"><MapPin :size="13" /> {{ t('account.settings.address') }}</label>
```
```html
<!-- old_string -->
            <div v-if="profileError" class="alert alert-danger small py-2 mb-0 rounded-3">⚠️ {{ profileError }}</div>
```
```html
<!-- new_string -->
            <div v-if="profileError" class="alert alert-danger small py-2 mb-0 rounded-3 d-flex align-items-center gap-1"><AlertTriangle :size="14" /> {{ profileError }}</div>
```
```html
<!-- old_string -->
            <div v-if="profileSuccess" class="alert alert-success small py-2 mb-0 rounded-3">✅ {{ profileSuccess }}</div>
```
```html
<!-- new_string -->
            <div v-if="profileSuccess" class="alert alert-success small py-2 mb-0 rounded-3 d-flex align-items-center gap-1"><CheckCircle2 :size="14" /> {{ profileSuccess }}</div>
```
```html
<!-- old_string -->
                {{ profileSaving ? '⏳ ' + t('common.saving') : '💾 ' + t('common.save') }}</button>
```
```html
<!-- new_string -->
                <span class="d-inline-flex align-items-center gap-1"><component :is="profileSaving ? Loader2 : Save" :size="14" :class="{ 'spin-icon': profileSaving }" /> {{ profileSaving ? t('common.saving') : t('common.save') }}</span></button>
```

- [ ] **Edit — toggle icon wishlist (nếu có, kiểm tra riêng — xem note dưới):** file này KHÔNG có toggle `❤️`/`🤍` kiểu `isWishlisted ? ... : ...` (đó là ở `ProductDetail.vue`/`ProductCard.vue`, Task 15) — bỏ qua ở đây, chỉ có 🤍/❤️ dùng tĩnh (icon-only nút, đã xử lý ở trên).

- [ ] **Thêm CSS xoay cho `Loader2` (nếu chưa có class `.spin-icon` trong `assets/main.css`):**
```css
/* new_string — thêm vào cuối assets/main.css */
.spin-icon { animation: spin-icon-rotate 0.8s linear infinite; }
@keyframes spin-icon-rotate { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) {
  .spin-icon { animation: none; }
}
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/pages/AccountPage.vue src/assets/main.css`.

---

## Task 14: `pages/CustomerPage.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/pages/CustomerPage.vue`

- [ ] **Import:**
```js
import { Laptop, Gamepad2, Zap, Apple, Star, Wrench, Flame, ShoppingCart, X, ShoppingBag } from '@lucide/vue';
```

- [ ] **Edit — category icons (dòng 113, 119, 125, 131, 137, 143):**
```js
// old_string
    icon: "💻",
```
```js
// new_string (danh mục Office — xác nhận đúng khối trước khi edit vì "icon:" một mình không unique; dùng thêm dòng `id`/`key` liền kề của từng khối làm context)
    icon: Laptop,
```
```js
// old_string
    icon: "🎮",
```
```js
// new_string
    icon: Gamepad2,
```
```js
// old_string
    icon: "⚡",
```
```js
// new_string
    icon: Zap,
```
```js
// old_string
    icon: "🍎",
```
```js
// new_string
    icon: Apple,
```
```js
// old_string
    icon: "⭐",
```
```js
// new_string
    icon: Star,
```
```js
// old_string
    icon: "🔧",
```
```js
// new_string
    icon: Wrench,
```

Sidebar nav render tại dòng 411 (`{{ cat.icon }}`) — đổi sang `<component :is="cat.icon" :size="16" />`.

- [ ] **Edit (dòng 361, 797, 824, 832):**
```html
<!-- old_string -->
          🔥 {{ t("home.tickerBadge") }}
```
```html
<!-- new_string -->
          <Flame :size="13" style="vertical-align:-2px;" /> {{ t("home.tickerBadge") }}
```
```html
<!-- old_string -->
              <span style="font-size: 1.1rem">🛒</span>
```
```html
<!-- new_string -->
              <span><ShoppingCart :size="18" /></span>
```
```html
<!-- old_string -->
              ✕
```
```html
<!-- new_string -->
              <X :size="16" />
```
```html
<!-- old_string -->
            <div style="font-size: 3rem; opacity: 0.2">🛍️</div>
```
```html
<!-- new_string -->
            <div style="opacity: 0.2"><ShoppingBag :size="48" /></div>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/pages/CustomerPage.vue`.

---

## Task 15: `components/product/ProductDetail.vue` + `ProductCard.vue` + `ProductCompareModal.vue`

Cả ba đều thuộc "product family", dùng chung khái niệm (fallback ảnh, yêu thích, rating) — bundle vào 1 task.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/product/ProductDetail.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/product/ProductCard.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/product/ProductCompareModal.vue`

- [ ] **`ProductDetail.vue` — Import:**
```js
import { Heart, Laptop, Palette, Shield, Star } from '@lucide/vue';
```

- [ ] **`ProductDetail.vue` — Edit (dòng 23, 46, 125, 126, 227, 254, 264, 283, 298):**
```html
<!-- old_string -->
        {{ isWishlisted ? '❤️' : '🤍' }}
```
```html
<!-- new_string -->
        <Heart :size="18" :fill="isWishlisted ? 'currentColor' : 'none'" />
```
```html
<!-- old_string -->
            <span v-else style="font-size:6rem;">💻</span>
```
```html
<!-- new_string -->
            <span v-else><Laptop :size="96" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
            <span v-if="activeVariant.mauSac">🎨 {{ t('productDetail.color') }} <strong style="color:var(--text-primary);">{{ activeVariant.mauSac }}</strong></span>
```
```html
<!-- new_string -->
            <span v-if="activeVariant.mauSac" class="d-inline-flex align-items-center gap-1"><Palette :size="14" /> {{ t('productDetail.color') }} <strong style="color:var(--text-primary);">{{ activeVariant.mauSac }}</strong></span>
```
```html
<!-- old_string -->
            <span v-if="activeVariant.baoHanhThang">🛡️ {{ t('productDetail.warranty') }} <strong style="color:var(--text-primary);">{{ activeVariant.baoHanhThang }} {{ t('productDetail.months') }}</strong></span>
```
```html
<!-- new_string -->
            <span v-if="activeVariant.baoHanhThang" class="d-inline-flex align-items-center gap-1"><Shield :size="14" /> {{ t('productDetail.warranty') }} <strong style="color:var(--text-primary);">{{ activeVariant.baoHanhThang }} {{ t('productDetail.months') }}</strong></span>
```
```html
<!-- old_string -->
              <span v-else style="font-size:2.5rem;">💻</span>
```
```html
<!-- new_string -->
              <span v-else><Laptop :size="40" color="var(--text-muted)" /></span>
```
```html
<!-- old_string -->
            · ⭐ {{ avgRating.toFixed(1) }} ({{ reviews.length }})
```
```html
<!-- new_string -->
            · <Star :size="13" fill="currentColor" style="vertical-align:-2px;" /> {{ avgRating.toFixed(1) }} ({{ reviews.length }})
```
```html
<!-- old_string -->
            >{{ n <= newSoSao ? '⭐' : '☆' }}</button>
```
```html
<!-- new_string -->
            ><Star :size="20" :fill="n <= newSoSao ? 'currentColor' : 'none'" /></button>
```
```html
<!-- old_string -->
              {{ t('review.yourReview') }} · {{ '⭐'.repeat(myReview.soSao) }}
```
```html
<!-- new_string -->
              {{ t('review.yourReview') }} · <span class="d-inline-flex" style="gap:1px;"><Star v-for="n in myReview.soSao" :key="n" :size="13" fill="currentColor" /></span>
```
```html
<!-- old_string -->
              <span style="font-size:12px;">{{ '⭐'.repeat(r.soSao) }}</span>
```
```html
<!-- new_string -->
              <span class="d-inline-flex" style="gap:1px;"><Star v-for="n in r.soSao" :key="n" :size="12" fill="currentColor" /></span>
```

- [ ] **`ProductCard.vue` — Import + Edit (dòng 19, 35, 51):**
```js
import { Laptop, Heart, Star } from '@lucide/vue';
```
```html
<!-- old_string -->
        💻
```
```html
<!-- new_string -->
        <Laptop :size="40" color="var(--text-muted)" />
```
```html
<!-- old_string -->
        {{ isWishlisted ? '❤️' : '🤍' }}
```
```html
<!-- new_string -->
        <Heart :size="16" :fill="isWishlisted ? 'currentColor' : 'none'" />
```
```html
<!-- old_string -->
        ⭐ {{ rating.diemTrungBinh.toFixed(1) }} ({{ rating.tongSoDanhGia }})
```
```html
<!-- new_string -->
        <Star :size="12" fill="currentColor" style="vertical-align:-2px;" /> {{ rating.diemTrungBinh.toFixed(1) }} ({{ rating.tongSoDanhGia }})
```

- [ ] **`ProductCompareModal.vue` — Import + Edit (dòng 16):**
```js
import { Laptop } from '@lucide/vue';
```
```html
<!-- old_string -->
                  <span v-else style="font-size:1.5rem;">💻</span>
```
```html
<!-- new_string -->
                  <span v-else><Laptop :size="24" color="var(--text-muted)" /></span>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/product/ProductDetail.vue src/components/product/ProductCard.vue src/components/product/ProductCompareModal.vue`.

---

## Task 16: `components/checkout/CheckoutModal.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/checkout/CheckoutModal.vue`

- [ ] **Import:**
```js
import { CheckCircle2, Laptop, Banknote, Smartphone, Landmark, ImageOff } from '@lucide/vue';
```

- [ ] **Edit (dòng 14, 82, 214, 232, 250, 273):**
```html
<!-- old_string -->
               style="width:72px;height:72px;background:rgba(72,199,142,0.15);color:#48c78e;font-size:2rem;">✓</div>
```
```html
<!-- new_string -->
               style="width:72px;height:72px;background:rgba(72,199,142,0.15);color:#48c78e;"><CheckCircle2 :size="32" /></div>
```
```html
<!-- old_string -->
                  <div v-else class="d-flex align-items-center justify-content-center rounded-2" style="width:36px;height:36px;background:var(--bg-card-inset);font-size:1rem;">💻</div>
```
```html
<!-- new_string -->
                  <div v-else class="d-flex align-items-center justify-content-center rounded-2" style="width:36px;height:36px;background:var(--bg-card-inset);"><Laptop :size="16" color="var(--text-muted)" /></div>
```
```html
<!-- old_string -->
                     style="width:42px;height:42px;background:#2a2000;font-size:1.3rem;">💵</div>
```
```html
<!-- new_string -->
                     style="width:42px;height:42px;background:#2a2000;"><Banknote :size="20" color="#facc15" /></div>
```
```html
<!-- old_string -->
                     style="width:42px;height:42px;background:#0a1a2a;font-size:1.3rem;">📱</div>
```
```html
<!-- new_string -->
                     style="width:42px;height:42px;background:#0a1a2a;"><Smartphone :size="20" color="#60a5fa" /></div>
```
```html
<!-- old_string -->
                     style="width:42px;height:42px;background:#0a1a0a;font-size:1.3rem;">🏦</div>
```
```html
<!-- new_string -->
                     style="width:42px;height:42px;background:#0a1a0a;"><Landmark :size="20" color="#34d399" /></div>
```
```html
<!-- old_string -->
              <span style="font-size:1.8rem;">📵</span>{{ t('checkout.qrImageFailed') }}
```
```html
<!-- new_string -->
              <ImageOff :size="29" />{{ t('checkout.qrImageFailed') }}
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/checkout/CheckoutModal.vue`.

---

## Task 17: `components/cart/CartItem.vue` + `components/layout/NavBar.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/cart/CartItem.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/layout/NavBar.vue`

- [ ] **`CartItem.vue` — Import + Edit (dòng 9):**
```js
import { Laptop } from '@lucide/vue';
```
```html
<!-- old_string -->
           style="width:64px;height:64px;background:var(--bg-card-alt);font-size:1.6rem;">💻</div>
```
```html
<!-- new_string -->
           style="width:64px;height:64px;background:var(--bg-card-alt);"><Laptop :size="26" color="var(--text-muted)" /></div>
```

- [ ] **`NavBar.vue` — Import:**
```js
import { BadgeCheck, Truck, RefreshCw, Moon, Sun, Menu, MapPin, Search, ShoppingCart } from '@lucide/vue';
```

- [ ] **`NavBar.vue` — Edit (dòng 10, 11, 12, 20, 60, 125, 147, 158):**
```html
<!-- old_string -->
        <span class="d-none d-lg-inline">✓ {{ t('nav.genuine') }}</span>
```
```html
<!-- new_string -->
        <span class="d-none d-lg-inline d-inline-flex align-items-center gap-1"><BadgeCheck :size="13" /> {{ t('nav.genuine') }}</span>
```
```html
<!-- old_string -->
        <span class="d-none d-xl-inline">🚚 {{ t('nav.freeShip') }}</span>
```
```html
<!-- new_string -->
        <span class="d-none d-xl-inline d-inline-flex align-items-center gap-1"><Truck :size="13" /> {{ t('nav.freeShip') }}</span>
```
```html
<!-- old_string -->
        <span class="d-none d-xl-inline">🔄 {{ t('nav.tradeIn') }}</span>
```
```html
<!-- new_string -->
        <span class="d-none d-xl-inline d-inline-flex align-items-center gap-1"><RefreshCw :size="13" /> {{ t('nav.tradeIn') }}</span>
```
```html
<!-- old_string -->
          {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
```
```html
<!-- new_string -->
          <component :is="ThemeStore.mode === 'dark' ? Moon : Sun" :size="18" />
```
```html
<!-- old_string -->
          ☰ <span class="d-none d-sm-inline">{{ t('nav.categories') }}</span>
```
```html
<!-- new_string -->
          <Menu :size="16" style="vertical-align:-3px;" /> <span class="d-none d-sm-inline">{{ t('nav.categories') }}</span>
```
```html
<!-- old_string -->
        📍
```
```html
<!-- new_string -->
        <MapPin :size="15" />
```
```html
<!-- old_string -->
          🔎
```
```html
<!-- new_string -->
          <Search :size="15" />
```
```html
<!-- old_string -->
          🛒 <span class="d-none d-sm-inline">{{ t('nav.cart') }}</span>
```
```html
<!-- new_string -->
          <ShoppingCart :size="16" style="vertical-align:-3px;" /> <span class="d-none d-sm-inline">{{ t('nav.cart') }}</span>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/cart/CartItem.vue src/components/layout/NavBar.vue`.

---

## Task 18: `components/auth/RegisterForm.vue` + `components/auth/LoginForm.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/auth/RegisterForm.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/auth/LoginForm.vue`

- [ ] **`RegisterForm.vue` — Import + Edit (dòng 65, 83):**
```js
import { Eye, EyeOff } from '@lucide/vue';
```
```html
<!-- old_string -->
                  {{ showPassword ? '🙈' : '👁' }}
```
```html
<!-- new_string -->
                  <component :is="showPassword ? EyeOff : Eye" :size="16" />
```
```html
<!-- old_string -->
                  {{ showConfirm ? '🙈' : '👁' }}
```
```html
<!-- new_string -->
                  <component :is="showConfirm ? EyeOff : Eye" :size="16" />
```

- [ ] **`LoginForm.vue` — Import + Edit (dòng 34):**
```js
import { Eye, EyeOff } from '@lucide/vue';
```
```html
<!-- old_string -->
              {{ showPassword ? '🙈' : '👁' }}
```
```html
<!-- new_string -->
              <component :is="showPassword ? EyeOff : Eye" :size="16" />
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/auth/RegisterForm.vue src/components/auth/LoginForm.vue`.

---

## Task 19: `components/order/OrderStatusTimeline.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/order/OrderStatusTimeline.vue`

- [ ] **Import:**
```js
import { Check, Send, Bike, PartyPopper, FileText, CheckCircle2, Package } from '@lucide/vue';
```

- [ ] **Edit (dòng 20, 46 — checkmark badge, giống nhau ở desktop/mobile):**
```html
<!-- old_string -->
                    style="width:15px; height:15px; bottom:-2px; right:-2px; background:var(--accent); color:var(--accent-text); font-size:9px; opacity:0.55; border:1px solid var(--bg-hover);">✓</span>
```
```html
<!-- new_string (áp dụng cho cả 2 vị trí — dòng 20 desktop, dòng 46 mobile; thêm 2-3 dòng context phía trên để phân biệt khi Edit) -->
                    style="width:15px; height:15px; bottom:-2px; right:-2px; background:var(--accent); color:var(--accent-text); opacity:0.55; border:1px solid var(--bg-hover); display:flex; align-items:center; justify-content:center;"><Check :size="9" /></span>
```

- [ ] **Edit — step icon config (dòng 82-88):**
```js
// old_string
  { title: t('orderStatus.timeline.shippingTitle'),        desc: t('orderStatus.timeline.shippingDesc'),        icon: '📤' },
  { title: t('orderStatus.timeline.outForDeliveryTitle'),  desc: t('orderStatus.timeline.outForDeliveryDesc'),  icon: '🛵' },
  { title: t('orderStatus.timeline.deliveredTitle'),       desc: t('orderStatus.timeline.deliveredDesc'),       icon: '🎉' },
```
```js
// new_string
  { title: t('orderStatus.timeline.shippingTitle'),        desc: t('orderStatus.timeline.shippingDesc'),        icon: Send },
  { title: t('orderStatus.timeline.outForDeliveryTitle'),  desc: t('orderStatus.timeline.outForDeliveryDesc'),  icon: Bike },
  { title: t('orderStatus.timeline.deliveredTitle'),       desc: t('orderStatus.timeline.deliveredDesc'),       icon: PartyPopper },
```
```js
// old_string
  { title: t('orderStatus.timeline.placedTitle'),    desc: t('orderStatus.timeline.placedDesc'),    icon: '📝' },
  { title: t('orderStatus.timeline.confirmedTitle'), desc: t('orderStatus.timeline.confirmedDesc'), icon: '✅' },
  { title: t('orderStatus.timeline.packingTitle'),   desc: t('orderStatus.timeline.packingDesc'),   icon: '📦' },
```
```js
// new_string
  { title: t('orderStatus.timeline.placedTitle'),    desc: t('orderStatus.timeline.placedDesc'),    icon: FileText },
  { title: t('orderStatus.timeline.confirmedTitle'), desc: t('orderStatus.timeline.confirmedDesc'), icon: CheckCircle2 },
  { title: t('orderStatus.timeline.packingTitle'),   desc: t('orderStatus.timeline.packingDesc'),   icon: Package },
```

- [ ] **Edit — render step icon (dòng 17 desktop, dòng 43 mobile — `{{ step.icon }}`):**
```html
<!-- old_string -->
{{ step.icon }}
```
```html
<!-- new_string (áp dụng ở cả 2 vị trí render, xác nhận đúng dòng bằng context xung quanh trước khi edit) -->
<component :is="step.icon" :size="18" />
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/order/OrderStatusTimeline.vue`.

---

## Task 20: `components/account/LuckyWheelPanel.vue`

**Files:** Modify: `FrontEnd/QLBanMayTinh/src/components/account/LuckyWheelPanel.vue`

- [ ] **Import:**
```js
import { Triangle, PartyPopper, Clover } from '@lucide/vue';
```
Nếu `Clover` không tồn tại trong phiên bản `@lucide/vue` đã cài (kiểm tra bằng cách chạy `npm run dev` và xem lỗi import), thay bằng `Frown` — cùng vai trò "kết quả trượt", cập nhật cả import lẫn dòng dùng bên dưới.

- [ ] **Edit (dòng 120, 153, 166):**
```html
<!-- old_string -->
        <div class="position-absolute top-0 start-50 translate-middle-x" style="z-index:2; font-size:28px; margin-top:-14px;">🔻</div>
```
```html
<!-- new_string -->
        <div class="position-absolute top-0 start-50 translate-middle-x" style="z-index:2; margin-top:-14px;"><Triangle :size="22" style="transform:rotate(180deg);" fill="currentColor" /></div>
```
```html
<!-- old_string -->
          <div style="font-size:2.4rem;">🎉</div>
```
```html
<!-- new_string -->
          <div><PartyPopper :size="38" /></div>
```
```html
<!-- old_string -->
          <div style="font-size:2.4rem;">🍀</div>
```
```html
<!-- new_string -->
          <div><Clover :size="38" /></div>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. File: `git add src/components/account/LuckyWheelPanel.vue`.

---

## Task 21: `components/common/ToastHost.vue` + toast block trong `App.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/ToastHost.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue`

- [ ] **`ToastHost.vue` — Import + Edit (dòng 10):**
```js
import { CheckCircle2, XCircle } from '@lucide/vue';
```
```html
<!-- old_string -->
      <span style="font-size:1.1rem; flex-shrink:0;">{{ ToastState.type === 'success' ? '✓' : '✕' }}</span>
```
```html
<!-- new_string -->
      <span style="flex-shrink:0;"><component :is="ToastState.type === 'success' ? CheckCircle2 : XCircle" :size="18" /></span>
```

- [ ] **`App.vue` — Import + Edit (dòng 385-389):**
```js
import { CheckCircle2, XCircle, Info } from '@lucide/vue';
```
```html
<!-- old_string -->
        <span style="font-size: 1.1rem; flex-shrink: 0">
          {{
            toast.type === "success" ? "✓" : toast.type === "error" ? "✕" : "ℹ"
          }}
        </span>
```
```html
<!-- new_string -->
        <span style="flex-shrink: 0">
          <component
            :is="toast.type === 'success' ? CheckCircle2 : toast.type === 'error' ? XCircle : Info"
            :size="18"
          />
        </span>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/common/ToastHost.vue src/App.vue`.

---

## Task 22: `components/common/EmptyState.vue` + `ErrorBoundary.vue` + `ConfirmDialog.vue`

Ba file nhỏ, cùng vai trò "icon cảnh báo/trạng thái trống" — bundle vào 1 task.

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/EmptyState.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/ErrorBoundary.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/ConfirmDialog.vue`

- [ ] **`EmptyState.vue` — Import + Edit (dòng 4):**
```js
import { Inbox } from '@lucide/vue';
```
```js
// old_string
  icon: { type: String, default: "📭" },
```
```js
// new_string
  icon: { type: Object, default: () => Inbox },
```
Dòng 15 (`{{ icon }}`) đổi sang `<component :is="icon" :size="40" />`. **Lưu ý:** đây là breaking change cho prop `icon` — grep toàn bộ `<EmptyState` trong codebase để tìm nơi truyền `icon="..."` bằng chuỗi emoji tùy biến (nếu có, đổi caller sang truyền icon component thay vì string) trước khi coi task này hoàn tất.

- [ ] **`ErrorBoundary.vue` — Import + Edit (dòng 14):**
```js
import { AlertTriangle } from '@lucide/vue';
```
```html
<!-- old_string -->
    <div style="font-size: 2.5rem">⚠️</div>
```
```html
<!-- new_string -->
    <div><AlertTriangle :size="40" /></div>
```

- [ ] **`ConfirmDialog.vue` — Import + Edit (dòng 5):**
```js
import { AlertTriangle } from '@lucide/vue';
```
```html
<!-- old_string -->
           style="width:56px;height:56px;background:rgba(239,68,68,0.12);font-size:1.5rem;">⚠️</div>
```
```html
<!-- new_string -->
           style="width:56px;height:56px;background:rgba(239,68,68,0.12);"><AlertTriangle :size="24" color="#ef4444" /></div>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/common/EmptyState.vue src/components/common/ErrorBoundary.vue src/components/common/ConfirmDialog.vue`.

---

## Task 23: `components/common/BarChart.vue` + `pages/NotFoundPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/BarChart.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/NotFoundPage.vue`

- [ ] **`BarChart.vue` — Import + Edit (dòng 7):**
```js
import { Laptop } from '@lucide/vue';
```
```html
<!-- old_string -->
        <span v-else style="font-size:0.85rem;">💻</span>
```
```html
<!-- new_string -->
        <span v-else><Laptop :size="14" color="var(--text-muted)" /></span>
```

- [ ] **`NotFoundPage.vue` — Import + Edit (dòng 11, 14):**
```js
import { Search, ArrowLeft } from '@lucide/vue';
```
```html
<!-- old_string -->
      <div style="font-size:4rem">🔍</div>
```
```html
<!-- new_string -->
      <div><Search :size="64" color="var(--text-muted)" /></div>
```
```html
<!-- old_string -->
      <router-link to="/" class="btn btn-warning fw-bold rounded-pill px-4 py-2">← Về trang chủ</router-link>
```
```html
<!-- new_string -->
      <router-link to="/" class="btn btn-warning fw-bold rounded-pill px-4 py-2 d-inline-flex align-items-center gap-1"><ArrowLeft :size="15" /> Về trang chủ</router-link>
```

- [ ] **Verify + lint + commit** theo Standard Task Recipe. Files: `git add src/components/common/BarChart.vue src/pages/NotFoundPage.vue`.

---

## Task 24: Toàn bộ suite + kiểm tra cuối

**Files:** không sửa file mới, chỉ verify tổng thể.

- [ ] **Bước 1:** Chạy toàn bộ test suite:
```bash
npm run test
```
Expected: PASS toàn bộ (không chỉ `orderStatus`).

- [ ] **Bước 2:** Build production để bắt lỗi import/type còn sót:
```bash
npm run build
```
Expected: build thành công, không cảnh báo icon import thiếu.

- [ ] **Bước 3:** `npm run dev`, đi qua từng trang đã sửa (Admin: Dashboard/Products/Orders/Inventory/POS/Warranty/Reports/Settings/DmCategory; Storefront: Account/Customer/ProductDetail/Cart/Checkout/NavBar/Login/Register) ở **cả dark và light theme**, xác nhận không còn emoji sót lại đóng vai trò icon chức năng (grep lại toàn `src` bằng pattern Unicode emoji đã dùng lúc audit để đối chiếu số lượng — chỉ còn lại emoji trang trí trong `i18n/locales/*.js`, RevenueBarChart.vue dòng 29, và các trường hợp đã ghi chú loại trừ).
- [ ] **Bước 4:** Không commit riêng — đây là bước kiểm tra tổng, nếu phát hiện sai sót quay lại đúng task tương ứng để sửa và commit ở task đó.
