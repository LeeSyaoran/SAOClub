package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhieuBaoHanhService {

    @Autowired
    private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh() {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh();
    }

    public Page<PhieuBaoHanhResponse> hienThiPhieuBaoHanh(Pageable pageable) {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh(pageable);
    }

    public PhieuBaoHanh getById(Integer id) {
        return phieuBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id));
    }

    // Trước đây không kiểm tra thứ tự các mốc ngày — có thể lưu ngày hết bảo hành sớm hơn
    // ngày mua, hoặc ngày trả khách sớm hơn ngày tiếp nhận sửa, do nhập tay sai hoặc bug UI.
    private void kiemTraKhoangNgayHopLe(PhieuBaoHanhRequest request) {
        if (!request.getNgayHetBh().isAfter(request.getNgayMua()))
            throw new IllegalArgumentException("Ngày hết bảo hành phải sau ngày mua");
        if (request.getNgayTiepNhan() != null && request.getNgayTiepNhan().isBefore(request.getNgayMua()))
            throw new IllegalArgumentException("Ngày tiếp nhận không thể trước ngày mua");
        if (request.getNgayTraKhach() != null && request.getNgayTiepNhan() != null
                && request.getNgayTraKhach().isBefore(request.getNgayTiepNhan()))
            throw new IllegalArgumentException("Ngày trả khách không thể trước ngày tiếp nhận");
    }

    public PhieuBaoHanh create(PhieuBaoHanhRequest request) {
        kiemTraKhoangNgayHopLe(request);
        PhieuBaoHanh entity = new PhieuBaoHanh();
        // BeanUtils copies: ngayMua, ngayHetBh, ngayTiepNhan, ngayTraKhach,
        //                   moTaLoi, ketQuaXuLy, trangThai, chiPhiPhatSinh, ghiChu
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        PhieuBaoHanh saved = phieuBaoHanhRepository.save(entity);
        capNhatSerialTheoTrangThai(null, saved);
        return saved;
    }

    public PhieuBaoHanh update(Integer id, PhieuBaoHanhRequest request) {
        kiemTraKhoangNgayHopLe(request);
        PhieuBaoHanh entity = getById(id);
        String trangThaiCu = entity.getTrangThai();
        BeanUtils.copyProperties(request, entity, "baoHanhId", "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        PhieuBaoHanh saved = phieuBaoHanhRepository.save(entity);
        capNhatSerialTheoTrangThai(trangThaiCu, saved);
        return saved;
    }

    private static final java.util.Set<String> TRANG_THAI_DA_DONG =
            java.util.Set.of("da_xu_ly", "het_bao_hanh", "tu_choi");

    // Serial cua khach dang o xuong sua (trang_thai = "loi_bao_hanh", da co san trong DB
    // CHECK constraint nhung truoc gio khong noi nao gan) — gan khi phieu VUA chuyen sang
    // "dang_xu_ly" (may dang thuc su o cua hang), tra lai "da_ban" khi phieu VUA dong (da_xu_ly/
    // het_bao_hanh/tu_choi — sua xong, tu choi, hay het han deu tra may lai cho khach nhu nhau).
    // Khong dung toi ton_kho: serial khach da mua khong nam trong kho ban, chi la trang thai
    // hien thi cho nhan vien biet may nao dang "ket" o xuong sua.
    // ponytail: khong khoa cheo voi luong tra hang — neu cung 1 serial vua duoc mo phieu tra
    // hang song song (hiem), phieu bao hanh dong sau co the ghi de "da_ban" len tren "trong_kho".
    private void capNhatSerialTheoTrangThai(String trangThaiCu, PhieuBaoHanh phieu) {
        ChiTietSanPham serial = phieu.getChiTietSanPham();
        if (serial == null) return;

        boolean vuaVaoXuLy = "dang_xu_ly".equals(phieu.getTrangThai()) && !"dang_xu_ly".equals(trangThaiCu);
        boolean vuaDong = TRANG_THAI_DA_DONG.contains(phieu.getTrangThai())
                && !TRANG_THAI_DA_DONG.contains(trangThaiCu);

        if (vuaVaoXuLy) {
            serial.setTrangThai("loi_bao_hanh");
            chiTietSanPhamRepository.save(serial);
        } else if (vuaDong) {
            serial.setTrangThai("da_ban");
            chiTietSanPhamRepository.save(serial);
        }
    }

    public void delete(Integer id) {
        if (!phieuBaoHanhRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id);
        phieuBaoHanhRepository.deleteById(id);
    }
}
