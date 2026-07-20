package com.example.backend.controller;

import com.example.backend.entity.ChiTietRam;
import com.example.backend.request.ChiTietRamRequest;
import com.example.backend.response.ChiTietRamResponse;
import com.example.backend.service.ChiTietRamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-ram")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietRamController {

    @Autowired
    private ChiTietRamService chiTietRamService;

    @GetMapping
    public List<ChiTietRamResponse> getAll() {
        return chiTietRamService.hienThiChiTietRam();
    }

    @GetMapping("/{id}")
    public ChiTietRam getById(@PathVariable Integer id) {
        return chiTietRamService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietRamRequest request) {
        chiTietRamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietRamRequest request) {
        chiTietRamService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietRamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
