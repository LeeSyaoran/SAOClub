package com.example.backend.controller;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.request.ChiTietOcungRequest;
import com.example.backend.response.ChiTietOcungResponse;
import com.example.backend.service.ChiTietOcungService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-o-cung")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietOcungController {

    @Autowired
    private ChiTietOcungService chiTietOcungService;

    @GetMapping
    public List<ChiTietOcungResponse> getAll() {
        return chiTietOcungService.hienThiChiTietOcung();
    }

    @GetMapping("/{id}")
    public ChiTietOcung getById(@PathVariable Integer id) {
        return chiTietOcungService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietOcungRequest request) {
        chiTietOcungService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietOcungRequest request) {
        chiTietOcungService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietOcungService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
