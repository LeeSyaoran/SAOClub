<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as SanPhamService from "../../Service/SanPhamService.js";
import * as BienTheSanPhamService from "../../Service/BienTheSanPhamService.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import * as DanhMucService from "../../Service/DanhMucService.js";
import * as DmService from "../../Service/DmService.js";
import { authHeaders } from "../../Service/api.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { SuppliersStore, ensureSuppliers } from "../../stores/suppliers.js";

// Không dựa vào ProductsTable.vue (tab anh em) đã tải sẵn ProductsStore — self-contained,
// đúng pattern ensureX() dùng chung toàn app (no-op nếu đã tải/đang tải).
onMounted(() => { ensureProducts(); });

// Danh sách PHẲNG mọi biến thể (sửa/thêm/xóa trực tiếp) — tách ra khỏi ProductsTable.vue
// (vốn trước đây phải mở "Chi tiết sản phẩm" rồi mới sửa được 1 biến thể) để đứng ngang
// hàng CPU/RAM/GPU/Ổ cứng, đúng yêu cầu "1 tab riêng bên ngoài".
const props = defineProps({ readonly: { type: Boolean, default: false } });

// ── Danh muc/hang/CPU/RAM/o cung/GPU — chi can khi mo form them/sua bien the. Copy nguyen
// pattern tu ProductsTable.vue (Task goc) — 2 component doc lap, khong dang chia se state.
const categories = ref([]);
const brands = ref([]);
const cpuList = ref([]);
const ramList = ref([]);
const oCungList = ref([]);
const gpuList = ref([]);

let productRefDataPromise = null;
const ensureProductRefData = () => {
  if (productRefDataPromise) return productRefDataPromise;
  productRefDataPromise = Promise.all([
    DanhMucService.getAll().catch(() => []),
    DmService.getThuongHieu().catch(() => []),
    DmService.getCpu().catch(() => []),
    DmService.getRam().catch(() => []),
    DmService.getOCung().catch(() => []),
    DmService.getGpu().catch(() => []),
    ensureSuppliers(),
  ]).then(([cat, br, cpu, ram, oc, gpu]) => {
    categories.value = cat;
    brands.value = br;
    cpuList.value = cpu;
    ramList.value = ram;
    oCungList.value = oc;
    gpuList.value = gpu;
  });
  return productRefDataPromise;
};
const suppliers = computed(() => SuppliersStore.items);

// ── Bo loc danh sach bien the phang (moi dong = 1 bien the, ProductsStore.items da dung
// dinh dang nay san) ────────────────────────────────────────────────────────────────
const variantSearch = ref("");
const filteredVariants = computed(() => {
  const q = variantSearch.value.trim().toLowerCase();
  if (!q) return ProductsStore.items;
  return ProductsStore.items.filter((p) =>
    (p.tenSanPham ?? '').toLowerCase().includes(q) ||
    (p.maSku ?? '').toLowerCase().includes(q)
  );
});
// Bỏ tiền tố hãng CPU (Intel Core/AMD Ryzen) — dư thừa, không cần trong bảng liệt kê gọn,
// tên đầy đủ vẫn hiện nguyên trong ProductDetailModal.vue lúc xem chi tiết.
const shortCpu = (cpu) => cpu?.replace(/^(Intel Core|AMD Ryzen)\s+/i, '') ?? '';
const configLabel = (p) => [shortCpu(p.cpu), p.ram, p.oCung].filter(Boolean).join(' · ') || '—';

// ── Modal them/sua bien the ───────────────────────────────────────────────────────────
const showVariantModal = ref(false);
const editingId = ref(null); // sanPhamId dang sua (null = dang them bien the moi)
const formError = ref("");
const soSerialMoi = ref('');
const imagePreview  = ref('');
const imageFilePending = ref(null);

const PHAN_LOAI_TAG_OPTIONS = [
  { value: 'gaming', label: 'Gaming' },
  { value: 'van_phong', label: 'Văn phòng' },
  { value: 'sinh_vien', label: 'Sinh viên' },
  { value: 'do_hoa', label: 'Đồ họa' },
  { value: 'ky_thuat', label: 'Kỹ thuật' },
  { value: 'macbook', label: 'MacBook' },
  { value: 'cu', label: 'Cũ' },
  { value: 'gia_re', label: 'Giá rẻ' },
  { value: 'linh_kien', label: 'Linh kiện' },
];
const toggleTag = (value) => {
  const tags = form.phanLoaiTags.split(',').map(s => s.trim()).filter(Boolean);
  const idx = tags.indexOf(value);
  if (idx === -1) tags.push(value); else tags.splice(idx, 1);
  form.phanLoaiTags = tags.join(',');
  form.phanLoaiTen = tags
    .map(v => PHAN_LOAI_TAG_OPTIONS.find(o => o.value === v)?.label)
    .filter(Boolean)
    .join(', ');
};
const isTagSelected = (value) => form.phanLoaiTags.split(',').map(s => s.trim()).includes(value);

const emptyForm = () => ({
  bienTheId: null,
  tenSanPham: "",
  thuongHieuId: null,
  danhMucId: null,
  nhaCungCapId: null,
  loaiSanPham: "",
  maSku: "",
  cpuId: null,
  ramId: null,
  oCungId: null,
  gpuId: null,
  kichThuocManHinh: "",
  heDieuHanh: "",
  pin: "",
  trongLuongKg: "",
  mauSac: "",
  giaBan: "",
  giaNhap: "",
  baoHanhThang: "",
  moTa: "",
  hinhAnhChinh: "",
  trangThai: "active",
  phanLoaiTags: "",
  phanLoaiTen: "",
});
const form = reactive(emptyForm());
const resetImageState = () => { imagePreview.value = ''; imageFilePending.value = null; };

// ── Them bien the moi cho 1 san pham DA TON TAI — can chon san pham truoc (khac
// ProductsTable.vue cu, truoc day mo tu trong "Chi tiet san pham" nen da co san context) ──
const addVariantMode      = ref(false);
const addVariantSanPhamId = ref(null);
const addVariantSanPhamName = ref('');
const variantProductSearch = ref('');

// San pham doc nhat (dedupe theo sanPhamId) de tim/chon khi them bien the moi
const distinctProducts = computed(() => {
  const map = new Map();
  ProductsStore.items.forEach(p => { if (!map.has(p.sanPhamId)) map.set(p.sanPhamId, p); });
  return [...map.values()];
});
const searchedProducts = computed(() => {
  const q = variantProductSearch.value.trim().toLowerCase();
  if (!q) return [];
  return distinctProducts.value.filter(p => (p.tenSanPham ?? '').toLowerCase().includes(q)).slice(0, 10);
});

const openAddVariantFlow = async () => {
  await ensureProductRefData();
  Object.assign(form, emptyForm());
  editingId.value = null;
  addVariantMode.value = true;
  addVariantSanPhamId.value = null;
  addVariantSanPhamName.value = '';
  variantProductSearch.value = '';
  formError.value = '';
  soSerialMoi.value = '';
  resetImageState();
  showVariantModal.value = true;
};
const pickProductForVariant = (p) => {
  form.tenSanPham   = p.tenSanPham;
  form.thuongHieuId  = p.thuongHieuId;
  form.danhMucId     = p.danhMucId;
  form.loaiSanPham   = p.loaiSanPham;
  addVariantSanPhamId.value = p.sanPhamId;
  addVariantSanPhamName.value = p.tenSanPham;
};
const changeProductForVariant = () => {
  addVariantSanPhamId.value = null;
  addVariantSanPhamName.value = '';
  variantProductSearch.value = '';
};

const openEdit = async (p) => {
  await ensureProductRefData();
  Object.assign(form, {
    bienTheId: p.bienTheId,
    tenSanPham: p.tenSanPham,
    thuongHieuId: p.thuongHieuId,
    danhMucId: p.danhMucId,
    nhaCungCapId: p.nhaCungCapId,
    loaiSanPham: p.loaiSanPham,
    maSku: p.maSku,
    cpuId: cpuList.value.find((c) => c.tenCpu === p.cpu)?.cpuId ?? null,
    ramId: ramList.value.find((r) => r.dungLuong === p.ram)?.ramId ?? null,
    oCungId: oCungList.value.find((o) => o.loaiOcung === p.oCung)?.oCungId ?? null,
    gpuId: gpuList.value.find((g) => g.tenGpu === p.gpu)?.gpuId ?? null,
    kichThuocManHinh: p.kichThuocManHinh,
    heDieuHanh: p.heDieuHanh,
    pin: p.pin,
    trongLuongKg: p.trongLuongKg,
    mauSac: p.mauSac,
    giaBan: p.giaBan,
    giaNhap: p.giaNhap,
    baoHanhThang: p.baoHanhThang,
    moTa: p.moTa,
    hinhAnhChinh: p.hinhAnhChinh,
    trangThai: p.trangThai,
    phanLoaiTags: p.phanLoaiTags ?? "",
    phanLoaiTen: p.phanLoaiTen ?? "",
  });
  editingId.value = p.sanPhamId;
  addVariantMode.value = false;
  addVariantSanPhamId.value = null;
  formError.value = "";
  imagePreview.value = p.hinhAnhChinh || '';
  imageFilePending.value = null;
  showVariantModal.value = true;
};

const handleImageFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  imageFilePending.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const saveVariant = async () => {
  formError.value = "";

  if (imageFilePending.value) {
    const fd = new FormData();
    fd.append('file', imageFilePending.value);
    try {
      const upRes = await fetch('/api/upload/image', { method: 'POST', headers: authHeaders(), body: fd });
      if (upRes.ok) {
        const upData = await upRes.json();
        form.hinhAnhChinh = upData.url;
      } else {
        formError.value = t('admin.errors.uploadFailed', { status: upRes.status });
        return;
      }
    } catch (e) {
      formError.value = t('admin.errors.uploadError', { message: e.message });
      return;
    }
  }

  if (addVariantMode.value) {
    if (!addVariantSanPhamId.value) { formError.value = t('admin.variantModal.pickProductLabel'); return; }
    if (!form.maSku.trim()) { formError.value = t('admin.errors.skuRequired'); return; }
    const variantBody = {
      sanPhamId: addVariantSanPhamId.value,
      maSku: form.maSku,
      giaNhap: Number(form.giaNhap),
      giaBan: Number(form.giaBan),
      baoHanhThang: Number(form.baoHanhThang) || 0,
      hinhAnhBienThe: form.hinhAnhChinh,
      trangThai: form.trangThai,
      mauSac: form.mauSac,
      cpuId: form.cpuId ? Number(form.cpuId) : null,
      ramId: form.ramId ? Number(form.ramId) : null,
      oCungId: form.oCungId ? Number(form.oCungId) : null,
      gpuId: form.gpuId ? Number(form.gpuId) : null,
      kichThuocManHinh: form.kichThuocManHinh,
      heDieuHanh: form.heDieuHanh,
      pin: form.pin,
      trongLuongKg: form.trongLuongKg ? Number(form.trongLuongKg) : null,
    };
    try {
      const res = await BienTheSanPhamService.create(variantBody);
      if (!res.ok) {
        formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
        return;
      }
      const created = await res.json();
      if (soSerialMoi.value.trim()) {
        await ChiTietSanPhamService.create({
          bienTheId: created.bienTheId,
          soSerial: soSerialMoi.value.trim(),
          trangThai: 'trong_kho',
          ngayNhapKho: nowLocalIso(),
        }).catch(() => {});
      }
      showVariantModal.value = false;
      resetImageState();
      await refreshProducts();
    } catch (e) {
      formError.value = e.message;
    }
    return;
  }

  const body = {
    ...form,
    thuongHieuId: Number(form.thuongHieuId),
    danhMucId: Number(form.danhMucId),
    nhaCungCapId: form.nhaCungCapId ? Number(form.nhaCungCapId) : null,
    cpuId: form.cpuId ? Number(form.cpuId) : null,
    ramId: form.ramId ? Number(form.ramId) : null,
    oCungId: form.oCungId ? Number(form.oCungId) : null,
    gpuId: form.gpuId ? Number(form.gpuId) : null,
    giaBan: Number(form.giaBan),
    giaNhap: Number(form.giaNhap),
    trongLuongKg: form.trongLuongKg ? Number(form.trongLuongKg) : null,
    baoHanhThang: Number(form.baoHanhThang),
  };
  try {
    const res = await SanPhamService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showVariantModal.value = false;
    resetImageState();
    await refreshProducts();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteVariant = async (bienTheId) => {
  const sku = ProductsStore.items.find(p => p.bienTheId === bienTheId)?.maSku ?? '';
  const daGiaoDich = await BienTheSanPhamService.hasTransactionHistory(bienTheId).catch(() => false);
  if (daGiaoDich) {
    showToast(t('admin.errors.cannotDeleteVariant', { sku }));
    return;
  }
  if (!(await askConfirm(t('admin.confirm.deleteVariantSimple', { sku })))) return;
  const res = await BienTheSanPhamService.remove(bienTheId);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  await refreshProducts();
};
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredVariants.length }}/{{ ProductsStore.items.length }} {{ t('admin.variants.countSuffix') }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="variantSearch" class="form-control form-control-sm" style="width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.variants.searchPlaceholder')" />
      <button v-if="!readonly" class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddVariantFlow">{{ t('admin.variants.add') }}</button>
    </div>
  </div>
  <div v-if="ProductsStore.loading" class="text-secondary small">{{ t('admin.variants.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft); font-size:0.82rem;">
      <thead><tr>
        <th style="width:36px;">{{ t('admin.common.stt') }}</th>
        <th style="width:150px;">{{ t('admin.variants.colSku') }}</th><th style="width:220px;">{{ t('admin.variants.colProduct') }}</th>
        <th>{{ t('admin.variants.colConfig') }}</th><th style="width:100px;">{{ t('admin.variants.colColor') }}</th>
        <th style="width:120px;">{{ t('admin.variants.colPriceSell') }}</th><th style="width:100px;">{{ t('admin.variants.colStatus') }}</th><th style="width:110px;">{{ t('admin.variants.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(p, idx) in filteredVariants" :key="p.bienTheId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary text-truncate" style="font-family:monospace; font-size:0.76rem; max-width:150px;" :title="p.maSku">{{ p.maSku }}</td>
          <td class="text-truncate" style="max-width:220px;" :title="p.tenSanPham">{{ p.tenSanPham }}</td>
          <td class="text-secondary text-truncate" style="max-width:260px;" :title="configLabel(p)">{{ configLabel(p) }}</td>
          <td class="text-truncate" style="max-width:100px;">{{ p.mauSac || '—' }}</td>
          <td class="text-nowrap">{{ formatPrice(p.giaBan) }}</td>
          <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'" style="font-size:0.72rem;">{{ statusLabel(p.trangThai) }}</span></td>
          <td>
            <div class="d-flex gap-1">
              <button v-if="!readonly" class="btn btn-sm btn-outline-warning" style="font-size:0.72rem; padding:2px 7px;" @click="openEdit(p)">{{ t('admin.variants.edit') }}</button>
              <button v-if="!readonly" class="btn btn-sm btn-outline-danger"  style="font-size:0.72rem; padding:2px 7px;" @click="deleteVariant(p.bienTheId)">{{ t('admin.variants.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredVariants.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.variants.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ MODAL BIEN THE (them/sua) ══ -->
  <div v-if="showVariantModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showVariantModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:860px;max-width:96vw;max-height:92vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--border-color);">
        <div>
          <div class="fw-bold text-light" style="font-size:1rem;">{{ addVariantMode ? t('admin.variantModal.addVariant') : t('admin.productModal.titleEdit') }}</div>
          <div v-if="addVariantMode && addVariantSanPhamName" class="text-secondary" style="font-size:0.72rem;margin-top:2px;">{{ addVariantSanPhamName }}</div>
          <div v-else-if="editingId" class="text-secondary" style="font-size:0.72rem;margin-top:2px;">{{ t('admin.productModal.idLabel') }} {{ editingId }}</div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showVariantModal=false"></button>
      </div>

      <!-- Buoc 1 (chi khi them moi): chon san pham -->
      <div v-if="addVariantMode && !addVariantSanPhamId" class="overflow-y-auto px-4 py-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.variantModal.pickProductLabel') }}</label>
        <input v-model="variantProductSearch" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" :placeholder="t('admin.variantModal.pickProductPlaceholder')" />
        <div v-if="variantProductSearch.trim()" class="mt-1 rounded-2 overflow-hidden" style="max-height:220px;overflow-y:auto;border:1px solid var(--border-color-soft);">
          <div v-for="p in searchedProducts" :key="p.sanPhamId" class="p-2" style="cursor:pointer;" @click="pickProductForVariant(p)">
            {{ p.tenSanPham }} <span class="text-secondary" style="font-size:0.75rem;">— {{ p.tenThuongHieu }}</span>
          </div>
          <div v-if="searchedProducts.length===0" class="p-2 text-secondary small">{{ t('admin.variantModal.pickProductEmpty') }}</div>
        </div>
      </div>

      <!-- Buoc 2: form bien the (them, sau khi da chon san pham — hoac sua) -->
      <div v-else class="overflow-y-auto px-4 py-3" style="gap:0;">
        <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>

        <div v-if="addVariantMode" class="d-flex align-items-center justify-content-between p-2 rounded-2 mb-3" style="background:var(--bg-input);">
          <span>{{ addVariantSanPhamName }}</span>
          <button class="btn btn-sm btn-outline-secondary" style="font-size:0.72rem;" @click="changeProductForVariant">{{ t('admin.variantModal.changeProduct') }}</button>
        </div>

        <!-- ── Thong tin co ban ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionBasic') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-8" v-if="!addVariantMode">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.nameLabel') }}</label>
              <input v-model="form.tenSanPham" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.namePlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.skuLabel') }}</label>
              <input v-model="form.maSku" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); font-family:monospace;" :placeholder="t('admin.productModal.skuPlaceholder')" />
            </div>
            <div class="col-3" v-if="!addVariantMode">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.typeLabel') }}</label>
              <select v-model="form.loaiSanPham" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option value="" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option value="LAPTOP">{{ t('admin.productModal.typeLaptop') }}</option>
                <option value="PHU_KIEN">{{ t('admin.productModal.typeAccessory') }}</option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.statusLabel') }}</label>
              <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option value="active">{{ t('admin.productModal.statusActive') }}</option>
                <option value="inactive">{{ t('admin.productModal.statusInactive') }}</option>
                <option value="ngung_kin_doanh">{{ t('admin.productModal.statusDiscontinued') }}</option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.colorLabel') }}</label>
              <input v-model="form.mauSac" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.colorPlaceholder')" />
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.warrantyLabel') }}</label>
              <input v-model="form.baoHanhThang" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
            <template v-if="!addVariantMode">
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.brandLabel') }}</label>
              <select v-model="form.thuongHieuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option v-for="b in brands" :key="b.thuongHieuId" :value="b.thuongHieuId">{{ b.tenThuongHieu }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.categoryLabel') }}</label>
              <select v-model="form.danhMucId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.tenDanhMuc }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.supplierLabel') }}</label>
              <select v-model="form.nhaCungCapId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="s in suppliers" :key="s.nhaCungCapId" :value="s.nhaCungCapId">{{ s.tenNhaCungCap }}</option>
              </select>
            </div>
            </template>
          </div>
        </div>

        <!-- ── Cau hinh ky thuat ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionTech') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.cpuLabel') }}</label>
              <select v-model="form.cpuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="c in cpuList" :key="c.cpuId" :value="c.cpuId">{{ c.tenCpu }}</option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.gpuLabel') }}</label>
              <select v-model="form.gpuId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="g in gpuList" :key="g.gpuId" :value="g.gpuId">{{ g.tenGpu }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.ramLabel') }}</label>
              <select v-model="form.ramId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="r in ramList" :key="r.ramId" :value="r.ramId">{{ r.dungLuong }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.storageLabel') }}</label>
              <select v-model="form.oCungId" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
                <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                <option v-for="o in oCungList" :key="o.oCungId" :value="o.oCungId">{{ o.loaiOcung }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.screenLabel') }}</label>
              <input v-model="form.kichThuocManHinh" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.screenPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.osLabel') }}</label>
              <input v-model="form.heDieuHanh" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.osPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.batteryLabel') }}</label>
              <input v-model="form.pin" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.batteryPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.weightLabel') }}</label>
              <input v-model="form.trongLuongKg" type="number" step="0.1" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
          </div>
        </div>

        <!-- ── Gia ca ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionPrice') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.priceSellLabel') }}</label>
              <input v-model="form.giaBan" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.priceBuyLabel') }}</label>
              <input v-model="form.giaNhap" type="number" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
            </div>
          </div>
        </div>

        <!-- ── Hinh anh, mo ta, phan loai ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">{{ t('admin.productModal.sectionMedia') }}</div>
        <div class="rounded-3 p-3 mb-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
          <div class="row g-3">
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.imageLabel') }}</label>
              <div class="d-flex align-items-center gap-3">
                <label class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary" style="width:110px;height:88px;cursor:pointer;flex-shrink:0;overflow:hidden;background:var(--bg-card-inset);">
                  <img v-if="imagePreview" :src="imagePreview" style="width:110px;height:88px;object-fit:contain;" />
                  <template v-else>
                    <span style="font-size:1.4rem;">&#128247;</span>
                    <span style="font-size:0.68rem;margin-top:4px;">{{ t('admin.productModal.imageClickToChoose') }}</span>
                  </template>
                  <input type="file" accept="image/*" class="d-none" @change="handleImageFile" />
                </label>
                <div v-if="imageFilePending" class="text-warning" style="font-size:0.75rem;">{{ imageFilePending.name }}</div>
                <div v-else class="text-secondary" style="font-size:0.75rem;">{{ t('admin.productModal.imageFormats') }}</div>
              </div>
            </div>
            <template v-if="!addVariantMode">
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.descLabel') }}</label>
              <textarea v-model="form.moTa" rows="3" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"></textarea>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.tagsLabel') }} <span class="text-warning small">{{ t('admin.productModal.tagsHint') }}</span></label>
              <div class="d-flex flex-wrap gap-2">
                <button v-for="opt in PHAN_LOAI_TAG_OPTIONS" :key="opt.value" type="button"
                        class="btn btn-sm" :class="isTagSelected(opt.value) ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
                        style="font-size:0.75rem;padding:3px 12px;border-radius:999px;"
                        @click="toggleTag(opt.value)">{{ opt.label }}</button>
              </div>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.tagNameLabel') }} <span class="text-muted small">{{ t('admin.productModal.tagNameHint') }}</span></label>
              <input v-model="form.phanLoaiTen" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.productModal.tagNamePlaceholder')" />
            </div>
            </template>
          </div>
        </div>

        <!-- ── Serial (chi khi them moi) ── -->
        <div v-if="addVariantMode">
          <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:var(--accent-fg);">{{ t('admin.productModal.sectionSerial') }}</div>
          <div class="rounded-3 p-3" style="background:var(--bg-input);border:1px solid var(--border-color);">
            <label class="form-label small text-secondary mb-1">{{ t('admin.productModal.serialLabel') }}</label>
            <input v-model="soSerialMoi" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong); font-family:monospace;" :placeholder="t('admin.productModal.serialPlaceholder')" />
            <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t('admin.productModal.serialHint') }}</div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="!addVariantMode || addVariantSanPhamId" class="d-flex justify-content-end gap-2 px-4 py-3" style="border-top:1px solid var(--border-color);">
        <button class="btn btn-sm btn-outline-secondary px-3" @click="showVariantModal=false">{{ t('admin.productModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold px-4" @click="saveVariant">{{ addVariantMode ? t('admin.variantModal.addVariant') : t('admin.productModal.update') }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Bootstrap .text-light hardcode mau trang co dinh — ghi de theo theme hien tai, dong bo
   ProductsTable.vue. CSS scoped khong ke thua qua bien gioi component nen phai copy lai. */
.text-light {
  color: var(--text-primary) !important;
}
</style>
