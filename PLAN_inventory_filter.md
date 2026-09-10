# Kế hoạch sửa thanh filter Tồn kho & Phiếu nhập

## Bối cảnh

Có 2 vấn đề trong `FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue`:

1. **Thanh filter Phiếu nhập** hiển thị `admin.inventory.advancedFilter` thay vì text thân thiện — vì 2 key i18n chưa được định nghĩa. Hơn nữa, do nhiều control + nút "Bộ lọc nâng cao" + 3 nút hành động (In PDF / Xuất Excel / Tạo phiếu nhập) nên thanh bị xuống hàng 2-3 hàng, gây rối.
2. **Modal "Chi tiết serial"** (xem sản phẩm từ tab Tồn kho) có thanh tìm kiếm + filter bị đè lên nhau — do CSS của modal dùng chung class với panel chính nhưng kích thước khác nhau.

## File sửa

- [FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js](FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js) — thêm 2 key thiếu trong `admin.inventory`
- [FrontEnd/QLBanMayTinh/src/i18n/locales/en.js](FrontEnd/QLBanMayTinh/src/i18n/locales/en.js) — tương ứng tiếng Anh
- [FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue](FrontEnd/QLBanMayTinh/src/components/admin/InventoryPanel.vue) — áp dụng pattern Filter button + collapse panel đã có sẵn ở tab Tồn kho (dòng 993-1036)

## Thay đổi cụ thể

### 1. i18n — bổ sung key

**vi.js** (dòng ~1245, sau `inventory.*` hiện có):
```js
advancedFilter: "Bộ lọc nâng cao",
resetFilter: "Đặt lại",
supplierLabel: "Nhà cung cấp",
dateRangeLabel: "Khoảng ngày",
```

**en.js**:
```js
advancedFilter: "Advanced filter",
resetFilter: "Reset",
supplierLabel: "Supplier",
dateRangeLabel: "Date range",
```

### 2. InventoryPanel.vue — thanh Phiếu nhập

**Trước** (dòng 1127-1148) — thanh dài gồm: search + supplier select + adv button + reset + 3 nút action, dễ xuống hàng.

**Sau** — áp dụng đúng pattern tab Tồn kho (đã có sẵn, dòng 993-998):
```vue
<div class="inv-bar">
  <span class="inv-bar__count">{{ filteredPhieuNhap.length }}/{{ phieuNhapList.length }}</span>
  <div class="inv-search">
    <Search :size="14" class="inv-search__icon" />
    <input v-model="phieuNhapSearch" :placeholder="t('admin.phieuNhap.searchPlaceholder')" />
  </div>
  <button class="inv-btn inv-btn--ghost" :class="{ 'is-on': showPhieuNhapAdv }"
          @click="showPhieuNhapAdv = !showPhieuNhapAdv">
    <Filter :size="14" />
    {{ t('admin.inventory.advancedFilter') }}
    <span v-if="phieuNhapActiveFilterCount" class="inv-chip">{{ phieuNhapActiveFilterCount }}</span>
    <ChevronDown :size="13" class="inv-caret" :class="{ 'is-open': showPhieuNhapAdv }" />
  </button>
  <div class="inv-bar__actions">
    <button class="inv-btn inv-btn--ghost" @click="printPhieuNhapList"><Printer :size="14" /> {{ t('admin.phieuNhap.printPdf') }}</button>
    <button class="inv-btn inv-btn--ghost" @click="exportPhieuNhapExcel"><Download :size="14" /> {{ t('admin.phieuNhap.exportExcel') }}</button>
    <button class="inv-btn inv-btn--primary" @click="openAddPhieuNhap"><Plus :size="14" /> {{ t('admin.phieuNhap.add') }}</button>
  </div>
</div>

<div class="inv-filter" :class="{ 'is-open': showPhieuNhapAdv }">
  <div class="inv-filter__panel">
    <div class="inv-filter__grid">
      <label class="inv-field">
        <span>{{ t('admin.inventory.dateRangeLabel') }}</span>
        <div class="inv-date-range">
          <input type="date" v-model="phieuNhapDateFrom" class="inv-date" />
          <span class="inv-date-sep">→</span>
          <input type="date" v-model="phieuNhapDateTo" class="inv-date" />
        </div>
      </label>
      <label class="inv-field">
        <span>{{ t('admin.inventory.supplierLabel') }}</span>
        <select v-model="phieuNhapSupplierFilter" class="inv-select">
          <option value="">{{ t('admin.inventory.filterAll') }}</option>
          <option v-for="s in suppliers" :key="s.nhaCungCapId" :value="s.nhaCungCapId">{{ s.tenNhaCungCap }}</option>
        </select>
      </label>
      <label class="inv-field">
        <span>{{ t('admin.phieuNhap.colStaff') }}</span>
        <select v-model="phieuNhapStaffFilter" class="inv-select">
          <option value="">{{ t('admin.inventory.filterAll') }}</option>
          <option v-for="s in staff" :key="s.nhanVienId" :value="s.nhanVienId">{{ s.hoTen }}</option>
        </select>
      </label>
    </div>
    <div class="inv-filter__foot">
      <div class="inv-filter__btns">
        <button class="inv-btn inv-btn--ghost" @click="resetPhieuNhapFilters">{{ t('admin.inventory.resetFilter') }}</button>
        <button class="inv-btn inv-btn--primary" @click="showPhieuNhapAdv = false">{{ t('admin.variants.filterDone') || 'Xong' }}</button>
      </div>
    </div>
  </div>
</div>
```

### 3. InventoryPanel.vue — bỏ code thừa

- Xóa `phieuNhapHasActiveFilter` → thay bằng `phieuNhapActiveFilterCount` (đếm số filter đang đặt) để hiển thị badge số trên nút Filter.
- Xóa `<transition name="inv-collapse">` đã thêm trước đó vì nay dùng pattern `.inv-filter.is-open` có sẵn, đã có CSS `.inv-filter__panel` collapse mượt.

### 4. InventoryPanel.vue — sửa modal Chi tiết serial bị đè

Tìm class dùng chung giữa panel chính và modal Chi tiết serial. Modal dùng các class `inv-search`, `inv-select` — vốn được style cho container rộng. Khi ở trong modal hẹp, các input bị overflow.

**Cách xử lý**: thêm scope `.inv-modal` cho các input trong modal:
```css
.inv-modal .inv-search { max-width: none; flex: 1; }
.inv-modal .inv-select { width: 100%; }
```

## Tái sử dụng từ code hiện có

- Pattern Filter + collapse panel: tab Tồn kho (dòng 993-1036) — đã chạy tốt, áp dụng nguyên xi.
- CSS `.inv-filter__panel` đã có sẵn collapse animation, không cần viết lại.
- i18n keys `filterAll`, `colSupplier`, `colStaff`, `colCode`, `colDate` đã có.

## Verification

1. Refresh trình duyệt, vào tab **Kho hàng → Phiếu nhập**.
   - Thanh filter phải hiển thị đúng 1 hàng, gọn.
   - Text nút là "Bộ lọc nâng cao" (không còn `admin.inventory.advancedFilter`).
   - Click nút → panel mở/đóng mượt với 3 trường: Khoảng ngày / Nhà cung cấp / Nhân viên.
   - Badge số hiển thị trên nút khi có filter đang đặt.

2. Vào tab **Tồn kho**, click "Xem chi tiết" một sản phẩm → modal Chi tiết serial mở.
   - Thanh tìm kiếm + filter bên trong modal hiển thị đúng, không bị đè.
