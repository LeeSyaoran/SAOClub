package com.example.backend.controller;

import com.example.backend.entity.LichSuTonKho;
import com.example.backend.repository.LichSuTonKhoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lich-su-ton-kho")
public class LichSuTonKhoController {

    @Autowired
    private LichSuTonKhoRepository repository;

    @GetMapping
    public List<LichSuTonKho> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public LichSuTonKho getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch sử tồn kho không tồn tại với id: " + id));
    }

    @PostMapping
    public LichSuTonKho create(@RequestBody LichSuTonKho item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public LichSuTonKho update(@PathVariable Integer id, @RequestBody LichSuTonKho item) {
        item.setLichSuId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
