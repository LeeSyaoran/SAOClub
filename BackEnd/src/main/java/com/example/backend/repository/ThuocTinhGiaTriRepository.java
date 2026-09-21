package com.example.backend.repository;

import com.example.backend.entity.ThuocTinhGiaTri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThuocTinhGiaTriRepository extends JpaRepository<ThuocTinhGiaTri, Integer> {

    List<ThuocTinhGiaTri> findByThuocTinh_ThuocTinhIdOrderByThuTuAsc(Integer thuocTinhId);

    boolean existsByThuocTinh_ThuocTinhIdAndGiaTri(Integer thuocTinhId, String giaTri);

    @Modifying
    @Query("DELETE FROM ThuocTinhGiaTri t WHERE t.thuocTinh.thuocTinhId = :thuocTinhId")
    void deleteByThuocTinhId(@Param("thuocTinhId") Integer thuocTinhId);
}
