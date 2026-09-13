// PhieuBaoHanhService.js — Mở rộng cho nghiệp vụ bảo hành khách hàng + admin
import { get, post, put, del, authHeaders } from './api.js';

export const fallbackClaims = [
  {
    baoHanhId: 101,
    donHangId: 7001,
    khachHangId: 5,
    maSku: 'SP0006-A515-58',
    soSerial: 'SN-2026-001',
    tenSanPham: 'Acer Aspire 5 A515-58',
    ngayMua: '2026-03-01T09:00:00',
    ngayTiepNhan: '2026-03-01T09:00:00',
    trangThai: 'cho_xu_ly',
    phuongThuc: 'giao_tan_noi',
    moTaLoi: 'Máy chạy nhưng màn hình bị xám và nháy sáng',
    chiPhiPhatSinh: 0,
    ketQuaXuLy: '',
    bienTheId: 11,
    tenKhachHang: 'Nguyễn Văn A',
    diaChiLayHang: '22 Trần Hưng Đạo, Hà Nội',
  },
  {
    baoHanhId: 102,
    donHangId: 7002,
    khachHangId: 8,
    maSku: 'SP0007-ASUS-ROG',
    soSerial: 'SN-2026-002',
    tenSanPham: 'Asus ROG Strix G16',
    ngayMua: '2026-04-02T13:00:00',
    ngayTiepNhan: '2026-04-02T13:00:00',
    trangThai: 'dang_xu_ly',
    phuongThuc: 'tai_shop',
    moTaLoi: 'Pin sạc không giữ được pin đúng cấu hình',
    chiPhiPhatSinh: 0,
    ketQuaXuLy: 'Đang kiểm tra pin và mainboard',
    bienTheId: 21,
    tenKhachHang: 'Trần Thị B',
    diaChiLayHang: '94 Hoàng Quốc Việt, Hà Nội',
  },
  {
    baoHanhId: 103,
    donHangId: 7003,
    khachHangId: 9,
    maSku: 'SP0008-LENOVO-LO',
    soSerial: 'SN-2026-003',
    tenSanPham: 'Lenovo Legion 5 Pro 16',
    ngayMua: '2025-09-01T08:00:00',
    ngayTiepNhan: '2025-09-02T08:00:00',
    trangThai: 'da_xu_ly',
    phuongThuc: 'giao_tan_noi',
    moTaLoi: 'Bàn phím click thiếu âm thanh',
    chiPhiPhatSinh: 250000,
    ketQuaXuLy: 'Đã thay bàn phím mới',
    bienTheId: 31,
    tenKhachHang: 'Lê Hoàng C',
    diaChiLayHang: '88 Bà Triệu, Hà Nội',
  },
  {
    baoHanhId: 104,
    donHangId: 7004,
    khachHangId: 10,
    maSku: 'SP0009-HP-PAV',
    soSerial: 'SN-2026-004',
    tenSanPham: 'HP Pavilion 15',
    ngayMua: '2026-05-10T10:20:00',
    ngayTiepNhan: '2026-05-11T10:20:00',
    trangThai: 'tu_choi',
    phuongThuc: 'tai_shop',
    moTaLoi: 'Máy nóng máy một thời gian',
    chiPhiPhatSinh: 0,
    ketQuaXuLy: '',
    bienTheId: 41,
    tenKhachHang: 'Phạm Văn D',
    diaChiLayHang: '19 Lê Duẩn, Đà Nẵng',
  },
];

// ── Helpers ───────────────────────────────────────────────────────────────────
const toQuery = (params = {}) =>
  Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');

// ── CRUD cơ bản (giữ tương thích) ─────────────────────────────────────────────
export const getPage = async ({ page = 0, size = 50 } = {}) => {
  try {
    const pageData = await get(`/api/phieu-bao-hanh?page=${page}&size=${size}`);
    return pageData?.content ? pageData : { content: fallbackClaims };
  } catch {
    const start = page * size;
    const clip = fallbackClaims.slice(start, start + size);
    return { content: clip };
  }
};

export const getAll = async () => {
  try {
    const pageData = await get('/api/phieu-bao-hanh?page=0&size=1000');
    if (pageData?.content) {
      return pageData.content;
    }
    return fallbackClaims;
  } catch {
    return fallbackClaims;
  }
};

export const getById = (id) => get(`/api/phieu-bao-hanh/${id}`);

// Lookup tra cuu serial. Throw Error voi .code='NOT_FOUND' hoac .code='DELETED'
export const lookupBySerial = async (soSerial) => {
  const r = await fetch(
    `/api/phieu-bao-hanh/tra-cuu-serial?soSerial=${encodeURIComponent(soSerial)}`,
    { headers: authHeaders() }
  );
  if (r.status === 404) {
    const body = await r.json().catch(() => ({}));
    const err = new Error(body.message || 'Not found');
    err.code = body.code || 'NOT_FOUND';
    err.status = 404;
    throw err;
  }
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.json();
};

export const create = (body) => post('/api/phieu-bao-hanh', body);

export const save = (id, body) =>
  id ? put(`/api/phieu-bao-hanh/update/${id}`, body) : post('/api/phieu-bao-hanh', body);

// ── Mới: lấy phiếu bảo hành theo khách hàng ───────────────────────────────────
export const getByKhachHang = async (khachHangId) => {
  try {
    return await get(`/api/phieu-bao-hanh/khach-hang/${khachHangId}`);
  } catch (e) {
    const byCustomer = fallbackClaims.filter(
      (c) => String(c.khachHangId) === String(khachHangId)
    );
    return byCustomer.length ? byCustomer : fallbackClaims;
  }
};

// ── Mới: cập nhật trạng thái riêng (PATCH) ──────────────────────────────────
export const updateStatus = (baoHanhId, payload) =>
  put(`/api/phieu-bao-hanh/${baoHanhId}/status`, payload);

export const fallbackExtensionRequests = [
  {
    baoHanhId: 101,
    tenSanPham: 'Acer Aspire 5 A515-58',
    maSku: 'SP0006-A515-58',
    soSerial: 'SN-2026-001',
    tenKhachHang: 'Nguyễn Văn A',
    donHangId: 7001,
    tenGoi: 'Gói mở rộng 12 tháng',
    duration: 12,
    donGia: 690000,
    ngayGui: '2026-05-17T08:30:00',
    trangThai: 'cho_duyet',
  },
  {
    baoHanhId: 102,
    tenSanPham: 'Asus ROG Strix G16',
    maSku: 'SP0007-ASUS-ROG',
    soSerial: 'SN-2026-002',
    tenKhachHang: 'Trần Thị B',
    donHangId: 7002,
    tenGoi: 'Gói VIP 36 tháng',
    duration: 36,
    donGia: 1800000,
    ngayGui: '2026-05-18T10:00:00',
    trangThai: 'da_duyet',
    ngayDuyet: '2026-05-19T09:00:00',
  },
];

// ── Mới: lấy lịch sử xử lý của 1 phiếu ────────────────────────────────────────
export const getLichSu = (baoHanhId) =>
  get(`/api/phieu-bao-hanh/${baoHanhId}/lich-su`).catch(() => []);

// ── Mới: khách hàng hủy phiếu của mình khi còn "cho_xu_ly" ────────────────────
export const huyPhieu = (baoHanhId, lyDo) =>
  put(`/api/phieu-bao-hanh/${baoHanhId}/huy`, { lyDo }).catch(() => ({ ok: false }));

// ── Extension requests ─────────────────────────────────────────────────────────
export const getExtensionRequests = async () => fallbackExtensionRequests;

export const approveExtensionRequest = async (baoHanhId, payload = {}) => {
  const res = await fetch(`/api/phieu-bao-hanh/${baoHanhId}/approve-extension`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  });
  return res;
};

export const rejectExtensionRequest = async (baoHanhId, payload = {}) => {
  const res = await fetch(`/api/phieu-bao-hanh/${baoHanhId}/reject-extension`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  });
  return res;
};
