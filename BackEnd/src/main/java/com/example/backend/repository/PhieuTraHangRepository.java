package com.example.backend.repository;

import com.example.backend.entity.PhieuTraHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuTraHangRepository extends JpaRepository<PhieuTraHang, Integer> {
}
