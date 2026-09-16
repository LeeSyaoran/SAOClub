package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.SanPhamYeuThich;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.SanPhamYeuThichRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.SanPhamYeuThichResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class SanPhamYeuThichService {

    @Autowired
    private SanPhamYeuThichRepository sanPhamYeuThichRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    private boolean isStaff() {
        Collection<? extends GrantedAuthority> roles =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return roles.stream().anyMatch(r ->
                r.getAuthority().equals("ROLE_ADMIN") ||
                r.getAuthority().equals("ROLE_NHAN_VIEN") ||
                r.getAuthority().equals("ROLE_QUAN_KHO"));
    }

    private KhachHang currentKhachHang() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan tk = taiKhoanRepository.findByUsername(username).orElse(null);
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Tài khoản không liên kết với khách hàng");
        return tk.getKhachHang();
    }

    public List<SanPhamYeuThichResponse> danhSach(Integer khachHangId) {
        if (khachHangId != null) {
            // Staff xem wishlist theo khachHangId
            if (!isStaff()) {
                throw new AccessDeniedException("Chỉ nhân viên mới được xem wishlist của khách hàng khác");
            }
            return sanPhamYeuThichRepository.hienThiTheoKhachHang(khachHangId);
        }
        // Không có khachHangId → staff không có wishlist riêng → trả rỗng thay vì lỗi
        try {
            return sanPhamYeuThichRepository.hienThiTheoKhachHang(currentKhachHang().getKhachHangId());
        } catch (AccessDeniedException e) {
            return List.of(); // Staff account: trả empty, frontend không bị lỗi 403
        }
    }

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
