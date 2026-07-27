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

    // ── Kiểm tra quyền: nhân viên/admin/quản kho xem tất cả, khách chỉ xem đơn của chính mình ──
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
        // Trước đây không kiểm tra chủ đơn — khách hàng đăng nhập bất kỳ có thể đoán donHangId
        // của người khác rồi thêm dòng vào đơn đó. Tái dùng đúng logic staff-or-owner đã có
        // sẵn cho getByDonHangId().
        if (!isStaffOrOwner(request.getDonHangId()))
            throw new AccessDeniedException("Không có quyền thêm sản phẩm vào đơn hàng này");

        ChiTietDonHang entity = new ChiTietDonHang();
        // BeanUtils copies: soLuong, ghiChu — donGia/giamGiaDong KHÔNG copy từ request nữa,
        // xem lý do bên dưới (giá đóng băng theo DB, không tin giá client gửi lên).
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "chiTietId", "donGia", "giamGiaDong");

        DonHang donHang = donHangRepository.getReferenceById(request.getDonHangId());
        entity.setDonHang(donHang);
        BienTheSanPham bienThe = bienTheSanPhamRepository.findById(request.getBienTheId())
                .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại với id: " + request.getBienTheId()));
        entity.setBienThe(bienThe);

        // Giá đóng băng: luôn lấy don_gia từ giá bán hiện tại trong DB, không tin donGia client
        // gửi lên — trước đây BeanUtils copy thẳng donGia từ request, khách tự sửa payload
        // (vd DevTools) có thể mua giá bất kỳ. Chưa nơi nào trong frontend gửi giamGiaDong khác
        // 0 lúc tạo dòng mới (chiết khấu chỉ chỉnh được sau qua update(), vốn đã staff-only),
        // nên khoá luôn giamGiaDong = 0 ở bước tạo cho đồng bộ.
        entity.setDonGia(bienThe.getGiaBan());
        entity.setGiamGiaDong(java.math.BigDecimal.ZERO);

        // Gán serial cụ thể cho dòng đơn hàng này + trừ tồn kho.
        // ChiTietSanPham.trangThai chuyển khỏi "trong_kho" sẽ tự kích hoạt trigger
        // trg_CapNhatTonKhoThucTe trừ ton_kho.so_luong_ton_thuc_te tương ứng.
        List<ChiTietSanPham> assignedSerials;
        if (request.getChiTietId() != null) {
            // Đã chỉ định seri cụ thể (vd: nhân viên chọn tay tại quầy). Khóa PESSIMISTIC_WRITE +
            // kiểm tra lại trạng thái ngay trong transaction — trước đây dùng findById() thường,
            // 2 request cùng lúc (double-click, 2 nhân viên) có thể cùng gán trùng 1 serial đã bán.
            ChiTietSanPham chosen = chiTietSanPhamRepository.findByIdForUpdate(request.getChiTietId())
                    .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + request.getChiTietId()));
            if (!"trong_kho".equals(chosen.getTrangThai()))
                throw new IllegalArgumentException("Serial này đã được gán cho đơn khác, vui lòng chọn serial khác");
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

    // Nếu sửa dòng đổi sang serial khác (hoặc bỏ hẳn serial) — trả serial CŨ về "trong_kho"
    // trước, cùng lý do như delete() phía dưới. Không tự đặt trạng thái cho serial MỚI được
    // chọn (giu_hang/da_ban tùy kênh bán) — hiện chưa có nơi nào trong frontend gọi update()
    // này, chỉ vá đúng phần chắc chắn đúng (trả cái cũ), tránh đoán sai logic chưa từng chạy.
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

    // Trước đây xóa thẳng dòng đơn hàng mà không trả serial đã gán (giu_hang/da_ban) về lại
    // "trong_kho" — serial kẹt vĩnh viễn ở trạng thái đã bán dù dòng đơn đã bị xóa, làm tồn
    // kho thực tế sai lệch âm thầm. Mirror đúng logic DonHangService.releaseSerialsToStock()
    // nhưng chỉ cho 1 dòng thay vì cả đơn.
    //
    // chi_tiet_don_hang_serial CÓ ON DELETE CASCADE ở DB, nhưng Hibernate không biết gì về
    // cascade ở tầng DB — nó chỉ thấy 1 ChiTietDonHangSerial đã load (persistent, không đổi)
    // vẫn tham chiếu tới ChiTietDonHang sắp bị remove() trong cùng persistence context, nên
    // ném TransientPropertyValueException lúc flush/commit (500) trước khi câu DELETE thật
    // sự chạy tới DB. Phải xóa link tường minh qua Hibernate trước để nó tự biết, không dựa
    // vào cascade ở DB mà Hibernate không thấy được.
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

    // Toàn bộ serial đang giữ chỗ/đã gán cho từng dòng của 1 đơn — dùng cho modal "Chọn
    // serial" trước khi đóng gói (đơn online có thể có nhiều serial/dòng nên không đủ nếu
    // chỉ lấy serial đại diện từ ChiTietDonHangResponse).
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHangId(Integer donHangId) {
        return chiTietDonHangSerialRepository.findByDonHangId(donHangId);
    }
}
