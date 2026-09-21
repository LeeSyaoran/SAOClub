import { reactive } from "vue";
import * as KhuyenMaiService from "../services/KhuyenMaiService.js";

export const PromotionsStore = reactive({ items: [], loading: false, loaded: false });

let promotionsPromise = null;

// Reset dữ liệu store khi đăng xuất hoặc đổi tài khoản
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
