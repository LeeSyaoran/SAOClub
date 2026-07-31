import { reactive } from "vue";
import * as NhanVienService from "../services/NhanVienService.js";

export const StaffStore = reactive({ items: [], loading: false, loaded: false });

let staffPromise = null;

// Xem resetProducts() ở stores/products.js — cùng lý do reset khi đổi tài khoản cùng tab.
export const resetStaff = () => {
  staffPromise = null;
  StaffStore.items = [];
  StaffStore.loaded = false;
};

export const ensureStaff = () => {
  if (staffPromise) return staffPromise;
  staffPromise = NhanVienService.getAll().catch(() => []).then((list) => {
    StaffStore.items = list;
    StaffStore.loaded = true;
  });
  return staffPromise;
};

// AdminPage.vue cần refetch có chủ đích sau khi thêm/xóa nhân viên (ensureStaff() chỉ tải
// 1 lần rồi cache promise, không refetch lại) — thêm refreshStaff() theo đúng pattern
// refreshXxx() của các store khác.
export const refreshStaff = async () => {
  StaffStore.loading = true;
  try {
    StaffStore.items = await NhanVienService.getAll().catch(() => []);
    StaffStore.loaded = true;
  } finally {
    StaffStore.loading = false;
  }
  return StaffStore.items;
};
