package com.example.backend.repository;

import com.example.backend.entity.AiKienThuc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKienThucRepository extends JpaRepository<AiKienThuc, Long> {

    Page<AiKienThuc> findByActiveTrue(Pageable pageable);

    Page<AiKienThuc> findByLoaiAndActiveTrue(String loai, Pageable pageable);

    List<AiKienThuc> findByLoaiAndActiveTrueOrderByUpdatedAtDesc(String loai);

    @Query("""
        SELECT k FROM AiKienThuc k
        WHERE k.active = true
        AND (k.loai = 'FAQ' OR k.loai = 'CHINH_SACH' OR k.loai = 'KHAC')
        ORDER BY k.updatedAt DESC
    """)
    List<AiKienThuc> findGeneralKnowledge();

    @Query("""
        SELECT k FROM AiKienThuc k
        WHERE k.active = true
        AND k.loai = 'SAN_PHAM'
        ORDER BY k.updatedAt DESC
    """)
    List<AiKienThuc> findProductKnowledge();

    @Query("""
        SELECT k FROM AiKienThuc k
        WHERE k.active = true
        AND (
            (LOWER(k.tieuDe) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (LOWER(k.noiDung) LIKE LOWER(CONCAT('%', :keyword, '%')))
        )
        ORDER BY k.updatedAt DESC
    """)
    List<AiKienThuc> searchByKeyword(@Param("keyword") String keyword);

    @Query(value = """
        SELECT TOP :limit k.* FROM ai_kien_thuc k
        WHERE k.active = 1
        AND (
            (LOWER(k.tieu_de) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (LOWER(k.noi_dung) LIKE LOWER(CONCAT('%', :keyword, '%')))
        )
        ORDER BY k.updated_at DESC
    """, nativeQuery = true)
    List<AiKienThuc> searchByKeywordNative(@Param("keyword") String keyword, @Param("limit") int limit);

    long countByLoai(String loai);

    List<AiKienThuc> findBySanPhamId(Integer sanPhamId);
}
