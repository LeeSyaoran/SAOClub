package com.example.backend.controller;

import com.example.backend.entity.CauHinhVongQuay;
import com.example.backend.request.CauHinhVongQuayRequest;
import com.example.backend.response.CauHinhVongQuayResponse;
import com.example.backend.response.KetQuaQuayResponse;
import com.example.backend.response.LichSuQuayResponse;
import com.example.backend.service.VongQuayService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vong-quay")
public class VongQuayController {

    @Autowired
    private VongQuayService vongQuayService;

    @GetMapping("cau-hinh")
    public CauHinhVongQuayResponse getCauHinh() {
        return vongQuayService.getCauHinhChoKhachHang();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("cau-hinh")
    public CauHinhVongQuay capNhatCauHinh(@Valid @RequestBody CauHinhVongQuayRequest req) {
        return vongQuayService.capNhatCauHinh(req);
    }

    @PostMapping("quay")
    public KetQuaQuayResponse quay() {
        return vongQuayService.quay();
    }

    @GetMapping("lich-su/cua-toi")
    public List<LichSuQuayResponse> getLichSuCuaToi() {
        return vongQuayService.getLichSuCuaToi();
    }
}
