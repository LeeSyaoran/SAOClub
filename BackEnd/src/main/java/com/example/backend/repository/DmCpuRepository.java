package com.example.backend.repository;

import com.example.backend.entity.DmCpu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DmCpuRepository extends JpaRepository<DmCpu, Integer> {
}
