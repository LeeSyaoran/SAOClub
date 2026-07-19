import { reactive } from "vue";
import * as PhieuTraHangService from "../Service/PhieuTraHangService.js";

export const ReturnsStore = reactive({ items: [], loading: false, loaded: false });

let returnsPromise = null;
export const ensureReturns = () => {
  if (returnsPromise) return returnsPromise;
  returnsPromise = refreshReturns();
  return returnsPromise;
};

export const refreshReturns = async () => {
  ReturnsStore.loading = true;
  try {
    ReturnsStore.items = await PhieuTraHangService.getAll().catch(() => []);
    ReturnsStore.loaded = true;
  } finally {
    ReturnsStore.loading = false;
  }
  return ReturnsStore.items;
};
