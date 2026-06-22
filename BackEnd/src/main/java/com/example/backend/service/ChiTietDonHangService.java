package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.response.ChiTietDonHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietDonHangService {

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<ChiTietDonHangResponse> hienThiChiTietDonHang() {
        return chiTietDonHangRepository.hienThiChiTietDonHang();
    }

    public ChiTietDonHang getById(Integer id) {
        return chiTietDonHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn hàng không tồn tại với id: " + id));
    }

    public ChiTietDonHang create(ChiTietDonHangRequest request) {
        ChiTietDonHang entity = new ChiTietDonHang();
        // BeanUtils copies: soLuong, donGia, giamGiaDong, ghiChu
        // Bỏ qua: donHangId, bienTheId, chiTietId (khác tên với entity)
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "chiTietId");

        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        // chiTietId (ChiTietSanPham) có thể null — theo dõi serial cụ thể (optional)
        if (request.getChiTietId() != null)
            entity.setChiTietSanPham(chiTietSanPhamRepository.getReferenceById(request.getChiTietId()));

        return chiTietDonHangRepository.save(entity);
    }

    public ChiTietDonHang update(Integer id, ChiTietDonHangRequest request) {
        ChiTietDonHang entity = getById(id);
        BeanUtils.copyProperties(request, entity, "id", "donHangId", "bienTheId", "chiTietId");

        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);

        return chiTietDonHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!chiTietDonHangRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết đơn hàng không tồn tại với id: " + id);
        chiTietDonHangRepository.deleteById(id);
    }
}
