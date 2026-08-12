package com.example.backend.controller;

import com.example.backend.entity.DanhMuc;
import com.example.backend.request.DanhMucRequest;
import com.example.backend.response.DanhMucResponse;
import com.example.backend.service.DanhMucService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<DanhMuc> create(@Valid @RequestBody DanhMucRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(danhMucService.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @Valid @RequestBody DanhMucRequest request) {
        danhMucService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        danhMucService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
