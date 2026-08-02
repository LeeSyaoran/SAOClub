<script setup>
import { ref, computed, onMounted } from "vue";
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
  (CustomersStore.items ?? []).find((c) => c.khachHangId === id)?.hoTen ?? `KH#${id}`;
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

// ── Bo loc + danh sach ──────────────────────────────────────────────────────
const search = ref("");
const filteredReturns = computed(() => {
  const items = ReturnsStore?.items ?? [];
  const q = search.value.trim().toLowerCase();
  if (!q) return items;
  return items.filter((p) => {
    const name = customerName(
      orderById(p.donHangId)?.khachHangId ?? -1,
    ).toLowerCase();
    return (
      String(p.phieuTraId).includes(q) ||
      (p.maPhieu ?? "").toLowerCase().includes(q) ||
      name.includes(q)
    );
  });
});
const { currentPage, totalPages, pagedItems: pagedReturns, pageSize } = usePagination(filteredReturns);

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

const deleteReturn = async (id) => {
  if (!(await askConfirm(t("admin.confirm.deleteReturn")))) return;
  const res = await PhieuTraHangService.remove(id);
  if (!res.ok) {
    showToast(
      await res
        .text()
        .catch(() => t("admin.errors.deleteFailed", { status: res.status })),
    );
    return;
  }
  await refreshReturns();
};
</script>

<template>
  <div
    class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"
  >
    <span class="text-secondary small"
      >{{ filteredReturns.length }}/{{ (ReturnsStore?.items ?? []).length }}
      {{ t("admin.returns.countSuffix") }}</span
    >
    <div class="d-flex gap-2 flex-wrap">
      <input
        v-model="search"
        class="form-control form-control-sm"
        style="
          width: 240px;
          background: var(--bg-input);
          border-color: var(--border-color-strong);
          color: var(--text-primary);
        "
        :placeholder="t('admin.returns.searchPlaceholder')"
      />
      <button
        v-if="!readonly"
        class="btn btn-sm btn-warning text-dark fw-bold"
        @click="openAdd"
      >
        {{ t("admin.returns.add") }}
      </button>
    </div>
  </div>

  <div v-if="ReturnsStore.loading" class="text-secondary small">
    {{ t("admin.returns.loading") }}
  </div>
  <div v-else class="table-responsive">
    <table
      class="table table-hover table-sm align-middle"
      style="
        --bs-table-bg: var(--bg-card);
        --bs-table-color: var(--text-primary);
        --bs-table-hover-bg: var(--bg-hover);
        --bs-table-hover-color: var(--text-primary);
        --bs-table-border-color: var(--border-color-soft);
      "
    >
      <thead>
        <tr>
          <th style="width: 40px">{{ t("admin.common.stt") }}</th>
          <th>{{ t("admin.returns.colId") }}</th>
          <th>{{ t("admin.returns.colOrder") }}</th>
          <th>{{ t("admin.returns.colCustomer") }}</th>
          <th>{{ t("admin.returns.colAmount") }}</th>
          <th>{{ t("admin.returns.colHinhThucHoan") }}</th>
          <th>{{ t("admin.returns.colStatus") }}</th>
          <th>{{ t("admin.returns.colAction") }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(p, idx) in pagedReturns" :key="p.phieuTraId">
          <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
          <td class="text-secondary" style="font-family: monospace">
            {{ p.maPhieu || "#" + p.phieuTraId }}
          </td>
          <td class="text-secondary">
            {{ orderById(p.donHangId)?.maDonHang || "#" + p.donHangId }}
          </td>
          <td>{{ customerName(orderById(p.donHangId)?.khachHangId ?? -1) }}</td>
          <td class="text-warning fw-semibold">
            {{ formatPrice(p.soTienHoan) }}
          </td>
          <td class="text-secondary">
            {{ hinhThucHoanLabel(p.hinhThucHoan) }}
          </td>
          <td>
            <span
              class="badge"
              :style="{
                background: statusColor(p.trangThai).bg,
                color: statusColor(p.trangThai).text,
              }"
              >{{ statusLabel(p.trangThai) }}</span
            >
          </td>
          <td>
            <div class="d-flex gap-1">
              <button
                class="btn btn-sm btn-outline-warning"
                style="font-size: 0.78rem; padding: 2px 8px"
                @click="openDetail(p)"
              >
                {{
                  readonly ? t("admin.returns.view") : t("admin.returns.edit")
                }}
              </button>
              <button
                v-if="!readonly"
                class="btn btn-sm btn-outline-danger"
                style="font-size: 0.78rem; padding: 2px 8px"
                @click="deleteReturn(p.phieuTraId)"
              >
                {{ t("admin.returns.delete") }}
              </button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredReturns.length === 0">
          <td colspan="8" class="text-center text-secondary">
            {{ t("admin.returns.empty") }}
          </td>
        </tr>
      </tbody>
    </table>
    <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
  </div>

  <!-- ══ MODAL PHIEU TRA HANG ══ -->
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

      <!-- Chon don hang -->
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{
          t("admin.returnModal.orderLabel")
        }}</label>
        <div
          v-if="selectedOrder"
          class="d-flex align-items-center justify-content-between p-2 rounded-2"
          style="background: var(--bg-input)"
        >
          <span
            >{{ selectedOrder.maDonHang || "#" + selectedOrder.donHangId }} —
            {{ customerName(selectedOrder.khachHangId) }}</span
          >
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
                  type="checkbox"
                  v-model="l.checked"
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
                  · SN {{ l.soSerial }}</span
                >
              </td>
              <td class="px-2 py-1 text-center">{{ l.soLuongDaMua }}</td>
              <td class="px-2 py-1 text-center">
                <input
                  type="number"
                  min="1"
                  :max="l.soLuongDaMua"
                  v-model.number="l.soLuongTra"
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
        <div class="col-6" v-if="canPickStaff">
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
        <div class="col-6" v-else>
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
            type="datetime-local"
            v-model="form.ngayTra"
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
            type="number"
            min="0"
            v-model.number="form.soTienHoan"
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
              type="checkbox"
              class="form-check-input"
              id="khachCoMat"
              v-model="khachCoMat"
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
              >{{ t("admin.returnModal.customerPresentLabel") }}</label
            >
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
