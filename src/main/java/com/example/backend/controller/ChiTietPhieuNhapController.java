package com.example.backend.controller;

import com.example.backend.entity.ChiTietPhieuNhap;
import com.example.backend.repository.ChiTietPhieuNhapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chi-tiet-phieu-nhap")
public class ChiTietPhieuNhapController {

    @Autowired
    private ChiTietPhieuNhapRepository repository;

    @GetMapping
    public List<ChiTietPhieuNhap> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ChiTietPhieuNhap getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết phiếu nhập không tồn tại với id: " + id));
    }

    @PostMapping
    public ChiTietPhieuNhap create(@RequestBody ChiTietPhieuNhap item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ChiTietPhieuNhap update(@PathVariable Integer id, @RequestBody ChiTietPhieuNhap item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
