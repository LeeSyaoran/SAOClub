# Restyle Admin Dashboard theo mẫu "Joint Payroll" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle tab Dashboard (`AdminDashboard.vue`) trong Admin panel theo bố cục 3
hàng của ảnh mẫu "Joint Payroll" (gradient tím-hồng, ring/dot-matrix/radar/heatmap), map
từng khối sang số liệu shop máy tính hiện có — không bịa dữ liệu không có thật, không
thêm API/backend mới.

**Architecture:** Thuần frontend. Thêm 4 component SVG thuần (`RingProgress`,
`DotMatrix`, `RadarChart`, `CalendarHeatmap`) theo đúng khuôn 4 component chart hiện có
(`DonutChart`/`GaugeChart`/`BarChart`/`TrendChart` — SVG tay, không dependency ngoài).
Thêm 1 token gradient mới vào `theme.css`. Thêm vài `computed` mới trong `AdminPage.vue`
(nguồn dữ liệu, giữ đúng convention prop-drilling xuống `AdminDashboard.vue` đang có).
Restructure lại template `AdminDashboard.vue`.

**Tech Stack:** Vue 3 `<script setup>`, Bootstrap 5 grid, CSS variables cho theming. Dự
án có `vitest` + `@vue/test-utils` nhưng theo khảo sát `src/__tests__/` chỉ test
`services/`, `stores/`, `utils/` — **không có test nào cho file `.vue`** (component hay
page). Plan này giữ đúng convention đó: không thêm test tự động cho `.vue`, verify bằng
`npm run lint` (cú pháp/style) + kiểm tra thủ công qua `npm run dev` (ghi rõ trong từng
task).

## Global Constraints

- Không thêm dependency chart mới (Chart.js/ApexCharts/D3...) — 100% SVG tay theo
  convention hiện có.
- Không đổi API/backend — 0 file trong `BackEnd/` bị đụng tới.
- Token gradient mới (`--gradient-brand`) chỉ dùng ở: sidebar nav active, logo badge, 4
  chart mới. Không đổi cách dùng `--accent`/`--accent-2` ở nơi khác trong app.
- KPI radar chỉ 1 series (không so sánh kỳ trước/kỳ này).
- `DotMatrix` dùng lưới cố định 50 dot (5 hàng × 10 cột), không vẽ nhiều hàng
  riêng/category như ảnh gốc.
- Quyết định thêm khi viết plan (spec không nói rõ, xử lý ở đây để không mất dữ liệu):
  - Khối "Payroll + Total Sales" (1a) gộp CẢ 2 stat chính (doanh thu tháng, tổng đơn
    hàng) LẪN 3 stat phụ đang có ở dashboard cũ (tổng sản phẩm, tổng khách hàng, doanh
    thu cả năm) thành 1 card — không bỏ số liệu nào đang hiển thị.
  - Block "Chỉ số vận hành" (KPI Health, 3 `GaugeChart` nửa hình tròn) và block "Xu
    hướng doanh thu" (`TrendChart` đứng riêng) trong `AdminDashboard.vue` hiện tại bị
    XOÁ khỏi vị trí cũ — dữ liệu của chúng chuyển vào đúng vị trí mới trong bố cục 3
    hàng (không hiển thị trùng lặp 2 nơi).
  - `GaugeChart.vue` sau khi bị bỏ dùng ở `AdminDashboard.vue` không còn nơi nào khác
    dùng (đã xác minh bằng grep toàn `src/`) → xoá file, theo đúng nguyên tắc không để
    code chết. Đánh đổi: mất mã màu theo ngưỡng (đỏ/vàng/xanh) mà `GaugeChart` đang có —
    `RingProgress` luôn dùng gradient thương hiệu, không đổi màu theo ngưỡng, đúng tinh
    thần ảnh mẫu (ảnh mẫu cũng không tô màu theo ngưỡng).
- File locale hiện chỉ còn `vi.js`/`en.js` (đã bỏ zh/ja/ko ở phiên làm việc trước) — key
  mới thêm ở cả 2 file, giữ cấu trúc song song.

---

### Task 1: Thêm token gradient vào theme

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/assets/theme.css:34` (cuối block `[data-theme="dark"]`), `theme.css:62` (cuối block `[data-theme="light"]`)

**Interfaces:**
- Produces: CSS custom property `--gradient-brand` (khác giá trị giữa dark/light, resolve
  runtime theo `[data-theme]` hiện tại). Dùng ở Task 4 (`RingProgress`), Task 6
  (`RadarChart`), Task 9 (sidebar).

- [ ] **Step 1: Thêm dòng vào block dark**

Trong `theme.css`, dòng 34 hiện là:
```css
  --shadow-color: rgba(12,12,21,0.5);
}
```
Sửa thành:
```css
  --shadow-color: rgba(12,12,21,0.5);
  --gradient-brand: linear-gradient(135deg, var(--accent-2), var(--accent));
}
```

- [ ] **Step 2: Thêm dòng vào block light**

Dòng 62 hiện là:
```css
  --shadow-color: rgba(225,29,72,0.1);
}
```
Sửa thành:
```css
  --shadow-color: rgba(225,29,72,0.1);
  --gradient-brand: linear-gradient(135deg, var(--accent-2), var(--accent));
}
```

- [ ] **Step 3: Verify bằng mắt**

Chạy `npm run dev` trong `FrontEnd/QLBanMayTinh/`, mở DevTools, chọn thẻ `<html>` hoặc
`<body>`, kiểm tra `getComputedStyle(document.body).getPropertyValue('--gradient-brand')`
trong Console trả về 1 chuỗi `linear-gradient(...)` không rỗng, ở cả 2 theme (bấm nút
toggle dark/light trên topbar).

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/assets/theme.css
git commit -m "feat(theme): add --gradient-brand token for dashboard restyle"
```

---

### Task 2: Thêm i18n key mới

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js:600` (trong block `dashboard`, sau `unitsSold`)
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js:600` (tương ứng)

**Interfaces:**
- Produces: key `admin.dashboard.positions`, `admin.dashboard.activeProducts`,
  `admin.dashboard.kpiRadarChart`, `admin.dashboard.weeklyRevenueChart`,
  `admin.dashboard.orderHeatmap`. Dùng ở Task 10.
- Tái dùng key có sẵn (không tạo mới): `admin.dashboard.gaugeCompletion`,
  `gaugePayment`, `gaugeStock`, `revenueThisMonth`, `totalOrders`, `totalProducts`,
  `totalCustomers`, `revenueThisYear`, `chartEmptyOrders`, `emptyProducts`.

- [ ] **Step 1: Thêm key vào `vi.js`**

Dòng 600 hiện tại (`unitsSold: "{count} máy",`) — thêm ngay sau:
```js
      unitsSold: "{count} máy",
      positions: "Nhân viên theo chức vụ",
      activeProducts: "Sản phẩm đang bán",
      kpiRadarChart: "Chỉ số vận hành tổng quan",
      weeklyRevenueChart: "Doanh thu theo ngày trong tuần",
      orderHeatmap: "Đơn hàng theo ngày",
```

- [ ] **Step 2: Thêm key vào `en.js`**

Dòng 600 hiện tại (`unitsSold: "{count} units",`) — thêm ngay sau:
```js
      unitsSold: "{count} units",
      positions: "Staff by role",
      activeProducts: "Active products",
      kpiRadarChart: "Operational overview",
      weeklyRevenueChart: "Revenue by weekday",
      orderHeatmap: "Orders by day",
```

- [ ] **Step 3: Verify**

Chạy `npm run lint` trong `FrontEnd/QLBanMayTinh/` — không lỗi cú pháp JS ở 2 file vừa
sửa.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(i18n): add dashboard keys for payroll-style restyle"
```

---

### Task 3: `BarChart.vue` — thêm chế độ cột đứng (`vertical`)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/BarChart.vue` (toàn bộ file, 38 dòng)

**Interfaces:**
- Consumes: không đổi — vẫn `data: [{ label, value, color?, displayValue? }]`.
- Produces: prop mới `vertical: Boolean` (default `false`, không đổi hành vi khi không
  truyền). Dùng ở Task 10 với `vertical` (không giá trị = `true`).

- [ ] **Step 1: Thay toàn bộ nội dung file**

```vue
<template>
  <!-- Biểu đồ cột — ngang (mặc định) hoặc đứng (vertical) — không cần thư viện ngoài -->
  <div v-if="!vertical" class="d-flex flex-column gap-3">
    <div v-for="(row, i) in rows" :key="i" class="d-flex align-items-center gap-2">
      <div v-if="row.image !== undefined" class="rounded-2 d-flex align-items-center justify-content-center overflow-hidden flex-shrink-0"
           style="width:30px; height:26px; background:var(--bg-card-inset);">
        <img v-if="row.image" :src="row.image" :alt="row.label" style="width:100%; height:100%; object-fit:contain; padding:2px;" />
        <span v-else><Laptop :size="14" color="var(--text-muted)" /></span>
      </div>
      <div class="text-truncate flex-shrink-0" style="width:110px; font-size:11.5px; color:var(--text-secondary);" :title="row.label">
        {{ row.label }}
      </div>
      <div class="flex-grow-1 rounded-pill overflow-hidden" style="background:var(--bg-hover); height:14px;">
        <div class="h-100 rounded-pill" :style="{ width: row.pct + '%', background: row.color || 'var(--accent)', transition: 'width .6s ease' }"></div>
      </div>
      <div class="fw-bold text-end flex-shrink-0" style="width:64px; font-size:11.5px; color:var(--text-primary);">
        {{ row.displayValue ?? row.value }}
      </div>
    </div>
    <div v-if="rows.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
  </div>

  <div v-else class="d-flex align-items-end gap-2" style="height:150px;">
    <div v-for="(row, i) in rows" :key="i" class="d-flex flex-column align-items-center flex-grow-1 h-100">
      <div class="flex-grow-1 d-flex align-items-end w-100">
        <div class="w-100 rounded-top-2"
             :style="{ height: Math.max(row.pct, row.value > 0 ? 4 : 0) + '%', background: row.color || 'var(--accent)', transition: 'height .6s ease' }"></div>
      </div>
      <div class="text-truncate mt-1" style="font-size:10px; color:var(--text-muted); max-width:100%;">{{ row.label }}</div>
      <div class="fw-bold text-truncate" style="font-size:10px; color:var(--text-primary); max-width:100%;">{{ row.displayValue ?? row.value }}</div>
    </div>
    <div v-if="rows.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { Laptop } from '@lucide/vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value, color?, displayValue? }]
  emptyText: { type: String, default: '' },
  vertical:  { type: Boolean, default: false },
});

const max = computed(() => Math.max(...props.data.map(d => d.value || 0), 1));
const rows = computed(() =>
  props.data.map(d => ({ ...d, pct: d.value > 0 ? Math.max(4, (d.value / max.value) * 100) : 0 }))
);
</script>
```

- [ ] **Step 2: Verify không vỡ chỗ đang dùng ngang**

`BarChart` đang được dùng ở `AdminDashboard.vue` cho "Top 5 bán chạy"/"5 bán chậm" —
không truyền `vertical` nên vẫn render nhánh `v-if="!vertical"` y hệt trước. Chạy `npm
run dev`, vào tab Dashboard, xác nhận 2 khối đó không đổi giao diện.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/common/BarChart.vue
git commit -m "feat(common): add vertical column mode to BarChart"
```

---

### Task 4: `RingProgress.vue` — vòng tròn tiến độ gradient (mới)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/common/RingProgress.vue`

**Interfaces:**
- Produces: component `RingProgress` với props `value: Number` (0-100, required),
  `label: String`, `size: Number` (default 110), `thickness: Number` (default 10),
  `color1`/`color2: String` (default `var(--accent-2)`/`var(--accent)`). Dùng ở Task 10.

- [ ] **Step 1: Tạo file**

```vue
<template>
  <!-- Vòng tròn tiến độ 1 giá trị (0-100), gradient thương hiệu — khác DonutChart.vue
       (nhiều segment + legend): chỉ 1 giá trị, không legend, không thư viện ngoài. -->
  <div class="d-flex flex-column align-items-center">
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
      <defs>
        <linearGradient :id="gradientId" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" :stop-color="color1" />
          <stop offset="100%" :stop-color="color2" />
        </linearGradient>
      </defs>
      <circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" stroke="var(--bg-hover)" :stroke-width="thickness" />
      <circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" :stroke="`url(#${gradientId})`" :stroke-width="thickness"
              stroke-linecap="round"
              :stroke-dasharray="`${dash} ${circumference - dash}`"
              style="transform:rotate(-90deg); transform-origin:center; transition:stroke-dasharray .6s ease;" />
      <text :x="size / 2" :y="size / 2 + 7" text-anchor="middle" fill="var(--text-heading)" style="font-size:20px; font-weight:800;">{{ Math.round(clamped) }}%</text>
    </svg>
    <div v-if="label" class="small text-center mt-1" style="color:var(--text-secondary); max-width:100%;">{{ label }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  value:     { type: Number, required: true }, // 0-100
  label:     { type: String, default: '' },
  size:      { type: Number, default: 110 },
  thickness: { type: Number, default: 10 },
  color1:    { type: String, default: 'var(--accent-2)' },
  color2:    { type: String, default: 'var(--accent)' },
});

// id ngẫu nhiên/instance — nhiều RingProgress trên cùng trang không được trùng id
// <linearGradient>, nếu không SVG sau sẽ vẽ đè gradient của SVG trước.
const gradientId = `ring-grad-${Math.random().toString(36).slice(2, 9)}`;

const radius = computed(() => props.size / 2 - props.thickness / 2 - 2);
const circumference = computed(() => 2 * Math.PI * radius.value);
const clamped = computed(() => Math.min(100, Math.max(0, props.value)));
const dash = computed(() => (clamped.value / 100) * circumference.value);
</script>
```

- [ ] **Step 2: Verify tạm thời**

Chưa có nơi nào dùng component này (sẽ dùng ở Task 10). Verify bằng cách tạm thêm 1 dòng
`<RingProgress :value="65" label="Test" />` vào đầu template `AdminDashboard.vue` (nhớ
import tạm), chạy `npm run dev`, xác nhận vòng tròn gradient hiện đúng ~65%, số "65%" ở
giữa, ở cả 2 theme — rồi XOÁ dòng test này trước khi qua bước commit (không để lại code
test tạm trong file).

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/common/RingProgress.vue
git commit -m "feat(common): add RingProgress component"
```

---

### Task 5: `DotMatrix.vue` — lưới chấm tỉ lệ (mới)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/common/DotMatrix.vue`

**Interfaces:**
- Produces: component `DotMatrix` với props `data: Array` (`[{ label, value, color }]`,
  required), `emptyText: String`. Dùng ở Task 10 cho khối "Positions".

- [ ] **Step 1: Tạo file**

```vue
<template>
  <!-- Lưới chấm tỉ lệ theo danh mục — dùng cho "Nhân viên theo chức vụ". Lưới cố định
       50 dot (5 hàng x 10 cột), mỗi category lấp 1 số dot liên tục tỉ lệ theo value. -->
  <div class="d-flex align-items-center gap-3 flex-wrap">
    <svg :width="gridWidth" :height="gridHeight" :viewBox="`0 0 ${gridWidth} ${gridHeight}`" style="flex-shrink:0;">
      <circle v-for="(dot, i) in dots" :key="i" :cx="dot.x" :cy="dot.y" :r="DOT_RADIUS" :fill="dot.color" />
    </svg>
    <div class="d-flex flex-column gap-2">
      <div v-for="(seg, i) in segments" :key="i" class="d-flex align-items-center gap-2">
        <span class="rounded-circle flex-shrink-0" :style="{ background: seg.color, width: '9px', height: '9px' }"></span>
        <span class="flex-grow-1" style="font-size:12px; color:var(--text-primary);">{{ seg.label }}</span>
        <span class="fw-bold" style="font-size:12px; color:var(--text-secondary);">{{ Math.round(seg.pct) }}%</span>
      </div>
      <div v-if="segments.length === 0" class="small" style="color:var(--text-muted);">{{ emptyText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ label, value, color }]
  emptyText: { type: String, default: '' },
});

const TOTAL_DOTS = 50;
const COLS = 10;
const DOT_RADIUS = 5;
const GAP = 14;
const gridWidth = COLS * GAP;
const gridHeight = Math.ceil(TOTAL_DOTS / COLS) * GAP;

const total = computed(() => props.data.reduce((s, d) => s + (d.value || 0), 0));

const segments = computed(() => {
  if (total.value === 0) return [];
  return props.data.filter(d => d.value > 0).map(d => ({ ...d, pct: (d.value / total.value) * 100 }));
});

// Category cuối lấy hết dot còn lại (thay vì round riêng từng category) để tổng luôn
// đúng 50, không lệch do làm tròn.
const dots = computed(() => {
  if (total.value === 0) return [];
  const result = [];
  let dotIndex = 0;
  let allocated = 0;
  segments.value.forEach((seg, i) => {
    const isLast = i === segments.value.length - 1;
    const count = isLast ? TOTAL_DOTS - allocated : Math.round((seg.value / total.value) * TOTAL_DOTS);
    allocated += count;
    for (let n = 0; n < count && dotIndex < TOTAL_DOTS; n++, dotIndex++) {
      const col = dotIndex % COLS;
      const row = Math.floor(dotIndex / COLS);
      result.push({ x: col * GAP + GAP / 2, y: row * GAP + GAP / 2, color: seg.color });
    }
  });
  return result;
});
</script>
```

- [ ] **Step 2: Verify tạm thời**

Tạm thêm `<DotMatrix :data="[{label:'A',value:3,color:'#7c3aed'},{label:'B',value:1,color:'#f43f5e'}]" />`
vào đầu template `AdminDashboard.vue`, `npm run dev`, xác nhận ~38 dot màu tím + ~12 dot
màu hồng lấp liên tục theo hàng, legend hiện đúng "75%"/"25%". Xoá dòng test trước khi
commit.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/common/DotMatrix.vue
git commit -m "feat(common): add DotMatrix component"
```

---

### Task 6: `RadarChart.vue` — radar nhiều trục 1 series (mới)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/common/RadarChart.vue`

**Interfaces:**
- Produces: component `RadarChart` với props `data: Array` (`[{ axis, value }]`, value
  0-100, required), `size: Number` (default 220), `color: String` (default
  `var(--accent)`), `emptyText: String`. Dùng ở Task 10 cho khối "KPI".

- [ ] **Step 1: Tạo file**

```vue
<template>
  <!-- Radar nhiều trục, 1 series — không so sánh 2 kỳ (xem Global Constraints trong
       plan). Toạ độ mỗi trục chia đều 360°, bắt đầu từ đỉnh (12h). -->
  <svg v-if="data.length" :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
    <polygon v-for="ring in GRID_RINGS" :key="ring" :points="ringPoints(ring)"
             fill="none" stroke="var(--border-color-soft)" stroke-width="1" />
    <line v-for="(p, i) in axisPoints" :key="'axis' + i" :x1="center" :y1="center" :x2="p.x" :y2="p.y"
          stroke="var(--border-color-soft)" stroke-width="1" />
    <polygon :points="valuePolygon" fill="var(--accent-2)" fill-opacity="0.25" :stroke="color" stroke-width="2"
              style="transition: points .6s ease;" />
    <circle v-for="(p, i) in valuePoints" :key="'dot' + i" :cx="p.x" :cy="p.y" r="3" :fill="color" />
    <text v-for="(p, i) in labelPoints" :key="'label' + i" :x="p.x" :y="p.y"
          :text-anchor="p.anchor" dominant-baseline="middle"
          fill="var(--text-secondary)" style="font-size:10px;">{{ data[i].axis }}</text>
  </svg>
  <div v-else class="small text-center py-3" style="color:var(--text-muted);">{{ emptyText }}</div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ axis, value }] value 0-100
  size:      { type: Number, default: 220 },
  color:     { type: String, default: 'var(--accent)' },
  emptyText: { type: String, default: '' },
});

const GRID_RINGS = [0.25, 0.5, 0.75, 1];

const center = computed(() => props.size / 2);
const maxRadius = computed(() => props.size / 2 - 28);
const n = computed(() => props.data.length);

const angleFor = (i) => (2 * Math.PI * i) / n.value - Math.PI / 2;

const pointAt = (i, radiusFraction) => {
  const a = angleFor(i);
  return {
    x: center.value + Math.cos(a) * maxRadius.value * radiusFraction,
    y: center.value + Math.sin(a) * maxRadius.value * radiusFraction,
  };
};

const ringPoints = (fraction) =>
  Array.from({ length: n.value }, (_, i) => {
    const p = pointAt(i, fraction);
    return `${p.x},${p.y}`;
  }).join(' ');

const axisPoints = computed(() => Array.from({ length: n.value }, (_, i) => pointAt(i, 1)));

const valuePoints = computed(() =>
  props.data.map((d, i) => pointAt(i, Math.min(100, Math.max(0, d.value)) / 100))
);
const valuePolygon = computed(() => valuePoints.value.map(p => `${p.x},${p.y}`).join(' '));

const labelPoints = computed(() =>
  Array.from({ length: n.value }, (_, i) => {
    const p = pointAt(i, 1.18);
    const cos = Math.cos(angleFor(i));
    const anchor = cos > 0.3 ? 'start' : cos < -0.3 ? 'end' : 'middle';
    return { ...p, anchor };
  })
);
</script>
```

- [ ] **Step 2: Verify tạm thời**

Tạm thêm `<RadarChart :data="[{axis:'A',value:80},{axis:'B',value:40},{axis:'C',value:90},{axis:'D',value:60}]" />`
vào đầu template `AdminDashboard.vue`, `npm run dev`, xác nhận hình tứ giác lệch đúng
hình dạng theo 4 giá trị, nhãn A/B/C/D không đè lên hình. Xoá dòng test trước khi commit.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/common/RadarChart.vue
git commit -m "feat(common): add RadarChart component"
```

---

### Task 7: `CalendarHeatmap.vue` — lịch tháng theo mật độ (mới)

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/common/CalendarHeatmap.vue`

**Interfaces:**
- Produces: component `CalendarHeatmap` với props `data: Array` (`[{ day, count }]`,
  required), `month: Number` (0-11, required), `year: Number` (required). Dùng ở Task 10
  cho khối "Heat Map".

- [ ] **Step 1: Tạo file**

```vue
<template>
  <!-- Lưới tháng, đậm nhạt theo count/ngày — dùng CSS grid, không cần SVG. -->
  <div class="d-grid gap-1" style="grid-template-columns: repeat(7, 1fr);">
    <div v-for="dow in DOW_LABELS" :key="dow" class="text-center" style="font-size:9px; color:var(--text-muted);">{{ dow }}</div>
    <div v-for="n in leadingBlanks" :key="'blank' + n"></div>
    <div v-for="cell in cells" :key="cell.day"
         class="rounded-2 d-flex align-items-center justify-content-center"
         :class="{ 'ring-today': cell.isToday }"
         :style="{ aspectRatio: '1', background: cellColor(cell.count), fontSize: '9px', color: cell.count > 0 ? '#fff' : 'var(--text-muted)' }">
      {{ cell.day }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  data:  { type: Array, required: true }, // [{ day, count }]
  month: { type: Number, required: true }, // 0-11
  year:  { type: Number, required: true },
});

const DOW_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

const countByDay = computed(() => new Map(props.data.map(d => [d.day, d.count])));
const maxCount = computed(() => Math.max(1, ...props.data.map(d => d.count)));
const daysInMonth = computed(() => new Date(props.year, props.month + 1, 0).getDate());
// getDay() trả 0=Chủ nhật — quy về cột Thứ 2 đầu tuần (0=T2...6=CN) cho khớp DOW_LABELS.
const leadingBlanks = computed(() => (new Date(props.year, props.month, 1).getDay() + 6) % 7);

const TODAY = new Date();
const cells = computed(() =>
  Array.from({ length: daysInMonth.value }, (_, i) => {
    const day = i + 1;
    return {
      day,
      count: countByDay.value.get(day) || 0,
      isToday: props.year === TODAY.getFullYear() && props.month === TODAY.getMonth() && day === TODAY.getDate(),
    };
  })
);

const cellColor = (count) => {
  if (count === 0) return 'var(--bg-hover)';
  const pct = Math.round((0.35 + (count / maxCount.value) * 0.65) * 100);
  return `color-mix(in srgb, var(--accent-2) ${pct}%, transparent)`;
};
</script>

<style scoped>
.ring-today { outline: 2px solid var(--accent); outline-offset: 1px; }
</style>
```

- [ ] **Step 2: Verify tạm thời**

Tạm thêm `<CalendarHeatmap :data="[{day:5,count:3},{day:12,count:1}]" :month="new Date().getMonth()" :year="new Date().getFullYear()" />`
vào đầu template `AdminDashboard.vue`, `npm run dev`, xác nhận lưới tháng hiện đúng, ô
ngày 5 đậm hơn ô ngày 12, ô hôm nay có viền nổi bật. Kiểm tra `color-mix()` render đúng
màu (không hiện trong suốt hoàn toàn) trên trình duyệt Chrome/Edge hiện tại. Xoá dòng
test trước khi commit.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/common/CalendarHeatmap.vue
git commit -m "feat(common): add CalendarHeatmap component"
```

---

### Task 8: `AdminPage.vue` — computed dữ liệu mới + eager-load staff/chức vụ

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue:447-465` (sau `stockHealthRate`/`gaugeColor`, thêm computed mới)
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue:514-534` (`fetchAll` + comment liên quan)

**Interfaces:**
- Consumes: `orders`, `products`, `groupedProducts`, `activeProducts`,
  `orderCompletionRate`, `paymentRate`, `stockHealthRate`, `weekChartFrom`,
  `weekChartTo`, `ordersInWeekRange`, `staff`, `chucVuName` — tất cả đã có sẵn trong
  file này.
- Produces (dùng ở Task 10, truyền xuống `AdminDashboard.vue` qua props):
  `activeProductRatio: number`, `weeklyRevenueChart: Array<{label, value}>`,
  `monthlyOrderHeat: Array<{day, count}>`, `kpiRadarData: Array<{axis, value}>`,
  `staffByRole: Array<{label, value, color}>`.

- [ ] **Step 1: Thêm computed mới sau `gaugeColor`**

Tìm dòng 464 (`const gaugeColor = (pct) => (pct >= 70 ? '#22c55e' : pct >= 40 ?
'#facc15' : '#f87171');`) — thêm ngay sau (trước comment `// ── Doanh thu theo tháng
(trend) ──`):

```js
// ── Dữ liệu mới cho bố cục Dashboard kiểu "Joint Payroll" ─────────────────────
// Tỉ lệ sản phẩm đang bán/tổng sản phẩm — dùng cho ring "Sản phẩm đang bán" và 1 trục
// radar KPI. groupedProducts (không phải products) vì products là 1 dòng/biến thể.
const activeProductRatio = computed(() =>
  groupedProducts.value.length ? (activeProducts.value / groupedProducts.value.length) * 100 : 0
);

// Doanh thu theo từng ngày trong tuần hiện tại — dùng lại đúng ordersInWeekRange đã có
// (tính theo weekChartAnchor, mặc định tuần hiện tại) cho biểu đồ cột "Weekly Payroll
// Budget". getDay() trả 0=Chủ nhật — quy về mảng bắt đầu Thứ 2 (index 0) cho khớp UI.
const weeklyRevenueChart = computed(() => {
  const dayLabels = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
  const sums = new Array(7).fill(0);
  ordersInWeekRange.value.forEach((o) => {
    if (!o.ngayDat) return;
    const idx = (new Date(o.ngayDat).getDay() + 6) % 7;
    sums[idx] += Number(o.thanhTien) || 0;
  });
  return dayLabels.map((label, i) => ({ label, value: sums[i] }));
});

// Số đơn hàng theo từng ngày trong THÁNG HIỆN TẠI — cho "Heat Map" lịch. Chỉ tính 1
// lần lúc load trang (không tự cập nhật qua nửa đêm — chấp nhận được cho 1 dashboard
// admin, F5 lại nếu cần xem đúng tháng mới).
const monthlyOrderHeat = computed(() => {
  const now = new Date();
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const d = new Date(o.ngayDat);
    if (d.getFullYear() !== now.getFullYear() || d.getMonth() !== now.getMonth()) return;
    map[d.getDate()] = (map[d.getDate()] || 0) + 1;
  });
  return Object.entries(map).map(([day, count]) => ({ day: Number(day), count }));
});

// Gộp 4 tỉ lệ vận hành đã có (đơn hoàn tất/thanh toán/tồn kho/sản phẩm đang bán) thành
// 1 mảng cho RadarChart — tái dùng đúng key i18n các gauge cũ, không tạo nhãn mới.
const kpiRadarData = computed(() => [
  { axis: t('admin.dashboard.gaugeCompletion'), value: orderCompletionRate.value },
  { axis: t('admin.dashboard.gaugePayment'), value: paymentRate.value },
  { axis: t('admin.dashboard.gaugeStock'), value: stockHealthRate.value },
  { axis: t('admin.dashboard.activeProducts'), value: activeProductRatio.value },
]);

// Nhân viên theo chức vụ — cho DotMatrix "Positions". Màu cố định nhỏ (không sinh màu
// động) vì số chức vụ thực tế của shop chỉ vài nhóm, lặp lại bảng nếu nhiều hơn.
const ROLE_COLORS = ['#7c3aed', '#f43f5e', '#22c55e', '#facc15', '#0e7490'];
const staffByRole = computed(() => {
  const map = {};
  staff.value.forEach((s) => {
    const name = chucVuName(s.chucVuId);
    map[name] = (map[name] || 0) + 1;
  });
  return Object.entries(map).map(([label, value], i) => ({
    label, value, color: ROLE_COLORS[i % ROLE_COLORS.length],
  }));
});
```

- [ ] **Step 2: Sửa `fetchAll` để tải sẵn staff/chức vụ**

Đoạn hiện tại (dòng ~514-534):
```js
// ── Fetch ─────────────────────────────────────────────────────────────────────
// Chỉ tải 6 bảng chính lúc vào trang (dashboard + các bảng danh sách cần ngay).
// Danh mục/hãng/CPU/RAM/ổ cứng/GPU (ensureProductRefData) đã chuyển vào
// ProductsTable.vue (Task 3) — chỉ tab Sản phẩm cần. Chức vụ (ensureChucVuList) vẫn
// KHÔNG tải ở đây — chỉ tải khi vào trang Nhân viên, xem ensureChucVuList() bên dưới.
// Với dữ liệu lớn, bớt 7-8 lệnh gọi song song này giúp trang vào nhanh hơn hẳn.
// Nhân viên KHÔNG tải ở đây nữa — không có KPI/dashboard/POS nào cần đến staff.value,
// chỉ tab Nhân viên và tab Phiếu nhập (staffName/staffOptions) cần, cả 2 đều lazy-load
// qua ensureStaff() (stores/staff.js). products/orders/customers/promotions/inventory VẪN
// tải eager vì dashboard KPI + POS (tìm SP, áp mã khuyến mãi, tra cứu KH) cần ngay.
const fetchAll = async () => {
  await Promise.all([
    refreshProducts(),
    refreshOrders(),
    refreshCustomers(),
    refreshPromotions(),
    refreshInventory(),
    refreshDoiThuong(),
  ]);
  await autoMergeAllDuplicates();
};
```

Sửa thành:
```js
// ── Fetch ─────────────────────────────────────────────────────────────────────
// Danh mục/hãng/CPU/RAM/ổ cứng/GPU (ensureProductRefData) đã chuyển vào
// ProductsTable.vue (Task 3) — chỉ tab Sản phẩm cần, vẫn lazy-load riêng.
// Nhân viên + chức vụ NAY tải eager (ensureStaff/ensureChucVuList, có cache promise nên
// gọi lại ở navigate('staff') không tốn thêm lần fetch) — khối "Positions" ở Dashboard
// cần staffByRole ngay khi vào trang, đổi lại quyết định lazy-load cũ (staff từng không
// cần cho dashboard/KPI/POS, nay dashboard cần).
const fetchAll = async () => {
  await Promise.all([
    refreshProducts(),
    refreshOrders(),
    refreshCustomers(),
    refreshPromotions(),
    refreshInventory(),
    refreshDoiThuong(),
    ensureStaff(),
    ensureChucVuList(),
  ]);
  await autoMergeAllDuplicates();
};
```

- [ ] **Step 3: Verify**

Chạy `npm run dev`, đăng nhập admin, mở tab Dashboard (mặc định) — mở DevTools Network,
xác nhận có gọi API danh sách nhân viên (`NhanVienService.getAll` → endpoint nhân viên)
ngay khi vào trang, không cần bấm vào tab "Nhân viên". Vào tab Nhân viên, xác nhận danh
sách vẫn hiện đúng (không gọi lại API lần 2 nhờ cache promise của `ensureStaff`).

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(admin): add dashboard computed data + eager-load staff/chucVu"
```

---

### Task 9: `AdminPage.vue` — restyle sidebar active/logo theo gradient

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue` (logo badge, gần dòng 1004 —
  số dòng đã lệch so với bản gốc vì Task 8 chèn thêm ~40 dòng computed phía trên; tìm
  đúng đoạn code nêu ở Step 1 bằng nội dung, không dựa số dòng)
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue` (`.adm-nav.active`, gần dòng
  1525, cùng lưu ý số dòng lệch như trên — tìm đúng đoạn code ở Step 2)

**Interfaces:** Không đổi — thuần CSS, không props/state mới.

- [ ] **Step 1: Đổi nền logo badge**

Dòng 1003-1004 hiện tại:
```html
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:var(--accent);color:var(--accent-text);font-size:0.8rem;">SAO</div>
```
Sửa `background:var(--accent);` thành `background:var(--gradient-brand);`:
```html
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:var(--gradient-brand);color:var(--accent-text);font-size:0.8rem;">SAO</div>
```

- [ ] **Step 2: Đổi nav-item active sang pill gradient + glow**

Dòng 1525 hiện tại:
```css
.adm-nav.active { background: rgba(244,63,94,0.12); color: var(--accent-fg); }
```
Sửa thành:
```css
.adm-nav.active {
  background: var(--gradient-brand);
  color: var(--accent-text);
  box-shadow: 0 2px 10px -2px rgba(244,63,94,0.5);
}
```

- [ ] **Step 3: Verify bằng mắt**

`npm run dev`, mở Admin panel, xác nhận: logo "SAO" ở góc trên sidebar có nền gradient
tím-hồng; mục sidebar đang chọn (mặc định "Dashboard") có nền pill gradient + đổ bóng
nhẹ, chữ trắng đọc rõ (không bị chìm vào nền). Bấm qua lại vài mục khác trong sidebar,
xác nhận trạng thái active di chuyển đúng. Kiểm tra ở cả 2 theme dark/light (nút toggle
trên topbar) — `--accent-text` là trắng ở cả 2 theme nên chữ luôn đọc được trên gradient.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "style(admin): gradient sidebar active state + logo badge"
```

---

### Task 10: `AdminDashboard.vue` — restructure bố cục 3 hàng + wire props mới

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/AdminDashboard.vue` (toàn bộ file)
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue` (props truyền vào
  `<AdminDashboard>`, gần dòng 1111-1139 — số dòng đã lệch do Task 8/9 chèn/sửa code
  phía trên, tìm đúng đoạn code nêu ở Step 2 bằng nội dung)
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue` (xoá import `GaugeChart` chết
  — xem Step 3)
- Delete: `FrontEnd/QLBanMayTinh/src/components/common/GaugeChart.vue` (không còn nơi
  nào dùng sau task này — đã xác minh bằng grep)

**Interfaces:**
- Consumes: mọi prop cũ của `AdminDashboard.vue` (không đổi tên/kiểu) + 5 prop mới từ
  Task 8 (`activeProductRatio`, `weeklyRevenueChart`, `monthlyOrderHeat`, `kpiRadarData`,
  `staffByRole`) + 4 component từ Task 4-7 (`RingProgress`, `DotMatrix`, `RadarChart`,
  `CalendarHeatmap`) + `BarChart` với `vertical` từ Task 3 + key i18n từ Task 2.

- [ ] **Step 1: Thay toàn bộ nội dung `AdminDashboard.vue`**

```vue
<script setup>
import { computed } from "vue";
import { t } from "../../i18n/index.js";
import {
  Laptop, Users, Wallet, Calendar, AlertTriangle, PieChart, Flame,
  Turtle, Activity, TrendingUp, Archive, Monitor, Tag, FolderOpen, Banknote, Bookmark,
} from '@lucide/vue';
import { ProductsStore } from "../../stores/products.js";
import { OrdersStore } from "../../stores/orders.js";
import { CustomersStore } from "../../stores/customers.js";
import { InventoryStore } from "../../stores/inventory.js";
import { StaffStore } from "../../stores/staff.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import DonutChart from "../common/DonutChart.vue";
import BarChart from "../common/BarChart.vue";
import TrendChart from "../common/TrendChart.vue";
import RingProgress from "../common/RingProgress.vue";
import DotMatrix from "../common/DotMatrix.vue";
import RadarChart from "../common/RadarChart.vue";
import CalendarHeatmap from "../common/CalendarHeatmap.vue";

const props = defineProps({
  totalProducts: Number,
  totalOrders: Number,
  totalCustomers: Number,
  revenueThisMonth: Number,
  revenueThisMonthDelta: Number,
  revenueThisYear: Number,
  lowStockItems: { type: Array, default: () => [] },
  statusChartDate: String,
  isStatusChartToday: Boolean,
  ordersOnStatusChartDate: { type: Array, default: () => [] },
  orderStatusChartData: { type: Array, default: () => [] },
  weekChartAnchor: String,
  isWeekChartCurrentWeek: Boolean,
  weekChartRangeLabel: String,
  ordersInWeekRange: { type: Array, default: () => [] },
  weekOrderStatusChartData: { type: Array, default: () => [] },
  topSellingChart: { type: Array, default: () => [] },
  slowSellingChart: { type: Array, default: () => [] },
  orderCompletionRate: Number,
  paymentRate: Number,
  stockHealthRate: Number,
  revenueTrendChart: { type: Array, default: () => [] },
  products: { type: Array, default: () => [] },
  activeProductRatio: Number,
  weeklyRevenueChart: { type: Array, default: () => [] },
  monthlyOrderHeat: { type: Array, default: () => [] },
  kpiRadarData: { type: Array, default: () => [] },
  staffByRole: { type: Array, default: () => [] },
});

const emit = defineEmits([
  "update:statusChartDate",
  "update:weekChartAnchor",
  "resetToCurrentWeek",
  "backToToday",
]);

const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

const anyStoreLoading = computed(() =>
  ProductsStore.loading || OrdersStore.loading || CustomersStore.loading || InventoryStore.loading || !StaffStore.loaded
);

// ensureStaff() không set StaffStore.loading (chỉ refreshStaff() có) — dùng .loaded làm
// cờ chờ thay vì .loading cho đúng với cách store này báo trạng thái.
const weeklyRevenueBarData = computed(() =>
  props.weeklyRevenueChart.map((d) => ({ ...d, color: 'var(--accent-2)', displayValue: formatPrice(d.value) }))
);

// Tính 1 lần lúc mount — component giữ nguyên qua v-show nên không tự cập nhật qua nửa
// đêm, chấp nhận được cho dashboard admin (F5 lại nếu cần đúng tháng mới).
const now = new Date();
const heatmapMonth = now.getMonth();
const heatmapYear = now.getFullYear();
</script>

<template>
  <section>
    <div v-if="anyStoreLoading" class="text-secondary small">{{ t('admin.dashboard.loading') }}</div>
    <template v-else>

      <!-- ══════════ HÀNG 1: Doanh thu/Đơn + 3 ring vận hành + Positions ══════════ -->
      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-2 d-flex align-items-center gap-1"><Wallet :size="14" /> {{ t('admin.dashboard.revenueThisMonth') }}</div>
              <div class="d-flex align-items-center gap-2 mb-1 flex-wrap">
                <span class="fw-black" style="font-size:1.5rem; color:var(--text-heading);">{{ formatPrice(revenueThisMonth) }}</span>
                <span v-if="revenueThisMonthDelta !== null" class="fw-bold" style="font-size:0.72rem;"
                      :style="{ color: revenueThisMonthDelta >= 0 ? '#22c55e' : '#f87171' }">
                  {{ revenueThisMonthDelta >= 0 ? '▲' : '▼' }} {{ Math.abs(revenueThisMonthDelta) }}%
                </span>
              </div>
              <div class="fw-bold mb-3" style="font-size:1.1rem; color:var(--text-primary);">
                {{ totalOrders }} <span class="small fw-normal" style="color:var(--text-secondary);">{{ t('admin.dashboard.totalOrders') }}</span>
              </div>
              <div class="d-flex flex-column gap-1 pt-2" style="border-top:1px solid var(--border-color-soft);">
                <div class="d-flex justify-content-between small">
                  <span style="color:var(--text-secondary);">{{ t('admin.dashboard.totalProducts') }}</span>
                  <span class="fw-semibold">{{ totalProducts }}</span>
                </div>
                <div class="d-flex justify-content-between small">
                  <span style="color:var(--text-secondary);">{{ t('admin.dashboard.totalCustomers') }}</span>
                  <span class="fw-semibold">{{ totalCustomers }}</span>
                </div>
                <div class="d-flex justify-content-between small">
                  <span style="color:var(--text-secondary);">{{ t('admin.dashboard.revenueThisYear') }}</span>
                  <span class="fw-semibold">{{ formatPrice(revenueThisYear) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center">
              <RingProgress :value="orderCompletionRate" :label="t('admin.dashboard.gaugeCompletion')" />
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center">
              <RingProgress :value="paymentRate" :label="t('admin.dashboard.gaugePayment')" />
            </div>
          </div>
        </div>
        <div class="col-6 col-xl-2">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center">
              <RingProgress :value="stockHealthRate" :label="t('admin.dashboard.gaugeStock')" />
            </div>
          </div>
        </div>

        <div class="col-12 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Users :size="14" /> {{ t('admin.dashboard.positions') }}</div>
              <DotMatrix :data="staffByRole" :empty-text="t('admin.dashboard.emptyProducts')" />
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════ HÀNG 2: Doanh thu theo tháng (line lớn) + KPI radar ══════════ -->
      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-7">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><TrendingUp :size="14" /> {{ t('admin.dashboard.revenueTrendChart') }}</div>
              <TrendChart :data="revenueTrendChart" :height="180" color="#f06b81" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center">
              <div class="fw-semibold small text-secondary mb-2 align-self-start d-flex align-items-center gap-1"><Activity :size="14" /> {{ t('admin.dashboard.kpiRadarChart') }}</div>
              <RadarChart :data="kpiRadarData" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════ HÀNG 3: Sản phẩm đang bán + Doanh thu theo tuần + Lịch đơn hàng ══════════ -->
      <div class="row g-3 mb-4">
        <div class="col-12 col-md-4 col-xl-3">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body d-flex flex-column align-items-center justify-content-center h-100">
              <RingProgress :value="activeProductRatio" :label="t('admin.dashboard.activeProducts')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-md-8 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Calendar :size="14" /> {{ t('admin.dashboard.weeklyRevenueChart') }}</div>
              <BarChart :data="weeklyRevenueBarData" vertical :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-4">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Flame :size="14" /> {{ t('admin.dashboard.orderHeatmap') }}</div>
              <CalendarHeatmap :data="monthlyOrderHeat" :month="heatmapMonth" :year="heatmapYear" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="lowStockItems.length" class="alert alert-danger small py-2 mb-3 d-flex align-items-center gap-2">
        <span class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
              style="width:22px;height:22px;background:rgba(248,113,113,0.25);"><AlertTriangle :size="13" color="#f87171" /></span>
        {{ t('admin.dashboard.lowStockAlert', { count: lowStockItems.length }) }}
      </div>

      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-5">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                <div class="fw-semibold small text-secondary d-flex align-items-center gap-1"><PieChart :size="14" /> {{ t('admin.dashboard.ordersByStatusChart') }}</div>
                <div class="d-flex align-items-center gap-2">
                  <input type="date" :value="statusChartDate" :max="toDateInputValue(new Date())"
                         @input="$emit('update:statusChartDate', $event.target.value)"
                         class="form-control form-control-sm"
                         style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); width:auto; font-size:0.78rem; padding:2px 8px;" />
                  <button v-if="!isStatusChartToday" type="button" class="btn btn-sm py-0 px-2"
                          style="font-size:0.72rem; color:var(--accent-fg); border:1px solid var(--border-color-strong);"
                          @click="$emit('backToToday')">
                    {{ t('admin.dashboard.backToToday') }}
                  </button>
                </div>
              </div>
              <div class="mb-3">
                <span class="badge rounded-pill" style="background:var(--bg-card-inset); color:var(--text-secondary); font-weight:600;">
                  {{ isStatusChartToday
                    ? t('admin.dashboard.todayOrders', { count: ordersOnStatusChartDate.length })
                    : t('admin.dashboard.ordersOnDate', { count: ordersOnStatusChartDate.length }) }}
                </span>
              </div>
              <DonutChart
                :data="orderStatusChartData"
                :center-value="String(ordersOnStatusChartDate.length)"
                :center-label="t('admin.dashboard.totalOrders')"
                :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-7">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                <div class="fw-semibold small text-secondary d-flex align-items-center gap-1"><Calendar :size="14" /> {{ t('admin.dashboard.ordersByWeekChart') }}</div>
                <div class="d-flex align-items-center gap-2 flex-wrap">
                  <input type="date" :value="weekChartAnchor" :max="toDateInputValue(new Date())"
                         @input="$emit('update:weekChartAnchor', $event.target.value)"
                         class="form-control form-control-sm"
                         style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); width:auto; font-size:0.78rem; padding:2px 8px;" />
                  <button v-if="!isWeekChartCurrentWeek" type="button" class="btn btn-sm py-0 px-2"
                          style="font-size:0.72rem; color:var(--accent-fg); border:1px solid var(--border-color-strong);"
                          @click="$emit('resetToCurrentWeek')">
                    {{ t('admin.dashboard.backToThisWeek') }}
                  </button>
                </div>
              </div>
              <div class="mb-3 d-flex align-items-center gap-2 flex-wrap">
                <span class="badge rounded-pill" style="background:var(--bg-card-inset); color:var(--text-secondary); font-weight:600;">
                  {{ weekChartRangeLabel }}
                </span>
                <span class="badge rounded-pill" style="background:var(--bg-card-inset); color:var(--text-secondary); font-weight:600;">
                  {{ t('admin.dashboard.ordersInRange', { count: ordersInWeekRange.length }) }}
                </span>
              </div>
              <DonutChart
                :data="weekOrderStatusChartData"
                :center-value="String(ordersInWeekRange.length)"
                :center-label="t('admin.dashboard.totalOrders')"
                :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3 mb-4">
        <div class="col-12 col-xl-6">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Flame :size="14" /> {{ t('admin.dashboard.topSellingChart') }}</div>
              <BarChart :data="topSellingChart" :empty-text="t('admin.dashboard.chartEmptyOrders')" />
            </div>
          </div>
        </div>
        <div class="col-12 col-xl-6">
          <div class="card border-secondary h-100" style="background:var(--bg-hover);">
            <div class="card-body">
              <div class="fw-semibold small text-secondary mb-3 d-flex align-items-center gap-1"><Turtle :size="14" /> {{ t('admin.dashboard.slowSellingChart') }}</div>
              <BarChart :data="slowSellingChart" :empty-text="t('admin.dashboard.chartEmptyProducts')" />
            </div>
          </div>
        </div>
      </div>

      <div class="small fw-semibold text-secondary mb-2 d-flex align-items-center gap-1"><Archive :size="14" /> {{ t('admin.dashboard.recentProducts') }}</div>
      <div class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th></th><th><Monitor :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colName') }}</th><th><Tag :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colBrand') }}</th><th><FolderOpen :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colCategory') }}</th><th><Banknote :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colPrice') }}</th><th><Bookmark :size="13" style="vertical-align:-2px;" /> {{ t('admin.dashboard.colStatus') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in products.slice(0,5)" :key="p.sanPhamId">
              <td style="width:48px;">
                <div class="rounded-2 d-flex align-items-center justify-content-center overflow-hidden"
                     style="width:38px;height:32px;background:var(--bg-card-inset);">
                  <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham"
                       style="width:100%;height:100%;object-fit:contain;padding:2px;" />
                  <span v-else><Laptop :size="16" color="var(--text-muted)" /></span>
                </div>
              </td>
              <td>{{ p.tenSanPham }}</td>
              <td>{{ p.tenThuongHieu }}</td>
              <td>{{ p.tenDanhMuc }}</td>
              <td>{{ formatPrice(p.giaBan) }}</td>
              <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
            </tr>
            <tr v-if="products.length===0"><td colspan="6" class="text-center text-secondary">{{ t('admin.dashboard.emptyProducts') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>
```

- [ ] **Step 2: Truyền 5 prop mới từ `AdminPage.vue`**

Trong `AdminPage.vue`, khối `<AdminDashboard ... />` (dòng 1111-1139) — thêm 5 dòng
`:prop="..."` mới ngay trước dòng `:products="products"` (dòng 1135):

```html
          :revenue-trend-chart="revenueTrendChart"
          :active-product-ratio="activeProductRatio"
          :weekly-revenue-chart="weeklyRevenueChart"
          :monthly-order-heat="monthlyOrderHeat"
          :kpi-radar-data="kpiRadarData"
          :staff-by-role="staffByRole"
          :products="products"
```

- [ ] **Step 3: Xoá import chết `GaugeChart` khỏi `AdminPage.vue`**

`AdminPage.vue` có sẵn dòng sau ở đầu file (trong khối import các component admin):
```js
import GaugeChart from "../components/common/GaugeChart.vue";
```
Import này vốn đã KHÔNG được dùng ở đâu trong template của chính `AdminPage.vue` (đã xác
minh bằng grep trước khi viết plan) — chỉ còn sót lại từ trước. Sau Step 1, `GaugeChart.vue`
bị xoá (Step 4) — nếu để nguyên dòng import này, app sẽ lỗi build (import 1 file không còn
tồn tại). Xoá nguyên dòng import này khỏi `AdminPage.vue`.

- [ ] **Step 4: Xoá `GaugeChart.vue`**

```bash
git rm FrontEnd/QLBanMayTinh/src/components/common/GaugeChart.vue
```

- [ ] **Step 5: Verify toàn diện**

`npm run dev`, vào Admin → tab Dashboard:
- Xác nhận hiện đủ 3 hàng mới theo đúng thứ tự: (1) stat + 3 ring + positions, (2) line
  doanh thu + radar, (3) ring sản phẩm đang bán + cột tuần + lịch tháng.
- Xác nhận các khối cũ vẫn còn nguyên bên dưới: cảnh báo sắp hết hàng (nếu có), 2 donut
  trạng thái đơn (hôm nay/tuần) với input ngày vẫn đổi được, top/slow bán chạy, bảng sản
  phẩm gần đây.
- Đổi theme dark ↔ light (nút topbar), kiểm tra không có chữ mất tương phản hoặc canvas
  vỡ layout ở block nào trong 3 hàng mới.
- Thu nhỏ cửa sổ trình duyệt xuống độ rộng mobile, xác nhận 3 hàng mới xếp chồng dọc hợp
  lý (không tràn ngang, không cột nào bị bóp quá nhỏ đọc không nổi).
- Mở tab "Nhân viên" trong sidebar, xác nhận danh sách nhân viên vẫn hiện đúng như
  trước (không bị ảnh hưởng bởi việc eager-load ở Task 8).
- Chạy `npm run lint` — không lỗi.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/AdminDashboard.vue FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(admin): restructure Dashboard into Joint-Payroll-style 3-row layout"
```
