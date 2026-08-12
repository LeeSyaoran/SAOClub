package com.example.backend.controller;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamPublicResponse;
import com.example.backend.response.BienTheSanPhamResponse;
import com.example.backend.service.BienTheSanPhamService;
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
@RequestMapping("/api/bien-the-san-pham")
public class BienTheSanPhamController {

    @Autowired
    private BienTheSanPhamService bienTheSanPhamService;

    @GetMapping
    public Page<BienTheSanPhamResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bienTheSanPhamService.hienThiBienTheSanPham(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public BienTheSanPhamPublicResponse getById(@PathVariable Integer id) {
        return bienTheSanPhamService.getPublicById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/staff")
    public List<BienTheSanPhamResponse> getAllStaff() {
        return bienTheSanPhamService.hienThiBienTheSanPham();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/staff/{id}")
    public BienTheSanPhamResponse getByIdStaff(@PathVariable Integer id) {
        return bienTheSanPhamService.getResponseById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<BienTheSanPham> create(@Valid @RequestBody BienTheSanPhamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bienTheSanPhamService.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody BienTheSanPhamRequest request) {
        bienTheSanPhamService.update(id, request);
        return ResponseEntity.ok().build();
    }

}
