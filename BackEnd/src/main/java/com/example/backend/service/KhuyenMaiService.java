package com.example.backend.service;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.entity.KhuyenMaiSanPham;
import com.example.backend.entity.SanPham;
import com.example.backend.repository.KhuyenMaiRepository;
import com.example.backend.repository.KhuyenMaiSanPhamRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.request.KhuyenMaiRequest;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KhuyenMaiService {

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;

    @Autowired
    private KhuyenMaiSanPhamRepository khuyenMaiSanPhamRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    public List<KhuyenMaiResponse> hienThiKhuyenMai() {
        return khuyenMaiRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Kiểm tra tính hợp lệ của mã khuyến mãi
    public KhuyenMai kiemTraMaKhuyenMai(String maKhuyenMai) {
        if (maKhuyenMai == null || maKhuyenMai.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập mã khuyến mãi");
        KhuyenMai km = khuyenMaiRepository.findByMaKhuyenMaiIgnoreCase(maKhuyenMai.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));
        if (!"active".equals(km.getTrangThai()))
            throw new IllegalArgumentException("Mã khuyến mãi không còn hiệu lực (đã ngừng hoạt động)");
        LocalDateTime now = LocalDateTime.now();
        if (km.getNgayBatDau() != null && now.isBefore(km.getNgayBatDau()))
            throw new IllegalArgumentException("Mã khuyến mãi chưa đến thời gian áp dụng");
        if (km.getNgayKetThuc() != null && now.isAfter(km.getNgayKetThuc()))
            throw new IllegalArgumentException("Mã khuyến mãi đã hết hạn");
        int daDung = km.getSoLanDaDung() != null ? km.getSoLanDaDung() : 0;
        if (km.getSoLuongToiDa() != null && daDung >= km.getSoLuongToiDa())
            throw new IllegalArgumentException("Mã khuyến mãi đã hết lượt sử dụng");
        return km;
    }

    public KhuyenMai getById(Integer id) {
        return khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khuyến mãi không tồn tại với id: " + id));
    }

    @Transactional
    public KhuyenMai create(KhuyenMaiRequest request) {
        KhuyenMai entity = new KhuyenMai();
        BeanUtils.copyProperties(request, entity, "khuyenMaiId", "ngayTao", "soLanDaDung", "sanPhamIds");
        entity.setNgayTao(LocalDateTime.now());
        entity.setSoLanDaDung(0);
        KhuyenMai saved = khuyenMaiRepository.save(entity);

        // Lưu danh sách sản phẩm áp dụng
        luuSanPhamApDung(saved.getKhuyenMaiId(), request.getSanPhamIds());

        return saved;
    }

    @Transactional
    public KhuyenMai update(Integer id, KhuyenMaiRequest request) {
        KhuyenMai entity = getById(id);
        BeanUtils.copyProperties(request, entity, "khuyenMaiId", "ngayTao", "soLanDaDung", "sanPhamIds");
        KhuyenMai saved = khuyenMaiRepository.save(entity);

        // Cập nhật danh sách sản phẩm áp dụng
        luuSanPhamApDung(saved.getKhuyenMaiId(), request.getSanPhamIds());

        return saved;
    }

    /**
     * Lấy danh sách sản phẩm áp dụng cho khuyến mãi
     */
    public List<KhuyenMaiResponse.SanPhamSimpleResponse> getSanPhamApDung(Integer khuyenMaiId) {
        List<KhuyenMaiSanPham> list = khuyenMaiSanPhamRepository.findByKhuyenMai_KhuyenMaiId(khuyenMaiId);
        return list.stream()
                .map(kmsp -> {
                    SanPham sp = kmsp.getSanPham();
                    return new KhuyenMaiResponse.SanPhamSimpleResponse(
                            sp.getSanPhamId(),
                            sp.getTenSanPham(),
                            sp.getHinhAnhChinh()
                    );
                })
                .collect(Collectors.toList());
    }

    // Kiểm tra khuyến mãi có áp dụng cho sản phẩm hay không
    public boolean kiemTraSanPhamApDung(Integer khuyenMaiId, Integer sanPhamId) {
        // Nếu không có sản phẩm nào trong bảng khuyen_mai_san_pham → áp dụng cho tất cả
        if (!khuyenMaiSanPhamRepository.existsByKhuyenMai_KhuyenMaiId(khuyenMaiId)) {
            return true;
        }
        // Kiểm tra sản phẩm có trong danh sách cho phép không
        return khuyenMaiSanPhamRepository.existsByKhuyenMaiIdAndSanPhamId(khuyenMaiId, sanPhamId);
    }

    private void luuSanPhamApDung(Integer khuyenMaiId, List<Integer> sanPhamIds) {
        // Xóa tất cả sản phẩm cũ
        khuyenMaiSanPhamRepository.deleteByKhuyenMaiId(khuyenMaiId);

        // Thêm danh sách mới nếu có
        if (sanPhamIds != null && !sanPhamIds.isEmpty()) {
            KhuyenMai khuyenMai = khuyenMaiRepository.getReferenceById(khuyenMaiId);
            for (Integer sanPhamId : sanPhamIds) {
                SanPham sanPham = sanPhamRepository.getReferenceById(sanPhamId);
                KhuyenMaiSanPham kmsp = new KhuyenMaiSanPham();
                kmsp.setKhuyenMai(khuyenMai);
                kmsp.setSanPham(sanPham);
                khuyenMaiSanPhamRepository.save(kmsp);
            }
        }
    }

    private KhuyenMaiResponse toResponse(KhuyenMai km) {
        KhuyenMaiResponse resp = new KhuyenMaiResponse();
        resp.setKhuyenMaiId(km.getKhuyenMaiId());
        resp.setMaKhuyenMai(km.getMaKhuyenMai());
        resp.setTenKhuyenMai(km.getTenKhuyenMai());
        resp.setLoai(km.getLoai());
        resp.setGiaTri(km.getGiaTri());
        resp.setGiaTriToiDa(km.getGiaTriToiDa());
        resp.setDonHangToiThieu(km.getDonHangToiThieu());
        resp.setNgayBatDau(km.getNgayBatDau());
        resp.setNgayKetThuc(km.getNgayKetThuc());
        resp.setSoLuongToiDa(km.getSoLuongToiDa());
        resp.setSoLanDaDung(km.getSoLanDaDung());
        resp.setSoLuotConLai(km.getSoLuongToiDa() != null
                ? km.getSoLuongToiDa() - (km.getSoLanDaDung() != null ? km.getSoLanDaDung() : 0)
                : null);
        resp.setTrangThai(km.getTrangThai());
        resp.setNgayTao(km.getNgayTao());

        // Lấy danh sách sản phẩm áp dụng
        List<KhuyenMaiSanPham> sanPhams = khuyenMaiSanPhamRepository.findByKhuyenMai_KhuyenMaiId(km.getKhuyenMaiId());
        if (!sanPhams.isEmpty()) {
            resp.setSanPhams(sanPhams.stream()
                    .map(kmsp -> {
                        SanPham sp = kmsp.getSanPham();
                        return new KhuyenMaiResponse.SanPhamSimpleResponse(
                                sp.getSanPhamId(),
                                sp.getTenSanPham(),
                                sp.getHinhAnhChinh()
                        );
                    })
                    .collect(Collectors.toList()));
        }

        return resp;
    }
}
