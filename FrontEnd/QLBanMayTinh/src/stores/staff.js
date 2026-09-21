import { reactive } from "vue";
import * as NhanVienService from "../services/NhanVienService.js";

export const StaffStore = reactive({ items: [], loading: false, loaded: false });

let staffPromise = null;

// Reset danh sách nhân viên khi đăng xuất hoặc đổi tài khoản
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

// Tải lại danh sách nhân viên từ backend
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
