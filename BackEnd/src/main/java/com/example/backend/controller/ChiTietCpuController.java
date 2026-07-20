package com.example.backend.controller;

import com.example.backend.entity.ChiTietCpu;
import com.example.backend.request.ChiTietCpuRequest;
import com.example.backend.response.ChiTietCpuResponse;
import com.example.backend.service.ChiTietCpuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-cpu")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietCpuController {

    @Autowired
    private ChiTietCpuService chiTietCpuService;

    @GetMapping
    public List<ChiTietCpuResponse> getAll() {
        return chiTietCpuService.hienThiChiTietCpu();
    }

    @GetMapping("/{id}")
    public ChiTietCpu getById(@PathVariable Integer id) {
        return chiTietCpuService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietCpuRequest request) {
        chiTietCpuService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietCpuRequest request) {
        chiTietCpuService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietCpuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
