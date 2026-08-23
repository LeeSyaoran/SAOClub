<script setup>
import { ref, computed, reactive, watch, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import {
  CheckCircle2, XCircle, Clock, Package, ClipboardList, BarChart3, AlertTriangle,
  Ban, Search, Pencil, Printer, Download, Plus, Check, X, Trash2, Truck,
  Building2, User, Calendar, FileText, FolderOpen, Filter, ChevronDown,
} from '@lucide/vue';
import { nowLocalIso } from "../../utils/datetime.js";
import { formatPrice, formatDate, statusLabel, toLocalDT } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import * as XLSX from "xlsx";
import SearchSelect from "../common/SearchSelect.vue";
import * as TonKhoService from "../../services/TonKhoService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as PhieuNhapKhoService from "../../services/PhieuNhapKhoService.js";
import * as ChiTietPhieuNhapService from "../../services/ChiTietPhieuNhapService.js";
import * as BienTheSanPhamService from "../../services/BienTheSanPhamService.js";
import { InventoryStore, ensureInventory, refreshInventory } from "../../stores/inventory.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { SuppliersStore, ensureSuppliers } from "../../stores/suppliers.js";
import { StaffStore, ensureStaff } from "../../stores/staff.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";

// products/inventory: chi doc (khong CRUD o day) — ProductsStore da duoc tai eager tu
// fetchAll() (AdminPage.vue), goi lai ensureProducts() o day chi de an toan neu component
// nay lo mount truoc luc do (cached-promise, khong tai trung).
onMounted(() => {
  ensureInventory();
  ensureProducts();
});

// t() trả nguyên chuỗi key (vd "admin.common.filter") nếu chưa có bản dịch, KHÔNG nhận
// tham số fallback như tt() bên dưới — dùng cho vài nhãn UI mới thêm chưa kịp có key i18n.
const tt = (key, fallback) => {
  const s = t(key);
  return !s || s === key ? fallback : s;
};

const inventory = computed(() => InventoryStore.items ?? []);
const products = computed(() => ProductsStore.items ?? []);
const suppliers = computed(() => SuppliersStore.items ?? []);
const staff = computed(() => StaffStore.items ?? []);

// ── Tab noi bo: Ton kho | Phieu nhap kho ──────────────────────────────────────
const khoTab = ref('ton-kho'); // 'ton-kho' | 'phieu-nhap'

const getVariantInfo = (item) => products.value.find(p => p.bienTheId === item.bienThe?.bienTheId);
const maSanPhamCuaItem = (item) => {
  const sp = item.bienThe?.sanPham;
  if (sp?.maSanPham) return sp.maSanPham;
  return sp?.sanPhamId != null ? 'SP' + String(sp.sanPhamId).padStart(4, '0') : '—';
};

// BienTheSanPhamService.update() dùng BeanUtils.copyProperties nên cần body ĐẦY ĐỦ (thiếu
// field nào sẽ bị null hết field đó) — dựng lại từ entity lồng nhau trả về trong
// TonKho.bienThe (đọc thẳng, không cần gọi thêm API CPU/RAM/GPU/Ổ cứng).
const buildBienTheUpdateBody = (bienThe, overrides = {}) => ({
  sanPhamId: bienThe?.sanPham?.sanPhamId,
  maSku: bienThe?.maSku ?? '',
  barcode: bienThe?.barcode ?? '',
  giaNhap: Number(bienThe?.giaNhap ?? 0),
  giaBan: Number(bienThe?.giaBan ?? 0),
  baoHanhThang: Number(bienThe?.baoHanhThang ?? 0),
  hinhAnhBienThe: bienThe?.hinhAnhBienThe ?? '',
  trangThai: bienThe?.trangThai ?? '',
  mauSac: bienThe?.mauSac ?? '',
  cpuId: bienThe?.cpu?.cpuId ?? null,
  ramId: bienThe?.ram?.ramId ?? null,
  oCungId: bienThe?.oCung?.oCungId ?? null,
  gpuId: bienThe?.gpu?.gpuId ?? null,
  kichThuocManHinh: bienThe?.kichThuocManHinh ?? '',
  heDieuHanh: bienThe?.heDieuHanh ?? '',
  pin: bienThe?.pin ?? '',
  trongLuongKg: bienThe?.trongLuongKg != null ? Number(bienThe.trongLuongKg) : 0,
  ...overrides,
});

// Đồng bộ giá nhập của biến thể theo đơn giá của phiếu nhập mới nhất — theo đúng yêu cầu:
// biến thể mới tạo giaNhap=0, sau khi nhập hàng thì lấy giá nhập từ phiếu.
const syncGiaNhapFromReceipt = async (bienTheId, donGia) => {
  const item = inventory.value.find((i) => i.bienThe?.bienTheId === bienTheId);
  // item/bienThe không tìm thấy nghĩa là inventory.value đang là snapshot cũ (biến thể này
  // mới toanh, refreshInventory() ở cuối savePhieuNhap() chưa kịp chạy) — không phải lỗi,
  // chỉ là chưa đồng bộ được NGAY, im lặng bỏ qua lần này là đúng (không phải bug cần báo).
  if (!item?.bienThe) return;
  try {
    const body = buildBienTheUpdateBody(item.bienThe, { giaNhap: Number(donGia) || 0 });
    const res = await BienTheSanPhamService.update(bienTheId, body);
    if (!res.ok) {
      const text = await res.text().catch(() => res.statusText);
      showToast(tt('admin.inventory.syncGiaNhapFailed', 'Không tự cập nhật được giá nhập cho') + ` ${item.bienThe.maSku}: ${text}`);
    }
  } catch (e) {
    showToast(tt('admin.inventory.syncGiaNhapFailed', 'Không tự cập nhật được giá nhập cho') + ` ${item.bienThe.maSku}: ${e.message}`);
  }
};

// ── Phân loại 1 dòng tồn kho — 4 nhóm ─────────────────────────────────────────────────
// "pending" (Chờ nhập hàng): biến thể vừa tạo, CHƯA đủ giá nhập/giá bán (mặc định = 0 lúc
// tạo mới) — coi như chưa sẵn sàng bán, KHÔNG hiện ở danh sách chính, chỉ xem được qua
// ô thống kê "Chờ nhập hàng". Biến thể chỉ thật sự "vào kho" sau khi 1 phiếu nhập gán giá
// nhập + serial cho nó (xem savePhieuNhap()).
// "out" (Hết hàng): đã đủ giá nhưng tồn = 0 (bán hết) — cũng ẩn khỏi ds chính, chỉ xem qua
// ô "Hết hàng" để không làm rối danh sách "đang có thể bán".
// "low"/"ok": đủ giá + còn hàng — hiện bình thường ở danh sách chính.
const isPendingItem = (v) => !(Number(v?.giaNhap) > 0) || !(Number(v?.giaBan) > 0);
const stockStatusOf = (item, v) => {
  if (isPendingItem(v)) return 'pending';
  if ((item.soLuongTon ?? 0) === 0) return 'out';
  if (item.soLuongTon != null && item.tonKhoToiThieu != null && item.soLuongTon <= item.tonKhoToiThieu) return 'low';
  return 'ok';
};
const stockStatusLabel = (s) => ({
  pending: tt('admin.inventory.filterPending', 'Chờ nhập hàng'),
  out: t('admin.inventory.filterOut'),
  low: t('admin.inventory.filterLow'),
  ok: t('admin.inventory.filterOk'),
}[s] || '—');
const pendingItems = computed(() => inventory.value.filter((item) => isPendingItem(getVariantInfo(item))));
const outOfStockItems = computed(() =>
  inventory.value.filter((item) => stockStatusOf(item, getVariantInfo(item)) === 'out'),
);
// "Sắp hết" (khác Hết hàng): còn hàng nhưng <= tối thiểu
const lowStockOnlyItems = computed(() =>
  inventory.value.filter((item) => stockStatusOf(item, getVariantInfo(item)) === 'low'),
);
const totalStockQty = computed(() => inventory.value.reduce((s, i) => s + (i.soLuongTon || 0), 0));

// ── So sánh config với variant đầu tiên của cùng sản phẩm (để highlight giá trị khác nhau) ──
const getFirstVariantOfProduct = (v) => products.value.find(p => p.sanPhamId === v?.sanPhamId && p.bienTheId !== v?.bienTheId);
const hasCpuDiff = (v) => { const first = getFirstVariantOfProduct(v); return !first || v?.cpu !== first.cpu; };
const hasRamDiff = (v) => { const first = getFirstVariantOfProduct(v); return !first || v?.ram !== first.ram; };
const hasOCungDiff = (v) => { const first = getFirstVariantOfProduct(v); return !first || v?.oCung !== first.oCung; };
const hasMauSacDiff = (v) => { const first = getFirstVariantOfProduct(v); return !first || v?.mauSac !== first.mauSac; };

// ── Tồn kho: bảng PHẲNG theo từng biến thể (không gộp theo sản phẩm nữa) — biến thể
// mới tạo gần nhất hiện đầu trang, có bộ lọc riêng (trạng thái tồn/thương hiệu/danh mục). ──
const inventorySearch = ref('');
const isInvFilterOpen = ref(false);
const invFilterStatus = ref(''); // '' | 'pending' | 'out' | 'low' | 'ok'
const invFilterThuongHieu = ref('');
const invFilterDanhMuc = ref('');
// Bấm vào 1 trong 3 ô thống kê "Chờ nhập hàng/Sắp hết/Hết hàng" — bấm lại lần nữa thì tắt,
// quay về danh sách mặc định.
const toggleInvQuickFilter = (status) => { invFilterStatus.value = invFilterStatus.value === status ? '' : status; };

// Thương hiệu/danh mục lấy thẳng từ dữ liệu đang có trong ProductsStore — không gọi thêm
// API danh mục riêng, bộ lọc luôn khớp 100% với những gì có trong bảng tồn kho.
const invBrandOptions = computed(() => {
  const map = new Map();
  products.value.forEach((p) => { if (p.thuongHieuId != null && !map.has(p.thuongHieuId)) map.set(p.thuongHieuId, p.tenThuongHieu ?? '—'); });
  return [...map].map(([value, label]) => ({ value, label })).sort((a, b) => String(a.label).localeCompare(String(b.label), 'vi'));
});
const invCategoryOptions = computed(() => {
  const map = new Map();
  products.value.forEach((p) => { if (p.danhMucId != null && !map.has(p.danhMucId)) map.set(p.danhMucId, p.tenDanhMuc ?? '—'); });
  return [...map].map(([value, label]) => ({ value, label })).sort((a, b) => String(a.label).localeCompare(String(b.label), 'vi'));
});
const invActiveFilterCount = computed(() =>
  [invFilterStatus.value, invFilterThuongHieu.value, invFilterDanhMuc.value].filter((v) => v !== '').length,
);
const clearInvFilters = () => {
  inventorySearch.value = '';
  invFilterStatus.value = '';
  invFilterThuongHieu.value = '';
  invFilterDanhMuc.value = '';
};

const flatInventory = computed(() => {
  const q = inventorySearch.value.trim().toLowerCase();
  return inventory.value
    .map((item) => {
      const v = getVariantInfo(item);
      return { item, v, status: stockStatusOf(item, v) };
    })
    .filter(({ item, v, status }) => {
      if (q) {
        const hay = [item.bienThe?.maSku, item.bienThe?.sanPham?.tenSanPham, v?.tenSanPham, maSanPhamCuaItem(item)]
          .filter(Boolean).join(' ').toLowerCase();
        if (!hay.includes(q)) return false;
      }
      if (invFilterThuongHieu.value && String(v?.thuongHieuId ?? '') !== String(invFilterThuongHieu.value)) return false;
      if (invFilterDanhMuc.value && String(v?.danhMucId ?? '') !== String(invFilterDanhMuc.value)) return false;
      // Có chọn lọc rõ ràng (kể cả từ bấm ô thống kê) -> hiện ĐÚNG nhóm đó.
      if (invFilterStatus.value) return status === invFilterStatus.value;
      // Mặc định: ẩn "Chờ nhập hàng" (chưa đủ giá) và "Hết hàng" (đã bán hết) khỏi ds chính,
      // chỉ xem được qua các ô thống kê tương ứng — theo đúng yêu cầu nghiệp vụ.
      return status !== 'pending' && status !== 'out';
    })
    // Mới tạo gần nhất lên đầu — ưu tiên ngày tạo của biến thể, nếu thiếu thì lùi về
    // bienTheId (tự tăng, càng lớn càng mới) để vẫn có thứ tự hợp lý.
    .sort((a, b) => {
      const da = a.v?.ngayTao ? new Date(a.v.ngayTao).getTime() : (a.item.bienThe?.bienTheId ?? 0);
      const db = b.v?.ngayTao ? new Date(b.v.ngayTao).getTime() : (b.item.bienThe?.bienTheId ?? 0);
      return db - da;
    });
});
const { currentPage: invCurrentPage, totalPages: invTotalPages, pagedItems: pagedFlatInventory } = usePagination(flatInventory);
watch([inventorySearch, invFilterStatus, invFilterThuongHieu, invFilterDanhMuc], () => { invCurrentPage.value = 0; });

// ── Ô chi tiết 1 dòng tồn kho — GỘP 2 modal cũ (xem serial / sửa+thêm hàng) thành 1
// modal có 2 tab, mở bằng cách bấm vào dòng (bỏ hẳn nút cây bút riêng). ──────────────────
const showDetailModal = ref(false);
const detailItem = ref(null); // tonKho item đang xem
const detailTab = ref('serials'); // 'serials' | 'add'

// Tab "Danh sách serial" — chỉ xem, có tìm kiếm + lọc trạng thái (không còn ô thêm dòng
// nhanh ở đây nữa, thêm serial dồn hết sang tab "Thêm hàng").
const detailSerials = ref([]);
const detailSerialsLoading = ref(false);
const detailSerialSearch = ref('');
const detailSerialStatusFilter = ref('');
const SERIAL_STATUS_OPTIONS = ['trong_kho', 'giu_hang', 'da_ban', 'loi_bao_hanh', 'da_tra_hang'];
const filteredDetailSerials = computed(() => {
  const q = detailSerialSearch.value.trim().toLowerCase();
  return detailSerials.value.filter((s) => {
    if (q && !String(s.soSerial ?? '').toLowerCase().includes(q)) return false;
    if (detailSerialStatusFilter.value && s.trangThai !== detailSerialStatusFilter.value) return false;
    return true;
  });
});

// Xóa serial thêm nhầm — chỉ cho phép khi đang "trong_kho" (server chặn nếu đã bán/đã dùng).
const removeStockSerial = async (chiTietId) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const bienTheId = detailItem.value?.bienThe?.bienTheId;
  try {
    const res = await ChiTietSanPhamService.remove(chiTietId);
    if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteSerialError'))); return; }
    detailSerials.value = detailSerials.value.filter((s) => s.chiTietId !== chiTietId);
    const updatedStock = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    if (updatedStock) {
      const idx = inventory.value.findIndex((i) => i.tonKhoId === updatedStock.tonKhoId);
      if (idx !== -1) inventory.value[idx] = updatedStock;
    }
  } catch (e) { showToast(e.message); }
};

// Tab "Thêm hàng" — y hệt logic cũ của nút cây bút: sửa số đang giữ/tồn tối thiểu, thêm
// serial mới (gõ tay từng dòng hoặc nhập file). soLuongTon KHÔNG sửa tay được — chỉ tăng
// khi thêm serial mới, khớp đúng thực tế: mỗi máy nhập kho có 1 serial.
const stockSaving = ref(false);
const stockForm = reactive({ soLuongGiu: 0, tonKhoToiThieu: 0, newSerials: [''], giaBan: 0 });
const addStockSerialRow = () => stockForm.newSerials.push('');
const removeStockSerialRow = (idx) => {
  if (stockForm.newSerials.length > 1) stockForm.newSerials.splice(idx, 1);
  else stockForm.newSerials[idx] = '';
};
// Nhập hàng loạt từ file — .xlsx/.xls đọc qua thư viện xlsx (mọi ô có dữ liệu, không
// phân biệt hàng/cột), .csv/.txt đọc thẳng dạng text (mỗi serial 1 dòng hoặc cách nhau
// bằng dấu phẩy) — khỏi phải gõ/dán tay từng dòng.
const importSerialsFromFile = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  const ext = file.name.split('.').pop()?.toLowerCase();
  let parsed;
  if (ext === 'xlsx' || ext === 'xls') {
    const buf = await file.arrayBuffer();
    const wb = XLSX.read(buf, { type: 'array' });
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 });
    parsed = rows.flat().map((v) => String(v ?? '').trim()).filter(Boolean);
  } else {
    const text = await file.text();
    parsed = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
  }
  const existing = stockForm.newSerials.filter(Boolean);
  stockForm.newSerials = [...existing, ...parsed].length ? [...existing, ...parsed] : [''];
  e.target.value = '';
};

const loadDetailSerials = async (bienTheId) => {
  detailSerialsLoading.value = true;
  detailSerials.value = bienTheId ? await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []) : [];
  detailSerialsLoading.value = false;
};

const openStockDetail = async (item) => {
  detailItem.value = item;
  detailTab.value = 'serials';
  detailSerialSearch.value = '';
  detailSerialStatusFilter.value = '';
  stockForm.soLuongGiu = item.soLuongGiu ?? 0;
  stockForm.tonKhoToiThieu = item.tonKhoToiThieu ?? 0;
  stockForm.newSerials = [''];
  // Giá bán nhập tay ở đây — đọc từ ProductsStore (nguồn giaBan/giaNhap dùng để phân loại
  // pending/out, xem isPendingItem) chứ không phải item.bienThe (entity lồng nhau từ TonKho
  // có thể không đồng bộ tức thời bằng store dùng chung).
  stockForm.giaBan = Number(getVariantInfo(item)?.giaBan ?? 0);
  showDetailModal.value = true;
  await loadDetailSerials(item.bienThe?.bienTheId);
};

const saveStock = async () => {
  if (stockSaving.value) return;
  stockSaving.value = true;
  const item = detailItem.value;
  const bienTheId = item.bienThe?.bienTheId;
  try {
    // 1) Thêm từng serial mới — mỗi cái tự tăng soLuongTon ở server (trigger DB tính lại
    // từ số serial "trong_kho", không phải giá trị FE gửi lên).
    const serials = stockForm.newSerials.map((s) => s.trim()).filter(Boolean);
    for (const soSerial of serials) {
      const res = await ChiTietSanPhamService.create({
        bienTheId, soSerial, trangThai: 'trong_kho',
        ngayNhapKho: nowLocalIso(),
      });
      if (!res.ok) { showToast(t('admin.errors.addSerialError')); return; }
    }
    // 2) Đang giữ / tồn kho tối thiểu — 2 field còn lại được sửa tay bình thường.
    const res = await TonKhoService.update(item.tonKhoId, {
      soLuongGiu: Number(stockForm.soLuongGiu),
      tonKhoToiThieu: Number(stockForm.tonKhoToiThieu),
    });
    if (!res.ok) { showToast(t('admin.errors.updateFailed', { status: res.status })); return; }
    // 3) Giá bán — chỉ gọi update biến thể nếu có đổi, tránh ghi đè vô ích. Đây là bước
    // "tốt nghiệp" khỏi Chờ nhập hàng: đủ giaNhap (đã có từ phiếu nhập) + giaBan (nhập ở đây).
    const currentGiaBan = Number(getVariantInfo(item)?.giaBan ?? 0);
    if (item.bienThe && Number(stockForm.giaBan) !== currentGiaBan) {
      const body = buildBienTheUpdateBody(item.bienThe, { giaBan: Number(stockForm.giaBan) || 0 });
      const priceRes = await BienTheSanPhamService.update(bienTheId, body);
      if (!priceRes.ok) { showToast(t('admin.errors.updateFailed', { status: priceRes.status })); return; }
    }
    // Lấy lại đúng dòng vừa đổi để có soLuongTon mới nhất do server tính, rồi quay về tab
    // danh sách serial để thấy ngay kết quả — không đóng hẳn modal. refreshProducts() để
    // giaBan/giaNhap vừa đổi phản ánh ngay ở bảng chính + phân loại pending/out (đọc từ
    // ProductsStore, không phải item.bienThe) — không cần F5.
    const [updated] = await Promise.all([
      TonKhoService.getByBienThe(bienTheId).catch(() => null),
      refreshProducts().catch(() => {}),
    ]);
    const idx = inventory.value.findIndex((i) => i.tonKhoId === item.tonKhoId);
    if (idx !== -1 && updated) inventory.value[idx] = updated;
    stockForm.newSerials = [''];
    await loadDetailSerials(bienTheId);
    detailTab.value = 'serials';
    showToast(tt('admin.stockModal.savedToast', 'Đã cập nhật tồn kho'));
  } catch (e) {
    showToast(e.message);
  } finally {
    stockSaving.value = false;
  }
};

const stockDetailStatusLabel = (s) => t(`admin.statusLabel.${s}`);

// ── Phieu nhap kho ───────────────────────────────────────────────────────────
const phieuNhapList = ref([]);
const chiTietPhieuNhapList = ref([]);

let phieuNhapDataPromise = null;
const ensurePhieuNhapData = () => {
  if (phieuNhapDataPromise) return phieuNhapDataPromise;
  phieuNhapDataPromise = Promise.all([
    PhieuNhapKhoService.getAll().catch(() => []),
    ChiTietPhieuNhapService.getAll().catch(() => []),
    ensureSuppliers(),
    ensureStaff(),
  ]).then(([pn, ct]) => {
    phieuNhapList.value = pn;
    chiTietPhieuNhapList.value = ct;
  });
  return phieuNhapDataPromise;
};

const supplierName = (id) => suppliers.value.find(s => s.nhaCungCapId === id)?.tenNhaCungCap ?? '—';
const staffName = (id) => staff.value.find(s => s.nhanVienId === id)?.hoTen ?? '—';

// Cùng tông màu với orderStatusColor() (utils/orderStatus.js) — dùng lại đúng hex
// cho vàng/xanh lá/đỏ để nhất quán trạng thái trên toàn app.
const phieuNhapStatusColor = (s) => {
  if (s === 'hoan_thanh') return { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' };
  if (s === 'huy')        return { bg: 'rgba(239,68,68,0.15)',  text: '#f87171' };
  return                         { bg: 'rgba(250,204,21,0.15)', text: '#facc15' }; // cho_duyet
};
const phieuNhapStatusIcon = (s) => (s === 'hoan_thanh' ? CheckCircle2 : s === 'huy' ? XCircle : Clock);

const phieuNhapCounts = computed(() => ({
  total: phieuNhapList.value.length,
  choDuyet: phieuNhapList.value.filter(p => p.trangThai === 'cho_duyet').length,
  hoanThanh: phieuNhapList.value.filter(p => p.trangThai === 'hoan_thanh').length,
  huy: phieuNhapList.value.filter(p => p.trangThai === 'huy').length,
}));

const phieuNhapSearch = ref('');
const phieuNhapStatusFilter = ref('');
const filteredPhieuNhap = computed(() =>
  phieuNhapList.value
    .filter(p => !phieuNhapSearch.value || (p.maPhieuNhap ?? '').toLowerCase().includes(phieuNhapSearch.value.toLowerCase()))
    .filter(p => !phieuNhapStatusFilter.value || p.trangThai === phieuNhapStatusFilter.value)
    .sort((a, b) => new Date(b.ngayNhap) - new Date(a.ngayNhap)),
);
const { currentPage: pnCurrentPage, totalPages: pnTotalPages, pagedItems: pagedPhieuNhap, pageSize: pnPageSize } = usePagination(filteredPhieuNhap);

// San pham + bien the de chon khi tao dong phieu nhap — lay tu ton kho (da co san, khoi tai them)
// Options dang {value,label} de dung truc tiep voi SearchSelect.
const productOptionsForPhieuNhap = computed(() => {
  const map = new Map();
  for (const item of inventory.value) {
    const sp = item.bienThe?.sanPham;
    if (sp?.sanPhamId != null && !map.has(sp.sanPhamId)) {
      map.set(sp.sanPhamId, { value: sp.sanPhamId, label: sp.tenSanPham ?? '' });
    }
  }
  return [...map.values()];
});
const variantOptionsByProduct = computed(() => {
  const map = new Map();
  for (const item of inventory.value) {
    const bt = item.bienThe;
    const sanPhamId = bt?.sanPham?.sanPhamId;
    if (sanPhamId == null || bt?.bienTheId == null) continue;
    if (!map.has(sanPhamId)) map.set(sanPhamId, new Map());
    const variants = map.get(sanPhamId);
    if (!variants.has(bt.bienTheId)) {
      const specs = [bt.mauSac, bt.cpu?.tenCpu, bt.ram?.dungLuong].filter(Boolean).join(' · ');
      variants.set(bt.bienTheId, { value: bt.bienTheId, label: specs ? `${bt.maSku} — ${specs}` : bt.maSku });
    }
  }
  return map;
});
const variantsForProduct = (sanPhamId) => [...(variantOptionsByProduct.value.get(Number(sanPhamId)) ?? new Map()).values()];
const supplierOptions = computed(() => suppliers.value.map(s => ({ value: s.nhaCungCapId, label: s.tenNhaCungCap })));
const staffOptions = computed(() => staff.value.map(s => ({ value: s.nhanVienId, label: s.hoTen })));

const showPhieuNhapModal = ref(false);
const phieuNhapFormError = ref('');
const phieuNhapSaving = ref(false);
const emptyPhieuNhapItem = () => ({ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0, serials: [''], lockedCount: 0 });
const emptyPhieuNhapForm = () => {
  const now = new Date();
  const local = nowLocalIso(now).slice(0, 16);
  return {
    nhaCungCapId: '',
    nhanVienId: '',
    ngayNhap: local,
    ghiChu: '',
    items: [emptyPhieuNhapItem()],
  };
};
const phieuNhapForm = reactive(emptyPhieuNhapForm());
const phieuNhapItemsTotal = computed(() =>
  phieuNhapForm.items.reduce((s, i) => s + (Number(i.soLuong) || 0) * (Number(i.donGia) || 0), 0),
);
const addPhieuNhapItemRow = () => phieuNhapForm.items.push(emptyPhieuNhapItem());
// Giữ mảng serials luôn đúng độ dài soLuong — thêm ô trống ở cuối khi tăng số lượng, bớt ô
// trống ở cuối khi giảm (không bao giờ đụng tới các serial đã có sẵn ở đầu mảng — lockedCount
// serial đầu là serial đã ghi nhận thật vào kho từ trước, không cho sửa/xóa qua form này nữa).
const syncPhieuNhapItemSerials = (row) => {
  const target = Math.max(Number(row.soLuong) || 0, row.lockedCount ?? 0);
  if (row.serials.length < target) {
    row.serials.push(...Array(target - row.serials.length).fill(''));
  } else if (row.serials.length > target) {
    row.serials.splice(target);
  }
};
// HTML min="1" chỉ chặn nút spinner, gõ tay vẫn nhập được số âm/0 — kẹp lại ngay lúc nhập
// (backend cũng đã chặn ở ChiTietPhieuNhapRequest, kẹp ở đây để báo sai ngay thay vì đợi lưu).
// Không cho giảm dưới lockedCount — bấy nhiêu serial đã nhập kho thật rồi, sửa phiếu không
// được phép "rút" máy đã có serial ra khỏi phiếu.
const clampPhieuNhapSoLuong = (row) => {
  row.soLuong = Math.max(1, row.lockedCount ?? 0, Math.trunc(Number(row.soLuong)) || 1);
  syncPhieuNhapItemSerials(row);
};
const removePhieuNhapItemRow = (idx) => {
  if (phieuNhapForm.items.length > 1) {
    phieuNhapForm.items.splice(idx, 1);
  } else {
    // Chỉ còn 1 dòng — không xóa hẳn (form sẽ trống hoàn toàn), reset về giá trị rỗng.
    phieuNhapForm.items[idx] = emptyPhieuNhapItem();
  }
};
// Mỗi dòng phải nhập đủ serial khớp số lượng (không tính các serial đã khóa/có sẵn) trước
// khi được lưu — theo đúng yêu cầu "phải nhập tay serial cho đủ số lượng".
const phieuNhapSerialsIncomplete = computed(() =>
  phieuNhapForm.items.some((i) => i.bienTheId && i.serials.some((s) => !s.trim())),
);
const editingPhieuNhapId = ref(null);
const openAddPhieuNhap = () => {
  editingPhieuNhapId.value = null;
  Object.assign(phieuNhapForm, emptyPhieuNhapForm());
  phieuNhapFormError.value = '';
  showPhieuNhapModal.value = true;
};
// Chỉ sửa được khi còn "cho_duyet" — đã duyệt/hủy thì coi như chốt sổ, sửa lại sẽ sai đối
// chiếu với NCC. Nạp lại đúng dữ liệu đang có: header + từng dòng chi tiết (kèm id để
// savePhieuNhap() biết dòng nào update, dòng nào tạo mới/xóa khi lưu) + serial đã nhập thật
// của riêng phiếu này (khóa lại, chỉ cho thêm serial mới nếu tăng số lượng).
const openEditPhieuNhap = async (p) => {
  editingPhieuNhapId.value = p.phieuNhapId;
  const bienTheToSanPham = new Map(products.value.map(pp => [pp.bienTheId, pp.sanPhamId]));
  const existingSerials = await ChiTietSanPhamService.getByPhieuNhap(p.phieuNhapId).catch(() => []);
  const serialsByBienThe = new Map();
  for (const s of existingSerials) {
    if (!serialsByBienThe.has(s.bienTheId)) serialsByBienThe.set(s.bienTheId, []);
    serialsByBienThe.get(s.bienTheId).push(s.soSerial);
  }
  const items = chiTietPhieuNhapList.value
    .filter(c => c.phieuNhapId === p.phieuNhapId)
    .map(c => {
      const locked = serialsByBienThe.get(c.bienTheId) ?? [];
      const soLuong = c.soLuong;
      const serials = [...locked];
      while (serials.length < soLuong) serials.push('');
      return {
        id: c.id,
        sanPhamId: bienTheToSanPham.get(c.bienTheId) ?? '',
        bienTheId: c.bienTheId,
        soLuong,
        donGia: c.donGiaNhap,
        serials,
        lockedCount: locked.length,
      };
    });
  Object.assign(phieuNhapForm, {
    nhaCungCapId: p.nhaCungCapId,
    nhanVienId: p.nhanVienId,
    ngayNhap: (p.ngayNhap || '').slice(0, 16),
    ghiChu: p.ghiChu === '—' ? '' : (p.ghiChu || ''),
    items: items.length ? items : [emptyPhieuNhapItem()],
  });
  phieuNhapFormError.value = '';
  showPhieuNhapModal.value = true;
};
const savePhieuNhap = async () => {
  phieuNhapFormError.value = '';
  if (!phieuNhapForm.nhaCungCapId || !phieuNhapForm.nhanVienId) {
    phieuNhapFormError.value = t('admin.phieuNhapModal.missingRequired');
    return;
  }
  const items = phieuNhapForm.items.filter(i => i.bienTheId);
  if (items.length === 0) {
    phieuNhapFormError.value = t('admin.phieuNhapModal.missingItems');
    return;
  }
  if (phieuNhapSerialsIncomplete.value) {
    phieuNhapFormError.value = tt('admin.phieuNhapModal.missingSerials', 'Vui lòng nhập đủ số serial cho từng dòng hàng');
    return;
  }
  if (phieuNhapSaving.value) return;
  phieuNhapSaving.value = true;
  try {
    const headerBody = {
      nhaCungCapId: Number(phieuNhapForm.nhaCungCapId),
      nhanVienId: Number(phieuNhapForm.nhanVienId),
      ngayNhap: toLocalDT(phieuNhapForm.ngayNhap),
      tongTien: phieuNhapItemsTotal.value,
      trangThai: 'cho_duyet',
      ghiChu: phieuNhapForm.ghiChu || '—',
    };
    const res = await PhieuNhapKhoService.save(editingPhieuNhapId.value, headerBody);
    if (!res.ok) {
      phieuNhapFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    let phieuNhapId = editingPhieuNhapId.value;
    if (editingPhieuNhapId.value) {
      // Đối chiếu dòng cũ/mới: id có sẵn -> update, không có id -> tạo mới,
      // dòng cũ không còn trong form -> xóa.
      const originalIds = chiTietPhieuNhapList.value
        .filter(c => c.phieuNhapId === phieuNhapId).map(c => c.id);
      const keptIds = items.filter(i => i.id).map(i => i.id);
      for (const oldId of originalIds.filter(id => !keptIds.includes(id))) {
        await ChiTietPhieuNhapService.remove(oldId);
      }
      for (const i of items) {
        const body = {
          phieuNhapId, bienTheId: Number(i.bienTheId),
          soLuong: Number(i.soLuong) || 0, donGiaNhap: Number(i.donGia) || 0,
        };
        if (i.id) await ChiTietPhieuNhapService.update(i.id, body);
        else await ChiTietPhieuNhapService.create(body);
      }
    } else {
      const created = await res.json();
      phieuNhapId = created.phieuNhapId;
      for (const i of items) {
        await ChiTietPhieuNhapService.create({
          phieuNhapId,
          bienTheId: Number(i.bienTheId),
          soLuong: Number(i.soLuong) || 0,
          donGiaNhap: Number(i.donGia) || 0,
        });
      }
    }
    // Tạo serial vật lý cho phần MỚI của mỗi dòng (bỏ qua lockedCount serial đầu — đã ghi
    // nhận thật từ trước), rồi đồng bộ giá nhập của biến thể theo đơn giá phiếu này.
    for (const i of items) {
      const newSerials = i.serials.slice(i.lockedCount ?? 0).map(s => s.trim()).filter(Boolean);
      const bienTheId = Number(i.bienTheId);
      for (const soSerial of newSerials) {
        await ChiTietSanPhamService.create({
          bienTheId, phieuNhapId, soSerial, trangThai: 'trong_kho',
          ngayNhapKho: toLocalDT(phieuNhapForm.ngayNhap),
        }).catch(() => {});
      }
      await syncGiaNhapFromReceipt(bienTheId, i.donGia);
    }
    // API tạo trả về entity lồng nhau (nhaCungCap/nhanVien object) khác format phẳng của
    // getAll() (PhieuNhapKhoResponse) — tải lại danh sách thay vì tự ráp để tránh lệch dữ liệu.
    [phieuNhapList.value, chiTietPhieuNhapList.value] = await Promise.all([
      PhieuNhapKhoService.getAll().catch(() => phieuNhapList.value),
      ChiTietPhieuNhapService.getAll().catch(() => chiTietPhieuNhapList.value),
    ]);
    // refreshProducts() vì syncGiaNhapFromReceipt() ở trên vừa đổi giaNhap của biến thể —
    // bảng chính/phân loại pending đọc giá từ ProductsStore, không refresh sẽ phải F5 mới
    // thấy giá mới hoặc thấy hàng "tốt nghiệp" khỏi Chờ nhập hàng.
    await Promise.all([refreshInventory(), refreshProducts()]).catch(() => {});
    showPhieuNhapModal.value = false;
  } catch (e) {
    phieuNhapFormError.value = e.message;
  } finally {
    phieuNhapSaving.value = false;
  }
};
const deletePhieuNhap = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deletePhieuNhap')))) return;
  const res = await PhieuNhapKhoService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  phieuNhapList.value = phieuNhapList.value.filter(p => p.phieuNhapId !== id);
  chiTietPhieuNhapList.value = chiTietPhieuNhapList.value.filter(c => c.phieuNhapId !== id);
};

const updatePhieuNhapStatus = async (p, trangThai) => {
  const res = await PhieuNhapKhoService.save(p.phieuNhapId, {
    nhaCungCapId: p.nhaCungCapId,
    nhanVienId: p.nhanVienId,
    ngayNhap: p.ngayNhap,
    tongTien: p.tongTien,
    trangThai,
    ghiChu: p.ghiChu,
  });
  if (!res.ok) { showToast(t('admin.errors.updateFailed', { status: res.status })); return; }
  const idx = phieuNhapList.value.findIndex(x => x.phieuNhapId === p.phieuNhapId);
  if (idx !== -1) phieuNhapList.value[idx] = { ...phieuNhapList.value[idx], trangThai };
};

const showPhieuNhapDetailModal = ref(false);
const phieuNhapDetailData = ref(null);
const phieuNhapDetailItems = computed(() =>
  chiTietPhieuNhapList.value.filter(c => c.phieuNhapId === phieuNhapDetailData.value?.phieuNhapId),
);
// Serial thật đã nhập kho của riêng phiếu này — gộp theo bienTheId để hiện dưới từng dòng hàng.
const phieuNhapDetailSerials = ref([]);
const phieuNhapDetailSerialsFor = (bienTheId) =>
  phieuNhapDetailSerials.value.filter((s) => s.bienTheId === bienTheId);
const openPhieuNhapDetail = async (p) => {
  phieuNhapDetailData.value = p;
  showPhieuNhapDetailModal.value = true;
  phieuNhapDetailSerials.value = await ChiTietSanPhamService.getByPhieuNhap(p.phieuNhapId).catch(() => []);
};

// Phiếu nhập kho chỉ là chứng từ đối soát nhà cung cấp — hoàn toàn tách rời việc nhập serial
// thật vào kho (tab "Tồn kho", vì serial là mã vật lý trên máy, hệ thống không tự bịa ra
// được). soLuong ghi trên phiếu có thể không khớp số serial nhân viên đã thực sự nhập —
// hiện cảnh báo đối chiếu (không chặn, vì 2 việc có thể lệch thời điểm) để nhân viên tự biết
// còn thiếu bao nhiêu máy chưa gán serial cho đúng lô hàng này.
const tonThucTeCuaBienThe = (bienTheId) =>
  inventory.value.find((i) => i.bienThe?.bienTheId === bienTheId)?.soLuongTon ?? 0;

const printEsc = (v) => String(v ?? '').replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));

// In HTML qua iframe ẩn thay vì window.open('_blank') — tránh mở tab/cửa sổ mới lộ ra
// phía sau hộp thoại in, và tránh window.print() in nguyên trang admin (sidebar, topbar...).
const printHtmlInIframe = (html) => {
  const iframe = document.createElement('iframe');
  iframe.style.cssText = 'position:fixed;width:0;height:0;border:0;visibility:hidden;';
  document.body.appendChild(iframe);
  iframe.contentDocument.write(html);
  iframe.contentDocument.close();
  iframe.onload = () => {
    iframe.contentWindow.print();
    setTimeout(() => document.body.removeChild(iframe), 500);
  };
};

// In DANH SÁCH nhiều phiếu — chỉ bảng tóm tắt (báo cáo/xem nhanh cho quản lý), không
// kèm chi tiết từng dòng hàng (kèm hết sẽ ra rất nhiều trang nếu danh sách dài).
// Muốn xem chi tiết 1 phiếu cụ thể để đối chiếu/ký nhận thì dùng "In phiếu" trong
// modal Chi tiết (xem printPhieuNhapDetail bên dưới).
const printPhieuNhapList = () => {
  const headers = ['Mã', 'Ngày nhập', 'Nhà cung cấp', 'Nhân viên', 'Tổng tiền', 'Trạng thái'];
  const rows = filteredPhieuNhap.value.map(p => [
    p.maPhieuNhap, formatDate(p.ngayNhap), supplierName(p.nhaCungCapId),
    staffName(p.nhanVienId), formatPrice(p.tongTien ?? 0), statusLabel(p.trangThai),
  ]);
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Danh sách phiếu nhập</title>
    <style>
      body{font-family:Arial,sans-serif;padding:24px;color:#111;}
      h1{font-size:18px;margin-bottom:16px;}
      table{width:100%;border-collapse:collapse;font-size:13px;}
      th,td{border:1px solid #999;padding:6px 10px;text-align:left;}
      th{background:#eee;}
    </style></head><body>
    <h1>Danh sách phiếu nhập kho</h1>
    <table><thead><tr>${headers.map(h => `<th>${printEsc(h)}</th>`).join('')}</tr></thead>
    <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${printEsc(c)}</td>`).join('')}</tr>`).join('')}</tbody></table>
    </body></html>`;
  printHtmlInIframe(html);
};

// In 1 PHIẾU — chứng từ đầy đủ để đối chiếu/lưu kho: thông tin phiếu, bảng chi tiết hàng,
// tổng tiền, và 3 dòng ký tên (người lập phiếu / thủ kho / người giao hàng).
const printPhieuNhapDetail = (p) => {
  if (!p) return;
  const items = chiTietPhieuNhapList.value.filter(c => c.phieuNhapId === p.phieuNhapId);
  const itemRows = items.map((c, i) => `<tr>
      <td class="center">${i + 1}</td>
      <td>${printEsc(c.maSku)}</td>
      <td class="center">${printEsc(c.soLuong)}</td>
      <td class="right">${printEsc(formatPrice(c.donGiaNhap))}</td>
      <td class="right">${printEsc(formatPrice(c.thanhTien))}</td>
    </tr>`).join('') || `<tr><td colspan="5" class="center muted">Không có hàng</td></tr>`;
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>Phiếu nhập ${printEsc(p.maPhieuNhap)}</title>
    <style>
      body{font-family:Arial,sans-serif;padding:28px;color:#111;}
      h1{font-size:18px;margin:0 0 4px;}
      .sub{font-size:12px;color:#555;margin-bottom:18px;}
      .info{display:flex;justify-content:space-between;font-size:13px;margin-bottom:16px;}
      table{width:100%;border-collapse:collapse;font-size:13px;margin-bottom:6px;}
      th,td{border:1px solid #999;padding:6px 8px;text-align:left;}
      th{background:#eee;}
      .center{text-align:center;} .right{text-align:right;} .muted{color:#888;}
      .total{text-align:right;font-weight:bold;font-size:14px;margin-bottom:40px;}
      .signs{display:flex;justify-content:space-between;text-align:center;font-size:13px;}
      .signs div{width:30%;}
      .sign-line{margin-top:60px;border-top:1px solid #333;padding-top:4px;}
    </style></head><body>
    <h1>PHIẾU NHẬP KHO</h1>
    <div class="sub">Số phiếu: ${printEsc(p.maPhieuNhap)}</div>
    <div class="info">
      <div>Ngày nhập: ${printEsc(formatDate(p.ngayNhap))}</div>
      <div>Nhà cung cấp: ${printEsc(supplierName(p.nhaCungCapId))}</div>
      <div>Nhân viên: ${printEsc(staffName(p.nhanVienId))}</div>
    </div>
    <table>
      <thead><tr><th style="width:36px;">#</th><th>Mã SKU</th><th class="center" style="width:80px;">Số lượng</th><th class="right" style="width:120px;">Đơn giá</th><th class="right" style="width:130px;">Thành tiền</th></tr></thead>
      <tbody>${itemRows}</tbody>
    </table>
    <div class="total">Tổng tiền: ${printEsc(formatPrice(p.tongTien ?? 0))}</div>
    <div class="signs">
      <div>Người lập phiếu<div class="sign-line"></div></div>
      <div>Thủ kho<div class="sign-line"></div></div>
      <div>Người giao hàng<div class="sign-line"></div></div>
    </div>
    </body></html>`;
  printHtmlInIframe(html);
};

// Xuất CSV (mở được bằng Excel) — khỏi cần thêm thư viện xlsx cho một bảng đơn giản
const exportPhieuNhapExcel = () => {
  const rows = [
    ['Mã', 'Ngày nhập', 'Nhà cung cấp', 'Nhân viên', 'Tổng tiền', 'Trạng thái'],
    ...filteredPhieuNhap.value.map(p => [
      p.maPhieuNhap, formatDate(p.ngayNhap), supplierName(p.nhaCungCapId),
      staffName(p.nhanVienId), p.tongTien ?? 0, statusLabel(p.trangThai),
    ]),
  ];
  const csv = rows.map(r => r.map(v => `"${String(v ?? '').replaceAll('"', '""')}"`).join(',')).join('\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `phieu-nhap-kho-${Date.now()}.csv`;
  a.click();
  URL.revokeObjectURL(url);
};
</script>

<template>
  <div class="inv">
    <div class="inv-tabs">
      <button class="inv-btn inv-btn--ghost" :class="{ 'is-on': khoTab==='ton-kho' }" @click="khoTab='ton-kho'">
        <Package :size="15" /> {{ t('admin.inventory.tabStock') }}
      </button>
      <button class="inv-btn inv-btn--ghost" :class="{ 'is-on': khoTab==='phieu-nhap' }" @click="khoTab='phieu-nhap'; ensurePhieuNhapData()">
        <ClipboardList :size="15" /> {{ t('admin.inventory.tabReceipts') }}
      </button>
    </div>

    <template v-if="khoTab==='ton-kho'">
      <div class="inv-stats">
        <div class="inv-stat inv-stat--blue" :class="{ 'is-on': !invFilterStatus }" @click="invFilterStatus = ''">
          <div class="inv-stat__icon"><Package :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.inventory.statTotalSku') }}</div>
            <div class="inv-stat__value">{{ inventory.length }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--green">
          <div class="inv-stat__icon"><BarChart3 :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.inventory.statTotalStock') }}</div>
            <div class="inv-stat__value">{{ totalStockQty }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--cyan" :class="{ 'is-on': invFilterStatus === 'pending' }" @click="toggleInvQuickFilter('pending')">
          <div class="inv-stat__icon"><Truck :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ tt('admin.inventory.statPending', 'Chờ nhập hàng') }}</div>
            <div class="inv-stat__value">{{ pendingItems.length }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--amber" :class="{ 'is-on': invFilterStatus === 'low' }" @click="toggleInvQuickFilter('low')">
          <div class="inv-stat__icon"><AlertTriangle :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.inventory.statLowStock') }}</div>
            <div class="inv-stat__value">{{ lowStockOnlyItems.length }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--red" :class="{ 'is-on': invFilterStatus === 'out' }" @click="toggleInvQuickFilter('out')">
          <div class="inv-stat__icon"><Ban :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.inventory.statOutOfStock') }}</div>
            <div class="inv-stat__value">{{ outOfStockItems.length }}</div>
          </div>
        </div>
      </div>

      <!-- ══════════ CARD LỚN DUY NHẤT: toolbar + filter + bảng nằm chung ══════════ -->
      <section class="inv-card">
        <p v-if="invFilterStatus" class="inv-quickview-note">
          {{ tt('admin.inventory.quickViewNote', 'Đang xem') }}: <b>{{ stockStatusLabel(invFilterStatus) }}</b>
          <button type="button" class="inv-quickview-note__clear" @click="invFilterStatus = ''">{{ tt('admin.inventory.quickViewClear', 'Xem danh sách bình thường') }}</button>
        </p>

        <!-- THANH CÔNG CỤ -->
        <div class="inv-bar">
          <span class="inv-bar__count">{{ flatInventory.length }}/{{ inventory.length }} {{ t('admin.inventory.colSku') }}</span>
          <div class="inv-search">
            <Search :size="14" class="inv-search__icon" />
            <input v-model="inventorySearch" :placeholder="t('admin.inventory.searchPlaceholder')" />
          </div>
          <button type="button" class="inv-btn inv-btn--ghost" :class="{ 'is-on': isInvFilterOpen }" @click="isInvFilterOpen = !isInvFilterOpen">
            <Filter :size="14" /> {{ tt('admin.common.filter', 'Bộ lọc') }}
            <span v-if="invActiveFilterCount" class="inv-chip">{{ invActiveFilterCount }}</span>
            <ChevronDown :size="13" class="inv-caret" :class="{ 'is-open': isInvFilterOpen }" />
          </button>
        </div>

        <!-- BỘ LỌC (nằm trong card) -->
        <div class="inv-filter" :class="{ 'is-open': isInvFilterOpen }">
          <div class="inv-filter__panel">
            <div class="inv-filter__grid">
              <label class="inv-field">
                <span>{{ t('admin.inventory.colStock') }}</span>
                <select v-model="invFilterStatus">
                  <option value="">{{ t('admin.inventory.filterAll') }}</option>
                  <option value="pending">{{ tt('admin.inventory.statPending', 'Chờ nhập hàng') }}</option>
                  <option value="out">{{ t('admin.inventory.filterOut') }}</option>
                  <option value="low">{{ t('admin.inventory.filterLow') }}</option>
                  <option value="ok">{{ t('admin.inventory.filterOk') }}</option>
                </select>
              </label>
              <label class="inv-field">
                <span>{{ t('admin.productModal.brandLabel') }}</span>
                <select v-model="invFilterThuongHieu">
                  <option value="">{{ t('admin.inventory.filterAll') }}</option>
                  <option v-for="o in invBrandOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
                </select>
              </label>
              <label class="inv-field">
                <span>{{ t('admin.productModal.categoryLabel') }}</span>
                <select v-model="invFilterDanhMuc">
                  <option value="">{{ t('admin.inventory.filterAll') }}</option>
                  <option v-for="o in invCategoryOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
                </select>
              </label>
            </div>
            <div class="inv-filter__foot">
              <div class="inv-filter__btns">
                <button type="button" class="inv-btn inv-btn--ghost" @click="clearInvFilters">{{ tt('admin.variants.clearFilters', 'Xóa lọc') }}</button>
                <button type="button" class="inv-btn inv-btn--primary" @click="isInvFilterOpen = false">{{ tt('admin.variants.filterDone', 'Xong') }}</button>
              </div>
            </div>
          </div>
        </div>

        <!-- BẢNG -->
        <div v-if="InventoryStore.loading" class="inv-empty">{{ t('admin.inventory.loading') }}</div>
        <div v-else class="inv-table-wrap">
          <table class="inv-table">
            <thead>
              <tr>
                <th>{{ tt('admin.inventory.colProductCode', 'Mã sản phẩm') }}</th>
                <th>{{ t('admin.variants.colProduct') }}</th>
                <th>{{ t('admin.variants.colConfig') }}</th>
                <th class="ta-r">{{ t('admin.variants.colPriceSell') }}</th>
                <th class="ta-r">{{ tt('admin.inventory.colPriceBuy', 'Giá vốn') }}</th>
                <th class="ta-c">{{ t('admin.inventory.colStock') }}</th>
                <th class="ta-c">{{ tt('admin.inventory.colHeld', 'Giữ') }}</th>
                <th>{{ t('admin.variants.colStatus') }}</th>
                <th>{{ tt('admin.inventory.colUpdatedAt', 'Ngày cập nhật') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="({ item, v, status }) in pagedFlatInventory" :key="item.tonKhoId" class="inv-row" @click="openStockDetail(item)">
                <td class="inv-code">{{ maSanPhamCuaItem(item) }}</td>
                <td class="inv-td-name">
                  <div class="inv-name">
                    <img :src="v?.hinhAnhChinh" class="inv-thumb" alt="" @error="$event.target.style.visibility='hidden'" />
                    <div class="inv-name__text">
                      <div class="inv-name__main">{{ v?.tenSanPham || item.bienThe?.sanPham?.tenSanPham || '—' }}</div>
                      <div class="inv-name__sub">{{ item.bienThe?.maSku || '—' }}</div>
                    </div>
                  </div>
                </td>
                <td class="inv-muted">
                  <div v-if="v?.cpu || v?.ram || v?.mauSac" class="inv-config">
                    <span :class="{ 'inv-config-diff': hasCpuDiff(v) }">{{ v.cpu }}</span>
                    <span :class="{ 'inv-config-diff': hasRamDiff(v) }">{{ v.ram }}</span>
                    <span :class="{ 'inv-config-diff': hasMauSacDiff(v) }">{{ v.mauSac }}</span>
                  </div>
                  <span v-else>—</span>
                </td>
                <td class="ta-r inv-price">{{ formatPrice(v?.giaBan) }}</td>
                <td class="ta-r inv-muted">{{ formatPrice(v?.giaNhap) }}</td>
                <td class="ta-c"><span class="inv-ton" :class="{ 'text-danger': status==='out', 'text-warning': status==='low', 'text-success': status==='ok', 'text-info': status==='pending' }">{{ item.soLuongTon ?? '—' }}</span></td>
                <td class="ta-c"><span class="inv-held" :class="{ 'text-warning': item.soLuongGiu > 0 }">{{ item.soLuongGiu ?? 0 }}</span></td>
                <td>
                  <span class="inv-tag" :class="'inv-tag--' + status">{{ stockStatusLabel(status) }}</span>
                </td>
                <td class="inv-muted">{{ formatDate(v?.ngayCapNhat) }}</td>
              </tr>
              <tr v-if="flatInventory.length === 0"><td colspan="9" class="inv-empty">{{ t('admin.inventory.empty') }}</td></tr>
            </tbody>
          </table>
        </div>
        <footer v-if="invTotalPages > 1" class="inv-pager">
          <Pagination :current-page="invCurrentPage" :total-pages="invTotalPages" @page-change="invCurrentPage = $event" />
        </footer>
      </section>
    </template>

    <!-- ══ TAB: PHIEU NHAP ══ -->
    <template v-else-if="khoTab==='phieu-nhap'">
      <div class="inv-stats">
        <div class="inv-stat inv-stat--purple">
          <div class="inv-stat__icon"><ClipboardList :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.phieuNhap.statTotal') }}</div>
            <div class="inv-stat__value">{{ phieuNhapCounts.total }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--amber">
          <div class="inv-stat__icon"><Clock :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.phieuNhap.statPending') }}</div>
            <div class="inv-stat__value">{{ phieuNhapCounts.choDuyet }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--green">
          <div class="inv-stat__icon"><CheckCircle2 :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.phieuNhap.statDone') }}</div>
            <div class="inv-stat__value">{{ phieuNhapCounts.hoanThanh }}</div>
          </div>
        </div>
        <div class="inv-stat inv-stat--red">
          <div class="inv-stat__icon"><XCircle :size="22" /></div>
          <div>
            <div class="inv-stat__label">{{ t('admin.phieuNhap.statCancelled') }}</div>
            <div class="inv-stat__value">{{ phieuNhapCounts.huy }}</div>
          </div>
        </div>
      </div>

      <div class="inv-bar">
        <div class="inv-search">
          <Search :size="14" class="inv-search__icon" />
          <input v-model="phieuNhapSearch" :placeholder="t('admin.phieuNhap.searchPlaceholder')" />
        </div>
        <select v-model="phieuNhapStatusFilter" class="inv-select">
          <option value="">{{ t('admin.inventory.filterAll') }}</option>
          <option value="cho_duyet">{{ t('admin.statusLabel.cho_duyet') }}</option>
          <option value="hoan_thanh">{{ t('admin.statusLabel.hoan_thanh') }}</option>
          <option value="huy">{{ t('admin.statusLabel.huy') }}</option>
        </select>
        <div class="inv-bar__actions">
          <button class="inv-btn inv-btn--ghost" @click="printPhieuNhapList"><Printer :size="14" /> {{ t('admin.phieuNhap.printPdf') }}</button>
          <button class="inv-btn inv-btn--ghost" @click="exportPhieuNhapExcel"><Download :size="14" /> {{ t('admin.phieuNhap.exportExcel') }}</button>
          <button class="inv-btn inv-btn--primary" @click="openAddPhieuNhap"><Plus :size="14" /> {{ t('admin.phieuNhap.add') }}</button>
        </div>
      </div>

      <section class="inv-card">
        <div class="inv-table-wrap">
          <table class="inv-table">
            <thead>
              <tr>
                <th style="width:40px;">{{ t('admin.common.stt') }}</th>
                <th>{{ t('admin.phieuNhap.colCode') }}</th>
                <th>{{ t('admin.phieuNhap.colDate') }}</th>
                <th>{{ t('admin.phieuNhap.colSupplier') }}</th>
                <th>{{ t('admin.phieuNhap.colStaff') }}</th>
                <th class="ta-r">{{ t('admin.phieuNhap.colTotal') }}</th>
                <th>{{ t('admin.phieuNhap.colStatus') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(p, idx) in pagedPhieuNhap" :key="p.phieuNhapId" class="inv-row" @click="openPhieuNhapDetail(p)">
                <td class="inv-muted">{{ pnCurrentPage * pnPageSize + idx + 1 }}</td>
                <td class="inv-code">{{ p.maPhieuNhap }}</td>
                <td>{{ formatDate(p.ngayNhap) }}</td>
                <td>{{ supplierName(p.nhaCungCapId) }}</td>
                <td>{{ staffName(p.nhanVienId) }}</td>
                <td class="ta-r inv-price">{{ formatPrice(p.tongTien) }}</td>
                <td>
                  <span class="inv-tag" :style="{ background: phieuNhapStatusColor(p.trangThai).bg, color: phieuNhapStatusColor(p.trangThai).text }">
                    <component :is="phieuNhapStatusIcon(p.trangThai)" :size="13" /> {{ statusLabel(p.trangThai) }}
                  </span>
                </td>
              </tr>
              <tr v-if="filteredPhieuNhap.length===0"><td colspan="7" class="inv-empty">{{ t('admin.phieuNhap.empty') }}</td></tr>
            </tbody>
          </table>
        </div>
        <footer v-if="pnTotalPages > 1" class="inv-pager">
          <Pagination :current-page="pnCurrentPage" :total-pages="pnTotalPages" @page-change="pnCurrentPage = $event" />
        </footer>
      </section>
    </template>
  </div>

  <!-- ══ MODAL TAO PHIEU NHAP ══ -->
  <div v-if="showPhieuNhapModal" class="inv-modal-mask" @click.self="showPhieuNhapModal=false">
    <div class="inv-modal">
      <header class="inv-modal__head">
        <span>{{ editingPhieuNhapId ? t('admin.phieuNhapModal.titleEdit') : t('admin.phieuNhapModal.title') }}</span>
        <button class="inv-icon-btn" :aria-label="t('common.close')" @click="showPhieuNhapModal=false"><X :size="16" /></button>
      </header>
      <div class="inv-modal__body">
        <p v-if="phieuNhapFormError" class="inv-alert">{{ phieuNhapFormError }}</p>
        <div class="inv-grid mb-3">
          <label class="inv-field">
            <span>{{ t('admin.phieuNhapModal.supplierLabel') }}</span>
            <SearchSelect v-model="phieuNhapForm.nhaCungCapId" :options="supplierOptions" :placeholder="t('admin.phieuNhapModal.selectPlaceholder')" />
          </label>
          <label class="inv-field">
            <span>{{ t('admin.phieuNhapModal.staffLabel') }}</span>
            <SearchSelect v-model="phieuNhapForm.nhanVienId" :options="staffOptions" :placeholder="t('admin.phieuNhapModal.selectPlaceholder')" />
          </label>
          <label class="inv-field">
            <span>{{ t('admin.phieuNhapModal.dateLabel') }}</span>
            <input v-model="phieuNhapForm.ngayNhap" type="datetime-local" />
          </label>
          <label class="inv-field">
            <span>{{ t('admin.phieuNhapModal.noteLabel') }}</span>
            <input v-model="phieuNhapForm.ghiChu" />
          </label>
        </div>

        <div class="inv-section-title"><Package :size="14" /> {{ t('admin.phieuNhapModal.itemsLabel') }}</div>
        <div class="inv-item-head">
          <span style="flex:2 1 0;">{{ t('admin.phieuNhapModal.colProduct') }}</span>
          <span style="flex:2 1 0;">{{ t('admin.phieuNhapModal.colVariant') }}</span>
          <span style="flex:0 0 80px;">{{ t('admin.phieuNhapModal.colQty') }}</span>
          <span style="flex:0 0 110px;">{{ t('admin.phieuNhapModal.colPrice') }}</span>
          <span style="flex:0 0 34px;"></span>
        </div>
        <div class="d-flex flex-column gap-2 mb-2">
          <div v-for="(row, idx) in phieuNhapForm.items" :key="idx" class="inv-item-block">
            <div class="inv-item-row">
              <div style="flex:2 1 0;min-width:0;">
                <SearchSelect
                  v-model="row.sanPhamId" :options="productOptionsForPhieuNhap"
                  :placeholder="t('admin.phieuNhapModal.selectProductPlaceholder')"
                  @update:model-value="row.bienTheId=''; row.lockedCount=0; row.serials=['']"
                />
              </div>
              <div style="flex:2 1 0;min-width:0;">
                <SearchSelect
                  v-model="row.bienTheId" :disabled="!row.sanPhamId"
                  :options="variantsForProduct(row.sanPhamId)"
                  :placeholder="t('admin.phieuNhapModal.selectVariantPlaceholder')"
                  @update:model-value="row.lockedCount=0; syncPhieuNhapItemSerials(row)"
                />
              </div>
              <input v-model="row.soLuong" type="number" min="1" style="flex:0 0 80px;" :placeholder="t('admin.phieuNhapModal.qtyPlaceholder')" @change="clampPhieuNhapSoLuong(row)" />
              <input v-model="row.donGia" type="number" min="0" style="flex:0 0 110px;" :placeholder="t('admin.phieuNhapModal.unitPricePlaceholder')" />
              <button class="inv-icon-btn inv-icon-btn--danger" style="flex:0 0 34px;" :aria-label="t('common.remove')" @click="removePhieuNhapItemRow(idx)"><X :size="14" /></button>
            </div>
            <div v-if="row.bienTheId" class="inv-serial-grid">
              <span class="inv-hint" style="grid-column:1/-1;">{{ tt('admin.phieuNhapModal.serialsLabel', 'Serial cho dòng này (đủ số lượng)') }}</span>
              <input
                v-for="(s, sIdx) in row.serials" :key="sIdx"
                v-model="row.serials[sIdx]" :disabled="sIdx < (row.lockedCount ?? 0)"
                class="inv-mono" :placeholder="`Serial #${sIdx + 1}`"
              />
            </div>
          </div>
        </div>
        <button class="inv-btn inv-btn--ghost mb-3" @click="addPhieuNhapItemRow">{{ t('admin.phieuNhapModal.addRow') }}</button>

        <div class="inv-total">{{ t('admin.phieuNhapModal.totalLabel') }} {{ formatPrice(phieuNhapItemsTotal) }}</div>
      </div>
      <footer class="inv-modal__foot inv-modal__foot--end">
        <button class="inv-btn inv-btn--ghost" @click="showPhieuNhapModal=false">{{ t('admin.phieuNhapModal.cancel') }}</button>
        <button class="inv-btn inv-btn--primary" :disabled="phieuNhapSaving" @click="savePhieuNhap">{{ editingPhieuNhapId ? t('admin.phieuNhapModal.saveEdit') : t('admin.phieuNhapModal.save') }}</button>
      </footer>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET PHIEU NHAP ══ -->
  <div v-if="showPhieuNhapDetailModal" class="inv-modal-mask" @click.self="showPhieuNhapDetailModal=false">
    <div class="inv-modal inv-modal--hep">
      <header v-if="phieuNhapDetailData" class="inv-modal__head">
        <div class="d-flex align-items-center gap-3">
          <div class="inv-modal__icon"><ClipboardList :size="18" /></div>
          <div>
            <div style="font-weight:700;font-size:0.95rem;">
              {{ t('admin.phieuNhapDetailModal.title') }}
              <span class="inv-mono inv-muted" style="margin-left:4px;font-size:0.8rem;">{{ phieuNhapDetailData.maPhieuNhap }}</span>
            </div>
            <div class="inv-muted" style="font-size:0.78rem;">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }} · {{ formatDate(phieuNhapDetailData.ngayNhap) }}</div>
          </div>
        </div>
        <button class="inv-icon-btn" :aria-label="t('common.close')" @click="showPhieuNhapDetailModal=false"><X :size="16" /></button>
      </header>

      <div v-if="phieuNhapDetailData" class="inv-modal__body" style="padding:0;">
        <div class="inv-chips">
          <span class="inv-chip-info"><Building2 :size="13" /> {{ t('admin.phieuNhap.colSupplier') }}: <b>{{ supplierName(phieuNhapDetailData.nhaCungCapId) }}</b></span>
          <span class="inv-chip-info"><User :size="13" /> {{ t('admin.phieuNhap.colStaff') }}: <b>{{ staffName(phieuNhapDetailData.nhanVienId) }}</b></span>
          <span class="inv-chip-info"><Calendar :size="13" /> {{ t('admin.phieuNhap.colDate') }}: <b>{{ formatDate(phieuNhapDetailData.ngayNhap) }}</b></span>
          <span class="inv-tag" :style="{ background: phieuNhapStatusColor(phieuNhapDetailData.trangThai).bg, color: phieuNhapStatusColor(phieuNhapDetailData.trangThai).text }">
            <component :is="phieuNhapStatusIcon(phieuNhapDetailData.trangThai)" :size="13" /> {{ statusLabel(phieuNhapDetailData.trangThai) }}
          </span>
          <div v-if="phieuNhapDetailData.ghiChu" class="inv-muted" style="width:100%;font-size:0.8rem;font-style:italic;display:flex;align-items:center;gap:4px;"><FileText :size="12" /> {{ phieuNhapDetailData.ghiChu }}</div>
        </div>

        <div class="inv-table-wrap" style="padding:16px;">
          <table class="inv-table">
            <thead>
              <tr>
                <th>{{ t('admin.inventory.colSku') }}</th>
                <th class="ta-c">{{ t('admin.phieuNhapModal.qtyPlaceholder') }}</th>
                <th class="ta-c">{{ t('admin.phieuNhapModal.actualStockLabel') }}</th>
                <th class="ta-r">{{ t('admin.phieuNhapModal.unitPricePlaceholder') }}</th>
                <th class="ta-r">{{ t('admin.phieuNhapModal.totalLabel') }}</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="c in phieuNhapDetailItems" :key="c.id">
                <tr class="inv-row">
                  <td class="inv-code">{{ c.maSku }}</td>
                  <td class="ta-c" style="font-weight:700;">{{ c.soLuong }}</td>
                  <td class="ta-c">
                    <span :class="tonThucTeCuaBienThe(c.bienTheId) < c.soLuong ? 'text-warning' : 'text-success'" :title="t('admin.phieuNhapModal.actualStockHint')">
                      {{ tonThucTeCuaBienThe(c.bienTheId) }}
                    </span>
                  </td>
                  <td class="ta-r inv-muted">{{ formatPrice(c.donGiaNhap) }}</td>
                  <td class="ta-r inv-price">{{ formatPrice(c.thanhTien) }}</td>
                </tr>
                <tr v-if="phieuNhapDetailSerialsFor(c.bienTheId).length" class="inv-row" style="cursor:default;">
                  <td colspan="5" style="padding-top:0;">
                    <div class="inv-serial-chip-row">
                      <span v-for="s in phieuNhapDetailSerialsFor(c.bienTheId)" :key="s.chiTietId" class="inv-tag inv-tag--soft inv-mono">{{ s.soSerial }}</span>
                    </div>
                  </td>
                </tr>
              </template>
              <tr v-if="phieuNhapDetailItems.length===0"><td colspan="5" class="inv-empty">{{ t('admin.phieuNhap.empty') }}</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <footer v-if="phieuNhapDetailData" class="inv-modal__foot">
        <span class="inv-muted" style="font-size:0.85rem;">{{ phieuNhapDetailItems.length }} {{ t('admin.inventory.colSku') }}</span>
        <div class="d-flex align-items-center gap-2">
          <span class="inv-muted" style="font-size:0.85rem;">{{ t('admin.phieuNhapModal.totalLabel') }}</span>
          <span class="inv-price" style="font-size:1.15rem;">{{ formatPrice(phieuNhapDetailData.tongTien) }}</span>
        </div>
      </footer>
      <footer v-if="phieuNhapDetailData" class="inv-modal__foot inv-modal__foot--end" style="border-top:none;padding-top:0;flex-wrap:wrap;">
        <template v-if="phieuNhapDetailData.trangThai==='cho_duyet'">
          <button class="inv-btn inv-btn--ghost inv-btn--ok" @click="updatePhieuNhapStatus(phieuNhapDetailData,'hoan_thanh')"><Check :size="14" /> {{ t('admin.phieuNhap.approve') }}</button>
          <button class="inv-btn inv-btn--ghost inv-btn--danger" @click="updatePhieuNhapStatus(phieuNhapDetailData,'huy')"><X :size="14" /> {{ t('admin.phieuNhap.cancel') }}</button>
          <button class="inv-btn inv-btn--ghost" @click="showPhieuNhapDetailModal=false; openEditPhieuNhap(phieuNhapDetailData)"><Pencil :size="14" /> {{ t('admin.phieuNhap.editAction') }}</button>
          <button class="inv-btn inv-btn--ghost inv-btn--danger" @click="showPhieuNhapDetailModal=false; deletePhieuNhap(phieuNhapDetailData.phieuNhapId)"><Trash2 :size="14" /> {{ t('admin.phieuNhap.deleteAction') }}</button>
        </template>
        <button class="inv-btn inv-btn--ghost" @click="printPhieuNhapDetail(phieuNhapDetailData)"><Printer :size="14" /> {{ t('admin.phieuNhap.printPdf') }}</button>
        <button class="inv-btn inv-btn--ghost" style="margin-left:auto;" @click="showPhieuNhapDetailModal=false">{{ t('admin.promoModal.cancel') }}</button>
      </footer>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET BIEN THE (gop 2 tab: Danh sach serial / Them hang) ══ -->
  <div v-if="showDetailModal" class="inv-modal-mask" @click.self="showDetailModal=false">
    <div class="inv-modal" style="width:760px;">
      <header class="inv-modal__head" style="align-items:flex-start;">
        <div>
          <div style="font-weight:700;font-size:0.95rem;">
            {{ t('admin.stockDetailModal.titlePrefix') }} {{ detailItem?.bienThe?.maSku || '—' }}
          </div>
          <div style="font-size:0.8rem;color:var(--muted);margin-top:2px;">{{ detailItem?.bienThe?.sanPham?.tenSanPham || '—' }}</div>
          <div class="d-flex gap-1 mt-1 flex-wrap">
            <span v-if="getVariantInfo(detailItem)?.cpu" class="inv-tag inv-tag--soft">{{ getVariantInfo(detailItem).cpu }}</span>
            <span v-if="getVariantInfo(detailItem)?.ram" class="inv-tag inv-tag--soft">{{ getVariantInfo(detailItem).ram }}</span>
            <span v-if="getVariantInfo(detailItem)?.oCung" class="inv-tag inv-tag--soft">{{ getVariantInfo(detailItem).oCung }}</span>
            <span v-if="getVariantInfo(detailItem)?.mauSac" class="inv-tag inv-tag--soft">{{ getVariantInfo(detailItem).mauSac }}</span>
          </div>
        </div>
        <button class="inv-icon-btn" :aria-label="t('common.close')" @click="showDetailModal=false"><X :size="16" /></button>
      </header>

      <div class="inv-tabs" style="padding:12px 20px 0;">
        <button type="button" class="inv-btn inv-btn--ghost" :class="{ 'is-on': detailTab==='serials' }" @click="detailTab='serials'">{{ tt('admin.stockDetailModal.tabSerials', 'Danh sách serial') }}</button>
        <button type="button" class="inv-btn inv-btn--ghost" :class="{ 'is-on': detailTab==='add' }" @click="detailTab='add'">{{ tt('admin.stockDetailModal.tabAdd', 'Thêm hàng') }}</button>
      </div>

      <!-- Tab: Danh sach serial -->
      <template v-if="detailTab==='serials'">
        <div class="inv-modal__body" style="padding-bottom:0;">
          <div class="d-flex gap-2">
            <div class="inv-search" style="flex:1;">
              <Search :size="14" class="inv-search__icon" />
              <input v-model="detailSerialSearch" :placeholder="tt('admin.stockDetailModal.searchPlaceholder', 'Tìm serial...')" />
            </div>
            <select v-model="detailSerialStatusFilter" class="inv-select" style="width:170px;">
              <option value="">{{ tt('admin.stockDetailModal.allStatus', 'Tất cả trạng thái') }}</option>
              <option v-for="s in SERIAL_STATUS_OPTIONS" :key="s" :value="s">{{ stockDetailStatusLabel(s) }}</option>
            </select>
          </div>
        </div>
        <div class="inv-modal__body" style="padding-top:10px;overflow-y:auto;max-height:420px;">
          <div v-if="detailSerialsLoading" class="inv-empty">{{ t('admin.stockDetailModal.loading') }}</div>
          <div v-else-if="filteredDetailSerials.length === 0" class="inv-empty">{{ t('admin.stockDetailModal.empty') }}</div>
          <table v-else class="inv-table">
            <thead>
              <tr>
                <th style="width:40px;">{{ t('admin.stockDetailModal.colIndex') }}</th>
                <th>{{ t('admin.stockDetailModal.colSerial') }}</th>
                <th>{{ t('admin.stockDetailModal.colImportDate') }}</th>
                <th>{{ t('admin.stockDetailModal.colStatus') }}</th>
                <th style="width:60px;"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, idx) in filteredDetailSerials" :key="s.chiTietId" class="inv-row">
                <td class="inv-muted">{{ idx + 1 }}</td>
                <td class="inv-mono" style="font-weight:600;">{{ s.soSerial }}</td>
                <td class="inv-muted">{{ formatDate(s.ngayNhapKho) }}</td>
                <td>{{ stockDetailStatusLabel(s.trangThai) }}</td>
                <td>
                  <button v-if="s.trangThai==='trong_kho'" class="inv-icon-btn inv-icon-btn--danger" :title="t('admin.stockDetailModal.deleteSerial')" :aria-label="t('admin.stockDetailModal.deleteSerial')" @click="removeStockSerial(s.chiTietId)"><X :size="12" /></button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer class="inv-modal__foot inv-modal__foot--end">
          <button class="inv-btn inv-btn--ghost" @click="showDetailModal=false">{{ t('admin.stockModal.cancel') }}</button>
        </footer>
      </template>

      <!-- Tab: Them hang -->
      <template v-else>
        <div class="inv-modal__body">
          <div class="inv-grid">
            <label class="inv-field">
              <span>{{ tt('admin.stockModal.giaNhapLabel', 'Giá nhập') }}</span>
              <div class="inv-readonly">{{ formatPrice(getVariantInfo(detailItem)?.giaNhap) }}</div>
              <em class="inv-hint">{{ tt('admin.stockModal.giaNhapHint', 'Tự lấy từ phiếu nhập gần nhất, không sửa tay ở đây') }}</em>
            </label>
            <label class="inv-field">
              <span>{{ tt('admin.stockModal.giaBanLabel', 'Giá bán') }}</span>
              <input v-model="stockForm.giaBan" type="number" min="0" />
              <em class="inv-hint">{{ tt('admin.stockModal.giaBanHint', 'Nhập đủ giá nhập + giá bán + serial thì hàng mới rời khỏi "Chờ nhập hàng"') }}</em>
            </label>
            <label class="inv-field">
              <span>{{ t('admin.stockModal.stockLabel') }}</span>
              <div class="inv-readonly">{{ detailItem?.soLuongTon ?? 0 }}</div>
              <em class="inv-hint">{{ t('admin.stockModal.stockHint') }}</em>
            </label>
            <label class="inv-field">
              <span>{{ t('admin.stockModal.heldLabel') }}</span>
              <input v-model="stockForm.soLuongGiu" type="number" min="0" />
            </label>
            <label class="inv-field" style="grid-column:1/-1;">
              <span>{{ t('admin.stockModal.minStockLabel') }}</span>
              <input v-model="stockForm.tonKhoToiThieu" type="number" min="0" />
            </label>
            <div class="inv-field" style="grid-column:1/-1;">
              <div class="d-flex justify-content-between align-items-center mb-1">
                <span>{{ t('admin.stockModal.newSerialsLabel') }}</span>
                <label class="inv-btn inv-btn--ghost inv-btn--sm" style="cursor:pointer;">
                  <FolderOpen :size="14" /> {{ t('admin.stockModal.importFromFile') }}
                  <input type="file" accept=".csv,.txt,.xlsx,.xls" class="d-none" @change="importSerialsFromFile" />
                </label>
              </div>
              <div class="d-flex flex-column gap-2">
                <div v-for="(s, idx) in stockForm.newSerials" :key="idx" class="d-flex gap-2 align-items-center">
                  <input v-model="stockForm.newSerials[idx]" :placeholder="t('admin.stockModal.serialPlaceholder')" />
                  <button class="inv-icon-btn inv-icon-btn--danger" :aria-label="t('common.remove')" @click="removeStockSerialRow(idx)"><X :size="14" /></button>
                </div>
              </div>
              <button class="inv-btn inv-btn--ghost mt-2" @click="addStockSerialRow">{{ t('admin.stockModal.addSerialRow') }}</button>
              <em class="inv-hint">{{ t('admin.stockModal.importHint') }}</em>
            </div>
          </div>
        </div>
        <footer class="inv-modal__foot inv-modal__foot--end">
          <button class="inv-btn inv-btn--ghost" @click="showDetailModal=false">{{ t('admin.stockModal.cancel') }}</button>
          <button class="inv-btn inv-btn--primary" :disabled="stockSaving" @click="saveStock">{{ t('admin.stockModal.save') }}</button>
        </footer>
      </template>
    </div>
  </div>

</template>

<style scoped>
/* Nhại đúng bảng màu + tỉ lệ của HangHoa.vue để đồng bộ phong cách toàn bộ khối quản trị —
   cố tình dùng cùng giá trị hex/hồng cứng như HangHoa.vue thay vì biến theme sáng/tối dùng
   chung, cho khớp pixel với các màn hình khác. */
.inv, .inv-modal-mask {
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

  /* 3D Shadow Variables */
  --sh-1: 0 1px 2px rgba(168, 27, 93, .08), 0 1px 3px rgba(168, 27, 93, .05);
  --sh-2: 0 4px 6px rgba(168, 27, 93, .1), 0 2px 4px rgba(168, 27, 93, .06);
  --sh-3: 0 10px 15px rgba(168, 27, 93, .12), 0 4px 6px rgba(168, 27, 93, .08);
}
.inv { font-size: 14px; color: var(--ink); }
.ta-r { text-align: right; }
.ta-c { text-align: center; }
.inv-muted { color: var(--muted); }
.inv-mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }

/* ══════════ TAB TON KHO / PHIEU NHAP ══════════ */
.inv-tabs { display: flex; gap: 8px; margin-bottom: 14px; }

/* ══════════ NÚT ══════════ */
.inv-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 999px; border: 1px solid transparent;
  font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer; white-space: nowrap;
  transition: all 0.15s ease;
}
.inv-btn--sm { padding: 5px 11px; font-size: 12.5px; }
.inv-btn--primary {
  background: var(--pink-600); color: #fff;
  box-shadow: 0 3px 0 #9b1d5c, 0 4px 8px rgba(168, 27, 93, 0.3);
  border-bottom-width: 3px;
}
.inv-btn--primary:hover:not(:disabled) {
  background: var(--pink-700);
  box-shadow: 0 4px 0 #7a1550, 0 6px 12px rgba(168, 27, 93, 0.35);
  transform: translateY(-1px);
}
.inv-btn--primary:active:not(:disabled) {
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  transform: translateY(1px);
}
.inv-btn--ghost { background: #fff; color: var(--pink-700); border-color: var(--pink-200); }
.inv-btn--ghost:hover:not(:disabled) {
  background: var(--pink-50); border-color: var(--pink-300);
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.15);
}
.inv-btn--ghost.is-on { background: var(--pink-100); border-color: var(--pink-300); }
.inv-btn--ok { color: var(--ok-text); border-color: #bbf7d0; }
.inv-btn--ok:hover:not(:disabled) { background: var(--ok-bg); box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.inv-btn--danger { color: var(--danger); border-color: #fecaca; }
.inv-btn--danger:hover:not(:disabled) { background: #fef2f2; box-shadow: 0 2px 4px rgba(220,38,38,0.15); }
.inv-btn:disabled { opacity: .45; cursor: not-allowed; }

.inv-icon-btn {
  background: #fff; border: 1px solid var(--pink-200); color: var(--pink-700);
  width: 30px; height: 30px; border-radius: 50%; cursor: pointer;
  display: inline-grid; place-items: center; flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.1);
  transition: all 0.15s ease;
}
.inv-icon-btn:hover {
  background: var(--pink-50);
  box-shadow: 0 4px 8px rgba(168, 27, 93, 0.15);
  transform: translateY(-1px);
}
.inv-icon-btn--danger { color: var(--danger); border-color: #fecaca; }
.inv-icon-btn--danger:hover { background: #fef2f2; box-shadow: 0 4px 8px rgba(220,38,38,0.2); }

/* ══════════ STAT CARD — khối màu đậm giống ảnh mẫu, icon/số trắng ══════════ */
.inv-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 12px; margin-bottom: 14px; }
.inv-stat {
  display: flex; align-items: center; gap: 14px;
  border-radius: 14px; padding: 16px 18px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, .12);
  color: #fff; cursor: pointer; user-select: none;
  border: 2px solid transparent; transition: transform .12s, box-shadow .12s, border-color .12s;
}
.inv-stat:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0, 0, 0, .18); }
.inv-stat.is-on { border-color: rgba(255, 255, 255, .85); }
.inv-stat--blue   { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.inv-stat--green  { background: linear-gradient(135deg, #34d399, #059669); cursor: default; }
.inv-stat--green:hover { transform: none; box-shadow: 0 4px 14px rgba(0, 0, 0, .12); }
.inv-stat--amber  { background: linear-gradient(135deg, #fbbf24, #d97706); }
.inv-stat--red    { background: linear-gradient(135deg, #f87171, #dc2626); }
.inv-stat--purple { background: linear-gradient(135deg, #a78bfa, #7c3aed); }
.inv-stat--cyan   { background: linear-gradient(135deg, #22d3ee, #0891b2); }
.inv-stat__icon {
  width: 46px; height: 46px; border-radius: 12px; flex-shrink: 0;
  background: rgba(255, 255, 255, .22);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.inv-stat__label { font-size: 12.5px; color: rgba(255, 255, 255, .85); margin-bottom: 2px; font-weight: 600; }
.inv-stat__value { font-size: 1.6rem; font-weight: 800; color: #fff; }

.inv-quickview-note {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  background: var(--pink-50); border: 1px solid var(--pink-200); border-radius: 10px;
  padding: 8px 14px; margin: -2px 0 14px; font-size: 13px; color: var(--ink);
}
.inv-quickview-note__clear {
  margin-left: auto; background: none; border: none; color: var(--pink-600);
  font-size: 12.5px; font-weight: 600; cursor: pointer; text-decoration: underline;
  font-family: inherit; padding: 0;
}
.inv-quickview-note__clear:hover { color: var(--pink-700); }

/* ══════════ THANH CÔNG CỤ ══════════ */
.inv-bar {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  padding: 12px 16px; margin-bottom: 12px; box-shadow: var(--sh-2);
}
.inv-bar__actions { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-wrap: wrap; }

.inv-search { position: relative; flex: 1 1 240px; min-width: 200px; max-width: 340px; }
.inv-search input {
  width: 100%; padding: 8px 14px 8px 34px;
  border: 1px solid var(--pink-200); border-radius: 999px;
  font-size: 13px; background: var(--pink-50); font-family: inherit; color: var(--ink);
}
.inv-search input:focus { outline: none; border-color: var(--pink-500); background: #fff; box-shadow: 0 0 0 3px var(--pink-100); }
.inv-search__icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--pink-500); pointer-events: none; }

.inv-select {
  height: 34px; padding: 0 30px 0 12px; border-radius: 999px;
  border: 1px solid var(--pink-200); background: var(--pink-50); color: var(--ink);
  font-size: 13px; font-family: inherit; cursor: pointer; appearance: none;
  background-image: linear-gradient(45deg, transparent 50%, var(--muted) 50%),
                    linear-gradient(135deg, var(--muted) 50%, transparent 50%);
  background-position: calc(100% - 15px) 14px, calc(100% - 10px) 14px;
  background-size: 5px 5px, 5px 5px; background-repeat: no-repeat;
}
.inv-select:focus { outline: none; border-color: var(--pink-500); }

.inv-chip {
  background: var(--pink-600); color: #fff; border-radius: 999px;
  padding: 0 6px; font-size: 11px; line-height: 17px; min-width: 17px; text-align: center;
}
.inv-caret { transition: transform .2s; }
.inv-caret.is-open { transform: rotate(180deg); }

/* ══════════ BỘ LỌC (nằm trong card) ══════════ */
.inv-filter { display: grid; grid-template-rows: 0fr; transition: grid-template-rows .22s ease; }
.inv-filter.is-open { grid-template-rows: 1fr; }
.inv-filter__panel {
  overflow: hidden; background: var(--pink-50);
  padding: 0 16px; transition: padding .22s ease;
}
.inv-filter.is-open .inv-filter__panel { padding: 14px 16px; }
.inv-filter__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.inv-filter__foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--line);
}
.inv-filter__count { font-size: 12.5px; color: var(--muted); }
.inv-filter__btns { display: flex; gap: 8px; }

/* ══════════ Ô NHẬP DÙNG CHUNG ══════════ */
.inv-field { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.inv-field > span { font-size: 12px; font-weight: 700; color: var(--pink-700); }
.inv-field input, .inv-field select, .inv-item-row input, .inv-solo-input, .inv-serial-grid input {
  width: 100%; padding: 9px 11px;
  border: 1px solid var(--field); border-radius: 9px;
  font-size: 13px; color: var(--ink); background: #fff; font-family: inherit;
  transition: border-color .15s, box-shadow .15s;
}
.inv-field input:focus, .inv-field select:focus, .inv-item-row input:focus, .inv-solo-input:focus, .inv-serial-grid input:focus {
  outline: none; border-color: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100);
}
.inv-readonly {
  padding: 9px 11px; border: 1px solid var(--line); border-radius: 9px;
  font-size: 13px; color: var(--muted); background: var(--pink-50);
}
.inv-hint { font-size: 11.5px; color: var(--muted); font-style: normal; margin-top: 2px; }
.inv-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.inv-picked { font-size: 13px; color: var(--muted); background: var(--pink-50); border-radius: 10px; padding: 8px 12px; margin-bottom: 14px; }
.inv-alert { background: #fef2f2; color: var(--danger); border-radius: 10px; padding: 8px 12px; font-size: 13px; margin-bottom: 12px; }
.inv-section-title {
  display: flex; align-items: center; gap: 6px;
  text-transform: uppercase; font-weight: 700; font-size: 11.5px; letter-spacing: .06em;
  color: var(--pink-700); margin-bottom: 8px;
}
.inv-item-head { display: flex; gap: 8px; margin-bottom: 4px; font-size: 12px; font-weight: 700; color: var(--pink-700); }
.inv-item-row { display: flex; gap: 8px; align-items: center; }
.inv-item-block { border: 1px solid var(--line); border-radius: 10px; padding: 8px; background: var(--pink-50); }
.inv-serial-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 6px;
  margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--pink-200);
}
.inv-serial-grid input { font-size: 12.5px; padding: 6px 8px; }
.inv-serial-grid input:disabled { background: var(--pink-100); color: var(--muted); cursor: not-allowed; }
.inv-serial-chip-row { display: flex; flex-wrap: wrap; gap: 5px; }
.inv-total { text-align: right; font-weight: 800; font-size: 1.05rem; color: var(--ink); }

/* ══════════ THẺ + BẢNG ══════════ */
.inv-card { background: #fff; border: 1px solid var(--line); border-radius: 14px; overflow: hidden; box-shadow: var(--sh-2); }

/* THANH CÔNG CỤ (nằm trong card, có border-bottom) */
.inv-bar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 12px 16px; background: #fff;
  border-bottom: 1px solid var(--pink-50);
}
.inv-bar__count { font-size: 12.5px; color: var(--muted); font-weight: 600; }
.inv-search { position: relative; flex: 1 1 280px; max-width: 320px; }

.inv-table-wrap { overflow-x: auto; }
.inv-table { width: 100%; border-collapse: collapse; }
.inv-table th {
  background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 800; text-align: left; text-transform: uppercase; letter-spacing: .4px;
  padding: 11px 12px; white-space: nowrap; border-bottom: none;
}
.inv-table thead th:first-child { border-top-left-radius: 13px; }
.inv-table thead th:last-child { border-top-right-radius: 13px; }
.inv-table td { padding: 11px 12px; border-bottom: 1px solid var(--line); vertical-align: middle; }
.inv-table tbody tr:last-child td { border-bottom: none; }
.inv-row { cursor: pointer; transition: background-color .12s; }
.inv-row:hover { background: var(--pink-50); }

.inv-code { color: var(--pink-700); font-weight: 700; }
.inv-price { font-weight: 700; font-variant-numeric: tabular-nums; }
.inv-ton { font-weight: 700; font-variant-numeric: tabular-nums; }
.inv-held { font-weight: 600; font-variant-numeric: tabular-nums; }
.text-warning { color: #d97706; }
.text-danger { color: #dc2626; }
.text-success { color: #059669; }
.text-info { color: #2563eb; }

.inv-thumb { width: 36px; height: 36px; object-fit: cover; border-radius: 9px; border: 1px solid var(--line); background: #fff; flex-shrink: 0; }
.inv-name { display: flex; align-items: center; gap: 10px; min-width: 0; }
.inv-name__text { min-width: 0; }
.inv-name__main { font-weight: 600; line-height: 1.35; word-break: break-word; }
.inv-name__sub { font-size: 11.5px; color: var(--muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; margin-top: 2px; }
.inv-config { display: flex; flex-wrap: wrap; gap: 6px; font-size: 12px; }
.inv-config span { padding: 2px 6px; border-radius: 4px; }
.inv-config-diff { font-weight: 600; color: #3b82f6; background: #eff6ff; }

.inv-tag { display: inline-flex; align-items: center; gap: 5px; padding: 2px 9px; border-radius: 999px; font-size: 11.5px; font-weight: 700; white-space: nowrap; }
.inv-tag--ok { background: var(--ok-bg); color: var(--ok-text); }
.inv-tag--low { background: #fff7ed; color: #c2650a; }
.inv-tag--out { background: #fef2f2; color: var(--danger); }
.inv-tag--soft { background: var(--pink-100); color: var(--pink-700); }

.inv-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.inv-empty { padding: 40px 20px; text-align: center; color: var(--muted); font-size: 13.5px; }

.inv-pager { display: flex; justify-content: flex-end; padding: 10px 16px; background: var(--pink-50); border-top: 1px solid var(--line); }

.inv-chips { display: flex; flex-wrap: wrap; gap: 8px; padding: 14px 20px; border-bottom: 1px solid var(--line); }
.inv-chip-info {
  display: inline-flex; align-items: center; gap: 6px;
  background: var(--pink-50); border-radius: 999px; padding: 4px 12px;
  font-size: 12.5px; color: var(--muted);
}
.inv-chip-info b { color: var(--ink); font-weight: 700; }

/* ══════════ MODAL ══════════ */
.inv-modal-mask {
  position: fixed; inset: 0; z-index: 1050;
  background: rgba(31,41,55,.5); display: flex; align-items: flex-start; justify-content: center;
  padding: 5vh 20px 20px; font-size: 14px; color: var(--ink);
}
.inv-modal {
  background: #fff; width: 640px; max-width: 100%; max-height: 90vh;
  border-radius: 16px; display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 22px 55px rgba(168,27,93,.25);
}
.inv-modal--hep { width: 520px; }
.inv-modal__head {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 16px 20px 12px; background: var(--pink-50); border-bottom: 1px solid var(--line);
}
.inv-modal__icon {
  width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--pink-100); color: var(--pink-700);
}
.inv-modal__body { padding: 20px; overflow-y: auto; background: #fffafc; }
.inv-modal__foot {
  display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 14px 20px; border-top: 1px solid var(--line); background: var(--pink-50);
}
.inv-modal__foot--end { justify-content: flex-end; }
</style>