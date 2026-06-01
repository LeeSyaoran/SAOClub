package com.example.backend.repository;

import com.example.backend.entity.LichSuTonKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuTonKhoRepository extends JpaRepository<LichSuTonKho, Integer> {
}
