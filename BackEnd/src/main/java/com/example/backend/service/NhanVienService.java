package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.NhanVienRequest;
import com.example.backend.response.NhanVienResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NhanVienService {

    @Autowired private NhanVienRepository nhanVienRepository;
    @Autowired private ChucVuRepository chucVuRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public List<NhanVienResponse> hienThiNhanVien() {
        return nhanVienRepository.hienThiNhanVien();
    }

    public Page<NhanVienResponse> hienThiNhanVien(Pageable pageable) {
        return nhanVienRepository.hienThiNhanVien(pageable);
    }

    public NhanVien getById(Integer id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại với id: " + id));
    }

    @Transactional
    public NhanVien create(NhanVienRequest request) {
        ChucVu chucVu = chucVuRepository.getReferenceById(request.getChucVuId());

        NhanVien entity = new NhanVien();
        entity.setHoTen(request.getHoTen());
        entity.setSoDienThoai(request.getSoDienThoai());
        entity.setEmail(request.getEmail());
        entity.setChucVu(chucVu);
        entity.setLuongCoBan(request.getLuongCoBan());
        entity.setTrangThai(request.getTrangThai());
        entity.setNgayTao(LocalDateTime.now());
        NhanVien saved = nhanVienRepository.save(entity);

        // Tạo tai_khoan nếu có username
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String rawPassword = (request.getMatKhauHash() != null && !request.getMatKhauHash().isBlank())
                    ? request.getMatKhauHash() : "123456";

            TaiKhoan tk = new TaiKhoan();
            tk.setUsername(request.getUsername().trim());
            tk.setMatKhauHash(passwordEncoder.encode(rawPassword));
            tk.setChucVu(chucVu);
            tk.setNhanVien(saved);
            tk.setTrangThai("active");
            tk.setNgayTao(LocalDateTime.now());
            taiKhoanRepository.save(tk);
        }
        return saved;
    }

    @Transactional
    public NhanVien update(Integer id, NhanVienRequest request) {
        NhanVien entity = getById(id);
        entity.setHoTen(request.getHoTen());
        entity.setSoDienThoai(request.getSoDienThoai());
        entity.setEmail(request.getEmail());
        entity.setChucVu(chucVuRepository.getReferenceById(request.getChucVuId()));
        entity.setLuongCoBan(request.getLuongCoBan());
        entity.setTrangThai(request.getTrangThai());
        NhanVien saved = nhanVienRepository.save(entity);

        // Cập nhật mật khẩu nếu được cung cấp
        if (request.getMatKhauHash() != null && !request.getMatKhauHash().isBlank()) {
            taiKhoanRepository.findByNhanVien_NhanVienId(id).ifPresent(tk -> {
                tk.setMatKhauHash(passwordEncoder.encode(request.getMatKhauHash()));
                taiKhoanRepository.save(tk);
            });
        }
        return saved;
    }

}
