<script setup>
import { ref } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import { CustomersStore, refreshCustomers } from "../../stores/customers.js";

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

const isEditing = () => !!editingCustomerId.value;

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
  if (!customerForm.value.hoTen.trim()) { customerFormError.value = t('admin.customerModal.nameRequired'); return; }
  if (!customerForm.value.soDienThoai.trim()) { customerFormError.value = t('admin.customerModal.phoneRequired'); return; }
  if (!customerForm.value.diaChi.trim()) { customerFormError.value = t('admin.customerModal.addressRequired'); return; }
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
    if (editingCustomerId.value) {
      const items = CustomersStore.items ?? [];
      const idx = items.findIndex((c) => c.khachHangId === editingCustomerId.value);
      saved = { ...(idx !== -1 ? items[idx] : {}), ...body };
      if (idx !== -1) CustomersStore.items[idx] = saved;
    } else {
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
  <div v-if="modelValue" class="cfm-backdrop" @click.self="close">
    <div class="cfm-shell">
      <!-- ── Header ── -->
      <div class="cfm-header">
        <div class="cfm-header-icon">
          <i class="fa" :class="isEditing() ? 'fa-user-edit' : 'fa-user-plus'"></i>
        </div>
        <div class="cfm-header-text">
          <h3 class="cfm-title">{{ isEditing() ? t('admin.customerModal.titleEdit') : t('admin.customerModal.titleAdd') }}</h3>
          <p class="cfm-subtitle">{{ isEditing() ? 'Cập nhật thông tin khách hàng' : 'Thêm khách hàng mới vào hệ thống' }}</p>
        </div>
        <button class="cfm-close" :aria-label="t('common.close')" @click="close">
          <i class="fa fa-times"></i>
        </button>
      </div>

      <!-- ── Body ── -->
      <div class="cfm-body">
        <div v-if="customerFormError" class="cfm-error">
          <i class="fa fa-exclamation-circle"></i>
          {{ customerFormError }}
        </div>

        <!-- Section: Thông tin cá nhân -->
        <div class="cfm-section">
          <div class="cfm-section-title">
            <i class="fa fa-id-card"></i>
            Thông tin cá nhân
          </div>
          <div class="cfm-fields">
            <div class="cfm-field">
              <label class="cfm-label">Họ tên <span class="cfm-required">*</span></label>
              <div class="cfm-input-wrap">
                <i class="fa fa-user cfm-input-icon"></i>
                <input v-model="customerForm.hoTen" class="cfm-input" placeholder="Nhập họ tên khách hàng" />
              </div>
            </div>
            <div class="cfm-field">
              <label class="cfm-label">Số điện thoại <span class="cfm-required">*</span></label>
              <div class="cfm-input-wrap">
                <i class="fa fa-phone cfm-input-icon"></i>
                <input v-model="customerForm.soDienThoai" class="cfm-input" placeholder="0xxx xxx xxx" />
              </div>
            </div>
            <div class="cfm-field">
              <label class="cfm-label">Email</label>
              <div class="cfm-input-wrap">
                <i class="fa fa-envelope cfm-input-icon"></i>
                <input v-model="customerForm.email" type="email" class="cfm-input" placeholder="email@example.com" />
              </div>
            </div>
            <div class="cfm-field cfm-field--full">
              <label class="cfm-label">Địa chỉ <span class="cfm-required">*</span></label>
              <div class="cfm-input-wrap">
                <i class="fa fa-map-marker-alt cfm-input-icon"></i>
                <input v-model="customerForm.diaChi" class="cfm-input" placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố" />
              </div>
            </div>
          </div>
        </div>

        <!-- Section: Thông tin doanh nghiệp -->
        <div class="cfm-section">
          <div class="cfm-section-title">
            <i class="fa fa-building"></i>
            Thông tin doanh nghiệp (nếu có)
          </div>
          <div class="cfm-fields">
            <div class="cfm-field">
              <label class="cfm-label">Loại khách</label>
              <div class="cfm-input-wrap">
                <i class="fa fa-tag cfm-input-icon"></i>
                <select v-model="customerForm.loaiKhach" class="cfm-input cfm-select">
                  <option value="ca_nhan">{{ t('admin.customerModal.typePersonal') }}</option>
                  <option value="doanh_nghiep">{{ t('admin.customerModal.typeBusiness') }}</option>
                </select>
              </div>
            </div>
            <div class="cfm-field">
              <label class="cfm-label">Tên công ty</label>
              <div class="cfm-input-wrap">
                <i class="fa fa-briefcase cfm-input-icon"></i>
                <input v-model="customerForm.tenCongTy" class="cfm-input" placeholder="Tên công ty" />
              </div>
            </div>
            <div class="cfm-field">
              <label class="cfm-label">Mã số thuế</label>
              <div class="cfm-input-wrap">
                <i class="fa fa-file-alt cfm-input-icon"></i>
                <input v-model="customerForm.maSoThue" class="cfm-input" placeholder="Mã số thuế công ty" />
              </div>
            </div>
          </div>
        </div>

        <!-- Section: Cài đặt tài khoản -->
        <div class="cfm-section">
          <div class="cfm-section-title">
            <i class="fa fa-cog"></i>
            Cài đặt tài khoản
          </div>
          <div class="cfm-fields">
            <div class="cfm-field">
              <label class="cfm-label">Điểm tích lũy</label>
              <div class="cfm-input-wrap">
                <i class="fa fa-star cfm-input-icon cfm-input-icon--gold"></i>
                <input v-model="customerForm.diemTichLuy" type="number" min="0" class="cfm-input" placeholder="0" />
              </div>
            </div>
            <div class="cfm-field">
              <label class="cfm-label">Trạng thái</label>
              <div class="cfm-input-wrap">
                <i class="fa fa-toggle-on cfm-input-icon"></i>
                <select v-model="customerForm.trangThai" class="cfm-input cfm-select">
                  <option value="active">{{ t('admin.customerModal.statusActive') }}</option>
                  <option value="inactive">{{ t('admin.customerModal.statusLocked') }}</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Footer ── -->
      <div class="cfm-footer">
        <button class="cfm-btn cfm-btn--ghost" @click="close">
          <i class="fa fa-arrow-left"></i> Hủy bỏ
        </button>
        <button class="cfm-btn cfm-btn--primary" :disabled="saving" @click="saveCustomer">
          <i class="fa" :class="saving ? 'fa-spinner fa-spin' : (isEditing() ? 'fa-save' : 'fa-plus')"></i>
          {{ saving ? 'Đang lưu...' : (isEditing() ? t('admin.customerModal.update') : t('admin.customerModal.addNew')) }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Backdrop ── */
.cfm-backdrop {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: var(--bg-overlay, rgba(0,0,0,0.5));
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

/* ── Shell ── */
.cfm-shell {
  background: #fff;
  border-radius: 20px;
  width: 580px;
  max-width: 100%;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(168, 27, 93, 0.3);
  border: 1px solid var(--pink-200, #ffcfe1);
}

/* ── Header ── */
.cfm-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--pink-400, #ec4899) 0%, var(--pink-600, #db2777) 100%);
  color: #fff;
}
.cfm-header-icon {
  width: 48px; height: 48px;
  background: rgba(255,255,255,0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border: 2px solid rgba(255,255,255,0.3);
}
.cfm-header-text { flex: 1; }
.cfm-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 2px;
  color: #fff;
}
.cfm-subtitle {
  font-size: 12px;
  margin: 0;
  opacity: 0.85;
}
.cfm-close {
  background: rgba(255,255,255,0.15);
  border: none;
  color: #fff;
  width: 36px; height: 36px;
  border-radius: 10px;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}
.cfm-close:hover { background: rgba(255,255,255,0.3); transform: rotate(90deg); }

/* ── Body ── */
.cfm-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* ── Error ── */
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
.cfm-error i { font-size: 16px; }

/* ── Section ── */
.cfm-section {
  margin-bottom: 20px;
}
.cfm-section:last-child { margin-bottom: 0; }

.cfm-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--pink-700, #a81b5d);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--pink-100, #ffe6f0);
}
.cfm-section-title i { font-size: 14px; }

/* ── Fields ── */
.cfm-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.cfm-field { display: flex; flex-direction: column; gap: 6px; }
.cfm-field--full { grid-column: 1 / -1; }

.cfm-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted, #6b7280);
  display: flex;
  align-items: center;
  gap: 4px;
}
.cfm-required { color: #ef4444; }

.cfm-input-wrap {
  position: relative;
}
.cfm-input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  color: var(--pink-500, #db2777);
  opacity: 0.7;
  pointer-events: none;
  z-index: 1;
}
.cfm-input-icon--gold { color: #f59e0b; }

.cfm-input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 2px solid var(--pink-100, #ffe6f0);
  border-radius: 10px;
  font-size: 13px;
  background: var(--pink-50, #fff5f9);
  color: var(--ink, #1f2937);
  transition: all 0.2s ease;
  font-family: inherit;
}
.cfm-input:focus {
  outline: none;
  border-color: var(--pink-400, #ec4899);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.15);
}
.cfm-input::placeholder { color: #d1d5db; }
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
  padding: 16px 24px;
  border-top: 1px solid var(--pink-100, #ffe6f0);
  background: #fff;
}

.cfm-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.cfm-btn--primary {
  background: linear-gradient(180deg, var(--pink-500, #db2777) 0%, var(--pink-700, #a81b5d) 100%);
  color: #fff;
  box-shadow: 0 3px 0 var(--pink-700, #a81b5d), 0 4px 12px rgba(168, 27, 93, 0.35);
  border-bottom: 3px solid var(--pink-700, #a81b5d);
}
.cfm-btn--primary:hover:not(:disabled) {
  background: linear-gradient(180deg, var(--pink-400, #ec4899) 0%, var(--pink-600, #db2777) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 0 var(--pink-700, #a81b5d), 0 6px 16px rgba(168, 27, 93, 0.4);
}
.cfm-btn--primary:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  border-bottom-width: 0;
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
.cfm-btn i { font-size: 12px; }

/* ── Responsive ── */
@media (max-width: 560px) {
  .cfm-shell { width: 100%; border-radius: 16px; }
  .cfm-fields { grid-template-columns: 1fr; }
  .cfm-field--full { grid-column: 1; }
  .cfm-header { padding: 16px; }
  .cfm-body { padding: 16px; }
  .cfm-footer { padding: 12px 16px; }
}
</style>
