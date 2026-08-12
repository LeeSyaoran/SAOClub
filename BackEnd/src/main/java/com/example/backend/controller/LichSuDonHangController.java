package com.example.backend.controller;

import com.example.backend.response.LichSuDonHangResponse;
import com.example.backend.service.LichSuDonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
