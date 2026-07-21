package com.example.backend.repository;

import com.example.backend.entity.PhieuGiamGiaCaNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuGiamGiaCaNhanRepository extends JpaRepository<PhieuGiamGiaCaNhan, Integer> {
    List<PhieuGiamGiaCaNhan> findByKhachHang_KhachHangId(Integer khachHangId);
    Optional<PhieuGiamGiaCaNhan> findByMaPhieu(String maPhieu);
}
