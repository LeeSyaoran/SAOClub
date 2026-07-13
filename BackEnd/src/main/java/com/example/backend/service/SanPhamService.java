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

    // Chuỗi rỗng ("") không khớp ":keyword IS NULL" trong JPQL — chuẩn hóa về null ở đây
    // để ô tìm kiếm trống trên frontend không vô tình lọc mất tất cả kết quả.
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

    public SanPham createSanPham(SanPhamRequest request) {
        // Tạo SanPham — BeanUtils chỉ copy field có cùng tên & kiểu (tenSanPham, loaiSanPham, moTa, hinhAnhChinh, trangThai)
        SanPham sanPham = new SanPham();
        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao");
        sanPham.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());

        // Xử lý khóa ngoại SanPham
        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        if (request.getNhaCungCapId() != null)
            sanPham.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));

        SanPham saved = sanPhamRepository.save(sanPham);

        // Tạo BienTheSanPham — BeanUtils copy: maSku, giaBan, giaNhap, baoHanhThang, mauSac,
        // kichThuocManHinh, heDieuHanh, pin, trongLuongKg, trangThai, hinhAnhBienThe
        BienTheSanPham bt = new BienTheSanPham();
        BeanUtils.copyProperties(request, bt, "bienTheId");
        bt.setSanPham(saved);

        // Xử lý khóa ngoại linh kiện (cpuId/ramId/oCungId/gpuId khác tên với cpu/ram/oCung/gpu nên không bị copy)
        bt.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        bt.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        bt.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        bt.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        bienTheSanPhamRepository.save(bt);

        return saved;
    }

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

        // Cập nhật variant nếu bienTheId được truyền
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

    // Sản phẩm đã có biến thể nào qua giao dịch chưa — dùng để FE hỏi trước khi hiện hộp
    // thoại xóa.
    public boolean hasTransactionHistory(Integer sanPhamId) {
        return bienTheSanPhamRepository.findBySanPham_SanPhamId(sanPhamId).stream()
                .anyMatch(bt -> bienTheSanPhamService.hasTransactionHistory(bt.getBienTheId()));
    }

    // Xóa từng biến thể trước (tái dùng guard "chưa từng giao dịch" của
    // BienTheSanPhamService.delete() — nếu bất kỳ biến thể nào đã bán/bảo hành, toàn bộ
    // giao dịch bị hủy, không xóa dở dang) rồi mới xóa sản phẩm.
    @Transactional
    public void deleteSanPham(Integer sanPhamId) {
        if (!sanPhamRepository.existsById(sanPhamId)) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId);
        }
        for (BienTheSanPham bt : bienTheSanPhamRepository.findBySanPham_SanPhamId(sanPhamId)) {
            bienTheSanPhamService.delete(bt.getBienTheId());
        }
        sanPhamRepository.deleteById(sanPhamId);
    }
}
