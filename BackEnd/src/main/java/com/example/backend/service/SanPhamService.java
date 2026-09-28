package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.SanPhamHinhAnh;
import com.example.backend.repository.*;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.response.SanPhamCreatedResponse;
import com.example.backend.response.SanPhamChiTietResponse;
import com.example.backend.response.SanPhamResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SanPhamService {

    private static final Logger log = LoggerFactory.getLogger(SanPhamService.class);

    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;
    @Autowired
    private DanhMucRepository danhMucRepository;
    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;
    @Autowired
    private DmRamRepository dmRamRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;
    @Autowired
    private SanPhamHinhAnhRepository sanPhamHinhAnhRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired(required = false)
    private PhanLoaiService phanLoaiService;

    public Page<SanPhamResponse> hienThiSanPham(String keyword, Integer danhMucId,
                                                Integer thuongHieuId, String trangThai,
                                                Pageable pageable) {
        return sanPhamRepository.hienThiSanPham(chuanHoa(keyword), danhMucId, thuongHieuId,
                chuanHoa(trangThai), pageable);
    }

    public SanPham getSanPhamById(Integer sanPhamId) {
        return sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));
    }

    @Transactional(readOnly = true)
    public SanPhamChiTietResponse getSanPhamChiTiet(Integer sanPhamId) {
        SanPham sp = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));

        List<BienTheSanPham> bienTheList = bienTheSanPhamRepository.findChiTietTheoSanPham(sanPhamId);
        List<String> hinhAnhList = sanPhamHinhAnhRepository.layDuongDanTheoSanPham(sanPhamId);

        // Tính khoảng giá
        BigDecimal giaBanMin = null, giaBanMax = null;
        for (BienTheSanPham bt : bienTheList) {
            if (bt.getGiaBan() != null) {
                if (giaBanMin == null || bt.getGiaBan().compareTo(giaBanMin) < 0) giaBanMin = bt.getGiaBan();
                if (giaBanMax == null || bt.getGiaBan().compareTo(giaBanMax) > 0) giaBanMax = bt.getGiaBan();
            }
        }

        // Lấy thông số chung từ biến thể đầu tiên
        BienTheSanPham btDau = bienTheList.isEmpty() ? null : bienTheList.get(0);

        // Map biến thể
        List<SanPhamChiTietResponse.BienTheChiTietResponse> variants = bienTheList.stream()
                .map(bt -> {
                    SanPhamChiTietResponse.BienTheChiTietResponse item = new SanPhamChiTietResponse.BienTheChiTietResponse();
                    item.setBienTheId(bt.getBienTheId());
                    item.setMaSku(bt.getMaSku());
                    item.setBarcode(bt.getBarcode());
                    item.setMauSac(bt.getMauSac());
                    item.setCpu(bt.getCpu() != null ? bt.getCpu().getTenCpu() : null);
                    item.setCpuId(bt.getCpu() != null ? bt.getCpu().getCpuId() : null);
                    item.setRam(bt.getRam() != null ? bt.getRam().getDungLuong() : null);
                    item.setRamId(bt.getRam() != null ? bt.getRam().getRamId() : null);
                    item.setOCung(bt.getOCung() != null ? bt.getOCung().getLoaiOcung() : null);
                    item.setOCungId(bt.getOCung() != null ? bt.getOCung().getOCungId() : null);
                    item.setGpu(bt.getGpu() != null ? bt.getGpu().getTenGpu() : null);
                    item.setGpuId(bt.getGpu() != null ? bt.getGpu().getGpuId() : null);
                    item.setGiaNhap(bt.getGiaNhap());
                    item.setGiaBan(bt.getGiaBan());
                    item.setTrangThai(bt.getTrangThai());
                    item.setHinhAnhBienThe(bt.getHinhAnhBienThe());
                    item.setBaoHanhThang(bt.getBaoHanhThang());
                    item.setKichThuocManHinh(bt.getKichThuocManHinh());
                    item.setHeDieuHanh(bt.getHeDieuHanh());
                    item.setPin(bt.getPin());
                    item.setTrongLuongKg(bt.getTrongLuongKg());
                    item.setMoTa(bt.getMoTa());
                    item.setSoLuongTon(null);
                    return item;
                })
                .toList();

        // Tạo response
        SanPhamChiTietResponse resp = new SanPhamChiTietResponse();
        resp.setSanPhamId(sp.getSanPhamId());
        resp.setMaSanPham(sp.getMaSanPham());
        resp.setTenSanPham(sp.getTenSanPham());
        resp.setThuongHieuId(sp.getThuongHieu() != null ? sp.getThuongHieu().getThuongHieuId() : null);
        resp.setTenThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : null);
        resp.setDanhMucId(sp.getDanhMuc() != null ? sp.getDanhMuc().getId() : null);
        resp.setTenDanhMuc(sp.getDanhMuc() != null ? sp.getDanhMuc().getTenDanhMuc() : null);
        resp.setNhaCungCapId(sp.getNhaCungCap() != null ? sp.getNhaCungCap().getNhaCungCapId() : null);
        resp.setTenNhaCungCap(sp.getNhaCungCap() != null ? sp.getNhaCungCap().getTenNhaCungCap() : null);
        resp.setLoaiSanPham(sp.getLoaiSanPham());
        resp.setMoTa(sp.getMoTa());
        resp.setHinhAnhChinh(sp.getHinhAnhChinh());
        resp.setHinhAnhList(hinhAnhList);
        resp.setTrangThai(btDau != null ? btDau.getTrangThai() : null);
        resp.setNgayTao(sp.getNgayTao());
        resp.setNgayCapNhat(sp.getNgayCapNhat());
        resp.setKhachDat(null); // Có thể tính sau nếu cần

        // Thông số chung
        if (btDau != null) {
            resp.setKichThuocManHinh(btDau.getKichThuocManHinh());
            resp.setHeDieuHanh(btDau.getHeDieuHanh());
            resp.setPin(btDau.getPin());
            resp.setTrongLuongKg(btDau.getTrongLuongKg());
            resp.setBaoHanhThang(btDau.getBaoHanhThang());
            resp.setPhanLoaiTags(btDau.getPhanLoaiTags());
            resp.setPhanLoaiTen(btDau.getPhanLoaiTen());
        }

        if (phanLoaiService != null) {
            resp.setPhanLoaiIds(phanLoaiService.cuaSanPham(sanPhamId));
            resp.setPhanLoai(phanLoaiService.maCuaSanPham(sanPhamId));
        }

        // Khoảng giá
        if (giaBanMin != null && giaBanMax != null) {
            if (giaBanMin.equals(giaBanMax)) {
                resp.setKhoangGia(giaBanMin.toString());
            } else {
                resp.setKhoangGia(giaBanMin + " – " + giaBanMax);
            }
        }

        resp.setVariants(variants);

        return resp;
    }

    // Tạo sản phẩm và biến thể mặc định
    @Transactional
    public SanPhamCreatedResponse createSanPham(SanPhamRequest request) {
        String maSanPham = chuanHoa(request.getMaSanPham());
        kiemTraTrungMaSanPham(maSanPham, null);

        // Kiểm tra trùng mã vạch biến thể
        String barcodeBienThe = chuanHoa(request.getBarcodeBienThe());
        kiemTraTrungBarcodeBienThe(barcodeBienThe, null);

        SanPham sanPham = new SanPham();
        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao", "maSanPham");
        // Auto-generate maSanPham nếu request không truyền
        if (maSanPham == null || maSanPham.isBlank()) {
            sanPham.setMaSanPham(null); // tạm null, sẽ fill sau khi có id
        } else {
            sanPham.setMaSanPham(maSanPham);
        }
        sanPham.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());
        // Lấy ảnh đầu tiên làm ảnh đại diện
        if (request.getHinhAnhList() != null && !request.getHinhAnhList().isEmpty())
            sanPham.setHinhAnhChinh(request.getHinhAnhList().get(0));

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        if (request.getNhaCungCapId() != null)
            sanPham.setNhaCungCap(nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()));

        SanPham saved = sanPhamRepository.save(sanPham);

        if (entityManager != null) entityManager.refresh(saved);

        // Fill maSanPham nếu chưa có (tạo mới mà không truyền mã)
        if (saved.getMaSanPham() == null || saved.getMaSanPham().isBlank()) {
            saved.setMaSanPham(String.format("SP%04d", saved.getSanPhamId()));
            saved = sanPhamRepository.save(saved);
            if (entityManager != null) entityManager.refresh(saved);
        }

        if (request.getHinhAnhList() != null) luuDanhSachHinhAnh(saved.getSanPhamId(), request.getHinhAnhList());

        BienTheSanPham bt = new BienTheSanPham();
        // Sao chép thuộc tính cho biến thể
        BeanUtils.copyProperties(request, bt, "bienTheId", "ngayTao");
        bt.setNgayTao(request.getNgayTao() != null ? request.getNgayTao() : LocalDateTime.now());
        bt.setSanPham(saved);
        bt.setTrangThai(trangThaiBienThe(request.getTrangThai()));
        bt.setBarcode(barcodeBienThe);
        String maSku = chuanHoa(request.getMaSku());
        if (maSku == null) {
            String prefix = (maSanPham != null ? maSanPham : "SP" + saved.getSanPhamId()).toUpperCase();
            maSku = prefix + "-001";
        }
        bt.setMaSku(maSku);
        if (bt.getGiaBan() == null) bt.setGiaBan(BigDecimal.ZERO);
        if (bt.getGiaNhap() == null) bt.setGiaNhap(BigDecimal.ZERO);
        if (bt.getBaoHanhThang() == null) bt.setBaoHanhThang(12);
        if (request.getMoTaBienThe() != null) bt.setMoTa(request.getMoTaBienThe());
        ganLinhKien(bt, request);

        BienTheSanPham savedBt = bienTheSanPhamRepository.save(bt);
        if (entityManager != null) entityManager.flush(); // ponytail: ensure bien_the ID is generated before returning response

        return new SanPhamCreatedResponse(saved.getSanPhamId(), saved.getMaSanPham(),
                savedBt.getBarcode(), savedBt.getBienTheId(), savedBt.getMaSku());
    }

    @Transactional
    public void updateSanPham(Integer sanPhamId, SanPhamRequest request) {
        log.info("[DEBUG updateSanPham] sanPhamId={}, nhaCungCapId={}, thuongHieuId={}, danhMucId={}",
                sanPhamId, request.getNhaCungCapId(), request.getThuongHieuId(), request.getDanhMucId());
        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));
        log.info("[DEBUG updateSanPham] DB nhaCungCapId={}, tenNhaCungCap={}",
                sanPham.getNhaCungCap() != null ? sanPham.getNhaCungCap().getNhaCungCapId() : "null",
                sanPham.getNhaCungCap() != null ? sanPham.getNhaCungCap().getTenNhaCungCap() : "null");

        String oldTenSanPham = sanPham.getTenSanPham();
        Integer oldThuongHieuId = sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getThuongHieuId() : null;
        Integer oldDanhMucId = sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getId() : null;
        Integer oldNhaCungCapId = sanPham.getNhaCungCap() != null ? sanPham.getNhaCungCap().getNhaCungCapId() : null;
        String oldLoaiSanPham = sanPham.getLoaiSanPham();
        String oldMoTa = sanPham.getMoTa();
        String oldHinhAnhChinh = sanPham.getHinhAnhChinh();
        String oldTrangThai = sanPham.getTrangThai();

        String maSanPham = chuanHoa(request.getMaSanPham());
        kiemTraTrungMaSanPham(maSanPham, sanPhamId);

        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao", "maSanPham");
        // Chỉ cập nhật maSanPham khi request truyền giá trị hợp lệ, không được set null
        if (maSanPham != null && !maSanPham.isBlank()) {
            sanPham.setMaSanPham(maSanPham);
        }
        if (request.getNgayTao() != null) sanPham.setNgayTao(request.getNgayTao());
        if (request.getHinhAnhList() != null && !request.getHinhAnhList().isEmpty())
            sanPham.setHinhAnhChinh(request.getHinhAnhList().get(0));

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        sanPham.setNhaCungCap(request.getNhaCungCapId() != null
                ? nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()) : null);

        log.info("[DEBUG updateSanPham] BEFORE save: nhaCungCap will be set to nhaCungCapId={}, entity.tenSanPham={}",
                request.getNhaCungCapId(), sanPham.getTenSanPham());
        sanPhamRepository.save(sanPham);
        if (entityManager != null) entityManager.flush(); // ponytail: ensure JPA dirty-check is flushed to DB immediately
        log.info("[DEBUG updateSanPham] AFTER flush: nhaCungCapId={}", sanPham.getNhaCungCap() != null ? sanPham.getNhaCungCap().getNhaCungCapId() : "null");
        if (request.getHinhAnhList() != null) luuDanhSachHinhAnh(sanPhamId, request.getHinhAnhList());

        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "tenSanPham", oldTenSanPham, sanPham.getTenSanPham(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "thuongHieuId", oldThuongHieuId, request.getThuongHieuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "danhMucId", oldDanhMucId, request.getDanhMucId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "nhaCungCapId", oldNhaCungCapId, request.getNhaCungCapId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "loaiSanPham", oldLoaiSanPham, sanPham.getLoaiSanPham(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "moTa", oldMoTa, sanPham.getMoTa(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "hinhAnhChinh", oldHinhAnhChinh, sanPham.getHinhAnhChinh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, null, "san_pham", "trangThai", oldTrangThai, sanPham.getTrangThai(), nguoiSua);

        if (request.getBienTheId() != null) {
            BienTheSanPham bt = bienTheSanPhamRepository.findById(request.getBienTheId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Biến thể không tồn tại với id: " + request.getBienTheId()));

            // Lưu lại tất cả giá trị cũ TRƯỚC KHI cập nhật biến thể
            BigDecimal oldGiaNhapBienThe = bt.getGiaNhap();
            BigDecimal oldGiaBanBienThe = bt.getGiaBan();
            String oldBarcodeBienThe = bt.getBarcode();
            String oldMaSku = bt.getMaSku();
            String oldMauSac = bt.getMauSac();
            Integer oldBaoHanhThang = bt.getBaoHanhThang();
            String oldKichThuocManHinh = bt.getKichThuocManHinh();
            String oldHeDieuHanh = bt.getHeDieuHanh();
            String oldPin = bt.getPin();
            BigDecimal oldTrongLuongKg = bt.getTrongLuongKg();
            String oldHinhAnhBienThe = bt.getHinhAnhBienThe();
            String oldTrangThaiBienThe = bt.getTrangThai();
            Integer oldCpuId = bt.getCpu() != null ? bt.getCpu().getCpuId() : null;
            Integer oldRamId = bt.getRam() != null ? bt.getRam().getRamId() : null;
            Integer oldOCungId = bt.getOCung() != null ? bt.getOCung().getOCungId() : null;
            Integer oldGpuId = bt.getGpu() != null ? bt.getGpu().getGpuId() : null;
            String oldMoTaBienThe = bt.getMoTa();

            String barcodeBienThe = chuanHoa(request.getBarcodeBienThe());
            kiemTraTrungBarcodeBienThe(barcodeBienThe, bt.getBienTheId());

            if (request.getMaSku() != null && !request.getMaSku().isBlank()) {
                String maSku = request.getMaSku().trim();
                kiemTraTrungMaSku(maSku, bt.getBienTheId());
                bt.setMaSku(maSku);
            }
            if (request.getGiaBan() != null) bt.setGiaBan(request.getGiaBan());
            if (request.getGiaNhap() != null) bt.setGiaNhap(request.getGiaNhap());
            if (request.getBaoHanhThang() != null) bt.setBaoHanhThang(request.getBaoHanhThang());
            if (request.getMauSac() != null) bt.setMauSac(request.getMauSac());
            if (request.getKichThuocManHinh() != null) bt.setKichThuocManHinh(request.getKichThuocManHinh());
            if (request.getHeDieuHanh() != null) bt.setHeDieuHanh(request.getHeDieuHanh());
            if (request.getPin() != null) bt.setPin(request.getPin());
            if (request.getTrongLuongKg() != null) bt.setTrongLuongKg(request.getTrongLuongKg());
            if (request.getHinhAnhBienThe() != null) bt.setHinhAnhBienThe(request.getHinhAnhBienThe());
            if (request.getPhanLoaiTags() != null) bt.setPhanLoaiTags(request.getPhanLoaiTags());
            if (request.getPhanLoaiTen() != null) bt.setPhanLoaiTen(request.getPhanLoaiTen());
            if (request.getTrangThai() != null) bt.setTrangThai(trangThaiBienThe(request.getTrangThai()));
            if (request.getMoTaBienThe() != null) bt.setMoTa(request.getMoTaBienThe());
            bt.setBarcode(barcodeBienThe);
            bt.setSanPham(sanPham);
            ganLinhKien(bt, request);

            BienTheSanPham savedBt = bienTheSanPhamRepository.save(bt);
            if (entityManager != null) entityManager.flush(); // ponytail: ensure bien_the update is flushed to DB immediately

            // Ghi log tất cả trường có thể thay đổi
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "maSku", oldMaSku, savedBt.getMaSku(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "giaNhap", oldGiaNhapBienThe, savedBt.getGiaNhap(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "giaBan", oldGiaBanBienThe, savedBt.getGiaBan(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "barcode", oldBarcodeBienThe, savedBt.getBarcode(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "baoHanhThang", oldBaoHanhThang, savedBt.getBaoHanhThang(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "mauSac", oldMauSac, savedBt.getMauSac(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "cpuId", oldCpuId, savedBt.getCpu() != null ? savedBt.getCpu().getCpuId() : null, nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "ramId", oldRamId, savedBt.getRam() != null ? savedBt.getRam().getRamId() : null, nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "oCungId", oldOCungId, savedBt.getOCung() != null ? savedBt.getOCung().getOCungId() : null, nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "gpuId", oldGpuId, savedBt.getGpu() != null ? savedBt.getGpu().getGpuId() : null, nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "kichThuocManHinh", oldKichThuocManHinh, savedBt.getKichThuocManHinh(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "heDieuHanh", oldHeDieuHanh, savedBt.getHeDieuHanh(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "pin", oldPin, savedBt.getPin(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "trongLuongKg", oldTrongLuongKg, savedBt.getTrongLuongKg(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "hinhAnhBienThe", oldHinhAnhBienThe, savedBt.getHinhAnhBienThe(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "trangThai", oldTrangThaiBienThe, savedBt.getTrangThai(), nguoiSua);
            lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, savedBt.getBienTheId(), "bien_the", "moTa", oldMoTaBienThe, savedBt.getMoTa(), nguoiSua);
        }
    }

    public boolean hasTransactionHistory(Integer sanPhamId) {
        return bienTheSanPhamRepository.hasTransactionHistoryBySanPhamId(sanPhamId);
    }

    /** Gallery ảnh ngoài ảnh đại diện — trang chi tiết khách hàng hiển thị dạng nhiều ảnh. */
    public List<String> layDanhSachHinhAnh(Integer sanPhamId) {
        return sanPhamHinhAnhRepository.layDuongDanTheoSanPham(sanPhamId);
    }

    @Transactional
    public void deleteSanPham(Integer sanPhamId) {
        if (!sanPhamRepository.existsById(sanPhamId))
            throw new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId);
        for (BienTheSanPham bt : bienTheSanPhamRepository.findBySanPham_SanPhamId(sanPhamId)) {
            bienTheSanPhamRepository.deleteById(bt.getBienTheId());
        }
        sanPhamRepository.deleteById(sanPhamId);
    }

    /* ───────────────────────── Helper ───────────────────────── */

    private void ganLinhKien(BienTheSanPham bt, SanPhamRequest request) {
        if (request.getCpuId() != null) {
            bt.setCpu(dmCpuRepository.getReferenceById(request.getCpuId()));
        }
        if (request.getRamId() != null) {
            bt.setRam(dmRamRepository.getReferenceById(request.getRamId()));
        }
        if (request.getOCungId() != null) {
            bt.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        }
        if (request.getGpuId() != null) {
            bt.setGpu(dmGpuRepository.getReferenceById(request.getGpuId()));
        }
    }

    // Cập nhật danh sách ảnh gallery của sản phẩm
    private void luuDanhSachHinhAnh(Integer sanPhamId, List<String> duongDanList) {
        sanPhamHinhAnhRepository.deleteBySanPhamId(sanPhamId);
        SanPham ref = sanPhamRepository.getReferenceById(sanPhamId);
        List<SanPhamHinhAnh> rows = new ArrayList<>();
        int thuTu = 0;
        for (String duongDan : duongDanList) {
            if (duongDan == null || duongDan.isBlank()) continue;
            SanPhamHinhAnh h = new SanPhamHinhAnh();
            h.setSanPham(ref);
            h.setDuongDan(duongDan.trim());
            h.setThuTu(thuTu++);
            rows.add(h);
        }
        sanPhamHinhAnhRepository.saveAll(rows);
    }

    // Chuẩn hóa chuỗi rỗng thành null
    private String chuanHoa(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // Quy đổi trạng thái cho biến thể sản phẩm
    private String trangThaiBienThe(String trangThai) {
        return "active".equalsIgnoreCase(trangThai) ? "active" : "inactive";
    }

    // Kiểm tra trùng mã sản phẩm
    private void kiemTraTrungMaSanPham(String maSanPham, Integer boQuaId) {
        if (maSanPham != null) {
            boolean trung = boQuaId == null
                    ? sanPhamRepository.existsByMaSanPham(maSanPham)
                    : sanPhamRepository.existsByMaSanPhamAndSanPhamIdNot(maSanPham, boQuaId);
            if (trung) throw new IllegalArgumentException("Mã sản phẩm '" + maSanPham + "' đã được dùng");
        }
    }

    /**
     * Lấy tất cả sản phẩm cho dropdown chọn (khuyến mãi, etc.)
     */
    public List<SanPhamResponse> getDanhSachChon() {
        return sanPhamRepository.hienThiSanPham(null, null, null, null, Pageable.unpaged()).getContent();
    }

    /** Barcode cấp biến thể — bảng bien_the_san_pham riêng, kiểm tra trùng barcode biến thể. */
    private void kiemTraTrungBarcodeBienThe(String barcode, Integer boQuaBienTheId) {
        if (barcode == null) return;
        boolean trung = boQuaBienTheId == null
                ? bienTheSanPhamRepository.existsByBarcode(barcode)
                : bienTheSanPhamRepository.existsByBarcodeAndBienTheIdNot(barcode, boQuaBienTheId);
        if (trung) throw new IllegalArgumentException("Barcode '" + barcode + "' đã được dùng");
    }

    /** Kiểm tra trùng SKU rõ ràng */
    private void kiemTraTrungMaSku(String maSku, Integer boQuaBienTheId) {
        if (maSku == null || maSku.isBlank()) return;
        boolean trung = boQuaBienTheId == null
                ? bienTheSanPhamRepository.existsByMaSku(maSku.trim())
                : bienTheSanPhamRepository.existsByMaSkuAndBienTheIdNot(maSku.trim(), boQuaBienTheId);
        if (trung) throw new IllegalArgumentException("Mã SKU '" + maSku.trim() + "' đã được dùng");
    }
}