<template>
  <div v-if="maVanDon || history.length" class="rounded-3 p-3" style="background:var(--bg-card-alt);">
    <div v-if="maVanDon" class="d-flex align-items-center gap-2 mb-2 pb-2" style="border-bottom:1px solid var(--border-color-soft);">
      <span style="font-size:0.85rem; color:var(--text-secondary);">📦 {{ t('account.trackingCode') }}:</span>
      <span class="fw-bold" style="color:var(--text-primary); font-size:0.85rem;">{{ maVanDon }}</span>
      <button class="btn btn-sm px-2 py-0" style="font-size:11px; border:1px solid var(--border-color-strong); background:var(--bg-input); color:var(--text-secondary);" @click="copyCode">
        {{ copied ? t('account.trackingCodeCopied') : t('account.trackingCodeCopy') }}
      </button>
    </div>
    <div v-if="history.length" class="d-flex flex-column gap-2">
      <div v-for="entry in history" :key="entry.lichSuId" class="d-flex gap-2">
        <span style="font-size:0.78rem; color:var(--text-secondary); min-width:130px;">{{ formatDate(entry.thoiGian) }}</span>
        <span style="font-size:0.82rem; color:var(--text-primary);">{{ orderStatusIcon(entry.trangThaiMoi) }} {{ orderStatusLabel(entry.trangThaiMoi) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { t, I18nStore } from '../../i18n/index.js';
import { orderStatusLabel, orderStatusIcon } from '../../utils/orderStatus.js';

const props = defineProps({
  maVanDon: { type: String, default: '' },
  history: { type: Array, default: () => [] },
});

const copied = ref(false);
const copyCode = async () => {
  if (!props.maVanDon) return;
  await navigator.clipboard.writeText(props.maVanDon);
  copied.value = true;
  setTimeout(() => { copied.value = false; }, 1500);
};

const formatDate = (d) => {
  if (!d) return '—';
  try { return new Date(d).toLocaleString(I18nStore.locale); } catch { return d; }
};
</script>
