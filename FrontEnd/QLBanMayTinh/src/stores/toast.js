import { reactive } from "vue";

// ── Toast Store — thay window.alert() trên các trang staff (Admin/Staff/WarehouseManagement) ──
// Trước đây là state+hàm cục bộ trong AdminPage.vue — promote lên đây theo đúng pattern
// stores/confirm.js đã có, để dùng lại được ở StaffPage/WarehouseManagementPage.
export const ToastState = reactive({ show: false, msg: "", type: "success" });

let toastTimer = null;

export const showToast = (msg, type = "error") => {
  clearTimeout(toastTimer);
  ToastState.msg = msg;
  ToastState.type = type;
  ToastState.show = true;
  // Lỗi (đặc biệt lý do chặn xóa) thường dài hơn — cho thêm thời gian đọc so với thông báo
  // thành công ngắn gọn.
  toastTimer = setTimeout(() => { ToastState.show = false; }, type === "error" ? 6000 : 3500);
};
