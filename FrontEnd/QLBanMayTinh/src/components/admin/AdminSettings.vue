<script setup>
import { t } from "../../i18n/index.js";
import { I18nStore, LOCALES, setLocale } from "../../i18n/index.js";
import { ThemeStore, toggleTheme } from "../../stores/theme.js";
import { SettingsStore } from "../../stores/settings.js";

const props = defineProps({
  cdMatKhauCu: String,
  cdMatKhauMoi: String,
  cdMatKhauXacNhan: String,
  cdMatKhauError: String,
  cdMatKhauSuccess: String,
  cdMatKhauLoading: Boolean,
  cdForm: { type: Object, default: () => ({}) },
  cdLogoPreview: String,
  cdStoreError: String,
  cdStoreSaved: Boolean,
  cdStoreSaving: Boolean,
  cdNguongTonKho: Number,
  cdApplyingThreshold: Boolean,
});

const emit = defineEmits([
  "update:cdMatKhauCu",
  "update:cdMatKhauMoi",
  "update:cdMatKhauXacNhan",
  "update:cdNguongTonKho",
  "changePassword",
  "handleLogoFile",
  "saveStore",
  "applyThreshold",
  "saveAppearance",
]);
</script>

<template>
  <section>
    <div class="row g-3">
      <div class="col-12 col-xl-6">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body">
            <div class="fw-bold mb-3">🔑 {{ t('admin.settings.changePasswordTitle') }}</div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
              <input type="password" :value="cdMatKhauCu" @input="$emit('update:cdMatKhauCu', $event.target.value)" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
              <input type="password" :value="cdMatKhauMoi" @input="$emit('update:cdMatKhauMoi', $event.target.value)" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
              <input type="password" :value="cdMatKhauXacNhan" @input="$emit('update:cdMatKhauXacNhan', $event.target.value)" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div v-if="cdMatKhauError" class="text-danger small mb-2">{{ cdMatKhauError }}</div>
            <div v-if="cdMatKhauSuccess" class="text-success small mb-2">{{ cdMatKhauSuccess }}</div>
            <button class="btn btn-warning btn-sm" :disabled="cdMatKhauLoading || !cdMatKhauCu || !cdMatKhauMoi" @click="$emit('changePassword')">
              {{ t('admin.settings.changePasswordButton') }}
            </button>
          </div>
        </div>
      </div>

      <div class="col-12 col-xl-6">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body">
            <div class="fw-bold mb-3">🏪 {{ t('admin.settings.storeInfoTitle') }}</div>
            <div class="d-flex align-items-center gap-3 mb-3">
              <label class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary" style="width:88px;height:70px;cursor:pointer;flex-shrink:0;overflow:hidden;background:var(--bg-card-inset);">
                <img v-if="cdLogoPreview" :src="cdLogoPreview" style="width:88px;height:70px;object-fit:contain;" />
                <span v-else style="font-size:1.3rem;">🖼️</span>
                <input type="file" accept="image/*" class="d-none" @change="$emit('handleLogoFile', $event)" />
              </label>
              <span class="text-secondary small">{{ t('admin.settings.storeLogo') }}</span>
            </div>
            <div class="row g-2 mb-3">
              <div class="col-12">
                <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeName') }}</label>
                <input v-model="cdForm.tenCuaHang" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              </div>
              <div class="col-12">
                <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeAddress') }}</label>
                <input v-model="cdForm.diaChi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              </div>
              <div class="col-6">
                <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storePhone') }}</label>
                <input v-model="cdForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              </div>
              <div class="col-6">
                <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeEmail') }}</label>
                <input v-model="cdForm.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              </div>
              <div class="col-12">
                <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeTaxCode') }}</label>
                <input v-model="cdForm.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              </div>
            </div>
            <div v-if="cdStoreError" class="text-danger small mb-2">{{ cdStoreError }}</div>
            <div v-if="cdStoreSaved" class="text-success small mb-2">{{ t('admin.settings.saved') }}</div>
            <button class="btn btn-warning btn-sm" :disabled="cdStoreSaving" @click="$emit('saveStore')">
              {{ t('admin.settings.saveButton') }}
            </button>
          </div>
        </div>
      </div>

      <div class="col-12 col-xl-6">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body">
            <div class="fw-bold mb-3">📦 {{ t('admin.settings.lowStockThresholdTitle') }}</div>
            <div class="mb-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.lowStockThresholdLabel') }}</label>
              <input type="number" min="0" :value="cdNguongTonKho" @input="$emit('update:cdNguongTonKho', Number($event.target.value))" class="form-control form-control-sm" style="width:120px;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <button class="btn btn-outline-warning btn-sm" :disabled="cdApplyingThreshold" @click="$emit('applyThreshold')">
              {{ t('admin.settings.applyToAllButton') }}
            </button>
          </div>
        </div>
      </div>

      <div class="col-12 col-xl-6">
        <div class="card border-secondary h-100" style="background:var(--bg-hover);">
          <div class="card-body">
            <div class="fw-bold mb-3">🎨 {{ t('admin.settings.appearanceTitle') }}</div>
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
              <span class="text-secondary">{{ t('admin.settings.themeLabel') }}</span>
              <button type="button" class="btn btn-sm btn-outline-secondary" @click="toggleTheme">
                {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
              </button>
            </div>
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
              <span class="text-secondary">{{ t('admin.settings.languageLabel') }}</span>
              <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                      :value="I18nStore.locale" @change="setLocale($event.target.value)">
                <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code">{{ loc.flag }} {{ loc.label }}</option>
              </select>
            </div>
            <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
              <span class="text-secondary">{{ t('admin.settings.defaultLanguageLabel') }}</span>
              <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                      v-model="SettingsStore.ngonNguMacDinh"
                      @change="$emit('saveAppearance')">
                <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code">{{ loc.flag }} {{ loc.label }}</option>
              </select>
            </div>
            <div class="d-flex justify-content-between align-items-center py-2 small">
              <span class="text-secondary">{{ t('admin.settings.numberFormatLabel') }}</span>
              <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                      v-model="SettingsStore.dinhDangSo"
                      @change="$emit('saveAppearance')">
                <option value="vi">{{ t('admin.settings.numberFormatVi') }}</option>
                <option value="en">{{ t('admin.settings.numberFormatEn') }}</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
