<template>
  <div class="qlhh">
    <nav class="qlhh-nav">
      <button
        v-for="m in mucs"
        :key="m.key"
        class="qlhh-nav__item"
        :class="{ 'is-on': muc === m.key }"
        @click="muc = m.key"
      >
        <i class="fa" :class="m.icon"></i>
        {{ m.label }}
      </button>
    </nav>

    <!-- KeepAlive giữ nguyên bộ lọc, trang đang xem và dữ liệu đã tải của từng mục
         khi người dùng chuyển qua lại, khỏi phải gọi API lại từ đầu mỗi lần bấm. -->
    <KeepAlive>
      <SanPhamView v-if="muc === 'sanpham'" />
      <KhoHangView v-else />
    </KeepAlive>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// Đổi đường dẫn cho khớp cấu trúc thư mục của bạn nếu hai file nằm chỗ khác.
import SanPhamView from './HangHoa.vue'
import KhoHangView from './KhoHang.vue'

const mucs = [
  { key: 'sanpham', label: 'Sản phẩm', icon: 'fa-cube' },
  { key: 'kho', label: 'Kho hàng', icon: 'fa-archive' }
]

const muc = ref('sanpham')
</script>

<style scoped>
.qlhh {
  --pink-50: #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-600: #db2777;
  --pink-700: #a81b5d;
  --line: #f1dbe6;
  --muted: #6b7280;
}

.qlhh-nav {
  display: flex;
  gap: 6px;
  padding: 6px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 999px;
  width: fit-content;
  max-width: 100%;
  overflow-x: auto;
}

.qlhh-nav__item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color .15s, color .15s;
}

.qlhh-nav__item:hover { background: var(--pink-50); color: var(--pink-700); }
.qlhh-nav__item.is-on { background: var(--pink-600); color: #fff; }
.qlhh-nav__item:focus-visible { outline: 2px solid var(--pink-600); outline-offset: 2px; }
</style>