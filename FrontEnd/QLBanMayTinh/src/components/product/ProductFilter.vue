<template>
  <!-- Bộ lọc sản phẩm — dùng ở trang khách hàng, bên trái hoặc dạng chip ngang -->
  <div class="d-flex flex-column gap-3">

    <!-- ── Lọc theo Thương hiệu ── -->
    <div>
      <div class="fw-bold small text-secondary text-uppercase mb-2"
           style="letter-spacing:0.05em; font-size:0.72rem;">Thương hiệu</div>
      <div class="d-flex flex-wrap gap-2">
        <button v-for="b in brands" :key="b"
                class="btn btn-sm"
                :class="selectedBrands.includes(b) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                style="font-size:0.78rem; border-radius:20px;"
                @click="toggleBrand(b)">
          {{ b }}
        </button>
      </div>
    </div>

    <!-- ── Lọc theo Khoảng giá ── -->
    <div>
      <div class="fw-bold small text-secondary text-uppercase mb-2"
           style="letter-spacing:0.05em; font-size:0.72rem;">Khoảng giá</div>
      <div class="d-flex flex-wrap gap-2">
        <button v-for="range in priceRanges" :key="range.label"
                class="btn btn-sm"
                :class="selectedPrice === range.label ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                style="font-size:0.78rem; border-radius:20px;"
                @click="selectPrice(range)">
          {{ range.label }}
        </button>
      </div>
    </div>

    <!-- ── Lọc theo Danh mục ── -->
    <div v-if="categories.length">
      <div class="fw-bold small text-secondary text-uppercase mb-2"
           style="letter-spacing:0.05em; font-size:0.72rem;">Danh mục</div>
      <div class="d-flex flex-wrap gap-2">
        <button v-for="c in categories" :key="c.id"
                class="btn btn-sm"
                :class="selectedCategory === c.id ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                style="font-size:0.78rem; border-radius:20px;"
                @click="selectCategory(c.id)">
          {{ c.tenDanhMuc }}
        </button>
      </div>
    </div>

    <!-- ── Nút xóa bộ lọc ── -->
    <div v-if="hasFilter">
      <button class="btn btn-sm btn-outline-danger"
              style="font-size:0.78rem; border-radius:20px;"
              @click="clearAll">
        ✕ Xóa bộ lọc
      </button>
    </div>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  // Danh sách tên thương hiệu (string[])
  brands:     { type: Array, default: () => [] },
  // Danh sách danh mục từ API [{ id, tenDanhMuc }]
  categories: { type: Array, default: () => [] },
});

// Emit 'change' mỗi khi bộ lọc thay đổi — App.vue lắng nghe để filter danh sách sản phẩm
const emit = defineEmits(['change']);

// Trạng thái bộ lọc nội bộ
const selectedBrands   = ref([]);
const selectedPrice    = ref('');
const selectedCategory = ref(null);
const priceMin         = ref(null);
const priceMax         = ref(null);

// Các khoảng giá cố định
const priceRanges = [
  { label: 'Dưới 10 triệu',    min: 0,          max: 10_000_000  },
  { label: '10 - 20 triệu',    min: 10_000_000,  max: 20_000_000  },
  { label: '20 - 30 triệu',    min: 20_000_000,  max: 30_000_000  },
  { label: 'Trên 30 triệu',    min: 30_000_000,  max: Infinity    },
];

// Có bộ lọc nào đang active không
const hasFilter = computed(
  () => selectedBrands.value.length > 0 || selectedPrice.value || selectedCategory.value
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
  emitChange();
};

// Gửi bộ lọc hiện tại lên component cha
const emitChange = () => {
  emit('change', {
    brands:   selectedBrands.value,
    priceMin: priceMin.value,
    priceMax: priceMax.value,
    category: selectedCategory.value,
  });
};
</script>
