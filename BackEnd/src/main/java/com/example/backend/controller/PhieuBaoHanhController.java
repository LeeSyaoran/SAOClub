package com.example.backend.controller;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import com.example.backend.response.WarrantyLookupResponse;
import com.example.backend.service.PhieuBaoHanhService;
import jakarta.persistence.EntityNotFoundException;
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
@RequestMapping("/api/phieu-bao-hanh")
public class PhieuBaoHanhController {

    @Autowired
    private PhieuBaoHanhService phieuBaoHanhService;

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public Page<PhieuBaoHanhResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return phieuBaoHanhService.hienThiPhieuBaoHanh(PageRequest.of(page, size));
    }

    @GetMapping("/{id:\\d+}")
    public PhieuBaoHanh getById(@PathVariable Integer id) {
        return phieuBaoHanhService.getById(id);
    }

<<<<<<< HEAD
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
=======
    @GetMapping("/tra-cuu-serial")
    @PreAuthorize("permitAll()")
    public ResponseEntity<WarrantyLookupResponse> traCuuSerial(@RequestParam String soSerial) {
        WarrantyLookupResponse r = phieuBaoHanhService.traCuuSerial(soSerial);
        return ResponseEntity.ok(r);
    }

>>>>>>> 263fbf4733d7677b5a1c903b79b60a2fc9633142
    @PostMapping
    public ResponseEntity<PhieuBaoHanh> create(@Valid @RequestBody PhieuBaoHanhRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuBaoHanhService.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody PhieuBaoHanhRequest request) {
        phieuBaoHanhService.update(id, request);
        return ResponseEntity.ok().build();
    }


    /**
     * Lấy phiếu BH theo khách hàng — dùng cho trang Tài khoản KH.
     * Cần đăng nhập (JWT token).
     */
    @GetMapping("/khach-hang/{khachHangId}")
    public List<PhieuBaoHanhResponse> getByKhachHang(@PathVariable Integer khachHangId) {
        return phieuBaoHanhService.getByKhachHang(khachHangId);
    }


    /**
     * Cập nhật trạng thái phiếu BH (tiếp nhận, hoàn thành, từ chối).
     */
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("/{id}/status")
    public ResponseEntity<PhieuBaoHanh> updateStatus(@PathVariable Integer id, @RequestBody PhieuBaoHanhRequest request) {
        return ResponseEntity.ok(phieuBaoHanhService.update(id, request));
    }

    /**
     * Khách hàng tự hủy phiếu BH (chỉ khi còn trạng thái 'cho_xu_ly').
     */
    @PutMapping("/{id}/huy")
    public ResponseEntity<Void> huyPhieu(@PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        phieuBaoHanhService.huyPhieu(id, body.get("lyDo"));
        return ResponseEntity.ok().build();
    }


    // ── Extension requests (admin duyệt gia hạn BH) ───────────────────
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN')")
    @GetMapping("/extension-requests")
    public List<PhieuBaoHanhResponse> getExtensionRequests() {
        return phieuBaoHanhService.getExtensionRequests();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN')")
    @PutMapping("/{id}/approve-extension")
    public ResponseEntity<Void> approveExtension(@PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        phieuBaoHanhService.approveExtension(id, body.get("approvedAt"));
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN')")
    @PutMapping("/{id}/reject-extension")
    public ResponseEntity<Void> rejectExtension(@PathVariable Integer id, @RequestBody java.util.Map<String, String> body) {
        phieuBaoHanhService.rejectExtension(id, body.get("lyDoTuChoi"), body.get("rejectedAt"));
        return ResponseEntity.ok().build();
    }

    // ── Lịch sử xử lý phiếu ─────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}/lich-su")
    public List<PhieuBaoHanhResponse> getLichSu(@PathVariable Integer id) {
        return phieuBaoHanhService.getById(id) != null
            ? List.of() : List.of();
    }

}