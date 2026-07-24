package com.example.backend.controller;

import com.example.backend.entity.DmCpu;
import com.example.backend.repository.DmCpuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Danh mục CPU — dữ liệu tham chiếu đơn giản, không cần service riêng
@RestController
@RequestMapping("/api/dm-cpu")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class DmCpuController {

    @Autowired
    private DmCpuRepository repository;

    @GetMapping
    public List<DmCpu> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DmCpu getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CPU không tồn tại với id: " + id));
    }

    @PostMapping
    public ResponseEntity<DmCpu> create(@RequestBody DmCpu item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody DmCpu item) {
        item.setCpuId(id);
        repository.save(item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repository.existsById(id))
            throw new IllegalArgumentException("CPU không tồn tại với id: " + id);
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
