# Tối ưu hiển thị biến thể ở màn Bán hàng tại quầy (POS) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gộp lưới sản phẩm trong `PosPanel.vue` (màn Bán hàng tại quầy) còn 1 card/sản
phẩm thay vì 1 card/biến thể, thêm modal chọn cấu hình/màu (theo đúng UX bên khách hàng)
trước khi vào modal "Chọn serial" sẵn có — không đổi bất kỳ luồng nghiệp vụ hay backend
nào đang có.

**Architecture:** Thuần frontend. Tách 1 hàm dùng chung (`utils/productGrouping.js`) cho
việc gộp biến thể, dùng lại ở cả `App.vue` (refactor, không đổi hành vi) và `PosPanel.vue`
(dùng mới). Thêm 1 modal mới inline trong `PosPanel.vue` (theo đúng convention 2 modal
sẵn có trong file này), copy logic chọn cấu hình/màu từ `ProductDetail.vue`.

**Tech Stack:** Vue 3 `<script setup>`, không có framework test frontend trong dự án này
(xác nhận: không có `vitest`/`jest` trong `package.json`) — verify bằng `npm run build`
(Vite build = kiểm tra cú pháp/type) + 1 lượt kiểm tra thủ công/Playwright cuối plan.

## Global Constraints

- Không đổi API/backend — 0 file trong `BackEnd/` bị đụng tới.
- Không sửa `posOpenSerialPicker`, `posSelectSerial`, `setSerialTrangThai`, modal "Chọn
  serial", `posStage`/cổng xác định khách hàng, giỏ hàng POS, giữ đơn, mã khuyến mãi,
  `posPlaceOrder` — chỉ được GỌI các hàm này, không sửa bên trong.
- Modal chọn cấu hình/màu **luôn mở** khi bấm "Thêm vào giỏ", kể cả sản phẩm chỉ có 1
  biến thể (quyết định UX đã chốt cùng người dùng — đổi lấy 1 click thêm để nhất quán).
- Tái dùng key i18n có sẵn thay vì tạo mới trùng nghĩa: `home.fromPrice`,
  `productDetail.versions`, `productDetail.colorHeading`, `productDetail.defaultConfig`,
  `common.close`. Chỉ thêm 2 key mới thực sự chưa có: `admin.pos.chooseVariant`,
  `admin.pos.continueToSerial` (cả 5 locale: vi/en/ja/ko/zh).
- Mọi file locale phải giữ cấu trúc song song (cùng key, cùng vị trí tương đối) — quy
  ước đã có của dự án.

---

### Task 1: Tách hàm gộp sản phẩm dùng chung + refactor App.vue

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/utils/productGrouping.js`
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue:276-283` (variantCountMap), `App.vue:344-360` (dedup trong filteredProducts)

**Interfaces:**
- Produces: `groupBySanPham(items: Array) => Array` (1 phần tử/sanPhamId, biến thể đại
  diện ưu tiên active rồi giá thấp nhất), `variantCountBySanPham(items: Array) =>
  Map<sanPhamId, number>`. Dùng lại ở Task 3.

- [ ] **Step 1: Tạo file `utils/productGrouping.js`**

```js
// Gộp danh sách biến thể phẳng (từ API /api/san-pham/hien-thi, 1 dòng/biến thể) theo
// sanPhamId — dùng chung cho lưới sản phẩm trang khách hàng (App.vue) và màn Bán hàng
// tại quầy (PosPanel.vue), tránh 2 nơi tự viết lại quy tắc chọn biến thể đại diện rồi
// lệch nhau (từng là nguồn gốc 1 bug: card hiện sai "Hết hàng").

// 1 phần tử / sanPhamId — ưu tiên biến thể còn hàng (active), rồi mới đến giá thấp nhất
// trong nhóm đó làm đại diện. Nếu chỉ so giá thấp nhất, 1 biến thể hết hàng trùng giá với
// biến thể còn hàng khác sẽ khiến cả card hiện "Hết hàng" dù sản phẩm vẫn mua được.
export const groupBySanPham = (items) => [
  ...items
    .reduce((map, p) => {
      const ex = map.get(p.sanPhamId);
      if (!ex) { map.set(p.sanPhamId, p); return map; }
      const pActive = p.trangThai === 'active';
      const exActive = ex.trangThai === 'active';
      if (pActive !== exActive ? pActive : Number(p.giaBan) < Number(ex.giaBan))
        map.set(p.sanPhamId, p);
      return map;
    }, new Map())
    .values(),
];

// sanPhamId → số biến thể trong `items` — dùng để quyết định hiện tiền tố "Từ" trên card
// (nhiều biến thể = giá đại diện chỉ là giá thấp nhất, không phải giá duy nhất).
export const variantCountBySanPham = (items) => {
  const map = new Map();
  items.forEach((p) => map.set(p.sanPhamId, (map.get(p.sanPhamId) || 0) + 1));
  return map;
};
```

- [ ] **Step 2: Import hàm mới trong `App.vue`**

Thêm vào khối import ở đầu file (cạnh các import util khác, ví dụ gần
`formatPrice`/tương tự — giữ nguyên style import hiện có của file):

```js
import { groupBySanPham, variantCountBySanPham } from "./utils/productGrouping.js";
```

- [ ] **Step 3: Thay `variantCountMap` bằng bản gọi hàm dùng chung**

Tìm đúng khối hiện có:

```js
// Map sanPhamId → số lượng biến thể (để card biết hiển thị "Từ X.XXXđ" hay không)
const variantCountMap = computed(() => {
  const map = new Map();
  products.value.forEach((p) =>
    map.set(p.sanPhamId, (map.get(p.sanPhamId) || 0) + 1),
  );
  return map;
});
```

Thay bằng:

```js
// Map sanPhamId → số lượng biến thể (để card biết hiển thị "Từ X.XXXđ" hay không)
const variantCountMap = computed(() => variantCountBySanPham(products.value));
```

- [ ] **Step 4: Thay khối dedup trong `filteredProducts` bằng bản gọi hàm dùng chung**

Tìm đúng khối hiện có:

```js
  // Deduplicate: 1 card / sanPhamId — ưu tiên biến thể còn hàng (active), rồi mới đến
  // giá thấp nhất trong nhóm đó làm đại diện. Nếu chọn đại diện chỉ theo giá thấp nhất,
  // 1 biến thể hết hàng trùng giá với biến thể còn hàng khác sẽ khiến cả card hiện
  // "Hết hàng" dù sản phẩm vẫn còn biến thể khác mua được.
  const deduped = [
    ...filtered
      .reduce((map, p) => {
        const ex = map.get(p.sanPhamId);
        if (!ex) { map.set(p.sanPhamId, p); return map; }
        const pActive = p.trangThai === 'active';
        const exActive = ex.trangThai === 'active';
        if (pActive !== exActive ? pActive : Number(p.giaBan) < Number(ex.giaBan))
          map.set(p.sanPhamId, p);
        return map;
      }, new Map())
      .values(),
  ];

  return deduped.sort((a, b) => {
```

Thay bằng:

```js
  // Deduplicate: 1 card / sanPhamId (quy tắc chọn đại diện — xem utils/productGrouping.js)
  const deduped = groupBySanPham(filtered);

  return deduped.sort((a, b) => {
```

- [ ] **Step 5: Build để xác nhận không lỗi**

Run: `npm run build` (trong `FrontEnd/QLBanMayTinh/`)
Expected: `✓ built` không có dòng lỗi. Đây là refactor thuần (behavior giữ nguyên) — nếu
build sạch, trang chủ khách hàng vẫn hoạt động y hệt trước.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/utils/productGrouping.js FrontEnd/QLBanMayTinh/src/App.vue
git commit -m "refactor(frontend): extract shared product-grouping util from App.vue"
```

---

### Task 2: Thêm 2 key i18n mới (5 locale)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`, `en.js`, `ja.js`, `ko.js`, `zh.js`

**Interfaces:**
- Produces: `admin.pos.chooseVariant`, `admin.pos.continueToSerial` — dùng ở Task 3.

- [ ] **Step 1: Thêm 2 key vào `vi.js`**

Tìm dòng (trong khối `admin.pos`):

```js
      needCustomerFirst: "Vui lòng xác định khách hàng (bên khung giỏ hàng) trước khi thêm sản phẩm",
    },
```

Thay bằng:

```js
      needCustomerFirst: "Vui lòng xác định khách hàng (bên khung giỏ hàng) trước khi thêm sản phẩm",
      chooseVariant: "Chọn cấu hình / màu sắc",
      continueToSerial: "Tiếp tục chọn serial →",
    },
```

- [ ] **Step 2: Thêm 2 key vào `en.js`**

Tìm dòng:

```js
      needCustomerFirst: "Please identify the customer (in the cart panel) before adding products",
    },
```

Thay bằng:

```js
      needCustomerFirst: "Please identify the customer (in the cart panel) before adding products",
      chooseVariant: "Choose configuration / color",
      continueToSerial: "Continue to serial →",
    },
```

- [ ] **Step 3: Thêm 2 key vào `ja.js`**

Tìm dòng:

```js
      needCustomerFirst: "商品を追加する前に、カート欄で顧客を確認してください",
    },
```

Thay bằng:

```js
      needCustomerFirst: "商品を追加する前に、カート欄で顧客を確認してください",
      chooseVariant: "構成・カラーを選択",
      continueToSerial: "シリアル選択へ進む →",
    },
```

- [ ] **Step 4: Thêm 2 key vào `ko.js`**

Tìm dòng:

```js
      needCustomerFirst: "상품을 추가하기 전에 장바구니 영역에서 고객을 먼저 확인해 주세요",
    },
```

Thay bằng:

```js
      needCustomerFirst: "상품을 추가하기 전에 장바구니 영역에서 고객을 먼저 확인해 주세요",
      chooseVariant: "구성 / 색상 선택",
      continueToSerial: "시리얼 선택으로 계속 →",
    },
```

- [ ] **Step 5: Thêm 2 key vào `zh.js`**

Tìm dòng:

```js
      needCustomerFirst: "请先在购物车区域确认客户，再添加商品",
    },
```

Thay bằng:

```js
      needCustomerFirst: "请先在购物车区域确认客户，再添加商品",
      chooseVariant: "选择配置 / 颜色",
      continueToSerial: "继续选择序列号 →",
    },
```

- [ ] **Step 6: Build để xác nhận không lỗi cú pháp**

Run: `npm run build`
Expected: `✓ built`

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/*.js
git commit -m "feat(i18n): add POS variant-picker strings (5 locales)"
```

---

### Task 3: `PosPanel.vue` — lưới sản phẩm gộp + modal chọn cấu hình/màu

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

**Interfaces:**
- Consumes: `groupBySanPham`, `variantCountBySanPham` (Task 1); `admin.pos.chooseVariant`,
  `admin.pos.continueToSerial` (Task 2); `posStage`, `posError`, `posStartInvoice`,
  `posOpenSerialPicker` (đã có sẵn trong file, không đổi).
- Produces: `posProductGroups`, `posVariantCountMap`, `posOpenVariantPicker(p)` — điểm
  gọi mới thay cho `posOpenSerialPicker(p)` trực tiếp từ nút "Thêm vào giỏ".

- [ ] **Step 1: Import hàm gộp dùng chung**

Thêm vào khối import ở đầu file:

```js
import { groupBySanPham, variantCountBySanPham } from "../../utils/productGrouping.js";
```

- [ ] **Step 2: Thêm computed gộp sản phẩm — ngay sau `posProducts`**

Tìm đúng khối hiện có (kết thúc `posProducts`):

```js
const posProducts = computed(() => {
  const q = posSearch.value.toLowerCase();
  return ProductsStore.items.filter(
    (p) =>
      p.trangThai === "active" &&
      (!q ||
        p.tenSanPham.toLowerCase().includes(q) ||
        (p.maSku ?? "").toLowerCase().includes(q)),
  );
});
```

Thêm ngay sau (giữ nguyên `posProducts` như cũ):

```js
// Gộp posProducts (đã lọc active + tìm kiếm) còn 1 card/sản phẩm — dùng cho lưới hiển
// thị. posProducts (biến thể phẳng) vẫn giữ nguyên, dùng làm pool cho modal chọn cấu
// hình/màu bên dưới (Bước "Chọn cấu hình/màu").
const posProductGroups = computed(() => groupBySanPham(posProducts.value));
const posVariantCountMap = computed(() => variantCountBySanPham(posProducts.value));
```

- [ ] **Step 3: Thêm state + logic modal chọn cấu hình/màu**

Chèn khối mới NGAY TRƯỚC comment `// Ban tai quay bat buoc chon serial cu the...` (đầu
phần serial-picker hiện có):

```js
// ── Chon cau hinh/mau truoc khi vao modal chon serial ─────────────────────────
// Luon mo modal nay khi bam "Them vao gio", ke ca san pham chi co 1 bien the — dong
// nhat trai nghiem cho moi truong hop (khac voi trang khach hang, von bo qua buoc nay
// neu chi co 1 lua chon — xem App.vue handleQuickAdd). Bam "Tiep tuc chon serial" se
// goi thang posOpenSerialPicker() hien co, khong doi gi ben trong ham do.
const showVariantPicker = ref(false);
const variantPickerBase = ref(null); // san pham dai dien vua bam (tu posProductGroups)
const variantPickerActiveConfigKey = ref('');
const variantPickerActiveColor = ref('');

const variantConfigKey = (v) => `${v.cpu ?? ''}|${v.ram ?? ''}|${v.oCung ?? ''}`;

// Toan bo bien the cung sanPhamId, lay tu pool da loc active + tim kiem hien co
// (posProducts) — POS khong bao gio cho chon 1 cau hinh da het hang, dung y het hanh
// vi loc "active" dang co truoc khi co thay doi nay.
const variantPickerVariants = computed(() =>
  posProducts.value.filter((v) => v.sanPhamId === variantPickerBase.value?.sanPhamId),
);

// Cau hinh duy nhat (deduplicate theo cpu+ram+oCung) — copy logic tu ProductDetail.vue
const variantPickerConfigs = computed(() => {
  const seen = new Set();
  return variantPickerVariants.value.filter((v) => {
    const k = variantConfigKey(v);
    if (seen.has(k)) return false;
    seen.add(k); return true;
  });
});

// Mau sac cua cau hinh dang chon (deduplicate theo mauSac)
const variantPickerColorsForConfig = computed(() => {
  const seen = new Set();
  return variantPickerVariants.value
    .filter((v) => variantConfigKey(v) === variantPickerActiveConfigKey.value)
    .filter((v) => {
      const c = v.mauSac ?? '';
      if (seen.has(c)) return false;
      seen.add(c); return true;
    });
});

// Bien the hien tai = giao cua cau hinh + mau da chon
const variantPickerActiveVariant = computed(() =>
  variantPickerVariants.value.find((v) =>
    variantConfigKey(v) === variantPickerActiveConfigKey.value &&
    (v.mauSac ?? '') === variantPickerActiveColor.value,
  ) ?? variantPickerBase.value,
);

const variantPickerSelectConfig = (v) => {
  variantPickerActiveConfigKey.value = variantConfigKey(v);
  const available = variantPickerVariants.value.filter(
    (vv) => variantConfigKey(vv) === variantPickerActiveConfigKey.value,
  );
  if (!available.find((vv) => (vv.mauSac ?? '') === variantPickerActiveColor.value))
    variantPickerActiveColor.value = available[0]?.mauSac ?? '';
};
const variantPickerSelectColor = (v) => { variantPickerActiveColor.value = v.mauSac ?? ''; };

// Nhan 2 dong cho nut cau hinh — copy tu ProductDetail.vue
const variantPickerConfigLabel = (v) => ({
  line1: v.cpu || v.ram || t('productDetail.defaultConfig'),
  line2: [v.ram, v.oCung].filter(Boolean).join(' · '),
});

// Mau dot cho color swatch — copy nguyen bang mau tu ProductDetail.vue
const variantPickerColorDot = (mauSac) => {
  if (!mauSac) return '#555';
  const s = mauSac.toLowerCase();
  const map = [
    ['đen', '#18181b'], ['den', '#18181b'],
    ['trắng', '#e4e4e7'], ['trang', '#e4e4e7'],
    ['bạc', '#94a3b8'], ['bac', '#94a3b8'],
    ['xám', '#6b7280'], ['xam', '#6b7280'],
    ['đỏ', '#dc2626'], ['do', '#dc2626'],
    ['xanh lá', '#16a34a'], ['xanh la', '#16a34a'],
    ['xanh dương', '#2563eb'], ['xanh duong', '#2563eb'],
    ['xanh', '#2563eb'],
    ['vàng', '#ca8a04'], ['vang', '#ca8a04'],
    ['hồng', '#ec4899'], ['hong', '#ec4899'],
    ['tím', '#9333ea'], ['tim', '#9333ea'],
    ['cam', '#ea580c'],
    ['nâu', '#92400e'], ['nau', '#92400e'],
  ];
  const found = map.find(([k]) => s.includes(k));
  return found ? found[1] : '#555';
};

// Mo modal chon cau hinh/mau — thay the diem goi cu tu nut "Them vao gio" tren card san
// pham. Giu nguyen dung guard dang co o dau posOpenSerialPicker (chan neu chua xac dinh
// khach hang), copy nguyen khong doi.
const posOpenVariantPicker = (p) => {
  if (posStage.value !== 'selling') {
    if (posStage.value === 'start') posStartInvoice();
    posError.value = t('admin.pos.needCustomerFirst');
    return;
  }
  variantPickerBase.value = p;
  variantPickerActiveConfigKey.value = variantConfigKey(p);
  variantPickerActiveColor.value = p.mauSac ?? '';
  showVariantPicker.value = true;
};

// Chot bien the da chon -> dong modal nay, mo modal chon serial hien co (khong sua gi
// ben trong posOpenSerialPicker).
const posConfirmVariant = () => {
  showVariantPicker.value = false;
  posOpenSerialPicker(variantPickerActiveVariant.value);
};

```

(Dòng trống cuối khối trên là chủ đích, ngăn cách với comment `// Ban tai quay...` theo
sau — giữ nguyên toàn bộ phần serial-picker hiện có, không sửa gì.)

- [ ] **Step 4: Sửa template lưới sản phẩm — gộp card + thêm tiền tố "Từ" + đổi click handler**

Tìm đúng khối hiện có:

```html
      <div v-else class="row g-2 overflow-y-auto">
        <div v-for="p in posProducts" :key="p.bienTheId" class="col-6 col-xl-4">
          <div class="card h-100 border-secondary" style="background:var(--bg-hover);">
            <div class="d-flex align-items-center justify-content-center" style="height:88px;background:var(--bg-card-inset);">
              <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:6px;" />
              <span v-else style="font-size:1.8rem;">💻</span>
            </div>
            <div class="card-body p-2 d-flex flex-column gap-1">
              <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
              <div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
              <div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
              <div class="fw-bold text-warning" style="font-size:0.95rem;">{{ formatPrice(p.giaBan) }}</div>
              <button class="btn btn-sm btn-warning text-dark fw-bold mt-auto" @click="posOpenSerialPicker(p)">{{ t('admin.pos.addToCart') }}</button>
            </div>
          </div>
        </div>
        <div v-if="posProducts.length===0" class="col-12 text-center text-secondary small py-4">{{ t('admin.pos.noProductsFound') }}</div>
      </div>
```

Thay bằng:

```html
      <div v-else class="row g-2 overflow-y-auto">
        <div v-for="p in posProductGroups" :key="p.sanPhamId" class="col-6 col-xl-4">
          <div class="card h-100 border-secondary" style="background:var(--bg-hover);">
            <div class="d-flex align-items-center justify-content-center" style="height:88px;background:var(--bg-card-inset);">
              <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" style="width:100%;height:100%;object-fit:contain;padding:6px;" />
              <span v-else style="font-size:1.8rem;">💻</span>
            </div>
            <div class="card-body p-2 d-flex flex-column gap-1">
              <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
              <div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
              <div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
              <div class="fw-bold text-warning" style="font-size:0.95rem;">
                <span v-if="(posVariantCountMap.get(p.sanPhamId) || 0) > 1" class="fw-normal" style="font-size:0.7rem;color:var(--text-secondary);">{{ t('home.fromPrice') }} </span>{{ formatPrice(p.giaBan) }}
              </div>
              <button class="btn btn-sm btn-warning text-dark fw-bold mt-auto" @click="posOpenVariantPicker(p)">{{ t('admin.pos.addToCart') }}</button>
            </div>
          </div>
        </div>
        <div v-if="posProductGroups.length===0" class="col-12 text-center text-secondary small py-4">{{ t('admin.pos.noProductsFound') }}</div>
      </div>
```

- [ ] **Step 5: Thêm modal chọn cấu hình/màu vào template**

Chèn khối mới NGAY TRƯỚC `<!-- ══ MODAL CHON SERIAL (POS) ══ -->` (giữ modal serial y
hệt như cũ, không sửa):

```html
  <!-- ══ MODAL CHON CAU HINH/MAU (POS) ══ -->
  <div v-if="showVariantPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showVariantPicker=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:80vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <div>
          <div>{{ t('admin.pos.chooseVariant') }}</div>
          <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ variantPickerBase?.tenSanPham }} — {{ variantPickerBase?.maSku }}</div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showVariantPicker=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-3">
        <div class="fw-bold text-warning" style="font-size:1.1rem;">{{ formatPrice(variantPickerActiveVariant?.giaBan) }}</div>

        <div v-if="variantPickerConfigs.length > 1">
          <div class="fw-semibold mb-2" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-secondary);">
            {{ t('productDetail.versions', { count: variantPickerConfigs.length }) }}
          </div>
          <div class="d-flex flex-wrap gap-2">
            <button v-for="v in variantPickerConfigs" :key="variantConfigKey(v)"
                    class="btn btn-sm d-flex flex-column align-items-start text-start px-3 py-2"
                    style="border-radius:10px;"
                    :style="variantPickerActiveConfigKey === variantConfigKey(v)
                      ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                      : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
                    @click="variantPickerSelectConfig(v)">
              <span class="fw-semibold" style="font-size:11px;line-height:1.5;">{{ variantPickerConfigLabel(v).line1 }}</span>
              <span v-if="variantPickerConfigLabel(v).line2" style="font-size:10px;opacity:0.75;">{{ variantPickerConfigLabel(v).line2 }}</span>
            </button>
          </div>
        </div>

        <div v-if="variantPickerColorsForConfig.some(v => v.mauSac)">
          <div class="fw-semibold mb-2" style="font-size:0.72rem;text-transform:uppercase;letter-spacing:0.06em;color:var(--text-secondary);">
            {{ t('productDetail.colorHeading') }}
          </div>
          <div class="d-flex flex-wrap gap-2">
            <button v-for="v in variantPickerColorsForConfig" :key="v.bienTheId"
                    class="btn btn-sm d-flex align-items-center gap-2 px-3 py-2"
                    style="border-radius:10px;"
                    :style="variantPickerActiveColor === v.mauSac
                      ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                      : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
                    @click="variantPickerSelectColor(v)">
              <span class="rounded-circle flex-shrink-0" :style="`width:13px;height:13px;background:${variantPickerColorDot(v.mauSac)};border:1.5px solid #666;display:inline-block;`"></span>
              <div class="d-flex flex-column align-items-start text-start">
                <span class="fw-semibold" style="font-size:11px;line-height:1.3;">{{ v.mauSac }}</span>
                <span style="font-size:10px;color:var(--accent-fg);">{{ formatPrice(v.giaBan) }}</span>
              </div>
            </button>
          </div>
        </div>

        <button class="btn btn-warning text-dark fw-bold mt-2" @click="posConfirmVariant">{{ t('admin.pos.continueToSerial') }}</button>
      </div>
    </div>
  </div>

```

- [ ] **Step 6: Build để xác nhận không lỗi**

Run: `npm run build`
Expected: `✓ built` không lỗi.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue
git commit -m "feat(frontend): group POS product grid by product, add variant picker before serial picker"
```

---

### Task 4: Kiểm tra sống end-to-end (Playwright)

**Files:** không có (chỉ verify, không sửa code)

- [ ] **Step 1: Đăng nhập admin hoặc staff, mở màn "Bán hàng"**

- [ ] **Step 2: Xác nhận lưới sản phẩm đã gộp**

Kiểm tra 1 sản phẩm có nhiều biến thể (ví dụ "Acer Aspire 5 A515-58", 4 biến thể trong
seed data) chỉ hiện **đúng 1 card**, có tiền tố "Từ" trước giá.

- [ ] **Step 3: Xác nhận cổng khách hàng vẫn hoạt động y hệt cũ**

Bấm "Thêm vào giỏ" khi chưa "Tạo hóa đơn"/chưa xác định khách hàng → vẫn thấy đúng
hành vi cũ (tự mở bước "Tạo hóa đơn"/nhập SĐT + báo lỗi `needCustomerFirst`), KHÔNG mở
modal chọn cấu hình/màu trong tình huống này (đúng vì guard chạy trước, giữ nguyên).

- [ ] **Step 4: Sau khi đã xác định khách hàng — bấm "Thêm vào giỏ" trên sản phẩm nhiều biến thể**

Xác nhận modal "Chọn cấu hình / màu sắc" mở, hiện đúng tên + SKU sản phẩm, các nút cấu
hình/màu bấm được, giá cập nhật đúng theo lựa chọn.

- [ ] **Step 5: Bấm "Tiếp tục chọn serial"**

Xác nhận modal "Chọn serial (IMEI)" mở tiếp theo (y hệt UI cũ), danh sách serial đúng
với BIẾN THỂ đã chọn ở bước trước (không phải biến thể đại diện ban đầu).

- [ ] **Step 6: Chọn 1 serial, xác nhận dòng được thêm đúng vào giỏ POS**

- [ ] **Step 7: Thử sản phẩm chỉ có 1 biến thể**

Xác nhận modal "Chọn cấu hình / màu sắc" vẫn mở (theo quyết định "luôn mở"), không có
phần cấu hình/màu (vì `configs.length` không > 1 và không có màu để so), chỉ có giá +
nút "Tiếp tục chọn serial" — bấm vào vẫn mở đúng modal chọn serial của đúng biến thể đó.

- [ ] **Step 8: Hoàn tất tạo đơn**

Đi hết luồng (giỏ hàng → tạo đơn) như bình thường, xác nhận đơn được tạo thành công,
không có gì khác biệt so với trước khi có thay đổi này (đúng yêu cầu "không đổi luồng
hoạt động vốn có").

- [ ] **Step 9: Hồi quy nhanh trang khách hàng (do Task 1 refactor App.vue)**

Mở trang chủ khách hàng, xác nhận lưới sản phẩm vẫn gộp đúng như trước (không hiện
trùng card), badge "Còn hàng/Hết hàng" vẫn đúng, mở 1 sản phẩm vẫn vào đúng
`ProductDetail.vue` với bộ chọn cấu hình/màu hoạt động bình thường.
