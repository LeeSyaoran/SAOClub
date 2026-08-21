import { defineStore } from "pinia";
import { pinia } from "./pinia.js";

let toastTimer = null;

export const useToastStore = defineStore("toast", {
  state: () => ({
    show: false,
    msg: "",
    type: "success",
  }),
  actions: {
    showToast(msg, type = "success") {
      clearTimeout(toastTimer);
      this.msg = msg;
      this.type = type;
      this.show = true;
      toastTimer = setTimeout(() => { this.show = false; }, type === "error" ? 6000 : 3500);
    },
  },
});

export const ToastState = useToastStore(pinia);
export const showToast = (msg, type) => ToastState.showToast(msg, type);
