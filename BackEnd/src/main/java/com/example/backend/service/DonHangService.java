package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.LichSuDonHang;
import com.example.backend.entity.ThanhToan;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.DonHangRequest;
import com.example.backend.request.XacNhanDonHangLineRequest;
import com.example.backend.request.XacNhanDonHangRequest;
import com.example.backend.response.DonHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private LichSuDonHangRepository lichSuDonHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;

    public Page<DonHangResponse> hienThiDonHang(Integer khachHangId, Pageable pageable) {
        return donHangRepository.hienThiDonHang(khachHangId, pageable);
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

    @Transactional
    public DonHang update(Integer id, DonHangRequest request) {
        DonHang entity = getById(id);
        String oldStatus = entity.getTrangThaiDonHang();
        BeanUtils.copyProperties(request, entity,
                "id", "khachHangId", "nhanVienId", "khuyenMaiId", "diaChiGiaoHangId");

        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        entity.setKhuyenMai(request.getKhuyenMaiId() != null
                ? khuyenMaiRepository.getReferenceById(request.getKhuyenMaiId()) : null);
        entity.setDiaChiGiaoHang(request.getDiaChiGiaoHangId() != null
                ? diaChiGiaoHangRepository.getReferenceById(request.getDiaChiGiaoHangId()) : null);

        DonHang saved = donHangRepository.save(entity);

        // Đơn vừa chuyển sang "cancelled" (trước đó chưa hủy) -> trả serial đã giữ/đã bán
        // về lại "trong_kho", giống hệt delete() — nếu không, serial sẽ kẹt vĩnh viễn ở
        // "giu_hang"/"da_ban" dù đơn đã hủy, làm lệch tồn kho thật.
        if ("cancelled".equals(request.getTrangThaiDonHang()) && !"cancelled".equals(oldStatus)) {
            releaseSerialsToStock(id);
        }

        // Báo real-time cho admin (tab khác) + trang khách hàng đang mở đơn này, khỏi cần F5.
        sseService.notifyOrderUpdate(id);

        return saved;
    }

    // Trả toàn bộ serial đã gắn với đơn hàng về lại "trong_kho" — dùng chung khi xóa đơn
    // (delete) hoặc khi đơn chuyển sang trạng thái "cancelled" (update).
    private void releaseSerialsToStock(Integer donHangId) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(donHangId);
        for (ChiTietDonHang item : items) {
            // Serial đại diện trên FK đơn (chi_tiet_id) — luôn có nếu dòng đã gán serial.
            if (item.getChiTietSanPham() != null) {
                item.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(item.getChiTietSanPham());
            }
            // Đơn có so_luong > 1: các serial còn lại chỉ nằm trong bảng join, không nằm
            // trên FK đại diện — phải trả riêng, nếu không sẽ kẹt vĩnh viễn ở "giu_hang"/
            // "da_ban" dù đơn đã hủy, làm lệch tồn kho thật.
            for (ChiTietDonHangSerial link : chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId())) {
                link.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(link.getChiTietSanPham());
            }
        }
    }

    // Xoá đơn hàng — trước tiên xoá các dòng chi tiết + lịch sử tồn kho (FK) và trả
    // seri đã bán về "trong_kho" (trigger DB tự cộng lại tồn kho), nếu không sẽ vỡ FK
    // khi đơn đã có sản phẩm, và tồn kho sẽ bị lệch vì seri vẫn coi như đã bán.
    @Transactional
    public void delete(Integer id) {
        if (!donHangRepository.existsById(id))
            throw new IllegalArgumentException("Đơn hàng không tồn tại với id: " + id);

        releaseSerialsToStock(id);
        chiTietDonHangRepository.deleteAll(chiTietDonHangRepository.findEntityByDonHangId(id));
        lichSuTonKhoRepository.deleteAll(lichSuTonKhoRepository.findByDonHang_Id(id));
        donHangRepository.deleteById(id);
    }

    // Chọn serial cho từng dòng + chốt "da_ban" + chuyển trạng thái "confirmed" trong 1
    // transaction — chỉ áp dụng đơn online (đơn tại quầy đã chốt serial ngay lúc tạo dòng
    // đơn, không qua bước xác nhận nên không cần gọi endpoint này). Sau khi xác nhận xong,
    // bước "đóng gói" (confirmed → processing) chỉ còn là đổi trạng thái đơn thuần, serial
    // đã chốt xong ở đây rồi.
    @Transactional
    public void xacNhanDonHang(Integer donHangId, XacNhanDonHangRequest request) {
        DonHang donHang = getById(donHangId);
        if (!"online".equals(donHang.getKenhBan()))
            throw new IllegalArgumentException("Chỉ đơn hàng online mới cần chọn serial trước khi xác nhận");
        if (!"pending".equals(donHang.getTrangThaiDonHang()))
            throw new IllegalArgumentException("Đơn hàng phải ở trạng thái 'Chờ xác nhận' mới xác nhận được");

        for (XacNhanDonHangLineRequest line : request.getLines()) {
            ChiTietDonHang item = chiTietDonHangRepository.findById(line.getChiTietDonHangId())
                    .orElseThrow(() -> new IllegalArgumentException("Dòng đơn hàng không tồn tại với id: " + line.getChiTietDonHangId()));
            if (!item.getDonHang().getId().equals(donHangId))
                throw new IllegalArgumentException("Dòng #" + item.getId() + " không thuộc đơn hàng này");

            List<Integer> serialIds = line.getSerialIds();
            if (new HashSet<>(serialIds).size() != serialIds.size())
                throw new IllegalArgumentException("Dòng #" + item.getId() + " chọn trùng serial");
            if (serialIds.size() != item.getSoLuong())
                throw new IllegalArgumentException(
                        "Dòng #" + item.getId() + " cần đúng " + item.getSoLuong() + " serial, đã chọn " + serialIds.size());

            List<ChiTietDonHangSerial> existingLinks = chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId());
            Set<Integer> reservedForThisLine = existingLinks.stream()
                    .map(l -> l.getChiTietSanPham().getChiTietId())
                    .collect(Collectors.toSet());

            List<ChiTietSanPham> finalSerials = new ArrayList<>();
            for (Integer serialId : serialIds) {
                ChiTietSanPham serial = chiTietSanPhamRepository.findById(serialId)
                        .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + serialId));
                if (!serial.getBienThe().getBienTheId().equals(item.getBienThe().getBienTheId()))
                    throw new IllegalArgumentException("Serial " + serial.getSoSerial() + " không thuộc đúng sản phẩm của dòng #" + item.getId());
                boolean daGiuChoDongNay = reservedForThisLine.contains(serialId);
                if (!"trong_kho".equals(serial.getTrangThai()) && !daGiuChoDongNay)
                    throw new IllegalArgumentException("Serial " + serial.getSoSerial() + " không còn khả dụng, vui lòng chọn lại");
                finalSerials.add(serial);
            }

            // Trả các serial đã giữ chỗ trước đó nhưng bị bỏ chọn (admin đổi ý) về lại kho
            for (ChiTietDonHangSerial link : existingLinks) {
                if (!serialIds.contains(link.getChiTietSanPham().getChiTietId())) {
                    link.getChiTietSanPham().setTrangThai("trong_kho");
                    chiTietSanPhamRepository.save(link.getChiTietSanPham());
                }
            }
            chiTietDonHangSerialRepository.deleteByChiTietDonHang_Id(item.getId());

            for (ChiTietSanPham serial : finalSerials) {
                serial.setTrangThai("da_ban");
                chiTietSanPhamRepository.save(serial);
                ChiTietDonHangSerial link = new ChiTietDonHangSerial();
                link.setChiTietDonHang(item);
                link.setChiTietSanPham(serial);
                chiTietDonHangSerialRepository.save(link);
            }

            // Đồng bộ serial đại diện trên FK đơn — nếu admin đổi serial khác lúc xác nhận,
            // các nơi hiển thị dựa trên FK này (chi tiết đơn, bảo hành...) vẫn đúng.
            item.setChiTietSanPham(finalSerials.get(0));
            chiTietDonHangRepository.save(item);
        }

        donHang.setTrangThaiDonHang("confirmed");
        donHangRepository.save(donHang);
        sseService.notifyOrderUpdate(donHangId);
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
            DonHang source = getById(sourceId);
            // Đơn "pending" (chưa xác nhận) chưa chắc admin đã kiểm tra kỹ nội dung —
            // gộp lúc này dễ gộp nhầm đơn khách vừa đặt lỡ tay. Bắt buộc xác nhận trước.
            if ("pending".equals(source.getTrangThaiDonHang()))
                throw new IllegalArgumentException(
                        "Đơn #" + sourceId + " chưa được xác nhận, không thể gộp");
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
            for (LichSuDonHang lsdh : lichSuDonHangRepository.findByDonHangId(sourceId)) {
                lsdh.setDonHangId(target.getId());
                lichSuDonHangRepository.save(lsdh);
            }
            donHangRepository.deleteById(sourceId);
        }
        recalculateTongTien(targetId);
    }
}
