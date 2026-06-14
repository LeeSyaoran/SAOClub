package com.example.backend.controller;

import com.example.backend.entity.DonHang;
import com.example.backend.repository.DonHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/don-hang")
public class DonHangController {

    @Autowired
    private DonHangRepository repository;

    @GetMapping
    public List<DonHang> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DonHang getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id));
    }

    @PostMapping
    public DonHang create(@RequestBody DonHang item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DonHang update(@PathVariable Integer id, @RequestBody DonHang item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
