package com.example.backend.repository;

import com.example.backend.entity.PhieuBaoHanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuBaoHanhRepository extends JpaRepository<PhieuBaoHanh, Integer> {
}
