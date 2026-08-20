import { ref, onMounted, onBeforeUnmount } from "vue";

// Thanh cong cu nam trong component con, nhung vung cuon thuc su la div cha (vd khung
// noi dung admin ".overflow-y-auto") — phai tim ngược len ancestor co overflow-y
// auto/scroll gan nhat thay vi gan thang vao window.
function findScrollParent(el) {
  let node = el?.parentElement;
  while (node && node !== document.body) {
    const { overflowY } = getComputedStyle(node);
    if (overflowY === "auto" || overflowY === "scroll") return node;
    node = node.parentElement;
  }
  return window;
}

/**
 * Hieu ung "thanh cong cu tu an khi cuon xuong, hien lai khi cuon len" — khoa o
 * top:0 (position: sticky ap o noi goi), chi phan ben duoi duoc cuon troi qua.
 * targetRef: ref tro toi phan tu sticky (dung de tim đúng ancestor cuon).
 */
export function useAutoHideOnScroll(targetRef, { threshold = 8 } = {}) {
  const hidden = ref(false);
  let scrollEl = null;
  // anchorY chi doi khi THUC SU doi trang thai (an/hien) — KHONG doi o moi tick.
  // Cuon muot/inertia ban ra rat nhieu event voi delta tung tick chi vai px; neu lay
  // lastY theo tung tick (nhu ban truoc) thi delta gan nhu khong bao gio vuot threshold,
  // menu chi tro lung lung nua vien roi dung lai giua chung thay vi troi han di. Dùng
  // mot moc co dinh (anchorY) tinh tong khoang da cuon ke tu lan doi trang thai gan nhat
  // thi menu luon troi/tro het mot mach.
  let anchorY = 0;

  const getY = () => (scrollEl === window ? window.scrollY : scrollEl.scrollTop);

  const onScroll = () => {
    const y = getY();
    if (y <= 4) {
      hidden.value = false;
      anchorY = y;
      return;
    }
    const delta = y - anchorY;
    if (delta > threshold) {
      hidden.value = true;
      anchorY = y;
    } else if (delta < -threshold) {
      hidden.value = false;
      anchorY = y;
    }
  };

  onMounted(() => {
    scrollEl = findScrollParent(targetRef.value);
    anchorY = getY();
    scrollEl.addEventListener("scroll", onScroll, { passive: true });
  });
  onBeforeUnmount(() => {
    scrollEl?.removeEventListener("scroll", onScroll);
  });

  return { hidden };
}
