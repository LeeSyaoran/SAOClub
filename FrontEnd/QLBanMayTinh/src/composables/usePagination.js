import { ref, computed, watch } from "vue";

// Phan trang phia client — cac bang admin deu tai het du lieu 1 lan (getAll(), phan lon
// endpoint chua ho tro Pageable o backend) roi tim kiem/loc tren mang co san, nen phan
// trang o day chi la cat lat mang da co san trong bo nho, khong goi lai API.
// currentPage 0-based — khop dung API cua components/common/Pagination.vue (component
// da co san, dung chung). itemsRef: computed/ref cua mang DA loc/tim kiem (vd filteredItems).
// pageSize mac dinh 100, theo dung yeu cau khong hien qua 100 dong/trang.
export function usePagination(itemsRef, pageSize = 100) {
  const currentPage = ref(0);
  const totalPages = computed(() => Math.max(1, Math.ceil(itemsRef.value.length / pageSize)));
  const pagedItems = computed(() => {
    const start = currentPage.value * pageSize;
    return itemsRef.value.slice(start, start + pageSize);
  });
  // Danh sach nguon doi (go tim kiem, doi bo loc...) luon ve lai trang dau — tranh dung o
  // trang giua chung ket qua moi, de gay hieu lam da het ket qua.
  watch(itemsRef, () => { currentPage.value = 0; });
  return { currentPage, totalPages, pagedItems, pageSize };
}
