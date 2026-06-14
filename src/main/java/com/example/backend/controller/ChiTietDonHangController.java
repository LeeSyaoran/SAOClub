package com.example.backend.controller;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.repository.ChiTietDonHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chi-tiet-don-hang")
public class ChiTietDonHangController {

    @Autowired
    private ChiTietDonHangRepository repository;

    @GetMapping
    public List<ChiTietDonHang> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ChiTietDonHang getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn hàng không tồn tại với id: " + id));
    }

    @PostMapping
    public ChiTietDonHang create(@RequestBody ChiTietDonHang item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ChiTietDonHang update(@PathVariable Integer id, @RequestBody ChiTietDonHang item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
