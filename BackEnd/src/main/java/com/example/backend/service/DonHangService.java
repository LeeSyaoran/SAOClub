package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.ThanhToan;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.DonHangRequest;
import com.example.backend.response.DonHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DonHangService {

    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private SseService sseService;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;
    @Autowired
    private DiaChiGiaoHangRepository diaChiGiaoHangRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private ThanhToanRepository thanhToanRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<DonHangResponse> hienThiDonHang() {
        return donHangRepository.hienThiDonHang();
    }

    public DonHang getById(Integer id) {
        return donHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id));
    }

    public DonHang create(DonHangRequest request) {
        DonHang entity = new DonHang();
        // BeanUtils copies: maDonHang, diaChiGiaoHangText, nguoiNhan, sdtNguoiNhan,
        //                   tongTien, giamGia, phiVanChuyen, thanhTien, ngayDat,
        //                   ngayGiaoDuKien, ngayGiaoThucTe, trangThaiDonHang,
        //                   trangThaiThanhToan, kenhBan, ghiChu
        // Bỏ qua: khachHangId, nhanVienId, khuyenMaiId, diaChiGiaoHangId (khác tên với entity)
        BeanUtils.copyProperties(request, entity,
                "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId");

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        // nhanVienId nullable: nhân viên không bắt buộc (đơn hàng online)
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        if (request.getKhuyenMaiId() != null)
            entity.setKhuyenMai(khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()));
        if (request.getDiaChiGiaoHangId() != null)
            entity.setDiaChiGiaoHang(diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()));

        DonHang saved = donHangRepository.save(entity);
        sseService.notifyNewOrder(saved.getId());
        return saved;
    }

    public DonHang update(Integer id, DonHangRequest request) {
        DonHang entity = getById(id);
        BeanUtils.copyProperties(request, entity,
                "id", "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId");

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        entity.setKhuyenMai(request.getKhuyenMaiId() != null
                ? khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()) : null);
        entity.setDiaChiGiaoHang(request.getDiaChiGiaoHangId() != null
                ? diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()) : null);

        return donHangRepository.save(entity);
    }

    // Xoá đơn hàng — trước tiên xoá các dòng chi tiết + lịch sử tồn kho (FK) và trả
    // seri đã bán về "trong_kho" (trigger DB tự cộng lại tồn kho), nếu không sẽ vỡ FK
    // khi đơn đã có sản phẩm, và tồn kho sẽ bị lệch vì seri vẫn coi như đã bán.
    @Transactional
    public void delete(Integer id) {
        if (!donHangRepository.existsById(id))
            throw new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id);

        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(id);
        for (ChiTietDonHang item : items) {
            if (item.getChiTietSanPham() != null) {
                item.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(item.getChiTietSanPham());
            }
        }
        chiTietDonHangRepository.deleteAll(items);
        lichSuTonKhoRepository.deleteAll(lichSuTonKhoRepository.findByDonHang_Id(id));
        donHangRepository.deleteById(id);
    }

    // Tính lại tong_tien từ tổng các dòng chi tiết (don_gia * so_luong - giam_gia_dong)
    public void recalculateTongTien(Integer orderId) {
        DonHang order = getById(orderId);
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(orderId);
        BigDecimal total = items.stream()
            .map(item -> {
                BigDecimal line = item.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong()));
                return item.getGiamGiaDong() != null ? line.subtract(item.getGiamGiaDong()) : line;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTongTien(total);
        donHangRepository.save(order);
    }

    // Gộp nhiều đơn hàng vào 1 đơn đích — toàn bộ chi tiết chuyển sang targetId, các source bị xóa
    @Transactional
    public void mergeOrders(Integer targetId, List<Integer> sourceIds) {
        DonHang target = getById(targetId);
        for (Integer sourceId : sourceIds) {
            if (sourceId.equals(targetId)) continue;
            getById(sourceId); // validate tồn tại trước khi thao tác
            List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(sourceId);
            for (ChiTietDonHang item : items) {
                item.setDonHang(target);
                chiTietDonHangRepository.save(item);
            }
            // Chuyển các bản ghi phụ thuộc khác sang targetId trước khi xóa source —
            // nếu không, DELETE sẽ vi phạm FK (thanh_toan/lich_su_ton_kho/phieu_tra_hang/
            // phieu_bao_hanh đều tham chiếu don_hang_id, không có ON DELETE CASCADE).
            for (ThanhToan tt : thanhToanRepository.findByDonHang_Id(sourceId)) {
                tt.setDonHang(target);
                thanhToanRepository.save(tt);
            }
            for (LichSuTonKho lstk : lichSuTonKhoRepository.findByDonHang_Id(sourceId)) {
                lstk.setDonHang(target);
                lichSuTonKhoRepository.save(lstk);
            }
            for (PhieuTraHang ptr : phieuTraHangRepository.findByDonHang_Id(sourceId)) {
                ptr.setDonHang(target);
                phieuTraHangRepository.save(ptr);
            }
            for (PhieuBaoHanh pbh : phieuBaoHanhRepository.findByDonHang_Id(sourceId)) {
                pbh.setDonHang(target);
                phieuBaoHanhRepository.save(pbh);
            }
            donHangRepository.deleteById(sourceId);
        }
        recalculateTongTien(targetId);
    }
}
