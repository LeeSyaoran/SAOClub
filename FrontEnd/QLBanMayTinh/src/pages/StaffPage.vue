<script setup>
defineEmits(['addToCart', 'buyAgainUnavailable', 'goHome']);
import { computed, onMounted, onUnmounted, ref } from "vue";
import { AuthStore } from "../stores/index.js";
import { t } from "../i18n/index.js";
import { ThemeStore, toggleTheme } from "../stores/theme.js";
import ConfirmDialog from "../components/common/ConfirmDialog.vue";
import ToastHost from "../components/common/ToastHost.vue";
import UserProfileMenu from "../components/admin/UserProfileMenu.vue";
import PosPanel from "../components/admin/PosPanel.vue";
import OrdersTable from "../components/admin/OrdersTable.vue";
import CustomersTable from "../components/admin/CustomersTable.vue";
import ProductsTable from "../components/admin/ProductsTable.vue";
import BienTheTable from "../components/admin/BienTheTable.vue";
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
import { OrdersStore, connectOrderEvents, disconnectOrderEvents } from "../stores/orders.js";
import { CustomersStore, ensureCustomers } from "../stores/customers.js";
import { refreshReturns } from "../stores/returns.js";

// ── Navigation — mac dinh vao thang Ban hang (viec chinh hang ngay cua nhan vien) ──
const currentPage = ref("ban-hang");
// Sidebar bat/tat duoc o moi kich thuoc man hinh, mac dinh mo tren desktop, dong tren
// mobile (dong bo AdminPage.vue). ponytail: khong dong bo lai khi resize giua chung.
const sidebarOpen = ref(window.matchMedia("(min-width: 768px)").matches);
const productsMainTab = ref("sanPham");
const navigate = (page) => {
  currentPage.value = page;
  if (window.matchMedia("(max-width: 767.98px)").matches) sidebarOpen.value = false; // chon xong tu dong dong lai tren mobile
  // ReturnsPanel.vue chỉ tải dữ liệu 1 lần lúc mount — làm mới lại mỗi lần vào tab để
  // thấy yêu cầu trả hàng khách vừa gửi (xem AdminPage.vue navigate() cùng lý do).
  if (page === "tra-hang") refreshReturns();
};

const PAGE_META = {
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
};
const topbarTitle = computed(() => t(PAGE_META[currentPage.value]?.titleKey ?? "admin.pageMeta.banHang.title"));
const topbarSub = computed(() => t(PAGE_META[currentPage.value]?.subKey ?? ""));
const topbarIcon = computed(() => PAGE_META[currentPage.value]?.icon ?? "🛒");

// ── Badge sidebar: don hang hom nay, tong khach hang — doc thang tu store, khong
// can qua AdminPage.vue vi trang nay doc lap hoan toan ──
const toDateInputValue = (d) => {
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};
const todayOrdersCount = computed(
  () => OrdersStore.items.filter((o) => o.ngayDat?.slice(0, 10) === toDateInputValue(new Date())).length,
);
const totalCustomers = computed(() => CustomersStore.items.length);

onMounted(() => {
  ensureCustomers();
  connectOrderEvents(AuthStore.user?.token);
});
onUnmounted(() => {
  disconnectOrderEvents();
});
</script>

<template>
  <!-- Layout chinh: sidebar ben trai + main content ben phai — dong bo AdminPage.vue -->
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

      <!-- Nav staff -->
      <nav class="flex-grow-1 d-flex flex-column px-2 pb-2">
        <div class="adm-nav" :class="{active: currentPage==='ban-hang'}" @click="navigate('ban-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C4.328 11.142 4 11.574 4 12a2 2 0 002 2h10a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 4H6.28l-.31-1.243A1 1 0 005 2H3z"/><path d="M16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/></svg>
          {{ t('admin.sidebar.banHang') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='orders'}" @click="navigate('orders')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"/><path fill-rule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.orders') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ todayOrdersCount }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='customers'}" @click="navigate('customers')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/></svg>
          {{ t('admin.sidebar.customers') }}
          <span class="badge bg-warning text-dark ms-auto" style="font-size:0.68rem;">{{ totalCustomers }}</span>
        </div>
        <div class="adm-nav" :class="{active: currentPage==='tra-hang'}" @click="navigate('tra-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9.707 3.293a1 1 0 010 1.414L7.414 7H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.traHang') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='products'}" @click="navigate('products')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zm0 8a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zm6-6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zm0 8a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
          {{ t('admin.sidebar.products') }}
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
        <section v-show="currentPage === 'ban-hang'"><PosPanel /></section>
        <section v-show="currentPage === 'orders'"><OrdersTable :can-delete="false" /></section>
        <section v-show="currentPage === 'customers'"><CustomersTable /></section>
        <section v-show="currentPage === 'tra-hang'"><ReturnsPanel :can-pick-staff="true" /></section>
        <section v-show="currentPage === 'products'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='sanPham'}" @click="productsMainTab='sanPham'">{{ t('admin.productsTabs.sanPham') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='bienThe'}" @click="productsMainTab='bienThe'">{{ t('admin.productsTabs.bienThe') }}</button></li>
          </ul>
          <div v-show="productsMainTab==='sanPham'"><ProductsTable :readonly="true" /></div>
          <div v-show="productsMainTab==='bienThe'"><BienTheTable :readonly="true" /></div>
        </section>
      </div>
    </main>
  </div>

  <ConfirmDialog />
  <ToastHost />
</template>

<style scoped>
/* Nav item: dong bo AdminPage.vue (.adm-nav/.adm-icon/.adm-nav-label) — CSS scoped
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
