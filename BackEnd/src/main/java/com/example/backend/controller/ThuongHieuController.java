package com.example.backend.controller;

import com.example.backend.entity.ThuongHieu;
import com.example.backend.request.ThuongHieuRequest;
import com.example.backend.response.ThuongHieuResponse;
import com.example.backend.service.ThuongHieuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thuong-hieu")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuService thuongHieuService;

    @GetMapping
    public List<ThuongHieuResponse> getAll() {
        return thuongHieuService.hienThiThuongHieu();
    }

    @GetMapping("/{id}")
    public ThuongHieu getById(@PathVariable Integer id) {
        return thuongHieuService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<ThuongHieu> create(@Valid @RequestBody ThuongHieuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thuongHieuService.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @Valid @RequestBody ThuongHieuRequest request) {
        thuongHieuService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        thuongHieuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
