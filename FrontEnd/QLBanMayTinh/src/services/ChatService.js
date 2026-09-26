import { get, post, authHeaders } from "./api.js";

const getBaseUrl = () => {
  const raw = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
  // Relative path like "/api" → resolve against origin
  if (!raw.match(/^https?:\/\//)) {
    return window.location.origin + (raw.endsWith("/") ? raw.slice(0, -1) : raw);
  }
  // Full URL without /api suffix → append /api
  const url = raw.endsWith("/api") ? raw : raw + "/api";
  return url;
};

const BASE = getBaseUrl();

// ─── Public APIs (khách hàng) ────────────────────────────────────────────────

/** Tạo phiên chat mới */
export async function taoPhienChat({ sessionId, khachHangId, hoTen }) {
  const params = new URLSearchParams();
  if (sessionId) params.set("sessionId", sessionId);
  if (khachHangId) params.set("khachHangId", khachHangId);
  if (hoTen) params.set("hoTen", hoTen);
  const res = await post(`${BASE}/chat/tao-phien?${params.toString()}`, {});
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/** Lấy phiên chat theo sessionId */
export async function layPhienChat(sessionId) {
  return get(`${BASE}/chat/phien/${sessionId}`);
}

/** Lấy tin nhắn (phân trang) */
export async function layTinNhan(cuocTroChuyenId, page = 0, size = 50) {
  return get(`${BASE}/chat/${cuocTroChuyenId}/tin-nhan?page=${page}&size=${size}`);
}

/** Gửi tin nhắn từ khách hàng */
export async function guiTinNhan(cuocTroChuyenId, noiDung, sessionId) {
  const body = { noiDung };
  if (sessionId) body.sessionId = sessionId;
  const res = await post(`${BASE}/chat/${cuocTroChuyenId}/tin-nhan`, body);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ─── Staff APIs ──────────────────────────────────────────────────────────────

/** Danh sách cuộc trò chuyện */
export async function layDanhSachChat(loai, trangThai, page = 0, size = 20) {
  const params = new URLSearchParams({ loai, page, size });
  if (trangThai) params.set("trangThai", trangThai);
  return get(`${BASE}/chat/danh-sach?${params}`);
}

/** Nhân viên nhận tiếp */
export async function nhanTiepChat(cuocTroChuyenId) {
  const res = await post(`${BASE}/chat/${cuocTroChuyenId}/nhan-tiep`, {});
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/** Gửi tin nhắn từ nhân viên */
export async function guiTinNhanTuNhanVien(cuocTroChuyenId, noiDung) {
  const res = await post(`${BASE}/chat/${cuocTroChuyenId}/nv-tin-nhan`, { noiDung });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/** Quay lại AI */
export async function quayLaiAI(cuocTroChuyenId) {
  const res = await post(`${BASE}/chat/${cuocTroChuyenId}/quay-lai-ai`, {});
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/** Đóng cuộc trò chuyện */
export async function dongChat(cuocTroChuyenId) {
  const res = await post(`${BASE}/chat/${cuocTroChuyenId}/dong`, {});
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/** Lịch sử chat của khách hàng */
export async function layLichSuChatKhach(khachHangId, page = 0, size = 10) {
  return get(`${BASE}/chat/khach/${khachHangId}?page=${page}&size=${size}`);
}

/** Thông báo đếm */
export async function demThongBao() {
  return get(`${BASE}/chat/thong-bao`);
}

// ─── AI Knowledge Base APIs ──────────────────────────────────────────────────

/** Danh sách kiến thức AI */
export async function layKienThucAI(page = 0, size = 20) {
  return get(`${BASE}/chat/ai-kien-thuc?page=${page}&size=${size}`);
}

export async function layKienThucTheoLoai(loai, page = 0, size = 20) {
  return get(`${BASE}/chat/ai-kien-thuc/theo-loai?loai=${loai}&page=${page}&size=${size}`);
}

export async function taoKienThuc(data) {
  const res = await post(`${BASE}/chat/ai-kien-thuc`, data);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function capNhatKienThuc(id, data) {
  const res = await post(`${BASE}/chat/ai-kien-thuc/${id}`, data);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function xoaKienThuc(id) {
  const res = await fetch(`${BASE}/chat/ai-kien-thuc/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(await res.text());
}
