// Backend lưu các cột ngày giờ (ngayDat, ngayNhapKho, ngayTao...) dưới dạng
// LocalDateTime "trần" (không có timezone). new Date().toISOString() luôn quy
// đổi sang UTC, nên gần nửa đêm giờ VN (UTC+7) nó trả về NGÀY HÔM TRƯỚC — đơn
// hàng/phiếu nhập bị lưu nhầm sang ngày cũ dù tạo lúc "đã sang ngày mới".
// Dùng hàm này ở mọi nơi build timestamp "bây giờ" gửi lên backend.
export const nowLocalIso = (date = new Date()) =>
  new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 19);
