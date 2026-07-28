import { get, post } from './api.js';

// Tao record thanh toan cho 1 don — dung o POS ngay sau khi tao don + dong san pham
// thanh cong (xem PosPanel.vue posPlaceOrder).
export const create = (body) => post('/api/thanh-toan', body);

// Toan bo record thanh toan cua 1 don — dung o modal "Chi tiet don hang" (OrdersTable.vue).
export const getByDonHang = (donHangId) => get(`/api/thanh-toan/don-hang/${donHangId}`);
