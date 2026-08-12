package com.example.backend.repository;

import com.example.backend.entity.KhachHang;
import com.example.backend.response.KhachHangResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	java.util.Optional<KhachHang> findWithLockByKhachHangId(Integer khachHangId);

	@Query("SELECT new com.example.backend.response.KhachHangResponse(k.khachHangId, k.hoTen, k.soDienThoai, k.email, k.diaChi, k.loaiKhach, k.tenCongTy, k.maSoThue, k.diemTichLuy, k.soDuVi, k.trangThai, k.ngayTao) FROM KhachHang k")
	java.util.List<KhachHangResponse> hienThiKhachHang();

	@Query(value = "SELECT new com.example.backend.response.KhachHangResponse(k.khachHangId, k.hoTen, k.soDienThoai, k.email, k.diaChi, k.loaiKhach, k.tenCongTy, k.maSoThue, k.diemTichLuy, k.soDuVi, k.trangThai, k.ngayTao) FROM KhachHang k",
		   countQuery = "SELECT COUNT(k) FROM KhachHang k")
	Page<KhachHangResponse> hienThiKhachHang(Pageable pageable);

	boolean existsBySoDienThoai(String soDienThoai);

	java.util.Optional<KhachHang> findBySoDienThoai(String soDienThoai);

	@Query("""
	SELECT new com.example.backend.response.CustomerSpendingResponse(kh.khachHangId, kh.hoTen, COUNT(d), SUM(d.thanhTien))
	FROM DonHang d JOIN d.khachHang kh
	WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay AND d.trangThaiDonHang <> 'cancelled'
	GROUP BY kh.khachHangId, kh.hoTen
	ORDER BY SUM(d.thanhTien) DESC
	""")
	java.util.List<com.example.backend.response.CustomerSpendingResponse> chiTieuTheoKhachHang(
			@org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
			@org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay,
			org.springframework.data.domain.Pageable pageable);
}
