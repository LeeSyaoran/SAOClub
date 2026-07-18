# Backend Role Permissions Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khoá các endpoint backend hiện đang mở cho "bất kỳ ai đăng nhập" (kể cả khách hàng thường) về đúng "chỉ staff" (admin/nhân viên/quản kho), theo đúng convention `@PreAuthorize` đã có sẵn trong dự án (`ChucVuController`, `NhanVienController`, `KhachHangController`).

**Architecture:** Plan 1/4 trong chuỗi 4 plan xây StaffPage/WarehouseManagementPage (xem spec `docs/superpowers/specs/2026-07-18-staff-warehouse-pages-design.md`). Đây là plan độc lập, không phụ thuộc frontend — làm trước vì rủi ro thấp và không đụng file nào 3 plan sau sẽ sửa.

**Tech Stack:** Spring Boot 4.0.6, Spring Security 7.0.5 (`@PreAuthorize` + `@EnableMethodSecurity` đã bật sẵn).

## Global Constraints

- Style annotation: theo đúng `ChucVuController.java` (class-level, đặt trên `@RestController`, có comment 1 dòng giải thích) và `KhachHangController.java` (method-level, đặt ngay trên `@XxxMapping`, có comment 1 dòng giải thích).
- Role literal dùng `hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')` — đúng chuỗi đã dùng trong `KhachHangController.java`, KHÔNG dùng `hasRole` số ít trừ khi chỉ giới hạn đúng 1 role.
- TUYỆT ĐỐI không thêm `@PreAuthorize` vào bất kỳ method nào đã xác nhận đang được `CheckoutModal.vue`/`AccountPage.vue` gọi (xem bảng "Method giữ nguyên mở" ở Task 2) — thêm nhầm sẽ làm hỏng luồng mua hàng của khách.
- Không đụng `PhieuTraHangController`, `ChiTietTraHangController`, `PhieuBaoHanhController`, `DiaChiGiaoHangController` — ngoài phạm vi spec.

---

### Task 1: Khoá 6 controller kho/thanh-toán thuần túy (class-level)

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/TonKhoController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/PhieuNhapKhoController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ChiTietPhieuNhapController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/NhaCungCapController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/LichSuTonKhoController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ThanhToanController.java`

**Interfaces:** không đổi chữ ký method nào — chỉ thêm annotation ở class level. Không ảnh hưởng plan 2/3/4 (các plan sau không sửa 6 file này).

**Bối cảnh đã xác nhận (không cần re-verify):** đã grep toàn bộ `FrontEnd/QLBanMayTinh/src` (bao gồm `AccountPage.vue`, `CheckoutModal.vue`, `App.vue`, mọi component `product/`) — không có bất kỳ service nào trong 6 controller này được gọi từ code khách hàng. Chỉ `AdminPage.vue` gọi `TonKhoService`/`PhieuNhapKhoService`; `NhaCungCapService`/`LichSuTonKhoService`/`ThanhToanService` hiện chưa có file service frontend nào cả (chưa ai gọi).

- [ ] **Step 1: Sửa `TonKhoController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/TonKhoController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.TonKho;
import com.example.backend.service.TonKhoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Tồn kho — chỉ staff (admin/nhân viên/quản kho) thao tác. Không có nơi nào trong code
// khách hàng (checkout, AccountPage) gọi tới controller này — đã xác nhận qua grep toàn bộ
// frontend trước khi thêm annotation.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/ton-kho")
public class TonKhoController {

    @Autowired
    private TonKhoService tonKhoService;

    @GetMapping
    public List<TonKho> getAll() {
        return tonKhoService.getAll();
    }

    @GetMapping("/{id}")
    public TonKho getById(@PathVariable Integer id) {
        return tonKhoService.getById(id);
    }

    // GET theo bienTheId — dùng để kiểm tra số lượng tồn trước khi tạo đơn hàng
    @GetMapping("/bien-the/{bienTheId}")
    public TonKho getByBienTheId(@PathVariable Integer bienTheId) {
        return tonKhoService.getByBienTheId(bienTheId);
    }

    @PostMapping
    public ResponseEntity<TonKho> create(@RequestBody TonKho item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tonKhoService.create(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody TonKho item) {
        tonKhoService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        tonKhoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Sửa `PhieuNhapKhoController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/PhieuNhapKhoController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.PhieuNhapKho;
import com.example.backend.request.PhieuNhapKhoRequest;
import com.example.backend.response.PhieuNhapKhoResponse;
import com.example.backend.service.PhieuNhapKhoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Phiếu nhập kho — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/phieu-nhap-kho")
public class PhieuNhapKhoController {

    @Autowired
    private PhieuNhapKhoService phieuNhapKhoService;

    @GetMapping
    public List<PhieuNhapKhoResponse> getAll() {
        return phieuNhapKhoService.hienThiPhieuNhapKho();
    }

    @GetMapping("/{id}")
    public PhieuNhapKho getById(@PathVariable Integer id) {
        return phieuNhapKhoService.getById(id);
    }

    // POST — service xử lý FK nhaCungCap, nhanVien
    @PostMapping
    public ResponseEntity<PhieuNhapKho> create(@Valid @RequestBody PhieuNhapKhoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuNhapKhoService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody PhieuNhapKhoRequest request) {
        phieuNhapKhoService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        phieuNhapKhoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Sửa `ChiTietPhieuNhapController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/ChiTietPhieuNhapController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietPhieuNhap;
import com.example.backend.request.ChiTietPhieuNhapRequest;
import com.example.backend.response.ChiTietPhieuNhapResponse;
import com.example.backend.service.ChiTietPhieuNhapService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chi tiết phiếu nhập kho — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/chi-tiet-phieu-nhap")
public class ChiTietPhieuNhapController {

    @Autowired
    private ChiTietPhieuNhapService chiTietPhieuNhapService;

    @GetMapping
    public List<ChiTietPhieuNhapResponse> getAll() {
        return chiTietPhieuNhapService.hienThiChiTietPhieuNhap();
    }

    @GetMapping("/{id}")
    public ChiTietPhieuNhap getById(@PathVariable Integer id) {
        return chiTietPhieuNhapService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ChiTietPhieuNhap> create(@Valid @RequestBody ChiTietPhieuNhapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chiTietPhieuNhapService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietPhieuNhapRequest request) {
        chiTietPhieuNhapService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietPhieuNhapService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Sửa `NhaCungCapController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/NhaCungCapController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.NhaCungCap;
import com.example.backend.response.NhaCungCapResponse;
import com.example.backend.service.NhaCungCapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Nhà cung cấp — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/nha-cung-cap")
public class NhaCungCapController {

    @Autowired
    private NhaCungCapService nhaCungCapService;

    @GetMapping
    public List<NhaCungCapResponse> getAll() {
        return nhaCungCapService.hienThiNhaCungCap();
    }

    @GetMapping("/{id}")
    public NhaCungCap getById(@PathVariable Integer id) {
        return nhaCungCapService.getById(id);
    }

    @PostMapping
    public ResponseEntity<NhaCungCap> create(@RequestBody NhaCungCap item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhaCungCapService.create(item));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody NhaCungCap item) {
        nhaCungCapService.update(id, item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        nhaCungCapService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 5: Sửa `LichSuTonKhoController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/LichSuTonKhoController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.LichSuTonKho;
import com.example.backend.request.LichSuTonKhoRequest;
import com.example.backend.response.LichSuTonKhoResponse;
import com.example.backend.service.LichSuTonKhoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Lịch sử tồn kho — chỉ staff, không có nơi nào trong code khách hàng gọi tới.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/lich-su-ton-kho")
public class LichSuTonKhoController {

    @Autowired
    private LichSuTonKhoService lichSuTonKhoService;

    @GetMapping
    public List<LichSuTonKhoResponse> getAll() {
        return lichSuTonKhoService.hienThiLichSuTonKho();
    }

    @GetMapping("/{id}")
    public LichSuTonKho getById(@PathVariable Integer id) {
        return lichSuTonKhoService.getById(id);
    }

    // POST — service xử lý nhiều FK optional (bienThe, chiTiet, donHang, phieuNhap, nhanVien)
    @PostMapping
    public ResponseEntity<LichSuTonKho> create(@Valid @RequestBody LichSuTonKhoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lichSuTonKhoService.create(request));
    }

    // Lịch sử tồn kho không cho phép cập nhật — chỉ ghi thêm và xóa (audit trail)
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lichSuTonKhoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 6: Sửa `ThanhToanController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/ThanhToanController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.ThanhToan;
import com.example.backend.request.ThanhToanRequest;
import com.example.backend.response.ThanhToanResponse;
import com.example.backend.service.ThanhToanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Thanh toán — chỉ staff. Hiện chưa có service/component frontend nào gọi tới controller
// này (tính năng chưa được wire lên UI) — khoá trước theo nguyên tắc least-privilege.
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
@RestController
@RequestMapping("/api/thanh-toan")
public class ThanhToanController {

    @Autowired
    private ThanhToanService thanhToanService;

    @GetMapping
    public List<ThanhToanResponse> getAll() {
        return thanhToanService.hienThiThanhToan();
    }

    @GetMapping("/{id}")
    public ThanhToan getById(@PathVariable Integer id) {
        return thanhToanService.getById(id);
    }

    // POST — service xử lý FK donHang
    @PostMapping
    public ResponseEntity<ThanhToan> create(@Valid @RequestBody ThanhToanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thanhToanService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ThanhToanRequest request) {
        thanhToanService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        thanhToanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Biên dịch toàn bộ backend**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Chạy toàn bộ test hiện có (không có test riêng cho 6 controller này — xác nhận không có test nào khác bị ảnh hưởng)**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o test
```
Expected: `BUILD SUCCESS`, không có test nào fail (test hiện có gọi thẳng Service, không qua controller/security filter chain nên không bị ảnh hưởng bởi annotation mới).

- [ ] **Step 9: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/TonKhoController.java \
  BackEnd/src/main/java/com/example/backend/controller/PhieuNhapKhoController.java \
  BackEnd/src/main/java/com/example/backend/controller/ChiTietPhieuNhapController.java \
  BackEnd/src/main/java/com/example/backend/controller/NhaCungCapController.java \
  BackEnd/src/main/java/com/example/backend/controller/LichSuTonKhoController.java \
  BackEnd/src/main/java/com/example/backend/controller/ThanhToanController.java
git commit -m "fix: restrict warehouse/payment controllers to staff roles"
```

---

### Task 2: Khoá method staff-only trong `DonHangController` + `ChiTietDonHangController`

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DonHangController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ChiTietDonHangController.java`

**Interfaces:** không đổi chữ ký method nào — chỉ thêm annotation ở method level. Class vẫn mở (`@RestController` không có `@PreAuthorize` ở class), method nào cần khoá thì tự thêm annotation riêng.

**⚠️ Rủi ro cao hơn Task 1 — đọc kỹ bảng dưới trước khi sửa, TUYỆT ĐỐI không thêm `@PreAuthorize` vào cột "giữ nguyên mở":**

| Controller | Method | Quyết định | Lý do (đã xác nhận qua grep) |
|---|---|---|---|
| DonHangController | `create` | Giữ mở | `CheckoutModal.vue:595` gọi khi khách đặt hàng |
| DonHangController | `delete` | Giữ mở | `CheckoutModal.vue:619` gọi rollback khi tạo đơn lỗi giữa chừng |
| DonHangController | `getAll` | Giữ mở | `AccountPage.vue:102` gọi qua `khachHangId` param cho "Đơn hàng của tôi" |
| DonHangController | `getById` | Giữ mở | Không ai gọi hiện tại, nhưng là GET đơn giản, không phải hành động staff-only rõ ràng — không khoá để tránh suy đoán sai |
| DonHangController | `subscribe` (SSE) | Giữ mở | `AccountPage.vue:192` VÀ `AdminPage.vue:2779` đều dùng |
| DonHangController | `update` | **Khoá staff-only** | Chỉ `AdminPage.vue:2072,2115` gọi |
| DonHangController | `merge` | **Khoá staff-only** | Chỉ `AdminPage.vue:1163,1985` gọi |
| DonHangController | `recalculate` | **Khoá staff-only** | Chỉ `AdminPage.vue:1896,1947,1961` gọi |
| DonHangController | `xacNhan` | **Khoá staff-only** | Chỉ `AdminPage.vue:2182` gọi |
| ChiTietDonHangController | `create` | Giữ mở | `CheckoutModal.vue:608` gọi khi khách đặt hàng |
| ChiTietDonHangController | `getByDonHang` | Giữ mở | `AccountPage.vue:111` gọi để xem chi tiết đơn của mình |
| ChiTietDonHangController | `getAll` | **Khoá staff-only** | Không nơi nào trong frontend gọi |
| ChiTietDonHangController | `getById` | **Khoá staff-only** | Không nơi nào trong frontend gọi |
| ChiTietDonHangController | `update` | **Khoá staff-only** | Không nơi nào trong frontend gọi |
| ChiTietDonHangController | `delete` | **Khoá staff-only** | Không nơi nào trong frontend gọi |
| ChiTietDonHangController | `getSerialsByDonHang` | **Khoá staff-only** | Chỉ `AdminPage.vue:2143` gọi (modal "Chọn serial trước khi đóng gói") |

- [ ] **Step 1: Sửa `DonHangController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/DonHangController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.DonHang;
import com.example.backend.request.DonHangRequest;
import com.example.backend.request.XacNhanDonHangRequest;
import com.example.backend.request.MergeOrderRequest;
import com.example.backend.response.DonHangResponse;
import com.example.backend.service.DonHangService;
import com.example.backend.service.SseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/don-hang")
public class DonHangController {

    @Autowired
    private DonHangService donHangService;
    @Autowired
    private SseService sseService;

    // GET /api/don-hang?page=0&size=20&khachHangId=... — DTO query với LEFT JOIN nullable
    // FKs (nhanVien, khuyenMai, diaChiGiaoHang có thể null), có phân trang.
    // khachHangId optional: trang "Đơn hàng của tôi" truyền vào để chỉ lấy đơn của khách
    // đó thay vì tải hết đơn toàn hệ thống rồi lọc ở trình duyệt (rất chậm khi số đơn lớn).
    // Giữ mở cho mọi role đã đăng nhập — cả AccountPage (khách) lẫn Admin/Staff đều gọi.
    @GetMapping
    public Page<DonHangResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer khachHangId) {
        return donHangService.hienThiDonHang(khachHangId, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public DonHang getById(@PathVariable Integer id) {
        return donHangService.getById(id);
    }

    // POST — service xử lý các FK: khachHang, nhanVien, khuyenMai, diaChiGiaoHang.
    // Giữ mở — CheckoutModal.vue gọi khi khách đặt hàng.
    @PostMapping
    public ResponseEntity<DonHang> create(@Valid @RequestBody DonHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donHangService.create(request));
    }

    // Cập nhật đơn hàng (trạng thái, thông tin giao...) — chỉ staff thao tác qua AdminPage.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DonHangRequest request) {
        donHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    // Xoá đơn — giữ mở: CheckoutModal.vue gọi để rollback khi tạo đơn lỗi giữa chừng.
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        donHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Gộp nhiều đơn hàng của cùng khách vào 1 đơn đích — chỉ staff (AdminPage).
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PostMapping("merge")
    public ResponseEntity<Void> merge(@RequestBody MergeOrderRequest request) {
        donHangService.mergeOrders(request.getTargetId(), request.getSourceIds());
        return ResponseEntity.ok().build();
    }

    // Tính lại tong_tien sau khi thêm/xóa sản phẩm trong đơn — chỉ staff (AdminPage).
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PatchMapping("{id}/recalculate")
    public ResponseEntity<Void> recalculate(@PathVariable Integer id) {
        donHangService.recalculateTongTien(id);
        return ResponseEntity.ok().build();
    }

    // Chọn serial cho từng dòng + chốt bán + chuyển sang "confirmed" — chỉ staff (AdminPage),
    // đơn tại quầy đã chốt serial ngay lúc tạo, không qua bước này.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PatchMapping("{id}/xac-nhan")
    public ResponseEntity<Void> xacNhan(@PathVariable Integer id, @Valid @RequestBody XacNhanDonHangRequest request) {
        donHangService.xacNhanDonHang(id, request);
        return ResponseEntity.ok().build();
    }

    // SSE — giữ mở: cả AccountPage (khách theo dõi đơn của mình) lẫn AdminPage (staff) đều subscribe.
    @GetMapping(value = "events", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }
}
```

- [ ] **Step 2: Sửa `ChiTietDonHangController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/ChiTietDonHangController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.request.ChiTietDonHangRequest;
import com.example.backend.response.ChiTietDonHangResponse;
import com.example.backend.response.ChiTietDonHangSerialResponse;
import com.example.backend.service.ChiTietDonHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-don-hang")
public class ChiTietDonHangController {

    @Autowired
    private ChiTietDonHangService chiTietDonHangService;

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping
    public List<ChiTietDonHangResponse> getAll() {
        return chiTietDonHangService.hienThiChiTietDonHang();
    }

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}")
    public ChiTietDonHang getById(@PathVariable Integer id) {
        return chiTietDonHangService.getById(id);
    }

    // Giữ mở — AccountPage.vue gọi để xem chi tiết đơn của chính khách hàng.
    @GetMapping("/don-hang/{donHangId}")
    public List<ChiTietDonHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getByDonHangId(donHangId);
    }

    // Chỉ AdminPage gọi (modal "Chọn serial trước khi đóng gói") — chỉ staff.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/don-hang/{donHangId}/serials")
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getSerialsByDonHangId(donHangId);
    }

    // Giữ mở — CheckoutModal.vue gọi khi khách đặt hàng (tạo từng dòng chi tiết đơn).
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietDonHangRequest request) {
        chiTietDonHangService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietDonHangRequest request) {
        chiTietDonHangService.update(id, request);
        return ResponseEntity.ok().build();
    }

    // Không nơi nào trong frontend gọi hiện tại — khoá staff-only theo least-privilege.
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietDonHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Biên dịch + chạy test hiện có**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
.\mvnw.cmd -o test "-Dtest=DonHangServiceTest,ChiTietDonHangServiceTest"
```
Expected: `BUILD SUCCESS` cả 2 lệnh — test Service hiện có gọi thẳng Service, không qua controller nên không bị ảnh hưởng.

- [ ] **Step 4: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/DonHangController.java \
  BackEnd/src/main/java/com/example/backend/controller/ChiTietDonHangController.java
git commit -m "fix: restrict staff-only order actions (update/merge/recalculate/xacNhan) while keeping customer checkout flow open"
```

---

### Task 3: Kiểm thử thủ công xác nhận không phá checkout + đúng phân quyền

**Files:** không có file thay đổi — chỉ chạy và quan sát qua HTTP trực tiếp (không cần trình duyệt cho task này, chỉ verify API).

- [ ] **Step 1: Chạy backend**

```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd spring-boot:run
```

- [ ] **Step 2: Tạo 1 tài khoản khách hàng thử (throwaway) qua API**

```bash
curl -s -X POST http://localhost:8080/api/khach-hang/register -H "Content-Type: application/json" \
  -d '{"hoTen":"Test Perm","soDienThoai":"0977000111","email":"testperm@example.com","username":"testperm_temp","matKhau":"Test@12345"}'
```
Đăng nhập lấy JWT:
```bash
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"testperm_temp","password":"Test@12345"}'
```

- [ ] **Step 3: Xác nhận khách hàng vẫn gọi được các endpoint giữ mở**

Với JWT khách hàng vừa lấy (`$CUST_TOKEN`):
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/don-hang?khachHangId=1 -H "Authorization: Bearer $CUST_TOKEN"
```
Expected: `200` (không phải `403`).

- [ ] **Step 4: Xác nhận khách hàng bị chặn ở các endpoint mới khoá**

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/ton-kho -H "Authorization: Bearer $CUST_TOKEN"
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/nha-cung-cap -H "Authorization: Bearer $CUST_TOKEN"
curl -s -o /dev/null -w "%{http_code}\n" -X PATCH http://localhost:8080/api/don-hang/1/recalculate -H "Authorization: Bearer $CUST_TOKEN"
```
Expected: cả 3 đều `403`.

- [ ] **Step 5: Xác nhận staff (nhan_vien) vẫn gọi được các endpoint mới khoá**

Tạo 1 tài khoản `nhan_vien` thử qua SQL (theo đúng cách đã dùng ở phiên trước — insert `nhan_vien` + `tai_khoan` với `chuc_vu_id=2`, set `mat_khau_hash` qua BCryptPasswordEncoder, login lấy JWT thật), rồi gọi lại đúng 3 endpoint ở Step 4 với JWT staff.
Expected: cả 3 đều `200`/`201` (không phải `403`).

Dọn dẹp: xoá tài khoản khách hàng thử (Step 2) và tài khoản nhân viên thử (Step 5) khỏi DB sau khi test xong.

- [ ] **Step 6: Dừng server**

`Ctrl+C`.

---

## Tự rà soát (self-review)

**1. Phủ đủ spec:**
- 5 controller kho thuần túy + `ThanhToanController` → Task 1. ✅
- `DonHangController`/`ChiTietDonHangController` method-level, đúng danh sách đã rà trong spec (bổ sung `getSerialsByDonHang` phát hiện thêm khi đọc code thực tế — spec ban đầu bỏ sót method này) → Task 2. ✅
- Xác nhận checkout/AccountPage không bị hỏng → Task 3. ✅

**2. Không còn placeholder** — mọi file đều có code đầy đủ, không có TODO.

**3. Nhất quán:** annotation string `hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')` giống hệt nhau xuyên suốt, đúng với chuỗi đã dùng trong `KhachHangController.java` hiện có.

## Ngoài phạm vi

- `PhieuTraHangController`, `ChiTietTraHangController`, `PhieuBaoHanhController`, `DiaChiGiaoHangController` — không đụng tới.
- Ownership-check cho `DonHangController.getAll` (khách đổi `khachHangId` trên URL xem đơn người khác) — lỗ hổng khác, không phát sinh từ thiếu `@PreAuthorize`, để dành yêu cầu riêng.
- Plan 2 (tách component dùng chung), Plan 3 (StaffPage), Plan 4 (WarehouseManagementPage) — plan riêng, không phụ thuộc plan này (frontend không đổi ở đây).
