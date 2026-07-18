import { reactive } from "vue";
import * as KhachHangService from "../Service/KhachHangService.js";

export const CustomersStore = reactive({ items: [], loading: false, loaded: false });

let customersPromise = null;
export const ensureCustomers = () => {
  if (customersPromise) return customersPromise;
  customersPromise = refreshCustomers();
  return customersPromise;
};

export const refreshCustomers = async () => {
  CustomersStore.loading = true;
  try {
    CustomersStore.items = await KhachHangService.getAll().catch(() => []);
    CustomersStore.loaded = true;
  } finally {
    CustomersStore.loading = false;
  }
  return CustomersStore.items;
};
