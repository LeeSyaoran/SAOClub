// Quản lý quy tắc nghiệp vụ bảo hành

// Kiểm tra quyền chỉnh sửa thông tin bảo hành
export const canEditWarranty = (item) => {
  if (!item) return false;
  // Không cho sửa nếu đang có phiếu xử lý
  if (item.coPhieuDangXuLy === true) return false;
  return true;
};

// Tính trạng thái bảo hành dựa trên ngày hết hạn
export const computeWarrantyStatus = (ngayHetBh) => {
  if (!ngayHetBh) return 'khong_xac_dinh';
  const days = Math.ceil((new Date(ngayHetBh) - new Date()) / 86400000);
  if (days <= 0) return 'het_bh';
  if (days <= 30) return 'sap_het_bh';
  return 'con_bh';
};
