# Cải thiện POS (sub-project 1/6 từ note) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thực hiện 6 cải thiện cho màn Bán hàng tại quầy (`PosPanel.vue`) theo spec `docs/superpowers/specs/2026-08-12-pos-cai-thien-tu-note.md`.

**Architecture:** Sửa thuần frontend (Vue), không đổi API/DB — trừ Task 4 chỉ đổi payload gửi lên (dùng đúng field/state machine backend đã có sẵn, không sửa backend). Không có test tự động frontend component-level trong dự án này (xem `__tests__/` — chỉ có utils/services/stores) nên verify bằng build + thao tác tay qua trình duyệt (Docker), đúng pattern đã dùng xuyên suốt các plan trước.

**Tech Stack:** Vue 3 `<script setup>`, `@lucide/vue` icon, i18n thuần object literal (`vi.js`/`en.js`).

## Global Constraints

- Không đụng `CheckoutModal.vue` (luồng online) — chỉ tham khảo cách nó tạo QR (VietQR API) để tái dùng bên POS.
- Không đụng backend — mọi state machine/endpoint cần dùng đã có sẵn (xem spec).
- Việc #7 trong note (ưu tiên giữ hàng tại quầy) đã xác nhận hoạt động đúng sẵn — **không** nằm trong plan này.
- File chính bị sửa: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue` (908 dòng, hiện có) — mọi số dòng trích dẫn dưới đây là số dòng TRƯỚC khi task đó chạy; số dòng sẽ lệch dần qua từng task, dùng nội dung text để định vị (Edit theo chuỗi, không theo số dòng).

---

### Task 1: Bỏ hiển thị SKU + danh mục trên card sản phẩm

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue` (khối card lưới sản phẩm, gần dòng 585-591)

**Interfaces:** Không có — thuần xóa 2 dòng template, không đổi biến/hàm nào.

- [ ] **Step 1: Xóa 2 dòng hiển thị SKU và thương hiệu/danh mục**

Tìm khối:
```html
              <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
              <div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
              <div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
              <div class="fw-bold text-warning" style="font-size:0.95rem;">
```
Sửa thành:
```html
              <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
              <div class="fw-bold text-warning" style="font-size:0.95rem;">
```

- [ ] **Step 2: Build frontend, xác nhận không lỗi**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: `✓ built` không có dòng error.

- [ ] **Step 3: Verify tay qua trình duyệt**

Mở `/#/admin` → tab "Bán hàng" → xác nhận card sản phẩm chỉ còn tên + giá, không còn SKU/thương hiệu/danh mục.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue
git commit -m "refactor(pos): bỏ hiển thị SKU và danh mục trên card sản phẩm"
```

---

### Task 2: Tìm không phân biệt dấu tiếng Việt

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/utils/adminFormat.js` (thêm hàm mới)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue` (dòng 40-49, `posProducts` computed)

**Interfaces:**
- Produces: `boDauTiengViet(str: string): string` — export mới trong `adminFormat.js`, dùng lại được ở các sub-project sau nếu cần (không dùng ở task khác trong plan này).

- [ ] **Step 1: Thêm hàm `boDauTiengViet` vào `adminFormat.js`**

Thêm vào cuối file (sau `toLocalDT`):
```javascript

// Bỏ dấu tiếng Việt để so khớp tìm kiếm không phân biệt dấu (gõ "laptop dell"
// vẫn khớp "Laptop Dell") — dùng NFD tách dấu khỏi ký tự gốc rồi xóa combining marks,
// xử lý riêng đ/Đ vì Unicode không tách nó qua NFD như các ký tự có dấu khác.
export const boDauTiengViet = (str) =>
  (str ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase();
```

- [ ] **Step 2: Dùng hàm này trong `posProducts` computed của `PosPanel.vue`**

Tìm khối (dòng 40-49):
```javascript
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
Sửa thành:
```javascript
const posProducts = computed(() => {
  const q = boDauTiengViet(posSearch.value.toLowerCase());
  return ProductsStore.items.filter(
    (p) =>
      p.trangThai === "active" &&
      (!q ||
        boDauTiengViet(p.tenSanPham).includes(q) ||
        boDauTiengViet(p.maSku ?? "").includes(q)),
  );
});
```

- [ ] **Step 3: Thêm import `boDauTiengViet` vào đầu `PosPanel.vue`**

Tìm dòng:
```javascript
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
```
Sửa thành:
```javascript
import { formatPrice, formatDate, boDauTiengViet } from "../../utils/adminFormat.js";
```

- [ ] **Step 4: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: `✓ built`, không lỗi import.

- [ ] **Step 5: Verify tay qua trình duyệt**

Gõ "dell" hoặc "may tinh" (không dấu) vào ô tìm — xác nhận vẫn lọc đúng sản phẩm có dấu tiếng Việt trong tên.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/utils/adminFormat.js FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue
git commit -m "feat(pos): tìm sản phẩm không phân biệt dấu tiếng Việt"
```

---

### Task 3: Chọn nhiều serial cùng lúc

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

**Interfaces:**
- Consumes: `posCart` (ref array, đã có), `setSerialTrangThai(item, trangThai)` (đã có, dòng 334-341), `serialPickerProduct`/`serialPickerList`/`serialPickerSwapChiTietId` (đã có).
- Produces: `serialPickerChosenIds` (ref Set<number>) — reset mỗi lần mở picker; dùng bởi template modal chọn serial.

- [ ] **Step 1: Thêm state `serialPickerChosenIds` và reset nó khi mở picker**

Tìm dòng (gần dòng 308):
```javascript
const serialPickerSwapChiTietId = ref(null);
```
Thêm ngay sau:
```javascript
// Cac serial da duoc tick chon trong lan mo picker nay (chi dung khi them-moi, khong
// dung khi doi-serial vi luong doi van la 1-doi-1).
const serialPickerChosenIds = ref(new Set());
```

Tìm hàm `posOpenSerialPicker` (dòng 310-329), thêm dòng reset ngay sau `serialPickerSwapChiTietId.value = swapChiTietId;`:
```javascript
  serialPickerProduct.value = p;
  serialPickerSwapChiTietId.value = swapChiTietId;
  serialPickerChosenIds.value = new Set();
```

- [ ] **Step 2: Thêm hàm toggle chọn/bỏ chọn serial**

Thêm ngay sau hàm `posOpenSerialPicker` (trước `setSerialTrangThai`):
```javascript
const posToggleSerial = (serial) => {
  const next = new Set(serialPickerChosenIds.value);
  if (next.has(serial.chiTietId)) next.delete(serial.chiTietId);
  else next.add(serial.chiTietId);
  serialPickerChosenIds.value = next;
};
```

- [ ] **Step 3: Sửa `posSelectSerial` thành nhận nhiều serial cùng lúc**

Tìm hàm hiện có (dòng 343-368):
```javascript
const posSelectSerial = async (serial) => {
  const p = serialPickerProduct.value;
  const item = {
    sanPhamId: p.sanPhamId,
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    maSku: p.maSku,
    giaBan: p.giaBan,
    hinhAnhChinh: p.hinhAnhChinh,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  };
  const swapId = serialPickerSwapChiTietId.value;
  const oldItem = swapId != null ? posCart.value.find((i) => i.chiTietId === swapId) : null;
  posCart.value = swapId != null
    ? posCart.value.map((i) => (i.chiTietId === swapId ? item : i))
    : [...posCart.value, item];
  showSerialPicker.value = false;
  serialPickerSwapChiTietId.value = null;
  // Danh dau giu ngay khi chon — de phien POS khac (hoac don khac) khong the chon trung
  // serial nay, ke ca khi don nay chua duoc "giu don" chinh thuc.
  await setSerialTrangThai(item, 'giu_hang');
  if (oldItem) await setSerialTrangThai(oldItem, 'trong_kho');
};
```
Giữ nguyên hàm này (dùng cho luồng đổi-serial 1-đổi-1, gọi trực tiếp khi bấm 1 serial trong chế độ swap). Thêm hàm mới ngay sau nó, dùng cho luồng thêm-mới nhiều-serial:
```javascript
// Them nhieu serial cung luc vao gio — chi dung khi KHONG phai doi-serial (swapChiTietId
// null). Moi serial da chon tao 1 dong rieng trong posCart, giong het cau truc item cua
// posSelectSerial(), roi danh dau giu_hang tung cai.
const posAddChosenSerials = async () => {
  const p = serialPickerProduct.value;
  const chosen = serialPickerList.value.filter((s) => serialPickerChosenIds.value.has(s.chiTietId));
  const items = chosen.map((serial) => ({
    sanPhamId: p.sanPhamId,
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    maSku: p.maSku,
    giaBan: p.giaBan,
    hinhAnhChinh: p.hinhAnhChinh,
    chiTietId: serial.chiTietId,
    soSerial: serial.soSerial,
    ngayNhapKho: serial.ngayNhapKho,
    soLuong: 1,
  }));
  posCart.value = [...posCart.value, ...items];
  showSerialPicker.value = false;
  await Promise.all(items.map((item) => setSerialTrangThai(item, 'giu_hang')));
};
```

- [ ] **Step 4: Sửa template modal chọn serial cho chế độ chọn-nhiều**

Tìm khối template (dòng 834-858):
```html
  <!-- ══ MODAL CHON SERIAL (POS) ══ -->
  <div v-if="showSerialPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showSerialPicker=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:75vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <div>
          <div>{{ t('admin.pos.chooseSerial') }}</div>
          <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ serialPickerProduct?.tenSanPham }} — {{ serialPickerProduct?.maSku }}</div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showSerialPicker=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div v-if="serialPickerLoading" class="text-secondary small text-center py-4">{{ t('admin.pos.loading') }}</div>
        <div v-else-if="serialPickerList.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noSerialAvailable') }}</div>
        <button
          v-for="s in serialPickerList" v-else :key="s.chiTietId"
          class="btn btn-outline-warning d-flex justify-content-between align-items-center"
          style="font-family:monospace;font-size:0.85rem;"
          @click="posSelectSerial(s)"
        >
          <span>{{ s.soSerial }}</span>
          <span class="text-secondary" style="font-size:0.7rem;">{{ formatDate(s.ngayNhapKho) }}</span>
        </button>
      </div>
    </div>
  </div>
```
Sửa thành (chế độ swap giữ hành vi cũ — bấm là chọn ngay; chế độ thêm-mới chuyển sang tick-chọn + nút xác nhận):
```html
  <!-- ══ MODAL CHON SERIAL (POS) ══ -->
  <div v-if="showSerialPicker" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showSerialPicker=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:75vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <div>
          <div>{{ t('admin.pos.chooseSerial') }}</div>
          <div class="text-secondary fw-normal" style="font-size:0.75rem;">{{ serialPickerProduct?.tenSanPham }} — {{ serialPickerProduct?.maSku }}</div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showSerialPicker=false"></button>
      </div>
      <div class="overflow-y-auto p-3 d-flex flex-column gap-2">
        <div v-if="serialPickerLoading" class="text-secondary small text-center py-4">{{ t('admin.pos.loading') }}</div>
        <div v-else-if="serialPickerList.length===0" class="text-secondary small text-center py-4">{{ t('admin.pos.noSerialAvailable') }}</div>
        <button
          v-for="s in serialPickerList" v-else :key="s.chiTietId"
          class="btn d-flex justify-content-between align-items-center"
          :class="serialPickerSwapChiTietId == null && serialPickerChosenIds.has(s.chiTietId) ? 'btn-warning text-dark' : 'btn-outline-warning'"
          style="font-family:monospace;font-size:0.85rem;"
          @click="serialPickerSwapChiTietId != null ? posSelectSerial(s) : posToggleSerial(s)"
        >
          <span>{{ s.soSerial }}</span>
          <span class="text-secondary" style="font-size:0.7rem;">{{ formatDate(s.ngayNhapKho) }}</span>
        </button>
      </div>
      <div v-if="serialPickerSwapChiTietId == null" class="p-3 border-top border-secondary">
        <button
          class="btn btn-warning text-dark fw-bold w-100" :disabled="serialPickerChosenIds.size === 0"
          @click="posAddChosenSerials"
        >
          {{ t('admin.pos.addChosenSerials', { count: serialPickerChosenIds.size }) }}
        </button>
      </div>
    </div>
  </div>
```

- [ ] **Step 5: Thêm i18n key `admin.pos.addChosenSerials`**

Trong `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`, tìm key `chooseSerial:` trong namespace `pos` (tìm bằng `grep -n "chooseSerial:" FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`), thêm ngay sau dòng đó:
```javascript
      addChosenSerials: "Thêm {count} máy vào giỏ",
```
Trong `en.js`, tìm đúng vị trí tương ứng (`chooseSerial:`), thêm:
```javascript
      addChosenSerials: "Add {count} unit(s) to cart",
```

- [ ] **Step 6: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: `✓ built`.

- [ ] **Step 7: Verify tay qua trình duyệt**

Bán tại quầy → thêm 1 sản phẩm có ≥3 serial còn hàng → mở modal chọn serial → tick chọn 3 serial → bấm "Thêm 3 máy vào giỏ" → xác nhận cả 3 dòng lên giỏ hàng đúng, mỗi dòng đúng serial đã chọn. Thử luồng đổi-serial (nút 🔄 trên 1 dòng đã có sẵn trong giỏ) — xác nhận vẫn là bấm-là-đổi-ngay như cũ (không có nút xác nhận riêng).

- [ ] **Step 8: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(pos): cho chọn nhiều serial cùng lúc khi thêm vào giỏ"
```

---

### Task 4: Giao tận nơi tại quầy (thay phí vận chuyển vô nghĩa)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

**Interfaces:**
- Produces: `posDeliveryMode` (ref: `'pickup' | 'delivery'`, mặc định `'pickup'`), `posDeliveryAddress` (ref string) — dùng trong `posPlaceOrder`.
- Consumes: `posFee` (đã có, dòng 90), `DonHangService.create`/`DonHangService.update` (đã có).

- [ ] **Step 1: Thêm state `posDeliveryMode` và `posDeliveryAddress`**

Tìm dòng:
```javascript
const posPaymentMethod = ref(null); // 1 trong POS_PAYMENT_METHODS — bat buoc chon truoc khi tao don
```
Thêm ngay sau:
```javascript
// 'pickup' (mac dinh, khach tu lay tai quay — khong tinh phi/dia chi) hoac 'delivery'
// (giao tan noi — tinh phi nhu online, don dung o "confirmed" thay vi nhay thang "delivered").
const posDeliveryMode = ref('pickup');
const posDeliveryAddress = ref('');
```

- [ ] **Step 2: Ẩn `posFee` khi `pickup`, dùng trong tổng tiền**

Tìm dòng (gần dòng 90):
```javascript
const posFee = computed(() => (posCartTotal.value >= 300000 ? 0 : 30000));
```
Sửa thành:
```javascript
const posFee = computed(() => {
  if (posDeliveryMode.value !== 'delivery') return 0;
  return posCartTotal.value >= 300000 ? 0 : 30000;
});
```
(Không cần đổi `posGrandTotal` — nó đã cộng `posFee.value` sẵn, tự động về 0 khi pickup.)

- [ ] **Step 3: Reset `posDeliveryMode`/`posDeliveryAddress` ở mọi nơi reset form**

Có 3 chỗ reset state (đã đọc toàn file): `posHoldOrder` (dòng ~180-190), `posReset` (dòng ~386-399), và cuối `posPlaceOrder` khi thành công (dòng ~555-559). Thêm 2 dòng sau vào cả 3 chỗ, ngay sau dòng `posPaymentMethod.value = null;` (hoặc `posPaymentMethod = null;` — copy đúng theo từng hàm):
```javascript
  posDeliveryMode.value = 'pickup';
  posDeliveryAddress.value = '';
```

- [ ] **Step 4: Thêm toggle UI trong khung giỏ hàng, trước phần "Tổng tiền"**

Tìm khối (dòng 728-734):
```html
        <!-- Tong tien -->
        <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
          <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
          <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
          <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.shippingFeeLabel') }}</span><span>{{ posFee===0?t('admin.pos.free'):formatPrice(posFee) }}</span></div>
          <div class="d-flex justify-content-between fw-bold"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
        </div>
```
Sửa thành:
```html
        <!-- Giao hang -->
        <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
          <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">{{ t('admin.pos.deliveryModeLabel') }}</div>
          <div class="d-flex gap-1">
            <button
              class="btn btn-sm flex-fill" style="font-size:0.75rem;"
              :class="posDeliveryMode==='pickup' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
              @click="posDeliveryMode='pickup'"
            >{{ t('admin.pos.pickupAtStore') }}</button>
            <button
              class="btn btn-sm flex-fill" style="font-size:0.75rem;"
              :class="posDeliveryMode==='delivery' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
              @click="posDeliveryMode='delivery'"
            >{{ t('admin.pos.deliverToAddress') }}</button>
          </div>
          <input
            v-if="posDeliveryMode==='delivery'" v-model="posDeliveryAddress" class="form-control form-control-sm"
            style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);"
            :placeholder="t('admin.pos.deliveryAddressPlaceholder')"
          />
        </div>
        <!-- Tong tien -->
        <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
          <div class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.subtotalLabel') }}</span><span>{{ formatPrice(posCartTotal) }}</span></div>
          <div v-if="posGiamGia > 0" class="d-flex justify-content-between text-success small"><span>{{ t('checkout.discount') }}</span><span>-{{ formatPrice(posGiamGia) }}</span></div>
          <div v-if="posDeliveryMode==='delivery'" class="d-flex justify-content-between text-secondary small"><span>{{ t('admin.pos.shippingFeeLabel') }}</span><span>{{ posFee===0?t('admin.pos.free'):formatPrice(posFee) }}</span></div>
          <div class="d-flex justify-content-between fw-bold"><span>{{ t('admin.pos.totalLabel') }}</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
        </div>
```

- [ ] **Step 5: Sửa `posPlaceOrder` — dùng địa chỉ giao khi delivery, không nhảy thẳng "delivered" khi delivery**

Tìm khối (dòng 484-547 — chỉ những dòng cần đổi, giữ nguyên phần còn lại):
```javascript
    const orderRes = await DonHangService.create({
      khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
      diaChiGiaoHangText: posFoundCust.value.diaChi ?? "Tai cua hang",
      khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
      tongTien: posCartTotal.value, giamGia: posGiamGia.value,
      phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
      ngayDat,
      trangThaiDonHang: "confirmed", trangThaiThanhToan: "paid", kenhBan: "in_store",
    });
```
Sửa thành:
```javascript
    const diaChiGiao = posDeliveryMode.value === 'delivery'
      ? posDeliveryAddress.value.trim()
      : (posFoundCust.value.diaChi ?? "Tai cua hang");
    const orderRes = await DonHangService.create({
      khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
      diaChiGiaoHangText: diaChiGiao,
      khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
      tongTien: posCartTotal.value, giamGia: posGiamGia.value,
      phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
      ngayDat,
      trangThaiDonHang: "confirmed", trangThaiThanhToan: "paid", kenhBan: "in_store",
    });
```
Và tìm khối `finalizeRes` ngay sau (dòng 537-547):
```javascript
      const finalizeRes = await DonHangService.update(donHangId, {
        khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
        diaChiGiaoHangText: posFoundCust.value.diaChi ?? "Tai cua hang",
        khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
        tongTien: posCartTotal.value, giamGia: posGiamGia.value,
        phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
        ngayDat,
        ngayGiaoThucTe: nowLocalIso(),
        trangThaiDonHang: "delivered", trangThaiThanhToan: "paid", kenhBan: "in_store",
      });
      if (!finalizeRes.ok) throw new Error(t('admin.errors.createOrderError', { message: await parsePosApiError(finalizeRes) }));
```
Sửa thành — chỉ chuyển thẳng "delivered" khi khách tự lấy tại quầy; khi giao tận nơi, để nguyên "confirmed" và bỏ qua bước finalize này (nhân viên giao hàng sẽ tự cập nhật trạng thái tiếp qua màn Đơn hàng, giống đơn online):
```javascript
      if (posDeliveryMode.value === 'pickup') {
        const finalizeRes = await DonHangService.update(donHangId, {
          khachHangId, nguoiNhan, sdtNguoiNhan: posFoundCust.value.soDienThoai,
          diaChiGiaoHangText: diaChiGiao,
          khuyenMaiId: posAppliedPromo.value?.khuyenMaiId ?? null,
          tongTien: posCartTotal.value, giamGia: posGiamGia.value,
          phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
          ngayDat,
          ngayGiaoThucTe: nowLocalIso(),
          trangThaiDonHang: "delivered", trangThaiThanhToan: "paid", kenhBan: "in_store",
        });
        if (!finalizeRes.ok) throw new Error(t('admin.errors.createOrderError', { message: await parsePosApiError(finalizeRes) }));
      }
```

- [ ] **Step 6: Thêm 4 i18n key mới**

Trong `vi.js`, tìm `shippingFeeLabel:` trong namespace `pos` (`grep -n "shippingFeeLabel:" FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`), thêm ngay trước dòng đó:
```javascript
      deliveryModeLabel: "Hình thức nhận hàng",
      pickupAtStore: "Khách tự lấy",
      deliverToAddress: "Giao tận nơi",
      deliveryAddressPlaceholder: "Nhập địa chỉ giao hàng...",
```
Trong `en.js`, tìm đúng vị trí tương ứng, thêm:
```javascript
      deliveryModeLabel: "Delivery method",
      pickupAtStore: "Customer pickup",
      deliverToAddress: "Deliver to address",
      deliveryAddressPlaceholder: "Enter delivery address...",
```

- [ ] **Step 7: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: `✓ built`.

- [ ] **Step 8: Verify tay qua trình duyệt**

Bán tại quầy, thêm sản phẩm, mặc định "Khách tự lấy" — xác nhận không hiện dòng phí vận chuyển, tổng tiền = tạm tính - giảm giá. Bấm "Giao tận nơi" — xác nhận hiện ô địa chỉ + dòng phí vận chuyển xuất hiện lại. Tạo đơn với "Giao tận nơi" — vào màn Đơn hàng xác nhận đơn ở trạng thái "Đã xác nhận" (không tự nhảy "Đã giao"). Tạo đơn khác với "Khách tự lấy" — xác nhận đơn lên thẳng "Đã giao" như hành vi cũ.

- [ ] **Step 9: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(pos): thêm lựa chọn giao tận nơi, ẩn phí vận chuyển khi khách tự lấy"
```

---

### Task 5: Nút mở tab xem khuyến mãi

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

**Interfaces:** Không có — thuần thêm 1 nút gọi `window.open`.

- [ ] **Step 1: Thêm icon `ExternalLink` vào import**

Tìm dòng:
```javascript
import { Laptop, ShoppingCart, Receipt, Info, RefreshCw, X, Check } from '@lucide/vue';
```
Sửa thành:
```javascript
import { Laptop, ShoppingCart, Receipt, Info, RefreshCw, X, Check, ExternalLink } from '@lucide/vue';
```

- [ ] **Step 2: Thêm nút cạnh ô nhập mã khuyến mãi**

Tìm khối (dòng 710-713):
```html
        <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
          <div class="d-flex gap-2 position-relative">
            <input v-model="posPromoCode" class="form-control form-control-sm" style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('checkout.promoPlaceholder')" @input="onPosPromoInput" @focus="onPosPromoFocus" @blur="showPosPromoSuggestions = false" @keyup.enter="posApplyPromo" />
            <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="posApplyPromo">{{ t('checkout.apply') }}</button>
```
Sửa thành (thêm 1 nút icon mới ngay sau nút "Áp dụng"):
```html
        <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
          <div class="d-flex gap-2 position-relative">
            <input v-model="posPromoCode" class="form-control form-control-sm" style="background:var(--bg-hover);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('checkout.promoPlaceholder')" @input="onPosPromoInput" @focus="onPosPromoFocus" @blur="showPosPromoSuggestions = false" @keyup.enter="posApplyPromo" />
            <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="posApplyPromo">{{ t('checkout.apply') }}</button>
            <button
              class="btn btn-sm btn-outline-secondary flex-shrink-0" style="padding:2px 8px;"
              :aria-label="t('admin.pos.viewPromotionsTab')" :title="t('admin.pos.viewPromotionsTab')"
              @click="() => window.open('/#/admin', '_blank')"
            ><ExternalLink :size="14" /></button>
```

- [ ] **Step 3: Thêm i18n key `admin.pos.viewPromotionsTab`**

Trong `vi.js`, cạnh `paymentMethodLabel:` trong namespace `pos` (tìm bằng `grep -n "paymentMethodLabel:" FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`), thêm:
```javascript
      viewPromotionsTab: "Xem danh sách khuyến mãi ở tab mới",
```
Trong `en.js`, tương ứng:
```javascript
      viewPromotionsTab: "View promotions in a new tab",
```

- [ ] **Step 4: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: `✓ built`.

- [ ] **Step 5: Verify tay qua trình duyệt**

Bấm nút mới cạnh ô mã khuyến mãi — xác nhận mở đúng 1 tab mới tới `/#/admin` (nhân viên tự bấm sang tab "Khuyến mãi" — không có deep-link tự động, đúng theo spec).

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(pos): thêm nút mở tab mới xem danh sách khuyến mãi"
```

---

### Task 6: QR chuyển khoản + giả lập đã quét

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/utils/orderStatus.js` (bỏ `'vnpay'` khỏi `POS_PAYMENT_METHODS`)
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue`

**Interfaces:**
- Produces: `posQrScanned` (ref boolean, mặc định `false`) — dùng để khoá nút "Tạo đơn" khi phương thức là `chuyen_khoan`.
- Consumes: `posPaymentMethod`, `posGrandTotal`, `posFoundCust` (đã có).

- [ ] **Step 1: Bỏ `'vnpay'` khỏi `POS_PAYMENT_METHODS`**

Tìm dòng (dòng 61):
```javascript
export const POS_PAYMENT_METHODS = ['tien_mat', 'vnpay', 'chuyen_khoan', 'the_tin_dung'];
```
Sửa thành:
```javascript
export const POS_PAYMENT_METHODS = ['tien_mat', 'chuyen_khoan', 'the_tin_dung'];
```

- [ ] **Step 2: Thêm state `posQrScanned`, reset ở mọi nơi reset form + khi đổi phương thức thanh toán**

Tìm dòng (đã thêm ở Task 4, giờ thêm tiếp ngay sau):
```javascript
const posDeliveryMode = ref('pickup');
const posDeliveryAddress = ref('');
```
Thêm ngay sau:
```javascript
// Xac nhan thu cong "da quet QR" — chua co webhook ngan hang that nen nhan vien tu bam
// sau khi (gia lap) thay khach quet xong. Reset ve false moi khi doi phuong thuc thanh toan.
const posQrScanned = ref(false);
```

Ở cả 3 hàm reset (`posHoldOrder`, `posReset`, cuối `posPlaceOrder`) — thêm ngay sau 2 dòng vừa thêm ở Task 4 Step 3:
```javascript
  posQrScanned.value = false;
```

- [ ] **Step 3: Reset `posQrScanned` khi đổi phương thức thanh toán**

Tìm khối chọn phương thức thanh toán trong template (dòng 736-752):
```html
            <button
              v-for="m in POS_PAYMENT_METHODS" :key="m"
              class="btn btn-sm flex-fill d-flex flex-column align-items-center py-2"
              style="border-radius:8px;font-size:0.65rem;"
              :style="posPaymentMethod === m
                ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
              @click="posPaymentMethod = m"
            >
```
Sửa `@click` thành:
```html
              @click="posPaymentMethod = m; posQrScanned = false"
```

- [ ] **Step 4: Thêm computed `posQrImageUrl` và state ảnh lỗi**

Thêm ngay sau khai báo `posQrScanned` (script setup, gần đầu file):
```javascript
const posQrImageFailed = ref(false);
// Tai dung dung cach CheckoutModal.vue tao QR — VietQR API, cung tai khoan VCB demo.
const posQrImageUrl = computed(() => {
  const bank    = 'VCB';
  const account = '9876543210';
  const info    = encodeURIComponent('Thanh toan SAO LAPTOP');
  const name    = encodeURIComponent('SAO LAPTOP');
  return `https://img.vietqr.io/image/${bank}-${account}-compact2.png?amount=${posGrandTotal.value}&addInfo=${info}&accountName=${name}`;
});
```
(Đặt sau khi `posGrandTotal` đã được khai báo ở trên, vì computed này dùng `posGrandTotal.value`.)

- [ ] **Step 5: Thêm import icon `ImageOff`**

Tìm dòng (đã sửa ở Task 5):
```javascript
import { Laptop, ShoppingCart, Receipt, Info, RefreshCw, X, Check, ExternalLink } from '@lucide/vue';
```
Sửa thành:
```javascript
import { Laptop, ShoppingCart, Receipt, Info, RefreshCw, X, Check, ExternalLink, ImageOff } from '@lucide/vue';
```

- [ ] **Step 6: Thêm khối QR + nút giả lập quét vào template, ngay dưới hàng nút chọn phương thức**

Tìm khối (dòng 735-752, phần "Phuong thuc thanh toan"):
```html
        <!-- Phuong thuc thanh toan -->
        <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
          <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">{{ t('admin.pos.paymentMethodLabel') }}</div>
          <div class="d-flex gap-1">
            <button
              v-for="m in POS_PAYMENT_METHODS" :key="m"
              class="btn btn-sm flex-fill d-flex flex-column align-items-center py-2"
              style="border-radius:8px;font-size:0.65rem;"
              :style="posPaymentMethod === m
                ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
              @click="posPaymentMethod = m; posQrScanned = false"
            >
              <component :is="paymentMethodIcon(m)" :size="18" />
              <span>{{ paymentMethodLabel(m) }}</span>
            </button>
          </div>
        </div>
```
Sửa thành (thêm khối QR ngay sau hàng nút):
```html
        <!-- Phuong thuc thanh toan -->
        <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
          <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">{{ t('admin.pos.paymentMethodLabel') }}</div>
          <div class="d-flex gap-1">
            <button
              v-for="m in POS_PAYMENT_METHODS" :key="m"
              class="btn btn-sm flex-fill d-flex flex-column align-items-center py-2"
              style="border-radius:8px;font-size:0.65rem;"
              :style="posPaymentMethod === m
                ? 'background:rgba(244,63,94,0.12);border:1.5px solid var(--accent);color:var(--accent-fg);'
                : 'background:var(--bg-input);border:1.5px solid var(--border-color-strong);color:var(--text-secondary);'"
              @click="posPaymentMethod = m; posQrScanned = false"
            >
              <component :is="paymentMethodIcon(m)" :size="18" />
              <span>{{ paymentMethodLabel(m) }}</span>
            </button>
          </div>
          <div v-if="posPaymentMethod === 'chuyen_khoan'" class="d-flex flex-column align-items-center gap-2 p-3 rounded-3" style="background:var(--bg-card-inset);">
            <img
              v-if="!posQrImageFailed" :src="posQrImageUrl" alt="VietQR" style="width:160px;height:160px;border-radius:10px;background:#fff;padding:4px;"
              @error="posQrImageFailed = true"
            />
            <div
              v-else class="d-flex flex-column align-items-center justify-content-center text-center small"
              style="width:160px;height:160px;border-radius:10px;background:var(--bg-card-alt);color:var(--text-secondary);gap:6px;"
            >
              <ImageOff :size="24" />{{ t('checkout.qrImageFailed') }}
            </div>
            <button
              class="btn btn-sm w-100" :class="posQrScanned ? 'btn-success' : 'btn-outline-warning'"
              @click="posQrScanned = !posQrScanned"
            >
              <Check v-if="posQrScanned" :size="14" style="vertical-align:-2px;" />
              {{ posQrScanned ? t('admin.pos.qrScannedConfirmed') : t('admin.pos.simulateQrScan') }}
            </button>
          </div>
        </div>
```

- [ ] **Step 7: Khoá nút "Tạo đơn" khi chọn chuyển khoản mà chưa giả lập quét**

Tìm dòng (gần dòng 766):
```html
            <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" :disabled="posStage !== 'selling' || !posCart.length || !posPaymentMethod || posPlacing" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
```
Sửa thành:
```html
            <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" :disabled="posStage !== 'selling' || !posCart.length || !posPaymentMethod || posPlacing || (posPaymentMethod === 'chuyen_khoan' && !posQrScanned)" @click="posPlaceOrder">{{ t('admin.pos.createOrder') }}</button>
```

- [ ] **Step 8: Thêm 2 i18n key mới**

Trong `vi.js`, cạnh `paymentMethodLabel:` trong namespace `pos`, thêm:
```javascript
      simulateQrScan: "Giả lập đã quét",
      qrScannedConfirmed: "Đã xác nhận quét QR",
```
Trong `en.js`, tương ứng:
```javascript
      simulateQrScan: "Simulate QR scan",
      qrScannedConfirmed: "QR scan confirmed",
```

- [ ] **Step 9: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: `✓ built`.

- [ ] **Step 10: Verify tay qua trình duyệt**

Bán tại quầy, chọn phương thức "Chuyển khoản" — xác nhận không còn nút "VNPay" trong danh sách (chỉ còn Tiền mặt/Chuyển khoản/Thẻ tín dụng), hiện mã QR thật (ảnh load được từ vietqr.io) + nút "Giả lập đã quét". Nút "Tạo đơn" phải disabled tới khi bấm "Giả lập đã quét" — bấm xong nút chuyển "Đã xác nhận quét QR" (màu xanh) và "Tạo đơn" mở khoá. Đổi sang "Tiền mặt" — xác nhận khối QR biến mất, "Tạo đơn" không còn bị khoá bởi điều kiện QR.

- [ ] **Step 11: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/utils/orderStatus.js FrontEnd/QLBanMayTinh/src/components/admin/PosPanel.vue FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(pos): gộp VNPay vào Chuyển khoản, thêm QR + giả lập xác nhận đã quét"
```

---

### Task 7: Xác nhận end-to-end qua Docker

**Files:** không sửa file nào — chỉ verify.

- [ ] **Step 1: Rebuild frontend container**

Frontend chạy qua Vite dev server trong Docker với volume mount — HMR tự áp thay đổi, không cần rebuild image. Nếu nghi ngờ HMR bị stale (như đã gặp ở sub-project trước), làm mới hẳn bằng cách điều hướng trình duyệt sang `about:blank` rồi quay lại `/#/admin` để ép tải lại toàn bộ module.

- [ ] **Step 2: Chạy trọn 1 kịch bản bán hàng tại quầy đầy đủ**

Đăng nhập admin → Bán hàng → tìm 1 sản phẩm (thử gõ không dấu) → thêm vào giỏ chọn 2 serial cùng lúc → áp thử 1 mã khuyến mãi (dùng nút mở tab mới để tra mã) → chọn "Giao tận nơi" nhập địa chỉ → chọn phương thức "Chuyển khoản" → giả lập đã quét → tạo đơn. Vào màn Đơn hàng xác nhận đơn vừa tạo đúng: đúng sản phẩm/serial, đúng phí giao hàng, trạng thái "Đã xác nhận" (không tự "Đã giao" vì chọn giao tận nơi), đúng phương thức thanh toán "Chuyển khoản".

- [ ] **Step 3: Không cần commit — task này chỉ verify.**

## Self-Review Note

- **Spec coverage:** 6 việc trong spec ↔ đúng 6 task (1-6). Task 7 phủ mục "Kiểm tra" cuối spec (kịch bản end-to-end).
- **Placeholder scan:** không còn "TBD"/"tương tự Task N" — mọi step có code cụ thể, mọi vị trí chèn i18n key đều có lệnh `grep` để định vị chính xác (tránh phụ thuộc số dòng có thể lệch).
- **Type/signature consistency:** `posDeliveryMode`/`posDeliveryAddress` (Task 4) và `posQrScanned` (Task 6) đều được thêm vào đúng cả 3 hàm reset (`posHoldOrder`, `posReset`, cuối `posPlaceOrder`) — không sót hàm nào khiến state kẹt lại giữa 2 đơn liên tiếp. `serialPickerChosenIds` (Task 3) chỉ dùng trong nhánh thêm-mới, không ảnh hưởng nhánh đổi-serial vốn vẫn gọi thẳng `posSelectSerial`.
