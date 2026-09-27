// Quản lý giỏ hàng POS (danh sách sản phẩm, serial, người thực hiện)
import { ref, computed } from 'vue';

const readSavedCart = () => {
  try {
    return JSON.parse(localStorage.getItem('posCart') || '[]');
  } catch {
    return [];
  }
};

// Danh sách đầy đủ các item trong giỏ hàng POS
export const posCartItems = ref(readSavedCart());

// Set các chiTietId đang có trong giỏ POS
export const posCartChiTietIds = computed(() =>
  new Set(posCartItems.value.map((item) => item.chiTietId).filter(Boolean))
);

// Map đếm số lượng serial trong giỏ POS theo bienTheId (loại trừ serial lỗi/bảo hành)
export const posCartCountsByBienThe = computed(() => {
  const map = {};
  posCartItems.value.forEach((item) => {
    if (item.trangThai === 'loi_bao_hanh') return;
    if (item.bienTheId != null) {
      map[item.bienTheId] = (map[item.bienTheId] || 0) + (item.soLuong ?? 1);
    }
  });
  return map;
});

// Lấy thông tin item trong giỏ POS theo chiTietId (kèm thông tin performerRole, performerName)
export const getPosCartItem = (chiTietId) => {
  return posCartItems.value.find((item) => item.chiTietId === chiTietId) || null;
};

// Đồng bộ giỏ hàng POS
export function syncPosCart(items) {
  posCartItems.value = items || [];
  try {
    localStorage.setItem('posCart', JSON.stringify(items || []));
  } catch (_) {}
}

// Lắng nghe thay đổi storage từ các tab khác
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key === 'posCart') {
      try {
        posCartItems.value = JSON.parse(e.newValue || '[]');
      } catch (_) {}
    }
  });
}
