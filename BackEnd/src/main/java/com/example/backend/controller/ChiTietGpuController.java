package com.example.backend.controller;

import com.example.backend.entity.ChiTietGpu;
import com.example.backend.request.ChiTietGpuRequest;
import com.example.backend.response.ChiTietGpuResponse;
import com.example.backend.service.ChiTietGpuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-gpu")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietGpuController {

    @Autowired
    private ChiTietGpuService chiTietGpuService;

    @GetMapping
    public List<ChiTietGpuResponse> getAll() {
        return chiTietGpuService.hienThiChiTietGpu();
    }

    @GetMapping("/{id}")
    public ChiTietGpu getById(@PathVariable Integer id) {
        return chiTietGpuService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietGpuRequest request) {
        chiTietGpuService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietGpuRequest request) {
        chiTietGpuService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietGpuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
