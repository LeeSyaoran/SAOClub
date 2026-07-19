package com.example.backend.repository;

import com.example.backend.entity.KhachHang;
import com.example.backend.response.KhachHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
	@Query("SELECT new com.example.backend.response.KhachHangResponse(k.khachHangId, k.hoTen, k.soDienThoai, k.email, k.diaChi, k.loaiKhach, k.tenCongTy, k.maSoThue, k.diemTichLuy, k.soDuVi, k.trangThai, k.ngayTao) FROM KhachHang k")
	java.util.List<KhachHangResponse> hienThiKhachHang();

	boolean existsBySoDienThoai(String soDienThoai);

	java.util.Optional<KhachHang> findBySoDienThoai(String soDienThoai);

	// Chi tiêu từng khách trong khoảng ngày — dùng cho báo cáo "Khách hàng nổi bật" (top
	// chi tiêu + tỷ lệ mua lại). Không phân trang ở DB (Pageable ở service truyền
	// Pageable.unpaged() khi cần đếm tỷ lệ mua lại trên toàn bộ, hoặc PageRequest khi
	// service tự giới hạn) — JOIN thường vì 1 đơn luôn có khách hàng (NOT NULL).
	@Query("""
	SELECT new com.example.backend.response.CustomerSpendingResponse(kh.khachHangId, kh.hoTen, COUNT(d), SUM(d.thanhTien))
	FROM DonHang d JOIN d.khachHang kh
	WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay
	GROUP BY kh.khachHangId, kh.hoTen
	ORDER BY SUM(d.thanhTien) DESC
	""")
	java.util.List<com.example.backend.response.CustomerSpendingResponse> chiTieuTheoKhachHang(
			@org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
			@org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay,
			org.springframework.data.domain.Pageable pageable);
}
