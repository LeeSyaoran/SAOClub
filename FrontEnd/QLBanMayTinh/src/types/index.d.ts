export interface SanPham {
  sanPhamId: number;
  tenSanPham: string;
  tenThuongHieu?: string;
  tenDanhMuc?: string;
  danhMucId?: number;
  hinhAnhChinh?: string;
  giaBan: number;
  giaNhap?: number;
  moTa?: string;
  trangThai?: string;
  phanLoaiTags?: string;
  soLuongTon?: number;
  bienTheId?: number;
}

export interface BienTheSanPham {
  bienTheId: number;
  sanPhamId: number;
  tenSanPham: string;
  sku?: string;
  giaBan: number;
  giaNhap?: number;
  mauSac?: string;
  cpu?: string;
  ram?: string;
  oCung?: string;
  gpu?: string;
  manHinh?: string;
  heDieuHanh?: string;
  pin?: string;
  trongLuong?: number;
  soLuongTon?: number;
  trangThai?: string;
}

export interface DonHang {
  donHangId: number;
  maDonHang?: string;
  khachHangId?: number;
  tenKhachHang?: string;
  ngayDat?: string;
  trangThaiDonHang?: string;
  trangThaiThanhToan?: string;
  thanhTien?: number;
  phiVanChuyen?: number;
  giamGia?: number;
  phuongThucThanhToan?: string;
}

export interface KhachHang {
  khachHangId: number;
  hoTen?: string;
  soDienThoai?: string;
  email?: string;
  diaChi?: string;
  tongChiTieu?: number;
  diemTichLuy?: number;
  trangThai?: string;
}

export interface User {
  id: number;
  hoTen: string;
  username: string;
  role: string;
  token: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}
