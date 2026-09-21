import { reactive } from "vue";

// Quản lý trạng thái hộp thoại xác nhận dùng chung
export const ConfirmState = reactive({ show: false, message: "" });

let resolver = null;

export const askConfirm = (message) => {
  ConfirmState.message = message;
  ConfirmState.show = true;
  return new Promise((resolve) => { resolver = resolve; });
};

export const resolveConfirm = (result) => {
  ConfirmState.show = false;
  resolver?.(result);
  resolver = null;
};
