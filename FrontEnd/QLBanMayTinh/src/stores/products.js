import { reactive } from "vue";
import * as SanPhamService from "../services/SanPhamService.js";

// Store dữ liệu sản phẩm dùng chung
export const ProductsStore = reactive({ items: [], loading: false, loaded: false });

let productsPromise = null;

// Reset dữ liệu sản phẩm khi đăng xuất hoặc đổi tài khoản
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
