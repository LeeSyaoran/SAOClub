package com.example.backend.service;

import com.example.backend.entity.CauHinhVongQuay;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.KhuyenMai;
import com.example.backend.entity.LichSuQuay;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.CauHinhVongQuayRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.KhuyenMaiRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.CauHinhVongQuayRequest;
import com.example.backend.response.CauHinhVongQuayResponse;
import com.example.backend.response.KetQuaQuayResponse;
import com.example.backend.response.KhuyenMaiResponse;
import com.example.backend.response.LichSuQuayResponse;
import com.example.backend.response.PhieuGiamGiaCaNhanResponse;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class VongQuayService {

    @Autowired private CauHinhVongQuayRepository cauHinhRepository;
    @Autowired private LichSuQuayRepository lichSuQuayRepository;
    @Autowired private KhuyenMaiRepository khuyenMaiRepository;
    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private EntityManager entityManager;

    private static final Random RANDOM = new Random();

    // Giống PhieuGiamGiaCaNhanService.currentKhachHangId() — chặn tài khoản staff gọi nhầm.
    private Integer currentKhachHangId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan tk = taiKhoanRepository.findByUsername(username).orElse(null);
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Chỉ khách hàng mới quay được vòng quay");
        return tk.getKhachHang().getKhachHangId();
    }

    @Transactional
    public CauHinhVongQuay getOrCreateCauHinh() {
        return cauHinhRepository.findById(1)
                .orElseGet(() -> cauHinhRepository.save(
                        new CauHinhVongQuay(1, 100, 30, LocalDateTime.now())));
    }

    public CauHinhVongQuayResponse getCauHinhChoKhachHang() {
        CauHinhVongQuay ch = getOrCreateCauHinh();
        List<KhuyenMaiResponse> khaDung = khuyenMaiRepository.findActiveKhaDung().stream()
                .map(VongQuayService::toKhuyenMaiResponse)
                .toList();
        return new CauHinhVongQuayResponse(ch.getDiemMoiLuot(), ch.getTyLeTruot(), khaDung);
    }

    // KhuyenMaiResponse chỉ có @AllArgsConstructor (13 field) — không có constructor nhận
    // thẳng KhuyenMai, nên map tường minh, dùng lại ở cả getCauHinhChoKhachHang() lẫn quay().
    private static KhuyenMaiResponse toKhuyenMaiResponse(KhuyenMai k) {
        return new KhuyenMaiResponse(k.getKhuyenMaiId(), k.getMaKhuyenMai(), k.getTenKhuyenMai(),
                k.getLoai(), k.getGiaTri(), k.getGiaTriToiDa(), k.getDonHangToiThieu(),
                k.getNgayBatDau(), k.getNgayKetThuc(), k.getSoLuongToiDa(), k.getSoLanDaDung(),
                k.getTrangThai(), k.getNgayTao());
    }

    @Transactional
    public CauHinhVongQuay capNhatCauHinh(CauHinhVongQuayRequest req) {
        CauHinhVongQuay ch = getOrCreateCauHinh();
        ch.setDiemMoiLuot(req.getDiemMoiLuot());
        ch.setTyLeTruot(req.getTyLeTruot());
        ch.setNgayCapNhat(LocalDateTime.now());
        return cauHinhRepository.save(ch);
    }

    @Transactional
    public KetQuaQuayResponse quay() {
        Integer khachHangId = currentKhachHangId();
        CauHinhVongQuay cauHinh = getOrCreateCauHinh();

        // Khoá ghi — chặn 2 lượt quay đồng thời cùng đọc trùng số dư điểm (double-spend),
        // đúng pattern PhieuGiamGiaCaNhanService.doiThuong().
        KhachHang khachHang = khachHangRepository.findWithLockByKhachHangId(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
        if (khachHang.getDiemTichLuy() < cauHinh.getDiemMoiLuot())
            throw new IllegalArgumentException("Không đủ điểm để quay");

        khachHang.setDiemTichLuy(khachHang.getDiemTichLuy() - cauHinh.getDiemMoiLuot());
        khachHangRepository.save(khachHang);

        LichSuQuay lichSu = new LichSuQuay();
        lichSu.setKhachHang(khachHang);
        lichSu.setNgayQuay(LocalDateTime.now());
        lichSu.setDiemDaTru(cauHinh.getDiemMoiLuot());

        List<KhuyenMai> khaDung = khuyenMaiRepository.findActiveKhaDung();
        boolean truot = khaDung.isEmpty() || RANDOM.nextInt(100) < cauHinh.getTyLeTruot();

        if (truot) {
            lichSu.setKetQua("truot");
            lichSuQuayRepository.save(lichSu);
            return new KetQuaQuayResponse("truot", null, null, khachHang.getDiemTichLuy());
        }

        KhuyenMai trung = khaDung.get(RANDOM.nextInt(khaDung.size()));

        PhieuGiamGiaCaNhan phieu = new PhieuGiamGiaCaNhan();
        phieu.setKhachHang(khachHang);
        phieu.setDoiThuong(null); // phiếu từ vòng quay, không gắn danh mục đổi thưởng
        phieu.setLoai(trung.getLoai());
        // gia_tri của khuyen_mai là DECIMAL(18,2), của phieu_giam_gia_ca_nhan là DECIMAL(18,0)
        // — làm tròn khi clone để tránh lệch giữa entity Java và cột DB.
        phieu.setGiaTri(trung.getGiaTri().setScale(0, RoundingMode.HALF_UP));
        phieu.setGiaTriToiDa(trung.getGiaTriToiDa() == null ? null
                : trung.getGiaTriToiDa().setScale(0, RoundingMode.HALF_UP));
        phieu.setDaSuDung(false);
        phieu.setNgayDoi(LocalDateTime.now());
        phieu.setNgayHetHan(LocalDateTime.now().plusDays(30));
        PhieuGiamGiaCaNhan savedPhieu = phieuGiamGiaCaNhanRepository.save(phieu);
        // Hibernate không tự động re-read DB-generated DEFAULT column (maPhieu).
        // Trong @Transactional, findById() chỉ trả cached object — phải dùng entityManager.refresh()
        // để buộc Hibernate SELECT lại từ DB và update fields của object hiện tại.
        entityManager.refresh(savedPhieu);

        lichSu.setKetQua("trung");
        lichSu.setKhuyenMai(trung);
        lichSu.setPhieuGiamGiaCaNhan(savedPhieu);
        lichSuQuayRepository.save(lichSu);

        return new KetQuaQuayResponse("trung", toKhuyenMaiResponse(trung),
                new PhieuGiamGiaCaNhanResponse(savedPhieu.getPhieuId(), savedPhieu.getMaPhieu(),
                        savedPhieu.getLoai(), savedPhieu.getGiaTri(), savedPhieu.getGiaTriToiDa(),
                        savedPhieu.getDaSuDung(), savedPhieu.getNgayDoi(), savedPhieu.getNgayHetHan()),
                khachHang.getDiemTichLuy());
    }

    public List<LichSuQuayResponse> getLichSuCuaToi() {
        return lichSuQuayRepository.findResponsesByKhachHangId(currentKhachHangId());
    }
}
