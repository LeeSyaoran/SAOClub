<script setup>
import { ref } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../Service/KhachHangService.js";
import { CustomersStore, refreshCustomers } from "../../stores/customers.js";

// ── Modal "Thêm/Sửa khách hàng" — dùng chung bởi CustomersTable.vue (sở hữu, mở
// trống qua nút "Thêm khách hàng") và PosPanel.vue (Task 6, mở với SĐT điền sẵn từ
// luồng "thêm khách hàng nhanh" khi tạo hóa đơn POS). openForCreate/openForEdit lộ
// ra ngoài qua defineExpose vì mỗi nơi gọi cần truyền dữ liệu khởi tạo khác nhau.
defineProps({
  modelValue: { type: Boolean, default: false },
});
const emit = defineEmits(["update:modelValue", "saved"]);

const editingCustomerId = ref(null);
const customerFormError = ref("");
const saving = ref(false);
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
const customerForm = ref(emptyCustomerForm());

const openForCreate = (prefill = {}) => {
  editingCustomerId.value = null;
  customerForm.value = { ...emptyCustomerForm(), ...prefill };
  customerFormError.value = "";
  emit("update:modelValue", true);
};
const openForEdit = (customer) => {
  editingCustomerId.value = customer.khachHangId;
  customerForm.value = {
    hoTen: customer.hoTen,
    soDienThoai: customer.soDienThoai,
    email: customer.email ?? "",
    diaChi: customer.diaChi ?? "",
    loaiKhach: customer.loaiKhach ?? "ca_nhan",
    tenCongTy: customer.tenCongTy ?? "",
    maSoThue: customer.maSoThue ?? "",
    diemTichLuy: customer.diemTichLuy ?? 0,
    trangThai: customer.trangThai ?? "active",
  };
  customerFormError.value = "";
  emit("update:modelValue", true);
};
defineExpose({ openForCreate, openForEdit });

const close = () => emit("update:modelValue", false);

const saveCustomer = async () => {
  customerFormError.value = "";
  if (!customerForm.value.hoTen.trim()) {
    customerFormError.value = t('admin.customerModal.nameRequired');
    return;
  }
  if (!customerForm.value.soDienThoai.trim()) {
    customerFormError.value = t('admin.customerModal.phoneRequired');
    return;
  }
  if (!customerForm.value.diaChi.trim()) {
    customerFormError.value = t('admin.customerModal.addressRequired');
    return;
  }
  if (saving.value) return;
  saving.value = true;
  const body = {
    ...customerForm.value,
    diemTichLuy: Number(customerForm.value.diemTichLuy),
  };
  try {
    const res = await KhachHangService.save(editingCustomerId.value, body);
    if (!res.ok) {
      customerFormError.value = t('admin.errors.saveFailedWithText', { status: res.status, text: await res.text() });
      return;
    }
    close();
    let saved;
    // Khách hàng là entity phẳng (không join tên qua ID) nên vá cục bộ an toàn,
    // khỏi phải tải lại cả bảng khách hàng.
    if (editingCustomerId.value) {
      const idx = CustomersStore.items.findIndex((c) => c.khachHangId === editingCustomerId.value);
      saved = { ...(idx !== -1 ? CustomersStore.items[idx] : {}), ...body };
      if (idx !== -1) CustomersStore.items[idx] = saved;
    } else {
      // PUT /update trả 200 rỗng (không body) nên chỉ POST mới parse được response —
      // xem KhachHangController.update() trả ResponseEntity<Void>.
      saved = await res.json();
      await refreshCustomers();
    }
    emit("saved", saved);
  } catch (e) {
    customerFormError.value = e.message;
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div v-if="modelValue" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="close">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:560px;max-width:95vw;max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ editingCustomerId?t('admin.customerModal.titleEdit'):t('admin.customerModal.titleAdd') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="close"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="customerFormError" class="alert alert-danger small py-2 mb-3">{{ customerFormError }}</div>
        <div class="row g-3">
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.fullNameLabel') }}</label><input v-model="customerForm.hoTen" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.phoneLabel') }}</label><input v-model="customerForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.emailLabel') }}</label><input v-model="customerForm.email" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.typeLabel') }}</label><select v-model="customerForm.loaiKhach" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="ca_nhan">{{ t('admin.customerModal.typePersonal') }}</option><option value="doanh_nghiep">{{ t('admin.customerModal.typeBusiness') }}</option></select></div>
          <div class="col-12"><label class="form-label small text-secondary">{{ t('admin.customerModal.addressLabel') }}</label><input v-model="customerForm.diaChi" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.companyNameLabel') }}</label><input v-model="customerForm.tenCongTy" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.taxCodeLabel') }}</label><input v-model="customerForm.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.pointsLabel') }}</label><input v-model="customerForm.diemTichLuy" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
          <div class="col-6"><label class="form-label small text-secondary">{{ t('admin.customerModal.statusLabel') }}</label><select v-model="customerForm.trangThai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)"><option value="active">{{ t('admin.customerModal.statusActive') }}</option><option value="inactive">{{ t('admin.customerModal.statusLocked') }}</option></select></div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="close">{{ t('admin.customerModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="saveCustomer">{{ editingCustomerId?t('admin.customerModal.update'):t('admin.customerModal.addNew') }}</button>
      </div>
    </div>
  </div>
</template>
