package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.DongTraRequest;
import com.example.backend.request.PhieuTraHangRequest;
import com.example.backend.request.YeuCauTraHangRequest;
import com.example.backend.response.PhieuTraHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PhieuTraHangService {

    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private ChiTietTraHangRepository chiTietTraHangRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    private String ensureMaPhieu(PhieuTraHang phieu) {
        if (phieu.getMaPhieu() != null && !phieu.getMaPhieu().isBlank()) {
            return phieu.getMaPhieu();
        }
        return "TR-" + phieu.getPhieuTraId();
    }

    private void ensureMaPhieuForSaved(PhieuTraHang phieu) {
        if (phieu.getPhieuTraId() == null) {
            return;
        }
        if (phieu.getMaPhieu() == null || phieu.getMaPhieu().isBlank()) {
            phieu.setMaPhieu(ensureMaPhieu(phieu));
            phieuTraHangRepository.save(phieu);
        }
    }

    public List<PhieuTraHangResponse> hienThiPhieuTraHang() {
        return phieuTraHangRepository.hienThiPhieuTraHang();
    }

    public PhieuTraHang getById(Integer id) {
        return phieuTraHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + id));
    }

    @Transactional
    public PhieuTraHang create(PhieuTraHangRequest request) {
        DonHang donHang = donHangRepository.findById(request.getDonHangId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + request.getDonHangId()));
        kiemTraGioiHanSoTienHoan(donHang, null, request.getSoTienHoan());

        PhieuTraHang entity = new PhieuTraHang();
        BeanUtils.copyProperties(request, entity, "donHangId", "nhanVienId");
        entity.setDonHang(donHang);
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        PhieuTraHang saved = phieuTraHangRepository.save(entity);
        ensureMaPhieuForSaved(saved);
        congViNeuVuaHoanTat(null, saved);
        truHoiDiemNeuVuaHoanTat(null, saved);
        capNhatKhoNeuVuaHoanTat(null, saved);
        capNhatDonHangNeuVuaHoanTat(null, saved);
        return saved;
    }

    @Transactional
    public PhieuTraHang update(Integer id, PhieuTraHangRequest request) {
        PhieuTraHang entity = getById(id);
        String trangThaiCu = entity.getTrangThai();
        chanSuaSauKhiDaCongVi(entity, request);
        DonHang donHang = donHangRepository.findById(request.getDonHangId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + request.getDonHangId()));
        kiemTraGioiHanSoTienHoan(donHang, entity.getPhieuTraId(), request.getSoTienHoan());
        BeanUtils.copyProperties(request, entity, "phieuTraId", "donHangId", "nhanVienId");
        entity.setDonHang(donHang);
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        PhieuTraHang saved = phieuTraHangRepository.save(entity);
        ensureMaPhieuForSaved(saved);
        congViNeuVuaHoanTat(trangThaiCu, saved);
        truHoiDiemNeuVuaHoanTat(trangThaiCu, saved);
        capNhatKhoNeuVuaHoanTat(trangThaiCu, saved);
        capNhatDonHangNeuVuaHoanTat(trangThaiCu, saved);
        return saved;
    }

    private void kiemTraGioiHanSoTienHoan(DonHang donHang, Integer excludePhieuId, BigDecimal soTienHoanMoi) {
        if (soTienHoanMoi == null || soTienHoanMoi.signum() <= 0) return;
        BigDecimal daHoanCacPhieuKhac = phieuTraHangRepository.findByDonHang_Id(donHang.getId()).stream()
                .filter(p -> excludePhieuId == null || !excludePhieuId.equals(p.getPhieuTraId()))
                .filter(p -> !"tu_choi".equals(p.getTrangThai()))
                .map(p -> p.getSoTienHoan() != null ? p.getSoTienHoan() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongSauKhiLuu = daHoanCacPhieuKhac.add(soTienHoanMoi);
        BigDecimal gioiHan = donHang.getThanhTien() != null ? donHang.getThanhTien() : donHang.getTongTien();
        if (gioiHan != null && tongSauKhiLuu.compareTo(gioiHan) > 0)
            throw new IllegalArgumentException(
                    "Tổng tiền hoàn của đơn #" + donHang.getId() + " (" + tongSauKhiLuu
                            + ") vượt quá số tiền đơn hàng đã thanh toán (" + gioiHan + ")");
    }

    private void chanSuaSauKhiDaCongVi(PhieuTraHang entity, PhieuTraHangRequest request) {
        boolean daXuLyRoi = "da_xu_ly".equals(entity.getTrangThai());
        if (!daXuLyRoi) return;

        boolean doiTrangThai = !"da_xu_ly".equals(request.getTrangThai());
        boolean doiHinhThucHoan = !java.util.Objects.equals(entity.getHinhThucHoan(), request.getHinhThucHoan());
        boolean doiSoTienHoan = entity.getSoTienHoan() == null
                ? request.getSoTienHoan() != null
                : entity.getSoTienHoan().compareTo(request.getSoTienHoan()) != 0;

        if (doiTrangThai || doiHinhThucHoan || doiSoTienHoan) {
            throw new IllegalArgumentException(
                    "Phiếu đã xử lý xong (đã cộng ví và/hoặc trừ điểm tích lũy) — không thể đổi trạng thái/hình thức hoàn/số tiền hoàn nữa");
        }
    }

    private void congViNeuVuaHoanTat(String trangThaiCu, PhieuTraHang phieu) {
        boolean vuaChuyenSangDaXuLy = "da_xu_ly".equals(phieu.getTrangThai()) && !"da_xu_ly".equals(trangThaiCu);
        if (!vuaChuyenSangDaXuLy) return;
        if (!"vi".equals(phieu.getHinhThucHoan())) return;
        if (phieu.getSoTienHoan() == null || phieu.getSoTienHoan().signum() <= 0) return;

        KhachHang khachHang = phieu.getDonHang().getKhachHang();
        khachHang.setSoDuVi(khachHang.getSoDuVi().add(phieu.getSoTienHoan()));
        khachHangRepository.save(khachHang);
    }

    private void truHoiDiemNeuVuaHoanTat(String trangThaiCu, PhieuTraHang phieu) {
        boolean vuaChuyenSangDaXuLy = "da_xu_ly".equals(phieu.getTrangThai()) && !"da_xu_ly".equals(trangThaiCu);
        if (!vuaChuyenSangDaXuLy) return;
        if (phieu.getSoTienHoan() == null || phieu.getSoTienHoan().signum() <= 0) return;

        KhachHang khachHang = phieu.getDonHang().getKhachHang();
        int diemTru = phieu.getSoTienHoan()
                .divide(BigDecimal.valueOf(10000), 0, java.math.RoundingMode.FLOOR)
                .intValue();
        int diemHienTai = khachHang.getDiemTichLuy() != null ? khachHang.getDiemTichLuy() : 0;
        khachHang.setDiemTichLuy(Math.max(0, diemHienTai - diemTru));
        khachHangRepository.save(khachHang);
    }

    private void capNhatDonHangNeuVuaHoanTat(String trangThaiCu, PhieuTraHang phieu) {
        boolean vuaChuyenSangDaXuLy = "da_xu_ly".equals(phieu.getTrangThai()) && !"da_xu_ly".equals(trangThaiCu);
        if (!vuaChuyenSangDaXuLy) return;

        DonHang donHang = phieu.getDonHang();
        boolean coDoi = false;
        if (phieu.getSoTienHoan() != null && phieu.getSoTienHoan().signum() > 0) {
            donHang.setTrangThaiThanhToan("refunded");
            coDoi = true;
        }
        if ("delivered".equals(donHang.getTrangThaiDonHang())) {
            donHang.setTrangThaiDonHang("returned");
            coDoi = true;
        }
        if (coDoi) donHangRepository.save(donHang);
    }

    private void capNhatKhoNeuVuaHoanTat(String trangThaiCu, PhieuTraHang phieu) {
        boolean vuaChuyenSangDaXuLy = "da_xu_ly".equals(phieu.getTrangThai()) && !"da_xu_ly".equals(trangThaiCu);
        if (!vuaChuyenSangDaXuLy) return;

        List<ChiTietTraHang> dongTraHang = chiTietTraHangRepository.findByPhieuTraHang_PhieuTraId(phieu.getPhieuTraId());
        for (ChiTietTraHang dong : dongTraHang) {
            ChiTietSanPham serial = dong.getChiTietSanPham();
            if (serial == null || laHangLoi(dong.getTinhTrang())) continue;
            serial.setTrangThai("trong_kho");
            chiTietSanPhamRepository.save(serial);
        }
    }

    private boolean laHangLoi(String tinhTrang) {
        if (tinhTrang == null) return false;
        String s = tinhTrang.toLowerCase(java.util.Locale.ROOT);
        return s.contains("lỗi") || s.contains("loi") || s.contains("hỏng") || s.contains("hong") || s.contains("hư");
    }


    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private void assertIsOwner(DonHang donHang) {
        TaiKhoan tk = currentAccount();
        boolean laChuDon = tk != null && tk.getKhachHang() != null && donHang.getKhachHang() != null
                && donHang.getKhachHang().getKhachHangId().equals(tk.getKhachHang().getKhachHangId());
        if (!laChuDon)
            throw new AccessDeniedException("Không có quyền tạo yêu cầu trả hàng cho đơn này");
    }

    private boolean isStaffOrOwner(DonHang donHang) {
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;
        return tk.getKhachHang() != null && donHang.getKhachHang() != null
                && donHang.getKhachHang().getKhachHangId().equals(tk.getKhachHang().getKhachHangId());
    }

    @Transactional
    public PhieuTraHang taoYeuCauTuKhachHang(YeuCauTraHangRequest request) {
        DonHang donHang = donHangRepository.findById(request.getDonHangId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + request.getDonHangId()));
        assertIsOwner(donHang);

        if (!"delivered".equals(donHang.getTrangThaiDonHang()))
            throw new IllegalArgumentException("Chỉ có thể yêu cầu trả hàng khi đơn đã giao");
        if (donHang.getNgayGiaoThucTe() == null
                || LocalDateTime.now().isAfter(donHang.getNgayGiaoThucTe().plusDays(7)))
            throw new IllegalArgumentException("Đã quá hạn 7 ngày trả hàng kể từ khi nhận hàng");

        boolean coPhieuActive = phieuTraHangRepository.findByDonHang_Id(donHang.getId()).stream()
                .anyMatch(p -> "cho_xu_ly".equals(p.getTrangThai()) || "da_xu_ly".equals(p.getTrangThai()));
        if (coPhieuActive)
            throw new IllegalArgumentException("Đơn này đã có yêu cầu trả hàng đang xử lý");

        java.util.Map<Integer, Integer> tongSoLuongTheoDong = new java.util.HashMap<>();
        for (DongTraRequest d : request.getDongTra())
            tongSoLuongTheoDong.merge(d.getChiTietDonHangId(), d.getSoLuong(), Integer::sum);
        for (var entry : tongSoLuongTheoDong.entrySet()) {
            ChiTietDonHang item = chiTietDonHangRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Dòng đơn hàng không tồn tại với id: " + entry.getKey()));
            if (!item.getDonHang().getId().equals(donHang.getId()))
                throw new IllegalArgumentException("Dòng #" + item.getId() + " không thuộc đơn hàng này");
            if (entry.getValue() > item.getSoLuong())
                throw new IllegalArgumentException(
                        "Dòng #" + item.getId() + " chỉ mua " + item.getSoLuong() + ", không thể trả " + entry.getValue());
        }

        List<ChiTietTraHang> dongTraHang = new ArrayList<>();
        BigDecimal tongTienHoan = BigDecimal.ZERO;
        for (DongTraRequest d : request.getDongTra()) {
            ChiTietDonHang item = chiTietDonHangRepository.findById(d.getChiTietDonHangId())
                    .orElseThrow(() -> new IllegalArgumentException("Dòng đơn hàng không tồn tại với id: " + d.getChiTietDonHangId()));

            ChiTietTraHang dong = new ChiTietTraHang();
            dong.setBienThe(item.getBienThe());
            dong.setChiTietSanPham(item.getChiTietSanPham());
            dong.setSoLuong(d.getSoLuong());
            dong.setDonGiaHoan(item.getDonGia());
            dongTraHang.add(dong);
            tongTienHoan = tongTienHoan.add(item.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())));
        }

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setDonHang(donHang);
        phieu.setNhanVien(null);
        phieu.setLyDo(request.getLyDo());
        phieu.setNgayTra(LocalDateTime.now());
        phieu.setTrangThai("cho_xu_ly");
        phieu.setSoTienHoan(tongTienHoan);
        phieu.setHinhThucHoan("vi");
        PhieuTraHang saved = phieuTraHangRepository.save(phieu);
        ensureMaPhieuForSaved(saved);

        for (ChiTietTraHang dong : dongTraHang) {
            dong.setPhieuTraHang(saved);
            chiTietTraHangRepository.save(dong);
        }
        return saved;
    }

    public List<PhieuTraHangResponse> getByDonHang(Integer donHangId) {
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + donHangId));
        if (!isStaffOrOwner(donHang))
            throw new AccessDeniedException("Không có quyền xem yêu cầu trả hàng của đơn này");
        return phieuTraHangRepository.findByDonHang_Id(donHangId).stream()
                .map(p -> new PhieuTraHangResponse(
                        p.getPhieuTraId(), p.getDonHang().getId(),
                        p.getNhanVien() != null ? p.getNhanVien().getNhanVienId() : null,
                        p.getLyDo(), p.getNgayTra(), p.getTrangThai(), p.getSoTienHoan(), p.getHinhThucHoan(), p.getGhiChu(),
                        p.getMaPhieu() != null && !p.getMaPhieu().isBlank() ? p.getMaPhieu() : ensureMaPhieu(p)))
                .toList();
    }

}
