# Plan: Sửa lỗi xóa phiếu nhập để lại serial orphan trong DB

## Context

**Triệu chứng (từ user):**
- Tạo phiếu nhập mới với serial SR0011 → SR0020 → bị báo "Serial đã tồn tại trong kho".
- Mở tab serial của phiếu mới → rỗng.
- Trong các phiếu nhập hiện có → không thấy serial này.
- Không có lịch sử tạo phiếu nào chứa serial này.

**Nguyên nhân thực sự (xác nhận với user):**
- Phiếu nhập TRƯỚC đó đã từng có serial SR0011 → SR0020 (user xác nhận).
- Phiếu đó đã bị **xóa cứng** bằng `PhieuNhapKhoService.delete(id)`.

**Bug:** `PhieuNhapKhoService.delete()` ở [PhieuNhapKhoService.java:76-82](BackEnd/src/main/java/com/example/backend/service/PhieuNhapKhoService.java#L76-L82) chỉ xóa `chi_tiet_phieu_nhap` + `phieu_nhap_kho`, **không đụng `chi_tiet_san_pham`** (serial đã được insert từ lúc `approve()`). Hệ quả:

1. Serial `SR0011` → `SR0020` vẫn nằm trong bảng `chi_tiet_san_pham` với `da_xoa = false` và `trang_thai = 'trong_kho'` (trỏ FK `phieu_nhap_id` vào phiếu đã bị xóa → orphan).
2. Lần nhập lại, frontend gọi `PhieuNhapKhoService.kiemTraSerialDb()` ([PhieuNhapKhoService.js:18](FrontEnd/QLBanMayTinh/src/services/PhieuNhapKhoService.js)) → backend `kiemTraSerialVoiDb` ([PhieuNhapKhoService.java:90-96](BackEnd/src/main/java/com/example/backend/service/PhieuNhapKhoService.java#L90)) dùng `findBySoSerialInAndDaXoaFalse` → vẫn tìm thấy serial orphan → trả về list trùng → FE toast báo "đã tồn tại".
3. Tab serial của phiếu mới rỗng vì phiếu mới chưa được duyệt (`approve()` chưa chạy), `findByPhieuNhapId(mới)` trả `[]`. Serial orphan thuộc phiếu cũ đã xóa nên `findByPhieuNhapId(cũ)` cũng không trả (phiếu cũ đã bốc hơi khỏi bảng `phieu_nhap_kho`).
4. Các phiếu hiện có không chứa serial đó vì serial orphan trỏ FK vào phiếu không tồn tại.

## Mục tiêu

Khi xóa phiếu nhập, dọn sạch các serial đã được tạo từ phiếu đó để tránh orphan — **trừ các serial đã bán/đang giữ hàng**, vì chúng liên kết với đơn hàng thật và phải bảo toàn.

## Phương án

Sửa `PhieuNhapKhoService.delete()`:

- Truy vấn tất cả `chi_tiet_san_pham` của phiếu (đã có sẵn qua repo method mới).
- Lọc ra các serial **chưa bán**:
  - `trang_thai = 'trong_kho'` (chưa xuất kho)
  - **Và** không có FK trong `chi_tiet_don_hang_serial`
- Soft-delete các serial đó: set `da_xoa = true`, set `ghi_chu = "Xóa cùng phiếu nhập #X"`, **KHÔNG xóa cứng** để giữ audit và tránh phá unique constraint của serial đã từng bán.
- Ghi `lich_su_ton_kho` với `loai_bien_dong = 'xoa_serial'`, `so_luong_thay_doi = -1`, `ghi_chu` giải thích lý do.
- Sau khi dọn serial, xóa `chi_tiet_phieu_nhap` + `phieu_nhap_kho` như cũ.

**Vì sao soft-delete thay vì xóa cứng:**
- Bảng `chi_tiet_san_pham.so_serial` có ràng buộc `UNIQUE` — nếu trong quá khứ user từng bán serial SR0011 rồi trả hàng về kho, xóa cứng sẽ phá vỡ khả năng audit/đối soát với lịch sử đơn hàng cũ. Soft-delete giữ row nhưng loại khỏi mọi query `da_xoa=false`.
- Đồng thời, đảm bảo `kiemTraSerialVoiDb` (dùng `findBySoSerialInAndDaXoaFalse`) sẽ **không** bắt được serial orphan sau khi xóa → sửa được triệu chứng user báo.

**Vì sao KHÔNG xóa serial đã bán:**
- Serial `trang_thai = 'da_ban'` hoặc `trang_thai = 'giu_hang'` đang liên kết với đơn hàng trong `chi_tiet_don_hang_serial` — xóa sẽ phá vỡ tính toàn vẹn tham chiếu và lịch sử bảo hành. Nếu muốn xóa, user phải hủy đơn hàng trước.

## Files thay đổi

### 1. `BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java`

Thêm method:

```java
// Lấy tất cả serial (kể cả đã bán/giữ) thuộc 1 phiếu nhập — dùng cho dọn rác khi xóa phiếu.
List<ChiTietSanPham> findByPhieuNhap_PhieuNhapId(Integer phieuNhapId);
```

Không cần filter `daXoa` vì service muốn thấy cả soft-deleted để tránh xóa trùng lần nữa.

### 2. `BackEnd/src/main/java/com/example/backend/repository/ChiTietDonHangSerialRepository.java`

Thêm method:

```java
// Check serial đã được bán/giao cho khách — dùng để quyết định có được xóa khi xóa phiếu nhập không.
boolean existsByChiTietSanPham_ChiTietId(Integer chiTietId);
```

### 3. `BackEnd/src/main/java/com/example/backend/service/PhieuNhapKhoService.java`

Inject thêm `ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;`.

Sửa `delete(Integer id)` thành:

```java
@Transactional
public void delete(Integer id) {
    if (!phieuNhapKhoRepository.existsById(id))
        throw new IllegalArgumentException("Phiếu nhập kho không tồn tại với id: " + id);

    PhieuNhapKho phieu = getById(id);

    // 1) Dọn serial thuộc phiếu mà CHƯA bán/giữ — soft-delete để không vi phạm UNIQUE(so_serial)
    //    cho serial từng bán-trả-hàng. Serial đã bán/giữ giữ nguyên để bảo toàn tham chiếu
    //    từ chi_tiet_don_hang_serial và lịch sử bảo hành.
    List<ChiTietSanPham> serials = chiTietSanPhamRepository.findByPhieuNhap_PhieuNhapId(id);
    int deleted = 0;
    for (ChiTietSanPham s : serials) {
        if (Boolean.TRUE.equals(s.getDaXoa())) continue;
        if (chiTietDonHangSerialRepository.existsByChiTietSanPham_ChiTietId(s.getChiTietId())) continue;
        // Chỉ xóa được serial 'trong_kho' — 'da_ban' / 'giu_hang' đều đã có FK đơn hàng thật.
        if (!"trong_kho".equals(s.getTrangThai())) continue;
        s.setDaXoa(true);
        s.setGhiChu("Đã xóa cùng phiếu nhập #" + id + " (" + phieu.getMaPhieuNhap() + ")");
        chiTietSanPhamRepository.save(s);
        // Audit log
        LichSuTonKho log = new LichSuTonKho();
        log.setBienThe(s.getBienThe());
        log.setChiTietSanPham(s);
        log.setLoaiBienDong("xoa_serial");
        log.setSoLuongThayDoi(-1);
        log.setPhieuNhapKho(null);  // FK phiếu sắp xóa
        log.setNhanVien(phieu.getNhanVien());
        log.setGhiChu("Xóa serial " + s.getSoSerial() + " do xóa phiếu nhập #" + id);
        log.setNgayTao(LocalDateTime.now());
        lichSuTonKhoRepository.save(log);
        deleted++;
    }
    if (deleted > 0)
        log.info("Đã soft-delete {} serial khi xóa phiếu nhập #{}", deleted, id);

    // 2) Xóa dòng phiếu + header phiếu (logic cũ).
    chiTietPhieuNhapRepository.deleteByPhieuNhapKho_PhieuNhapId(id);
    phieuNhapKhoRepository.deleteById(id);
}
```

**Lưu ý quan trọng:**
- Không gọi `releaseOrphanSerials()` ở đây vì:
  - Serial `trong_kho` không phải `giu_hang` → không phải orphan theo logic hiện tại.
  - Soft-delete là đủ — nó loại khỏi query `daXoa=false` và khỏi bảng tồn kho thực tế.
- `log` (SLF4J) cần inject `private static final Logger log = LoggerFactory.getLogger(PhieuNhapKhoService.class);` — thêm ở đầu class (class hiện chưa có).

### 4. Dọn dữ liệu cũ (1 lần)

User nói "đã có" serial orphan SR0011-SR0020 trong DB. **Sau khi deploy fix**, cần dọn tay 1 lần bằng SQL để các serial orphan hiện tại không còn cản trở:

```sql
-- Tìm serial trỏ vào phiếu không tồn tại
SELECT c.chi_tiet_id, c.so_serial, c.trang_thai, c.phieu_nhap_id
FROM chi_tiet_san_pham c
LEFT JOIN phieu_nhap_kho p ON c.phieu_nhap_id = p.phieu_nhap_id
WHERE c.da_xoa = false AND p.phieu_nhap_id IS NULL;

-- Soft-delete chúng
UPDATE chi_tiet_san_pham
SET da_xoa = true, ghi_chu = CONCAT(IFNULL(ghi_chu,''), ' [Dọn rác orphan]')
WHERE phieu_nhap_id NOT IN (SELECT phieu_nhap_id FROM phieu_nhap_kho)
  AND da_xoa = false
  AND trang_thai = 'trong_kho'
  AND chi_tiet_id NOT IN (SELECT chi_tiet_id FROM chi_tiet_don_hang_serial);
```

(Ghi chú: Câu SQL sẽ chạy thủ công sau khi user xác nhận; không phải code Java.)

## Verification

1. **Backend compile + chạy được:**
   - `mvn -pl BackEnd compile` (hoặc IDE).
   - Restart backend.

2. **Test happy path:**
   - Tạo phiếu nhập với 5 serial mới (chưa có trong DB) → lưu → duyệt → thấy serial xuất hiện trong tab serial của phiếu.
   - Xóa phiếu đó.
   - Tạo lại phiếu nhập cũ serial → không còn báo "đã có".

3. **Test biên:**
   - Tạo phiếu → duyệt → bán 1 serial (qua POS / tạo đơn) → xóa phiếu nhập → serial đã bán vẫn còn trong DB (`trang_thai='da_ban'`, `da_xoa=false`), không bị soft-delete. Lần nhập lại serial này vẫn báo "đã có" → đúng (vì nó thật sự đang được dùng).
   - Serial `trong_kho` không liên kết đơn nào: soft-delete OK, log ghi audit.

4. **Test tab serial phiếu mới (case user báo):**
   - Trước fix: tab serial rỗng (vì approve chưa chạy) — đúng.
   - Sau khi chạy SQL dọn orphan + import lại: tạo phiếu với SR0011-SR0020 (đã từng ở phiếu cũ) → lưu → duyệt → tab serial hiển thị 10 serial.

## Phạm vi KHÔNG đụng

- Không đổi logic `approve()`.
- Không đổi logic `kiemTraSerialVoiDb` / `kiemTraSerialTrung` — chúng vẫn đúng với giả định serial chưa xóa.
- Không sửa frontend (FE đã chính xác — vấn đề nằm ở DB state, không phải logic FE).
