package com.example.backend.repository;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    Optional<KhuyenMai> findByMaKhuyenMaiIgnoreCase(String maKhuyenMai);

    @Query("SELECT k FROM KhuyenMai k WHERE k.trangThai = 'active' " +
           "AND k.ngayBatDau <= CURRENT_TIMESTAMP AND k.ngayKetThuc >= CURRENT_TIMESTAMP")
    List<KhuyenMai> findActiveKhaDung();
}
