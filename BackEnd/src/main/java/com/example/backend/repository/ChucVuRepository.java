package com.example.backend.repository;

import com.example.backend.entity.ChucVu;
import com.example.backend.response.ChucVuResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChucVuRepository extends JpaRepository<ChucVu, Integer> {
    @Query("SELECT new com.example.backend.response.ChucVuResponse(c.id, c.tenChucVu, c.moTa) FROM ChucVu c")
    List<ChucVuResponse> hienThiChucVu();

    java.util.Optional<ChucVu> findByMaChucVu(String maChucVu);
}
