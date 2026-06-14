package com.example.backend.controller;

import com.example.backend.entity.DmGpu;
import com.example.backend.repository.DmGpuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
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
    public DmGpu create(@RequestBody DmGpu item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DmGpu update(@PathVariable Integer id, @RequestBody DmGpu item) {
        item.setGpuId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
