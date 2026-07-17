package com.example.backend.controller;

import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import com.example.backend.service.CaiDatHeThongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// /api/cai-dat/** — KHÔNG nằm trong permitAll() của SecurityConfig, nên mọi endpoint ở đây
// tự động yêu cầu JWT hợp lệ qua .anyRequest().authenticated() (xem SecurityConfig.java).
@RestController
@RequestMapping("/api/cai-dat")
public class CaiDatController {

    @Autowired
    private CaiDatHeThongService caiDatHeThongService;

    @GetMapping
    public CaiDatHeThongResponse get() {
        return caiDatHeThongService.get();
    }

    @PutMapping
    public CaiDatHeThongResponse update(@Valid @RequestBody CaiDatHeThongRequest req) {
        return caiDatHeThongService.update(req);
    }

    @PostMapping("/ap-dung-nguong-ton-kho")
    public Map<String, Integer> apDungNguongTonKho(@RequestBody Map<String, Integer> body) {
        int nguong = body.getOrDefault("nguong", 0);
        int soBienTheDaCapNhat = caiDatHeThongService.apDungNguongTonKhoChoTatCa(nguong);
        return Map.of("soBienTheDaCapNhat", soBienTheDaCapNhat);
    }
}
