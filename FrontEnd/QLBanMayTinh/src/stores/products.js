import { reactive } from "vue";
import * as SanPhamService from "../services/SanPhamService.js";

// ── Products Store — dữ liệu sản phẩm dùng chung nhiều trang (ProductsTable, PosPanel,
// OrdersTable). fetch-once-cache: gọi ensureProducts() nhiều lần chỉ tải 1 lần, refresh()
// dùng khi cần tải lại có chủ đích (sau khi thêm/sửa/xóa sản phẩm). ──
export const ProductsStore = reactive({ items: [], loading: false, loaded: false });

let productsPromise = null;

// Đăng xuất/đổi tài khoản cùng tab không reload trang — promise nhớ nhớ (ensureX) sẽ giữ mãi
// dữ liệu của phiên cũ nếu không reset, tài khoản mới vào thấy dữ liệu cũ/lệch cho tới khi
// tự bấm refresh. Gọi từ resetAllStores() (xem stores/resetAll.js) mỗi lần clearSession().
export const resetProducts = () => {
  productsPromise = null;
  ProductsStore.items = [];
  ProductsStore.loaded = false;
};

export const ensureProducts = () => {
  if (productsPromise) return productsPromise;
  productsPromise = refreshProducts();
  return productsPromise;
};

export const refreshProducts = async () => {
  ProductsStore.loading = true;
  try {
    ProductsStore.items = await SanPhamService.getAll().catch(() => []);
    ProductsStore.loaded = true;
  } finally {
    ProductsStore.loading = false;
  }
  return ProductsStore.items;
};
