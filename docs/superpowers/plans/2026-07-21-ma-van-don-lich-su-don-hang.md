# Mã vận đơn & Lịch sử trạng thái đơn hàng — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm mã vận đơn (nhập tay bởi nhân viên/admin) và log lịch sử trạng thái đơn hàng theo mốc thời gian (tự động ghi qua DB trigger), hiển thị cho khách hàng trong `AccountPage.vue` dưới timeline hiện có.

**Architecture:** DB trigger `AFTER UPDATE` trên `don_hang` tự ghi 1 dòng vào bảng mới `lich_su_don_hang` mỗi khi `trang_thai_don_hang` đổi — không cần sửa code Java ở phía ghi. Backend chỉ thêm 1 API đọc (`GET /api/lich-su-don-hang/don-hang/{id}`) và field `maVanDon` trên `DonHang`. Frontend thêm input mã vận đơn vào modal cập nhật đơn hàng có sẵn (`OrdersTable.vue`, dùng chung Admin/Staff) và 1 component hiển thị mới (`OrderTrackingLog.vue`) nhúng vào `AccountPage.vue`.

**Tech Stack:** Spring Boot + JPA (Java), SQL Server (T-SQL trigger), Vue 3 `<script setup>`.

## Global Constraints

- Mọi thay đổi `Database/QLBanMayTinh.sql` phải idempotent (bọc `IF NOT EXISTS` / `CREATE OR ALTER`) — user luôn chạy lại toàn bộ file, không chạy snippet lẻ riêng.
- Không thêm cột `ghi_chu` tự do vào `lich_su_don_hang` — log chỉ tự động, không cho nhập tay (đã chốt trong spec).
- Mã vận đơn là text tự do, không có CHECK constraint, không validate bắt buộc phía backend.
- Endpoint `GET /api/lich-su-don-hang/don-hang/{id}` giữ mở (không `@PreAuthorize`) — khách hàng gọi trực tiếp từ `AccountPage.vue`.

Spec đầy đủ: `docs/superpowers/specs/2026-07-21-ma-van-don-lich-su-don-hang-design.md`.

---

### Task 1: Database schema — cột `ma_van_don`, bảng `lich_su_don_hang`, trigger tự ghi log

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (thêm vào cuối file)

**Interfaces:**
- Produces: cột `don_hang.ma_van_don VARCHAR(50) NULL`; bảng `lich_su_don_hang(lich_su_id, don_hang_id, trang_thai_cu, trang_thai_moi, thoi_gian)`; trigger `trg_don_hang_log_trangthai` tự INSERT khi `don_hang.trang_thai_don_hang` đổi.

- [ ] **Step 1: Thêm khối SQL vào cuối `Database/QLBanMayTinh.sql`**

```sql
-- ============================================================
--  Mã vận đơn & Lịch sử trạng thái đơn hàng
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('don_hang') AND name = 'ma_van_don')
BEGIN
    ALTER TABLE don_hang ADD ma_van_don VARCHAR(50) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_don_hang')
BEGIN
    CREATE TABLE lich_su_don_hang (
        lich_su_id     INT           IDENTITY(1,1) PRIMARY KEY,
        don_hang_id    INT           NOT NULL,
        trang_thai_cu  NVARCHAR(30)  NULL,
        trang_thai_moi NVARCHAR(30)  NOT NULL,
        thoi_gian      DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lsdh_don_hang FOREIGN KEY (don_hang_id) REFERENCES don_hang(don_hang_id) ON DELETE CASCADE
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lsdh_don_hang')
    CREATE INDEX IX_lsdh_don_hang ON lich_su_don_hang(don_hang_id, thoi_gian);
GO

-- Tự ghi log mỗi khi trạng thái đơn đổi — chỗ duy nhất phát sinh log, không cần backend
-- Java can thiệp, không sợ thiếu dòng nếu sau này có thêm đường cập nhật trạng thái khác.
CREATE OR ALTER TRIGGER trg_don_hang_log_trangthai
ON don_hang
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(trang_thai_don_hang)
    BEGIN
        INSERT INTO lich_su_don_hang (don_hang_id, trang_thai_cu, trang_thai_moi, thoi_gian)
        SELECT i.don_hang_id, d.trang_thai_don_hang, i.trang_thai_don_hang, GETDATE()
        FROM inserted i
        JOIN deleted d ON d.don_hang_id = i.don_hang_id
        WHERE d.trang_thai_don_hang <> i.trang_thai_don_hang;
    END
END
GO
```

- [ ] **Step 2: Chạy lại toàn bộ file `QLBanMayTinh.sql` trên DB dev (theo đúng cách user luôn làm)**

- [ ] **Step 3: Verify bằng tay — trigger thực sự ghi log**

Chạy trong SSMS (thay `1` bằng 1 `don_hang_id` có thật trong DB dev):

```sql
UPDATE don_hang SET trang_thai_don_hang = 'confirmed' WHERE don_hang_id = 1;
SELECT * FROM lich_su_don_hang WHERE don_hang_id = 1 ORDER BY thoi_gian DESC;
```

Expected: 1 dòng mới với `trang_thai_cu` = trạng thái trước đó, `trang_thai_moi = 'confirmed'`.

- [ ] **Step 4: Verify re-run idempotent — chạy lại toàn bộ file lần 2, không lỗi**

- [ ] **Step 5: Commit**

```bash
git add "Database/QLBanMayTinh.sql"
git commit -m "feat: add ma_van_don column and lich_su_don_hang auto-log trigger"
```

---

### Task 2: Backend — field `maVanDon` trên đơn hàng + API đọc lịch sử trạng thái

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/entity/DonHang.java`
- Modify: `BackEnd/src/main/java/com/example/backend/request/DonHangRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/response/DonHangResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/DonHangRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/entity/LichSuDonHang.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/LichSuDonHangResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/LichSuDonHangRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/LichSuDonHangService.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/LichSuDonHangController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/LichSuDonHangServiceTest.java`

**Interfaces:**
- Consumes: `LichSuDonHangRepository.getByDonHangId(Integer)` (Task này tự định nghĩa).
- Produces: `GET /api/lich-su-don-hang/don-hang/{donHangId}` → `List<LichSuDonHangResponse>`; `DonHangResponse.maVanDon` (String); `LichSuDonHangRepository.findByDonHangId(Integer)` → `List<LichSuDonHang>` entity (dùng ở Task 3).

- [ ] **Step 1: Thêm field `maVanDon` vào `DonHang` entity**

Trong `DonHang.java`, thêm sau field `ghiChu` (dòng cuối cùng trước `}`):

```java
    // Nhập tay bởi nhân viên/admin khi chuyển đơn sang "shipping" — text tự do, không phải
    // mã tra cứu thật của đơn vị vận chuyển.
    @Column(name = "ma_van_don", length = 50)
    private String maVanDon;
```

- [ ] **Step 2: Thêm field vào `DonHangRequest` và `DonHangResponse`**

Trong `DonHangRequest.java`, thêm sau `private String ghiChu;`:

```java

    private String maVanDon;
```

Trong `DonHangResponse.java`, thêm sau `private String ghiChu;`:

```java
    private String maVanDon;
```

- [ ] **Step 3: Cập nhật JPQL projection trong `DonHangRepository.hienThiDonHang()`**

Trong `DonHangRepository.java`, sửa constructor `SELECT new ...DonHangResponse(...)`:

```java
    @Query(value = """
    SELECT new com.example.backend.response.DonHangResponse(
        d.id, d.maDonHang,
        kh.khachHangId,
        nv.nhanVienId,
        km.khuyenMaiId,
        dcgh.id, dcgh.diaChi,
        d.nguoiNhan, d.sdtNguoiNhan,
        d.tongTien, d.giamGia, d.phiVanChuyen, d.thanhTien,
        d.ngayDat, d.ngayGiaoDuKien, d.ngayGiaoThucTe,
        d.trangThaiDonHang, d.trangThaiThanhToan, d.kenhBan, d.ghiChu, d.maVanDon
    )
    FROM DonHang d
    JOIN d.khachHang kh
    LEFT JOIN d.nhanVien nv
    LEFT JOIN d.khuyenMai km
    LEFT JOIN d.diaChiGiaoHang dcgh
    WHERE (:khachHangId IS NULL OR kh.khachHangId = :khachHangId)
    ORDER BY d.ngayDat DESC
    """,
    countQuery = """
    SELECT COUNT(d) FROM DonHang d JOIN d.khachHang kh
    WHERE (:khachHangId IS NULL OR kh.khachHangId = :khachHangId)
    """)
    Page<DonHangResponse> hienThiDonHang(@Param("khachHangId") Integer khachHangId, Pageable pageable);
```

(Chỉ đổi 1 dòng: thêm `, d.maVanDon` vào cuối danh sách constructor args — khớp đúng vị trí field mới thêm cuối `DonHangResponse`.)

- [ ] **Step 4: Build backend để chắc chắn không lỗi compile**

Run: `cd BackEnd && mvn -q compile`
Expected: BUILD SUCCESS (không lỗi biên dịch từ 3 file vừa sửa).

- [ ] **Step 5: Tạo entity `LichSuDonHang`**

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
@Table(name = "lich_su_don_hang")
public class LichSuDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lich_su_id")
    private Integer lichSuId;

    // Plain FK column, không dùng @ManyToOne — bảng này chỉ được TRIGGER trong DB ghi (xem
    // trg_don_hang_log_trangthai trong QLBanMayTinh.sql). Java chỉ đọc, trừ lúc gộp đơn cần
    // đổi lại donHangId (xem DonHangService.mergeOrders() ở Task 3) — không cần điều hướng
    // quan hệ JPA cho việc đó, set thẳng Integer là đủ.
    @Column(name = "don_hang_id", nullable = false)
    private Integer donHangId;

    @Column(name = "trang_thai_cu", length = 30)
    private String trangThaiCu;

    @Column(name = "trang_thai_moi", length = 30, nullable = false)
    private String trangThaiMoi;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;
}
```

- [ ] **Step 6: Tạo `LichSuDonHangResponse`**

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
public class LichSuDonHangResponse {
    private Integer lichSuId;
    private Integer donHangId;
    private String trangThaiCu;
    private String trangThaiMoi;
    private LocalDateTime thoiGian;
}
```

- [ ] **Step 7: Tạo `LichSuDonHangRepository`**

```java
package com.example.backend.repository;

import com.example.backend.entity.LichSuDonHang;
import com.example.backend.response.LichSuDonHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuDonHangRepository extends JpaRepository<LichSuDonHang, Integer> {

    @Query("SELECT new com.example.backend.response.LichSuDonHangResponse(l.lichSuId, l.donHangId, l.trangThaiCu, l.trangThaiMoi, l.thoiGian) " +
           "FROM LichSuDonHang l WHERE l.donHangId = :donHangId ORDER BY l.thoiGian ASC")
    List<LichSuDonHangResponse> getByDonHangId(@Param("donHangId") Integer donHangId);

    // Dùng khi gộp đơn (DonHangService.mergeOrders, Task 3) — chuyển log của đơn nguồn sang
    // đơn đích trước khi xóa đơn nguồn, giữ nguyên lịch sử thay vì mất do cascade delete.
    List<LichSuDonHang> findByDonHangId(Integer donHangId);
}
```

- [ ] **Step 8: Viết test cho `LichSuDonHangService` (viết trước, chạy fail trước khi có service)**

```java
package com.example.backend.service;

import com.example.backend.repository.LichSuDonHangRepository;
import com.example.backend.response.LichSuDonHangResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LichSuDonHangServiceTest {

    @Mock private LichSuDonHangRepository lichSuDonHangRepository;

    @InjectMocks
    private LichSuDonHangService service;

    @Test
    void getByDonHang_traVeDungThuTuTheoRepository() {
        LichSuDonHangResponse r1 = new LichSuDonHangResponse(1, 5, null, "pending", LocalDateTime.now());
        LichSuDonHangResponse r2 = new LichSuDonHangResponse(2, 5, "pending", "confirmed", LocalDateTime.now());
        when(lichSuDonHangRepository.getByDonHangId(5)).thenReturn(List.of(r1, r2));

        List<LichSuDonHangResponse> result = service.getByDonHang(5);

        assertThat(result).containsExactly(r1, r2);
    }
}
```

- [ ] **Step 9: Chạy test, xác nhận FAIL (chưa có class `LichSuDonHangService`)**

Run: `cd BackEnd && mvn -q test -Dtest=LichSuDonHangServiceTest`
Expected: FAIL biên dịch — `LichSuDonHangService` không tồn tại.

- [ ] **Step 10: Tạo `LichSuDonHangService`**

```java
package com.example.backend.service;

import com.example.backend.repository.LichSuDonHangRepository;
import com.example.backend.response.LichSuDonHangResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichSuDonHangService {

    @Autowired
    private LichSuDonHangRepository lichSuDonHangRepository;

    public List<LichSuDonHangResponse> getByDonHang(Integer donHangId) {
        return lichSuDonHangRepository.getByDonHangId(donHangId);
    }
}
```

- [ ] **Step 11: Chạy lại test, xác nhận PASS**

Run: `cd BackEnd && mvn -q test -Dtest=LichSuDonHangServiceTest`
Expected: PASS (1 test).

- [ ] **Step 12: Tạo `LichSuDonHangController`**

```java
package com.example.backend.controller;

import com.example.backend.response.LichSuDonHangResponse;
import com.example.backend.service.LichSuDonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Giữ mở — AccountPage.vue gọi để khách hàng xem lịch sử đơn của chính mình, giống
// ChiTietDonHangController.getByDonHang(). Chỉ có GET vì bảng lich_su_don_hang chỉ được
// trigger DB ghi (trg_don_hang_log_trangthai), không có endpoint tạo/sửa/xoá.
@RestController
@RequestMapping("/api/lich-su-don-hang")
public class LichSuDonHangController {

    @Autowired
    private LichSuDonHangService lichSuDonHangService;

    @GetMapping("/don-hang/{donHangId}")
    public List<LichSuDonHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return lichSuDonHangService.getByDonHang(donHangId);
    }
}
```

- [ ] **Step 13: Chạy toàn bộ test suite backend, xác nhận không phá vỡ gì**

Run: `cd BackEnd && mvn -q test`
Expected: BUILD SUCCESS, tất cả test cũ vẫn PASS.

- [ ] **Step 14: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/DonHang.java \
        BackEnd/src/main/java/com/example/backend/request/DonHangRequest.java \
        BackEnd/src/main/java/com/example/backend/response/DonHangResponse.java \
        BackEnd/src/main/java/com/example/backend/repository/DonHangRepository.java \
        BackEnd/src/main/java/com/example/backend/entity/LichSuDonHang.java \
        BackEnd/src/main/java/com/example/backend/response/LichSuDonHangResponse.java \
        BackEnd/src/main/java/com/example/backend/repository/LichSuDonHangRepository.java \
        BackEnd/src/main/java/com/example/backend/service/LichSuDonHangService.java \
        BackEnd/src/main/java/com/example/backend/controller/LichSuDonHangController.java \
        BackEnd/src/test/java/com/example/backend/service/LichSuDonHangServiceTest.java
git commit -m "feat: add maVanDon field and read-only order status history API"
```

---

### Task 3: Backend — giữ lịch sử khi gộp đơn (`mergeOrders`)

**Bối cảnh:** `DonHangService.mergeOrders()` đã có sẵn vòng lặp chuyển `thanh_toan`/`lich_su_ton_kho`/`phieu_tra_hang`/`phieu_bao_hanh` sang đơn đích trước khi xóa đơn nguồn (tránh vỡ FK + mất dữ liệu). `lich_su_don_hang` mới thêm ở Task 2 cũng tham chiếu `don_hang_id` và có `ON DELETE CASCADE` — nếu không reparent trước, log lịch sử của đơn nguồn sẽ biến mất khi gộp đơn, không nhất quán với 4 bảng kia.

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/service/DonHangService.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java`

**Interfaces:**
- Consumes: `LichSuDonHangRepository.findByDonHangId(Integer)` → `List<LichSuDonHang>` (Task 2).

- [ ] **Step 1: Viết test trước — xác nhận log được chuyển sang đơn đích khi gộp**

Trong `DonHangServiceTest.java`:
- Thêm import: `import com.example.backend.entity.LichSuDonHang;`
- Thêm vào danh sách static imports: `import static org.mockito.Mockito.verify;`
- Thêm mock field: `@Mock private LichSuDonHangRepository lichSuDonHangRepository;`
- Thêm test mới:

```java
    @Test
    void mergeOrders_hopLe_chuyenLichSuDonHangSangDonDich() {
        DonHang target = new DonHang();
        target.setId(1);
        target.setTrangThaiDonHang("confirmed");
        DonHang source = new DonHang();
        source.setId(2);
        source.setTrangThaiDonHang("confirmed");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(target));
        when(donHangRepository.findById(2)).thenReturn(Optional.of(source));
        when(chiTietDonHangRepository.findEntityByDonHangId(2)).thenReturn(List.of());
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of());
        when(thanhToanRepository.findByDonHang_Id(2)).thenReturn(List.of());
        when(lichSuTonKhoRepository.findByDonHang_Id(2)).thenReturn(List.of());
        when(phieuTraHangRepository.findByDonHang_Id(2)).thenReturn(List.of());
        when(phieuBaoHanhRepository.findByDonHang_Id(2)).thenReturn(List.of());

        LichSuDonHang log = new LichSuDonHang();
        log.setLichSuId(9);
        log.setDonHangId(2);
        log.setTrangThaiMoi("confirmed");
        when(lichSuDonHangRepository.findByDonHangId(2)).thenReturn(List.of(log));

        service.mergeOrders(1, List.of(2));

        assertThat(log.getDonHangId()).isEqualTo(1);
        verify(lichSuDonHangRepository).save(log);
    }
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `cd BackEnd && mvn -q test -Dtest=DonHangServiceTest#mergeOrders_hopLe_chuyenLichSuDonHangSangDonDich`
Expected: FAIL — `lichSuDonHangRepository` chưa tồn tại trong `DonHangService`, hoặc log không được reparent (`donHangId` vẫn = 2).

- [ ] **Step 3: Thêm dependency + logic reparent vào `DonHangService.mergeOrders()`**

Thêm field vào đầu class (cạnh `phieuBaoHanhRepository`):

```java
    @Autowired
    private LichSuDonHangRepository lichSuDonHangRepository;
```

Thêm import: `import com.example.backend.entity.LichSuDonHang;`

Trong `mergeOrders()`, thêm vòng lặp mới ngay sau khối `for (PhieuBaoHanh pbh : ...)` và trước `donHangRepository.deleteById(sourceId);`:

```java
            for (LichSuDonHang lsdh : lichSuDonHangRepository.findByDonHangId(sourceId)) {
                lsdh.setDonHangId(target.getId());
                lichSuDonHangRepository.save(lsdh);
            }
```

- [ ] **Step 4: Chạy lại test, xác nhận PASS**

Run: `cd BackEnd && mvn -q test -Dtest=DonHangServiceTest`
Expected: PASS toàn bộ (bao gồm test mới + các test cũ trong file).

- [ ] **Step 5: Chạy toàn bộ test suite backend**

Run: `cd BackEnd && mvn -q test`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/DonHangService.java \
        BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java
git commit -m "fix: preserve order status history when merging orders"
```

---

### Task 4: Frontend Admin/Staff — nhập mã vận đơn trong `OrdersTable.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`

**Interfaces:**
- Consumes: `PUT /api/don-hang/update/{id}` nay chấp nhận thêm field `maVanDon` trong body (Task 2).

- [ ] **Step 1: Thêm field `maVanDon` vào `orderStatusForm`**

Trong `OrdersTable.vue`, sửa khai báo `orderStatusForm`:

```js
const orderStatusForm = reactive({
  trangThaiDonHang: "",
  trangThaiThanhToan: "",
  ngayGiaoDuKien: "", // Ngày dự kiến giao hàng
  ngayGiaoThucTe: "", // Ngày khách nhận hàng thực tế
  maVanDon: "",        // Mã vận đơn — nhân viên/admin nhập tay khi chuyển sang "Đang giao"
});
```

- [ ] **Step 2: Prefill `maVanDon` trong `openOrderStatus()`**

```js
const openOrderStatus = (o) => {
  editingOrder.value = o;
  orderStatusForm.trangThaiDonHang = o.trangThaiDonHang ?? "";
  orderStatusForm.trangThaiThanhToan = o.trangThaiThanhToan ?? "";
  orderStatusForm.ngayGiaoDuKien = o.ngayGiaoDuKien?.slice(0, 16) ?? "";
  orderStatusForm.ngayGiaoThucTe = o.ngayGiaoThucTe?.slice(0, 16) ?? "";
  orderStatusForm.maVanDon = o.maVanDon ?? "";
  orderStatusError.value = "";
  showOrderModal.value = true;
};
```

- [ ] **Step 3: Thêm `maVanDon` vào `buildOrderUpdateBody()`**

```js
const buildOrderUpdateBody = (o, { trangThaiDonHang, trangThaiThanhToan, ngayGiaoDuKien, ngayGiaoThucTe, maVanDon }) => ({
  khachHangId: o.khachHangId,
  nhanVienId: o.nhanVienId ?? null,
  khuyenMaiId: o.khuyenMaiId ?? null,
  diaChiGiaoHangId: o.diaChiGiaoHangId ?? null,
  diaChiGiaoHangText: o.diaChiGiaoHangText ?? null,
  nguoiNhan: o.nguoiNhan || customerName(o.khachHangId),
  sdtNguoiNhan:
    o.sdtNguoiNhan ||
    (CustomersStore.items.find((c) => c.khachHangId === o.khachHangId)
      ?.soDienThoai ?? ""),
  tongTien: o.tongTien ?? 0,
  giamGia: o.giamGia ?? 0,
  phiVanChuyen: o.phiVanChuyen ?? 0,
  thanhTien: o.thanhTien ?? 0,
  ngayDat: o.ngayDat?.slice(0, 19),
  ngayGiaoDuKien: ngayGiaoDuKien || null,
  ngayGiaoThucTe: ngayGiaoThucTe || null,
  trangThaiDonHang,
  trangThaiThanhToan,
  kenhBan: o.kenhBan ?? null,
  ghiChu: o.ghiChu ?? null,
  maVanDon: maVanDon || null,
});
```

- [ ] **Step 4: Truyền `maVanDon` ở 2 nơi gọi `buildOrderUpdateBody()`**

Trong `saveOrderStatus()`, sửa lời gọi:

```js
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: orderStatusForm.trangThaiDonHang,
    trangThaiThanhToan: orderStatusForm.trangThaiThanhToan,
    ngayGiaoDuKien: orderStatusForm.ngayGiaoDuKien,
    ngayGiaoThucTe: orderStatusForm.ngayGiaoThucTe,
    maVanDon: orderStatusForm.maVanDon,
  });
```

Trong `advanceOrderStatus()`, sửa toàn bộ hàm — chặn bước "Đang giao" lại để bắt buộc mở modal nhập mã vận đơn, giống tiền lệ case "confirmed + online":

```js
const advanceOrderStatus = async (o) => {
  const next = NEXT_ORDER_STATUS[o.trangThaiDonHang];
  if (!next) return;
  if (next === 'confirmed' && o.kenhBan === 'online') {
    await openXacNhanSerialModal(o);
    return;
  }
  // Chuyển sang "Đang giao" bắt buộc dừng lại nhập mã vận đơn — mở modal thay vì 1-click.
  if (next === 'shipping') {
    openOrderStatus(o);
    orderStatusForm.trangThaiDonHang = 'shipping';
    return;
  }
  const body = buildOrderUpdateBody(o, {
    trangThaiDonHang: next,
    trangThaiThanhToan: o.trangThaiThanhToan,
    ngayGiaoDuKien: o.ngayGiaoDuKien,
    ngayGiaoThucTe: next === 'delivered' && !o.ngayGiaoThucTe
      ? nowLocalIso()
      : o.ngayGiaoThucTe,
    maVanDon: o.maVanDon,
  });
  const res = await DonHangService.update(o.donHangId, body);
  if (!res.ok) { showToast(await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }))); return; }
  await refreshOrders();
};
```

- [ ] **Step 5: Thêm input "Mã vận đơn" vào modal**

Trong template, ngay sau dòng `<div><label ...>{{ t('admin.orderStatusModal.statusLabel') }}</label><select v-model="orderStatusForm.trangThaiDonHang" ...>...</select></div>` (dòng có `option value="returned"`), thêm:

```html
          <div><label class="form-label small text-secondary">{{ t('admin.orderStatusModal.trackingCodeLabel') }}</label><input v-model="orderStatusForm.maVanDon" type="text" class="form-control form-control-sm" :placeholder="t('admin.orderStatusModal.trackingCodePlaceholder')" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" /></div>
```

- [ ] **Step 6: Thêm i18n key `admin.orderStatusModal.trackingCodeLabel` / `trackingCodePlaceholder` vào cả 5 file locale**

`vi.js` (trong khối `orderStatusModal`, sau `actualDeliveryLabel`):

```js
      trackingCodeLabel: "Mã vận đơn",
      trackingCodePlaceholder: "VD: GHN1234567890",
```

`en.js`:

```js
      trackingCodeLabel: "Tracking code",
      trackingCodePlaceholder: "e.g. GHN1234567890",
```

`ja.js`:

```js
      trackingCodeLabel: "追跡番号",
      trackingCodePlaceholder: "例: GHN1234567890",
```

`ko.js`:

```js
      trackingCodeLabel: "운송장 번호",
      trackingCodePlaceholder: "예: GHN1234567890",
```

`zh.js`:

```js
      trackingCodeLabel: "运单号",
      trackingCodePlaceholder: "例如：GHN1234567890",
```

- [ ] **Step 7: Verify bằng tay trên trình duyệt**

Run: `cd FrontEnd/QLBanMayTinh && npm run dev`

1. Đăng nhập Admin hoặc Staff, mở trang Đơn hàng.
2. Bấm nút "bước tiếp theo" trên 1 đơn đang ở trạng thái "Đang đóng gói" (`processing`).
3. Expected: modal "Cập nhật trạng thái đơn hàng" mở ra, dropdown trạng thái đã sẵn "Đang giao", có ô "Mã vận đơn" trống.
4. Gõ 1 mã bất kỳ, bấm "Lưu".
5. Expected: đơn chuyển sang "Đang giao" thành công, mở lại modal của đúng đơn đó → ô "Mã vận đơn" hiện lại đúng giá trị vừa nhập (xác nhận đã lưu vào DB).
6. Các bước 1-click khác (VD "Đóng gói" → không mở modal) vẫn hoạt động như cũ.

- [ ] **Step 8: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/OrdersTable.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js
git commit -m "feat: require tracking code entry when order moves to shipping"
```

---

### Task 5: Frontend khách hàng — hiện mã vận đơn + log lịch sử trong `AccountPage.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/LichSuDonHangService.js`
- Create: `FrontEnd/QLBanMayTinh/src/components/order/OrderTrackingLog.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`

**Interfaces:**
- Consumes: `GET /api/lich-su-don-hang/don-hang/{donHangId}` → `[{ lichSuId, donHangId, trangThaiCu, trangThaiMoi, thoiGian }]` (Task 2); `o.maVanDon` từ `DonHangResponse` (Task 2); `orderStatusLabel`/`orderStatusIcon` từ `utils/orderStatus.js` (đã có sẵn).
- Produces: component `OrderTrackingLog` — props `{ maVanDon: String, history: Array }`.

- [ ] **Step 1: Tạo `Service/LichSuDonHangService.js`**

```js
import { get } from './api.js';

// Lấy lịch sử thay đổi trạng thái của 1 đơn hàng, theo mốc thời gian tăng dần.
export const getByDonHang = (donHangId) => get(`/api/lich-su-don-hang/don-hang/${donHangId}`);
```

- [ ] **Step 2: Thêm i18n keys `account.trackingCode` / `trackingCodeCopy` / `trackingCodeCopied` vào cả 5 file locale**

`vi.js` (trong khối `account`, ngay sau `buyAgain: "Mua lại",`):

```js
    trackingCode: "Mã vận đơn",
    trackingCodeCopy: "Sao chép",
    trackingCodeCopied: "Đã sao chép",
```

`en.js` (sau `buyAgain: "Buy Again",`):

```js
    trackingCode: "Tracking code",
    trackingCodeCopy: "Copy",
    trackingCodeCopied: "Copied",
```

`ja.js` (sau `buyAgain: "再購入",`):

```js
    trackingCode: "追跡番号",
    trackingCodeCopy: "コピー",
    trackingCodeCopied: "コピーしました",
```

`ko.js` (sau `buyAgain: "재구매",`):

```js
    trackingCode: "운송장 번호",
    trackingCodeCopy: "복사",
    trackingCodeCopied: "복사됨",
```

`zh.js` (sau `buyAgain: "再次购买",`):

```js
    trackingCode: "运单号",
    trackingCodeCopy: "复制",
    trackingCodeCopied: "已复制",
```

- [ ] **Step 3: Tạo component `src/components/order/OrderTrackingLog.vue`**

```vue
<template>
  <div v-if="maVanDon || history.length" class="rounded-3 p-3" style="background:var(--bg-card-alt);">
    <div v-if="maVanDon" class="d-flex align-items-center gap-2 mb-2 pb-2" style="border-bottom:1px solid var(--border-color-soft);">
      <span style="font-size:0.85rem; color:var(--text-secondary);">📦 {{ t('account.trackingCode') }}:</span>
      <span class="fw-bold" style="color:var(--text-primary); font-size:0.85rem;">{{ maVanDon }}</span>
      <button class="btn btn-sm px-2 py-0" style="font-size:11px; border:1px solid var(--border-color-strong); background:var(--bg-input); color:var(--text-secondary);" @click="copyCode">
        {{ copied ? t('account.trackingCodeCopied') : t('account.trackingCodeCopy') }}
      </button>
    </div>
    <div v-if="history.length" class="d-flex flex-column gap-2">
      <div v-for="entry in history" :key="entry.lichSuId" class="d-flex gap-2">
        <span style="font-size:0.78rem; color:var(--text-secondary); min-width:130px;">{{ formatDate(entry.thoiGian) }}</span>
        <span style="font-size:0.82rem; color:var(--text-primary);">{{ orderStatusIcon(entry.trangThaiMoi) }} {{ orderStatusLabel(entry.trangThaiMoi) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { t, I18nStore } from '../../i18n/index.js';
import { orderStatusLabel, orderStatusIcon } from '../../utils/orderStatus.js';

const props = defineProps({
  maVanDon: { type: String, default: '' },
  history: { type: Array, default: () => [] },
});

const copied = ref(false);
const copyCode = async () => {
  if (!props.maVanDon) return;
  await navigator.clipboard.writeText(props.maVanDon);
  copied.value = true;
  setTimeout(() => { copied.value = false; }, 1500);
};

const formatDate = (d) => {
  if (!d) return '—';
  try { return new Date(d).toLocaleString(I18nStore.locale); } catch { return d; }
};
</script>
```

- [ ] **Step 4: Nạp lịch sử song song trong `fetchData()` của `AccountPage.vue`**

Thêm import ở đầu file:

```js
import * as LichSuDonHangService from "../Service/LichSuDonHangService.js";
import OrderTrackingLog from "../components/order/OrderTrackingLog.vue";
```

Thêm state cạnh `itemsByOrder`:

```js
const historyByOrder = ref({});  // { [donHangId]: LichSuDonHangResponse[] }
```

Trong `fetchData()`, ngay sau khối gán `itemsByOrder.value = Object.fromEntries(entries);`, thêm:

```js
    const historyEntries = await Promise.all(
      orders.value.map(async (o) => [
        o.donHangId,
        await LichSuDonHangService.getByDonHang(o.donHangId).catch(() => []),
      ])
    );
    historyByOrder.value = Object.fromEntries(historyEntries);
```

- [ ] **Step 5: Nhúng `<OrderTrackingLog>` vào thẻ đơn tab "Đang giao"**

Trong template, ngay sau khối:

```html
            <!-- Ngày giao dự kiến / ngày nhận hàng thực tế -->
            <div v-if="o.ngayGiaoDuKien || o.ngayGiaoThucTe" ...>
              ...
            </div>
```

Thêm:

```html
            <!-- Mã vận đơn + lịch sử trạng thái -->
            <OrderTrackingLog v-if="o.trangThaiDonHang === 'shipping'"
                               :ma-van-don="o.maVanDon || ''"
                               :history="historyByOrder[o.donHangId] || []"
                               class="mb-3" />
```

- [ ] **Step 6: Nhúng `<OrderTrackingLog>` vào dòng mở rộng tab "Hoàn tất"**

Trong khối `v-if="expandedHistoryOrders.has(o.donHangId)"` (phần liệt kê sản phẩm đã mua), thêm ngay trước thẻ `</div>` đóng khối đó:

```html
              <OrderTrackingLog v-if="o.trangThaiDonHang === 'delivered'"
                                 :ma-van-don="o.maVanDon || ''"
                                 :history="historyByOrder[o.donHangId] || []"
                                 class="mt-2" />
```

- [ ] **Step 7: Verify bằng tay trên trình duyệt**

Run: `cd FrontEnd/QLBanMayTinh && npm run dev`

1. Đăng nhập 1 tài khoản khách hàng có đơn đang ở trạng thái "Đang giao" (đã nhập mã vận đơn ở Task 4) → vào "Đơn hàng của tôi" → tab "Đang giao".
2. Expected: dưới timeline 5 bước, hiện khối mới có mã vận đơn (nút "Sao chép" hoạt động — bấm đổi thành "Đã sao chép" 1.5s) và danh sách log các mốc trạng thái đã qua, đúng thứ tự thời gian tăng dần.
3. Chuyển đơn đó sang "Đã giao" (từ Admin/Staff) → refresh trang khách hàng → tab "Hoàn tất" → bấm mở đơn.
4. Expected: khối mã vận đơn + log tương tự hiện ra, có thêm dòng log mới nhất ("Giao thành công").
5. Với đơn "Chờ xác nhận" (chưa có mã vận đơn) → không hiện khối này (vì `v-if` chỉ áp dụng tab "Đang giao"/"Hoàn tất").

- [ ] **Step 8: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/LichSuDonHangService.js \
        FrontEnd/QLBanMayTinh/src/components/order/OrderTrackingLog.vue \
        FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js
git commit -m "feat: show tracking code and status history log to customers"
```

---

## Self-Review

**1. Spec coverage:**
- Cột `ma_van_don` + bảng `lich_su_don_hang` + trigger tự ghi → Task 1 ✅
- Nhân viên/admin nhập mã vận đơn qua `OrdersTable.vue` dùng chung → Task 4 ✅
- API đọc lịch sử cho khách hàng, giữ mở → Task 2 ✅
- Giữ timeline cũ, thêm log bên dưới cho tab "Đang giao"/"Hoàn tất" → Task 5 ✅
- Reparent log khi gộp đơn (bổ sung phát hiện trong lúc lập plan, nhất quán với 4 bảng con khác `mergeOrders()` đã xử lý) → Task 3 ✅

**2. Placeholder scan:** không còn "TBD"/"tương tự Task N" — mọi step đều có code đầy đủ hoặc lệnh chạy cụ thể.

**3. Type/tên nhất quán:** `maVanDon` (String) giữ nguyên tên xuyên suốt entity/request/response/form/prop; `LichSuDonHangResponse` field order khớp đúng constructor JPQL; `historyByOrder`/`itemsByOrder` cùng shape `{ [donHangId]: Array }`.
