import { reactive } from "vue";
import * as SanPhamService from "../Service/SanPhamService.js";

// ── Products Store — dữ liệu sản phẩm dùng chung nhiều trang (ProductsTable, PosPanel,
// OrdersTable). fetch-once-cache: gọi ensureProducts() nhiều lần chỉ tải 1 lần, refresh()
// dùng khi cần tải lại có chủ đích (sau khi thêm/sửa/xóa sản phẩm). ──
export const ProductsStore = reactive({ items: [], loading: false, loaded: false });

let productsPromise = null;
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
