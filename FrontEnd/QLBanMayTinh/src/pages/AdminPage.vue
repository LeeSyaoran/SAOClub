<script setup>
import { ref, computed, onMounted, reactive } from "vue";

// ── Navigation ───────────────────────────────────────────────────────────────
const currentRole = ref("admin");
const currentPage = ref("dashboard");
const navigate = (page) => {
  currentPage.value = page;
};
const switchRole = (role) => {
  currentRole.value = role;
  currentPage.value = role === "admin" ? "dashboard" : "user-home";
};
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

// ── User ─────────────────────────────────────────────────────────────────────
const userDisplayName = ref("Admin");
const userAvatar = computed(() =>
  userDisplayName.value.charAt(0).toUpperCase(),
);
const userDisplayRole = computed(() =>
  currentRole.value === "admin" ? "Quan tri vien" : "Nguoi dung",
);

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
    (t) =>
      t.soLuongTon != null &&
      t.tonKhoToiThieu != null &&
      t.soLuongTon <= t.tonKhoToiThieu,
  ),
);

// ── Fetch ─────────────────────────────────────────────────────────────────────
const safeFetch = async (url) => {
  try {
    const res = await fetch(url);
    if (!res.ok) return [];
    return await res.json();
  } catch {
    return [];
  }
};

const fetchAll = async () => {
  loading.value = true;
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
    safeFetch("/api/san-pham/hien-thi"),
    safeFetch("/api/don-hang"),
    safeFetch("/api/khach-hang"),
    safeFetch("/api/nhan-vien"),
    safeFetch("/api/khuyen-mai"),
    safeFetch("/api/ton-kho"),
    safeFetch("/api/danh-muc"),
    safeFetch("/api/thuong-hieu"),
    safeFetch("/api/nha-cung-cap"),
    safeFetch("/api/chuc-vu"),
    safeFetch("/api/dm-cpu"),
    safeFetch("/api/dm-ram"),
    safeFetch("/api/dm-o-cung"),
    safeFetch("/api/dm-gpu"),
  ]);
  loading.value = false;
};

// ── Products CRUD ─────────────────────────────────────────────────────────────
const showProductModal = ref(false);
const editingId = ref(null);
const formError = ref("");

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
});
const form = reactive(emptyForm());

const openAdd = () => {
  Object.assign(form, emptyForm());
  editingId.value = null;
  formError.value = "";
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
  });
  editingId.value = p.sanPhamId;
  formError.value = "";
  showProductModal.value = true;
};
const saveProduct = async () => {
  formError.value = "";
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
    ngayTao: new Date().toISOString().slice(0, 19),
  };
  try {
    const url = editingId.value
      ? `/api/san-pham/update/${editingId.value}`
      : "/api/san-pham";
    const method = editingId.value ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      formError.value = `Loi: ${res.status} ${await res.text()}`;
      return;
    }
    showProductModal.value = false;
    products.value = await safeFetch("/api/san-pham/hien-thi");
  } catch (e) {
    formError.value = e.message;
  }
};
const deleteProduct = async (id) => {
  if (!confirm("Ban co chac muon xoa san pham nay?")) return;
  const res = await fetch(`/api/san-pham/delete/${id}`, { method: "DELETE" });
  if (!res.ok) {
    alert(`Xoa that bai: ${res.status}`);
    return;
  }
  products.value = await safeFetch("/api/san-pham/hien-thi");
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
    const url = editingCustomerId.value
      ? `/api/khach-hang/update/${editingCustomerId.value}`
      : "/api/khach-hang";
    const method = editingCustomerId.value ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      customerFormError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showCustomerModal.value = false;
    customers.value = await safeFetch("/api/khach-hang");
  } catch (e) {
    customerFormError.value = e.message;
  }
};
const deleteCustomer = async (id) => {
  if (!confirm("Xoa khach hang nay?")) return;
  const res = await fetch(`/api/khach-hang/delete/${id}`, { method: "DELETE" });
  if (!res.ok) {
    alert(`Xoa that bai: ${res.status}`);
    return;
  }
  customers.value = await safeFetch("/api/khach-hang");
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
    const url = editingStaffId.value
      ? `/api/nhan-vien/update/${editingStaffId.value}`
      : "/api/nhan-vien";
    const method = editingStaffId.value ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      staffFormError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showStaffModal.value = false;
    staff.value = await safeFetch("/api/nhan-vien");
  } catch (e) {
    staffFormError.value = e.message;
  }
};
const deleteStaff = async (id) => {
  if (!confirm("Xoa nhan vien nay?")) return;
  const res = await fetch(`/api/nhan-vien/delete/${id}`, { method: "DELETE" });
  if (!res.ok) {
    alert(`Xoa that bai: ${res.status}`);
    return;
  }
  staff.value = await safeFetch("/api/nhan-vien");
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
    const url = editingPromoId.value
      ? `/api/khuyen-mai/update/${editingPromoId.value}`
      : "/api/khuyen-mai";
    const method = editingPromoId.value ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      promoFormError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showPromoModal.value = false;
    promotions.value = await safeFetch("/api/khuyen-mai");
  } catch (e) {
    promoFormError.value = e.message;
  }
};
const deletePromo = async (id) => {
  if (!confirm("Xoa khuyen mai nay?")) return;
  const res = await fetch(`/api/khuyen-mai/delete/${id}`, { method: "DELETE" });
  if (!res.ok) {
    alert(`Xoa that bai: ${res.status}`);
    return;
  }
  promotions.value = await safeFetch("/api/khuyen-mai");
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
        ?.soDienThoai ??
        ""),
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
    const res = await fetch(`/api/don-hang/update/${o.donHangId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      orderStatusError.value = `Loi ${res.status}: ${await res.text()}`;
      return;
    }
    showOrderModal.value = false;
    orders.value = await safeFetch("/api/don-hang");
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
    const res = await fetch(`/api/ton-kho/update/${item.tonKhoId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      alert(`Loi: ${res.status}`);
      return;
    }
    showStockModal.value = false;
    inventory.value = await safeFetch("/api/ton-kho");
  } catch (e) {
    alert(e.message);
  }
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
  if (!posCart.value.length) {
    posError.value = "Gio hang dang trong!";
    return;
  }
  if (!posPhone.value.trim()) {
    posError.value = "Vui long nhap SDT khach hang!";
    return;
  }
  posError.value = "";
  posSuccess.value = false;
  try {
    let khachHangId = posFoundCust.value?.khachHangId;
    if (!khachHangId) {
      const name = posNewName.value.trim() || posPhone.value.trim();
      const kRes = await fetch("/api/khach-hang", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          hoTen: name,
          soDienThoai: posPhone.value.trim(),
          email: "",
          diaChi: "Tai cua hang",
          loaiKhach: "ca_nhan",
          diemTichLuy: 0,
          trangThai: "active",
        }),
      });
      if (!kRes.ok) throw new Error(`Loi tao khach hang: ${await kRes.text()}`);
      const kh = await kRes.json();
      khachHangId = kh.khachHangId;
      customers.value = await safeFetch("/api/khach-hang");
    }
    const nguoiNhan =
      posFoundCust.value?.hoTen ??
      (posNewName.value.trim() || posPhone.value.trim());
    const orderRes = await fetch("/api/don-hang", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        khachHangId,
        nguoiNhan,
        sdtNguoiNhan: posPhone.value.trim(),
        diaChiGiaoHangText: posFoundCust.value?.diaChi ?? "Tai cua hang",
        tongTien: posCartTotal.value,
        giamGia: 0,
        phiVanChuyen: posFee.value,
        thanhTien: posGrandTotal.value,
        ngayDat: new Date().toISOString().slice(0, 19),
        trangThaiDonHang: "confirmed",
        trangThaiThanhToan: "paid",
        kenhBan: "in_store",
      }),
    });
    if (!orderRes.ok)
      throw new Error(`Loi tao don hang: ${await orderRes.text()}`);
    const created = await orderRes.json();
    const donHangId = created.id ?? created.donHangId;
    for (const item of posCart.value) {
      const ctRes = await fetch("/api/chi-tiet-don-hang", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          donHangId,
          bienTheId: item.bienTheId,
          soLuong: item.soLuong,
          donGia: item.giaBan,
          giamGiaDong: 0,
        }),
      });
      if (!ctRes.ok)
        throw new Error(`Loi them san pham: ${await ctRes.text()}`);
    }
    posSuccess.value = true;
    posCart.value = [];
    posPhone.value = "";
    posFoundCust.value = null;
    posNewName.value = "";
    orders.value = await safeFetch("/api/don-hang");
  } catch (e) {
    posError.value = e.message;
  }
};

onMounted(fetchAll);
</script>

<template>
  <div class="dashboard-shell">
    <!-- ── Sidebar ── -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="logo-mark">SAO</div>
        <div class="logo-text">
          <div class="logo-name">SAOPhone</div>
          <div class="logo-sub">He thong quan ly</div>
        </div>
      </div>

      <div class="role-switch">
        <button
          class="role-btn"
          :class="{ active: currentRole === 'admin' }"
          @click="switchRole('admin')"
        >
          Admin
        </button>
        <button
          class="role-btn"
          :class="{ active: currentRole === 'user' }"
          @click="switchRole('user')"
        >
          Nhân viên
        </button>
      </div>

      <nav class="nav-group" v-show="currentRole === 'admin'">
        <div class="nav-label">Tong quan</div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'dashboard' }"
          @click="navigate('dashboard')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              d="M3 4a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1V4zM3 11a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1H4a1 1 0 01-1-1v-3zm7 0a1 1 0 011-1h3a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-3z"
            />
          </svg>
          Dashboard
        </div>

        <div class="nav-label">Quan ly</div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'products' }"
          @click="navigate('products')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 8a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zm6-6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zm0 8a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
            />
          </svg>
          San pham
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'orders' }"
          @click="navigate('orders')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" />
            <path
              fill-rule="evenodd"
              d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z"
              clip-rule="evenodd"
            />
          </svg>
          Don hang
          <span class="nav-badge">{{ totalOrders }}</span>
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'customers' }"
          @click="navigate('customers')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"
            />
          </svg>
          Khach hang
          <span class="nav-badge">{{ totalCustomers }}</span>
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'inventory' }"
          @click="navigate('inventory')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path d="M4 3a2 2 0 100 4h12a2 2 0 100-4H4z" />
            <path
              fill-rule="evenodd"
              d="M3 8h14v7a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm5 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z"
              clip-rule="evenodd"
            />
          </svg>
          Kho hang
          <span
            v-if="lowStockItems.length"
            class="nav-badge"
            style="background: #e05252"
            >{{ lowStockItems.length }}</span
          >
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'promotions' }"
          @click="navigate('promotions')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M5 5a3 3 0 015-2.236A3 3 0 0114.83 6H16a2 2 0 110 4h-5V9a1 1 0 10-2 0v1H4a2 2 0 110-4h1.17C5.06 5.687 5 5.35 5 5zm4 1V5a1 1 0 10-1 1h1zm3 0a1 1 0 10-1-1v1h1z"
              clip-rule="evenodd"
            />
            <path d="M9 11H3v5a2 2 0 002 2h4v-7zm2 7h4a2 2 0 002-2v-5h-6v7z" />
          </svg>
          Khuyen mai
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'ban-hang' }"
          @click="navigate('ban-hang')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C4.328 11.142 4 11.574 4 12a2 2 0 002 2h10a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 4H6.28l-.31-1.243A1 1 0 005 2H3z"
            />
            <path
              d="M16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"
            />
          </svg>
          Ban hang
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'staff' }"
          @click="navigate('staff')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
              clip-rule="evenodd"
            />
          </svg>
          Nhan vien
        </div>

        <div class="nav-label">Phan tich</div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'reports' }"
          @click="navigate('reports')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zm6-4a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-3a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"
            />
          </svg>
          Bao cao
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'settings' }"
          @click="navigate('settings')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z"
              clip-rule="evenodd"
            />
          </svg>
          Cai dat
        </div>
      </nav>

      <nav class="nav-group" v-show="currentRole === 'user'">
        <div class="nav-label">Trang cua toi</div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'user-home' }"
          @click="navigate('user-home')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"
            />
          </svg>
          Trang chu
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'user-orders' }"
          @click="navigate('user-orders')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" />
            <path
              fill-rule="evenodd"
              d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5z"
              clip-rule="evenodd"
            />
          </svg>
          Don hang cua toi
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'user-browse' }"
          @click="navigate('user-browse')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M10 2a4 4 0 00-4 4v1H5a1 1 0 00-.994.89l-1 9A1 1 0 004 18h12a1 1 0 00.994-1.11l-1-9A1 1 0 0015 7h-1V6a4 4 0 00-4-4zm2 5V6a2 2 0 10-4 0v1h4zm-6 3a1 1 0 112 0 1 1 0 01-2 0zm7-1a1 1 0 100 2 1 1 0 000-2z"
              clip-rule="evenodd"
            />
          </svg>
          Mua sam
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'user-warranty' }"
          @click="navigate('user-warranty')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
              clip-rule="evenodd"
            />
          </svg>
          Bao hanh
        </div>
        <div
          class="nav-item"
          :class="{ active: currentPage === 'user-profile' }"
          @click="navigate('user-profile')"
        >
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
              clip-rule="evenodd"
            />
          </svg>
          Ho so
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card">
          <div class="avatar">{{ userAvatar }}</div>
          <div class="user-info">
            <div class="user-name">{{ userDisplayName }}</div>
            <div class="user-role">{{ userDisplayRole }}</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- ── Main ── -->
    <main class="main">
      <div class="topbar">
        <div>
          <div class="page-title">{{ topbarTitle }}</div>
          <div class="page-breadcrumb">{{ topbarSub }}</div>
        </div>
        <div class="topbar-right">
          <div class="icon-btn">&#128276;</div>
        </div>
      </div>

      <div class="content">
        <!-- ── Dashboard ── -->
        <section v-show="currentPage === 'dashboard'" class="page">
          <div v-if="loading" class="loading-msg">Dang tai du lieu...</div>
          <template v-else>
            <div class="stat-grid">
              <div class="stat-card">
                <div class="stat-label">Tong san pham</div>
                <div class="stat-value">{{ totalProducts }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">Don hang</div>
                <div class="stat-value">{{ totalOrders }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">Khach hang</div>
                <div class="stat-value">{{ totalCustomers }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-label">Doanh thu</div>
                <div class="stat-value revenue">
                  {{ formatPrice(totalRevenue) }}
                </div>
              </div>
            </div>

            <div v-if="lowStockItems.length" class="alert-bar">
              &#9888; {{ lowStockItems.length }} bien the san pham sap het hang
            </div>

            <div class="section-title" style="margin-top: 24px">
              San pham gan day
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Ten san pham</th>
                    <th>Thuong hieu</th>
                    <th>Danh muc</th>
                    <th>Gia ban</th>
                    <th>Trang thai</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="p in products.slice(0, 5)" :key="p.sanPhamId">
                    <td>{{ p.tenSanPham }}</td>
                    <td>{{ p.tenThuongHieu }}</td>
                    <td>{{ p.tenDanhMuc }}</td>
                    <td>{{ formatPrice(p.giaBan) }}</td>
                    <td>
                      <span
                        class="badge"
                        :class="
                          p.trangThai === 'active'
                            ? 'badge-green'
                            : 'badge-gray'
                        "
                        >{{ statusLabel(p.trangThai) }}</span
                      >
                    </td>
                  </tr>
                  <tr v-if="products.length === 0">
                    <td colspan="5" class="empty-row">Chua co san pham</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </section>

        <!-- ── Products ── -->
        <section v-show="currentPage === 'products'" class="page">
          <div class="page-header">
            <span>{{ totalProducts }} san pham</span>
            <button class="btn-primary" @click="openAdd">
              + Them san pham
            </button>
          </div>
          <div v-if="loading" class="loading-msg">Dang tai...</div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Ten san pham</th>
                  <th>SKU</th>
                  <th>Thuong hieu</th>
                  <th>Danh muc</th>
                  <th>Gia ban</th>
                  <th>Trang thai</th>
                  <th>Thao tac</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in products" :key="p.sanPhamId">
                  <td>{{ p.tenSanPham }}</td>
                  <td class="text-dim">{{ p.maSku }}</td>
                  <td>{{ p.tenThuongHieu }}</td>
                  <td>{{ p.tenDanhMuc }}</td>
                  <td>{{ formatPrice(p.giaBan) }}</td>
                  <td>
                    <span
                      class="badge"
                      :class="
                        p.trangThai === 'active' ? 'badge-green' : 'badge-gray'
                      "
                      >{{ statusLabel(p.trangThai) }}</span
                    >
                  </td>
                  <td>
                    <div class="action-btns">
                      <button class="btn-edit" @click="openEdit(p)">Sua</button>
                      <button
                        class="btn-delete"
                        @click="deleteProduct(p.sanPhamId)"
                      >
                        Xoa
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="products.length === 0">
                  <td colspan="7" class="empty-row">Chua co san pham</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Orders ── -->
        <section v-show="currentPage === 'orders'" class="page">
          <div class="page-header">
            <span>{{ totalOrders }} don hang</span>
          </div>
          <div v-if="loading" class="loading-msg">Dang tai...</div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Ma don</th>
                  <th>Khach hang</th>
                  <th>Thanh tien</th>
                  <th>TT Don hang</th>
                  <th>TT Thanh toan</th>
                  <th>Ngay dat</th>
                  <th>Thao tac</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in orders" :key="o.donHangId">
                  <td class="text-dim">#{{ o.donHangId }}</td>
                  <td>{{ customerName(o.khachHangId) }}</td>
                  <td>{{ formatPrice(o.thanhTien) }}</td>
                  <td>
                    <span class="badge badge-blue">{{
                      o.trangThaiDonHang || "—"
                    }}</span>
                  </td>
                  <td>
                    <span
                      class="badge"
                      :class="
                        o.trangThaiThanhToan === 'paid'
                          ? 'badge-green'
                          : 'badge-gray'
                      "
                      >{{ o.trangThaiThanhToan || "—" }}</span
                    >
                  </td>
                  <td>{{ formatDate(o.ngayDat) }}</td>
                  <td>
                    <button class="btn-edit" @click="openOrderStatus(o)">
                      Cap nhat
                    </button>
                  </td>
                </tr>
                <tr v-if="orders.length === 0">
                  <td colspan="7" class="empty-row">Chua co don hang</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Customers ── -->
        <section v-show="currentPage === 'customers'" class="page">
          <div class="page-header">
            <span>{{ totalCustomers }} khach hang</span>
            <button class="btn-primary" @click="openAddCustomer">
              + Them khach hang
            </button>
          </div>
          <div v-if="loading" class="loading-msg">Dang tai...</div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Ho ten</th>
                  <th>Dien thoai</th>
                  <th>Email</th>
                  <th>Loai khach</th>
                  <th>Diem</th>
                  <th>Trang thai</th>
                  <th>Thao tac</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in customers" :key="c.khachHangId">
                  <td>{{ c.hoTen }}</td>
                  <td class="text-dim">{{ c.soDienThoai }}</td>
                  <td class="text-dim">{{ c.email }}</td>
                  <td>{{ c.loaiKhach || "—" }}</td>
                  <td>{{ c.diemTichLuy ?? 0 }}</td>
                  <td>
                    <span
                      class="badge"
                      :class="
                        c.trangThai === 'active' ? 'badge-green' : 'badge-gray'
                      "
                      >{{ statusLabel(c.trangThai) }}</span
                    >
                  </td>
                  <td>
                    <div class="action-btns">
                      <button class="btn-edit" @click="openEditCustomer(c)">
                        Sua
                      </button>
                      <button
                        class="btn-delete"
                        @click="deleteCustomer(c.khachHangId)"
                      >
                        Xoa
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="customers.length === 0">
                  <td colspan="7" class="empty-row">Chua co khach hang</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Inventory ── -->
        <section v-show="currentPage === 'inventory'" class="page">
          <div class="page-header">
            <span>{{ inventory.length }} bien the</span>
            <span v-if="lowStockItems.length" class="low-stock-warn"
              >&#9888; {{ lowStockItems.length }} sap het hang</span
            >
          </div>
          <div v-if="loading" class="loading-msg">Dang tai...</div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>San pham</th>
                  <th>SKU</th>
                  <th>Ton kho</th>
                  <th>Dang giu</th>
                  <th>Ton toi thieu</th>
                  <th>Cap nhat</th>
                  <th>Thao tac</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in inventory"
                  :key="item.tonKhoId"
                  :class="{
                    'row-warn':
                      item.soLuongTon != null &&
                      item.tonKhoToiThieu != null &&
                      item.soLuongTon <= item.tonKhoToiThieu,
                  }"
                >
                  <td>{{ item.bienThe?.sanPham?.tenSanPham ?? "—" }}</td>
                  <td class="text-dim">{{ item.bienThe?.maSku ?? "—" }}</td>
                  <td>
                    <strong>{{ item.soLuongTon ?? "—" }}</strong>
                  </td>
                  <td>{{ item.soLuongGiu ?? 0 }}</td>
                  <td>{{ item.tonKhoToiThieu ?? "—" }}</td>
                  <td class="text-dim">{{ formatDate(item.ngayCapNhat) }}</td>
                  <td>
                    <button class="btn-edit" @click="openEditStock(item)">
                      Cap nhat
                    </button>
                  </td>
                </tr>
                <tr v-if="inventory.length === 0">
                  <td colspan="7" class="empty-row">Chua co du lieu ton kho</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Promotions ── -->
        <section v-show="currentPage === 'promotions'" class="page">
          <div class="page-header">
            <span>{{ promotions.length }} khuyen mai</span>
            <button class="btn-primary" @click="openAddPromo">
              + Them khuyen mai
            </button>
          </div>
          <div v-if="loading" class="loading-msg">Dang tai...</div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Ma KM</th>
                  <th>Ten</th>
                  <th>Loai</th>
                  <th>Gia tri</th>
                  <th>Bat dau</th>
                  <th>Ket thuc</th>
                  <th>Da dung</th>
                  <th>Trang thai</th>
                  <th>Thao tac</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in promotions" :key="p.khuyenMaiId">
                  <td class="text-dim">{{ p.maKhuyenMai }}</td>
                  <td>{{ p.tenKhuyenMai }}</td>
                  <td>{{ p.loai === "percent" ? "Phan tram" : "So tien" }}</td>
                  <td>
                    {{
                      p.loai === "percent"
                        ? `${p.giaTri}%`
                        : formatPrice(p.giaTri)
                    }}
                  </td>
                  <td>{{ formatDate(p.ngayBatDau) }}</td>
                  <td>{{ formatDate(p.ngayKetThuc) }}</td>
                  <td>{{ p.soLanDaDung ?? 0 }}/{{ p.soLuongToiDa ?? "∞" }}</td>
                  <td>
                    <span
                      class="badge"
                      :class="
                        p.trangThai === 'active' ? 'badge-green' : 'badge-gray'
                      "
                      >{{ statusLabel(p.trangThai) }}</span
                    >
                  </td>
                  <td>
                    <div class="action-btns">
                      <button class="btn-edit" @click="openEditPromo(p)">
                        Sua
                      </button>
                      <button
                        class="btn-delete"
                        @click="deletePromo(p.khuyenMaiId)"
                      >
                        Xoa
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="promotions.length === 0">
                  <td colspan="9" class="empty-row">Chua co khuyen mai</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Staff ── -->
        <section v-show="currentPage === 'staff'" class="page">
          <div class="page-header">
            <span>{{ staff.length }} nhan vien</span>
            <button class="btn-primary" @click="openAddStaff">
              + Them nhan vien
            </button>
          </div>
          <div v-if="loading" class="loading-msg">Dang tai...</div>
          <div v-else class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Ho ten</th>
                  <th>Dien thoai</th>
                  <th>Email</th>
                  <th>Chuc vu</th>
                  <th>Username</th>
                  <th>Luong co ban</th>
                  <th>Trang thai</th>
                  <th>Thao tac</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in staff" :key="s.nhanVienId">
                  <td>{{ s.hoTen }}</td>
                  <td class="text-dim">{{ s.soDienThoai }}</td>
                  <td class="text-dim">{{ s.email }}</td>
                  <td>{{ chucVuName(s.chucVuId) }}</td>
                  <td class="text-dim">{{ s.username }}</td>
                  <td>{{ formatPrice(s.luongCoBan) }}</td>
                  <td>
                    <span
                      class="badge"
                      :class="
                        s.trangThai === 'active' ? 'badge-green' : 'badge-gray'
                      "
                      >{{ statusLabel(s.trangThai) }}</span
                    >
                  </td>
                  <td>
                    <div class="action-btns">
                      <button class="btn-edit" @click="openEditStaff(s)">
                        Sua
                      </button>
                      <button
                        class="btn-delete"
                        @click="deleteStaff(s.nhanVienId)"
                      >
                        Xoa
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="staff.length === 0">
                  <td colspan="8" class="empty-row">Chua co nhan vien</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Reports ── -->
        <section v-show="currentPage === 'reports'" class="page">
          <div class="stat-grid" style="margin-bottom: 24px">
            <div class="stat-card">
              <div class="stat-label">Tong doanh thu</div>
              <div class="stat-value revenue">
                {{ formatPrice(totalRevenue) }}
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">San pham dang ban</div>
              <div class="stat-value">{{ activeProducts }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">Khuyen mai dang chay</div>
              <div class="stat-value">{{ activePromos }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">Bien the sap het hang</div>
              <div
                class="stat-value"
                :style="lowStockItems.length ? 'color:#e05252' : ''"
              >
                {{ lowStockItems.length }}
              </div>
            </div>
          </div>

          <div class="section-title">Don hang theo trang thai</div>
          <div class="table-wrap" style="margin-bottom: 24px">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Trang thai</th>
                  <th>So luong</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in ordersByStatus" :key="row.status">
                  <td>
                    <span class="badge badge-blue">{{
                      row.status || "Chua co"
                    }}</span>
                  </td>
                  <td>
                    <strong>{{ row.count }}</strong>
                  </td>
                </tr>
                <tr v-if="ordersByStatus.length === 0">
                  <td colspan="2" class="empty-row">Chua co don hang</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="section-title">Top 5 san pham gia cao nhat</div>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Ten san pham</th>
                  <th>Thuong hieu</th>
                  <th>Gia ban</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(p, i) in [...products]
                    .sort((a, b) => (b.giaBan ?? 0) - (a.giaBan ?? 0))
                    .slice(0, 5)"
                  :key="p.sanPhamId"
                >
                  <td class="text-dim">{{ i + 1 }}</td>
                  <td>{{ p.tenSanPham }}</td>
                  <td>{{ p.tenThuongHieu }}</td>
                  <td>{{ formatPrice(p.giaBan) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ── Settings ── -->
        <section v-show="currentPage === 'settings'" class="page">
          <div class="settings-card">
            <div class="settings-title">Thong tin he thong</div>
            <div class="settings-row">
              <span>Ten he thong</span><span>SAOPhone Admin</span>
            </div>
            <div class="settings-row">
              <span>Phien ban</span><span>1.0.0</span>
            </div>
            <div class="settings-row">
              <span>Backend API</span><span>http://localhost:8080</span>
            </div>
            <div class="settings-row">
              <span>Database</span><span>SQL Server — QLBanMayTinh</span>
            </div>
            <div class="settings-row">
              <span>Trang thai</span
              ><span><span class="badge badge-green">Hoat dong</span></span>
            </div>
          </div>
        </section>

        <!-- ── Ban hang (POS) ── -->
        <section v-show="currentPage === 'ban-hang'" class="page">
          <div class="pos-layout">
            <!-- LEFT: product grid -->
            <div class="pos-products">
              <div class="pos-search-bar">
                <input
                  v-model="posSearch"
                  class="pos-search-input"
                  placeholder="Tim san pham theo ten hoac SKU..."
                />
              </div>
              <div v-if="loading" class="loading-msg">Dang tai...</div>
              <div v-else class="pos-grid">
                <div
                  v-for="p in posProducts"
                  :key="p.bienTheId"
                  class="pos-card"
                >
                  <div class="pos-card-name">{{ p.tenSanPham }}</div>
                  <div class="pos-card-sku text-dim">{{ p.maSku }}</div>
                  <div class="pos-card-brand text-dim">
                    {{ p.tenThuongHieu }} · {{ p.tenDanhMuc }}
                  </div>
                  <div class="pos-card-price">{{ formatPrice(p.giaBan) }}</div>
                  <button class="pos-add-btn" @click="posAddToCart(p)">
                    + Them vao gio
                  </button>
                </div>
                <div
                  v-if="posProducts.length === 0"
                  class="empty-row"
                  style="grid-column: 1/-1; padding: 32px; text-align: center"
                >
                  Khong tim thay san pham
                </div>
              </div>
            </div>

            <!-- RIGHT: cart -->
            <div class="pos-cart">
              <div class="pos-cart-header">
                Gio hang
                <span class="pos-cart-count"
                  >{{ posCart.length }} san pham</span
                >
              </div>

              <!-- cart items -->
              <div class="pos-cart-items">
                <div v-if="posCart.length === 0" class="pos-cart-empty">
                  Chua co san pham nao
                </div>
                <div
                  v-for="item in posCart"
                  :key="item.bienTheId"
                  class="pos-cart-item"
                >
                  <div class="pos-ci-info">
                    <div class="pos-ci-name">{{ item.tenSanPham }}</div>
                    <div class="pos-ci-sku text-dim">{{ item.maSku }}</div>
                  </div>
                  <div class="pos-ci-controls">
                    <button
                      class="pos-qty-btn"
                      @click="
                        item.soLuong > 1
                          ? item.soLuong--
                          : posRemove(item.bienTheId)
                      "
                    >
                      −
                    </button>
                    <span class="pos-qty">{{ item.soLuong }}</span>
                    <button class="pos-qty-btn" @click="item.soLuong++">
                      +
                    </button>
                    <button
                      class="pos-rm-btn"
                      @click="posRemove(item.bienTheId)"
                    >
                      ✕
                    </button>
                  </div>
                  <div class="pos-ci-price">
                    {{ formatPrice(item.giaBan * item.soLuong) }}
                  </div>
                </div>
              </div>

              <!-- totals -->
              <div class="pos-totals">
                <div class="pos-total-row">
                  <span>Tong hang:</span
                  ><span>{{ formatPrice(posCartTotal) }}</span>
                </div>
                <div class="pos-total-row">
                  <span>Phi van chuyen:</span
                  ><span>{{
                    posFee === 0 ? "Mien phi" : formatPrice(posFee)
                  }}</span>
                </div>
                <div class="pos-total-row pos-total-grand">
                  <span>Thanh toan:</span
                  ><span>{{ formatPrice(posGrandTotal) }}</span>
                </div>
              </div>

              <!-- customer -->
              <div class="pos-customer">
                <div class="pos-section-label">Thong tin khach hang</div>
                <div class="pos-phone-row">
                  <input
                    v-model="posPhone"
                    class="pos-input"
                    placeholder="So dien thoai *"
                    @keyup.enter="posLookup"
                  />
                  <button class="pos-lookup-btn" @click="posLookup">Tim</button>
                </div>
                <div v-if="posFoundCust" class="pos-found-cust">
                  ✓ {{ posFoundCust.hoTen }} · {{ posFoundCust.soDienThoai }}
                </div>
                <div v-else-if="posPhone.trim()" class="pos-new-cust">
                  <input
                    v-model="posNewName"
                    class="pos-input"
                    placeholder="Ho ten khach moi (neu co)"
                  />
                  <div class="pos-new-label">
                    Khach hang moi se duoc tao tu dong
                  </div>
                </div>
              </div>

              <!-- messages -->
              <div v-if="posError" class="pos-error">{{ posError }}</div>
              <div v-if="posSuccess" class="pos-success">
                ✓ Tao don hang thanh cong!
              </div>

              <!-- actions -->
              <div class="pos-actions">
                <button class="btn-cancel" @click="posReset" style="flex: 1">
                  Lam moi
                </button>
                <button
                  class="btn-primary"
                  @click="posPlaceOrder"
                  style="flex: 2"
                >
                  Tao don hang
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ── User placeholder pages ── -->
        <section
          v-show="
            [
              'user-home',
              'user-orders',
              'user-browse',
              'user-warranty',
              'user-profile',
            ].includes(currentPage)
          "
          class="page placeholder-page"
        >
          <div class="placeholder-icon">&#128101;</div>
          <div class="placeholder-title">{{ topbarTitle }}</div>
          <div class="placeholder-sub">
            Xem trang khach hang tai trang chu chinh
          </div>
        </section>
      </div>
      <!-- /content -->
    </main>
  </div>

  <!-- ── Product Modal ── -->
  <div
    v-if="showProductModal"
    class="modal-overlay"
    @click.self="showProductModal = false"
  >
    <div class="modal">
      <div class="modal-header">
        <span>{{ editingId ? "Cap nhat san pham" : "Them san pham moi" }}</span>
        <button class="modal-close" @click="showProductModal = false">
          &#10005;
        </button>
      </div>
      <div class="modal-body">
        <div v-if="formError" class="form-error">{{ formError }}</div>
        <div class="form-grid">
          <div class="form-group">
            <label>Ten san pham *</label
            ><input v-model="form.tenSanPham" placeholder="Ten san pham" />
          </div>
          <div class="form-group">
            <label>Ma SKU *</label
            ><input v-model="form.maSku" placeholder="SKU-001" />
          </div>
          <div class="form-group">
            <label>Loai san pham *</label>
            <select v-model="form.loaiSanPham">
              <option value="" disabled>-- Chon loai --</option>
              <option value="LAPTOP">Laptop</option>
              <option value="DIEN_THOAI">Dien thoai</option>
              <option value="PHU_KIEN">Phu kien</option>
            </select>
          </div>
          <div class="form-group">
            <label>Trang thai *</label>
            <select v-model="form.trangThai">
              <option value="active">Hoat dong</option>
              <option value="inactive">Ngung ban</option>
              <option value="ngung_kin_doanh">Ngung kinh doanh</option>
            </select>
          </div>
          <div class="form-group">
            <label>Thuong hieu *</label>
            <select v-model="form.thuongHieuId">
              <option :value="null" disabled>-- Chon thuong hieu --</option>
              <option
                v-for="b in brands"
                :key="b.thuongHieuId"
                :value="b.thuongHieuId"
              >
                {{ b.tenThuongHieu }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Danh muc *</label>
            <select v-model="form.danhMucId">
              <option :value="null" disabled>-- Chon danh muc --</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">
                {{ c.tenDanhMuc }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Nha cung cap</label>
            <select v-model="form.nhaCungCapId">
              <option :value="null">-- Khong co --</option>
              <option
                v-for="s in suppliers"
                :key="s.nhaCungCapId"
                :value="s.nhaCungCapId"
              >
                {{ s.tenNhaCungCap }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Mau sac *</label
            ><input v-model="form.mauSac" placeholder="Den" />
          </div>
          <div class="form-group">
            <label>CPU</label>
            <select v-model="form.cpuId">
              <option :value="null">-- Khong co --</option>
              <option v-for="c in cpuList" :key="c.cpuId" :value="c.cpuId">
                {{ c.tenCpu }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>RAM</label>
            <select v-model="form.ramId">
              <option :value="null">-- Khong co --</option>
              <option v-for="r in ramList" :key="r.ramId" :value="r.ramId">
                {{ r.dungLuong }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>O cung</label>
            <select v-model="form.oCungId">
              <option :value="null">-- Khong co --</option>
              <option
                v-for="o in oCungList"
                :key="o.oCungId"
                :value="o.oCungId"
              >
                {{ o.loaiOcung }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>GPU</label>
            <select v-model="form.gpuId">
              <option :value="null">-- Khong co --</option>
              <option v-for="g in gpuList" :key="g.gpuId" :value="g.gpuId">
                {{ g.tenGpu }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Man hinh</label
            ><input v-model="form.kichThuocManHinh" placeholder='15.6" FHD' />
          </div>
          <div class="form-group">
            <label>He dieu hanh</label
            ><input v-model="form.heDieuHanh" placeholder="Windows 11" />
          </div>
          <div class="form-group">
            <label>Pin</label><input v-model="form.pin" placeholder="72Wh" />
          </div>
          <div class="form-group">
            <label>Trong luong (kg)</label
            ><input v-model="form.trongLuongKg" type="number" step="0.1" />
          </div>
          <div class="form-group">
            <label>Gia ban (VND) *</label
            ><input v-model="form.giaBan" type="number" />
          </div>
          <div class="form-group">
            <label>Gia nhap (VND) *</label
            ><input v-model="form.giaNhap" type="number" />
          </div>
          <div class="form-group">
            <label>Bao hanh (thang) *</label
            ><input v-model="form.baoHanhThang" type="number" />
          </div>
          <div class="form-group span-2">
            <label>Hinh anh chinh (URL)</label
            ><input v-model="form.hinhAnhChinh" placeholder="https://..." />
          </div>
          <div class="form-group span-2">
            <label>Mo ta</label
            ><textarea v-model="form.moTa" rows="3"></textarea>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showProductModal = false">
          Huy
        </button>
        <button class="btn-primary" @click="saveProduct">
          {{ editingId ? "Cap nhat" : "Them moi" }}
        </button>
      </div>
    </div>
  </div>

  <!-- ── Customer Modal ── -->
  <div
    v-if="showCustomerModal"
    class="modal-overlay"
    @click.self="showCustomerModal = false"
  >
    <div class="modal" style="width: 560px">
      <div class="modal-header">
        <span>{{
          editingCustomerId ? "Cap nhat khach hang" : "Them khach hang moi"
        }}</span>
        <button class="modal-close" @click="showCustomerModal = false">
          &#10005;
        </button>
      </div>
      <div class="modal-body">
        <div v-if="customerFormError" class="form-error">
          {{ customerFormError }}
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label>Ho ten *</label><input v-model="customerForm.hoTen" />
          </div>
          <div class="form-group">
            <label>Dien thoai *</label
            ><input v-model="customerForm.soDienThoai" />
          </div>
          <div class="form-group">
            <label>Email *</label><input v-model="customerForm.email" />
          </div>
          <div class="form-group">
            <label>Loai khach</label>
            <select v-model="customerForm.loaiKhach">
              <option value="ca_nhan">Ca nhan</option>
              <option value="doanh_nghiep">Doanh nghiep</option>
            </select>
          </div>
          <div class="form-group span-2">
            <label>Dia chi</label><input v-model="customerForm.diaChi" />
          </div>
          <div class="form-group">
            <label>Ten cong ty</label><input v-model="customerForm.tenCongTy" />
          </div>
          <div class="form-group">
            <label>Ma so thue</label><input v-model="customerForm.maSoThue" />
          </div>
          <div class="form-group">
            <label>Diem tich luy</label
            ><input v-model="customerForm.diemTichLuy" type="number" min="0" />
          </div>
          <div class="form-group">
            <label>Trang thai</label>
            <select v-model="customerForm.trangThai">
              <option value="active">Hoat dong</option>
              <option value="inactive">Khoa</option>
            </select>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showCustomerModal = false">
          Huy
        </button>
        <button class="btn-primary" @click="saveCustomer">
          {{ editingCustomerId ? "Cap nhat" : "Them moi" }}
        </button>
      </div>
    </div>
  </div>

  <!-- ── Staff Modal ── -->
  <div
    v-if="showStaffModal"
    class="modal-overlay"
    @click.self="showStaffModal = false"
  >
    <div class="modal" style="width: 560px">
      <div class="modal-header">
        <span>{{
          editingStaffId ? "Cap nhat nhan vien" : "Them nhan vien moi"
        }}</span>
        <button class="modal-close" @click="showStaffModal = false">
          &#10005;
        </button>
      </div>
      <div class="modal-body">
        <div v-if="staffFormError" class="form-error">{{ staffFormError }}</div>
        <div class="form-grid">
          <div class="form-group">
            <label>Ho ten *</label><input v-model="staffForm.hoTen" />
          </div>
          <div class="form-group">
            <label>Dien thoai *</label><input v-model="staffForm.soDienThoai" />
          </div>
          <div class="form-group">
            <label>Email *</label><input v-model="staffForm.email" />
          </div>
          <div class="form-group">
            <label>Chuc vu *</label>
            <select v-model="staffForm.chucVuId">
              <option :value="null" disabled>-- Chon chuc vu --</option>
              <option v-for="cv in chucVuList" :key="cv.id" :value="cv.id">
                {{ cv.tenChucVu }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Username *</label><input v-model="staffForm.username" />
          </div>
          <div class="form-group">
            <label
              >Mat khau
              {{ editingStaffId ? "(de trong neu khong doi)" : "*" }}</label
            >
            <input v-model="staffForm.matKhauHash" type="password" />
          </div>
          <div class="form-group">
            <label>Luong co ban *</label
            ><input v-model="staffForm.luongCoBan" type="number" min="0" />
          </div>
          <div class="form-group">
            <label>Trang thai</label>
            <select v-model="staffForm.trangThai">
              <option value="active">Hoat dong</option>
              <option value="inactive">Nghi viec</option>
            </select>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showStaffModal = false">Huy</button>
        <button class="btn-primary" @click="saveStaff">
          {{ editingStaffId ? "Cap nhat" : "Them moi" }}
        </button>
      </div>
    </div>
  </div>

  <!-- ── Promotion Modal ── -->
  <div
    v-if="showPromoModal"
    class="modal-overlay"
    @click.self="showPromoModal = false"
  >
    <div class="modal" style="width: 620px">
      <div class="modal-header">
        <span>{{
          editingPromoId ? "Cap nhat khuyen mai" : "Them khuyen mai moi"
        }}</span>
        <button class="modal-close" @click="showPromoModal = false">
          &#10005;
        </button>
      </div>
      <div class="modal-body">
        <div v-if="promoFormError" class="form-error">{{ promoFormError }}</div>
        <div class="form-grid">
          <div class="form-group">
            <label>Ma khuyen mai *</label
            ><input v-model="promoForm.maKhuyenMai" />
          </div>
          <div class="form-group">
            <label>Ten khuyen mai *</label
            ><input v-model="promoForm.tenKhuyenMai" />
          </div>
          <div class="form-group">
            <label>Loai</label>
            <select v-model="promoForm.loai">
              <option value="percent">Phan tram (%)</option>
              <option value="fixed">So tien (VND)</option>
            </select>
          </div>
          <div class="form-group">
            <label
              >Gia tri
              {{ promoForm.loai === "percent" ? "(%)" : "(VND)" }}</label
            ><input v-model="promoForm.giaTri" type="number" />
          </div>
          <div class="form-group">
            <label>Giam toi da (VND)</label
            ><input v-model="promoForm.giaTriToiDa" type="number" />
          </div>
          <div class="form-group">
            <label>Don hang toi thieu (VND)</label
            ><input v-model="promoForm.donHangToiThieu" type="number" />
          </div>
          <div class="form-group">
            <label>Ngay bat dau *</label
            ><input v-model="promoForm.ngayBatDau" type="datetime-local" />
          </div>
          <div class="form-group">
            <label>Ngay ket thuc *</label
            ><input v-model="promoForm.ngayKetThuc" type="datetime-local" />
          </div>
          <div class="form-group">
            <label>So luong toi da</label
            ><input v-model="promoForm.soLuongToiDa" type="number" />
          </div>
          <div class="form-group">
            <label>Trang thai</label>
            <select v-model="promoForm.trangThai">
              <option value="active">Hoat dong</option>
              <option value="inactive">Ngung</option>
            </select>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showPromoModal = false">Huy</button>
        <button class="btn-primary" @click="savePromo">
          {{ editingPromoId ? "Cap nhat" : "Them moi" }}
        </button>
      </div>
    </div>
  </div>

  <!-- ── Order Status Modal ── -->
  <div
    v-if="showOrderModal"
    class="modal-overlay"
    @click.self="showOrderModal = false"
  >
    <div class="modal" style="width: 460px">
      <div class="modal-header">
        <span>Cap nhat trang thai don hang</span>
        <button class="modal-close" @click="showOrderModal = false">
          &#10005;
        </button>
      </div>
      <div class="modal-body">
        <div v-if="orderStatusError" class="form-error">
          {{ orderStatusError }}
        </div>
        <div v-if="editingOrder" class="order-info">
          Don hang #{{ editingOrder.donHangId }} — Khach:
          <strong>{{ customerName(editingOrder.khachHangId) }}</strong>
        </div>
        <div class="form-grid" style="grid-template-columns: 1fr">
          <div class="form-group">
            <label>Trang thai don hang</label>
            <select v-model="orderStatusForm.trangThaiDonHang">
              <option value="pending">Cho xac nhan</option>
              <option value="confirmed">Da xac nhan</option>
              <option value="processing">Dang xu ly</option>
              <option value="shipping">Dang giao</option>
              <option value="delivered">Da giao</option>
              <option value="cancelled">Huy</option>
              <option value="returned">Hoan tra</option>
            </select>
          </div>
          <div class="form-group">
            <label>Trang thai thanh toan</label>
            <select v-model="orderStatusForm.trangThaiThanhToan">
              <option value="unpaid">Chua thanh toan</option>
              <option value="partial">Thanh toan mot phan</option>
              <option value="paid">Da thanh toan</option>
              <option value="refunded">Hoan tien</option>
            </select>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showOrderModal = false">Huy</button>
        <button class="btn-primary" @click="saveOrderStatus">Luu</button>
      </div>
    </div>
  </div>

  <!-- ── Stock Edit Modal ── -->
  <div
    v-if="showStockModal"
    class="modal-overlay"
    @click.self="showStockModal = false"
  >
    <div class="modal" style="width: 420px">
      <div class="modal-header">
        <span>Cap nhat ton kho</span>
        <button class="modal-close" @click="showStockModal = false">
          &#10005;
        </button>
      </div>
      <div class="modal-body">
        <div v-if="editingStock" class="order-info">
          {{ editingStock.bienThe?.sanPham?.tenSanPham ?? "—" }} — SKU:
          <strong>{{ editingStock.bienThe?.maSku ?? "—" }}</strong>
        </div>
        <div class="form-grid" style="grid-template-columns: 1fr 1fr">
          <div class="form-group">
            <label>So luong ton</label
            ><input v-model="stockForm.soLuongTon" type="number" min="0" />
          </div>
          <div class="form-group">
            <label>Dang giu</label
            ><input v-model="stockForm.soLuongGiu" type="number" min="0" />
          </div>
          <div class="form-group span-2">
            <label>Ton kho toi thieu (canh bao)</label
            ><input v-model="stockForm.tonKhoToiThieu" type="number" min="0" />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="showStockModal = false">Huy</button>
        <button class="btn-primary" @click="saveStock">Luu</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-shell {
  display: flex;
  height: 100vh;
  background: #0d0d0d;
  color: #f0f0f0;
  font-family: "Inter", sans-serif;
  overflow: hidden;
}

/* ── Sidebar ── */
.sidebar {
  width: 240px;
  min-width: 240px;
  background: #111;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #f4c200;
  color: #111;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 0.8rem;
  flex-shrink: 0;
}
.logo-name {
  font-weight: 700;
  font-size: 0.95rem;
}
.logo-sub {
  font-size: 0.7rem;
  color: #888;
}
.role-switch {
  display: flex;
  gap: 6px;
  padding: 14px 14px 8px;
}
.role-btn {
  flex: 1;
  padding: 7px 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 7px;
  background: transparent;
  color: #888;
  cursor: pointer;
  font-size: 0.82rem;
  font-weight: 500;
  transition: all 0.15s;
}
.role-btn.active {
  background: rgba(244, 194, 0, 0.15);
  border-color: #f4c200;
  color: #f4c200;
}
.nav-group {
  flex: 1;
  padding: 6px 10px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.nav-label {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #555;
  text-transform: uppercase;
  padding: 10px 8px 3px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 7px;
  cursor: pointer;
  font-size: 0.87rem;
  color: #ccc;
  transition:
    background 0.12s,
    color 0.12s;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #f0f0f0;
}
.nav-item.active {
  background: rgba(244, 194, 0, 0.12);
  color: #f4c200;
}
.nav-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
  opacity: 0.75;
}
.nav-item.active .nav-icon {
  opacity: 1;
}
.nav-badge {
  margin-left: auto;
  background: #f4c200;
  color: #111;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 999px;
}
.sidebar-footer {
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.user-card {
  display: flex;
  align-items: center;
  gap: 9px;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #f4c200;
  color: #111;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 0.9rem;
  flex-shrink: 0;
}
.user-name {
  font-size: 0.85rem;
  font-weight: 600;
}
.user-role {
  font-size: 0.72rem;
  color: #888;
}

/* ── Main ── */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  background: #111;
}
.page-title {
  font-size: 1.05rem;
  font-weight: 700;
}
.page-breadcrumb {
  font-size: 0.78rem;
  color: #888;
  margin-top: 2px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.06);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 0.95rem;
}
.content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.page {
  display: block;
}
.loading-msg {
  color: #888;
  padding: 16px 0;
}

/* Stats */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 14px;
}
.stat-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 20px;
}
.stat-label {
  font-size: 0.78rem;
  color: #888;
  margin-bottom: 7px;
}
.stat-value {
  font-size: 1.55rem;
  font-weight: 700;
}
.stat-value.revenue {
  font-size: 1.1rem;
}

/* Alert */
.alert-bar {
  margin-top: 14px;
  padding: 10px 16px;
  background: rgba(224, 82, 82, 0.12);
  border: 1px solid rgba(224, 82, 82, 0.25);
  border-radius: 8px;
  color: #e05252;
  font-size: 0.84rem;
}
.low-stock-warn {
  color: #e05252;
  font-size: 0.83rem;
  font-weight: 600;
}

/* Table */
.section-title {
  font-size: 0.92rem;
  font-weight: 600;
  color: #ccc;
  margin-bottom: 10px;
}
.table-wrap {
  overflow-x: auto;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.86rem;
}
.data-table th {
  text-align: left;
  padding: 9px 12px;
  color: #888;
  font-weight: 600;
  font-size: 0.76rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.data-table td {
  padding: 11px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  vertical-align: middle;
}
.data-table tr:hover td {
  background: rgba(255, 255, 255, 0.03);
}
.row-warn td {
  background: rgba(224, 82, 82, 0.06) !important;
}
.empty-row {
  text-align: center;
  color: #666;
  padding: 28px !important;
}
.text-dim {
  color: #888;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 0.73rem;
  font-weight: 600;
}
.badge-green {
  background: rgba(72, 199, 142, 0.15);
  color: #48c78e;
}
.badge-gray {
  background: rgba(255, 255, 255, 0.08);
  color: #888;
}
.badge-blue {
  background: rgba(80, 160, 255, 0.15);
  color: #60a0ff;
}

/* Page header / action buttons */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #888;
  font-size: 0.88rem;
}
.action-btns {
  display: flex;
  gap: 5px;
}
.btn-edit {
  padding: 4px 10px;
  border-radius: 5px;
  border: none;
  background: rgba(244, 194, 0, 0.15);
  color: #f4c200;
  cursor: pointer;
  font-size: 0.78rem;
}
.btn-delete {
  padding: 4px 10px;
  border-radius: 5px;
  border: none;
  background: rgba(220, 53, 69, 0.15);
  color: #e05252;
  cursor: pointer;
  font-size: 0.78rem;
}
.btn-edit:hover {
  background: rgba(244, 194, 0, 0.3);
}
.btn-delete:hover {
  background: rgba(220, 53, 69, 0.3);
}
.btn-primary {
  padding: 8px 18px;
  border-radius: 7px;
  border: none;
  background: #f4c200;
  color: #111;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.86rem;
}
.btn-primary:hover {
  background: #ffd100;
}
.btn-cancel {
  padding: 8px 18px;
  border-radius: 7px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: transparent;
  color: #ccc;
  cursor: pointer;
  font-size: 0.86rem;
}

/* Placeholder */
.placeholder-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 12px;
  color: #555;
}
.placeholder-icon {
  font-size: 2.8rem;
}
.placeholder-title {
  font-size: 1.15rem;
  font-weight: 600;
  color: #888;
}
.placeholder-sub {
  font-size: 0.83rem;
}

/* Settings */
.settings-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 24px;
  max-width: 520px;
}
.settings-title {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 18px;
}
.settings-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 0.88rem;
}
.settings-row:last-child {
  border-bottom: none;
}
.settings-row span:first-child {
  color: #888;
}

/* ── POS / Ban hang ── */
.pos-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 18px;
  height: calc(100vh - 120px);
}
.pos-products {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}
.pos-search-bar {
  flex-shrink: 0;
}
.pos-search-input {
  width: 100%;
  padding: 9px 14px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #eee;
  font-size: 0.88rem;
  box-sizing: border-box;
  outline: none;
}
.pos-search-input:focus {
  border-color: #f4c200;
}
.pos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}
.pos-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.pos-card-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: #eee;
}
.pos-card-sku {
  font-size: 0.76rem;
}
.pos-card-brand {
  font-size: 0.75rem;
}
.pos-card-price {
  font-size: 0.95rem;
  font-weight: 700;
  color: #f4c200;
  margin-top: 4px;
}
.pos-add-btn {
  margin-top: auto;
  padding: 7px;
  border-radius: 7px;
  border: none;
  background: #f4c200;
  color: #111;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.8rem;
}
.pos-add-btn:hover {
  background: #ffd100;
}

.pos-cart {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 16px;
  gap: 14px;
  overflow: hidden;
}
.pos-cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  font-size: 0.95rem;
}
.pos-cart-count {
  font-size: 0.78rem;
  color: #888;
  font-weight: 400;
}
.pos-cart-items {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pos-cart-empty {
  color: #555;
  font-size: 0.84rem;
  text-align: center;
  padding: 24px 0;
}
.pos-cart-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
}
.pos-ci-info {
  flex: 1;
  min-width: 0;
}
.pos-ci-name {
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pos-ci-sku {
  font-size: 0.73rem;
}
.pos-ci-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.pos-qty-btn {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: transparent;
  color: #ccc;
  cursor: pointer;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pos-qty-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}
.pos-qty {
  min-width: 22px;
  text-align: center;
  font-size: 0.84rem;
}
.pos-rm-btn {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: none;
  background: rgba(220, 53, 69, 0.2);
  color: #e05252;
  cursor: pointer;
  font-size: 0.72rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pos-rm-btn:hover {
  background: rgba(220, 53, 69, 0.4);
}
.pos-ci-price {
  font-size: 0.8rem;
  font-weight: 600;
  color: #f4c200;
  flex-shrink: 0;
  min-width: 72px;
  text-align: right;
}

.pos-totals {
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.pos-total-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.83rem;
  color: #888;
}
.pos-total-grand {
  font-size: 0.96rem;
  font-weight: 700;
  color: #eee;
  margin-top: 4px;
}

.pos-customer {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.pos-section-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.pos-phone-row {
  display: flex;
  gap: 6px;
}
.pos-input {
  flex: 1;
  padding: 8px 10px;
  border-radius: 7px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #eee;
  font-size: 0.84rem;
  outline: none;
}
.pos-input:focus {
  border-color: #f4c200;
}
.pos-lookup-btn {
  padding: 8px 12px;
  border-radius: 7px;
  border: none;
  background: rgba(244, 194, 0, 0.15);
  color: #f4c200;
  cursor: pointer;
  font-size: 0.83rem;
  white-space: nowrap;
}
.pos-lookup-btn:hover {
  background: rgba(244, 194, 0, 0.3);
}
.pos-found-cust {
  font-size: 0.82rem;
  color: #48c78e;
  background: rgba(72, 199, 142, 0.1);
  border-radius: 6px;
  padding: 6px 10px;
}
.pos-new-cust {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.pos-new-label {
  font-size: 0.75rem;
  color: #888;
}
.pos-error {
  font-size: 0.82rem;
  color: #e05252;
  background: rgba(220, 53, 69, 0.1);
  border-radius: 6px;
  padding: 7px 10px;
}
.pos-success {
  font-size: 0.82rem;
  color: #48c78e;
  background: rgba(72, 199, 142, 0.1);
  border-radius: 6px;
  padding: 7px 10px;
}
.pos-actions {
  display: flex;
  gap: 8px;
}

/* ── Modal ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: #181818;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  width: 780px;
  max-width: 95vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  font-weight: 700;
  font-size: 0.95rem;
}
.modal-close {
  background: transparent;
  border: none;
  color: #888;
  font-size: 1rem;
  cursor: pointer;
}
.modal-close:hover {
  color: #f0f0f0;
}
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  padding: 14px 22px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}
.form-error {
  background: rgba(220, 53, 69, 0.15);
  border: 1px solid rgba(220, 53, 69, 0.3);
  border-radius: 7px;
  padding: 9px 13px;
  color: #e05252;
  font-size: 0.83rem;
  margin-bottom: 14px;
}
.order-info {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 7px;
  padding: 9px 13px;
  color: #aaa;
  font-size: 0.83rem;
  margin-bottom: 14px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.form-group.span-2 {
  grid-column: span 2;
}
.form-group label {
  font-size: 0.76rem;
  color: #999;
  font-weight: 500;
}
.form-group input,
.form-group select,
.form-group textarea {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 7px;
  padding: 8px 11px;
  color: #f0f0f0;
  font-size: 0.86rem;
  outline: none;
  font-family: inherit;
}
.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #f4c200;
  background: rgba(244, 194, 0, 0.04);
}
.form-group select option {
  background: #222;
}
.form-group textarea {
  resize: vertical;
}
</style>
