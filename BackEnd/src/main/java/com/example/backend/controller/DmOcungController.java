package com.example.backend.controller;

import com.example.backend.entity.DmOcung;
import com.example.backend.repository.DmOcungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm-o-cung")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class DmOcungController {

    @Autowired
    private DmOcungRepository repository;

    @GetMapping
    public List<DmOcung> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DmOcung getById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ổ cứng không tồn tại với id: " + id));
    }

    @PostMapping
    public ResponseEntity<DmOcung> create(@RequestBody DmOcung item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody DmOcung item) {
        item.setOCungId(id);
        repository.save(item);
        return ResponseEntity.ok().build();
    }

}
