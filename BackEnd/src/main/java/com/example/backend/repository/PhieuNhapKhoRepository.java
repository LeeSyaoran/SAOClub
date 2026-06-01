package com.example.backend.repository;

import com.example.backend.entity.PhieuNhapKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuNhapKhoRepository extends JpaRepository<PhieuNhapKho, Integer> {
}
