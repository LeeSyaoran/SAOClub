package com.example.backend.repository;

import com.example.backend.entity.DonHang;
import com.example.backend.response.DonHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {
	@Query("SELECT new com.example.backend.response.DonHangResponse(d.id, d.maDonHang, d.khachHang.khachHangId, d.nhanVien.nhanVienId, d.khuyenMai.khuyenMaiId, d.diaChiGiaoHang.id, d.diaChiGiaoHang.diaChi, d.nguoiNhan, d.sdtNguoiNhan, d.tongTien, d.giamGia, d.phiVanChuyen, d.thanhTien, d.ngayDat, d.ngayGiaoDuKien, d.ngayGiaoThucTe, d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu) FROM DonHang d")
	java.util.List<DonHangResponse> hienThiDonHang();
}
