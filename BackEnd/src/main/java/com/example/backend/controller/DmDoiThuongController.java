package com.example.backend.controller;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.request.DmDoiThuongRequest;
import com.example.backend.response.DmDoiThuongResponse;
import com.example.backend.service.DmDoiThuongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm-doi-thuong")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class DmDoiThuongController {

    @Autowired
    private DmDoiThuongService dmDoiThuongService;

    // Giữ mở — khách hàng cần xem danh mục để chọn đổi (AccountPage.vue).
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<DmDoiThuongResponse> getAll() {
        return dmDoiThuongService.hienThiDmDoiThuong();
    }

    @GetMapping("/{id}")
    public DmDoiThuong getById(@PathVariable Integer id) {
        return dmDoiThuongService.getById(id);
    }

    @PostMapping
    public ResponseEntity<DmDoiThuong> create(@Valid @RequestBody DmDoiThuongRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dmDoiThuongService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DmDoiThuongRequest request) {
        dmDoiThuongService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        dmDoiThuongService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
