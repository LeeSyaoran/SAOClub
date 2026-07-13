package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietDonHangSerialRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.LichSuTonKhoRepository;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.response.ChiTietDonHangResponse;
import com.example.backend.response.ChiTietDonHangSerialResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<ChiTietDonHangResponse> hienThiChiTietDonHang() {
        return chiTietDonHangRepository.hienThiChiTietDonHang();
    }

    public List<ChiTietDonHangResponse> getByDonHangId(Integer donHangId) {
        return chiTietDonHangRepository.findByDonHangId(donHangId);
    }

    public ChiTietDonHang getById(Integer id) {
        return chiTietDonHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn hàng không tồn tại với id: " + id));
    }

    @Transactional
    public ChiTietDonHang create(ChiTietDonHangRequest request) {
        ChiTietDonHang entity = new ChiTietDonHang();
        // BeanUtils copies: soLuong, donGia, giamGiaDong, ghiChu
        // Bỏ qua: donHangId, bienTheId, chiTietId (khác tên với entity)
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "chiTietId");

        DonHang donHang = donHangRepository.getReferenceById(request.getDonHangId());
        entity.setDonHang(donHang);
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));

        // Gán serial cụ thể cho dòng đơn hàng này + trừ tồn kho.
        // ChiTietSanPham.trangThai chuyển khỏi "trong_kho" sẽ tự kích hoạt trigger
        // trg_CapNhatTonKhoThucTe trừ ton_kho.so_luong_ton_thuc_te tương ứng.
        List<ChiTietSanPham> assignedSerials;
        if (request.getChiTietId() != null) {
            // Đã chỉ định seri cụ thể (vd: nhân viên chọn tay tại quầy)
            ChiTietSanPham chosen = chiTietSanPhamRepository.findById(request.getChiTietId())
                    .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + request.getChiTietId()));
            entity.setChiTietSanPham(chosen);
            assignedSerials = List.of(chosen);
        } else {
            // Tự động gán seri còn trong kho theo thứ tự nhập trước (FIFO)
            int soLuong = request.getSoLuong() != null ? request.getSoLuong() : 1;
            List<ChiTietSanPham> available = chiTietSanPhamRepository
                    .findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(request.getBienTheId(), "trong_kho");
            if (available.size() < soLuong)
                throw new IllegalArgumentException(
                        "Không đủ hàng trong kho: cần " + soLuong + ", còn " + available.size());
            assignedSerials = available.subList(0, soLuong);
            // Chỉ gắn 1 seri đại diện lên dòng đơn hàng (FK chi_tiet_id là 1-1) — bảng join
            // chi_tiet_don_hang_serial bên dưới mới là nguồn đầy đủ khi so_luong > 1.
            entity.setChiTietSanPham(assignedSerials.get(0));
        }

        ChiTietDonHang saved = chiTietDonHangRepository.save(entity);

        // Đơn online: chỉ giữ chỗ ("giu_hang") — admin xác nhận/đổi serial ở bước xác nhận
        // (xem DonHangService.xacNhanDonHang) mới chốt "da_ban". Đơn tại quầy (in_store): chốt
        // bán ngay như trước, không qua bước xác nhận (nhân viên đã cầm máy trên tay).
        boolean online = "online".equals(donHang.getKenhBan());
        String trangThaiMoi = online ? "giu_hang" : "da_ban";

        for (ChiTietSanPham serial : assignedSerials) {
            serial.setTrangThai(trangThaiMoi);
            chiTietSanPhamRepository.save(serial);
            // Ghi vào bảng join cho MỌI serial (kể cả đơn tại quầy) — đây là nguồn duy nhất
            // biết đủ mọi serial của 1 dòng khi so_luong > 1, FK đơn chỉ giữ 1 đại diện.
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

    public ChiTietDonHang update(Integer id, ChiTietDonHangRequest request) {
        ChiTietDonHang entity = getById(id);
        BeanUtils.copyProperties(request, entity, "id", "donHangId", "bienTheId", "chiTietId");

        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);

        return chiTietDonHangRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!chiTietDonHangRepository.existsById(id))
            throw new IllegalArgumentException("Chi tiết đơn hàng không tồn tại với id: " + id);
        chiTietDonHangRepository.deleteById(id);
    }

    // Toàn bộ serial đang giữ chỗ/đã gán cho từng dòng của 1 đơn — dùng cho modal "Chọn
    // serial" trước khi đóng gói (đơn online có thể có nhiều serial/dòng nên không đủ nếu
    // chỉ lấy serial đại diện từ ChiTietDonHangResponse).
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHangId(Integer donHangId) {
        return chiTietDonHangSerialRepository.findByDonHangId(donHangId);
    }
}
