# Thiết kế: Vòng quay may mắn (khách hàng đổi điểm quay khuyến mãi)

## Bối cảnh

Khách hàng hiện có `khach_hang.diem_tich_luy` (cộng tự động khi đơn hàng `delivered`, qua
trigger `trg_don_hang_cong_diem`), tiêu điểm được duy nhất qua 1 luồng đã có sẵn: đổi điểm
lấy 1 phần thưởng cố định trong danh mục `dm_doi_thuong`
(`PhieuGiamGiaCaNhanService.doiThuong()`), sinh ra 1 dòng `phieu_giam_gia_ca_nhan` — voucher
cá nhân, có mã riêng (DB tự sinh), hạn dùng 30 ngày, được `DonHangService.create()` kiểm tra
đầy đủ (thuộc đúng khách, chưa dùng, chưa hết hạn) lúc checkout.

Tách biệt với đó là `khuyen_mai` — mã khuyến mãi CÔNG KHAI (ai cũng nhập được lúc checkout),
quản lý CRUD trực tiếp trong `AdminPage.vue` (section `currentPage === 'promotions'`,
~dòng 1319), có giới hạn số lần dùng chung (`so_luong_toi_da`/`so_lan_da_dung`), không gắn
với cá nhân khách nào.

Yêu cầu: thêm 1 vòng quay may mắn — khách tốn điểm tích lũy để quay, phần thưởng lấy TỪ
danh sách khuyến mãi (`khuyen_mai`) đang active, khi trúng thì sinh ra 1 **phiếu giảm giá cá
nhân mới** (không phải mở khoá mã công khai) — tái dùng nguyên cơ chế `phieu_giam_gia_ca_nhan`
đã có, chỉ thay nguồn dữ liệu clone từ `dm_doi_thuong` sang `khuyen_mai`. Xác nhận quan trọng:
`PhieuGiamGiaCaNhan.doiThuong` là `@ManyToOne` KHÔNG bắt buộc
(`doi_thuong_id INT NULL` ở DB) — phiếu do vòng quay tạo ra hoàn toàn hợp lệ với
`doiThuong = null`, không cần đổi entity/bảng `phieu_giam_gia_ca_nhan`, và
`DonHangService.create()` không hề tham chiếu `doiThuong` khi validate lúc checkout (chỉ
check `khachHang`/`daSuDung`/`ngayHetHan`) — nên phiếu do vòng quay sinh ra dùng được ở
checkout ngay, không cần sửa gì thêm ở đó.

**Lưu ý sai khác nhỏ so với lúc brainstorm**: khảo sát lại `AccountPage.vue` (`TABS`, dòng
71-77) cho thấy không có tab "Đổi thưởng" độc lập — phần đổi điểm/danh sách voucher hiện nằm
LỒNG trong tab "Cài đặt" (`id: "settings"`, dòng 556+). Vòng quay sẽ thêm thành 1 tab TOP-LEVEL
mới trong mảng `TABS` (id `"wheel"`, icon 🎡) thay vì "cạnh tab Đổi thưởng" như mô tả ban đầu
— giữ đúng tinh thần (dễ thấy, gần chỗ đang hiện điểm/voucher) nhưng khớp đúng cấu trúc tab
thật của trang.

## Phạm vi

1. Bảng mới `cau_hinh_vong_quay` (cấu hình chung, 1 dòng) và `lich_su_quay` (lịch sử mỗi lượt
   quay) — thêm vào `Database/QLBanMayTinh.sql` theo đúng pattern idempotent
   (`IF NOT EXISTS (SELECT 1 FROM sys.tables ...) BEGIN CREATE TABLE ... END`) đã dùng cho
   mọi bảng khác trong file, vì người dùng luôn chạy lại NGUYÊN file này mỗi lần.
2. Backend: `VongQuayService` + `VongQuayController` mới, tái dùng pattern khoá
   `findWithLockByKhachHangId` đã có ở `PhieuGiamGiaCaNhanService`.
3. Frontend khách hàng: tab mới "🎡 Vòng quay may mắn" trong `AccountPage.vue`, component mới
   `LuckyWheelPanel.vue`.
4. Frontend admin: khối cấu hình nhỏ (điểm/lượt, % trượt) chèn vào đầu section khuyến mãi có
   sẵn trong `AdminPage.vue`.
5. i18n: thêm khối `wheel.*` (khách hàng) và `admin.wheelConfig.*` (admin) cho cả 5 ngôn ngữ.

## Dữ liệu (DB)

Thêm vào `Database/QLBanMayTinh.sql`, cạnh khối tạo bảng `phieu_giam_gia_ca_nhan` (theo đúng
style file: kiểm tra tồn tại trước khi tạo):

```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cau_hinh_vong_quay')
BEGIN
    CREATE TABLE cau_hinh_vong_quay (
        id             INT            NOT NULL PRIMARY KEY CHECK (id = 1),
        diem_moi_luot  INT            NOT NULL CHECK (diem_moi_luot > 0),
        ty_le_truot    INT            NOT NULL CHECK (ty_le_truot BETWEEN 0 AND 100),
        ngay_cap_nhat  DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_quay')
BEGIN
    CREATE TABLE lich_su_quay (
        id                         INT      IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id              INT      NOT NULL,
        ngay_quay                  DATETIME NOT NULL DEFAULT GETDATE(),
        ket_qua                    NVARCHAR(10) NOT NULL CONSTRAINT CK_lsq_ket_qua CHECK (ket_qua IN (N'trung', N'truot')),
        khuyen_mai_id              INT      NULL,
        phieu_giam_gia_ca_nhan_id  INT      NULL,
        diem_da_tru                INT      NOT NULL,
        CONSTRAINT FK_lsq_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_lsq_khuyen_mai FOREIGN KEY (khuyen_mai_id) REFERENCES khuyen_mai(khuyen_mai_id),
        CONSTRAINT FK_lsq_phieu FOREIGN KEY (phieu_giam_gia_ca_nhan_id) REFERENCES phieu_giam_gia_ca_nhan(phieu_id)
    );
END
GO
```

`id = 1` CHECK constraint trên `cau_hinh_vong_quay` đảm bảo mãi mãi chỉ có đúng 1 dòng —
không cần seed INSERT trong SQL (tránh xung đột lúc file bị chạy lại): `VongQuayService` tự
tạo dòng mặc định (100 điểm/lượt, 30% trượt) ở lần `GET` đầu tiên nếu bảng đang rỗng.

## Backend

### Entity mới

`entity/CauHinhVongQuay.java`:
```java
@Data @NoArgsConstructor @AllArgsConstructor @Getter @Setter
@Entity
@Table(name = "cau_hinh_vong_quay")
public class CauHinhVongQuay {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "diem_moi_luot", nullable = false)
    private Integer diemMoiLuot;

    @Column(name = "ty_le_truot", nullable = false)
    private Integer tyLeTruot;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat;
}
```

`entity/LichSuQuay.java` — theo đúng khuôn `PhieuGiamGiaCaNhan.java` (các `@ManyToOne(fetch =
FetchType.LAZY)` nullable cho `khuyenMai`/`phieuGiamGiaCaNhan`):
```java
@Data @NoArgsConstructor @AllArgsConstructor @Getter @Setter
@Entity
@Table(name = "lich_su_quay")
public class LichSuQuay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ngay_quay", nullable = false)
    private LocalDateTime ngayQuay;

    @Column(name = "ket_qua", length = 10, nullable = false)
    private String ketQua; // "trung" | "truot"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_giam_gia_ca_nhan_id")
    private PhieuGiamGiaCaNhan phieuGiamGiaCaNhan;

    @Column(name = "diem_da_tru", nullable = false)
    private Integer diemDaTru;
}
```

### Repository mới

```java
public interface CauHinhVongQuayRepository extends JpaRepository<CauHinhVongQuay, Integer> {}

public interface LichSuQuayRepository extends JpaRepository<LichSuQuay, Integer> {
    // Projection thẳng ra Response (đúng pattern ChiTietDonHangSerialRepository.findByDonHangId)
    // — tránh N+1 lazy-load lịch sử.khuyenMai.tenKhuyenMai riêng cho mỗi dòng.
    @Query("SELECT new com.example.backend.response.LichSuQuayResponse(" +
           "l.id, l.ngayQuay, l.ketQua, k.tenKhuyenMai, l.diemDaTru) " +
           "FROM LichSuQuay l LEFT JOIN l.khuyenMai k " +
           "WHERE l.khachHang.khachHangId = :khachHangId ORDER BY l.ngayQuay DESC")
    List<LichSuQuayResponse> findResponsesByKhachHangId(@Param("khachHangId") Integer khachHangId);
}
```

`KhuyenMaiRepository` cần thêm 1 query lấy khuyến mãi active còn hiệu lực (dùng chung cho
việc vẽ vòng quay VÀ việc random chọn thưởng, tránh 2 nơi lặp lại điều kiện lọc):
```java
@Query("SELECT k FROM KhuyenMai k WHERE k.trangThai = 'active' " +
       "AND k.ngayBatDau <= CURRENT_TIMESTAMP AND k.ngayKetThuc >= CURRENT_TIMESTAMP")
List<KhuyenMai> findActiveKhaDung();
```

### `VongQuayService` — mới, mirror `PhieuGiamGiaCaNhanService`

```java
@Service
public class VongQuayService {
    @Autowired private CauHinhVongQuayRepository cauHinhRepository;
    @Autowired private LichSuQuayRepository lichSuQuayRepository;
    @Autowired private KhuyenMaiRepository khuyenMaiRepository;
    @Autowired private KhachHangRepository khachHangRepository;
    @Autowired private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;

    private static final Random RANDOM = new Random();

    // Giống PhieuGiamGiaCaNhanService.currentKhachHangId() — chặn tài khoản staff gọi nhầm.
    private Integer currentKhachHangId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TaiKhoan tk = taiKhoanRepository.findByUsername(username).orElse(null);
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Chỉ khách hàng mới quay được vòng quay");
        return tk.getKhachHang().getKhachHangId();
    }

    @Transactional
    public CauHinhVongQuay getOrCreateCauHinh() {
        return cauHinhRepository.findById(1)
                .orElseGet(() -> cauHinhRepository.save(
                        new CauHinhVongQuay(1, 100, 30, LocalDateTime.now())));
    }

    public CauHinhVongQuayResponse getCauHinhChoKhachHang() {
        CauHinhVongQuay ch = getOrCreateCauHinh();
        List<KhuyenMaiResponse> khaDung = khuyenMaiRepository.findActiveKhaDung().stream()
                .map(VongQuayService::toKhuyenMaiResponse)
                .toList();
        return new CauHinhVongQuayResponse(ch.getDiemMoiLuot(), ch.getTyLeTruot(), khaDung);
    }

    // KhuyenMaiResponse chỉ có @AllArgsConstructor (13 field, xem response/KhuyenMaiResponse.java)
    // — không có constructor nhận thẳng KhuyenMai, nên map tường minh ở đây, dùng lại ở cả
    // getCauHinhChoKhachHang() lẫn quay().
    private static KhuyenMaiResponse toKhuyenMaiResponse(KhuyenMai k) {
        return new KhuyenMaiResponse(k.getKhuyenMaiId(), k.getMaKhuyenMai(), k.getTenKhuyenMai(),
                k.getLoai(), k.getGiaTri(), k.getGiaTriToiDa(), k.getDonHangToiThieu(),
                k.getNgayBatDau(), k.getNgayKetThuc(), k.getSoLuongToiDa(), k.getSoLanDaDung(),
                k.getTrangThai(), k.getNgayTao());
    }

    @Transactional
    public CauHinhVongQuay capNhatCauHinh(CauHinhVongQuayRequest req) {
        CauHinhVongQuay ch = getOrCreateCauHinh();
        ch.setDiemMoiLuot(req.getDiemMoiLuot());
        ch.setTyLeTruot(req.getTyLeTruot());
        ch.setNgayCapNhat(LocalDateTime.now());
        return cauHinhRepository.save(ch);
    }

    @Transactional
    public KetQuaQuayResponse quay() {
        Integer khachHangId = currentKhachHangId();
        CauHinhVongQuay cauHinh = getOrCreateCauHinh();

        // Khoá ghi — chặn 2 lượt quay đồng thời cùng đọc trùng số dư điểm (double-spend),
        // đúng pattern PhieuGiamGiaCaNhanService.doiThuong().
        KhachHang khachHang = khachHangRepository.findWithLockByKhachHangId(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
        if (khachHang.getDiemTichLuy() < cauHinh.getDiemMoiLuot())
            throw new IllegalArgumentException("Không đủ điểm để quay");

        khachHang.setDiemTichLuy(khachHang.getDiemTichLuy() - cauHinh.getDiemMoiLuot());
        khachHangRepository.save(khachHang);

        LichSuQuay lichSu = new LichSuQuay();
        lichSu.setKhachHang(khachHang);
        lichSu.setNgayQuay(LocalDateTime.now());
        lichSu.setDiemDaTru(cauHinh.getDiemMoiLuot());

        List<KhuyenMai> khaDung = khuyenMaiRepository.findActiveKhaDung();
        boolean truot = khaDung.isEmpty() || RANDOM.nextInt(100) < cauHinh.getTyLeTruot();

        if (truot) {
            lichSu.setKetQua("truot");
            lichSuQuayRepository.save(lichSu);
            return new KetQuaQuayResponse("truot", null, null, khachHang.getDiemTichLuy());
        }

        KhuyenMai trung = khaDung.get(RANDOM.nextInt(khaDung.size()));

        PhieuGiamGiaCaNhan phieu = new PhieuGiamGiaCaNhan();
        phieu.setKhachHang(khachHang);
        phieu.setDoiThuong(null); // phiếu từ vòng quay, không gắn danh mục đổi thưởng
        phieu.setLoai(trung.getLoai());
        // gia_tri/gia_tri_toi_da của khuyen_mai là DECIMAL(18,2), của phieu_giam_gia_ca_nhan
        // là DECIMAL(18,0) — làm tròn khi clone để tránh lệch giữa entity Java và cột DB.
        phieu.setGiaTri(trung.getGiaTri().setScale(0, RoundingMode.HALF_UP));
        phieu.setGiaTriToiDa(trung.getGiaTriToiDa() == null ? null
                : trung.getGiaTriToiDa().setScale(0, RoundingMode.HALF_UP));
        phieu.setDaSuDung(false);
        phieu.setNgayDoi(LocalDateTime.now());
        phieu.setNgayHetHan(LocalDateTime.now().plusDays(30));
        PhieuGiamGiaCaNhan savedPhieu = phieuGiamGiaCaNhanRepository.save(phieu);

        lichSu.setKetQua("trung");
        lichSu.setKhuyenMai(trung);
        lichSu.setPhieuGiamGiaCaNhan(savedPhieu);
        lichSuQuayRepository.save(lichSu);

        return new KetQuaQuayResponse("trung", toKhuyenMaiResponse(trung),
                new PhieuGiamGiaCaNhanResponse(savedPhieu.getPhieuId(), savedPhieu.getMaPhieu(),
                        savedPhieu.getLoai(), savedPhieu.getGiaTri(), savedPhieu.getGiaTriToiDa(),
                        savedPhieu.getDaSuDung(), savedPhieu.getNgayDoi(), savedPhieu.getNgayHetHan()),
                khachHang.getDiemTichLuy());
    }

    public List<LichSuQuayResponse> getLichSuCuaToi() {
        return lichSuQuayRepository.findResponsesByKhachHangId(currentKhachHangId());
    }
}
```

### Response DTO mới

Đều `@NoArgsConstructor @AllArgsConstructor @Getter @Setter`, đúng khuôn `KhuyenMaiResponse`/
`PhieuGiamGiaCaNhanResponse` đã xác nhận ở trên (field theo đúng thứ tự khai báo):

- `CauHinhVongQuayResponse(Integer diemMoiLuot, Integer tyLeTruot, List<KhuyenMaiResponse> khuyenMaiKhaDung)`
- `KetQuaQuayResponse(String ketQua, KhuyenMaiResponse khuyenMai, PhieuGiamGiaCaNhanResponse phieuGiamGia, Integer diemConLai)`
- `LichSuQuayResponse(Integer id, LocalDateTime ngayQuay, String ketQua, String tenKhuyenMai, Integer diemDaTru)`
  — thứ tự này PHẢI khớp đúng danh sách trong `SELECT new ...LichSuQuayResponse(...)` ở
  `LichSuQuayRepository.findResponsesByKhachHangId` phía trên.
- `request/CauHinhVongQuayRequest { @NotNull @Min(1) Integer diemMoiLuot; @NotNull @Min(0) @Max(100) Integer tyLeTruot; }`

### `VongQuayController`

Theo đúng 2 pattern đang dùng song song trong codebase: GET không có `@PreAuthorize` (rơi
vào `.anyRequest().authenticated()` trong `SecurityConfig`, tức "mở cho mọi role đã đăng
nhập" — giống `KhuyenMaiController.getAll()`); còn action tốn điểm/đổi cấu hình thì chặn rõ
bằng `@PreAuthorize`, giống `PhieuGiamGiaCaNhanController`/`KhuyenMaiController`:

```java
@RestController
@RequestMapping("/api/vong-quay")
public class VongQuayController {
    @Autowired private VongQuayService vongQuayService;

    @GetMapping("cau-hinh")
    public CauHinhVongQuayResponse getCauHinh() {
        return vongQuayService.getCauHinhChoKhachHang();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("cau-hinh")
    public CauHinhVongQuay capNhatCauHinh(@Valid @RequestBody CauHinhVongQuayRequest req) {
        return vongQuayService.capNhatCauHinh(req);
    }

    // Không @PreAuthorize role — service tự chặn qua currentKhachHangId(), đúng pattern
    // PhieuGiamGiaCaNhanController (chỉ cần isAuthenticated ở tầng controller).
    @PostMapping("quay")
    public KetQuaQuayResponse quay() {
        return vongQuayService.quay();
    }

    @GetMapping("lich-su/cua-toi")
    public List<LichSuQuayResponse> getLichSuCuaToi() {
        return vongQuayService.getLichSuCuaToi();
    }
}
```

## Frontend

### `Service/VongQuayService.js` (mới)

```js
import { get, post, put } from './api.js';

export const getCauHinh = () => get('/api/vong-quay/cau-hinh');
export const capNhatCauHinh = (body) => put('/api/vong-quay/cau-hinh', body);
export const quay = () => post('/api/vong-quay/quay');
export const getLichSuCuaToi = () => get('/api/vong-quay/lich-su/cua-toi');
```

### `components/account/LuckyWheelPanel.vue` (mới)

- `onMounted`: gọi `getCauHinh()` → lưu `diemMoiLuot`, `tyLeTruot` (chỉ hiển thị tham khảo,
  không dùng để tính toán ở client), `khuyenMaiKhaDung`.
- Vẽ vòng quay: `<div>` tròn, mỗi ô là 1 hình quạt dùng CSS `conic-gradient` chia đều theo
  `khuyenMaiKhaDung.length + 1` (ô cuối luôn là "Chúc may mắn lần sau"), nhãn mỗi ô hiện tóm
  tắt (`loai==='percent' ? '-'+giaTri+'%' : '-'+formatPrice(giaTri)`).
  Cần map cố định thứ tự ô ↔ index để biết góc animate đến đúng ô server trả về — giữ
  `khuyenMaiKhaDung` bất biến trong 1 lần load trang (không refetch giữa các lượt quay,
  chỉ refetch điểm còn lại từ response của chính `quay()`).
- Nút "Quay ngay" — `:disabled="(profile.diemTichLuy < config.diemMoiLuot) || spinning"`.
  `profile` (điểm hiện tại) nhận qua prop từ `AccountPage.vue` (đã load sẵn cho phần hiển thị
  điểm ở header, dòng 313-316) — không tự fetch lại profile riêng trong component này.
- `onSpin()`:
  ```js
  spinning.value = true;
  const res = await VongQuayService.quay();
  const targetIndex = res.ketQua === 'truot'
    ? khuyenMaiKhaDung.value.length // ô cuối
    : khuyenMaiKhaDung.value.findIndex(k => k.khuyenMaiId === res.khuyenMai.khuyenMaiId);
  const anglePerSlice = 360 / (khuyenMaiKhaDung.value.length + 1);
  // Quay thêm nhiều vòng (vd 5*360) rồi dừng đúng giữa ô targetIndex, trừ góc xoay hiện tại
  // để animation luôn quay THEO CHIỀU THUẬN, không giật ngược.
  rotation.value += 5 * 360 + (360 - targetIndex * anglePerSlice - anglePerSlice / 2) - (rotation.value % 360);
  // setTimeout khớp đúng CSS transition-duration, rồi mới hiện modal kết quả + cập nhật điểm
  setTimeout(() => { showResult(res); spinning.value = false; }, 4000);
  ```
- Modal kết quả: trúng → hiện phiếu vừa nhận (mã, loại, giá trị, hạn dùng) + nút "Xem voucher
  của tôi" (chuyển sang phần voucher trong tab Cài đặt); trượt → thông báo nhẹ "Chúc may mắn
  lần sau!".

### `pages/AccountPage.vue`

Thêm vào mảng `TABS` (dòng 71-77), sau `"cancelled"` trước `"settings"`:
```js
{ id: "wheel", icon: "🎡", label: t("account.tabWheel") },
```
Thêm block hiển thị (theo khuôn các `v-if="activeTab === '...'"` đã có):
```html
<div v-if="activeTab === 'wheel'">
  <LuckyWheelPanel />
</div>
```
Import `LuckyWheelPanel` ở đầu file.

### `pages/AdminPage.vue`

Chèn khối cấu hình vào đầu section `currentPage === 'promotions'` (trước dòng 1321, khối
"count + nút Thêm"):
```html
<div class="d-flex align-items-center gap-3 p-3 mb-3 rounded-3"
     style="background:var(--bg-card-inset); border:1px solid var(--border-color);">
  <span class="fw-bold small">{{ t('admin.wheelConfig.title') }}</span>
  <label class="small text-secondary mb-0">{{ t('admin.wheelConfig.pointsPerSpin') }}</label>
  <input v-model.number="wheelConfig.diemMoiLuot" type="number" min="1" class="form-control form-control-sm" style="width:90px;" />
  <label class="small text-secondary mb-0">{{ t('admin.wheelConfig.missRate') }}</label>
  <input v-model.number="wheelConfig.tyLeTruot" type="number" min="0" max="100" class="form-control form-control-sm" style="width:70px;" />
  <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveWheelConfig">{{ t('common.save') }}</button>
</div>
```
`wheelConfig` ref load qua `VongQuayService.getCauHinh()` khi vào tab `promotions` (cùng chỗ
đang load `promotions`), `saveWheelConfig()` gọi `VongQuayService.capNhatCauHinh(wheelConfig.value)`.

### i18n

5 file `src/i18n/locales/{vi,en,ja,ko,zh}.js`, thêm 2 khối mới theo đúng convention hiện có:
- `account.tabWheel`, và nhóm `wheel.*` (tiêu đề, nút quay, nhãn "còn X điểm", thông báo
  trúng/trượt, nhãn nút xem voucher).
- `admin.wheelConfig.*` (title, pointsPerSpin, missRate — tái dùng `common.save` đã có sẵn).

## Xử lý lỗi & edge case

- Không đủ điểm → `IllegalArgumentException("Không đủ điểm để quay")` → 400 (đã có
  `GlobalExceptionHandler.handlerBusinessErrors` bắt sẵn `IllegalArgumentException`, không
  cần viết handler mới).
- Tài khoản không phải khách hàng gọi `POST /quay` → `AccessDeniedException` → 403 (đã có
  `handlerAccessDenied` bắt sẵn).
- Không có khuyến mãi active lúc quay (dù lúc tải trang có) → tự động coi là `truot`, không
  ném lỗi, khách vẫn mất đúng số điểm đã công bố trước khi quay (nhất quán với hành vi UI:
  điểm đã trừ khi bấm quay, không có "hoàn điểm nếu không có gì để trúng" vì đây thuộc rủi ro
  đã biết trước, tương tự 1 lượt quay trong đời thực).
- 2 lượt quay gửi đồng thời cùng 1 khách chỉ đủ điểm 1 lần → `findWithLockByKhachHangId` khoá
  transaction, request thứ 2 đợi request đầu commit rồi mới đọc số dư mới → tự nhận đúng lỗi
  "Không đủ điểm".
- Bảng `cau_hinh_vong_quay` rỗng (lần chạy đầu) → `getOrCreateCauHinh()` tự tạo dòng mặc định
  (100 điểm/lượt, 30% trượt), không cần seed SQL, không lỗi 500.
- Admin nhập `diemMoiLuot <= 0` hoặc `tyLeTruot` ngoài 0–100 → chặn ngay ở `@Valid` trên
  `CauHinhVongQuayRequest`, trả 400 kèm message field cụ thể (`handlerValidateErrors`).
- Mất mạng/đóng tab giữa lúc quay → điểm đã trừ + phiếu (nếu trúng) đã lưu ở server TRƯỚC khi
  trả response, nên phiếu vẫn xuất hiện trong danh sách voucher (tab Cài đặt) dù client bỏ lỡ
  animation/modal kết quả.

## Kiểm thử

- Unit test `VongQuayService.quay()`:
  - Đủ điểm + có khuyến mãi active + roll trúng → điểm bị trừ đúng, 1 `PhieuGiamGiaCaNhan`
    được tạo với `doiThuong = null`, `loai/giaTri` khớp khuyến mãi trúng, 1 `LichSuQuay` ghi
    đúng `ket_qua='trung'`.
  - Không đủ điểm → ném `IllegalArgumentException`, KHÔNG trừ điểm, KHÔNG tạo phiếu (mock
    repository, assert `save()` không được gọi).
  - Danh sách khuyến mãi active rỗng → luôn ra `ket_qua='truot'` bất kể roll ngẫu nhiên.
  - Tài khoản không có `KhachHang` liên kết gọi `quay()` → ném `AccessDeniedException`.
- Verify sống qua trình duyệt thật (đăng nhập `khachhang`/`123456`):
  1. Vào tab "🎡 Vòng quay may mắn", xác nhận số ô = số khuyến mãi active hiện có + 1 ô
     "Chúc may mắn lần sau".
  2. Quay khi đủ điểm nhiều lần liên tiếp — xác nhận điểm giảm đúng mỗi lần, phiếu trúng (nếu
     có) xuất hiện ngay trong danh sách voucher.
  3. Dùng thử 1 phiếu vừa trúng ở checkout — xác nhận áp dụng giảm giá đúng, không cần sửa gì
     thêm ở `CheckoutModal.vue`/`DonHangService`.
  4. Đăng nhập `admin`, đổi `diemMoiLuot`/`tyLeTruot` ở section Khuyến mãi, quay lại tab khách
     hàng xác nhận áp dụng ngay (không cần đăng xuất/đăng nhập lại).
  5. Hạ điểm khách hàng xuống dưới `diemMoiLuot` (qua sửa tay ở admin) → xác nhận nút "Quay
     ngay" tự động disable.

## Ngoài phạm vi

- Không thêm màn hình quản lý riêng cho "danh sách khuyến mãi tham gia vòng quay" — mọi
  khuyến mãi `active` + còn hiệu lực ngày đều tự động là 1 ô, admin chỉ cần bật/tắt khuyến mãi
  như bình thường.
- Không thêm giới hạn số lượt quay/ngày — chỉ giới hạn tự nhiên qua số điểm khách có.
  Nếu sau này cần giới hạn thời gian, có thể thêm dựa trên `lich_su_quay.ngay_quay` mà không
  đổi schema.
- Không hiển thị UI "lịch sử quay" ở bước này dù đã có sẵn API/bảng `lich_su_quay` — chỉ ghi
  lại để có audit trail, phòng khi cần đối soát điểm hoặc bổ sung UI sau mà không phải đổi
  schema. Nếu muốn hiện ngay, chỉ cần thêm 1 danh sách nhỏ gọi `getLichSuCuaToi()` trong
  `LuckyWheelPanel.vue` — không nằm trong phạm vi lần này trừ khi được yêu cầu thêm.
