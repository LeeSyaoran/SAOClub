// Quản lý danh sách chiTietId trong giỏ hàng POS
import { ref, computed } from 'vue';
import { watch } from 'vue';

// Danh sách chiTietId đang có trong giỏ hàng
export const posCartChiTietIds = ref(new Set(
  JSON.parse(localStorage.getItem('posCart') || '[]')
    .map((item) => item.chiTietId)
    .filter(Boolean)
));

// Đồng bộ danh sách sản phẩm trong giỏ hàng POS
export function syncPosCart(items) {
  const ids = new Set(
    (items || [])
      .map((item) => item.chiTietId)
      .filter(Boolean)
  );
  posCartChiTietIds.value = ids;
  // Lưu giỏ hàng vào localStorage
  try {
    localStorage.setItem('posCart', JSON.stringify(items || []));
  } catch (_) {}
}
