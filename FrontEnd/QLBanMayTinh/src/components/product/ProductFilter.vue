<template>
  <div class="d-flex flex-column gap-3">
    <!-- Danh mục — ưu tiên cao nhất -->
    <div>
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.category') }}
      </div>
      <!-- Skeleton while loading, stable height to prevent jump -->
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 4" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else-if="categories.length" class="d-flex flex-wrap gap-2">
        <button
          v-for="c in categories" :key="c.id"
          class="btn btn-sm"
          :class="selectedCategory === c.id ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="selectCategory(c.id)"
        >
          {{ c.tenDanhMuc }}
        </button>
      </div>
    </div>

    <!-- Thương hiệu -->
    <div>
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.brand') }}
      </div>
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 6" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else class="d-flex flex-wrap gap-2">
        <button
          v-for="b in brands" :key="b"
          class="btn btn-sm"
          :class="selectedBrands.includes(b) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="toggleBrand(b)"
        >
          {{ b }}
        </button>
      </div>
    </div>

    <!-- Khoảng giá -->
    <div>
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.priceRange') }}
      </div>
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 4" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else class="d-flex flex-wrap gap-2">
        <button
          v-for="range in priceRanges" :key="range.label"
          class="btn btn-sm"
          :class="selectedPrice === range.label ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="selectPrice(range)"
        >
          {{ range.label }}
        </button>
      </div>
    </div>

    <div v-if="cpus.length">
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.cpu') }}
      </div>
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 5" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else class="d-flex flex-wrap gap-2">
        <button
          v-for="c in cpus" :key="c"
          class="btn btn-sm"
          :class="selectedCpu.includes(c) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="toggleSpec(selectedCpu, c)"
        >
          {{ c }}
        </button>
      </div>
    </div>

    <div v-if="rams.length">
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.ram') }}
      </div>
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 4" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else class="d-flex flex-wrap gap-2">
        <button
          v-for="r in rams" :key="r"
          class="btn btn-sm"
          :class="selectedRam.includes(r) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="toggleSpec(selectedRam, r)"
        >
          {{ r }}
        </button>
      </div>
    </div>

    <div v-if="gpus.length">
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.gpu') }}
      </div>
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 4" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else class="d-flex flex-wrap gap-2">
        <button
          v-for="g in gpus" :key="g"
          class="btn btn-sm"
          :class="selectedGpu.includes(g) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="toggleSpec(selectedGpu, g)"
        >
          {{ g }}
        </button>
      </div>
    </div>

    <div v-if="storages.length">
      <div
        class="fw-bold small text-uppercase mb-2"
        style="letter-spacing:0.05em; font-size:0.72rem; color:var(--text-secondary);"
      >
        {{ t('productFilter.storage') }}
      </div>
      <div v-if="loading" class="d-flex flex-wrap gap-2">
        <div v-for="i in 4" :key="i" class="rounded-pill skeleton-chip" />
      </div>
      <div v-else class="d-flex flex-wrap gap-2">
        <button
          v-for="s in storages" :key="s"
          class="btn btn-sm"
          :class="selectedStorage.includes(s) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
          style="font-size:0.78rem; border-radius:20px;"
          @click="toggleSpec(selectedStorage, s)"
        >
          {{ s }}
        </button>
      </div>
    </div>

    <div v-if="hasFilter">
      <button
        class="btn btn-sm btn-outline-danger"
        style="font-size:0.78rem; border-radius:20px;"
        @click="clearAll"
      >
        {{ t('productFilter.clearFilter') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { t } from '../../i18n/index.js';

const props = defineProps({
  // Danh sách tên thương hiệu (string[])
  brands:     { type: Array, default: () => [] },
  // Danh sách danh mục từ API [{ id, tenDanhMuc }]
  categories: { type: Array, default: () => [] },
  // Danh sách giá trị cấu hình xuất hiện trong tập sản phẩm hiện tại (string[]) — CustomerPage.vue
  // tự tính từ products.value, chỉ hiện chip cho giá trị THỰC SỰ có hàng, tránh chip chọn xong
  // ra danh sách rỗng.
  cpus:     { type: Array, default: () => [] },
  rams:     { type: Array, default: () => [] },
  gpus:     { type: Array, default: () => [] },
  storages: { type: Array, default: () => [] },
  // Loading state — hiện skeleton thay vì placeholder trống
  loading:   { type: Boolean, default: false },
});

// Emit 'change' mỗi khi bộ lọc thay đổi — App.vue lắng nghe để filter danh sách sản phẩm
const emit = defineEmits(['change']);

// Trạng thái bộ lọc nội bộ
const selectedBrands   = ref([]);
const selectedPrice    = ref('');
const selectedCategory = ref(null);
const priceMin         = ref(null);
const priceMax         = ref(null);
const selectedCpu      = ref([]);
const selectedRam      = ref([]);
const selectedGpu      = ref([]);
const selectedStorage  = ref([]);

// Bật/tắt 1 giá trị trong mảng multi-select (dùng chung cho CPU/RAM/GPU/Ổ cứng — cùng kiểu
// chip nhiều lựa chọn như Thương hiệu, khác Giá/Danh mục là chọn đơn).
const toggleSpec = (arr, value) => {
  const idx = arr.indexOf(value);
  if (idx === -1) arr.push(value);
  else arr.splice(idx, 1);
  emitChange();
};

// Các khoảng giá cố định (nhãn dịch theo ngôn ngữ hiện tại)
const priceRanges = computed(() => {
  const labels = t('productFilter.priceRanges');
  return [
    { label: labels[0], min: 0,          max: 10_000_000  },
    { label: labels[1], min: 10_000_000,  max: 20_000_000  },
    { label: labels[2], min: 20_000_000,  max: 30_000_000  },
    { label: labels[3], min: 30_000_000,  max: Infinity    },
  ];
});

// Có bộ lọc nào đang active không
const hasFilter = computed(
  () => selectedBrands.value.length > 0 || selectedPrice.value || selectedCategory.value
    || selectedCpu.value.length > 0 || selectedRam.value.length > 0
    || selectedGpu.value.length > 0 || selectedStorage.value.length > 0
);

// Bật/tắt một thương hiệu
const toggleBrand = (brand) => {
  const idx = selectedBrands.value.indexOf(brand);
  if (idx === -1) selectedBrands.value.push(brand);
  else selectedBrands.value.splice(idx, 1);
  emitChange();
};

// Chọn khoảng giá (chọn lại cái đang chọn thì bỏ)
const selectPrice = (range) => {
  if (selectedPrice.value === range.label) {
    selectedPrice.value = '';
    priceMin.value = null;
    priceMax.value = null;
  } else {
    selectedPrice.value = range.label;
    priceMin.value = range.min;
    priceMax.value = range.max;
  }
  emitChange();
};

// Chọn danh mục
const selectCategory = (id) => {
  selectedCategory.value = selectedCategory.value === id ? null : id;
  emitChange();
};

// Xóa toàn bộ bộ lọc
const clearAll = () => {
  selectedBrands.value   = [];
  selectedPrice.value    = '';
  selectedCategory.value = null;
  priceMin.value         = null;
  priceMax.value         = null;
  selectedCpu.value      = [];
  selectedRam.value      = [];
  selectedGpu.value      = [];
  selectedStorage.value  = [];
  emitChange();
};

// Gửi bộ lọc hiện tại lên component cha
const emitChange = () => {
  emit('change', {
    brands:   selectedBrands.value,
    priceMin: priceMin.value,
    priceMax: priceMax.value,
    category: selectedCategory.value,
    cpu:      selectedCpu.value,
    ram:      selectedRam.value,
    gpu:      selectedGpu.value,
    storage:  selectedStorage.value,
  });
};
</script>

<style scoped>
/* Skeleton chip — stable height matches real button to prevent layout shift */
.skeleton-chip {
  height: 28px;
  width: 80px;
  background: var(--bg-card-alt);
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}
@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}
</style>
