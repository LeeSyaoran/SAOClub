<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { t } from "../../i18n/index.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { formatDateTime, statusLabel } from "../../utils/adminFormat.js";
import ProductFormModal from "./ProductFormModal.vue";
import BienTheTable from "./BienTheTable.vue";
import { Image } from "@lucide/vue";
import * as SanPhamService from "../../services/SanPhamService.js";

const props = defineProps({ sanPhamId: { type: Number, required: true } });
const router = useRouter();

const history = ref([]);
const historyLoading = ref(false);
const loadHistory = async () => {
  historyLoading.value = true;
  try {
    history.value = await SanPhamService.getLichSu(props.sanPhamId);
  } catch (e) {
    history.value = [];
  } finally {
    historyLoading.value = false;
  }
};

onMounted(() => {
  ensureProducts();
  loadHistory();
});

const productVariants = computed(() =>
  (ProductsStore.items ?? []).filter((p) => p.sanPhamId === props.sanPhamId),
);
const productInfo = computed(() => productVariants.value[0] ?? null);

const activeTab = ref("info");
const showEditModal = ref(false);
const onSaved = () => refreshProducts();
const back = () => router.push("/admin");
</script>

<template>
  <div v-if="!productInfo" class="text-secondary small">{{ t("admin.productDetail.loading") }}</div>
  <div v-else>
    <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
      <div>
        <button class="btn btn-sm btn-outline-secondary mb-2" @click="back">{{ t("common.back") }}</button>
        <div class="fw-bold" style="font-size: 1.1rem">
          {{ t("admin.productDetail.breadcrumb") }} &gt; {{ productInfo.tenSanPham }}
        </div>
      </div>
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="showEditModal = true">
        {{ t("admin.productDetail.editButton") }}
      </button>
    </div>

    <div class="d-flex gap-2 mb-3" style="border-bottom: 1px solid var(--border-color)">
      <button
        class="btn btn-sm"
        :class="activeTab === 'info' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
        style="border-radius: 6px 6px 0 0"
        @click="activeTab = 'info'"
      >
        {{ t("admin.productDetail.tabInfo") }}
      </button>
      <button
        class="btn btn-sm"
        :class="activeTab === 'variants' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
        style="border-radius: 6px 6px 0 0"
        @click="activeTab = 'variants'"
      >
        {{ t("admin.productDetail.tabVariants") }}
      </button>
      <button
        class="btn btn-sm"
        :class="activeTab === 'history' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
        style="border-radius: 6px 6px 0 0"
        @click="activeTab = 'history'"
      >
        {{ t("admin.productDetail.tabHistory") }}
      </button>
    </div>

    <section v-show="activeTab === 'info'">
      <div class="row g-3">
        <div class="col-md-6">
          <div class="rounded-3 p-3" style="background: var(--bg-card); border: 1px solid var(--border-color)">
            <div class="text-uppercase fw-bold mb-2" style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa">
              {{ t("admin.productDetail.cardBasic") }}
            </div>
            <div class="d-flex align-items-center gap-3 mb-3">
              <div
                class="rounded-2 d-flex align-items-center justify-content-center flex-shrink-0"
                style="width: 64px; height: 64px; background: var(--bg-card-inset); overflow: hidden"
              >
                <img
                  v-if="productInfo.hinhAnhChinh"
                  :src="productInfo.hinhAnhChinh"
                  :alt="productInfo.tenSanPham"
                  style="width: 100%; height: 100%; object-fit: cover"
                />
                <Image v-else :size="20" color="var(--text-muted)" />
              </div>
              <div>
                <div class="fw-bold">{{ productInfo.tenSanPham }}</div>
                <span class="badge" :class="productInfo.trangThai === 'active' ? 'bg-success' : 'bg-secondary'">{{
                  statusLabel(productInfo.trangThai)
                }}</span>
              </div>
            </div>
            <div class="small text-secondary d-flex flex-column gap-1">
              <div>{{ t("admin.productModal.brandLabel").replace(" *", "") }}: <span class="text-primary">{{ productInfo.tenThuongHieu }}</span></div>
              <div>{{ t("admin.productModal.categoryLabel").replace(" *", "") }}: <span class="text-primary">{{ productInfo.tenDanhMuc }}</span></div>
              <div>{{ t("admin.productModal.supplierLabel").replace(" *", "") }}: <span class="text-primary">{{ productInfo.tenNhaCungCap || t("admin.productModal.noneOption") }}</span></div>
              <div>{{ t("admin.productModal.typeLabel").replace(" *", "") }}: <span class="text-primary">{{ productInfo.loaiSanPham }}</span></div>
              <div>{{ t("admin.productDetail.releaseDate") }}: <span class="text-primary">{{ formatDateTime(productInfo.ngayTao) }}</span></div>
              <div v-if="productInfo.moTa">{{ t("admin.productModal.descLabel") }}: <span class="text-primary">{{ productInfo.moTa }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-show="activeTab === 'variants'">
      <BienTheTable :filter-san-pham-id="sanPhamId" />
    </section>

    <section v-show="activeTab === 'history'">
      <div v-if="historyLoading" class="text-secondary small">{{ t("admin.productDetail.loading") }}</div>
      <div v-else class="table-responsive">
        <table
          class="table table-hover table-sm align-middle"
          style="--bs-table-bg: var(--bg-card); --bs-table-color: var(--text-primary); --bs-table-border-color: var(--border-color-soft)"
        >
          <thead>
            <tr>
              <th>{{ t("admin.productDetail.historyColTime") }}</th>
              <th>{{ t("admin.productDetail.historyColUser") }}</th>
              <th>{{ t("admin.productDetail.historyColTarget") }}</th>
              <th>{{ t("admin.productDetail.historyColField") }}</th>
              <th>{{ t("admin.productDetail.historyColOld") }}</th>
              <th>{{ t("admin.productDetail.historyColNew") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="h in history" :key="h.lichSuId">
              <td class="text-secondary" style="font-size: 0.78rem">{{ formatDateTime(h.thoiGian) }}</td>
              <td>{{ h.tenNhanVien || t("admin.productDetail.historyUnknownUser") }}</td>
              <td>
                <span v-if="h.doiTuong === 'bien_the'">{{ t("admin.productDetail.historyTargetVariant") }} ({{ h.maSku }})</span>
                <span v-else>{{ t("admin.productDetail.historyTargetProduct") }}</span>
              </td>
              <td>{{ t(`admin.productDetail.fields.${h.tenTruong}`) }}</td>
              <td class="text-secondary">{{ h.giaTriCu ?? "—" }}</td>
              <td class="text-primary">{{ h.giaTriMoi ?? "—" }}</td>
            </tr>
            <tr v-if="history.length === 0">
              <td colspan="6" class="text-center text-secondary">{{ t("admin.productDetail.historyEmpty") }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <ProductFormModal v-model="showEditModal" mode="edit" :san-pham-id="sanPhamId" @saved="onSaved" />
  </div>
</template>
