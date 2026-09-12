// stores/warranty.js — Quản lý logic quyết định cho BH
// Đồng bộ với backend (ChiTietSanPham.canEdit, etc.)

/**
 * Có thể admin/staff sửa trực tiếp thông tin BH (ngày mua, ngày hết BH, ghi chú) khi:
 *  - Sản phẩm chưa có phiếu BH nào đang ở trạng thái chờ_xử_ly hoặc đang_xử_ly
 * Khi đã có phiếu đang xử lý, mọi thay đổi phải đi qua phiếu BH để audit.
 */
export const canEditWarranty = (item) => {
  if (!item) return false;
  // item.coPhieuDangXuLy được backend tính sẵn (optional)
  if (item.coPhieuDangXuLy === true) return false;
  return true;
};

/**
 * Trạng thái BH lấy từ ngayHetBh:
 *   còn_bh        — còn hạn (> 30 ngày)
 *   sap_het_bh    — sắp hết (0..30 ngày)
 *   het_bh        — đã hết
 */
export const computeWarrantyStatus = (ngayHetBh) => {
  if (!ngayHetBh) return 'khong_xac_dinh';
  const days = Math.ceil((new Date(ngayHetBh) - new Date()) / 86400000);
  if (days <= 0) return 'het_bh';
  if (days <= 30) return 'sap_het_bh';
  return 'con_bh';
};
