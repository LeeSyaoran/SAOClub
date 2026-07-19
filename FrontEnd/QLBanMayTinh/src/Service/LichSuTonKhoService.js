import { get } from './api.js';

// Lịch sử tồn kho — chỉ đọc, backend cố ý không có endpoint cập nhật (audit trail,
// chỉ ghi thêm/xóa ở phía backend cho các luồng nghiệp vụ khác, UI không tự tạo/xóa).
export const getAll = () => get('/api/lich-su-ton-kho');
