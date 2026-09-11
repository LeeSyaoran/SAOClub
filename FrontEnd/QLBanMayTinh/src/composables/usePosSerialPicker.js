import { ref } from 'vue';
import * as ChiTietSanPhamService from '../services/ChiTietSanPhamService.js';
import { refreshProducts } from '../stores/products.js';
import { refreshInventory } from '../stores/inventory.js';
import { bumpSerialEvent } from '../stores/serialEvents.js';

export function usePosSerialPicker() {
  // POS session ID — persistent across reloads
  const posSessionId = ref(
    localStorage.getItem('pos_session_id') || (() => {
      const id = crypto.randomUUID();
      localStorage.setItem('pos_session_id', id);
      return id;
    })()
  );

  const selectedSerials = ref(new Set()); // chiTietId -> serial info
  const serialCache = ref({}); // bienTheId -> ChiTietSanPhamResponse[]

  // Load serials by bienTheId (with caching)
  const loadSerialsByBienThe = async (bienTheId) => {
    if (serialCache.value[bienTheId]) return serialCache.value[bienTheId];
    const serials = await ChiTietSanPhamService.getByBienThe(bienTheId).catch(() => []);
    serialCache.value[bienTheId] = serials;
    return serials;
  };

  // Get available serials (trong_kho only)
  const getAvailableSerials = async (bienTheId) => {
    const serials = await loadSerialsByBienThe(bienTheId);
    return serials.filter(s => s.trangThai === 'trong_kho');
  };

  // Toggle serial selection in picker
  const toggleSerial = (serial) => {
    const next = new Set(selectedSerials.value);
    if (next.has(serial.chiTietId)) {
      next.delete(serial.chiTietId);
    } else {
      next.add(serial.chiTietId);
    }
    selectedSerials.value = next;
    return selectedSerials.value.has(serial.chiTietId);
  };

  const isSerialSelected = (chiTietId) => selectedSerials.value.has(chiTietId);

  const clearSelectedSerials = () => {
    selectedSerials.value = new Set();
  };

  // Set serial status (giu_hang / trong_kho)
  const setSerialTrangThai = async (item, trangThai) => {
    await ChiTietSanPhamService.update(item.chiTietId, {
      bienTheId: item.bienTheId,
      soSerial: item.soSerial,
      trangThai,
      ngayNhapKho: item.ngayNhapKho,
    }).catch(() => {});
    // Refresh caches after status change
    refreshProducts();
    refreshInventory();
    bumpSerialEvent();
  };

  // Invalidate cache when needed (after status change)
  const invalidateCache = (bienTheId) => {
    if (bienTheId && serialCache.value[bienTheId]) {
      delete serialCache.value[bienTheId];
    }
  };

  return {
    posSessionId,
    selectedSerials,
    serialCache,
    loadSerialsByBienThe,
    getAvailableSerials,
    toggleSerial,
    isSerialSelected,
    clearSelectedSerials,
    setSerialTrangThai,
    invalidateCache,
  };
}
