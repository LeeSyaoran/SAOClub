package com.example.backend.controller;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamResponse;
import com.example.backend.service.BienTheSanPhamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/bien-the-san-pham")
public class BienTheSanPhamController {

    @Autowired
    private BienTheSanPhamService bienTheSanPhamService;

    @GetMapping
    public List<BienTheSanPhamResponse> getAll() {
        return bienTheSanPhamService.hienThiBienTheSanPham();
    }

    @GetMapping("/{id}")
    public BienTheSanPham getById(@PathVariable Integer id) {
        return bienTheSanPhamService.getById(id);
    }

    // POST — service xử lý FK: sanPham, cpu, ram, oCung, gpu
    @PostMapping
    public ResponseEntity<BienTheSanPham> create(@Valid @RequestBody BienTheSanPhamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bienTheSanPhamService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody BienTheSanPhamRequest request) {
        bienTheSanPhamService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        bienTheSanPhamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
