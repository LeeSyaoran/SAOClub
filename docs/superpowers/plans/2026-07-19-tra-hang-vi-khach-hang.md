# Trả hàng & Ví khách hàng Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a "Trả hàng" (return/RMA) UI shared across StaffPage/WarehouseManagementPage/AdminPage on top of the already-existing `PhieuTraHangController`/`ChiTietTraHangController` backend, and add a customer wallet (`so_du_vi`) that is auto-credited when a return's refund method is "ví" (wallet).

**Architecture:** Frontend: 1 shared `ReturnsPanel.vue` component (props `readonly`, `canPickStaff`) + 2 new frontend services + 1 new store, mounted into 3 pages. Backend: 2 new columns (`phieu_tra_hang.hinh_thuc_hoan`, `khach_hang.so_du_vi`), wallet-credit logic added to the existing `PhieuTraHangService`, and a `@PreAuthorize` lock added to the 2 controllers (currently open to any authenticated user — confirmed unused by any customer-facing flow).

**Tech Stack:** Spring Boot 4 / Java 17 / JPA (Hibernate) / SQL Server backend; Vue 3 `<script setup>` / Vite frontend. Backend tests: JUnit 5 + Mockito + AssertJ (`BackEnd/mvnw.cmd test`). No frontend test framework exists in this repo — frontend tasks are verified manually via `npm run dev`.

## Global Constraints

- DB changes go at the end of `Database/QLBanMayTinh.sql`, each wrapped in `IF NOT EXISTS (...) BEGIN ... END` — the user always re-runs the entire file, so every statement must be idempotent.
- `phieu_tra_hang.trang_thai` uses the enum already defined by its CHECK constraint: `cho_xu_ly` / `da_xu_ly` / `tu_choi` — do not invent new values or reuse the phiếu nhập kho enum (`cho_duyet`/`hoan_thanh`/`huy`), they are different tables.
- Frontend services import `get/post/put/del` from `Service/api.js` and follow the exact `NhaCungCapService.js` shape: `getAll`, `getById`, `save(id, body)`, `remove(id)`.
- Vue components that need shared data import the relevant store directly (`OrdersStore`, `ProductsStore`, `StaffStore`, `CustomersStore`, `AuthStore`) — pages never pass this data down via props.
- i18n: every new user-facing string needs a key in all 5 locale files (`vi.js`, `en.js`, `zh.js`, `ko.js`, `ja.js`) under the top-level `admin:` (or `account:` for the wallet badge) namespace.
- Money fields sent to the backend as `LocalDateTime` must go through `nowLocalIso()` (`utils/datetime.js`), never `new Date().toISOString()` — the latter shifts near-midnight Vietnam-time values to the wrong calendar day.

---

## Task 1: DB schema — `so_du_vi` + `hinh_thuc_hoan`

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (append after the `phieu_tra_hang` table definition, inside section "9. THANH TOÁN & TRẢ HÀNG & BẢO HÀNH", right after its closing `END` around line 504)

**Interfaces:**
- Produces: column `khach_hang.so_du_vi DECIMAL(18,0) NOT NULL DEFAULT 0`, column `phieu_tra_hang.hinh_thuc_hoan NVARCHAR(20) NOT NULL DEFAULT N'vi'` — consumed by Task 2 (entities) and Task 3 (wallet-credit logic).

- [ ] **Step 1: Append the two idempotent `ALTER TABLE` blocks**

Insert immediately after the `phieu_tra_hang` table's closing `END` (the block ending at line 504 in the current file, right before the next `IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chi_tiet_tra_hang')` section):

```sql
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('khach_hang') AND name = 'so_du_vi')
BEGIN
    ALTER TABLE khach_hang ADD so_du_vi DECIMAL(18,0) NOT NULL DEFAULT 0
        CONSTRAINT CK_kh_sodu_vi CHECK (so_du_vi >= 0);
END

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('phieu_tra_hang') AND name = 'hinh_thuc_hoan')
BEGIN
    ALTER TABLE phieu_tra_hang ADD hinh_thuc_hoan NVARCHAR(20) NOT NULL DEFAULT N'vi'
        CONSTRAINT CK_pth_hinhthuchoan CHECK (hinh_thuc_hoan IN (N'tien_mat', N'vi'));
END
```

- [ ] **Step 2: Verify constraint names don't already exist elsewhere in the file**

Run (from repo root):
```bash
grep -n "CK_kh_sodu_vi\|CK_pth_hinhthuchoan" "Database/QLBanMayTinh.sql"
```
Expected: exactly the 2 occurrences you just added (no pre-existing duplicates — SQL Server errors on duplicate constraint names).

- [ ] **Step 3: Commit**

```bash
git add "Database/QLBanMayTinh.sql"
git commit -m "feat(db): add khach_hang.so_du_vi and phieu_tra_hang.hinh_thuc_hoan columns"
```

---

## Task 2: Backend entities, DTOs, repositories

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/entity/KhachHang.java`
- Modify: `BackEnd/src/main/java/com/example/backend/entity/PhieuTraHang.java`
- Modify: `BackEnd/src/main/java/com/example/backend/response/KhachHangResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/response/PhieuTraHangResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/request/PhieuTraHangRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/KhachHangRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/PhieuTraHangRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/KhachHangService.java`

**Interfaces:**
- Produces: `KhachHang.getSoDuVi()/setSoDuVi(BigDecimal)`, `PhieuTraHang.getHinhThucHoan()/setHinhThucHoan(String)`, `PhieuTraHangRequest.getHinhThucHoan()`, `PhieuTraHangResponse.getHinhThucHoan()`, `KhachHangResponse.getSoDuVi()` — consumed by Task 3 (wallet credit logic) and by the frontend (Task 5/7/11) via JSON field `hinhThucHoan`/`soDuVi`.

- [ ] **Step 1: Add `soDuVi` to the `KhachHang` entity**

In `BackEnd/src/main/java/com/example/backend/entity/KhachHang.java`, add the import and field:

```java
import java.math.BigDecimal;
```

```java
    @Column(name = "diem_tich_luy", nullable = false)
    private Integer diemTichLuy;

    @Column(name = "so_du_vi", precision = 18, scale = 0)
    private BigDecimal soDuVi;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;
```

- [ ] **Step 2: Add `hinhThucHoan` to the `PhieuTraHang` entity**

In `BackEnd/src/main/java/com/example/backend/entity/PhieuTraHang.java`:

```java
    @Column(name = "so_tien_hoan", precision = 18, scale = 2)
    private BigDecimal soTienHoan;

    @Column(name = "hinh_thuc_hoan", length = 20)
    private String hinhThucHoan;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;
```

- [ ] **Step 3: Add `hinhThucHoan` to `PhieuTraHangRequest`/`PhieuTraHangResponse`**

`BackEnd/src/main/java/com/example/backend/request/PhieuTraHangRequest.java`:

```java
    @NotNull(message = "Số tiền hoàn không được để trống")
    @PositiveOrZero(message = "Số tiền hoàn phải lớn hơn hoặc bằng 0")
    private BigDecimal soTienHoan;

    @NotBlank(message = "Hình thức hoàn không được để trống")
    @Pattern(regexp = "tien_mat|vi", message = "Hình thức hoàn phải là 'tien_mat' hoặc 'vi'")
    private String hinhThucHoan;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
```

Add `import jakarta.validation.constraints.Pattern;` if not already covered by the existing `jakarta.validation.constraints.*` wildcard import (it is — no change needed there).

`BackEnd/src/main/java/com/example/backend/response/PhieuTraHangResponse.java`:

```java
    private BigDecimal soTienHoan;
    private String hinhThucHoan;
    private String ghiChu;
```

- [ ] **Step 4: Update `PhieuTraHangRepository`'s JPQL projection**

`BackEnd/src/main/java/com/example/backend/repository/PhieuTraHangRepository.java`:

```java
    @Query("SELECT new com.example.backend.response.PhieuTraHangResponse(p.phieuTraId, p.donHang.id, p.nhanVien.nhanVienId, p.lyDo, p.ngayTra, p.trangThai, p.soTienHoan, p.hinhThucHoan, p.ghiChu) FROM PhieuTraHang p")
    List<PhieuTraHangResponse> hienThiPhieuTraHang();
```

- [ ] **Step 5: Add `soDuVi` to `KhachHangResponse` + `KhachHangRepository`'s JPQL**

`BackEnd/src/main/java/com/example/backend/response/KhachHangResponse.java`:

```java
    private Integer diemTichLuy;
    private java.math.BigDecimal soDuVi;
    private String trangThai;
```

`BackEnd/src/main/java/com/example/backend/repository/KhachHangRepository.java`:

```java
	@Query("SELECT new com.example.backend.response.KhachHangResponse(k.khachHangId, k.hoTen, k.soDienThoai, k.email, k.diaChi, k.loaiKhach, k.tenCongTy, k.maSoThue, k.diemTichLuy, k.soDuVi, k.trangThai, k.ngayTao) FROM KhachHang k")
	java.util.List<KhachHangResponse> hienThiKhachHang();
```

- [ ] **Step 6: Fix `KhachHangService` to never insert a NULL wallet balance**

`so_du_vi` is `NOT NULL` at the DB level but is **not** a field on `KhachHangRequest` (customers/staff never set it directly — only the wallet-credit logic in Task 3 changes it). Since Hibernate sends every mapped column explicitly on INSERT (it does not rely on the DB `DEFAULT`), every code path that does `new KhachHang()` must set `soDuVi` explicitly or the INSERT violates the `CK_kh_sodu_vi`/`NOT NULL` constraint.

In `BackEnd/src/main/java/com/example/backend/service/KhachHangService.java`:

```java
    public KhachHang create(KhachHangRequest request) {
        KhachHang entity = new KhachHang();
        BeanUtils.copyProperties(request, entity);
        entity.setSoDuVi(java.math.BigDecimal.ZERO);
        entity.setNgayTao(LocalDateTime.now());
        return khachHangRepository.save(entity);
    }
```

And in `register()`:

```java
        entity.setDiemTichLuy(0);
        entity.setTrangThai("active");
        entity.setSoDuVi(java.math.BigDecimal.ZERO);
        entity.setLoaiKhach("ca_nhan");
        entity.setNgayTao(LocalDateTime.now());
```

(`createGuest()` on the controller calls `khachHangService.create(...)`, so it's covered by the `create()` change above — no separate edit needed.)

- [ ] **Step 7: Compile-check**

Run:
```bash
cd "BackEnd" && ./mvnw.cmd compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/KhachHang.java \
        BackEnd/src/main/java/com/example/backend/entity/PhieuTraHang.java \
        BackEnd/src/main/java/com/example/backend/response/KhachHangResponse.java \
        BackEnd/src/main/java/com/example/backend/response/PhieuTraHangResponse.java \
        BackEnd/src/main/java/com/example/backend/request/PhieuTraHangRequest.java \
        BackEnd/src/main/java/com/example/backend/repository/KhachHangRepository.java \
        BackEnd/src/main/java/com/example/backend/repository/PhieuTraHangRepository.java \
        BackEnd/src/main/java/com/example/backend/service/KhachHangService.java
git commit -m "feat: wire hinhThucHoan and soDuVi through entities, DTOs, and repositories"
```

---

## Task 3: Wallet-credit logic in `PhieuTraHangService`

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/service/PhieuTraHangService.java`
- Create: `BackEnd/src/test/java/com/example/backend/service/PhieuTraHangServiceTest.java`

**Interfaces:**
- Consumes: `KhachHang.getSoDuVi()/setSoDuVi()` (Task 2), `DonHang.getKhachHang()` (existing), `KhachHangRepository.save()` (existing).
- Produces: `PhieuTraHangService.create(PhieuTraHangRequest)` / `.update(Integer, PhieuTraHangRequest)` now credit the customer's wallet exactly once when a phiếu transitions into `da_xu_ly` with `hinhThucHoan == "vi"`.

- [ ] **Step 1: Write the failing test**

Create `BackEnd/src/test/java/com/example/backend/service/PhieuTraHangServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.PhieuTraHangRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhieuTraHangServiceTest {

    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private NhanVienRepository nhanVienRepository;
    @Mock private KhachHangRepository khachHangRepository;

    @InjectMocks
    private PhieuTraHangService service;

    private PhieuTraHangRequest requestDaXuLyQuaVi(Integer donHangId, BigDecimal soTien) {
        PhieuTraHangRequest r = new PhieuTraHangRequest();
        r.setDonHangId(donHangId);
        r.setNhanVienId(null);
        r.setLyDo("Hàng lỗi");
        r.setNgayTra(LocalDateTime.now());
        r.setTrangThai("da_xu_ly");
        r.setSoTienHoan(soTien);
        r.setHinhThucHoan("vi");
        r.setGhiChu("—");
        return r;
    }

    @Test
    void update_chuyenDaXuLy_hinhThucVi_congTienVaoVi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.getReferenceById(9)).thenReturn(donHang);
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        verify(khachHangRepository).save(kh);
    }

    @Test
    void update_chuyenDaXuLy_hinhThucTienMat_khongDungToiVi() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("cho_xu_ly");

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.getReferenceById(9)).thenReturn(donHang);
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        PhieuTraHangRequest req = requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000));
        req.setHinhThucHoan("tien_mat");

        service.update(5, req);

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }

    @Test
    void update_daXuLyRoiSuaLaiVanDaXuLy_khongCongViLanNua() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        kh.setSoDuVi(BigDecimal.valueOf(100_000));

        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setPhieuTraId(5);
        phieu.setDonHang(donHang);
        phieu.setTrangThai("da_xu_ly"); // đã xử lý từ trước

        when(phieuTraHangRepository.findById(5)).thenReturn(Optional.of(phieu));
        when(donHangRepository.getReferenceById(9)).thenReturn(donHang);
        when(phieuTraHangRepository.save(phieu)).thenReturn(phieu);

        service.update(5, requestDaXuLyQuaVi(9, BigDecimal.valueOf(50_000)));

        assertThat(kh.getSoDuVi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        verify(khachHangRepository, never()).save(any());
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:
```bash
cd "BackEnd" && ./mvnw.cmd test -Dtest=PhieuTraHangServiceTest
```
Expected: compile error or `khachHangRepository` field not found — `PhieuTraHangService` doesn't autowire `KhachHangRepository` yet.

- [ ] **Step 3: Implement the wallet-credit logic**

Replace the full contents of `BackEnd/src/main/java/com/example/backend/service/PhieuTraHangService.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuTraHang;
import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.PhieuTraHangRepository;
import com.example.backend.request.PhieuTraHangRequest;
import com.example.backend.response.PhieuTraHangResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PhieuTraHangService {

    @Autowired
    private PhieuTraHangRepository phieuTraHangRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<PhieuTraHangResponse> hienThiPhieuTraHang() {
        return phieuTraHangRepository.hienThiPhieuTraHang();
    }

    public PhieuTraHang getById(Integer id) {
        return phieuTraHangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + id));
    }

    @Transactional
    public PhieuTraHang create(PhieuTraHangRequest request) {
        PhieuTraHang entity = new PhieuTraHang();
        // BeanUtils copies: lyDo, ngayTra, trangThai, soTienHoan, hinhThucHoan, ghiChu
        BeanUtils.copyProperties(request, entity, "donHangId", "nhanVienId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        if (request.getNhanVienId() != null)
            entity.setNhanVien(nhanVienRepository.getReferenceById(request.getNhanVienId()));
        PhieuTraHang saved = phieuTraHangRepository.save(entity);
        congViNeuVuaHoanTat(null, saved);
        return saved;
    }

    @Transactional
    public PhieuTraHang update(Integer id, PhieuTraHangRequest request) {
        PhieuTraHang entity = getById(id);
        String trangThaiCu = entity.getTrangThai();
        BeanUtils.copyProperties(request, entity, "phieuTraId", "donHangId", "nhanVienId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setNhanVien(request.getNhanVienId() != null
                ? nhanVienRepository.getReferenceById(request.getNhanVienId()) : null);
        PhieuTraHang saved = phieuTraHangRepository.save(entity);
        congViNeuVuaHoanTat(trangThaiCu, saved);
        return saved;
    }

    // Cộng tiền vào ví khách hàng khi phiếu VỪA chuyển sang "da_xu_ly" (trạng thái cũ khác
    // "da_xu_ly" — tránh cộng 2 lần nếu sửa 1 phiếu đã xử lý) và hình thức hoàn là "vi".
    // Hoàn "tien_mat" không đụng ví — nhân viên tự đưa tiền mặt ngoài hệ thống.
    private void congViNeuVuaHoanTat(String trangThaiCu, PhieuTraHang phieu) {
        boolean vuaChuyenSangDaXuLy = "da_xu_ly".equals(phieu.getTrangThai()) && !"da_xu_ly".equals(trangThaiCu);
        if (!vuaChuyenSangDaXuLy) return;
        if (!"vi".equals(phieu.getHinhThucHoan())) return;
        if (phieu.getSoTienHoan() == null || phieu.getSoTienHoan().signum() <= 0) return;

        KhachHang khachHang = phieu.getDonHang().getKhachHang();
        khachHang.setSoDuVi(khachHang.getSoDuVi().add(phieu.getSoTienHoan()));
        khachHangRepository.save(khachHang);
    }

    public void delete(Integer id) {
        if (!phieuTraHangRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu trả hàng không tồn tại với id: " + id);
        phieuTraHangRepository.deleteById(id);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:
```bash
cd "BackEnd" && ./mvnw.cmd test -Dtest=PhieuTraHangServiceTest
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/PhieuTraHangService.java \
        BackEnd/src/test/java/com/example/backend/service/PhieuTraHangServiceTest.java
git commit -m "feat: credit customer wallet when a return is processed via 'vi'"
```

---

## Task 4: Lock `@PreAuthorize` on the 2 return controllers

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/PhieuTraHangController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ChiTietTraHangController.java`
- Create: `BackEnd/src/test/java/com/example/backend/controller/PhieuTraHangAuthorizationTest.java`

**Interfaces:**
- Produces: both controllers now require `ADMIN`, `NHAN_VIEN`, or `QUAN_KHO` role (class-level `@PreAuthorize`) — matches the pattern already applied to `NhaCungCapController`/`TonKhoController`.

- [ ] **Step 1: Write the failing test**

Create `BackEnd/src/test/java/com/example/backend/controller/PhieuTraHangAuthorizationTest.java`:

```java
package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// Xác nhận PhieuTraHangController/ChiTietTraHangController bị khoá quyền — trước khi có
// UI (Task 5-10) gọi tới, 2 controller này KHÔNG có bất kỳ @PreAuthorize nào (mở cho mọi
// role đã đăng nhập, kể cả khách hàng). Đã xác nhận (grep) chưa có luồng khách hàng nào
// gọi 2 endpoint này nên khoá an toàn tuyệt đối, đúng tiền lệ NhaCungCapController/TonKhoController.
class PhieuTraHangAuthorizationTest {

    @Test
    void phieuTraHangController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = PhieuTraHangController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietTraHangController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietTraHangController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:
```bash
cd "BackEnd" && ./mvnw.cmd test -Dtest=PhieuTraHangAuthorizationTest
```
Expected: both tests FAIL with `pa` being `null`.

- [ ] **Step 3: Add the class-level annotation to both controllers**

`BackEnd/src/main/java/com/example/backend/controller/PhieuTraHangController.java`:

```java
import org.springframework.security.access.prepost.PreAuthorize;
```

```java
@RestController
@RequestMapping("/api/phieu-tra-hang")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class PhieuTraHangController {
```

`BackEnd/src/main/java/com/example/backend/controller/ChiTietTraHangController.java`:

```java
import org.springframework.security.access.prepost.PreAuthorize;
```

```java
@RestController
@RequestMapping("/api/chi-tiet-tra-hang")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietTraHangController {
```

- [ ] **Step 4: Run to verify it passes**

Run:
```bash
cd "BackEnd" && ./mvnw.cmd test -Dtest=PhieuTraHangAuthorizationTest
```
Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 5: Run the full backend test suite**

Run:
```bash
cd "BackEnd" && ./mvnw.cmd test
```
Expected: `BUILD SUCCESS`, no regressions.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/PhieuTraHangController.java \
        BackEnd/src/main/java/com/example/backend/controller/ChiTietTraHangController.java \
        BackEnd/src/test/java/com/example/backend/controller/PhieuTraHangAuthorizationTest.java
git commit -m "feat(security): lock phieu-tra-hang/chi-tiet-tra-hang to staff roles"
```

---

## Task 5: Frontend services + store

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/PhieuTraHangService.js`
- Create: `FrontEnd/QLBanMayTinh/src/Service/ChiTietTraHangService.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/returns.js`

**Interfaces:**
- Produces: `PhieuTraHangService.{getAll,getById,save,remove}`, `ChiTietTraHangService.{getAll,create,update,remove}`, `ReturnsStore` (`{items, loading, loaded}`), `ensureReturns()`, `refreshReturns()` — consumed by Task 7 (`ReturnsPanel.vue`).

- [ ] **Step 1: Create `PhieuTraHangService.js`**

```javascript
import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/phieu-tra-hang');

export const getById = (id) => get(`/api/phieu-tra-hang/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-tra-hang/update/${id}`, body) : post('/api/phieu-tra-hang', body);

export const remove = (id) => del(`/api/phieu-tra-hang/delete/${id}`);
```

- [ ] **Step 2: Create `ChiTietTraHangService.js`**

```javascript
import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/chi-tiet-tra-hang');

export const create = (body) => post('/api/chi-tiet-tra-hang', body);

export const update = (id, body) => put(`/api/chi-tiet-tra-hang/update/${id}`, body);

export const remove = (id) => del(`/api/chi-tiet-tra-hang/delete/${id}`);
```

- [ ] **Step 3: Create `stores/returns.js`**

```javascript
import { reactive } from "vue";
import * as PhieuTraHangService from "../Service/PhieuTraHangService.js";

export const ReturnsStore = reactive({ items: [], loading: false, loaded: false });

let returnsPromise = null;
export const ensureReturns = () => {
  if (returnsPromise) return returnsPromise;
  returnsPromise = refreshReturns();
  return returnsPromise;
};

export const refreshReturns = async () => {
  ReturnsStore.loading = true;
  try {
    ReturnsStore.items = await PhieuTraHangService.getAll().catch(() => []);
    ReturnsStore.loaded = true;
  } finally {
    ReturnsStore.loading = false;
  }
  return ReturnsStore.items;
};
```

- [ ] **Step 4: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Open the dev server URL, open the browser devtools console, and paste:
```javascript
import('/src/Service/PhieuTraHangService.js').then(m => m.getAll().then(console.log))
```
Expected: no import error, logs an array (empty or with existing rows) — confirms the service module loads and the endpoint responds now that Task 4's `@PreAuthorize` is in place (must be logged in as `admin`/`nhan_vien`/`quan_kho` in that browser tab for the call to succeed, otherwise expect a 403).

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/PhieuTraHangService.js \
        FrontEnd/QLBanMayTinh/src/Service/ChiTietTraHangService.js \
        FrontEnd/QLBanMayTinh/src/stores/returns.js
git commit -m "feat: add PhieuTraHangService/ChiTietTraHangService frontend services + store"
```

---

## Task 6: i18n keys (5 locales)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: `admin.sidebar.traHang`, `admin.pageMeta.traHang.{title,sub}`, `admin.returns.*`, `admin.returnStatus.*`, `admin.hinhThucHoan.*`, `admin.returnModal.*`, `admin.confirm.deleteReturn`, `account.walletBalance` — consumed by Task 7 (`ReturnsPanel.vue`), Task 8-10 (page wiring), Task 11 (`AccountPage.vue`).

Each locale file has the identical structure (`sidebar`/`pageMeta`/`suppliers`/`supplierModal`/`inventory`/`inventoryHistory` all appear in that order at nearly the same line numbers). Apply the same 4 edits to every file below, translating the string values.

- [ ] **Step 1: `vi.js` — sidebar + pageMeta**

In `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`, edit the `sidebar` block (around line 416-418):

```javascript
      suppliers: "Nhà cung cấp",
      inventoryHistory: "Lịch sử tồn kho",
      traHang: "Trả hàng",
    },
```

And the `pageMeta` block (around line 436-438):

```javascript
      suppliers:        { title: "Nhà cung cấp",   sub: "Quản lý nhà cung cấp" },
      inventoryHistory:  { title: "Lịch sử tồn kho", sub: "Lịch sử biến động tồn kho" },
      traHang:           { title: "Trả hàng", sub: "Quản lý phiếu trả hàng" },
    };
```
(Keep the exact closing token that was already there — `};` or `},` — only add the `traHang:` line above it.)

- [ ] **Step 2: `vi.js` — new `returns`/`returnStatus`/`hinhThucHoan`/`returnModal` sections**

Insert right after the `supplierModal: { ... },` block closes (around line 762, right before `inventory: {`):

```javascript
    returns: {
      countSuffix: "phiếu trả hàng",
      add: "+ Tạo phiếu trả hàng",
      loading: "Đang tải...",
      colId: "Mã phiếu",
      colOrder: "Đơn hàng",
      colCustomer: "Khách hàng",
      colAmount: "Số tiền hoàn",
      colHinhThucHoan: "Hình thức hoàn",
      colStatus: "Trạng thái",
      colAction: "Thao tác",
      edit: "Sửa",
      view: "Xem",
      delete: "Xóa",
      empty: "Chưa có phiếu trả hàng",
      searchPlaceholder: "Tìm mã phiếu, khách hàng...",
    },

    returnStatus: {
      cho_xu_ly: "Chờ xử lý",
      da_xu_ly: "Đã xử lý",
      tu_choi: "Từ chối",
    },

    hinhThucHoan: {
      tien_mat: "Tiền mặt",
      vi: "Ví điện tử",
    },

    returnModal: {
      titleAdd: "Tạo phiếu trả hàng",
      titleEdit: "Chi tiết phiếu trả hàng",
      orderLabel: "Đơn hàng *",
      orderSearchPlaceholder: "Tìm theo mã đơn, tên khách, SĐT...",
      orderSearchEmpty: "Không tìm thấy đơn hàng",
      changeOrder: "Đổi đơn hàng",
      staffLabel: "Nhân viên xử lý",
      reasonLabel: "Lý do trả hàng *",
      dateLabel: "Ngày trả",
      statusLabel: "Trạng thái",
      amountLabel: "Số tiền hoàn",
      hinhThucHoanLabel: "Hình thức hoàn",
      customerPresentLabel: "Khách có mặt tại cửa hàng",
      noteLabel: "Ghi chú",
      lineItemsTitle: "Sản phẩm trả",
      colProduct: "Sản phẩm",
      colSku: "SKU",
      colBought: "SL đã mua",
      colReturnQty: "SL trả",
      colCondition: "Tình trạng",
      conditionGood: "Tốt",
      conditionBad: "Lỗi/Hỏng",
      orderRequired: "Vui lòng chọn đơn hàng",
      reasonRequired: "Vui lòng nhập lý do trả hàng",
      lineRequired: "Vui lòng chọn ít nhất 1 sản phẩm cần trả",
      cancel: "Hủy",
      save: "Lưu",
      close: "Đóng",
    },

```

- [ ] **Step 3: `vi.js` — `admin.confirm.deleteReturn` + `account.walletBalance`**

Find the `confirm:` block (contains `deleteSupplier`, per Task's earlier context around the same area as `admin.confirm.deleteSupplier` used in `SupplierManager.vue`) and add:

```javascript
      deleteReturn: "Xóa phiếu trả hàng này?",
```

Find `account.points: "{points} điểm tích lũy",` (line 351) and add right after it:

```javascript
    points: "{points} điểm tích lũy",
    walletBalance: "Số dư ví: {amount}",
```

- [ ] **Step 4: Repeat steps 1-3 for `en.js`**

sidebar/pageMeta (around line 406-408 / 426-428):
```javascript
      suppliers: "Suppliers",
      inventoryHistory: "Inventory history",
      traHang: "Returns",
    },
```
```javascript
      suppliers:        { title: "Suppliers",   sub: "Manage suppliers" },
      inventoryHistory:  { title: "Inventory history", sub: "Stock movement history" },
      traHang:           { title: "Returns", sub: "Manage product returns" },
    };
```

New sections (after `supplierModal` closes, before `inventory: {` around line 749):
```javascript
    returns: {
      countSuffix: "returns",
      add: "+ Create return",
      loading: "Loading...",
      colId: "Return ID",
      colOrder: "Order",
      colCustomer: "Customer",
      colAmount: "Refund amount",
      colHinhThucHoan: "Refund method",
      colStatus: "Status",
      colAction: "Action",
      edit: "Edit",
      view: "View",
      delete: "Delete",
      empty: "No returns yet",
      searchPlaceholder: "Search return ID, customer...",
    },

    returnStatus: {
      cho_xu_ly: "Pending",
      da_xu_ly: "Processed",
      tu_choi: "Rejected",
    },

    hinhThucHoan: {
      tien_mat: "Cash",
      vi: "Wallet",
    },

    returnModal: {
      titleAdd: "Create return",
      titleEdit: "Return details",
      orderLabel: "Order *",
      orderSearchPlaceholder: "Search order ID, customer name, phone...",
      orderSearchEmpty: "No matching order",
      changeOrder: "Change order",
      staffLabel: "Handled by",
      reasonLabel: "Return reason *",
      dateLabel: "Return date",
      statusLabel: "Status",
      amountLabel: "Refund amount",
      hinhThucHoanLabel: "Refund method",
      customerPresentLabel: "Customer is present in store",
      noteLabel: "Note",
      lineItemsTitle: "Returned items",
      colProduct: "Product",
      colSku: "SKU",
      colBought: "Qty bought",
      colReturnQty: "Qty returned",
      colCondition: "Condition",
      conditionGood: "Good",
      conditionBad: "Faulty",
      orderRequired: "Please select an order",
      reasonRequired: "Please enter a return reason",
      lineRequired: "Please select at least 1 item to return",
      cancel: "Cancel",
      save: "Save",
      close: "Close",
    },

```

`confirm.deleteReturn`:
```javascript
      deleteReturn: "Delete this return?",
```

`account.walletBalance` (right after `points: "{points} loyalty points",` or equivalent existing key):
```javascript
    walletBalance: "Wallet balance: {amount}",
```

- [ ] **Step 5: Repeat for `zh.js`**

sidebar/pageMeta:
```javascript
      suppliers: "供应商",
      inventoryHistory: "库存历史",
      traHang: "退货",
    },
```
```javascript
      suppliers:        { title: "供应商",   sub: "管理供应商" },
      inventoryHistory:  { title: "库存历史", sub: "库存变动历史" },
      traHang:           { title: "退货", sub: "管理退货单" },
    };
```

New sections:
```javascript
    returns: {
      countSuffix: "个退货单",
      add: "+ 创建退货单",
      loading: "加载中...",
      colId: "退货单号",
      colOrder: "订单",
      colCustomer: "客户",
      colAmount: "退款金额",
      colHinhThucHoan: "退款方式",
      colStatus: "状态",
      colAction: "操作",
      edit: "编辑",
      view: "查看",
      delete: "删除",
      empty: "暂无退货单",
      searchPlaceholder: "搜索退货单号、客户...",
    },

    returnStatus: {
      cho_xu_ly: "待处理",
      da_xu_ly: "已处理",
      tu_choi: "已拒绝",
    },

    hinhThucHoan: {
      tien_mat: "现金",
      vi: "钱包",
    },

    returnModal: {
      titleAdd: "创建退货单",
      titleEdit: "退货单详情",
      orderLabel: "订单 *",
      orderSearchPlaceholder: "按订单号、客户姓名、电话搜索...",
      orderSearchEmpty: "未找到匹配订单",
      changeOrder: "更换订单",
      staffLabel: "处理员工",
      reasonLabel: "退货原因 *",
      dateLabel: "退货日期",
      statusLabel: "状态",
      amountLabel: "退款金额",
      hinhThucHoanLabel: "退款方式",
      customerPresentLabel: "客户在店",
      noteLabel: "备注",
      lineItemsTitle: "退货商品",
      colProduct: "商品",
      colSku: "SKU",
      colBought: "购买数量",
      colReturnQty: "退货数量",
      colCondition: "状况",
      conditionGood: "良好",
      conditionBad: "故障/损坏",
      orderRequired: "请选择订单",
      reasonRequired: "请输入退货原因",
      lineRequired: "请至少选择1件退货商品",
      cancel: "取消",
      save: "保存",
      close: "关闭",
    },

```

`confirm.deleteReturn`: `"deleteReturn": "确定删除此退货单？",`
`account.walletBalance`: `"walletBalance": "钱包余额：{amount}",`

- [ ] **Step 6: Repeat for `ko.js`**

sidebar/pageMeta:
```javascript
      suppliers: "공급업체",
      inventoryHistory: "재고 이력",
      traHang: "반품",
    },
```
```javascript
      suppliers:        { title: "공급업체",   sub: "공급업체 관리" },
      inventoryHistory:  { title: "재고 이력", sub: "재고 변동 이력" },
      traHang:           { title: "반품", sub: "반품 전표 관리" },
    };
```

New sections:
```javascript
    returns: {
      countSuffix: "건의 반품",
      add: "+ 반품 등록",
      loading: "로딩 중...",
      colId: "반품 번호",
      colOrder: "주문",
      colCustomer: "고객",
      colAmount: "환불 금액",
      colHinhThucHoan: "환불 방식",
      colStatus: "상태",
      colAction: "작업",
      edit: "수정",
      view: "보기",
      delete: "삭제",
      empty: "반품 내역이 없습니다",
      searchPlaceholder: "반품 번호, 고객 검색...",
    },

    returnStatus: {
      cho_xu_ly: "처리 대기",
      da_xu_ly: "처리 완료",
      tu_choi: "거부됨",
    },

    hinhThucHoan: {
      tien_mat: "현금",
      vi: "지갑",
    },

    returnModal: {
      titleAdd: "반품 등록",
      titleEdit: "반품 상세",
      orderLabel: "주문 *",
      orderSearchPlaceholder: "주문 번호, 고객명, 전화번호로 검색...",
      orderSearchEmpty: "일치하는 주문이 없습니다",
      changeOrder: "주문 변경",
      staffLabel: "처리 담당자",
      reasonLabel: "반품 사유 *",
      dateLabel: "반품일",
      statusLabel: "상태",
      amountLabel: "환불 금액",
      hinhThucHoanLabel: "환불 방식",
      customerPresentLabel: "고객이 매장에 있음",
      noteLabel: "메모",
      lineItemsTitle: "반품 상품",
      colProduct: "상품",
      colSku: "SKU",
      colBought: "구매 수량",
      colReturnQty: "반품 수량",
      colCondition: "상태",
      conditionGood: "양호",
      conditionBad: "불량/손상",
      orderRequired: "주문을 선택해 주세요",
      reasonRequired: "반품 사유를 입력해 주세요",
      lineRequired: "반품할 상품을 1개 이상 선택해 주세요",
      cancel: "취소",
      save: "저장",
      close: "닫기",
    },

```

`confirm.deleteReturn`: `"deleteReturn": "이 반품 전표를 삭제할까요?",`
`account.walletBalance`: `"walletBalance": "지갑 잔액: {amount}",`

- [ ] **Step 7: Repeat for `ja.js`**

sidebar/pageMeta:
```javascript
      suppliers: "仕入先",
      inventoryHistory: "在庫履歴",
      traHang: "返品",
    },
```
```javascript
      suppliers:        { title: "仕入先",   sub: "仕入先管理" },
      inventoryHistory:  { title: "在庫履歴", sub: "在庫変動履歴" },
      traHang:           { title: "返品", sub: "返品伝票管理" },
    };
```

New sections:
```javascript
    returns: {
      countSuffix: "件の返品",
      add: "+ 返品を作成",
      loading: "読み込み中...",
      colId: "返品番号",
      colOrder: "注文",
      colCustomer: "顧客",
      colAmount: "返金額",
      colHinhThucHoan: "返金方法",
      colStatus: "状態",
      colAction: "操作",
      edit: "編集",
      view: "表示",
      delete: "削除",
      empty: "返品はまだありません",
      searchPlaceholder: "返品番号、顧客名で検索...",
    },

    returnStatus: {
      cho_xu_ly: "処理待ち",
      da_xu_ly: "処理済み",
      tu_choi: "却下",
    },

    hinhThucHoan: {
      tien_mat: "現金",
      vi: "ウォレット",
    },

    returnModal: {
      titleAdd: "返品を作成",
      titleEdit: "返品の詳細",
      orderLabel: "注文 *",
      orderSearchPlaceholder: "注文番号、顧客名、電話番号で検索...",
      orderSearchEmpty: "一致する注文が見つかりません",
      changeOrder: "注文を変更",
      staffLabel: "担当者",
      reasonLabel: "返品理由 *",
      dateLabel: "返品日",
      statusLabel: "状態",
      amountLabel: "返金額",
      hinhThucHoanLabel: "返金方法",
      customerPresentLabel: "顧客が来店中",
      noteLabel: "備考",
      lineItemsTitle: "返品商品",
      colProduct: "商品",
      colSku: "SKU",
      colBought: "購入数量",
      colReturnQty: "返品数量",
      colCondition: "状態",
      conditionGood: "良好",
      conditionBad: "不良/破損",
      orderRequired: "注文を選択してください",
      reasonRequired: "返品理由を入力してください",
      lineRequired: "返品する商品を1つ以上選択してください",
      cancel: "キャンセル",
      save: "保存",
      close: "閉じる",
    },

```

`confirm.deleteReturn`: `"deleteReturn": "この返品伝票を削除しますか？",`
`account.walletBalance`: `"walletBalance": "ウォレット残高: {amount}",`

- [ ] **Step 8: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Open the dev server in a browser, open devtools console, and paste for each locale file to confirm no syntax errors:
```javascript
import('/src/i18n/locales/vi.js').then(m => console.log(m.default.admin.returnModal.titleAdd))
import('/src/i18n/locales/en.js').then(m => console.log(m.default.admin.returnModal.titleAdd))
import('/src/i18n/locales/zh.js').then(m => console.log(m.default.admin.returnModal.titleAdd))
import('/src/i18n/locales/ko.js').then(m => console.log(m.default.admin.returnModal.titleAdd))
import('/src/i18n/locales/ja.js').then(m => console.log(m.default.admin.returnModal.titleAdd))
```
Expected: each logs the translated string, no import/parse errors. (Adjust the import path/export shape if the actual module doesn't `export default` — check `FrontEnd/QLBanMayTinh/src/i18n/index.js` for how locale files are consumed if this errors, and use the matching named export instead.)

- [ ] **Step 9: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat(i18n): add returns/wallet translation keys for all 5 locales"
```

---

## Task 7: `ReturnsPanel.vue` component

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/ReturnsPanel.vue`

**Interfaces:**
- Consumes: `PhieuTraHangService.{getAll,save,remove}` (Task 5), `ChiTietTraHangService.{getAll,create,update,remove}` (Task 5), `ReturnsStore/ensureReturns/refreshReturns` (Task 5), `ChiTietDonHangService.getByDonHang(donHangId)` (existing — returns `[{id,donHangId,bienTheId,maSku,chiTietId,soSerial,soLuong,donGia,giamGiaDong,thanhTien,ghiChu}]`), `OrdersStore/ensureOrders` (existing), `CustomersStore/ensureCustomers` (existing), `ProductsStore/ensureProducts` (existing), `StaffStore/ensureStaff` (existing), `AuthStore` (existing), `nowLocalIso()` (existing, `utils/datetime.js`), i18n keys from Task 6.
- Produces: `<ReturnsPanel :readonly="Boolean" :can-pick-staff="Boolean" />` — consumed by Task 8/9/10.

- [ ] **Step 1: Create the component**

```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as PhieuTraHangService from "../../Service/PhieuTraHangService.js";
import * as ChiTietTraHangService from "../../Service/ChiTietTraHangService.js";
import * as ChiTietDonHangService from "../../Service/ChiTietDonHangService.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { AuthStore } from "../../stores/index.js";
import { OrdersStore, ensureOrders } from "../../stores/orders.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import { StaffStore, ensureStaff } from "../../stores/staff.js";
import { ReturnsStore, ensureReturns, refreshReturns } from "../../stores/returns.js";

const props = defineProps({
  readonly: { type: Boolean, default: false },
  canPickStaff: { type: Boolean, default: false },
});

onMounted(() => {
  ensureReturns();
  ensureOrders();
  ensureCustomers();
  ensureProducts();
  if (props.canPickStaff) ensureStaff();
});

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) => CustomersStore.items.find(c => c.khachHangId === id)?.hoTen ?? `KH#${id}`;
const productByBienThe = (bienTheId) => ProductsStore.items.find(p => p.bienTheId === bienTheId);
const staffName = (id) => StaffStore.items.find(s => s.nhanVienId === id)?.hoTen ?? '—';
const staffOptions = computed(() => StaffStore.items.map(s => ({ nhanVienId: s.nhanVienId, hoTen: s.hoTen })));
const orderById = (donHangId) => OrdersStore.items.find(o => o.donHangId === donHangId);

const STATUS_COLOR = {
  cho_xu_ly: { bg: '#fde68a', text: '#92400e' },
  da_xu_ly:  { bg: '#bbf7d0', text: '#166534' },
  tu_choi:   { bg: '#fecaca', text: '#991b1b' },
};
const statusColor = (s) => STATUS_COLOR[s] ?? { bg: '#e5e7eb', text: '#374151' };
const statusLabel = (s) => t(`admin.returnStatus.${s}`);
const hinhThucHoanLabel = (h) => t(`admin.hinhThucHoan.${h}`);

// ── Bo loc + danh sach ──────────────────────────────────────────────────────
const search = ref("");
const filteredReturns = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return ReturnsStore.items;
  return ReturnsStore.items.filter((p) => {
    const name = customerName(orderById(p.donHangId)?.khachHangId ?? -1).toLowerCase();
    return String(p.phieuTraId).includes(q) || name.includes(q);
  });
});

// ── Modal tao/sua/xem ─────────────────────────────────────────────────────────
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const orderSearch = ref("");
const selectedOrder = ref(null);
const lineItems = ref([]); // [{ id, bienTheId, chiTietId, maSku, soSerial, donGia, soLuongDaMua, soLuongTra, tinhTrang, checked }]
const orderLinesLoading = ref(false);
const khachCoMat = ref(false); // checkbox gate hình thức hoàn — không lưu DB

const emptyForm = () => ({
  donHangId: null,
  nhanVienId: props.canPickStaff ? '' : (AuthStore.user?.id ?? null),
  lyDo: "",
  ngayTra: nowLocalIso().slice(0, 16),
  trangThai: "cho_xu_ly",
  soTienHoan: 0,
  hinhThucHoan: "vi",
  ghiChu: "",
});
const form = ref(emptyForm());

const searchedOrders = computed(() => {
  const q = orderSearch.value.trim().toLowerCase();
  if (!q) return [];
  return OrdersStore.items.filter((o) =>
    String(o.donHangId).includes(q) ||
    customerName(o.khachHangId).toLowerCase().includes(q) ||
    (o.sdtNguoiNhan ?? '').includes(q)
  ).slice(0, 10);
});

const recalcSoTienHoan = () => {
  form.value.soTienHoan = lineItems.value
    .filter((l) => l.checked)
    .reduce((s, l) => s + (Number(l.donGia) || 0) * (Number(l.soLuongTra) || 0), 0);
};

const loadOrderLines = async (donHangId, existingLines = []) => {
  orderLinesLoading.value = true;
  try {
    const items = await ChiTietDonHangService.getByDonHang(donHangId).catch(() => []);
    lineItems.value = items.map((i) => {
      const existed = existingLines.find((c) => c.bienTheId === i.bienTheId && c.chiTietId === i.chiTietId);
      return {
        id: existed?.id ?? null,
        bienTheId: i.bienTheId,
        chiTietId: i.chiTietId,
        maSku: i.maSku,
        soSerial: i.soSerial,
        donGia: i.donGia,
        soLuongDaMua: i.soLuong,
        soLuongTra: existed?.soLuong ?? i.soLuong,
        tinhTrang: existed?.tinhTrang ?? 'tot',
        checked: !!existed,
      };
    });
  } finally {
    orderLinesLoading.value = false;
  }
};

const pickOrder = async (o) => {
  selectedOrder.value = o;
  form.value.donHangId = o.donHangId;
  orderSearch.value = "";
  await loadOrderLines(o.donHangId);
};

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  selectedOrder.value = null;
  orderSearch.value = "";
  lineItems.value = [];
  khachCoMat.value = false;
  formError.value = "";
  showModal.value = true;
};

const openDetail = async (p) => {
  editingId.value = p.phieuTraId;
  form.value = {
    donHangId: p.donHangId,
    nhanVienId: p.nhanVienId,
    lyDo: p.lyDo,
    ngayTra: p.ngayTra ? p.ngayTra.slice(0, 16) : nowLocalIso().slice(0, 16),
    trangThai: p.trangThai,
    soTienHoan: p.soTienHoan,
    hinhThucHoan: p.hinhThucHoan,
    ghiChu: p.ghiChu ?? '',
  };
  selectedOrder.value = orderById(p.donHangId) ?? null;
  khachCoMat.value = p.hinhThucHoan === 'tien_mat';
  formError.value = "";
  const allLines = await ChiTietTraHangService.getAll().catch(() => []);
  const mine = allLines.filter((c) => c.phieuTraId === p.phieuTraId);
  await loadOrderLines(p.donHangId, mine);
  showModal.value = true;
};

const saveReturn = async () => {
  formError.value = "";
  if (!form.value.donHangId) { formError.value = t('admin.returnModal.orderRequired'); return; }
  if (!form.value.lyDo.trim()) { formError.value = t('admin.returnModal.reasonRequired'); return; }
  const checkedLines = lineItems.value.filter((l) => l.checked);
  if (checkedLines.length === 0) { formError.value = t('admin.returnModal.lineRequired'); return; }

  try {
    const headerBody = {
      donHangId: form.value.donHangId,
      nhanVienId: form.value.nhanVienId ? Number(form.value.nhanVienId) : null,
      lyDo: form.value.lyDo,
      ngayTra: nowLocalIso(new Date(form.value.ngayTra)),
      trangThai: form.value.trangThai,
      soTienHoan: form.value.soTienHoan,
      hinhThucHoan: form.value.hinhThucHoan,
      ghiChu: form.value.ghiChu || '—',
    };
    const res = await PhieuTraHangService.save(editingId.value, headerBody);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }

    let phieuTraId = editingId.value;
    if (!phieuTraId) {
      const created = await res.json();
      phieuTraId = created.phieuTraId;
    }

    const originalIds = checkedLines.filter((l) => l.id).map((l) => l.id);
    const allExisting = editingId.value ? await ChiTietTraHangService.getAll().catch(() => []) : [];
    const mineExisting = allExisting.filter((c) => c.phieuTraId === phieuTraId).map((c) => c.id);
    for (const oldId of mineExisting.filter((id) => !originalIds.includes(id))) {
      await ChiTietTraHangService.remove(oldId);
    }
    for (const l of checkedLines) {
      const body = {
        phieuTraId,
        bienTheId: l.bienTheId,
        chiTietId: l.chiTietId,
        soLuong: l.soLuongTra,
        donGiaHoan: l.donGia,
        tinhTrang: l.tinhTrang,
      };
      if (l.id) await ChiTietTraHangService.update(l.id, body);
      else await ChiTietTraHangService.create(body);
    }

    showModal.value = false;
    await refreshReturns();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteReturn = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteReturn')))) return;
  const res = await PhieuTraHangService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  await refreshReturns();
};
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredReturns.length }}/{{ ReturnsStore.items.length }} {{ t('admin.returns.countSuffix') }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="search" class="form-control form-control-sm" style="width:240px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.returns.searchPlaceholder')" />
      <button v-if="!readonly" class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.returns.add') }}</button>
    </div>
  </div>

  <div v-if="ReturnsStore.loading" class="text-secondary small">{{ t('admin.returns.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.returns.colId') }}</th><th>{{ t('admin.returns.colOrder') }}</th><th>{{ t('admin.returns.colCustomer') }}</th>
        <th>{{ t('admin.returns.colAmount') }}</th><th>{{ t('admin.returns.colHinhThucHoan') }}</th><th>{{ t('admin.returns.colStatus') }}</th><th>{{ t('admin.returns.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(p, idx) in filteredReturns" :key="p.phieuTraId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary" style="font-family:monospace;">#{{ p.phieuTraId }}</td>
          <td class="text-secondary">#{{ p.donHangId }}</td>
          <td>{{ customerName(orderById(p.donHangId)?.khachHangId ?? -1) }}</td>
          <td class="text-warning fw-semibold">{{ formatPrice(p.soTienHoan) }}</td>
          <td class="text-secondary">{{ hinhThucHoanLabel(p.hinhThucHoan) }}</td>
          <td><span class="badge" :style="{ background: statusColor(p.trangThai).bg, color: statusColor(p.trangThai).text }">{{ statusLabel(p.trangThai) }}</span></td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openDetail(p)">{{ readonly ? t('admin.returns.view') : t('admin.returns.edit') }}</button>
              <button v-if="!readonly" class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteReturn(p.phieuTraId)">{{ t('admin.returns.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredReturns.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.returns.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ MODAL PHIEU TRA HANG ══ -->
  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:640px;max-width:96vw;max-height:90vh;overflow-y:auto;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.returnModal.titleEdit') : t('admin.returnModal.titleAdd') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <!-- Chon don hang -->
      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.orderLabel') }}</label>
        <div v-if="selectedOrder" class="d-flex align-items-center justify-content-between p-2 rounded-2" style="background:var(--bg-input);">
          <span>#{{ selectedOrder.donHangId }} — {{ customerName(selectedOrder.khachHangId) }}</span>
          <button v-if="!editingId" class="btn btn-sm btn-outline-secondary" style="font-size:0.72rem;" @click="selectedOrder=null; form.donHangId=null; lineItems=[]">{{ t('admin.returnModal.changeOrder') }}</button>
        </div>
        <template v-else>
          <input v-model="orderSearch" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" :placeholder="t('admin.returnModal.orderSearchPlaceholder')" />
          <div v-if="orderSearch.trim()" class="mt-1 rounded-2 overflow-hidden" style="max-height:160px;overflow-y:auto;border:1px solid var(--border-color-soft);">
            <div v-for="o in searchedOrders" :key="o.donHangId" class="p-2" style="cursor:pointer;" @click="pickOrder(o)">
              #{{ o.donHangId }} — {{ customerName(o.khachHangId) }}
            </div>
            <div v-if="searchedOrders.length===0" class="p-2 text-secondary small">{{ t('admin.returnModal.orderSearchEmpty') }}</div>
          </div>
        </template>
      </div>

      <!-- Danh sach dong san pham -->
      <div v-if="selectedOrder" class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.lineItemsTitle') }}</label>
        <div v-if="orderLinesLoading" class="text-secondary small">{{ t('admin.returns.loading') }}</div>
        <table v-else class="w-100 mb-0" style="font-size:0.8rem;">
          <thead><tr style="background:var(--bg-input);">
            <th class="px-2 py-1" style="width:26px;"></th>
            <th class="px-2 py-1">{{ t('admin.returnModal.colProduct') }}</th>
            <th class="px-2 py-1">{{ t('admin.returnModal.colSku') }}</th>
            <th class="px-2 py-1 text-center">{{ t('admin.returnModal.colBought') }}</th>
            <th class="px-2 py-1 text-center">{{ t('admin.returnModal.colReturnQty') }}</th>
            <th class="px-2 py-1">{{ t('admin.returnModal.colCondition') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="l in lineItems" :key="`${l.bienTheId}-${l.chiTietId}`" style="border-top:1px solid var(--border-color-soft);">
              <td class="px-2 py-1"><input type="checkbox" v-model="l.checked" :disabled="readonly" @change="recalcSoTienHoan" /></td>
              <td class="px-2 py-1">{{ productByBienThe(l.bienTheId)?.tenSanPham || '—' }}</td>
              <td class="px-2 py-1 text-secondary" style="font-family:monospace;">{{ l.maSku }}<span v-if="l.soSerial" class="text-info"> · SN {{ l.soSerial }}</span></td>
              <td class="px-2 py-1 text-center">{{ l.soLuongDaMua }}</td>
              <td class="px-2 py-1 text-center"><input type="number" min="1" :max="l.soLuongDaMua" v-model.number="l.soLuongTra" :disabled="readonly || !l.checked" class="form-control form-control-sm" style="width:64px;background:var(--bg-input);color:var(--text-primary);" @change="recalcSoTienHoan" /></td>
              <td class="px-2 py-1">
                <select v-model="l.tinhTrang" :disabled="readonly || !l.checked" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);">
                  <option value="tot">{{ t('admin.returnModal.conditionGood') }}</option>
                  <option value="loi">{{ t('admin.returnModal.conditionBad') }}</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6" v-if="canPickStaff">
          <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.staffLabel') }}</label>
          <select v-model="form.nhanVienId" :disabled="readonly" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
            <option value="">—</option>
            <option v-for="s in staffOptions" :key="s.nhanVienId" :value="s.nhanVienId">{{ s.hoTen }}</option>
          </select>
        </div>
        <div class="col-6" v-else>
          <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.staffLabel') }}</label>
          <div class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-secondary);border-color:var(--border-color-strong);">{{ staffName(form.nhanVienId) }}</div>
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.dateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayTra" :disabled="readonly" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.reasonLabel') }}</label>
        <input v-model="form.lyDo" :disabled="readonly" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="row g-2 mb-2 align-items-end">
        <div class="col-4">
          <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.amountLabel') }}</label>
          <input type="number" min="0" v-model.number="form.soTienHoan" :disabled="readonly" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-4">
          <div class="form-check mb-1">
            <input type="checkbox" class="form-check-input" id="khachCoMat" v-model="khachCoMat" :disabled="readonly" @change="() => { if (!khachCoMat && form.hinhThucHoan === 'tien_mat') form.hinhThucHoan = 'vi'; }" />
            <label class="form-check-label small text-secondary" for="khachCoMat">{{ t('admin.returnModal.customerPresentLabel') }}</label>
          </div>
        </div>
        <div class="col-4">
          <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.hinhThucHoanLabel') }}</label>
          <select v-model="form.hinhThucHoan" :disabled="readonly" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
            <option value="vi">{{ t('admin.hinhThucHoan.vi') }}</option>
            <option value="tien_mat" :disabled="!khachCoMat">{{ t('admin.hinhThucHoan.tien_mat') }}</option>
          </select>
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.statusLabel') }}</label>
        <select v-model="form.trangThai" :disabled="readonly" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="cho_xu_ly">{{ t('admin.returnStatus.cho_xu_ly') }}</option>
          <option value="da_xu_ly">{{ t('admin.returnStatus.da_xu_ly') }}</option>
          <option value="tu_choi">{{ t('admin.returnStatus.tu_choi') }}</option>
        </select>
      </div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.returnModal.noteLabel') }}</label>
        <input v-model="form.ghiChu" :disabled="readonly" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ readonly ? t('admin.returnModal.close') : t('admin.returnModal.cancel') }}</button>
        <button v-if="!readonly" class="btn btn-sm btn-warning text-dark fw-bold" @click="saveReturn">{{ t('admin.returnModal.save') }}</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
The component isn't mounted anywhere yet — verify Vite compiles it without error by watching the terminal output while the dev server is running (no `[vue/compiler-sfc]` errors printed for `ReturnsPanel.vue`).

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/ReturnsPanel.vue
git commit -m "feat: add ReturnsPanel.vue shared returns management component"
```

---

## Task 8: Wire into `StaffPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue`

**Interfaces:**
- Consumes: `ReturnsPanel.vue` (Task 7) with default props (`readonly=false`, `canPickStaff=false` → nhân viên tự khoá `nhanVienId` = chính mình).

- [ ] **Step 1: Import + register the new tab**

In `FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue`:

```javascript
import ProductsTable from "../components/admin/ProductsTable.vue";
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
```

```javascript
const PAGE_META = {
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
};
```

- [ ] **Step 2: Add the nav item** (right after the "customers" nav item, before "products")

```html
        <div class="adm-nav" :class="{active: currentPage==='tra-hang'}" @click="navigate('tra-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9.707 3.293a1 1 0 010 1.414L7.414 7H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.traHang') }}
        </div>
```

- [ ] **Step 3: Add the section**

```html
        <section v-show="currentPage === 'tra-hang'"><ReturnsPanel /></section>
        <section v-show="currentPage === 'products'"><ProductsTable :readonly="true" /></section>
```

- [ ] **Step 4: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Log in as a `nhan_vien` account, land on `#staff`, click the new "Trả hàng" nav item. Verify: the panel loads, "+ Tạo phiếu trả hàng" is visible, clicking it opens the modal, searching an order shows results, picking an order loads its line items, the nhân viên field shows your own name as static text (not a dropdown).

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/StaffPage.vue
git commit -m "feat: add Trả hàng tab to StaffPage"
```

---

## Task 9: Wire into `WarehouseManagementPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue`

**Interfaces:**
- Consumes: `ReturnsPanel.vue` (Task 7) with `:readonly="true"`.

- [ ] **Step 1: Import + register**

```javascript
import InventoryHistoryPanel from "../components/admin/InventoryHistoryPanel.vue";
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
```

```javascript
const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
};
```

- [ ] **Step 2: Add the nav item** (after "inventoryHistory")

```html
        <div class="adm-nav" :class="{active: currentPage==='traHang'}" @click="navigate('traHang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9.707 3.293a1 1 0 010 1.414L7.414 7H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.traHang') }}
        </div>
```

- [ ] **Step 3: Add the section**

```html
        <section v-show="currentPage === 'traHang'"><ReturnsPanel :readonly="true" /></section>
```

- [ ] **Step 4: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Log in as a `quan_kho` account, land on `#kho`, click "Trả hàng". Verify: list renders, no "+ Tạo phiếu trả hàng" button, "Xem" button (not "Sửa") opens the modal read-only (all inputs disabled, no Save button, "Đóng" button only).

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue
git commit -m "feat: add read-only Trả hàng tab to WarehouseManagementPage"
```

---

## Task 10: Wire into `AdminPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `ReturnsPanel.vue` (Task 7) with `:can-pick-staff="true"`.

- [ ] **Step 1: Import + register**

Near the top with the other component imports in `AdminPage.vue`:

```javascript
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
```

In `PAGE_META` (`AdminPage.vue:47-58`):

```javascript
const PAGE_META = {
  dashboard: { titleKey: "admin.pageMeta.dashboard.title", subKey: "admin.pageMeta.dashboard.sub", icon: "📊" },
  products: { titleKey: "admin.pageMeta.products.title", subKey: "admin.pageMeta.products.sub", icon: "💻" },
  orders: { titleKey: "admin.pageMeta.orders.title", subKey: "admin.pageMeta.orders.sub", icon: "🧾" },
  customers: { titleKey: "admin.pageMeta.customers.title", subKey: "admin.pageMeta.customers.sub", icon: "👥" },
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  "tra-hang": { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  promotions: { titleKey: "admin.pageMeta.promotions.title", subKey: "admin.pageMeta.promotions.sub", icon: "🏷️" },
  staff: { titleKey: "admin.pageMeta.staff.title", subKey: "admin.pageMeta.staff.sub", icon: "🧑‍💼" },
  "ban-hang": { titleKey: "admin.pageMeta.banHang.title", subKey: "admin.pageMeta.banHang.sub", icon: "🛒" },
  reports: { titleKey: "admin.pageMeta.reports.title", subKey: "admin.pageMeta.reports.sub", icon: "📈" },
  settings: { titleKey: "admin.pageMeta.settings.title", subKey: "admin.pageMeta.settings.sub", icon: "⚙️" },
};
```

- [ ] **Step 2: Find the sidebar nav list and add the nav item**

Locate the `<div class="adm-nav" :class="{active: currentPage==='inventory'}" @click="navigate('inventory')">...</div>` block in the template (search for `navigate('inventory')`), and add a new nav item immediately after it:

```html
        <div class="adm-nav" :class="{active: currentPage==='tra-hang'}" @click="navigate('tra-hang')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9.707 3.293a1 1 0 010 1.414L7.414 7H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.traHang') }}
        </div>
```

- [ ] **Step 3: Find the content sections and add the new section**

Locate `<section v-show="currentPage === 'inventory'">...</section>` in the template and add immediately after its closing tag:

```html
        <section v-show="currentPage === 'tra-hang'"><ReturnsPanel :can-pick-staff="true" /></section>
```

- [ ] **Step 4: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Log in as `admin`, click the new "Trả hàng" nav item. Verify: full CRUD works, opening "+ Tạo phiếu trả hàng" shows a "Nhân viên xử lý" dropdown (not static text) populated from the staff list.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat: add Trả hàng page to AdminPage with staff picker"
```

---

## Task 11: Wallet badge in `AccountPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`

**Interfaces:**
- Consumes: `profile.soDuVi` (populated automatically once Task 2 adds the field to the `KhachHang` entity — `AccountPage.vue` already does `profile.value = await KhachHangService.getById(auth.user.id)`, which returns the raw entity), `formatPrice()` (already defined locally in this file at line 127), `account.walletBalance` i18n key (Task 6).

- [ ] **Step 1: Add the badge next to the existing loyalty points badge**

In `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`, right after the "Điểm tích lũy" badge block (around line 225-229):

```html
        <!-- Điểm tích lũy -->
        <div v-if="profile" class="d-flex align-items-center gap-1 px-3 py-1 rounded-pill fw-bold"
             style="background:rgba(244,63,94,0.1); border:1px solid rgba(244,63,94,0.25); color:var(--accent-fg); font-size:12px; white-space:nowrap;">
          🎁 {{ t('account.points', { points: profile.diemTichLuy ?? 0 }) }}
        </div>

        <!-- So du vi -->
        <div v-if="profile" class="d-flex align-items-center gap-1 px-3 py-1 rounded-pill fw-bold"
             style="background:rgba(34,197,94,0.1); border:1px solid rgba(34,197,94,0.25); color:#22c55e; font-size:12px; white-space:nowrap;">
          💰 {{ t('account.walletBalance', { amount: formatPrice(profile.soDuVi ?? 0) }) }}
        </div>
```

- [ ] **Step 2: Manual verification**

Run:
```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Log in as a customer account, open the account page. Verify: the new "💰 Số dư ví: 0 ₫" (or formatted currency) badge shows next to the loyalty points badge.

Then, end-to-end check the whole feature: as `nhan_vien`/`admin`, create a return for one of that customer's delivered orders with `hinhThucHoan = vi` and `trangThai = da_xu_ly` with a non-zero `soTienHoan`, save it, then reload the customer's account page and confirm the wallet balance increased by exactly that amount.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue
git commit -m "feat: show wallet balance badge on AccountPage"
```

---

## Self-Review

**1. Spec coverage:**
- ReturnsPanel shared across StaffPage/WarehouseManagementPage/AdminPage with correct props per role → Tasks 7, 8, 9, 10. ✅
- Correct `trang_thai` enum (`cho_xu_ly`/`da_xu_ly`/`tu_choi`) → used consistently in Tasks 3, 6, 7. ✅
- `hinh_thuc_hoan` column + gate checkbox (UI-only, not persisted) → Task 1 (DB), Task 7 Step 1 (`khachCoMat` ref, never sent to backend). ✅
- Wallet auto-credit on `da_xu_ly` + `vi`, no double-credit on re-save → Task 3, covered by 3 unit tests. ✅
- `@PreAuthorize` lock on both controllers → Task 4. ✅
- Wallet balance visible to customer → Task 11. ✅
- Non-goals (spend from wallet, wallet history page, order status auto-change, phiếu bảo hành) → intentionally no task touches these.

**2. Placeholder scan:** No TBD/"add later"/"similar to Task N" found — every step has literal code or an exact manual-verification procedure.

**3. Type consistency:** `hinhThucHoan` used identically (camelCase, values `"tien_mat"`/`"vi"`) across Task 2 (entity/DTO), Task 3 (service logic + tests), Task 6 (i18n keys `admin.hinhThucHoan.*`), Task 7 (component). `trangThai` values (`cho_xu_ly`/`da_xu_ly`/`tu_choi`) consistent across Task 3 tests, Task 6 i18n, Task 7 template. `ReturnsStore`/`ensureReturns`/`refreshReturns` names match between Task 5 (definition) and Task 7 (usage). `PhieuTraHangService.save(id, body)` / `remove(id)` signatures match between Task 5 and Task 7.
