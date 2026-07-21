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

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public List<ChiTietDonHangResponse> getAll() {
        return chiTietDonHangService.hienThiChiTietDonHang();
    }

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}")
    public ChiTietDonHang getById(@PathVariable Integer id) {
        return chiTietDonHangService.getById(id);
    }

    // Giữ mở — AccountPage.vue gọi để xem chi tiết đơn của chính khách hàng.
    @GetMapping("/don-hang/{donHangId}")
    public List<ChiTietDonHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getByDonHangId(donHangId);
    }

    // Chỉ AdminPage gọi (modal "Chọn serial trước khi đóng gói") — chỉ staff.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/don-hang/{donHangId}/serials")
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getSerialsByDonHangId(donHangId);
    }

    // Giữ mở ở tầng route — CheckoutModal.vue gọi khi khách đặt hàng (tạo từng dòng chi tiết
    // đơn) — nhưng service kiểm tra chủ đơn (isStaffOrOwner), khách không thêm được dòng vào
    // đơn của người khác.
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietDonHangRequest request) {
        chiTietDonHangService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietDonHangRequest request) {
        chiTietDonHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietDonHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
