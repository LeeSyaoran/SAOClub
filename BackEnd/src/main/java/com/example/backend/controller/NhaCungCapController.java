package com.example.backend.controller;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.repository.NhaCungCapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/nha-cung-cap")
public class NhaCungCapController {

    @Autowired
    private NhaCungCapRepository repository;

    @GetMapping
    public List<NhaCungCap> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public NhaCungCap getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhà cung cấp không tồn tại với id: " + id));
    }

    @PostMapping
    public NhaCungCap create(@RequestBody NhaCungCap item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public NhaCungCap update(@PathVariable Integer id, @RequestBody NhaCungCap item) {
        item.setNhaCungCapId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
