package com.example.backend.controller;

import com.example.backend.entity.DiaChiGiaoHang;
import com.example.backend.repository.DiaChiGiaoHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dia-chi-giao-hang")
public class DiaChiGiaoHangController {

    @Autowired
    private DiaChiGiaoHangRepository repository;

    @GetMapping
    public List<DiaChiGiaoHang> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DiaChiGiaoHang getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ giao hàng không tồn tại với id: " + id));
    }

    @PostMapping
    public DiaChiGiaoHang create(@RequestBody DiaChiGiaoHang item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DiaChiGiaoHang update(@PathVariable Integer id, @RequestBody DiaChiGiaoHang item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
