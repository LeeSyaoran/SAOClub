package com.example.backend.repository;

import com.example.backend.entity.ChiTietGpu;
import com.example.backend.response.ChiTietGpuResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietGpuRepository extends JpaRepository<ChiTietGpu, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietGpuResponse(c.chiTietGpuId, c.gpu.gpuId, c.gpu.tenGpu, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietGpu c")
    List<ChiTietGpuResponse> hienThiChiTietGpu();
}
