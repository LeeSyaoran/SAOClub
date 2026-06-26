package com.example.backend.repository;

import com.example.backend.entity.SanPham;
import com.example.backend.response.SanPhamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    // Trả về 1 dòng/biến thể, gộp đủ thông tin sản phẩm + biến thể vào SanPhamResponse.
    // Dùng LEFT JOIN toàn bộ để sản phẩm thiếu danh mục / thương hiệu vẫn hiển thị.
    // Kết quả mới nhất lên đầu (ORDER BY ngayTao DESC).
    @Query("""
    SELECT new com.example.backend.response.SanPhamResponse(
        sp.sanPhamId,
        bt.bienTheId,
        sp.tenSanPham,
        dm.id,
        dm.tenDanhMuc,
        th.thuongHieuId,
        th.tenThuongHieu,
        ncc.tenNhaCungCap,
        ncc.nhaCungCapId,
        sp.loaiSanPham,
        bt.maSku,
        cpu.tenCpu,
        ram.dungLuong,
        oCung.loaiOcung,
        gpu.tenGpu,
        bt.kichThuocManHinh,
        bt.heDieuHanh,
        bt.pin,
        bt.trongLuongKg,
        bt.mauSac,
        bt.giaBan,
        bt.giaNhap,
        bt.baoHanhThang,
        sp.moTa,
        sp.hinhAnhChinh,
        bt.trangThai,
        sp.ngayTao,
        bt.phanLoaiTags,
        bt.phanLoaiTen
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN sp.danhMuc dm
    LEFT JOIN sp.thuongHieu th
    LEFT JOIN sp.nhaCungCap ncc
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    ORDER BY sp.ngayTao DESC
    """)
    List<SanPhamResponse> hienThiSanPham();
}
