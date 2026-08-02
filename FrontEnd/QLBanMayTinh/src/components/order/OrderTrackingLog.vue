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
      <div v-for="(entry, idx) in sortedHistory" :key="entry.lichSuId" class="d-flex gap-2">
        <span class="small" :style="idx === 0 ? 'color:var(--text-primary);' : 'color:var(--text-secondary); opacity:0.6;'" style="min-width:130px;">{{ formatDate(entry.thoiGian) }}</span>
        <span :class="idx === 0 ? 'fw-bold' : ''" :style="idx === 0 ? 'font-size:0.82rem; color:var(--accent-fg);' : 'font-size:0.82rem; color:var(--text-secondary); opacity:0.6;'">{{ orderStatusIcon(entry.trangThaiMoi) }} {{ orderStatusLabel(entry.trangThaiMoi) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { t, I18nStore } from '../../i18n/index.js';
import { orderStatusLabel, orderStatusIcon } from '../../utils/orderStatus.js';

const props = defineProps({
  maVanDon: { type: String, default: '' },
  history: { type: Array, default: () => [] },
});

// Mới nhất lên đầu (kiểu Shopee) — API trả về theo thứ tự cũ->mới (thời gian tăng dần).
const sortedHistory = computed(() =>
  [...props.history].sort((a, b) => new Date(b.thoiGian) - new Date(a.thoiGian))
);

const copied = ref(false);
const copyCode = async () => {
  if (!props.maVanDon || !navigator.clipboard) return;
  try {
    await navigator.clipboard.writeText(props.maVanDon);
    copied.value = true;
    setTimeout(() => { copied.value = false; }, 1500);
  } catch {}
};

const formatDate = (d) => {
  if (!d) return '—';
  try { return new Date(d).toLocaleString(I18nStore.locale); } catch { return d; }
};
</script>
