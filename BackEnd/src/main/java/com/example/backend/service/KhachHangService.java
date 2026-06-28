package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.KhachHangRegisterRequest;
import com.example.backend.request.KhachHangRequest;
import com.example.backend.response.KhachHangLoginResponse;
import com.example.backend.response.KhachHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private ChucVuRepository chucVuRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<KhachHangResponse> hienThiKhachHang() {
        return khachHangRepository.hienThiKhachHang();
    }

    public KhachHang getById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + id));
    }

    public KhachHang create(KhachHangRequest request) {
        KhachHang entity = new KhachHang();
        BeanUtils.copyProperties(request, entity);
        entity.setNgayTao(LocalDateTime.now());
        return khachHangRepository.save(entity);
    }

    public KhachHang update(Integer id, KhachHangRequest request) {
        KhachHang entity = getById(id);
        BeanUtils.copyProperties(request, entity, "khachHangId", "ngayTao");
        return khachHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!khachHangRepository.existsById(id))
            throw new IllegalArgumentException("Khách hàng không tồn tại với id: " + id);
        khachHangRepository.deleteById(id);
    }

    @Transactional
    public KhachHangLoginResponse register(KhachHangRegisterRequest request) {
        if (taiKhoanRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username đã tồn tại");
        if (khachHangRepository.existsBySoDienThoai(request.getSoDienThoai()))
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");

        KhachHang entity = new KhachHang();
        entity.setHoTen(request.getHoTen());
        entity.setSoDienThoai(request.getSoDienThoai());
        entity.setEmail(request.getEmail());
        entity.setDiaChi(request.getDiaChi());
        entity.setDiemTichLuy(0);
        entity.setTrangThai("hoat_dong");
        entity.setLoaiKhach("ca_nhan");
        entity.setNgayTao(LocalDateTime.now());
        KhachHang savedKh = khachHangRepository.save(entity);

        ChucVu chucVu = chucVuRepository.findByMaChucVu("khach_hang")
                .orElseThrow(() -> new IllegalStateException("Thiếu dữ liệu chức vụ 'khach_hang'"));

        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(request.getUsername());
        tk.setMatKhauHash(passwordEncoder.encode(request.getPassword()));
        tk.setChucVu(chucVu);
        tk.setKhachHang(savedKh);
        tk.setTrangThai("active");
        tk.setNgayTao(LocalDateTime.now());
        taiKhoanRepository.save(tk);

        return new KhachHangLoginResponse(
                savedKh.getKhachHangId(), savedKh.getHoTen(), request.getUsername(),
                savedKh.getSoDienThoai(), savedKh.getEmail(),
                savedKh.getDiemTichLuy(), savedKh.getTrangThai());
    }
}
