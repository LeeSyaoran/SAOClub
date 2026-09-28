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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class LichSuThayDoiSanPhamService {

    @Autowired private LichSuThayDoiSanPhamRepository lichSuThayDoiSanPhamRepository;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;

    public NhanVien nguoiSuaHienTai() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return taiKhoanRepository.findAll().stream()
                    .filter(tk -> tk.getNhanVien() != null)
                    .map(tk -> tk.getNhanVien())
                    .findFirst()
                    .orElse(null);
        }
        return taiKhoanRepository.findByUsername(auth.getName())
                .map(tk -> tk.getNhanVien())
                .orElse(null);
    }

    public void ghiNeuThayDoi(Integer sanPhamId, Integer bienTheId, String doiTuong,
                               String tenTruong, Object giaTriCu, Object giaTriMoi, NhanVien nguoiSua) {
        // Chuyển null thành chuỗi null, không phải "null"
        String cu = giaTriCu == null ? null : String.valueOf(giaTriCu);
        String moi = giaTriMoi == null ? null : String.valueOf(giaTriMoi);

        // Trim sau khi đã lọc null
        if (cu != null) cu = cu.trim();
        if (moi != null) moi = moi.trim();

        if ((cu == null || cu.isEmpty()) && (moi == null || moi.isEmpty())) return;
        if (Objects.equals(cu, moi)) return;

        // So sánh số học nếu cả hai đều là số (tránh log 1.70 -> 1.7 hoặc 28990000.00 -> 28990000)
        if (cu != null && moi != null) {
            try {
                BigDecimal bCu = new BigDecimal(cu);
                BigDecimal bMoi = new BigDecimal(moi);
                if (bCu.compareTo(bMoi) == 0) return;
            } catch (NumberFormatException ignored) {}
        }

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
