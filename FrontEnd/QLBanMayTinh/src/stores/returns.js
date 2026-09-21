import { reactive } from "vue";
import * as PhieuTraHangService from "../services/PhieuTraHangService.js";

export const ReturnsStore = reactive({ items: [], loading: false, loaded: false });

let returnsPromise = null;

// Reset dữ liệu store khi đăng xuất hoặc đổi tài khoản
export const resetReturns = () => {
  returnsPromise = null;
  ReturnsStore.items = [];
  ReturnsStore.loaded = false;
};

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
