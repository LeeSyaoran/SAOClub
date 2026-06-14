package com.example.backend.controller;

import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.PhieuTraHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/phieu-tra-hang")
public class PhieuTraHangController {

    @Autowired
    private PhieuTraHangRepository repository;

    @GetMapping
    public List<PhieuTraHang> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public PhieuTraHang getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + id));
    }

    @PostMapping
    public PhieuTraHang create(@RequestBody PhieuTraHang item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public PhieuTraHang update(@PathVariable Integer id, @RequestBody PhieuTraHang item) {
        item.setPhieuTraId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
