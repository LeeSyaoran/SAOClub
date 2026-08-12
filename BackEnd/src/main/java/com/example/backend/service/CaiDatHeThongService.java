package com.example.backend.service;

import com.example.backend.entity.CaiDatHeThong;
import com.example.backend.repository.CaiDatHeThongRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaiDatHeThongService {

    private static final int ID = 1;

    @Autowired
    private CaiDatHeThongRepository caiDatHeThongRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;

    private CaiDatHeThong getEntity() {
        return caiDatHeThongRepository.findById(ID)
                .orElseThrow(() -> new IllegalStateException("Chưa khởi tạo cài đặt hệ thống"));
    }

    public CaiDatHeThongResponse get() {
        CaiDatHeThong c = getEntity();
        return new CaiDatHeThongResponse(c.getTenCuaHang(), c.getDiaChi(), c.getSoDienThoai(),
                c.getEmail(), c.getMaSoThue(), c.getLogoUrl(), c.getNguongTonKhoMacDinh(),
                c.getNgonNguMacDinh(), c.getDinhDangSo());
    }

    public CaiDatHeThongResponse update(CaiDatHeThongRequest req) {
        CaiDatHeThong c = getEntity();
        c.setTenCuaHang(req.getTenCuaHang());
        c.setDiaChi(req.getDiaChi());
        c.setSoDienThoai(req.getSoDienThoai());
        c.setEmail(req.getEmail());
        c.setMaSoThue(req.getMaSoThue());
        c.setLogoUrl(req.getLogoUrl());
        c.setNgonNguMacDinh(req.getNgonNguMacDinh());
        c.setDinhDangSo(req.getDinhDangSo());
        caiDatHeThongRepository.save(c);
        return get();
    }

    @Transactional
    public int apDungNguongTonKhoChoTatCa(int nguong) {
        CaiDatHeThong c = getEntity();
        c.setNguongTonKhoMacDinh(nguong);
        caiDatHeThongRepository.save(c);
        return tonKhoRepository.capNhatNguongChoTatCa(nguong);
    }
}
