package com.example.backend.controller;

import com.example.backend.entity.AiKienThuc;
import com.example.backend.repository.AiKienThucRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/ai-kien-thuc")
public class AiKienThucController {

    @Autowired
    private AiKienThucRepository aiKienThucRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public Page<AiKienThuc> layTatCa(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return aiKienThucRepository.findByActiveTrue(PageRequest.of(page, size));
    }

    @GetMapping("/theo-loai")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public Page<AiKienThuc> layTheoLoai(
            @RequestParam String loai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return aiKienThucRepository.findByLoaiAndActiveTrue(loai, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN', 'QUAN_KHO')")
    public ResponseEntity<AiKienThuc> layChiTiet(@PathVariable Long id) {
        return aiKienThucRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiKienThuc> taoMoi(@Valid @RequestBody AiKienThucRequest req) {
        AiKienThuc entity = new AiKienThuc();
        entity.setLoai(req.getLoai());
        entity.setTieuDe(req.getTieuDe());
        entity.setNoiDung(req.getNoiDung());
        entity.setActive(true);
        entity = aiKienThucRepository.save(entity);
        return ResponseEntity.ok(entity);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiKienThuc> capNhat(@PathVariable Long id, @Valid @RequestBody AiKienThucRequest req) {
        return aiKienThucRepository.findById(id)
                .map(entity -> {
                    entity.setLoai(req.getLoai());
                    entity.setTieuDe(req.getTieuDe());
                    entity.setNoiDung(req.getNoiDung());
                    if (req.getActive() != null) entity.setActive(req.getActive());
                    return ResponseEntity.ok(aiKienThucRepository.save(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> xoa(@PathVariable Long id) {
        return aiKienThucRepository.findById(id)
                .map(entity -> {
                    entity.setActive(false);
                    aiKienThucRepository.save(entity);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Request DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiKienThucRequest {
        public String loai;
        public String tieuDe;
        public String noiDung;
        public Boolean active;
    }
}
