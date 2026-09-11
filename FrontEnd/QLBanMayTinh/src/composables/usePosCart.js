import { ref, computed } from 'vue';

export function usePosCart() {
  const cart = ref([]);

  const cartTotal = computed(() =>
    cart.value.reduce((sum, item) => sum + (item.giaBan * item.soLuong), 0)
  );

  const addToCart = (item) => {
    const existing = cart.value.find(i => i.bienTheId === item.bienTheId);
    if (existing) {
      existing.soLuong += 1;
    } else {
      cart.value.push({ ...item, soLuong: 1 });
    }
  };

  const removeFromCart = (bienTheId) => {
    const idx = cart.value.findIndex(i => i.bienTheId === bienTheId);
    if (idx !== -1) cart.value.splice(idx, 1);
  };

  const updateQty = (bienTheId, qty) => {
    const item = cart.value.find(i => i.bienTheId === bienTheId);
    if (item) {
      if (qty <= 0) removeFromCart(bienTheId);
      else item.soLuong = qty;
    }
  };

  const clearCart = () => {
    cart.value = [];
  };

  const getCartItems = () => cart.value;

  return {
    cart,
    cartTotal,
    addToCart,
    removeFromCart,
    updateQty,
    clearCart,
    getCartItems,
  };
}
