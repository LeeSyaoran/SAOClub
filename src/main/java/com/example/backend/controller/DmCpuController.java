package com.example.backend.controller;

import com.example.backend.entity.DmCpu;
import com.example.backend.repository.DmCpuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dm-cpu")
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
    public DmCpu create(@RequestBody DmCpu item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DmCpu update(@PathVariable Integer id, @RequestBody DmCpu item) {
        item.setCpuId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
