package com.example.backend.controller;

import com.example.backend.entity.SanPham;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamResponse;
import com.example.backend.service.SanPhamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/san-pham")
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    // GET /api/san-pham/hien-thi
    // Trả về danh sách sản phẩm kèm thông tin variant (bien_the_san_pham) qua JPQL DTO query
    // Tránh N+1 query so với dùng findAll() + serialize entity lồng nhau
    @GetMapping("hien-thi")
    public List<SanPhamResponse> getAll() {
        return sanPhamService.hienThiSanPham();
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
    @PostMapping
    public ResponseEntity<SanPham> create(@Valid @RequestBody SanPhamRequest request) {
        SanPham created = sanPhamService.createSanPham(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/san-pham/update/{id}
    // Cập nhật SanPham + BienTheSanPham (nếu request có bienTheId)
    // Trả 200 OK không có body (đã update thành công)
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody SanPhamRequest request) {
        sanPhamService.updateSanPham(id, request);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/san-pham/delete/{id}
    // Xóa sản phẩm — DB có ON DELETE CASCADE nên bien_the_san_pham liên quan tự xóa theo
    // Trả 204 No Content (chuẩn REST cho DELETE thành công)
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sanPhamService.deleteSanPham(id);
        return ResponseEntity.noContent().build();
    }
}
