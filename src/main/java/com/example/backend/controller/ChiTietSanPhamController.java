package com.example.backend.controller;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.repository.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chi-tiet-san-pham")
public class ChiTietSanPhamController {

    @Autowired
    private ChiTietSanPhamRepository repository;

    @GetMapping
    public List<ChiTietSanPham> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ChiTietSanPham getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
    }

    @PostMapping
    public ChiTietSanPham create(@RequestBody ChiTietSanPham item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ChiTietSanPham update(@PathVariable Integer id, @RequestBody ChiTietSanPham item) {
        item.setChiTietId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
