package com.example.backend.controller;

import com.example.backend.entity.SanPham;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamResponse;
import com.example.backend.service.SanPhamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/san-pham")
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    // GET /api/san-pham/hien-thi?page=0&size=20&keyword=&danhMucId=&thuongHieuId=&trangThai=
    // Trả về Page<SanPhamResponse> (1 dòng/variant) qua JPQL DTO query, có phân trang + lọc
    // động ở tầng SQL — tránh tải toàn bộ bảng như trước (N+1 tránh được nhờ DTO projection,
    // dữ liệu lớn tránh được nhờ Pageable).
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

    // GET /api/san-pham/{id}
    // Trả về entity SanPham theo ID (dùng cho form chỉnh sửa load thông tin cơ bản)
    @GetMapping("/{id}")
    public SanPham getById(@PathVariable Integer id) {
        return sanPhamService.getSanPhamById(id);
    }

    // POST /api/san-pham
    // Tạo sản phẩm mới — service sẽ tạo cả SanPham lẫn BienTheSanPham trong cùng một request
    // @Valid kích hoạt validation từ annotation trong SanPhamRequest
    // Trả 201 Created + body SanPham (có sanPhamId) để frontend biết ID mới tạo
    // Chỉ staff — trước đây permitAll ở SecurityConfig không giới hạn method nên ai cũng
    // POST/PUT/DELETE được mà không cần đăng nhập.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<SanPham> create(@Valid @RequestBody SanPhamRequest request) {
        SanPham created = sanPhamService.createSanPham(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/san-pham/update/{id}
    // Cập nhật SanPham + BienTheSanPham (nếu request có bienTheId)
    // Trả 200 OK không có body (đã update thành công)
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody SanPhamRequest request) {
        sanPhamService.updateSanPham(id, request);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/san-pham/delete/{id}
    // Xóa sản phẩm + toàn bộ biến thể liên quan — chỉ thành công nếu CHƯA biến thể nào qua
    // giao dịch (xem SanPhamService.deleteSanPham). Trả 204 No Content khi xóa thành công.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sanPhamService.deleteSanPham(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/san-pham/{id}/co-giao-dich — FE gọi trước khi hiện hộp thoại xóa, để hỏi câu
    // đơn giản nếu an toàn hoặc báo lý do chặn nếu không, thay vì phải bấm xóa rồi mới biết.
    @GetMapping("/{id}/co-giao-dich")
    public boolean hasTransactionHistory(@PathVariable Integer id) {
        return sanPhamService.hasTransactionHistory(id);
    }
}
