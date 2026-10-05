package com.example.backend.security;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra quyền truy cập đơn hàng, dùng trong @PreAuthorize:
 *   @PreAuthorize("@orderAccessGuard.canView(#donHangId)")
 * Nhân viên (mọi role khác khach_hang) được xem tất cả; khách hàng chỉ xem đơn của mình.
 */
@Component("orderAccessGuard")
public class OrderAccessGuard {

    private final DonHangRepository donHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public OrderAccessGuard(DonHangRepository donHangRepository, TaiKhoanRepository taiKhoanRepository) {
        this.donHangRepository = donHangRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    public boolean canView(Integer donHangId) {
        if (donHangId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return false;

        TaiKhoan tk = taiKhoanRepository.findByUsername(auth.getName()).orElse(null);
        if (tk == null || tk.getChucVu() == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;

        DonHang dh = donHangRepository.findById(donHangId).orElse(null);
        return dh != null
                && tk.getKhachHang() != null
                && dh.getKhachHang() != null
                && tk.getKhachHang().getKhachHangId().equals(dh.getKhachHang().getKhachHangId());
    }
}

