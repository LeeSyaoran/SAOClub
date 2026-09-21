import { ref, onMounted, onBeforeUnmount } from "vue";

// Tìm phần tử cha có thanh cuộn gần nhất
function findScrollParent(el) {
  let node = el?.parentElement;
  while (node && node !== document.body) {
    const { overflowY } = getComputedStyle(node);
    if (overflowY === "auto" || overflowY === "scroll") return node;
    node = node.parentElement;
  }
  return window;
}

// Tự động ẩn thanh công cụ khi cuộn xuống và hiện khi cuộn lên
export function useAutoHideOnScroll(targetRef, { threshold = 8 } = {}) {
  const hidden = ref(false);
  let scrollEl = null;
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
