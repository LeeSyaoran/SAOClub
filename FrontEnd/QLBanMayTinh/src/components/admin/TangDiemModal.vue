<script setup>
import { ref } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../services/KhachHangService.js";
import { showToast } from "../../stores/toast.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["update:modelValue", "gifted"]);

const soDiem = ref(null);
const lyDo = ref("");
const error = ref("");
const saving = ref(false);

const close = () => {
  emit("update:modelValue", false);
  soDiem.value = null;
  lyDo.value = "";
  error.value = "";
};

const submit = async () => {
  error.value = "";
  if (!soDiem.value || soDiem.value <= 0) {
    error.value = t("admin.giftPointsModal.amountRequired");
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const res = await KhachHangService.tangDiem(props.customerId, {
      soDiem: Number(soDiem.value),
      lyDo: lyDo.value || null,
    });
    if (!res.ok) {
      error.value = await res.text();
      return;
    }
    showToast(t("admin.giftPointsModal.success", { points: soDiem.value }), "success");
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
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:420px;max-width:95vw;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.giftPointsModal.title') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="close"></button>
      </div>
      <div class="p-4">
        <div v-if="error" class="alert alert-danger small py-2 mb-3">{{ error }}</div>
        <div class="mb-3">
          <label class="form-label small text-secondary">{{ t('admin.giftPointsModal.amountLabel') }}</label>
          <input v-model.number="soDiem" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
        </div>
        <div>
          <label class="form-label small text-secondary">{{ t('admin.giftPointsModal.reasonLabel') }}</label>
          <input v-model="lyDo" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="close">{{ t('admin.giftPointsModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="submit">{{ t('admin.giftPointsModal.submit') }}</button>
      </div>
    </div>
  </div>
</template>
