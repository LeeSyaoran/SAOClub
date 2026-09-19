package com.example.backend.repository;

import com.example.backend.entity.CuocTroChuyen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuocTroChuyenRepository extends JpaRepository<CuocTroChuyen, Long> {

    Optional<CuocTroChuyen> findBySessionId(String sessionId);

    Optional<CuocTroChuyen> findBySessionIdAndLoaiKhach(String sessionId, String loaiKhach);

    Page<CuocTroChuyen> findByLoaiKhachOrderByUpdatedAtDesc(String loaiKhach, Pageable pageable);

    Page<CuocTroChuyen> findByLoaiKhachAndTrangThaiOrderByUpdatedAtDesc(String loaiKhach, String trangThai, Pageable pageable);

    @Query("""
        SELECT c FROM CuocTroChuyen c
        WHERE c.loaiKhach = :loaiKhach
        ORDER BY
            CASE c.trangThai
                WHEN 'HOI_DAP_AI' THEN 0
                WHEN 'CHAT_NHAN_VIEN' THEN 1
                ELSE 2
            END,
            c.updatedAt DESC
    """)
    Page<CuocTroChuyen> findByLoaiKhachWithPriority(@Param("loaiKhach") String loaiKhach, Pageable pageable);

    @Query("""
        SELECT c FROM CuocTroChuyen c
        WHERE c.trangThai = :trangThai
        AND (:loaiKhach IS NULL OR c.loaiKhach = :loaiKhach)
        ORDER BY c.updatedAt DESC
    """)
    Page<CuocTroChuyen> findByTrangThaiAndLoaiKhach(
            @Param("trangThai") String trangThai,
            @Param("loaiKhach") String loaiKhach,
            Pageable pageable);

    long countByTrangThaiAndLoaiKhach(String trangThai, String loaiKhach);

    long countByLoaiKhachAndTrangThaiIn(String loaiKhach, java.util.List<String> trangThais);
}
