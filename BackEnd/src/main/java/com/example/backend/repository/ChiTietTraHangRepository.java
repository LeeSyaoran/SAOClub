package com.example.backend.repository;

import com.example.backend.entity.ChiTietTraHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietTraHangRepository extends JpaRepository<ChiTietTraHang, Integer> {
}
