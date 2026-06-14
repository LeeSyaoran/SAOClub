package com.example.backend.controller;

import com.example.backend.entity.DanhMuc;
import com.example.backend.repository.DanhMucRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/danh-muc")
public class DanhMucController {

    @Autowired
    private DanhMucRepository repository;

    @GetMapping
    public List<DanhMuc> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DanhMuc getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại với id: " + id));
    }

    @PostMapping
    public DanhMuc create(@RequestBody DanhMuc item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DanhMuc update(@PathVariable Integer id, @RequestBody DanhMuc item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
