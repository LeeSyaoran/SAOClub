import { reactive } from "vue";
import * as TonKhoService from "../Service/TonKhoService.js";

export const InventoryStore = reactive({ items: [], loading: false, loaded: false });

let inventoryPromise = null;

// Xem resetProducts() ở stores/products.js — cùng lý do reset khi đổi tài khoản cùng tab.
export const resetInventory = () => {
  inventoryPromise = null;
  InventoryStore.items = [];
  InventoryStore.loaded = false;
};

export const ensureInventory = () => {
  if (inventoryPromise) return inventoryPromise;
  inventoryPromise = refreshInventory();
  return inventoryPromise;
};

export const refreshInventory = async () => {
  InventoryStore.loading = true;
  try {
    InventoryStore.items = await TonKhoService.getAll().catch(() => []);
    InventoryStore.loaded = true;
  } finally {
    InventoryStore.loading = false;
  }
  return InventoryStore.items;
};
