package com.example.backend.controller;

import com.example.backend.entity.PhieuNhapKho;
import com.example.backend.repository.PhieuNhapKhoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/phieu-nhap-kho")
public class PhieuNhapKhoController {

    @Autowired
    private PhieuNhapKhoRepository repository;

    @GetMapping
    public List<PhieuNhapKho> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public PhieuNhapKho getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu nhập kho không tồn tại với id: " + id));
    }

    @PostMapping
    public PhieuNhapKho create(@RequestBody PhieuNhapKho item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public PhieuNhapKho update(@PathVariable Integer id, @RequestBody PhieuNhapKho item) {
        item.setPhieuNhapId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
