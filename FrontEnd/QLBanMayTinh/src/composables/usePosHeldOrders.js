// usePosHeldOrders — hold / resume / delete pending POS orders
import { ref } from "vue";

const STORAGE_KEY = "saophone_pos_held_orders";

export function usePosHeldOrders() {
  const heldOrders = ref([]);

  // Load from localStorage on init
  const loadFromStorage = () => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      heldOrders.value = raw ? JSON.parse(raw) : [];
    } catch {
      heldOrders.value = [];
    }
  };
  loadFromStorage();

  // Sync when other tabs change localStorage
  if (typeof window !== "undefined") {
    window.addEventListener("storage", (e) => {
      if (e.key === STORAGE_KEY) loadFromStorage();
    });
  }

  const saveToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(heldOrders.value));
    } catch { /* quota exceeded */ }
  };

  // cart + extras object (phone, foundCust, promoCode, appliedPromo, paymentMethod)
  const saveOrder = (cart, extras = {}) => {
    const order = {
      id: Date.now(),
      heldAt: new Date().toISOString(),
      cart: [...cart],
      phone: extras.phone || "",
      foundCust: extras.foundCust || null,
      promoCode: extras.promoCode || "",
      appliedPromo: extras.appliedPromo || null,
      paymentMethod: extras.paymentMethod || null,
    };
    heldOrders.value.unshift(order);
    saveToStorage();
    return order;
  };

  const loadOrder = (orderId) =>
    heldOrders.value.find((o) => o.id === orderId) || null;

  const deleteOrder = (orderId) => {
    const held = heldOrders.value.find((o) => o.id === orderId);
    heldOrders.value = heldOrders.value.filter((o) => o.id !== orderId);
    saveToStorage();
    return held; // caller releases serials if needed
  };

  const clearAllHeld = () => {
    heldOrders.value = [];
    localStorage.removeItem(STORAGE_KEY);
  };

  return {
    heldOrders,
    saveOrder,
    loadOrder,
    deleteOrder,
    clearAllHeld,
  };
}
