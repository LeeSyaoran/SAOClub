import { reactive } from "vue";

// ── Confirm Store — thay window.confirm() bằng dialog cùng theme app ────────
// Dùng: if (!(await askConfirm(t('admin.confirm.deleteProduct')))) return;
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
