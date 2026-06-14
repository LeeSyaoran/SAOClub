package com.example.backend.repository;

import com.example.backend.entity.DanhMuc;
import com.example.backend.response.DanhMucResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {
	@Query("SELECT new com.example.backend.response.DanhMucResponse(d.id, d.tenDanhMuc, d.moTa, d.trangThai, d.ngayTao) FROM DanhMuc d")
	java.util.List<DanhMucResponse> hienThiDanhMuc();
}
