package com.example.backend.service;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.LichSuDonHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.LichSuDonHangResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichSuDonHangService {

    @Autowired
    private LichSuDonHangRepository lichSuDonHangRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    public List<LichSuDonHangResponse> getByDonHang(Integer donHangId) {
        if (!isStaffOrOwner(donHangId))
            throw new AccessDeniedException("Không có quyền xem đơn hàng này");
        return lichSuDonHangRepository.getByDonHangId(donHangId);
    }

    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private boolean isStaffOrOwner(Integer donHangId) {
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + donHangId));
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;
        return tk.getKhachHang() != null
                && donHang.getKhachHang() != null
                && tk.getKhachHang().getKhachHangId().equals(donHang.getKhachHang().getKhachHangId());
    }
}
