package com.example.backend.controller;

import com.example.backend.entity.ThuocTinh;
import com.example.backend.entity.ThuocTinhGiaTri;
import com.example.backend.request.ThuocTinhGiaTriRequest;
import com.example.backend.request.ThuocTinhRequest;
import com.example.backend.response.ThuocTinhResponse;
import com.example.backend.repository.ThuocTinhGiaTriRepository;
import com.example.backend.repository.ThuocTinhRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thuoc-tinh")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ThuocTinhController {

    @Autowired
    private ThuocTinhRepository thuocTinhRepository;

    @Autowired
    private ThuocTinhGiaTriRepository giaTriRepository;

    // ==================== THUỘC TÍNH ====================

    @GetMapping
    public List<ThuocTinhResponse> getAll() {
        return thuocTinhRepository.findAllByOrderByThuTuHienThiAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ThuocTinhResponse getById(@PathVariable Integer id) {
        ThuocTinh tt = thuocTinhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không tồn tại với id: " + id));
        return toResponse(tt);
    }

    @GetMapping("/ten-truong/{tenTruong}")
    public ThuocTinhResponse getByTenTruong(@PathVariable String tenTruong) {
        ThuocTinh tt = thuocTinhRepository.findByTenTruong(tenTruong)
                .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không tồn tại với tên trường: " + tenTruong));
        return toResponse(tt);
    }

    @PostMapping
    public ResponseEntity<ThuocTinhResponse> create(@RequestBody ThuocTinhRequest request) {
        // Kiểm tra trùng tenTruong
        if (thuocTinhRepository.existsByTenTruong(request.getTenTruong())) {
            return ResponseEntity.badRequest().build();
        }

        ThuocTinh tt = new ThuocTinh();
        tt.setTenTruong(request.getTenTruong());
        tt.setTenHienThi(request.getTenHienThi());
        tt.setLoaiDuLieu(request.getLoaiDuLieu() != null ? request.getLoaiDuLieu() : "text");
        tt.setBatBuoc(request.getBatBuoc() != null ? request.getBatBuoc() : false);
        tt.setThuTuHienThi(request.getThuTuHienThi() != null ? request.getThuTuHienThi() : 0);
        tt.setTrangThai(request.getTrangThai() != null ? request.getTrangThai() : "active");
        tt.setPhamVi(request.getPhamVi() != null ? request.getPhamVi() : "san_pham");

        ThuocTinh saved = thuocTinhRepository.save(tt);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThuocTinhResponse> update(@PathVariable Integer id, @RequestBody ThuocTinhRequest request) {
        ThuocTinh tt = thuocTinhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không tồn tại với id: " + id));

        // Kiểm tra trùng tenTruong (nếu thay đổi)
        if (!tt.getTenTruong().equals(request.getTenTruong())
                && thuocTinhRepository.existsByTenTruong(request.getTenTruong())) {
            return ResponseEntity.badRequest().build();
        }

        tt.setTenTruong(request.getTenTruong());
        tt.setTenHienThi(request.getTenHienThi());
        if (request.getLoaiDuLieu() != null) {
            tt.setLoaiDuLieu(request.getLoaiDuLieu());
        }
        if (request.getBatBuoc() != null) {
            tt.setBatBuoc(request.getBatBuoc());
        }
        if (request.getThuTuHienThi() != null) {
            tt.setThuTuHienThi(request.getThuTuHienThi());
        }
        if (request.getTrangThai() != null) {
            tt.setTrangThai(request.getTrangThai());
        }
        if (request.getPhamVi() != null) {
            tt.setPhamVi(request.getPhamVi());
        }

        ThuocTinh updated = thuocTinhRepository.save(tt);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!thuocTinhRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        thuocTinhRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ==================== GIÁ TRỊ THUỘC TÍNH ====================

    @GetMapping("/{id}/gia-tri")
    public List<ThuocTinhResponse.GiaTriResponse> getGiaTri(@PathVariable Integer id) {
        return giaTriRepository.findByThuocTinh_ThuocTinhIdOrderByThuTuAsc(id)
                .stream()
                .map(gt -> new ThuocTinhResponse.GiaTriResponse(gt.getGiaTriId(), gt.getGiaTri(), gt.getThuTu()))
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/gia-tri")
    public ResponseEntity<ThuocTinhResponse.GiaTriResponse> addGiaTri(
            @PathVariable Integer id,
            @RequestBody ThuocTinhGiaTriRequest request) {

        ThuocTinh tt = thuocTinhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không tồn tại với id: " + id));

        ThuocTinhGiaTri gt = new ThuocTinhGiaTri();
        gt.setThuocTinh(tt);
        gt.setGiaTri(request.getGiaTri());
        gt.setThuTu(request.getThuTu() != null ? request.getThuTu() : 0);

        ThuocTinhGiaTri saved = giaTriRepository.save(gt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ThuocTinhResponse.GiaTriResponse(saved.getGiaTriId(), saved.getGiaTri(), saved.getThuTu()));
    }

    @DeleteMapping("/{id}/gia-tri/{gvid}")
    public ResponseEntity<Void> deleteGiaTri(@PathVariable Integer id, @PathVariable Integer gvid) {
        if (!giaTriRepository.existsById(gvid)) {
            return ResponseEntity.notFound().build();
        }
        giaTriRepository.deleteById(gvid);
        return ResponseEntity.ok().build();
    }

    // ==================== HELPER ====================

    private ThuocTinhResponse toResponse(ThuocTinh tt) {
        List<ThuocTinhResponse.GiaTriResponse> giaTriResponses = tt.getGiaTriList().stream()
                .map(gt -> new ThuocTinhResponse.GiaTriResponse(gt.getGiaTriId(), gt.getGiaTri(), gt.getThuTu()))
                .collect(Collectors.toList());

        return new ThuocTinhResponse(
                tt.getThuocTinhId(),
                tt.getTenTruong(),
                tt.getTenHienThi(),
                tt.getLoaiDuLieu(),
                tt.getBatBuoc(),
                tt.getThuTuHienThi(),
                tt.getTrangThai(),
                tt.getPhamVi() != null ? tt.getPhamVi() : "san_pham",
                giaTriResponses
        );
    }
}
