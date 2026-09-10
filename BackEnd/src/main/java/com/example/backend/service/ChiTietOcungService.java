package com.example.backend.service;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.repository.ChiTietOcungRepository;
import com.example.backend.repository.DmOcungRepository;
import com.example.backend.request.ChiTietOcungRequest;
import com.example.backend.response.ChiTietOcungResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChiTietOcungService {

    private static final Pattern SERIAL_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]{3,50}$");

    @Autowired
    private ChiTietOcungRepository chiTietOcungRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;

    public List<ChiTietOcungResponse> hienThiChiTietOcung() {
        return chiTietOcungRepository.hienThiChiTietOcung();
    }

    public ChiTietOcung getById(Integer id) {
        return chiTietOcungRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial ổ cứng không tồn tại với id: " + id));
    }

    @Transactional
    public ChiTietOcung create(ChiTietOcungRequest request) {
        String serial = request.getSoSerial().trim();
        if (!SERIAL_PATTERN.matcher(serial).matches())
            throw new IllegalArgumentException("Số serial ổ cứng chỉ được chứa chữ cái, số và dấu gạch ngang (3-50 ký tự)");
        if (chiTietOcungRepository.existsBySoSerialIgnoreCase(serial))
            throw new IllegalArgumentException("Số serial \"" + serial + "\" đã tồn tại trong hệ thống");

        ChiTietOcung entity = new ChiTietOcung();
        BeanUtils.copyProperties(request, entity, "oCungId");
        entity.setSoSerial(serial);
        entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        entity.setTrangThai("trong_kho");
        return chiTietOcungRepository.save(entity);
    }

    @Transactional
    public ChiTietOcung update(Integer id, ChiTietOcungRequest request) {
        ChiTietOcung entity = getById(id);
        String serial = request.getSoSerial().trim();
        if (!serial.equalsIgnoreCase(entity.getSoSerial())) {
            if (!SERIAL_PATTERN.matcher(serial).matches())
                throw new IllegalArgumentException("Số serial ổ cứng chỉ được chứa chữ cái, số và dấu gạch ngang (3-50 ký tự)");
            if (chiTietOcungRepository.existsBySoSerialIgnoreCase(serial))
                throw new IllegalArgumentException("Số serial \"" + serial + "\" đã tồn tại trong hệ thống");
        }
        BeanUtils.copyProperties(request, entity, "chiTietOCungId", "oCungId");
        entity.setSoSerial(serial);
        entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        return chiTietOcungRepository.save(entity);
    }

    @Transactional
    public void delete(Integer id) {
        ChiTietOcung entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        // Ghi lịch sử trước khi xóa — để audit.
        // bien_the_id trong lich_su_ton_kho là NOT NULL nên bỏ qua ghi lichSu cho chi_tiet_o_cung
        // (ổ cứng rời không có bienThe, nằm ngoài luồng ton_kho chính). Toast thành công
        // ở FE đã thông báo cho nhân viên là serial đã xóa.
        chiTietOcungRepository.deleteById(id);
    }
}
