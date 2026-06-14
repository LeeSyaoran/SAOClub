package com.example.backend.controller;

import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.repository.ChiTietTraHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chi-tiet-tra-hang")
public class ChiTietTraHangController {

    @Autowired
    private ChiTietTraHangRepository repository;

    @GetMapping
    public List<ChiTietTraHang> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ChiTietTraHang getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết trả hàng không tồn tại với id: " + id));
    }

    @PostMapping
    public ChiTietTraHang create(@RequestBody ChiTietTraHang item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public ChiTietTraHang update(@PathVariable Integer id, @RequestBody ChiTietTraHang item) {
        item.setId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
