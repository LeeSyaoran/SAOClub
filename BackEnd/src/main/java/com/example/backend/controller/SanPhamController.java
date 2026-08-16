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

    // GET /api/san-pham/hien-thi?page=0&size=20&keyword=&danhMucId=&thuongHieuId=&trangThai=
    // Trả Page<SanPhamResponse> (1 dòng/biến thể) qua JPQL DTO query, phân trang + lọc ở
    // tầng SQL.
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

    // GET /api/san-pham/{id} — entity SanPham theo ID (form chỉnh sửa load thông tin cơ bản)
    @GetMapping("/{id}")
    public SanPham getById(@PathVariable Integer id) {
        return sanPhamService.getSanPhamById(id);
    }

    // POST /api/san-pham
    // Service tạo cả SanPham lẫn BienTheSanPham đầu tiên trong cùng một transaction, nên
    // request BẮT BUỘC có maSku/giaNhap/giaBan — thiếu là ba cột NOT NULL nhận NULL và
    // toàn bộ giao dịch rollback.
    // Trả 201 + SanPhamCreatedResponse thay vì entity: entity chứa proxy LAZY, Jackson
    // serialize sẽ vỡ giữa chừng và frontend mất id vừa tạo.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<SanPhamCreatedResponse> create(@Valid @RequestBody SanPhamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sanPhamService.createSanPham(request));
    }

    // PUT /api/san-pham/update/{id} — cập nhật SanPham + BienTheSanPham (nếu có bienTheId)
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody SanPhamRequest request) {
        sanPhamService.updateSanPham(id, request);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/san-pham/delete/{id} — chỉ thành công nếu chưa biến thể nào qua giao dịch
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sanPhamService.deleteSanPham(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/san-pham/{id}/co-giao-dich — FE gọi trước khi hiện hộp thoại xóa.
    @GetMapping("/{id}/co-giao-dich")
    public boolean hasTransactionHistory(@PathVariable Integer id) {
        return sanPhamService.hasTransactionHistory(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}/lich-su")
    public List<LichSuThayDoiSanPhamResponse> getLichSu(@PathVariable Integer id) {
        return lichSuThayDoiSanPhamService.layLichSu(id);
    }
}
