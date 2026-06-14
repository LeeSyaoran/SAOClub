package com.example.backend.service;

import com.example.backend.entity.DanhMuc;
import com.example.backend.entity.NhaCungCap;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.ThuongHieu;
import com.example.backend.repository.DanhMucRepository;
import com.example.backend.repository.NhaCungCapRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.ThuongHieuRepository;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final NhaCungCapRepository nhaCungCapRepository;

    @Autowired
    public SanPhamService(SanPhamRepository sanPhamRepository,
                          ThuongHieuRepository thuongHieuRepository,
                          DanhMucRepository danhMucRepository,
                          NhaCungCapRepository nhaCungCapRepository) {
        this.sanPhamRepository = sanPhamRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.danhMucRepository = danhMucRepository;
        this.nhaCungCapRepository = nhaCungCapRepository;
    }

    public List<SanPhamResponse> hienThiSanPham() {
        return sanPhamRepository.hienThiSanPham();
    }

    public SanPham getSanPhamById(Integer sanPhamId) {
        return sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));
    }

    public void createSanPham(SanPhamRequest request) {
        SanPham sanPham = new SanPham();
        BeanUtils.copyProperties(request, sanPham, "sanPhamId");
        if (request.getThuongHieuId() != null) {
            sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        }
        if (request.getDanhMucId() != null) {
            sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        }
        if (request.getNhaCungCapId() != null) {
            sanPham.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));
        }
        sanPhamRepository.save(sanPham);
    }

    public void updateSanPham(Integer sanPhamId, SanPhamRequest request) {
        sanPhamRepository.findById(sanPhamId).ifPresent(sanPham -> {
            BeanUtils.copyProperties(request, sanPham, "sanPhamId");
            if (request.getThuongHieuId() != null) {
                sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
            }
            if (request.getDanhMucId() != null) {
                sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
            }
            if (request.getNhaCungCapId() != null) {
                sanPham.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));
            }
            sanPhamRepository.save(sanPham);
        });
    }

    public void deleteSanPham(Integer sanPhamId) {
        if (!sanPhamRepository.existsById(sanPhamId)) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId);
        }
        sanPhamRepository.deleteById(sanPhamId);
    }

    private ThuongHieu findThuongHieu(Integer id) {
        return thuongHieuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại với id: " + id));
    }

    private DanhMuc findDanhMuc(Integer id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với id: " + id));
    }

    private NhaCungCap findNhaCungCap(Integer id) {
        return nhaCungCapRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhà cung cấp không tồn tại với id: " + id));
    }
}
