package com.example.backend.service;

import com.example.backend.entity.DonHang;
import com.example.backend.repository.*;
import com.example.backend.request.DonHangRequest;
import com.example.backend.response.DonHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DonHangService {

    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;
    @Autowired
    private DiaChiGiaoHangRepository diaChiGiaoHangRepository;

    public List<DonHangResponse> hienThiDonHang() {
        return donHangRepository.hienThiDonHang();
    }

    public DonHang getById(Integer id) {
        return donHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id));
    }

    public DonHang create(DonHangRequest request) {
        DonHang entity = new DonHang();
        // BeanUtils copies: maDonHang, diaChiGiaoHangText, nguoiNhan, sdtNguoiNhan,
        //                   tongTien, giamGia, phiVanChuyen, thanhTien, ngayDat,
        //                   ngayGiaoDuKien, ngayGiaoThucTe, trangThaiDonHang,
        //                   trangThaiThanhToan, kenhBan, ghiChu
        // Bỏ qua: khachHangId, nhanVienId, khuyenMaiId, diaChiGiaoHangId (khác tên với entity)
        BeanUtils.copyProperties(request, entity,
                "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId");

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        // nhanVienId nullable: nhân viên không bắt buộc (đơn hàng online)
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        if (request.getKhuyenMaiId() != null)
            entity.setKhuyenMai(khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()));
        if (request.getDiaChiGiaoHangId() != null)
            entity.setDiaChiGiaoHang(diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()));

        return donHangRepository.save(entity);
    }

    public DonHang update(Integer id, DonHangRequest request) {
        DonHang entity = getById(id);
        BeanUtils.copyProperties(request, entity,
                "id", "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId");

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        entity.setKhuyenMai(request.getKhuyenMaiId() != null
                ? khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()) : null);
        entity.setDiaChiGiaoHang(request.getDiaChiGiaoHangId() != null
                ? diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()) : null);

        return donHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!donHangRepository.existsById(id))
            throw new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id);
        donHangRepository.deleteById(id);
    }
}
