import { reactive } from "vue";
import * as NhaCungCapService from "../Service/NhaCungCapService.js";

export const SuppliersStore = reactive({ items: [], loading: false, loaded: false });

let suppliersPromise = null;
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
