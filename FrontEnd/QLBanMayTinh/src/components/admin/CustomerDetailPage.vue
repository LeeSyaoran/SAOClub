<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { CustomersStore, refreshCustomers } from "../../stores/customers.js";
import { OrdersStore } from "../../stores/orders.js";
import * as PhieuGiamGiaCaNhanService from "../../Service/PhieuGiamGiaCaNhanService.js";
import * as KhachHangService from "../../Service/KhachHangService.js";
import { formatPrice, formatDate, formatDateTime, statusLabel } from "../../utils/adminFormat.js";
import { orderStatusLabel, orderStatusColor } from "../../utils/orderStatus.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import TangDiemModal from "./TangDiemModal.vue";
import TangVoucherModal from "./TangVoucherModal.vue";

const props = defineProps({
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["back"]);

const customer = computed(() =>
  CustomersStore.items.find((c) => c.khachHangId === props.customerId) ?? null,
);

const customerOrders = computed(() =>
  OrdersStore.items
    .filter((o) => o.khachHangId === props.customerId)
    .sort((a, b) => new Date(b.ngayDat) - new Date(a.ngayDat)),
);

const totalSpent = computed(() =>
  customerOrders.value
    .filter((o) => o.trangThaiDonHang !== "cancelled")
    .reduce((sum, o) => sum + (o.thanhTien || 0), 0),
);

const vouchers = ref([]);
const vouchersLoading = ref(true);
const pointHistory = ref([]);
const pointHistoryLoading = ref(true);

const loadVouchers = async () => {
  vouchersLoading.value = true;
  try {
    vouchers.value = await PhieuGiamGiaCaNhanService.getByKhachHang(props.customerId);
  } catch {
    vouchers.value = [];
  } finally {
    vouchersLoading.value = false;
  }
};

const loadPointHistory = async () => {
  pointHistoryLoading.value = true;
  try {
    pointHistory.value = await KhachHangService.getLichSuDiem(props.customerId);
  } catch {
    pointHistory.value = [];
  } finally {
    pointHistoryLoading.value = false;
  }
};

onMounted(() => {
  loadVouchers();
  loadPointHistory();
});

const voucherStatus = (v) => {
  if (v.daSuDung) return { key: "voucherStatusUsed", cls: "bg-secondary" };
  if (new Date(v.ngayHetHan) < new Date()) return { key: "voucherStatusExpired", cls: "bg-danger" };
  return { key: "voucherStatusActive", cls: "bg-success" };
};

const customerFormModalRef = ref(null);
const showCustomerModal = ref(false);
const showGiftPointsModal = ref(false);
const showGiftVoucherModal = ref(false);
</script>

<template>
  <div v-if="!customer" class="text-secondary small">{{ t('admin.customerDetail.notFound') }}</div>
  <div v-else>
    <button class="btn btn-sm btn-outline-secondary mb-3" @click="emit('back')">{{ t('admin.customerDetail.back') }}</button>

    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-4">
      <div>
        <div class="fw-black fs-4" style="color:var(--text-heading);">{{ customer.hoTen }}</div>
        <div class="text-secondary small">{{ customer.soDienThoai }} · {{ customer.email || '—' }}</div>
        <span class="badge mt-1" :class="customer.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(customer.trangThai) }}</span>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <button class="btn btn-sm btn-outline-warning" @click="customerFormModalRef.openForEdit(customer)">{{ t('admin.customerDetail.edit') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="showGiftPointsModal = true">{{ t('admin.customerDetail.giftPoints') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="showGiftVoucherModal = true">{{ t('admin.customerDetail.giftVoucher') }}</button>
      </div>
    </div>

    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiTotalSpent') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ formatPrice(totalSpent) }}</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiOrderCount') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ customerOrders.length }}</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiPoints') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ customer.diemTichLuy ?? 0 }}</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiLastOrder') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ customerOrders[0] ? formatDate(customerOrders[0].ngayDat) : t('admin.customerDetail.noLastOrder') }}</div>
        </div>
      </div>
    </div>

    <div class="mb-4">
      <div class="fw-bold mb-2">{{ t('admin.customerDetail.ordersTitle') }}</div>
      <div v-if="customerOrders.length === 0" class="text-secondary small">{{ t('admin.customerDetail.ordersEmpty') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th>{{ t('admin.customerDetail.colOrderCode') }}</th>
            <th>{{ t('admin.customerDetail.colOrderDate') }}</th>
            <th>{{ t('admin.customerDetail.colOrderTotal') }}</th>
            <th>{{ t('admin.customerDetail.colOrderStatus') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="o in customerOrders" :key="o.donHangId">
              <td>{{ o.maDonHang }}</td>
              <td class="text-secondary">{{ formatDate(o.ngayDat) }}</td>
              <td>{{ formatPrice(o.thanhTien) }}</td>
              <td><span class="badge" :style="{background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text}">{{ orderStatusLabel(o.trangThaiDonHang) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mb-4">
      <div class="fw-bold mb-2">{{ t('admin.customerDetail.vouchersTitle') }}</div>
      <div v-if="vouchersLoading" class="text-secondary small">{{ t('admin.customers.loading') }}</div>
      <div v-else-if="vouchers.length === 0" class="text-secondary small">{{ t('admin.customerDetail.vouchersEmpty') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th>{{ t('admin.customerDetail.colVoucherCode') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherType') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherValue') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherSource') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherStatus') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherExpiry') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="v in vouchers" :key="v.phieuId">
              <td>{{ v.maPhieu }}</td>
              <td>{{ v.loai === 'percent' ? t('admin.customerDetail.typePercent') : t('admin.customerDetail.typeFixed') }}</td>
              <td>{{ v.loai === 'percent' ? `${v.giaTri}%` : formatPrice(v.giaTri) }}</td>
              <td class="text-secondary">{{ v.nguon === 'Admin tặng' ? t('admin.customerDetail.voucherSourceGifted') : t('admin.customerDetail.voucherSourceRedeemed') }}</td>
              <td><span class="badge" :class="voucherStatus(v).cls">{{ t(`admin.customerDetail.${voucherStatus(v).key}`) }}</span></td>
              <td class="text-secondary">{{ formatDate(v.ngayHetHan) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mb-4">
      <div class="fw-bold mb-2">{{ t('admin.customerDetail.pointHistoryTitle') }}</div>
      <div v-if="pointHistoryLoading" class="text-secondary small">{{ t('admin.customers.loading') }}</div>
      <div v-else-if="pointHistory.length === 0" class="text-secondary small">{{ t('admin.customerDetail.pointHistoryEmpty') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th>{{ t('admin.customerDetail.colPointAmount') }}</th>
            <th>{{ t('admin.customerDetail.colPointReason') }}</th>
            <th>{{ t('admin.customerDetail.colPointBy') }}</th>
            <th>{{ t('admin.customerDetail.colPointDate') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in pointHistory" :key="p.id">
              <td class="fw-bold text-warning">+{{ p.soDiem }}</td>
              <td class="text-secondary">{{ p.lyDo || '—' }}</td>
              <td class="text-secondary">{{ p.tenNhanVien }}</td>
              <td class="text-secondary">{{ formatDateTime(p.ngayTao) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <CustomerFormModal ref="customerFormModalRef" v-model="showCustomerModal" />
    <TangDiemModal v-model="showGiftPointsModal" :customer-id="customerId" @gifted="() => { loadPointHistory(); refreshCustomers(); }" />
    <TangVoucherModal v-model="showGiftVoucherModal" :customer-id="customerId" @gifted="loadVouchers" />
  </div>
</template>
