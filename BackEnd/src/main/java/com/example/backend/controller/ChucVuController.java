package com.example.backend.controller;

import com.example.backend.entity.ChucVu;
import com.example.backend.response.ChucVuResponse;
import com.example.backend.service.ChucVuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/chuc-vu")
public class ChucVuController {

    @Autowired
    private ChucVuService chucVuService;

    @GetMapping
    public List<ChucVuResponse> getAll() {
        return chucVuService.hienThiChucVu();
    }

    @GetMapping("/{id}")
    public ChucVu getById(@PathVariable Integer id) {
        return chucVuService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ChucVu> create(@RequestBody ChucVu item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chucVuService.create(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody ChucVu item) {
        chucVuService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chucVuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
