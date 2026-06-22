package com.example.backend.controller;

import com.example.backend.entity.DanhMuc;
import com.example.backend.response.DanhMucResponse;
import com.example.backend.service.DanhMucService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/danh-muc")
public class DanhMucController {

    @Autowired
    private DanhMucService danhMucService;

    @GetMapping
    public List<DanhMucResponse> getAll() {
        return danhMucService.hienThiDanhMuc();
    }

    @GetMapping("/{id}")
    public DanhMuc getById(@PathVariable Integer id) {
        return danhMucService.getById(id);
    }

    @PostMapping
    public ResponseEntity<DanhMuc> create(@RequestBody DanhMuc item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(danhMucService.create(item));
    }

    // DanhMuc dùng field "id" (không phải danhMucId) — xem entity DanhMuc
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody DanhMuc item) {
        danhMucService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        danhMucService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
