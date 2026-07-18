import { reactive } from "vue";
import * as DmService from "../Service/DmService.js";

export const SuppliersStore = reactive({ items: [], loading: false, loaded: false });

let suppliersPromise = null;
export const ensureSuppliers = () => {
  if (suppliersPromise) return suppliersPromise;
  suppliersPromise = DmService.getNhaCungCap().catch(() => []).then((list) => {
    SuppliersStore.items = list;
    SuppliersStore.loaded = true;
  });
  return suppliersPromise;
};
