package com.example.backend.controller;

import com.example.backend.entity.DmOcung;
import com.example.backend.repository.DmOcungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dm-o-cung")
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
    public DmOcung create(@RequestBody DmOcung item) {
        return repository.save(item);
    }

    @PutMapping("update/{id}")
    public DmOcung update(@PathVariable Integer id, @RequestBody DmOcung item) {
        item.setOCungId(id);
        return repository.save(item);
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
