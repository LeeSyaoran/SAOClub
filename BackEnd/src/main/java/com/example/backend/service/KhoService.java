package com.example.backend.service;

import com.example.backend.request.NhapKhoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiệp vụ kho: nhập hàng theo serial, chỉnh sửa biến thể, đổi trạng thái từng máy.
 *
 * ══════════ QUY ƯỚC QUAN TRỌNG NHẤT ══════════
 * ton_kho.so_luong_ton_thuc_te là số DẪN XUẤT, do trigger trg_CapNhatTonKhoThucTe trên
 * bảng chi_tiet_san_pham tự cộng/trừ mỗi khi một serial được thêm, đổi trạng thái hoặc xóa.
 * Vì vậy khi nhập hàng theo serial, service TUYỆT ĐỐI KHÔNG được tự cộng thêm vào ton_kho —
 * làm vậy là đếm hai lần (nhập 10 máy thành 20). Chỉ nhánh nhập KHÔNG serial mới cộng tay.
 *
 * Dùng JdbcTemplate thay vì JPA cho module này: các thao tác đều là cập nhật theo tập hợp
 * (batch insert serial, cộng tồn, ghi lịch sử) và phải phối hợp chính xác với trigger — viết
 * thẳng SQL rõ ràng và dễ kiểm chứng hơn là qua entity.
 */
@Service
public class KhoService {

    private static final Set<String> TRANG_THAI_SERIAL = Set.of(
            "trong_kho", "giu_hang", "da_ban", "loi_bao_hanh", "da_tra_hang");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbc;

    /* ═══════════════════ ĐỌC DỮ LIỆU ═══════════════════ */

    /** Toàn bộ biến thể kèm tồn kho — nguồn cho bảng chính của màn Kho hàng. */
    public List<Map<String, Object>> danhSachTonKho() {
        return jdbc.queryForList("""
            SELECT bt.bien_the_id AS bienTheId, bt.ma_sku AS maSku,
                   bt.gia_nhap AS giaNhap, bt.gia_ban AS giaBan, bt.bao_hanh_thang AS baoHanhThang,
                   bt.mau_sac AS mauSac, bt.trang_thai AS trangThai,
                   bt.kich_thuoc_man_hinh AS kichThuocManHinh, bt.he_dieu_hanh AS heDieuHanh,
                   bt.pin AS pin, bt.trong_luong_kg AS trongLuongKg,
                   bt.hinh_anh_bien_the AS hinhAnhBienThe,
                   bt.phan_loai_tags AS phanLoaiTags, bt.phan_loai_ten AS phanLoaiTen,
                   bt.cpu_id AS cpuId, bt.ram_id AS ramId, bt.o_cung_id AS oCungId, bt.gpu_id AS gpuId,
                   cpu.ten_cpu AS tenCpu, ram.dung_luong AS tenRam,
                   oc.loai_o_cung AS tenOCung, gpu.ten_gpu AS tenGpu,
                   sp.san_pham_id AS sanPhamId, sp.ma_san_pham AS maSanPham, sp.barcode AS barcode,
                   sp.ten_san_pham AS tenSanPham, sp.hinh_anh_chinh AS hinhAnhChinh,
                   th.thuong_hieu_id AS thuongHieuId, th.ten_thuong_hieu AS tenThuongHieu,
                   ncc.ten_nha_cung_cap AS tenNhaCungCap,
                   ISNULL(tk.so_luong_ton_thuc_te, 0) AS tonThucTe,
                   ISNULL(tk.so_luong_giu, 0) AS dangGiu,
                   ISNULL(tk.so_luong_ton_thuc_te, 0) - ISNULL(tk.so_luong_giu, 0) AS coTheBan,
                   ISNULL(tk.ton_kho_toi_thieu, 5) AS tonToiThieu,
                   (SELECT COUNT(*) FROM chi_tiet_san_pham c WHERE c.bien_the_id = bt.bien_the_id) AS tongSerial
            FROM bien_the_san_pham bt
            JOIN san_pham sp        ON sp.san_pham_id = bt.san_pham_id
            LEFT JOIN thuong_hieu th   ON th.thuong_hieu_id = sp.thuong_hieu_id
            LEFT JOIN nha_cung_cap ncc ON ncc.nha_cung_cap_id = sp.nha_cung_cap_id
            LEFT JOIN ton_kho tk    ON tk.bien_the_id = bt.bien_the_id
            LEFT JOIN dm_cpu cpu    ON cpu.cpu_id = bt.cpu_id
            LEFT JOIN dm_ram ram    ON ram.ram_id = bt.ram_id
            LEFT JOIN dm_o_cung oc  ON oc.o_cung_id = bt.o_cung_id
            LEFT JOIN dm_gpu gpu    ON gpu.gpu_id = bt.gpu_id
            WHERE bt.da_xoa = 0 AND sp.da_xoa = 0
            ORDER BY sp.ten_san_pham, bt.ma_sku
            """);
    }

    public List<Map<String, Object>> danhSachSerial(Integer bienTheId) {
        return jdbc.queryForList("""
            SELECT chi_tiet_id AS chiTietId, so_serial AS soSerial, trang_thai AS trangThai,
                   ngay_nhap_kho AS ngayNhapKho, ghi_chu AS ghiChu
            FROM chi_tiet_san_pham
            WHERE bien_the_id = ?
            ORDER BY chi_tiet_id DESC
            """, bienTheId);
    }

    public List<Map<String, Object>> lichSuKho(Integer bienTheId) {
        return jdbc.queryForList("""
            SELECT TOP 200 ls.lich_su_id AS id, ls.loai_bien_dong AS loai,
                   ls.so_luong_thay_doi AS soLuong, ls.ghi_chu AS ghiChu, ls.ngay_tao AS ngayTao,
                   nv.ho_ten AS nhanVien, pn.ma_phieu_nhap AS maPhieuNhap
            FROM lich_su_ton_kho ls
            LEFT JOIN nhan_vien nv     ON nv.nhan_vien_id = ls.nhan_vien_id
            LEFT JOIN phieu_nhap_kho pn ON pn.phieu_nhap_id = ls.phieu_nhap_id
            WHERE ls.bien_the_id = ?
            ORDER BY ls.lich_su_id DESC
            """, bienTheId);
    }

    public List<Map<String, Object>> danhSachNhanVien() {
        return jdbc.queryForList("""
            SELECT nhan_vien_id AS nhanVienId, ho_ten AS hoTen
            FROM nhan_vien
            WHERE da_xoa = 0 AND trang_thai = N'active'
            ORDER BY ho_ten
            """);
    }

    public List<Map<String, Object>> danhSachPhieuNhap() {
        return jdbc.queryForList("""
            SELECT TOP 100 pn.phieu_nhap_id AS phieuNhapId, pn.ma_phieu_nhap AS maPhieuNhap,
                   pn.ngay_nhap AS ngayNhap, pn.tong_tien AS tongTien, pn.trang_thai AS trangThai,
                   pn.ghi_chu AS ghiChu, ncc.ten_nha_cung_cap AS tenNhaCungCap,
                   nv.ho_ten AS nhanVien,
                   (SELECT SUM(ct.so_luong) FROM chi_tiet_phieu_nhap ct
                     WHERE ct.phieu_nhap_id = pn.phieu_nhap_id) AS tongSoLuong
            FROM phieu_nhap_kho pn
            LEFT JOIN nha_cung_cap ncc ON ncc.nha_cung_cap_id = pn.nha_cung_cap_id
            LEFT JOIN nhan_vien nv     ON nv.nhan_vien_id = pn.nhan_vien_id
            ORDER BY pn.phieu_nhap_id DESC
            """);
    }

    /* ═══════════════════ NHẬP HÀNG ═══════════════════ */

    /**
     * Toàn bộ phiếu nhập nằm trong MỘT transaction: hỏng bất kỳ dòng nào thì không có gì
     * được ghi, tránh cảnh phiếu có mà serial thiếu hoặc tồn kho lệch.
     */
    @Transactional
    public Map<String, Object> nhapHang(NhapKhoRequest req) {
        kiemTraDongNhap(req);

        LocalDateTime ngayNhap = req.getNgayNhap() != null ? req.getNgayNhap() : LocalDateTime.now();
        BigDecimal tongTien = BigDecimal.ZERO;
        for (NhapKhoRequest.DongNhap d : req.getDongNhap()) {
            tongTien = tongTien.add(d.getDonGiaNhap().multiply(BigDecimal.valueOf(d.getSoLuong())));
        }

        // Tổng tiền tính lại ở server, không tin số do client gửi lên
        Integer phieuNhapId = jdbc.queryForObject("""
                INSERT INTO phieu_nhap_kho (nha_cung_cap_id, nhan_vien_id, ngay_nhap, tong_tien, trang_thai, ghi_chu)
                OUTPUT INSERTED.phieu_nhap_id
                VALUES (?, ?, ?, ?, N'hoan_thanh', ?)
                """, Integer.class,
                req.getNhaCungCapId(), req.getNhanVienId(), Timestamp.valueOf(ngayNhap), tongTien, req.getGhiChu());

        int tongSerial = 0;

        for (NhapKhoRequest.DongNhap d : req.getDongNhap()) {
            jdbc.update("""
                    INSERT INTO chi_tiet_phieu_nhap (phieu_nhap_id, bien_the_id, so_luong, don_gia_nhap)
                    VALUES (?, ?, ?, ?)
                    """, phieuNhapId, d.getBienTheId(), d.getSoLuong(), d.getDonGiaNhap());

            if (d.isTheoSerial()) {
                List<Object[]> batch = new ArrayList<>();
                for (String serial : d.getSerials()) {
                    batch.add(new Object[]{d.getBienTheId(), serial.trim(), Timestamp.valueOf(ngayNhap), d.getGhiChu()});
                }
                try {
                    // Trigger trg_CapNhatTonKhoThucTe tự cộng ton_kho theo số dòng vừa thêm.
                    jdbc.batchUpdate("""
                            INSERT INTO chi_tiet_san_pham (bien_the_id, so_serial, trang_thai, ngay_nhap_kho, ghi_chu)
                            VALUES (?, ?, N'trong_kho', ?, ?)
                            """, batch);
                } catch (DuplicateKeyException e) {
                    // Chặn được ở bước kiểm tra phía trên, trừ khi có người nhập song song
                    throw new IllegalArgumentException(
                            "Một số serial vừa bị người khác nhập trước. Tải lại trang và kiểm tra lại danh sách serial.");
                }
                tongSerial += batch.size();
            } else {
                // Không quản lý serial: tự cộng tồn (không có trigger nào lo hộ ở nhánh này)
                jdbc.update("""
                        INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu)
                        SELECT ?, 0, 0, 5
                        WHERE NOT EXISTS (SELECT 1 FROM ton_kho WHERE bien_the_id = ?)
                        """, d.getBienTheId(), d.getBienTheId());

                jdbc.update("""
                        UPDATE ton_kho
                        SET so_luong_ton_thuc_te = so_luong_ton_thuc_te + ?, ngay_cap_nhat = GETDATE()
                        WHERE bien_the_id = ?
                        """, d.getSoLuong(), d.getBienTheId());
            }

            jdbc.update("""
                    INSERT INTO lich_su_ton_kho (bien_the_id, loai_bien_dong, so_luong_thay_doi,
                                                 phieu_nhap_id, nhan_vien_id, ghi_chu)
                    VALUES (?, N'nhap', ?, ?, ?, ?)
                    """, d.getBienTheId(), d.getSoLuong(), phieuNhapId, req.getNhanVienId(),
                    d.isTheoSerial() ? "Nhập kho theo serial" : "Nhập kho không quản lý serial");

            if (req.isCapNhatGiaNhap()) capNhatGiaNhap(d.getBienTheId(), d.getDonGiaNhap());
        }

        String maPhieu = jdbc.queryForObject(
                "SELECT ma_phieu_nhap FROM phieu_nhap_kho WHERE phieu_nhap_id = ?", String.class, phieuNhapId);

        Map<String, Object> ketQua = new LinkedHashMap<>();
        ketQua.put("phieuNhapId", phieuNhapId);
        ketQua.put("maPhieuNhap", maPhieu);
        ketQua.put("soDong", req.getDongNhap().size());
        ketQua.put("soSerial", tongSerial);
        ketQua.put("tongTien", tongTien);
        return ketQua;
    }

    /* ═══════════════════ SỬA / ĐIỀU CHỈNH ═══════════════════ */

    /** Cập nhật mọi trường có thể sửa của một biến thể. */
    @Transactional
    public void capNhatBienThe(Integer bienTheId, Map<String, Object> body) {
        BigDecimal giaNhap = soTien(body.get("giaNhap"));
        BigDecimal giaBan = soTien(body.get("giaBan"));
        if (giaBan.compareTo(giaNhap.multiply(new BigDecimal("0.5"))) < 0)
            throw new IllegalArgumentException("Giá bán phải lớn hơn hoặc bằng 50% giá nhập");

        String trangThai = "active".equals(body.get("trangThai")) ? "active" : "inactive";
        String maSku = chuoi(body.get("maSku"));
        if (maSku == null) throw new IllegalArgumentException("Mã SKU không được để trống");

        Integer trungSku = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bien_the_san_pham WHERE ma_sku = ? AND bien_the_id <> ?",
                Integer.class, maSku, bienTheId);
        if (trungSku != null && trungSku > 0)
            throw new IllegalArgumentException("Mã SKU '" + maSku + "' đã thuộc về biến thể khác");

        jdbc.update("""
                UPDATE bien_the_san_pham
                SET ma_sku = ?, gia_nhap = ?, gia_ban = ?, bao_hanh_thang = ?, mau_sac = ?,
                    cpu_id = ?, ram_id = ?, o_cung_id = ?, gpu_id = ?,
                    kich_thuoc_man_hinh = ?, he_dieu_hanh = ?, pin = ?, trong_luong_kg = ?,
                    hinh_anh_bien_the = ?, phan_loai_tags = ?, phan_loai_ten = ?,
                    trang_thai = ?, ngay_cap_nhat = GETDATE()
                WHERE bien_the_id = ?
                """,
                maSku, giaNhap, giaBan, so(body.get("baoHanhThang")), chuoi(body.get("mauSac")),
                so(body.get("cpuId")), so(body.get("ramId")), so(body.get("oCungId")), so(body.get("gpuId")),
                chuoi(body.get("kichThuocManHinh")), chuoi(body.get("heDieuHanh")), chuoi(body.get("pin")),
                soTienHoacNull(body.get("trongLuongKg")), chuoi(body.get("hinhAnhBienThe")),
                chuoi(body.get("phanLoaiTags")), chuoi(body.get("phanLoaiTen")),
                trangThai, bienTheId);
    }

    /** Đổi trạng thái một máy cụ thể. Tồn kho do trigger tự điều chỉnh theo trạng thái mới. */
    @Transactional
    public void doiTrangThaiSerial(Integer chiTietId, String trangThaiMoi, String ghiChu, Integer nhanVienId) {
        if (!TRANG_THAI_SERIAL.contains(trangThaiMoi))
            throw new IllegalArgumentException("Trạng thái serial không hợp lệ: " + trangThaiMoi);

        Map<String, Object> hienTai = jdbc.queryForMap(
                "SELECT bien_the_id, so_serial, trang_thai FROM chi_tiet_san_pham WHERE chi_tiet_id = ?", chiTietId);
        String trangThaiCu = (String) hienTai.get("trang_thai");
        if (trangThaiMoi.equals(trangThaiCu)) return;

        jdbc.update("UPDATE chi_tiet_san_pham SET trang_thai = ?, ghi_chu = ? WHERE chi_tiet_id = ?",
                trangThaiMoi, ghiChu, chiTietId);

        int thayDoi = ("trong_kho".equals(trangThaiMoi) ? 1 : 0) - ("trong_kho".equals(trangThaiCu) ? 1 : 0);
        jdbc.update("""
                INSERT INTO lich_su_ton_kho (bien_the_id, chi_tiet_id, loai_bien_dong, so_luong_thay_doi,
                                             nhan_vien_id, ghi_chu)
                VALUES (?, ?, N'dieu_chinh', ?, ?, ?)
                """,
                hienTai.get("bien_the_id"), chiTietId, thayDoi, nhanVienId,
                "Serial " + hienTai.get("so_serial") + ": " + trangThaiCu + " → " + trangThaiMoi);
    }

    @Transactional
    public void capNhatTonToiThieu(Integer bienTheId, Integer tonToiThieu) {
        if (tonToiThieu == null || tonToiThieu < 0)
            throw new IllegalArgumentException("Tồn tối thiểu phải lớn hơn hoặc bằng 0");

        jdbc.update("""
                INSERT INTO ton_kho (bien_the_id, so_luong_ton_thuc_te, so_luong_giu, ton_kho_toi_thieu)
                SELECT ?, 0, 0, ?
                WHERE NOT EXISTS (SELECT 1 FROM ton_kho WHERE bien_the_id = ?)
                """, bienTheId, tonToiThieu, bienTheId);

        jdbc.update("UPDATE ton_kho SET ton_kho_toi_thieu = ?, ngay_cap_nhat = GETDATE() WHERE bien_the_id = ?",
                tonToiThieu, bienTheId);
    }

    /* ═══════════════════ Helper ═══════════════════ */

    /** Kiểm tra sạch sẽ trước khi ghi: số lượng, serial trùng trong phiếu, serial đã có trong kho. */
    private void kiemTraDongNhap(NhapKhoRequest req) {
        Set<String> trongPhieu = new HashSet<>();
        List<String> tatCaSerial = new ArrayList<>();

        for (NhapKhoRequest.DongNhap d : req.getDongNhap()) {
            Integer ton = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM bien_the_san_pham WHERE bien_the_id = ? AND da_xoa = 0",
                    Integer.class, d.getBienTheId());
            if (ton == null || ton == 0)
                throw new IllegalArgumentException("Biến thể id " + d.getBienTheId() + " không tồn tại");

            if (!d.isTheoSerial()) continue;

            List<String> serials = d.getSerials() == null ? List.of() : d.getSerials().stream()
                    .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList();

            if (serials.size() != d.getSoLuong())
                throw new IllegalArgumentException("Khai báo nhập " + d.getSoLuong() + " máy nhưng có "
                        + serials.size() + " serial — số serial phải khớp đúng số lượng");

            for (String s : serials) {
                if (!trongPhieu.add(s))
                    throw new IllegalArgumentException("Serial '" + s + "' bị lặp lại trong cùng phiếu nhập");
            }
            tatCaSerial.addAll(serials);
        }

        if (tatCaSerial.isEmpty()) return;

        List<String> daCo = namedJdbc.queryForList(
                "SELECT so_serial FROM chi_tiet_san_pham WHERE so_serial IN (:serials)",
                new MapSqlParameterSource("serials", tatCaSerial), String.class);

        if (!daCo.isEmpty())
            throw new IllegalArgumentException("Các serial sau đã tồn tại trong hệ thống: "
                    + String.join(", ", daCo) + ". Vui lòng kiểm tra lại.");
    }

    private void capNhatGiaNhap(Integer bienTheId, BigDecimal donGia) {
        BigDecimal giaBan = jdbc.queryForObject(
                "SELECT gia_ban FROM bien_the_san_pham WHERE bien_the_id = ?", BigDecimal.class, bienTheId);
        if (giaBan != null && giaBan.compareTo(donGia.multiply(new BigDecimal("0.5"))) < 0)
            throw new IllegalArgumentException(
                    "Không cập nhật được giá nhập cho biến thể " + bienTheId
                            + ": giá bán hiện tại thấp hơn 50% giá nhập mới. Sửa giá bán trước.");

        jdbc.update("UPDATE bien_the_san_pham SET gia_nhap = ?, ngay_cap_nhat = GETDATE() WHERE bien_the_id = ?",
                donGia, bienTheId);
    }

    private String chuoi(Object v) {
        String s = v == null ? null : String.valueOf(v).trim();
        return (s == null || s.isEmpty()) ? null : s;
    }

    private Integer so(Object v) {
        String s = chuoi(v);
        return s == null ? null : Integer.valueOf(s);
    }

    private BigDecimal soTien(Object v) {
        String s = chuoi(v);
        return s == null ? BigDecimal.ZERO : new BigDecimal(s);
    }

    private BigDecimal soTienHoacNull(Object v) {
        String s = chuoi(v);
        return s == null ? null : new BigDecimal(s);
    }
}