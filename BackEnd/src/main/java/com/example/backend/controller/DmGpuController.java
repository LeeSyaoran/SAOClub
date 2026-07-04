package com.example.backend.controller;

import com.example.backend.entity.DmGpu;
import com.example.backend.repository.DmGpuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Danh mục GPU — dữ liệu tham chiếu đơn giản, không cần service riêng
@RestController
@RequestMapping("/api/dm-gpu")
public class DmGpuController {

    @Autowired
    private DmGpuRepository repository;

    @GetMapping
    public List<DmGpu> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DmGpu getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GPU không tồn tại với id: " + id));
    }

    @PostMapping
    public ResponseEntity<DmGpu> create(@RequestBody DmGpu item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody DmGpu item) {
        item.setGpuId(id);
        repository.save(item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repository.existsById(id))
            throw new IllegalArgumentException("GPU không tồn tại với id: " + id);
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
