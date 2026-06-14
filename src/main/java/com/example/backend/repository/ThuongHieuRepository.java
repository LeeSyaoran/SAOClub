package com.example.backend.repository;

import com.example.backend.entity.ThuongHieu;
import com.example.backend.response.ThuongHieuResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Integer> {
	@Query("SELECT new com.example.backend.response.ThuongHieuResponse(t.thuongHieuId, t.tenThuongHieu, t.quocGia, t.moTa, t.trangThai, t.ngayTao) FROM ThuongHieu t")
	java.util.List<ThuongHieuResponse> hienThiThuongHieu();
}
