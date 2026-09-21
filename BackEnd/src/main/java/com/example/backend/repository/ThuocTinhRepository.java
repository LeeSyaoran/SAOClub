package com.example.backend.repository;

import com.example.backend.entity.ThuocTinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThuocTinhRepository extends JpaRepository<ThuocTinh, Integer> {

    // Lấy tất cả thuộc tính kèm danh sách giá trị
    @Query("SELECT DISTINCT t FROM ThuocTinh t LEFT JOIN FETCH t.giaTriList ORDER BY t.thuTuHienThi ASC")
    List<ThuocTinh> findAllByOrderByThuTuHienThiAsc();

    @Query("SELECT DISTINCT t FROM ThuocTinh t LEFT JOIN FETCH t.giaTriList WHERE t.trangThai = :trangThai ORDER BY t.thuTuHienThi ASC")
    List<ThuocTinh> findByTrangThaiOrderByThuTuHienThiAsc(String trangThai);

    @Query("SELECT t FROM ThuocTinh t LEFT JOIN FETCH t.giaTriList WHERE t.tenTruong = :tenTruong")
    Optional<ThuocTinh> findByTenTruong(String tenTruong);

    boolean existsByTenTruong(String tenTruong);
}
