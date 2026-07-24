import { reactive } from "vue";
import * as PhieuBaoHanhService from "../Service/PhieuBaoHanhService.js";

export const BaoHanhStore = reactive({ items: [], loading: false, loaded: false });

let baoHanhPromise = null;

// Xem resetProducts() ở stores/products.js — cùng lý do reset khi đổi tài khoản cùng tab.
export const resetBaoHanh = () => {
  baoHanhPromise = null;
  BaoHanhStore.items = [];
  BaoHanhStore.loaded = false;
};

export const ensureBaoHanh = () => {
  if (baoHanhPromise) return baoHanhPromise;
  baoHanhPromise = refreshBaoHanh();
  return baoHanhPromise;
};

export const refreshBaoHanh = async () => {
  BaoHanhStore.loading = true;
  try {
    BaoHanhStore.items = await PhieuBaoHanhService.getAll().catch(() => []);
    BaoHanhStore.loaded = true;
  } finally {
    BaoHanhStore.loading = false;
  }
  return BaoHanhStore.items;
};
