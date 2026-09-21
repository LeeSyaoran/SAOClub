package com.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Dịch vụ quản lý phân loại sản phẩm
@Service
public class PhanLoaiService {

    @Autowired
    private JdbcTemplate jdbc;

    public List<Map<String, Object>> danhSach() {
        return jdbc.queryForList("""
                SELECT phan_loai_id AS phanLoaiId, ma_phan_loai AS maPhanLoai,
                       ten_phan_loai AS tenPhanLoai, mo_ta AS moTa, thu_tu AS thuTu,
                       trang_thai AS trangThai
                FROM phan_loai
                WHERE trang_thai = N'active'
                ORDER BY thu_tu, ten_phan_loai
                """);
    }

    public List<Integer> cuaSanPham(Integer sanPhamId) {
        return jdbc.queryForList(
                "SELECT phan_loai_id FROM san_pham_phan_loai WHERE san_pham_id = ? ORDER BY phan_loai_id",
                Integer.class, sanPhamId);
    }

    // Cập nhật danh sách phân loại cho sản phẩm
    @Transactional
    public void luuChoSanPham(Integer sanPhamId, List<Integer> phanLoaiIds) {
        jdbc.update("DELETE FROM san_pham_phan_loai WHERE san_pham_id = ?", sanPhamId);

        if (phanLoaiIds == null || phanLoaiIds.isEmpty()) return;

        List<Object[]> batch = new ArrayList<>();
        phanLoaiIds.stream().filter(java.util.Objects::nonNull).distinct()
                .forEach(id -> batch.add(new Object[]{sanPhamId, id}));

        jdbc.batchUpdate("INSERT INTO san_pham_phan_loai (san_pham_id, phan_loai_id) VALUES (?, ?)", batch);
    }
}