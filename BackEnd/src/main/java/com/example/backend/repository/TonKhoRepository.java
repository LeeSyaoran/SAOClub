package com.example.backend.repository;

import com.example.backend.entity.TonKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TonKhoRepository extends JpaRepository<TonKho, Integer> {
    // Tìm tồn kho theo biến thể — TonKho có @OneToOne với BienTheSanPham
    Optional<TonKho> findByBienTheBienTheId(Integer bienTheId);

    // Số biến thể sắp hết/hết hàng cho Dashboard KPI — khớp định nghĩa "lowStockItems"
    // hiện có ở frontend: còn tồn <= mức tối thiểu (bao gồm cả trường hợp = 0).
    @Query("SELECT COUNT(t) FROM TonKho t WHERE t.tonKhoToiThieu IS NOT NULL AND t.soLuongTon <= t.tonKhoToiThieu")
    long countLowStock();

    // Xóa dòng tồn kho khi xóa hẳn biến thể/sản phẩm (chưa từng bán).
    void deleteByBienThe_BienTheId(Integer bienTheId);
}
