<template>
  <div
    class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
    style="background:var(--bg-overlay); z-index:1080;" @click.self="$emit('close')"
  >
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card); border:1px solid var(--border-color-strong); width:480px; max-width:95vw; max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('account.returnModalTitle') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="$emit('close')"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="error" class="alert alert-danger small py-2 mb-3">{{ error }}</div>
        <div class="mb-2 small text-secondary">{{ t('account.returnModalSelectItems') }}</div>
        <div class="d-flex flex-column gap-2 mb-3">
          <div
            v-for="line in lines" :key="line.chiTietDonHangId"
            class="d-flex align-items-center gap-2 p-2 rounded-3" style="background:var(--bg-card-inset);"
          >
            <input v-model="line.checked" type="checkbox" class="form-check-input mt-0" />
            <span class="flex-grow-1" style="font-size:12.5px; color:var(--text-primary);">{{ line.tenSanPham }}</span>
            <input
              v-model.number="line.soLuong" type="number" min="1" :max="line.soLuongMua"
              :disabled="!line.checked"
              class="form-control form-control-sm" style="width:64px; background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong);"
            />
            <span class="text-secondary" style="font-size:11px;">/ {{ line.soLuongMua }}</span>
          </div>
        </div>
        <label class="form-label small text-secondary">{{ t('account.returnModalReasonLabel') }}</label>
        <textarea
          v-model="lyDo" rows="3" class="form-control form-control-sm"
          :placeholder="t('account.returnModalReasonPlaceholder')"
          style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong);"
        ></textarea>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="$emit('close')">{{ t('account.returnModalCancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="submitting" @click="submit">{{ t('account.returnModalSubmit') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { t } from '../../i18n/index.js';
import * as PhieuTraHangService from '../../services/PhieuTraHangService.js';

const props = defineProps({
  order: { type: Object, required: true },
  items: { type: Array, default: () => [] },
});
const emit = defineEmits(['close', 'submitted']);

const lines = ref(props.items.map(item => ({
  chiTietDonHangId: item.id,
  tenSanPham: item.tenSanPham || item.maSku,
  soLuongMua: item.soLuong,
  soLuong: item.soLuong,
  checked: false,
})));
const lyDo = ref('');
const error = ref('');
const submitting = ref(false);

const submit = async () => {
  error.value = '';
  const chosen = lines.value.filter(l => l.checked);
  if (chosen.length === 0) { error.value = t('account.returnModalErrorNoItems'); return; }
  if (!lyDo.value.trim()) { error.value = t('account.returnModalErrorNoReason'); return; }

  submitting.value = true;
  try {
    const res = await PhieuTraHangService.taoYeuCau({
      donHangId: props.order.donHangId,
      lyDo: lyDo.value.trim(),
      dongTra: chosen.map(l => ({ chiTietDonHangId: l.chiTietDonHangId, soLuong: l.soLuong })),
    });
    if (!res.ok) { error.value = await res.text().catch(() => res.statusText); return; }
    emit('submitted');
  } finally {
    submitting.value = false;
  }
};
</script>
