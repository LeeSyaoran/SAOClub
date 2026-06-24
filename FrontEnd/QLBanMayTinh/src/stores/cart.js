import { reactive, computed } from "vue";

// ── Cart Store ─────────────────────────────────────────────────────────────────
// Lưu giỏ hàng vào localStorage để không mất khi F5 trang

const STORAGE_KEY = "saophone_cart";

// Đọc giỏ hàng đã lưu từ localStorage khi app khởi động
const savedCart = (() => {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? []; }
  catch { return []; }
})();

// Hàm ghi localStorage — gọi sau mỗi thao tác thay đổi giỏ
const persist = (items) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
};

// State giỏ hàng — mảng các item: { bienTheId, tenSanPham, maSku, giaBan, soLuong, hinhAnhChinh }
export const cartStore = reactive({
  items: savedCart,
  // Trạng thái mở/đóng panel giỏ hàng
  isOpen: false,
});

// ── Computed helpers ──────────────────────────────────────────────────────────

// Tổng số lượng tất cả sản phẩm trong giỏ
export const cartCount = computed(() =>
  cartStore.items.reduce((sum, i) => sum + i.soLuong, 0)
);

// Tổng tiền hàng (chưa bao gồm phí ship)
export const cartSubTotal = computed(() =>
  cartStore.items.reduce((sum, i) => sum + i.giaBan * i.soLuong, 0)
);

// Phí vận chuyển: miễn phí nếu đơn từ 300.000đ
export const shippingFee = computed(() =>
  cartSubTotal.value >= 300_000 ? 0 : 30_000
);

// Tổng thanh toán = hàng + ship
export const cartTotal = computed(() =>
  cartSubTotal.value + shippingFee.value
);

// ── Actions ───────────────────────────────────────────────────────────────────

// Thêm sản phẩm vào giỏ — nếu đã có thì tăng số lượng
export const addToCart = (product) => {
  const existing = cartStore.items.find((i) => i.bienTheId === product.bienTheId);
  if (existing) {
    existing.soLuong++;
  } else {
    cartStore.items.push({
      bienTheId:     product.bienTheId,
      tenSanPham:    product.tenSanPham,
      maSku:         product.maSku ?? "",
      giaBan:        product.giaBan,
      soLuong:       1,
      hinhAnhChinh:  product.hinhAnhChinh ?? null,
    });
  }
  persist(cartStore.items);
};

// Tăng số lượng 1 sản phẩm
export const increaseQty = (bienTheId) => {
  const item = cartStore.items.find((i) => i.bienTheId === bienTheId);
  if (item) { item.soLuong++; persist(cartStore.items); }
};

// Giảm số lượng — nếu về 0 thì xóa luôn
export const decreaseQty = (bienTheId) => {
  const idx = cartStore.items.findIndex((i) => i.bienTheId === bienTheId);
  if (idx === -1) return;
  if (cartStore.items[idx].soLuong > 1) {
    cartStore.items[idx].soLuong--;
  } else {
    cartStore.items.splice(idx, 1);
  }
  persist(cartStore.items);
};

// Xóa hẳn 1 sản phẩm khỏi giỏ
export const removeFromCart = (bienTheId) => {
  const idx = cartStore.items.findIndex((i) => i.bienTheId === bienTheId);
  if (idx !== -1) { cartStore.items.splice(idx, 1); persist(cartStore.items); }
};

// Xóa toàn bộ giỏ hàng (sau khi đặt hàng thành công)
export const clearCart = () => {
  cartStore.items.splice(0);
  localStorage.removeItem(STORAGE_KEY);
};

// Bật/tắt panel giỏ hàng
export const toggleCart = () => { cartStore.isOpen = !cartStore.isOpen; };
export const openCart   = () => { cartStore.isOpen = true; };
export const closeCart  = () => { cartStore.isOpen = false; };
