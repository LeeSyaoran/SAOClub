package com.example.backend.repository;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.response.DmDoiThuongResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DmDoiThuongRepository extends JpaRepository<DmDoiThuong, Integer> {
    @Query("SELECT new com.example.backend.response.DmDoiThuongResponse(d.doiThuongId, d.ten, d.moTa, d.diemCan, d.loai, d.giaTri, d.giaTriToiDa, d.trangThai, d.ngayTao) FROM DmDoiThuong d")
    List<DmDoiThuongResponse> hienThiDmDoiThuong();
}
