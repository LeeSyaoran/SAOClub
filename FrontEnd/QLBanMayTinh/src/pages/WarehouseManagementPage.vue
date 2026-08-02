<script setup>
defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome']);
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
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
import SerialManager from "../components/admin/SerialManager.vue";
import DmCategoryTable from "../components/admin/DmCategoryTable.vue";
import * as DmService from "../services/DmService.js";
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../services/ChiTietLinhKienService.js";
import { refreshReturns } from "../stores/returns.js";

// ── Navigation — mac dinh vao thang Kho hang (viec chinh hang ngay cua quan ly kho) ──
const currentPage = ref("inventory");
// Sidebar bat/tat duoc o moi kich thuoc man hinh, mac dinh mo tren desktop, dong tren
// mobile (dong bo AdminPage.vue). ponytail: khong dong bo lai khi resize giua chung.
const sidebarOpen = ref(window.matchMedia("(min-width: 768px)").matches);
const navigate = (page) => {
  currentPage.value = page;
  if (window.matchMedia("(max-width: 767.98px)").matches) sidebarOpen.value = false; // chon xong tu dong dong lai tren mobile
  // ReturnsPanel.vue chỉ tải dữ liệu 1 lần lúc mount — làm mới lại mỗi lần vào tab để
  // thấy yêu cầu trả hàng khách vừa gửi (xem AdminPage.vue navigate() cùng lý do).
  if (page === "traHang") refreshReturns();
};

const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  warrantyClaims: { titleKey: "admin.pageMeta.warrantyClaims.title", subKey: "admin.pageMeta.warrantyClaims.sub", icon: "🛡️" },
  serial: { titleKey: "admin.pageMeta.serial.title", subKey: "admin.pageMeta.serial.sub", icon: "🔢" },
  cpu: { titleKey: "admin.pageMeta.cpu.title", subKey: "admin.pageMeta.cpu.sub", icon: "🧠" },
  ram: { titleKey: "admin.pageMeta.ram.title", subKey: "admin.pageMeta.ram.sub", icon: "💾" },
  gpu: { titleKey: "admin.pageMeta.gpu.title", subKey: "admin.pageMeta.gpu.sub", icon: "🎮" },
  oCung: { titleKey: "admin.pageMeta.oCung.title", subKey: "admin.pageMeta.oCung.sub", icon: "💽" },
};
const topbarTitle = computed(() => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.inventory.title"));
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "📦");
</script>

<template>
  <!-- Layout chinh: sidebar ben trai + main content ben phai — dong bo AdminPage.vue/StaffPage.vue -->
  <div class="d-flex overflow-hidden" style="height:100vh; background:var(--bg-page-alt); color:var(--text-primary); font-family:'Nunito Sans',sans-serif;">

    <!-- Lớp phủ mờ phía sau sidebar khi mở trên mobile — bấm ra ngoài để đóng -->
    <div v-if="sidebarOpen" class="d-md-none position-fixed top-0 start-0 w-100 h-100"
         style="background:rgba(0,0,0,0.5); z-index:1039;"
         @click="sidebarOpen = false"></div>

    <!-- ══════════ SIDEBAR ══════════ -->
    <aside class="d-flex flex-column border-end flex-shrink-0 adm-sidebar"
           :class="{ 'adm-sidebar-open': sidebarOpen }"
           style="background:var(--bg-card-inset); border-color:var(--border-color)!important; overflow-y:auto;">

      <!-- Logo -->
      <div class="d-flex align-items-center gap-2 p-3 border-bottom adm-brand-row"
           style="border-color:var(--border-color-soft)!important;">
        <div class="rounded-circle d-flex align-items-center justify-content-center fw-black flex-shrink-0"
             style="width:38px;height:38px;background:var(--accent);color:var(--accent-text);font-size:0.8rem;">SAO</div>
        <div class="adm-brand-text">
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
        <div class="adm-nav" :class="{active: currentPage==='warrantyClaims'}" @click="navigate('warrantyClaims')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.warrantyClaims') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='serial'}" @click="navigate('serial')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 6a1 1 0 011-1h12a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zm3 2a1 1 0 100 2h.01a1 1 0 100-2H6zm3 0a1 1 0 100 2h.01a1 1 0 100-2H9z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.serial') }}
        </div>
        <div class="adm-nav-label">{{ t('admin.sidebar.groupComponents') }}</div>
        <div class="adm-nav" :class="{active: currentPage==='cpu'}" @click="navigate('cpu')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v12a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm3 3v6h8V7H6z" clip-rule="evenodd"/></svg>
          {{ t('admin.productsTabs.cpu') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='ram'}" @click="navigate('ram')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M2 6a2 2 0 012-2h12a2 2 0 012 2v3a2 2 0 01-2 2H4a2 2 0 01-2-2V6zm3 1a1 1 0 000 2h1a1 1 0 100-2H5zm4 0a1 1 0 100 2h1a1 1 0 100-2H9zm4 0a1 1 0 100 2h1a1 1 0 100-2h-1z" clip-rule="evenodd"/></svg>
          {{ t('admin.productsTabs.ram') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='gpu'}" @click="navigate('gpu')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V5zm5 6a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"/></svg>
          {{ t('admin.productsTabs.gpu') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='oCung'}" @click="navigate('oCung')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 3a7 7 0 100 14 7 7 0 000-14zm1 4a1 1 0 10-2 0v3a1 1 0 00.293.707l2 2a1 1 0 001.414-1.414L11 8.586V7z" clip-rule="evenodd"/></svg>
          {{ t('admin.productsTabs.oCung') }}
        </div>
      </nav>

      <UserProfileMenu :show-settings-link="false" />
    </aside><!-- /sidebar -->

    <!-- ══════════ MAIN CONTENT ══════════ -->
    <main class="flex-grow-1 d-flex flex-column overflow-hidden">

      <!-- Topbar -->
      <div class="d-flex align-items-center justify-content-between p-3 border-bottom"
           style="background:var(--bg-card-inset); border-color:var(--border-color)!important;">
        <div class="d-flex align-items-center gap-2">
          <button type="button" class="d-flex align-items-center justify-content-center rounded-2 border-0"
                  style="width:34px;height:34px;background:var(--bg-hover);color:var(--text-primary);cursor:pointer;font-size:1.1rem;"
                  :aria-label="t('admin.sidebar.toggleMenu')" :title="t('admin.sidebar.toggleMenu')"
                  @click="sidebarOpen = !sidebarOpen">{{ sidebarOpen ? '✕' : '☰' }}</button>
          <div>
            <div class="fw-bold" style="font-size:1.05rem;">{{ topbarIcon }} {{ topbarTitle }}</div>
            <div style="font-size:0.78rem;color:var(--text-muted);">{{ topbarSub }}</div>
          </div>
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
        <section v-show="currentPage === 'warrantyClaims'"><WarrantyPanel /></section>
        <section v-show="currentPage === 'serial'"><SerialManager /></section>
        <section v-show="currentPage === 'cpu'">
          <DmCategoryTable :service="DmService.DmCpuService" id-field="cpuId" name-field="tenCpu" :label="t('admin.productsTabs.cpu')" :name-label="t('admin.productsTabs.cpu')" :serial-service="ChiTietCpuService" serial-field-name="cpuId" />
        </section>
        <section v-show="currentPage === 'ram'">
          <DmCategoryTable :service="DmService.DmRamService" id-field="ramId" name-field="dungLuong" :label="t('admin.productsTabs.ram')" :name-label="t('admin.productsTabs.ram')" :serial-service="ChiTietRamService" serial-field-name="ramId" />
        </section>
        <section v-show="currentPage === 'gpu'">
          <DmCategoryTable :service="DmService.DmGpuService" id-field="gpuId" name-field="tenGpu" :label="t('admin.productsTabs.gpu')" :name-label="t('admin.productsTabs.gpu')" :serial-service="ChiTietGpuService" serial-field-name="gpuId" />
        </section>
        <section v-show="currentPage === 'oCung'">
          <DmCategoryTable :service="DmService.DmOCungService" id-field="oCungId" name-field="loaiOcung" :label="t('admin.productsTabs.oCung')" :name-label="t('admin.productsTabs.oCung')" :serial-service="ChiTietOCungService" serial-field-name="oCungId" />
        </section>
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
.adm-nav-label {
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #555;
  text-transform: uppercase;
  padding: 10px 8px 3px;
}

/* Sidebar an/hien theo sidebarOpen o moi kich thuoc man hinh (dong bo AdminPage.vue).
   Desktop: thu gon con dai icon 60px (van dieu huong duoc, khong mat han). Mobile (<768px):
   luon giu width day du, truot vao/ra bang transform (overlay de len noi dung). */
.adm-sidebar {
  width: 240px;
  overflow: hidden;
  transition: width 0.2s ease;
}
.adm-sidebar:not(.adm-sidebar-open) { width: 60px; }

/* Trang thai rail (desktop, dong): chi con icon, an het chu/badge/nhom/chan sidebar.
   Dung font-size:0 de an text-node tran (khong bang <span>) — icon SVG kich thuoc px co dinh
   nen khong bi anh huong. Tren mobile trang thai nay nam ngoai man hinh nen khong ai thay. */
.adm-sidebar:not(.adm-sidebar-open) .adm-nav {
  font-size: 0;
  justify-content: center;
  padding-left: 6px;
  padding-right: 6px;
}
.adm-sidebar:not(.adm-sidebar-open) .adm-nav .badge,
.adm-sidebar:not(.adm-sidebar-open) .adm-nav-label {
  display: none;
}
.adm-sidebar:not(.adm-sidebar-open) .adm-brand-row {
  justify-content: center;
  padding-left: 6px;
  padding-right: 6px;
}
.adm-sidebar:not(.adm-sidebar-open) .adm-brand-text,
.adm-sidebar:not(.adm-sidebar-open) :deep(.adm-sidebar-footer) {
  display: none;
}

@media (max-width: 767.98px) {
  .adm-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 1040;
    width: 240px !important;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }
  .adm-sidebar.adm-sidebar-open { transform: translateX(0); }
}
</style>
