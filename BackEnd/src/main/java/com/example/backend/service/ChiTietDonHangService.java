package com.example.backend.service;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.LichSuTonKho;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.LichSuTonKhoRepository;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.response.ChiTietDonHangResponse;
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

        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
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
            // Chỉ gắn 1 seri đại diện lên dòng đơn hàng (FK chi_tiet_id là 1-1),
            // các seri còn lại (nếu so_luong > 1) vẫn được đánh dấu đã bán bên dưới.
            entity.setChiTietSanPham(assignedSerials.get(0));
        }

        ChiTietDonHang saved = chiTietDonHangRepository.save(entity);

        for (ChiTietSanPham serial : assignedSerials) {
            serial.setTrangThai("da_ban");
            chiTietSanPhamRepository.save(serial);
        }

        LichSuTonKho lichSu = new LichSuTonKho();
        lichSu.setBienThe(entity.getBienThe());
        lichSu.setChiTietSanPham(assignedSerials.isEmpty() ? null : assignedSerials.get(0));
        lichSu.setLoaiBienDong("xuat_ban");
        lichSu.setSoLuongThayDoi(-assignedSerials.size());
        lichSu.setDonHang(entity.getDonHang());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSu.setGhiChu("Bán hàng — đơn #" + request.getDonHangId());
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
}
