package com.example.backend.service;

import com.example.backend.entity.TonKho;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.response.TonKhoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TonKhoService {

    @Autowired
    private TonKhoRepository tonKhoRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;

    @Transactional
    public List<TonKhoResponse> getAll() {
        bienTheSanPhamRepository.findAll().forEach(bt -> {
            if (tonKhoRepository.findByBienTheBienTheId(bt.getBienTheId()).isEmpty()) {
                TonKho tk = new TonKho();
                tk.setBienThe(bt);
                tk.setSoLuongTon(0);
                tk.setSoLuongGiu(0);
                tk.setTonKhoToiThieu(5);
                tk.setNgayTao(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                tk.setNgayCapNhat(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                tonKhoRepository.save(tk);
            }
        });
        return tonKhoRepository.findAllAsResponse();
    }

    public TonKho getById(Integer id) {
        return tonKhoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tồn kho không tồn tại với id: " + id));
    }

    public TonKho getByBienTheId(Integer bienTheId) {
        return tonKhoRepository.findByBienTheBienTheId(bienTheId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho biến thể id: " + bienTheId));
    }

    public TonKhoResponse getResponseByBienTheId(Integer bienTheId) {
        return tonKhoRepository.findResponseByBienTheId(bienTheId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho cho biến thể id: " + bienTheId));
    }

    public TonKho create(TonKho item) {
        item.setNgayCapNhat(LocalDateTime.now());
        return tonKhoRepository.save(item);
    }

    public TonKho update(Integer id, TonKho item) {
        TonKho existing = getById(id);
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
