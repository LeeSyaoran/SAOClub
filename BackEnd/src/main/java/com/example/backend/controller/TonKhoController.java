package com.example.backend.controller;

import com.example.backend.entity.TonKho;
import com.example.backend.service.TonKhoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Tồn kho — chỉ staff (admin/nhân viên/quản kho) thao tác. Không có nơi nào trong code
// khách hàng (checkout, AccountPage) gọi tới controller này — đã xác nhận qua grep toàn bộ
// frontend trước khi thêm annotation.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/ton-kho")
public class TonKhoController {

    @Autowired
    private TonKhoService tonKhoService;

    @GetMapping
    public List<TonKho> getAll() {
        return tonKhoService.getAll();
    }

    @GetMapping("/{id}")
    public TonKho getById(@PathVariable Integer id) {
        return tonKhoService.getById(id);
    }

    // GET theo bienTheId — dùng để kiểm tra số lượng tồn trước khi tạo đơn hàng
    @GetMapping("/bien-the/{bienTheId}")
    public TonKho getByBienTheId(@PathVariable Integer bienTheId) {
        return tonKhoService.getByBienTheId(bienTheId);
    }

    @PostMapping
    public ResponseEntity<TonKho> create(@RequestBody TonKho item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tonKhoService.create(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody TonKho item) {
        tonKhoService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        tonKhoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
