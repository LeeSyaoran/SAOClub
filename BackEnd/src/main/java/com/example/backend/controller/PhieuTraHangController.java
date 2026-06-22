package com.example.backend.controller;

import com.example.backend.entity.PhieuTraHang;
import com.example.backend.request.PhieuTraHangRequest;
import com.example.backend.response.PhieuTraHangResponse;
import com.example.backend.service.PhieuTraHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/phieu-tra-hang")
public class PhieuTraHangController {

    @Autowired
    private PhieuTraHangService phieuTraHangService;

    @GetMapping
    public List<PhieuTraHangResponse> getAll() {
        return phieuTraHangService.hienThiPhieuTraHang();
    }

    @GetMapping("/{id}")
    public PhieuTraHang getById(@PathVariable Integer id) {
        return phieuTraHangService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PhieuTraHang> create(@Valid @RequestBody PhieuTraHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuTraHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody PhieuTraHangRequest request) {
        phieuTraHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        phieuTraHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
