<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
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
import { Image, Hash, Tag, Laptop, Layers, Building2, DollarSign, Activity, Calendar, CalendarCheck, SlidersHorizontal } from "@lucide/vue";

const props = defineProps({ readonly: { type: Boolean, default: false } });
const router = useRouter();

onMounted(() => {
  ensureProducts();
});

// ── Sắp xếp nâng cao ──────────────────────────────────────────────────────────
// sortKey: 'stt_desc' | 'stt_asc' | 'name_asc' | 'name_desc'
const sortKey = ref("stt_desc");
const sortOptions = [
  { value: "stt_desc",  label: "STT: Lớn → Nhỏ" },
  { value: "stt_asc",   label: "STT: Nhỏ → Lớn" },
  { value: "name_asc",  label: "Tên: A → Z" },
  { value: "name_desc", label: "Tên: Z → A" },
];

// ── Bo loc + gop bien the theo sanPhamId cho bang ─────────────────────────────
const productSearch = ref("");

/** Nhóm theo sanPhamId, chưa sort */
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

/** Danh sách đã sắp xếp theo sortKey */
const sortedGroupedProducts = computed(() => {
  const list = [...groupedProducts.value];
  switch (sortKey.value) {
    case "stt_asc":
      return list.sort((a, b) => a.sanPhamId - b.sanPhamId);
    case "name_asc":
      return list.sort((a, b) =>
        (a.tenSanPham ?? "").localeCompare(b.tenSanPham ?? "", "vi")
      );
    case "name_desc":
      return list.sort((a, b) =>
        (b.tenSanPham ?? "").localeCompare(a.tenSanPham ?? "", "vi")
      );
    case "stt_desc":
    default:
      return list.sort((a, b) => b.sanPhamId - a.sanPhamId);
  }
});

/** Danh sách sau khi lọc theo từ khóa tìm kiếm */
const filteredGroupedProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase();
  if (!q) return sortedGroupedProducts.value;
  return sortedGroupedProducts.value.filter(
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
  router.push(`/admin/san-pham/${sanPhamId}`);
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
    <div class="d-flex gap-2 flex-wrap align-items-center">
      <!-- Tìm kiếm -->
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
      <!-- Lọc nâng cao: sắp xếp -->
      <select
        v-model="sortKey"
        class="form-select form-select-sm sort-select"
        title="Sắp xếp nâng cao"
        style="
          width: auto;
          min-width: 160px;
          background: var(--bg-input);
          border-color: var(--border-color-strong);
          color: var(--text-primary);
        "
      >
        <option
          v-for="opt in sortOptions"
          :key="opt.value"
          :value="opt.value"
        >{{ opt.label }}</option>
      </select>
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
          <th style="width: 4%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Hash :size="12" /> {{ t("admin.common.stt") }}</span></th>
          <th style="width: 9%;"><span class="d-inline-flex align-items-center gap-1.5"><Tag :size="12" /> {{ t("admin.products.colSku") }}</span></th>
          <th style="width: 24%;"><span class="d-inline-flex align-items-center gap-1.5"><Laptop :size="12" /> {{ t("admin.products.colName") }}</span></th>
          <th style="width: 10%;"><span class="d-inline-flex align-items-center gap-1.5"><Layers :size="12" /> {{ t("admin.products.colCategory") }}</span></th>
          <th style="width: 10%;"><span class="d-inline-flex align-items-center gap-1.5"><Building2 :size="12" /> {{ t("admin.products.colBrand") }}</span></th>
          <th style="width: 9%; text-align: right;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><DollarSign :size="12" /> {{ t("admin.products.colPriceFrom") }}</span></th>
          <th style="width: 9%; text-align: right;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><DollarSign :size="12" /> {{ t("admin.products.colPriceTo") }}</span></th>
          <th style="width: 8%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Activity :size="12" /> {{ t("admin.products.colStatus") }}</span></th>
          <th style="width: 6%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Calendar :size="12" /> {{ t("admin.products.colCreated") }}</span></th>
          <th style="width: 6%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><CalendarCheck :size="12" /> {{ t("admin.products.colUpdated") }}</span></th>
          <th style="width: 5%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><SlidersHorizontal :size="12" /> {{ t("admin.products.colAction") }}</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(p, idx) in pagedProducts" :key="p.sanPhamId">
          <td class="text-secondary text-center">{{
            sortKey === 'stt_desc'
              ? filteredGroupedProducts.length - (currentPage * pageSize + idx)
              : currentPage * pageSize + idx + 1
          }}</td>
          <td class="text-secondary" style="font-family: monospace; font-size: 0.8rem">{{ p.maSanPham }}</td>
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
          <td class="text-end">{{ formatPrice(p.minPrice) }}</td>
          <td class="text-end">{{ formatPrice(p.maxPrice) }}</td>
          <td class="text-center">
            <span
              class="badge"
              :class="p.trangThai === 'active' ? 'bg-success' : 'bg-secondary'"
            >{{ statusLabel(p.trangThai) }}</span>
          </td>
          <td class="text-secondary text-center" style="font-size: 0.78rem">{{ formatDateTime(p.ngayTao) }}</td>
          <td class="text-secondary text-center" style="font-size: 0.78rem">{{ formatDateTime(p.ngayCapNhat) }}</td>
          <td class="text-center">
            <div class="d-flex justify-content-center gap-1">
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

.sort-select option {
  background: var(--bg-card);
  color: var(--text-primary);
}
</style>
