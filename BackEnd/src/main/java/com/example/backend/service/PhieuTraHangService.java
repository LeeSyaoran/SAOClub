package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.PhieuTraHangRequest;
import com.example.backend.response.PhieuTraHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PhieuTraHangService {

    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<PhieuTraHangResponse> hienThiPhieuTraHang() {
        return phieuTraHangRepository.hienThiPhieuTraHang();
    }

    public PhieuTraHang getById(Integer id) {
        return phieuTraHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + id));
    }

    @Transactional
    public PhieuTraHang create(PhieuTraHangRequest request) {
        PhieuTraHang entity = new PhieuTraHang();
        // BeanUtils copies: lyDo, ngayTra, trangThai, soTienHoan, hinhThucHoan, ghiChu
        BeanUtils.copyProperties(request, entity, "donHangId", "nhanVienId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        PhieuTraHang saved = phieuTraHangRepository.save(entity);
        congViNeuVuaHoanTat(null, saved);
        return saved;
    }

    @Transactional
    public PhieuTraHang update(Integer id, PhieuTraHangRequest request) {
        PhieuTraHang entity = getById(id);
        String trangThaiCu = entity.getTrangThai();
        chanSuaSauKhiDaCongVi(entity, request);
        BeanUtils.copyProperties(request, entity, "phieuTraId", "donHangId", "nhanVienId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        PhieuTraHang saved = phieuTraHangRepository.save(entity);
        congViNeuVuaHoanTat(trangThaiCu, saved);
        return saved;
    }

    // Guard: một khi phiếu đã hoàn tiền qua ví (trang_thai="da_xu_ly" + hinh_thuc_hoan="vi"),
    // chặn sửa trangThai/hinhThucHoan/soTienHoan — 3 trường này quyết định số dư ví, sửa "ngầm"
    // sẽ làm lệch ví (không có bảng ledger để đối soát lại). Các trường khác (lyDo, ghiChu,
    // nhanVienId, ngayTra) vẫn sửa tự do.
    private void chanSuaSauKhiDaCongVi(PhieuTraHang entity, PhieuTraHangRequest request) {
        boolean daCongViQua = "da_xu_ly".equals(entity.getTrangThai()) && "vi".equals(entity.getHinhThucHoan());
        if (!daCongViQua) return;

        boolean doiTrangThai = !"da_xu_ly".equals(request.getTrangThai());
        boolean doiHinhThucHoan = !"vi".equals(request.getHinhThucHoan());
        boolean doiSoTienHoan = entity.getSoTienHoan() == null
                ? request.getSoTienHoan() != null
                : entity.getSoTienHoan().compareTo(request.getSoTienHoan()) != 0;

        if (doiTrangThai || doiHinhThucHoan || doiSoTienHoan) {
            throw new IllegalArgumentException(
                    "Phiếu đã hoàn tiền qua ví — không thể đổi trạng thái/hình thức hoàn/số tiền hoàn nữa");
        }
    }

    // Cộng tiền vào ví khách hàng khi phiếu VỪA chuyển sang "da_xu_ly" (trạng thái cũ khác
    // "da_xu_ly" — tránh cộng 2 lần nếu sửa 1 phiếu đã xử lý) và hình thức hoàn là "vi".
    // Hoàn "tien_mat" không đụng ví — nhân viên tự đưa tiền mặt ngoài hệ thống.
    private void congViNeuVuaHoanTat(String trangThaiCu, PhieuTraHang phieu) {
        boolean vuaChuyenSangDaXuLy = "da_xu_ly".equals(phieu.getTrangThai()) && !"da_xu_ly".equals(trangThaiCu);
        if (!vuaChuyenSangDaXuLy) return;
        if (!"vi".equals(phieu.getHinhThucHoan())) return;
        if (phieu.getSoTienHoan() == null || phieu.getSoTienHoan().signum() <= 0) return;

        KhachHang khachHang = phieu.getDonHang().getKhachHang();
        khachHang.setSoDuVi(khachHang.getSoDuVi().add(phieu.getSoTienHoan()));
        khachHangRepository.save(khachHang);
    }

    public void delete(Integer id) {
        if (!phieuTraHangRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + id);
        phieuTraHangRepository.deleteById(id);
    }
}
