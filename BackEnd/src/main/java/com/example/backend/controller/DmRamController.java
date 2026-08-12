package com.example.backend.controller;

import com.example.backend.entity.DmRam;
import com.example.backend.repository.DmRamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm-ram")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class DmRamController {

    @Autowired
    private DmRamRepository repository;

    @GetMapping
    public List<DmRam> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DmRam getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RAM không tồn tại với id: " + id));
    }

    @PostMapping
    public ResponseEntity<DmRam> create(@RequestBody DmRam item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody DmRam item) {
        item.setRamId(id);
        repository.save(item);
        return ResponseEntity.ok().build();
    }

}
