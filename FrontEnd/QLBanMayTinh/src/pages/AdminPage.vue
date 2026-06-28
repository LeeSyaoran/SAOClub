<script setup>
import { ref, computed, onMounted, onUnmounted, reactive } from "vue";
import { AuthStore, clearSession } from "../stores/index.js";
import * as SanPhamService   from "../Service/SanPhamService.js";
import * as KhachHangService from "../Service/KhachHangService.js";
import * as NhanVienService  from "../Service/NhanVienService.js";
import * as DonHangService   from "../Service/DonHangService.js";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";
import * as TonKhoService          from "../Service/TonKhoService.js";
import * as DanhMucService         from "../Service/DanhMucService.js";
import * as DmService              from "../Service/DmService.js";
import * as ChiTietSanPhamService  from "../Service/ChiTietSanPhamService.js";
import * as ChiTietDonHangService  from "../Service/ChiTietDonHangService.js";

// ── Navigation ───────────────────────────────────────────────────────────────
const currentRole = computed(() => {
  return AuthStore.user?.role || "khach_hang";
});
const PAGE_META = {
  dashboard: { title: "Dashboard", sub: "Tong quan he thong" },
  products: { title: "San pham", sub: "Quan ly danh sach san pham" },
  orders: { title: "Don hang", sub: "Quan ly don hang" },
  customers: { title: "Khach hang", sub: "Quan ly khach hang" },
  inventory: { title: "Kho hang", sub: "Quan ly ton kho" },
  promotions: { title: "Khuyen mai", sub: "Quan ly chuong trinh khuyen mai" },
  staff: { title: "Nhan vien", sub: "Quan ly nhan vien" },
  "ban-hang": { title: "Ban hang", sub: "Ban hang tai quay (POS)" },
  reports: { title: "Bao cao", sub: "Bao cao & thong ke" },
  settings: { title: "Cai dat", sub: "Cai dat he thong" },
  "user-home": { title: "Trang chu", sub: "Chao mung ban quay lai" },
  "user-orders": { title: "Don hang cua toi", sub: "Theo doi don hang" },
  "user-browse": { title: "Mua sam", sub: "Kham pha san pham" },
  "user-warranty": { title: "Bao hanh", sub: "Quan ly bao hanh" },
  "user-profile": { title: "Ho so", sub: "Thong tin tai khoan" },
};
const topbarTitle = computed(
  () => PAGE_META[currentPage.value]?.title ?? "Dashboard",
);
const topbarSub = computed(() => PAGE_META[currentPage.value]?.sub ?? "");
// ================= Navigation =================
const currentPage = ref(
    currentRole.value === "admin"
        ? "dashboard"
        : currentRole.value === "nhan_vien"
            ? "ban-hang"
            : "user-home"
);

const navigate = (page) => {

  // ADMIN
  if (
      currentRole.value === "admin" &&
      [
        "dashboard",
        "products",
        "orders",
        "customers",
        "inventory",
        "promotions",
        "ban-hang",
        "staff",
        "reports",
        "settings"
      ].includes(page)
  ) {
    currentPage.value = page;
    return;
  }

  // NHÂN VIÊN
  if (
      currentRole.value === "nhan_vien" &&
      [
        "dashboard",
        "ban-hang",
        "orders",
        "customers",
        "products"
      ].includes(page)
  ) {
    currentPage.value = page;
    return;
  } {
    currentPage.value = page;
    return;
  }

};
// ── User ─────────────────────────────────────────────────────────────────────
const userDisplayName = computed(() => AuthStore.user?.hoTen ?? AuthStore.user?.username ?? "Admin");
const userAvatar = computed(() => userDisplayName.value.charAt(0).toUpperCase());
const userDisplayRole = computed(() => {
  const role = AuthStore.user?.role;
  if (role === "admin")     return "Quản trị viên";
  if (role === "nhan_vien") return "Nhân viên";
  if (role === "quan_kho")  return "Quản lý kho";
  return "Người dùng";
});

const logout = () => {
  clearSession();
  window.location.hash = '';
};

// ── Data refs ─────────────────────────────────────────────────────────────────
const products = ref([]);
const orders = ref([]);
const customers = ref([]);
const staff = ref([]);
const promotions = ref([]);
const inventory = ref([]);
const categories = ref([]);
const brands = ref([]);
const suppliers = ref([]);
const chucVuList = ref([]);
const cpuList = ref([]);
const ramList = ref([]);
const oCungList = ref([]);
const gpuList = ref([]);
const loading = ref(false);

// ── Helpers ───────────────────────────────────────────────────────────────────
const statusLabel = (s) =>
  ({
    active: "Hoat dong",
    inactive: "Ngung ban",
    ngung_kin_doanh: "Ngung kinh doanh",
  })[s] ?? s;

const formatPrice = (v) =>
  v == null
    ? "—"
    : new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(v);

const formatDate = (d) => {
  if (!d) return "—";
  try {
    return new Date(d).toLocaleDateString("vi-VN");
  } catch {
    return d;
  }
};

const toLocalDT = (s) =>
  s ? (s.length === 16 ? s + ":00" : s.slice(0, 19)) : null;

const customerName = (id) =>
  customers.value.find((c) => c.khachHangId === id)?.hoTen ?? `KH#${id}`;
const chucVuName = (id) =>
  chucVuList.value.find((c) => c.id === id)?.tenChucVu ?? "—";

// ── Dashboard stats ───────────────────────────────────────────────────────────
const totalProducts = computed(() => products.value.length);
const totalOrders = computed(() => orders.value.length);
const totalCustomers = computed(() => customers.value.length);
const totalRevenue = computed(() =>
  orders.value.reduce((s, o) => s + (Number(o.thanhTien) || 0), 0),
);

// ── Reports stats ─────────────────────────────────────────────────────────────
const ordersByStatus = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    map[o.trangThaiDonHang] = (map[o.trangThaiDonHang] || 0) + 1;
  });
  return Object.entries(map).map(([k, v]) => ({ status: k, count: v }));
});
const activeProducts = computed(
  () => products.value.filter((p) => p.trangThai === "active").length,
);
const activePromos = computed(() => {
  const now = new Date();
  return promotions.value.filter(
    (p) => p.trangThai === "active" && new Date(p.ngayKetThuc) > now,
  ).length;
});
const lowStockItems = computed(() =>
  inventory.value.filter(
    (t) => t.soLuongTon != null && t.tonKhoToiThieu != null && t.soLuongTon <= t.tonKhoToiThieu,
  ),
);
const outOfStockItems = computed(() =>
  inventory.value.filter(t => (t.soLuongTon ?? 0) === 0),
);

// ── Inventory grouped by product ──────────────────────────────────────────────
const inventorySearch = ref('');
const expandedGroups = ref({});
const toggleGroup = (name) => { expandedGroups.value[name] = !expandedGroups.value[name]; };

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
    });
});

const getVariantInfo = (item) => products.value.find(p => p.bienTheId === item.bienThe?.bienTheId);
const stockClass = (item) => {
  if ((item.soLuongTon ?? 0) === 0) return 'text-danger';
  if (item.soLuongTon != null && item.tonKhoToiThieu != null && item.soLuongTon <= item.tonKhoToiThieu) return 'text-warning';
  return 'text-success';
};

// ── Fetch ─────────────────────────────────────────────────────────────────────
const fetchAll = async () => {
  loading.value = true;
  const safe = (p) => p.catch(() => []);
  [
    products.value,
    orders.value,
    customers.value,
    staff.value,
    promotions.value,
    inventory.value,
    categories.value,
    brands.value,
    suppliers.value,
    chucVuList.value,
    cpuList.value,
    ramList.value,
    oCungList.value,
    gpuList.value,
  ] = await Promise.all([
    safe(SanPhamService.getAll()),
    safe(DonHangService.getAll()),
    safe(KhachHangService.getAll()),
    safe(NhanVienService.getAll()),
    safe(KhuyenMaiService.getAll()),
    safe(TonKhoService.getAll()),
    safe(DanhMucService.getAll()),
    safe(DmService.getThuongHieu()),
    safe(DmService.getNhaCungCap()),
    safe(DmService.getChucVu()),
    safe(DmService.getCpu()),
    safe(DmService.getRam()),
    safe(DmService.getOCung()),
    safe(DmService.getGpu()),
  ]);
  loading.value = false;
  await autoMergeAllDuplicates();
};

// Tự động gộp tất cả đơn cùng khách + cùng ngày khi tải trang
const autoMergeAllDuplicates = async () => {
  const groups = {};
  for (const o of orders.value) {
    const key = `${o.khachHangId}_${o.ngayDat?.slice(0, 10)}`;
    if (!groups[key]) groups[key] = [];
    groups[key].push(o);
  }
  const toMerge = Object.values(groups).filter(g => g.length > 1);
  if (toMerge.length === 0) return;
  for (const group of toMerge) {
    group.sort((a, b) => a.donHangId - b.donHangId);
    const targetId = group[0].donHangId;
    const sourceIds = group.slice(1).map(o => o.donHangId);
    await DonHangService.merge(targetId, sourceIds);
  }
  orders.value = await DonHangService.getAll().catch(() => []);
};

// ── Products: gộp theo sanPhamId cho bảng ─────────────────────────────────────
const groupedProducts = computed(() => {
  const map = new Map();
  products.value.forEach(p => {
    if (!map.has(p.sanPhamId)) {
      map.set(p.sanPhamId, { ...p, variantCount: 1, minPrice: Number(p.giaBan) });
    } else {
      const ex = map.get(p.sanPhamId);
      ex.variantCount++;
      if (Number(p.giaBan) < ex.minPrice) ex.minPrice = Number(p.giaBan);
    }
  });
  return [...map.values()];
});

// ── Variant detail modal ───────────────────────────────────────────────────────
const showVariantModal   = ref(false);
const variantModalName   = ref('');
const variantModalList   = ref([]);
const variantSerialMap   = ref({});   // bienTheId → serial[]
const variantSerialLoad  = ref(false);
const serialInputs       = ref({});   // bienTheId → { soSerial, saving }

const showDetailModal  = ref(false);
const detailModalName  = ref('');
const detailModalList  = ref([]);
const detailSerialMap  = ref({});  // bienTheId → serial[]

// Helper: fetch serial của nhiều bienTheId song song → { bienTheId: serial[] }
const fetchSerialMap = async (bienTheIds) => {
  const results = await Promise.all(
    bienTheIds.map(id => ChiTietSanPhamService.getByBienThe(id).catch(() => []))
  );
  const map = {};
  bienTheIds.forEach((id, i) => { map[id] = results[i]; });
  return map;
};

const openDetail = async (sanPhamId, name) => {
  detailModalName.value = name;
  const list = products.value.filter(p => p.sanPhamId === sanPhamId);
  detailModalList.value = list;
  detailSerialMap.value = {};
  showDetailModal.value = true;
  detailSerialMap.value = await fetchSerialMap(list.map(v => v.bienTheId));
};

const openVariants = async (sanPhamId, name) => {
  variantModalName.value = name;
  const list = products.value.filter(p => p.sanPhamId === sanPhamId);
  variantModalList.value = list;
  variantSerialMap.value = {};
  const inputs = {};
  list.forEach(v => { inputs[v.bienTheId] = { soSerial: '', saving: false }; });
  serialInputs.value = inputs;
  showVariantModal.value = true;

  variantSerialLoad.value = true;
  variantSerialMap.value = await fetchSerialMap(list.map(v => v.bienTheId));
  variantSerialLoad.value = false;
};

const addSerial = async (bienTheId) => {
  const inp = serialInputs.value[bienTheId];
  if (!inp?.soSerial?.trim()) return;
  inp.saving = true;
  try {
    const res = await ChiTietSanPhamService.create({
      bienTheId,
      soSerial: inp.soSerial.trim(),
      trangThai: 'trong_kho',
      ngayNhapKho: new Date().toISOString().slice(0, 19),
    });
    if (!res.ok) throw new Error('Lỗi thêm serial');
    // Chỉ refresh serial của biến thể vừa thêm
    const updated = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
    variantSerialMap.value = { ...variantSerialMap.value, [bienTheId]: updated };
    inp.soSerial = '';
  } finally {
    inp.saving = false;
  }
};

// ── Products CRUD ─────────────────────────────────────────────────────────────
const showProductModal = ref(false);
const editingId = ref(null);
const formError = ref("");

// Serial number cho lần tạo mới
const soSerialMoi = ref('');
// Preview ảnh + file thực tế chờ upload
const imagePreview  = ref('');
const imageFilePending = ref(null);

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

const resetImageState = () => {
  imagePreview.value = '';
  imageFilePending.value = null;
};

const openAdd = () => {
  Object.assign(form, emptyForm());
  editingId.value = null;
  formError.value = "";
  soSerialMoi.value = '';
  resetImageState();
  showProductModal.value = true;
};
const openEdit = (p) => {
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
    oCungId:
      oCungList.value.find((o) => o.loaiOcung === p.oCung)?.oCungId ?? null,
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
  formError.value = "";
  imagePreview.value = p.hinhAnhChinh || '';
  imageFilePending.value = null;
  showProductModal.value = true;
};

const handleImageFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  imageFilePending.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const saveProduct = async () => {
  formError.value = "";

  // Upload ảnh trước nếu có file mới chọn
  if (imageFilePending.value) {
    const fd = new FormData();
    fd.append('file', imageFilePending.value);
    try {
      const upRes = await fetch('/api/upload/image', { method: 'POST', body: fd });
      if (upRes.ok) {
        const upData = await upRes.json();
        form.hinhAnhChinh = upData.url;
      } else {
        formError.value = `Upload anh that bai: ${upRes.status}`;
        return;
      }
    } catch (e) {
      formError.value = `Upload anh loi: ${e.message}`;
      return;
    }
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
    ...(editingId.value ? {} : { ngayTao: new Date().toISOString().slice(0, 19) }),
  };
  try {
    const res = await SanPhamService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = `Loi: ${res.status} ${await res.text()}`;
      return;
    }

    // Nếu tạo mới + có serial → tìm bienTheId vừa tạo rồi POST chi_tiet
    if (!editingId.value && soSerialMoi.value.trim()) {
      const newList = await SanPhamService.getAll().catch(() => []);
      const newVariant = [...newList].reverse().find(p => p.maSku === form.maSku);
      if (newVariant) {
        await ChiTietSanPhamService.create({
          bienTheId: newVariant.bienTheId,
          soSerial: soSerialMoi.value.trim(),
          trangThai: 'trong_kho',
          ngayNhapKho: new Date().toISOString().slice(0, 19),
        }).catch(() => {});
      }
    }

    showProductModal.value = false;
    resetImageState();
    products.value = await SanPhamService.getAll().catch(() => []);
  } catch (e) {
    formError.value = e.message;
  }
};
const deleteProduct = async (id) => {
  if (!confirm("Ban co chac muon xoa san pham nay?")) return;
  const res = await SanPhamService.remove(id);
  if (!res.ok) { alert(`Xoa that bai: ${res.status}`); return; }
  products.value = await SanPhamService.getAll().catch(() => []);
};

// ── Customers CRUD ────────────────────────────────────────────────────────────
const showCustomerModal = ref(false);
const editingCustomerId = ref(null);
const customerFormError = ref("");
const emptyCustomerForm = () => ({
  hoTen: "",
  soDienThoai: "",
  email: "",
  diaChi: "",
  loaiKhach: "ca_nhan",
  tenCongTy: "",
  maSoThue: "",
  diemTichLuy: 0,
  trangThai: "active",
});
const customerForm = reactive(emptyCustomerForm());

const openAddCustomer = () => {
  Object.assign(customerForm, emptyCustomerForm());
  editingCustomerId.value = null;
  customerFormError.value = "";
  showCustomerModal.value = true;
};
const openEditCustomer = (c) => {
  Object.assign(customerForm, {
    hoTen: c.hoTen,
    soDienThoai: c.soDienThoai,
    email: c.email ?? "",
    diaChi: c.diaChi ?? "",
    loaiKhach: c.loaiKhach ?? "ca_nhan",
    tenCongTy: c.tenCongTy ?? "",
    maSoThue: c.maSoThue ?? "",
    diemTichLuy: c.diemTichLuy ?? 0,
    trangThai: c.trangThai ?? "active",
  });
  editingCustomerId.value = c.khachHangId;
  customerFormError.value = "";
  showCustomerModal.value = true;
};
const saveCustomer = async () => {
  customerFormError.value = "";
  const body = {
    ...customerForm,
    diemTichLuy: Number(customerForm.diemTichLuy),
  };
  try {
    const res = await KhachHangService.save(editingCustomerId.value, body);
    if (!res.ok) {
      customerFormError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showCustomerModal.value = false;
    customers.value = await KhachHangService.getAll().catch(() => []);
  } catch (e) {
    customerFormError.value = e.message;
  }
};
const deleteCustomer = async (id) => {
  if (!confirm("Xoa khach hang nay?")) return;
  const res = await KhachHangService.remove(id);
  if (!res.ok) { alert(`Xoa that bai: ${res.status}`); return; }
  customers.value = await KhachHangService.getAll().catch(() => []);
};

// ── Staff CRUD ────────────────────────────────────────────────────────────────
const showStaffModal = ref(false);
const editingStaffId = ref(null);
const staffFormError = ref("");
const emptyStaffForm = () => ({
  hoTen: "",
  soDienThoai: "",
  email: "",
  chucVuId: null,
  username: "",
  matKhauHash: "",
  luongCoBan: 0,
  trangThai: "active",
});
const staffForm = reactive(emptyStaffForm());

const openAddStaff = () => {
  Object.assign(staffForm, emptyStaffForm());
  editingStaffId.value = null;
  staffFormError.value = "";
  showStaffModal.value = true;
};
const openEditStaff = (s) => {
  Object.assign(staffForm, {
    hoTen: s.hoTen,
    soDienThoai: s.soDienThoai,
    email: s.email ?? "",
    chucVuId: s.chucVuId,
    username: s.username ?? "",
    matKhauHash: "",
    luongCoBan: s.luongCoBan ?? 0,
    trangThai: s.trangThai ?? "active",
  });
  editingStaffId.value = s.nhanVienId;
  staffFormError.value = "";
  showStaffModal.value = true;
};
const saveStaff = async () => {
  staffFormError.value = "";
  const body = {
    ...staffForm,
    chucVuId: Number(staffForm.chucVuId),
    luongCoBan: Number(staffForm.luongCoBan),
  };
  try {
    const res = await NhanVienService.save(editingStaffId.value, body);
    if (!res.ok) {
      staffFormError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showStaffModal.value = false;
    staff.value = await NhanVienService.getAll().catch(() => []);
  } catch (e) {
    staffFormError.value = e.message;
  }
};
const deleteStaff = async (id) => {
  if (!confirm("Xoa nhan vien nay?")) return;
  const res = await NhanVienService.remove(id);
  if (!res.ok) { alert(`Xoa that bai: ${res.status}`); return; }
  staff.value = await NhanVienService.getAll().catch(() => []);
};

// ── Promotions CRUD ───────────────────────────────────────────────────────────
const showPromoModal = ref(false);
const editingPromoId = ref(null);
const promoFormError = ref("");
const emptyPromoForm = () => ({
  maKhuyenMai: "",
  tenKhuyenMai: "",
  loai: "percent",
  giaTri: "",
  giaTriToiDa: "",
  donHangToiThieu: "",
  ngayBatDau: "",
  ngayKetThuc: "",
  soLuongToiDa: "",
  trangThai: "active",
});
const promoForm = reactive(emptyPromoForm());

const openAddPromo = () => {
  Object.assign(promoForm, emptyPromoForm());
  editingPromoId.value = null;
  promoFormError.value = "";
  showPromoModal.value = true;
};
const openEditPromo = (p) => {
  const dt = (d) => (d ? d.slice(0, 16) : "");
  Object.assign(promoForm, {
    maKhuyenMai: p.maKhuyenMai,
    tenKhuyenMai: p.tenKhuyenMai,
    loai: p.loai ?? "percent",
    giaTri: p.giaTri ?? "",
    giaTriToiDa: p.giaTriToiDa ?? "",
    donHangToiThieu: p.donHangToiThieu ?? "",
    ngayBatDau: dt(p.ngayBatDau),
    ngayKetThuc: dt(p.ngayKetThuc),
    soLuongToiDa: p.soLuongToiDa ?? "",
    trangThai: p.trangThai ?? "active",
  });
  editingPromoId.value = p.khuyenMaiId;
  promoFormError.value = "";
  showPromoModal.value = true;
};
const savePromo = async () => {
  promoFormError.value = "";
  const body = {
    ...promoForm,
    giaTri: promoForm.giaTri ? Number(promoForm.giaTri) : null,
    giaTriToiDa: promoForm.giaTriToiDa ? Number(promoForm.giaTriToiDa) : null,
    donHangToiThieu: promoForm.donHangToiThieu
      ? Number(promoForm.donHangToiThieu)
      : null,
    soLuongToiDa: promoForm.soLuongToiDa
      ? Number(promoForm.soLuongToiDa)
      : null,
    ngayBatDau: toLocalDT(promoForm.ngayBatDau),
    ngayKetThuc: toLocalDT(promoForm.ngayKetThuc),
  };
  try {
    const res = await KhuyenMaiService.save(editingPromoId.value, body);
    if (!res.ok) {
      promoFormError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showPromoModal.value = false;
    promotions.value = await KhuyenMaiService.getAll().catch(() => []);
  } catch (e) {
    promoFormError.value = e.message;
  }
};
const deletePromo = async (id) => {
  if (!confirm("Xoa khuyen mai nay?")) return;
  const res = await KhuyenMaiService.remove(id);
  if (!res.ok) { alert(`Xoa that bai: ${res.status}`); return; }
  promotions.value = await KhuyenMaiService.getAll().catch(() => []);
};

// ── Orders CRUD ───────────────────────────────────────────────────────────────
const deleteOrder = async (id) => {
  if (!confirm('Ban co chac muon xoa don hang nay? Hanh dong nay khong the hoan tac.')) return;
  const res = await DonHangService.remove(id);
  if (!res.ok) { alert(`Xoa that bai: ${res.status}`); return; }
  orders.value = await DonHangService.getAll().catch(() => []);
};

// ── Order detail modal (xem san pham trong don) ───────────────────────────────
const showOrderDetailModal = ref(false);
const orderDetailData      = ref(null);   // don hang dang xem
const orderDetailItems     = ref([]);     // ChiTietDonHangResponse[]
const orderDetailLoading   = ref(false);

const openOrderDetail = async (o) => {
  orderDetailData.value  = o;
  orderDetailItems.value = [];
  showOrderDetailModal.value = true;
  orderDetailLoading.value = true;
  try {
    orderDetailItems.value = await ChiTietDonHangService.getByDonHang(o.donHangId).catch(() => []);
  } finally {
    orderDetailLoading.value = false;
  }
};

// Tim ten san pham tu bienTheId trong danh sach products da load
const productByBienThe = (bienTheId) => products.value.find(p => p.bienTheId === bienTheId);

// ── Them san pham vao don ─────────────────────────────────────────────────────
const addItemMode           = ref(false);
const addItemBienTheId      = ref('');
const addItemQty            = ref(1);
const addItemLoading        = ref(false);
const addItemSearch         = ref('');
const addItemSelectedSpId   = ref(null);  // sanPhamId dang mo xem bien the

// Modal chi tiet san pham (khi click vao card)
const showAddItemDetailModal  = ref(false);
const addItemDetailGroup      = ref(null);   // group dang xem { sanPhamId, tenSanPham, ... variants[] }
const addItemSelectedConfig   = ref(null);   // cpu+ram+oCung key
const addItemSelectedColor    = ref(null);   // mauSac

// Cac phien ban doc nhat (cpu + ram + oCung) cho san pham dang xem
const addItemConfigs = computed(() => {
  if (!addItemDetailGroup.value) return [];
  const seen = new Set();
  const result = [];
  for (const v of addItemDetailGroup.value.variants) {
    const key = [v.cpu, v.ram, v.oCung].filter(Boolean).join('|');
    if (!seen.has(key)) { seen.add(key); result.push({ key, cpu: v.cpu, ram: v.ram, oCung: v.oCung }); }
  }
  return result;
});

// Cac mau sac trong phien ban dang chon
const addItemColorsForConfig = computed(() => {
  if (!addItemDetailGroup.value || !addItemSelectedConfig.value) return [];
  const [cpu, ram, oCung] = addItemSelectedConfig.value.split('|');
  return addItemDetailGroup.value.variants.filter(v =>
    (v.cpu || '') === (cpu || '') &&
    (v.ram || '') === (ram || '') &&
    (v.oCung || '') === (oCung || '')
  );
});

// Bien the hien tai dua vao config + mau sac dang chon
const addItemCurrentVariant = computed(() =>
  addItemColorsForConfig.value.find(v => v.mauSac === addItemSelectedColor.value) ||
  addItemColorsForConfig.value[0] || null
);

const openAddItemDetail = (group) => {
  addItemDetailGroup.value = group;
  addItemSelectedConfig.value = addItemConfigs.value[0]?.key ?? null;
  addItemSelectedColor.value  = addItemColorsForConfig.value[0]?.mauSac ?? null;
  showAddItemDetailModal.value = true;
};

// Khi chon config moi → reset color ve first of that config
const selectConfig = (key) => {
  addItemSelectedConfig.value = key;
  const [cpu, ram, oCung] = key.split('|');
  const first = addItemDetailGroup.value?.variants.find(v =>
    (v.cpu || '') === (cpu || '') && (v.ram || '') === (ram || '') && (v.oCung || '') === (oCung || '')
  );
  addItemSelectedColor.value = first?.mauSac ?? null;
};

const confirmAddFromDetail = async () => {
  const v = addItemCurrentVariant.value;
  if (!v) return;
  addItemLoading.value = true;
  showAddItemDetailModal.value = false;
  try {
    const res = await DonHangService.addChiTiet({
      donHangId:   orderDetailData.value.donHangId,
      bienTheId:   v.bienTheId,
      soLuong:     addItemQty.value,
      donGia:      v.giaBan,
      giamGiaDong: 0,
    });
    if (!res.ok) { alert(`Them that bai: ${res.status}`); return; }
    await DonHangService.recalculate(orderDetailData.value.donHangId);
    await refreshOrderDetail();
    addItemQty.value = 1;
  } finally {
    addItemLoading.value = false;
  }
};

// Nhom products theo sanPhamId, lay gia thap nhat, loc theo search
const addItemProductGroups = computed(() => {
  const q = addItemSearch.value.toLowerCase().trim();
  const map = {};
  for (const p of products.value) {
    if (!map[p.sanPhamId]) {
      map[p.sanPhamId] = { sanPhamId: p.sanPhamId, tenSanPham: p.tenSanPham,
        tenThuongHieu: p.tenThuongHieu, hinhAnhChinh: p.hinhAnhChinh,
        phanLoaiTen: p.phanLoaiTen, variants: [] };
    }
    map[p.sanPhamId].variants.push(p);
  }
  let groups = Object.values(map);
  if (q) groups = groups.filter(g =>
    g.tenSanPham.toLowerCase().includes(q) || g.tenThuongHieu?.toLowerCase().includes(q)
  );
  groups.forEach(g => {
    g.minPrice = Math.min(...g.variants.map(v => Number(v.giaBan) || 0));
  });
  return groups.sort((a, b) => a.tenSanPham.localeCompare(b.tenSanPham));
});

const refreshOrderDetail = async () => {
  orders.value = await DonHangService.getAll().catch(() => []);
  const updated = orders.value.find(o => o.donHangId === orderDetailData.value?.donHangId);
  if (updated) orderDetailData.value = updated;
  orderDetailItems.value = await ChiTietDonHangService.getByDonHang(orderDetailData.value.donHangId).catch(() => []);
};

const addItemToOrder = async () => {
  if (!addItemBienTheId.value || addItemQty.value < 1) return;
  const v = productByBienThe(Number(addItemBienTheId.value));
  if (!v) return;
  addItemLoading.value = true;
  try {
    const res = await DonHangService.addChiTiet({
      donHangId:    orderDetailData.value.donHangId,
      bienTheId:    v.bienTheId,
      soLuong:      addItemQty.value,
      donGia:       v.giaBan,
      giamGiaDong:  0,
    });
    if (!res.ok) { alert(`Them that bai: ${res.status}`); return; }
    await DonHangService.recalculate(orderDetailData.value.donHangId);
    await refreshOrderDetail();
    addItemBienTheId.value = '';
    addItemQty.value = 1;
    addItemMode.value = false;
  } finally {
    addItemLoading.value = false;
  }
};

const removeItemFromOrder = async (chiTietId) => {
  if (!confirm('Xoa san pham nay khoi don hang?')) return;
  const res = await fetch(`/api/chi-tiet-don-hang/delete/${chiTietId}`, { method: 'DELETE' });
  if (!res.ok) { alert(`Xoa that bai: ${res.status}`); return; }
  await DonHangService.recalculate(orderDetailData.value.donHangId);
  await refreshOrderDetail();
};

// ── Gop don hang ─────────────────────────────────────────────────────────────
const mergeLoading = ref(false);

// Don hang cung khach, cung ngay dat (khong tinh gio), khac don hien tai
const mergeCandidates = computed(() => {
  if (!orderDetailData.value) return [];
  const curDate = orderDetailData.value.ngayDat?.slice(0, 10);
  return orders.value.filter(o =>
    o.khachHangId === orderDetailData.value.khachHangId &&
    o.donHangId   !== orderDetailData.value.donHangId &&
    o.ngayDat?.slice(0, 10) === curDate
  );
});

// Gop tat ca don cung ngay cung khach vao don hien tai (khong can chon thu cong)
const autoMergeOrders = async () => {
  if (mergeCandidates.value.length === 0) return;
  if (!confirm(`Gop ${mergeCandidates.value.length} don hang cung ngay vao don #${orderDetailData.value.donHangId}?`)) return;
  mergeLoading.value = true;
  try {
    const res = await DonHangService.merge(
      orderDetailData.value.donHangId,
      mergeCandidates.value.map(o => o.donHangId)
    );
    if (!res.ok) { alert(`Gop that bai: ${res.status}`); return; }
    await refreshOrderDetail();
  } finally {
    mergeLoading.value = false;
  }
};

// Mo chi tiet 1 bien the cu the (khong load tat ca bien the cung san pham)
const openVariantDetail = (bienTheId) => {
  const v = productByBienThe(bienTheId);
  if (!v) return;
  detailModalName.value = v.tenSanPham;
  detailModalList.value = [v];
  detailSerialMap.value = {};
  showDetailModal.value = true;
  ChiTietSanPhamService.getByBienThe(bienTheId)
    .catch(() => [])
    .then(serials => { detailSerialMap.value = { [bienTheId]: serials }; });
};

// ── Order status helpers ──────────────────────────────────────────────────────
const orderStatusLabel = (s) => {
  const m = {
    pending:    'Cho xac nhan',
    confirmed:  'Da xac nhan',
    processing: 'Dang xu ly',
    shipping:   'Dang van chuyen',
    delivered:  'Da giao',
    cancelled:  'Da huy',
    returned:   'Da tra hang',
  };
  return m[s] || s;
};
const orderStatusColor = (s) => {
  if (s === 'pending')    return { bg: 'rgba(148,163,184,0.15)', text: '#94a3b8' };
  if (s === 'confirmed')  return { bg: 'rgba(59,130,246,0.15)',  text: '#60a5fa' };
  if (s === 'processing') return { bg: 'rgba(250,204,21,0.15)',  text: '#facc15' };
  if (s === 'shipping')   return { bg: 'rgba(139,92,246,0.15)',  text: '#a78bfa' };
  if (s === 'delivered')  return { bg: 'rgba(34,197,94,0.15)',   text: '#22c55e' };
  if (s === 'cancelled')  return { bg: 'rgba(239,68,68,0.15)',   text: '#f87171' };
  if (s === 'returned')   return { bg: 'rgba(251,146,60,0.15)',  text: '#fb923c' };
  return { bg: 'rgba(107,114,128,0.15)', text: '#9ca3af' };
};

// ── Orders status update ──────────────────────────────────────────────────────
const showOrderModal = ref(false);
const editingOrder = ref(null);
const orderStatusError = ref("");
const orderStatusForm = reactive({
  trangThaiDonHang: "",
  trangThaiThanhToan: "",
});

const openOrderStatus = (o) => {
  editingOrder.value = o;
  orderStatusForm.trangThaiDonHang = o.trangThaiDonHang ?? "";
  orderStatusForm.trangThaiThanhToan = o.trangThaiThanhToan ?? "";
  orderStatusError.value = "";
  showOrderModal.value = true;
};
const saveOrderStatus = async () => {
  orderStatusError.value = "";
  const o = editingOrder.value;
  const body = {
    khachHangId: o.khachHangId,
    nhanVienId: o.nhanVienId ?? null,
    khuyenMaiId: o.khuyenMaiId ?? null,
    diaChiGiaoHangId: o.diaChiGiaoHangId ?? null,
    diaChiGiaoHangText: o.diaChiGiaoHangText ?? null,
    nguoiNhan: o.nguoiNhan || customerName(o.khachHangId),
    sdtNguoiNhan:
      o.sdtNguoiNhan ||
      (customers.value.find((c) => c.khachHangId === o.khachHangId)
        ?.soDienThoai ?? ""),
    tongTien: o.tongTien ?? 0,
    giamGia: o.giamGia ?? 0,
    phiVanChuyen: o.phiVanChuyen ?? 0,
    thanhTien: o.thanhTien ?? 0,
    ngayDat: o.ngayDat?.slice(0, 19),
    ngayGiaoDuKien: o.ngayGiaoDuKien?.slice(0, 19) ?? null,
    ngayGiaoThucTe: o.ngayGiaoThucTe?.slice(0, 19) ?? null,
    trangThaiDonHang: orderStatusForm.trangThaiDonHang,
    trangThaiThanhToan: orderStatusForm.trangThaiThanhToan,
    kenhBan: o.kenhBan ?? null,
    ghiChu: o.ghiChu ?? null,
  };
  try {
    const res = await DonHangService.update(o.donHangId, body);
    if (!res.ok) {
      orderStatusError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showOrderModal.value = false;
    orders.value = await DonHangService.getAll().catch(() => []);
  } catch (e) {
    orderStatusError.value = e.message;
  }
};

// ── Inventory stock edit ──────────────────────────────────────────────────────
const showStockModal = ref(false);
const editingStock = ref(null);
const stockForm = reactive({ soLuongTon: 0, soLuongGiu: 0, tonKhoToiThieu: 0 });

const openEditStock = (item) => {
  editingStock.value = item;
  stockForm.soLuongTon = item.soLuongTon ?? 0;
  stockForm.soLuongGiu = item.soLuongGiu ?? 0;
  stockForm.tonKhoToiThieu = item.tonKhoToiThieu ?? 0;
  showStockModal.value = true;
};
const saveStock = async () => {
  const item = editingStock.value;
  const body = {
    soLuongTon: Number(stockForm.soLuongTon),
    soLuongGiu: Number(stockForm.soLuongGiu),
    tonKhoToiThieu: Number(stockForm.tonKhoToiThieu),
  };
  try {
    const res = await TonKhoService.update(item.tonKhoId, body);
    if (!res.ok) { alert(`Loi: ${res.status}`); return; }
    showStockModal.value = false;
    inventory.value = await TonKhoService.getAll().catch(() => []);
  } catch (e) {
    alert(e.message);
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
      ngayNhapKho: new Date().toISOString().slice(0, 19),
    });
    if (!res.ok) throw new Error('Loi them serial');
    stockDetailSerials.value = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
    inventory.value = await TonKhoService.getAll().catch(() => []);
    stockDetailNewSerial.value = '';
  } catch(e) { alert(e.message); }
  finally { stockDetailSaving.value = false; }
};

const stockDetailStatusLabel = (s) => {
  const m = {
    trong_kho:    'Trong kho',
    giu_hang:     'Dang dat hang',
    da_ban:       'Da ban',
    loi_bao_hanh: 'Bao hanh',
    da_tra_hang:  'Da tra hang',
  };
  return m[s] || s;
};
const stockDetailStatusColor = (s) => {
  if (s === 'trong_kho')    return '#22c55e';
  if (s === 'giu_hang')     return '#facc15';
  if (s === 'da_ban')       return '#94a3b8';
  if (s === 'loi_bao_hanh') return '#fb923c';
  if (s === 'da_tra_hang')  return '#38bdf8';
  return '#6b7280';
};

// ── POS / Ban hang ───────────────────────────────────────────────────────────
const posSearch = ref("");
const posCart = ref([]);
const posPhone = ref("");
const posFoundCust = ref(null);
const posNewName = ref("");
const posError = ref("");
const posSuccess = ref(false);

const posProducts = computed(() => {
  const q = posSearch.value.toLowerCase();
  return products.value.filter(
    (p) =>
      p.trangThai === "active" &&
      (!q ||
        p.tenSanPham.toLowerCase().includes(q) ||
        (p.maSku ?? "").toLowerCase().includes(q)),
  );
});
const posCartTotal = computed(() =>
  posCart.value.reduce((s, i) => s + i.giaBan * i.soLuong, 0),
);
const posFee = computed(() => (posCartTotal.value >= 300000 ? 0 : 30000));
const posGrandTotal = computed(() => posCartTotal.value + posFee.value);

const posAddToCart = (p) => {
  const ex = posCart.value.find((i) => i.bienTheId === p.bienTheId);
  if (ex) {
    ex.soLuong++;
  } else
    posCart.value.push({
      bienTheId: p.bienTheId,
      tenSanPham: p.tenSanPham,
      maSku: p.maSku,
      giaBan: p.giaBan,
      soLuong: 1,
    });
};
const posRemove = (bienTheId) => {
  posCart.value = posCart.value.filter((i) => i.bienTheId !== bienTheId);
};
const posReset = () => {
  posCart.value = [];
  posPhone.value = "";
  posFoundCust.value = null;
  posNewName.value = "";
  posError.value = "";
  posSuccess.value = false;
};

const posLookup = () => {
  posFoundCust.value =
    customers.value.find((c) => c.soDienThoai === posPhone.value.trim()) ??
    null;
};

const posPlaceOrder = async () => {
  if (!posCart.value.length) { posError.value = "Gio hang dang trong!"; return; }
  if (!posPhone.value.trim()) { posError.value = "Vui long nhap SDT khach hang!"; return; }
  posError.value = "";
  posSuccess.value = false;
  try {
    let khachHangId = posFoundCust.value?.khachHangId;
    if (!khachHangId) {
      const name = posNewName.value.trim() || posPhone.value.trim();
      const kRes = await KhachHangService.save(null, {
        hoTen: name, soDienThoai: posPhone.value.trim(),
        email: "", diaChi: "Tai cua hang",
        loaiKhach: "ca_nhan", diemTichLuy: 0, trangThai: "active",
      });
      if (!kRes.ok) throw new Error(`Loi tao khach hang: ${await kRes.text()}`);
      const kh = await kRes.json();
      khachHangId = kh.khachHangId;
      customers.value = await KhachHangService.getAll().catch(() => []);
    }
    const nguoiNhan = posFoundCust.value?.hoTen ?? (posNewName.value.trim() || posPhone.value.trim());
    const orderRes = await DonHangService.create({
      khachHangId, nguoiNhan, sdtNguoiNhan: posPhone.value.trim(),
      diaChiGiaoHangText: posFoundCust.value?.diaChi ?? "Tai cua hang",
      tongTien: posCartTotal.value, giamGia: 0,
      phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
      ngayDat: new Date().toISOString().slice(0, 19),
      trangThaiDonHang: "confirmed", trangThaiThanhToan: "paid", kenhBan: "in_store",
    });
    if (!orderRes.ok) throw new Error(`Loi tao don hang: ${await orderRes.text()}`);
    const created = await orderRes.json();
    const donHangId = created.id ?? created.donHangId;
    for (const item of posCart.value) {
      const ctRes = await DonHangService.addChiTiet({
        donHangId, bienTheId: item.bienTheId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0,
      });
      if (!ctRes.ok) throw new Error(`Loi them san pham: ${await ctRes.text()}`);
    }
    posSuccess.value = true;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null; posNewName.value = "";
    orders.value = await DonHangService.getAll().catch(() => []);
  } catch (e) {
    posError.value = e.message;
  }
};

let orderSse = null;

onMounted(async () => {

  if (currentRole.value === "admin") {
    currentPage.value = "dashboard";
  }

  if (currentRole.value === "nhan_vien") {
    currentPage.value = "user-home";
  }

  await fetchAll();

  orderSse = new EventSource("/api/don-hang/events");

  orderSse.addEventListener("new-order", async () => {
    orders.value = await DonHangService.getAll().catch(() => []);
    await autoMergeAllDuplicates();
  });

});

onUnmounted(() => {
  if (orderSse) orderSse.close();
});
</script>

<template>
  <!-- Layout chính: sidebar bên trái + main content bên phải -->
  <div class="d-flex overflow-hidden" style="height:100vh; background:#0d0d0d; color:#f0f0f0; font-family:'Inter',sans-serif;">

    <!-- ══════════ SIDEBAR ══════════ -->
    <aside class="d-flex flex-column border-end flex-shrink-0"
           style="width:240px; background:#111; border-color:rgba(255,255,255,0.08)!important; overflow-y:auto;">

      <!-- Logo -->
      <div class="d-flex align-items-center gap-2 p-3 border-bottom"
           style="border-color:rgba(255,255,255,0.06)!important;">
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:#f4c200;color:#111;font-size:0.8rem;">SAO</div>
        <div>
          <div class="fw-bold" style="font-size:0.95rem;">SAOPhone</div>
          <div style="font-size:0.7rem;color:#888;">He thong quan ly</div>
        </div>
      </div>

      <!-- Chuyển role Admin / Nhan vien -->
      <div class="d-flex gap-2 p-3 pb-2">

      </div>

      <!-- Nav admin -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2" v-if="currentRole === 'admin'">
        <div class="adm-nav-label">Tong quan</div>
        <div class="adm-nav" :class="{active: currentPage==='dashboard'}" @click="navigate('dashboard')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 4a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1V4zM3 11a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1v-3zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-3z"/></svg>
          Dashboard
        </div>

        <div class="adm-nav-label">Quan ly</div>
        <div class="adm-nav" :class="{active: currentPage==='products'}" @click="navigate('products')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 8a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zm6-6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zm0 8a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
          San pham
        </div>
        <div class="adm-nav" :class="{active: currentPage==='orders'}" @click="navigate('orders')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z" clip-rule="evenodd"/></svg>
          Don hang
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ totalOrders }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='customers'}" @click="navigate('customers')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/></svg>
          Khach hang
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ totalCustomers }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='inventory'}" @click="navigate('inventory')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M4 3a2 2 0 100 4h12a2 2 0 100-4H4z"/><path fill-rule="evenodd" d="M3 8h14v7a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm5 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" clip-rule="evenodd"/></svg>
          Kho hang
          <span v-if="lowStockItems.length" class="badge bg-danger ms-auto" style="font-size:0.68rem;">{{ lowStockItems.length }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='promotions'}" @click="navigate('promotions')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M5 5a3 3 0 015-2.236A3 3 0 0114.83 6H16a2 2 0 110 4h-5V9a1 1 0 10-2 0v1H4a2 2 0 110-4h1.17C5.06 5.687 5 5.35 5 5zm4 1V5a1 1 0 10-1 1h1zm3 0a1 1 0 10-1-1v1h1z" clip-rule="evenodd"/><path d="M9 11H3v5a2 2 0 002 2h4v-7zm2 7h4a2 2 0 002-2v-5h-6v7z"/></svg>
          Khuyen mai
        </div>
        <div class="adm-nav" :class="{active: currentPage==='ban-hang'}" @click="navigate('ban-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C4.328 11.142 4 11.574 4 12a2 2 0 002 2h10a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 4H6.28l-.31-1.243A1 1 0 005 2H3z"/><path d="M16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/></svg>
          Ban hang
        </div>
        <div class="adm-nav" :class="{active: currentPage==='staff'}" @click="navigate('staff')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/></svg>
          Nhan vien
        </div>

        <div class="adm-nav-label">Phan tich</div>
        <div class="adm-nav" :class="{active: currentPage==='reports'}" @click="navigate('reports')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zm6-4a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-3a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/></svg>
          Bao cao
        </div>
        <div class="adm-nav" :class="{active: currentPage==='settings'}" @click="navigate('settings')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/></svg>
          Cai dat
        </div>
      </nav>

      <!-- Nav nhan vien -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2"
           v-if="currentRole === 'nhan_vien'">

        <div class="adm-nav-label">Nhân viên</div>

        <div class="adm-nav"
             :class="{active: currentPage==='dashboard'}"
             @click="navigate('dashboard')">
          Dashboard
        </div>

        <div class="adm-nav"
             :class="{active: currentPage==='ban-hang'}"
             @click="navigate('ban-hang')">
          Bán hàng
        </div>

        <div class="adm-nav"
             :class="{active: currentPage==='orders'}"
             @click="navigate('orders')">
          Đơn hàng
        </div>

        <div class="adm-nav"
             :class="{active: currentPage==='customers'}"
             @click="navigate('customers')">
          Khách hàng
        </div>

        <div class="adm-nav"
             :class="{active: currentPage==='products'}"
             @click="navigate('products')">
          Sản phẩm
        </div>

      </nav>
      <nav
          class="flex-grow-1 d-flex flex-column px-2 pb-2"
          v-if="currentRole === 'quan_kho'">

        <div class="adm-nav-label">
          Quản lý kho
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='dashboard'}"
            @click="navigate('dashboard')">

          Dashboard
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='products'}"
            @click="navigate('products')">

          Sản phẩm
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='inventory'}"
            @click="navigate('inventory')">

          Kho hàng
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='import'}"
            @click="navigate('import')">

          Phiếu nhập
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='export'}"
            @click="navigate('export')">

          Phiếu xuất
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='supplier'}"
            @click="navigate('supplier')">

          Nhà cung cấp
        </div>

        <div
            class="adm-nav"
            :class="{active: currentPage==='serial'}"
            @click="navigate('serial')">

          Serial / IMEI
        </div>

      </nav>

      <!-- Footer sidebar: thong tin user + logout -->
      <div class="p-3 border-top" style="border-color:rgba(255,255,255,0.06)!important;">
        <div class="d-flex align-items-center gap-2 mb-2">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
               style="width:34px;height:34px;background:#f4c200;color:#111;font-size:0.9rem;">{{ userAvatar }}</div>
          <div class="flex-grow-1" style="min-width:0;">
            <div class="fw-semibold text-truncate" style="font-size:0.85rem;">{{ userDisplayName }}</div>
            <div style="font-size:0.72rem;color:#888;">{{ userDisplayRole }}</div>
          </div>
        </div>
        <button class="btn btn-sm w-100 fw-semibold"
                style="background:#1a1a1a; border:1px solid #7f1d1d; border-radius:8px; color:#f87171; font-size:0.78rem;"
                @click="logout">
          Dang xuat
        </button>
      </div>
    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">

      <!-- Topbar: tieu de trang hien tai -->
      <div class="d-flex align-items-center justify-content-between p-3 border-bottom"
           style="background:#111; border-color:rgba(255,255,255,0.07)!important;">
        <div>
          <div class="fw-bold" style="font-size:1.05rem;">{{ topbarTitle }}</div>
          <div style="font-size:0.78rem;color:#888;">{{ topbarSub }}</div>
        </div>
        <div class="d-flex align-items-center gap-2">
          <div class="d-flex align-items-center justify-content-center rounded-2"
               style="width:34px;height:34px;background:rgba(255,255,255,0.06);cursor:pointer;">&#128276;</div>
        </div>
      </div>

      <!-- Noi dung trang (scroll duoc) -->
      <div class="flex-grow-1 overflow-y-auto p-4">

        <!-- ── Dashboard ── -->
        <section v-show="currentPage === 'dashboard'">
          <div v-if="loading" class="text-secondary small">Dang tai du lieu...</div>
          <template v-else>
            <!-- Stat cards -->
            <div class="row g-3 mb-4">
              <div class="col-6 col-xl-3">
                <div class="card border-secondary h-100" style="background:rgba(255,255,255,0.04);">
                  <div class="card-body">
                    <div class="text-secondary small mb-2">Tong san pham</div>
                    <div class="fw-bold" style="font-size:1.55rem;">{{ totalProducts }}</div>
                  </div>
                </div>
              </div>
              <div class="col-6 col-xl-3">
                <div class="card border-secondary h-100" style="background:rgba(255,255,255,0.04);">
                  <div class="card-body">
                    <div class="text-secondary small mb-2">Don hang</div>
                    <div class="fw-bold" style="font-size:1.55rem;">{{ totalOrders }}</div>
                  </div>
                </div>
              </div>
              <div class="col-6 col-xl-3">
                <div class="card border-secondary h-100" style="background:rgba(255,255,255,0.04);">
                  <div class="card-body">
                    <div class="text-secondary small mb-2">Khach hang</div>
                    <div class="fw-bold" style="font-size:1.55rem;">{{ totalCustomers }}</div>
                  </div>
                </div>
              </div>
              <div class="col-6 col-xl-3">
                <div class="card border-secondary h-100" style="background:rgba(255,255,255,0.04);">
                  <div class="card-body">
                    <div class="text-secondary small mb-2">Doanh thu</div>
                    <div class="fw-bold" style="font-size:1.1rem;">{{ formatPrice(totalRevenue) }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Canh bao het hang -->
            <div v-if="lowStockItems.length" class="alert alert-danger small py-2 mb-3">
              &#9888; {{ lowStockItems.length }} bien the san pham sap het hang
            </div>

            <!-- Bang san pham gan day -->
            <div class="small fw-semibold text-secondary mb-2">San pham gan day</div>
            <div class="table-responsive">
              <table class="table table-dark table-hover table-sm align-middle">
                <thead><tr>
                  <th>Ten san pham</th><th>Thuong hieu</th><th>Danh muc</th><th>Gia ban</th><th>Trang thai</th>
                </tr></thead>
                <tbody>
                  <tr v-for="p in products.slice(0,5)" :key="p.sanPhamId">
                    <td>{{ p.tenSanPham }}</td>
                    <td>{{ p.tenThuongHieu }}</td>
                    <td>{{ p.tenDanhMuc }}</td>
                    <td>{{ formatPrice(p.giaBan) }}</td>
                    <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
                  </tr>
                  <tr v-if="products.length===0"><td colspan="5" class="text-center text-secondary">Chua co san pham</td></tr>
                </tbody>
              </table>
            </div>
          </template>
        </section>

        <!-- ── San pham ── -->
        <section v-show="currentPage === 'products'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ totalProducts }} san pham</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">+ Them san pham</button>
          </div>
          <div v-if="loading" class="text-secondary small">Dang tai...</div>
          <div v-else class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr>
                <th>Ten san pham</th><th>Thuong hieu</th><th>Danh muc</th>
                <th>Bien the</th><th>Gia tu</th><th>Trang thai</th><th>Thao tac</th>
              </tr></thead>
              <tbody>
                <tr v-for="p in groupedProducts" :key="p.sanPhamId">
                  <td>{{ p.tenSanPham }}</td>
                  <td>{{ p.tenThuongHieu }}</td>
                  <td>{{ p.tenDanhMuc }}</td>
                  <td class="text-center">
                    <span class="badge bg-secondary">{{ p.variantCount }}</span>
                  </td>
                  <td>{{ formatPrice(p.minPrice) }}</td>
                  <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-primary" style="font-size:0.78rem; padding:2px 8px;" @click="openDetail(p.sanPhamId, p.tenSanPham)">Chi tiet</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteProduct(p.sanPhamId)">Xoa</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="groupedProducts.length===0"><td colspan="7" class="text-center text-secondary">Chua co san pham</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Don hang ── -->
        <section v-show="currentPage === 'orders'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ totalOrders }} don hang</span>
          </div>
          <div v-if="loading" class="text-secondary small">Dang tai...</div>
          <div v-else class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>Ma don</th><th>Khach hang</th><th>Thanh tien</th><th>TT Don hang</th><th>TT Thanh toan</th><th>Ngay dat</th><th>Thao tac</th></tr></thead>
              <tbody>
                <tr v-for="o in orders" :key="o.donHangId">
                  <td class="text-secondary">#{{ o.donHangId }}</td>
                  <td>{{ customerName(o.khachHangId) }}</td>
                  <td>{{ formatPrice(o.thanhTien) }}</td>
                  <td>
                    <span class="badge" :style="{ background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text }">
                      {{ orderStatusLabel(o.trangThaiDonHang) }}
                    </span>
                  </td>
                  <td><span class="badge" :class="o.trangThaiThanhToan==='paid'?'bg-success':'bg-secondary'">{{ o.trangThaiThanhToan||'—' }}</span></td>
                  <td>{{ formatDate(o.ngayDat) }}</td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-info"    style="font-size:0.78rem;padding:2px 8px;" @click="openOrderDetail(o)">Chi tiet</button>
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openOrderStatus(o)">Cap nhat</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem;padding:2px 8px;" @click="deleteOrder(o.donHangId)">Xoa</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="orders.length===0"><td colspan="8" class="text-center text-secondary">Chua co don hang</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Khach hang ── -->
        <section v-show="currentPage === 'customers'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ totalCustomers }} khach hang</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddCustomer">+ Them khach hang</button>
          </div>
          <div v-if="loading" class="text-secondary small">Dang tai...</div>
          <div v-else class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>Ho ten</th><th>Dien thoai</th><th>Email</th><th>Loai khach</th><th>Diem</th><th>Trang thai</th><th>Thao tac</th></tr></thead>
              <tbody>
                <tr v-for="c in customers" :key="c.khachHangId">
                  <td>{{ c.hoTen }}</td>
                  <td class="text-secondary">{{ c.soDienThoai }}</td>
                  <td class="text-secondary">{{ c.email }}</td>
                  <td>{{ c.loaiKhach||'—' }}</td>
                  <td>{{ c.diemTichLuy??0 }}</td>
                  <td><span class="badge" :class="c.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(c.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditCustomer(c)">Sua</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteCustomer(c.khachHangId)">Xoa</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="customers.length===0"><td colspan="7" class="text-center text-secondary">Chua co khach hang</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Kho hang ── -->
        <section v-show="currentPage === 'inventory'">
          <!-- Summary + Search -->
          <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
            <span class="text-secondary small">{{ inventoryGrouped.length }} san pham · {{ inventory.length }} SKU</span>
            <span v-if="outOfStockItems.length" class="badge bg-danger">● {{ outOfStockItems.length }} het hang</span>
            <span v-if="lowStockItems.length" class="badge bg-warning text-dark">⚠ {{ lowStockItems.length }} sap het</span>
            <div class="ms-auto" style="min-width:200px;">
              <input v-model="inventorySearch" class="form-control form-control-sm"
                     placeholder="Tim san pham..."
                     style="background:#1a1a1a;border-color:#333;color:#e0e0e0;font-size:0.82rem;" />
            </div>
          </div>

          <div v-if="loading" class="text-secondary small py-4 text-center">Dang tai...</div>
          <div v-else class="d-flex flex-column gap-2">

            <div v-for="group in inventoryGrouped" :key="group.name"
                 class="rounded-3 overflow-hidden"
                 style="background:#1a1a1a;border:1px solid #2a2a2a;">

              <!-- Product header -->
              <div class="d-flex align-items-center px-3 py-2 gap-3"
                   style="cursor:pointer;transition:background 0.15s;"
                   @mouseenter="$event.currentTarget.style.background='#202020'"
                   @mouseleave="$event.currentTarget.style.background=''"
                   @click="toggleGroup(group.name)">
                <img v-if="group.hinhAnh" :src="group.hinhAnh"
                     style="width:44px;height:36px;object-fit:contain;border-radius:4px;background:#111;flex-shrink:0;" />
                <div v-else style="width:44px;height:36px;background:#222;border-radius:4px;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:1rem;">💻</div>
                <div class="flex-grow-1 min-width-0">
                  <div class="text-white fw-semibold" style="font-size:0.88rem;">{{ group.name }}</div>
                  <div class="text-secondary" style="font-size:0.72rem;">
                    {{ group.thuongHieu ? group.thuongHieu + ' · ' : '' }}{{ group.items.length }} bien the · Tong ton: <strong :class="group.totalTon===0?'text-danger':group.totalTon<5?'text-warning':'text-success'">{{ group.totalTon }}</strong>
                  </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <span v-if="group.outCount" class="badge bg-danger" style="font-size:0.7rem;">{{ group.outCount }} het hang</span>
                  <span v-else-if="group.lowCount" class="badge bg-warning text-dark" style="font-size:0.7rem;">{{ group.lowCount }} sap het</span>
                  <span v-else class="badge bg-success" style="font-size:0.7rem;">OK</span>
                  <span class="text-secondary" style="font-size:0.75rem;width:12px;text-align:center;">{{ expandedGroups[group.name] ? '▲' : '▼' }}</span>
                </div>
              </div>

              <!-- Variant detail table -->
              <div v-if="expandedGroups[group.name]" style="border-top:1px solid #252525;">
                <table class="w-100" style="border-collapse:collapse;font-size:0.8rem;">
                  <thead>
                    <tr style="background:#1e1e1e;">
                      <th class="px-3 py-2 text-secondary" style="font-weight:500;width:22%;">SKU</th>
                      <th class="px-3 py-2 text-secondary" style="font-weight:500;">Cau hinh</th>
                      <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:80px;">Ton</th>
                      <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:70px;">Giu</th>
                      <th class="px-3 py-2 text-secondary text-center" style="font-weight:500;width:80px;">Toi thieu</th>
                      <th class="px-3 py-2 text-secondary" style="font-weight:500;width:90px;"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in group.items" :key="item.tonKhoId"
                        style="border-top:1px solid #222;">
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
                                  @click.stop="openStockDetail(item)">Chi tiet</button>
                          <button class="btn btn-sm btn-outline-warning"
                                  style="font-size:0.72rem;padding:2px 8px;"
                                  @click.stop="openEditStock(item)">Cap nhat</button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div v-if="inventoryGrouped.length === 0" class="text-secondary small text-center py-5">Khong co du lieu</div>
          </div>
        </section>

        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ promotions.length }} khuyen mai</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddPromo">+ Them khuyen mai</button>
          </div>
          <div v-if="loading" class="text-secondary small">Dang tai...</div>
          <div v-else class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>Ma KM</th><th>Ten</th><th>Loai</th><th>Gia tri</th><th>Bat dau</th><th>Ket thuc</th><th>Da dung</th><th>Trang thai</th><th>Thao tac</th></tr></thead>
              <tbody>
                <tr v-for="p in promotions" :key="p.khuyenMaiId">
                  <td class="text-secondary">{{ p.maKhuyenMai }}</td>
                  <td>{{ p.tenKhuyenMai }}</td>
                  <td>{{ p.loai==='percent'?'Phan tram':'So tien' }}</td>
                  <td>{{ p.loai==='percent'?`${p.giaTri}%`:formatPrice(p.giaTri) }}</td>
                  <td>{{ formatDate(p.ngayBatDau) }}</td>
                  <td>{{ formatDate(p.ngayKetThuc) }}</td>
                  <td>{{ p.soLanDaDung??0 }}/{{ p.soLuongToiDa??'∞' }}</td>
                  <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditPromo(p)">Sua</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deletePromo(p.khuyenMaiId)">Xoa</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="promotions.length===0"><td colspan="9" class="text-center text-secondary">Chua co khuyen mai</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Nhan vien ── -->
        <section v-show="currentPage === 'staff'">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ staff.length }} nhan vien</span>
            <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAddStaff">+ Them nhan vien</button>
          </div>
          <div v-if="loading" class="text-secondary small">Dang tai...</div>
          <div v-else class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>Ho ten</th><th>Dien thoai</th><th>Email</th><th>Chuc vu</th><th>Username</th><th>Luong co ban</th><th>Trang thai</th><th>Thao tac</th></tr></thead>
              <tbody>
                <tr v-for="s in staff" :key="s.nhanVienId">
                  <td>{{ s.hoTen }}</td>
                  <td class="text-secondary">{{ s.soDienThoai }}</td>
                  <td class="text-secondary">{{ s.email }}</td>
                  <td>{{ chucVuName(s.chucVuId) }}</td>
                  <td class="text-secondary">{{ s.username }}</td>
                  <td>{{ formatPrice(s.luongCoBan) }}</td>
                  <td><span class="badge" :class="s.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(s.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditStaff(s)">Sua</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteStaff(s.nhanVienId)">Xoa</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="staff.length===0"><td colspan="8" class="text-center text-secondary">Chua co nhan vien</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Bao cao ── -->
        <section v-show="currentPage === 'reports'">
          <div class="row g-3 mb-4">
            <div class="col-6 col-xl-3">
              <div class="card border-secondary" style="background:rgba(255,255,255,0.04);"><div class="card-body">
                <div class="text-secondary small mb-1">Tong doanh thu</div>
                <div class="fw-bold" style="font-size:1.1rem;">{{ formatPrice(totalRevenue) }}</div>
              </div></div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary" style="background:rgba(255,255,255,0.04);"><div class="card-body">
                <div class="text-secondary small mb-1">San pham dang ban</div>
                <div class="fw-bold" style="font-size:1.55rem;">{{ activeProducts }}</div>
              </div></div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary" style="background:rgba(255,255,255,0.04);"><div class="card-body">
                <div class="text-secondary small mb-1">Khuyen mai dang chay</div>
                <div class="fw-bold" style="font-size:1.55rem;">{{ activePromos }}</div>
              </div></div>
            </div>
            <div class="col-6 col-xl-3">
              <div class="card border-secondary" style="background:rgba(255,255,255,0.04);"><div class="card-body">
                <div class="text-secondary small mb-1">Bien the sap het hang</div>
                <div class="fw-bold" :class="lowStockItems.length?'text-danger':''" style="font-size:1.55rem;">{{ lowStockItems.length }}</div>
              </div></div>
            </div>
          </div>
          <div class="small fw-semibold text-secondary mb-2">Don hang theo trang thai</div>
          <div class="table-responsive mb-4">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>Trang thai</th><th>So luong</th></tr></thead>
              <tbody>
                <tr v-for="row in ordersByStatus" :key="row.status">
                  <td><span class="badge bg-primary bg-opacity-25 text-primary">{{ row.status||'Chua co' }}</span></td>
                  <td><strong>{{ row.count }}</strong></td>
                </tr>
                <tr v-if="ordersByStatus.length===0"><td colspan="2" class="text-center text-secondary">Chua co don hang</td></tr>
              </tbody>
            </table>
          </div>
          <div class="small fw-semibold text-secondary mb-2">Top 5 san pham gia cao nhat</div>
          <div class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>#</th><th>Ten san pham</th><th>Thuong hieu</th><th>Gia ban</th></tr></thead>
              <tbody>
                <tr v-for="(p,i) in [...products].sort((a,b)=>(b.giaBan??0)-(a.giaBan??0)).slice(0,5)" :key="p.sanPhamId">
                  <td class="text-secondary">{{ i+1 }}</td><td>{{ p.tenSanPham }}</td><td>{{ p.tenThuongHieu }}</td><td>{{ formatPrice(p.giaBan) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Cai dat ── -->
        <section v-show="currentPage === 'settings'">
          <div class="card border-secondary" style="background:rgba(255,255,255,0.04); max-width:520px;">
            <div class="card-body">
              <div class="fw-bold mb-3">Thong tin he thong</div>
              <div v-for="row in [
                {label:'Ten he thong', value:'SAOPhone Admin'},
                {label:'Phien ban', value:'1.0.0'},
                {label:'Backend API', value:'http://localhost:8080'},
                {label:'Database', value:'SQL Server — QLBanMayTinh'},
              ]" :key="row.label"
                   class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                <span class="text-secondary">{{ row.label }}</span>
                <span>{{ row.value }}</span>
              </div>
              <div class="d-flex justify-content-between align-items-center py-2 small">
                <span class="text-secondary">Trang thai</span>
                <span class="badge bg-success">Hoat dong</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Ban hang (POS) ── -->
        <section v-show="currentPage === 'ban-hang'">
          <div class="pos-grid-layout">
            <!-- LEFT: tim kiem + san pham -->
            <div class="d-flex flex-column gap-3 overflow-hidden">
              <input v-model="posSearch" class="form-control form-control-sm"
                     style="background:rgba(255,255,255,0.05); border-color:rgba(255,255,255,0.12); color:#eee;"
                     placeholder="Tim san pham theo ten hoac SKU..." />
              <div v-if="loading" class="text-secondary small">Dang tai...</div>
              <div v-else class="row g-2 overflow-y-auto">
                <div v-for="p in posProducts" :key="p.bienTheId" class="col-6 col-xl-4">
                  <div class="card h-100 border-secondary" style="background:rgba(255,255,255,0.04);">
                    <div class="card-body p-2 d-flex flex-column gap-1">
                      <div class="fw-semibold small text-light">{{ p.tenSanPham }}</div>
                      <div class="text-secondary" style="font-size:0.76rem;">{{ p.maSku }}</div>
                      <div class="text-secondary" style="font-size:0.75rem;">{{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}</div>
                      <div class="fw-bold text-warning" style="font-size:0.95rem;">{{ formatPrice(p.giaBan) }}</div>
                      <button class="btn btn-sm btn-warning text-dark fw-bold mt-auto" @click="posAddToCart(p)">+ Them vao gio</button>
                    </div>
                  </div>
                </div>
                <div v-if="posProducts.length===0" class="col-12 text-center text-secondary small py-4">Khong tim thay san pham</div>
              </div>
            </div>

            <!-- RIGHT: gio hang POS -->
            <div class="card border-secondary d-flex flex-column overflow-hidden" style="background:rgba(255,255,255,0.03);">
              <div class="card-header border-secondary d-flex justify-content-between align-items-center fw-bold">
                Gio hang <span class="text-secondary fw-normal small">{{ posCart.length }} san pham</span>
              </div>
              <!-- Danh sach san pham trong gio -->
              <div class="flex-grow-1 overflow-y-auto p-2 d-flex flex-column gap-1">
                <div v-if="posCart.length===0" class="text-secondary small text-center py-4">Chua co san pham nao</div>
                <div v-for="item in posCart" :key="item.bienTheId"
                     class="d-flex align-items-center gap-2 p-2 rounded-2" style="background:rgba(255,255,255,0.04);">
                  <div class="flex-grow-1" style="min-width:0;">
                    <div class="fw-semibold small text-light text-truncate">{{ item.tenSanPham }}</div>
                    <div class="text-secondary" style="font-size:0.73rem;">{{ item.maSku }}</div>
                  </div>
                  <div class="d-flex align-items-center gap-1 flex-shrink-0">
                    <button class="btn btn-sm btn-outline-secondary" style="width:22px;height:22px;padding:0;font-size:0.85rem;"
                            @click="item.soLuong>1?item.soLuong--:posRemove(item.bienTheId)">−</button>
                    <span class="text-center" style="min-width:22px;font-size:0.84rem;">{{ item.soLuong }}</span>
                    <button class="btn btn-sm btn-outline-secondary" style="width:22px;height:22px;padding:0;font-size:0.85rem;"
                            @click="item.soLuong++">+</button>
                    <button class="btn btn-sm btn-outline-danger" style="width:20px;height:20px;padding:0;font-size:0.72rem;"
                            @click="posRemove(item.bienTheId)">✕</button>
                  </div>
                  <div class="text-warning fw-bold flex-shrink-0 text-end" style="font-size:0.8rem;min-width:72px;">{{ formatPrice(item.giaBan*item.soLuong) }}</div>
                </div>
              </div>
              <!-- Tong tien -->
              <div class="p-2 border-top border-secondary d-flex flex-column gap-1">
                <div class="d-flex justify-content-between text-secondary small"><span>Tong hang:</span><span>{{ formatPrice(posCartTotal) }}</span></div>
                <div class="d-flex justify-content-between text-secondary small"><span>Phi van chuyen:</span><span>{{ posFee===0?'Mien phi':formatPrice(posFee) }}</span></div>
                <div class="d-flex justify-content-between fw-bold"><span>Thanh toan:</span><span>{{ formatPrice(posGrandTotal) }}</span></div>
              </div>
              <!-- Khach hang -->
              <div class="p-2 border-top border-secondary d-flex flex-column gap-2">
                <div class="text-uppercase text-secondary fw-bold" style="font-size:0.78rem;letter-spacing:0.04em;">Thong tin khach hang</div>
                <div class="d-flex gap-2">
                  <input v-model="posPhone" class="form-control form-control-sm" style="background:rgba(255,255,255,0.05);border-color:rgba(255,255,255,0.12);color:#eee;" placeholder="So dien thoai *" @keyup.enter="posLookup" />
                  <button class="btn btn-sm btn-outline-warning flex-shrink-0" @click="posLookup">Tim</button>
                </div>
                <div v-if="posFoundCust" class="small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">✓ {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}</div>
                <div v-else-if="posPhone.trim()" class="d-flex flex-column gap-1">
                  <input v-model="posNewName" class="form-control form-control-sm" style="background:rgba(255,255,255,0.05);border-color:rgba(255,255,255,0.12);color:#eee;" placeholder="Ho ten khach moi (neu co)" />
                  <div class="text-secondary" style="font-size:0.75rem;">Khach hang moi se duoc tao tu dong</div>
                </div>
                <div v-if="posError" class="small p-2 rounded-2" style="background:rgba(220,53,69,0.1);color:#e05252;">{{ posError }}</div>
                <div v-if="posSuccess" class="small p-2 rounded-2" style="background:rgba(72,199,142,0.1);color:#48c78e;">✓ Tao don hang thanh cong!</div>
                <div class="d-flex gap-2">
                  <button class="btn btn-sm btn-outline-secondary flex-grow-1" @click="posReset">Lam moi</button>
                  <button class="btn btn-sm btn-warning text-dark fw-bold" style="flex:2;" @click="posPlaceOrder">Tao don hang</button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Trang placeholder cho nhan vien ── -->
        <section
            v-if="currentRole === 'khach_hang' &&
           ['user-home','user-orders','user-browse','user-warranty','user-profile'].includes(currentPage)"
            class="d-flex flex-column align-items-center justify-content-center text-secondary"
            style="min-height:300px;gap:12px;"
        >
          <div style="font-size:2.8rem;">&#128101;</div>
          <div class="fw-bold" style="color:#888;font-size:1.15rem;">
            {{ topbarTitle }}
          </div>
          <div style="font-size:0.83rem;">
            Xem trang khách hàng tại trang chủ chính
          </div>
        </section>

      </div><!-- /content -->
    </main>
  </div><!-- /dashboard-shell -->

  <!-- ══ MODAL SAN PHAM ══ -->
  <div v-if="showProductModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.7);z-index:1000;" @click.self="showProductModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:860px;max-width:96vw;max-height:92vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid #2a2a2a;">
        <div>
          <div class="fw-bold text-light" style="font-size:1rem;">{{ editingId ? 'Cap nhat san pham' : 'Them san pham moi' }}</div>
          <div v-if="editingId" class="text-secondary" style="font-size:0.72rem;margin-top:2px;">ID: {{ editingId }}</div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showProductModal=false"></button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto px-4 py-3" style="gap:0;">
        <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>

        <!-- ── Thong tin co ban ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">Thong tin co ban</div>
        <div class="rounded-3 p-3 mb-3" style="background:#1e1e1e;border:1px solid #2a2a2a;">
          <div class="row g-3">
            <div class="col-8">
              <label class="form-label small text-secondary mb-1">Ten san pham *</label>
              <input v-model="form.tenSanPham" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Ten san pham" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Ma SKU *</label>
              <input v-model="form.maSku" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="SKU-001" style="font-family:monospace;" />
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">Loai san pham *</label>
              <select v-model="form.loaiSanPham" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option value="" disabled>-- Chon --</option>
                <option value="LAPTOP">Laptop</option>
                <option value="PHU_KIEN">Phu kien</option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">Trang thai *</label>
              <select v-model="form.trangThai" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option value="active">Hoat dong</option>
                <option value="inactive">Ngung ban</option>
                <option value="ngung_kin_doanh">Ngung kinh doanh</option>
              </select>
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">Mau sac *</label>
              <input v-model="form.mauSac" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Den, Bac..." />
            </div>
            <div class="col-3">
              <label class="form-label small text-secondary mb-1">Bao hanh (thang) *</label>
              <input v-model="form.baoHanhThang" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Thuong hieu *</label>
              <select v-model="form.thuongHieuId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null" disabled>-- Chon --</option>
                <option v-for="b in brands" :key="b.thuongHieuId" :value="b.thuongHieuId">{{ b.tenThuongHieu }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Danh muc *</label>
              <select v-model="form.danhMucId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null" disabled>-- Chon --</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.tenDanhMuc }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Nha cung cap</label>
              <select v-model="form.nhaCungCapId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null">-- Khong co --</option>
                <option v-for="s in suppliers" :key="s.nhaCungCapId" :value="s.nhaCungCapId">{{ s.tenNhaCungCap }}</option>
              </select>
            </div>
          </div>
        </div>

        <!-- ── Cau hinh ky thuat ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">Cau hinh ky thuat</div>
        <div class="rounded-3 p-3 mb-3" style="background:#1e1e1e;border:1px solid #2a2a2a;">
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">CPU</label>
              <select v-model="form.cpuId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null">-- Khong co --</option>
                <option v-for="c in cpuList" :key="c.cpuId" :value="c.cpuId">{{ c.tenCpu }}</option>
              </select>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">GPU</label>
              <select v-model="form.gpuId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null">-- Khong co --</option>
                <option v-for="g in gpuList" :key="g.gpuId" :value="g.gpuId">{{ g.tenGpu }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">RAM</label>
              <select v-model="form.ramId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null">-- Khong co --</option>
                <option v-for="r in ramList" :key="r.ramId" :value="r.ramId">{{ r.dungLuong }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">O cung</label>
              <select v-model="form.oCungId" class="form-select form-select-sm bg-dark text-light border-secondary">
                <option :value="null">-- Khong co --</option>
                <option v-for="o in oCungList" :key="o.oCungId" :value="o.oCungId">{{ o.loaiOcung }}</option>
              </select>
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Man hinh</label>
              <input v-model="form.kichThuocManHinh" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder='15.6" FHD' />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">He dieu hanh</label>
              <input v-model="form.heDieuHanh" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Windows 11" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Pin</label>
              <input v-model="form.pin" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="72Wh" />
            </div>
            <div class="col-4">
              <label class="form-label small text-secondary mb-1">Trong luong (kg)</label>
              <input v-model="form.trongLuongKg" type="number" step="0.1" class="form-control form-control-sm bg-dark text-light border-secondary" />
            </div>
          </div>
        </div>

        <!-- ── Gia ca ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">Gia ca</div>
        <div class="rounded-3 p-3 mb-3" style="background:#1e1e1e;border:1px solid #2a2a2a;">
          <div class="row g-3">
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">Gia ban (VND) *</label>
              <input v-model="form.giaBan" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" />
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">Gia nhap (VND) *</label>
              <input v-model="form.giaNhap" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" />
            </div>
          </div>
        </div>

        <!-- ── Hinh anh, mo ta, phan loai ── -->
        <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#60a5fa;">Hinh anh & Mo ta</div>
        <div class="rounded-3 p-3 mb-3" style="background:#1e1e1e;border:1px solid #2a2a2a;">
          <div class="row g-3">
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">Hinh anh chinh</label>
              <div class="d-flex align-items-center gap-3">
                <label class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary" style="width:110px;height:88px;cursor:pointer;flex-shrink:0;overflow:hidden;background:#111;">
                  <img v-if="imagePreview" :src="imagePreview" style="width:110px;height:88px;object-fit:contain;" />
                  <template v-else>
                    <span style="font-size:1.4rem;">&#128247;</span>
                    <span style="font-size:0.68rem;margin-top:4px;">Click de chon</span>
                  </template>
                  <input type="file" accept="image/*" class="d-none" @change="handleImageFile" />
                </label>
                <div v-if="imageFilePending" class="text-warning" style="font-size:0.75rem;">{{ imageFilePending.name }}</div>
                <div v-else class="text-secondary" style="font-size:0.75rem;">JPG, PNG, WEBP</div>
              </div>
            </div>
            <div class="col-12">
              <label class="form-label small text-secondary mb-1">Mo ta</label>
              <textarea v-model="form.moTa" rows="3" class="form-control form-control-sm bg-dark text-light border-secondary"></textarea>
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">Phan loai Tags <span class="text-warning small">(de loc)</span></label>
              <input v-model="form.phanLoaiTags" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="gaming,do_hoa  hoac  van_phong,sinh_vien" />
            </div>
            <div class="col-6">
              <label class="form-label small text-secondary mb-1">Phan loai Ten <span class="text-muted small">(ten hien thi)</span></label>
              <input v-model="form.phanLoaiTen" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Gaming, Do hoa" />
            </div>
          </div>
        </div>

        <!-- ── Serial (chi khi them moi) ── -->
        <div v-if="!editingId">
          <div class="text-uppercase fw-bold mb-2" style="font-size:0.65rem;letter-spacing:0.1em;color:#facc15;">Serial dau tien</div>
          <div class="rounded-3 p-3" style="background:#1e1e1e;border:1px solid #2a2a2a;">
            <label class="form-label small text-secondary mb-1">So Serial <span class="text-danger">*</span></label>
            <input v-model="soSerialMoi" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="VD: SN2024DELL001" style="font-family:monospace;" />
            <div class="text-secondary mt-1" style="font-size:0.72rem;">Bat buoc khi them moi — dung de quan ly kho</div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="d-flex justify-content-end gap-2 px-4 py-3" style="border-top:1px solid #2a2a2a;">
        <button class="btn btn-sm btn-outline-secondary px-3" @click="showProductModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold px-4" @click="saveProduct">{{ editingId ? 'Cap nhat' : 'Them moi' }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL BIEN THE SAN PHAM ══ -->
  <div v-if="showVariantModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.72);z-index:1050;" @click.self="showVariantModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.12);width:960px;max-width:96vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>Bien the: {{ variantModalName }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showVariantModal=false"></button>
      </div>
      <div class="overflow-y-auto p-3">
        <!-- moi bien the la 1 card -->
        <div v-for="v in variantModalList" :key="v.bienTheId" class="mb-3 rounded-3 overflow-hidden" style="border:1px solid #2a2a2a;">
          <!-- header bien the -->
          <div class="d-flex align-items-center gap-3 px-3 py-2 justify-content-between" style="background:#222;">
            <div class="d-flex align-items-center gap-3 flex-wrap">
              <span class="text-secondary" style="font-size:0.75rem;font-family:monospace;">{{ v.maSku }}</span>
              <span class="badge rounded-pill" style="font-size:0.72rem;background:#333;color:#eee;">{{ v.mauSac }}</span>
              <span class="text-light" style="font-size:0.82rem;">{{ v.cpu }} · {{ v.ram }} · {{ v.oCung }}</span>
              <span class="text-secondary" style="font-size:0.8rem;">{{ v.kichThuocManHinh }}</span>
              <span class="fw-bold text-warning" style="font-size:0.88rem;">{{ formatPrice(v.giaBan) }}</span>
              <span class="badge" :class="v.trangThai==='active'?'bg-success':'bg-secondary'" style="font-size:0.7rem;">{{ statusLabel(v.trangThai) }}</span>
            </div>
            <button class="btn btn-sm btn-outline-warning flex-shrink-0" style="font-size:0.75rem;padding:2px 8px;" @click="showVariantModal=false; openEdit(v)">Sua</button>
          </div>

        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET SAN PHAM ══ -->
  <div v-if="showDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.75);z-index:1060;" @click.self="showDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.12);width:1100px;max-width:97vw;max-height:92vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>Chi tiet: {{ detailModalName }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showDetailModal=false"></button>
      </div>
      <div class="overflow-y-auto p-3">
        <div v-for="v in detailModalList" :key="v.bienTheId" class="mb-4 rounded-3 overflow-hidden" style="border:1px solid #2a2a2a;">
          <!-- Header bien the -->
          <div class="d-flex align-items-center justify-content-between gap-3 p-3" style="background:#222;">
            <div class="d-flex align-items-center gap-3">
              <img v-if="v.hinhAnhChinh" :src="v.hinhAnhChinh" style="width:72px;height:54px;object-fit:contain;background:#111;border-radius:6px;padding:4px;" />
              <span v-else style="font-size:2rem;width:72px;text-align:center;">💻</span>
              <div>
                <div class="fw-bold text-light" style="font-size:0.95rem;">{{ v.tenSanPham }}</div>
                <div class="text-secondary" style="font-size:0.75rem;font-family:monospace;">{{ v.maSku }}</div>
              </div>
            </div>
            <button class="btn btn-sm btn-outline-warning flex-shrink-0" style="font-size:0.75rem;padding:3px 12px;" @click="showDetailModal=false; openEdit(v)">Sua</button>
          </div>
          <!-- Bang thong tin 4 cot (label | value | label | value) -->
          <table class="w-100 mb-0" style="border-collapse:collapse;font-size:0.8rem;">
            <tbody>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Thuong hieu</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.tenThuongHieu }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Danh muc</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.tenDanhMuc }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Nha cung cap</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.tenNhaCungCap }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Loai san pham</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.loaiSanPham }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Gia ban</td>
                <td class="px-3 py-1 fw-bold" style="background:#1a1a1a;color:#facc15;">{{ formatPrice(v.giaBan) }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Gia nhap</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ formatPrice(v.giaNhap) }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Bao hanh</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.baoHanhThang ? v.baoHanhThang + ' thang' : '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Mau sac</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.mauSac }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Trang thai</td>
                <td class="px-3 py-1" style="background:#1a1a1a;">
                  <span class="badge" :class="v.trangThai==='active'?'bg-success':'bg-secondary'" style="font-size:0.7rem;">{{ statusLabel(v.trangThai) }}</span>
                </td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">CPU</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.cpu || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">RAM</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.ram || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">O cung</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.oCung || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">GPU</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.gpu || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Man hinh</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.kichThuocManHinh || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">He dieu hanh</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.heDieuHanh || '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Pin</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.pin || '—' }}</td>
              </tr>
              <tr style="border-top:1px solid #222;">
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Trong luong</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.trongLuongKg ? v.trongLuongKg + ' kg' : '—' }}</td>
                <td class="px-3 py-1 text-secondary" style="background:#161616;font-weight:600;">Phan loai</td>
                <td class="px-3 py-1 text-light" style="background:#1a1a1a;">{{ v.phanLoaiTen || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <!-- ══ MODAL KHACH HANG ══ -->
  <div v-if="showCustomerModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.65);z-index:1000;" @click.self="showCustomerModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:560px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingCustomerId?'Cap nhat khach hang':'Them khach hang moi' }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showCustomerModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="customerFormError" class="alert alert-danger small py-2 mb-3">{{ customerFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">Ho ten *</label><input v-model="customerForm.hoTen" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Dien thoai *</label><input v-model="customerForm.soDienThoai" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Email *</label><input v-model="customerForm.email" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Loai khach</label><select v-model="customerForm.loaiKhach" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="ca_nhan">Ca nhan</option><option value="doanh_nghiep">Doanh nghiep</option></select></div>
          <div class="col-12"><label class="form-label small text-secondary">Dia chi</label><input v-model="customerForm.diaChi" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Ten cong ty</label><input v-model="customerForm.tenCongTy" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Ma so thue</label><input v-model="customerForm.maSoThue" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Diem tich luy</label><input v-model="customerForm.diemTichLuy" type="number" min="0" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Trang thai</label><select v-model="customerForm.trangThai" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="active">Hoat dong</option><option value="inactive">Khoa</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showCustomerModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveCustomer">{{ editingCustomerId?'Cap nhat':'Them moi' }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL NHAN VIEN ══ -->
  <div v-if="showStaffModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.65);z-index:1000;" @click.self="showStaffModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:560px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingStaffId?'Cap nhat nhan vien':'Them nhan vien moi' }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showStaffModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="staffFormError" class="alert alert-danger small py-2 mb-3">{{ staffFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">Ho ten *</label><input v-model="staffForm.hoTen" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Dien thoai *</label><input v-model="staffForm.soDienThoai" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Email *</label><input v-model="staffForm.email" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Chuc vu *</label><select v-model="staffForm.chucVuId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null" disabled>-- Chon chuc vu --</option><option v-for="cv in chucVuList" :key="cv.id" :value="cv.id">{{ cv.tenChucVu }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Username *</label><input v-model="staffForm.username" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Mat khau {{ editingStaffId?'(de trong neu khong doi)':'*' }}</label><input v-model="staffForm.matKhauHash" type="password" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Luong co ban *</label><input v-model="staffForm.luongCoBan" type="number" min="0" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Trang thai</label><select v-model="staffForm.trangThai" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="active">Hoat dong</option><option value="inactive">Nghi viec</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showStaffModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveStaff">{{ editingStaffId?'Cap nhat':'Them moi' }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL KHUYEN MAI ══ -->
  <div v-if="showPromoModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.65);z-index:1000;" @click.self="showPromoModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:620px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingPromoId?'Cap nhat khuyen mai':'Them khuyen mai moi' }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showPromoModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="promoFormError" class="alert alert-danger small py-2 mb-3">{{ promoFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">Ma khuyen mai *</label><input v-model="promoForm.maKhuyenMai" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Ten khuyen mai *</label><input v-model="promoForm.tenKhuyenMai" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Loai</label><select v-model="promoForm.loai" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="percent">Phan tram (%)</option><option value="fixed">So tien (VND)</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Gia tri {{ promoForm.loai==='percent'?'(%)':'(VND)' }}</label><input v-model="promoForm.giaTri" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Giam toi da (VND)</label><input v-model="promoForm.giaTriToiDa" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Don hang toi thieu (VND)</label><input v-model="promoForm.donHangToiThieu" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Ngay bat dau *</label><input v-model="promoForm.ngayBatDau" type="datetime-local" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Ngay ket thuc *</label><input v-model="promoForm.ngayKetThuc" type="datetime-local" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">So luong toi da</label><input v-model="promoForm.soLuongToiDa" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Trang thai</label><select v-model="promoForm.trangThai" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="active">Hoat dong</option><option value="inactive">Ngung</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showPromoModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="savePromo">{{ editingPromoId?'Cap nhat':'Them moi' }}</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL THEM SAN PHAM CHI TIET ══ -->
  <div v-if="showAddItemDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.85);z-index:1070;" @click.self="showAddItemDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#111;border:1px solid rgba(255,255,255,0.1);width:960px;max-width:97vw;max-height:93vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid #1e1e1e;">
        <span class="text-secondary" style="font-size:0.8rem;">Chon san pham de them vao don hang</span>
        <button class="btn-close btn-close-white btn-sm" @click="showAddItemDetailModal=false"></button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto flex-grow-1 p-0" v-if="addItemDetailGroup">
        <div class="d-flex" style="min-height:400px;">

          <!-- Left: Anh san pham -->
          <div class="d-flex flex-column align-items-center justify-content-center p-4"
               style="width:42%;background:#0d0d0d;border-right:1px solid #1e1e1e;flex-shrink:0;">
            <div style="width:100%;max-width:320px;aspect-ratio:4/3;display:flex;align-items:center;justify-content:center;background:#141414;border-radius:12px;overflow:hidden;padding:16px;">
              <img v-if="(addItemCurrentVariant || addItemDetailGroup.variants[0])?.hinhAnhChinh"
                   :src="(addItemCurrentVariant || addItemDetailGroup.variants[0]).hinhAnhChinh"
                   style="max-width:100%;max-height:100%;object-fit:contain;" />
              <span v-else style="font-size:4rem;">💻</span>
            </div>
            <div class="mt-3 d-flex gap-1 flex-wrap justify-content-center">
              <span v-for="tag in (addItemDetailGroup.variants[0]?.phanLoaiTen||'').split(',').filter(Boolean)"
                    :key="tag" class="badge" style="background:#2a2a00;color:#facc15;font-size:0.7rem;">{{ tag.trim() }}</span>
            </div>
          </div>

          <!-- Right: Thong tin + chon bien the -->
          <div class="d-flex flex-column p-4 overflow-y-auto flex-grow-1">
            <!-- Ten + thuong hieu -->
            <div class="text-secondary mb-1" style="font-size:0.78rem;">
              {{ addItemDetailGroup.variants[0]?.tenThuongHieu }} · {{ addItemDetailGroup.variants[0]?.tenDanhMuc }}
            </div>
            <h5 class="fw-bold text-light mb-2">{{ addItemDetailGroup.tenSanPham }}</h5>
            <div class="mb-3" style="font-size:1.4rem;font-weight:700;color:#facc15;">
              {{ addItemCurrentVariant ? formatPrice(addItemCurrentVariant.giaBan) : formatPrice(addItemDetailGroup.minPrice) }}
              <span class="text-secondary ms-2" style="font-size:0.8rem;font-weight:400;">🚚 Giao nhanh 2H · Mien phi tu 300K</span>
            </div>

            <!-- Chon phien ban (CPU + RAM + Storage) -->
            <div v-if="addItemConfigs.length > 1" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">
                PHIEN BAN ({{ addItemConfigs.length }} CAU HINH)
              </div>
              <div class="d-flex flex-wrap gap-2">
                <button v-for="cfg in addItemConfigs" :key="cfg.key"
                        @click="selectConfig(cfg.key)"
                        class="btn btn-sm text-start"
                        style="padding:8px 12px;border-radius:8px;min-width:140px;"
                        :style="addItemSelectedConfig === cfg.key
                          ? 'background:#1e1a00;border:2px solid #facc15;color:#facc15;'
                          : 'background:#1a1a1a;border:1px solid #333;color:#aaa;'">
                  <div style="font-size:0.78rem;font-weight:600;">{{ cfg.cpu || 'Tieu chuan' }}</div>
                  <div style="font-size:0.68rem;">{{ [cfg.ram, cfg.oCung].filter(Boolean).join(' · ') }}</div>
                </button>
              </div>
            </div>

            <!-- Chon mau sac -->
            <div v-if="addItemColorsForConfig.length > 0" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">MAU SAC</div>
              <div class="d-flex flex-wrap gap-2">
                <button v-for="v in addItemColorsForConfig" :key="v.bienTheId"
                        @click="addItemSelectedColor = v.mauSac"
                        class="btn btn-sm"
                        style="padding:6px 14px;border-radius:8px;"
                        :style="addItemSelectedColor === v.mauSac
                          ? 'background:#1e1a00;border:2px solid #facc15;color:#facc15;'
                          : 'background:#1a1a1a;border:1px solid #333;color:#ccc;'">
                  <div style="font-size:0.78rem;font-weight:600;">{{ v.mauSac }}</div>
                  <div style="font-size:0.7rem;color:#facc15;">{{ formatPrice(v.giaBan) }}</div>
                </button>
              </div>
            </div>

            <!-- Thong tin chon -->
            <div v-if="addItemCurrentVariant" class="mb-3 py-2 px-3 rounded-3" style="background:#1a1a1a;font-size:0.8rem;">
              <span class="text-secondary">Mau: </span>
              <strong class="text-light">{{ addItemCurrentVariant.mauSac }}</strong>
              <span class="mx-2 text-secondary">·</span>
              <span class="text-secondary">Bao hanh: </span>
              <strong class="text-light">{{ addItemCurrentVariant.baoHanhThang ? addItemCurrentVariant.baoHanhThang + ' thang' : '—' }}</strong>
              <span class="mx-2 text-secondary">·</span>
              <span class="text-secondary">SKU: </span>
              <span class="text-light" style="font-family:monospace;font-size:0.75rem;">{{ addItemCurrentVariant.maSku }}</span>
            </div>

            <!-- Thong so ky thuat -->
            <div v-if="addItemCurrentVariant" class="mb-3">
              <div class="text-secondary mb-2" style="font-size:0.72rem;font-weight:700;letter-spacing:.05em;">THONG SO KY THUAT</div>
              <table style="width:100%;font-size:0.78rem;border-collapse:collapse;">
                <tr v-for="([label, val]) in [
                  ['Bo xu ly (CPU)', addItemCurrentVariant.cpu],
                  ['RAM', addItemCurrentVariant.ram],
                  ['O cung', addItemCurrentVariant.oCung],
                  ['Card do hoa', addItemCurrentVariant.gpu],
                  ['Man hinh', addItemCurrentVariant.kichThuocManHinh],
                  ['He dieu hanh', addItemCurrentVariant.heDieuHanh],
                  ['Pin', addItemCurrentVariant.pin],
                  ['Trong luong', addItemCurrentVariant.trongLuongKg ? addItemCurrentVariant.trongLuongKg + ' kg' : null],
                ].filter(([,v]) => v)" :key="label" style="border-top:1px solid #1e1e1e;">
                  <td class="py-1 text-secondary" style="padding-left:0;width:44%;">{{ label }}</td>
                  <td class="py-1 text-light fw-semibold">{{ val }}</td>
                </tr>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer: so luong + them -->
      <div class="px-4 py-3 d-flex align-items-center gap-3" style="border-top:1px solid #1e1e1e;background:#0d0d0d;">
        <span class="text-secondary" style="font-size:0.85rem;">So luong:</span>
        <input v-model.number="addItemQty" type="number" min="1" max="99"
               class="form-control form-control-sm"
               style="width:80px;background:#1e1e1e;color:#e0e0e0;border-color:#444;" />
        <button class="btn btn-warning flex-grow-1 fw-bold" style="font-size:0.9rem;"
                :disabled="!addItemCurrentVariant || addItemQty < 1 || addItemLoading"
                @click="confirmAddFromDetail">
          {{ addItemLoading ? 'Dang them...' : 'Them vao don hang' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET DON HANG ══ -->
  <div v-if="showOrderDetailModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.75);z-index:1050;" @click.self="showOrderDetailModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.12);width:720px;max-width:96vw;max-height:90vh;">

      <!-- Header -->
      <div class="d-flex justify-content-between align-items-center px-4 py-3" style="border-bottom:1px solid #252525;">
        <div>
          <div class="fw-bold text-white" style="font-size:0.95rem;">
            Don hang #{{ orderDetailData?.donHangId }}
            <span v-if="orderDetailData?.maDonHang" class="text-secondary ms-2" style="font-size:0.8rem;font-family:monospace;">{{ orderDetailData.maDonHang }}</span>
          </div>
          <div class="text-secondary" style="font-size:0.78rem;">
            {{ customerName(orderDetailData?.khachHangId) }} · {{ formatDate(orderDetailData?.ngayDat) }}
          </div>
        </div>
        <button class="btn-close btn-close-white btn-sm" @click="showOrderDetailModal=false"></button>
      </div>

      <!-- Toan bo body scroll cung nhau -->
      <div class="overflow-y-auto flex-grow-1">
      <!-- Danh sach san pham trong don -->
      <div class="p-3">
        <div v-if="orderDetailLoading" class="text-secondary small text-center py-4">Dang tai...</div>
        <div v-else-if="orderDetailItems.length === 0" class="text-secondary small text-center py-4">Khong co san pham nao</div>
        <table v-else class="w-100 mb-0" style="border-collapse:collapse;font-size:0.82rem;">
          <thead>
            <tr style="background:#222;">
              <th class="px-3 py-2 text-secondary" style="font-weight:600;width:38%;">San pham</th>
              <th class="px-3 py-2 text-secondary" style="font-weight:600;width:16%;font-family:monospace;">SKU</th>
              <th class="px-3 py-2 text-secondary text-center" style="font-weight:600;width:8%;">SL</th>
              <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:14%;">Don gia</th>
              <th class="px-3 py-2 text-secondary text-end" style="font-weight:600;width:14%;">Thanh tien</th>
              <th class="px-3 py-2 text-secondary" style="font-weight:600;width:10%;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in orderDetailItems" :key="item.id" style="border-top:1px solid #252525;">
              <td class="px-3 py-2">
                <div class="d-flex align-items-center gap-2">
                  <img v-if="productByBienThe(item.bienTheId)?.hinhAnhChinh"
                       :src="productByBienThe(item.bienTheId).hinhAnhChinh"
                       style="width:36px;height:28px;object-fit:contain;border-radius:4px;background:#111;flex-shrink:0;" />
                  <span v-else style="font-size:1.2rem;flex-shrink:0;">💻</span>
                  <span class="text-light">{{ productByBienThe(item.bienTheId)?.tenSanPham || '—' }}</span>
                </div>
              </td>
              <td class="px-3 py-2 text-secondary" style="font-family:monospace;font-size:0.75rem;">{{ item.maSku }}</td>
              <td class="px-3 py-2 text-center text-white fw-bold">{{ item.soLuong }}</td>
              <td class="px-3 py-2 text-end text-secondary">{{ formatPrice(item.donGia) }}</td>
              <td class="px-3 py-2 text-end text-warning fw-semibold">{{ formatPrice(item.thanhTien) }}</td>
              <td class="px-3 py-2">
                <div class="d-flex gap-1 justify-content-center">
                  <button v-if="productByBienThe(item.bienTheId)"
                          class="btn btn-sm btn-outline-secondary"
                          style="font-size:0.72rem;padding:2px 6px;"
                          @click="openVariantDetail(item.bienTheId)">
                    Chi tiet
                  </button>
                  <button class="btn btn-sm btn-outline-danger"
                          style="font-size:0.72rem;padding:2px 6px;"
                          @click="removeItemFromOrder(item.id)">
                    Xoa
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Footer: tong ket -->
      <div v-if="orderDetailData" class="px-4 py-3 d-flex flex-column gap-1" style="border-top:1px solid #252525;background:#141414;">
        <div class="d-flex justify-content-between small text-secondary">
          <span>Tam tinh</span><span>{{ formatPrice(orderDetailData.tongTien) }}</span>
        </div>
        <div v-if="orderDetailData.giamGia > 0" class="d-flex justify-content-between small text-success">
          <span>Giam gia</span><span>− {{ formatPrice(orderDetailData.giamGia) }}</span>
        </div>
        <div class="d-flex justify-content-between small text-secondary">
          <span>Phi van chuyen</span>
          <span :class="orderDetailData.phiVanChuyen === 0 ? 'text-success' : ''">
            {{ orderDetailData.phiVanChuyen === 0 ? 'Mien phi' : formatPrice(orderDetailData.phiVanChuyen) }}
          </span>
        </div>
        <div class="d-flex justify-content-between fw-bold pt-2 mt-1" style="border-top:1px solid #2a2a2a;">
          <span class="text-white">Thanh tien</span>
          <span class="text-warning" style="font-size:1rem;">{{ formatPrice(orderDetailData.thanhTien) }}</span>
        </div>
        <div class="d-flex justify-content-between small mt-2 pt-2" style="border-top:1px solid #1e1e1e;">
          <span class="text-secondary">Trang thai don</span>
          <span class="badge" :style="{ background: orderStatusColor(orderDetailData.trangThaiDonHang).bg, color: orderStatusColor(orderDetailData.trangThaiDonHang).text }">
            {{ orderStatusLabel(orderDetailData.trangThaiDonHang) }}
          </span>
        </div>
        <div class="d-flex justify-content-between small">
          <span class="text-secondary">Thanh toan</span>
          <span class="badge" :class="orderDetailData.trangThaiThanhToan==='paid'?'bg-success':'bg-secondary'">
            {{ orderDetailData.trangThaiThanhToan }}
          </span>
        </div>

        <!-- Them san pham moi vao don -->
        <div class="mt-3 pt-2" style="border-top:1px solid #222;">
          <div class="d-flex align-items-center justify-content-between mb-2" style="cursor:pointer;"
               @click="addItemMode = !addItemMode; addItemBienTheId = ''; addItemSelectedSpId = null; addItemSearch = ''">
            <span class="fw-semibold" style="font-size:0.85rem;color:#e0e0e0;">Them san pham moi</span>
            <span style="color:#888;font-size:0.75rem;">{{ addItemMode ? '▲' : '▼' }}</span>
          </div>

          <div v-if="addItemMode">
            <!-- Tim kiem -->
            <input v-model="addItemSearch" type="text" placeholder="Tim san pham..."
                   class="form-control form-control-sm mb-3"
                   style="background:#1e1e1e;color:#e0e0e0;border-color:#444;font-size:0.8rem;" />

            <!-- Grid san pham -->
            <div class="d-grid gap-2 mb-2" style="grid-template-columns:repeat(3,1fr);max-height:280px;overflow-y:auto;">
              <div v-for="g in addItemProductGroups" :key="g.sanPhamId"
                   @click="openAddItemDetail(g)"
                   class="rounded-3 overflow-hidden"
                   style="cursor:pointer;border:1px solid #2a2a2a;background:#1a1a1a;transition:border-color .15s;"
                   @mouseenter="$event.currentTarget.style.borderColor='#facc15'"
                   @mouseleave="$event.currentTarget.style.borderColor='#2a2a2a'">
                <div style="background:#111;height:80px;display:flex;align-items:center;justify-content:center;overflow:hidden;">
                  <img v-if="g.hinhAnhChinh" :src="g.hinhAnhChinh"
                       style="max-height:76px;max-width:100%;object-fit:contain;" />
                  <span v-else style="font-size:1.8rem;">💻</span>
                </div>
                <div class="px-2 py-1">
                  <div class="fw-semibold text-light" style="font-size:0.72rem;line-height:1.3;
                       display:-webkit-box;-webkit-line-clamp:2;line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
                    {{ g.tenSanPham }}
                  </div>
                  <div class="text-secondary" style="font-size:0.65rem;">{{ g.tenThuongHieu }}</div>
                  <div style="font-size:0.72rem;color:#facc15;font-weight:600;margin-top:2px;">
                    Tu {{ formatPrice(g.minPrice) }}
                  </div>
                  <div v-if="g.phanLoaiTen" class="mt-1">
                    <span v-for="tag in (g.phanLoaiTen||'').split(',').filter(Boolean)" :key="tag"
                          class="badge bg-secondary me-1" style="font-size:0.58rem;">{{ tag.trim() }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="text-secondary text-center py-1" style="font-size:0.75rem;">Nhan vao san pham de chon phien ban</div>
          </div>
        </div>

        <!-- Canh bao gop don: chi 1 nut, tu dong gop tat ca don cung ngay -->
        <div v-if="mergeCandidates.length > 0" class="mt-2 pt-2 d-flex align-items-center justify-content-between gap-2"
             style="border-top:1px solid #222;background:#1a1500;border-radius:6px;padding:8px 12px;">
          <span style="font-size:0.78rem;color:#fbbf24;">
            Co {{ mergeCandidates.length }} don hang cung ngay cua khach nay
            <span class="text-secondary ms-1">(#{{ mergeCandidates.map(o => o.donHangId).join(', #') }})</span>
          </span>
          <button class="btn btn-sm btn-warning flex-shrink-0" style="font-size:0.78rem;padding:3px 10px;"
                  :disabled="mergeLoading"
                  @click="autoMergeOrders">
            {{ mergeLoading ? '...' : 'Gop tat ca' }}
          </button>
        </div>
      </div>
      </div><!-- end outer scroll wrapper -->
    </div>
  </div>

  <!-- ══ MODAL TRANG THAI DON HANG ══ -->
  <div v-if="showOrderModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.65);z-index:1000;" @click.self="showOrderModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:460px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>Cap nhat trang thai don hang</span>
        <button class="btn-close btn-close-white btn-sm" @click="showOrderModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="orderStatusError" class="alert alert-danger small py-2 mb-3">{{ orderStatusError }}</div>
        <div v-if="editingOrder" class="small p-2 rounded-2 mb-3 text-secondary" style="background:rgba(255,255,255,0.04);">
          Don hang #{{ editingOrder.donHangId }} — Khach: <strong>{{ customerName(editingOrder.khachHangId) }}</strong>
        </div>
        <div class="d-flex flex-column gap-3">
          <div><label class="form-label small text-secondary">Trang thai don hang</label><select v-model="orderStatusForm.trangThaiDonHang" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="pending">Cho xac nhan</option><option value="confirmed">Da xac nhan</option><option value="processing">Dang xu ly</option><option value="shipping">Dang giao</option><option value="delivered">Da giao</option><option value="cancelled">Huy</option><option value="returned">Hoan tra</option></select></div>
          <div><label class="form-label small text-secondary">Trang thai thanh toan</label><select v-model="orderStatusForm.trangThaiThanhToan" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="unpaid">Chua thanh toan</option><option value="partial">Thanh toan mot phan</option><option value="paid">Da thanh toan</option><option value="refunded">Hoan tien</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showOrderModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveOrderStatus">Luu</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL TON KHO ══ -->
  <div v-if="showStockModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.65);z-index:1000;" @click.self="showStockModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:420px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>Cap nhat ton kho</span>
        <button class="btn-close btn-close-white btn-sm" @click="showStockModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="editingStock" class="small p-2 rounded-2 mb-3 text-secondary" style="background:rgba(255,255,255,0.04);">
          {{ editingStock.bienThe?.sanPham?.tenSanPham??'—' }} — SKU: <strong>{{ editingStock.bienThe?.maSku??'—' }}</strong>
        </div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">So luong ton</label><input v-model="stockForm.soLuongTon" type="number" min="0" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Dang giu</label><input v-model="stockForm.soLuongGiu" type="number" min="0" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-12"><label class="form-label small text-secondary">Ton kho toi thieu (canh bao)</label><input v-model="stockForm.tonKhoToiThieu" type="number" min="0" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showStockModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveStock">Luu</button>
      </div>
    </div>
  </div>

  <!-- ══ MODAL CHI TIET SERIAL ══ -->
  <div v-if="showStockDetailModal"
       class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:rgba(0,0,0,0.7);z-index:1060;"
       @click.self="showStockDetailModal=false">
    <div class="rounded-4 d-flex flex-column"
         style="background:#181818;border:1px solid rgba(255,255,255,0.12);width:760px;max-width:96vw;max-height:88vh;">

      <!-- Header -->
      <div class="d-flex align-items-start justify-content-between px-4 py-3" style="border-bottom:1px solid #252525;">
        <div>
          <div class="fw-bold text-white" style="font-size:0.95rem;">
            Chi tiet serial — {{ stockDetailItem?.bienThe?.maSku || '—' }}
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
              <span class="fw-bold text-white" style="font-size:1.2rem;">{{ stockDetailSerials.filter(s=>s.trangThai==='trong_kho').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">Trong kho</div>
          </div>
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#94a3b8;flex-shrink:0;"></span>
              <span class="fw-bold text-white" style="font-size:1.2rem;">{{ stockDetailSerials.filter(s=>s.trangThai==='da_ban').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">Da ban</div>
          </div>
          <div class="text-center">
            <div class="d-flex align-items-center justify-content-center gap-1">
              <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#fb923c;flex-shrink:0;"></span>
              <span class="fw-bold text-white" style="font-size:1.2rem;">{{ stockDetailSerials.filter(s=>s.trangThai==='loi_bao_hanh').length }}</span>
            </div>
            <div class="text-secondary" style="font-size:0.7rem;">Dang bao hanh</div>
          </div>
          <button class="btn-close btn-close-white btn-sm ms-2" @click="showStockDetailModal=false"></button>
        </div>
      </div>

      <!-- Serial list -->
      <div class="overflow-y-auto flex-grow-1">
        <div v-if="stockDetailLoading" class="text-secondary small text-center py-5">Dang tai...</div>
        <div v-else-if="stockDetailSerials.length === 0" class="text-secondary small text-center py-5">Chua co serial nao</div>
        <table v-else class="w-100" style="border-collapse:collapse;font-size:0.82rem;">
          <thead>
            <tr style="background:#1e1e1e;position:sticky;top:0;">
              <th class="px-4 py-2 text-secondary" style="font-weight:500;width:40px;">#</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">So serial</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">Ngay nhap</th>
              <th class="px-4 py-2 text-secondary" style="font-weight:500;">Trang thai</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(s, idx) in stockDetailSerials" :key="s.chiTietId"
                style="border-top:1px solid #222;">
              <td class="px-4 py-2 text-secondary">{{ idx + 1 }}</td>
              <td class="px-4 py-2 text-white fw-semibold" style="font-family:monospace;">{{ s.soSerial }}</td>
              <td class="px-4 py-2 text-secondary">{{ formatDate(s.ngayNhapKho) }}</td>
              <td class="px-4 py-2">
                <span
                  style="display:inline-block;width:10px;height:10px;border-radius:50%;"
                  :style="{ background: stockDetailStatusColor(s.trangThai) }"
                  :title="stockDetailStatusLabel(s.trangThai)"
                ></span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* CSS toi thieu cho nhung gi Bootstrap khong the thay the */

/* Nav item: hover va active state voi mau vang dac trung */
.adm-nav {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 7px;
  cursor: pointer;
  font-size: 0.87rem;
  color: #ccc;
  transition: background 0.12s, color 0.12s;
  user-select: none;
}
.adm-nav:hover { background: rgba(255,255,255,0.06); color: #f0f0f0; }
.adm-nav.active { background: rgba(244,194,0,0.12); color: #f4c200; }
.adm-nav.active .adm-icon { opacity: 1; }

/* Icon trong nav */
.adm-icon { width: 17px; height: 17px; flex-shrink: 0; opacity: 0.75; }

/* Tieu de phan nhom trong sidebar */
.adm-nav-label {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #555;
  text-transform: uppercase;
  padding: 10px 8px 3px;
}

/* Row do trong bang ton kho khi san pham sap het */
.row-warn td { background: rgba(224,82,82,0.06) !important; }

/* Layout POS: 2 cot, chiem toan bo chieu cao */
.pos-grid-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 18px;
  height: calc(100vh - 120px);
}
</style>
