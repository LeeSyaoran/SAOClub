<script setup>
import { ref, reactive, computed, watch, onMounted } from "vue";
import JsBarcode from "jsbarcode";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as SanPhamService from "../../services/SanPhamService.js";
import * as BienTheSanPhamService from "../../services/BienTheSanPhamService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as DanhMucService from "../../services/DanhMucService.js";
import * as DmService from "../../services/DmService.js";
import { authHeaders } from "../../services/api.js";
import { formatPrice, statusLabel } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { refreshInventory } from "../../stores/inventory.js";
import { SuppliersStore, ensureSuppliers } from "../../stores/suppliers.js";
import { Camera, Image, Cpu, MemoryStick, HardDrive, Monitor, Barcode } from '@lucide/vue';
import Pagination from "../common/Pagination.vue";
import SearchSelect from "../common/SearchSelect.vue";
import { usePagination } from "../../composables/usePagination.js";
import { useAutoHideOnScroll } from "../../composables/useAutoHideOnScroll.js";
import ProductDetailModal from "./ProductDetailModal.vue";

// Không dựa vào ProductsTable.vue (tab anh em) đã tải sẵn ProductsStore — self-contained,
// đúng pattern ensureX() dùng chung toàn app (no-op nếu đã tải/đang tải).
onMounted(() => { ensureProducts(); });

// Danh sách PHẲNG mọi biến thể (sửa/thêm/xóa trực tiếp) — tách ra khỏi ProductsTable.vue
// (vốn trước đây phải mở "Chi tiết sản phẩm" rồi mới sửa được 1 biến thể) để đứng ngang
// hàng CPU/RAM/GPU/Ổ cứng, đúng yêu cầu "1 tab riêng bên ngoài".
const props = defineProps({
  readonly: { type: Boolean, default: false },
  filterSanPhamId: { type: Number, default: null },
  // Giá nhập (giá vốn) là số nhạy cảm — chỉ quản lý mới cần thấy biên lợi nhuận.
  // Truyền :can-view-cost="false" cho nhân viên bán hàng để ẩn cả cột lẫn dòng trong chi tiết.
  canViewCost: { type: Boolean, default: true },
});

// t() có thể chưa có key mới → rơi về chuỗi tiếng Việt mặc định thay vì hiện trơ key ra UI.
// Thêm key vào i18n lúc nào cũng được, không phải sửa lại component.
const tt = (key, fallback) => {
  const s = t(key);
  return !s || s === key ? fallback : s;
};

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
const suppliers = computed(() => SuppliersStore.items ?? []);

// ── Tim kiem + bo loc ────────────────────────────────────────────────────────────────
// Danh sach lua chon cua 3 dropdown lay thang tu du lieu dang hien (ProductsStore.items),
// KHONG goi them API danh muc/thuong hieu: bo loc luon khop 100% voi nhung gi co trong
// bang, khong bao gio hien mot hang ma loc ra 0 dong.
const variantSearch = ref("");
const filterThuongHieu = ref("");
const filterDanhMuc = ref("");
const filterTrangThai = ref("");
const isFilterOpen = ref(false);

// Thanh cong cu + bo loc dinh sticky tren dau bang, tu an khi cuon xuong / hien lai
// khi cuon len (giong hieu ung ben HangHoa.vue).
const stickyHeadEl = ref(null);
const { hidden: barHidden } = useAutoHideOnScroll(stickyHeadEl);

const allVariants = computed(() => {
  const all = ProductsStore.items ?? [];
  return props.filterSanPhamId != null
    ? all.filter((p) => p.sanPhamId === props.filterSanPhamId)
    : all;
});

const optionsOf = (idKey, nameKey) => {
  const map = new Map();
  allVariants.value.forEach((p) => {
    if (p[idKey] != null && !map.has(p[idKey])) map.set(p[idKey], p[nameKey] ?? "—");
  });
  return [...map].map(([value, label]) => ({ value, label }))
    .sort((a, b) => String(a.label).localeCompare(String(b.label), "vi"));
};
const brandOptions = computed(() => optionsOf("thuongHieuId", "tenThuongHieu"));
const categoryOptions = computed(() => optionsOf("danhMucId", "tenDanhMuc"));
const statusOptions = computed(() =>
  [...new Set(allVariants.value.map((p) => p.trangThai).filter(Boolean))]
    .map((value) => ({ value, label: statusLabel(value) }))
);

const filteredVariants = computed(() => {
  const q = variantSearch.value.trim().toLowerCase();
  return allVariants.value.filter((p) => {
    if (filterThuongHieu.value !== "" && String(p.thuongHieuId) !== String(filterThuongHieu.value)) return false;
    if (filterDanhMuc.value !== "" && String(p.danhMucId) !== String(filterDanhMuc.value)) return false;
    if (filterTrangThai.value !== "" && p.trangThai !== filterTrangThai.value) return false;
    if (!q) return true;
    return [p.tenSanPham, p.maSku, p.barcodeBienThe, p.mauSac, p.cpu]
      .some((f) => (f ?? "").toString().toLowerCase().includes(q));
  });
});

const activeFilterCount = computed(() =>
  [filterThuongHieu.value, filterDanhMuc.value, filterTrangThai.value].filter((v) => v !== "").length
);
const clearFilters = () => {
  variantSearch.value = "";
  filterThuongHieu.value = "";
  filterDanhMuc.value = "";
  filterTrangThai.value = "";
};

const { currentPage, totalPages, pagedItems: pagedVariants, pageSize } = usePagination(filteredVariants);
// Đổi bộ lọc mà vẫn đứng ở trang 5 thì bảng trông như rỗng — luôn kéo về trang đầu.
watch([variantSearch, filterThuongHieu, filterDanhMuc, filterTrangThai], () => { currentPage.value = 0; });

// Bỏ tiền tố hãng CPU (Intel Core/AMD Ryzen) — dư thừa, không cần trong bảng liệt kê gọn,
// tên đầy đủ vẫn hiện nguyên trong ô chi tiết lúc bấm vào dòng.
const shortCpu = (cpu) => cpu?.replace(/^(Intel Core|AMD Ryzen)\s+/i, '') ?? '';
const configLabel = (p) => [shortCpu(p.cpu), p.ram, p.oCung, p.gpu].filter(Boolean).join(' · ') || '—';
const stockOf = (p) => Number(p.soLuongTon ?? 0);
const stockClass = (p) => (stockOf(p) === 0 ? 'vt-stock--out' : stockOf(p) <= 5 ? 'vt-stock--low' : '');
// Biên lợi nhuận — chỉ có ý nghĩa khi biết cả giá vốn lẫn giá bán
const marginOf = (p) => {
  const ban = Number(p.giaBan ?? 0);
  const nhap = Number(p.giaNhap ?? 0);
  if (!ban || !nhap) return null;
  return (((ban - nhap) / ban) * 100).toFixed(1);
};
const colCount = computed(() => (props.canViewCost ? 10 : 9));

// ── Ve ma vach ───────────────────────────────────────────────────────────────────────
// Mã vạch sinh từ CSDL là EAN-13 (13 số, có chữ số kiểm tra) → vẽ đúng chuẩn EAN13 để máy
// quét đọc được; mã cũ/nhập tay không đủ chuẩn thì rơi về CODE128 (mã hoá được mọi ký tự).
const barcodeFormat = (v) => (/^\d{13}$/.test(String(v ?? '')) ? 'EAN13' : 'CODE128');
const drawBarcode = (el, value, opts = {}) => {
  if (!el || !value) return;
  const base = { height: 24, width: 1.2, displayValue: false, margin: 0, ...opts };
  try {
    JsBarcode(el, value, { format: barcodeFormat(value), ...base });
  } catch {
    // EAN13 sai chữ số kiểm tra sẽ ném lỗi — vẫn vẽ được dưới dạng CODE128
    try { JsBarcode(el, value, { format: 'CODE128', ...base }); } catch { /* bỏ qua */ }
  }
};
// Ve qua ref callback ngay khi <svg> mount — chi render cho dong dang hien (pagedVariants),
// khong ton cong ve het 41+ bien the cung luc.
const renderBarcodeBig = (el, value) => drawBarcode(el, value, { height: 60, width: 2, displayValue: true, fontSize: 14 });

const escapeHtml = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// In tem 58mm — dựng SVG rời rồi ghi thẳng vào cửa sổ in, không cần thư viện in riêng.
const printLabel = (p) => {
  if (!p?.barcodeBienThe) {
    showToast(tt('admin.variants.noBarcode', 'Phiên bản này chưa có mã vạch để in'), 'error');
    return;
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  drawBarcode(svg, p.barcodeBienThe, { height: 55, width: 1.8, displayValue: true, fontSize: 13 });
  svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');

  const win = window.open('', '_blank', 'width=430,height=560');
  if (!win) {
    showToast(tt('admin.variants.popupBlocked', 'Trình duyệt đang chặn cửa sổ in — cho phép pop-up rồi thử lại'), 'error');
    return;
  }
  win.document.write(`<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
    <title>Tem ${escapeHtml(p.maSku)}</title>
    <style>
      *{box-sizing:border-box} body{margin:0;padding:16px;font-family:"Segoe UI",Roboto,Arial,sans-serif;color:#111}
      .tem{width:58mm;border:1px dashed #bbb;border-radius:6px;padding:8px 10px;text-align:center}
      .ten{font-size:11px;font-weight:700;line-height:1.3;word-break:break-word}
      .cfg{font-size:9px;color:#555;line-height:1.3;margin:2px 0 4px;word-break:break-word}
      .sku{font-size:9px;font-family:ui-monospace,Menlo,monospace;margin-bottom:4px}
      .gia{font-size:13px;font-weight:700;margin-top:4px}
      @media print{ body{padding:0} .tem{border:none} }
    </style></head><body>
    <div class="tem">
      <div class="ten">${escapeHtml(p.tenSanPham)}</div>
      <div class="cfg">${escapeHtml([p.mauSac, configLabel(p)].filter(Boolean).join(' · '))}</div>
      <div class="sku">SKU: ${escapeHtml(p.maSku)}</div>
      ${svg.outerHTML}
      <div class="gia">${escapeHtml(formatPrice(p.giaBan))}</div>
    </div></body></html>`);
  win.document.close();
  win.focus();
  setTimeout(() => win.print(), 350);
};

// ── Bấm vào dòng → ô chi tiết (thay cho 2 nút "Chi tiết"/"Sửa" ở cuối mỗi dòng) ───────
// Mọi thao tác trên 1 biến thể gom về đây: xem thông tin trước, hành động nằm ở chân ô.
const showRowDetail = ref(false);
const selectedVariant = ref(null);
const openRowDetail = (p) => {
  selectedVariant.value = p;
  showRowDetail.value = true;
};
// Sau khi lưu, ProductsStore đổi tham chiếu → lấy lại đúng bản mới nhất theo bienTheId
const detailVariant = computed(() => {
  const id = selectedVariant.value?.bienTheId;
  if (id == null) return null;
  return (ProductsStore.items ?? []).find((p) => p.bienTheId === id) ?? selectedVariant.value;
});

// ── Modal "Chi tiet san pham" (dung lai ProductDetailModal.vue) — xem CA san pham ─────
const showDetailModal = ref(false);
const detailSanPhamId = ref(null);
const detailSanPhamName = ref("");
const detailBienTheId = ref(null);
const openProductDetail = (p) => {
  detailSanPhamId.value = p.sanPhamId;
  detailSanPhamName.value = p.tenSanPham;
  detailBienTheId.value = null; // xem toàn bộ phiên bản của sản phẩm, không lọc 1 dòng
  showRowDetail.value = false;
  showDetailModal.value = true;
};

// ── Modal them/sua bien the ───────────────────────────────────────────────────────────
const showVariantModal = ref(false);
const editingId = ref(null); // sanPhamId dang sua (null = dang them bien the moi)
const formError = ref("");
const saving = ref(false);
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

// ── Combobox thật (SearchSelect, không dùng datalist trình duyệt — popup datalist
// không style được, mỗi máy/trình duyệt hiện một kiểu xấu khác nhau) + tag hiện giá trị
// đang chọn bên dưới kèm nút xóa. Combobox vẫn luôn còn đó để chọn lại. ─────────────────
const NONE_LABEL = () => tt('admin.productModal.noneOption', 'Không chọn');
const cpuOptions = computed(() => [
  { value: null, label: NONE_LABEL() },
  ...cpuList.value.map((c) => ({ value: c.cpuId, label: c.tenCpu })),
]);
const gpuOptions = computed(() => [
  { value: null, label: NONE_LABEL() },
  ...gpuList.value.map((g) => ({ value: g.gpuId, label: g.tenGpu })),
]);
const ramOptions = computed(() => [
  { value: null, label: NONE_LABEL() },
  ...ramList.value.map((r) => ({ value: r.ramId, label: r.dungLuong })),
]);
const oCungOptions = computed(() => [
  { value: null, label: NONE_LABEL() },
  ...oCungList.value.map((o) => ({ value: o.oCungId, label: o.loaiOcung })),
]);
// Tên hiển thị cho tag của các trường chọn theo ID
const cpuName = (id) => cpuList.value.find((c) => c.cpuId === id)?.tenCpu ?? '';
const gpuName = (id) => gpuList.value.find((g) => g.gpuId === id)?.tenGpu ?? '';
const ramName = (id) => ramList.value.find((r) => r.ramId === id)?.dungLuong ?? '';
const oCungName = (id) => oCungList.value.find((o) => o.oCungId === id)?.loaiOcung ?? '';

// Trường chuỗi/số không có bảng riêng (màu, màn hình, HĐH, pin, bảo hành, trọng lượng) —
// gợi ý gộp: danh sách cố định + mọi giá trị thực tế đang có trong dữ liệu + giá trị hiện
// tại của form (không mất khi sửa 1 biến thể có giá trị lạ, hiếm, không nằm trong gợi ý).
const stringOptionsFor = (field, base) => {
  const real = allVariants.value.map((p) => p[field]).filter((v) => v !== null && v !== undefined && v !== '');
  const cur = form[field];
  const all = new Set([...base, ...real]);
  if (cur !== null && cur !== undefined && cur !== '') all.add(cur);
  return [...all].map((v) => ({ value: v, label: String(v) }));
};
const mauSacOptions = computed(() => stringOptionsFor('mauSac', ['Đen', 'Trắng', 'Bạc', 'Xám', 'Xanh Dương', 'Xanh Lá', 'Đỏ', 'Vàng', 'Hồng', 'Tím', 'Cam', 'Nâu']));
const manHinhOptions = computed(() => stringOptionsFor('kichThuocManHinh', ['15.6" FHD 60Hz', '15.6" FHD 144Hz', '15.6" QHD 240Hz', '16" 2.5K 120Hz', '16" FHD 165Hz', '16" WQXGA 165Hz', '16" 2.8K OLED 120Hz']));
const heDieuHanhOptions = computed(() => stringOptionsFor('heDieuHanh', ['Windows 11 Home', 'Windows 11 Pro', 'macOS', 'Không kèm HĐH']));
const pinOptions = computed(() => stringOptionsFor('pin', ['41Wh', '48Wh', '50Wh', '52Wh', '54Wh', '57Wh', '75Wh', '80Wh', '86Wh', '90Wh']));
const baoHanhOptions = computed(() => stringOptionsFor('baoHanhThang', [6, 12, 18, 24, 36]).sort((a, b) => Number(a.value) - Number(b.value)));
const trongLuongOptions = computed(() => stringOptionsFor('trongLuongKg', [1.2, 1.3, 1.5, 1.7, 1.8, 2.0, 2.3, 2.5]).sort((a, b) => Number(a.value) - Number(b.value)));

const emptyForm = () => ({
  bienTheId: null,
  tenSanPham: "",
  thuongHieuId: null,
  danhMucId: null,
  nhaCungCapId: null,
  loaiSanPham: "",
  maSku: "",
  barcodeBienThe: "",
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

// Sinh mã vạch EAN-13 hợp lệ ngay trên form (cùng công thức với file CSDL): '893' + 9 số +
// chữ số kiểm tra (vị trí lẻ ×1 + vị trí chẵn ×3, lấy phần bù 10). Có chữ số kiểm tra đúng
// thì máy quét mới đọc được.
const checkDigitEan13 = (base12) => {
  let sum = 0;
  for (let i = 0; i < 12; i++) sum += Number(base12[i] || 0) * (i % 2 === 0 ? 1 : 3);
  return String((10 - (sum % 10)) % 10);
};
const generateBarcode = () => {
  const used = new Set((ProductsStore.items ?? []).map((p) => p.barcodeBienThe).filter(Boolean));
  for (let i = 0; i < 60; i++) {
    const base = '893' + String(Math.floor(Math.random() * 1e9)).padStart(9, '0');
    const code = base + checkDigitEan13(base);
    if (!used.has(code)) { form.barcodeBienThe = code; return; }
  }
};

// ── Them bien the moi cho 1 san pham DA TON TAI — can chon san pham truoc (khac
// ProductsTable.vue cu, truoc day mo tu trong "Chi tiet san pham" nen da co san context) ──
const addVariantMode      = ref(false);
const addVariantSanPhamId = ref(null);
const addVariantSanPhamName = ref('');
const variantProductSearch = ref('');

// San pham doc nhat (dedupe theo sanPhamId) de tim/chon khi them bien the moi
const distinctProducts = computed(() => {
  const map = new Map();
  (ProductsStore.items ?? []).forEach(p => { if (!map.has(p.sanPhamId)) map.set(p.sanPhamId, p); });
  return [...map.values()];
});
const searchedProducts = computed(() => {
  const q = variantProductSearch.value.trim().toLowerCase();
  // Không gõ gì -> liệt kê sẵn toàn bộ sản phẩm (số lượng sản phẩm nhỏ, không cần bắt gõ
  // trước mới thấy như ReturnsPanel.vue's order picker — đơn hàng nhiều hơn hẳn nên phải
  // bắt gõ, sản phẩm thì không).
  const list = q ? distinctProducts.value.filter(p => (p.tenSanPham ?? '').toLowerCase().includes(q)) : distinctProducts.value;
  return list.slice(0, 30);
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
  generateBarcode();
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
    barcodeBienThe: p.barcodeBienThe ?? "",
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
  showRowDetail.value = false;
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
  if (saving.value) return;
  saving.value = true;
  try {

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

  // Mã vạch là cột riêng của biến thể, có unique index — chặn sớm ở form cho khỏi ăn
  // lỗi 500 từ CSDL, và báo đúng chỗ sai thay vì "duplicate key" khó hiểu.
  const barcode = (form.barcodeBienThe ?? '').trim();
  if (barcode) {
    if (!/^\d{8,13}$/.test(barcode)) {
      formError.value = tt('admin.errors.barcodeFormat', 'Mã vạch chỉ gồm 8–13 chữ số');
      return;
    }
    const trung = (ProductsStore.items ?? []).some(
      (p) => p.barcodeBienThe === barcode && p.bienTheId !== form.bienTheId
    );
    if (trung) {
      formError.value = tt('admin.errors.barcodeDuplicate', 'Mã vạch này đã có phiên bản khác dùng');
      return;
    }
  }

  if (addVariantMode.value) {
    if (!addVariantSanPhamId.value) { formError.value = t('admin.variantModal.pickProductLabel'); return; }
    if (!form.maSku.trim()) { formError.value = t('admin.errors.skuRequired'); return; }
    const variantBody = {
      sanPhamId: addVariantSanPhamId.value,
      maSku: form.maSku,
      barcode: barcode || null,
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
      showToast(tt('admin.variants.addedToast', 'Đã thêm phiên bản mới'));
      await refreshProducts();
      // Biến thể mới phải hiện ngay ở "Hàng sắp về" bên Kho hàng — InventoryStore tách rời
      // ProductsStore, không tự làm mới theo.
      refreshInventory().catch(() => {});
    } catch (e) {
      formError.value = e.message;
    }
    return;
  }

  const body = {
    ...form,
    barcodeBienThe: barcode || null,
    barcode: barcode || null, // gửi kèm cả 2 tên trường để khớp dù DTO backend đặt tên nào
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
    showToast(tt('admin.variants.savedToast', 'Đã lưu thay đổi'));
    await refreshProducts();
  } catch (e) {
    formError.value = e.message;
  }
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <!-- ══════════ THANH CÔNG CỤ + BỘ LỌC — thẻ riêng, khóa trên đầu, tự ẩn khi cuộn xuống ══════════ -->
  <div ref="stickyHeadEl" class="vt-sticky-head" :class="{ 'is-hidden': barHidden }">
    <div class="vt-toolbar-card">
      <div class="vt-toolbar">
        <div class="vt-toolbar__left">
          <div class="vt-search">
            <i class="fa fa-search vt-search__icon"></i>
            <input v-model="variantSearch" :placeholder="tt('admin.variants.searchPlaceholder2', 'Tìm tên, SKU, màu…')" />
            <button v-if="variantSearch" class="vt-search__clear" :title="tt('admin.variants.clearSearch', 'Xóa tìm kiếm')" @click="variantSearch = ''">
              <i class="fa fa-times"></i>
            </button>
          </div>
        </div>

        <div class="vt-toolbar__right">
          <button type="button" class="vt-btn vt-btn--ghost" :class="{ 'is-on': isFilterOpen }" @click="isFilterOpen = !isFilterOpen">
            <i class="fa fa-filter"></i>
            {{ tt('admin.variants.filters', 'Bộ lọc') }}
            <span v-if="activeFilterCount" class="vt-filter-badge">{{ activeFilterCount }}</span>
            <i class="fa fa-chevron-down vt-caret" :class="{ 'is-open': isFilterOpen }"></i>
          </button>

          <button v-if="!readonly" class="vt-btn vt-btn--primary" @click="openAddVariantFlow">
            <i class="fa fa-plus"></i> {{ t('admin.variants.add') }}
          </button>
        </div>
      </div>
    </div>

    <!-- ══════════ BỘ LỌC (thu gọn) — thẻ riêng, chỉ hiện khi mở ══════════ -->
    <div class="vt-filter" :class="{ 'is-open': isFilterOpen }">
      <div class="vt-filter__panel">
        <div class="vt-filter__grid">
          <label class="vt-field">
            <span>{{ tt('admin.variants.filterBrand', 'Thương hiệu') }}</span>
            <select v-model="filterThuongHieu">
              <option value="">{{ tt('admin.variants.filterBrandAll', 'Tất cả thương hiệu') }}</option>
              <option v-for="o in brandOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>

          <label class="vt-field">
            <span>{{ tt('admin.variants.filterCategory', 'Danh mục') }}</span>
            <select v-model="filterDanhMuc">
              <option value="">{{ tt('admin.variants.filterCategoryAll', 'Tất cả danh mục') }}</option>
              <option v-for="o in categoryOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>

          <label class="vt-field">
            <span>{{ tt('admin.variants.filterStatus', 'Trạng thái') }}</span>
            <select v-model="filterTrangThai">
              <option value="">{{ tt('admin.variants.filterStatusAll', 'Tất cả trạng thái') }}</option>
              <option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>
        </div>

        <div class="vt-filter__foot">
          <span class="vt-filter__count">
            {{ filteredVariants.length }}/{{ allVariants.length }} {{ t('admin.variants.countSuffix') }}
          </span>
          <div class="vt-filter__btns">
            <button type="button" class="vt-btn vt-btn--ghost" @click="clearFilters">
              {{ tt('admin.variants.clearFilters', 'Xóa lọc') }}
            </button>
            <button type="button" class="vt-btn vt-btn--primary" @click="isFilterOpen = false">
              {{ tt('admin.variants.filterDone', 'Xong') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <!-- ══════════ BẢNG — thẻ riêng, bấm vào dòng để xem chi tiết ══════════ -->
  <div class="vt-card">
    <div v-if="ProductsStore.loading" class="vt-empty">{{ t('admin.variants.loading') }}</div>
    <div v-else class="vt-table-wrap">
      <table class="vt-table">
        <thead>
          <tr>
            <th class="vt-col-stt">{{ t('admin.common.stt') }}</th>
            <th class="vt-col-img">{{ t('admin.variants.colImage') }}</th>
            <th class="vt-col-sku">{{ t('admin.variants.colSku') }}</th>
            <th class="vt-col-name">{{ t('admin.variants.colProduct') }}</th>
            <th class="vt-col-config">{{ t('admin.variants.colConfig') }}</th>
            <th class="vt-col-color">{{ t('admin.variants.colColor') }}</th>
            <th class="vt-col-num">{{ tt('admin.variants.colStock', 'Tồn kho') }}</th>
            <th v-if="canViewCost" class="vt-col-price">{{ tt('admin.variants.colPriceBuy', 'Giá nhập') }}</th>
            <th class="vt-col-price">{{ t('admin.variants.colPriceSell') }}</th>
            <th class="vt-col-status">{{ t('admin.variants.colStatus') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(p, idx) in pagedVariants" :key="p.bienTheId"
            class="vt-row" tabindex="0"
            :title="tt('admin.variants.rowHint', 'Bấm để xem chi tiết')"
            @click="openRowDetail(p)" @keydown.enter.prevent="openRowDetail(p)"
          >
            <td class="vt-muted">{{ currentPage * pageSize + idx + 1 }}</td>
            <td>
              <div class="vt-thumb">
                <img v-if="p.hinhAnhChinh" :src="p.hinhAnhChinh" :alt="p.tenSanPham" />
                <Image v-else :size="14" color="var(--muted)" />
              </div>
            </td>
            <td class="vt-sku" :title="p.maSku">{{ p.maSku }}</td>
            <td class="vt-name" :title="p.tenSanPham">{{ p.tenSanPham }}</td>
            <td class="vt-config" :title="configLabel(p)">
              <div v-if="p.cpu || p.ram || p.oCung" class="vt-config__list">
                <span v-if="p.cpu"><Cpu :size="12" />{{ shortCpu(p.cpu) }}</span>
                <span v-if="p.ram"><MemoryStick :size="12" />{{ p.ram }}</span>
                <span v-if="p.oCung"><HardDrive :size="12" />{{ p.oCung }}</span>
              </div>
              <span v-else>—</span>
            </td>
            <td class="vt-color">{{ p.mauSac || '—' }}</td>
            <td class="vt-col-num">
              <span class="vt-stock" :class="stockClass(p)">{{ stockOf(p) }}</span>
            </td>
            <td v-if="canViewCost" class="vt-col-price vt-muted">{{ formatPrice(p.giaNhap) }}</td>
            <td class="vt-col-price vt-price">{{ formatPrice(p.giaBan) }}</td>
            <td>
              <span class="vt-tag" :class="p.trangThai === 'active' ? 'vt-tag--on' : 'vt-tag--off'">
                {{ statusLabel(p.trangThai) }}
              </span>
            </td>
          </tr>
          <tr v-if="filteredVariants.length === 0">
            <td :colspan="colCount" class="vt-empty">{{ t('admin.variants.empty') }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="totalPages > 1" class="vt-pager">
        <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
      </div>
    </div>
  </div>

  <!-- ══════════ Ô CHI TIẾT MỘT PHIÊN BẢN (thay cho 2 nút cuối dòng) ══════════ -->
  <div v-if="showRowDetail && detailVariant" class="vt-mask" @click.self="showRowDetail = false">
    <div class="vt-modal vt-modal--detail" role="dialog" aria-modal="true">
      <div class="vt-modal__head">
        <div class="vt-head-main">
          <div class="vt-head-title">{{ detailVariant.tenSanPham }}</div>
          <div class="vt-head-sub">
            <span class="vt-chip">{{ detailVariant.maSku }}</span>
            <span class="vt-tag" :class="detailVariant.trangThai === 'active' ? 'vt-tag--on' : 'vt-tag--off'">
              {{ statusLabel(detailVariant.trangThai) }}
            </span>
            <span>{{ detailVariant.tenDanhMuc || '—' }} » {{ detailVariant.tenThuongHieu || '—' }}</span>
          </div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showRowDetail = false"></button>
      </div>

      <div class="vt-modal__body">
        <div class="vt-detail-top">
          <div class="vt-detail-media">
            <div class="vt-detail-img">
              <img v-if="detailVariant.hinhAnhChinh" :src="detailVariant.hinhAnhChinh" :alt="detailVariant.tenSanPham" />
              <Image v-else :size="28" color="var(--text-muted)" />
            </div>
            <div class="vt-barcode-box">
              <svg v-if="detailVariant.barcodeBienThe" :ref="(el) => renderBarcodeBig(el, detailVariant.barcodeBienThe)"></svg>
              <div v-else class="vt-barcode-empty">
                <Barcode :size="18" /> {{ tt('admin.variants.noBarcodeShort', 'Chưa có mã vạch') }}
              </div>
            </div>
          </div>

          <dl class="vt-detail-grid">
            <div>
              <dt>{{ tt('admin.variants.colStock', 'Tồn kho') }}</dt>
              <dd><span class="vt-stock" :class="stockClass(detailVariant)">{{ stockOf(detailVariant) }}</span></dd>
            </div>
            <div><dt>{{ t('admin.variants.colPriceSell') }}</dt><dd class="vt-price">{{ formatPrice(detailVariant.giaBan) }}</dd></div>
            <div v-if="canViewCost"><dt>{{ tt('admin.variants.colPriceBuy', 'Giá nhập') }}</dt><dd>{{ formatPrice(detailVariant.giaNhap) }}</dd></div>
            <div v-if="canViewCost && marginOf(detailVariant)">
              <dt>{{ tt('admin.variants.margin', 'Biên lợi nhuận') }}</dt><dd>{{ marginOf(detailVariant) }}%</dd>
            </div>
            <div><dt>{{ t('admin.productModal.colorLabel') }}</dt><dd>{{ detailVariant.mauSac || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.warrantyLabel') }}</dt><dd>{{ detailVariant.baoHanhThang ?? '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.cpuLabel') }}</dt><dd>{{ detailVariant.cpu || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.ramLabel') }}</dt><dd>{{ detailVariant.ram || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.storageLabel') }}</dt><dd>{{ detailVariant.oCung || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.gpuLabel') }}</dt><dd>{{ detailVariant.gpu || '—' }}</dd></div>
            <div><dt><Monitor :size="12" /> {{ t('admin.productModal.screenLabel') }}</dt><dd>{{ detailVariant.kichThuocManHinh || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.osLabel') }}</dt><dd>{{ detailVariant.heDieuHanh || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.batteryLabel') }}</dt><dd>{{ detailVariant.pin || '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.weightLabel') }}</dt><dd>{{ detailVariant.trongLuongKg ? detailVariant.trongLuongKg + ' kg' : '—' }}</dd></div>
            <div><dt>{{ t('admin.productModal.supplierLabel') }}</dt><dd>{{ detailVariant.tenNhaCungCap || '—' }}</dd></div>
            <div><dt>{{ tt('admin.variants.colBarcode', 'Mã vạch') }}</dt><dd class="vt-mono">{{ detailVariant.barcodeBienThe || '—' }}</dd></div>
          </dl>
        </div>

        <div v-if="detailVariant.phanLoaiTen" class="vt-detail-block">
          <div class="vt-section-title">{{ t('admin.productModal.tagsLabel') }}</div>
          <div class="vt-tag-list">
            <span v-for="tag in detailVariant.phanLoaiTen.split(',')" :key="tag" class="vt-chip">{{ tag.trim() }}</span>
          </div>
        </div>

        <div v-if="detailVariant.moTa" class="vt-detail-block">
          <div class="vt-section-title">{{ t('admin.productModal.descLabel') }}</div>
          <p class="vt-desc">{{ detailVariant.moTa }}</p>
        </div>
      </div>

      <div class="vt-modal__foot">
        <button class="vt-btn vt-btn--ghost" @click="showRowDetail = false">{{ t('common.close') }}</button>
        <div class="vt-foot-right">
          <button class="vt-btn vt-btn--ghost" @click="openProductDetail(detailVariant)">
            {{ tt('admin.variants.viewProduct', 'Xem cả sản phẩm') }}
          </button>
          <button class="vt-btn vt-btn--ghost" @click="printLabel(detailVariant)">
            {{ tt('admin.variants.printLabel', 'In tem mã') }}
          </button>
          <button v-if="!readonly" class="vt-btn vt-btn--primary" @click="openEdit(detailVariant)">
            {{ t('admin.variants.edit') }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- ══════════ MODAL THÊM / SỬA PHIÊN BẢN ══════════ -->
  <div v-if="showVariantModal" class="vt-mask" @click.self="showVariantModal = false">
    <div class="vt-modal vt-modal--form" role="dialog" aria-modal="true">
      <div class="vt-modal__head">
        <div class="vt-head-main">
          <div class="vt-head-title">{{ addVariantMode ? t('admin.variantModal.addVariant') : t('admin.productModal.titleEdit') }}</div>
          <div v-if="addVariantMode && addVariantSanPhamName" class="vt-head-sub">{{ addVariantSanPhamName }}</div>
          <div v-else-if="editingId" class="vt-head-sub">{{ t('admin.productModal.idLabel') }} {{ editingId }}</div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showVariantModal = false"></button>
      </div>

      <!-- Bước chọn sản phẩm (chỉ khi thêm phiên bản mới) -->
      <div v-if="addVariantMode && !addVariantSanPhamId" class="vt-modal__body">
        <label class="vt-label">{{ t('admin.variantModal.pickProductLabel') }}</label>
        <input v-model="variantProductSearch" class="form-control form-control-sm vt-input" :placeholder="t('admin.variantModal.pickProductPlaceholder')" />
        <div class="vt-pick-list">
          <div v-for="p in searchedProducts" :key="p.sanPhamId" class="vt-pick-item" @click="pickProductForVariant(p)">
            {{ p.tenSanPham }} <span class="vt-muted">— {{ p.tenThuongHieu }}</span>
          </div>
          <div v-if="searchedProducts.length === 0" class="vt-pick-empty">{{ t('admin.variantModal.pickProductEmpty') }}</div>
        </div>
      </div>

      <div v-else class="vt-modal__body">
        <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>

        <div v-if="addVariantMode" class="vt-picked">
          <span>{{ addVariantSanPhamName }}</span>
          <button class="vt-btn vt-btn--ghost" @click="changeProductForVariant">{{ t('admin.variantModal.changeProduct') }}</button>
        </div>

        <!-- ── Thông tin cơ bản ── -->
        <div class="vt-section-title">{{ t('admin.productModal.sectionBasic') }}</div>
        <div class="vt-panel">
          <div class="row g-3">
            <div v-if="!addVariantMode" class="col-8">
              <label class="vt-label">{{ t('admin.productModal.nameLabel') }}</label>
              <input v-model="form.tenSanPham" class="form-control form-control-sm vt-input" :placeholder="t('admin.productModal.namePlaceholder')" />
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.skuLabel') }}</label>
              <input v-model="form.maSku" class="form-control form-control-sm vt-input vt-mono" :placeholder="t('admin.productModal.skuPlaceholder')" />
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.variantModal.barcodeLabel') }}</label>
              <div class="vt-inline">
                <input v-model.trim="form.barcodeBienThe" class="form-control form-control-sm vt-input vt-mono" :placeholder="t('admin.variantModal.barcodePlaceholder')" />
                <button class="vt-btn vt-btn--ghost vt-icon-btn" type="button" :title="tt('admin.variants.genBarcode', 'Sinh mã vạch EAN-13')" @click="generateBarcode">
                  <i class="fa fa-refresh"></i>
                </button>
              </div>
            </div>
            <div v-if="!addVariantMode" class="col-4">
              <label class="vt-label">{{ t('admin.productModal.typeLabel') }}</label>
              <select v-model="form.loaiSanPham" class="form-select form-select-sm vt-input">
                <option value="" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                <option value="LAPTOP">{{ t('admin.productModal.typeLaptop') }}</option>
                <option value="PHU_KIEN">{{ t('admin.productModal.typeAccessory') }}</option>
              </select>
            </div>
            <div class="col-4">
              <!-- Chỉ active/inactive: ràng buộc CK_bt_trangthai của bảng biến thể không nhận
                   'ngung_kinh_doanh' (giá trị đó chỉ hợp lệ ở bảng san_pham) — trước đây còn
                   gõ nhầm thành 'ngung_kin_doanh' nên lưu là đổ. -->
              <label class="vt-label">{{ t('admin.productModal.statusLabel') }}</label>
              <select v-model="form.trangThai" class="form-select form-select-sm vt-input">
                <option value="active">{{ t('admin.productModal.statusActive') }}</option>
                <option value="inactive">{{ t('admin.productModal.statusInactive') }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.colorLabel') }}</label>
              <SearchSelect v-model="form.mauSac" :options="mauSacOptions" :placeholder="t('admin.productModal.colorPlaceholder')" />
              <div v-if="form.mauSac" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ form.mauSac }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.mauSac = ''">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.warrantyLabel') }}</label>
              <SearchSelect v-model="form.baoHanhThang" :options="baoHanhOptions" placeholder="Số tháng" />
            </div>
            <template v-if="!addVariantMode">
              <div class="col-4">
                <label class="vt-label">{{ t('admin.productModal.brandLabel') }}</label>
                <select v-model="form.thuongHieuId" class="form-select form-select-sm vt-input">
                  <option :value="null" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                  <option v-for="b in brands" :key="b.thuongHieuId" :value="b.thuongHieuId">{{ b.tenThuongHieu }}</option>
                </select>
              </div>
              <div class="col-4">
                <label class="vt-label">{{ t('admin.productModal.categoryLabel') }}</label>
                <select v-model="form.danhMucId" class="form-select form-select-sm vt-input">
                  <option :value="null" disabled>{{ t('admin.productModal.selectPlaceholder') }}</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.tenDanhMuc }}</option>
                </select>
              </div>
              <div class="col-4">
                <label class="vt-label">{{ t('admin.productModal.supplierLabel') }}</label>
                <select v-model="form.nhaCungCapId" class="form-select form-select-sm vt-input">
                  <option :value="null">{{ t('admin.productModal.noneOption') }}</option>
                  <option v-for="s in suppliers" :key="s.nhaCungCapId" :value="s.nhaCungCapId">{{ s.tenNhaCungCap }}</option>
                </select>
              </div>
            </template>
          </div>
        </div>

        <!-- ── Cấu hình kỹ thuật ── -->
        <div class="vt-section-title">{{ t('admin.productModal.sectionTech') }}</div>
        <div class="vt-panel">
          <div class="row g-3">
            <div class="col-6">
              <label class="vt-label">{{ t('admin.productModal.cpuLabel') }}</label>
              <SearchSelect v-model="form.cpuId" :options="cpuOptions" :placeholder="t('admin.productModal.noneOption')" />
              <div v-if="form.cpuId" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ cpuName(form.cpuId) }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.cpuId = null">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-6">
              <label class="vt-label">{{ t('admin.productModal.gpuLabel') }}</label>
              <SearchSelect v-model="form.gpuId" :options="gpuOptions" :placeholder="t('admin.productModal.noneOption')" />
              <div v-if="form.gpuId" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ gpuName(form.gpuId) }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.gpuId = null">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.ramLabel') }}</label>
              <SearchSelect v-model="form.ramId" :options="ramOptions" :placeholder="t('admin.productModal.noneOption')" />
              <div v-if="form.ramId" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ ramName(form.ramId) }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.ramId = null">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.storageLabel') }}</label>
              <SearchSelect v-model="form.oCungId" :options="oCungOptions" :placeholder="t('admin.productModal.noneOption')" />
              <div v-if="form.oCungId" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ oCungName(form.oCungId) }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.oCungId = null">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.screenLabel') }}</label>
              <SearchSelect v-model="form.kichThuocManHinh" :options="manHinhOptions" :placeholder="t('admin.productModal.screenPlaceholder')" />
              <div v-if="form.kichThuocManHinh" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ form.kichThuocManHinh }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.kichThuocManHinh = ''">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.osLabel') }}</label>
              <SearchSelect v-model="form.heDieuHanh" :options="heDieuHanhOptions" :placeholder="t('admin.productModal.osPlaceholder')" />
              <div v-if="form.heDieuHanh" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ form.heDieuHanh }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.heDieuHanh = ''">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.batteryLabel') }}</label>
              <SearchSelect v-model="form.pin" :options="pinOptions" :placeholder="t('admin.productModal.batteryPlaceholder')" />
              <div v-if="form.pin" class="vt-picked-tags">
                <span class="vt-tag-pill">
                  {{ form.pin }}
                  <button type="button" aria-label="Bỏ chọn" @click="form.pin = ''">&times;</button>
                </span>
              </div>
            </div>
            <div class="col-4">
              <label class="vt-label">{{ t('admin.productModal.weightLabel') }}</label>
              <SearchSelect v-model="form.trongLuongKg" :options="trongLuongOptions" placeholder="Trọng lượng (kg)" />
            </div>
          </div>
        </div>

        <!-- ── Giá ── -->
        <div class="vt-section-title">{{ t('admin.productModal.sectionPrice') }}</div>
        <div class="vt-panel">
          <div class="row g-3">
            <div class="col-6">
              <label class="vt-label">{{ t('admin.productModal.priceSellLabel') }}</label>
              <input v-model="form.giaBan" type="number" min="0" class="form-control form-control-sm vt-input" />
            </div>
            <div class="col-6">
              <label class="vt-label">{{ t('admin.productModal.priceBuyLabel') }}</label>
              <input v-model="form.giaNhap" type="number" min="0" class="form-control form-control-sm vt-input" />
            </div>
          </div>
        </div>

        <!-- ── Hình ảnh, mô tả, phân loại ── -->
        <div class="vt-section-title">{{ t('admin.productModal.sectionMedia') }}</div>
        <div class="vt-panel">
          <div class="row g-3">
            <div class="col-12">
              <label class="vt-label">{{ t('admin.productModal.imageLabel') }}</label>
              <div class="vt-upload">
                <label class="vt-upload__box">
                  <img v-if="imagePreview" :src="imagePreview" />
                  <template v-else>
                    <Camera :size="22" color="var(--text-muted)" />
                    <span>{{ t('admin.productModal.imageClickToChoose') }}</span>
                  </template>
                  <input type="file" accept="image/*" class="d-none" @change="handleImageFile" />
                </label>
                <div v-if="imageFilePending" class="vt-hl small">{{ imageFilePending.name }}</div>
                <div v-else class="vt-muted small">{{ t('admin.productModal.imageFormats') }}</div>
              </div>
            </div>
            <template v-if="!addVariantMode">
              <div class="col-12">
                <label class="vt-label">{{ t('admin.productModal.descLabel') }}</label>
                <textarea v-model="form.moTa" rows="3" class="form-control form-control-sm vt-input"></textarea>
              </div>
              <div class="col-6">
                <label class="vt-label">
                  {{ t('admin.productModal.tagsLabel') }} <span class="vt-hl">{{ t('admin.productModal.tagsHint') }}</span>
                </label>
                <div class="vt-tag-list">
                  <button
                    v-for="opt in PHAN_LOAI_TAG_OPTIONS" :key="opt.value" type="button"
                    class="vt-tag-btn" :class="{ 'is-on': isTagSelected(opt.value) }"
                    @click="toggleTag(opt.value)"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              </div>
              <div class="col-6">
                <label class="vt-label">
                  {{ t('admin.productModal.tagNameLabel') }} <span class="vt-muted">{{ t('admin.productModal.tagNameHint') }}</span>
                </label>
                <input v-model="form.phanLoaiTen" class="form-control form-control-sm vt-input" :placeholder="t('admin.productModal.tagNamePlaceholder')" />
              </div>
            </template>
          </div>
        </div>

        <!-- ── Serial (chỉ khi thêm mới) ── -->
        <template v-if="addVariantMode">
          <div class="vt-section-title">{{ t('admin.productModal.sectionSerial') }}</div>
          <div class="vt-panel">
            <label class="vt-label">{{ t('admin.productModal.serialLabel') }}</label>
            <input v-model="soSerialMoi" class="form-control form-control-sm vt-input vt-mono" :placeholder="t('admin.productModal.serialPlaceholder')" />
            <div class="vt-hint">{{ t('admin.productModal.serialHint') }}</div>
          </div>
        </template>
      </div>

      <div v-if="!addVariantMode || addVariantSanPhamId" class="vt-modal__foot vt-modal__foot--end">
        <button class="vt-btn vt-btn--ghost" @click="showVariantModal = false">{{ t('admin.productModal.cancel') }}</button>
        <button class="vt-btn vt-btn--primary" :disabled="saving" @click="saveVariant">
          {{ addVariantMode ? t('admin.variantModal.addVariant') : t('admin.productModal.update') }}
        </button>
      </div>
    </div>
  </div>

  <ProductDetailModal
    v-model="showDetailModal"
    :san-pham-id="detailSanPhamId"
    :san-pham-name="detailSanPhamName"
    :only-bien-the-ids="detailBienTheId"
  />
</template>

<style scoped>
/* Nhại đúng bảng màu + tỉ lệ của HangHoa.vue (tab "Hàng hóa") để 2 màn hình đồng bộ
   phong cách — cố tình dùng cùng giá trị hex/hồng cứng như HangHoa.vue thay vì biến
   theme sáng/tối dùng chung, cho khớp pixel với màn hình đó. */
.vt-card, .vt-sticky-head, .vt-mask {
  --pink-50:  #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-300: #f7a8c8;
  --pink-500: #ec4899;
  --pink-600: #db2777;
  --pink-700: #a81b5d;

  --ink:   #1f2937;
  --muted: #6b7280;
  --line:  #f1dbe6;
  --field: #d9b3c6;
  --danger: #dc2626;
  --ok-bg:   #ecfdf5;
  --ok-text: #047857;

  --sh-1: 0 1px 2px rgba(168, 27, 93, .06);
  --sh-2: 0 4px 14px rgba(168, 27, 93, .10);
}
.vt-card, .vt-sticky-head { font-size: 14px; color: var(--ink); }

/* ══════════ THẺ BAO NGOÀI ══════════ */
/* .vt-card (bảng) là thẻ đứng riêng, KHÔNG chứa .vt-sticky-head bên trong nữa — tách
   thanh công cụ/bộ lọc và bảng thành 2 thẻ độc lập có khoảng cách, giống hh-bar/
   hh-filter/hh-card bên HangHoa.vue, thay vì gộp chung 1 khối lớn như trước. */
.vt-card {
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  overflow: hidden; box-shadow: var(--sh-1);
}
/* Sticky-head chỉ là khung định vị (dính đầu + tự ẩn khi cuộn) — KHÔNG mang nền/viền
   riêng, để .vt-toolbar-card và .vt-filter__panel bên trong tách hẳn thành 2 thẻ
   bo tròn độc lập có khoảng cách, giống bố cục hh-bar/hh-filter/hh-card bên HangHoa.vue. */
.vt-sticky-head {
  position: sticky; top: 0; z-index: 5;
  transition: transform .25s ease;
}
.vt-sticky-head.is-hidden { transform: translateY(-100%); }
.vt-muted { color: var(--muted); }
.vt-hl { color: var(--pink-600); font-weight: 600; }

.vt-toolbar-card {
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  margin-bottom: 12px; box-shadow: var(--sh-1); overflow: hidden;
}

/* ══════════ NÚT ══════════ */
.vt-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 999px; border: 1px solid transparent;
  font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer; white-space: nowrap;
  transition: background-color .15s, border-color .15s, color .15s, box-shadow .15s;
}
.vt-btn--primary { background: var(--pink-600); color: #fff; box-shadow: var(--sh-1); }
.vt-btn--primary:hover:not(:disabled) { background: var(--pink-700); box-shadow: var(--sh-2); }
.vt-btn--ghost { background: #fff; color: var(--pink-700); border-color: var(--pink-200); }
.vt-btn--ghost:hover:not(:disabled) { background: var(--pink-50); border-color: var(--pink-300); }
.vt-btn--ghost.is-on { background: var(--pink-100); border-color: var(--pink-300); }
.vt-btn:disabled { opacity: .45; cursor: not-allowed; }
.vt-btn:focus-visible { outline: 2px solid var(--pink-500); outline-offset: 2px; }
.vt-icon-btn { padding: 7px 10px; }

/* ══════════ THANH CÔNG CỤ ══════════ */
.vt-toolbar {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  padding: 12px 16px; background: #fff;
}
.vt-toolbar__left { display: flex; align-items: center; gap: 10px; flex: 1 1 auto; min-width: 0; }
.vt-toolbar__right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; margin-left: auto; }

.vt-search { position: relative; width: 320px; max-width: 100%; }
.vt-search input {
  width: 100%; padding: 8px 32px 8px 34px;
  border: 1px solid var(--pink-200); border-radius: 999px;
  font-size: 13px; background: var(--pink-50); color: var(--ink); font-family: inherit;
}
.vt-search input:focus { outline: none; border-color: var(--pink-500); background: #fff; box-shadow: 0 0 0 3px var(--pink-100); }
.vt-search__icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); color: var(--pink-500); pointer-events: none; }
.vt-search__clear {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  background: none; border: none; color: var(--muted); cursor: pointer; padding: 4px;
}
.vt-search__clear:hover { color: var(--pink-600); }

/* ══════════ BỘ LỌC (thu gọn) — thẻ riêng, bo tròn, tách khỏi thanh công cụ ══════════ */
.vt-filter { display: grid; grid-template-rows: 0fr; transition: grid-template-rows .22s ease, margin-bottom .22s ease; margin-bottom: 0; }
.vt-filter.is-open { grid-template-rows: 1fr; margin-bottom: 12px; }
.vt-filter__panel {
  overflow: hidden; background: #fff; border: 1px solid var(--line);
  border-radius: 14px; padding: 0 16px; transition: padding .22s ease; box-shadow: var(--sh-1);
}
.vt-filter.is-open .vt-filter__panel { padding: 16px; }
.vt-filter__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.vt-filter__foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--line);
}
.vt-filter__count { font-size: 12.5px; color: var(--muted); }
.vt-filter__btns { display: flex; gap: 8px; }

.vt-field { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.vt-field > span { font-size: 12px; font-weight: 700; color: var(--pink-700); }
.vt-field select {
  width: 100%; height: 34px; padding: 0 30px 0 11px; border-radius: 9px;
  border: 1px solid var(--field); background: #fff; color: var(--ink);
  font-size: 13px; font-family: inherit; cursor: pointer; appearance: none;
  background-image: linear-gradient(45deg, transparent 50%, var(--muted) 50%),
                    linear-gradient(135deg, var(--muted) 50%, transparent 50%);
  background-position: calc(100% - 15px) 14px, calc(100% - 10px) 14px;
  background-size: 5px 5px, 5px 5px; background-repeat: no-repeat;
}
.vt-field select:hover { border-color: var(--pink-300); }
.vt-field select:focus { outline: none; border-color: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100); }

.vt-filter-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 17px; height: 17px; padding: 0 5px; border-radius: 999px;
  background: var(--pink-600); color: #fff; font-size: 11px; font-weight: 700;
}
.vt-caret { font-size: 10px; transition: transform .2s; }
.vt-caret.is-open { transform: rotate(180deg); }

/* ══════════ BẢNG ══════════ */
/* table-layout: fixed + moi cot deu co width — trinh khong-gian-thua-do-mot-cot-choan-het
   (truoc day cot "Cau hinh" khong co width nen an het toan bo khoang trong con lai). */
/* width:100% ep table luon vua khung, cot hep di la chu bi cat + "..." (vd "Trang th...").
   Doi sang min-width:100% + khong ep width — man rong thi cac cot van gian ti le lap day
   nhu cu, man/khung hep hon tong do rong cot thi table tu no rong ra, .vt-table-wrap
   overflow-x:auto se hien thanh cuon ngang thay vi bop chu. */
.vt-table-wrap { overflow-x: auto; }
.vt-table { min-width: 100%; table-layout: fixed; border-collapse: collapse; }
.vt-table th {
  background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 800; text-align: left; text-transform: uppercase; letter-spacing: .4px;
  padding: 11px 12px; white-space: nowrap; border-bottom: none;
  overflow: hidden; text-overflow: ellipsis;
}
/* border-collapse:collapse tren <table> khong duoc overflow:hidden cua .vt-card bo
   goc dung cach — o dau hang tieu de vuot goc tron, trong nhu net ke de len vien
   card. Bo goc thang vao chinh o th dau/cuoi de nen hong di dung theo duong bo tron. */
.vt-table thead th:first-child { border-top-left-radius: 13px; }
.vt-table thead th:last-child { border-top-right-radius: 13px; }
.vt-table td {
  padding: 11px 12px; border-bottom: 1px solid var(--line);
  vertical-align: middle; color: var(--ink);
}
.vt-table tbody tr:last-child td { border-bottom: none; }
.vt-row { cursor: pointer; transition: background-color .12s; }
.vt-row:hover { background: var(--pink-50); }
.vt-row:focus-visible { outline: 2px solid var(--pink-500); outline-offset: -2px; }

.vt-col-stt { width: 54px; }
.vt-col-img { width: 56px; }
.vt-col-sku { width: 150px; }
.vt-col-name { width: 200px; }
.vt-col-config { width: 220px; }
.vt-col-color { width: 90px; }
.vt-col-num { width: 80px; text-align: center; }
.vt-col-price { width: 110px; text-align: right; white-space: nowrap; }
.vt-col-status { width: 90px; }

.vt-thumb {
  width: 36px; height: 36px; border-radius: 9px; flex-shrink: 0; overflow: hidden;
  background: #fff; border: 1px solid var(--line);
  display: flex; align-items: center; justify-content: center;
}
.vt-thumb img { width: 100%; height: 100%; object-fit: cover; }

.vt-sku {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.76rem;
  color: var(--pink-700); font-weight: 700;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.vt-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; }
.vt-color { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vt-config { color: var(--muted); overflow: hidden; }
.vt-config__list { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 0.74rem; }
.vt-config__list span { display: inline-flex; align-items: center; gap: 4px; }

.vt-price { font-weight: 600; font-variant-numeric: tabular-nums; }
.vt-stock {
  display: inline-block; min-width: 34px; padding: 1px 8px; border-radius: 999px;
  font-weight: 700; font-size: 12px; font-variant-numeric: tabular-nums;
  background: var(--pink-50); color: var(--ink);
}
.vt-stock--low { background: #fff7ed; color: #c2650a; }
.vt-stock--out { background: #fef2f2; color: var(--danger); }

.vt-empty { padding: 40px 20px; text-align: center; color: var(--muted); font-size: 13.5px; }

.vt-tag {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 2px 9px; border-radius: 999px; font-size: 11.5px; font-weight: 700;
}
.vt-tag--on  { background: var(--ok-bg); color: var(--ok-text); }
.vt-tag--off { background: #f3f4f6; color: var(--muted); }

.vt-pager {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 18px; background: var(--pink-50); border-top: 1px solid var(--line);
  flex-wrap: wrap; font-size: 12.5px; color: var(--muted);
}

/* ══════════ MODAL DÙNG CHUNG ══════════ */
.vt-mask {
  position: fixed; inset: 0; z-index: 1000; padding: 20px;
  background: rgba(26,16,51,0.45);
  display: flex; align-items: center; justify-content: center;
}
.vt-modal {
  background: #fff; border: 1px solid var(--line); color: var(--ink);
  border-radius: 16px; display: flex; flex-direction: column;
  max-width: 96vw; max-height: 92vh; overflow: hidden;
  box-shadow: var(--sh-2);
}
.vt-modal--detail { width: 780px; }
.vt-modal--form { width: 860px; }

.vt-modal__head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
  padding: 14px 20px; border-bottom: 1px solid var(--line);
}
.vt-head-main { min-width: 0; }
.vt-head-title { font-weight: 700; font-size: 1rem; color: var(--ink); line-height: 1.35; word-break: break-word; }
.vt-head-sub {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  margin-top: 4px; font-size: 0.74rem; color: var(--muted);
}

.vt-modal__body { padding: 16px 20px; overflow-y: auto; }
.vt-modal__foot {
  display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 12px 20px; border-top: 1px solid var(--line);
}
.vt-modal__foot--end { justify-content: flex-end; }
.vt-foot-right { display: flex; gap: 8px; flex-wrap: wrap; }

/* ══════════ Ô CHI TIẾT PHIÊN BẢN ══════════ */
.vt-detail-top { display: grid; grid-template-columns: 200px 1fr; gap: 18px; align-items: start; }
.vt-detail-media { display: flex; flex-direction: column; gap: 10px; }
.vt-detail-img {
  width: 100%; aspect-ratio: 1 / 1; border-radius: 12px; overflow: hidden;
  background: var(--pink-50); border: 1px solid var(--line);
  display: flex; align-items: center; justify-content: center;
}
.vt-detail-img img { width: 100%; height: 100%; object-fit: contain; }

.vt-barcode-box {
  background: #fff; border: 1px solid var(--line); border-radius: 10px; padding: 8px;
  display: flex; align-items: center; justify-content: center; min-height: 64px;
}
.vt-barcode-box svg { max-width: 100%; height: auto; }
.vt-barcode-empty { display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: 0.74rem; }

.vt-detail-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px 18px; margin: 0;
}
.vt-detail-grid dt {
  display: flex; align-items: center; gap: 4px;
  font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em;
  color: var(--muted); margin-bottom: 3px;
}
.vt-detail-grid dd {
  margin: 0; padding-bottom: 4px; font-size: 0.84rem; color: var(--ink);
  border-bottom: 1px solid var(--line); word-break: break-word;
}
.vt-detail-block { margin-top: 18px; }
.vt-desc { margin: 0; font-size: 0.82rem; line-height: 1.6; color: var(--muted); white-space: pre-line; }
.vt-mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
.vt-chip {
  display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 0.72rem; font-weight: 700;
  background: var(--pink-100); color: var(--pink-700);
}

/* ══════════ FORM ══════════ */
.vt-section-title {
  text-transform: uppercase; font-weight: 700; font-size: 0.65rem; letter-spacing: 0.1em;
  color: var(--pink-700); margin-bottom: 8px;
}
.vt-section-title:not(:first-child) { margin-top: 4px; }
.vt-panel {
  background: var(--pink-50); border: 1px solid var(--line);
  border-radius: 12px; padding: 14px; margin-bottom: 16px;
}
.vt-label {
  display: block; margin-bottom: 4px;
  font-size: 0.75rem; font-weight: 700; color: var(--pink-700);
}
.vt-input {
  background: #fff !important; color: var(--ink) !important;
  border-color: var(--field) !important;
}
.vt-input::placeholder { color: #b9a3ae; }
.vt-input:hover { border-color: var(--pink-300) !important; }
.vt-input:focus {
  background: #fff !important; color: var(--ink) !important;
  border-color: var(--pink-500) !important; box-shadow: 0 0 0 3px var(--pink-100) !important;
}
.vt-inline { display: flex; gap: 6px; align-items: center; }
.vt-inline > input { flex: 1; min-width: 0; }
.vt-hint { margin-top: 4px; font-size: 0.72rem; color: var(--muted); }

.vt-picked {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 8px 12px; border-radius: 10px; background: var(--pink-50); margin-bottom: 14px;
}

.vt-pick-list {
  margin-top: 6px; max-height: 240px; overflow-y: auto;
  border: 1px solid var(--line); border-radius: 10px;
}
.vt-pick-item { padding: 8px 12px; cursor: pointer; font-size: 0.84rem; }
.vt-pick-item:hover { background: var(--pink-50); }
.vt-pick-empty { padding: 12px; font-size: 0.8rem; color: var(--muted); }

.vt-upload { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.vt-upload__box {
  width: 110px; height: 88px; flex-shrink: 0; cursor: pointer; overflow: hidden;
  border: 1px dashed var(--field); border-radius: 12px;
  background: #fff; color: var(--muted);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  font-size: 0.68rem; text-align: center;
}
.vt-upload__box:hover { border-color: var(--pink-400, var(--pink-500)); }
.vt-upload__box img { width: 110px; height: 88px; object-fit: contain; }

.vt-tag-list { display: flex; flex-wrap: wrap; gap: 8px; }

/* Tag hiển thị giá trị vừa chọn trong combobox (CPU/RAM/GPU/Ổ cứng/Màu sắc/Màn hình/
   HĐH/Pin) — bấm dấu x để bỏ chọn, y hệt kiểu hh-tag-pill bên HangHoa.vue. */
.vt-picked-tags { margin-top: 6px; }
.vt-tag-pill {
  display: inline-flex; align-items: center; gap: 6px; max-width: 100%;
  background: var(--pink-100); color: var(--pink-700);
  border: 1px solid var(--pink-200); border-radius: 999px;
  padding: 3px 6px 3px 11px; font-size: 12.5px; font-weight: 600;
  word-break: break-word;
}
.vt-tag-pill button {
  background: var(--pink-200); border: none; color: var(--pink-700);
  width: 17px; height: 17px; border-radius: 50%; line-height: 1; flex-shrink: 0;
  font-size: 13px; cursor: pointer; display: grid; place-items: center;
}
.vt-tag-pill button:hover { background: var(--pink-600); color: #fff; }

.vt-tag-btn {
  padding: 3px 12px; border-radius: 999px; cursor: pointer; font-size: 0.75rem;
  background: #fff; color: var(--pink-700);
  border: 1px solid var(--pink-200);
}
.vt-tag-btn:hover { background: var(--pink-50); border-color: var(--pink-300); }
.vt-tag-btn.is-on { background: var(--pink-600); border-color: var(--pink-600); color: #fff; font-weight: 700; }

/* ══════════ MÀN HÌNH NHỎ ══════════ */
@media (max-width: 900px) {
  .vt-detail-top { grid-template-columns: 1fr; }
  .vt-detail-img { max-width: 220px; }
  .vt-toolbar__right { width: 100%; margin-left: 0; }
  .vt-search { max-width: none; }
}
</style>