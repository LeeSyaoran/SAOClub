<script setup>
import { computed, ref } from "vue";
import { t } from "../i18n/index.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import ToastHost from "../components/common/ToastHost.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import InventoryPanel from "../components/admin/InventoryPanel.vue";
import SupplierManager from "../components/admin/SupplierManager.vue";
import InventoryHistoryPanel from "../components/admin/InventoryHistoryPanel.vue";
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";

// ── Navigation — mac dinh vao thang Kho hang (viec chinh hang ngay cua quan ly kho) ──
const currentPage = ref("inventory");
const navigate = (page) => { currentPage.value = page; };

const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
};
const topbarTitle = computed(() => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.inventory.title"));
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "📦");
</script>

<template>
  <!-- Layout chinh: sidebar ben trai + main content ben phai — dong bo AdminPage.vue/StaffPage.vue -->
  <div class="d-flex overflow-hidden" style="height:100vh; background:var(--bg-page-alt); color:var(--text-primary); font-family:'Nunito Sans',sans-serif;">

    <!-- ══════════ SIDEBAR ══════════ -->
    <aside class="d-flex flex-column border-end flex-shrink-0"
           style="width:240px; background:var(--bg-card-inset); border-color:var(--border-color)!important; overflow-y:auto;">

      <!-- Logo -->
      <div class="d-flex align-items-center gap-2 p-3 border-bottom"
           style="border-color:var(--border-color-soft)!important;">
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:var(--accent);color:var(--accent-text);font-size:0.8rem;">SAO</div>
        <div>
          <div class="fw-bold" style="font-size:0.95rem;">{{ t('admin.brand.name') }}</div>
          <div style="font-size:0.7rem;color:var(--text-muted);">{{ t('admin.brand.tagline') }}</div>
        </div>
      </div>

      <!-- Nav kho -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2">
        <div class="adm-nav" :class="{active: currentPage==='inventory'}" @click="navigate('inventory')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M4 3a2 2 0 100 4h12a2 2 0 100-4H4z"/><path fill-rule="evenodd" d="M3 8h14v7a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm5 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.inventory') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='suppliers'}" @click="navigate('suppliers')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z"/><path d="M3 4a1 1 0 00-1 1v9a2 2 0 002 2h.05a2.5 2.5 0 014.9 0h4.1a2.5 2.5 0 014.9 0H18a1 1 0 001-1v-4a1 1 0 00-.293-.707l-3-3A1 1 0 0015 7h-1V5a1 1 0 00-1-1H3z"/></svg>
          {{ t('admin.sidebar.suppliers') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='inventoryHistory'}" @click="navigate('inventoryHistory')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.inventoryHistory') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='traHang'}" @click="navigate('traHang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9.707 3.293a1 1 0 010 1.414L7.414 7H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.traHang') }}
        </div>
      </nav>

      <UserProfileMenu :show-settings-link="false" />
    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">

      <!-- Topbar -->
      <div class="d-flex align-items-center justify-content-between p-3 border-bottom"
           style="background:var(--bg-card-inset); border-color:var(--border-color)!important;">
        <div>
          <div class="fw-bold" style="font-size:1.05rem;">{{ topbarIcon }} {{ topbarTitle }}</div>
          <div style="font-size:0.78rem;color:var(--text-muted);">{{ topbarSub }}</div>
        </div>
        <button type="button" class="d-flex align-items-center justify-content-center rounded-2 border-0"
                style="width:34px;height:34px;background:var(--bg-hover);color:var(--text-primary);cursor:pointer;font-size:1rem;"
                :title="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                :aria-label="ThemeStore.mode === 'dark' ? t('theme.toggleToLight') : t('theme.toggleToDark')"
                @click="toggleTheme">
          {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
        </button>
      </div>

      <!-- Noi dung -->
      <div class="flex-grow-1 overflow-y-auto p-3">
        <section v-show="currentPage === 'inventory'"><InventoryPanel /></section>
        <section v-show="currentPage === 'suppliers'"><SupplierManager /></section>
        <section v-show="currentPage === 'inventoryHistory'"><InventoryHistoryPanel /></section>
        <section v-show="currentPage === 'traHang'"><ReturnsPanel :readonly="true" /></section>
      </div>
    </main>
  </div>

  <ConfirmDialog />
  <ToastHost />
</template>

<style scoped>
/* Nav item: dong bo AdminPage.vue/StaffPage.vue (.adm-nav/.adm-icon) — CSS scoped
   khong ke thua qua bien gioi component nen phai copy lai o day. */
.adm-nav {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 7px;
  cursor: pointer;
  font-size: 0.87rem;
  color: var(--text-primary);
  transition: background 0.12s, color 0.12s;
  user-select: none;
}
.adm-nav:hover { background: var(--bg-hover); color: var(--text-heading); }
.adm-nav.active { background: rgba(244,63,94,0.12); color: var(--accent-fg); }
.adm-nav.active .adm-icon { opacity: 1; }
.adm-icon { width: 17px; height: 17px; flex-shrink: 0; opacity: 0.75; }
</style>
