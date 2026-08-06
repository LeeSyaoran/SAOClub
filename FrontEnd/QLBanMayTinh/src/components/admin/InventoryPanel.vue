<script setup>
import { ref, computed, reactive, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import {
  CheckCircle2, XCircle, Clock, Package, ClipboardList, BarChart3, AlertTriangle,
  Ban, Laptop, Search, Pencil, Printer, Download, Plus, Check, X, Trash2,
  Building2, User, Calendar, FileText, FolderOpen,
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
import { InventoryStore, ensureInventory } from "../../stores/inventory.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
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

const inventory = computed(() => InventoryStore.items ?? []);
const products = computed(() => ProductsStore.items ?? []);
const suppliers = computed(() => SuppliersStore.items ?? []);
const staff = computed(() => StaffStore.items ?? []);

// ── Tab noi bo: Ton kho | Phieu nhap kho ──────────────────────────────────────
const khoTab = ref('ton-kho'); // 'ton-kho' | 'phieu-nhap'

const lowStockItems = computed(() =>
  inventory.value.filter(
    (t) => t.soLuongTon != null && t.tonKhoToiThieu != null && t.soLuongTon <= t.tonKhoToiThieu,
  ),
);
const outOfStockItems = computed(() =>
  inventory.value.filter(t => (t.soLuongTon ?? 0) === 0),
);
// "Sắp hết" (khác Hết hàng): còn hàng nhưng <= tối thiểu
const lowStockOnlyItems = computed(() =>
  inventory.value.filter(t => (t.soLuongTon ?? 0) > 0 && t.tonKhoToiThieu != null && t.soLuongTon <= t.tonKhoToiThieu),
);
const totalStockQty = computed(() => inventory.value.reduce((s, i) => s + (i.soLuongTon || 0), 0));

// ── Inventory grouped by product ──────────────────────────────────────────────
const inventorySearch = ref('');
const inventoryStatusFilter = ref('all'); // all | out | low | ok
const expandedGroups = ref({});
const toggleGroup = (name) => { expandedGroups.value[name] = !expandedGroups.value[name]; };
const allGroupsExpanded = computed(() =>
  inventoryGrouped.value.length > 0 && inventoryGrouped.value.every(g => expandedGroups.value[g.name]),
);
const toggleAllGroups = () => {
  const next = !allGroupsExpanded.value;
  inventoryGrouped.value.forEach(g => { expandedGroups.value[g.name] = next; });
};

const inventoryGrouped = computed(() => {
  const groups = {};
  for (const item of inventory.value) {
    const name = item.bienThe?.sanPham?.tenSanPham || '—';
    if (!groups[name]) groups[name] = [];
    groups[name].push(item);
  }
  return Object.entries(groups)
    .filter(([name]) => !inventorySearch.value || name.toLowerCase().includes(inventorySearch.value.toLowerCase()))
    .map(([name, items]) => {
      const p = products.value.find(p => p.tenSanPham === name);
      const totalTon = items.reduce((s, i) => s + (i.soLuongTon || 0), 0);
      const outCount = items.filter(i => (i.soLuongTon ?? 0) === 0).length;
      const lowCount = items.filter(i => i.soLuongTon != null && i.tonKhoToiThieu != null && i.soLuongTon > 0 && i.soLuongTon <= i.tonKhoToiThieu).length;
      return { name, items, hinhAnh: p?.hinhAnhChinh, thuongHieu: p?.thuongHieu, totalTon, outCount, lowCount };
    })
    .filter(g => {
      if (inventoryStatusFilter.value === 'out') return g.outCount > 0;
      if (inventoryStatusFilter.value === 'low') return g.lowCount > 0;
      if (inventoryStatusFilter.value === 'ok') return g.outCount === 0 && g.lowCount === 0;
      return true;
    });
});
const { currentPage: invCurrentPage, totalPages: invTotalPages, pagedItems: pagedInventoryGrouped } = usePagination(inventoryGrouped);

const getVariantInfo = (item) => products.value.find(p => p.bienTheId === item.bienThe?.bienTheId);
const stockClass = (item) => {
  if ((item.soLuongTon ?? 0) === 0) return 'text-danger';
  if (item.soLuongTon != null && item.tonKhoToiThieu != null && item.soLuongTon <= item.tonKhoToiThieu) return 'text-warning';
  return 'text-success';
};

// ── Stock Modal (them serial / sua giu hang, ton kho toi thieu) ──────────────
const showStockModal = ref(false);
const editingStock = ref(null);
const stockSaving = ref(false);
// soLuongTon KHÔNG sửa tay được nữa — chỉ tăng khi nhập serial mới (xem newSerials),
// khớp đúng thực tế: mỗi máy nhập kho phải có 1 serial, số lượng = số serial đang "trong_kho".
const stockForm = reactive({ soLuongGiu: 0, tonKhoToiThieu: 0, newSerials: [''] });

const openEditStock = (item) => {
  editingStock.value = item;
  stockForm.soLuongGiu = item.soLuongGiu ?? 0;
  stockForm.tonKhoToiThieu = item.tonKhoToiThieu ?? 0;
  stockForm.newSerials = [''];
  showStockModal.value = true;
};
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
const saveStock = async () => {
  if (stockSaving.value) return;
  stockSaving.value = true;
  const item = editingStock.value;
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
    showStockModal.value = false;
    // Lấy lại đúng dòng vừa đổi để có soLuongTon mới nhất do server tính.
    const updated = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    const idx = inventory.value.findIndex((i) => i.tonKhoId === item.tonKhoId);
    if (idx !== -1 && updated) inventory.value[idx] = updated;
  } catch (e) {
    showToast(e.message);
  } finally {
    stockSaving.value = false;
  }
};

// ── Stock Detail Modal (serial numbers) ──────────────────────────────────────
const showStockDetailModal = ref(false);
const stockDetailItem      = ref(null);   // tonKho item
const stockDetailSerials   = ref([]);
const stockDetailLoading   = ref(false);
const stockDetailNewSerial = ref('');
const stockDetailSaving    = ref(false);

const openStockDetail = async (item) => {
  stockDetailItem.value    = item;
  stockDetailSerials.value = [];
  stockDetailNewSerial.value = '';
  showStockDetailModal.value = true;
  stockDetailLoading.value   = true;
  const bienTheId = item.bienThe?.bienTheId;
  if (bienTheId) {
    stockDetailSerials.value = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
  }
  stockDetailLoading.value = false;
};

const addStockSerial = async () => {
  if (!stockDetailNewSerial.value.trim()) return;
  const bienTheId = stockDetailItem.value?.bienThe?.bienTheId;
  if (!bienTheId) return;
  stockDetailSaving.value = true;
  try {
    const res = await ChiTietSanPhamService.create({
      bienTheId,
      soSerial: stockDetailNewSerial.value.trim(),
      trangThai: 'trong_kho',
      ngayNhapKho: nowLocalIso(),
    });
    if (!res.ok) throw new Error(t('admin.errors.addSerialError'));
    stockDetailSerials.value = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
    // Chỉ lấy lại đúng 1 dòng tồn kho vừa đổi (server tự tính lại soLuongTon
    // từ số serial), khỏi phải tải cả bảng tồn kho.
    const updatedStock = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    if (updatedStock) {
      const idx = inventory.value.findIndex((i) => i.tonKhoId === updatedStock.tonKhoId);
      if (idx !== -1) inventory.value[idx] = updatedStock;
    }
    stockDetailNewSerial.value = '';
  } catch(e) { showToast(e.message); }
  finally { stockDetailSaving.value = false; }
};

// Xóa serial thêm nhầm — chỉ cho phép khi đang "trong_kho" (server chặn nếu đã bán/đã dùng).
const removeStockSerial = async (chiTietId) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const bienTheId = stockDetailItem.value?.bienThe?.bienTheId;
  try {
    const res = await ChiTietSanPhamService.remove(chiTietId);
    if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteSerialError'))); return; }
    stockDetailSerials.value = stockDetailSerials.value.filter((s) => s.chiTietId !== chiTietId);
    const updatedStock = await TonKhoService.getByBienThe(bienTheId).catch(() => null);
    if (updatedStock) {
      const idx = inventory.value.findIndex((i) => i.tonKhoId === updatedStock.tonKhoId);
      if (idx !== -1) inventory.value[idx] = updatedStock;
    }
  } catch (e) { showToast(e.message); }
};

const stockDetailStatusLabel = (s) => t(`admin.statusLabel.${s}`);
const stockDetailStatusColor = (s) => {
  if (s === 'trong_kho')    return '#22c55e';
  if (s === 'giu_hang')     return '#facc15';
  if (s === 'da_ban')       return '#94a3b8';
  if (s === 'loi_bao_hanh') return '#fb923c';
  if (s === 'da_tra_hang')  return '#38bdf8';
  return '#6b7280';
};

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
const emptyPhieuNhapForm = () => {
  const now = new Date();
  const local = nowLocalIso(now).slice(0, 16);
  return {
    nhaCungCapId: '',
    nhanVienId: '',
    ngayNhap: local,
    ghiChu: '',
    items: [{ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 }],
  };
};
const phieuNhapForm = reactive(emptyPhieuNhapForm());
const phieuNhapItemsTotal = computed(() =>
  phieuNhapForm.items.reduce((s, i) => s + (Number(i.soLuong) || 0) * (Number(i.donGia) || 0), 0),
);
const addPhieuNhapItemRow = () => phieuNhapForm.items.push({ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 });
// HTML min="1" chỉ chặn nút spinner, gõ tay vẫn nhập được số âm/0 — kẹp lại ngay lúc nhập
// (backend cũng đã chặn ở ChiTietPhieuNhapRequest, kẹp ở đây để báo sai ngay thay vì đợi lưu).
const clampPhieuNhapSoLuong = (row) => { row.soLuong = Math.max(1, Math.trunc(Number(row.soLuong)) || 1); };
const removePhieuNhapItemRow = (idx) => {
  if (phieuNhapForm.items.length > 1) {
    phieuNhapForm.items.splice(idx, 1);
  } else {
    // Chỉ còn 1 dòng — không xóa hẳn (form sẽ trống hoàn toàn), reset về giá trị rỗng.
    phieuNhapForm.items[idx] = { sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 };
  }
};
const editingPhieuNhapId = ref(null);
const openAddPhieuNhap = () => {
  editingPhieuNhapId.value = null;
  Object.assign(phieuNhapForm, emptyPhieuNhapForm());
  phieuNhapFormError.value = '';
  showPhieuNhapModal.value = true;
};
// Chỉ sửa được khi còn "cho_duyet" — đã duyệt/hủy thì coi như chốt sổ, sửa lại sẽ sai đối
// chiếu với NCC. Nạp lại đúng dữ liệu đang có: header + từng dòng chi tiết (kèm id để
// savePhieuNhap() biết dòng nào update, dòng nào tạo mới/xóa khi lưu).
const openEditPhieuNhap = (p) => {
  editingPhieuNhapId.value = p.phieuNhapId;
  const bienTheToSanPham = new Map(products.value.map(pp => [pp.bienTheId, pp.sanPhamId]));
  const items = chiTietPhieuNhapList.value
    .filter(c => c.phieuNhapId === p.phieuNhapId)
    .map(c => ({
      id: c.id,
      sanPhamId: bienTheToSanPham.get(c.bienTheId) ?? '',
      bienTheId: c.bienTheId,
      soLuong: c.soLuong,
      donGia: c.donGiaNhap,
    }));
  Object.assign(phieuNhapForm, {
    nhaCungCapId: p.nhaCungCapId,
    nhanVienId: p.nhanVienId,
    ngayNhap: (p.ngayNhap || '').slice(0, 16),
    ghiChu: p.ghiChu === '—' ? '' : (p.ghiChu || ''),
    items: items.length ? items : [{ sanPhamId: '', bienTheId: '', soLuong: 1, donGia: 0 }],
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
    if (editingPhieuNhapId.value) {
      // Đối chiếu dòng cũ/mới: id có sẵn -> update, không có id -> tạo mới,
      // dòng cũ không còn trong form -> xóa.
      const phieuNhapId = editingPhieuNhapId.value;
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
      for (const i of items) {
        await ChiTietPhieuNhapService.create({
          phieuNhapId: created.phieuNhapId,
          bienTheId: Number(i.bienTheId),
          soLuong: Number(i.soLuong) || 0,
          donGiaNhap: Number(i.donGia) || 0,
        });
      }
    }
    // API tạo trả về entity lồng nhau (nhaCungCap/nhanVien object) khác format phẳng của
    // getAll() (PhieuNhapKhoResponse) — tải lại danh sách thay vì tự ráp để tránh lệch dữ liệu.
    [phieuNhapList.value, chiTietPhieuNhapList.value] = await Promise.all([
      PhieuNhapKhoService.getAll().catch(() => phieuNhapList.value),
      ChiTietPhieuNhapService.getAll().catch(() => chiTietPhieuNhapList.value),
    ]);
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
const openPhieuNhapDetail = (p) => {
  phieuNhapDetailData.value = p;
  showPhieuNhapDetailModal.value = true;
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
  <div>
    <!-- Tabs -->
    <div class="d-flex gap-2 mb-3">
      <button class="btn btn-sm fw-bold" :class="khoTab==='ton-kho' ? 'btn-warning text-dark' : 'btn-outline-secondary'"
              @click="khoTab='ton-kho'"><Package :size="15" style="vertical-align:-2px;" /> {{ t('admin.inventory.tabStock') }}</button>
      <button class="btn btn-sm fw-bold" :class="khoTab==='phieu-nhap' ? 'btn-warning text-dark' : 'btn-outline-secondary'"
              @click="khoTab='phieu-nhap'; ensurePhieuNhapData()"><ClipboardList :size="15" style="vertical-align:-2px;" /> {{ t('admin.inventory.tabReceipts') }}</button>
    </div>

    <!-- ══ TAB: TON KHO ══ -->
    <template v-if="khoTab==='ton-kho'">
    <div class="row g-3 mb-3">
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(96,165,250,0.15);"><Package :size="20" color="#60a5fa" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.inventory.statTotalSku') }}</div>
              <div class="fw-bold" style="font-size:1.55rem;">{{ inventory.length }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(52,211,153,0.15);"><BarChart3 :size="20" color="#34d399" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.inventory.statTotalStock') }}</div>
              <div class="fw-bold" style="font-size:1.55rem;">{{ totalStockQty }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(250,204,21,0.15);"><AlertTriangle :size="20" color="#facc15" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.inventory.statLowStock') }}</div>
              <div class="fw-bold" :style="lowStockOnlyItems.length?{color:'#facc15'}:{}" style="font-size:1.55rem;">{{ lowStockOnlyItems.length }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(244,63,94,0.15);"><Ban :size="20" color="var(--accent-fg)" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.inventory.statOutOfStock') }}</div>
              <div class="fw-bold" :style="outOfStockItems.length?{color:'#f87171'}:{}" style="font-size:1.55rem;">{{ outOfStockItems.length }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Summary + Search -->
    <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
      <span class="text-secondary small">{{ t('admin.inventory.summary', { groups: inventoryGrouped.length, skus: inventory.length }) }}</span>
      <span v-if="outOfStockItems.length" class="badge d-inline-flex align-items-center gap-1" style="background:rgba(244,63,94,0.15);color:#f87171;"><Ban :size="12" /> {{ outOfStockItems.length }} {{ t('admin.inventory.outOfStock') }}</span>
      <span v-if="lowStockItems.length" class="badge d-inline-flex align-items-center gap-1" style="background:rgba(250,204,21,0.15);color:#facc15;"><AlertTriangle :size="12" /> {{ lowStockItems.length }} {{ t('admin.inventory.lowStock') }}</span>
      <button class="btn btn-sm btn-outline-info" style="font-size:0.78rem;padding:2px 10px;" @click="toggleAllGroups">
        {{ allGroupsExpanded ? '▲ ' + t('admin.inventory.collapseAll') : '▼ ' + t('admin.inventory.expandAll') }}
      </button>
      <select v-model="inventoryStatusFilter" class="form-select form-select-sm" style="width:auto;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.8rem;">
        <option value="all">{{ t('admin.inventory.filterAll') }}</option>
        <option value="out">{{ t('admin.inventory.filterOut') }}</option>
        <option value="low">{{ t('admin.inventory.filterLow') }}</option>
        <option value="ok">{{ t('admin.inventory.filterOk') }}</option>
      </select>
      <div class="ms-auto" style="min-width:200px;">
        <input v-model="inventorySearch" class="form-control form-control-sm"
               :placeholder="t('admin.inventory.searchPlaceholder')"
               style="background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;" />
      </div>
    </div>

    <div v-if="InventoryStore.loading" class="text-secondary small py-4 text-center">{{ t('admin.inventory.loading') }}</div>
    <div v-else class="d-flex flex-column gap-2">

      <div v-for="group in pagedInventoryGrouped" :key="group.name"
           class="rounded-3 overflow-hidden"
           style="background:var(--bg-card);border:1px solid var(--border-color);">

        <!-- Product header -->
        <div class="d-flex align-items-center px-3 py-2 gap-3"
             style="cursor:pointer;transition:background 0.15s;"
             @mouseenter="$event.currentTarget.style.background='var(--bg-hover)'"
             @mouseleave="$event.currentTarget.style.background=''"
             @click="toggleGroup(group.name)">
          <img v-if="group.hinhAnh" :src="group.hinhAnh"
               style="width:44px;height:36px;object-fit:contain;border-radius:4px;background:var(--bg-card-inset);flex-shrink:0;" />
          <div v-else style="width:44px;height:36px;background:var(--bg-input);border-radius:4px;flex-shrink:0;display:flex;align-items:center;justify-content:center;"><Laptop :size="16" color="var(--text-muted)" /></div>
          <div class="flex-grow-1 min-width-0">
            <div class="fw-semibold" style="font-size:0.88rem;color:var(--text-heading);">{{ group.name }}</div>
            <div class="text-secondary" style="font-size:0.72rem;">
              {{ group.thuongHieu ? group.thuongHieu + ' · ' : '' }}{{ group.items.length }} {{ t('admin.inventory.totalStockLabel') }} <strong :class="group.totalTon===0?'text-danger':group.totalTon<5?'text-warning':'text-success'">{{ group.totalTon }}</strong>
            </div>
          </div>
          <div class="d-flex align-items-center gap-2">
            <span v-if="group.outCount" class="badge d-inline-flex align-items-center gap-1" style="font-size:0.7rem;background:rgba(244,63,94,0.15);color:#f87171;"><Ban :size="11" /> {{ group.outCount }} {{ t('admin.inventory.outOfStock') }}</span>
            <span v-else-if="group.lowCount" class="badge d-inline-flex align-items-center gap-1" style="font-size:0.7rem;background:rgba(250,204,21,0.15);color:#facc15;"><AlertTriangle :size="11" /> {{ group.lowCount }} {{ t('admin.inventory.lowStock') }}</span>
            <span v-else class="badge d-inline-flex align-items-center gap-1" style="font-size:0.7rem;background:rgba(34,197,94,0.15);color:#22c55e;"><CheckCircle2 :size="11" /> {{ t('admin.inventory.ok') }}</span>
            <span class="text-secondary" style="font-size:0.75rem;width:12px;text-align:center;">{{ expandedGroups[group.name] ? '▲' : '▼' }}</span>
          </div>
        </div>

        <!-- Variant detail table -->
        <div v-if="expandedGroups[group.name]" style="border-top:1px solid var(--border-color-soft);">
          <table class="w-100" style="border-collapse:collapse;font-size:0.8rem;">
            <thead>
              <tr style="background:var(--bg-input);">
                <th class="px-3 py-2 text-secondary" style="font-weight:500;width:22%;">{{ t('admin.inventory.colSku') }}</th>
                <th class="px-3 py-2 text-secondary" style="font-weight:500;">{{ t('admin.inventory.colConfig') }}</th>
                <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:80px;">{{ t('admin.inventory.colStock') }}</th>
                <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:70px;">{{ t('admin.inventory.colHeld') }}</th>
                <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:80px;">{{ t('admin.inventory.colMinStock') }}</th>
                <th class="px-3 py-2 text-secondary" style="font-weight:500;width:90px;"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in group.items" :key="item.tonKhoId"
                  style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-2 text-secondary" style="font-family:monospace;font-size:0.73rem;">{{ item.bienThe?.maSku || '—' }}</td>
                <td class="px-3 py-2">
                  <div class="d-flex gap-1 flex-wrap">
                    <span v-if="getVariantInfo(item)?.cpu" class="badge" style="background:#2a2a3a;color:#aab;font-size:0.68rem;">{{ getVariantInfo(item).cpu }}</span>
                    <span v-if="getVariantInfo(item)?.ram" class="badge" style="background:#2a3a2a;color:#aba;font-size:0.68rem;">{{ getVariantInfo(item).ram }}</span>
                    <span v-if="getVariantInfo(item)?.oCung" class="badge" style="background:#3a2a2a;color:#baa;font-size:0.68rem;">{{ getVariantInfo(item).oCung }}</span>
                    <span v-if="getVariantInfo(item)?.mauSac" class="badge" style="background:#2a2a2a;color:#999;font-size:0.68rem;">{{ getVariantInfo(item).mauSac }}</span>
                  </div>
                </td>
                <td class="px-3 py-2 text-center">
                  <span :class="stockClass(item)" class="fw-bold" style="font-size:0.88rem;">{{ item.soLuongTon ?? '—' }}</span>
                </td>
                <td class="px-3 py-2 text-center text-secondary">{{ item.soLuongGiu ?? 0 }}</td>
                <td class="px-3 py-2 text-center text-secondary">{{ item.tonKhoToiThieu ?? '—' }}</td>
                <td class="px-3 py-2">
                  <div class="d-flex gap-1">
                    <button class="btn btn-sm btn-outline-info"
                            style="font-size:0.72rem;padding:2px 8px;"
                            @click.stop="openStockDetail(item)"><Search :size="13" style="vertical-align:-2px;" /> {{ t('admin.inventory.detail') }}</button>
                    <button class="btn btn-sm btn-outline-warning"
                            style="font-size:0.72rem;padding:2px 8px;"
                            @click.stop="openEditStock(item)"><Pencil :size="13" style="vertical-align:-2px;" /> {{ t('admin.inventory.update') }}</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="inventoryGrouped.length === 0" class="text-secondary small text-center py-5">{{ t('admin.inventory.empty') }}</div>
      <Pagination :current-page="invCurrentPage" :total-pages="invTotalPages" @page-change="invCurrentPage = $event" />
    </div>
    </template>

    <!-- ══ TAB: PHIEU NHAP ══ -->
    <template v-else-if="khoTab==='phieu-nhap'">
    <div class="row g-3 mb-3">
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(167,139,250,0.15);"><ClipboardList :size="20" color="#a78bfa" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statTotal') }}</div>
              <div class="fw-bold" style="font-size:1.55rem;">{{ phieuNhapCounts.total }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(250,204,21,0.15);"><Clock :size="20" color="#facc15" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statPending') }}</div>
              <div class="fw-bold" :style="phieuNhapCounts.choDuyet?{color:'#facc15'}:{}" style="font-size:1.55rem;">{{ phieuNhapCounts.choDuyet }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(34,197,94,0.15);"><CheckCircle2 :size="20" color="#22c55e" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statDone') }}</div>
              <div class="fw-bold" style="font-size:1.55rem;color:#22c55e;">{{ phieuNhapCounts.hoanThanh }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
                 style="width:44px;height:44px;background:rgba(244,63,94,0.15);"><XCircle :size="20" color="var(--accent-fg)" /></div>
            <div>
              <div class="text-secondary small mb-1">{{ t('admin.phieuNhap.statCancelled') }}</div>
              <div class="fw-bold" :style="phieuNhapCounts.huy?{color:'#f87171'}:{}" style="font-size:1.55rem;">{{ phieuNhapCounts.huy }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
      <input v-model="phieuNhapSearch" class="form-control form-control-sm" style="max-width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;"
             :placeholder="t('admin.phieuNhap.searchPlaceholder')" />
      <select v-model="phieuNhapStatusFilter" class="form-select form-select-sm" style="width:auto;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.8rem;">
        <option value="">{{ t('admin.inventory.filterAll') }}</option>
        <option value="cho_duyet">{{ t('admin.statusLabel.cho_duyet') }}</option>
        <option value="hoan_thanh">{{ t('admin.statusLabel.hoan_thanh') }}</option>
        <option value="huy">{{ t('admin.statusLabel.huy') }}</option>
      </select>
      <div class="ms-auto d-flex gap-2">
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapList"><Printer :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.printPdf') }}</button>
        <button class="btn btn-sm btn-outline-success" @click="exportPhieuNhapExcel"><Download :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.exportExcel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPhieuNhap"><Plus :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.add') }}</button>
      </div>
    </div>

    <div class="table-responsive">
      <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
        <thead><tr>
          <th style="width:40px;">{{ t('admin.common.stt') }}</th>
          <th>{{ t('admin.phieuNhap.colCode') }}</th>
          <th>{{ t('admin.phieuNhap.colDate') }}</th>
          <th>{{ t('admin.phieuNhap.colSupplier') }}</th>
          <th>{{ t('admin.phieuNhap.colStaff') }}</th>
          <th>{{ t('admin.phieuNhap.colTotal') }}</th>
          <th>{{ t('admin.phieuNhap.colStatus') }}</th>
          <th>{{ t('admin.phieuNhap.colAction') }}</th>
        </tr></thead>
        <tbody>
          <tr v-for="(p, idx) in pagedPhieuNhap" :key="p.phieuNhapId">
            <td class="text-secondary">{{ pnCurrentPage * pnPageSize + idx + 1 }}</td>
            <td class="text-secondary" style="font-family:monospace;">{{ p.maPhieuNhap }}</td>
            <td>{{ formatDate(p.ngayNhap) }}</td>
            <td>{{ supplierName(p.nhaCungCapId) }}</td>
            <td>{{ staffName(p.nhanVienId) }}</td>
            <td>{{ formatPrice(p.tongTien) }}</td>
            <td>
              <span class="badge" :style="{ background: phieuNhapStatusColor(p.trangThai).bg, color: phieuNhapStatusColor(p.trangThai).text }">
                <component :is="phieuNhapStatusIcon(p.trangThai)" :size="13" /> {{ statusLabel(p.trangThai) }}
              </span>
            </td>
            <td>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-info" style="font-size:0.72rem;padding:2px 8px;" @click="openPhieuNhapDetail(p)"><Search :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.viewDetail') }}</button>
                <template v-if="p.trangThai==='cho_duyet'">
                  <button class="btn btn-sm btn-outline-success" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'hoan_thanh')"><Check :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.approve') }}</button>
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="updatePhieuNhapStatus(p,'huy')"><X :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.cancel') }}</button>
                  <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openEditPhieuNhap(p)"><Pencil :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.editAction') }}</button>
                  <button class="btn btn-sm btn-outline-danger" style="font-size:0.72rem;padding:2px 8px;" @click="deletePhieuNhap(p.phieuNhapId)"><Trash2 :size="12" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.deleteAction') }}</button>
                </template>
              </div>
            </td>
          </tr>
          <tr v-if="filteredPhieuNhap.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.phieuNhap.empty') }}</td></tr>
        </tbody>
      </table>
      <Pagination :current-page="pnCurrentPage" :total-pages="pnTotalPages" @page-change="pnCurrentPage = $event" />
    </div>
    </template>
  </div>

  <!-- ══ MODAL TAO PHIEU NHAP ══ -->
  <div v-if="showPhieuNhapModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showPhieuNhapModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:860px;max-width:96vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingPhieuNhapId ? t('admin.phieuNhapModal.titleEdit') : t('admin.phieuNhapModal.title') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showPhieuNhapModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="phieuNhapFormError" class="alert alert-danger small py-2 mb-3">{{ phieuNhapFormError }}</div>
        <div class="row g-3 mb-3">
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.supplierLabel') }}</label>
            <SearchSelect v-model="phieuNhapForm.nhaCungCapId" :options="supplierOptions" :placeholder="t('admin.phieuNhapModal.selectPlaceholder')" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.staffLabel') }}</label>
            <SearchSelect v-model="phieuNhapForm.nhanVienId" :options="staffOptions" :placeholder="t('admin.phieuNhapModal.selectPlaceholder')" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.dateLabel') }}</label>
            <input v-model="phieuNhapForm.ngayNhap" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.phieuNhapModal.noteLabel') }}</label>
            <input v-model="phieuNhapForm.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
        </div>

        <div class="fw-semibold small text-secondary mb-2 d-flex align-items-center gap-1"><Package :size="14" /> {{ t('admin.phieuNhapModal.itemsLabel') }}</div>
        <div class="d-flex gap-2 mb-1">
          <label class="form-label small text-secondary mb-0" style="flex:2 1 0;">{{ t('admin.phieuNhapModal.colProduct') }}</label>
          <label class="form-label small text-secondary mb-0" style="flex:2 1 0;">{{ t('admin.phieuNhapModal.colVariant') }}</label>
          <label class="form-label small text-secondary mb-0" style="flex:0 0 80px;">{{ t('admin.phieuNhapModal.colQty') }}</label>
          <label class="form-label small text-secondary mb-0" style="flex:0 0 110px;">{{ t('admin.phieuNhapModal.colPrice') }}</label>
          <span style="flex:0 0 34px;"></span>
        </div>
        <div class="d-flex flex-column gap-2 mb-2">
          <div v-for="(row, idx) in phieuNhapForm.items" :key="idx" class="d-flex gap-2 align-items-center">
            <div style="flex:2 1 0;min-width:0;">
              <SearchSelect v-model="row.sanPhamId" @update:model-value="row.bienTheId=''"
                            :options="productOptionsForPhieuNhap"
                            :placeholder="t('admin.phieuNhapModal.selectProductPlaceholder')" />
            </div>
            <div style="flex:2 1 0;min-width:0;">
              <SearchSelect v-model="row.bienTheId" :disabled="!row.sanPhamId"
                            :options="variantsForProduct(row.sanPhamId)"
                            :placeholder="t('admin.phieuNhapModal.selectVariantPlaceholder')" />
            </div>
            <input v-model="row.soLuong" type="number" min="1" class="form-control form-control-sm" style="flex:0 0 80px;background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.phieuNhapModal.qtyPlaceholder')" @change="clampPhieuNhapSoLuong(row)" />
            <input v-model="row.donGia" type="number" min="0" class="form-control form-control-sm" style="flex:0 0 110px;background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.phieuNhapModal.unitPricePlaceholder')" />
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;flex:0 0 34px;" :aria-label="t('common.remove')" @click="removePhieuNhapItemRow(idx)"><X :size="14" /></button>
          </div>
        </div>
        <button class="btn btn-sm btn-outline-warning mb-3" @click="addPhieuNhapItemRow">{{ t('admin.phieuNhapModal.addRow') }}</button>

        <div class="d-flex justify-content-end fw-bold" style="font-size:1.05rem;">
          {{ t('admin.phieuNhapModal.totalLabel') }} {{ formatPrice(phieuNhapItemsTotal) }}
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showPhieuNhapModal=false">{{ t('admin.phieuNhapModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="phieuNhapSaving" @click="savePhieuNhap">{{ editingPhieuNhapId ? t('admin.phieuNhapModal.saveEdit') : t('admin.phieuNhapModal.save') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET PHIEU NHAP ══ -->
  <div v-if="showPhieuNhapDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showPhieuNhapDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:680px;max-width:96vw;max-height:90vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid var(--border-color-soft);" v-if="phieuNhapDetailData">
        <div class="d-flex align-items-center gap-3">
          <div class="rounded-3 d-flex align-items-center justify-content-center flex-shrink-0"
               style="width:40px;height:40px;background:rgba(167,139,250,0.15);"><ClipboardList :size="18" color="#a78bfa" /></div>
          <div>
            <div class="fw-bold" style="font-size:0.95rem;color:var(--text-heading);">
              {{ t('admin.phieuNhapDetailModal.title') }}
              <span class="text-secondary ms-1" style="font-size:0.8rem;font-family:monospace;">{{ phieuNhapDetailData.maPhieuNhap }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.78rem;">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }} · {{ formatDate(phieuNhapDetailData.ngayNhap) }}</div>
          </div>
        </div>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showPhieuNhapDetailModal=false"></button>
      </div>

      <div class="overflow-y-auto flex-grow-1" v-if="phieuNhapDetailData">
        <!-- Info chips -->
        <div class="d-flex flex-wrap gap-2 p-3" style="border-bottom:1px solid var(--border-color-soft);">
          <span class="d-flex align-items-center gap-1 rounded-pill px-3 py-1 small" style="background:var(--bg-card-alt);">
            <Building2 :size="13" style="vertical-align:-2px;" /> <span class="text-secondary">{{ t('admin.phieuNhap.colSupplier') }}:</span> <span class="text-light fw-semibold">{{ supplierName(phieuNhapDetailData.nhaCungCapId) }}</span>
          </span>
          <span class="d-flex align-items-center gap-1 rounded-pill px-3 py-1 small" style="background:var(--bg-card-alt);">
            <User :size="13" style="vertical-align:-2px;" /> <span class="text-secondary">{{ t('admin.phieuNhap.colStaff') }}:</span> <span class="text-light fw-semibold">{{ staffName(phieuNhapDetailData.nhanVienId) }}</span>
          </span>
          <span class="d-flex align-items-center gap-1 rounded-pill px-3 py-1 small" style="background:var(--bg-card-alt);">
            <Calendar :size="13" style="vertical-align:-2px;" /> <span class="text-secondary">{{ t('admin.phieuNhap.colDate') }}:</span> <span class="text-light fw-semibold">{{ formatDate(phieuNhapDetailData.ngayNhap) }}</span>
          </span>
          <span class="badge d-flex align-items-center" :style="{ background: phieuNhapStatusColor(phieuNhapDetailData.trangThai).bg, color: phieuNhapStatusColor(phieuNhapDetailData.trangThai).text }">
            <component :is="phieuNhapStatusIcon(phieuNhapDetailData.trangThai)" :size="13" /> {{ statusLabel(phieuNhapDetailData.trangThai) }}
          </span>
          <div v-if="phieuNhapDetailData.ghiChu" class="w-100 text-secondary small fst-italic d-flex align-items-center gap-1" style="padding-left:2px;"><FileText :size="12" /> {{ phieuNhapDetailData.ghiChu }}</div>
        </div>

        <!-- Danh sach hang -->
        <div class="p-3">
          <table class="w-100 mb-0" style="border-collapse:collapse;font-size:0.82rem;">
            <thead>
              <tr style="background:var(--bg-input);">
                <th class="px-3 py-2 text-secondary" style="font-weight:600;">{{ t('admin.inventory.colSku') }}</th>
                <th class="px-3 py-2 text-secondary text-center" style="font-weight:600;width:80px;">{{ t('admin.phieuNhapModal.qtyPlaceholder') }}</th>
                <th class="px-3 py-2 text-secondary text-center" style="font-weight:600;width:110px;">{{ t('admin.phieuNhapModal.actualStockLabel') }}</th>
                <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:130px;">{{ t('admin.phieuNhapModal.unitPricePlaceholder') }}</th>
                <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:140px;">{{ t('admin.phieuNhapModal.totalLabel') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in phieuNhapDetailItems" :key="c.id" style="border-top:1px solid var(--border-color-soft);">
                <td class="px-3 py-2 text-secondary" style="font-family:monospace;">{{ c.maSku }}</td>
                <td class="px-3 py-2 text-center fw-bold" style="color:var(--text-heading);">{{ c.soLuong }}</td>
                <td class="px-3 py-2 text-center">
                  <span :class="tonThucTeCuaBienThe(c.bienTheId) < c.soLuong ? 'text-warning' : 'text-success'" :title="t('admin.phieuNhapModal.actualStockHint')">
                    {{ tonThucTeCuaBienThe(c.bienTheId) }}
                  </span>
                </td>
                <td class="px-3 py-2 text-end text-secondary">{{ formatPrice(c.donGiaNhap) }}</td>
                <td class="px-3 py-2 text-end fw-semibold" style="color:var(--accent-fg);">{{ formatPrice(c.thanhTien) }}</td>
              </tr>
              <tr v-if="phieuNhapDetailItems.length===0"><td colspan="5" class="text-center text-secondary py-4">{{ t('admin.phieuNhap.empty') }}</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Footer: tong ket -->
      <div v-if="phieuNhapDetailData" class="px-4 py-3 d-flex justify-content-between align-items-center" style="border-top:1px solid var(--border-color-soft);background:var(--bg-card-alt);">
        <span class="text-secondary small">{{ phieuNhapDetailItems.length }} {{ t('admin.inventory.colSku') }}</span>
        <div class="d-flex align-items-center gap-2">
          <span class="text-secondary small">{{ t('admin.phieuNhapModal.totalLabel') }}</span>
          <span class="fw-bold" style="font-size:1.15rem;color:var(--accent-fg);">{{ formatPrice(phieuNhapDetailData.tongTien) }}</span>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 pt-0" v-if="phieuNhapDetailData">
        <button class="btn btn-sm btn-outline-danger" @click="printPhieuNhapDetail(phieuNhapDetailData)"><Printer :size="14" style="vertical-align:-2px;" /> {{ t('admin.phieuNhap.printPdf') }}</button>
        <button class="btn btn-sm btn-outline-secondary" @click="showPhieuNhapDetailModal=false">{{ t('admin.promoModal.cancel') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL TON KHO ══ -->
  <div v-if="showStockModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showStockModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.stockModal.title') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="showStockModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="editingStock" class="small p-2 rounded-2 mb-3 text-secondary" style="background:var(--bg-hover);">
          {{ editingStock.bienThe?.sanPham?.tenSanPham??'—' }} — {{ t('admin.stockModal.skuLabel') }} <strong>{{ editingStock.bienThe?.maSku??'—' }}</strong>
        </div>
        <div class="row g-3">
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.stockModal.stockLabel') }}</label>
            <div class="form-control form-control-sm d-flex align-items-center" style="background:var(--bg-hover); color:var(--text-secondary); border-color:var(--border-color-strong)">{{ editingStock?.soLuongTon ?? 0 }}</div>
            <div class="text-secondary" style="font-size:0.72rem;">{{ t('admin.stockModal.stockHint') }}</div>
          </div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.stockModal.heldLabel') }}</label><input v-model="stockForm.soLuongGiu" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-12"><label class="form-label small text-secondary">{{ t('admin.stockModal.minStockLabel') }}</label><input v-model="stockForm.tonKhoToiThieu" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-12">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <label class="form-label small text-secondary mb-0">{{ t('admin.stockModal.newSerialsLabel') }}</label>
              <label class="btn btn-sm btn-outline-info" style="padding:2px 10px;font-size:0.72rem;cursor:pointer;">
                <FolderOpen :size="14" style="vertical-align:-2px;" /> {{ t('admin.stockModal.importFromFile') }}
                <input type="file" accept=".csv,.txt,.xlsx,.xls" class="d-none" @change="importSerialsFromFile" />
              </label>
            </div>
            <div class="d-flex flex-column gap-2">
              <div v-for="(s, idx) in stockForm.newSerials" :key="idx" class="d-flex gap-2 align-items-center">
                <input v-model="stockForm.newSerials[idx]" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.stockModal.serialPlaceholder')" />
                <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" :aria-label="t('common.remove')" @click="removeStockSerialRow(idx)"><X :size="14" /></button>
              </div>
            </div>
            <button class="btn btn-sm btn-outline-warning mt-2" @click="addStockSerialRow">{{ t('admin.stockModal.addSerialRow') }}</button>
            <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t('admin.stockModal.importHint') }}</div>
          </div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showStockModal=false">{{ t('admin.stockModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="stockSaving" @click="saveStock">{{ t('admin.stockModal.save') }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET SERIAL ══ -->
  <div v-if="showStockDetailModal"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:var(--bg-overlay);z-index:1060;"
       @click.self="showStockDetailModal=false">
    <div class="rounded-4 d-flex flex-column"
         style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:760px;max-width:96vw;max-height:88vh;">

      <!-- Header -->
      <div class="d-flex align-items-start justify-content-between px-4 py-3" style="border-bottom:1px solid var(--border-color-soft);">
        <div>
          <div class="fw-bold" style="font-size:0.95rem;color:var(--text-heading);">
            {{ t('admin.stockDetailModal.titlePrefix') }} {{ stockDetailItem?.bienThe?.maSku || '—' }}
          </div>
          <div class="d-flex gap-1 mt-1 flex-wrap">
            <span v-if="getVariantInfo(stockDetailItem)?.cpu" class="badge" style="background:#2a2a3a;color:#aab;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).cpu }}</span>
            <span v-if="getVariantInfo(stockDetailItem)?.ram" class="badge" style="background:#2a3a2a;color:#aba;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).ram }}</span>
            <span v-if="getVariantInfo(stockDetailItem)?.oCung" class="badge" style="background:#3a2a2a;color:#baa;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).oCung }}</span>
            <span v-if="getVariantInfo(stockDetailItem)?.mauSac" class="badge" style="background:#2a2a2a;color:#999;font-size:0.7rem;">{{ getVariantInfo(stockDetailItem).mauSac }}</span>
          </div>
        </div>
        <div class="d-flex align-items-center gap-3">
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#22c55e;flex-shrink:0;"></span>
              <span class="fw-bold" style="font-size:1.2rem;color:var(--text-heading);">{{ stockDetailSerials.filter(s=>s.trangThai==='trong_kho').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">{{ t('admin.stockDetailModal.inStock') }}</div>
          </div>
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#94a3b8;flex-shrink:0;"></span>
              <span class="fw-bold" style="font-size:1.2rem;color:var(--text-heading);">{{ stockDetailSerials.filter(s=>s.trangThai==='da_ban').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">{{ t('admin.stockDetailModal.sold') }}</div>
          </div>
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#fb923c;flex-shrink:0;"></span>
              <span class="fw-bold" style="font-size:1.2rem;color:var(--text-heading);">{{ stockDetailSerials.filter(s=>s.trangThai==='loi_bao_hanh').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">{{ t('admin.stockDetailModal.warranty') }}</div>
          </div>
          <button class="btn-close btn-sm ms-2" :aria-label="t('common.close')" @click="showStockDetailModal=false"></button>
        </div>
      </div>

      <!-- Serial list -->
      <div class="overflow-y-auto flex-grow-1">
        <div v-if="stockDetailLoading" class="text-secondary small text-center py-5">{{ t('admin.stockDetailModal.loading') }}</div>
        <div v-else-if="stockDetailSerials.length === 0" class="text-secondary small text-center py-5">{{ t('admin.stockDetailModal.empty') }}</div>
        <table v-else class="w-100" style="border-collapse:collapse;font-size:0.82rem;">
          <thead>
            <tr style="background:var(--bg-input);position:sticky;top:0;">
              <th class="px-4 py-2 text-secondary" style="font-weight:500;width:40px;">{{ t('admin.stockDetailModal.colIndex') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">{{ t('admin.stockDetailModal.colSerial') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">{{ t('admin.stockDetailModal.colImportDate') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">{{ t('admin.stockDetailModal.colStatus') }}</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;width:60px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(s, idx) in stockDetailSerials" :key="s.chiTietId"
                style="border-top:1px solid var(--bg-input);">
              <td class="px-4 py-2 text-secondary">{{ idx + 1 }}</td>
              <td class="px-4 py-2 fw-semibold" style="font-family:monospace;color:var(--text-heading);">{{ s.soSerial }}</td>
              <td class="px-4 py-2 text-secondary">{{ formatDate(s.ngayNhapKho) }}</td>
              <td class="px-4 py-2">
                <span
                  style="display:inline-block;width:10px;height:10px;border-radius:50%;"
                  :style="{ background: stockDetailStatusColor(s.trangThai) }"
                  :title="stockDetailStatusLabel(s.trangThai)"
                ></span>
              </td>
              <td class="px-4 py-2">
                <button v-if="s.trangThai==='trong_kho'" class="btn btn-sm btn-outline-danger" style="padding:1px 7px;font-size:0.72rem;" :title="t('admin.stockDetailModal.deleteSerial')" :aria-label="t('admin.stockDetailModal.deleteSerial')" @click="removeStockSerial(s.chiTietId)"><X :size="11" /></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* Bootstrap .text-light hardcode mau trang co dinh — ghi de theo theme hien tai (dung
   trong modal Chi tiet phieu nhap, tren nen the/card, khong phai nen mau thuong hieu co
   dinh, nen an toan khi ghi de theo bien theme). Chuyen tu AdminPage.vue (Task 7) — class
   scoped theo component, khong ke thua tu style block cua component cha. */
.text-light {
  color: var(--text-primary) !important;
}
</style>
