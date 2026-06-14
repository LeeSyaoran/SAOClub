package com.example.backend.controller;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.repository.KhuyenMaiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/khuyen-mai")
public class KhuyenMaiController {

    @Autowired
    private KhuyenMaiRepository repository;

    @GetMapping
    public List<KhuyenMai> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public KhuyenMai getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khuyến mãi không tồn tại với id: " + id));
    }

    @PostMapping
    public KhuyenMai create(@RequestBody KhuyenMai item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public KhuyenMai update(@PathVariable Integer id, @RequestBody KhuyenMai item) {
        item.setKhuyenMaiId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
