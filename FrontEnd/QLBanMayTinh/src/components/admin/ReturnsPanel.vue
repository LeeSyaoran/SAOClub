<script setup>
import { ref, computed, onMounted, reactive, watch } from "vue";
import {
  Search, Filter, X, ChevronDown, ChevronUp,
  Hash, FileText, Package, User, DollarSign, CreditCard, Activity,
  SlidersHorizontal, RotateCcw, Clock, CheckCircle2, XCircle,
  Wallet, Banknote, Landmark, Eye, Edit3, Plus,
} from "@lucide/vue";
import { t } from "../../i18n/index.js";
import * as PhieuTraHangService from "../../services/PhieuTraHangService.js";
import * as ChiTietTraHangService from "../../services/ChiTietTraHangService.js";
import * as ChiTietDonHangService from "../../services/ChiTietDonHangService.js";
import { formatPrice } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { AuthStore } from "../../stores/index.js";
import { OrdersStore, ensureOrders } from "../../stores/orders.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { StaffStore, ensureStaff } from "../../stores/staff.js";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import {
  ReturnsStore,
  ensureReturns,
  refreshReturns,
} from "../../stores/returns.js";

const props = defineProps({
  readonly: { type: Boolean, default: false },
  canPickStaff: { type: Boolean, default: false },
});

onMounted(() => {
  ensureReturns();
  ensureOrders();
  ensureCustomers();
  ensureProducts();
  if (props.canPickStaff) ensureStaff();
});

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) =>
  (CustomersStore.items ?? []).find((c) => c.khachHangId === id)?.hoTen ?? (id > 0 ? `Khách #${id}` : 'Khách vãng lai');

const returnStats = computed(() => {
  const all = ReturnsStore?.items ?? [];
  const choXuLy = all.filter((r) => r.trangThai === 'cho_xu_ly').length;
  const daXuLy = all.filter((r) => r.trangThai === 'da_xu_ly').length;
  const tuChoi = all.filter((r) => r.trangThai === 'tu_choi').length;
  const tongTien = all
    .filter((r) => r.trangThai === 'da_xu_ly')
    .reduce((sum, r) => sum + (Number(r.soTienHoan) || 0), 0);
  return { total: all.length, choXuLy, daXuLy, tuChoi, tongTien };
});
const productByBienThe = (bienTheId) =>
  (ProductsStore.items ?? []).find((p) => p.bienTheId === bienTheId);
const staffName = (id) =>
  (StaffStore.items ?? []).find((s) => s.nhanVienId === id)?.hoTen ?? "—";
const staffOptions = computed(() =>
  (StaffStore.items ?? []).map((s) => ({ nhanVienId: s.nhanVienId, hoTen: s.hoTen })),
);
const orderById = (donHangId) =>
  (OrdersStore.items ?? []).find((o) => o.donHangId === donHangId);

const STATUS_COLOR = {
  cho_xu_ly: { bg: "#fde68a", text: "#92400e" },
  da_xu_ly: { bg: "#bbf7d0", text: "#166534" },
  tu_choi: { bg: "#fecaca", text: "#991b1b" },
};
const statusColor = (s) =>
  STATUS_COLOR[s] ?? { bg: "#e5e7eb", text: "#374151" };
const statusLabel = (s) => t(`admin.returnStatus.${s}`);
const hinhThucHoanLabel = (h) => t(`admin.hinhThucHoan.${h}`);

// ── Bộ lọc nâng cao: Trả hàng ───────────────────────────────────────────────
const search = ref("");
const isFilterOpen = ref(false);
const filters = reactive({
  trangThai: "",      // '' | 'cho_xu_ly' | 'da_xu_ly' | 'tu_choi'
  hinhThucHoan: "",   // '' | 'vi' | 'tien_mat' | 'chuyen_khoan'
  ngayFrom: "",       // YYYY-MM-DD
  ngayTo: "",
  tienMin: "",
  tienMax: "",
});

const activeFilterCount = computed(() => {
  return [filters.trangThai, filters.hinhThucHoan, filters.ngayFrom, filters.ngayTo,
    filters.tienMin !== "" ? filters.tienMin : "", filters.tienMax !== "" ? filters.tienMax : ""
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
  const q = search.value.trim().toLowerCase();
  return items.filter((p) => {
    // text search
    if (q) {
      const name = customerName(orderById(p.donHangId)?.khachHangId ?? -1).toLowerCase();
      const match = String(p.phieuTraId).includes(q) || (p.maPhieu ?? "").toLowerCase().includes(q) || name.includes(q);
      if (!match) return false;
    }
    if (filters.trangThai && p.trangThai !== filters.trangThai) return false;
    if (filters.hinhThucHoan && p.hinhThucHoan !== filters.hinhThucHoan) return false;
    // ngày trả
    if (filters.ngayFrom && (p.ngayTra ?? '').slice(0, 10) < filters.ngayFrom) return false;
    if (filters.ngayTo   && (p.ngayTra ?? '').slice(0, 10) > filters.ngayTo)   return false;
    // tiền hoàn range
    const tien = Number(p.soTienHoan ?? 0);
    if (filters.tienMin !== "" && tien < Number(filters.tienMin)) return false;
    if (filters.tienMax !== "" && tien > Number(filters.tienMax)) return false;
    return true;
  });
});
const { currentPage, totalPages, pagedItems: pagedReturns, pageSize } = usePagination(filteredReturns);
watch([search, () => filters.trangThai, () => filters.hinhThucHoan, () => filters.ngayFrom, () => filters.ngayTo, () => filters.tienMin, () => filters.tienMax], () => {
  currentPage.value = 0;
});

// ── Modal tao/sua/xem ─────────────────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const saving = ref(false);
const orderSearch = ref("");
const selectedOrder = ref(null);
const lineItems = ref([]); // [{ id, bienTheId, chiTietId, maSku, soSerial, donGia, soLuongDaMua, soLuongTra, tinhTrang, checked }]
const orderLinesLoading = ref(false);
const khachCoMat = ref(false); // checkbox gate hình thức hoàn — không lưu DB

const emptyForm = () => ({
  donHangId: null,
  nhanVienId: props.canPickStaff ? "" : (AuthStore.user?.id ?? null),
  lyDo: "",
  ngayTra: nowLocalIso().slice(0, 16),
  trangThai: "cho_xu_ly",
  soTienHoan: 0,
  hinhThucHoan: "vi",
  ghiChu: "",
});
const form = ref(emptyForm());

const searchedOrders = computed(() => {
  const q = orderSearch.value.trim().toLowerCase();
  if (!q) return [];
  return (OrdersStore.items ?? [])
    .filter(
      (o) =>
        String(o.donHangId).includes(q) ||
        (o.maDonHang ?? "").toLowerCase().includes(q) ||
        customerName(o.khachHangId).toLowerCase().includes(q) ||
        (o.sdtNguoiNhan ?? "").includes(q),
    )
    .slice(0, 10);
});

const recalcSoTienHoan = () => {
  form.value.soTienHoan = lineItems.value
    .filter((l) => l.checked)
    .reduce(
      (s, l) => s + (Number(l.donGia) || 0) * (Number(l.soLuongTra) || 0),
      0,
    );
};

// HTML min/max chỉ chặn nút mũi tên spinner, gõ tay vẫn nhập được số ngoài khoảng —
// kẹp lại đúng [1, soLuongDaMua] mỗi khi đổi, tránh soTienHoan tính sai theo số lượng ảo
// (backend đã chặn ở ChiTietTraHangService nhưng kẹp ở đây để báo sai ngay lúc nhập).
const clampSoLuongTra = (l) => {
  const n = Math.trunc(Number(l.soLuongTra)) || 1;
  l.soLuongTra = Math.min(Math.max(n, 1), l.soLuongDaMua);
  recalcSoTienHoan();
};

const loadOrderLines = async (donHangId, existingLines = []) => {
  orderLinesLoading.value = true;
  try {
    const items = await ChiTietDonHangService.getByDonHang(donHangId).catch(
      () => [],
    );
    lineItems.value = items.map((i) => {
      const existed = existingLines.find(
        (c) => c.bienTheId === i.bienTheId && c.chiTietId === i.chiTietId,
      );
      return {
        id: existed?.id ?? null,
        bienTheId: i.bienTheId,
        chiTietId: i.chiTietId,
        maSku: i.maSku,
        soSerial: i.soSerial,
        donGia: i.donGia,
        soLuongDaMua: i.soLuong,
        soLuongTra: existed?.soLuong ?? i.soLuong,
        tinhTrang: existed?.tinhTrang ?? "tot",
        checked: !!existed,
      };
    });
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
  form.value = emptyForm();
  selectedOrder.value = null;
  orderSearch.value = "";
  lineItems.value = [];
  khachCoMat.value = false;
  formError.value = "";
  showModal.value = true;
};

const openDetail = async (p) => {
  editingId.value = p.phieuTraId;
  form.value = {
    donHangId: p.donHangId,
    nhanVienId: p.nhanVienId,
    lyDo: p.lyDo,
    ngayTra: p.ngayTra ? p.ngayTra.slice(0, 16) : nowLocalIso().slice(0, 16),
    trangThai: p.trangThai,
    soTienHoan: p.soTienHoan,
    hinhThucHoan: p.hinhThucHoan,
    ghiChu: p.ghiChu ?? "",
  };
  selectedOrder.value = orderById(p.donHangId) ?? null;
  khachCoMat.value = p.hinhThucHoan === "tien_mat";
  formError.value = "";
  const allLines = await ChiTietTraHangService.getAll().catch(() => []);
  const mine = allLines.filter((c) => c.phieuTraId === p.phieuTraId);
  await loadOrderLines(p.donHangId, mine);
  showModal.value = true;
};

const saveReturn = async () => {
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
  if (checkedLines.length === 0) {
    formError.value = t("admin.returnModal.lineRequired");
    return;
  }

  if (saving.value) return;
  saving.value = true;
  try {
    const headerBody = {
      donHangId: form.value.donHangId,
      nhanVienId: form.value.nhanVienId ? Number(form.value.nhanVienId) : null,
      lyDo: form.value.lyDo,
      ngayTra: nowLocalIso(new Date(form.value.ngayTra)),
      trangThai: form.value.trangThai,
      soTienHoan: form.value.soTienHoan,
      hinhThucHoan: form.value.hinhThucHoan,
      ghiChu: form.value.ghiChu || "—",
    };
    const res = await PhieuTraHangService.save(editingId.value, headerBody);
    if (!res.ok) {
      formError.value = t("admin.errors.saveFailed", {
        status: res.status,
        text: await res.text(),
      });
      return;
    }

    let phieuTraId = editingId.value;
    if (!phieuTraId) {
      const created = await res.json();
      phieuTraId = created.phieuTraId;
    }

    const originalIds = checkedLines.filter((l) => l.id).map((l) => l.id);
    const allExisting = editingId.value
      ? await ChiTietTraHangService.getAll().catch(() => [])
      : [];
    const mineExisting = allExisting
      .filter((c) => c.phieuTraId === phieuTraId)
      .map((c) => c.id);
    for (const oldId of mineExisting.filter(
      (id) => !originalIds.includes(id),
    )) {
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
      if (l.id) await ChiTietTraHangService.update(l.id, body);
      else await ChiTietTraHangService.create(body);
    }

    showModal.value = false;
    await refreshReturns();
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
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Tổng phiếu trả</div>
            <div class="fs-4 fw-bold mt-1" style="color:var(--text-heading);">{{ returnStats.total }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(168,85,247,0.12);color:#a855f7;">
            <RotateCcw :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Chờ xử lý</div>
            <div class="fs-4 fw-bold mt-1 text-warning">{{ returnStats.choXuLy }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(234,179,8,0.12);color:#eab308;">
            <Clock :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div>
            <div class="text-secondary small fw-semibold" style="font-size:11.5px;">Đã xử lý</div>
            <div class="fs-4 fw-bold mt-1 text-success">{{ returnStats.daXuLy }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(34,197,94,0.12);color:#22c55e;">
            <CheckCircle2 :size="20" />
          </div>
        </div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border shadow-sm rounded-3 p-3 h-100" style="background:var(--bg-card); border-color:var(--border-color-soft) !important;">
        <div class="d-flex align-items-center justify-content-between">
          <div class="min-w-0 me-2">
            <div class="text-secondary small fw-semibold text-truncate" style="font-size:11.5px;">Tổng tiền đã hoàn</div>
            <div class="fs-5 fw-bold mt-1 text-danger font-monospace text-truncate">{{ formatPrice(returnStats.tongTien) }}</div>
          </div>
          <div class="rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:42px;height:42px;background:rgba(239,68,68,0.12);color:#ef4444;">
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
        <button v-if="activeFilterCount > 0" class="alt-btn alt-btn--ghost-sm" @click="resetFilters">
          <X :size="13" /> Xóa lọc
        </button>
        <button v-if="!readonly" class="alt-btn alt-btn--primary d-inline-flex align-items-center gap-1.5" @click="openAdd">
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
            <input v-model="filters.tienMin" type="number" min="0" placeholder="Từ" class="adv-filter-input" />
            <span class="adv-filter-sep">–</span>
            <input v-model="filters.tienMax" type="number" min="0" placeholder="Đến" class="adv-filter-input" />
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
            <th style="width: 45px"><span class="d-inline-flex align-items-center gap-1"><Hash :size="12" /> {{ t("admin.common.stt") }}</span></th>
            <th style="width: 90px"><span class="d-inline-flex align-items-center gap-1">{{ t("admin.returns.colId") }}</span></th>
            <th><span class="d-inline-flex align-items-center gap-1">{{ t("admin.returns.colOrder") }}</span></th>
            <th><span class="d-inline-flex align-items-center gap-1"><User :size="12" /> {{ t("admin.returns.colCustomer") }}</span></th>
            <th><span class="d-inline-flex align-items-center gap-1"><DollarSign :size="12" /> {{ t("admin.returns.colAmount") }}</span></th>
            <th><span class="d-inline-flex align-items-center gap-1"><CreditCard :size="12" /> {{ t("admin.returns.colHinhThucHoan") }}</span></th>
            <th><span class="d-inline-flex align-items-center gap-1"><Activity :size="12" /> {{ t("admin.returns.colStatus") }}</span></th>
            <th style="width: 100px"><span class="d-inline-flex align-items-center gap-1"><SlidersHorizontal :size="12" /> {{ t("admin.returns.colAction") }}</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(p, idx) in pagedReturns" :key="p.phieuTraId">
            <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
            <td>
              <span class="badge font-monospace bg-body-secondary text-body-secondary border px-2 py-1">
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
                <span :class="{ 'text-muted fst-italic': !orderById(p.donHangId)?.khachHangId || orderById(p.donHangId)?.khachHangId <= 0 }">
                  {{ customerName(orderById(p.donHangId)?.khachHangId ?? -1) }}
                </span>
              </div>
            </td>
            <td>
              <span class="fw-bold font-monospace" :class="Number(p.soTienHoan) > 0 ? 'text-danger' : 'text-muted'">
                {{ formatPrice(p.soTienHoan) }}
              </span>
            </td>
            <td>
              <span v-if="p.hinhThucHoan === 'vi'" class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <Wallet :size="12" /> Ví điện tử
              </span>
              <span v-else-if="p.hinhThucHoan === 'tien_mat'" class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <Banknote :size="12" /> Tiền mặt
              </span>
              <span v-else-if="p.hinhThucHoan === 'chuyen_khoan'" class="badge rounded-pill bg-primary-subtle text-primary border border-primary-subtle px-2 py-1 d-inline-flex align-items-center gap-1" style="font-size:11px;">
                <Landmark :size="12" /> Chuyển khoản
              </span>
              <span v-else class="badge rounded-pill bg-secondary-subtle text-secondary border px-2 py-1" style="font-size:11px;">
                {{ hinhThucHoanLabel(p.hinhThucHoan) }}
              </span>
            </td>
            <td>
              <span v-if="p.trangThai === 'cho_xu_ly'" class="badge rounded-pill bg-warning-subtle text-warning border border-warning-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1" style="font-size:11.5px;">
                <Clock :size="12" /> {{ statusLabel(p.trangThai) }}
              </span>
              <span v-else-if="p.trangThai === 'da_xu_ly'" class="badge rounded-pill bg-success-subtle text-success border border-success-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1" style="font-size:11.5px;">
                <CheckCircle2 :size="12" /> {{ statusLabel(p.trangThai) }}
              </span>
              <span v-else-if="p.trangThai === 'tu_choi'" class="badge rounded-pill bg-danger-subtle text-danger border border-danger-subtle px-2.5 py-1 d-inline-flex align-items-center gap-1" style="font-size:11.5px;">
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
                  style="font-size:12px;"
                  @click="openDetail(p)"
                >
                  <Eye v-if="readonly" :size="13" />
                  <Edit3 v-else :size="13" />
                  <span>{{ readonly ? t("admin.returns.view") : t("admin.returns.edit") }}</span>
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
      <div v-if="totalPages > 1" class="alt-pager"><Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" /></div>
    </div>
  </div>

  <div
    v-if="showModal"
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background: var(--bg-overlay); z-index: 1000"
    @click.self="showModal = false"
  >
    <div
      class="rounded-3 p-3"
      style="
        background: var(--bg-card);
        width: 640px;
        max-width: 96vw;
        max-height: 90vh;
        overflow-y: auto;
      "
    >
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color: var(--text-heading)">
          {{
            editingId
              ? t("admin.returnModal.titleEdit")
              : t("admin.returnModal.titleAdd")
          }}
        </div>
        <button
          class="btn-close btn-sm"
          :aria-label="t('common.close')"
          @click="showModal = false"
        ></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">
        {{ formError }}
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{
          t("admin.returnModal.orderLabel")
        }}</label>
        <div
          v-if="selectedOrder"
          class="d-flex align-items-center justify-content-between p-2 rounded-2"
          style="background: var(--bg-input)"
        >
          <span>{{ selectedOrder.maDonHang || "#" + selectedOrder.donHangId }} —
            {{ customerName(selectedOrder.khachHangId) }}</span>
          <button
            v-if="!editingId"
            class="btn btn-sm btn-outline-secondary"
            style="font-size: 0.72rem"
            @click="
              selectedOrder = null;
              form.donHangId = null;
              lineItems = [];
            "
          >
            {{ t("admin.returnModal.changeOrder") }}
          </button>
        </div>
        <template v-else>
          <input
            v-model="orderSearch"
            class="form-control form-control-sm"
            style="
              background: var(--bg-input);
              color: var(--text-primary);
              border-color: var(--border-color-strong);
            "
            :placeholder="t('admin.returnModal.orderSearchPlaceholder')"
          />
          <div
            v-if="orderSearch.trim()"
            class="mt-1 rounded-2 overflow-hidden"
            style="
              max-height: 160px;
              overflow-y: auto;
              border: 1px solid var(--border-color-soft);
            "
          >
            <div
              v-for="o in searchedOrders"
              :key="o.donHangId"
              class="p-2"
              style="cursor: pointer"
              @click="pickOrder(o)"
            >
              {{ o.maDonHang || "#" + o.donHangId }} —
              {{ customerName(o.khachHangId) }}
            </div>
            <div
              v-if="searchedOrders.length === 0"
              class="p-2 text-secondary small"
            >
              {{ t("admin.returnModal.orderSearchEmpty") }}
            </div>
          </div>
        </template>
      </div>

      <!-- Danh sach dong san pham -->
      <div v-if="selectedOrder" class="mb-2">
        <label class="form-label small text-secondary mb-1">{{
          t("admin.returnModal.lineItemsTitle")
        }}</label>
        <div v-if="orderLinesLoading" class="text-secondary small">
          {{ t("admin.returns.loading") }}
        </div>
        <table v-else class="w-100 mb-0" style="font-size: 0.8rem">
          <thead>
            <tr style="background: var(--bg-input)">
              <th class="px-2 py-1" style="width: 26px"></th>
              <th class="px-2 py-1">{{ t("admin.returnModal.colProduct") }}</th>
              <th class="px-2 py-1">{{ t("admin.returnModal.colSku") }}</th>
              <th class="px-2 py-1 text-center">
                {{ t("admin.returnModal.colBought") }}
              </th>
              <th class="px-2 py-1 text-center">
                {{ t("admin.returnModal.colReturnQty") }}
              </th>
              <th class="px-2 py-1">
                {{ t("admin.returnModal.colCondition") }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="l in lineItems"
              :key="`${l.bienTheId}-${l.chiTietId}`"
              style="border-top: 1px solid var(--border-color-soft)"
            >
              <td class="px-2 py-1">
                <input
                  v-model="l.checked"
                  type="checkbox"
                  :disabled="readonly"
                  @change="recalcSoTienHoan"
                />
              </td>
              <td class="px-2 py-1">
                {{ productByBienThe(l.bienTheId)?.tenSanPham || "—" }}
              </td>
              <td
                class="px-2 py-1 text-secondary"
                style="font-family: monospace"
              >
                {{ l.maSku
                }}<span v-if="l.soSerial" class="text-info">
                  · SN {{ l.soSerial }}</span>
              </td>
              <td class="px-2 py-1 text-center">{{ l.soLuongDaMua }}</td>
              <td class="px-2 py-1 text-center">
                <input
                  v-model.number="l.soLuongTra"
                  type="number"
                  min="1"
                  :max="l.soLuongDaMua"
                  :disabled="readonly || !l.checked"
                  class="form-control form-control-sm"
                  style="
                    width: 64px;
                    background: var(--bg-input);
                    color: var(--text-primary);
                  "
                  @change="clampSoLuongTra(l)"
                />
              </td>
              <td class="px-2 py-1">
                <select
                  v-model="l.tinhTrang"
                  :disabled="readonly || !l.checked"
                  class="form-select form-select-sm"
                  style="
                    background: var(--bg-input);
                    color: var(--text-primary);
                  "
                >
                  <option value="tot">
                    {{ t("admin.returnModal.conditionGood") }}
                  </option>
                  <option value="loi">
                    {{ t("admin.returnModal.conditionBad") }}
                  </option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="row g-2 mb-2">
        <div v-if="canPickStaff" class="col-6">
          <label class="form-label small text-secondary mb-1">{{
            t("admin.returnModal.staffLabel")
          }}</label>
          <select
            v-model="form.nhanVienId"
            :disabled="readonly"
            class="form-select form-select-sm"
            style="
              background: var(--bg-input);
              color: var(--text-primary);
              border-color: var(--border-color-strong);
            "
          >
            <option value="">—</option>
            <option
              v-for="s in staffOptions"
              :key="s.nhanVienId"
              :value="s.nhanVienId"
            >
              {{ s.hoTen }}
            </option>
          </select>
        </div>
        <div v-else class="col-6">
          <label class="form-label small text-secondary mb-1">{{
            t("admin.returnModal.staffLabel")
          }}</label>
          <div
            class="form-control form-control-sm"
            style="
              background: var(--bg-input);
              color: var(--text-secondary);
              border-color: var(--border-color-strong);
            "
          >
            {{ staffName(form.nhanVienId) }}
          </div>
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{
            t("admin.returnModal.dateLabel")
          }}</label>
          <input
            v-model="form.ngayTra"
            type="datetime-local"
            :disabled="readonly"
            class="form-control form-control-sm"
            style="
              background: var(--bg-input);
              color: var(--text-primary);
              border-color: var(--border-color-strong);
            "
          />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{
          t("admin.returnModal.reasonLabel")
        }}</label>
        <input
          v-model="form.lyDo"
          :disabled="readonly"
          class="form-control form-control-sm"
          style="
            background: var(--bg-input);
            color: var(--text-primary);
            border-color: var(--border-color-strong);
          "
        />
      </div>

      <div class="row g-2 mb-2 align-items-end">
        <div class="col-4">
          <label class="form-label small text-secondary mb-1">{{
            t("admin.returnModal.amountLabel")
          }}</label>
          <input
            v-model.number="form.soTienHoan"
            type="number"
            min="0"
            :disabled="readonly"
            class="form-control form-control-sm"
            style="
              background: var(--bg-input);
              color: var(--text-primary);
              border-color: var(--border-color-strong);
            "
          />
        </div>
        <div class="col-4">
          <div class="form-check mb-1">
            <input
              id="khachCoMat"
              v-model="khachCoMat"
              type="checkbox"
              class="form-check-input"
              :disabled="readonly"
              @change="
                () => {
                  if (!khachCoMat && form.hinhThucHoan === 'tien_mat')
                    form.hinhThucHoan = 'vi';
                }
              "
            />
            <label
              class="form-check-label small text-secondary"
              for="khachCoMat"
            >{{ t("admin.returnModal.customerPresentLabel") }}</label>
          </div>
        </div>
        <div class="col-4">
          <label class="form-label small text-secondary mb-1">{{
            t("admin.returnModal.hinhThucHoanLabel")
          }}</label>
          <select
            v-model="form.hinhThucHoan"
            :disabled="readonly"
            class="form-select form-select-sm"
            style="
              background: var(--bg-input);
              color: var(--text-primary);
              border-color: var(--border-color-strong);
            "
          >
            <option value="vi">{{ t("admin.hinhThucHoan.vi") }}</option>
            <option value="tien_mat" :disabled="!khachCoMat">
              {{ t("admin.hinhThucHoan.tien_mat") }}
            </option>
          </select>
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{
          t("admin.returnModal.statusLabel")
        }}</label>
        <select
          v-model="form.trangThai"
          :disabled="readonly"
          class="form-select form-select-sm"
          style="
            background: var(--bg-input);
            color: var(--text-primary);
            border-color: var(--border-color-strong);
          "
        >
          <option value="cho_xu_ly">
            {{ t("admin.returnStatus.cho_xu_ly") }}
          </option>
          <option value="da_xu_ly">
            {{ t("admin.returnStatus.da_xu_ly") }}
          </option>
          <option value="tu_choi">{{ t("admin.returnStatus.tu_choi") }}</option>
        </select>
      </div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{
          t("admin.returnModal.noteLabel")
        }}</label>
        <input
          v-model="form.ghiChu"
          :disabled="readonly"
          class="form-control form-control-sm"
          style="
            background: var(--bg-input);
            color: var(--text-primary);
            border-color: var(--border-color-strong);
          "
        />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button
          class="btn btn-sm btn-outline-secondary"
          @click="showModal = false"
        >
          {{
            readonly
              ? t("admin.returnModal.close")
              : t("admin.returnModal.cancel")
          }}
        </button>
        <button
          v-if="!readonly"
          class="btn btn-sm btn-warning text-dark fw-bold"
          :disabled="saving"
          @click="saveReturn"
        >
          {{ t("admin.returnModal.save") }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ─── Filter Button ─── */
.alt-btn--filter {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px; background: var(--surface, #fff);
  color: var(--ink, #1e293b); font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.15s ease;
}
.alt-btn--filter:hover, .alt-btn--filter-active {
  border-color: var(--pink-400, #f472b6);
  background: var(--pink-50, #fdf2f8); color: var(--pink-700, #be185d);
}
.filter-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px;
  background: var(--pink-600, #db2777); color: #fff; font-size: 11px; font-weight: 700;
}
.alt-btn--ghost-sm {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 5px 10px; border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px; background: transparent; color: var(--muted, #64748b);
  font-size: 12px; cursor: pointer; transition: all 0.15s ease;
}
.alt-btn--ghost-sm:hover { background: #fee2e2; color: #dc2626; border-color: #dc2626; }

/* ─── Advanced Filter Panel ─── */
.adv-filter-panel {
  border-top: 1px solid var(--border, #e2e8f0);
  background: var(--surface-alt, #f8fafc);
  padding: 12px 16px;
  animation: slideDown 0.15s ease;
}
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}
.adv-filter-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 12px; }
.adv-filter-group { display: flex; flex-direction: column; gap: 4px; min-width: 140px; }
.adv-filter-group--range { min-width: 240px; }
.adv-filter-label { font-size: 11px; font-weight: 600; color: var(--muted, #64748b); text-transform: uppercase; letter-spacing: 0.04em; }
.adv-filter-select, .adv-filter-input {
  padding: 6px 10px; border: 1px solid var(--border, #e2e8f0);
  border-radius: 7px; background: #fff; color: var(--ink, #1e293b);
  font-size: 13px; outline: none; transition: border-color 0.15s; width: 100%;
}
.adv-filter-select:focus, .adv-filter-input:focus { border-color: var(--pink-500, #ec4899); }
.adv-filter-range { display: flex; align-items: center; gap: 6px; }
.adv-filter-range .adv-filter-input { width: 100px; }
.adv-filter-sep { color: var(--muted, #94a3b8); font-size: 13px; font-weight: 600; }
.adv-filter-reset {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1px solid #dc2626; border-radius: 7px;
  background: transparent; color: #dc2626; font-size: 12px; font-weight: 500;
  cursor: pointer; align-self: flex-end; transition: all 0.15s;
}
.adv-filter-reset:hover { background: #dc2626; color: #fff; }
</style>
