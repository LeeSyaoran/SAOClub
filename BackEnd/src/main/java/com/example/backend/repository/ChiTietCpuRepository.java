package com.example.backend.repository;

import com.example.backend.entity.ChiTietCpu;
import com.example.backend.response.ChiTietCpuResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietCpuRepository extends JpaRepository<ChiTietCpu, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietCpuResponse(c.chiTietCpuId, c.cpu.cpuId, c.cpu.tenCpu, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietCpu c")
    List<ChiTietCpuResponse> hienThiChiTietCpu();
}
