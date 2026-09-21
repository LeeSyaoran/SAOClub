import { ref, computed, watch } from "vue";

// Phân trang dữ liệu danh sách phía client
export function usePagination(itemsRef, pageSize = 100) {
  const currentPage = ref(0);
  const totalPages = computed(() => Math.max(1, Math.ceil(itemsRef.value.length / pageSize)));
  const pagedItems = computed(() => {
    const start = currentPage.value * pageSize;
    return itemsRef.value.slice(start, start + pageSize);
  });
  // Đặt lại về trang đầu tiên khi danh sách thay đổi
  watch(itemsRef, () => { currentPage.value = 0; });
  return { currentPage, totalPages, pagedItems, pageSize };
}
