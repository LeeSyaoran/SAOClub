# Trang chi tiết khách hàng (Admin) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cho admin 1 trang riêng để xem chi tiết 1 khách hàng (thông tin, lịch sử mua hàng, phiếu giảm giá/điểm thưởng, lịch sử tặng điểm) và thực hiện 2 hành động: tặng điểm, tặng voucher.

**Architecture:** Backend thêm 1 bảng ledger mới (`lich_su_tang_diem`) + 4 endpoint mới (2 ở `KhachHangController`, 2 ở `PhieuGiamGiaCaNhanController`), tất cả `@PreAuthorize("hasRole('ADMIN')")`. Frontend thêm 1 trang drill-down trong `AdminPage.vue` (theo đúng pattern `currentPage` state-switch có sẵn, không dùng router) + 2 modal hành động, dữ liệu đơn hàng/khách hàng tái dùng store đã có sẵn (`OrdersStore`, `CustomersStore`), không gọi API mới.

**Tech Stack:** Spring Boot (Java 17+, JPA/Hibernate, SQL Server), Vue 3 `<script setup>`, Bootstrap classes + CSS vars có sẵn trong dự án, JUnit5 + Mockito + AssertJ cho test backend.

## Global Constraints

- Mọi schema thay đổi phải idempotent trong `Database/QLBanMayTinh.sql` (dùng đúng pattern `IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = '...') BEGIN ... END GO`) — người dùng luôn chạy lại toàn bộ file, không chạy snippet riêng.
- 2 hành động "Tặng điểm"/"Tặng voucher" chỉ role `ADMIN` gọi được — `@PreAuthorize("hasRole('ADMIN')")` ở method-level (override đúng class-level `isAuthenticated()` có sẵn ở `PhieuGiamGiaCaNhanController`).
- Không thêm tính năng trừ điểm/thu hồi voucher — chỉ tặng thêm, đúng phạm vi đã chốt trong spec.
- Không click-through xem chi tiết đơn hàng ở bảng "Đơn hàng" trong trang này — chỉ đọc tóm tắt.
- Tất cả text hiển thị qua i18n `t()`, phải có đủ 5 file locale (vi/en/zh/ja/ko), không để thiếu ngôn ngữ nào.
- Không có framework test frontend nào trong dự án này (chỉ có test backend) — verify frontend bằng Playwright thủ công ở task cuối, không tự bịa ra 1 framework mới.

Tham khảo spec đầy đủ: `docs/superpowers/specs/2026-07-27-admin-customer-detail-design.md`

---

## File Structure

**Backend (mới):**
- `BackEnd/src/main/java/com/example/backend/entity/LichSuTangDiem.java`
- `BackEnd/src/main/java/com/example/backend/repository/LichSuTangDiemRepository.java`
- `BackEnd/src/main/java/com/example/backend/response/LichSuTangDiemResponse.java`
- `BackEnd/src/main/java/com/example/backend/request/TangDiemRequest.java`
- `BackEnd/src/main/java/com/example/backend/request/TangVoucherRequest.java`
- `BackEnd/src/test/java/com/example/backend/service/KhachHangServiceTest.java`
- `BackEnd/src/test/java/com/example/backend/service/PhieuGiamGiaCaNhanServiceTest.java`

**Backend (sửa):**
- `Database/QLBanMayTinh.sql` — thêm bảng `lich_su_tang_diem`
- `BackEnd/src/main/java/com/example/backend/service/KhachHangService.java` — thêm `tangDiem()`, `layLichSuDiem()`
- `BackEnd/src/main/java/com/example/backend/controller/KhachHangController.java` — thêm 2 endpoint
- `BackEnd/src/main/java/com/example/backend/repository/LichSuQuayRepository.java` — thêm 1 query method
- `BackEnd/src/main/java/com/example/backend/response/PhieuGiamGiaCaNhanResponse.java` — thêm field `nguon`
- `BackEnd/src/main/java/com/example/backend/service/PhieuGiamGiaCaNhanService.java` — thêm `taoVoucherAdmin()`, `getByKhachHangIdForAdmin()`
- `BackEnd/src/main/java/com/example/backend/controller/PhieuGiamGiaCaNhanController.java` — thêm 2 endpoint

**Frontend (mới):**
- `FrontEnd/QLBanMayTinh/src/components/admin/TangDiemModal.vue`
- `FrontEnd/QLBanMayTinh/src/components/admin/TangVoucherModal.vue`
- `FrontEnd/QLBanMayTinh/src/components/admin/CustomerDetailPage.vue`

**Frontend (sửa):**
- `FrontEnd/QLBanMayTinh/src/Service/KhachHangService.js`
- `FrontEnd/QLBanMayTinh/src/Service/PhieuGiamGiaCaNhanService.js`
- `FrontEnd/QLBanMayTinh/src/i18n/locales/{vi,en,zh,ja,ko}.js`
- `FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue`
- `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

---

### Task 1: Bảng DB `lich_su_tang_diem`

**Files:**
- Modify: `Database/QLBanMayTinh.sql`

**Interfaces:**
- Produces: bảng `lich_su_tang_diem(id, khach_hang_id, nhan_vien_id, so_diem, ly_do, ngay_tao)` — Task 2 map entity vào bảng này.

- [ ] **Step 1: Thêm block tạo bảng idempotent**

Mở `Database/QLBanMayTinh.sql`, tìm block tạo bảng `phieu_giam_gia_ca_nhan` (tìm chuỗi `ALTER TABLE phieu_giam_gia_ca_nhan ADD don_hang_toi_thieu`). Thêm đoạn sau ngay sau `GO` kết thúc block đó (trước block `cau_hinh_vong_quay`):

```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_tang_diem')
BEGIN
    CREATE TABLE lich_su_tang_diem (
        id             INT           IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id  INT           NOT NULL,
        nhan_vien_id   INT           NOT NULL,
        so_diem        INT           NOT NULL CONSTRAINT CK_lstd_sodiem CHECK (so_diem > 0),
        ly_do          NVARCHAR(255) NULL,
        ngay_tao       DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lstd_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_lstd_nhan_vien  FOREIGN KEY (nhan_vien_id)  REFERENCES nhan_vien(nhan_vien_id)
    );
END
GO
```

- [ ] **Step 2: Chạy lại toàn bộ file SQL để tạo bảng**

Chạy lại `Database/QLBanMayTinh.sql` (đúng quy trình luôn dùng — DROP + CREATE DATABASE mới toàn bộ). Xác nhận không có lỗi cú pháp trong output.

- [ ] **Step 3: Commit**

```bash
git add "Database/QLBanMayTinh.sql"
git commit -m "feat(db): add lich_su_tang_diem table for admin point-grant ledger"
```

---

### Task 2: Entity + Repository + Response cho `LichSuTangDiem`

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/entity/LichSuTangDiem.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/LichSuTangDiemRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/LichSuTangDiemResponse.java`

**Interfaces:**
- Produces: `LichSuTangDiemRepository.findResponsesByKhachHangId(Integer): List<LichSuTangDiemResponse>` — Task 3 dùng để trả danh sách lịch sử điểm.
- Produces: `LichSuTangDiemResponse(Integer id, Integer soDiem, String lyDo, String tenNhanVien, LocalDateTime ngayTao)`.

- [ ] **Step 1: Tạo entity**

`BackEnd/src/main/java/com/example/backend/entity/LichSuTangDiem.java`:

```java
package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lich_su_tang_diem")
public class LichSuTangDiem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "so_diem", nullable = false)
    private Integer soDiem;

    @Column(name = "ly_do", length = 255)
    private String lyDo;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
```

- [ ] **Step 2: Tạo response DTO**

`BackEnd/src/main/java/com/example/backend/response/LichSuTangDiemResponse.java`:

```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LichSuTangDiemResponse {
    private Integer id;
    private Integer soDiem;
    private String lyDo;
    private String tenNhanVien;
    private LocalDateTime ngayTao;
}
```

- [ ] **Step 3: Tạo repository**

`BackEnd/src/main/java/com/example/backend/repository/LichSuTangDiemRepository.java`:

```java
package com.example.backend.repository;

import com.example.backend.entity.LichSuTangDiem;
import com.example.backend.response.LichSuTangDiemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuTangDiemRepository extends JpaRepository<LichSuTangDiem, Integer> {

    @Query("SELECT new com.example.backend.response.LichSuTangDiemResponse(" +
           "l.id, l.soDiem, l.lyDo, nv.hoTen, l.ngayTao) " +
           "FROM LichSuTangDiem l JOIN l.nhanVien nv " +
           "WHERE l.khachHang.khachHangId = :khachHangId ORDER BY l.ngayTao DESC")
    List<LichSuTangDiemResponse> findResponsesByKhachHangId(@Param("khachHangId") Integer khachHangId);
}
```

- [ ] **Step 4: Build để xác nhận compile được**

Run: `cd "BackEnd" && ./mvnw compile -q`
Expected: build thành công, không lỗi.

- [ ] **Step 5: Commit**

```bash
git add "BackEnd/src/main/java/com/example/backend/entity/LichSuTangDiem.java" \
        "BackEnd/src/main/java/com/example/backend/repository/LichSuTangDiemRepository.java" \
        "BackEnd/src/main/java/com/example/backend/response/LichSuTangDiemResponse.java"
git commit -m "feat(backend): add LichSuTangDiem entity/repository/response"
```

---

### Task 3: `KhachHangService.tangDiem()` + endpoints + test

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/TangDiemRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/KhachHangService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/KhachHangController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/KhachHangServiceTest.java`

**Interfaces:**
- Consumes: `LichSuTangDiemRepository.findResponsesByKhachHangId` (Task 2), `KhachHangRepository.findWithLockByKhachHangId` (đã có sẵn).
- Produces: `KhachHangService.tangDiem(Integer khachHangId, TangDiemRequest request): void`, `KhachHangService.layLichSuDiem(Integer khachHangId): List<LichSuTangDiemResponse>` — Task 5 (frontend service) gọi qua `POST /api/khach-hang/{id}/tang-diem` và `GET /api/khach-hang/{id}/lich-su-diem`.

- [ ] **Step 1: Viết test trước (fail vì `tangDiem` chưa tồn tại)**

`BackEnd/src/test/java/com/example/backend/service/KhachHangServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.LichSuTangDiem;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChucVuRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.LichSuTangDiemRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.TangDiemRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KhachHangServiceTest {

    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private ChucVuRepository chucVuRepository;
    @Mock private LichSuTangDiemRepository lichSuTangDiemRepository;

    @InjectMocks
    private KhachHangService service;

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
    }

    @Test
    void tangDiem_congDiemVaLuuLichSu() {
        loginAs("admin1");
        ChucVu chucVuAdmin = new ChucVu();
        chucVuAdmin.setMaChucVu("admin");
        NhanVien admin = new NhanVien();
        admin.setNhanVienId(9);
        admin.setHoTen("Admin Test");
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin1");
        tk.setChucVu(chucVuAdmin);
        tk.setNhanVien(admin);
        when(taiKhoanRepository.findByUsername("admin1")).thenReturn(Optional.of(tk));

        KhachHang khachHang = new KhachHang();
        khachHang.setKhachHangId(5);
        khachHang.setDiemTichLuy(100);
        when(khachHangRepository.findWithLockByKhachHangId(5)).thenReturn(Optional.of(khachHang));

        TangDiemRequest request = new TangDiemRequest(50, "Khách VIP");
        service.tangDiem(5, request);

        assertThat(khachHang.getDiemTichLuy()).isEqualTo(150);
        verify(khachHangRepository).save(khachHang);

        ArgumentCaptor<LichSuTangDiem> captor = ArgumentCaptor.forClass(LichSuTangDiem.class);
        verify(lichSuTangDiemRepository).save(captor.capture());
        LichSuTangDiem saved = captor.getValue();
        assertThat(saved.getSoDiem()).isEqualTo(50);
        assertThat(saved.getLyDo()).isEqualTo("Khách VIP");
        assertThat(saved.getKhachHang()).isEqualTo(khachHang);
        assertThat(saved.getNhanVien()).isEqualTo(admin);
    }

    @Test
    void tangDiem_khachHangKhongTonTai_nemLoi() {
        when(khachHangRepository.findWithLockByKhachHangId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tangDiem(99, new TangDiemRequest(10, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khách hàng không tồn tại");
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail (chưa có `TangDiemRequest`/`tangDiem`)**

Run: `cd "BackEnd" && ./mvnw test -Dtest=KhachHangServiceTest -q`
Expected: FAIL — lỗi biên dịch (không tìm thấy `TangDiemRequest`, `KhachHangService.tangDiem`).

- [ ] **Step 3: Tạo `TangDiemRequest`**

`BackEnd/src/main/java/com/example/backend/request/TangDiemRequest.java`:

```java
package com.example.backend.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TangDiemRequest {
    @NotNull(message = "Số điểm không được để trống")
    @Positive(message = "Số điểm phải lớn hơn 0")
    private Integer soDiem;

    private String lyDo;
}
```

- [ ] **Step 4: Thêm `tangDiem()` và `layLichSuDiem()` vào `KhachHangService`**

Trong `BackEnd/src/main/java/com/example/backend/service/KhachHangService.java`, thêm import:

```java
import com.example.backend.entity.LichSuTangDiem;
import com.example.backend.entity.NhanVien;
import com.example.backend.repository.LichSuTangDiemRepository;
import com.example.backend.request.TangDiemRequest;
import com.example.backend.response.LichSuTangDiemResponse;
```

Thêm field (cạnh các `@Autowired` khác):

```java
    @Autowired
    private LichSuTangDiemRepository lichSuTangDiemRepository;
```

Thêm method (cạnh `update()`):

```java
    // Tặng điểm — chỉ admin gọi (@PreAuthorize ở controller). Khóa ghi khách hàng đúng
    // pattern PhieuGiamGiaCaNhanService.doiThuong() để tránh 2 request tặng điểm đồng thời
    // đọc trùng số dư rồi cùng cộng (mất 1 lần cộng).
    @Transactional
    public void tangDiem(Integer khachHangId, TangDiemRequest request) {
        KhachHang khachHang = khachHangRepository.findWithLockByKhachHangId(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + khachHangId));
        khachHang.setDiemTichLuy(khachHang.getDiemTichLuy() + request.getSoDiem());
        khachHangRepository.save(khachHang);

        LichSuTangDiem lichSu = new LichSuTangDiem();
        lichSu.setKhachHang(khachHang);
        lichSu.setNhanVien(currentNhanVien());
        lichSu.setSoDiem(request.getSoDiem());
        lichSu.setLyDo(request.getLyDo());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSuTangDiemRepository.save(lichSu);
    }

    public List<LichSuTangDiemResponse> layLichSuDiem(Integer khachHangId) {
        return lichSuTangDiemRepository.findResponsesByKhachHangId(khachHangId);
    }

    private NhanVien currentNhanVien() {
        TaiKhoan tk = currentAccount();
        if (tk == null || tk.getNhanVien() == null)
            throw new AccessDeniedException("Chỉ nhân viên mới thực hiện được thao tác này");
        return tk.getNhanVien();
    }
```

- [ ] **Step 5: Chạy lại test, xác nhận pass**

Run: `cd "BackEnd" && ./mvnw test -Dtest=KhachHangServiceTest -q`
Expected: PASS — 2/2 test xanh.

- [ ] **Step 6: Thêm 2 endpoint vào `KhachHangController`**

Thêm import:

```java
import com.example.backend.request.TangDiemRequest;
import com.example.backend.response.LichSuTangDiemResponse;
```

Thêm method (cạnh `update()`):

```java
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/tang-diem")
    public ResponseEntity<Void> tangDiem(@PathVariable Integer id, @Valid @RequestBody TangDiemRequest request) {
        khachHangService.tangDiem(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/lich-su-diem")
    public List<LichSuTangDiemResponse> getLichSuDiem(@PathVariable Integer id) {
        return khachHangService.layLichSuDiem(id);
    }
```

- [ ] **Step 7: Build toàn bộ để chắc controller compile được**

Run: `cd "BackEnd" && ./mvnw compile -q`
Expected: build thành công.

- [ ] **Step 8: Commit**

```bash
git add "BackEnd/src/main/java/com/example/backend/request/TangDiemRequest.java" \
        "BackEnd/src/main/java/com/example/backend/service/KhachHangService.java" \
        "BackEnd/src/main/java/com/example/backend/controller/KhachHangController.java" \
        "BackEnd/src/test/java/com/example/backend/service/KhachHangServiceTest.java"
git commit -m "feat(backend): admin point-gifting endpoint + ledger read + tests"
```

---

### Task 4: Tặng voucher admin + danh sách voucher theo khách (kèm nguồn) + test

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/TangVoucherRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/LichSuQuayRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/response/PhieuGiamGiaCaNhanResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/PhieuGiamGiaCaNhanService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/PhieuGiamGiaCaNhanController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/PhieuGiamGiaCaNhanServiceTest.java`

**Interfaces:**
- Produces: `PhieuGiamGiaCaNhanService.taoVoucherAdmin(Integer khachHangId, TangVoucherRequest): PhieuGiamGiaCaNhan`, `PhieuGiamGiaCaNhanService.getByKhachHangIdForAdmin(Integer): List<PhieuGiamGiaCaNhanResponse>` (mỗi phần tử có thêm field `nguon`: `"Khách tự đổi / trúng thưởng"` hoặc `"Admin tặng"`) — Task 5 gọi qua `POST /api/phieu-giam-gia-ca-nhan/tang/{khachHangId}` và `GET /api/phieu-giam-gia-ca-nhan/khach-hang/{id}`.

- [ ] **Step 1: Viết test trước (fail vì chưa có `TangVoucherRequest`/`taoVoucherAdmin`)**

`BackEnd/src/test/java/com/example/backend/service/PhieuGiamGiaCaNhanServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.TangVoucherRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhieuGiamGiaCaNhanServiceTest {

    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private DmDoiThuongRepository dmDoiThuongRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private LichSuQuayRepository lichSuQuayRepository;

    @InjectMocks
    private PhieuGiamGiaCaNhanService service;

    @Test
    void taoVoucherAdmin_tao_thanhCong() {
        KhachHang khachHang = new KhachHang();
        khachHang.setKhachHangId(5);
        when(khachHangRepository.findById(5)).thenReturn(Optional.of(khachHang));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TangVoucherRequest request = new TangVoucherRequest(
                "percent", BigDecimal.valueOf(10), BigDecimal.valueOf(500000),
                LocalDateTime.now().plusDays(30), null);

        PhieuGiamGiaCaNhan result = service.taoVoucherAdmin(5, request);

        assertThat(result.getKhachHang()).isEqualTo(khachHang);
        assertThat(result.getDoiThuong()).isNull();
        assertThat(result.getDaSuDung()).isFalse();
        assertThat(result.getLoai()).isEqualTo("percent");
    }

    @Test
    void taoVoucherAdmin_phanTramVuot100_nemLoi() {
        TangVoucherRequest request = new TangVoucherRequest(
                "percent", BigDecimal.valueOf(150), null, LocalDateTime.now().plusDays(30), null);

        assertThatThrownBy(() -> service.taoVoucherAdmin(5, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được vượt quá 100%");
    }

    @Test
    void taoVoucherAdmin_hanSuDungDaQua_nemLoi() {
        TangVoucherRequest request = new TangVoucherRequest(
                "fixed", BigDecimal.valueOf(50000), null, LocalDateTime.now().minusDays(1), null);

        assertThatThrownBy(() -> service.taoVoucherAdmin(5, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tương lai");
    }

    @Test
    void getByKhachHangIdForAdmin_ganNhanNguonDung() {
        PhieuGiamGiaCaNhan tuDoiThuong = new PhieuGiamGiaCaNhan();
        tuDoiThuong.setPhieuId(1);
        tuDoiThuong.setDoiThuong(new DmDoiThuong());
        tuDoiThuong.setLoai("fixed");
        tuDoiThuong.setGiaTri(BigDecimal.TEN);
        tuDoiThuong.setDaSuDung(false);
        tuDoiThuong.setNgayDoi(LocalDateTime.now());
        tuDoiThuong.setNgayHetHan(LocalDateTime.now().plusDays(10));

        PhieuGiamGiaCaNhan adminTang = new PhieuGiamGiaCaNhan();
        adminTang.setPhieuId(2);
        adminTang.setLoai("percent");
        adminTang.setGiaTri(BigDecimal.TEN);
        adminTang.setDaSuDung(false);
        adminTang.setNgayDoi(LocalDateTime.now());
        adminTang.setNgayHetHan(LocalDateTime.now().plusDays(10));

        when(phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(5))
                .thenReturn(List.of(tuDoiThuong, adminTang));
        when(lichSuQuayRepository.findPhieuIdsByKhachHangId(5)).thenReturn(List.of());

        var result = service.getByKhachHangIdForAdmin(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNguon()).isEqualTo("Khách tự đổi / trúng thưởng");
        assertThat(result.get(1).getNguon()).isEqualTo("Admin tặng");
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `cd "BackEnd" && ./mvnw test -Dtest=PhieuGiamGiaCaNhanServiceTest -q`
Expected: FAIL — lỗi biên dịch (thiếu `TangVoucherRequest`, `taoVoucherAdmin`, `getByKhachHangIdForAdmin`, `findPhieuIdsByKhachHangId`, `getNguon()`).

- [ ] **Step 3: Tạo `TangVoucherRequest`**

`BackEnd/src/main/java/com/example/backend/request/TangVoucherRequest.java`:

```java
package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TangVoucherRequest {
    @NotBlank(message = "Loại voucher không được để trống")
    private String loai;

    @NotNull(message = "Giá trị không được để trống")
    @Positive(message = "Giá trị phải lớn hơn 0")
    private BigDecimal giaTri;

    private BigDecimal giaTriToiDa;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDateTime ngayHetHan;

    private BigDecimal donHangToiThieu;
}
```

- [ ] **Step 4: Thêm query method vào `LichSuQuayRepository`**

Thêm vào `BackEnd/src/main/java/com/example/backend/repository/LichSuQuayRepository.java` (trong interface, cạnh `findResponsesByKhachHangId`):

```java
    // Danh sách phieuId đã trúng qua vòng quay của 1 khách — dùng để suy ra cột "Nguồn"
    // trong danh sách voucher admin xem (không thêm cột DB mới).
    @Query("SELECT l.phieuGiamGiaCaNhan.phieuId FROM LichSuQuay l " +
           "WHERE l.khachHang.khachHangId = :khachHangId AND l.phieuGiamGiaCaNhan IS NOT NULL")
    List<Integer> findPhieuIdsByKhachHangId(@Param("khachHangId") Integer khachHangId);
```

- [ ] **Step 5: Thêm field `nguon` vào `PhieuGiamGiaCaNhanResponse`**

`BackEnd/src/main/java/com/example/backend/response/PhieuGiamGiaCaNhanResponse.java` — thêm field cuối class:

```java
    private String nguon;
```

(File đầy đủ sau khi sửa — chỉ thêm dòng cuối, các field/annotation khác giữ nguyên.)

- [ ] **Step 6: Thêm `taoVoucherAdmin()` + `getByKhachHangIdForAdmin()` vào `PhieuGiamGiaCaNhanService`, sửa `getCuaToi()`**

Thêm import:

```java
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.request.TangVoucherRequest;

import java.math.BigDecimal;
```

Thêm field:

```java
    @Autowired
    private LichSuQuayRepository lichSuQuayRepository;
```

Thêm method (cạnh `doiThuong()`):

```java
    // Admin tặng voucher trực tiếp — không qua đổi điểm, doiThuong=null (giống hệt cách
    // voucher trúng vòng quay cũng để doiThuong=null, xem VongQuayService.quay()).
    @Transactional
    public PhieuGiamGiaCaNhan taoVoucherAdmin(Integer khachHangId, TangVoucherRequest request) {
        if ("percent".equals(request.getLoai()) && request.getGiaTri().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Voucher giảm theo % không được vượt quá 100%");
        if (!request.getNgayHetHan().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Hạn sử dụng phải ở tương lai");

        KhachHang khachHang = khachHangRepository.findById(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại với id: " + khachHangId));

        PhieuGiamGiaCaNhan phieu = new PhieuGiamGiaCaNhan();
        phieu.setKhachHang(khachHang);
        phieu.setLoai(request.getLoai());
        phieu.setGiaTri(request.getGiaTri());
        phieu.setGiaTriToiDa(request.getGiaTriToiDa());
        phieu.setDaSuDung(false);
        phieu.setNgayDoi(LocalDateTime.now());
        phieu.setNgayHetHan(request.getNgayHetHan());
        phieu.setDonHangToiThieu(request.getDonHangToiThieu());
        return phieuGiamGiaCaNhanRepository.save(phieu);
    }

    // Admin xem toàn bộ voucher của 1 khách — khác getCuaToi() (tự phục vụ, suy khách hàng
    // từ SecurityContextHolder) vì admin cần xem CỦA NGƯỜI KHÁC theo id truyền vào.
    public List<PhieuGiamGiaCaNhanResponse> getByKhachHangIdForAdmin(Integer khachHangId) {
        List<Integer> phieuIdTrungThuong = lichSuQuayRepository.findPhieuIdsByKhachHangId(khachHangId);
        return phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(khachHangId).stream()
                .map(p -> new PhieuGiamGiaCaNhanResponse(
                        p.getPhieuId(), p.getMaPhieu(), p.getLoai(), p.getGiaTri(), p.getGiaTriToiDa(),
                        p.getDaSuDung(), p.getNgayDoi(), p.getNgayHetHan(), p.getDonHangToiThieu(),
                        p.getDoiThuong() != null || phieuIdTrungThuong.contains(p.getPhieuId())
                                ? "Khách tự đổi / trúng thưởng" : "Admin tặng"))
                .toList();
    }
```

Sửa `getCuaToi()` — thêm `null` vào cuối lời gọi constructor (field `nguon` mới thêm ở Task 4 Step 5 không tính ở endpoint tự phục vụ này):

```java
    public List<PhieuGiamGiaCaNhanResponse> getCuaToi() {
        KhachHang khachHang = currentKhachHang();
        return phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(khachHang.getKhachHangId()).stream()
                .map(p -> new PhieuGiamGiaCaNhanResponse(
                        p.getPhieuId(), p.getMaPhieu(), p.getLoai(), p.getGiaTri(), p.getGiaTriToiDa(),
                        p.getDaSuDung(), p.getNgayDoi(), p.getNgayHetHan(), p.getDonHangToiThieu(), null))
                .toList();
    }
```

- [ ] **Step 7: Chạy lại test, xác nhận pass**

Run: `cd "BackEnd" && ./mvnw test -Dtest=PhieuGiamGiaCaNhanServiceTest -q`
Expected: PASS — 4/4 test xanh.

- [ ] **Step 8: Thêm 2 endpoint vào `PhieuGiamGiaCaNhanController`**

Thêm import (`java.util.List` đã có sẵn trong file này từ trước — KHÔNG thêm lại, sẽ gây lỗi trùng import):

```java
import com.example.backend.request.TangVoucherRequest;
import jakarta.validation.Valid;
```

Thêm method (cạnh `getCuaToi()`):

```java
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("tang/{khachHangId}")
    public ResponseEntity<PhieuGiamGiaCaNhan> taoVoucherAdmin(@PathVariable Integer khachHangId,
                                                                @Valid @RequestBody TangVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuGiamGiaCaNhanService.taoVoucherAdmin(khachHangId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("khach-hang/{id}")
    public List<PhieuGiamGiaCaNhanResponse> getByKhachHang(@PathVariable Integer id) {
        return phieuGiamGiaCaNhanService.getByKhachHangIdForAdmin(id);
    }
```

- [ ] **Step 9: Build toàn bộ**

Run: `cd "BackEnd" && ./mvnw compile -q`
Expected: build thành công.

- [ ] **Step 10: Commit**

```bash
git add "BackEnd/src/main/java/com/example/backend/request/TangVoucherRequest.java" \
        "BackEnd/src/main/java/com/example/backend/repository/LichSuQuayRepository.java" \
        "BackEnd/src/main/java/com/example/backend/response/PhieuGiamGiaCaNhanResponse.java" \
        "BackEnd/src/main/java/com/example/backend/service/PhieuGiamGiaCaNhanService.java" \
        "BackEnd/src/main/java/com/example/backend/controller/PhieuGiamGiaCaNhanController.java" \
        "BackEnd/src/test/java/com/example/backend/service/PhieuGiamGiaCaNhanServiceTest.java"
git commit -m "feat(backend): admin voucher gifting + per-customer voucher list with source label"
```

---

### Task 5: Frontend service layer

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/Service/KhachHangService.js`
- Modify: `FrontEnd/QLBanMayTinh/src/Service/PhieuGiamGiaCaNhanService.js`

**Interfaces:**
- Consumes: `POST /api/khach-hang/{id}/tang-diem`, `GET /api/khach-hang/{id}/lich-su-diem`, `POST /api/phieu-giam-gia-ca-nhan/tang/{khachHangId}`, `GET /api/phieu-giam-gia-ca-nhan/khach-hang/{id}` (Task 3, 4).
- Produces: `KhachHangService.tangDiem(id, body): Promise<Response>`, `KhachHangService.getLichSuDiem(id): Promise<Array>`, `PhieuGiamGiaCaNhanService.taoVoucherAdmin(khachHangId, body): Promise<Response>`, `PhieuGiamGiaCaNhanService.getByKhachHang(khachHangId): Promise<Array>` — Task 7/9 (modal) và Task 9 (trang chi tiết) dùng các hàm này.

- [ ] **Step 1: Thêm hàm vào `KhachHangService.js`**

Thêm cuối file `FrontEnd/QLBanMayTinh/src/Service/KhachHangService.js`:

```js
// Admin tặng điểm cho 1 khách hàng — body: { soDiem, lyDo }
export const tangDiem = (id, body) => post(`/api/khach-hang/${id}/tang-diem`, body);

// Admin xem lịch sử tặng điểm của 1 khách hàng
export const getLichSuDiem = (id) => get(`/api/khach-hang/${id}/lich-su-diem`);
```

- [ ] **Step 2: Thêm hàm vào `PhieuGiamGiaCaNhanService.js`**

Thêm cuối file `FrontEnd/QLBanMayTinh/src/Service/PhieuGiamGiaCaNhanService.js`:

```js
// Admin tặng voucher trực tiếp cho 1 khách hàng — body: { loai, giaTri, giaTriToiDa, ngayHetHan, donHangToiThieu }
export const taoVoucherAdmin = (khachHangId, body) => post(`/api/phieu-giam-gia-ca-nhan/tang/${khachHangId}`, body);

// Admin xem toàn bộ voucher/điểm thưởng của 1 khách hàng
export const getByKhachHang = (khachHangId) => get(`/api/phieu-giam-gia-ca-nhan/khach-hang/${khachHangId}`);
```

- [ ] **Step 3: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/Service/KhachHangService.js" \
        "FrontEnd/QLBanMayTinh/src/Service/PhieuGiamGiaCaNhanService.js"
git commit -m "feat(frontend): service layer for admin point/voucher gifting"
```

---

### Task 6: i18n — thêm key cho cả 5 ngôn ngữ

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`

**Interfaces:**
- Produces: các key `admin.pageMeta.customerDetail.*`, `admin.customers.viewDetail`, `admin.customerDetail.*`, `admin.giftPointsModal.*`, `admin.giftVoucherModal.*` — Task 7, 8, 9 gọi qua `t('...')`.

- [ ] **Step 1: Thêm key vào `vi.js`**

Trong `admin.pageMeta` (cạnh key `customers`, khoảng dòng 499), thêm:

```js
      customerDetail: { title: "Chi tiết khách hàng", sub: "Thông tin, lịch sử mua hàng, ưu đãi" },
```

Trong `admin.customers` (cạnh key `edit`, khoảng dòng 815), thêm:

```js
      viewDetail: "Chi tiết",
```

Sau block `customerModal` (sau dòng 843, trước `suppliers`), thêm 3 namespace mới:

```js
    customerDetail: {
      notFound: "Không tìm thấy khách hàng",
      back: "← Quay lại danh sách khách hàng",
      edit: "Sửa thông tin",
      giftPoints: "🎁 Tặng điểm",
      giftVoucher: "🎟️ Tặng voucher",
      kpiTotalSpent: "Tổng chi tiêu",
      kpiOrderCount: "Số đơn",
      kpiPoints: "Điểm hiện có",
      kpiLastOrder: "Đơn gần nhất",
      noLastOrder: "Chưa có đơn nào",
      ordersTitle: "Đơn hàng",
      ordersEmpty: "Khách hàng chưa có đơn hàng nào",
      colOrderCode: "Mã đơn",
      colOrderDate: "Ngày đặt",
      colOrderTotal: "Tổng tiền",
      colOrderStatus: "Trạng thái",
      vouchersTitle: "Phiếu giảm giá / điểm thưởng",
      vouchersEmpty: "Khách hàng chưa có phiếu giảm giá nào",
      colVoucherCode: "Mã phiếu",
      colVoucherType: "Loại",
      colVoucherValue: "Giá trị",
      colVoucherSource: "Nguồn",
      colVoucherStatus: "Trạng thái",
      colVoucherExpiry: "Hạn dùng",
      voucherSourceRedeemed: "Khách tự đổi / trúng thưởng",
      voucherSourceGifted: "Admin tặng",
      voucherStatusUsed: "Đã dùng",
      voucherStatusExpired: "Hết hạn",
      voucherStatusActive: "Còn hạn",
      pointHistoryTitle: "Lịch sử tặng điểm",
      pointHistoryEmpty: "Chưa tặng điểm lần nào",
      colPointAmount: "Số điểm",
      colPointReason: "Lý do",
      colPointBy: "Người tặng",
      colPointDate: "Ngày tặng",
      typePercent: "Giảm %",
      typeFixed: "Giảm tiền mặt",
    },

    giftPointsModal: {
      title: "Tặng điểm cho khách hàng",
      amountLabel: "Số điểm *",
      reasonLabel: "Lý do (không bắt buộc)",
      cancel: "Hủy",
      submit: "Tặng điểm",
      amountRequired: "Vui lòng nhập số điểm lớn hơn 0",
      success: "Đã tặng {points} điểm cho khách hàng",
    },

    giftVoucherModal: {
      title: "Tặng voucher cho khách hàng",
      typeLabel: "Loại giảm giá *",
      valueLabelPercent: "Giá trị (%) *",
      valueLabelFixed: "Giá trị (₫) *",
      maxDiscountLabel: "Giảm tối đa (không bắt buộc)",
      minOrderLabel: "Đơn tối thiểu (không bắt buộc)",
      expiryLabel: "Hạn sử dụng *",
      cancel: "Hủy",
      submit: "Tặng voucher",
      valueRequired: "Vui lòng nhập giá trị lớn hơn 0",
      percentMax100: "Giá trị % không được vượt quá 100",
      expiryRequired: "Vui lòng chọn hạn sử dụng ở tương lai",
      success: "Đã tặng voucher cho khách hàng",
    },
```

- [ ] **Step 2: Thêm key tương ứng vào `en.js`**

Cùng vị trí nesting (`admin.pageMeta.customerDetail`, `admin.customers.viewDetail`, `admin.customerDetail`, `admin.giftPointsModal`, `admin.giftVoucherModal`):

```js
      customerDetail: { title: "Customer Detail", sub: "Info, purchase history, perks" },
```
```js
      viewDetail: "Detail",
```
```js
    customerDetail: {
      notFound: "Customer not found",
      back: "← Back to customer list",
      edit: "Edit info",
      giftPoints: "🎁 Gift points",
      giftVoucher: "🎟️ Gift voucher",
      kpiTotalSpent: "Total spent",
      kpiOrderCount: "Order count",
      kpiPoints: "Current points",
      kpiLastOrder: "Last order",
      noLastOrder: "No orders yet",
      ordersTitle: "Orders",
      ordersEmpty: "This customer has no orders yet",
      colOrderCode: "Order code",
      colOrderDate: "Order date",
      colOrderTotal: "Total",
      colOrderStatus: "Status",
      vouchersTitle: "Vouchers / Reward points",
      vouchersEmpty: "This customer has no vouchers yet",
      colVoucherCode: "Voucher code",
      colVoucherType: "Type",
      colVoucherValue: "Value",
      colVoucherSource: "Source",
      colVoucherStatus: "Status",
      colVoucherExpiry: "Expiry",
      voucherSourceRedeemed: "Self-redeemed / won",
      voucherSourceGifted: "Gifted by admin",
      voucherStatusUsed: "Used",
      voucherStatusExpired: "Expired",
      voucherStatusActive: "Active",
      pointHistoryTitle: "Point-gift history",
      pointHistoryEmpty: "No points gifted yet",
      colPointAmount: "Points",
      colPointReason: "Reason",
      colPointBy: "Gifted by",
      colPointDate: "Date",
      typePercent: "Percent off",
      typeFixed: "Fixed amount off",
    },

    giftPointsModal: {
      title: "Gift points to customer",
      amountLabel: "Points *",
      reasonLabel: "Reason (optional)",
      cancel: "Cancel",
      submit: "Gift points",
      amountRequired: "Please enter a number of points greater than 0",
      success: "Gifted {points} points to the customer",
    },

    giftVoucherModal: {
      title: "Gift voucher to customer",
      typeLabel: "Discount type *",
      valueLabelPercent: "Value (%) *",
      valueLabelFixed: "Value (₫) *",
      maxDiscountLabel: "Max discount (optional)",
      minOrderLabel: "Minimum order (optional)",
      expiryLabel: "Expiry date *",
      cancel: "Cancel",
      submit: "Gift voucher",
      valueRequired: "Please enter a value greater than 0",
      percentMax100: "Percent value cannot exceed 100",
      expiryRequired: "Please pick an expiry date in the future",
      success: "Voucher gifted to the customer",
    },
```

- [ ] **Step 3: Thêm key tương ứng vào `zh.js`**

```js
      customerDetail: { title: "客户详情", sub: "信息、购买记录、优惠" },
```
```js
      viewDetail: "详情",
```
```js
    customerDetail: {
      notFound: "未找到该客户",
      back: "← 返回客户列表",
      edit: "编辑信息",
      giftPoints: "🎁 赠送积分",
      giftVoucher: "🎟️ 赠送优惠券",
      kpiTotalSpent: "累计消费",
      kpiOrderCount: "订单数",
      kpiPoints: "当前积分",
      kpiLastOrder: "最近一次下单",
      noLastOrder: "暂无订单",
      ordersTitle: "订单",
      ordersEmpty: "该客户暂无订单",
      colOrderCode: "订单编号",
      colOrderDate: "下单日期",
      colOrderTotal: "总金额",
      colOrderStatus: "状态",
      vouchersTitle: "优惠券 / 积分奖励",
      vouchersEmpty: "该客户暂无优惠券",
      colVoucherCode: "优惠券编号",
      colVoucherType: "类型",
      colVoucherValue: "面值",
      colVoucherSource: "来源",
      colVoucherStatus: "状态",
      colVoucherExpiry: "有效期",
      voucherSourceRedeemed: "客户自行兑换/抽中",
      voucherSourceGifted: "管理员赠送",
      voucherStatusUsed: "已使用",
      voucherStatusExpired: "已过期",
      voucherStatusActive: "有效",
      pointHistoryTitle: "赠送积分记录",
      pointHistoryEmpty: "尚未赠送过积分",
      colPointAmount: "积分",
      colPointReason: "原因",
      colPointBy: "赠送人",
      colPointDate: "日期",
      typePercent: "百分比折扣",
      typeFixed: "固定金额折扣",
    },

    giftPointsModal: {
      title: "赠送积分给客户",
      amountLabel: "积分数 *",
      reasonLabel: "原因（可选）",
      cancel: "取消",
      submit: "赠送积分",
      amountRequired: "请输入大于 0 的积分数",
      success: "已赠送 {points} 积分给该客户",
    },

    giftVoucherModal: {
      title: "赠送优惠券给客户",
      typeLabel: "折扣类型 *",
      valueLabelPercent: "面值（%）*",
      valueLabelFixed: "面值（₫）*",
      maxDiscountLabel: "最高优惠金额（可选）",
      minOrderLabel: "最低订单金额（可选）",
      expiryLabel: "有效期 *",
      cancel: "取消",
      submit: "赠送优惠券",
      valueRequired: "请输入大于 0 的数值",
      percentMax100: "百分比数值不能超过 100",
      expiryRequired: "请选择未来的有效期日期",
      success: "已赠送优惠券给该客户",
    },
```

- [ ] **Step 4: Thêm key tương ứng vào `ja.js`**

```js
      customerDetail: { title: "顧客詳細", sub: "情報・購入履歴・特典" },
```
```js
      viewDetail: "詳細",
```
```js
    customerDetail: {
      notFound: "顧客が見つかりません",
      back: "← 顧客一覧に戻る",
      edit: "情報を編集",
      giftPoints: "🎁 ポイントを付与",
      giftVoucher: "🎟️ クーポンを付与",
      kpiTotalSpent: "累計購入金額",
      kpiOrderCount: "注文数",
      kpiPoints: "現在のポイント",
      kpiLastOrder: "最新の注文",
      noLastOrder: "まだ注文がありません",
      ordersTitle: "注文履歴",
      ordersEmpty: "この顧客の注文はまだありません",
      colOrderCode: "注文番号",
      colOrderDate: "注文日",
      colOrderTotal: "合計金額",
      colOrderStatus: "状態",
      vouchersTitle: "クーポン / ポイント特典",
      vouchersEmpty: "この顧客のクーポンはまだありません",
      colVoucherCode: "クーポン番号",
      colVoucherType: "種類",
      colVoucherValue: "割引額",
      colVoucherSource: "取得方法",
      colVoucherStatus: "状態",
      colVoucherExpiry: "有効期限",
      voucherSourceRedeemed: "顧客自身が交換/当選",
      voucherSourceGifted: "管理者が付与",
      voucherStatusUsed: "使用済み",
      voucherStatusExpired: "期限切れ",
      voucherStatusActive: "有効",
      pointHistoryTitle: "ポイント付与履歴",
      pointHistoryEmpty: "まだポイントを付与していません",
      colPointAmount: "ポイント数",
      colPointReason: "理由",
      colPointBy: "付与者",
      colPointDate: "付与日",
      typePercent: "割引率(%)",
      typeFixed: "固定額割引",
    },

    giftPointsModal: {
      title: "顧客にポイントを付与",
      amountLabel: "ポイント数 *",
      reasonLabel: "理由（任意）",
      cancel: "キャンセル",
      submit: "ポイントを付与",
      amountRequired: "0より大きいポイント数を入力してください",
      success: "{points}ポイントを顧客に付与しました",
    },

    giftVoucherModal: {
      title: "顧客にクーポンを付与",
      typeLabel: "割引タイプ *",
      valueLabelPercent: "割引率（%）*",
      valueLabelFixed: "割引額（₫）*",
      maxDiscountLabel: "最大割引額（任意）",
      minOrderLabel: "最低注文金額（任意）",
      expiryLabel: "有効期限 *",
      cancel: "キャンセル",
      submit: "クーポンを付与",
      valueRequired: "0より大きい値を入力してください",
      percentMax100: "割引率は100を超えられません",
      expiryRequired: "未来の有効期限を選択してください",
      success: "クーポンを顧客に付与しました",
    },
```

- [ ] **Step 5: Thêm key tương ứng vào `ko.js`**

```js
      customerDetail: { title: "고객 상세", sub: "정보, 구매 내역, 혜택" },
```
```js
      viewDetail: "상세보기",
```
```js
    customerDetail: {
      notFound: "고객을 찾을 수 없습니다",
      back: "← 고객 목록으로 돌아가기",
      edit: "정보 수정",
      giftPoints: "🎁 포인트 지급",
      giftVoucher: "🎟️ 쿠폰 지급",
      kpiTotalSpent: "총 구매 금액",
      kpiOrderCount: "주문 수",
      kpiPoints: "현재 포인트",
      kpiLastOrder: "최근 주문",
      noLastOrder: "아직 주문이 없습니다",
      ordersTitle: "주문 내역",
      ordersEmpty: "이 고객은 아직 주문이 없습니다",
      colOrderCode: "주문 번호",
      colOrderDate: "주문일",
      colOrderTotal: "총 금액",
      colOrderStatus: "상태",
      vouchersTitle: "쿠폰 / 포인트 리워드",
      vouchersEmpty: "이 고객은 아직 쿠폰이 없습니다",
      colVoucherCode: "쿠폰 번호",
      colVoucherType: "유형",
      colVoucherValue: "값",
      colVoucherSource: "출처",
      colVoucherStatus: "상태",
      colVoucherExpiry: "유효기간",
      voucherSourceRedeemed: "고객이 직접 교환/당첨",
      voucherSourceGifted: "관리자 지급",
      voucherStatusUsed: "사용됨",
      voucherStatusExpired: "만료됨",
      voucherStatusActive: "사용 가능",
      pointHistoryTitle: "포인트 지급 내역",
      pointHistoryEmpty: "아직 지급된 포인트가 없습니다",
      colPointAmount: "포인트",
      colPointReason: "사유",
      colPointBy: "지급자",
      colPointDate: "날짜",
      typePercent: "퍼센트 할인",
      typeFixed: "정액 할인",
    },

    giftPointsModal: {
      title: "고객에게 포인트 지급",
      amountLabel: "포인트 *",
      reasonLabel: "사유 (선택)",
      cancel: "취소",
      submit: "포인트 지급",
      amountRequired: "0보다 큰 포인트를 입력해 주세요",
      success: "고객에게 {points} 포인트를 지급했습니다",
    },

    giftVoucherModal: {
      title: "고객에게 쿠폰 지급",
      typeLabel: "할인 유형 *",
      valueLabelPercent: "값 (%) *",
      valueLabelFixed: "값 (₫) *",
      maxDiscountLabel: "최대 할인 금액 (선택)",
      minOrderLabel: "최소 주문 금액 (선택)",
      expiryLabel: "유효기간 *",
      cancel: "취소",
      submit: "쿠폰 지급",
      valueRequired: "0보다 큰 값을 입력해 주세요",
      percentMax100: "퍼센트 값은 100을 초과할 수 없습니다",
      expiryRequired: "미래 날짜의 유효기간을 선택해 주세요",
      success: "고객에게 쿠폰을 지급했습니다",
    },
```

- [ ] **Step 6: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/en.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js"
git commit -m "feat(i18n): add customer detail / gift points / gift voucher keys (5 locales)"
```

---

### Task 7: `TangDiemModal.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/TangDiemModal.vue`

**Interfaces:**
- Consumes: `KhachHangService.tangDiem(id, body)` (Task 5), i18n key `admin.giftPointsModal.*` (Task 6).
- Produces: component nhận prop `modelValue: Boolean`, `customerId: Number`; emit `update:modelValue`, `gifted` — Task 9 (`CustomerDetailPage.vue`) dùng.

- [ ] **Step 1: Tạo component**

`FrontEnd/QLBanMayTinh/src/components/admin/TangDiemModal.vue`:

```vue
<script setup>
import { ref } from "vue";
import { t } from "../../i18n/index.js";
import * as KhachHangService from "../../Service/KhachHangService.js";
import { showToast } from "../../stores/toast.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["update:modelValue", "gifted"]);

const soDiem = ref(null);
const lyDo = ref("");
const error = ref("");
const saving = ref(false);

const close = () => {
  emit("update:modelValue", false);
  soDiem.value = null;
  lyDo.value = "";
  error.value = "";
};

const submit = async () => {
  error.value = "";
  if (!soDiem.value || soDiem.value <= 0) {
    error.value = t("admin.giftPointsModal.amountRequired");
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const res = await KhachHangService.tangDiem(props.customerId, {
      soDiem: Number(soDiem.value),
      lyDo: lyDo.value || null,
    });
    if (!res.ok) {
      error.value = await res.text();
      return;
    }
    showToast(t("admin.giftPointsModal.success", { points: soDiem.value }), "success");
    emit("gifted");
    close();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div v-if="modelValue" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="close">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:420px;max-width:95vw;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.giftPointsModal.title') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="close"></button>
      </div>
      <div class="p-4">
        <div v-if="error" class="alert alert-danger small py-2 mb-3">{{ error }}</div>
        <div class="mb-3">
          <label class="form-label small text-secondary">{{ t('admin.giftPointsModal.amountLabel') }}</label>
          <input v-model.number="soDiem" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
        </div>
        <div>
          <label class="form-label small text-secondary">{{ t('admin.giftPointsModal.reasonLabel') }}</label>
          <input v-model="lyDo" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="close">{{ t('admin.giftPointsModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="submit">{{ t('admin.giftPointsModal.submit') }}</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/components/admin/TangDiemModal.vue"
git commit -m "feat(frontend): add TangDiemModal component"
```

---

### Task 8: `TangVoucherModal.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/TangVoucherModal.vue`

**Interfaces:**
- Consumes: `PhieuGiamGiaCaNhanService.taoVoucherAdmin(khachHangId, body)` (Task 5), i18n key `admin.giftVoucherModal.*`, `admin.customerDetail.typePercent`/`typeFixed` (Task 6).
- Produces: component nhận prop `modelValue: Boolean`, `customerId: Number`; emit `update:modelValue`, `gifted` — Task 9 dùng.

- [ ] **Step 1: Tạo component**

`FrontEnd/QLBanMayTinh/src/components/admin/TangVoucherModal.vue`:

```vue
<script setup>
import { ref, computed } from "vue";
import { t } from "../../i18n/index.js";
import * as PhieuGiamGiaCaNhanService from "../../Service/PhieuGiamGiaCaNhanService.js";
import { showToast } from "../../stores/toast.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["update:modelValue", "gifted"]);

const emptyForm = () => ({
  loai: "percent",
  giaTri: null,
  giaTriToiDa: null,
  ngayHetHan: "",
  donHangToiThieu: null,
});
const form = ref(emptyForm());
const error = ref("");
const saving = ref(false);

const valueLabel = computed(() =>
  form.value.loai === "percent"
    ? t("admin.giftVoucherModal.valueLabelPercent")
    : t("admin.giftVoucherModal.valueLabelFixed"),
);

const close = () => {
  emit("update:modelValue", false);
  form.value = emptyForm();
  error.value = "";
};

const submit = async () => {
  error.value = "";
  if (!form.value.giaTri || form.value.giaTri <= 0) {
    error.value = t("admin.giftVoucherModal.valueRequired");
    return;
  }
  if (form.value.loai === "percent" && form.value.giaTri > 100) {
    error.value = t("admin.giftVoucherModal.percentMax100");
    return;
  }
  if (!form.value.ngayHetHan || new Date(form.value.ngayHetHan) <= new Date()) {
    error.value = t("admin.giftVoucherModal.expiryRequired");
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    const body = {
      loai: form.value.loai,
      giaTri: Number(form.value.giaTri),
      giaTriToiDa: form.value.giaTriToiDa ? Number(form.value.giaTriToiDa) : null,
      ngayHetHan: new Date(form.value.ngayHetHan).toISOString(),
      donHangToiThieu: form.value.donHangToiThieu ? Number(form.value.donHangToiThieu) : null,
    };
    const res = await PhieuGiamGiaCaNhanService.taoVoucherAdmin(props.customerId, body);
    if (!res.ok) {
      error.value = await res.text();
      return;
    }
    showToast(t("admin.giftVoucherModal.success"), "success");
    emit("gifted");
    close();
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div v-if="modelValue" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="close">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card);border:1px solid var(--border-color-strong);width:480px;max-width:95vw;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('admin.giftVoucherModal.title') }}</span>
        <button class="btn-close btn-sm" :aria-label="t('common.close')" @click="close"></button>
      </div>
      <div class="p-4">
        <div v-if="error" class="alert alert-danger small py-2 mb-3">{{ error }}</div>
        <div class="row g-3">
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.typeLabel') }}</label>
            <select v-model="form.loai" class="form-select form-select-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)">
              <option value="percent">{{ t('admin.customerDetail.typePercent') }}</option>
              <option value="fixed">{{ t('admin.customerDetail.typeFixed') }}</option>
            </select>
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ valueLabel }}</label>
            <input v-model.number="form.giaTri" type="number" min="1" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.maxDiscountLabel') }}</label>
            <input v-model.number="form.giaTriToiDa" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-6">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.minOrderLabel') }}</label>
            <input v-model.number="form.donHangToiThieu" type="number" min="0" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
          <div class="col-12">
            <label class="form-label small text-secondary">{{ t('admin.giftVoucherModal.expiryLabel') }}</label>
            <input v-model="form.ngayHetHan" type="date" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" />
          </div>
        </div>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="close">{{ t('admin.giftVoucherModal.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="saving" @click="submit">{{ t('admin.giftVoucherModal.submit') }}</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/components/admin/TangVoucherModal.vue"
git commit -m "feat(frontend): add TangVoucherModal component"
```

---

### Task 9: `CustomerDetailPage.vue` + điểm truy cập trong `CustomersTable.vue`/`AdminPage.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/CustomerDetailPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `TangDiemModal.vue` (Task 7), `TangVoucherModal.vue` (Task 8), `PhieuGiamGiaCaNhanService.getByKhachHang`/`KhachHangService.getLichSuDiem` (Task 5), `CustomerFormModal.vue` (đã có sẵn), `CustomersStore`/`OrdersStore` (đã có sẵn), `orderStatusLabel`/`orderStatusColor`/`formatPrice`/`formatDate`/`formatDateTime`/`statusLabel` (đã có sẵn).
- Produces: component `CustomerDetailPage.vue` nhận prop `customerId: Number`, emit `back` — dùng trong `AdminPage.vue` section `currentPage === 'customer-detail'`.

- [ ] **Step 1: Tạo `CustomerDetailPage.vue`**

`FrontEnd/QLBanMayTinh/src/components/admin/CustomerDetailPage.vue`:

```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { CustomersStore } from "../../stores/customers.js";
import { OrdersStore } from "../../stores/orders.js";
import * as PhieuGiamGiaCaNhanService from "../../Service/PhieuGiamGiaCaNhanService.js";
import * as KhachHangService from "../../Service/KhachHangService.js";
import { formatPrice, formatDate, formatDateTime, statusLabel } from "../../utils/adminFormat.js";
import { orderStatusLabel, orderStatusColor } from "../../utils/orderStatus.js";
import CustomerFormModal from "./CustomerFormModal.vue";
import TangDiemModal from "./TangDiemModal.vue";
import TangVoucherModal from "./TangVoucherModal.vue";

const props = defineProps({
  customerId: { type: Number, required: true },
});
const emit = defineEmits(["back"]);

const customer = computed(() =>
  CustomersStore.items.find((c) => c.khachHangId === props.customerId) ?? null,
);

const customerOrders = computed(() =>
  OrdersStore.items
    .filter((o) => o.khachHangId === props.customerId)
    .slice()
    .sort((a, b) => new Date(b.ngayDat) - new Date(a.ngayDat)),
);

const totalSpent = computed(() =>
  customerOrders.value
    .filter((o) => o.trangThaiDonHang !== "cancelled")
    .reduce((sum, o) => sum + (o.thanhTien || 0), 0),
);

const vouchers = ref([]);
const vouchersLoading = ref(true);
const pointHistory = ref([]);
const pointHistoryLoading = ref(true);

const loadVouchers = async () => {
  vouchersLoading.value = true;
  try {
    vouchers.value = await PhieuGiamGiaCaNhanService.getByKhachHang(props.customerId);
  } catch {
    vouchers.value = [];
  } finally {
    vouchersLoading.value = false;
  }
};

const loadPointHistory = async () => {
  pointHistoryLoading.value = true;
  try {
    pointHistory.value = await KhachHangService.getLichSuDiem(props.customerId);
  } catch {
    pointHistory.value = [];
  } finally {
    pointHistoryLoading.value = false;
  }
};

onMounted(() => {
  loadVouchers();
  loadPointHistory();
});

const voucherStatus = (v) => {
  if (v.daSuDung) return { key: "voucherStatusUsed", cls: "bg-secondary" };
  if (new Date(v.ngayHetHan) < new Date()) return { key: "voucherStatusExpired", cls: "bg-danger" };
  return { key: "voucherStatusActive", cls: "bg-success" };
};

const customerFormModalRef = ref(null);
const showCustomerModal = ref(false);
const showGiftPointsModal = ref(false);
const showGiftVoucherModal = ref(false);
</script>

<template>
  <div v-if="!customer" class="text-secondary small">{{ t('admin.customerDetail.notFound') }}</div>
  <div v-else>
    <button class="btn btn-sm btn-outline-secondary mb-3" @click="emit('back')">{{ t('admin.customerDetail.back') }}</button>

    <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-4">
      <div>
        <div class="fw-black fs-4" style="color:var(--text-heading);">{{ customer.hoTen }}</div>
        <div class="text-secondary small">{{ customer.soDienThoai }} · {{ customer.email || '—' }}</div>
        <span class="badge mt-1" :class="customer.trangThai==='active'?'bg-success':'bg-secondary'">{{ statusLabel(customer.trangThai) }}</span>
      </div>
      <div class="d-flex gap-2 flex-wrap">
        <button class="btn btn-sm btn-outline-warning" @click="customerFormModalRef.openForEdit(customer)">{{ t('admin.customerDetail.edit') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="showGiftPointsModal = true">{{ t('admin.customerDetail.giftPoints') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="showGiftVoucherModal = true">{{ t('admin.customerDetail.giftVoucher') }}</button>
      </div>
    </div>

    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiTotalSpent') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ formatPrice(totalSpent) }}</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiOrderCount') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ customerOrders.length }}</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiPoints') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ customer.diemTichLuy ?? 0 }}</div>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="rounded-4 p-3" style="background:var(--bg-card); border:1px solid var(--border-color-soft);">
          <div class="text-secondary small mb-1">{{ t('admin.customerDetail.kpiLastOrder') }}</div>
          <div class="fw-bold" style="font-size:1.2rem;">{{ customerOrders[0] ? formatDate(customerOrders[0].ngayDat) : t('admin.customerDetail.noLastOrder') }}</div>
        </div>
      </div>
    </div>

    <div class="mb-4">
      <div class="fw-bold mb-2">{{ t('admin.customerDetail.ordersTitle') }}</div>
      <div v-if="customerOrders.length === 0" class="text-secondary small">{{ t('admin.customerDetail.ordersEmpty') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th>{{ t('admin.customerDetail.colOrderCode') }}</th>
            <th>{{ t('admin.customerDetail.colOrderDate') }}</th>
            <th>{{ t('admin.customerDetail.colOrderTotal') }}</th>
            <th>{{ t('admin.customerDetail.colOrderStatus') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="o in customerOrders" :key="o.donHangId">
              <td>{{ o.maDonHang }}</td>
              <td class="text-secondary">{{ formatDate(o.ngayDat) }}</td>
              <td>{{ formatPrice(o.thanhTien) }}</td>
              <td><span class="badge" :style="{background: orderStatusColor(o.trangThaiDonHang).bg, color: orderStatusColor(o.trangThaiDonHang).text}">{{ orderStatusLabel(o.trangThaiDonHang) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mb-4">
      <div class="fw-bold mb-2">{{ t('admin.customerDetail.vouchersTitle') }}</div>
      <div v-if="vouchersLoading" class="text-secondary small">{{ t('admin.customers.loading') }}</div>
      <div v-else-if="vouchers.length === 0" class="text-secondary small">{{ t('admin.customerDetail.vouchersEmpty') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th>{{ t('admin.customerDetail.colVoucherCode') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherType') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherValue') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherSource') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherStatus') }}</th>
            <th>{{ t('admin.customerDetail.colVoucherExpiry') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="v in vouchers" :key="v.phieuId">
              <td>{{ v.maPhieu }}</td>
              <td>{{ v.loai === 'percent' ? t('admin.customerDetail.typePercent') : t('admin.customerDetail.typeFixed') }}</td>
              <td>{{ v.loai === 'percent' ? `${v.giaTri}%` : formatPrice(v.giaTri) }}</td>
              <td class="text-secondary">{{ v.nguon === 'Admin tặng' ? t('admin.customerDetail.voucherSourceGifted') : t('admin.customerDetail.voucherSourceRedeemed') }}</td>
              <td><span class="badge" :class="voucherStatus(v).cls">{{ t(`admin.customerDetail.${voucherStatus(v).key}`) }}</span></td>
              <td class="text-secondary">{{ formatDate(v.ngayHetHan) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mb-4">
      <div class="fw-bold mb-2">{{ t('admin.customerDetail.pointHistoryTitle') }}</div>
      <div v-if="pointHistoryLoading" class="text-secondary small">{{ t('admin.customers.loading') }}</div>
      <div v-else-if="pointHistory.length === 0" class="text-secondary small">{{ t('admin.customerDetail.pointHistoryEmpty') }}</div>
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
          <thead><tr>
            <th>{{ t('admin.customerDetail.colPointAmount') }}</th>
            <th>{{ t('admin.customerDetail.colPointReason') }}</th>
            <th>{{ t('admin.customerDetail.colPointBy') }}</th>
            <th>{{ t('admin.customerDetail.colPointDate') }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in pointHistory" :key="p.id">
              <td class="fw-bold text-warning">+{{ p.soDiem }}</td>
              <td class="text-secondary">{{ p.lyDo || '—' }}</td>
              <td class="text-secondary">{{ p.tenNhanVien }}</td>
              <td class="text-secondary">{{ formatDateTime(p.ngayTao) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <CustomerFormModal ref="customerFormModalRef" v-model="showCustomerModal" />
    <TangDiemModal v-model="showGiftPointsModal" :customer-id="customerId" @gifted="loadPointHistory" />
    <TangVoucherModal v-model="showGiftVoucherModal" :customer-id="customerId" @gifted="loadVouchers" />
  </div>
</template>
```

- [ ] **Step 2: Thêm nút "Chi tiết" vào `CustomersTable.vue`**

Trong `FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue`, thêm emit:

```js
const emit = defineEmits(["view-detail"]);
```

Trong cột thao tác (dòng có nút Sửa/Xóa), thêm nút trước nút Sửa:

```html
              <button class="btn btn-sm btn-outline-primary" style="font-size:0.78rem; padding:2px 8px;" @click="emit('view-detail', c.khachHangId)">{{ t('admin.customers.viewDetail') }}</button>
```

- [ ] **Step 3: Wiring trong `AdminPage.vue`**

Thêm import (cạnh `import CustomersTable ...`):

```js
import CustomerDetailPage from "../components/admin/CustomerDetailPage.vue";
```

Thêm state (cạnh `const currentPage = ref("dashboard");`):

```js
const selectedCustomerId = ref(null);
const openCustomerDetail = (id) => {
  selectedCustomerId.value = id;
  currentPage.value = "customer-detail";
};
```

Thêm entry vào `PAGE_META` (cạnh `customers:`):

```js
  "customer-detail": { titleKey: "admin.pageMeta.customerDetail.title", subKey: "admin.pageMeta.customerDetail.sub", icon: "👤" },
```

Sửa section `customers` (thêm listener) và thêm section mới ngay sau:

```html
        <section v-show="currentPage === 'customers'">
          <CustomersTable @view-detail="openCustomerDetail" />
        </section>

        <!-- ── Chi tiet khach hang ── -->
        <section v-show="currentPage === 'customer-detail'">
          <CustomerDetailPage v-if="selectedCustomerId" :customer-id="selectedCustomerId" @back="currentPage = 'customers'" />
        </section>
```

- [ ] **Step 4: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/components/admin/CustomerDetailPage.vue" \
        "FrontEnd/QLBanMayTinh/src/components/admin/CustomersTable.vue" \
        "FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue"
git commit -m "feat(frontend): add admin customer detail page with entry point"
```

---

### Task 10: Verify sống bằng Playwright (thủ công — dự án không có test frontend tự động)

**Files:** không tạo/sửa file — chỉ chạy app thật và quan sát.

- [ ] **Step 1: Khởi động backend + frontend**

Đảm bảo backend (Spring Boot, cổng 8080) và frontend (`npm run dev`, cổng 5173) đang chạy. Nếu chưa, khởi động cả 2.

- [ ] **Step 2: Đăng nhập admin, mở trang Khách hàng**

Mở `http://localhost:5173`, đăng nhập tài khoản role admin, vào tab "Khách hàng" trong Admin.

- [ ] **Step 3: Mở chi tiết 1 khách hàng**

Bấm nút "Chi tiết" ở 1 dòng bất kỳ. Xác nhận: trang chuyển sang view chi tiết, tiêu đề topbar đổi thành "Chi tiết khách hàng", hiện đúng tên/SĐT/email/trạng thái, 4 thẻ KPI có số liệu hợp lý (khớp với dữ liệu đơn hàng của khách đó), bảng "Đơn hàng" liệt kê đúng các đơn của khách này (đối chiếu nhanh với tab Đơn hàng lọc theo tên), bảng "Phiếu giảm giá/điểm thưởng" và "Lịch sử tặng điểm" hiện đúng trạng thái rỗng nếu khách chưa có gì.

- [ ] **Step 4: Tặng điểm**

Bấm "🎁 Tặng điểm", nhập số điểm (vd 100) + lý do, bấm "Tặng điểm". Xác nhận: toast thành công hiện ra, thẻ KPI "Điểm hiện có" tăng đúng số vừa tặng (reload lại bảng Khách hàng để xác nhận `diemTichLuy` đã cộng ở DB), bảng "Lịch sử tặng điểm" có thêm 1 dòng mới đúng số điểm/lý do/tên admin.

- [ ] **Step 5: Tặng voucher**

Bấm "🎟️ Tặng voucher", chọn loại "Giảm %", nhập giá trị 15, giảm tối đa 200000, hạn dùng 1 ngày trong tương lai, bấm "Tặng voucher". Xác nhận: toast thành công, bảng "Phiếu giảm giá/điểm thưởng" có thêm 1 dòng mới, cột Nguồn = "Admin tặng", cột Trạng thái = "Còn hạn".

- [ ] **Step 6: Kiểm tra validate lỗi**

Thử tặng voucher loại "Giảm %" với giá trị 150 → xác nhận hiện lỗi "Giá trị % không được vượt quá 100" ngay trong modal, không đóng modal, không gọi API thành công (không có voucher rác được tạo).

- [ ] **Step 7: Kiểm tra phân quyền**

Đăng xuất, đăng nhập bằng tài khoản role `nhan_vien` (nhân viên bán hàng). Xác nhận: tài khoản này vào `#staff` (StaffPage.vue), không có đường nào vào được `AdminPage.vue`/trang chi tiết khách hàng này — đúng như đã chốt "chỉ Admin".

- [ ] **Step 8: Dọn dữ liệu test (nếu cần)**

Nếu các bước trên tạo dữ liệu test không mong muốn (điểm/voucher tặng thử), ghi chú lại cho người dùng biết — không tự xóa dữ liệu DB nếu không được yêu cầu.

---

## Self-Review Notes

- **Spec coverage:** đối chiếu từng mục trong spec — điều hướng (Task 9), bố cục trang + KPI + đơn hàng + voucher/điểm + lịch sử tặng điểm (Task 9), backend ledger (Task 1-3), backend voucher admin (Task 4), phân quyền admin-only (Task 3, 4, xác minh thủ công Task 10 Step 7), validate lỗi (Task 4, 8, xác minh Task 10 Step 6) — đủ, không thiếu mục nào.
- **Placeholder scan:** không còn "TBD"/"TODO" nào trong plan — mọi step đều có code/lệnh cụ thể.
- **Type consistency:** `TangDiemRequest(Integer soDiem, String lyDo)`, `TangVoucherRequest(String loai, BigDecimal giaTri, BigDecimal giaTriToiDa, LocalDateTime ngayHetHan, BigDecimal donHangToiThieu)`, `LichSuTangDiemResponse(Integer id, Integer soDiem, String lyDo, String tenNhanVien, LocalDateTime ngayTao)` dùng nhất quán giữa Task 2/3/4 (định nghĩa) và Task 5/7/8/9 (tiêu thụ ở frontend service + modal).
