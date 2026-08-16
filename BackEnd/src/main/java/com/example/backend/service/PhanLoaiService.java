package com.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Phân loại theo mục đích sử dụng (văn phòng, gaming, đồ họa…).
 *
 * Bảng nối san_pham_phan_loai đã có trigger trg_SyncPhanLoaiTags: mỗi lần thêm/xóa dòng
 * nối, trigger tự cập nhật hai cột cache phan_loai_tags / phan_loai_ten trên tất cả biến
 * thể của sản phẩm đó. Vì vậy service này chỉ cần ghi đúng bảng nối, không đụng vào cache.
 */
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

    /** Ghi đè toàn bộ danh sách phân loại của một sản phẩm (danh sách rỗng = bỏ hết). */
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