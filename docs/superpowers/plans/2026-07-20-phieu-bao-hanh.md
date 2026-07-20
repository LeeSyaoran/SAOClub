# Phiếu bảo hành Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix a pre-existing backend bug in `PhieuBaoHanhController`'s DTOs/service, then build a `WarrantyPanel.vue` UI (extending the existing read-only "còn hạn bảo hành" list with real phiếu-bảo-hành CRUD) mounted into `WarehouseManagementPage.vue` and `AdminPage.vue`.

**Architecture:** Backend: fix field-naming/wiring bugs in `PhieuBaoHanhRequest`/`Response`/`Service`/`Repository` (currently unused by any frontend, so zero-risk to change shape), extend `WarrantyStatusResponse` with 3 raw FK ids needed to create a claim from a list row, lock the controller. Frontend: 1 shared component combining the existing under-warranty list (extracted from `AdminPage.vue`) with a new phiếu-bảo-hành CRUD table, mounted with identical (no role-gated) props in both pages — this entity has no nhân viên field, so unlike Trả hàng there's no `readonly`/`canPickStaff` prop split.

**Tech Stack:** Spring Boot 4 / Java 17 / JPA (Hibernate) / SQL Server backend; Vue 3 `<script setup>` / Vite frontend. Backend tests: JUnit 5 + Mockito + AssertJ (`BackEnd/mvnw.cmd test`). No frontend test framework exists in this repo — frontend tasks are verified manually via `npm run dev`.

## Global Constraints

- No DB schema changes in this plan — `phieu_bao_hanh` and its CHECK constraints already exist. Every fix here is Java/Vue only.
- `phieu_bao_hanh.trang_thai` uses the enum already defined by its CHECK constraint: `con_bao_hanh` / `dang_xu_ly` / `da_xu_ly` / `het_bao_hanh` / `tu_choi` — do not invent other values.
- `PhieuBaoHanhRequest`/`Response` currently use `sanPhamId` (actually means `bienTheId` on write, means real `san_pham_id` on read — a genuine bug) and a dead `serialNumber` String field. This plan renames them to `bienTheId` (Integer, required) and `chiTietId` (Integer, optional) — matching the exact convention already used by `ChiTietTraHangRequest`/`Response`. No other frontend/backend code currently consumes these DTOs, so this is a safe, zero-blast-radius rename.
- Frontend services import `get/post/put/del` from `Service/api.js` and follow the exact `PhieuTraHangService.js` shape: `getAll`, `getById`, `save(id, body)`, `remove(id)`.
- Money fields sent to the backend as `LocalDateTime` must go through `nowLocalIso()` (`utils/datetime.js`), never `new Date().toISOString()`.
- i18n: every new user-facing string needs a key in all 5 locale files (`vi.js`, `en.js`, `zh.js`, `ko.js`, `ja.js`) under the `admin:` namespace.

---

## Task 1: Fix `PhieuBaoHanhRequest`/`Response`/`Service`/`Repository`

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/request/PhieuBaoHanhRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/response/PhieuBaoHanhResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/PhieuBaoHanhService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/PhieuBaoHanhRepository.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/PhieuBaoHanhServiceTest.java`

**Interfaces:**
- Produces: `PhieuBaoHanhRequest` fields `donHangId`, `bienTheId` (required), `khachHangId`, `chiTietId` (optional), `ngayMua`, `ngayHetBh` (required), `ngayTiepNhan`, `ngayTraKhach` (optional), `moTaLoi` (required), `ketQuaXuLy` (optional), `trangThai` (required), `chiPhiPhatSinh` (required), `ghiChu` (required). `PhieuBaoHanhResponse` mirrors this plus `baoHanhId`, `maSku`, `soSerial` (both read-only display fields). Consumed by Task 4 (frontend service) and Task 6 (`WarrantyPanel.vue`).

- [ ] **Step 1: Write the failing test**

Create `BackEnd/src/test/java/com/example/backend/service/PhieuBaoHanhServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhieuBaoHanhServiceTest {

    @Mock private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @InjectMocks
    private PhieuBaoHanhService service;

    private PhieuBaoHanhRequest requestCoBan() {
        PhieuBaoHanhRequest r = new PhieuBaoHanhRequest();
        r.setDonHangId(1);
        r.setBienTheId(2);
        r.setKhachHangId(3);
        r.setNgayMua(LocalDateTime.now());
        r.setNgayHetBh(LocalDateTime.now().plusMonths(12));
        r.setMoTaLoi("Máy không lên nguồn");
        r.setTrangThai("con_bao_hanh");
        r.setChiPhiPhatSinh(BigDecimal.ZERO);
        r.setGhiChu("—");
        return r;
    }

    @Test
    void create_coChiTietId_ganChiTietSanPham() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(100);
        ChiTietSanPham serialMock = new ChiTietSanPham();
        serialMock.setChiTietId(100);
        when(chiTietSanPhamRepository.getReferenceById(100)).thenReturn(serialMock);
        when(phieuBaoHanhRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhieuBaoHanh saved = service.create(req);

        assertThat(saved.getChiTietSanPham()).isSameAs(serialMock);
    }

    @Test
    void create_khongCoChiTietId_khongGanChiTietSanPham() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setChiTietId(null);
        when(phieuBaoHanhRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PhieuBaoHanh saved = service.create(req);

        verify(chiTietSanPhamRepository, never()).getReferenceById(any());
        assertThat(saved.getChiTietSanPham()).isNull();
    }

    @Test
    void create_ganDungBienTheTuBienTheId_khongPhaiSanPhamId() {
        PhieuBaoHanhRequest req = requestCoBan();
        req.setBienTheId(42);
        when(phieuBaoHanhRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(req);

        verify(bienTheSanPhamRepository).getReferenceById(42);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test -Dtest=PhieuBaoHanhServiceTest
```
Expected: compile error — `PhieuBaoHanhRequest` has no `setBienTheId`/`setChiTietId` methods yet (still has `setSanPhamId`/`setSerialNumber`), and `PhieuBaoHanhService` has no `ChiTietSanPhamRepository` field.

- [ ] **Step 3: Fix `PhieuBaoHanhRequest.java`**

Replace the full file contents:

```java
package com.example.backend.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PhieuBaoHanhRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotNull(message = "Biến thể sản phẩm không được để trống")
    private Integer bienTheId;

    @NotNull(message = "Khách hàng không được để trống")
    private Integer khachHangId;

    private Integer chiTietId;

    @NotNull(message = "Ngày mua không được để trống")
    private LocalDateTime ngayMua;

    @NotNull(message = "Ngày hết bảo hành không được để trống")
    private LocalDateTime ngayHetBh;

    private LocalDateTime ngayTiepNhan;

    private LocalDateTime ngayTraKhach;

    @NotBlank(message = "Mô tả lỗi không được để trống")
    private String moTaLoi;

    private String ketQuaXuLy;

    @NotBlank(message = "Trạng thái không được để trống")
    private String trangThai;

    @NotNull(message = "Chi phí phát sinh không được để trống")
    @PositiveOrZero(message = "Chi phí phát sinh phải lớn hơn hoặc bằng 0")
    private BigDecimal chiPhiPhatSinh;

    @NotBlank(message = "Ghi chú không được để trống")
    private String ghiChu;
}
```

- [ ] **Step 4: Fix `PhieuBaoHanhResponse.java`**

Replace the full file contents:

```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PhieuBaoHanhResponse {
    private Integer baoHanhId;
    private Integer donHangId;
    private Integer bienTheId;
    private String maSku;
    private Integer khachHangId;
    private Integer chiTietId;
    private String soSerial;
    private LocalDateTime ngayMua;
    private LocalDateTime ngayHetBh;
    private LocalDateTime ngayTiepNhan;
    private LocalDateTime ngayTraKhach;
    private String moTaLoi;
    private String ketQuaXuLy;
    private String trangThai;
    private BigDecimal chiPhiPhatSinh;
    private String ghiChu;
}
```

- [ ] **Step 5: Fix `PhieuBaoHanhRepository.java`**

Replace the full file contents:

```java
package com.example.backend.repository;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.response.PhieuBaoHanhResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhieuBaoHanhRepository extends JpaRepository<PhieuBaoHanh, Integer> {
    @Query("SELECT new com.example.backend.response.PhieuBaoHanhResponse(p.baoHanhId, p.donHang.id, p.bienThe.bienTheId, p.bienThe.maSku, p.khachHang.khachHangId, ctsp.chiTietId, ctsp.soSerial, p.ngayMua, p.ngayHetBh, p.ngayTiepNhan, p.ngayTraKhach, p.moTaLoi, p.ketQuaXuLy, p.trangThai, p.chiPhiPhatSinh, p.ghiChu) FROM PhieuBaoHanh p LEFT JOIN p.chiTietSanPham ctsp")
    List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh();

    List<PhieuBaoHanh> findByDonHang_Id(Integer donHangId);

    boolean existsByBienThe_BienTheId(Integer bienTheId);
}
```

- [ ] **Step 6: Fix `PhieuBaoHanhService.java`**

Replace the full file contents:

```java
package com.example.backend.service;

import com.example.backend.entity.PhieuBaoHanh;
import com.example.backend.repository.*;
import com.example.backend.request.PhieuBaoHanhRequest;
import com.example.backend.response.PhieuBaoHanhResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhieuBaoHanhService {

    @Autowired
    private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public List<PhieuBaoHanhResponse> hienThiPhieuBaoHanh() {
        return phieuBaoHanhRepository.hienThiPhieuBaoHanh();
    }

    public PhieuBaoHanh getById(Integer id) {
        return phieuBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id));
    }

    public PhieuBaoHanh create(PhieuBaoHanhRequest request) {
        PhieuBaoHanh entity = new PhieuBaoHanh();
        // BeanUtils copies: ngayMua, ngayHetBh, ngayTiepNhan, ngayTraKhach,
        //                   moTaLoi, ketQuaXuLy, trangThai, chiPhiPhatSinh, ghiChu
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        return phieuBaoHanhRepository.save(entity);
    }

    public PhieuBaoHanh update(Integer id, PhieuBaoHanhRequest request) {
        PhieuBaoHanh entity = getById(id);
        BeanUtils.copyProperties(request, entity, "baoHanhId", "donHangId", "bienTheId", "khachHangId", "chiTietId");
        entity.setDonHang(donHangRepository.getReferenceById(request.getDonHangId()));
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        entity.setKhachHang(khachHangRepository.getReferenceById(request.getKhachHangId()));
        entity.setChiTietSanPham(request.getChiTietId() != null
                ? chiTietSanPhamRepository.getReferenceById(request.getChiTietId()) : null);
        return phieuBaoHanhRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!phieuBaoHanhRepository.existsById(id))
            throw new IllegalArgumentException("Phiếu bảo hành không tồn tại với id: " + id);
        phieuBaoHanhRepository.deleteById(id);
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test -Dtest=PhieuBaoHanhServiceTest
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 8: Run the full backend suite**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test
```
Expected: `BUILD SUCCESS`, no regressions (no other file references the old `sanPhamId`/`serialNumber` field names — confirmed via the earlier codebase survey that nothing outside these 4 files touches `PhieuBaoHanh*`).

- [ ] **Step 9: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/request/PhieuBaoHanhRequest.java \
        BackEnd/src/main/java/com/example/backend/response/PhieuBaoHanhResponse.java \
        BackEnd/src/main/java/com/example/backend/service/PhieuBaoHanhService.java \
        BackEnd/src/main/java/com/example/backend/repository/PhieuBaoHanhRepository.java \
        BackEnd/src/test/java/com/example/backend/service/PhieuBaoHanhServiceTest.java
git commit -m "fix: correct PhieuBaoHanh bienTheId/chiTietId field wiring (was sanPhamId/serialNumber, dead on write)"
```

---

## Task 2: Add FK ids to `WarrantyStatusResponse`

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/response/WarrantyStatusResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java`

**Interfaces:**
- Produces: `WarrantyStatusResponse` gains `donHangId`, `bienTheId`, `khachHangId` (all `Integer`, appended at the end of the existing constructor) — consumed by Task 6 (`WarrantyPanel.vue`'s "Tạo phiếu bảo hành" button, which needs these raw ids to build a `PhieuBaoHanhRequest`).

- [ ] **Step 1: Add the 3 fields to `WarrantyStatusResponse.java`**

Replace the full file contents:

```java
package com.example.backend.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WarrantyStatusResponse {
    private Integer chiTietId;
    private String soSerial;
    private String maSku;
    private String tenSanPham;
    private Integer baoHanhThang;
    private LocalDateTime ngayGiaoThucTe;
    private String maDonHang;
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private Integer donHangId;
    private Integer bienTheId;
    private Integer khachHangId;
    // Tính ở service (ngayGiaoThucTe + baoHanhThang tháng) — không lấy được thẳng bằng JPQL
    // constructor-expression nên để trống ở đây, set sau khi query xong.
    private LocalDateTime ngayHetBaoHanh;

    public WarrantyStatusResponse(Integer chiTietId, String soSerial, String maSku, String tenSanPham,
                                   Integer baoHanhThang, LocalDateTime ngayGiaoThucTe, String maDonHang,
                                   String tenKhachHang, String soDienThoaiKhachHang,
                                   Integer donHangId, Integer bienTheId, Integer khachHangId) {
        this.chiTietId = chiTietId;
        this.soSerial = soSerial;
        this.maSku = maSku;
        this.tenSanPham = tenSanPham;
        this.baoHanhThang = baoHanhThang;
        this.ngayGiaoThucTe = ngayGiaoThucTe;
        this.maDonHang = maDonHang;
        this.tenKhachHang = tenKhachHang;
        this.soDienThoaiKhachHang = soDienThoaiKhachHang;
        this.donHangId = donHangId;
        this.bienTheId = bienTheId;
        this.khachHangId = khachHangId;
    }
}
```

- [ ] **Step 2: Update the JPQL in `ChiTietSanPhamRepository.java`**

Find the `timSerialDaBanCoGiaoHang` query (around line 33-46) and replace it with:

```java
    @Query("""
    SELECT new com.example.backend.response.WarrantyStatusResponse(
        c.chiTietId, c.soSerial, bt.maSku, sp.tenSanPham, bt.baoHanhThang,
        d.ngayGiaoThucTe, d.maDonHang, kh.hoTen, kh.soDienThoai,
        d.id, bt.bienTheId, kh.khachHangId
    )
    FROM ChiTietSanPham c
    JOIN c.bienThe bt
    JOIN bt.sanPham sp
    JOIN ChiTietDonHang cdh ON cdh.chiTietSanPham = c
    JOIN cdh.donHang d
    JOIN d.khachHang kh
    WHERE c.trangThai = 'da_ban' AND d.ngayGiaoThucTe IS NOT NULL
    """)
    List<WarrantyStatusResponse> timSerialDaBanCoGiaoHang();
```

Only the `SELECT new ...(...)` constructor-argument list changes (3 new args appended at the end); the `FROM`/`JOIN`/`WHERE` clauses are untouched.

- [ ] **Step 3: Compile-check**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run the full backend suite**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test
```
Expected: `BUILD SUCCESS`, no regressions.

- [ ] **Step 5: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/response/WarrantyStatusResponse.java \
        BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java
git commit -m "feat: add donHangId/bienTheId/khachHangId to WarrantyStatusResponse"
```

---

## Task 3: Lock `@PreAuthorize` on `PhieuBaoHanhController`

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/PhieuBaoHanhController.java`
- Test: `BackEnd/src/test/java/com/example/backend/controller/PhieuBaoHanhAuthorizationTest.java`

**Interfaces:**
- Produces: `PhieuBaoHanhController` requires `ADMIN`, `NHAN_VIEN`, or `QUAN_KHO` role — matches the pattern already applied to `PhieuTraHangController`/`NhaCungCapController`.

- [ ] **Step 1: Write the failing test**

Create `BackEnd/src/test/java/com/example/backend/controller/PhieuBaoHanhAuthorizationTest.java`:

```java
package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// PhieuBaoHanhController KHÔNG có bất kỳ @PreAuthorize nào trước task này (mở cho mọi
// role đã đăng nhập, kể cả khách hàng). Đã xác nhận (grep) chưa có luồng khách hàng nào
// gọi tới endpoint này nên khoá an toàn tuyệt đối, đúng tiền lệ PhieuTraHangController.
class PhieuBaoHanhAuthorizationTest {

    @Test
    void phieuBaoHanhController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = PhieuBaoHanhController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
```

- [ ] **Step 2: Run to verify it fails**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test -Dtest=PhieuBaoHanhAuthorizationTest
```
Expected: FAIL — `pa` is `null`.

- [ ] **Step 3: Add the class-level annotation**

In `BackEnd/src/main/java/com/example/backend/controller/PhieuBaoHanhController.java`, add the import:

```java
import org.springframework.security.access.prepost.PreAuthorize;
```

And the annotation:

```java
@RestController
@RequestMapping("/api/phieu-bao-hanh")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class PhieuBaoHanhController {
```

- [ ] **Step 4: Run to verify it passes**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test -Dtest=PhieuBaoHanhAuthorizationTest
```
Expected: `Tests run: 1, Failures: 0, Errors: 0`.

- [ ] **Step 5: Run the full backend suite**

```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); Set-Location "D:\project code\SAOClub\BackEnd"; & ".\mvnw.cmd" test
```
Expected: `BUILD SUCCESS`, no regressions.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/PhieuBaoHanhController.java \
        BackEnd/src/test/java/com/example/backend/controller/PhieuBaoHanhAuthorizationTest.java
git commit -m "feat(security): lock phieu-bao-hanh to staff roles"
```

---

## Task 4: Frontend service + store

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/PhieuBaoHanhService.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/baoHanh.js`

**Interfaces:**
- Produces: `PhieuBaoHanhService.{getAll,getById,save,remove}`, `BaoHanhStore` (`{items, loading, loaded}`), `ensureBaoHanh()`, `refreshBaoHanh()` — consumed by Task 6 (`WarrantyPanel.vue`).

- [ ] **Step 1: Create `PhieuBaoHanhService.js`**

```javascript
import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/phieu-bao-hanh');

export const getById = (id) => get(`/api/phieu-bao-hanh/${id}`);

export const save = (id, body) =>
  id ? put(`/api/phieu-bao-hanh/update/${id}`, body) : post('/api/phieu-bao-hanh', body);

export const remove = (id) => del(`/api/phieu-bao-hanh/delete/${id}`);
```

- [ ] **Step 2: Create `stores/baoHanh.js`**

```javascript
import { reactive } from "vue";
import * as PhieuBaoHanhService from "../Service/PhieuBaoHanhService.js";

export const BaoHanhStore = reactive({ items: [], loading: false, loaded: false });

let baoHanhPromise = null;
export const ensureBaoHanh = () => {
  if (baoHanhPromise) return baoHanhPromise;
  baoHanhPromise = refreshBaoHanh();
  return baoHanhPromise;
};

export const refreshBaoHanh = async () => {
  BaoHanhStore.loading = true;
  try {
    BaoHanhStore.items = await PhieuBaoHanhService.getAll().catch(() => []);
    BaoHanhStore.loaded = true;
  } finally {
    BaoHanhStore.loading = false;
  }
  return BaoHanhStore.items;
};
```

- [ ] **Step 3: Manual verification**

```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Confirm `VITE ... ready in ...ms` with no errors referencing either new file (Bash/Git-Bash's `npm run dev` has an unrelated `node` quoting glitch in this environment — use PowerShell).

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/PhieuBaoHanhService.js \
        FrontEnd/QLBanMayTinh/src/stores/baoHanh.js
git commit -m "feat: add PhieuBaoHanhService frontend service + store"
```

---

## Task 5: i18n keys (5 locales)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: `admin.warranty.createClaim`, `admin.warranty.colAction` (2 new keys added to the EXISTING `admin.warranty` section), `admin.warrantyClaims.*`, `admin.warrantyClaimModal.*`, `admin.warrantyClaimStatus.*` (3 new sections), `admin.confirm.deleteWarrantyClaim`, `admin.sidebar.warrantyClaims`, `admin.pageMeta.warrantyClaims.{title,sub}` — consumed by Task 6/7/8.

Each locale file has the identical structure. Apply the same 4 kinds of edits to every file below, translating the string values.

- [ ] **Step 1: `vi.js` — add 2 keys to the existing `warranty` section**

Find the existing `warranty: { ... daysLeft: "còn {count} ngày", },` block (around line 961-976) and add 2 keys right before the closing `},`:

```javascript
      daysLeft: "còn {count} ngày",
      colAction: "Thao tác",
      createClaim: "Tạo phiếu bảo hành",
    },
```

- [ ] **Step 2: `vi.js` — add `sidebar.warrantyClaims` and `pageMeta.warrantyClaims`**

In the `sidebar` block (the one used by `WarehouseManagementPage.vue`/`AdminPage.vue`, around line 416-418 — same block Task 6-10 of the Trả hàng plan added `traHang` to), add:

```javascript
      traHang: "Trả hàng",
      warrantyClaims: "Phiếu bảo hành",
    },
```

In the `pageMeta` block (around line 436-438, same block that already has `traHang`), add:

```javascript
      traHang:           { title: "Trả hàng", sub: "Quản lý phiếu trả hàng" },
      warrantyClaims:    { title: "Phiếu bảo hành", sub: "Tiếp nhận và xử lý bảo hành" },
    };
```
(Keep whatever closing token — `};` or `},` — was already there; only add the `warrantyClaims:` line above it.)

- [ ] **Step 3: `vi.js` — new `warrantyClaims`/`warrantyClaimModal`/`warrantyClaimStatus` sections**

Insert right after the `returnModal: { ... },` block closes (the block added by the Trả hàng plan, immediately before `inventory: {`):

```javascript
    warrantyClaims: {
      countSuffix: "phiếu bảo hành",
      loading: "Đang tải...",
      colId: "Mã phiếu",
      colProduct: "Sản phẩm",
      colSerial: "Serial",
      colCustomer: "Khách hàng",
      colOrder: "Đơn hàng",
      colReceived: "Ngày tiếp nhận",
      colReturned: "Ngày trả khách",
      colCost: "Chi phí phát sinh",
      colStatus: "Trạng thái",
      colAction: "Thao tác",
      edit: "Sửa",
      delete: "Xóa",
      empty: "Chưa có phiếu bảo hành",
      searchPlaceholder: "Tìm mã phiếu, khách hàng, serial...",
    },

    warrantyClaimStatus: {
      con_bao_hanh: "Mới tiếp nhận",
      dang_xu_ly: "Đang xử lý",
      da_xu_ly: "Đã trả khách",
      het_bao_hanh: "Từ chối - hết hạn",
      tu_choi: "Từ chối",
    },

    warrantyClaimModal: {
      titleAdd: "Tạo phiếu bảo hành",
      titleEdit: "Chi tiết phiếu bảo hành",
      productLabel: "Sản phẩm",
      serialLabel: "Serial",
      customerLabel: "Khách hàng",
      orderLabel: "Đơn hàng",
      purchaseDateLabel: "Ngày mua",
      expiryDateLabel: "Hết hạn bảo hành",
      receivedDateLabel: "Ngày tiếp nhận",
      returnedDateLabel: "Ngày trả khách",
      faultLabel: "Mô tả lỗi",
      resultLabel: "Kết quả xử lý",
      statusLabel: "Trạng thái",
      costLabel: "Chi phí phát sinh",
      noteLabel: "Ghi chú",
      faultRequired: "Vui lòng nhập mô tả lỗi",
      cancel: "Hủy",
      save: "Lưu",
    },

```

- [ ] **Step 4: `vi.js` — `admin.confirm.deleteWarrantyClaim`**

Find the `confirm:` block (same block containing `deleteReturn`, added by the Trả hàng plan) and add:

```javascript
      deleteWarrantyClaim: "Xóa phiếu bảo hành này?",
```

- [ ] **Step 5: Repeat steps 1-4 for `en.js`**

Warranty section additions:
```javascript
      daysLeft: "{count} days left",
      colAction: "Action",
      createClaim: "Create warranty claim",
    },
```

Sidebar/pageMeta:
```javascript
      traHang: "Returns",
      warrantyClaims: "Warranty claims",
    },
```
```javascript
      traHang:           { title: "Returns", sub: "Manage product returns" },
      warrantyClaims:    { title: "Warranty claims", sub: "Receive and process warranty claims" },
    };
```

New sections:
```javascript
    warrantyClaims: {
      countSuffix: "warranty claims",
      loading: "Loading...",
      colId: "Claim ID",
      colProduct: "Product",
      colSerial: "Serial",
      colCustomer: "Customer",
      colOrder: "Order",
      colReceived: "Received date",
      colReturned: "Returned date",
      colCost: "Extra cost",
      colStatus: "Status",
      colAction: "Action",
      edit: "Edit",
      delete: "Delete",
      empty: "No warranty claims yet",
      searchPlaceholder: "Search claim ID, customer, serial...",
    },

    warrantyClaimStatus: {
      con_bao_hanh: "Just received",
      dang_xu_ly: "Processing",
      da_xu_ly: "Returned to customer",
      het_bao_hanh: "Rejected - expired",
      tu_choi: "Rejected",
    },

    warrantyClaimModal: {
      titleAdd: "Create warranty claim",
      titleEdit: "Warranty claim details",
      productLabel: "Product",
      serialLabel: "Serial",
      customerLabel: "Customer",
      orderLabel: "Order",
      purchaseDateLabel: "Purchase date",
      expiryDateLabel: "Warranty expiry",
      receivedDateLabel: "Received date",
      returnedDateLabel: "Returned date",
      faultLabel: "Fault description",
      resultLabel: "Processing result",
      statusLabel: "Status",
      costLabel: "Extra cost",
      noteLabel: "Note",
      faultRequired: "Please enter a fault description",
      cancel: "Cancel",
      save: "Save",
    },

```

`confirm.deleteWarrantyClaim`: `"deleteWarrantyClaim": "Delete this warranty claim?",`

- [ ] **Step 6: Repeat for `zh.js`**

Warranty section additions:
```javascript
      daysLeft: "还剩 {count} 天",
      colAction: "操作",
      createClaim: "创建保修单",
    },
```

Sidebar/pageMeta:
```javascript
      traHang: "退货",
      warrantyClaims: "保修单",
    },
```
```javascript
      traHang:           { title: "退货", sub: "管理退货单" },
      warrantyClaims:    { title: "保修单", sub: "接收并处理保修申请" },
    };
```

New sections:
```javascript
    warrantyClaims: {
      countSuffix: "个保修单",
      loading: "加载中...",
      colId: "保修单号",
      colProduct: "商品",
      colSerial: "序列号",
      colCustomer: "客户",
      colOrder: "订单",
      colReceived: "接收日期",
      colReturned: "归还日期",
      colCost: "额外费用",
      colStatus: "状态",
      colAction: "操作",
      edit: "编辑",
      delete: "删除",
      empty: "暂无保修单",
      searchPlaceholder: "搜索保修单号、客户、序列号...",
    },

    warrantyClaimStatus: {
      con_bao_hanh: "刚接收",
      dang_xu_ly: "处理中",
      da_xu_ly: "已归还客户",
      het_bao_hanh: "拒绝 - 已过保",
      tu_choi: "已拒绝",
    },

    warrantyClaimModal: {
      titleAdd: "创建保修单",
      titleEdit: "保修单详情",
      productLabel: "商品",
      serialLabel: "序列号",
      customerLabel: "客户",
      orderLabel: "订单",
      purchaseDateLabel: "购买日期",
      expiryDateLabel: "保修到期日",
      receivedDateLabel: "接收日期",
      returnedDateLabel: "归还日期",
      faultLabel: "故障描述",
      resultLabel: "处理结果",
      statusLabel: "状态",
      costLabel: "额外费用",
      noteLabel: "备注",
      faultRequired: "请输入故障描述",
      cancel: "取消",
      save: "保存",
    },

```

`confirm.deleteWarrantyClaim`: `"deleteWarrantyClaim": "确定删除此保修单？",`

- [ ] **Step 7: Repeat for `ko.js`**

Warranty section additions:
```javascript
      daysLeft: "{count}일 남음",
      colAction: "작업",
      createClaim: "보증 신청서 작성",
    },
```

Sidebar/pageMeta:
```javascript
      traHang: "반품",
      warrantyClaims: "보증 신청서",
    },
```
```javascript
      traHang:           { title: "반품", sub: "반품 전표 관리" },
      warrantyClaims:    { title: "보증 신청서", sub: "보증 접수 및 처리" },
    };
```

New sections:
```javascript
    warrantyClaims: {
      countSuffix: "건의 보증 신청서",
      loading: "로딩 중...",
      colId: "신청서 번호",
      colProduct: "상품",
      colSerial: "시리얼",
      colCustomer: "고객",
      colOrder: "주문",
      colReceived: "접수일",
      colReturned: "반환일",
      colCost: "추가 비용",
      colStatus: "상태",
      colAction: "작업",
      edit: "수정",
      delete: "삭제",
      empty: "보증 신청서가 없습니다",
      searchPlaceholder: "신청서 번호, 고객, 시리얼 검색...",
    },

    warrantyClaimStatus: {
      con_bao_hanh: "접수됨",
      dang_xu_ly: "처리 중",
      da_xu_ly: "고객에게 반환됨",
      het_bao_hanh: "거부됨 - 기간 만료",
      tu_choi: "거부됨",
    },

    warrantyClaimModal: {
      titleAdd: "보증 신청서 작성",
      titleEdit: "보증 신청서 상세",
      productLabel: "상품",
      serialLabel: "시리얼",
      customerLabel: "고객",
      orderLabel: "주문",
      purchaseDateLabel: "구매일",
      expiryDateLabel: "보증 만료일",
      receivedDateLabel: "접수일",
      returnedDateLabel: "반환일",
      faultLabel: "고장 설명",
      resultLabel: "처리 결과",
      statusLabel: "상태",
      costLabel: "추가 비용",
      noteLabel: "메모",
      faultRequired: "고장 설명을 입력해 주세요",
      cancel: "취소",
      save: "저장",
    },

```

`confirm.deleteWarrantyClaim`: `"deleteWarrantyClaim": "이 보증 신청서를 삭제할까요?",`

- [ ] **Step 8: Repeat for `ja.js`**

Warranty section additions:
```javascript
      daysLeft: "残り{count}日",
      colAction: "操作",
      createClaim: "保証申請を作成",
    },
```

Sidebar/pageMeta:
```javascript
      traHang: "返品",
      warrantyClaims: "保証申請",
    },
```
```javascript
      traHang:           { title: "返品", sub: "返品伝票管理" },
      warrantyClaims:    { title: "保証申請", sub: "保証の受付と処理" },
    };
```

New sections:
```javascript
    warrantyClaims: {
      countSuffix: "件の保証申請",
      loading: "読み込み中...",
      colId: "申請番号",
      colProduct: "商品",
      colSerial: "シリアル",
      colCustomer: "顧客",
      colOrder: "注文",
      colReceived: "受付日",
      colReturned: "返却日",
      colCost: "追加費用",
      colStatus: "状態",
      colAction: "操作",
      edit: "編集",
      delete: "削除",
      empty: "保証申請はまだありません",
      searchPlaceholder: "申請番号、顧客、シリアルで検索...",
    },

    warrantyClaimStatus: {
      con_bao_hanh: "受付済み",
      dang_xu_ly: "処理中",
      da_xu_ly: "顧客へ返却済み",
      het_bao_hanh: "却下 - 期限切れ",
      tu_choi: "却下",
    },

    warrantyClaimModal: {
      titleAdd: "保証申請を作成",
      titleEdit: "保証申請の詳細",
      productLabel: "商品",
      serialLabel: "シリアル",
      customerLabel: "顧客",
      orderLabel: "注文",
      purchaseDateLabel: "購入日",
      expiryDateLabel: "保証期限",
      receivedDateLabel: "受付日",
      returnedDateLabel: "返却日",
      faultLabel: "故障内容",
      resultLabel: "処理結果",
      statusLabel: "状態",
      costLabel: "追加費用",
      noteLabel: "備考",
      faultRequired: "故障内容を入力してください",
      cancel: "キャンセル",
      save: "保存",
    },

```

`confirm.deleteWarrantyClaim`: `"deleteWarrantyClaim": "この保証申請を削除しますか？",`

- [ ] **Step 9: Manual verification**

```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 10; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Confirm clean compile with no errors referencing any of the 5 locale files (a syntax error — unbalanced braces, missing comma — surfaces as a Vite error naming that exact file).

- [ ] **Step 10: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat(i18n): add warranty claim translation keys for all 5 locales"
```

---

## Task 6: `WarrantyPanel.vue` component

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue`

**Interfaces:**
- Consumes: `PhieuBaoHanhService.{getAll,save,remove}` (Task 4), `BaoHanhStore/ensureBaoHanh/refreshBaoHanh` (Task 4), `ChiTietSanPhamService.getUnderWarranty()` (existing, now returns `donHangId`/`bienTheId`/`khachHangId` per Task 2), `CustomersStore/ensureCustomers` (existing), `nowLocalIso()` (existing, `utils/datetime.js`), i18n keys from Task 5.
- Produces: `<WarrantyPanel />` (no props — identical behavior wherever mounted) — consumed by Task 7/8.

- [ ] **Step 1: Create the component**

```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import * as PhieuBaoHanhService from "../../Service/PhieuBaoHanhService.js";
import { formatPrice, formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { CustomersStore, ensureCustomers } from "../../stores/customers.js";
import { BaoHanhStore, ensureBaoHanh, refreshBaoHanh } from "../../stores/baoHanh.js";

onMounted(() => {
  ensureWarrantyData();
  ensureBaoHanh();
  ensureCustomers();
});

// ── Bảng "Còn hạn bảo hành" — chuyển nguyên xi từ AdminPage.vue ────────────────
const warrantyList = ref([]);
const warrantyLoading = ref(false);
const warrantySearch = ref('');
let warrantyPromise = null;
const ensureWarrantyData = (force = false) => {
  if (warrantyPromise && !force) return warrantyPromise;
  warrantyLoading.value = true;
  warrantyPromise = ChiTietSanPhamService.getUnderWarranty().catch(() => []).then((list) => {
    warrantyList.value = list;
    warrantyLoading.value = false;
  });
  return warrantyPromise;
};
const filteredWarranty = computed(() => {
  const q = warrantySearch.value.trim().toLowerCase();
  if (!q) return warrantyList.value;
  return warrantyList.value.filter((w) =>
    [w.soSerial, w.maSku, w.tenSanPham, w.maDonHang, w.tenKhachHang, w.soDienThoaiKhachHang]
      .some((v) => (v || '').toLowerCase().includes(q)));
});
const daysUntilExpiry = (isoDate) => Math.ceil((new Date(isoDate) - new Date()) / 86400000);

// ── Helpers ───────────────────────────────────────────────────────────────────
const customerName = (id) => CustomersStore.items.find(c => c.khachHangId === id)?.hoTen ?? `KH#${id}`;
const statusLabel = (s) => t(`admin.warrantyClaimStatus.${s}`);
const STATUS_COLOR = {
  con_bao_hanh: { bg: '#bfdbfe', text: '#1e3a8a' },
  dang_xu_ly:   { bg: '#fde68a', text: '#92400e' },
  da_xu_ly:     { bg: '#bbf7d0', text: '#166534' },
  het_bao_hanh: { bg: '#fecaca', text: '#991b1b' },
  tu_choi:      { bg: '#e5e7eb', text: '#374151' },
};
const statusColor = (s) => STATUS_COLOR[s] ?? { bg: '#e5e7eb', text: '#374151' };

// ── Bảng "Phiếu bảo hành" (CRUD) ────────────────────────────────────────────────
const claimSearch = ref("");
const filteredClaims = computed(() => {
  const q = claimSearch.value.trim().toLowerCase();
  if (!q) return BaoHanhStore.items;
  return BaoHanhStore.items.filter((p) => {
    const name = customerName(p.khachHangId).toLowerCase();
    return String(p.baoHanhId).includes(q) || name.includes(q) || (p.soSerial ?? '').toLowerCase().includes(q);
  });
});

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const lockedInfo = ref(null); // { tenSanPham, maSku, soSerial, tenKhachHang, maDonHang } — hien thi tinh, khong sua

const emptyForm = () => ({
  donHangId: null, bienTheId: null, chiTietId: null, khachHangId: null,
  ngayMua: '', ngayHetBh: '',
  ngayTiepNhan: '', ngayTraKhach: '',
  moTaLoi: '', ketQuaXuLy: '', trangThai: 'con_bao_hanh',
  chiPhiPhatSinh: 0, ghiChu: '',
});
const form = ref(emptyForm());

const openCreateFromWarranty = (w) => {
  editingId.value = null;
  form.value = {
    ...emptyForm(),
    donHangId: w.donHangId,
    bienTheId: w.bienTheId,
    chiTietId: w.chiTietId,
    khachHangId: w.khachHangId,
    ngayMua: (w.ngayGiaoThucTe || '').slice(0, 16),
    ngayHetBh: (w.ngayHetBaoHanh || '').slice(0, 16),
  };
  lockedInfo.value = {
    tenSanPham: w.tenSanPham, maSku: w.maSku, soSerial: w.soSerial,
    tenKhachHang: w.tenKhachHang, maDonHang: w.maDonHang,
  };
  formError.value = "";
  showModal.value = true;
};

const openEdit = (p) => {
  editingId.value = p.baoHanhId;
  form.value = {
    donHangId: p.donHangId, bienTheId: p.bienTheId, chiTietId: p.chiTietId, khachHangId: p.khachHangId,
    ngayMua: (p.ngayMua || '').slice(0, 16),
    ngayHetBh: (p.ngayHetBh || '').slice(0, 16),
    ngayTiepNhan: (p.ngayTiepNhan || '').slice(0, 16),
    ngayTraKhach: (p.ngayTraKhach || '').slice(0, 16),
    moTaLoi: p.moTaLoi || '',
    ketQuaXuLy: p.ketQuaXuLy || '',
    trangThai: p.trangThai,
    chiPhiPhatSinh: p.chiPhiPhatSinh ?? 0,
    ghiChu: p.ghiChu || '',
  };
  lockedInfo.value = {
    tenSanPham: null, maSku: p.maSku, soSerial: p.soSerial,
    tenKhachHang: customerName(p.khachHangId), maDonHang: `#${p.donHangId}`,
  };
  formError.value = "";
  showModal.value = true;
};

const saveClaim = async () => {
  formError.value = "";
  if (!form.value.donHangId || !form.value.bienTheId || !form.value.khachHangId) {
    formError.value = t('admin.warrantyClaimModal.faultRequired');
    return;
  }
  if (!form.value.moTaLoi.trim()) {
    formError.value = t('admin.warrantyClaimModal.faultRequired');
    return;
  }
  try {
    const body = {
      donHangId: form.value.donHangId,
      bienTheId: form.value.bienTheId,
      chiTietId: form.value.chiTietId,
      khachHangId: form.value.khachHangId,
      ngayMua: nowLocalIso(new Date(form.value.ngayMua)),
      ngayHetBh: nowLocalIso(new Date(form.value.ngayHetBh)),
      ngayTiepNhan: form.value.ngayTiepNhan ? nowLocalIso(new Date(form.value.ngayTiepNhan)) : null,
      ngayTraKhach: form.value.ngayTraKhach ? nowLocalIso(new Date(form.value.ngayTraKhach)) : null,
      moTaLoi: form.value.moTaLoi,
      ketQuaXuLy: form.value.ketQuaXuLy || null,
      trangThai: form.value.trangThai,
      chiPhiPhatSinh: form.value.chiPhiPhatSinh || 0,
      ghiChu: form.value.ghiChu || '—',
    };
    const res = await PhieuBaoHanhService.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await refreshBaoHanh();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteClaim = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteWarrantyClaim')))) return;
  const res = await PhieuBaoHanhService.remove(id);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status }))); return; }
  await refreshBaoHanh();
};
</script>

<template>
  <!-- ══ BANG CON HAN BAO HANH ══ -->
  <div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
    <span class="text-secondary small">{{ filteredWarranty.length }} {{ t('admin.warranty.countSuffix') }}</span>
    <span class="badge" style="background:rgba(148,163,184,0.15);color:#94a3b8;font-size:0.72rem;">📅 {{ t('admin.warranty.today') }}: {{ formatDate(new Date()) }}</span>
    <input v-model="warrantySearch" class="form-control form-control-sm ms-auto" style="max-width:260px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);font-size:0.82rem;"
           :placeholder="t('admin.warranty.searchPlaceholder')" />
  </div>
  <div v-if="warrantyLoading" class="text-secondary small text-center py-5">{{ t('admin.warranty.loading') }}</div>
  <div v-else class="table-responsive mb-4">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.warranty.colSerial') }}</th>
        <th>{{ t('admin.warranty.colProduct') }}</th>
        <th>{{ t('admin.warranty.colCustomer') }}</th>
        <th>{{ t('admin.warranty.colPhone') }}</th>
        <th>{{ t('admin.warranty.colOrder') }}</th>
        <th>{{ t('admin.warranty.colDelivered') }}</th>
        <th>{{ t('admin.warranty.colExpires') }}</th>
        <th>{{ t('admin.warranty.colRemaining') }}</th>
        <th>{{ t('admin.warranty.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(w, idx) in filteredWarranty" :key="w.chiTietId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ w.soSerial }}</td>
          <td>{{ w.tenSanPham }} <span class="text-secondary" style="font-size:0.75rem;">({{ w.maSku }})</span></td>
          <td>{{ w.tenKhachHang }}</td>
          <td class="text-secondary">{{ w.soDienThoaiKhachHang }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ w.maDonHang }}</td>
          <td>{{ formatDate(w.ngayGiaoThucTe) }}</td>
          <td>{{ formatDate(w.ngayHetBaoHanh) }}</td>
          <td>
            <span class="badge" :style="daysUntilExpiry(w.ngayHetBaoHanh) <= 30
              ? { background: 'rgba(248,113,113,0.15)', color: '#f87171' }
              : daysUntilExpiry(w.ngayHetBaoHanh) <= 90
                ? { background: 'rgba(250,204,21,0.15)', color: '#facc15' }
                : { background: 'rgba(34,197,94,0.15)', color: '#22c55e' }">
              {{ t('admin.warranty.daysLeft', { count: daysUntilExpiry(w.ngayHetBaoHanh) }) }}
            </span>
          </td>
          <td>
            <button class="btn btn-sm btn-outline-warning" style="font-size:0.72rem;padding:2px 8px;" @click="openCreateFromWarranty(w)">🛡️ {{ t('admin.warranty.createClaim') }}</button>
          </td>
        </tr>
        <tr v-if="filteredWarranty.length===0"><td colspan="10" class="text-center text-secondary">{{ t('admin.warranty.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ BANG PHIEU BAO HANH ══ -->
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredClaims.length }}/{{ BaoHanhStore.items.length }} {{ t('admin.warrantyClaims.countSuffix') }}</span>
    <input v-model="claimSearch" class="form-control form-control-sm" style="width:240px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.warrantyClaims.searchPlaceholder')" />
  </div>
  <div v-if="BaoHanhStore.loading" class="text-secondary small">{{ t('admin.warrantyClaims.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead><tr>
        <th style="width:40px;">{{ t('admin.common.stt') }}</th>
        <th>{{ t('admin.warrantyClaims.colId') }}</th><th>{{ t('admin.warrantyClaims.colProduct') }}</th><th>{{ t('admin.warrantyClaims.colSerial') }}</th>
        <th>{{ t('admin.warrantyClaims.colCustomer') }}</th><th>{{ t('admin.warrantyClaims.colOrder') }}</th>
        <th>{{ t('admin.warrantyClaims.colCost') }}</th><th>{{ t('admin.warrantyClaims.colStatus') }}</th><th>{{ t('admin.warrantyClaims.colAction') }}</th>
      </tr></thead>
      <tbody>
        <tr v-for="(p, idx) in filteredClaims" :key="p.baoHanhId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td class="text-secondary" style="font-family:monospace;">#{{ p.baoHanhId }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ p.maSku }}</td>
          <td class="text-secondary" style="font-family:monospace;">{{ p.soSerial || '—' }}</td>
          <td>{{ customerName(p.khachHangId) }}</td>
          <td class="text-secondary">#{{ p.donHangId }}</td>
          <td class="text-warning fw-semibold">{{ formatPrice(p.chiPhiPhatSinh) }}</td>
          <td><span class="badge" :style="{ background: statusColor(p.trangThai).bg, color: statusColor(p.trangThai).text }">{{ statusLabel(p.trangThai) }}</span></td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(p)">{{ t('admin.warrantyClaims.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteClaim(p.baoHanhId)">{{ t('admin.warrantyClaims.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredClaims.length===0"><td colspan="9" class="text-center text-secondary">{{ t('admin.warrantyClaims.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <!-- ══ MODAL PHIEU BAO HANH ══ -->
  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:560px;max-width:96vw;max-height:90vh;overflow-y:auto;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.warrantyClaimModal.titleEdit') : t('admin.warrantyClaimModal.titleAdd') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div class="p-2 mb-3 rounded-2" style="background:var(--bg-input);">
        <div v-if="lockedInfo?.tenSanPham" class="small">{{ t('admin.warrantyClaimModal.productLabel') }}: <strong>{{ lockedInfo.tenSanPham }}</strong> ({{ lockedInfo.maSku }})</div>
        <div v-else class="small">{{ t('admin.warrantyClaimModal.productLabel') }}: <strong>{{ lockedInfo?.maSku }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.serialLabel') }}: <strong>{{ lockedInfo?.soSerial || '—' }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.customerLabel') }}: <strong>{{ lockedInfo?.tenKhachHang }}</strong></div>
        <div class="small">{{ t('admin.warrantyClaimModal.orderLabel') }}: <strong>{{ lockedInfo?.maDonHang }}</strong></div>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.purchaseDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayMua" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.expiryDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayHetBh" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.faultLabel') }} *</label>
        <input v-model="form.moTaLoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="con_bao_hanh">{{ t('admin.warrantyClaimStatus.con_bao_hanh') }}</option>
          <option value="dang_xu_ly">{{ t('admin.warrantyClaimStatus.dang_xu_ly') }}</option>
          <option value="da_xu_ly">{{ t('admin.warrantyClaimStatus.da_xu_ly') }}</option>
          <option value="het_bao_hanh">{{ t('admin.warrantyClaimStatus.het_bao_hanh') }}</option>
          <option value="tu_choi">{{ t('admin.warrantyClaimStatus.tu_choi') }}</option>
        </select>
      </div>

      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.receivedDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayTiepNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
        <div class="col-6">
          <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.returnedDateLabel') }}</label>
          <input type="datetime-local" v-model="form.ngayTraKhach" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
        </div>
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.resultLabel') }}</label>
        <input v-model="form.ketQuaXuLy" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-2">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.costLabel') }}</label>
        <input type="number" min="0" v-model.number="form.chiPhiPhatSinh" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.warrantyClaimModal.noteLabel') }}</label>
        <input v-model="form.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.warrantyClaimModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveClaim">{{ t('admin.warrantyClaimModal.save') }}</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Manual verification**

```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
The component isn't mounted anywhere yet — Vite won't compile an unimported SFC on its own. To smoke-test it, temporarily add `import WarrantyPanel from "./src/components/admin/WarrantyPanel.vue";` and a `<WarrantyPanel />` tag into an already-mounted page (e.g. `StaffPage.vue`), run the dev server, confirm no compile error, then **fully revert that temporary edit** before committing — `git diff`/`git status` must show ONLY the new `WarrantyPanel.vue` file before the commit in Step 3.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/WarrantyPanel.vue
git commit -m "feat: add WarrantyPanel.vue component (under-warranty list + claim CRUD)"
```

---

## Task 7: Wire into `WarehouseManagementPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue`

**Interfaces:**
- Consumes: `WarrantyPanel.vue` (Task 6), mounted with no props.

- [ ] **Step 1: Import + register**

```javascript
import ReturnsPanel from "../components/admin/ReturnsPanel.vue";
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
```

```javascript
const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  warrantyClaims: { titleKey: "admin.pageMeta.warrantyClaims.title", subKey: "admin.pageMeta.warrantyClaims.sub", icon: "🛡️" },
};
```

- [ ] **Step 2: Add the nav item** (right after the "traHang" nav item)

```html
        <div class="adm-nav" :class="{active: currentPage==='warrantyClaims'}" @click="navigate('warrantyClaims')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.warrantyClaims') }}
        </div>
```

- [ ] **Step 3: Add the section**

```html
        <section v-show="currentPage === 'traHang'"><ReturnsPanel :readonly="true" /></section>
        <section v-show="currentPage === 'warrantyClaims'"><WarrantyPanel /></section>
```

- [ ] **Step 4: Manual verification**

```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Confirm clean compile referencing `WarehouseManagementPage.vue`/`WarrantyPanel.vue`.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue
git commit -m "feat: add Phiếu bảo hành tab to WarehouseManagementPage"
```

---

## Task 8: Wire into `AdminPage.vue` (replace existing read-only tab)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `WarrantyPanel.vue` (Task 6), mounted with no props, replacing the existing inline read-only warranty table.

- [ ] **Step 1: Import**

Near the other component imports (after `import ReturnsPanel from "../components/admin/ReturnsPanel.vue";`):

```javascript
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
```

- [ ] **Step 2: Remove the now-superseded warranty script state**

Delete these lines (all now live inside `WarrantyPanel.vue`, Task 6):

```javascript
const warrantyList = ref([]);
const warrantyLoading = ref(false);
const warrantySearch = ref('');
let warrantyPromise = null;
const ensureWarrantyData = (force = false) => {
  if (warrantyPromise && !force) return warrantyPromise;
  warrantyLoading.value = true;
  warrantyPromise = ChiTietSanPhamService.getUnderWarranty().catch(() => []).then((list) => {
    warrantyList.value = list;
    warrantyLoading.value = false;
  });
  return warrantyPromise;
};
const filteredWarranty = computed(() => {
  const q = warrantySearch.value.trim().toLowerCase();
  if (!q) return warrantyList.value;
  return warrantyList.value.filter((w) =>
    [w.soSerial, w.maSku, w.tenSanPham, w.maDonHang, w.tenKhachHang, w.soDienThoaiKhachHang]
      .some((v) => (v || '').toLowerCase().includes(q)));
});
const daysUntilExpiry = (isoDate) => Math.ceil((new Date(isoDate) - new Date()) / 86400000);
```

Also remove the now-unused import (nothing else in this file uses `ChiTietSanPhamService`):

```javascript
import * as ChiTietSanPhamService  from "../Service/ChiTietSanPhamService.js";
```

- [ ] **Step 3: Simplify the nav button** (remove the now-dead `ensureWarrantyData()` call)

Find:
```html
              <button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'; ensureWarrantyData()">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
```
Replace with:
```html
              <button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
```

- [ ] **Step 4: Replace the tab body**

Find the entire `<!-- ══ TAB: BAO HANH ══ -->` block (from `<div v-show="inventoryMainTab==='bao-hanh'">` through its matching closing `</div>`, i.e. the whole read-only table markup) and replace it with:

```html
          <!-- ══ TAB: BAO HANH ══ -->
          <div v-show="inventoryMainTab==='bao-hanh'">
            <WarrantyPanel />
          </div>
```

- [ ] **Step 5: Manual verification**

```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Confirm clean compile with no errors referencing `AdminPage.vue`/`WarrantyPanel.vue`, and no leftover reference to `warrantyList`/`ensureWarrantyData`/`filteredWarranty`/`daysUntilExpiry`/`ChiTietSanPhamService` anywhere else in `AdminPage.vue` (grep the file to confirm zero remaining hits before committing).

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat: replace read-only warranty tab with full WarrantyPanel CRUD on AdminPage"
```

---

## Self-Review

**1. Spec coverage:**
- Backend field-naming/wiring bug fix (`sanPhamId`→`bienTheId`, `serialNumber`→`chiTietId`) → Task 1. ✅
- Validation relaxation (`ngayTiepNhan`/`ngayTraKhach`/`ketQuaXuLy` optional) → Task 1 (built into the replaced `PhieuBaoHanhRequest.java`). ✅
- `WarrantyStatusResponse` FK ids for the "create from list" flow → Task 2. ✅
- `@PreAuthorize` lock → Task 3. ✅
- Shared `WarrantyPanel.vue` combining list + CRUD, no `readonly`/`canPickStaff` props (entity has no nhân viên field) → Task 6. ✅
- Mounted into WarehouseManagementPage + AdminPage (replacing old inline tab), NOT StaffPage → Tasks 7, 8. ✅
- i18n across 5 locales → Task 5. ✅

**2. Placeholder scan:** No TBD/"add later"/vague steps found — every step has literal code or an exact manual-verification procedure.

**3. Type consistency:** `bienTheId`/`chiTietId` used identically (camelCase, Integer, `chiTietId` nullable) across Task 1 (Request/Response/Service/Repository), Task 2 (`WarrantyStatusResponse`), Task 6 (`WarrantyPanel.vue`'s `openCreateFromWarranty`/`saveClaim`). `trangThai` enum values (`con_bao_hanh`/`dang_xu_ly`/`da_xu_ly`/`het_bao_hanh`/`tu_choi`) consistent across Task 5 (i18n) and Task 6 (component template). `PhieuBaoHanhService.save(id, body)`/`remove(id)` signatures match between Task 4 (definition) and Task 6 (usage). `BaoHanhStore`/`ensureBaoHanh`/`refreshBaoHanh` names match between Task 4 and Task 6.
