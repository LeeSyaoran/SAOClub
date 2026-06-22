package com.example.backend.controller;

import com.example.backend.entity.ThuongHieu;
import com.example.backend.response.ThuongHieuResponse;
import com.example.backend.service.ThuongHieuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/thuong-hieu")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuService thuongHieuService;

    @GetMapping
    public List<ThuongHieuResponse> getAll() {
        return thuongHieuService.hienThiThuongHieu();
    }

    @GetMapping("/{id}")
    public ThuongHieu getById(@PathVariable Integer id) {
        return thuongHieuService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ThuongHieu> create(@RequestBody ThuongHieu item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thuongHieuService.create(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody ThuongHieu item) {
        thuongHieuService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        thuongHieuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
