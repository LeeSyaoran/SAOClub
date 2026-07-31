<script setup>
import { ref, computed } from "vue";
import { t } from "../../i18n/index.js";
import * as PhieuGiamGiaCaNhanService from "../../services/PhieuGiamGiaCaNhanService.js";
import { showToast } from "../../stores/toast.js";
import { nowLocalIso } from "../../utils/datetime.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["update:modelValue", "gifted"]);

const emptyForm = () => ({
  loai: "percent",
  giaTri: null,
  giaTriToiDa: null,
  ngayHetHan: "",
  donHangToiThieu: null,
});
const form = ref(emptyForm());
const error = ref("");
const saving = ref(false);

const valueLabel = computed(() =>
  form.value.loai === "percent"
    ? t("admin.giftVoucherModal.valueLabelPercent")
    : t("admin.giftVoucherModal.valueLabelFixed"),
);

const close = () => {
  emit("update:modelValue", false);
  form.value = emptyForm();
  error.value = "";
};

const submit = async () => {
  error.value = "";
  if (!form.value.giaTri || form.value.giaTri <= 0) {
    error.value = t("admin.giftVoucherModal.valueRequired");
    return;
  }
  if (form.value.loai === "percent" && form.value.giaTri > 100) {
    error.value = t("admin.giftVoucherModal.percentMax100");
    return;
  }
  // So sánh chuỗi ngày địa phương (YYYY-MM-DD) — không qua Date/toISOString() để tránh lệch
  // múi giờ (new Date("2026-08-15") parse thành UTC midnight). Cho phép chọn "hôm nay" vì
  // voucher sẽ hết hạn vào cuối ngày đó (xem submit() bên dưới), không phải đầu ngày.
  if (!form.value.ngayHetHan || form.value.ngayHetHan < nowLocalIso().slice(0, 10)) {
    error.value = t("admin.giftVoucherModal.expiryRequired");
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const body = {
      loai: form.value.loai,
      giaTri: Number(form.value.giaTri),
      giaTriToiDa: form.value.giaTriToiDa ? Number(form.value.giaTriToiDa) : null,
      // Chuỗi ngày-giờ địa phương "trần" (không timezone) — hết hạn vào cuối ngày đã chọn,
      // khớp quy ước của cả dự án (xem toLocalDT() trong utils/adminFormat.js).
      ngayHetHan: `${form.value.ngayHetHan}T23:59:59`,
      donHangToiThieu: form.value.donHangToiThieu ? Number(form.value.donHangToiThieu) : null,
    };
    const res = await PhieuGiamGiaCaNhanService.taoVoucherAdmin(props.customerId, body);
    if (!res.ok) {
      error.value = await res.text();
      return;
    }
    showToast(t("admin.giftVoucherModal.success"), "success");
    emit("gifted");
    close();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div v-if="modelValue" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="close">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.giftVoucherModal.title') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="close"></button>
      </div>
      <div class="p-4">
        <div v-if="error" class="alert alert-danger small py-2 mb-3">{{ error }}</div>
        <div class="row g-3">
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.typeLabel') }}</label>
            <select v-model="form.loai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
              <option value="percent">{{ t('admin.customerDetail.typePercent') }}</option>
              <option value="fixed">{{ t('admin.customerDetail.typeFixed') }}</option>
            </select>
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ valueLabel }}</label>
            <input v-model.number="form.giaTri" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.maxDiscountLabel') }}</label>
            <input v-model.number="form.giaTriToiDa" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.minOrderLabel') }}</label>
            <input v-model.number="form.donHangToiThieu" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-12">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.expiryLabel') }}</label>
            <input v-model="form.ngayHetHan" type="date" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="close">{{ t('admin.giftVoucherModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="submit">{{ t('admin.giftVoucherModal.submit') }}</button>
      </div>
    </div>
  </div>
</template>
