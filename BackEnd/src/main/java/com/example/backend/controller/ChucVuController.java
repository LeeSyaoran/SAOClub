package com.example.backend.controller;

import com.example.backend.entity.ChucVu;
import com.example.backend.repository.ChucVuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chuc-vu")
public class ChucVuController {

    @Autowired
    private ChucVuRepository repository;

    @GetMapping
    public List<ChucVu> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ChucVu getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chức vụ không tồn tại với id: " + id));
    }

    @PostMapping
    public ChucVu create(@RequestBody ChucVu item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ChucVu update(@PathVariable Integer id, @RequestBody ChucVu item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
