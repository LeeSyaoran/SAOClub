package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.SanPham;
import com.example.backend.repository.*;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;
    @Autowired
    private DanhMucRepository danhMucRepository;
    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;
    @Autowired
    private BienTheSanPhamService bienTheSanPhamService;

    public Page<SanPhamResponse> hienThiSanPham(String keyword, Integer danhMucId,
                                                 Integer thuongHieuId, String trangThai,
                                                 Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return sanPhamRepository.hienThiSanPham(kw, danhMucId, thuongHieuId, trangThai, pageable);
    }

    public SanPham getSanPhamById(Integer sanPhamId) {
        return sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));
    }

    @Transactional
    public SanPham createSanPham(SanPhamRequest request) {
        SanPham sanPham = new SanPham();
        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao");
        sanPham.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        if (request.getNhaCungCapId() != null)
            sanPham.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));

        SanPham saved = sanPhamRepository.save(sanPham);

        BienTheSanPham bt = new BienTheSanPham();
        BeanUtils.copyProperties(request, bt, "bienTheId");
        bt.setSanPham(saved);

        bt.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        bt.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        bt.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        bt.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        bienTheSanPhamRepository.save(bt);

        return saved;
    }

    @Transactional
    public void updateSanPham(Integer sanPhamId, SanPhamRequest request) {
        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));

        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao");
        if (request.getNgayTao() != null) sanPham.setNgayTao(request.getNgayTao());

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        sanPham.setNhaCungCap(request.getNhaCungCapId() != null
                ? nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()) : null);

        sanPhamRepository.save(sanPham);

        if (request.getBienTheId() != null) {
            BienTheSanPham bt = bienTheSanPhamRepository.findById(request.getBienTheId())
                    .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại với id: " + request.getBienTheId()));

            BeanUtils.copyProperties(request, bt, "bienTheId");
            bt.setSanPham(sanPham);
            bt.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
            bt.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
            bt.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
            bt.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

            bienTheSanPhamRepository.save(bt);
        }
    }

    public boolean hasTransactionHistory(Integer sanPhamId) {
        return bienTheSanPhamRepository.hasTransactionHistoryBySanPhamId(sanPhamId);
    }

    @Transactional
    public void deleteSanPham(Integer sanPhamId) {
        if (!sanPhamRepository.existsById(sanPhamId))
            throw new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId);
        for (BienTheSanPham bt : bienTheSanPhamRepository.findBySanPham_SanPhamId(sanPhamId)) {
            bienTheSanPhamRepository.deleteById(bt.getBienTheId());
        }
        sanPhamRepository.deleteById(sanPhamId);
    }
}
