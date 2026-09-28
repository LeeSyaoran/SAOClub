package com.example.backend.repository;

import com.example.backend.entity.TonKho;
import com.example.backend.response.TonKhoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TonKhoRepository extends JpaRepository<TonKho, Integer> {

    @Query("""
        SELECT new com.example.backend.response.TonKhoResponse(
            t.tonKhoId, t.bienThe.bienTheId, t.bienThe.maSku,
            t.bienThe.sanPham.tenSanPham, t.bienThe.mauSac,
            t.soLuongTon, t.soLuongGiu, t.tonKhoToiThieu, t.ngayCapNhat,
            (SELECT COUNT(ct1) FROM ChiTietSanPham ct1 WHERE ct1.bienThe.bienTheId = t.bienThe.bienTheId AND ct1.trangThai = 'da_ban' AND ct1.daXoa = false),
            (SELECT COUNT(ct2) FROM ChiTietSanPham ct2 WHERE ct2.bienThe.bienTheId = t.bienThe.bienTheId AND ct2.daXoa = false)
        )
        FROM TonKho t
        ORDER BY t.bienThe.sanPham.sanPhamId DESC, t.bienThe.bienTheId DESC
        """)
    List<TonKhoResponse> findAllAsResponse();

    @Query("""
        SELECT new com.example.backend.response.TonKhoResponse(
            t.tonKhoId, t.bienThe.bienTheId, t.bienThe.maSku,
            t.bienThe.sanPham.tenSanPham, t.bienThe.mauSac,
            t.soLuongTon, t.soLuongGiu, t.tonKhoToiThieu, t.ngayCapNhat,
            (SELECT COUNT(ct1) FROM ChiTietSanPham ct1 WHERE ct1.bienThe.bienTheId = t.bienThe.bienTheId AND ct1.trangThai = 'da_ban' AND ct1.daXoa = false),
            (SELECT COUNT(ct2) FROM ChiTietSanPham ct2 WHERE ct2.bienThe.bienTheId = t.bienThe.bienTheId AND ct2.daXoa = false)
        )
        FROM TonKho t
        WHERE t.bienThe.bienTheId = :bienTheId
        """)
    Optional<TonKhoResponse> findResponseByBienTheId(@Param("bienTheId") Integer bienTheId);

    Optional<TonKho> findByBienTheBienTheId(Integer bienTheId);

    @Query("SELECT COUNT(t) FROM TonKho t WHERE t.tonKhoToiThieu IS NOT NULL AND t.soLuongTon <= t.tonKhoToiThieu")
    long countLowStock();

    void deleteByBienThe_BienTheId(Integer bienTheId);

    @Modifying
    @Transactional
    @Query("UPDATE TonKho t SET t.tonKhoToiThieu = :nguong")
    int capNhatNguongChoTatCa(@Param("nguong") int nguong);
}
