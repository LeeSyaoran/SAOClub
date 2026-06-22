package com.example.backend.service;

import com.example.backend.entity.NhanVien;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.request.NhanVienRequest;
import com.example.backend.response.NhanVienResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private ChucVuRepository chucVuRepository;

    public List<NhanVienResponse> hienThiNhanVien() {
        return nhanVienRepository.hienThiNhanVien();
    }

    public NhanVien getById(Integer id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại với id: " + id));
    }

    public NhanVien create(NhanVienRequest request) {
        NhanVien entity = new NhanVien();
        // BeanUtils copies: hoTen, soDienThoai, email, username, matKhauHash, luongCoBan, trangThai
        // Bỏ qua chucVuId vì entity dùng object ChucVu (khác tên)
        BeanUtils.copyProperties(request, entity, "chucVuId");
        entity.setChucVu(chucVuRepository.getReferenceById(request.getChucVuId()));
        entity.setNgayTao(LocalDateTime.now());
        return nhanVienRepository.save(entity);
    }

    public NhanVien update(Integer id, NhanVienRequest request) {
        NhanVien entity = getById(id);
        BeanUtils.copyProperties(request, entity, "nhanVienId", "ngayTao", "chucVuId", "matKhauHash");
        entity.setChucVu(chucVuRepository.getReferenceById(request.getChucVuId()));
        if (request.getMatKhauHash() != null && !request.getMatKhauHash().isBlank())
            entity.setMatKhauHash(request.getMatKhauHash());
        return nhanVienRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!nhanVienRepository.existsById(id))
            throw new IllegalArgumentException("Nhân viên không tồn tại với id: " + id);
        nhanVienRepository.deleteById(id);
    }
}
