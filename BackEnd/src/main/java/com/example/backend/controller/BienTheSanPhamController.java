package com.example.backend.controller;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.repository.BienTheSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/bien-the-san-pham")
public class BienTheSanPhamController {

    @Autowired
    private BienTheSanPhamRepository repository;

    @GetMapping
    public List<BienTheSanPham> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public BienTheSanPham getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    @PostMapping
    public BienTheSanPham create(@RequestBody BienTheSanPham item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public BienTheSanPham update(@PathVariable Integer id, @RequestBody BienTheSanPham item) {
        item.setBienTheId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
