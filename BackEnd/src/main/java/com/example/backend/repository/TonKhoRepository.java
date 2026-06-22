package com.example.backend.repository;

import com.example.backend.entity.TonKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TonKhoRepository extends JpaRepository<TonKho, Integer> {
    // Tìm tồn kho theo biến thể — TonKho có @OneToOne với BienTheSanPham
    Optional<TonKho> findByBienTheBienTheId(Integer bienTheId);
}
