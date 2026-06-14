package com.example.backend.controller;

import com.example.backend.entity.NhanVien;
import com.example.backend.repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/nhan-vien")
public class NhanVienController {

    @Autowired
    private NhanVienRepository repository;

    @GetMapping
    public List<NhanVien> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public NhanVien getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại với id: " + id));
    }

    @PostMapping
    public NhanVien create(@RequestBody NhanVien item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public NhanVien update(@PathVariable Integer id, @RequestBody NhanVien item) {
        item.setNhanVienId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
