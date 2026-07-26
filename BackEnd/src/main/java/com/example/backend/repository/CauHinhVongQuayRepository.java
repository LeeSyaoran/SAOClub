package com.example.backend.repository;

import com.example.backend.entity.CauHinhVongQuay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CauHinhVongQuayRepository extends JpaRepository<CauHinhVongQuay, Integer> {
}
