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
            t.soLuongTon, t.soLuongGiu, t.tonKhoToiThieu, t.ngayCapNhat
        )
        FROM TonKho t
        ORDER BY t.ngayCapNhat DESC
        """)
    List<TonKhoResponse> findAllAsResponse();

    Optional<TonKho> findByBienTheBienTheId(Integer bienTheId);

    @Query("SELECT COUNT(t) FROM TonKho t WHERE t.tonKhoToiThieu IS NOT NULL AND t.soLuongTon <= t.tonKhoToiThieu")
    long countLowStock();

    void deleteByBienThe_BienTheId(Integer bienTheId);

    @Modifying
    @Transactional
    @Query("UPDATE TonKho t SET t.tonKhoToiThieu = :nguong")
    int capNhatNguongChoTatCa(@Param("nguong") int nguong);
}
