package com.example.backend.repository;

import com.example.backend.entity.TonKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TonKhoRepository extends JpaRepository<TonKho, Integer> {
}
