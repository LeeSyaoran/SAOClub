package com.example.backend.repository;

import com.example.backend.entity.TinNhan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, Long> {

    Page<TinNhan> findByCuocTroChuyenIdOrderByCreatedAtAsc(Long cuocTroChuyenId, Pageable pageable);

    List<TinNhan> findByCuocTroChuyenIdOrderByCreatedAtAsc(Long cuocTroChuyenId);

    long countByCuocTroChuyenIdAndDaDocFalse(Long cuocTroChuyenId);

    long countByCuocTroChuyenIdAndDaDocFalseAndNguoiGuiNot(Long cuocTroChuyenId, String nguoiGui);

    @Modifying
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.cuocTroChuyen.id = :ctcId AND t.daDoc = false")
    void markAllAsReadByCuocTroChuyenId(@Param("ctcId") Long cuocTroChuyenId);

    @Query("""
        SELECT t FROM TinNhan t
        WHERE t.cuocTroChuyen.id = :ctcId
        ORDER BY t.createdAt DESC
    """)
    List<TinNhan> findLatestByCuocTroChuyenId(@Param("ctcId") Long cuocTroChuyenId, Pageable pageable);

    long countByCuocTroChuyenKhachHangKhachHangId(Integer khachHangId);

    boolean existsByCuocTroChuyenId(Long cuocTroChuyenId);
}
