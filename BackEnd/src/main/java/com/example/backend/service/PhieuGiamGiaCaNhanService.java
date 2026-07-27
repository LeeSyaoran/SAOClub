package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.TangVoucherRequest;
import com.example.backend.response.PhieuGiamGiaCaNhanResponse;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PhieuGiamGiaCaNhanService {

    @Autowired
    private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Autowired
    private DmDoiThuongRepository dmDoiThuongRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private LichSuQuayRepository lichSuQuayRepository;
    @Autowired
    private EntityManager entityManager;

    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private Integer currentKhachHangId() {
        TaiKhoan tk = currentAccount();
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Chỉ khách hàng mới đổi được điểm thưởng");
        return tk.getKhachHang().getKhachHangId();
    }

    private KhachHang currentKhachHang() {
        return khachHangRepository.findById(currentKhachHangId())
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
    }

    @Transactional
    public PhieuGiamGiaCaNhan doiThuong(Integer doiThuongId) {
        DmDoiThuong doiThuong = dmDoiThuongRepository.findById(doiThuongId)
                .orElseThrow(() -> new IllegalArgumentException("Phần thưởng không tồn tại với id: " + doiThuongId));
        if (!"active".equals(doiThuong.getTrangThai()))
            throw new IllegalArgumentException("Phần thưởng này hiện không khả dụng");

        // Khóa ghi để đọc số dư điểm mới nhất — chặn 2 request đổi điểm đồng thời cùng
        // đọc trùng số dư rồi cùng trừ (double-spend điểm).
        KhachHang khachHang = khachHangRepository.findWithLockByKhachHangId(currentKhachHangId())
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
        if (khachHang.getDiemTichLuy() < doiThuong.getDiemCan())
            throw new IllegalArgumentException("Không đủ điểm để đổi phần thưởng này");

        khachHang.setDiemTichLuy(khachHang.getDiemTichLuy() - doiThuong.getDiemCan());
        khachHangRepository.save(khachHang);

        PhieuGiamGiaCaNhan phieu = new PhieuGiamGiaCaNhan();
        phieu.setKhachHang(khachHang);
        phieu.setDoiThuong(doiThuong);
        phieu.setLoai(doiThuong.getLoai());
        phieu.setGiaTri(doiThuong.getGiaTri());
        phieu.setGiaTriToiDa(doiThuong.getGiaTriToiDa());
        phieu.setDaSuDung(false);
        phieu.setNgayDoi(LocalDateTime.now());
        phieu.setNgayHetHan(LocalDateTime.now().plusDays(30));
        return phieuGiamGiaCaNhanRepository.save(phieu);
    }

    // Admin tặng voucher trực tiếp — không qua đổi điểm, doiThuong=null (giống hệt cách
    // voucher trúng vòng quay cũng để doiThuong=null, xem VongQuayService.quay()).
    @Transactional
    public PhieuGiamGiaCaNhan taoVoucherAdmin(Integer khachHangId, TangVoucherRequest request) {
        if ("percent".equals(request.getLoai()) && request.getGiaTri().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Voucher giảm theo % không được vượt quá 100%");
        if (!request.getNgayHetHan().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Hạn sử dụng phải ở tương lai");

        KhachHang khachHang = khachHangRepository.findById(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + khachHangId));

        PhieuGiamGiaCaNhan phieu = new PhieuGiamGiaCaNhan();
        phieu.setKhachHang(khachHang);
        phieu.setLoai(request.getLoai());
        phieu.setGiaTri(request.getGiaTri());
        phieu.setGiaTriToiDa(request.getGiaTriToiDa());
        phieu.setDaSuDung(false);
        phieu.setNgayDoi(LocalDateTime.now());
        phieu.setNgayHetHan(request.getNgayHetHan());
        phieu.setDonHangToiThieu(request.getDonHangToiThieu());
        PhieuGiamGiaCaNhan savedPhieu = phieuGiamGiaCaNhanRepository.save(phieu);
        // maPhieu là DEFAULT sinh bởi DB, insertable=false trên entity — Hibernate không tự
        // re-select sau INSERT, phải refresh() để đọc lại giá trị thật (giống VongQuayService.quay()).
        entityManager.refresh(savedPhieu);
        return savedPhieu;
    }

    // Admin xem toàn bộ voucher của 1 khách — khác getCuaToi() (tự phục vụ, suy khách hàng
    // từ SecurityContextHolder) vì admin cần xem CỦA NGƯỜI KHÁC theo id truyền vào.
    public List<PhieuGiamGiaCaNhanResponse> getByKhachHangIdForAdmin(Integer khachHangId) {
        List<Integer> phieuIdTrungThuong = lichSuQuayRepository.findPhieuIdsByKhachHangId(khachHangId);
        return phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(khachHangId).stream()
                .map(p -> new PhieuGiamGiaCaNhanResponse(
                        p.getPhieuId(), p.getMaPhieu(), p.getLoai(), p.getGiaTri(), p.getGiaTriToiDa(),
                        p.getDaSuDung(), p.getNgayDoi(), p.getNgayHetHan(), p.getDonHangToiThieu(),
                        p.getDoiThuong() != null || phieuIdTrungThuong.contains(p.getPhieuId())
                                ? "Khách tự đổi / trúng thưởng" : "Admin tặng"))
                .toList();
    }

    public List<PhieuGiamGiaCaNhanResponse> getCuaToi() {
        KhachHang khachHang = currentKhachHang();
        return phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(khachHang.getKhachHangId()).stream()
                .map(p -> new PhieuGiamGiaCaNhanResponse(
                        p.getPhieuId(), p.getMaPhieu(), p.getLoai(), p.getGiaTri(), p.getGiaTriToiDa(),
                        p.getDaSuDung(), p.getNgayDoi(), p.getNgayHetHan(), p.getDonHangToiThieu(), null))
                .toList();
    }
}
