package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.LichSuTangDiem;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.LichSuTangDiemRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.KhachHangRegisterRequest;
import com.example.backend.request.KhachHangRequest;
import com.example.backend.request.TangDiemRequest;
import com.example.backend.response.KhachHangLoginResponse;
import com.example.backend.response.KhachHangLookupResponse;
import com.example.backend.response.KhachHangResponse;
import com.example.backend.response.LichSuTangDiemResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private ChucVuRepository chucVuRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LichSuTangDiemRepository lichSuTangDiemRepository;

    public List<KhachHangResponse> hienThiKhachHang() {
        return khachHangRepository.hienThiKhachHang();
    }

    public Page<KhachHangResponse> hienThiKhachHang(Pageable pageable) {
        return khachHangRepository.hienThiKhachHang(pageable);
    }

    // Tra cứu theo SĐT cho luồng checkout (khách vãng lai lẫn đã đăng nhập) — không kiểm
    // tra staff-or-self như getById() vì lúc này người gọi CHƯA CHẮC đã có tài khoản/đăng
    // nhập (khách vãng lai). Chỉ trả về đúng 1 khách khớp SĐT, không lộ toàn bộ danh sách
    // như getAll() (vốn chỉ dành cho nhân viên/admin).
    //
    // Trả DTO rút gọn (KHÔNG phải entity KhachHang) — endpoint này permitAll, ai cũng gọi
    // được kể cả dò số điện thoại ngẫu nhiên, nên tuyệt đối không lộ soDuVi/diemTichLuy.
    public KhachHangLookupResponse findBySoDienThoai(String soDienThoai) {
        return khachHangRepository.findBySoDienThoai(soDienThoai)
                .map(k -> new KhachHangLookupResponse(
                        k.getKhachHangId(), k.getHoTen(), k.getSoDienThoai(), k.getEmail(), k.getDiaChi()))
                .orElse(null);
    }

    public KhachHang getById(Integer id) {
        KhachHang entity = khachHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + id));
        if (!isStaffOrSelf(id))
            throw new AccessDeniedException("Không có quyền xem khách hàng này");
        return entity;
    }

    @Transactional
    public KhachHang create(KhachHangRequest request) {
        KhachHang entity = new KhachHang();
        BeanUtils.copyProperties(request, entity);
        entity.setSoDuVi(java.math.BigDecimal.ZERO);
        entity.setNgayTao(LocalDateTime.now());
        return khachHangRepository.save(entity);
    }

    @Transactional
    public KhachHang update(Integer id, KhachHangRequest request) {
        KhachHang entity = getById(id); // đã kiểm tra staff-or-self ở trên

        // Khách tự sửa hồ sơ của mình: không cho đổi điểm tích lũy / trạng thái tài khoản —
        // chỉ nhân viên/quản trị mới được sửa 2 trường này.
        if (isStaff()) {
            BeanUtils.copyProperties(request, entity, "khachHangId", "ngayTao");
        } else {
            BeanUtils.copyProperties(request, entity, "khachHangId", "ngayTao", "diemTichLuy", "trangThai");
        }
        return khachHangRepository.save(entity);
    }

    // Tặng điểm — chỉ admin gọi (@PreAuthorize ở controller). Khóa ghi khách hàng đúng
    // pattern PhieuGiamGiaCaNhanService.doiThuong() để tránh 2 request tặng điểm đồng thời
    // đọc trùng số dư rồi cùng cộng (mất 1 lần cộng).
    @Transactional
    public void tangDiem(Integer khachHangId, TangDiemRequest request) {
        KhachHang khachHang = khachHangRepository.findWithLockByKhachHangId(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + khachHangId));
        khachHang.setDiemTichLuy(khachHang.getDiemTichLuy() + request.getSoDiem());
        khachHangRepository.save(khachHang);

        LichSuTangDiem lichSu = new LichSuTangDiem();
        lichSu.setKhachHang(khachHang);
        lichSu.setNhanVien(currentNhanVien());
        lichSu.setSoDiem(request.getSoDiem());
        lichSu.setLyDo(request.getLyDo());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSuTangDiemRepository.save(lichSu);
    }

    public List<LichSuTangDiemResponse> layLichSuDiem(Integer khachHangId) {
        return lichSuTangDiemRepository.findResponsesByKhachHangId(khachHangId);
    }

    private NhanVien currentNhanVien() {
        TaiKhoan tk = currentAccount();
        if (tk == null || tk.getNhanVien() == null)
            throw new AccessDeniedException("Chỉ nhân viên mới thực hiện được thao tác này");
        return tk.getNhanVien();
    }

    // ── Kiểm tra quyền: nhân viên/admin/quản kho xem tất cả, khách chỉ xem/sửa chính mình ──
    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private boolean isStaff() {
        TaiKhoan tk = currentAccount();
        return tk != null && !"khach_hang".equals(tk.getChucVu().getMaChucVu());
    }

    private boolean isStaffOrSelf(Integer khachHangId) {
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;
        return tk.getKhachHang() != null && khachHangId.equals(tk.getKhachHang().getKhachHangId());
    }

    @Transactional
    public void delete(Integer id) {
        if (!khachHangRepository.existsById(id))
            throw new IllegalArgumentException("Khách hàng không tồn tại với id: " + id);
        khachHangRepository.deleteById(id);
    }

    @Transactional
    public KhachHangLoginResponse register(KhachHangRegisterRequest request) {
        if (taiKhoanRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username đã tồn tại");
        if (khachHangRepository.existsBySoDienThoai(request.getSoDienThoai()))
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");

        KhachHang entity = new KhachHang();
        entity.setHoTen(request.getHoTen());
        entity.setSoDienThoai(request.getSoDienThoai());
        entity.setEmail(request.getEmail());
        entity.setDiaChi(request.getDiaChi());
        entity.setDiemTichLuy(0);
        entity.setTrangThai("active");
        entity.setSoDuVi(java.math.BigDecimal.ZERO);
        entity.setLoaiKhach("ca_nhan");
        entity.setNgayTao(LocalDateTime.now());
        KhachHang savedKh = khachHangRepository.save(entity);

        ChucVu chucVu = chucVuRepository.findByMaChucVu("khach_hang")
                .orElseThrow(() -> new IllegalStateException("Thiếu dữ liệu chức vụ 'khach_hang'"));

        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(request.getUsername());
        tk.setMatKhauHash(passwordEncoder.encode(request.getPassword()));
        tk.setChucVu(chucVu);
        tk.setKhachHang(savedKh);
        tk.setTrangThai("active");
        tk.setNgayTao(LocalDateTime.now());
        taiKhoanRepository.save(tk);

        return new KhachHangLoginResponse(
                savedKh.getKhachHangId(), savedKh.getHoTen(), request.getUsername(),
                savedKh.getSoDienThoai(), savedKh.getEmail(),
                savedKh.getDiemTichLuy(), savedKh.getTrangThai());
    }
}
