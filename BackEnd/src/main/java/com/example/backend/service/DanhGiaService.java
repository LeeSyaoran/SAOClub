package com.example.backend.service;

import com.example.backend.entity.DanhGia;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DanhGiaRepository;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.DanhGiaRequest;
import com.example.backend.response.DanhGiaAdminResponse;
import com.example.backend.response.DanhGiaResponse;
import com.example.backend.response.DanhGiaSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Danh gia san pham — chi khach hang co don hang DA GIAO chua san pham nay moi duoc danh
// gia ("verified purchase"), moi khach chi danh gia 1 lan / 1 san pham (UQ_dg_kh_sp).
@Service
public class DanhGiaService {

    @Autowired
    private DanhGiaRepository danhGiaRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    private KhachHang currentKhachHang() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan tk = taiKhoanRepository.findByUsername(username).orElse(null);
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Chỉ tài khoản khách hàng mới đánh giá được sản phẩm");
        return tk.getKhachHang();
    }

    public List<DanhGiaResponse> hienThiTheoSanPham(Integer sanPhamId) {
        return danhGiaRepository.hienThiTheoSanPham(sanPhamId);
    }

    public List<DanhGiaSummaryResponse> tongHopTatCa() {
        return danhGiaRepository.tongHopTatCa();
    }

    // Danh sach cho trang quan tri (admin/nhan_vien) — @PreAuthorize da chan role o controller,
    // khong can kiem tra gi them o day.
    public List<DanhGiaAdminResponse> hienThiTatCaAdmin() {
        return danhGiaRepository.hienThiTatCa();
    }

    // Xoa boi admin/nhan_vien — khong kiem tra chu so huu (khac xoa() cua khach hang tu xoa),
    // dung khi can kiem duyet noi dung vi pham.
    @Transactional
    public void xoaBoiAdmin(Integer danhGiaId) {
        if (!danhGiaRepository.existsById(danhGiaId))
            throw new IllegalArgumentException("Đánh giá không tồn tại với id: " + danhGiaId);
        danhGiaRepository.deleteById(danhGiaId);
    }

    @Transactional
    public DanhGia themDanhGia(DanhGiaRequest request) {
        KhachHang kh = currentKhachHang();

        if (danhGiaRepository.findByKhachHang_KhachHangIdAndSanPham_SanPhamId(kh.getKhachHangId(), request.getSanPhamId()).isPresent())
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này rồi");

        List<Integer> donHangDaGiao = danhGiaRepository.timDonHangDaGiao(kh.getKhachHangId(), request.getSanPhamId());
        if (donHangDaGiao.isEmpty())
            throw new IllegalArgumentException("Bạn cần mua và nhận được sản phẩm này trước khi đánh giá");

        DanhGia dg = new DanhGia();
        dg.setKhachHang(kh);
        dg.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        dg.setDonHang(donHangRepository.getReferenceById(donHangDaGiao.get(0)));
        dg.setSoSao(request.getSoSao());
        dg.setNoiDung(request.getNoiDung());
        dg.setNgayDanhGia(LocalDateTime.now());
        return danhGiaRepository.save(dg);
    }

    @Transactional
    public void xoa(Integer danhGiaId) {
        KhachHang kh = currentKhachHang();
        DanhGia dg = danhGiaRepository.findById(danhGiaId)
                .orElseThrow(() -> new IllegalArgumentException("Đánh giá không tồn tại với id: " + danhGiaId));
        if (!dg.getKhachHang().getKhachHangId().equals(kh.getKhachHangId()))
            throw new AccessDeniedException("Không có quyền xóa đánh giá này");
        danhGiaRepository.delete(dg);
    }
}
