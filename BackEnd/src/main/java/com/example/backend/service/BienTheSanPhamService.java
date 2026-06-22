package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.repository.*;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<BienTheSanPhamResponse> hienThiBienTheSanPham() {
        return bienTheSanPhamRepository.hienThiBienTheSanPham();
    }

    public BienTheSanPham getById(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPham create(BienTheSanPhamRequest request) {
        BienTheSanPham entity = new BienTheSanPham();
        // BeanUtils copies: maSku, giaNhap, giaBan, baoHanhThang, hinhAnhBienThe, trangThai, mauSac,
        //                   kichThuocManHinh, heDieuHanh, pin, trongLuongKg,
        //                   manHinhDt, cameraSau, cameraTruoc, dungLuongPinDt,
        //                   boNhoTrongDt, chipXuLyDt, soSim
        // Bỏ qua: sanPhamId, cpuId, ramId, oCungId, gpuId (khác tên với entity)
        BeanUtils.copyProperties(request, entity, "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        return bienTheSanPhamRepository.save(entity);
    }

    public BienTheSanPham update(Integer id, BienTheSanPhamRequest request) {
        BienTheSanPham entity = getById(id);
        BeanUtils.copyProperties(request, entity, "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        return bienTheSanPhamRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!bienTheSanPhamRepository.existsById(id))
            throw new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id);
        bienTheSanPhamRepository.deleteById(id);
    }
}
