package com.example.backend.controller;

import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.request.ChiTietTraHangRequest;
import com.example.backend.response.ChiTietTraHangResponse;
import com.example.backend.service.ChiTietTraHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-tra-hang")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietTraHangController {

    @Autowired
    private ChiTietTraHangService chiTietTraHangService;

    @GetMapping
    public List<ChiTietTraHangResponse> getAll() {
        return chiTietTraHangService.hienThiChiTietTraHang();
    }

    @GetMapping("/{id}")
    public ChiTietTraHang getById(@PathVariable Integer id) {
        return chiTietTraHangService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ChiTietTraHang> create(@Valid @RequestBody ChiTietTraHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chiTietTraHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietTraHangRequest request) {
        chiTietTraHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietTraHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
