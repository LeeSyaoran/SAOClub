package com.example.backend.controller;

import com.example.backend.entity.DiaChiGiaoHang;
import com.example.backend.request.DiaChiGiaoHangRequest;
import com.example.backend.response.DiaChiGiaoHangResponse;
import com.example.backend.service.DiaChiGiaoHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dia-chi-giao-hang")
public class DiaChiGiaoHangController {

    @Autowired
    private DiaChiGiaoHangService diaChiGiaoHangService;

    @GetMapping
    public List<DiaChiGiaoHangResponse> getAll() {
        return diaChiGiaoHangService.hienThiDiaChiGiaoHang();
    }

    @GetMapping("/{id}")
    public DiaChiGiaoHang getById(@PathVariable Integer id) {
        return diaChiGiaoHangService.getById(id);
    }

    @PostMapping
    public ResponseEntity<DiaChiGiaoHang> create(@Valid @RequestBody DiaChiGiaoHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diaChiGiaoHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DiaChiGiaoHangRequest request) {
        diaChiGiaoHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        diaChiGiaoHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
