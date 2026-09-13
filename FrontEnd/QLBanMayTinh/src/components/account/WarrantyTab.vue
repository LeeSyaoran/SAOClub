<script setup>
import { ref, computed, onMounted } from "vue";
import { AuthStore } from "../../stores/index.js";
import { WarrantyStore, ensureMyClaims } from "../../services/warrantyStore.js";
import { daysUntilWarrantyExpiry, isExpired } from "../../services/warrantyConstants.js";
import * as WarrantyService from "../../services/WarrantyService.js";
import Skeleton from "../common/Skeleton.vue";
import WarrantyClaimListPanel from "./WarrantyClaimListPanel.vue";
import ClaimFormModal from "./ClaimFormModal.vue";
import {
  Shield, ShieldCheck, ShieldAlert, ShieldX,
  ChevronDown, Plus, Clock, Calendar, Truck, Info, Package,
} from '@lucide/vue';

const emit = defineEmits(["toast", "view-product"]);
const auth = AuthStore;

const mainSubTab = ref('products');
const loadingOrders = ref(false);
const loadingClaims = ref(false);
const allProducts = ref([]);
const claims = ref([]);
const productFilter = ref('all');
const claimFormOpen = ref(false);
const claimFormProduct = ref(null);

const formatDate = (d) => {
  if (!d) return '—';
  try { return new Date(d).toLocaleDateString('vi-VN'); } catch { return d; }
};

const filterProducts = computed(() => {
  let list = allProducts.value;
  if (productFilter.value === 'active') list = list.filter(p => !isExpired(p.ngayHetBaoHanh) && daysUntilWarrantyExpiry(p.ngayHetBaoHanh) > 30);
  else if (productFilter.value === 'expiring') list = list.filter(p => { const d = daysUntilWarrantyExpiry(p.ngayHetBaoHanh); return d > 0 && d <= 30; });
  else if (productFilter.value === 'expired') list = list.filter(p => isExpired(p.ngayHetBaoHanh));
  return list;
});

const canCreateClaim = (item) => {
  if (!item) return false;
  if (isExpired(item.ngayHetBaoHanh)) return false;
  return !claims.value.some(c => c.chiTietId === item.chiTietId && ['cho_xu_ly', 'dang_xu_ly'].includes(c.trangThai));
};

const hasActiveClaim = (chiTietId) => claims.value.some(c => c.chiTietId === chiTietId && ['cho_xu_ly', 'dang_xu_ly'].includes(c.trangThai));

const loadOrdersAndProducts = async () => {
  loadingOrders.value = true;
  try {
    const khId = auth.user?.khachHangId || auth.user?.id;
    if (!khId) { allProducts.value = []; return; }

    // Gọi 1 API duy nhất — trả về sản phẩm đã giao + thông tin BH đầy đủ
    const products = await WarrantyService.getWarrantyProductsByKhachHang(khId);
    allProducts.value = products || [];
  } catch (e) {
    emit('toast', 'Không tải được danh sách sản phẩm bảo hành', 'error');
  } finally {
    loadingOrders.value = false;
  }
};

const loadClaims = async () => {
  loadingClaims.value = true;
  try {
    const khId = auth.user?.khachHangId || auth.user?.id;
    if (!khId) { claims.value = []; return; }
    const list = await ensureMyClaims(khId);
    claims.value = list || [];
  } catch (e) {
    emit('toast', 'Không tải được phiếu bảo hành', 'error');
    claims.value = [];
  } finally {
    loadingClaims.value = false;
  }
};

const openClaimForm = (product) => { claimFormProduct.value = product; claimFormOpen.value = true; };
const onClaimSubmitted = async () => { claimFormOpen.value = false; claimFormProduct.value = null; await loadClaims(); emit('toast', 'Đã gửi yêu cầu bảo hành thành công!', 'success'); };
const handleViewProduct = (item) => emit('view-product', item);
const onMainTabChange = async (tab) => { mainSubTab.value = tab; if (tab === 'products') await loadOrdersAndProducts(); else await loadClaims(); };

onMounted(async () => { await Promise.all([loadOrdersAndProducts(), loadClaims()]); });
</script>

<template>
  <div class="warranty-tab">
    <div class="warranty-main-tabs">
      <button class="warranty-main-tab" :class="{ 'is-active': mainSubTab === 'products' }" @click="onMainTabChange('products')">
        <Package :size="15" /> Sản phẩm
      </button>
      <button class="warranty-main-tab" :class="{ 'is-active': mainSubTab === 'claims' }" @click="onMainTabChange('claims')">
        <Shield :size="15" /> Phiếu bảo hành
        <span v-if="claims.filter(c => ['cho_xu_ly','dang_xu_ly'].includes(c.trangThai)).length" class="tab-badge">
          {{ claims.filter(c => ['cho_xu_ly','dang_xu_ly'].includes(c.trangThai)).length }}
        </span>
      </button>
    </div>

    <!-- TAB 1: SẢN PHẨM -->
    <div v-if="mainSubTab === 'products'" class="tab-content">
        <div class="filter-bar">
          <button v-for="f in [{id:'all',label:'Tất cả'},{id:'active',label:'Còn BH'},{id:'expiring',label:'Sắp hết'},{id:'expired',label:'Hết BH'}]" :key="f.id"
            class="filter-chip" :class="{ 'is-active': productFilter === f.id }" @click="productFilter = f.id">{{ f.label }}
            <span v-if="f.id==='active'" class="filter-count">{{ allProducts.filter(p=>!isExpired(p.ngayHetBaoHanh)&&daysUntilWarrantyExpiry(p.ngayHetBaoHanh)>30).length }}</span>
            <span v-else-if="f.id==='expiring'" class="filter-count filter-count--warning">{{ allProducts.filter(p=>{const d=daysUntilWarrantyExpiry(p.ngayHetBaoHanh);return d>0&&d<=30;}).length }}</span>
            <span v-else-if="f.id==='expired'" class="filter-count filter-count--expired">{{ allProducts.filter(p=>isExpired(p.ngayHetBaoHanh)).length }}</span>
          </button>
        </div>

        <div v-if="loadingOrders" class="loading-block"><div v-for="i in 5" :key="i"><Skeleton width="100%" height="64px" radius="10px" /></div></div>
        <div v-else-if="filterProducts.length === 0" class="empty-block">
          <Package :size="48" class="empty-icon" />
          <div class="empty-title">{{ productFilter==='all'?'Chưa có sản phẩm nào':productFilter==='active'?'Không có SP còn bảo hành':productFilter==='expiring'?'Không có SP sắp hết hạn':'Không có SP hết hạn' }}</div>
        </div>
        <div v-else class="product-table">
          <div class="product-table-header">
            <div class="col-thumb">Ảnh</div>
            <div class="col-info">Sản phẩm</div>
            <div class="col-order">Đơn hàng</div>
            <div class="col-bh">Bảo hành</div>
            <div class="col-action"></div>
          </div>
          <div v-for="item in filterProducts" :key="item.chiTietId" class="product-table-row">
            <div class="col-thumb" @click="handleViewProduct(item)">
              <div class="thumb-box"><img v-if="item.hinhAnh" :src="item.hinhAnh" /><Package v-else :size="20" /></div>
            </div>
            <div class="col-info">
              <div class="table-product-name" @click="handleViewProduct(item)">{{ item.tenSanPham || item.tenBienThe || '—' }}</div>
              <div class="table-meta"><span v-if="item.maSku">SKU: {{ item.maSku }}</span><span v-if="item.soSerial">Serial: {{ item.soSerial }}</span></div>
            </div>
            <div class="col-order">
              <div class="order-link">#{{ item.maDon || item.donHangId }}</div>
              <div class="order-date">{{ formatDate(item.ngayDat) }}</div>
            </div>
            <div class="col-bh">
              <div class="bh-status-pill" :class="{
                'bh-status-pill--active': !isExpired(item.ngayHetBaoHanh) && daysUntilWarrantyExpiry(item.ngayHetBaoHanh) > 30,
                'bh-status-pill--expiring': !isExpired(item.ngayHetBaoHanh) && daysUntilWarrantyExpiry(item.ngayHetBaoHanh) <= 30,
                'bh-status-pill--expired': isExpired(item.ngayHetBaoHanh),
              }">
                <component :is="isExpired(item.ngayHetBaoHanh)?ShieldX:daysUntilWarrantyExpiry(item.ngayHetBaoHanh)<=30?ShieldAlert:ShieldCheck" :size="11" />
                {{ isExpired(item.ngayHetBaoHanh)?'Hết BH':daysUntilWarrantyExpiry(item.ngayHetBaoHanh)<=30?`${daysUntilWarrantyExpiry(item.ngayHetBaoHanh)} ngày`:'Còn BH' }}
              </div>
              <div class="bh-expiry">Hết: {{ formatDate(item.ngayHetBaoHanh) }}</div>
            </div>
            <div class="col-action">
              <button v-if="canCreateClaim(item)" class="btn-claim-sm" @click="openClaimForm(item)"><Plus :size="12" /> Yêu cầu BH</button>
              <span v-else-if="isExpired(item.ngayHetBaoHanh)" class="expired-hint-sm">Hết hạn</span>
              <span v-else-if="hasActiveClaim(item.chiTietId)" class="pending-hint-sm"><Clock :size="10" /> Đang xử lý</span>
            </div>
          </div>
          <div class="table-summary">Hiển thị {{ filterProducts.length }} / {{ allProducts.length }} sản phẩm</div>
        </div>
    </div>

    <!-- TAB 2: PHIẾU BẢO HÀNH -->
    <div v-if="mainSubTab === 'claims'" class="tab-content">
      <WarrantyClaimListPanel :claims="claims" :loading="loadingClaims" @toast="(m,t)=>emit('toast',m,t)" @view-product="handleViewProduct" />
    </div>

    <!-- Claim Form Modal -->
    <ClaimFormModal v-if="claimFormOpen && claimFormProduct" :product="claimFormProduct"
      @close="claimFormOpen=false;claimFormProduct=null"
      @submitted="onClaimSubmitted"
      @toast="(m,t)=>emit('toast',m,t)"
    />
  </div>
</template>

<style scoped>
.warranty-tab { display: flex; flex-direction: column; }
.warranty-main-tabs { display: flex; gap: 4px; padding: 10px 16px; border-bottom: 1px solid var(--border,#e5e7eb); background: var(--gray-50,#f9fafb); }
.warranty-main-tab { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; background: transparent; border: 1.5px solid transparent; border-radius: 9999px; color: var(--text-secondary,#6b7280); font-size: 13px; font-weight: 600; cursor: pointer; transition: all .2s ease; white-space: nowrap; }
.warranty-main-tab:hover { background: var(--bg-card,#fff); color: var(--text-primary,#111827); border-color: var(--border,#e5e7eb); }
.warranty-main-tab.is-active { background: var(--pink-500,#db2777); color: white; border-color: var(--pink-500,#db2777); }
.tab-badge { min-width: 18px; height: 18px; padding: 0 5px; background: rgba(255,255,255,.25); border-radius: 9999px; font-size: 10px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.warranty-main-tab:not(.is-active) .tab-badge { background: var(--pink-500,#db2777); color: white; }
.tab-content { padding: 14px 16px; }
.view-toggle { display: flex; gap: 6px; margin-bottom: 12px; padding: 4px; background: var(--gray-100,#f3f4f6); border-radius: 10px; width: fit-content; }
.view-toggle-btn { display: inline-flex; align-items: center; gap: 5px; padding: 6px 14px; background: transparent; border: none; border-radius: 8px; color: var(--text-secondary,#6b7280); font-size: 12.5px; font-weight: 600; cursor: pointer; transition: all .2s ease; }
.view-toggle-btn:hover { color: var(--text-primary,#111827); }
.view-toggle-btn.is-active { background: var(--bg-card,#fff); color: var(--pink-600,#a82560); box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.filter-bar { display: flex; gap: 6px; margin-bottom: 12px; flex-wrap: wrap; }
.filter-chip { display: inline-flex; align-items: center; gap: 5px; padding: 6px 12px; background: var(--bg-card,#fff); border: 1.5px solid var(--border,#e5e7eb); border-radius: 9999px; color: var(--text-secondary,#6b7280); font-size: 12px; font-weight: 600; cursor: pointer; transition: all .15s ease; }
.filter-chip:hover { border-color: var(--pink-400,#ec4899); color: var(--pink-600,#a82560); }
.filter-chip.is-active { background: var(--pink-500,#db2777); border-color: var(--pink-500,#db2777); color: white; }
.filter-count { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 5px; background: rgba(255,255,255,.25); border-radius: 9999px; font-size: 10px; font-weight: 700; }
.filter-chip:not(.is-active) .filter-count { background: var(--pink-100,#fce7f3); color: var(--pink-600,#a82560); }
.filter-count--warning { background: rgba(245,158,11,.15) !important; color: #b45309 !important; }
.filter-chip.is-active .filter-count--warning { background: rgba(255,255,255,.25) !important; color: white !important; }
.filter-count--expired { background: rgba(107,114,128,.15) !important; color: #374151 !important; }
.filter-chip.is-active .filter-count--expired { background: rgba(255,255,255,.25) !important; color: white !important; }
.loading-block { display: flex; flex-direction: column; gap: 10px; }
.empty-block { text-align: center; padding: 40px 20px; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.empty-icon { color: var(--text-muted,#9ca3af); }
.empty-title { font-weight: 700; font-size: 14px; color: var(--text-primary,#111827); }
.empty-desc { font-size: 12.5px; color: var(--text-secondary,#6b7280); max-width: 300px; line-height: 1.5; }
.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-card { background: var(--bg-card,#fff); border: 1.5px solid var(--border,#e5e7eb); border-radius: 12px; overflow: hidden; transition: border-color .15s ease; }
.order-card:hover { border-color: var(--pink-300,#f7a8c8); }
.order-header { width: 100%; display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; background: transparent; border: none; cursor: pointer; text-align: left; gap: 10px; transition: background .15s ease; }
.order-header:hover { background: var(--pink-50,#fff5f9); }
.order-header-left { flex: 1; min-width: 0; }
.order-code { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.order-code-label { font-size: 11px; color: var(--text-muted,#9ca3af); text-transform: uppercase; letter-spacing: .05em; }
.order-code-value { font-size: 13px; color: var(--pink-600,#a82560); font-weight: 700; }
.order-meta { display: flex; gap: 12px; font-size: 11.5px; color: var(--text-secondary,#6b7280); flex-wrap: wrap; }
.order-meta span { display: inline-flex; align-items: center; gap: 3px; }
.order-header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.warranty-badge { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border-radius: 9999px; font-size: 10.5px; font-weight: 700; }
.warranty-badge.badge--active { background: rgba(34,197,94,.12); color: #15803d; }
.warranty-badge.badge--expiring { background: rgba(245,158,11,.15); color: #b45309; }
.warranty-badge.badge--expired { background: rgba(107,114,128,.12); color: #374151; }
.warranty-badge.badge--mixed { background: rgba(37,99,235,.12); color: #1d4ed8; }
.warranty-badge.badge--none { background: var(--gray-100,#f3f4f6); color: var(--text-muted,#9ca3af); }
.product-count { font-size: 11.5px; font-weight: 600; color: var(--text-secondary,#6b7280); background: var(--gray-100,#f3f4f6); padding: 3px 9px; border-radius: 9999px; }
.chevron { color: var(--text-muted,#9ca3af); transition: transform .2s ease; flex-shrink: 0; }
.chevron.is-open { transform: rotate(180deg); }
.order-products { border-top: 1px solid var(--border,#e5e7eb); padding: 10px; display: flex; flex-direction: column; gap: 8px; background: var(--gray-50,#f9fafb); }
.product-row { display: flex; align-items: center; gap: 10px; padding: 10px; background: var(--bg-card,#fff); border: 1px solid var(--border,#e5e7eb); border-radius: 10px; transition: all .15s ease; }
.product-row:hover { border-color: var(--pink-300,#f7a8c8); }
.product-thumb { width: 52px; height: 52px; background: var(--gray-50,#f9fafb); border: 1px solid var(--border,#e5e7eb); border-radius: 8px; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; cursor: pointer; transition: border-color .15s ease; }
.product-thumb:hover { border-color: var(--pink-400,#ec4899); }
.product-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 4px; }
.product-thumb svg { color: var(--text-muted,#9ca3af); }
.product-info { flex: 1; min-width: 0; }
.product-name { font-weight: 600; font-size: 12.5px; color: var(--text-primary,#111827); line-height: 1.4; cursor: pointer; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.product-name:hover { color: var(--pink-600,#a82560); }
.product-meta-row { display: flex; gap: 8px; margin-top: 3px; flex-wrap: wrap; }
.meta-chip { font-size: 10.5px; color: var(--text-muted,#9ca3af); background: var(--gray-100,#f3f4f6); padding: 1px 6px; border-radius: 4px; display: inline-flex; align-items: center; gap: 3px; font-family: 'SF Mono','Consolas',monospace; }
.product-bh { display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 110px; flex-shrink: 0; }
.bh-badge { display: flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 8px; width: 100%; justify-content: center; }
.bh-badge--active { background: rgba(34,197,94,.1); color: #15803d; }
.bh-badge--expiring { background: rgba(245,158,11,.12); color: #b45309; }
.bh-badge--expired { background: rgba(107,114,128,.1); color: #374151; }
.bh-badge-text { text-align: left; }
.bh-badge-main { font-size: 11.5px; font-weight: 700; line-height: 1.2; }
.bh-badge-sub { font-size: 9.5px; opacity: .75; margin-top: 1px; }
.active-claim-chip { display: inline-flex; align-items: center; gap: 3px; font-size: 10px; font-weight: 600; color: #1d4ed8; background: rgba(37,99,235,.1); padding: 2px 8px; border-radius: 9999px; }
.product-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.btn-claim { display: inline-flex; align-items: center; gap: 4px; padding: 7px 12px; background: var(--pink-500,#db2777); color: white; border: none; border-radius: 8px; font-size: 11.5px; font-weight: 700; cursor: pointer; white-space: nowrap; transition: all .2s ease; box-shadow: 0 2px 0 var(--pink-700,#a82560); }
.btn-claim:hover { background: var(--pink-600,#a82560); transform: translateY(-1px); }
.btn-claim:active { transform: translateY(1px); box-shadow: inset 0 2px 3px rgba(0,0,0,.2); }
.expired-hint { font-size: 11px; color: var(--text-muted,#9ca3af); font-style: italic; white-space: nowrap; }
.pending-hint { font-size: 11px; color: #1d4ed8; font-weight: 600; white-space: nowrap; }
.product-table { border: 1.5px solid var(--border,#e5e7eb); border-radius: 12px; overflow: hidden; }
.product-table-header { display: flex; align-items: center; padding: 10px 14px; background: var(--gray-100,#f3f4f6); font-size: 11px; font-weight: 700; color: var(--text-muted,#9ca3af); text-transform: uppercase; letter-spacing: .05em; gap: 8px; }
.product-table-row { display: flex; align-items: center; padding: 10px 14px; border-top: 1px solid var(--border,#e5e7eb); transition: background .15s ease; gap: 8px; }
.product-table-row:hover { background: var(--pink-50,#fff5f9); }
.col-thumb { width: 52px; flex-shrink: 0; }
.col-info { flex: 1; min-width: 0; }
.col-order { width: 100px; flex-shrink: 0; }
.col-bh { width: 110px; flex-shrink: 0; }
.col-action { width: 120px; flex-shrink: 0; display: flex; align-items: center; justify-content: flex-end; gap: 6px; }
.thumb-box { width: 48px; height: 48px; background: var(--gray-50,#f9fafb); border: 1px solid var(--border,#e5e7eb); border-radius: 8px; display: flex; align-items: center; justify-content: center; overflow: hidden; cursor: pointer; }
.thumb-box img { width: 100%; height: 100%; object-fit: contain; padding: 3px; }
.thumb-box svg { color: var(--text-muted,#9ca3af); }
.table-product-name { font-weight: 600; font-size: 12.5px; color: var(--text-primary,#111827); cursor: pointer; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.3; }
.table-product-name:hover { color: var(--pink-600,#a82560); }
.table-meta { display: flex; gap: 8px; font-size: 10.5px; color: var(--text-muted,#9ca3af); margin-top: 3px; flex-wrap: wrap; }
.order-link { font-size: 12px; font-weight: 700; color: var(--pink-600,#a82560); }
.order-date { font-size: 11px; color: var(--text-muted,#9ca3af); margin-top: 2px; }
.bh-status-pill { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 9999px; font-size: 11px; font-weight: 700; margin-bottom: 2px; }
.bh-status-pill--active { background: rgba(34,197,94,.12); color: #15803d; }
.bh-status-pill--expiring { background: rgba(245,158,11,.12); color: #b45309; }
.bh-status-pill--expired { background: rgba(107,114,128,.12); color: #374151; }
.bh-expiry { font-size: 10px; color: var(--text-muted,#9ca3af); }
.btn-claim-sm { display: inline-flex; align-items: center; gap: 3px; padding: 5px 10px; background: var(--pink-500,#db2777); color: white; border: none; border-radius: 6px; font-size: 11px; font-weight: 700; cursor: pointer; white-space: nowrap; transition: all .15s ease; }
.btn-claim-sm:hover { background: var(--pink-600,#a82560); }
.expired-hint-sm { font-size: 10.5px; color: var(--text-muted,#9ca3af); font-style: italic; }
.pending-hint-sm { display: inline-flex; align-items: center; gap: 3px; font-size: 10.5px; color: #1d4ed8; font-weight: 600; }
.table-summary { margin-top: 10px; font-size: 11.5px; color: var(--text-muted,#9ca3af); text-align: right; padding: 0 14px 10px; }
.slide-down-enter-active, .slide-down-leave-active { transition: all .25s ease; overflow: hidden; max-height: 2000px; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; max-height: 0; }
.slide-down-enter-to, .slide-down-leave-from { opacity: 1; max-height: 2000px; }
@media (max-width: 640px) {
  .col-order { display: none; }
  .col-bh { width: 90px; }
  .col-action { width: 80px; }
  .product-row { flex-wrap: wrap; }
  .product-info { width: calc(100% - 70px); }
  .product-bh { flex-direction: row; align-items: center; justify-content: space-between; width: 100%; }
  .bh-badge { width: auto; }
}
</style>
