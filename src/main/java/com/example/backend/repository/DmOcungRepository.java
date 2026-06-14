package com.example.backend.repository;

import com.example.backend.entity.DmOcung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DmOcungRepository extends JpaRepository<DmOcung, Integer> {
}
