package com.example.backend.repository;

import com.example.backend.entity.CaiDatHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaiDatHeThongRepository extends JpaRepository<CaiDatHeThong, Integer> {
}
