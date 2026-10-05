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
    @Autowired
    private PhieuTraHangService phieuTraHangService;
    @Autowired
    private ChiTietDonHangService chiTietDonHangService;

    @Transactional
    public List<ChiTietTraHangResponse> hienThiChiTietTraHang() {
        List<ChiTietTraHangResponse> list = chiTietTraHangRepository.hienThiChiTietTraHang();
        boolean hasMissingOrMulti = list.stream()
                .anyMatch(r -> r.getSoSerial() == null || (r.getSoLuong() != null && r.getSoLuong() > 1));
        if (!hasMissingOrMulti) {
            return list;
        }
        List<ChiTietTraHang> entities = chiTietTraHangRepository.findAll();
        java.util.Map<Integer, ChiTietTraHang> entityMap = new java.util.HashMap<>();
        for (ChiTietTraHang e : entities) {
            entityMap.put(e.getId(), e);
        }
        for (ChiTietTraHangResponse r : list) {
            if (r.getSoSerial() != null && (r.getSoLuong() == null || r.getSoLuong() <= 1)) {
                continue;
            }
            ChiTietTraHang e = entityMap.get(r.getId());
            if (e != null && e.getPhieuTraHang() != null && e.getPhieuTraHang().getDonHang() != null && e.getBienThe() != null) {
                int count = e.getSoLuong() != null && e.getSoLuong() > 0 ? e.getSoLuong() : 1;
                List<com.example.backend.entity.ChiTietSanPham> matchedSerials = new java.util.ArrayList<>();
                for (com.example.backend.entity.ChiTietDonHang line : chiTietDonHangRepository.findEntityByDonHangId(e.getPhieuTraHang().getDonHang().getId())) {
                    if (line.getBienThe() != null && e.getBienThe().getBienTheId().equals(line.getBienThe().getBienTheId())) {
                        List<com.example.backend.entity.ChiTietSanPham> ls = chiTietDonHangService.ensureAndGetSerialsForOrderLine(line);
                        for (com.example.backend.entity.ChiTietSanPham s : ls) {
                            if (matchedSerials.size() < count) {
                                matchedSerials.add(s);
                            }
                        }
                    }
                }
                if (!matchedSerials.isEmpty()) {
                    if (e.getChiTietSanPham() == null) {
                        e.setChiTietSanPham(matchedSerials.get(0));
                        chiTietTraHangRepository.save(e);
                    }
                    r.setChiTietId(matchedSerials.get(0).getChiTietId());
                    r.setSoSerial(matchedSerials.stream()
                            .map(com.example.backend.entity.ChiTietSanPham::getSoSerial)
                            .filter(java.util.Objects::nonNull)
                            .collect(java.util.stream.Collectors.joining(", ")));
                }
            }
        }
        return list;
    }

    public ChiTietTraHang getById(Integer id) {
        return chiTietTraHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id));
    }

    private com.example.backend.entity.ChiTietSanPham resolveSerialFromOrder(DonHang donHang, Integer bienTheId) {
        if (donHang == null || bienTheId == null) return null;
        for (com.example.backend.entity.ChiTietDonHang line : chiTietDonHangRepository.findEntityByDonHangId(donHang.getId())) {
            if (line.getBienThe() != null && bienTheId.equals(line.getBienThe().getBienTheId())) {
                List<com.example.backend.entity.ChiTietSanPham> serials = chiTietDonHangService.ensureAndGetSerialsForOrderLine(line);
                if (!serials.isEmpty()) {
                    return serials.get(0);
                }
            }
        }
        return null;
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
        } else {
            entity.setChiTietSanPham(resolveSerialFromOrder(phieu.getDonHang(), request.getBienTheId()));
        }
        ChiTietTraHang saved = chiTietTraHangRepository.save(entity);
        if ("da_xu_ly".equals(phieu.getTrangThai())) {
            phieuTraHangService.hoanKhoChoDongTra(phieu, saved);
        }
        return saved;
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
        if (request.getChiTietId() != null) {
            entity.setChiTietSanPham(chiTietSanPhamRepository.getReferenceById(request.getChiTietId()));
        } else if (entity.getChiTietSanPham() == null) {
            entity.setChiTietSanPham(resolveSerialFromOrder(phieu.getDonHang(), request.getBienTheId()));
        }
        ChiTietTraHang saved = chiTietTraHangRepository.save(entity);
        if ("da_xu_ly".equals(phieu.getTrangThai())) {
            phieuTraHangService.hoanKhoChoDongTra(phieu, saved);
        }
        return saved;
    }

    public void delete(Integer id) {
        if (!chiTietTraHangRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id);
        chiTietTraHangRepository.deleteById(id);
    }
}
