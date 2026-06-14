package com.example.backend.controller;

import com.example.backend.entity.ThanhToan;
import com.example.backend.repository.ThanhToanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/thanh-toan")
public class ThanhToanController {

    @Autowired
    private ThanhToanRepository repository;

    @GetMapping
    public List<ThanhToan> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ThanhToan getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thanh toán không tồn tại với id: " + id));
    }

    @PostMapping
    public ThanhToan create(@RequestBody ThanhToan item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ThanhToan update(@PathVariable Integer id, @RequestBody ThanhToan item) {
        item.setThanhToanId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
