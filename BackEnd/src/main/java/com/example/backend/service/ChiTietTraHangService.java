package com.example.backend.service;

import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.ChiTietTraHangRequest;
import com.example.backend.response.ChiTietTraHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChiTietTraHangService {

    @Autowired
    private ChiTietTraHangRepository chiTietTraHangRepository;
    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    public List<ChiTietTraHangResponse> hienThiChiTietTraHang() {
        return chiTietTraHangRepository.hienThiChiTietTraHang();
    }

    public ChiTietTraHang getById(Integer id) {
        return chiTietTraHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id));
    }

    @Transactional
    public ChiTietTraHang create(ChiTietTraHangRequest request) {
        PhieuTraHang phieu = phieuTraHangRepository.findById(request.getPhieuTraId())
                .orElseThrow(() -> new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + request.getPhieuTraId()));
        kiemTraSoLuongTraKhongVuotMua(phieu.getDonHang(), request.getBienTheId(), null, request.getSoLuong());

        ChiTietTraHang entity = new ChiTietTraHang();
        BeanUtils.copyProperties(request, entity, "phieuTraId", "bienTheId", "chiTietId");
        entity.setPhieuTraHang(phieu);
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        if (request.getChiTietId() != null) {
            entity.setChiTietSanPham(chiTietSanPhamRepository.getReferenceById(request.getChiTietId()));
        }
        return chiTietTraHangRepository.save(entity);
    }

    private void kiemTraSoLuongTraKhongVuotMua(DonHang donHang, Integer bienTheId, Integer excludeChiTietTraId, Integer soLuongMoi) {
        if (soLuongMoi == null || soLuongMoi <= 0) return;

        int tongDaMua = chiTietDonHangRepository.findEntityByDonHangId(donHang.getId()).stream()
                .filter(item -> bienTheId.equals(item.getBienThe().getBienTheId()))
                .mapToInt(item -> item.getSoLuong() != null ? item.getSoLuong() : 0)
                .sum();

        int tongDaTraCacDongKhac = phieuTraHangRepository.findByDonHang_Id(donHang.getId()).stream()
                .filter(p -> !"tu_choi".equals(p.getTrangThai()))
                .flatMap(p -> chiTietTraHangRepository.findByPhieuTraHang_PhieuTraId(p.getPhieuTraId()).stream())
                .filter(c -> !c.getId().equals(excludeChiTietTraId))
                .filter(c -> bienTheId.equals(c.getBienThe().getBienTheId()))
                .mapToInt(c -> c.getSoLuong() != null ? c.getSoLuong() : 0)
                .sum();

        if (tongDaTraCacDongKhac + soLuongMoi > tongDaMua)
            throw new IllegalArgumentException(
                    "Số lượng trả vượt quá số đã mua — đã mua " + tongDaMua
                            + ", đã trả " + tongDaTraCacDongKhac + ", muốn trả thêm " + soLuongMoi);
    }

    @Transactional
    public ChiTietTraHang update(Integer id, ChiTietTraHangRequest request) {
        ChiTietTraHang entity = getById(id);
        PhieuTraHang phieu = phieuTraHangRepository.findById(request.getPhieuTraId())
                .orElseThrow(() -> new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + request.getPhieuTraId()));
        kiemTraSoLuongTraKhongVuotMua(phieu.getDonHang(), request.getBienTheId(), id, request.getSoLuong());

        BeanUtils.copyProperties(request, entity, "id", "phieuTraId", "bienTheId", "chiTietId");
        entity.setPhieuTraHang(phieu);
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        return chiTietTraHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!chiTietTraHangRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id);
        chiTietTraHangRepository.deleteById(id);
    }
}
