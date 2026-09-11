import { ref, watch } from 'vue';

const STORAGE_KEY = 'saophone_pos_held_orders';

export function usePosHeldOrders() {
  const heldOrders = ref([]);

  // Load from localStorage on init
  const loadFromStorage = () => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      heldOrders.value = stored ? JSON.parse(stored) : [];
    } catch {
      heldOrders.value = [];
    }
  };

  loadFromStorage();

  // Sync when other tabs change localStorage
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
      if (e.key === STORAGE_KEY) {
        try {
          const raw = localStorage.getItem(STORAGE_KEY);
          heldOrders.value = raw ? JSON.parse(raw) : [];
        } catch { heldOrders.value = []; }
      }
    });
  }

  const saveToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(heldOrders.value));
    } catch { /* quota exceeded or serialization fail */ }
  };

  const saveOrder = (cart, customerId, staffId, extras = {}) => {
    const now = new Date();
    const order = {
      id: Date.now(),
      heldAt: now.toISOString(),
      cart: [...cart],
      phone: extras.phone || '',
      foundCust: extras.foundCust || null,
      promoCode: extras.promoCode || '',
      appliedPromo: extras.appliedPromo || null,
      paymentMethod: extras.paymentMethod || null,
    };
    heldOrders.value.unshift(order);
    saveToStorage();
    return order;
  };

  const loadOrder = (orderId) => {
    return heldOrders.value.find(o => o.id === orderId) || null;
  };

  const deleteOrder = (orderId) => {
    const held = heldOrders.value.find(o => o.id === orderId);
    heldOrders.value = heldOrders.value.filter(o => o.id !== orderId);
    saveToStorage();
    return held; // return for caller to release serials if needed
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
