package com.example.backend.controller;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.request.NhaCungCapRequest;
import com.example.backend.response.NhaCungCapResponse;
import com.example.backend.service.NhaCungCapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<NhaCungCap> create(@Valid @RequestBody NhaCungCapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhaCungCapService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @Valid @RequestBody NhaCungCapRequest request) {
        nhaCungCapService.update(id, request);
        return ResponseEntity.ok().build();
    }

}
