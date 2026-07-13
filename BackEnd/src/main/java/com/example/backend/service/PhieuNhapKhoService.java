package com.example.backend.service;

import com.example.backend.entity.PhieuNhapKho;
import com.example.backend.repository.ChiTietPhieuNhapRepository;
import com.example.backend.repository.NhaCungCapRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuNhapKhoRepository;
import com.example.backend.request.PhieuNhapKhoRequest;
import com.example.backend.response.PhieuNhapKhoResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PhieuNhapKhoService {

    @Autowired
    private PhieuNhapKhoRepository phieuNhapKhoRepository;
    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;

    public List<PhieuNhapKhoResponse> hienThiPhieuNhapKho() {
        return phieuNhapKhoRepository.hienThiPhieuNhapKho();
    }

    public PhieuNhapKho getById(Integer id) {
        return phieuNhapKhoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu nhập kho không tồn tại với id: " + id));
    }

    public PhieuNhapKho create(PhieuNhapKhoRequest request) {
        PhieuNhapKho entity = new PhieuNhapKho();
        // BeanUtils copies: maPhieuNhap, ngayNhap, tongTien, trangThai, ghiChu
        BeanUtils.copyProperties(request, entity, "nhaCungCapId", "nhanVienId");
        entity.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        return phieuNhapKhoRepository.save(entity);
    }

    public PhieuNhapKho update(Integer id, PhieuNhapKhoRequest request) {
        PhieuNhapKho entity = getById(id);
        BeanUtils.copyProperties(request, entity, "phieuNhapId", "nhaCungCapId", "nhanVienId");
        entity.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        return phieuNhapKhoRepository.save(entity);
    }

    // Xóa cả dòng chi tiết trước rồi mới xóa phiếu — phiếu nhập không cascade sang tồn kho/serial
    // (chỉ là chứng từ chi phí đối chiếu NCC), nên xóa an toàn, chỉ cần dọn FK chi_tiet_phieu_nhap.
    @Transactional
    public void delete(Integer id) {
        if (!phieuNhapKhoRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu nhập kho không tồn tại với id: " + id);
        chiTietPhieuNhapRepository.deleteByPhieuNhapKho_PhieuNhapId(id);
        phieuNhapKhoRepository.deleteById(id);
    }
}
