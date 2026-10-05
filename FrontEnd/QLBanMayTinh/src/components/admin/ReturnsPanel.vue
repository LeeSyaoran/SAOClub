<script setup>
import { ref, computed, onMounted, reactive, watch } from "vue";
import {
  Search,
  Filter,
  X,
  ChevronDown,
  ChevronUp,
  Hash,
  FileText,
  Package,
  User,
  DollarSign,
  CreditCard,
  Activity,
  SlidersHorizontal,
  RotateCcw,
  Clock,
  CheckCircle2,
  XCircle,
  Wallet,
  Banknote,
  Landmark,
  Eye,
  Edit3,
  Plus,
  AlertCircle,
} from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as PhieuTraHangService from "../../services/PhieuTraHangService.js";
import * as ChiTietTraHangService from "../../services/ChiTietTraHangService.js";
import * as ChiTietDonHangService from "../../services/ChiTietDonHangService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import { formatPrice } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { AuthStore } from "../../stores/index.js";
import { OrdersStore, ensureOrders } from "../../stores/orders.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { refreshInventory } from "../../stores/inventory.js";
import { StaffStore, ensureStaff } from "../../stores/staff.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { ReturnsStore, ensureReturns, refreshReturns } from "../../stores/returns.js";

const props = defineProps({
  readonly: { type: Boolean, default: false },
  canPickStaff: { type: Boolean, default: false },
});

onMounted(() => {
  ensureReturns();
  ensureOrders();
  ensureCustomers();
  ensureProducts();
  ensureStaff();
});

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) =>
  (CustomersStore.items ?? []).find((c) => c.khachHangId === id)?.hoTen ??
  (id > 0 ? `Khách #${id}` : "Khách vãng lai");

const returnStats = computed(() => {
  const all = ReturnsStore?.items ?? [];
  const choXuLy = all.filter((r) => r.trangThai === "cho_xu_ly").length;
  const daXuLy = all.filter((r) => r.trangThai === "da_xu_ly").length;
  const tuChoi = all.filter((r) => r.trangThai === "tu_choi").length;
  const tongTien = all
    .filter((r) => r.trangThai === "da_xu_ly")
    .reduce((sum, r) => sum + (Number(r.soTienHoan) || 0), 0);
  return { total: all.length, choXuLy, daXuLy, tuChoi, tongTien };
});
const productByBienThe = (bienTheId) =>
  (ProductsStore.items ?? []).find((p) => p.bienTheId === bienTheId);
const staffOptions = computed(() => {
  const list = (StaffStore.items ?? []).map((s) => ({
    nhanVienId: Number(s.nhanVienId),
    hoTen: s.hoTen,
  }));
  // Đảm bảo tài khoản đang đăng nhập (Admin hoặc Nhân viên) luôn có trong danh sách
  const myId = AuthStore.user?.id || AuthStore.user?.nhanVienId;
  if (myId) {
    const numId = Number(myId);
    const exists = list.some((s) => s.nhanVienId === numId);
    if (!exists) {
      const myName =
        AuthStore.user?.hoTen ||
        AuthStore.user?.username ||
        (AuthStore.user?.role === "admin" ? "Quản trị viên" : `Nhân viên #${numId}`);
      list.unshift({
        nhanVienId: numId,
        hoTen: `${myName}${AuthStore.user?.role === "admin" ? " (Admin)" : ""}`,
      });
    }
  }
  return list;
});

const staffName = (id) => {
  if (!id) return "—";
  const numId = Number(id);
  const found = staffOptions.value.find((s) => s.nhanVienId === numId);
  if (found) return found.hoTen;
  if (
    AuthStore.user &&
    (Number(AuthStore.user.id) === numId || Number(AuthStore.user.nhanVienId) === numId)
  ) {
    return AuthStore.user.hoTen || AuthStore.user.username || "Quản trị viên";
  }
  return `Nhân viên #${id}`;
};

const orderById = (donHangId) => (OrdersStore.items ?? []).find((o) => o.donHangId === donHangId);

const STATUS_COLOR = {
  cho_xu_ly: { bg: "#fde68a", text: "#92400e" },
  da_xu_ly: { bg: "#bbf7d0", text: "#166534" },
  tu_choi: { bg: "#fecaca", text: "#991b1b" },
};
const statusColor = (s) => STATUS_COLOR[s] ?? { bg: "#e5e7eb", text: "#374151" };
const statusLabel = (s) => t(`admin.returnStatus.${s}`);
const hinhThucHoanLabel = (h) => t(`admin.hinhThucHoan.${h}`);

// ── Bộ lọc nâng cao: Trả hàng ───────────────────────────────────────────────
const search = ref("");
const isFilterOpen = ref(false);
const filters = reactive({
  trangThai: "", // '' | 'cho_xu_ly' | 'da_xu_ly' | 'tu_choi'
  hinhThucHoan: "", // '' | 'vi' | 'tien_mat' | 'chuyen_khoan'
  ngayFrom: "", // YYYY-MM-DD
  ngayTo: "",
  tienMin: "",
  tienMax: "",
});

const activeFilterCount = computed(() => {
  return [
    filters.trangThai,
    filters.hinhThucHoan,
    filters.ngayFrom,
    filters.ngayTo,
    filters.tienMin !== "" ? filters.tienMin : "",
    filters.tienMax !== "" ? filters.tienMax : "",
  ].filter((v) => v !== "").length;
});

const resetFilters = () => {
  filters.trangThai = "";
  filters.hinhThucHoan = "";
  filters.ngayFrom = "";
  filters.ngayTo = "";
  filters.tienMin = "";
  filters.tienMax = "";
  search.value = "";
};

const filteredReturns = computed(() => {
  const items = ReturnsStore?.items ?? [];
  const rawQ = search.value.trim().toLowerCase();
  const q = rawQ.replace(/^#/, "");
  return (
    items
      .filter((p) => {
        // text search (hỗ trợ tìm theo: mã phiếu, mã đơn hàng, ID đơn, tên khách hàng)
        if (rawQ) {
          const o = orderById(p.donHangId);
          const name = customerName(o?.khachHangId ?? -1).toLowerCase();
          const maDon = (o?.maDonHang ?? "").toLowerCase();
          const donHangIdStr = String(p.donHangId ?? "").toLowerCase();
          const maPhieu = (p.maPhieu ?? "").toLowerCase();
          const phieuTraIdStr = String(p.phieuTraId ?? "").toLowerCase();

          const serialStr = getReceiptSerials(p.phieuTraId).join(" ").toLowerCase();

          const match =
            phieuTraIdStr.includes(rawQ) ||
            phieuTraIdStr.includes(q) ||
            maPhieu.includes(rawQ) ||
            maPhieu.includes(q) ||
            maDon.includes(rawQ) ||
            maDon.includes(q) ||
            donHangIdStr.includes(rawQ) ||
            donHangIdStr.includes(q) ||
            name.includes(rawQ) ||
            serialStr.includes(rawQ);

          if (!match) return false;
        }
        if (filters.trangThai && p.trangThai !== filters.trangThai) return false;
        if (filters.hinhThucHoan && p.hinhThucHoan !== filters.hinhThucHoan) return false;
        // ngày trả
        if (filters.ngayFrom && (p.ngayTra ?? "").slice(0, 10) < filters.ngayFrom) return false;
        if (filters.ngayTo && (p.ngayTra ?? "").slice(0, 10) > filters.ngayTo) return false;
        // tiền hoàn range
        const tien = Number(p.soTienHoan ?? 0);
        if (filters.tienMin !== "" && tien < Number(filters.tienMin)) return false;
        if (filters.tienMax !== "" && tien > Number(filters.tienMax)) return false;
        return true;
      })
      // Sắp xếp mới nhất lên đầu: dùng phieuTraId (tự tăng) để đảm bảo đúng thứ tự tạo
      .sort((a, b) => (b.phieuTraId ?? 0) - (a.phieuTraId ?? 0))
  );
});
const {
  currentPage,
  totalPages,
  pagedItems: pagedReturns,
  pageSize,
} = usePagination(filteredReturns);
watch(
  [
    search,
    () => filters.trangThai,
    () => filters.hinhThucHoan,
    () => filters.ngayFrom,
    () => filters.ngayTo,
    () => filters.tienMin,
    () => filters.tienMax,
  ],
  () => {
    currentPage.value = 0;
  },
);

// ── Modal tao/sua/xem ─────────────────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const orderSearch = ref("");
const selectedOrder = ref(null);
const lineItems = ref([]); // [{ id, bienTheId, chiTietId, maSku, soSerial, donGia, soLuongDaMua, soLuongTra, tinhTrang, checked }]
const orderLinesLoading = ref(false);
const initialTrangThai = ref("cho_xu_ly");

// Những đơn vốn đã ở trạng thái đã xử lý từ trước không cho sửa nữa
const isModalReadonly = computed(() => {
  if (props.readonly) return true;
  if (editingId.value && initialTrangThai.value === "da_xu_ly") return true;
  return false;
});

const currentHandlerDisplayName = computed(() => {
  const currentId = form.value.nhanVienId;
  const myUser = AuthStore.user;
  const myId = myUser?.id ?? myUser?.nhanVienId;
  const targetId =
    currentId != null && currentId !== "" ? Number(currentId) : myId ? Number(myId) : 1;

  if (myId && Number(myId) === targetId) {
    const name =
      myUser?.hoTen ||
      myUser?.username ||
      (myUser?.role === "admin" ? "Quản trị viên" : "Nhân viên");
    return myUser?.role === "admin" ? `${name} (Admin)` : name;
  }
  if (targetId === 1) {
    return "Quản trị viên (Admin)";
  }
  const s = (StaffStore.items ?? []).find((x) => Number(x.nhanVienId) === targetId);
  if (s?.hoTen) return s.hoTen;
  return `Nhân viên #${targetId}`;
});

const isAllChecked = computed(() => {
  return lineItems.value.length > 0 && lineItems.value.every((l) => l.checked);
});

const toggleSelectAll = (e) => {
  const val = e.target.checked;
  lineItems.value.forEach((l) => (l.checked = val));
  recalcSoTienHoan();
};

const emptyForm = () => {
  const myId = AuthStore.user?.id ?? AuthStore.user?.nhanVienId ?? 1;
  return {
    donHangId: null,
    nhanVienId: Number(myId),
    lyDo: "",
    ngayTra: nowLocalIso().slice(0, 16),
    trangThai: "cho_xu_ly",
    soTienHoan: 0,
    hinhThucHoan: "vi",
    ghiChu: "",
  };
};
const form = ref(emptyForm());

// Những đơn đã có phiếu trả ở trạng thái "da_xu_ly" — không cho tạo thêm
const donHangDaXuLyIds = computed(() => {
  return new Set(
    (ReturnsStore.items ?? []).filter((r) => r.trangThai === "da_xu_ly").map((r) => r.donHangId),
  );
});

const searchedOrders = computed(() => {
  const q = orderSearch.value.trim().toLowerCase();
  if (!q) return [];
  return (OrdersStore.items ?? [])
    .filter((o) => {
      // Loại bỏ các đơn đã có phiếu trả đã xử lý
      if (donHangDaXuLyIds.value.has(o.donHangId)) return false;
      return (
        String(o.donHangId).includes(q) ||
        (o.maDonHang ?? "").toLowerCase().includes(q) ||
        customerName(o.khachHangId).toLowerCase().includes(q)
      );
    })
    .slice(0, 10);
});

const recalcSoTienHoan = () => {
  form.value.soTienHoan = lineItems.value
    .filter((l) => l.checked)
    .reduce((s, l) => s + (Number(l.donGia) || 0) * (Number(l.soLuongTra) || 0), 0);
};

// Giới hạn số lượng sản phẩm hoàn trả hợp lệ
const clampSoLuongTra = (l) => {
  const n = Math.trunc(Number(l.soLuongTra)) || 1;
  l.soLuongTra = Math.min(Math.max(n, 1), l.soLuongDaMua);
  recalcSoTienHoan();
};

const parseSerialList = (item, existed) => {
  if (Array.isArray(item?.serials) && item.serials.length > 0) {
    return item.serials.map((s) => (typeof s === "string" ? s : s?.soSerial)).filter(Boolean);
  }
  const raw = item?.soSerial || existed?.soSerial || "";
  if (!raw) return [];
  return String(raw)
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
};

const getLineDisplayedSerials = (l) => {
  const list =
    Array.isArray(l?.serials) && l.serials.length > 0
      ? l.serials
      : l?.soSerial
        ? String(l.soSerial)
            .split(",")
            .map((s) => s.trim())
            .filter(Boolean)
        : [];
  const qty = Math.max(1, Number(l?.soLuongTra) || 1);
  return list.slice(0, qty);
};

const loadOrderLines = async (donHangId, existingLines = []) => {
  if (!donHangId) {
    lineItems.value = [];
    orderLinesLoading.value = false;
    return;
  }
  orderLinesLoading.value = true;
  try {
    const rawItems = await ChiTietDonHangService.getByDonHang(donHangId).catch(() => []);
    const items = Array.isArray(rawItems) ? rawItems : [];
    const isNew = !existingLines || existingLines.length === 0;

    if (items.length > 0) {
      const mapped = items.map((i) => {
        const existed = existingLines.find(
          (c) =>
            c.bienTheId === i.bienTheId && (c.chiTietId == null || c.chiTietId === i.chiTietId),
        );
        const serials = parseSerialList(i, existed);
        return {
          id: existed?.id ?? null,
          bienTheId: i.bienTheId,
          chiTietId: i.chiTietId ?? existed?.chiTietId ?? null,
          maSku: i.maSku,
          soSerial: serials.join(", ") || i.soSerial || existed?.soSerial || null,
          serials,
          donGia: i.donGia,
          soLuongDaMua: i.soLuong,
          soLuongTra: existed?.soLuong ?? i.soLuong,
          tinhTrang: existed?.tinhTrang ?? "tot",
          checked: existed ? true : isNew,
        };
      });
      if (!isNew) {
        for (const c of existingLines) {
          const alreadyMatched = mapped.some(
            (m) =>
              m.bienTheId === c.bienTheId && (c.chiTietId == null || m.chiTietId === c.chiTietId),
          );
          if (!alreadyMatched) {
            const serials = parseSerialList(null, c);
            mapped.push({
              id: c.id ?? null,
              bienTheId: c.bienTheId,
              chiTietId: c.chiTietId ?? null,
              maSku: c.maSku || productByBienThe(c.bienTheId)?.maSku || `#${c.bienTheId}`,
              soSerial: serials.join(", ") || c.soSerial || null,
              serials,
              donGia: c.donGiaHoan ?? 0,
              soLuongDaMua: c.soLuong ?? 1,
              soLuongTra: c.soLuong ?? 1,
              tinhTrang: c.tinhTrang ?? "tot",
              checked: true,
            });
          }
        }
      }
      lineItems.value = mapped;
    } else if (existingLines && existingLines.length > 0) {
      lineItems.value = existingLines.map((c) => {
        const serials = parseSerialList(null, c);
        return {
          id: c.id ?? null,
          bienTheId: c.bienTheId,
          chiTietId: c.chiTietId ?? null,
          maSku: c.maSku || productByBienThe(c.bienTheId)?.maSku || `#${c.bienTheId}`,
          soSerial: serials.join(", ") || c.soSerial || null,
          serials,
          donGia: c.donGiaHoan ?? 0,
          soLuongDaMua: c.soLuong ?? 1,
          soLuongTra: c.soLuong ?? 1,
          tinhTrang: c.tinhTrang ?? "tot",
          checked: true,
        };
      });
    } else {
      lineItems.value = [];
    }

    // Fallback: nếu dòng nào chưa có mã serial, tra cứu từ danh sách serial của biến thể
    for (const line of lineItems.value) {
      const needed = Math.max(1, Number(line.soLuongDaMua) || 1);
      if ((!line.serials || line.serials.length < needed) && line.bienTheId) {
        const variantSerials = await ChiTietSanPhamService.getByBienThe(line.bienTheId).catch(
          () => [],
        );
        if (Array.isArray(variantSerials) && variantSerials.length > 0) {
          const picked = [];
          const pickedIds = [];
          if (line.chiTietId) {
            const exact = variantSerials.find((s) => s.chiTietId === line.chiTietId);
            if (exact?.soSerial) {
              picked.push(exact.soSerial);
              pickedIds.push(exact.chiTietId);
            }
          }
          const sorted = [...variantSerials].sort((a, b) => {
            const score = (s) =>
              s.trangThai === "da_ban" ? 0 : s.trangThai === "trong_kho" ? 1 : 2;
            return score(a) - score(b);
          });
          for (const s of sorted) {
            if (picked.length >= needed) break;
            if (s?.soSerial && !picked.includes(s.soSerial)) {
              picked.push(s.soSerial);
              pickedIds.push(s.chiTietId);
            }
          }
          if (picked.length > 0) {
            line.serials = picked;
            line.soSerial = picked.join(", ");
            if (!line.chiTietId && pickedIds[0]) {
              line.chiTietId = pickedIds[0];
            }
            line.resolvedSerialIds = pickedIds.filter(Boolean);
          }
        }
      }
    }

    if (isNew && lineItems.value.length > 0) {
      recalcSoTienHoan();
    }
  } catch (err) {
    console.error("[ReturnsPanel] Lỗi khi tải chi tiết đơn hàng:", err);
    lineItems.value = [];
  } finally {
    orderLinesLoading.value = false;
  }
};

const pickOrder = async (o) => {
  selectedOrder.value = o;
  form.value.donHangId = o.donHangId;
  orderSearch.value = "";
  await loadOrderLines(o.donHangId);
};

const openAdd = () => {
  editingId.value = null;
  initialTrangThai.value = "cho_xu_ly";
  form.value = emptyForm();
  const myId = AuthStore.user?.id ?? AuthStore.user?.nhanVienId ?? 1;
  form.value.nhanVienId = Number(myId);
  selectedOrder.value = null;
  orderSearch.value = "";
  lineItems.value = [];
  formError.value = "";
  showModal.value = true;
};

const openDetail = async (p) => {
  editingId.value = p.phieuTraId;
  initialTrangThai.value = p.trangThai ?? "cho_xu_ly";
  const myId = AuthStore.user?.id ?? AuthStore.user?.nhanVienId ?? 1;
  form.value = {
    donHangId: p.donHangId,
    nhanVienId: p.nhanVienId != null ? Number(p.nhanVienId) : Number(myId),
    lyDo: p.lyDo ?? "",
    ngayTra: p.ngayTra ? p.ngayTra.slice(0, 16) : nowLocalIso().slice(0, 16),
    trangThai: p.trangThai ?? "cho_xu_ly",
    soTienHoan: p.soTienHoan ?? 0,
    hinhThucHoan: p.hinhThucHoan || "vi",
    ghiChu: p.ghiChu ?? "",
  };
  selectedOrder.value =
    orderById(p.donHangId) ??
    (p.donHangId
      ? {
          donHangId: p.donHangId,
          maDonHang: `#${p.donHangId}`,
          khachHangId: -1,
          tongTien: p.soTienHoan ?? 0,
        }
      : null);
  formError.value = "";
  const allLines = await ChiTietTraHangService.getAll().catch(() => []);
  const mine = allLines.filter((c) => c.phieuTraId === p.phieuTraId);
  await loadOrderLines(p.donHangId, mine);
  showModal.value = true;
};

const saveReturn = async () => {
  if (isModalReadonly.value) return;
  formError.value = "";
  if (!form.value.donHangId) {
    formError.value = t("admin.returnModal.orderRequired");
    return;
  }
  if (!form.value.lyDo.trim()) {
    formError.value = t("admin.returnModal.reasonRequired");
    return;
  }
  const checkedLines = lineItems.value.filter((l) => l.checked);
  if (checkedLines.length === 0 && (!editingId.value || lineItems.value.length > 0)) {
    formError.value = t("admin.returnModal.lineRequired");
    return;
  }

  if (saving.value) return;
  saving.value = true;
  try {
    let nhanVienIdToSave = form.value.nhanVienId ? Number(form.value.nhanVienId) : null;
    if (!nhanVienIdToSave) {
      nhanVienIdToSave = AuthStore.user?.id
        ? Number(AuthStore.user.id)
        : (staffOptions.value[0]?.nhanVienId ?? 1);
    }
    const headerBody = {
      donHangId: form.value.donHangId,
      nhanVienId: nhanVienIdToSave,
      lyDo: form.value.lyDo,
      ngayTra: nowLocalIso(new Date(form.value.ngayTra)),
      trangThai: form.value.trangThai,
      soTienHoan: form.value.soTienHoan,
      hinhThucHoan: form.value.hinhThucHoan || "vi",
      ghiChu: form.value.ghiChu || "—",
    };
    const res = await PhieuTraHangService.save(editingId.value, headerBody);
    if (!res.ok) {
      const errText = await res.text().catch(() => "");
      formError.value =
        errText ||
        t("admin.errors.saveFailed", {
          status: res.status,
          text: "",
        });
      return;
    }

    let phieuTraId = editingId.value;
    if (!phieuTraId) {
      const created = await res.json();
      phieuTraId = created.phieuTraId;
    }

    const originalIds = checkedLines.filter((l) => l.id).map((l) => l.id);
    const allExisting = editingId.value ? await ChiTietTraHangService.getAll().catch(() => []) : [];
    const mineExisting = allExisting.filter((c) => c.phieuTraId === phieuTraId).map((c) => c.id);
    for (const oldId of mineExisting.filter((id) => !originalIds.includes(id))) {
      await ChiTietTraHangService.remove(oldId);
    }
    for (const l of checkedLines) {
      const body = {
        phieuTraId,
        bienTheId: l.bienTheId,
        chiTietId: l.chiTietId,
        soLuong: l.soLuongTra,
        donGiaHoan: l.donGia,
        tinhTrang: l.tinhTrang,
      };
      const lineRes = l.id
        ? await ChiTietTraHangService.update(l.id, body)
        : await ChiTietTraHangService.create(body);
      if (!lineRes.ok) {
        const lineErr = await lineRes.text().catch(() => `HTTP ${lineRes.status}`);
        formError.value = lineErr || `Lỗi lưu chi tiết trả hàng (HTTP ${lineRes.status})`;
        return;
      }
    }

    const returnedSerials = checkedLines.flatMap((l) => getLineDisplayedSerials(l));
    showModal.value = false;
    await Promise.all([
      refreshReturns(),
      refreshInventory().catch(() => {}),
      refreshProducts().catch(() => {}),
    ]);
    if (form.value.trangThai === "da_xu_ly") {
      const snText = returnedSerials.length > 0 ? ` (${returnedSerials.join(", ")})` : "";
      showToast(`Đã duyệt phiếu trả hàng và hoàn serial${snText} vào kho!`, "success");
    } else {
      showToast("Đã lưu phiếu trả hàng thành công!", "success");
    }
  } catch (e) {
    formError.value = e.message;
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <!-- KPI Summary Cards -->
  <div class="row g-3 mb-3">
    <div class="col-6 col-md-3">
      <div
        class="card border shadow-sm rounded-3 p-3 h-100"
        style="background: var(--bg-card); border-color: var(--border-color-soft) !important"
      >
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size: 11.5px">
              Tổng phiếu trả
            </div>
            <div class="fs-4 fw-bold mt-1" style="color: var(--text-heading)">
              {{ returnStats.total }}
            </div>
          </div>
          <div
            class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
            style="width: 42px; height: 42px; background: rgba(168, 85, 247, 0.12); color: #a855f7"
          >
            <RotateCcw :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div
        class="card border shadow-sm rounded-3 p-3 h-100"
        style="background: var(--bg-card); border-color: var(--border-color-soft) !important"
      >
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size: 11.5px">Chờ xử lý</div>
            <div class="fs-4 fw-bold mt-1 text-warning">{{ returnStats.choXuLy }}</div>
          </div>
          <div
            class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
            style="width: 42px; height: 42px; background: rgba(234, 179, 8, 0.12); color: #eab308"
          >
            <Clock :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div
        class="card border shadow-sm rounded-3 p-3 h-100"
        style="background: var(--bg-card); border-color: var(--border-color-soft) !important"
      >
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size: 11.5px">Đã xử lý</div>
            <div class="fs-4 fw-bold mt-1 text-success">{{ returnStats.daXuLy }}</div>
          </div>
          <div
            class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
            style="width: 42px; height: 42px; background: rgba(34, 197, 94, 0.12); color: #22c55e"
          >
            <CheckCircle2 :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div
        class="card border shadow-sm rounded-3 p-3 h-100"
        style="background: var(--bg-card); border-color: var(--border-color-soft) !important"
      >
        <div class="d-flex align-items-center justify-content-between">
          <div class="min-w-0 me-2">
            <div class="text-secondary small fw-semibold text-truncate" style="font-size: 11.5px">
              Tổng tiền đã hoàn
            </div>
            <div class="fs-5 fw-bold mt-1 text-danger font-monospace text-truncate">
              {{ formatPrice(returnStats.tongTien) }}
            </div>
          </div>
          <div
            class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
            style="width: 42px; height: 42px; background: rgba(239, 68, 68, 0.12); color: #ef4444"
          >
            <DollarSign :size="20" />
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="alt-card">
    <div class="alt-toolbar">
      <div class="d-flex align-items-center gap-2">
        <RotateCcw :size="16" class="text-secondary" />
        <span class="alt-toolbar__count">{{ filteredReturns.length }}/{{ (ReturnsStore?.items ?? []).length }}
          {{ t("admin.returns.countSuffix") }}</span>
      </div>
      <div class="alt-toolbar__actions">
        <div class="alt-search">
          <Search class="alt-search__icon" :size="14" />
          <input v-model="search" :placeholder="t('admin.returns.searchPlaceholder')" />
        </div>
        <button
          class="alt-btn alt-btn--filter"
          :class="{ 'alt-btn--filter-active': activeFilterCount > 0 || isFilterOpen }"
          @click="isFilterOpen = !isFilterOpen"
        >
          <Filter :size="14" /> Bộ lọc
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
          <ChevronDown v-if="!isFilterOpen" :size="13" />
          <ChevronUp v-else :size="13" />
        </button>
        <button
          v-if="activeFilterCount > 0"
          class="alt-btn alt-btn--ghost-sm"
          @click="resetFilters"
        >
          <X :size="13" /> Xóa lọc
        </button>
        <button
          v-if="!readonly"
          class="alt-btn alt-btn--primary d-inline-flex align-items-center gap-1.5"
          @click="openAdd"
        >
          <Plus :size="14" /> {{ t("admin.returns.add") }}
        </button>
      </div>
    </div>

    <!-- Panel lọc nâng cao -->
    <div v-if="isFilterOpen" class="adv-filter-panel">
      <div class="adv-filter-row">
        <div class="adv-filter-group">
          <label class="adv-filter-label">Trạng thái</label>
          <select v-model="filters.trangThai" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="cho_xu_ly">Chờ xử lý</option>
            <option value="da_xu_ly">Đã xử lý</option>
            <option value="tu_choi">Từ chối</option>
          </select>
        </div>
        <div class="adv-filter-group">
          <label class="adv-filter-label">Hình thức hoàn</label>
          <select v-model="filters.hinhThucHoan" class="adv-filter-select">
            <option value="">Tất cả</option>
            <option value="vi">Ví điểm</option>
            <option value="tien_mat">Tiền mặt</option>
            <option value="chuyen_khoan">Chuyển khoản</option>
          </select>
        </div>
        <div class="adv-filter-group adv-filter-group--range">
          <label class="adv-filter-label">Ngày trả</label>
          <div class="adv-filter-range">
            <input v-model="filters.ngayFrom" type="date" class="adv-filter-input" />
            <span class="adv-filter-sep">–</span>
            <input v-model="filters.ngayTo" type="date" class="adv-filter-input" />
          </div>
        </div>
        <div class="adv-filter-group adv-filter-group--range">
          <label class="adv-filter-label">Tiền hoàn (₫)</label>
          <div class="adv-filter-range">
            <input
              v-model="filters.tienMin"
              type="number"
              min="0"
              placeholder="Từ"
              class="adv-filter-input"
            />
            <span class="adv-filter-sep">–</span>
            <input
              v-model="filters.tienMax"
              type="number"
              min="0"
              placeholder="Đến"
              class="adv-filter-input"
            />
          </div>
        </div>
        <button v-if="activeFilterCount > 0" class="adv-filter-reset" @click="resetFilters">
          <X :size="13" /> Xóa bộ lọc
        </button>
      </div>
    </div>

    <div v-if="ReturnsStore.loading" class="alt-empty">
      {{ t("admin.returns.loading") }}
    </div>
    <div v-else class="alt-table-wrap">
      <table class="alt-table">
        <thead>
          <tr>
            <th style="width: 45px">
              <span class="d-inline-flex align-items-center gap-1"><Hash :size="12" /> {{ t("admin.common.stt") }}</span>
            </th>
            <th style="width: 90px">
              <span class="d-inline-flex align-items-center gap-1">{{
                t("admin.returns.colId")
              }}</span>
            </th>
            <th>
              <span class="d-inline-flex align-items-center gap-1">{{
                t("admin.returns.colOrder")
              }}</span>
            </th>
            <th>
              <span class="d-inline-flex align-items-center gap-1"><User :size="12" /> {{ t("admin.returns.colCustomer") }}</span>
            </th>
            <th>
              <span class="d-inline-flex align-items-center gap-1"><DollarSign :size="12" /> {{ t("admin.returns.colAmount") }}</span>
            </th>
            <th>
              <span class="d-inline-flex align-items-center gap-1"><CreditCard :size="12" /> {{ t("admin.returns.colHinhThucHoan") }}</span>
            </th>
            <th>
              <span class="d-inline-flex align-items-center gap-1"><Activity :size="12" /> {{ t("admin.returns.colStatus") }}</span>
            </th>
            <th style="width: 100px">
              <span class="d-inline-flex align-items-center gap-1"><SlidersHorizontal :size="12" /> {{ t("admin.returns.colAction") }}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(p, idx) in pagedReturns" :key="p.phieuTraId">
            <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
            <td>
              <span
                class="badge font-monospace bg-body-secondary text-body-secondary border px-2 py-1"
              >
                {{ p.maPhieu || "#" + p.phieuTraId }}
              </span>
            </td>
            <td>
              <span class="badge font-monospace bg-light-subtle text-body border px-2 py-1">
                {{ orderById(p.donHangId)?.maDonHang || "#" + p.donHangId }}
              </span>
            </td>
            <td>
              <div class="d-flex align-items-center gap-1.5">
                <User :size="13" class="text-secondary flex-shrink-0" />
                <span
                  :class="{
                    'text-muted fst-italic':
                      !orderById(p.donHangId)?.khachHangId ||
                      orderById(p.donHangId)?.khachHangId <= 0,
                  }"
                >
                  {{ customerName(orderById(p.donHangId)?.khachHangId ?? -1) }}
                </span>
              </div>
            </td>
            <td>
              <span
                class="fw-bold font-monospace"
                :class="Number(p.soTienHoan) > 0 ? 'text-danger' : 'text-muted'"
              >
                {{ formatPrice(p.soTienHoan) }}
              </span>
            </td>
            <td>
              <span
                v-if="p.hinhThucHoan === 'vi'"
                class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2 py-1 d-inline-flex align-items-center gap-1"
                style="font-size: 11px"
              >
                <Wallet :size="12" /> Ví điện tử
              </span>
              <span
                v-else-if="p.hinhThucHoan === 'tien_mat'"
                class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2 py-1 d-inline-flex align-items-center gap-1"
                style="font-size: 11px"
              >
                <Banknote :size="12" /> Tiền mặt
              </span>
              <span
                v-else-if="p.hinhThucHoan === 'chuyen_khoan'"
                class="badge rounded-pill bg-primary-subtle text-primary border border-primary-subtle px-2 py-1 d-inline-flex align-items-center gap-1"
                style="font-size: 11px"
              >
                <Landmark :size="12" /> Chuyển khoản
              </span>
              <span
                v-else
                class="badge rounded-pill bg-secondary-subtle text-secondary border px-2 py-1"
                style="font-size: 11px"
              >
                {{ hinhThucHoanLabel(p.hinhThucHoan) }}
              </span>
            </td>
            <td>
              <span
                v-if="p.trangThai === 'cho_xu_ly'"
                class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1"
                style="font-size: 11.5px"
              >
                <Clock :size="12" /> {{ statusLabel(p.trangThai) }}
              </span>
              <span
                v-else-if="p.trangThai === 'da_xu_ly'"
                class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1"
                style="font-size: 11.5px"
              >
                <CheckCircle2 :size="12" /> {{ statusLabel(p.trangThai) }}
              </span>
              <span
                v-else-if="p.trangThai === 'tu_choi'"
                class="badge rounded-pill bg-danger-subtle text-danger border border-danger-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1"
                style="font-size: 11.5px"
              >
                <XCircle :size="12" /> {{ statusLabel(p.trangThai) }}
              </span>
              <span
                v-else
                class="alt-tag"
                :style="{
                  background: statusColor(p.trangThai).bg,
                  color: statusColor(p.trangThai).text,
                }"
              >{{ statusLabel(p.trangThai) }}</span>
            </td>
            <td>
              <div class="d-flex gap-1">
                <button
                  class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1 px-2.5 py-1 rounded-2"
                  style="font-size: 12px"
                  @click="openDetail(p)"
                >
                  <Eye v-if="readonly || p.trangThai === 'da_xu_ly'" :size="13" />
                  <Edit3 v-else :size="13" />
                  <span>{{
                    readonly || p.trangThai === "da_xu_ly"
                      ? t("admin.returns.view")
                      : t("admin.returns.edit")
                  }}</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="filteredReturns.length === 0">
            <td colspan="8" class="alt-empty">
              {{ t("admin.returns.empty") }}
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="totalPages > 1" class="alt-pager">
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          @page-change="currentPage = $event"
        />
      </div>
    </div>
  </div>

  <!-- ── Modal Tạo / Sửa phiếu trả hàng ──────────────────────────────────────── -->
  <div
    v-if="showModal"
    class="cfm-backdrop position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center p-3"
    style="background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(4px); z-index: 1050"
    @click.self="showModal = false"
  >
    <div class="cfm-shell return-modal-shell">
      <!-- ── Header ── -->
      <div class="cfm-header">
        <div class="cfm-header-icon">
          <RotateCcw :size="22" />
        </div>
        <div class="cfm-header-text">
          <h3 class="cfm-title">
            {{
              editingId
                ? isModalReadonly
                  ? "Chi tiết phiếu trả hàng"
                  : t("admin.returnModal.titleEdit")
                : t("admin.returnModal.titleAdd")
            }}
          </h3>
          <p class="cfm-subtitle">
            {{
              isModalReadonly
                ? "Phiếu trả hàng đã hoàn tất xử lý (chỉ xem)"
                : editingId
                  ? "Xem và cập nhật thông tin phiếu đổi trả hàng"
                  : "Tạo phiếu đổi trả & hoàn tiền cho khách hàng"
            }}
          </p>
        </div>
        <button class="cfm-close" :aria-label="t('common.close')" @click="showModal = false">
          <X :size="16" />
        </button>
      </div>

      <!-- ── Body ── -->
      <div class="cfm-body return-modal-body">
        <div v-if="formError" class="cfm-error">
          <AlertCircle :size="16" class="flex-shrink-0" />
          <span>{{ formError }}</span>
        </div>

        <!-- Thông báo phiếu đã xử lý (chỉ xem) -->
        <div
          v-if="isModalReadonly"
          class="alert py-2 px-3 small d-flex align-items-center gap-2 mb-3 rounded-3"
          style="
            background: rgba(16, 185, 129, 0.12);
            border: 1px solid rgba(16, 185, 129, 0.3);
            color: #047857;
          "
        >
          <CheckCircle2 :size="16" class="flex-shrink-0 text-success" />
          <span>Phiếu trả hàng này ở trạng thái <strong>Đã xử lý</strong> nên không thể chỉnh sửa. Bạn
            chỉ có thể xem chi tiết.</span>
        </div>

        <!-- Section 1: Đơn hàng & Sản phẩm trả -->
        <div class="cfm-section">
          <div class="cfm-section-title">
            <Package :size="15" />
            <span>ĐƠN HÀNG & SẢN PHẨM TRẢ</span>
          </div>

          <!-- Chọn đơn hàng -->
          <div class="mb-3">
            <label class="cfm-label mb-1.5">
              <span>{{ t("admin.returnModal.orderLabel") }}</span>
            </label>

            <!-- Khi đã chọn đơn hàng -->
            <div
              v-if="selectedOrder"
              class="return-order-card d-flex align-items-center justify-content-between p-3"
            >
              <div class="d-flex align-items-center gap-3">
                <div class="return-order-icon">
                  <FileText :size="20" />
                </div>
                <div>
                  <div class="d-flex align-items-center gap-2">
                    <span class="return-order-code">{{
                      selectedOrder.maDonHang || "#" + selectedOrder.donHangId
                    }}</span>
                    <span class="badge bg-secondary-subtle text-secondary small">{{
                      customerName(selectedOrder.khachHangId)
                    }}</span>
                  </div>
                  <div class="text-secondary small mt-1" style="font-size: 12px">
                    Tổng đơn:
                    <span class="fw-semibold text-dark">{{
                      formatPrice(selectedOrder.tongTien)
                    }}</span>
                    <span v-if="selectedOrder.ngayTao">
                      · {{ selectedOrder.ngayTao.slice(0, 10) }}</span>
                  </div>
                </div>
              </div>
              <button
                v-if="!editingId && !isModalReadonly"
                class="return-btn-change"
                @click="
                  selectedOrder = null;
                  form.donHangId = null;
                  lineItems = [];
                "
              >
                <RotateCcw :size="13" />
                <span>{{ t("admin.returnModal.changeOrder") }}</span>
              </button>
            </div>

            <!-- Khi chưa chọn đơn hàng: Tìm kiếm -->
            <div v-else class="position-relative">
              <div class="cfm-input-wrap">
                <Search class="cfm-input-icon" :size="15" />
                <input
                  v-model="orderSearch"
                  class="cfm-input"
                  :placeholder="t('admin.returnModal.orderSearchPlaceholder')"
                />
              </div>
              <div v-if="orderSearch.trim()" class="return-order-dropdown">
                <div
                  v-for="o in searchedOrders"
                  :key="o.donHangId"
                  class="return-order-item"
                  @click="pickOrder(o)"
                >
                  <div class="d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center gap-2">
                      <span class="return-order-code">{{ o.maDonHang || "#" + o.donHangId }}</span>
                      <span class="fw-medium text-dark">{{ customerName(o.khachHangId) }}</span>
                    </div>
                    <span class="fw-semibold text-pink-700 small">{{
                      formatPrice(o.tongTien)
                    }}</span>
                  </div>
                </div>
                <div
                  v-if="searchedOrders.length === 0"
                  class="p-3 text-secondary text-center small"
                >
                  Không tìm thấy đơn hàng phù hợp (đơn đã có phiếu trả xử lý xong sẽ không hiện ở
                  đây)
                </div>
              </div>
            </div>
          </div>

          <!-- Danh sách dòng sản phẩm trả -->
          <div v-if="selectedOrder" class="mt-3">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <label class="cfm-label mb-0">
                <span>{{ t("admin.returnModal.lineItemsTitle") }}</span>
                <span
                  v-if="lineItems.length > 0"
                  class="badge rounded-pill bg-pink-subtle text-pink-700 ms-1"
                  style="font-size: 11px"
                >
                  Đã chọn {{ lineItems.filter((l) => l.checked).length }}/{{ lineItems.length }}
                </span>
              </label>
              <div v-if="lineItems.length > 0 && !isModalReadonly" class="text-secondary small">
                <label
                  class="d-inline-flex align-items-center gap-1.5 cursor-pointer user-select-none"
                  style="font-size: 12px; cursor: pointer"
                >
                  <input
                    type="checkbox"
                    :checked="isAllChecked"
                    class="form-check-input mt-0"
                    style="cursor: pointer"
                    @change="toggleSelectAll"
                  />
                  <span class="fw-medium text-dark">Chọn tất cả</span>
                </label>
              </div>
            </div>

            <!-- Loading spinner -->
            <div v-if="orderLinesLoading" class="return-table-loading">
              <div class="spinner-border spinner-border-sm text-pink" role="status"></div>
              <span>Đang tải danh sách sản phẩm của đơn hàng...</span>
            </div>

            <!-- Empty state -->
            <div v-else-if="lineItems.length === 0" class="return-table-empty">
              <Package :size="24" class="text-muted mb-1 opacity-50" />
              <div>Đơn hàng này không có sản phẩm nào</div>
            </div>

            <!-- Table -->
            <div v-else class="return-table-card">
              <table class="return-table">
                <thead>
                  <tr>
                    <th style="width: 38px" class="text-center">#</th>
                    <th>{{ t("admin.returnModal.colProduct") }}</th>
                    <th>{{ t("admin.returnModal.colSku") }}</th>
                    <th style="min-width: 135px">Mã Serial</th>
                    <th class="text-center" style="width: 75px">
                      {{ t("admin.returnModal.colBought") }}
                    </th>
                    <th class="text-center" style="width: 90px">
                      {{ t("admin.returnModal.colReturnQty") }}
                    </th>
                    <th style="width: 130px">{{ t("admin.returnModal.colCondition") }}</th>
                    <th class="text-end" style="width: 120px">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="l in lineItems"
                    :key="`${l.bienTheId}-${l.chiTietId}`"
                    :class="{ 'is-selected': l.checked }"
                  >
                    <td class="text-center">
                      <input
                        v-model="l.checked"
                        type="checkbox"
                        class="form-check-input"
                        :disabled="isModalReadonly"
                        style="cursor: pointer"
                        @change="recalcSoTienHoan"
                      />
                    </td>
                    <td>
                      <div class="fw-semibold text-dark" style="font-size: 12.5px">
                        {{ productByBienThe(l.bienTheId)?.tenSanPham || "—" }}
                      </div>
                      <div
                        v-if="l.donGia"
                        class="text-secondary small mt-0.5"
                        style="font-size: 11px"
                      >
                        Đơn giá: {{ formatPrice(l.donGia) }}
                      </div>
                    </td>
                    <td>
                      <div class="return-sku-badge">{{ l.maSku }}</div>
                    </td>
                    <td>
                      <div
                        v-if="getLineDisplayedSerials(l).length > 0"
                        class="d-flex flex-wrap gap-1"
                      >
                        <span
                          v-for="sn in getLineDisplayedSerials(l)"
                          :key="sn"
                          class="return-serial-badge"
                        >
                          {{ sn }}
                        </span>
                      </div>
                      <span v-else class="text-muted small">—</span>
                    </td>
                    <td class="text-center">
                      <span class="badge bg-light text-dark border fw-medium px-2 py-1">{{
                        l.soLuongDaMua
                      }}</span>
                    </td>
                    <td class="text-center">
                      <input
                        v-model.number="l.soLuongTra"
                        type="number"
                        min="1"
                        :max="l.soLuongDaMua"
                        :disabled="isModalReadonly || !l.checked"
                        class="return-qty-input text-center"
                        @change="clampSoLuongTra(l)"
                      />
                    </td>
                    <td>
                      <select
                        v-model="l.tinhTrang"
                        :disabled="isModalReadonly || !l.checked"
                        class="return-cond-select"
                      >
                        <option value="tot">{{ t("admin.returnModal.conditionGood") }}</option>
                        <option value="loi">{{ t("admin.returnModal.conditionBad") }}</option>
                      </select>
                    </td>
                    <td
                      class="text-end fw-semibold"
                      style="font-size: 12.5px; color: var(--pink-700)"
                    >
                      {{ formatPrice((l.donGia || 0) * (l.soLuongTra || 0)) }}
                    </td>
                  </tr>
                </tbody>
              </table>

              <!-- Table summary footer -->
              <div
                class="return-table-footer d-flex align-items-center justify-content-between px-3 py-2"
              >
                <span class="text-secondary small">
                  Đã chọn:
                  <strong class="text-dark">{{ lineItems.filter((l) => l.checked).length }}</strong>
                  sản phẩm
                </span>
                <span class="small">
                  Tổng tiền hoàn ước tính:
                  <strong class="fs-6 text-pink-700 ms-1">{{
                    formatPrice(form.soTienHoan)
                  }}</strong>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Section 2: Thông tin xử lý & hoàn tiền -->
        <div class="cfm-section">
          <div class="cfm-section-title">
            <DollarSign :size="15" />
            <span>THÔNG TIN XỬ LÝ & HOÀN TIỀN</span>
          </div>

          <div class="cfm-fields">
            <!-- Người xử lý (tự load từ tài khoản login) -->
            <div class="cfm-field">
              <label class="cfm-label">
                <User :size="13" />
                <span>{{ t("admin.returnModal.staffLabel") }}</span>
              </label>

              <!-- Tự động load và hiển thị người xử lý theo tài khoản đăng nhập -->
              <div class="cfm-input-wrap">
                <User class="cfm-input-icon" :size="14" />
                <input
                  type="text"
                  :value="currentHandlerDisplayName"
                  disabled
                  class="cfm-input"
                  style="background: var(--pink-50, #fff5f9); cursor: not-allowed; font-weight: 600"
                  placeholder="Hệ thống tự động ghi nhận"
                />
              </div>
            </div>

            <!-- Ngày trả -->
            <div class="cfm-field">
              <label class="cfm-label">
                <Clock :size="13" />
                <span>{{ t("admin.returnModal.dateLabel") }}</span>
              </label>
              <div class="cfm-input-wrap">
                <Clock class="cfm-input-icon" :size="14" />
                <input
                  v-model="form.ngayTra"
                  type="datetime-local"
                  :disabled="isModalReadonly"
                  class="cfm-input"
                />
              </div>
            </div>

            <!-- Lý do trả hàng (Full width) -->
            <div class="cfm-field cfm-field--full">
              <label class="cfm-label">
                <FileText :size="13" />
                <span>{{ t("admin.returnModal.reasonLabel") }}</span>
                <span class="cfm-required">*</span>
              </label>
              <div class="cfm-input-wrap">
                <FileText class="cfm-input-icon" :size="14" />
                <input
                  v-model="form.lyDo"
                  :disabled="isModalReadonly"
                  class="cfm-input"
                  placeholder="Nhập lý do khách hàng trả hàng (VD: Đổi ý, sản phẩm lỗi phần cứng...)"
                />
              </div>
            </div>

            <!-- Số tiền hoàn -->
            <div class="cfm-field">
              <label class="cfm-label">
                <Wallet :size="13" />
                <span>{{ t("admin.returnModal.amountLabel") }} (VNĐ)</span>
                <span class="cfm-required">*</span>
              </label>
              <div class="cfm-input-wrap">
                <Wallet class="cfm-input-icon" :size="14" />
                <input
                  v-model.number="form.soTienHoan"
                  type="number"
                  min="0"
                  :disabled="isModalReadonly"
                  class="cfm-input fw-bold"
                  style="color: var(--pink-700, #a81b5d)"
                />
              </div>
            </div>

            <!-- Hình thức hoàn (bỏ khách có mặt tại cửa hàng) -->
            <div class="cfm-field">
              <label class="cfm-label">
                <CreditCard :size="13" />
                <span>{{ t("admin.returnModal.hinhThucHoanLabel") }}</span>
              </label>
              <div class="cfm-input-wrap">
                <CreditCard class="cfm-input-icon" :size="14" />
                <select
                  v-model="form.hinhThucHoan"
                  :disabled="isModalReadonly"
                  class="cfm-input cfm-select"
                >
                  <option value="vi">{{ t("admin.hinhThucHoan.vi") }} (Ví tài khoản)</option>
                  <option value="tien_mat">
                    {{ t("admin.hinhThucHoan.tien_mat") }} (Tại quầy)
                  </option>
                </select>
              </div>
            </div>

            <!-- Trạng thái -->
            <div class="cfm-field">
              <label class="cfm-label">
                <Activity :size="13" />
                <span>{{ t("admin.returnModal.statusLabel") }}</span>
              </label>
              <div class="cfm-input-wrap">
                <Activity class="cfm-input-icon" :size="14" />
                <select
                  v-model="form.trangThai"
                  :disabled="isModalReadonly"
                  class="cfm-input cfm-select"
                >
                  <option value="cho_xu_ly">{{ t("admin.returnStatus.cho_xu_ly") }}</option>
                  <option value="da_xu_ly">{{ t("admin.returnStatus.da_xu_ly") }}</option>
                  <option value="tu_choi">{{ t("admin.returnStatus.tu_choi") }}</option>
                </select>
              </div>
            </div>

            <!-- Ghi chú (Full width) -->
            <div class="cfm-field cfm-field--full">
              <label class="cfm-label">
                <span>{{ t("admin.returnModal.noteLabel") }}</span>
              </label>
              <input
                v-model="form.ghiChu"
                :disabled="isModalReadonly"
                class="cfm-input"
                placeholder="Ghi chú thêm về phụ kiện kèm theo, số seri máy, tình trạng vỏ hộp..."
              />
            </div>
          </div>
        </div>
      </div>

      <!-- ── Footer ── -->
      <div class="cfm-footer">
        <button class="cfm-btn cfm-btn--ghost" @click="showModal = false">
          {{ isModalReadonly ? t("admin.returnModal.close") : t("admin.returnModal.cancel") }}
        </button>
        <button
          v-if="!isModalReadonly"
          class="cfm-btn cfm-btn--primary"
          :disabled="saving"
          @click="saveReturn"
        >
          <span v-if="saving" class="spinner-border spinner-border-sm me-1"></span>
          <CheckCircle2 v-else :size="16" />
          <span>{{ t("admin.returnModal.save") }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ─── Filter Button ─── */
.alt-btn--filter {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  background: var(--surface, #fff);
  color: var(--ink, #1e293b);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}
.alt-btn--filter:hover,
.alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6);
  background: var(--pink-50, #fdf2f8);
  color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--pink-600, #db2777);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}
.alt-btn--ghost-sm {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  background: transparent;
  color: var(--muted, #64748b);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.alt-btn--ghost-sm:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #dc2626;
}

/* ─── Advanced Filter Panel ─── */
.adv-filter-panel {
  border-top: 1px solid var(--border, #e2e8f0);
  background: var(--surface-alt, #f8fafc);
  padding: 12px 16px;
  animation: slideDown 0.15s ease;
}
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.adv-filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
}
.adv-filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}
.adv-filter-group--range {
  min-width: 240px;
}
.adv-filter-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted, #64748b);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.adv-filter-select,
.adv-filter-input {
  padding: 6px 10px;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 7px;
  background: #fff;
  color: var(--ink, #1e293b);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
}
.adv-filter-select:focus,
.adv-filter-input:focus {
  border-color: var(--pink-500, #ec4899);
}
.adv-filter-range {
  display: flex;
  align-items: center;
  gap: 6px;
}
.adv-filter-range .adv-filter-input {
  width: 100px;
}
.adv-filter-sep {
  color: var(--muted, #94a3b8);
  font-size: 13px;
  font-weight: 600;
}
.adv-filter-reset {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid #dc2626;
  border-radius: 7px;
  background: transparent;
  color: #dc2626;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  align-self: flex-end;
  transition: all 0.15s;
}
.adv-filter-reset:hover {
  background: #dc2626;
  color: #fff;
}

/* ═══════════════════════════════════════════════════════════════════════════════
   RETURN MODAL - THEME SYSTEM
   ═══════════════════════════════════════════════════════════════════════════════ */
.return-modal-shell {
  background: #fff;
  border-radius: 20px;
  width: 880px;
  max-width: 100%;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(168, 27, 93, 0.28);
  border: 1px solid var(--pink-200, #ffcfe1);
  animation: modalIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.97) translateY(-8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* ── Header ── */
.cfm-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 24px;
  background: linear-gradient(135deg, var(--pink-400, #ec4899) 0%, var(--pink-600, #db2777) 100%);
  color: #fff;
}
.cfm-header-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}
.cfm-header-text {
  flex: 1;
  min-width: 0;
}
.cfm-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 2px;
  color: #fff;
}
.cfm-subtitle {
  font-size: 12px;
  margin: 0;
  opacity: 0.88;
}
.cfm-close {
  background: rgba(255, 255, 255, 0.18);
  border: none;
  color: #fff;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}
.cfm-close:hover {
  background: rgba(255, 255, 255, 0.32);
  transform: rotate(90deg);
}

/* ── Body ── */
.cfm-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ── Error Alert ── */
.cfm-error {
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fca5a5;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 13px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── Section ── */
.cfm-section {
  margin-bottom: 20px;
}
.cfm-section:last-child {
  margin-bottom: 0;
}

.cfm-section-title {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--pink-700, #a81b5d);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 6px;
  border-bottom: 2px solid var(--pink-100, #ffe6f0);
  letter-spacing: 0.03em;
}

/* ── Fields ── */
.cfm-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.cfm-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cfm-field--full {
  grid-column: 1 / -1;
}

.cfm-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted, #6b7280);
  display: flex;
  align-items: center;
  gap: 5px;
}
.cfm-required {
  color: #ef4444;
  font-weight: bold;
}

.cfm-input-wrap {
  position: relative;
  width: 100%;
}
.cfm-input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--pink-500, #db2777);
  opacity: 0.75;
  pointer-events: none;
  z-index: 1;
}

.cfm-input {
  width: 100%;
  padding: 9px 12px;
  border: 2px solid var(--pink-100, #ffe6f0);
  border-radius: 10px;
  font-size: 13px;
  background: var(--pink-50, #fff5f9);
  color: var(--ink, #1f2937);
  transition: all 0.2s ease;
  font-family: inherit;
  outline: none;
}
.cfm-input-wrap .cfm-input {
  padding-left: 36px;
}
.cfm-input:focus {
  border-color: var(--pink-400, #ec4899);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.15);
}
.cfm-input::placeholder {
  color: #d1d5db;
}
.cfm-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='%236b7280' viewBox='0 0 16 16'%3E%3Cpath d='M8 11L3 6h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
  cursor: pointer;
}

/* ── Footer ── */
.cfm-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  border-top: 1px solid var(--pink-100, #ffe6f0);
  background: #fff;
}

.cfm-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 10px;
  padding: 9px 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.cfm-btn--primary {
  background: linear-gradient(180deg, var(--pink-500, #db2777) 0%, var(--pink-700, #a81b5d) 100%);
  color: #fff;
  box-shadow:
    0 3px 0 var(--pink-700, #a81b5d),
    0 4px 12px rgba(168, 27, 93, 0.35);
  border-bottom: 3px solid var(--pink-700, #a81b5d);
}
.cfm-btn--primary:hover:not(:disabled) {
  background: linear-gradient(180deg, var(--pink-400, #ec4899) 0%, var(--pink-600, #db2777) 100%);
  transform: translateY(-1px);
  box-shadow:
    0 4px 0 var(--pink-700, #a81b5d),
    0 6px 16px rgba(168, 27, 93, 0.4);
}
.cfm-btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.cfm-btn--ghost {
  background: transparent;
  color: var(--muted, #6b7280);
  border: 2px solid var(--border-color-strong, #d1d5db);
}
.cfm-btn--ghost:hover {
  background: var(--pink-50, #fff5f9);
  border-color: var(--pink-300, #f7a8c8);
  color: var(--pink-700, #a81b5d);
}

/* ── Return Specific Elements ── */
.return-order-card {
  background: var(--pink-50, #fff5f9);
  border: 1.5px solid var(--pink-200, #ffcfe1);
  border-radius: 12px;
}
.return-order-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(219, 39, 119, 0.12);
  color: var(--pink-600, #db2777);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.return-order-code {
  font-family: monospace;
  font-weight: 700;
  color: var(--pink-700, #a81b5d);
  font-size: 13.5px;
}
.return-btn-change {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1.5px solid var(--pink-200, #ffcfe1);
  background: #fff;
  color: var(--pink-700, #a81b5d);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.return-btn-change:hover {
  background: var(--pink-100, #ffe6f0);
  border-color: var(--pink-400, #ec4899);
}
.return-order-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 180px;
  overflow-y: auto;
  background: #fff;
  border: 1.5px solid var(--pink-200, #ffcfe1);
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  z-index: 30;
}
.return-order-item {
  padding: 9px 14px;
  border-bottom: 1px solid var(--pink-50, #fff5f9);
  cursor: pointer;
  transition: background 0.15s;
}
.return-order-item:hover {
  background: var(--pink-50, #fff5f9);
}
.return-table-card {
  border: 1.5px solid var(--pink-200, #ffcfe1);
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}
.return-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}
.return-table th {
  background: var(--pink-50, #fff5f9);
  color: var(--pink-700, #a81b5d);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding: 10px 12px;
  border-bottom: 1.5px solid var(--pink-200, #ffcfe1);
}
.return-table td {
  padding: 9px 12px;
  border-bottom: 1px solid var(--pink-100, #ffe6f0);
  vertical-align: middle;
}
.return-table tr.is-selected {
  background: rgba(254, 242, 248, 0.45);
}
.return-table tr:last-child td {
  border-bottom: none;
}
.return-sku-badge {
  font-family: monospace;
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 7px;
  border-radius: 5px;
  display: inline-block;
}
.return-serial-badge {
  font-family: monospace;
  font-size: 11px;
  font-weight: 600;
  color: #0369a1;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
  padding: 2px 7px;
  border-radius: 5px;
  display: inline-block;
  line-height: 1.3;
}
.return-qty-input {
  width: 60px;
  padding: 5px 6px;
  border: 1.5px solid var(--pink-200, #ffcfe1);
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  background: #fff;
  color: var(--ink, #1f2937);
  outline: none;
  transition: border-color 0.15s;
}
.return-qty-input:focus {
  border-color: var(--pink-500, #db2777);
}
.return-cond-select {
  padding: 5px 8px;
  border: 1.5px solid var(--pink-200, #ffcfe1);
  border-radius: 8px;
  font-size: 12px;
  background: #fff;
  color: var(--ink, #1f2937);
  outline: none;
  transition: border-color 0.15s;
}
.return-cond-select:focus {
  border-color: var(--pink-500, #db2777);
}
.return-table-footer {
  background: var(--pink-50, #fff5f9);
  border-top: 1.5px solid var(--pink-200, #ffcfe1);
}
.return-table-loading,
.return-table-empty {
  padding: 24px;
  text-align: center;
  color: var(--muted, #6b7280);
  font-size: 13px;
  background: var(--pink-50, #fff5f9);
  border: 1.5px dashed var(--pink-200, #ffcfe1);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.return-handler-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 2px solid var(--pink-100, #ffe6f0);
  border-radius: 10px;
  background: var(--pink-50, #fff5f9);
  min-height: 42px;
}
.return-handler-avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--pink-500, #ec4899), var(--pink-700, #a81b5d));
  color: #fff;
  font-weight: 700;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.return-handler-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.return-handler-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink, #1f2937);
}
.return-handler-role {
  font-size: 11px;
  color: var(--muted, #6b7280);
}

@media (max-width: 600px) {
  .return-modal-shell {
    border-radius: 16px;
  }
  .cfm-fields {
    grid-template-columns: 1fr;
  }
  .cfm-field--full {
    grid-column: 1;
  }
  .cfm-header {
    padding: 16px;
  }
  .cfm-body {
    padding: 16px;
  }
  .cfm-footer {
    padding: 12px 16px;
  }
}
</style>
