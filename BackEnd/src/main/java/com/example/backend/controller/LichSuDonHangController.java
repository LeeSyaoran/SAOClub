package com.example.backend.controller;

import com.example.backend.response.LichSuDonHangResponse;
import com.example.backend.service.LichSuDonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Giữ mở — AccountPage.vue gọi để khách hàng xem lịch sử đơn của chính mình, giống
// ChiTietDonHangController.getByDonHang(). Chỉ có GET vì bảng lich_su_don_hang chỉ được
// trigger DB ghi (trg_don_hang_log_trangthai), không có endpoint tạo/sửa/xoá.
@RestController
@RequestMapping("/api/lich-su-don-hang")
public class LichSuDonHangController {

    @Autowired
    private LichSuDonHangService lichSuDonHangService;

    @GetMapping("/don-hang/{donHangId}")
    public List<LichSuDonHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return lichSuDonHangService.getByDonHang(donHangId);
    }
}
