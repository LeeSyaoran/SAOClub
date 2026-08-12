package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.WarrantyStatusResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChiTietSanPhamService {

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;

    public List<ChiTietSanPhamResponse> hienThiChiTietSanPham() {
        return chiTietSanPhamRepository.hienThiChiTietSanPham();
    }

    public List<ChiTietSanPhamResponse> getByBienTheId(Integer bienTheId) {
        return chiTietSanPhamRepository.findByBienTheId(bienTheId);
    }

    public ChiTietSanPham getById(Integer id) {
        return chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
    }

    public ChiTietSanPham create(ChiTietSanPhamRequest request) {
        ChiTietSanPham entity = new ChiTietSanPham();
        BeanUtils.copyProperties(request, entity, "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }

    @Transactional
    public ChiTietSanPham update(Integer id, ChiTietSanPhamRequest request) {
        ChiTietSanPham entity;
        if ("giu_hang".equals(request.getTrangThai())) {
            entity = chiTietSanPhamRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
            if (!"trong_kho".equals(entity.getTrangThai()))
                throw new IllegalArgumentException("Serial này vừa được giữ/bán bởi giao dịch khác, vui lòng chọn serial khác");
        } else {
            entity = getById(id);
        }
        BeanUtils.copyProperties(request, entity, "chiTietId", "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietSanPham entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa bán/chưa sử dụng)");
        }
        chiTietSanPhamRepository.deleteById(id);
    }

    public List<WarrantyStatusResponse> getStillUnderWarranty() {
        LocalDateTime now = LocalDateTime.now();
        List<WarrantyStatusResponse> list = chiTietSanPhamRepository.timSerialDaBanCoGiaoHang();
        list.forEach(w -> w.setNgayHetBaoHanh(w.getNgayGiaoThucTe().plusMonths(w.getBaoHanhThang() == null ? 0 : w.getBaoHanhThang())));
        list.removeIf(w -> !w.getNgayHetBaoHanh().isAfter(now));
        list.sort(Comparator.comparing(WarrantyStatusResponse::getNgayHetBaoHanh));
        return list;
    }
}
