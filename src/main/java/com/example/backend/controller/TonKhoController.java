package com.example.backend.controller;

import com.example.backend.entity.TonKho;
import com.example.backend.repository.TonKhoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/ton-kho")
public class TonKhoController {

    @Autowired
    private TonKhoRepository repository;

    @GetMapping
    public List<TonKho> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public TonKho getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tồn kho không tồn tại với id: " + id));
    }

    @PostMapping
    public TonKho create(@RequestBody TonKho item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public TonKho update(@PathVariable Integer id, @RequestBody TonKho item) {
        item.setTonKhoId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
