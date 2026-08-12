package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietDonHangSerialRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.LichSuTonKhoRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.response.ChiTietDonHangResponse;
import com.example.backend.response.ChiTietDonHangSerialResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChiTietDonHangService {

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    public List<ChiTietDonHangResponse> hienThiChiTietDonHang() {
        return chiTietDonHangRepository.hienThiChiTietDonHang();
    }

    public List<ChiTietDonHangResponse> getByDonHangId(Integer donHangId) {
        if (!isStaffOrOwner(donHangId))
            throw new AccessDeniedException("Không có quyền xem đơn hàng này");
        return chiTietDonHangRepository.findByDonHangId(donHangId);
    }

    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private boolean isStaffOrOwner(Integer donHangId) {
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + donHangId));
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;
        return tk.getKhachHang() != null
                && donHang.getKhachHang() != null
                && tk.getKhachHang().getKhachHangId().equals(donHang.getKhachHang().getKhachHangId());
    }

    public ChiTietDonHang getById(Integer id) {
        return chiTietDonHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn hàng không tồn tại với id: " + id));
    }

    @Transactional
    public ChiTietDonHang create(ChiTietDonHangRequest request) {
        if (!isStaffOrOwner(request.getDonHangId()))
            throw new AccessDeniedException("Không có quyền thêm sản phẩm vào đơn hàng này");

        ChiTietDonHang entity = new ChiTietDonHang();
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "chiTietId", "donGia", "giamGiaDong");

        DonHang donHang = donHangRepository.getReferenceById(request.getDonHangId());
        entity.setDonHang(donHang);
        BienTheSanPham bienThe = bienTheSanPhamRepository.findById(request.getBienTheId())
                .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại với id: " + request.getBienTheId()));
        entity.setBienThe(bienThe);

        entity.setDonGia(bienThe.getGiaBan());
        entity.setGiamGiaDong(java.math.BigDecimal.ZERO);

        List<ChiTietSanPham> assignedSerials;
        if (request.getChiTietId() != null) {
            ChiTietSanPham chosen = chiTietSanPhamRepository.findByIdForUpdate(request.getChiTietId())
                    .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + request.getChiTietId()));
            if (!"trong_kho".equals(chosen.getTrangThai()))
                throw new IllegalArgumentException("Serial này đã được gán cho đơn khác, vui lòng chọn serial khác");
            entity.setChiTietSanPham(chosen);
            assignedSerials = List.of(chosen);
        } else {
            int soLuong = request.getSoLuong() != null ? request.getSoLuong() : 1;
            List<ChiTietSanPham> available = chiTietSanPhamRepository
                    .findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(request.getBienTheId(), "trong_kho");
            if (available.size() < soLuong)
                throw new IllegalArgumentException(
                        "Không đủ hàng trong kho: cần " + soLuong + ", còn " + available.size());
            assignedSerials = available.subList(0, soLuong);
            entity.setChiTietSanPham(assignedSerials.get(0));
        }

        ChiTietDonHang saved = chiTietDonHangRepository.save(entity);

        boolean online = "online".equals(donHang.getKenhBan());
        String trangThaiMoi = online ? "giu_hang" : "da_ban";

        for (ChiTietSanPham serial : assignedSerials) {
            serial.setTrangThai(trangThaiMoi);
            chiTietSanPhamRepository.save(serial);
            ChiTietDonHangSerial link = new ChiTietDonHangSerial();
            link.setChiTietDonHang(saved);
            link.setChiTietSanPham(serial);
            chiTietDonHangSerialRepository.save(link);
        }

        LichSuTonKho lichSu = new LichSuTonKho();
        lichSu.setBienThe(entity.getBienThe());
        lichSu.setChiTietSanPham(assignedSerials.isEmpty() ? null : assignedSerials.get(0));
        lichSu.setLoaiBienDong(online ? "giu_hang" : "xuat_ban");
        lichSu.setSoLuongThayDoi(-assignedSerials.size());
        lichSu.setDonHang(entity.getDonHang());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSu.setGhiChu(online
                ? "Giữ chỗ — đơn #" + request.getDonHangId()
                : "Bán hàng — đơn #" + request.getDonHangId());
        lichSuTonKhoRepository.save(lichSu);

        return saved;
    }

    @Transactional
    public ChiTietDonHang update(Integer id, ChiTietDonHangRequest request) {
        ChiTietDonHang entity = getById(id);
        ChiTietSanPham serialCu = entity.getChiTietSanPham();
        boolean doiSerial = serialCu != null
                && (request.getChiTietId() == null || !serialCu.getChiTietId().equals(request.getChiTietId()));
        if (doiSerial) {
            serialCu.setTrangThai("trong_kho");
            chiTietSanPhamRepository.save(serialCu);
        }

        BeanUtils.copyProperties(request, entity, "id", "donHangId", "bienTheId", "chiTietId");

        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);

        return chiTietDonHangRepository.save(entity);
    }

    @Transactional
    public void delete(Integer id) {
        ChiTietDonHang entity = getById(id);
        if (entity.getChiTietSanPham() != null) {
            entity.getChiTietSanPham().setTrangThai("trong_kho");
            chiTietSanPhamRepository.save(entity.getChiTietSanPham());
        }
        for (ChiTietDonHangSerial link : chiTietDonHangSerialRepository.findByChiTietDonHang_Id(id)) {
            link.getChiTietSanPham().setTrangThai("trong_kho");
            chiTietSanPhamRepository.save(link.getChiTietSanPham());
        }
        chiTietDonHangSerialRepository.deleteByChiTietDonHang_Id(id);
        chiTietDonHangRepository.deleteById(id);
    }

    public List<ChiTietDonHangSerialResponse> getSerialsByDonHangId(Integer donHangId) {
        return chiTietDonHangSerialRepository.findByDonHangId(donHangId);
    }
}
