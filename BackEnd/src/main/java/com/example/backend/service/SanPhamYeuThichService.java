package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.SanPhamYeuThich;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.SanPhamYeuThichRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.SanPhamYeuThichResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Danh sach yeu thich cua khach hang — luon lay khach hang tu SecurityContextHolder, khong
// bao gio tin khachHangId tu client (giong pattern taoYeuCauTuKhachHang() trong
// PhieuTraHangService) — khach A khong the xem/xoa yeu thich cua khach B chi bang doan URL.
@Service
public class SanPhamYeuThichService {

    @Autowired
    private SanPhamYeuThichRepository sanPhamYeuThichRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    private KhachHang currentKhachHang() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan tk = taiKhoanRepository.findByUsername(username).orElse(null);
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Chỉ tài khoản khách hàng mới có danh sách yêu thích");
        return tk.getKhachHang();
    }

    public List<SanPhamYeuThichResponse> danhSach() {
        return sanPhamYeuThichRepository.hienThiTheoKhachHang(currentKhachHang().getKhachHangId());
    }

    // Idempotent — bam tim lai 1 san pham da yeu thich se khong loi, chi tra ve dong da co san
    // (tranh loi khi khach bam nut nhanh 2 lan lien tuc / double-submit).
    @Transactional
    public SanPhamYeuThich themVao(Integer bienTheId) {
        KhachHang kh = currentKhachHang();
        return sanPhamYeuThichRepository
                .findByKhachHang_KhachHangIdAndBienThe_BienTheId(kh.getKhachHangId(), bienTheId)
                .orElseGet(() -> {
                    SanPhamYeuThich yt = new SanPhamYeuThich();
                    yt.setKhachHang(kh);
                    yt.setBienThe(bienTheSanPhamRepository.getReferenceById(bienTheId));
                    yt.setNgayThem(LocalDateTime.now());
                    return sanPhamYeuThichRepository.save(yt);
                });
    }

    @Transactional
    public void xoa(Integer bienTheId) {
        KhachHang kh = currentKhachHang();
        sanPhamYeuThichRepository
                .findByKhachHang_KhachHangIdAndBienThe_BienTheId(kh.getKhachHangId(), bienTheId)
                .ifPresent(sanPhamYeuThichRepository::delete);
    }
}
