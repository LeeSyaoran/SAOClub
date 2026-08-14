<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as SanPhamService from "../../services/SanPhamService.js";
import { formatPrice, statusLabel, formatDateTime } from "../../utils/adminFormat.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import {
  ProductsStore,
  ensureProducts,
  refreshProducts,
} from "../../stores/products.js";
import ProductDetailModal from "./ProductDetailModal.vue";
import ProductFormModal from "./ProductFormModal.vue";
import Pagination from "../common/Pagination.vue";
import { usePagination } from "../../composables/usePagination.js";
import { Image } from "@lucide/vue";

const props = defineProps({ readonly: { type: Boolean, default: false } });

onMounted(() => {
  ensureProducts();
});

// ── Bo loc + gop bien the theo sanPhamId cho bang ─────────────────────────────
const productSearch = ref("");
const groupedProducts = computed(() => {
  const map = new Map();
  (ProductsStore.items ?? []).forEach((p) => {
    if (!map.has(p.sanPhamId)) {
      map.set(p.sanPhamId, {
        ...p,
        variantCount: 1,
        minPrice: Number(p.giaBan),
        maxPrice: Number(p.giaBan),
      });
    } else {
      const ex = map.get(p.sanPhamId);
      ex.variantCount++;
      if (Number(p.giaBan) < ex.minPrice) ex.minPrice = Number(p.giaBan);
      if (Number(p.giaBan) > ex.maxPrice) ex.maxPrice = Number(p.giaBan);
    }
  });
  return [...map.values()];
});
const filteredGroupedProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase();
  if (!q) return groupedProducts.value;
  return groupedProducts.value.filter(
    (p) =>
      (p.tenSanPham ?? "").toLowerCase().includes(q) ||
      (p.tenThuongHieu ?? "").toLowerCase().includes(q),
  );
});
const { currentPage, totalPages, pagedItems: pagedProducts, pageSize } = usePagination(filteredGroupedProducts);

// ── Modal "Chi tiet san pham" (xem) ───────────────────────────────────────────
const showDetailModal = ref(false);
const detailModalSanPhamId = ref(null);
const detailModalSanPhamName = ref("");
const openDetail = (sanPhamId, name) => {
  if (props.readonly) {
    detailModalSanPhamId.value = sanPhamId;
    detailModalSanPhamName.value = name;
    showDetailModal.value = true;
    return;
  }
  window.open(`${location.origin}${location.pathname}#/admin/san-pham/${sanPhamId}`, "_blank");
};

// ── Products CRUD (chỉ tạo mới — sửa/thêm biến thể đã chuyển sang BienTheTable.vue,
// xem tab "Biến thể") ───────────────────────────────────────────────────────────────
// Form thêm/sửa sản phẩm đã tách sang ProductFormModal.vue (dùng lại ở SanPhamDetailPage.vue).
const showProductModal = ref(false);
const formMode = ref("create");
const formSanPhamId = ref(null);

const openAdd = () => {
  formMode.value = "create";
  formSanPhamId.value = null;
  showProductModal.value = true;
};
const openEdit = (sanPhamId) => {
  formMode.value = "edit";
  formSanPhamId.value = sanPhamId;
  showProductModal.value = true;
};
// Xoa xong khong can tai lai ca bang — API tra 204 rong nen chi can biet ID
// vua xoa la du de loc khoi mang cuc bo (products = 1 dong/bien the, nen xoa
// san pham = xoa het cac dong cung sanPhamId).
// Hoi truoc khi bam xoa: san pham chua tung ban -> chi hoi xac nhan don gian; da co giao
// dich -> bao thang ly do khong xoa duoc, khoi can hoi "co chac khong" cho viec chac chan
// se that bai.
const deleteProduct = async (id) => {
  const name =
    (ProductsStore.items ?? []).find((p) => p.sanPhamId === id)?.tenSanPham ?? "";
  const daGiaoDich = await SanPhamService.hasTransactionHistory(id).catch(
    () => false,
  );
  if (daGiaoDich) {
    showToast(t("admin.errors.cannotDeleteProduct", { name }));
    return;
  }
  if (!(await askConfirm(t("admin.confirm.deleteProductSimple", { name }))))
    return;
  const res = await SanPhamService.remove(id);
  if (!res.ok) {
    showToast(
      await res
        .text()
        .catch(() => t("admin.errors.deleteFailed", { status: res.status })),
    );
    return;
  }
  await refreshProducts();
};
</script>

<template>
  <div
    class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"
  >
    <span class="text-secondary small">{{ filteredGroupedProducts.length }}/{{ groupedProducts.length }}
      {{ t("admin.products.countSuffix") }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input
        v-model="productSearch"
        class="form-control form-control-sm"
        style="
          width: 220px;
          background: var(--bg-input);
          border-color: var(--border-color-strong);
          color: var(--text-primary);
        "
        :placeholder="t('admin.products.searchPlaceholder')"
      />
      <button
        v-if="!readonly"
        class="btn btn-sm btn-warning text-dark fw-bold"
        @click="openAdd"
      >
        {{ t("admin.products.add") }}
      </button>
    </div>
  </div>
  <div v-if="ProductsStore.loading" class="text-secondary small">
    {{ t("admin.products.loading") }}
  </div>
  <div v-else class="table-responsive">
    <table
      class="table table-hover table-sm align-middle"
      style="
        --bs-table-bg: var(--bg-card);
        --bs-table-color: var(--text-primary);
        --bs-table-hover-bg: var(--bg-hover);
        --bs-table-hover-color: var(--text-primary);
        --bs-table-border-color: var(--border-color-soft);
      "
    >
      <thead>
        <tr>
          <th style="width: 40px">{{ t("admin.common.stt") }}</th>
          <th>{{ t("admin.products.colSku") }}</th>
          <th>{{ t("admin.products.colName") }}</th>
          <th>{{ t("admin.products.colCategory") }}</th>
          <th>{{ t("admin.products.colBrand") }}</th>
          <th>{{ t("admin.products.colPriceFrom") }}</th>
          <th>{{ t("admin.products.colPriceTo") }}</th>
          <th>{{ t("admin.products.colStatus") }}</th>
          <th>{{ t("admin.products.colCreated") }}</th>
          <th>{{ t("admin.products.colUpdated") }}</th>
          <th>{{ t("admin.products.colAction") }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(p, idx) in pagedProducts" :key="p.sanPhamId">
          <td class="text-secondary">{{ currentPage * pageSize + idx + 1 }}</td>
          <td class="text-secondary" style="font-family: monospace; font-size: 0.8rem">{{ p.maSku }}</td>
          <td>
            <div class="d-flex align-items-center gap-2">
              <div
                class="rounded-2 d-flex align-items-center justify-content-center flex-shrink-0"
                style="width: 32px; height: 32px; background: var(--bg-card-inset); overflow: hidden"
              >
                <img
                  v-if="p.hinhAnhChinh"
                  :src="p.hinhAnhChinh"
                  :alt="p.tenSanPham"
                  style="width: 100%; height: 100%; object-fit: cover"
                />
                <Image v-else :size="14" color="var(--text-muted)" />
              </div>
              {{ p.tenSanPham }}
            </div>
          </td>
          <td>{{ p.tenDanhMuc }}</td>
          <td>{{ p.tenThuongHieu }}</td>
          <td>{{ formatPrice(p.minPrice) }}</td>
          <td>{{ formatPrice(p.maxPrice) }}</td>
          <td>
            <span
              class="badge"
              :class="p.trangThai === 'active' ? 'bg-success' : 'bg-secondary'"
            >{{ statusLabel(p.trangThai) }}</span>
          </td>
          <td class="text-secondary" style="font-size: 0.78rem">{{ formatDateTime(p.ngayTao) }}</td>
          <td class="text-secondary" style="font-size: 0.78rem">{{ formatDateTime(p.ngayCapNhat) }}</td>
          <td>
            <div class="d-flex gap-1">
              <button
                class="btn btn-sm btn-outline-primary"
                style="font-size: 0.78rem; padding: 2px 8px"
                @click="openDetail(p.sanPhamId, p.tenSanPham)"
              >
                {{ t("admin.products.detail") }}
              </button>
              <button
                v-if="!readonly"
                class="btn btn-sm btn-outline-secondary"
                style="font-size: 0.78rem; padding: 2px 8px"
                @click="openEdit(p.sanPhamId)"
              >
                {{ t("admin.variants.edit") }}
              </button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredGroupedProducts.length === 0">
          <td colspan="11" class="text-center text-secondary">
            {{ t("admin.products.empty") }}
          </td>
        </tr>
      </tbody>
    </table>
    <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="currentPage = $event" />
  </div>

  <ProductDetailModal
    v-model="showDetailModal"
    :san-pham-id="detailModalSanPhamId"
    :san-pham-name="detailModalSanPhamName"
  />

  <ProductFormModal
    v-model="showProductModal"
    :mode="formMode"
    :san-pham-id="formSanPhamId"
    @saved="refreshProducts"
  />
</template>

<style scoped>
.text-light {
  color: var(--text-primary) !important;
}
</style>
