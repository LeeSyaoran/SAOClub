package com.example.backend.repository;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    @Query("SELECT new com.example.backend.response.KhuyenMaiResponse(k.khuyenMaiId, k.maKhuyenMai, k.tenKhuyenMai, k.loai, k.giaTri, k.giaTriToiDa, k.donHangToiThieu, k.ngayBatDau, k.ngayKetThuc, k.soLuongToiDa, k.soLanDaDung, k.trangThai, k.ngayTao) FROM KhuyenMai k")
    List<KhuyenMaiResponse> hienThiKhuyenMai();
}
