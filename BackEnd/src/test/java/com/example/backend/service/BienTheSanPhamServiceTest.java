package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.DmCpu;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.repository.*;
import com.example.backend.request.BienTheSanPhamRequest;
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
class BienTheSanPhamServiceTest {

    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private DmCpuRepository dmCpuRepository;
    @Mock private DmRamRepository dmRamRepository;
    @Mock private DmOcungRepository dmOcungRepository;
    @Mock private DmGpuRepository dmGpuRepository;
    @Mock private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Mock private TonKhoRepository tonKhoRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    @InjectMocks
    private BienTheSanPhamService service;

    @Test
    void update_ghiLogChoTatCaTruongTheoDoi_voiGiaTriCuMoiDung() {
        SanPham sp = new SanPham();
        sp.setSanPhamId(10);
        DmCpu cpuCu = new DmCpu();
        cpuCu.setCpuId(1);

        BienTheSanPham bt = new BienTheSanPham();
        bt.setBienTheId(20);
        bt.setSanPham(sp);
        bt.setMaSku("SKU-CU");
        bt.setGiaNhap(BigDecimal.valueOf(1000));
        bt.setGiaBan(BigDecimal.valueOf(1500));
        bt.setBaoHanhThang(12);
        bt.setHinhAnhBienThe("cu.jpg");
        bt.setTrangThai("active");
        bt.setMauSac("Đen");
        bt.setCpu(cpuCu);
        bt.setKichThuocManHinh("15.6\"");
        bt.setHeDieuHanh("Win11");
        bt.setPin("50Wh");
        bt.setTrongLuongKg(BigDecimal.valueOf(1.7));
        when(bienTheSanPhamRepository.findById(20)).thenReturn(Optional.of(bt));
        when(sanPhamRepository.getReferenceById(10)).thenReturn(sp);
        DmCpu cpuMoi = new DmCpu();
        cpuMoi.setCpuId(9);
        when(dmCpuRepository.getReferenceById(9)).thenReturn(cpuMoi);
        when(bienTheSanPhamRepository.save(any(BienTheSanPham.class))).thenAnswer(inv -> inv.getArgument(0));

        NhanVien nv = new NhanVien();
        nv.setNhanVienId(4);
        when(lichSuThayDoiSanPhamService.nguoiSuaHienTai()).thenReturn(nv);

        BienTheSanPhamRequest request = new BienTheSanPhamRequest();
        request.setSanPhamId(10);
        request.setMaSku("SKU-MOI");
        request.setGiaNhap(BigDecimal.valueOf(1000));
        request.setGiaBan(BigDecimal.valueOf(1800));
        request.setBaoHanhThang(12);
        request.setHinhAnhBienThe("cu.jpg");
        request.setTrangThai("active");
        request.setMauSac("Đen");
        request.setCpuId(9);
        request.setKichThuocManHinh("15.6\"");
        request.setHeDieuHanh("Win11");
        request.setPin("50Wh");
        request.setTrongLuongKg(BigDecimal.valueOf(1.7));

        service.update(20, request);

        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "maSku", "SKU-CU", "SKU-MOI", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "giaNhap", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "giaBan", BigDecimal.valueOf(1500), BigDecimal.valueOf(1800), nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "cpuId", 1, 9, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "ramId", null, null, nv);
    }
}
