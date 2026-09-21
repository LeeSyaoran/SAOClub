package com.example.backend.controller;

import com.example.backend.entity.SanPham;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamCreatedResponse;
import com.example.backend.response.SanPhamResponse;
import com.example.backend.response.LichSuThayDoiSanPhamResponse;
import com.example.backend.service.SanPhamService;
import com.example.backend.service.LichSuThayDoiSanPhamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/san-pham")
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    // Lấy danh sách sản phẩm phân trang và tìm kiếm
    @GetMapping("hien-thi")
    public Page<SanPhamResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer thuongHieuId,
            @RequestParam(required = false) String trangThai) {
        return sanPhamService.hienThiSanPham(keyword, danhMucId, thuongHieuId, trangThai, PageRequest.of(page, size));
    }

    // Lấy thông tin chi tiết sản phẩm theo ID
    @GetMapping("/{id}")
    public SanPham getById(@PathVariable Integer id) {
        return sanPhamService.getSanPhamById(id);
    }

    // Tạo mới sản phẩm kèm biến thể mặc định
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<SanPhamCreatedResponse> create(@Valid @RequestBody SanPhamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sanPhamService.createSanPham(request));
    }

    // Cập nhật thông tin sản phẩm và biến thể
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody SanPhamRequest request) {
        sanPhamService.updateSanPham(id, request);
        return ResponseEntity.ok().build();
    }

    // Xóa sản phẩm theo ID
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sanPhamService.deleteSanPham(id);
        return ResponseEntity.noContent().build();
    }

    // Kiểm tra sản phẩm đã có lịch sử giao dịch hay chưa
    @GetMapping("/{id}/co-giao-dich")
    public boolean hasTransactionHistory(@PathVariable Integer id) {
        return sanPhamService.hasTransactionHistory(id);
    }

    // Lấy danh sách ảnh gallery của sản phẩm
    @GetMapping("/{id}/hinh-anh")
    public List<String> getHinhAnh(@PathVariable Integer id) {
        return sanPhamService.layDanhSachHinhAnh(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}/lich-su")
    public List<LichSuThayDoiSanPhamResponse> getLichSu(@PathVariable Integer id) {
        return lichSuThayDoiSanPhamService.layLichSu(id);
    }

    // GET /api/san-pham/danh-sach-chon — lấy tất cả sản phẩm để chọn cho khuyến mãi
    @GetMapping("/danh-sach-chon")
    public List<SanPhamResponse> getDanhSachChon() {
        return sanPhamService.getDanhSachChon();
    }
}
