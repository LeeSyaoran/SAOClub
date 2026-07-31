import { reactive } from "vue";
import * as NhaCungCapService from "../services/NhaCungCapService.js";

export const SuppliersStore = reactive({ items: [], loading: false, loaded: false });

let suppliersPromise = null;

// Xem resetProducts() ở stores/products.js — cùng lý do reset khi đổi tài khoản cùng tab.
export const resetSuppliers = () => {
  suppliersPromise = null;
  SuppliersStore.items = [];
  SuppliersStore.loaded = false;
};

export const ensureSuppliers = () => {
  if (suppliersPromise) return suppliersPromise;
  suppliersPromise = refreshSuppliers();
  return suppliersPromise;
};

export const refreshSuppliers = async () => {
  SuppliersStore.loading = true;
  try {
    SuppliersStore.items = await NhaCungCapService.getAll().catch(() => []);
    SuppliersStore.loaded = true;
  } finally {
    SuppliersStore.loading = false;
  }
  return SuppliersStore.items;
};
