# Tích điểm mua hàng & Đổi điểm lấy voucher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tự động cộng điểm khi đơn giao thành công (10.000đ = 1 điểm), admin quản lý danh mục phần thưởng đổi điểm, khách hàng tự đổi điểm lấy voucher cá nhân (hạn 30 ngày) và dùng được ở checkout.

**Architecture:** Cộng điểm qua DB trigger (không đụng code Java, giống cơ chế mã vận đơn/lịch sử đơn hàng đã có). Danh mục đổi thưởng (`dm_doi_thuong`) là CRUD admin đơn giản, mirror y hệt `khuyen_mai` đã có. Voucher cá nhân (`phieu_giam_gia_ca_nhan`) snapshot dữ liệu tại thời điểm đổi, tách biệt hoàn toàn khỏi `khuyen_mai` — checkout hiện 2 khối chọn giảm giá riêng, khách chọn 1 trong 2.

**Tech Stack:** Spring Boot + JPA (Java), SQL Server (T-SQL trigger), Vue 3 `<script setup>`.

## Global Constraints

- Mọi thay đổi `Database/QLBanMayTinh.sql` phải idempotent (`IF NOT EXISTS` / `CREATE OR ALTER`).
- Không hoàn/trừ điểm khi đơn bị trả hàng sau đó (ngoài phạm vi).
- `phieu_giam_gia_ca_nhan` snapshot `loai`/`gia_tri`/`gia_tri_toi_da` tại thời điểm đổi — không tham chiếu sống tới `dm_doi_thuong`.
- Không cho dùng đồng thời mã khuyến mãi công khai + voucher cá nhân trong 1 đơn.
- Không sửa `KhuyenMaiController` (đang mở hoàn toàn không khóa quyền) — lỗ hổng có sẵn, ngoài phạm vi.
- Endpoint đổi thưởng/xem voucher của tôi tự suy khách hàng qua `SecurityContextHolder`, không nhận `khachHangId` từ client.

Spec đầy đủ: `docs/superpowers/specs/2026-07-21-tich-diem-doi-voucher-design.md`.

---

### Task 1: Database schema — trigger cộng điểm + 2 bảng mới

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (thêm vào cuối file)

**Interfaces:**
- Produces: `don_hang.da_cong_diem BIT`; trigger `trg_don_hang_cong_diem`; bảng `dm_doi_thuong`; bảng `phieu_giam_gia_ca_nhan`.

- [ ] **Step 1: Thêm khối SQL vào cuối `Database/QLBanMayTinh.sql`**

```sql
-- ============================================================
--  Tích điểm mua hàng & Đổi điểm lấy voucher
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('don_hang') AND name = 'da_cong_diem')
BEGIN
    ALTER TABLE don_hang ADD da_cong_diem BIT NOT NULL DEFAULT 0;
END
GO

CREATE OR ALTER TRIGGER trg_don_hang_cong_diem
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(trang_thai_don_hang)
    BEGIN
        UPDATE kh
        SET kh.diem_tich_luy = kh.diem_tich_luy + FLOOR(i.thanh_tien / 10000)
        FROM khach_hang kh
        JOIN inserted i ON i.khach_hang_id = kh.khach_hang_id
        JOIN deleted d ON d.don_hang_id = i.don_hang_id
        WHERE i.trang_thai_don_hang = N'delivered'
          AND d.trang_thai_don_hang <> N'delivered'
          AND i.da_cong_diem = 0;

        UPDATE don_hang SET da_cong_diem = 1
        WHERE don_hang_id IN (
            SELECT i.don_hang_id FROM inserted i JOIN deleted d ON d.don_hang_id = i.don_hang_id
            WHERE i.trang_thai_don_hang = N'delivered' AND d.trang_thai_don_hang <> N'delivered' AND i.da_cong_diem = 0
        );
    END
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'dm_doi_thuong')
BEGIN
    CREATE TABLE dm_doi_thuong (
        doi_thuong_id   INT            IDENTITY(1,1) PRIMARY KEY,
        ten             NVARCHAR(150)  NOT NULL,
        mo_ta           NVARCHAR(500)  NULL,
        diem_can        INT            NOT NULL CONSTRAINT CK_ddt_diemcan CHECK (diem_can > 0),
        loai            NVARCHAR(20)   NOT NULL CONSTRAINT CK_ddt_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri         DECIMAL(18,0)  NOT NULL CONSTRAINT CK_ddt_giatri CHECK (gia_tri > 0),
        CONSTRAINT CK_ddt_percent_max100 CHECK (loai <> N'percent' OR gia_tri <= 100),
        gia_tri_toi_da  DECIMAL(18,0)  NULL,
        trang_thai      NVARCHAR(20)   NOT NULL DEFAULT N'active'
            CONSTRAINT CK_ddt_trangthai CHECK (trang_thai IN (N'active', N'inactive')),
        ngay_tao        DATETIME       NOT NULL DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_giam_gia_ca_nhan')
BEGIN
    CREATE TABLE phieu_giam_gia_ca_nhan (
        phieu_id       INT            IDENTITY(1,1) PRIMARY KEY,
        khach_hang_id  INT            NOT NULL,
        doi_thuong_id  INT            NULL,
        ma_phieu       VARCHAR(50)    NOT NULL UNIQUE DEFAULT UPPER(LEFT(REPLACE(CAST(NEWID() AS VARCHAR(36)), '-', ''), 12)),
        loai           NVARCHAR(20)   NOT NULL CONSTRAINT CK_pggcn_loai CHECK (loai IN (N'percent', N'fixed')),
        gia_tri        DECIMAL(18,0)  NOT NULL,
        gia_tri_toi_da DECIMAL(18,0)  NULL,
        da_su_dung     BIT            NOT NULL DEFAULT 0,
        ngay_doi       DATETIME       NOT NULL DEFAULT GETDATE(),
        ngay_het_han   DATETIME       NOT NULL,
        don_hang_id    INT            NULL,
        CONSTRAINT FK_pggcn_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
        CONSTRAINT FK_pggcn_doi_thuong FOREIGN KEY (doi_thuong_id) REFERENCES dm_doi_thuong(doi_thuong_id),
        CONSTRAINT FK_pggcn_don_hang   FOREIGN KEY (don_hang_id)   REFERENCES don_hang(don_hang_id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_pggcn_khach_hang')
    CREATE INDEX IX_pggcn_khach_hang ON phieu_giam_gia_ca_nhan(khach_hang_id, da_su_dung);
GO
```

- [ ] **Step 2: Chạy lại toàn bộ file `QLBanMayTinh.sql` trên DB dev**

Đọc kỹ: file này **DROP + tạo lại toàn bộ database** ở đầu (dòng 9-17) — mọi dữ liệu hiện tại (đơn hàng, khách hàng đã test tay) sẽ bị xóa và seed lại từ đầu. Đây là hành vi CHỦ Ý, đã xác nhận với người dùng trong phiên trước — không phải lỗi. Chạy qua PowerShell đọc file đúng UTF-8 (không dùng `Invoke-SqlCmd -InputFile` trực tiếp — từng gây lỗi mojibake do thiếu xử lý encoding):

```powershell
$sql = [System.IO.File]::ReadAllText("Database/QLBanMayTinh.sql", [System.Text.Encoding]::UTF8)
Invoke-SqlCmd -Query $sql -ServerInstance "localhost,1433" -Database "master" -QueryTimeout 0
```

- [ ] **Step 3: Verify bằng tay — trigger cộng điểm hoạt động**

```sql
SELECT diem_tich_luy FROM khach_hang WHERE khach_hang_id = 1;  -- ghi lại giá trị trước
SELECT don_hang_id, thanh_tien, trang_thai_don_hang FROM don_hang WHERE khach_hang_id = 1 AND trang_thai_don_hang <> 'delivered';
UPDATE don_hang SET trang_thai_don_hang = 'delivered' WHERE don_hang_id = <id lấy ở trên>;
SELECT diem_tich_luy FROM khach_hang WHERE khach_hang_id = 1;  -- phải tăng đúng FLOOR(thanh_tien/10000)
UPDATE don_hang SET trang_thai_don_hang = 'confirmed' WHERE don_hang_id = <cùng id>;
UPDATE don_hang SET trang_thai_don_hang = 'delivered' WHERE don_hang_id = <cùng id>;
SELECT diem_tich_luy FROM khach_hang WHERE khach_hang_id = 1;  -- KHÔNG được tăng lần 2 (da_cong_diem chặn)
```

- [ ] **Step 4: Verify re-run idempotent — chạy lại toàn bộ file lần 2 (cùng cách Step 2), không lỗi**

- [ ] **Step 5: Commit**

```bash
git add "Database/QLBanMayTinh.sql"
git commit -m "feat: add loyalty points trigger and voucher redemption tables"
```

---

### Task 2: Backend — CRUD danh mục đổi thưởng (admin)

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/entity/DmDoiThuong.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/DmDoiThuongRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/DmDoiThuongResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/DmDoiThuongRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/DmDoiThuongService.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/DmDoiThuongController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/DmDoiThuongServiceTest.java`

**Interfaces:**
- Produces: `GET /api/dm-doi-thuong` (mở, mọi người đăng nhập xem được để chọn đổi), `POST`/`PUT update/{id}`/`DELETE delete/{id}` (khóa staff).

Mirror chính xác `KhuyenMai*` (entity/request/response/repository/service/controller) đã có sẵn — đọc `BackEnd/src/main/java/com/example/backend/{entity,request,response,repository,service,controller}/KhuyenMai*.java` trước khi viết để bám đúng style, chỉ đổi tên field/bảng.

- [ ] **Step 1: Tạo entity `DmDoiThuong`**

```java
package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dm_doi_thuong")
public class DmDoiThuong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doi_thuong_id")
    private Integer doiThuongId;

    @Column(name = "ten", length = 150)
    private String ten;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "diem_can", nullable = false)
    private Integer diemCan;

    @Column(name = "loai", length = 20)
    private String loai;

    @Column(name = "gia_tri", precision = 18, scale = 0)
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da", precision = 18, scale = 0)
    private BigDecimal giaTriToiDa;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}
```

- [ ] **Step 2: Tạo `DmDoiThuongRequest`**

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

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DmDoiThuongRequest {
    @NotBlank(message = "Tên phần thưởng không được để trống")
    private String ten;

    private String moTa;

    @NotNull(message = "Điểm cần đổi không được để trống")
    @Positive(message = "Điểm cần đổi phải lớn hơn 0")
    private Integer diemCan;

    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private String trangThai;
}
```

- [ ] **Step 3: Tạo `DmDoiThuongResponse`**

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
public class DmDoiThuongResponse {
    private Integer doiThuongId;
    private String ten;
    private String moTa;
    private Integer diemCan;
    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private String trangThai;
    private LocalDateTime ngayTao;
}
```

- [ ] **Step 4: Tạo `DmDoiThuongRepository`**

```java
package com.example.backend.repository;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.response.DmDoiThuongResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DmDoiThuongRepository extends JpaRepository<DmDoiThuong, Integer> {
    @Query("SELECT new com.example.backend.response.DmDoiThuongResponse(d.doiThuongId, d.ten, d.moTa, d.diemCan, d.loai, d.giaTri, d.giaTriToiDa, d.trangThai, d.ngayTao) FROM DmDoiThuong d")
    List<DmDoiThuongResponse> hienThiDmDoiThuong();
}
```

- [ ] **Step 5: Viết test trước cho `DmDoiThuongService`**

```java
package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.request.DmDoiThuongRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmDoiThuongServiceTest {

    @Mock private DmDoiThuongRepository dmDoiThuongRepository;

    @InjectMocks
    private DmDoiThuongService service;

    @Test
    void create_khoiTaoNgayTao() {
        when(dmDoiThuongRepository.save(org.mockito.ArgumentMatchers.any(DmDoiThuong.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DmDoiThuongRequest req = new DmDoiThuongRequest("Giảm 50k", "Mô tả", 500, "fixed", BigDecimal.valueOf(50_000), null, "active");
        DmDoiThuong saved = service.create(req);

        assertThat(saved.getNgayTao()).isNotNull();
        assertThat(saved.getDiemCan()).isEqualTo(500);
    }

    @Test
    void update_khongDoiNgayTao() {
        DmDoiThuong existing = new DmDoiThuong();
        existing.setDoiThuongId(1);
        existing.setNgayTao(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        when(dmDoiThuongRepository.findById(1)).thenReturn(Optional.of(existing));
        when(dmDoiThuongRepository.save(existing)).thenReturn(existing);

        DmDoiThuongRequest req = new DmDoiThuongRequest("Giảm 70k", null, 700, "fixed", BigDecimal.valueOf(70_000), null, "active");
        service.update(1, req);

        assertThat(existing.getNgayTao()).isEqualTo(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(existing.getDiemCan()).isEqualTo(700);
    }
}
```

- [ ] **Step 6: Chạy test, xác nhận FAIL**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test -Dtest=DmDoiThuongServiceTest`
Expected: FAIL biên dịch — `DmDoiThuongService` chưa tồn tại.

- [ ] **Step 7: Tạo `DmDoiThuongService`**

```java
package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.request.DmDoiThuongRequest;
import com.example.backend.response.DmDoiThuongResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DmDoiThuongService {

    @Autowired
    private DmDoiThuongRepository dmDoiThuongRepository;

    public List<DmDoiThuongResponse> hienThiDmDoiThuong() {
        return dmDoiThuongRepository.hienThiDmDoiThuong();
    }

    public DmDoiThuong getById(Integer id) {
        return dmDoiThuongRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phần thưởng không tồn tại với id: " + id));
    }

    public DmDoiThuong create(DmDoiThuongRequest request) {
        DmDoiThuong entity = new DmDoiThuong();
        BeanUtils.copyProperties(request, entity);
        entity.setNgayTao(LocalDateTime.now());
        return dmDoiThuongRepository.save(entity);
    }

    public DmDoiThuong update(Integer id, DmDoiThuongRequest request) {
        DmDoiThuong entity = getById(id);
        BeanUtils.copyProperties(request, entity, "doiThuongId", "ngayTao");
        return dmDoiThuongRepository.save(entity);
    }

    public void delete(Integer id) {
        if (!dmDoiThuongRepository.existsById(id))
            throw new IllegalArgumentException("Phần thưởng không tồn tại với id: " + id);
        dmDoiThuongRepository.deleteById(id);
    }
}
```

- [ ] **Step 8: Chạy lại test, xác nhận PASS**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test -Dtest=DmDoiThuongServiceTest`
Expected: PASS.

- [ ] **Step 9: Tạo `DmDoiThuongController`**

```java
package com.example.backend.controller;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.request.DmDoiThuongRequest;
import com.example.backend.response.DmDoiThuongResponse;
import com.example.backend.service.DmDoiThuongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dm-doi-thuong")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class DmDoiThuongController {

    @Autowired
    private DmDoiThuongService dmDoiThuongService;

    // Giữ mở — khách hàng cần xem danh mục để chọn đổi (AccountPage.vue).
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<DmDoiThuongResponse> getAll() {
        return dmDoiThuongService.hienThiDmDoiThuong();
    }

    @GetMapping("/{id}")
    public DmDoiThuong getById(@PathVariable Integer id) {
        return dmDoiThuongService.getById(id);
    }

    @PostMapping
    public ResponseEntity<DmDoiThuong> create(@Valid @RequestBody DmDoiThuongRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dmDoiThuongService.create(request));
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DmDoiThuongRequest request) {
        dmDoiThuongService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        dmDoiThuongService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 10: Chạy toàn bộ test suite backend**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test`
Expected: BUILD SUCCESS.

- [ ] **Step 11: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/DmDoiThuong.java \
        BackEnd/src/main/java/com/example/backend/request/DmDoiThuongRequest.java \
        BackEnd/src/main/java/com/example/backend/response/DmDoiThuongResponse.java \
        BackEnd/src/main/java/com/example/backend/repository/DmDoiThuongRepository.java \
        BackEnd/src/main/java/com/example/backend/service/DmDoiThuongService.java \
        BackEnd/src/main/java/com/example/backend/controller/DmDoiThuongController.java \
        BackEnd/src/test/java/com/example/backend/service/DmDoiThuongServiceTest.java
git commit -m "feat: add admin CRUD for redemption catalog"
```

---

### Task 3: Backend — đổi điểm lấy voucher + tiêu voucher lúc đặt hàng

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/entity/PhieuGiamGiaCaNhan.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/PhieuGiamGiaCaNhanResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/PhieuGiamGiaCaNhanRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/PhieuGiamGiaCaNhanService.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/PhieuGiamGiaCaNhanController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/request/DonHangRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/DonHangService.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/PhieuGiamGiaCaNhanServiceTest.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java`

**Interfaces:**
- Consumes: `KhachHang.getDiemTichLuy()`/`setDiemTichLuy()`, `DmDoiThuongRepository` (Task 2).
- Produces: `POST /api/phieu-giam-gia-ca-nhan/doi-thuong/{doiThuongId}` (201 + `PhieuGiamGiaCaNhan`); `GET /api/phieu-giam-gia-ca-nhan/cua-toi` (`List<PhieuGiamGiaCaNhanResponse>`); `DonHangRequest.phieuGiamGiaCaNhanId` (Integer, nullable).

- [ ] **Step 1: Tạo entity `PhieuGiamGiaCaNhan`**

```java
package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "phieu_giam_gia_ca_nhan")
public class PhieuGiamGiaCaNhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phieu_id")
    private Integer phieuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doi_thuong_id")
    private DmDoiThuong doiThuong;

    // Sinh tự động bởi DB (UPPER(LEFT(NEWID()...))) → chỉ đọc, không ghi qua JPA
    @Column(name = "ma_phieu", length = 50, insertable = false, updatable = false)
    private String maPhieu;

    @Column(name = "loai", length = 20)
    private String loai;

    @Column(name = "gia_tri", precision = 18, scale = 0)
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da", precision = 18, scale = 0)
    private BigDecimal giaTriToiDa;

    @Column(name = "da_su_dung", nullable = false)
    private Boolean daSuDung;

    @Column(name = "ngay_doi", nullable = false)
    private LocalDateTime ngayDoi;

    @Column(name = "ngay_het_han", nullable = false)
    private LocalDateTime ngayHetHan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id")
    private DonHang donHang;
}
```

- [ ] **Step 2: Tạo `PhieuGiamGiaCaNhanResponse`**

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
public class PhieuGiamGiaCaNhanResponse {
    private Integer phieuId;
    private String maPhieu;
    private String loai;
    private BigDecimal giaTri;
    private BigDecimal giaTriToiDa;
    private Boolean daSuDung;
    private LocalDateTime ngayDoi;
    private LocalDateTime ngayHetHan;
}
```

- [ ] **Step 3: Tạo `PhieuGiamGiaCaNhanRepository`**

```java
package com.example.backend.repository;

import com.example.backend.entity.PhieuGiamGiaCaNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuGiamGiaCaNhanRepository extends JpaRepository<PhieuGiamGiaCaNhan, Integer> {
    List<PhieuGiamGiaCaNhan> findByKhachHang_KhachHangId(Integer khachHangId);
    Optional<PhieuGiamGiaCaNhan> findByMaPhieu(String maPhieu);
}
```

- [ ] **Step 4: Viết test trước cho `PhieuGiamGiaCaNhanService.doiThuong()`**

Dùng đúng pattern mock `SecurityContextHolder` đã có ở `LichSuDonHangServiceTest.java`/`PhieuTraHangServiceTest.java` (đọc 1 trong 2 file đó trước khi viết).

```java
package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.DmDoiThuong;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhieuGiamGiaCaNhanServiceTest {

    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private DmDoiThuongRepository dmDoiThuongRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private PhieuGiamGiaCaNhanService service;

    @BeforeEach
    void setUp() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username, Integer khachHangId) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        kh.setDiemTichLuy(1000);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        when(taiKhoanRepository.findByUsername(username)).thenReturn(Optional.of(tk));
        when(khachHangRepository.findById(khachHangId)).thenReturn(Optional.of(kh));
    }

    private DmDoiThuong doiThuongActive(Integer id, Integer diemCan) {
        DmDoiThuong dt = new DmDoiThuong();
        dt.setDoiThuongId(id);
        dt.setDiemCan(diemCan);
        dt.setLoai("fixed");
        dt.setGiaTri(java.math.BigDecimal.valueOf(50_000));
        dt.setTrangThai("active");
        return dt;
    }

    @Test
    void doiThuong_duDiem_truDiemVaTaoPhieu() {
        loginAs("khach1", 42);
        when(dmDoiThuongRepository.findById(5)).thenReturn(Optional.of(doiThuongActive(5, 500)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class))).thenAnswer(inv -> inv.getArgument(0));

        PhieuGiamGiaCaNhan saved = service.doiThuong(5);

        assertThat(saved.getKhachHang().getDiemTichLuy()).isEqualTo(500); // 1000 - 500
        assertThat(saved.getDaSuDung()).isFalse();
        assertThat(saved.getLoai()).isEqualTo("fixed");
        assertThat(saved.getGiaTri()).isEqualByComparingTo(java.math.BigDecimal.valueOf(50_000));
        verify(khachHangRepository).save(any(KhachHang.class));
    }

    @Test
    void doiThuong_khongDuDiem_biChan() {
        loginAs("khach1", 42);
        when(dmDoiThuongRepository.findById(5)).thenReturn(Optional.of(doiThuongActive(5, 5000)));

        assertThatThrownBy(() -> service.doiThuong(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đủ điểm");
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
    }
}
```

- [ ] **Step 5: Chạy test, xác nhận FAIL (chưa có `PhieuGiamGiaCaNhanService`)**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test -Dtest=PhieuGiamGiaCaNhanServiceTest`
Expected: FAIL biên dịch.

- [ ] **Step 6: Tạo `PhieuGiamGiaCaNhanService`**

```java
package com.example.backend.service;

import com.example.backend.entity.DmDoiThuong;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.DmDoiThuongRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.PhieuGiamGiaCaNhanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PhieuGiamGiaCaNhanService {

    @Autowired
    private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Autowired
    private DmDoiThuongRepository dmDoiThuongRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private KhachHang currentKhachHang() {
        TaiKhoan tk = currentAccount();
        if (tk == null || tk.getKhachHang() == null)
            throw new AccessDeniedException("Chỉ khách hàng mới đổi được điểm thưởng");
        return khachHangRepository.findById(tk.getKhachHang().getKhachHangId())
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
    }

    @Transactional
    public PhieuGiamGiaCaNhan doiThuong(Integer doiThuongId) {
        DmDoiThuong doiThuong = dmDoiThuongRepository.findById(doiThuongId)
                .orElseThrow(() -> new IllegalArgumentException("Phần thưởng không tồn tại với id: " + doiThuongId));
        if (!"active".equals(doiThuong.getTrangThai()))
            throw new IllegalArgumentException("Phần thưởng này hiện không khả dụng");

        KhachHang khachHang = currentKhachHang();
        if (khachHang.getDiemTichLuy() < doiThuong.getDiemCan())
            throw new IllegalArgumentException("Không đủ điểm để đổi phần thưởng này");

        khachHang.setDiemTichLuy(khachHang.getDiemTichLuy() - doiThuong.getDiemCan());
        khachHangRepository.save(khachHang);

        PhieuGiamGiaCaNhan phieu = new PhieuGiamGiaCaNhan();
        phieu.setKhachHang(khachHang);
        phieu.setDoiThuong(doiThuong);
        phieu.setLoai(doiThuong.getLoai());
        phieu.setGiaTri(doiThuong.getGiaTri());
        phieu.setGiaTriToiDa(doiThuong.getGiaTriToiDa());
        phieu.setDaSuDung(false);
        phieu.setNgayDoi(LocalDateTime.now());
        phieu.setNgayHetHan(LocalDateTime.now().plusDays(30));
        return phieuGiamGiaCaNhanRepository.save(phieu);
    }

    public List<PhieuGiamGiaCaNhanResponse> getCuaToi() {
        KhachHang khachHang = currentKhachHang();
        return phieuGiamGiaCaNhanRepository.findByKhachHang_KhachHangId(khachHang.getKhachHangId()).stream()
                .map(p -> new PhieuGiamGiaCaNhanResponse(
                        p.getPhieuId(), p.getMaPhieu(), p.getLoai(), p.getGiaTri(), p.getGiaTriToiDa(),
                        p.getDaSuDung(), p.getNgayDoi(), p.getNgayHetHan()))
                .toList();
    }
}
```

- [ ] **Step 7: Chạy lại test, xác nhận PASS**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test -Dtest=PhieuGiamGiaCaNhanServiceTest`
Expected: PASS.

- [ ] **Step 8: Tạo `PhieuGiamGiaCaNhanController`**

```java
package com.example.backend.controller;

import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.response.PhieuGiamGiaCaNhanResponse;
import com.example.backend.service.PhieuGiamGiaCaNhanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chỉ đăng nhập là đủ — khách hàng tự đổi điểm/xem voucher của chính mình, service tự suy
// khách hàng qua SecurityContextHolder, không nhận khachHangId từ client.
@RestController
@RequestMapping("/api/phieu-giam-gia-ca-nhan")
@PreAuthorize("isAuthenticated()")
public class PhieuGiamGiaCaNhanController {

    @Autowired
    private PhieuGiamGiaCaNhanService phieuGiamGiaCaNhanService;

    @PostMapping("doi-thuong/{doiThuongId}")
    public ResponseEntity<PhieuGiamGiaCaNhan> doiThuong(@PathVariable Integer doiThuongId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuGiamGiaCaNhanService.doiThuong(doiThuongId));
    }

    @GetMapping("cua-toi")
    public List<PhieuGiamGiaCaNhanResponse> getCuaToi() {
        return phieuGiamGiaCaNhanService.getCuaToi();
    }
}
```

- [ ] **Step 9: Thêm `phieuGiamGiaCaNhanId` vào `DonHangRequest` + tiêu voucher lúc tạo đơn**

Trong `DonHangRequest.java`, thêm sau field `maVanDon`:

```java

    private Integer phieuGiamGiaCaNhanId;
```

Trong `DonHangService.java`, thêm field:

```java
    @Autowired
    private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
```

Sửa method `create()` — thêm logic tiêu voucher NGAY SAU `DonHang saved = donHangRepository.save(entity);` và TRƯỚC `sseService.notifyNewOrder(...)`:

```java
        if (request.getPhieuGiamGiaCaNhanId() != null) {
            PhieuGiamGiaCaNhan phieu = phieuGiamGiaCaNhanRepository.findById(request.getPhieuGiamGiaCaNhanId())
                    .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));
            if (!phieu.getKhachHang().getKhachHangId().equals(saved.getKhachHang().getKhachHangId()))
                throw new IllegalArgumentException("Voucher không thuộc về khách hàng này");
            if (Boolean.TRUE.equals(phieu.getDaSuDung()))
                throw new IllegalArgumentException("Voucher đã được sử dụng");
            if (LocalDateTime.now().isAfter(phieu.getNgayHetHan()))
                throw new IllegalArgumentException("Voucher đã hết hạn");
            phieu.setDaSuDung(true);
            phieu.setDonHang(saved);
            phieuGiamGiaCaNhanRepository.save(phieu);
        }
```

Thêm import `com.example.backend.entity.PhieuGiamGiaCaNhan;`, `com.example.backend.repository.PhieuGiamGiaCaNhanRepository;`, `java.time.LocalDateTime` (nếu chưa có).

- [ ] **Step 10: Viết test cho việc tiêu voucher trong `DonHangService.create()`**

Trong `DonHangServiceTest.java`, thêm mock `@Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;` và test:

```java
    @Test
    void create_coPhieuGiamGiaCaNhan_danhDauDaSuDung() {
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(1);
        when(khachHangRepository.getReferenceById(1)).thenReturn(kh);

        DonHang saved = new DonHang();
        saved.setId(10);
        saved.setKhachHang(kh);
        when(donHangRepository.save(any(DonHang.class))).thenReturn(saved);

        com.example.backend.entity.PhieuGiamGiaCaNhan phieu = new com.example.backend.entity.PhieuGiamGiaCaNhan();
        phieu.setPhieuId(7);
        phieu.setKhachHang(kh);
        phieu.setDaSuDung(false);
        phieu.setNgayHetHan(java.time.LocalDateTime.now().plusDays(10));
        when(phieuGiamGiaCaNhanRepository.findById(7)).thenReturn(Optional.of(phieu));

        com.example.backend.request.DonHangRequest req = new com.example.backend.request.DonHangRequest();
        req.setKhachHangId(1);
        req.setTrangThaiDonHang("pending");
        req.setTrangThaiThanhToan("unpaid");
        req.setNguoiNhan("A");
        req.setSdtNguoiNhan("0900000000");
        req.setTongTien(java.math.BigDecimal.ZERO);
        req.setGiamGia(java.math.BigDecimal.ZERO);
        req.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        req.setNgayDat(java.time.LocalDateTime.now());
        req.setPhieuGiamGiaCaNhanId(7);

        service.create(req);

        assertThat(phieu.getDaSuDung()).isTrue();
        assertThat(phieu.getDonHang()).isEqualTo(saved);
        verify(phieuGiamGiaCaNhanRepository).save(phieu);
    }
```

- [ ] **Step 11: Chạy toàn bộ test suite backend**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test`
Expected: BUILD SUCCESS.

- [ ] **Step 12: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/PhieuGiamGiaCaNhan.java \
        BackEnd/src/main/java/com/example/backend/response/PhieuGiamGiaCaNhanResponse.java \
        BackEnd/src/main/java/com/example/backend/repository/PhieuGiamGiaCaNhanRepository.java \
        BackEnd/src/main/java/com/example/backend/service/PhieuGiamGiaCaNhanService.java \
        BackEnd/src/main/java/com/example/backend/controller/PhieuGiamGiaCaNhanController.java \
        BackEnd/src/main/java/com/example/backend/request/DonHangRequest.java \
        BackEnd/src/main/java/com/example/backend/service/DonHangService.java \
        BackEnd/src/test/java/com/example/backend/service/PhieuGiamGiaCaNhanServiceTest.java \
        BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java
git commit -m "feat: let customers redeem points for personal vouchers and spend them at checkout"
```

---

### Task 4: Frontend Admin — CRUD danh mục đổi thưởng

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/DmDoiThuongService.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/doiThuong.js`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/{vi,en,ja,ko,zh}.js`

**Interfaces:**
- Consumes: `GET/POST/PUT/DELETE /api/dm-doi-thuong` (Task 2).

- [ ] **Step 1: Tạo `Service/DmDoiThuongService.js`** (mirror `Service/KhuyenMaiService.js`)

```js
import { get, post, put, del } from './api.js';

export const getAll = () => get('/api/dm-doi-thuong');

export const save = (id, body) =>
  id ? put(`/api/dm-doi-thuong/update/${id}`, body) : post('/api/dm-doi-thuong', body);

export const remove = (id) => del(`/api/dm-doi-thuong/delete/${id}`);
```

- [ ] **Step 2: Tạo `stores/doiThuong.js`** (mirror `stores/promotions.js` — đọc file đó trước để bám đúng)

```js
import { reactive } from "vue";
import * as DmDoiThuongService from "../Service/DmDoiThuongService.js";

export const DoiThuongStore = reactive({ items: [], loading: false, loaded: false });

let doiThuongPromise = null;
export const ensureDoiThuong = () => {
  if (doiThuongPromise) return doiThuongPromise;
  doiThuongPromise = refreshDoiThuong();
  return doiThuongPromise;
};

export const refreshDoiThuong = async () => {
  DoiThuongStore.loading = true;
  try {
    DoiThuongStore.items = await DmDoiThuongService.getAll().catch(() => []);
    DoiThuongStore.loaded = true;
  } finally {
    DoiThuongStore.loading = false;
  }
  return DoiThuongStore.items;
};
```

- [ ] **Step 3: Wiring trong `AdminPage.vue`**

Trong `AdminPage.vue`, phần "Khuyến mãi" (`promotions`) là khuôn mẫu 1:1 cho phần mới "Đổi thưởng" (`doi-thuong`) — đọc kỹ các đoạn sau trước khi viết, rồi lặp lại đúng cấu trúc, chỉ đổi tên biến/field:

- Import store: `import { PromotionsStore, ensurePromotions, refreshPromotions } from "../stores/promotions.js";` (dòng 41) → thêm dòng tương tự cho `DoiThuongStore`/`ensureDoiThuong`/`refreshDoiThuong` từ `../stores/doiThuong.js`.
- `PAGE_META` (dòng 51-63): thêm entry `"doi-thuong": { titleKey: "admin.pageMeta.doiThuong.title", subKey: "admin.pageMeta.doiThuong.sub", icon: "🎁" },`.
- Computed alias (dòng 79): thêm `const rewards = computed(() => DoiThuongStore.items);`.
- Nav item (khoảng dòng 900, cạnh `promotions`): thêm 1 `<div class="adm-nav" :class="{active: currentPage==='doi-thuong'}" @click="navigate('doi-thuong')">` với icon 🎁 và label `t('admin.sidebar.rewards')`.
- Script CRUD (khoảng dòng 601-680, khối `editingPromoId`/`promoForm`/`savePromo`/`deletePromo`): copy y hệt cấu trúc cho `editingRewardId`/`rewardForm`/`saveReward`/`deleteReward`, field đổi thành `ten, moTa, diemCan, loai, giaTri, giaTriToiDa, trangThai` (bỏ các field không có ở `dm_doi_thuong`: `maKhuyenMai`, `donHangToiThieu`, `ngayBatDau`, `ngayKetThuc`, `soLuongToiDa`, `soLanDaDung`).
- Template bảng (khoảng dòng 1250-1280, `<section v-show="currentPage === 'promotions'">`): thêm `<section v-show="currentPage === 'doi-thuong'">` tương tự, cột: STT, Tên, Điểm cần, Loại, Giá trị, Trạng thái, Hành động (bỏ cột Mã/Ngày bắt đầu/Ngày kết thúc/Đã dùng không áp dụng cho bảng này).
- Modal form (khoảng dòng 1595-1616): tương tự, input cho `ten` (text), `moTa` (textarea), `diemCan` (number), `loai` (select percent/fixed), `giaTri` (number), `giaTriToiDa` (number), `trangThai` (select active/inactive).

Khi tải dữ liệu trang (tìm hàm `fetchAll()` hoặc tương đương gọi `ensurePromotions()` lúc mount — khoảng dòng 792, 1001-1019), thêm `ensureDoiThuong()` vào cùng chỗ.

- [ ] **Step 4: Thêm i18n keys vào cả 5 file locale**

Đọc `admin.pageMeta.promotions`/`admin.sidebar.promotions`/`admin.promotions.*`/`admin.promoModal.*` trong `vi.js` trước để bám đúng cấu trúc, rồi thêm khối tương ứng cho `admin.pageMeta.doiThuong`/`admin.sidebar.rewards`/`admin.rewards.*`/`admin.rewardModal.*` — dịch thật cho cả 5 ngôn ngữ (không copy-paste tiếng Việt sang các file khác), theo đúng khuôn các tính năng trước đã làm trong phiên này.

- [ ] **Step 5: Verify bằng tay + build**

Run: `cd FrontEnd/QLBanMayTinh; npm run build` (PowerShell — xem "✓ built" trong output, bỏ qua NativeCommandError wrapper của stderr nếu build vẫn thành công).

Nếu chạy được `npm run dev`: đăng nhập admin → trang "Đổi thưởng" mới → thêm/sửa/xóa 1 phần thưởng, xác nhận hiện đúng trong bảng.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/DmDoiThuongService.js \
        FrontEnd/QLBanMayTinh/src/stores/doiThuong.js \
        FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js
git commit -m "feat: add admin page for redemption catalog management"
```

---

### Task 5: Frontend khách hàng — khu vực "Điểm & Voucher" trong AccountPage

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/PhieuGiamGiaCaNhanService.js`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/{vi,en,ja,ko,zh}.js`

**Interfaces:**
- Consumes: `GET /api/dm-doi-thuong`, `POST /api/phieu-giam-gia-ca-nhan/doi-thuong/{id}`, `GET /api/phieu-giam-gia-ca-nhan/cua-toi` (Task 2, 3).

- [ ] **Step 1: Tạo `Service/PhieuGiamGiaCaNhanService.js`**

```js
import { get, post } from './api.js';

export const doiThuong = (doiThuongId) => post(`/api/phieu-giam-gia-ca-nhan/doi-thuong/${doiThuongId}`, {});

export const getCuaToi = () => get('/api/phieu-giam-gia-ca-nhan/cua-toi');
```

- [ ] **Step 2: Wiring trong `AccountPage.vue`**

Thêm import:

```js
import * as DmDoiThuongService from "../Service/DmDoiThuongService.js";
import * as PhieuGiamGiaCaNhanService from "../Service/PhieuGiamGiaCaNhanService.js";
```

Thêm state cạnh `profile`:

```js
const rewards       = ref([]);   // danh mục phần thưởng đổi được
const myVouchers     = ref([]);  // voucher cá nhân đã đổi
const redeemingId    = ref(null); // đang gọi API đổi thưởng cho id nào (disable nút, tránh double-click)
const redeemError    = ref("");
```

Trong `fetchProfile()` (hoặc `onMounted`, chỗ nào đang gọi lúc mở trang), thêm:

```js
  rewards.value = await DmDoiThuongService.getAll().catch(() => []);
  myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
```

Thêm hàm đổi thưởng:

```js
const redeemReward = async (r) => {
  redeemError.value = "";
  redeemingId.value = r.doiThuongId;
  try {
    const res = await PhieuGiamGiaCaNhanService.doiThuong(r.doiThuongId);
    if (!res.ok) { redeemError.value = await res.text().catch(() => res.statusText); return; }
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
    await fetchProfile(); // cập nhật lại số điểm hiện tại trên badge header
  } finally {
    redeemingId.value = null;
  }
};
```

Trong template, thêm 1 card mới NGAY TRƯỚC card "Thông tin cá nhân" trong khối `<!-- ══ Tab: Cài đặt tài khoản ══ -->` (dòng ~512-516), cùng style `rounded-4 p-4` background card đã dùng:

```html
        <!-- Điểm & Voucher -->
        <div class="rounded-4 p-4" style="background:var(--bg-card); border:1px solid var(--border-color); box-shadow:0 4px 18px var(--shadow-color);">
          <div class="d-flex align-items-center gap-2 mb-4">
            <span style="font-size:1.3rem;">🎁</span>
            <div>
              <h5 class="fw-black mb-0" style="color:var(--text-heading);">{{ t('account.rewards.heading') }}</h5>
              <div style="color:var(--text-secondary); font-size:11.5px;">{{ t('account.rewards.subtitle', { points: profile?.diemTichLuy ?? 0 }) }}</div>
            </div>
          </div>

          <div v-if="redeemError" class="alert alert-danger small py-2 mb-3">{{ redeemError }}</div>

          <div class="mb-2 small fw-semibold" style="color:var(--text-secondary);">{{ t('account.rewards.catalogHeading') }}</div>
          <div class="d-flex flex-column gap-2 mb-4">
            <div v-for="r in rewards.filter(x => x.trangThai === 'active')" :key="r.doiThuongId"
                 class="d-flex align-items-center justify-content-between p-2 rounded-3" style="background:var(--bg-card-inset);">
              <div>
                <div class="fw-semibold" style="font-size:13px; color:var(--text-primary);">{{ r.ten }}</div>
                <div style="font-size:11px; color:var(--text-secondary);">{{ t('account.rewards.pointsCost', { points: r.diemCan }) }}</div>
              </div>
              <button class="btn btn-sm fw-bold rounded-pill px-3"
                      :disabled="(profile?.diemTichLuy ?? 0) < r.diemCan || redeemingId === r.doiThuongId"
                      style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-primary); font-size:11.5px;"
                      @click="redeemReward(r)">
                {{ t('account.rewards.redeemButton') }}
              </button>
            </div>
            <div v-if="rewards.filter(x => x.trangThai === 'active').length === 0" class="small" style="color:var(--text-secondary);">{{ t('account.rewards.catalogEmpty') }}</div>
          </div>

          <div class="mb-2 small fw-semibold" style="color:var(--text-secondary);">{{ t('account.rewards.myVouchersHeading') }}</div>
          <div class="d-flex flex-column gap-2">
            <div v-for="v in myVouchers" :key="v.phieuId"
                 class="d-flex align-items-center justify-content-between p-2 rounded-3" style="background:var(--bg-card-inset);">
              <div>
                <div class="fw-bold" style="font-size:12.5px; color:var(--text-primary);">{{ v.maPhieu }}</div>
                <div style="font-size:11px; color:var(--text-secondary);">{{ t('account.rewards.expiresOn', { date: formatDate(v.ngayHetHan) }) }}</div>
              </div>
              <span class="badge px-2 py-1 rounded-pill fw-semibold"
                    :style="v.daSuDung ? 'background:rgba(107,114,128,0.15);color:#9ca3af;' : 'background:rgba(34,197,94,0.15);color:#22c55e;'">
                {{ v.daSuDung ? t('account.rewards.used') : t('account.rewards.available') }}
              </span>
            </div>
            <div v-if="myVouchers.length === 0" class="small" style="color:var(--text-secondary);">{{ t('account.rewards.myVouchersEmpty') }}</div>
          </div>
        </div>
```

- [ ] **Step 3: Thêm i18n keys `account.rewards.*` vào cả 5 file locale**

`vi.js` (trong khối `account`, gần các key `settings`/`points`):

```js
    rewards: {
      heading: "Điểm & Voucher",
      subtitle: "Bạn đang có {points} điểm",
      catalogHeading: "Đổi điểm lấy voucher",
      pointsCost: "{points} điểm",
      redeemButton: "Đổi",
      catalogEmpty: "Chưa có phần thưởng nào để đổi.",
      myVouchersHeading: "Voucher đã đổi",
      expiresOn: "Hạn dùng: {date}",
      used: "Đã dùng",
      available: "Còn hiệu lực",
      myVouchersEmpty: "Bạn chưa đổi voucher nào.",
    },
```

(Dịch tương ứng cho `en.js`/`ja.js`/`ko.js`/`zh.js` — không copy-paste tiếng Việt, đọc key cạnh (`settings`) trong mỗi file trước để chèn đúng chỗ.)

- [ ] **Step 4: Verify bằng tay + build**

Run: `cd FrontEnd/QLBanMayTinh; npm run build`

Nếu chạy được `npm run dev`: đăng nhập khách hàng có điểm (nếu chưa có, tự chuyển 1 đơn sang "delivered" qua admin để trigger cộng điểm) → tab "Cài đặt tài khoản" → thấy khu vực "Điểm & Voucher" → đổi 1 phần thưởng → xác nhận điểm giảm đúng, voucher mới xuất hiện trong "Voucher đã đổi".

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/PhieuGiamGiaCaNhanService.js \
        FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js
git commit -m "feat: let customers redeem points for vouchers on the account page"
```

---

### Task 6: Frontend checkout — chọn voucher cá nhân

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/checkout/CheckoutModal.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/{vi,en,ja,ko,zh}.js`

**Interfaces:**
- Consumes: `GET /api/phieu-giam-gia-ca-nhan/cua-toi` (Task 3); `DonHangRequest.phieuGiamGiaCaNhanId` (Task 3).

- [ ] **Step 1: Thêm import + state**

```js
import * as PhieuGiamGiaCaNhanService from '../../Service/PhieuGiamGiaCaNhanService.js';
```

Thêm state cạnh `allPromos`/`appliedPromo`:

```js
const myVouchers    = ref([]);   // voucher cá nhân còn dùng được của khách đang checkout
const appliedVoucher = ref(null); // voucher cá nhân đang chọn (loại trừ với appliedPromo)
```

- [ ] **Step 2: Nạp voucher cá nhân khi mở modal (chỉ khi đã đăng nhập khách hàng)**

Trong hàm `watch(() => props.modelValue, ...)` (khoảng dòng 458-476), thêm sau khối nạp `allPromos`:

```js
  appliedVoucher.value = null;
  if (isLoggedInCustomer.value) {
    myVouchers.value = await PhieuGiamGiaCaNhanService.getCuaToi().catch(() => []);
  }
```

- [ ] **Step 3: Danh sách voucher hợp lệ (chưa dùng, chưa hết hạn) + hàm tính giảm giá + chọn/bỏ chọn**

Thêm ngay sau `eligiblePromos` computed (khoảng dòng 415-425):

```js
// Voucher cá nhân còn dùng được — chưa dùng, chưa hết hạn. Không lọc theo donHangToiThieu
// (voucher cá nhân không có trường này, khác khuyen_mai).
const eligibleVouchers = computed(() => {
  const now = new Date();
  return myVouchers.value
    .filter(v => !v.daSuDung && new Date(v.ngayHetHan) > now)
    .map(v => ({ ...v, discount: calcDiscountFor(v) }))
    .filter(v => v.discount > 0)
    .sort((a, b) => b.discount - a.discount);
});

// Chọn voucher cá nhân — loại trừ với mã khuyến mãi công khai (chỉ dùng 1 trong 2).
const selectVoucher = (v) => {
  if (appliedVoucher.value?.phieuId === v.phieuId) { appliedVoucher.value = null; return; }
  appliedVoucher.value = v;
  appliedPromo.value = null;
  checkoutForm.maKhuyenMai = '';
  promoMsg.value = '';
};
```

Sửa `selectPromo()` (dòng 518-527) để cũng bỏ chọn voucher khi chọn mã khuyến mãi — thêm dòng đầu hàm: `appliedVoucher.value = null;`.

Sửa `checkoutGiamGia` (dòng 410) — hiện chỉ tính từ `appliedPromo`, cần tính từ CẢ HAI (loại trừ nhau nên tối đa 1 cái có giá trị > 0 tại 1 thời điểm):

```js
const checkoutGiamGia = computed(() => calcDiscountFor(appliedPromo.value) + calcDiscountFor(appliedVoucher.value));
```

- [ ] **Step 4: Thêm khối "Voucher của bạn" trong template — ngay sau khối "Mã khuyến mãi" (dòng ~151-175)**

```html
          <!-- Voucher cá nhân (đổi từ điểm) -->
          <div v-if="isLoggedInCustomer && eligibleVouchers.length">
            <div class="fw-semibold mb-2" style="font-size:11px; letter-spacing:0.06em; text-transform:uppercase; color:var(--text-secondary);">{{ t('checkout.voucherHeading') }}</div>
            <div class="d-flex flex-column gap-2">
              <div v-for="v in eligibleVouchers" :key="v.phieuId"
                   class="d-flex align-items-center justify-content-between p-2 rounded-3"
                   style="cursor:pointer;border:1px solid;"
                   :style="appliedVoucher?.phieuId===v.phieuId ? 'border-color:var(--accent);background:rgba(244,63,94,0.08);' : 'border-color:var(--border-color-soft);background:var(--bg-card-alt);'"
                   @click="selectVoucher(v)">
                <span class="fw-bold small" style="color:var(--text-heading);">{{ v.maPhieu }}</span>
                <div class="text-warning fw-bold small flex-shrink-0 ms-2">− {{ formatPrice(v.discount) }}</div>
              </div>
            </div>
          </div>
```

- [ ] **Step 5: Gắn `phieuGiamGiaCaNhanId` vào `orderBody` lúc tạo đơn (dòng ~580-594)**

Thêm 1 dòng vào `orderBody`:

```js
      phieuGiamGiaCaNhanId: appliedVoucher.value?.phieuId ?? null,
```

- [ ] **Step 6: Thêm i18n key `checkout.voucherHeading` vào cả 5 file locale**

`vi.js`: `voucherHeading: "Voucher của bạn",` — chèn cạnh `promoHeading` đã có. Dịch tương ứng: `en.js`: `"Your vouchers"`, `ja.js`: `"あなたのバウチャー"`, `ko.js`: `"내 바우처"`, `zh.js`: `"您的优惠券"`.

- [ ] **Step 7: Verify bằng tay + build**

Run: `cd FrontEnd/QLBanMayTinh; npm run build`

Nếu chạy được `npm run dev`: đăng nhập khách có voucher (đổi ở Task 5) → giỏ hàng → thanh toán → thấy khối "Voucher của bạn" → chọn 1 voucher → xác nhận tổng tiền giảm đúng, chọn tiếp mã khuyến mãi công khai thì voucher tự bỏ chọn (ngược lại cũng vậy) → đặt hàng → vào lại "Điểm & Voucher" xác nhận voucher chuyển trạng thái "Đã dùng".

- [ ] **Step 8: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/checkout/CheckoutModal.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js
git commit -m "feat: let customers apply their personal voucher at checkout"
```

---

## Self-Review

**1. Spec coverage:**
- Tích điểm tự động lúc delivered, 10.000đ=1 điểm, không cộng 2 lần → Task 1 ✅
- Admin CRUD danh mục đổi thưởng → Task 2 (backend) + Task 4 (frontend) ✅
- Khách tự đổi điểm lấy voucher, hạn 30 ngày → Task 3 (backend) + Task 5 (frontend) ✅
- Áp voucher ở checkout, loại trừ với mã khuyến mãi công khai → Task 3 (backend tiêu voucher) + Task 6 (frontend) ✅

**2. Placeholder scan:** không còn "TBD" ở các task backend (code đầy đủ). Task 4/6 dùng anchor tham chiếu dòng cụ thể trong file lớn có sẵn (đúng cách đã dùng thành công cho Task 4/5 ở plan mã vận đơn trước) thay vì chép nguyên văn hàng nghìn dòng — implementer đọc file thật trước khi viết theo anchor.

**3. Type/tên nhất quán:** `phieuGiamGiaCaNhanId` giữ nguyên tên xuyên suốt request → entity → frontend orderBody; `loai`/`giaTri`/`giaTriToiDa` cùng tên field ở cả `dm_doi_thuong` và `phieu_giam_gia_ca_nhan` (snapshot).

**4. Rủi ro đã lường:** Task 1 Step 2 nhắc lại rõ hành vi DROP DATABASE của file .sql (đã xảy ra 1 lần trong phiên trước, đã xác nhận là chủ ý) để tránh implementer hoảng khi thấy dữ liệu bị xóa sau khi chạy.
