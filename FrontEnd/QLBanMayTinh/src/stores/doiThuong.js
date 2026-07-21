import { reactive } from "vue";
import * as DmDoiThuongService from "../Service/DmDoiThuongService.js";

export const DoiThuongStore = reactive({ items: [], loading: false, loaded: false });

let doiThuongPromise = null;
export const ensureDoiThuong = () => {
  if (doiThuongPromise) return doiThuongPromise;
  doiThuongPromise = refreshDoiThuong();
  return doiThuongPromise;
};

export const refreshDoiThuong = async () => {
  DoiThuongStore.loading = true;
  try {
    DoiThuongStore.items = await DmDoiThuongService.getAll().catch(() => []);
    DoiThuongStore.loaded = true;
  } finally {
    DoiThuongStore.loading = false;
  }
  return DoiThuongStore.items;
};
