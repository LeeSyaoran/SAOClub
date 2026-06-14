package com.example.backend.controller;

import com.example.backend.entity.ThuongHieu;
import com.example.backend.repository.ThuongHieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/thuong-hieu")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuRepository repository;

    @GetMapping
    public List<ThuongHieu> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ThuongHieu getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thương hiệu không tồn tại với id: " + id));
    }

    @PostMapping
    public ThuongHieu create(@RequestBody ThuongHieu item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ThuongHieu update(@PathVariable Integer id, @RequestBody ThuongHieu item) {
        item.setThuongHieuId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
