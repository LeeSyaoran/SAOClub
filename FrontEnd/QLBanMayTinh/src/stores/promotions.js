import { reactive } from "vue";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";

export const PromotionsStore = reactive({ items: [], loading: false, loaded: false });

let promotionsPromise = null;

// Xem resetProducts() ở stores/products.js — cùng lý do reset khi đổi tài khoản cùng tab.
export const resetPromotions = () => {
  promotionsPromise = null;
  PromotionsStore.items = [];
  PromotionsStore.loaded = false;
};

export const ensurePromotions = () => {
  if (promotionsPromise) return promotionsPromise;
  promotionsPromise = refreshPromotions();
  return promotionsPromise;
};

export const refreshPromotions = async () => {
  PromotionsStore.loading = true;
  try {
    PromotionsStore.items = await KhuyenMaiService.getAll().catch(() => []);
    PromotionsStore.loaded = true;
  } finally {
    PromotionsStore.loading = false;
  }
  return PromotionsStore.items;
};
