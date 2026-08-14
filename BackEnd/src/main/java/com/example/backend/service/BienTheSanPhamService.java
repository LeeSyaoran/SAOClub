package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.repository.*;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamPublicResponse;
import com.example.backend.response.BienTheSanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BienTheSanPhamService {

    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;
    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    public List<BienTheSanPhamResponse> hienThiBienTheSanPham() {
        return bienTheSanPhamRepository.hienThiBienTheSanPham();
    }

    public Page<BienTheSanPhamResponse> hienThiBienTheSanPham(Pageable pageable) {
        return bienTheSanPhamRepository.hienThiBienTheSanPham(pageable);
    }

    public List<BienTheSanPhamPublicResponse> hienThiBienTheSanPhamPublic() {
        return bienTheSanPhamRepository.hienThiBienTheSanPhamPublic();
    }

    public BienTheSanPhamPublicResponse getPublicById(Integer id) {
        return bienTheSanPhamRepository.findPublicById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPhamResponse getResponseById(Integer id) {
        return bienTheSanPhamRepository.findResponseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPham getById(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    @Transactional
    public BienTheSanPham create(BienTheSanPhamRequest request) {
        BienTheSanPham entity = new BienTheSanPham();
        BeanUtils.copyProperties(request, entity, "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        return bienTheSanPhamRepository.save(entity);
    }

    @Transactional
    public BienTheSanPham update(Integer id, BienTheSanPhamRequest request) {
        BienTheSanPham entity = getById(id);

        Integer sanPhamId = entity.getSanPham().getSanPhamId();
        String oldMaSku = entity.getMaSku();
        BigDecimal oldGiaNhap = entity.getGiaNhap();
        BigDecimal oldGiaBan = entity.getGiaBan();
        Integer oldBaoHanhThang = entity.getBaoHanhThang();
        String oldHinhAnhBienThe = entity.getHinhAnhBienThe();
        String oldTrangThai = entity.getTrangThai();
        String oldMauSac = entity.getMauSac();
        Integer oldCpuId = entity.getCpu() != null ? entity.getCpu().getCpuId() : null;
        Integer oldRamId = entity.getRam() != null ? entity.getRam().getRamId() : null;
        Integer oldOCungId = entity.getOCung() != null ? entity.getOCung().getOCungId() : null;
        Integer oldGpuId = entity.getGpu() != null ? entity.getGpu().getGpuId() : null;
        String oldKichThuocManHinh = entity.getKichThuocManHinh();
        String oldHeDieuHanh = entity.getHeDieuHanh();
        String oldPin = entity.getPin();
        BigDecimal oldTrongLuongKg = entity.getTrongLuongKg();

        BeanUtils.copyProperties(request, entity, "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);

        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "maSku", oldMaSku, saved.getMaSku(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaNhap", oldGiaNhap, saved.getGiaNhap(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaBan", oldGiaBan, saved.getGiaBan(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "baoHanhThang", oldBaoHanhThang, saved.getBaoHanhThang(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "hinhAnhBienThe", oldHinhAnhBienThe, saved.getHinhAnhBienThe(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trangThai", oldTrangThai, saved.getTrangThai(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "mauSac", oldMauSac, saved.getMauSac(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "cpuId", oldCpuId, request.getCpuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "ramId", oldRamId, request.getRamId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "oCungId", oldOCungId, request.getOCungId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "gpuId", oldGpuId, request.getGpuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "kichThuocManHinh", oldKichThuocManHinh, saved.getKichThuocManHinh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "heDieuHanh", oldHeDieuHanh, saved.getHeDieuHanh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "pin", oldPin, saved.getPin(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trongLuongKg", oldTrongLuongKg, saved.getTrongLuongKg(), nguoiSua);

        return saved;
    }

}
