package com.example.backend.controller;

import com.example.backend.entity.KhachHang;
import com.example.backend.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangRepository repository;

    @GetMapping
    public List<KhachHang> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + id));
    }

    @PostMapping
    public KhachHang create(@RequestBody KhachHang item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public KhachHang update(@PathVariable Integer id, @RequestBody KhachHang item) {
        item.setKhachHangId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
