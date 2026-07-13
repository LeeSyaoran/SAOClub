package com.example.backend.repository;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.response.BienTheSanPhamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BienTheSanPhamRepository extends JpaRepository<BienTheSanPham, Integer> {

    // DTO query — tránh N+1 khi serialize các @ManyToOne lồng nhau (cpu, ram, oCung, gpu)
    // Dùng LEFT JOIN vì cpu/ram/oCung/gpu đều nullable (điện thoại không có CPU laptop, ...)
    @Query("""
    SELECT new com.example.backend.response.BienTheSanPhamResponse(
        bt.bienTheId, sp.sanPhamId, bt.maSku, bt.giaNhap, bt.giaBan,
        bt.baoHanhThang, bt.hinhAnhBienThe, bt.trangThai, bt.mauSac,
        cpu.cpuId, cpu.tenCpu,
        ram.ramId, ram.dungLuong,
        oCung.oCungId, oCung.loaiOcung,
        gpu.gpuId, gpu.tenGpu,
        bt.kichThuocManHinh, bt.heDieuHanh, bt.pin, bt.trongLuongKg
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    ORDER BY sp.sanPhamId
    """)
    List<BienTheSanPhamResponse> hienThiBienTheSanPham();

    // Toàn bộ biến thể của 1 sản phẩm — dùng khi xóa cả sản phẩm (SanPhamService.deleteSanPham).
    List<BienTheSanPham> findBySanPham_SanPhamId(Integer sanPhamId);
}
