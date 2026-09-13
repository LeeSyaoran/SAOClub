// warrantyConstants.js — Hằng số trạng thái phiếu bảo hành + helpers
// Đồng bộ với backend (PhieuBaoHanh.trangThai).
//
// Luồng nghiệp vụ:
//   cho_xu_ly    — Khách gửi yêu cầu, admin CHƯA tiếp nhận
//   dang_xu_ly   — Admin tiếp nhận, đang sửa / theo dõi
//   da_xu_ly     — Hoàn thành, đã trả khách
//   tu_choi      — Admin từ chối (ngoài hạn / không đủ điều kiện / không phải lỗi BH)
//   da_huy       — Khách tự hủy yêu cầu (chỉ khi còn ở cho_xu_ly)

export const WARRANTY_STATUS = {
  CHO_XU_LY:  'cho_xu_ly',
  DANG_XU_LY: 'dang_xu_ly',
  DA_XU_LY:   'da_xu_ly',
  TU_CHOI:    'tu_choi',
  DA_HUY:     'da_huy',
};

// Thứ tự timeline
export const STATUS_FLOW = ['cho_xu_ly', 'dang_xu_ly', 'da_xu_ly'];

// Màu sắc cho từng trạng thái (đồng bộ theme hồng admin)
export const STATUS_COLOR = {
  cho_xu_ly:  { bg: 'rgba(250, 204, 21, 0.15)',  text: '#b45309', dot: '#f59e0b' },
  dang_xu_ly: { bg: 'rgba(59, 130, 246, 0.15)',  text: '#1d4ed8', dot: '#3b82f6' },
  da_xu_ly:   { bg: 'rgba(34, 197, 94, 0.15)',   text: '#15803d', dot: '#22c55e' },
  tu_choi:    { bg: 'rgba(239, 68, 68, 0.15)',   text: '#b91c1c', dot: '#ef4444' },
  da_huy:     { bg: 'rgba(107, 114, 128, 0.15)', text: '#374151', dot: '#6b7280' },
};

// Label ngắn gọn (i18n key dùng cho AccountPage)
export const STATUS_I18N_KEY = {
  cho_xu_ly:  'account.warranty.statusPending',
  dang_xu_ly: 'account.warranty.statusProcessing',
  da_xu_ly:   'account.warranty.statusDone',
  tu_choi:    'account.warranty.statusRejected',
  da_huy:     'account.warranty.statusCancelled',
};

// Cho phép khách hàng hủy phiếu của mình?
export const canKhachHangCancel = (trangThai) => trangThai === 'cho_xu_ly';

// Cho phép admin tiếp nhận (cho_xu_ly → dang_xu_ly)?
export const canAdminReceive = (trangThai) => trangThai === 'cho_xu_ly';

// Trạng thái "đang hoạt động" (chưa kết thúc)
export const isActive = (trangThai) =>
  ['cho_xu_ly', 'dang_xu_ly'].includes(trangThai);

// Tính số ngày còn bảo hành
export const daysUntilWarrantyExpiry = (ngayHetBaoHanh) => {
  if (!ngayHetBaoHanh) return 0;
  return Math.ceil((new Date(ngayHetBaoHanh) - new Date()) / 86400000);
};

// Đã hết hạn?
export const isExpired = (ngayHetBaoHanh) => daysUntilWarrantyExpiry(ngayHetBaoHanh) < 0;

// Loại file upload được phép
export const ACCEPTED_UPLOAD = 'image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,video/quicktime';
export const MAX_UPLOAD_SIZE = 20 * 1024 * 1024; // 20 MB
