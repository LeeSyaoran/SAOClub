import { reactive } from "vue";
import * as KhuyenMaiService from "../Service/KhuyenMaiService.js";

export const PromotionsStore = reactive({ items: [], loading: false, loaded: false });

let promotionsPromise = null;
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
