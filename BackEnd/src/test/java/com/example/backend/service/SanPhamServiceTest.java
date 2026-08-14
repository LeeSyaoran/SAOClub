package com.example.backend.service;

import com.example.backend.entity.DanhMuc;
import com.example.backend.entity.NhaCungCap;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.ThuongHieu;
import com.example.backend.repository.*;
import com.example.backend.request.SanPhamRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanPhamServiceTest {

    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private ThuongHieuRepository thuongHieuRepository;
    @Mock private DanhMucRepository danhMucRepository;
    @Mock private NhaCungCapRepository nhaCungCapRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private DmCpuRepository dmCpuRepository;
    @Mock private DmRamRepository dmRamRepository;
    @Mock private DmOcungRepository dmOcungRepository;
    @Mock private DmGpuRepository dmGpuRepository;
    @Mock private BienTheSanPhamService bienTheSanPhamService;
    @Mock private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    @InjectMocks
    private SanPhamService service;

    @Test
    void updateSanPham_ghiLogChoTatCaTruongTheoDoi_voiGiaTriCuMoiDung() {
        ThuongHieu thCu = new ThuongHieu(); thCu.setThuongHieuId(1);
        DanhMuc dmCu = new DanhMuc(); dmCu.setId(2);
        NhaCungCap nccCu = new NhaCungCap(); nccCu.setNhaCungCapId(3);

        SanPham sp = new SanPham();
        sp.setSanPhamId(10);
        sp.setTenSanPham("Ten cu");
        sp.setThuongHieu(thCu);
        sp.setDanhMuc(dmCu);
        sp.setNhaCungCap(nccCu);
        sp.setLoaiSanPham("LAPTOP");
        sp.setMoTa("Mo ta cu");
        sp.setHinhAnhChinh("cu.jpg");
        sp.setTrangThai("active");
        when(sanPhamRepository.findById(10)).thenReturn(Optional.of(sp));

        ThuongHieu thMoi = new ThuongHieu(); thMoi.setThuongHieuId(9);
        DanhMuc dmMoi = new DanhMuc(); dmMoi.setId(8);
        when(thuongHieuRepository.getReferenceById(9)).thenReturn(thMoi);
        when(danhMucRepository.getReferenceById(8)).thenReturn(dmMoi);
        when(nhaCungCapRepository.getReferenceById(7)).thenReturn(new NhaCungCap());
        when(sanPhamRepository.save(any(SanPham.class))).thenAnswer(inv -> inv.getArgument(0));

        NhanVien nv = new NhanVien(); nv.setNhanVienId(5);
        when(lichSuThayDoiSanPhamService.nguoiSuaHienTai()).thenReturn(nv);

        SanPhamRequest request = new SanPhamRequest();
        request.setTenSanPham("Ten moi");
        request.setThuongHieuId(9);
        request.setDanhMucId(8);
        request.setNhaCungCapId(7);
        request.setLoaiSanPham("LAPTOP");
        request.setMoTa("Mo ta moi");
        request.setHinhAnhChinh("cu.jpg");
        request.setTrangThai("inactive");
        request.setGiaBan(BigDecimal.valueOf(1000));
        request.setGiaNhap(BigDecimal.valueOf(800));
        request.setBaoHanhThang(12);

        service.updateSanPham(10, request);

        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "tenSanPham", "Ten cu", "Ten moi", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "thuongHieuId", 1, 9, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "danhMucId", 2, 8, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "nhaCungCapId", 3, 7, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "loaiSanPham", "LAPTOP", "LAPTOP", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "moTa", "Mo ta cu", "Mo ta moi", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "hinhAnhChinh", "cu.jpg", "cu.jpg", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "trangThai", "active", "inactive", nv);
    }
}
