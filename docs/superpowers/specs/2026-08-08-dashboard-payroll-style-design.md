# Restyle Admin Dashboard theo mẫu "Joint Payroll" — Design Spec

## Bối cảnh

Người dùng cung cấp ảnh mẫu 1 dashboard admin dạng payroll (theme tím-hồng gradient, có
bản dark và bản light) và muốn `AdminDashboard.vue` (tab Dashboard trong `AdminPage.vue`)
đổi sang giống bố cục + style ảnh mẫu nhất có thể.

Ảnh mẫu là dashboard payroll (Positions theo Masters/Office/Salesmans, KPI radar theo
nhân viên, Weekly Payroll Budget, lịch Heat Map) — không khớp trực tiếp với dữ liệu shop
máy tính hiện có (đơn hàng, tồn kho, doanh thu, sản phẩm). Đã thống nhất với người dùng:
sao chép sát bố cục 9 khối của ảnh, map từng khối sang số liệu gần nghĩa nhất trong hệ
thống hiện tại thay vì bịa dữ liệu không có thật.

## Cách tiếp cận

Dự án hiện có 4 chart component tự viết bằng SVG thuần trong `components/common/`
(`DonutChart`, `BarChart`, `GaugeChart`, `TrendChart`) — không dùng thư viện chart ngoài
nào (`package.json` không có Chart.js/ApexCharts/...). Tiếp tục đúng convention này: viết
thêm SVG component thuần theo đúng khuôn của các component hiện có (cùng cách tính toán
path/dash bằng `computed`, cùng cách nhận `data` qua props, cùng dùng CSS variable cho
màu) — không thêm dependency mới.

## 1. Theme — thêm gradient token, không đổi token cũ

`src/assets/theme.css`: thêm 1 token mới vào cả 2 block `[data-theme="dark"]` và
`[data-theme="light"]`:

```css
--gradient-brand: linear-gradient(135deg, var(--accent-2), var(--accent));
```

Token này CHỈ dùng cho 3 chỗ: pill nav-item active ở sidebar, badge logo, và
stroke/fill của các chart mới thêm ở Dashboard. Không đổi cách dùng `--accent`/
`--accent-2` ở nơi khác (Products, POS, Orders...) — các trang đó không bị ảnh hưởng.

## 2. Component SVG mới trong `components/common/`

Tất cả đều pure-props (không gọi API/store bên trong), theo đúng convention: `<script
setup>` + `computed` tính toán hình học SVG, không có state nội bộ ngoài props.

### `RingProgress.vue`
Vòng tròn tiến độ 1 giá trị (0-100), không có legend — khác `DonutChart.vue` (nhiều
segment + legend). Cấu trúc giống nửa đầu `DonutChart.vue`: 1 `<circle>` nền
(`var(--bg-hover)`) + 1 `<circle>` giá trị dùng `stroke-dasharray`/`stroke-dashoffset`,
nhưng stroke dùng `<linearGradient>` id riêng thay vì màu đặc. Props: `value` (0-100),
`label`, `color1`/`color2` (2 đầu gradient, mặc định lấy từ `--accent-2`/`--accent`),
`size`, `thickness`. Text `{{ value }}%` giữa tâm.

### `DotMatrix.vue`
Lưới chấm tỉ lệ theo danh mục, dùng cho khối "Positions". Props: `data: [{ label, value,
color }]`. Tổng cố định `TOTAL_DOTS = 50` (5 hàng × 10 cột), số dot của mỗi category =
`round(value / total * 50)`, các dot được lấp lần lượt theo thứ tự category trong `data`
(không phải 1 hàng riêng/category như ảnh gốc — đơn giản hoá vì shop chỉ có 2-4 chức vụ
thực tế, không cần nhiều hàng phân biệt). Mỗi dot là 1 `<circle>` nhỏ trong `<svg>`, tô
theo màu category tương ứng. Kèm danh sách nhãn + % bên cạnh (giống format legend của
`DonutChart.vue`).

### `RadarChart.vue`
Radar nhiều trục, 1 series duy nhất (không so sánh 2 kỳ — xem phần "Không làm" bên dưới).
Props: `data: [{ axis, value }]` (value 0-100), `size`, `color`. Tính toạ độ mỗi đỉnh bằng
góc `(2π / n) * i - π/2` và bán kính `= (value/100) * maxRadius`, vẽ:
- N đường lưới nan hoa (nền, `var(--border-color-soft)`) từ tâm ra mép.
- 1 `<polygon>` nối các đỉnh giá trị, `fill` màu gradient mờ (`fill-opacity: 0.25`),
  `stroke` màu đặc.
- Nhãn trục (`axis`) đặt ở mép ngoài mỗi nan hoa, `text-anchor` tính theo góc để không đè
  lên polygon.

### `CalendarHeatmap.vue`
Lưới tháng hiện tại, mỗi ô = 1 ngày, đậm nhạt theo số đơn hàng trong ngày đó. Props:
`data: [{ day, count }]` (day = 1..số ngày trong tháng), `month`, `year`. Render bằng CSS
grid 7 cột (giống cấu trúc tuần), không cần SVG — mỗi ô là 1 `<div>` màu nền tính theo
`opacity = 0.15 + (count / maxCount) * 0.85` trên nền `--accent-2`, số ngày hiển thị nhỏ ở
góc ô. Ô của "hôm nay" có viền nổi bật.

### `BarChart.vue` — thêm prop `vertical`
Không tạo file mới. Thêm prop `vertical: { type: Boolean, default: false }`. Khi
`vertical=true`, đổi template render cột đứng (dùng flex `align-items: flex-end`, mỗi
cột là 1 `<div>` cao theo %, label dưới chân) thay vì thanh ngang hiện tại — dùng lại y
hệt `rows` computed (`max`, `pct`) đã có, chỉ khác phần template.

## 3. Bố cục `AdminDashboard.vue` — 3 hàng

| # | Khối ảnh | Map sang | Component | Nguồn dữ liệu |
|---|---|---|---|---|
| 1a | Payroll + Total Sales | Doanh thu tháng này + Tổng đơn hàng | 2 stat card (giữ style card hiện có) | `revenueThisMonth`, `revenueThisMonthDelta`, `totalOrders` — đã có |
| 1b-d | 3 Budget ring | Hoàn thành đơn / Thanh toán / Tồn kho khỏe | `RingProgress` ×3 | `orderCompletionRate`, `paymentRate`, `stockHealthRate` — đã có (đang dùng cho `GaugeChart` ở khối KPI cũ, xem mục 4) |
| 1e | Positions | Nhân viên theo chức vụ | `DotMatrix` | **Mới** — cần `staff` + `chucVuList` (xem mục "Data flow") |
| 2a | Sales Growth (line lớn) | Doanh thu theo tháng | `TrendChart` (dùng nguyên, không sửa) | `revenueTrendChart` — đã có |
| 2b | KPI radar | 4 trục vận hành | `RadarChart` | `orderCompletionRate`, `paymentRate`, `stockHealthRate`, `activeProductRatio` (mới, trivial) |
| 3a | Balance Working Hours | Tỷ lệ sản phẩm đang bán | `RingProgress` | `activeProductRatio` (mới, trivial) |
| 3b | Weekly Payroll Budget (cột) | Doanh thu theo ngày trong tuần | `BarChart` (`vertical=true`) | **Mới** — group theo `weekChartFrom`/`weekChartTo` đã có sẵn |
| 3c | Heat Map lịch | Số đơn hàng theo ngày trong tháng | `CalendarHeatmap` | **Mới** — group `orders` theo ngày trong tháng hiện tại |

## 4. Data flow — thêm vào `AdminPage.vue`, truyền xuống qua props (đúng convention hiện tại)

```js
// Trivial — tái dùng activeProducts/groupedProducts đã có
const activeProductRatio = computed(() =>
  groupedProducts.value.length
    ? (activeProducts.value / groupedProducts.value.length) * 100
    : 0
);

// Mới — group orders trong tuần hiện tại (weekChartFrom/weekChartTo đã có) theo thứ
const weeklyRevenueChart = computed(() => {
  const days = ['T2','T3','T4','T5','T6','T7','CN'];
  const sums = new Array(7).fill(0);
  ordersInWeekRange.value.forEach(o => {
    const idx = (new Date(o.ngayDat).getDay() + 6) % 7; // 0=T2 ... 6=CN
    sums[idx] += Number(o.thanhTien) || 0;
  });
  return days.map((label, i) => ({ label, value: sums[i] }));
});

// Mới — group orders trong tháng hiện tại theo ngày, cho CalendarHeatmap
const monthlyOrderHeat = computed(() => {
  const now = new Date();
  const map = {};
  orders.value.forEach(o => {
    if (!o.ngayDat) return;
    const d = new Date(o.ngayDat);
    if (d.getFullYear() !== now.getFullYear() || d.getMonth() !== now.getMonth()) return;
    map[d.getDate()] = (map[d.getDate()] || 0) + 1;
  });
  return Object.entries(map).map(([day, count]) => ({ day: Number(day), count }));
});

// KPI radar — gộp 4 tỉ lệ đã có thành 1 mảng
const kpiRadarData = computed(() => [
  { axis: t('admin.dashboard.gaugeCompletion'), value: orderCompletionRate.value },
  { axis: t('admin.dashboard.gaugePayment'),    value: paymentRate.value },
  { axis: t('admin.dashboard.gaugeStock'),       value: stockHealthRate.value },
  { axis: t('admin.dashboard.activeProducts'),  value: activeProductRatio.value },
]);

// Positions — nhân viên theo chức vụ
const staffByRole = computed(() => {
  const map = {};
  staff.value.forEach(s => {
    const name = chucVuName(s.chucVuId);
    map[name] = (map[name] || 0) + 1;
  });
  return Object.entries(map).map(([label, value], i) => ({
    label, value, color: ROLE_COLORS[i % ROLE_COLORS.length],
  }));
});
```

`ROLE_COLORS` — hằng số mới, mảng 4-5 mã màu cố định lấy từ 2 đầu `--gradient-brand`
(`--accent-2`, `--accent`) và 2-3 màu trạng thái sẵn có (`#22c55e`, `#facc15`...), tránh
phải sinh màu động cho danh sách chức vụ có thể thay đổi.

**Thay đổi hành vi cần lưu ý:** `staff`/`chucVuList` hiện đang lazy-load (chỉ gọi
`ensureStaff()`/`ensureChucVuList()` khi `navigate('staff')`, xem comment ở `fetchAll` —
"Nhân viên KHÔNG tải ở đây nữa — không có KPI/dashboard/POS nào cần đến staff.value").
Khối "Positions" phá vỡ giả định đó. Xử lý: gọi `ensureStaff()` + `ensureChucVuList()`
ngay trong `fetchAll()` (dashboard là tab mặc định khi vào trang) — chấp nhận tải thêm 1
bảng nhỏ (nhân viên thường không nhiều dòng) lúc vào trang thay vì lazy, đổi lại
comment cũ trong code cho khớp.

## 5. Sidebar / topbar

Chỉ restyle, không đổi cấu trúc/logic điều hướng:
- `.adm-nav.active`: nền đổi từ màu đặc sang `var(--gradient-brand)`, thêm
  `box-shadow` glow nhẹ màu `--accent`.
- Badge logo (`SAO`): nền đổi sang `var(--gradient-brand)`.
- Topbar: không đổi (đã đủ gọn, khớp tinh thần ảnh mẫu).

## Phạm vi KHÔNG làm

- Không thêm dependency chart mới (Chart.js/ApexCharts/D3...).
- Không đổi API/backend — 100% tính từ dữ liệu đã tải sẵn ở frontend.
- KPI radar chỉ 1 series (không so sánh "tuần này/tuần trước" hay "tháng này/tháng
  trước" — tránh phải thêm truy vấn kỳ trước cho orders/customers, ngoài phạm vi ảnh
  mẫu cũng chỉ cần thể hiện snapshot hiện tại).
- `DotMatrix` không vẽ N hàng riêng biệt theo từng category như ảnh gốc (ảnh gốc có
  nhiều hàng vì nhiều nhân viên/category) — chỉ 1 lưới liên tục lấp theo tỉ lệ, đủ cho
  2-4 chức vụ thực tế của shop.
- Bảng sản phẩm gần đây, donut trạng thái đơn (hôm nay/tuần), top/slow sản phẩm: giữ
  nguyên cấu trúc, chỉ đổi màu card cho khớp bảng gradient mới — không đổi component.

## i18n

Dự án hiện chỉ còn 2 locale `vi`/`en` (đã bỏ `zh`/`ja`/`ko` trong phiên làm việc này).
Key mới cần thêm ở cả 2 file `locales/vi.js` và `locales/en.js`, namespace
`admin.dashboard.*`: `activeProducts` (nhãn "Sản phẩm đang bán"), `positions` (nhãn
"Nhân viên theo chức vụ"), `weeklyRevenue` (nhãn "Doanh thu theo tuần"), `orderHeatmap`
(nhãn "Đơn hàng theo ngày"). Ưu tiên tái dùng key có sẵn (`gaugeCompletion`,
`gaugePayment`, `gaugeStock`) cho radar thay vì tạo key trùng nghĩa.

## Testing

Không có test tự động cho `AdminDashboard.vue` (component UI thuần, đúng convention hiện
tại của dự án — verify bằng chạy thực tế). 4 component SVG mới (`RingProgress`,
`DotMatrix`, `RadarChart`, `CalendarHeatmap`) là pure-props không phụ thuộc store/API —
nếu dự án có sẵn hạ tầng test component (xem `src/__tests__/`) có thể thêm vài test nhỏ
kiểm tra tính toán hình học (dash offset, toạ độ radar) không NaN với input biên (data
rỗng, value=0, value=100) — quyết định cụ thể lúc viết plan. Verify chính bằng cách chạy
`npm run dev`, mở Dashboard ở cả 2 theme dark/light, kiểm tra không vỡ layout ở màn hẹp
(mobile sidebar overlay).
