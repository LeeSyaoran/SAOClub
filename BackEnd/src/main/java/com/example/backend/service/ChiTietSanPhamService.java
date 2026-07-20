package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.ChiTietSanPhamRepository;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.WarrantyStatusResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChiTietSanPhamService {

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;

    public List<ChiTietSanPhamResponse> hienThiChiTietSanPham() {
        return chiTietSanPhamRepository.hienThiChiTietSanPham();
    }

    public List<ChiTietSanPhamResponse> getByBienTheId(Integer bienTheId) {
        return chiTietSanPhamRepository.findByBienTheId(bienTheId);
    }

    public ChiTietSanPham getById(Integer id) {
        return chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết sản phẩm không tồn tại với id: " + id));
    }

    public ChiTietSanPham create(ChiTietSanPhamRequest request) {
        ChiTietSanPham entity = new ChiTietSanPham();
        // BeanUtils copies: soSerial, trangThai, ngayNhapKho, ghiChu
        BeanUtils.copyProperties(request, entity, "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }

    public ChiTietSanPham update(Integer id, ChiTietSanPhamRequest request) {
        ChiTietSanPham entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietId", "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }

    // Chỉ cho xóa serial đang "trong_kho" (thêm nhầm, chưa bán/chưa dùng) — serial đã bán
    // hoặc đã gắn vào đơn hàng/bảo hành mà xóa sẽ làm sai lịch sử giao dịch đã ghi nhận.
    public void delete(Integer id) {
        ChiTietSanPham entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa bán/chưa sử dụng)");
        }
        chiTietSanPhamRepository.deleteById(id);
    }

    // Danh sách serial đã bán CÒN trong hạn bảo hành — hạn tính từ ngày giao thực tế
    // (không phải ngày đặt/ngày bán) cộng số tháng bảo hành của biến thể. Serial đã hết
    // hạn tự động không còn xuất hiện ở đây nữa (không cần xóa dữ liệu, chỉ lọc theo ngày
    // mỗi lần gọi) — sắp hết hạn nhất hiển thị trước để dễ theo dõi.
    public List<WarrantyStatusResponse> getStillUnderWarranty() {
        LocalDateTime now = LocalDateTime.now();
        List<WarrantyStatusResponse> list = chiTietSanPhamRepository.timSerialDaBanCoGiaoHang();
        list.forEach(w -> w.setNgayHetBaoHanh(w.getNgayGiaoThucTe().plusMonths(w.getBaoHanhThang() == null ? 0 : w.getBaoHanhThang())));
        list.removeIf(w -> !w.getNgayHetBaoHanh().isAfter(now));
        list.sort(Comparator.comparing(WarrantyStatusResponse::getNgayHetBaoHanh));
        return list;
    }
}
