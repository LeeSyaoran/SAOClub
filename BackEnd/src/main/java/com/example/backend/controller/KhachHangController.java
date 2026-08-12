package com.example.backend.controller;

import com.example.backend.entity.KhachHang;
import com.example.backend.request.KhachHangRegisterRequest;
import com.example.backend.request.KhachHangRequest;
import com.example.backend.request.TangDiemRequest;
import com.example.backend.response.KhachHangLookupResponse;
import com.example.backend.response.KhachHangResponse;
import com.example.backend.response.LichSuTangDiemResponse;
import com.example.backend.service.KhachHangService;
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
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangService khachHangService;

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public Page<KhachHangResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return khachHangService.hienThiKhachHang(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return khachHangService.getById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping
    public ResponseEntity<KhachHang> create(@Valid @RequestBody KhachHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody KhachHangRequest request) {
        khachHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/tang-diem")
    public ResponseEntity<Void> tangDiem(@PathVariable Integer id, @Valid @RequestBody TangDiemRequest request) {
        khachHangService.tangDiem(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/lich-su-diem")
    public List<LichSuTangDiemResponse> getLichSuDiem(@PathVariable Integer id) {
        return khachHangService.layLichSuDiem(id);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody KhachHangRegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/tim-theo-sdt")
    public KhachHangLookupResponse findBySoDienThoai(@RequestParam String soDienThoai) {
        return khachHangService.findBySoDienThoai(soDienThoai);
    }

    @PostMapping("/khach-vang-lai")
    public ResponseEntity<KhachHang> createGuest(@Valid @RequestBody KhachHangRequest request) {
        request.setDiemTichLuy(0);
        request.setTrangThai("active");
        return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.create(request));
    }

}
