package com.example.backend.controller;

import com.example.backend.entity.DmRam;
import com.example.backend.repository.DmRamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dm-ram")
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
    public DmRam create(@RequestBody DmRam item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DmRam update(@PathVariable Integer id, @RequestBody DmRam item) {
        item.setRamId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
