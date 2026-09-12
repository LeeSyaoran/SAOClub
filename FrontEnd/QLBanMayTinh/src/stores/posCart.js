/**
 * posCart store — shared between PosPanel (cart owner) and SerialManager (read-only viewer).
 * SerialManager reads `posCartChiTietIds` to highlight serials already in the cart.
 * PosPanel calls `syncPosCart()` after every cart mutation so the highlight stays current.
 */
import { ref, computed } from 'vue';
import { watch } from 'vue';

// --- posCartChiTietIds (Set of chiTietId in cart) ---
// Derived from localStorage so SerialManager can read it without importing PosPanel state.
export const posCartChiTietIds = ref(new Set(
  JSON.parse(localStorage.getItem('posCart') || '[]')
    .map((item) => item.chiTietId)
    .filter(Boolean)
));

// --- syncPosCart ---
// Call after any posCart change: `[...items].map(item => item.chiTietId)`
export function syncPosCart(items) {
  const ids = new Set(
    (items || [])
      .map((item) => item.chiTietId)
      .filter(Boolean)
  );
  posCartChiTietIds.value = ids;
  // Also persist for recovery
  try {
    localStorage.setItem('posCart', JSON.stringify(items || []));
  } catch (_) {}
}
