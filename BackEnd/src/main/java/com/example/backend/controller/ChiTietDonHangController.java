package com.example.backend.controller;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.response.ChiTietDonHangResponse;
import com.example.backend.response.ChiTietDonHangSerialResponse;
import com.example.backend.service.ChiTietDonHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-don-hang")
public class ChiTietDonHangController {

    @Autowired
    private ChiTietDonHangService chiTietDonHangService;

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public List<ChiTietDonHangResponse> getAll() {
        return chiTietDonHangService.hienThiChiTietDonHang();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}")
    public ChiTietDonHang getById(@PathVariable Integer id) {
        return chiTietDonHangService.getById(id);
    }

    @GetMapping("/don-hang/{donHangId}")
    public List<ChiTietDonHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getByDonHangId(donHangId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/don-hang/{donHangId}/serials")
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getSerialsByDonHangId(donHangId);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietDonHangRequest request) {
        chiTietDonHangService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietDonHangRequest request) {
        chiTietDonHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietDonHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
