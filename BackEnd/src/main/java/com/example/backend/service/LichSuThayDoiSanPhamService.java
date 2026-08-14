package com.example.backend.service;

import com.example.backend.entity.LichSuThayDoiSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.LichSuThayDoiSanPhamRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.LichSuThayDoiSanPhamResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class LichSuThayDoiSanPhamService {

    @Autowired private LichSuThayDoiSanPhamRepository lichSuThayDoiSanPhamRepository;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;

    public NhanVien nguoiSuaHienTai() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username)
                .map(tk -> tk.getNhanVien())
                .orElse(null);
    }

    public void ghiNeuThayDoi(Integer sanPhamId, Integer bienTheId, String doiTuong,
                               String tenTruong, Object giaTriCu, Object giaTriMoi, NhanVien nguoiSua) {
        String cu = giaTriCu == null ? null : String.valueOf(giaTriCu);
        String moi = giaTriMoi == null ? null : String.valueOf(giaTriMoi);
        if (Objects.equals(cu, moi)) return;

        LichSuThayDoiSanPham log = new LichSuThayDoiSanPham();
        log.setSanPham(sanPhamRepository.getReferenceById(sanPhamId));
        log.setBienThe(bienTheId != null ? bienTheSanPhamRepository.getReferenceById(bienTheId) : null);
        log.setDoiTuong(doiTuong);
        log.setTenTruong(tenTruong);
        log.setGiaTriCu(cu);
        log.setGiaTriMoi(moi);
        log.setNhanVien(nguoiSua);
        lichSuThayDoiSanPhamRepository.save(log);
    }

    public List<LichSuThayDoiSanPhamResponse> layLichSu(Integer sanPhamId) {
        return lichSuThayDoiSanPhamRepository.hienThiLichSu(sanPhamId);
    }
}
