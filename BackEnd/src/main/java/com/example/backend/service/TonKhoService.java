package com.example.backend.service;

import com.example.backend.entity.TonKho;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.TonKhoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TonKhoService {

    @Autowired
    private TonKhoRepository tonKhoRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;

    public List<TonKho> getAll() {
        return tonKhoRepository.findAll();
    }

    public TonKho getById(Integer id) {
        return tonKhoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tồn kho không tồn tại với id: " + id));
    }

    // Tìm tồn kho theo biến thể — dùng để kiểm tra tồn kho trước khi tạo đơn hàng
    public TonKho getByBienTheId(Integer bienTheId) {
        return tonKhoRepository.findByBienTheBienTheId(bienTheId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho biến thể id: " + bienTheId));
    }

    public TonKho create(TonKho item) {
        item.setNgayCapNhat(LocalDateTime.now());
        return tonKhoRepository.save(item);
    }

    public TonKho update(Integer id, TonKho item) {
        TonKho existing = getById(id);
        existing.setSoLuongTon(item.getSoLuongTon());
        if (item.getSoLuongGiu()     != null) existing.setSoLuongGiu(item.getSoLuongGiu());
        if (item.getTonKhoToiThieu() != null) existing.setTonKhoToiThieu(item.getTonKhoToiThieu());
        existing.setNgayCapNhat(LocalDateTime.now());
        return tonKhoRepository.save(existing);
    }

    public void delete(Integer id) {
        if (!tonKhoRepository.existsById(id))
            throw new IllegalArgumentException("Tồn kho không tồn tại với id: " + id);
        tonKhoRepository.deleteById(id);
    }
}
