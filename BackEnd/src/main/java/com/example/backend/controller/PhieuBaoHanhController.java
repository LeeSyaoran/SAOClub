package com.example.backend.controller;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.PhieuBaoHanhRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/phieu-bao-hanh")
public class PhieuBaoHanhController {

    @Autowired
    private PhieuBaoHanhRepository repository;

    @GetMapping
    public List<PhieuBaoHanh> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public PhieuBaoHanh getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id));
    }

    @PostMapping
    public PhieuBaoHanh create(@RequestBody PhieuBaoHanh item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public PhieuBaoHanh update(@PathVariable Integer id, @RequestBody PhieuBaoHanh item) {
        item.setBaoHanhId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
