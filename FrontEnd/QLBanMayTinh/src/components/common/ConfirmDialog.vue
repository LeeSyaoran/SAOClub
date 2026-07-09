<template>
  <!-- Dialog xác nhận dùng chung theme app — thay window.confirm() gốc trình duyệt -->
  <Modal :model-value="ConfirmState.show" width="380px" @update:model-value="v => !v && resolveConfirm(false)">
    <div class="d-flex flex-column gap-3">
      <p class="mb-0" style="color:var(--text-primary); font-size:0.9rem; line-height:1.5;">{{ ConfirmState.message }}</p>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary px-3" style="border-radius:8px;" @click="resolveConfirm(false)">{{ t('common.cancel') }}</button>
        <button class="btn btn-sm btn-danger fw-bold px-3" style="border-radius:8px;" @click="resolveConfirm(true)">{{ t('common.confirm') }}</button>
      </div>
    </div>
  </Modal>
</template>

<script setup>
import Modal from './Modal.vue';
import { ConfirmState, resolveConfirm } from '../../stores/confirm.js';
import { t } from '../../i18n/index.js';
</script>
