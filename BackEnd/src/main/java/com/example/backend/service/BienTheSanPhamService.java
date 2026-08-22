package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.repository.*;
import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.response.BienTheSanPhamPublicResponse;
import com.example.backend.response.BienTheSanPhamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BienTheSanPhamService {

    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;
    @Autowired
    private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;
    @Autowired
    private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    public List<BienTheSanPhamResponse> hienThiBienTheSanPham() {
        return bienTheSanPhamRepository.hienThiBienTheSanPham();
    }

    public Page<BienTheSanPhamResponse> hienThiBienTheSanPham(Pageable pageable) {
        return bienTheSanPhamRepository.hienThiBienTheSanPham(pageable);
    }

    public List<BienTheSanPhamPublicResponse> hienThiBienTheSanPhamPublic() {
        return bienTheSanPhamRepository.hienThiBienTheSanPhamPublic();
    }

    public BienTheSanPhamPublicResponse getPublicById(Integer id) {
        return bienTheSanPhamRepository.findPublicById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPhamResponse getResponseById(Integer id) {
        return bienTheSanPhamRepository.findResponseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    public BienTheSanPham getById(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Biến thể sản phẩm không tồn tại với id: " + id));
    }

    @Transactional
    public BienTheSanPham create(BienTheSanPhamRequest request) {
        String barcode = chuanHoa(request.getBarcode());
        kiemTraTrungBarcode(barcode, null);

        BienTheSanPham entity = new BienTheSanPham();
        // "bienTheId": loai them cho khop update() — client lo gui id len thi save() thanh
        // UPDATE ban ghi cu chu khong INSERT ban moi.
        // "ngayTao": cot ngay_tao cua bien_the_san_pham la NOT NULL. Neu request co truong
        // nay va dang null, copyProperties se ghi null de len va lam ca giao dich do —
        // dung ly do bien the thu 2 tro di khong luu duoc, trong khi bien the dau tien (di
        // qua SanPhamService, da va tu truoc) van vao binh thuong.
        BeanUtils.copyProperties(request, entity,
                "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId", "barcode", "ngayTao");
        entity.setBarcode(barcode);
        // ngayTao nam o BaseEntity (lop cha), khong khai lai o day. Dat theo gia tri hien
        // co thay vi doc request.getNgayTao() — BienTheSanPhamRequest co the khong co truong
        // do, doc thang vao se khong bien dich duoc.
        if (entity.getNgayTao() == null) entity.setNgayTao(LocalDateTime.now());

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);

        // Dong ton_kho tuong ung do trigger TRG_TuDongTaoTonKho cua CSDL tu tao (xem cuoi
        // file QLBanMayTinh.sql). Truoc day cho nay con insert TonKho bang tay -> hai dong
        // cho cung mot bien_the_id -> dung rang buoc UNIQUE(bien_the_id) cua bang ton_kho
        // -> rollback ca bien the. Da bo han; dat o tang CSDL de moi duong ghi deu co ton kho.

        return saved;
    }

    @Transactional
    public BienTheSanPham update(Integer id, BienTheSanPhamRequest request) {
        BienTheSanPham entity = getById(id);

        Integer sanPhamId = entity.getSanPham().getSanPhamId();
        String oldMaSku = entity.getMaSku();
        String oldBarcode = entity.getBarcode();
        BigDecimal oldGiaNhap = entity.getGiaNhap();
        BigDecimal oldGiaBan = entity.getGiaBan();
        Integer oldBaoHanhThang = entity.getBaoHanhThang();
        String oldHinhAnhBienThe = entity.getHinhAnhBienThe();
        String oldTrangThai = entity.getTrangThai();
        String oldMauSac = entity.getMauSac();
        Integer oldCpuId = entity.getCpu() != null ? entity.getCpu().getCpuId() : null;
        Integer oldRamId = entity.getRam() != null ? entity.getRam().getRamId() : null;
        Integer oldOCungId = entity.getOCung() != null ? entity.getOCung().getOCungId() : null;
        Integer oldGpuId = entity.getGpu() != null ? entity.getGpu().getGpuId() : null;
        String oldKichThuocManHinh = entity.getKichThuocManHinh();
        String oldHeDieuHanh = entity.getHeDieuHanh();
        String oldPin = entity.getPin();
        BigDecimal oldTrongLuongKg = entity.getTrongLuongKg();

        String barcode = chuanHoa(request.getBarcode());
        kiemTraTrungBarcode(barcode, id);

        BeanUtils.copyProperties(request, entity, "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId", "barcode", "ngayTao");
        entity.setBarcode(barcode);

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);

        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "maSku", oldMaSku, saved.getMaSku(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "barcode", oldBarcode, saved.getBarcode(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaNhap", oldGiaNhap, saved.getGiaNhap(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaBan", oldGiaBan, saved.getGiaBan(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "baoHanhThang", oldBaoHanhThang, saved.getBaoHanhThang(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "hinhAnhBienThe", oldHinhAnhBienThe, saved.getHinhAnhBienThe(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trangThai", oldTrangThai, saved.getTrangThai(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "mauSac", oldMauSac, saved.getMauSac(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "cpuId", oldCpuId, request.getCpuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "ramId", oldRamId, request.getRamId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "oCungId", oldOCungId, request.getOCungId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "gpuId", oldGpuId, request.getGpuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "kichThuocManHinh", oldKichThuocManHinh, saved.getKichThuocManHinh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "heDieuHanh", oldHeDieuHanh, saved.getHeDieuHanh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "pin", oldPin, saved.getPin(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trongLuongKg", oldTrongLuongKg, saved.getTrongLuongKg(), nguoiSua);

        return saved;
    }

    /** Chuỗi rỗng phải về null: hai biến thể cùng để barcode "" sẽ đụng unique index. */
    private String chuanHoa(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Báo lỗi rõ ràng trước khi để SQL Server bắn unique violation khó đọc. */
    private void kiemTraTrungBarcode(String barcode, Integer boQuaId) {
        if (barcode == null) return;
        boolean trung = boQuaId == null
                ? bienTheSanPhamRepository.existsByBarcode(barcode)
                : bienTheSanPhamRepository.existsByBarcodeAndBienTheIdNot(barcode, boQuaId);
        if (trung) throw new IllegalArgumentException("Barcode '" + barcode + "' đã được dùng");
    }

}