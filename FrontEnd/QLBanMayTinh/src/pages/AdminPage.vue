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
  if (!posCart.value.length) { posError.value = "Gio hang dang trong!"; return; }
  if (!posPhone.value.trim()) { posError.value = "Vui long nhap SDT khach hang!"; return; }
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
          hoTen: name, soDienThoai: posPhone.value.trim(),
          email: "", diaChi: "Tai cua hang",
          loaiKhach: "ca_nhan", diemTichLuy: 0, trangThai: "active",
        }),
      });
      if (!kRes.ok) throw new Error(`Loi tao khach hang: ${await kRes.text()}`);
      const kh = await kRes.json();
      khachHangId = kh.khachHangId;
      customers.value = await safeFetch("/api/khach-hang");
    }
    const nguoiNhan = posFoundCust.value?.hoTen ?? (posNewName.value.trim() || posPhone.value.trim());
    const orderRes = await fetch("/api/don-hang", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        khachHangId, nguoiNhan, sdtNguoiNhan: posPhone.value.trim(),
        diaChiGiaoHangText: posFoundCust.value?.diaChi ?? "Tai cua hang",
        tongTien: posCartTotal.value, giamGia: 0,
        phiVanChuyen: posFee.value, thanhTien: posGrandTotal.value,
        ngayDat: new Date().toISOString().slice(0, 19),
        trangThaiDonHang: "confirmed", trangThaiThanhToan: "paid", kenhBan: "in_store",
      }),
    });
    if (!orderRes.ok) throw new Error(`Loi tao don hang: ${await orderRes.text()}`);
    const created = await orderRes.json();
    const donHangId = created.id ?? created.donHangId;
    for (const item of posCart.value) {
      const ctRes = await fetch("/api/chi-tiet-don-hang", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ donHangId, bienTheId: item.bienTheId, soLuong: item.soLuong, donGia: item.giaBan, giamGiaDong: 0 }),
      });
      if (!ctRes.ok) throw new Error(`Loi them san pham: ${await ctRes.text()}`);
    }
    posSuccess.value = true;
    posCart.value = []; posPhone.value = ""; posFoundCust.value = null; posNewName.value = "";
    orders.value = await safeFetch("/api/don-hang");
  } catch (e) {
    posError.value = e.message;
  }
};

onMounted(fetchAll);
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
        <button class="btn btn-sm flex-grow-1 fw-medium"
                :class="currentRole==='admin' ? 'btn-warning text-dark' : 'btn-outline-secondary text-secondary'"
                style="font-size:0.82rem; border-radius:7px;"
                @click="switchRole('admin')">Admin</button>
        <button class="btn btn-sm flex-grow-1 fw-medium"
                :class="currentRole==='user' ? 'btn-warning text-dark' : 'btn-outline-secondary text-secondary'"
                style="font-size:0.82rem; border-radius:7px;"
                @click="switchRole('user')">Nhan vien</button>
      </div>

      <!-- Nav admin -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2" v-show="currentRole === 'admin'">
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
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2" v-show="currentRole === 'user'">
        <div class="adm-nav-label">Trang cua toi</div>
        <div class="adm-nav" :class="{active: currentPage==='user-home'}" @click="navigate('user-home')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/></svg>
          Trang chu
        </div>
        <div class="adm-nav" :class="{active: currentPage==='user-orders'}" @click="navigate('user-orders')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5z" clip-rule="evenodd"/></svg>
          Don hang cua toi
        </div>
        <div class="adm-nav" :class="{active: currentPage==='user-browse'}" @click="navigate('user-browse')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 2a4 4 0 00-4 4v1H5a1 1 0 00-.994.89l-1 9A1 1 0 004 18h12a1 1 0 00.994-1.11l-1-9A1 1 0 0015 7h-1V6a4 4 0 00-4-4zm2 5V6a2 2 0 10-4 0v1h4zm-6 3a1 1 0 112 0 1 1 0 01-2 0zm7-1a1 1 0 100 2 1 1 0 000-2z" clip-rule="evenodd"/></svg>
          Mua sam
        </div>
        <div class="adm-nav" :class="{active: currentPage==='user-profile'}" @click="navigate('user-profile')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/></svg>
          Ho so
        </div>
      </nav>

      <!-- Footer sidebar: thong tin user -->
      <div class="p-3 border-top" style="border-color:rgba(255,255,255,0.06)!important;">
        <div class="d-flex align-items-center gap-2">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
               style="width:34px;height:34px;background:#f4c200;color:#111;font-size:0.9rem;">{{ userAvatar }}</div>
          <div>
            <div class="fw-semibold" style="font-size:0.85rem;">{{ userDisplayName }}</div>
            <div style="font-size:0.72rem;color:#888;">{{ userDisplayRole }}</div>
          </div>
        </div>
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
              <thead><tr><th>Ten san pham</th><th>SKU</th><th>Thuong hieu</th><th>Danh muc</th><th>Gia ban</th><th>Trang thai</th><th>Thao tac</th></tr></thead>
              <tbody>
                <tr v-for="p in products" :key="p.sanPhamId">
                  <td>{{ p.tenSanPham }}</td>
                  <td class="text-secondary">{{ p.maSku }}</td>
                  <td>{{ p.tenThuongHieu }}</td>
                  <td>{{ p.tenDanhMuc }}</td>
                  <td>{{ formatPrice(p.giaBan) }}</td>
                  <td><span class="badge" :class="p.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(p.trangThai) }}</span></td>
                  <td>
                    <div class="d-flex gap-1">
                      <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEdit(p)">Sua</button>
                      <button class="btn btn-sm btn-outline-danger"  style="font-size:0.78rem; padding:2px 8px;" @click="deleteProduct(p.sanPhamId)">Xoa</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="products.length===0"><td colspan="7" class="text-center text-secondary">Chua co san pham</td></tr>
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
                  <td><span class="badge bg-primary bg-opacity-25 text-primary">{{ o.trangThaiDonHang||'—' }}</span></td>
                  <td><span class="badge" :class="o.trangThaiThanhToan==='paid'?'bg-success':'bg-secondary'">{{ o.trangThaiThanhToan||'—' }}</span></td>
                  <td>{{ formatDate(o.ngayDat) }}</td>
                  <td><button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openOrderStatus(o)">Cap nhat</button></td>
                </tr>
                <tr v-if="orders.length===0"><td colspan="7" class="text-center text-secondary">Chua co don hang</td></tr>
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
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-secondary small">{{ inventory.length }} bien the</span>
            <span v-if="lowStockItems.length" class="text-danger small fw-bold">&#9888; {{ lowStockItems.length }} sap het hang</span>
          </div>
          <div v-if="loading" class="text-secondary small">Dang tai...</div>
          <div v-else class="table-responsive">
            <table class="table table-dark table-hover table-sm align-middle">
              <thead><tr><th>San pham</th><th>SKU</th><th>Ton kho</th><th>Dang giu</th><th>Ton toi thieu</th><th>Cap nhat</th><th>Thao tac</th></tr></thead>
              <tbody>
                <tr v-for="item in inventory" :key="item.tonKhoId"
                    :class="item.soLuongTon!=null&&item.tonKhoToiThieu!=null&&item.soLuongTon<=item.tonKhoToiThieu ? 'row-warn' : ''">
                  <td>{{ item.bienThe?.sanPham?.tenSanPham??'—' }}</td>
                  <td class="text-secondary">{{ item.bienThe?.maSku??'—' }}</td>
                  <td><strong>{{ item.soLuongTon??'—' }}</strong></td>
                  <td>{{ item.soLuongGiu??0 }}</td>
                  <td>{{ item.tonKhoToiThieu??'—' }}</td>
                  <td class="text-secondary">{{ formatDate(item.ngayCapNhat) }}</td>
                  <td><button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem; padding:2px 8px;" @click="openEditStock(item)">Cap nhat</button></td>
                </tr>
                <tr v-if="inventory.length===0"><td colspan="7" class="text-center text-secondary">Chua co du lieu ton kho</td></tr>
              </tbody>
            </table>
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
        <section v-show="['user-home','user-orders','user-browse','user-warranty','user-profile'].includes(currentPage)"
                 class="d-flex flex-column align-items-center justify-content-center text-secondary"
                 style="min-height:300px;gap:12px;">
          <div style="font-size:2.8rem;">&#128101;</div>
          <div class="fw-bold" style="color:#888;font-size:1.15rem;">{{ topbarTitle }}</div>
          <div style="font-size:0.83rem;">Xem trang khach hang tai trang chu chinh</div>
        </section>

      </div><!-- /content -->
    </main>
  </div><!-- /dashboard-shell -->

  <!-- ══ MODAL SAN PHAM ══ -->
  <div v-if="showProductModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(0,0,0,0.65);z-index:1000;" @click.self="showProductModal=false">
    <div class="rounded-4 d-flex flex-column" style="background:#181818;border:1px solid rgba(255,255,255,0.1);width:780px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingId?'Cap nhat san pham':'Them san pham moi' }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="showProductModal=false"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="formError" class="alert alert-danger small py-2 mb-3">{{ formError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">Ten san pham *</label><input v-model="form.tenSanPham" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Ten san pham" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Ma SKU *</label><input v-model="form.maSku" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="SKU-001" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Loai san pham *</label><select v-model="form.loaiSanPham" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="" disabled>-- Chon loai --</option><option value="LAPTOP">Laptop</option><option value="DIEN_THOAI">Dien thoai</option><option value="PHU_KIEN">Phu kien</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Trang thai *</label><select v-model="form.trangThai" class="form-select form-select-sm bg-dark text-light border-secondary"><option value="active">Hoat dong</option><option value="inactive">Ngung ban</option><option value="ngung_kin_doanh">Ngung kinh doanh</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Thuong hieu *</label><select v-model="form.thuongHieuId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null" disabled>-- Chon thuong hieu --</option><option v-for="b in brands" :key="b.thuongHieuId" :value="b.thuongHieuId">{{ b.tenThuongHieu }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Danh muc *</label><select v-model="form.danhMucId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null" disabled>-- Chon danh muc --</option><option v-for="c in categories" :key="c.id" :value="c.id">{{ c.tenDanhMuc }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Nha cung cap</label><select v-model="form.nhaCungCapId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null">-- Khong co --</option><option v-for="s in suppliers" :key="s.nhaCungCapId" :value="s.nhaCungCapId">{{ s.tenNhaCungCap }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Mau sac *</label><input v-model="form.mauSac" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Den" /></div>
          <div class="col-6"><label class="form-label small text-secondary">CPU</label><select v-model="form.cpuId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null">-- Khong co --</option><option v-for="c in cpuList" :key="c.cpuId" :value="c.cpuId">{{ c.tenCpu }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">RAM</label><select v-model="form.ramId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null">-- Khong co --</option><option v-for="r in ramList" :key="r.ramId" :value="r.ramId">{{ r.dungLuong }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">O cung</label><select v-model="form.oCungId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null">-- Khong co --</option><option v-for="o in oCungList" :key="o.oCungId" :value="o.oCungId">{{ o.loaiOcung }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">GPU</label><select v-model="form.gpuId" class="form-select form-select-sm bg-dark text-light border-secondary"><option :value="null">-- Khong co --</option><option v-for="g in gpuList" :key="g.gpuId" :value="g.gpuId">{{ g.tenGpu }}</option></select></div>
          <div class="col-6"><label class="form-label small text-secondary">Man hinh</label><input v-model="form.kichThuocManHinh" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder='15.6" FHD' /></div>
          <div class="col-6"><label class="form-label small text-secondary">He dieu hanh</label><input v-model="form.heDieuHanh" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="Windows 11" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Pin</label><input v-model="form.pin" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="72Wh" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Trong luong (kg)</label><input v-model="form.trongLuongKg" type="number" step="0.1" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Gia ban (VND) *</label><input v-model="form.giaBan" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Gia nhap (VND) *</label><input v-model="form.giaNhap" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Bao hanh (thang) *</label><input v-model="form.baoHanhThang" type="number" class="form-control form-control-sm bg-dark text-light border-secondary" /></div>
          <div class="col-6"><label class="form-label small text-secondary">Hinh anh chinh (URL)</label><input v-model="form.hinhAnhChinh" class="form-control form-control-sm bg-dark text-light border-secondary" placeholder="https://..." /></div>
          <div class="col-12"><label class="form-label small text-secondary">Mo ta</label><textarea v-model="form.moTa" rows="3" class="form-control form-control-sm bg-dark text-light border-secondary"></textarea></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="showProductModal=false">Huy</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveProduct">{{ editingId?'Cap nhat':'Them moi' }}</button>
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
