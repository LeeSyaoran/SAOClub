package com.example.backend.controller;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.response.NhaCungCapResponse;
import com.example.backend.service.NhaCungCapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Nhà cung cấp — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/nha-cung-cap")
public class NhaCungCapController {

    @Autowired
    private NhaCungCapService nhaCungCapService;

    @GetMapping
    public List<NhaCungCapResponse> getAll() {
        return nhaCungCapService.hienThiNhaCungCap();
    }

    @GetMapping("/{id}")
    public NhaCungCap getById(@PathVariable Integer id) {
        return nhaCungCapService.getById(id);
    }

    @PostMapping
    public ResponseEntity<NhaCungCap> create(@RequestBody NhaCungCap item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhaCungCapService.create(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody NhaCungCap item) {
        nhaCungCapService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        nhaCungCapService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
