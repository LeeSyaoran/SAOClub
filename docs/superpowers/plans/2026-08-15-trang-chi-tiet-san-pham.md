# Trang chi tiết sản phẩm (Admin) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khi Admin nhấn "Chi tiết" trên một sản phẩm, mở 1 tab trình duyệt mới dẫn tới trang chi tiết độc lập với 3 tab con (Thông tin sản phẩm / Biến thể sản phẩm / Lịch sử thay đổi), và ghi log từng trường thay đổi mỗi khi sản phẩm hoặc 1 biến thể của nó được cập nhật.

**Architecture:** Backend thêm 1 bảng log (`lich_su_thay_doi_san_pham`) + 1 service dùng chung (`LichSuThayDoiSanPhamService`) để so sánh cũ/mới và ghi log, được gọi từ `SanPhamService.updateSanPham()` và `BienTheSanPhamService.update()`. Frontend thêm 1 route mới (`/admin/san-pham/:id`) tái dùng `AdminPage.vue` làm shell, 1 trang mới `SanPhamDetailPage.vue` với 3 tab, và tách phần form thêm/sửa sản phẩm ra khỏi `ProductsTable.vue` thành `ProductFormModal.vue` dùng chung.

**Tech Stack:** Spring Boot + JPA/Hibernate + SQL Server (backend), Vue 3 `<script setup>` + Vue Router (hash history) (frontend), JUnit 5 + Mockito + AssertJ (backend test).

**Spec:** [docs/superpowers/specs/2026-08-14-trang-chi-tiet-san-pham-design.md](../specs/2026-08-14-trang-chi-tiet-san-pham-design.md) — plan này lập luận dựa trên spec, đọc cả 2 file.

## Global Constraints

- Route `/admin/san-pham/:id` chỉ cấp quyền `roles: ["admin"]` — không đổi hành vi cho `nhan_vien` (StaffPage.vue, `ProductsTable :readonly="true"`).
- Chỉ ghi log khi **update** sản phẩm/biến thể — không ghi khi tạo mới.
- `nhan_vien_id` trong log luôn resolve phía server qua `SecurityContextHolder`, không nhận từ request body.
- Không phân trang danh sách lịch sử (v1) — trả toàn bộ, `ORDER BY thoi_gian DESC`.
- Thay đổi SQL trong `Database/QLBanMayTinh.sql` phải idempotent (`IF NOT EXISTS (SELECT 1 FROM sys.tables ...)` cho bảng, `IF NOT EXISTS (SELECT 1 FROM sys.indexes ...)` cho index) — người dùng luôn chạy lại toàn bộ file.
- Không sửa `ProductDetailModal.vue`, không sửa hành vi hiện có của `BienTheTable.vue` khi không truyền `filterSanPhamId` (giữ nguyên cho tab "Biến thể" độc lập).

---

## Task 1: SQL — bảng `lich_su_thay_doi_san_pham`

**Files:**
- Modify: `Database/QLBanMayTinh.sql`

**Interfaces:**
- Produces: bảng `lich_su_thay_doi_san_pham` (cột: `lich_su_id, san_pham_id, bien_the_id, doi_tuong, ten_truong, gia_tri_cu, gia_tri_moi, nhan_vien_id, thoi_gian`) — Task 2 map entity vào bảng này.

- [ ] **Step 1: Thêm block SQL idempotent**

Mở `Database/QLBanMayTinh.sql`, tìm đoạn tạo bảng `lich_su_ton_kho` (idempotent, gần dòng 484-505) làm điểm neo, thêm ngay sau khối `GO` kết thúc đoạn đó (trước đoạn tạo bảng `thanh_toan`):

```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lich_su_thay_doi_san_pham')
BEGIN
    CREATE TABLE lich_su_thay_doi_san_pham (
        lich_su_id   INT           IDENTITY(1,1) PRIMARY KEY,
        san_pham_id  INT           NOT NULL,
        bien_the_id  INT           NULL,
        doi_tuong    NVARCHAR(20)  NOT NULL
            CONSTRAINT CK_lstsp_doi_tuong CHECK (doi_tuong IN (N'san_pham', N'bien_the')),
        ten_truong   NVARCHAR(50)  NOT NULL,
        gia_tri_cu   NVARCHAR(500) NULL,
        gia_tri_moi  NVARCHAR(500) NULL,
        nhan_vien_id INT           NULL,
        thoi_gian    DATETIME      NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_lstsp_san_pham  FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id) ON DELETE CASCADE,
        CONSTRAINT FK_lstsp_bien_the  FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(bien_the_id),
        CONSTRAINT FK_lstsp_nhan_vien FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lstsp_san_pham')
    CREATE INDEX IX_lstsp_san_pham ON lich_su_thay_doi_san_pham(san_pham_id, thoi_gian DESC);
GO
```

- [ ] **Step 2: Chạy lại toàn bộ file trong SSMS, xác nhận không lỗi**

Chạy `Database/QLBanMayTinh.sql` toàn bộ (Execute) 2 lần liên tiếp trên cùng 1 database — lần 2 phải **không báo lỗi** (xác nhận idempotent). Kiểm tra bảng đã tạo:
```sql
SELECT * FROM sys.tables WHERE name = 'lich_su_thay_doi_san_pham';
```
Expected: 1 dòng kết quả cả 2 lần chạy.

- [ ] **Step 3: Commit**

```bash
git add Database/QLBanMayTinh.sql
git commit -m "feat(db): thêm bảng lich_su_thay_doi_san_pham"
```

---

## Task 2: Backend — entity, response DTO, repository, `LichSuThayDoiSanPhamService`

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/entity/LichSuThayDoiSanPham.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/LichSuThayDoiSanPhamResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/LichSuThayDoiSanPhamRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/LichSuThayDoiSanPhamService.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/LichSuThayDoiSanPhamServiceTest.java`

**Interfaces:**
- Consumes: `SanPhamRepository.getReferenceById(Integer)`, `BienTheSanPhamRepository.getReferenceById(Integer)`, `TaiKhoanRepository.findByUsername(String): Optional<TaiKhoan>`, `TaiKhoan.getNhanVien(): NhanVien` (đã có sẵn).
- Produces (dùng ở Task 3, 4, 5):
  - `LichSuThayDoiSanPhamService.nguoiSuaHienTai(): NhanVien` — trả `null` nếu tài khoản không gắn `NhanVien`.
  - `LichSuThayDoiSanPhamService.ghiNeuThayDoi(Integer sanPhamId, Integer bienTheId, String doiTuong, String tenTruong, Object giaTriCu, Object giaTriMoi, NhanVien nguoiSua): void` — no-op nếu `String.valueOf(giaTriCu) == String.valueOf(giaTriMoi)` (cả 2 `null` cũng coi là bằng nhau).
  - `LichSuThayDoiSanPhamService.layLichSu(Integer sanPhamId): List<LichSuThayDoiSanPhamResponse>`.

- [ ] **Step 1: Viết test cho `LichSuThayDoiSanPhamService` (TDD — viết trước khi có class)**

Tạo `BackEnd/src/test/java/com/example/backend/service/LichSuThayDoiSanPhamServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.LichSuThayDoiSanPhamRepository;
import com.example.backend.repository.SanPhamRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LichSuThayDoiSanPhamServiceTest {

    @Mock private LichSuThayDoiSanPhamRepository lichSuThayDoiSanPhamRepository;
    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private LichSuThayDoiSanPhamService service;

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ghiNeuThayDoi_giaTriKhacNhau_luuLog() {
        when(sanPhamRepository.getReferenceById(10)).thenReturn(new SanPham());
        NhanVien nv = new NhanVien();
        nv.setNhanVienId(3);

        service.ghiNeuThayDoi(10, null, "san_pham", "tenSanPham", "Ten cu", "Ten moi", nv);

        verify(lichSuThayDoiSanPhamRepository).save(argThat(log ->
                "san_pham".equals(log.getDoiTuong())
                        && "tenSanPham".equals(log.getTenTruong())
                        && "Ten cu".equals(log.getGiaTriCu())
                        && "Ten moi".equals(log.getGiaTriMoi())
                        && log.getNhanVien() == nv
                        && log.getBienThe() == null));
    }

    @Test
    void ghiNeuThayDoi_giaTriGiongNhau_khongLuu() {
        service.ghiNeuThayDoi(10, null, "san_pham", "trangThai", "active", "active", null);

        verify(lichSuThayDoiSanPhamRepository, never()).save(any());
        verify(sanPhamRepository, never()).getReferenceById(any());
    }

    @Test
    void ghiNeuThayDoi_caHaiGiaTriNull_khongLuu() {
        service.ghiNeuThayDoi(10, null, "san_pham", "nhaCungCapId", null, null, null);

        verify(lichSuThayDoiSanPhamRepository, never()).save(any());
    }

    @Test
    void ghiNeuThayDoi_coBienThe_ganDungBienThe() {
        when(sanPhamRepository.getReferenceById(10)).thenReturn(new SanPham());
        BienTheSanPham bt = new BienTheSanPham();
        when(bienTheSanPhamRepository.getReferenceById(20)).thenReturn(bt);

        service.ghiNeuThayDoi(10, 20, "bien_the", "giaBan", "1000", "2000", null);

        verify(lichSuThayDoiSanPhamRepository).save(argThat(log -> log.getBienThe() == bt));
    }

    @Test
    void nguoiSuaHienTai_taiKhoanCoNhanVien_traVeNhanVien() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("nv1");
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        NhanVien nv = new NhanVien();
        nv.setNhanVienId(7);
        TaiKhoan tk = new TaiKhoan();
        tk.setNhanVien(nv);
        when(taiKhoanRepository.findByUsername("nv1")).thenReturn(Optional.of(tk));

        assertThat(service.nguoiSuaHienTai()).isEqualTo(nv);
    }

    @Test
    void nguoiSuaHienTai_taiKhoanKhongTonTai_traVeNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ghost");
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        when(taiKhoanRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(service.nguoiSuaHienTai()).isNull();
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận lỗi biên dịch (chưa có class nào)**

Run: `cd BackEnd && ./mvnw test -Dtest=LichSuThayDoiSanPhamServiceTest`
Expected: FAIL (biên dịch lỗi — `LichSuThayDoiSanPhamService`, `LichSuThayDoiSanPhamRepository` chưa tồn tại).

- [ ] **Step 3: Tạo entity `LichSuThayDoiSanPham`**

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
@Table(name = "lich_su_thay_doi_san_pham")
public class LichSuThayDoiSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lich_su_id")
    private Integer lichSuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_the_id")
    private BienTheSanPham bienThe;

    @Column(name = "doi_tuong", length = 20, nullable = false)
    private String doiTuong;

    @Column(name = "ten_truong", length = 50, nullable = false)
    private String tenTruong;

    @Column(name = "gia_tri_cu", length = 500)
    private String giaTriCu;

    @Column(name = "gia_tri_moi", length = 500)
    private String giaTriMoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nhan_vien_id")
    private NhanVien nhanVien;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;

    @PrePersist
    protected void onCreate() {
        this.thoiGian = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: Tạo response DTO `LichSuThayDoiSanPhamResponse`**

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
public class LichSuThayDoiSanPhamResponse {
    private Integer lichSuId;
    private String doiTuong;
    private Integer bienTheId;
    private String maSku;
    private String tenTruong;
    private String giaTriCu;
    private String giaTriMoi;
    private String tenNhanVien;
    private LocalDateTime thoiGian;
}
```

- [ ] **Step 5: Tạo repository `LichSuThayDoiSanPhamRepository`**

```java
package com.example.backend.repository;

import com.example.backend.entity.LichSuThayDoiSanPham;
import com.example.backend.response.LichSuThayDoiSanPhamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuThayDoiSanPhamRepository extends JpaRepository<LichSuThayDoiSanPham, Integer> {
    @Query("SELECT new com.example.backend.response.LichSuThayDoiSanPhamResponse(" +
           "l.lichSuId, l.doiTuong, bt.bienTheId, bt.maSku, l.tenTruong, l.giaTriCu, l.giaTriMoi, nv.hoTen, l.thoiGian) " +
           "FROM LichSuThayDoiSanPham l " +
           "LEFT JOIN l.bienThe bt " +
           "LEFT JOIN l.nhanVien nv " +
           "WHERE l.sanPham.sanPhamId = :sanPhamId " +
           "ORDER BY l.thoiGian DESC")
    List<LichSuThayDoiSanPhamResponse> hienThiLichSu(@Param("sanPhamId") Integer sanPhamId);
}
```

- [ ] **Step 6: Tạo `LichSuThayDoiSanPhamService`**

```java
package com.example.backend.service;

import com.example.backend.entity.LichSuThayDoiSanPham;
import com.example.backend.entity.NhanVien;
import com.example.backend.repository.BienTheSanPhamRepository;
import com.example.backend.repository.LichSuThayDoiSanPhamRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.LichSuThayDoiSanPhamResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class LichSuThayDoiSanPhamService {

    @Autowired private LichSuThayDoiSanPhamRepository lichSuThayDoiSanPhamRepository;
    @Autowired private SanPhamRepository sanPhamRepository;
    @Autowired private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Autowired private TaiKhoanRepository taiKhoanRepository;

    public NhanVien nguoiSuaHienTai() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username)
                .map(tk -> tk.getNhanVien())
                .orElse(null);
    }

    public void ghiNeuThayDoi(Integer sanPhamId, Integer bienTheId, String doiTuong,
                               String tenTruong, Object giaTriCu, Object giaTriMoi, NhanVien nguoiSua) {
        String cu = giaTriCu == null ? null : String.valueOf(giaTriCu);
        String moi = giaTriMoi == null ? null : String.valueOf(giaTriMoi);
        if (Objects.equals(cu, moi)) return;

        LichSuThayDoiSanPham log = new LichSuThayDoiSanPham();
        log.setSanPham(sanPhamRepository.getReferenceById(sanPhamId));
        log.setBienThe(bienTheId != null ? bienTheSanPhamRepository.getReferenceById(bienTheId) : null);
        log.setDoiTuong(doiTuong);
        log.setTenTruong(tenTruong);
        log.setGiaTriCu(cu);
        log.setGiaTriMoi(moi);
        log.setNhanVien(nguoiSua);
        lichSuThayDoiSanPhamRepository.save(log);
    }

    public List<LichSuThayDoiSanPhamResponse> layLichSu(Integer sanPhamId) {
        return lichSuThayDoiSanPhamRepository.hienThiLichSu(sanPhamId);
    }
}
```

- [ ] **Step 7: Chạy lại test, xác nhận pass**

Run: `cd BackEnd && ./mvnw test -Dtest=LichSuThayDoiSanPhamServiceTest`
Expected: PASS (6/6 test).

- [ ] **Step 8: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/LichSuThayDoiSanPham.java \
        BackEnd/src/main/java/com/example/backend/response/LichSuThayDoiSanPhamResponse.java \
        BackEnd/src/main/java/com/example/backend/repository/LichSuThayDoiSanPhamRepository.java \
        BackEnd/src/main/java/com/example/backend/service/LichSuThayDoiSanPhamService.java \
        BackEnd/src/test/java/com/example/backend/service/LichSuThayDoiSanPhamServiceTest.java
git commit -m "feat(backend): entity/repository/service ghi log lịch sử thay đổi sản phẩm"
```

---

## Task 3: Backend — `SanPhamService.updateSanPham()` ghi log theo trường

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/service/SanPhamService.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/SanPhamServiceTest.java` (file mới)

**Interfaces:**
- Consumes: `LichSuThayDoiSanPhamService.nguoiSuaHienTai()`, `LichSuThayDoiSanPhamService.ghiNeuThayDoi(...)` (Task 2).

- [ ] **Step 1: Viết test trước**

Tạo `BackEnd/src/test/java/com/example/backend/service/SanPhamServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.DanhMuc;
import com.example.backend.entity.NhaCungCap;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.entity.ThuongHieu;
import com.example.backend.repository.*;
import com.example.backend.request.SanPhamRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanPhamServiceTest {

    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private ThuongHieuRepository thuongHieuRepository;
    @Mock private DanhMucRepository danhMucRepository;
    @Mock private NhaCungCapRepository nhaCungCapRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private DmCpuRepository dmCpuRepository;
    @Mock private DmRamRepository dmRamRepository;
    @Mock private DmOcungRepository dmOcungRepository;
    @Mock private DmGpuRepository dmGpuRepository;
    @Mock private BienTheSanPhamService bienTheSanPhamService;
    @Mock private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    @InjectMocks
    private SanPhamService service;

    @Test
    void updateSanPham_ghiLogChoTatCaTruongTheoDoi_voiGiaTriCuMoiDung() {
        ThuongHieu thCu = new ThuongHieu(); thCu.setThuongHieuId(1);
        DanhMuc dmCu = new DanhMuc(); dmCu.setId(2);
        NhaCungCap nccCu = new NhaCungCap(); nccCu.setNhaCungCapId(3);

        SanPham sp = new SanPham();
        sp.setSanPhamId(10);
        sp.setTenSanPham("Ten cu");
        sp.setThuongHieu(thCu);
        sp.setDanhMuc(dmCu);
        sp.setNhaCungCap(nccCu);
        sp.setLoaiSanPham("LAPTOP");
        sp.setMoTa("Mo ta cu");
        sp.setHinhAnhChinh("cu.jpg");
        sp.setTrangThai("active");
        when(sanPhamRepository.findById(10)).thenReturn(Optional.of(sp));

        ThuongHieu thMoi = new ThuongHieu(); thMoi.setThuongHieuId(9);
        DanhMuc dmMoi = new DanhMuc(); dmMoi.setId(8);
        when(thuongHieuRepository.getReferenceById(9)).thenReturn(thMoi);
        when(danhMucRepository.getReferenceById(8)).thenReturn(dmMoi);
        when(nhaCungCapRepository.getReferenceById(7)).thenReturn(new NhaCungCap());
        when(sanPhamRepository.save(any(SanPham.class))).thenAnswer(inv -> inv.getArgument(0));

        NhanVien nv = new NhanVien(); nv.setNhanVienId(5);
        when(lichSuThayDoiSanPhamService.nguoiSuaHienTai()).thenReturn(nv);

        SanPhamRequest request = new SanPhamRequest();
        request.setTenSanPham("Ten moi");
        request.setThuongHieuId(9);
        request.setDanhMucId(8);
        request.setNhaCungCapId(7);
        request.setLoaiSanPham("LAPTOP");
        request.setMoTa("Mo ta moi");
        request.setHinhAnhChinh("cu.jpg");
        request.setTrangThai("inactive");
        request.setGiaBan(BigDecimal.valueOf(1000));
        request.setGiaNhap(BigDecimal.valueOf(800));
        request.setBaoHanhThang(12);

        service.updateSanPham(10, request);

        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "tenSanPham", "Ten cu", "Ten moi", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "thuongHieuId", 1, 9, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "danhMucId", 2, 8, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "nhaCungCapId", 3, 7, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "loaiSanPham", "LAPTOP", "LAPTOP", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "moTa", "Mo ta cu", "Mo ta moi", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "hinhAnhChinh", "cu.jpg", "cu.jpg", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, null, "san_pham", "trangThai", "active", "inactive", nv);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `cd BackEnd && ./mvnw test -Dtest=SanPhamServiceTest`
Expected: FAIL (chưa gọi `ghiNeuThayDoi` trong `updateSanPham`).

- [ ] **Step 3: Sửa `SanPhamService.java`**

Thêm import ở đầu file (sau `import com.example.backend.entity.SanPham;`):
```java
import com.example.backend.entity.NhanVien;
```

Thêm field autowired (cạnh `bienTheSanPhamService`):
```java
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;
```

Thay toàn bộ method `updateSanPham` bằng:

```java
    @Transactional
    public void updateSanPham(Integer sanPhamId, SanPhamRequest request) {
        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại với id: " + sanPhamId));

        String oldTenSanPham = sanPham.getTenSanPham();
        Integer oldThuongHieuId = sanPham.getThuongHieu() != null ? sanPham.getThuongHieu().getThuongHieuId() : null;
        Integer oldDanhMucId = sanPham.getDanhMuc() != null ? sanPham.getDanhMuc().getId() : null;
        Integer oldNhaCungCapId = sanPham.getNhaCungCap() != null ? sanPham.getNhaCungCap().getNhaCungCapId() : null;
        String oldLoaiSanPham = sanPham.getLoaiSanPham();
        String oldMoTa = sanPham.getMoTa();
        String oldHinhAnhChinh = sanPham.getHinhAnhChinh();
        String oldTrangThai = sanPham.getTrangThai();

        BeanUtils.copyProperties(request, sanPham, "sanPhamId", "bienTheId", "ngayTao");
        if (request.getNgayTao() != null) sanPham.setNgayTao(request.getNgayTao());

        sanPham.setThuongHieu(thuongHieuRepository.getReferenceById(request.getThuongHieuId()));
        sanPham.setDanhMuc(danhMucRepository.getReferenceById(request.getDanhMucId()));
        sanPham.setNhaCungCap(request.getNhaCungCapId() != null
                ? nhaCungCapRepository.getReferenceById(request.getNhaCungCapId()) : null);

        sanPhamRepository.save(sanPham);

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
                    .orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại với id: " + request.getBienTheId()));

            BeanUtils.copyProperties(request, bt, "bienTheId");
            bt.setSanPham(sanPham);
            bt.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
            bt.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
            bt.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
            bt.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

            bienTheSanPhamRepository.save(bt);
        }
    }
```

(Chỉ phần trên `sanPhamRepository.save(sanPham);` và các dòng `ghiNeuThayDoi` là mới — khối `if (request.getBienTheId() != null)` giữ nguyên y hệt, không log vì nhánh này hiện không được UI nào gọi tới nữa — xem ghi chú trong spec.)

- [ ] **Step 4: Chạy lại test, xác nhận pass**

Run: `cd BackEnd && ./mvnw test -Dtest=SanPhamServiceTest`
Expected: PASS.

- [ ] **Step 5: Chạy toàn bộ test backend để chắc không phá vỡ gì khác**

Run: `cd BackEnd && ./mvnw test`
Expected: PASS toàn bộ.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/SanPhamService.java \
        BackEnd/src/test/java/com/example/backend/service/SanPhamServiceTest.java
git commit -m "feat(backend): SanPhamService ghi log thay đổi từng trường khi update"
```

---

## Task 4: Backend — `BienTheSanPhamService.update()` ghi log theo trường

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/service/BienTheSanPhamService.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/BienTheSanPhamServiceTest.java` (file mới)

**Interfaces:**
- Consumes: `LichSuThayDoiSanPhamService.nguoiSuaHienTai()`, `LichSuThayDoiSanPhamService.ghiNeuThayDoi(...)` (Task 2).

- [ ] **Step 1: Viết test trước**

Tạo `BackEnd/src/test/java/com/example/backend/service/BienTheSanPhamServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.DmCpu;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.SanPham;
import com.example.backend.repository.*;
import com.example.backend.request.BienTheSanPhamRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BienTheSanPhamServiceTest {

    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private DmCpuRepository dmCpuRepository;
    @Mock private DmRamRepository dmRamRepository;
    @Mock private DmOcungRepository dmOcungRepository;
    @Mock private DmGpuRepository dmGpuRepository;
    @Mock private ChiTietPhieuNhapRepository chiTietPhieuNhapRepository;
    @Mock private TonKhoRepository tonKhoRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;

    @InjectMocks
    private BienTheSanPhamService service;

    @Test
    void update_ghiLogChoTatCaTruongTheoDoi_voiGiaTriCuMoiDung() {
        SanPham sp = new SanPham();
        sp.setSanPhamId(10);
        DmCpu cpuCu = new DmCpu();
        cpuCu.setCpuId(1);

        BienTheSanPham bt = new BienTheSanPham();
        bt.setBienTheId(20);
        bt.setSanPham(sp);
        bt.setMaSku("SKU-CU");
        bt.setGiaNhap(BigDecimal.valueOf(1000));
        bt.setGiaBan(BigDecimal.valueOf(1500));
        bt.setBaoHanhThang(12);
        bt.setHinhAnhBienThe("cu.jpg");
        bt.setTrangThai("active");
        bt.setMauSac("Đen");
        bt.setCpu(cpuCu);
        bt.setKichThuocManHinh("15.6\"");
        bt.setHeDieuHanh("Win11");
        bt.setPin("50Wh");
        bt.setTrongLuongKg(BigDecimal.valueOf(1.7));
        when(bienTheSanPhamRepository.findById(20)).thenReturn(Optional.of(bt));
        when(sanPhamRepository.getReferenceById(10)).thenReturn(sp);
        DmCpu cpuMoi = new DmCpu();
        cpuMoi.setCpuId(9);
        when(dmCpuRepository.getReferenceById(9)).thenReturn(cpuMoi);
        when(bienTheSanPhamRepository.save(any(BienTheSanPham.class))).thenAnswer(inv -> inv.getArgument(0));

        NhanVien nv = new NhanVien();
        nv.setNhanVienId(4);
        when(lichSuThayDoiSanPhamService.nguoiSuaHienTai()).thenReturn(nv);

        BienTheSanPhamRequest request = new BienTheSanPhamRequest();
        request.setSanPhamId(10);
        request.setMaSku("SKU-MOI");
        request.setGiaNhap(BigDecimal.valueOf(1000));
        request.setGiaBan(BigDecimal.valueOf(1800));
        request.setBaoHanhThang(12);
        request.setHinhAnhBienThe("cu.jpg");
        request.setTrangThai("active");
        request.setMauSac("Đen");
        request.setCpuId(9);
        request.setKichThuocManHinh("15.6\"");
        request.setHeDieuHanh("Win11");
        request.setPin("50Wh");
        request.setTrongLuongKg(BigDecimal.valueOf(1.7));

        service.update(20, request);

        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "maSku", "SKU-CU", "SKU-MOI", nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "giaNhap", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "giaBan", BigDecimal.valueOf(1500), BigDecimal.valueOf(1800), nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "cpuId", 1, 9, nv);
        verify(lichSuThayDoiSanPhamService).ghiNeuThayDoi(10, 20, "bien_the", "ramId", null, null, nv);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `cd BackEnd && ./mvnw test -Dtest=BienTheSanPhamServiceTest`
Expected: FAIL.

- [ ] **Step 3: Sửa `BienTheSanPhamService.java`**

Thêm import (sau `import com.example.backend.entity.BienTheSanPham;`):
```java
import com.example.backend.entity.NhanVien;
```

Thêm field autowired (cạnh `lichSuTonKhoRepository`):
```java
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;
```

Thay toàn bộ method `update` bằng:

```java
    @Transactional
    public BienTheSanPham update(Integer id, BienTheSanPhamRequest request) {
        BienTheSanPham entity = getById(id);

        Integer sanPhamId = entity.getSanPham().getSanPhamId();
        String oldMaSku = entity.getMaSku();
        BigDecimal oldGiaNhap = entity.getGiaNhap();
        BigDecimal oldGiaBan = entity.getGiaBan();
        Integer oldBaoHanhThang = entity.getBaoHanhThang();
        String oldHinhAnhBienThe = entity.getHinhAnhBienThe();
        String oldTrangThai = entity.getTrangThai();
        String oldMauSac = entity.getMauSac();
        Integer oldCpuId = entity.getCpu() != null ? entity.getCpu().getCpuId() : null;
        Integer oldRamId = entity.getRam() != null ? entity.getRam().getRamId() : null;
        Integer oldOCungId = entity.getOCung() != null ? entity.getOCung().getOCungId() : null;
        Integer oldGpuId = entity.getGpu() != null ? entity.getGpu().getGpuId() : null;
        String oldKichThuocManHinh = entity.getKichThuocManHinh();
        String oldHeDieuHanh = entity.getHeDieuHanh();
        String oldPin = entity.getPin();
        BigDecimal oldTrongLuongKg = entity.getTrongLuongKg();

        BeanUtils.copyProperties(request, entity, "bienTheId", "sanPhamId", "cpuId", "ramId", "oCungId", "gpuId");

        entity.setSanPham(sanPhamRepository.getReferenceById(request.getSanPhamId()));
        entity.setCpu(request.getCpuId() != null ? dmCpuRepository.getReferenceById(request.getCpuId()) : null);
        entity.setRam(request.getRamId() != null ? dmRamRepository.getReferenceById(request.getRamId()) : null);
        entity.setOCung(request.getOCungId() != null ? dmOcungRepository.getReferenceById(request.getOCungId()) : null);
        entity.setGpu(request.getGpuId() != null ? dmGpuRepository.getReferenceById(request.getGpuId()) : null);

        BienTheSanPham saved = bienTheSanPhamRepository.save(entity);

        NhanVien nguoiSua = lichSuThayDoiSanPhamService.nguoiSuaHienTai();
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "maSku", oldMaSku, saved.getMaSku(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaNhap", oldGiaNhap, saved.getGiaNhap(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "giaBan", oldGiaBan, saved.getGiaBan(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "baoHanhThang", oldBaoHanhThang, saved.getBaoHanhThang(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "hinhAnhBienThe", oldHinhAnhBienThe, saved.getHinhAnhBienThe(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trangThai", oldTrangThai, saved.getTrangThai(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "mauSac", oldMauSac, saved.getMauSac(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "cpuId", oldCpuId, request.getCpuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "ramId", oldRamId, request.getRamId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "oCungId", oldOCungId, request.getOCungId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "gpuId", oldGpuId, request.getGpuId(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "kichThuocManHinh", oldKichThuocManHinh, saved.getKichThuocManHinh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "heDieuHanh", oldHeDieuHanh, saved.getHeDieuHanh(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "pin", oldPin, saved.getPin(), nguoiSua);
        lichSuThayDoiSanPhamService.ghiNeuThayDoi(sanPhamId, id, "bien_the", "trongLuongKg", oldTrongLuongKg, saved.getTrongLuongKg(), nguoiSua);

        return saved;
    }
```

Thêm import `java.math.BigDecimal` nếu file chưa có (kiểm tra đầu file — `BienTheSanPhamService.java` hiện chưa import `BigDecimal` trực tiếp, cần thêm `import java.math.BigDecimal;`).

- [ ] **Step 4: Chạy lại test, xác nhận pass**

Run: `cd BackEnd && ./mvnw test -Dtest=BienTheSanPhamServiceTest`
Expected: PASS.

- [ ] **Step 5: Chạy toàn bộ test backend**

Run: `cd BackEnd && ./mvnw test`
Expected: PASS toàn bộ.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/BienTheSanPhamService.java \
        BackEnd/src/test/java/com/example/backend/service/BienTheSanPhamServiceTest.java
git commit -m "feat(backend): BienTheSanPhamService ghi log thay đổi từng trường khi update"
```

---

## Task 5: Backend endpoint + frontend service function

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/SanPhamController.java`
- Modify: `FrontEnd/QLBanMayTinh/src/services/SanPhamService.js`

**Interfaces:**
- Produces: `GET /api/san-pham/{id}/lich-su` → `List<LichSuThayDoiSanPhamResponse>` (dùng ở Task 11).
- Produces: `SanPhamService.getLichSu(id): Promise<LichSuThayDoiSanPhamResponse[]>` (frontend, dùng ở Task 11).

- [ ] **Step 1: Thêm endpoint vào `SanPhamController.java`**

Thêm import:
```java
import com.example.backend.response.LichSuThayDoiSanPhamResponse;
import com.example.backend.service.LichSuThayDoiSanPhamService;
import java.util.List;
```

Thêm field autowired (cạnh `sanPhamService`):
```java
    @Autowired
    private LichSuThayDoiSanPhamService lichSuThayDoiSanPhamService;
```

Thêm method (cuối class, trước dấu `}` đóng class):
```java
    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @GetMapping("/{id}/lich-su")
    public List<LichSuThayDoiSanPhamResponse> getLichSu(@PathVariable Integer id) {
        return lichSuThayDoiSanPhamService.layLichSu(id);
    }
```

- [ ] **Step 2: Biên dịch backend, xác nhận không lỗi**

Run: `cd BackEnd && ./mvnw compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Thêm hàm frontend vào `SanPhamService.js`**

Thêm cuối file:
```js
export const getLichSu = (id) => get(`/api/san-pham/${id}/lich-su`);
```

- [ ] **Step 4: Kiểm tra thủ công bằng curl (cần backend đang chạy + token admin hợp lệ)**

Run: `curl -s http://localhost:8080/api/san-pham/1/lich-su -H "Authorization: Bearer <token>"`
Expected: HTTP 200, JSON array (rỗng nếu sản phẩm 1 chưa từng được sửa).

- [ ] **Step 5: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/SanPhamController.java \
        FrontEnd/QLBanMayTinh/src/services/SanPhamService.js
git commit -m "feat: endpoint GET /api/san-pham/{id}/lich-su"
```

---

## Task 6: Frontend — tách `ProductFormModal.vue` khỏi `ProductsTable.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/ProductFormModal.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue`

**Interfaces:**
- Produces: `<ProductFormModal v-model="Boolean" mode="'create'|'edit'" :san-pham-id="Number|null" @saved="..." />` — dùng lại ở Task 9 (nút "Chỉnh sửa" trong `SanPhamDetailPage.vue`).

- [ ] **Step 1: Tạo `ProductFormModal.vue` — phần `<script setup>`**

```vue
<script setup>
import { reactive, ref, watch } from "vue";
import { t } from "../../i18n/index.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as SanPhamService from "../../services/SanPhamService.js";
import * as ChiTietSanPhamService from "../../services/ChiTietSanPhamService.js";
import * as DanhMucService from "../../services/DanhMucService.js";
import * as DmService from "../../services/DmService.js";
import { authHeaders } from "../../services/api.js";
import { ProductsStore } from "../../stores/products.js";
import { SuppliersStore, ensureSuppliers } from "../../stores/suppliers.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: "create" },
  sanPhamId: { type: Number, default: null },
});
const emit = defineEmits(["update:modelValue", "saved"]);

const suppliers = ref([]);
const categories = ref([]);
const brands = ref([]);
const cpuList = ref([]);
const ramList = ref([]);
const oCungList = ref([]);
const gpuList = ref([]);

let productRefDataPromise = null;
const ensureProductRefData = () => {
  if (productRefDataPromise) return productRefDataPromise;
  productRefDataPromise = Promise.all([
    DanhMucService.getAll().catch(() => []),
    DmService.getThuongHieu().catch(() => []),
    DmService.getCpu().catch(() => []),
    DmService.getRam().catch(() => []),
    DmService.getOCung().catch(() => []),
    DmService.getGpu().catch(() => []),
    ensureSuppliers(),
  ]).then(([cat, br, cpu, ram, oc, gpu]) => {
    categories.value = cat;
    brands.value = br;
    cpuList.value = cpu;
    ramList.value = ram;
    oCungList.value = oc;
    gpuList.value = gpu;
    suppliers.value = SuppliersStore.items ?? [];
  });
  return productRefDataPromise;
};

const formError = ref("");
const saving = ref(false);
const soSerialMoi = ref("");
const imagePreview = ref("");
const imageFilePending = ref(null);

const PHAN_LOAI_TAG_OPTIONS = [
  { value: "gaming", label: "Gaming" },
  { value: "van_phong", label: "Văn phòng" },
  { value: "sinh_vien", label: "Sinh viên" },
  { value: "do_hoa", label: "Đồ họa" },
  { value: "ky_thuat", label: "Kỹ thuật" },
  { value: "macbook", label: "MacBook" },
  { value: "cu", label: "Cũ" },
  { value: "gia_re", label: "Giá rẻ" },
  { value: "linh_kien", label: "Linh kiện" },
];
const toggleTag = (value) => {
  const tags = form.phanLoaiTags
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  const idx = tags.indexOf(value);
  if (idx === -1) tags.push(value);
  else tags.splice(idx, 1);
  form.phanLoaiTags = tags.join(",");
  form.phanLoaiTen = tags
    .map((v) => PHAN_LOAI_TAG_OPTIONS.find((o) => o.value === v)?.label)
    .filter(Boolean)
    .join(", ");
};
const isTagSelected = (value) =>
  form.phanLoaiTags
    .split(",")
    .map((s) => s.trim())
    .includes(value);

const emptyForm = () => ({
  bienTheId: null,
  tenSanPham: "",
  thuongHieuId: null,
  danhMucId: null,
  nhaCungCapId: null,
  loaiSanPham: "",
  maSku: "",
  cpuId: null,
  ramId: null,
  oCungId: null,
  gpuId: null,
  kichThuocManHinh: "",
  heDieuHanh: "",
  pin: "",
  trongLuongKg: "",
  mauSac: "",
  giaBan: "",
  giaNhap: "",
  baoHanhThang: "",
  moTa: "",
  hinhAnhChinh: "",
  trangThai: "active",
  phanLoaiTags: "",
  phanLoaiTen: "",
});
const form = reactive(emptyForm());

const resetImageState = () => {
  imagePreview.value = "";
  imageFilePending.value = null;
};

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return;
    await ensureProductRefData();
    formError.value = "";
    soSerialMoi.value = "";
    resetImageState();
    if (props.mode === "edit") {
      const variants = (ProductsStore.items ?? []).filter((p) => p.sanPhamId === props.sanPhamId);
      const base = variants[0];
      if (!base) {
        emit("update:modelValue", false);
        return;
      }
      Object.assign(form, {
        bienTheId: null,
        tenSanPham: base.tenSanPham || "",
        thuongHieuId: base.thuongHieuId,
        danhMucId: base.danhMucId,
        nhaCungCapId: base.nhaCungCapId,
        loaiSanPham: base.loaiSanPham || "",
        maSku: base.maSku || "",
        cpuId: base.cpuId,
        ramId: base.ramId,
        oCungId: base.oCungId,
        gpuId: base.gpuId,
        kichThuocManHinh: base.kichThuocManHinh || "",
        heDieuHanh: base.heDieuHanh || "",
        pin: base.pin || "",
        trongLuongKg: base.trongLuongKg ?? "",
        mauSac: base.mauSac || "",
        giaBan: base.giaBan ?? "",
        giaNhap: base.giaNhap ?? "",
        baoHanhThang: base.baoHanhThang ?? "",
        moTa: base.moTa || "",
        hinhAnhChinh: base.hinhAnhChinh || "",
        trangThai: base.trangThai || "active",
        phanLoaiTags: base.phanLoaiTags || "",
        phanLoaiTen: base.phanLoaiTen || "",
      });
      imagePreview.value = base.hinhAnhChinh || "";
    } else {
      Object.assign(form, emptyForm());
    }
  },
);

const handleImageFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  imageFilePending.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const close = () => emit("update:modelValue", false);

const save = async () => {
  formError.value = "";
  if (saving.value) return;
  saving.value = true;
  try {
    if (imageFilePending.value) {
      const fd = new FormData();
      fd.append("file", imageFilePending.value);
      try {
        const upRes = await fetch("/api/upload/image", {
          method: "POST",
          headers: authHeaders(),
          body: fd,
        });
        if (upRes.ok) {
          const upData = await upRes.json();
          form.hinhAnhChinh = upData.url;
        } else {
          formError.value = t("admin.errors.uploadFailed", { status: upRes.status });
          return;
        }
      } catch (e) {
        formError.value = t("admin.errors.uploadError", { message: e.message });
        return;
      }
    }

    const body = {
      ...form,
      thuongHieuId: Number(form.thuongHieuId),
      danhMucId: Number(form.danhMucId),
      nhaCungCapId: form.nhaCungCapId ? Number(form.nhaCungCapId) : null,
      cpuId: form.cpuId ? Number(form.cpuId) : null,
      ramId: form.ramId ? Number(form.ramId) : null,
      oCungId: form.oCungId ? Number(form.oCungId) : null,
      gpuId: form.gpuId ? Number(form.gpuId) : null,
      giaBan: Number(form.giaBan),
      giaNhap: Number(form.giaNhap),
      trongLuongKg: form.trongLuongKg ? Number(form.trongLuongKg) : null,
      baoHanhThang: Number(form.baoHanhThang),
      ngayTao: props.mode === "edit" ? null : nowLocalIso(),
    };
    if (props.mode === "edit") {
      body.bienTheId = null;
    }
    try {
      const res = await SanPhamService.save(props.mode === "edit" ? props.sanPhamId : null, body);
      if (!res.ok) {
        formError.value = t("admin.errors.saveFailed", { status: res.status, text: await res.text() });
        return;
      }

      if (soSerialMoi.value.trim()) {
        const newList = await SanPhamService.getAll().catch(() => []);
        const newVariant = [...newList].reverse().find((p) => p.maSku === form.maSku);
        if (newVariant) {
          await ChiTietSanPhamService.create({
            bienTheId: newVariant.bienTheId,
            soSerial: soSerialMoi.value.trim(),
            trangThai: "trong_kho",
            ngayNhapKho: nowLocalIso(),
          }).catch(() => {});
        }
      }

      resetImageState();
      emit("saved");
      emit("update:modelValue", false);
    } catch (e) {
      formError.value = e.message;
    }
  } finally {
    saving.value = false;
  }
};
</script>
```

- [ ] **Step 2: Thêm phần `<template>` — copy nguyên khối modal từ `ProductsTable.vue`**

Mở `ProductsTable.vue` (bản gốc TRƯỚC khi sửa ở Step 3), copy nguyên khối từ dòng `<div\n    v-if="showProductModal"` (dòng 490) tới hết `</div>` đóng khối modal (dòng 1135) — đây là toàn bộ form thêm/sửa sản phẩm — dán vào `<template>` của `ProductFormModal.vue`, sau đó áp dụng đúng 4 phép thay thế toàn cục (tìm-thay, mỗi cụm xuất hiện đúng số lần ghi chú, không có nơi nào khác trong khối bị ảnh hưởng ngoài ý muốn):

1. `v-if="showProductModal"` → `v-if="modelValue"` (1 chỗ)
2. `showProductModal = false` → `close()` (2 chỗ: `@click.self` trên overlay, và nút "Hủy" ở footer)
3. `editMode` → `(mode === 'edit')` (mọi chỗ — dùng an toàn trong `v-if`, `:disabled`, và biểu thức 3 ngôi `editMode ? t(...) : t(...)`)
4. `saveProduct` → `save` (1 chỗ, nút "Cập nhật/Thêm mới" ở footer)

Thêm khối `<style scoped>` giống `ProductsTable.vue`:
```vue
<style scoped>
.text-light {
  color: var(--text-primary) !important;
}
</style>
```

- [ ] **Step 3: Sửa `ProductsTable.vue` — bỏ phần đã tách, dùng lại `ProductFormModal`**

Trong `<script setup>`:
- Đổi `import { ref, reactive, computed, onMounted } from "vue";` thành `import { ref, computed, onMounted } from "vue";` (bỏ `reactive` — không còn dùng trong file này).
- Xoá các import chỉ phục vụ form đã chuyển đi: `nowLocalIso` (từ `../../utils/datetime.js`), `ChiTietSanPhamService`, `DanhMucService`, `DmService`, `authHeaders`, `SuppliersStore, ensureSuppliers`. **Giữ lại**: `authHeaders` KHÔNG bị xoá nếu còn dùng nơi khác trong file — kiểm tra bằng cách grep `authHeaders` trong file sau khi sửa; nếu không còn chỗ nào dùng thì xoá import.
- Thêm import: `import ProductFormModal from "./ProductFormModal.vue";` (cạnh `import ProductDetailModal from "./ProductDetailModal.vue";`).
- Xoá các khai báo: `showProductModal` giữ lại (đổi tên ý nghĩa — vẫn dùng làm `v-model`), nhưng xoá `editMode`, `editingSanPhamId`, `formError`, `saving`, `soSerialMoi`, `imagePreview`, `imageFilePending`, `PHAN_LOAI_TAG_OPTIONS`, `toggleTag`, `isTagSelected`, `emptyForm`, `form`, `resetImageState`, `handleImageFile`, `saveProduct` — toàn bộ đã chuyển sang `ProductFormModal.vue`.
- Thêm 2 ref mới cạnh `showProductModal`:
```js
const showProductModal = ref(false);
const formMode = ref("create");
const formSanPhamId = ref(null);
```
- Thay 2 hàm `openAdd`/`openEdit` bằng:
```js
const openAdd = () => {
  formMode.value = "create";
  formSanPhamId.value = null;
  showProductModal.value = true;
};
const openEdit = (sanPhamId) => {
  formMode.value = "edit";
  formSanPhamId.value = sanPhamId;
  showProductModal.value = true;
};
```

Trong `<template>`: thay toàn bộ khối `<div v-if="showProductModal" ...>...</div>` (dòng 490-1135 bản gốc) bằng:
```vue
  <ProductFormModal
    v-model="showProductModal"
    :mode="formMode"
    :san-pham-id="formSanPhamId"
    @saved="refreshProducts"
  />
```

- [ ] **Step 4: Build frontend, xác nhận không lỗi**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công, không lỗi import thiếu/thừa.

- [ ] **Step 5: Kiểm tra thủ công trên trình duyệt**

Mở `/#/admin` → tab Sản phẩm → "+ Thêm sản phẩm" (tạo mới thành công) và "Sửa" trên 1 dòng có sẵn (form hiện đúng dữ liệu cũ, lưu thành công, danh sách cập nhật).

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/ProductFormModal.vue \
        FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue
git commit -m "refactor(frontend): tách ProductFormModal.vue dùng chung khỏi ProductsTable.vue"
```

---

## Task 7: Frontend — nút "Chi tiết" mở tab mới (Admin), giữ nguyên cho readonly

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue`

**Interfaces:**
- Consumes: route `/admin/san-pham/:id` (Task 9 mới tạo trang thật — trước khi Task 9 hoàn thành, tab mới sẽ vào trang trắng/404, chấp nhận được vì đây là task trung gian trong cùng 1 plan, không phải trạng thái release).

- [ ] **Step 1: Sửa hàm `openDetail`**

Từ:
```js
const openDetail = (sanPhamId, name) => {
  detailModalSanPhamId.value = sanPhamId;
  detailModalSanPhamName.value = name;
  showDetailModal.value = true;
};
```
Thành:
```js
const openDetail = (sanPhamId, name) => {
  if (props.readonly) {
    detailModalSanPhamId.value = sanPhamId;
    detailModalSanPhamName.value = name;
    showDetailModal.value = true;
    return;
  }
  window.open(`${location.origin}${location.pathname}#/admin/san-pham/${sanPhamId}`, "_blank");
};
```

- [ ] **Step 2: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công.

- [ ] **Step 3: Kiểm tra thủ công**

- Ở `/#/admin` (không readonly): bấm "Chi tiết" → mở tab mới với URL `#/admin/san-pham/<id>` (nội dung trang thật sẽ có sau Task 9).
- Ở `/#/staff` (readonly): bấm "Chi tiết" → vẫn mở `ProductDetailModal` như cũ, không mở tab mới.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue
git commit -m "feat(frontend): nút Chi tiết sản phẩm mở tab mới cho Admin"
```

---

## Task 8: Frontend — `BienTheTable.vue` thêm prop `filterSanPhamId`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/BienTheTable.vue`

**Interfaces:**
- Produces: prop `filter-san-pham-id` (Number, optional) — dùng ở Task 10 (`<BienTheTable :filter-san-pham-id="sanPhamId" />`).

- [ ] **Step 1: Sửa `defineProps`**

Từ:
```js
const props = defineProps({ readonly: { type: Boolean, default: false } });
```
Thành:
```js
const props = defineProps({
  readonly: { type: Boolean, default: false },
  filterSanPhamId: { type: Number, default: null },
});
```

- [ ] **Step 2: Sửa `filteredVariants`**

Từ:
```js
const filteredVariants = computed(() => {
  const q = variantSearch.value.trim().toLowerCase();
  const all = ProductsStore.items ?? [];
  if (!q) return all;
  return all.filter((p) =>
    (p.tenSanPham ?? '').toLowerCase().includes(q) ||
    (p.maSku ?? '').toLowerCase().includes(q)
  );
});
```
Thành:
```js
const filteredVariants = computed(() => {
  const q = variantSearch.value.trim().toLowerCase();
  let all = ProductsStore.items ?? [];
  if (props.filterSanPhamId != null) {
    all = all.filter((p) => p.sanPhamId === props.filterSanPhamId);
  }
  if (!q) return all;
  return all.filter((p) =>
    (p.tenSanPham ?? '').toLowerCase().includes(q) ||
    (p.maSku ?? '').toLowerCase().includes(q)
  );
});
```

- [ ] **Step 2: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công.

- [ ] **Step 3: Kiểm tra thủ công**

Ở `/#/admin` → tab Sản phẩm → Biến thể: danh sách vẫn hiện đầy đủ như trước (không truyền `filterSanPhamId` → không lọc, hành vi cũ không đổi).

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/BienTheTable.vue
git commit -m "feat(frontend): BienTheTable.vue thêm prop filterSanPhamId"
```

---

## Task 9: Frontend — route mới, `AdminPage.vue` wiring, `SanPhamDetailPage.vue` (shell + tab "Thông tin sản phẩm")

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/router/index.js`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/SanPhamDetailPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`

**Interfaces:**
- Consumes: `ProductFormModal.vue` (Task 6), `BienTheTable.vue` với `filter-san-pham-id` (Task 8).
- Produces: route `/admin/san-pham/:id`; `SanPhamDetailPage.vue` với 2 `<section>` rỗng (`activeTab === 'variants'` và `activeTab === 'history'`) — Task 10, 11 điền nội dung.

- [ ] **Step 1: Thêm route**

Trong `FrontEnd/QLBanMayTinh/src/router/index.js`, thêm ngay sau dòng route `/admin`:
```js
  { path: "/admin/san-pham/:id", name: "admin-san-pham-detail", component: AdminPage, meta: { requiresAuth: true, roles: ["admin"] } },
```

- [ ] **Step 2: Sửa `AdminPage.vue` — đọc route param**

Thêm import ở đầu file (cạnh `import { AuthStore } from "../stores/index.js";`):
```js
import { useRoute } from "vue-router";
```

Đổi dòng 62 từ:
```js
const currentPage = ref("dashboard");
```
Thành:
```js
const route = useRoute();
const currentPage = ref(route.params.id ? "san-pham-detail" : "dashboard");
```

Thêm ngay sau dòng 68 (`const selectedCustomerId = ref(null);`):
```js
const selectedSanPhamId = ref(route.params.id ? Number(route.params.id) : null);
```

Thêm import component (cạnh `import CustomerDetailPage from "../components/admin/CustomerDetailPage.vue";`):
```js
import SanPhamDetailPage from "../components/admin/SanPhamDetailPage.vue";
```

Trong `<template>`, thêm section mới ngay sau khối `<!-- ── Chi tiet khach hang ── -->` (sau dòng đóng `</section>` của `customer-detail`):
```vue
        <!-- ── Chi tiet san pham (mo qua tab moi, xem ProductsTable.vue openDetail) ── -->
        <section v-show="currentPage === 'san-pham-detail'">
          <SanPhamDetailPage v-if="selectedSanPhamId" :key="selectedSanPhamId" :san-pham-id="selectedSanPhamId" />
        </section>
```

- [ ] **Step 3: Tạo `SanPhamDetailPage.vue` — shell + tab "Thông tin sản phẩm"**

```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { t } from "../../i18n/index.js";
import { ProductsStore, ensureProducts, refreshProducts } from "../../stores/products.js";
import { formatPrice, formatDateTime, statusLabel } from "../../utils/adminFormat.js";
import ProductFormModal from "./ProductFormModal.vue";
import BienTheTable from "./BienTheTable.vue";
import { Image } from "@lucide/vue";

const props = defineProps({ sanPhamId: { type: Number, required: true } });
const router = useRouter();

onMounted(() => {
  ensureProducts();
});

const productVariants = computed(() =>
  (ProductsStore.items ?? []).filter((p) => p.sanPhamId === props.sanPhamId),
);
const productInfo = computed(() => productVariants.value[0] ?? null);
const variantCount = computed(() => productVariants.value.length);
const priceRange = computed(() => {
  if (!productVariants.value.length) return null;
  const prices = productVariants.value.map((p) => Number(p.giaBan));
  return { min: Math.min(...prices), max: Math.max(...prices) };
});
const totalStock = computed(() =>
  productVariants.value.reduce((sum, p) => sum + (Number(p.soLuongTon) || 0), 0),
);
const lastUpdated = computed(() =>
  productVariants.value.reduce(
    (latest, p) => (!latest || (p.ngayCapNhat && new Date(p.ngayCapNhat) > new Date(latest)) ? p.ngayCapNhat : latest),
    null,
  ),
);

const activeTab = ref("info");
const showEditModal = ref(false);
const onSaved = () => refreshProducts();
const back = () => router.push("/admin");
</script>

<template>
  <div v-if="!productInfo" class="text-secondary small">{{ t("admin.productDetail.loading") }}</div>
  <div v-else>
    <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
      <div>
        <button class="btn btn-sm btn-outline-secondary mb-2" @click="back">{{ t("common.back") }}</button>
        <div class="fw-bold" style="font-size: 1.1rem">
          {{ t("admin.productDetail.breadcrumb") }} &gt; {{ productInfo.tenSanPham }}
        </div>
      </div>
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="showEditModal = true">
        {{ t("admin.productDetail.editButton") }}
      </button>
    </div>

    <div class="d-flex gap-2 mb-3" style="border-bottom: 1px solid var(--border-color)">
      <button
        class="btn btn-sm"
        :class="activeTab === 'info' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
        style="border-radius: 6px 6px 0 0"
        @click="activeTab = 'info'"
      >
        {{ t("admin.productDetail.tabInfo") }}
      </button>
      <button
        class="btn btn-sm"
        :class="activeTab === 'variants' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
        style="border-radius: 6px 6px 0 0"
        @click="activeTab = 'variants'"
      >
        {{ t("admin.productDetail.tabVariants") }} ({{ variantCount }})
      </button>
      <button
        class="btn btn-sm"
        :class="activeTab === 'history' ? 'btn-warning text-dark fw-bold' : 'btn-outline-secondary'"
        style="border-radius: 6px 6px 0 0"
        @click="activeTab = 'history'"
      >
        {{ t("admin.productDetail.tabHistory") }}
      </button>
    </div>

    <section v-show="activeTab === 'info'">
      <div class="row g-3">
        <div class="col-md-6">
          <div class="rounded-3 p-3" style="background: var(--bg-card); border: 1px solid var(--border-color)">
            <div class="text-uppercase fw-bold mb-2" style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa">
              {{ t("admin.productDetail.cardBasic") }}
            </div>
            <div class="d-flex align-items-center gap-3 mb-3">
              <div
                class="rounded-2 d-flex align-items-center justify-content-center flex-shrink-0"
                style="width: 64px; height: 64px; background: var(--bg-card-inset); overflow: hidden"
              >
                <img
                  v-if="productInfo.hinhAnhChinh"
                  :src="productInfo.hinhAnhChinh"
                  :alt="productInfo.tenSanPham"
                  style="width: 100%; height: 100%; object-fit: cover"
                />
                <Image v-else :size="20" color="var(--text-muted)" />
              </div>
              <div>
                <div class="fw-bold">{{ productInfo.tenSanPham }}</div>
                <span class="badge" :class="productInfo.trangThai === 'active' ? 'bg-success' : 'bg-secondary'">{{
                  statusLabel(productInfo.trangThai)
                }}</span>
              </div>
            </div>
            <div class="small text-secondary d-flex flex-column gap-1">
              <div>{{ t("admin.productModal.brandLabel") }}: <span class="text-primary">{{ productInfo.tenThuongHieu }}</span></div>
              <div>{{ t("admin.productModal.categoryLabel") }}: <span class="text-primary">{{ productInfo.tenDanhMuc }}</span></div>
              <div>{{ t("admin.productModal.supplierLabel") }}: <span class="text-primary">{{ productInfo.tenNhaCungCap || t("admin.productModal.noneOption") }}</span></div>
              <div>{{ t("admin.productModal.typeLabel") }}: <span class="text-primary">{{ productInfo.loaiSanPham }}</span></div>
              <div>{{ t("admin.productDetail.releaseDate") }}: <span class="text-primary">{{ formatDateTime(productInfo.ngayTao) }}</span></div>
              <div v-if="productInfo.moTa">{{ t("admin.productModal.descLabel") }}: <span class="text-primary">{{ productInfo.moTa }}</span></div>
            </div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="rounded-3 p-3" style="background: var(--bg-card); border: 1px solid var(--border-color)">
            <div class="text-uppercase fw-bold mb-2" style="font-size: 0.65rem; letter-spacing: 0.1em; color: #60a5fa">
              {{ t("admin.productDetail.cardStats") }}
            </div>
            <div class="small text-secondary d-flex flex-column gap-2">
              <div>{{ t("admin.productDetail.variantCount") }}: <span class="text-primary fw-bold">{{ variantCount }}</span></div>
              <div v-if="priceRange">
                {{ t("admin.productDetail.priceRange") }}:
                <span class="text-primary fw-bold">{{ formatPrice(priceRange.min) }} – {{ formatPrice(priceRange.max) }}</span>
              </div>
              <div>{{ t("admin.productDetail.totalStock") }}: <span class="text-primary fw-bold">{{ totalStock }}</span></div>
              <div>{{ t("admin.productDetail.updatedAt") }}: <span class="text-primary">{{ formatDateTime(lastUpdated) }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-show="activeTab === 'variants'"></section>

    <section v-show="activeTab === 'history'"></section>

    <ProductFormModal v-model="showEditModal" mode="edit" :san-pham-id="sanPhamId" @saved="onSaved" />
  </div>
</template>
```

- [ ] **Step 4: Thêm i18n keys — `vi.js`**

Trong `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`, thêm khối mới ngay sau block `variants: { ... }` (trước `detailModal: {`):
```js
    productDetail: {
      loading: "Đang tải...",
      breadcrumb: "Sản phẩm",
      editButton: "Chỉnh sửa",
      tabInfo: "Thông tin sản phẩm",
      tabVariants: "Biến thể sản phẩm",
      tabHistory: "Lịch sử thay đổi",
      cardBasic: "Thông tin cơ bản",
      cardStats: "Thống kê nhanh",
      releaseDate: "Ngày ra mắt",
      variantCount: "Số biến thể",
      priceRange: "Khoảng giá bán",
      totalStock: "Tổng tồn kho",
      updatedAt: "Cập nhật gần nhất",
    },
```

(Các key `historyColXxx`/`fields` sẽ thêm ở Task 11 cùng lúc với nội dung tab Lịch sử.)

- [ ] **Step 5: Thêm i18n keys — `en.js`**

Cùng vị trí tương ứng trong `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`:
```js
    productDetail: {
      loading: "Loading...",
      breadcrumb: "Products",
      editButton: "Edit",
      tabInfo: "Product info",
      tabVariants: "Variants",
      tabHistory: "Change history",
      cardBasic: "Basic info",
      cardStats: "Quick stats",
      releaseDate: "Release date",
      variantCount: "Variant count",
      priceRange: "Price range",
      totalStock: "Total stock",
      updatedAt: "Last updated",
    },
```

- [ ] **Step 6: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công.

- [ ] **Step 7: Kiểm tra thủ công**

Từ `/#/admin` → tab Sản phẩm → bấm "Chi tiết" 1 sản phẩm → tab mới mở đúng URL, hiện đúng breadcrumb, 2 card thông tin đúng dữ liệu, nút "Chỉnh sửa" mở form và lưu thành công. Bấm "← Quay lại" → về `/#/admin`.

- [ ] **Step 8: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/router/index.js \
        FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue \
        FrontEnd/QLBanMayTinh/src/components/admin/SanPhamDetailPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(frontend): route + trang chi tiết sản phẩm, tab Thông tin sản phẩm"
```

---

## Task 10: Frontend — tab "Biến thể sản phẩm (N)"

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/SanPhamDetailPage.vue`

**Interfaces:**
- Consumes: `BienTheTable.vue` prop `filter-san-pham-id` (Task 8).

- [ ] **Step 1: Điền nội dung section "variants"**

Trong `SanPhamDetailPage.vue`, thay:
```vue
    <section v-show="activeTab === 'variants'"></section>
```
Thành:
```vue
    <section v-show="activeTab === 'variants'">
      <BienTheTable :filter-san-pham-id="sanPhamId" />
    </section>
```

- [ ] **Step 2: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công.

- [ ] **Step 3: Kiểm tra thủ công**

Ở trang chi tiết sản phẩm, chuyển sang tab "Biến thể sản phẩm (N)" — số N khớp số dòng hiện ra, chỉ gồm biến thể của đúng sản phẩm đang xem (so với tab "Biến thể" độc lập ở sidebar — vẫn hiện đủ mọi sản phẩm, không bị ảnh hưởng).

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/SanPhamDetailPage.vue
git commit -m "feat(frontend): tab Biến thể sản phẩm trong trang chi tiết"
```

---

## Task 11: Frontend — tab "Lịch sử thay đổi"

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/SanPhamDetailPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`

**Interfaces:**
- Consumes: `SanPhamService.getLichSu(id)` (Task 5).

- [ ] **Step 1: Sửa `SanPhamDetailPage.vue` — thêm import, state, load lịch sử**

Thêm import:
```js
import * as SanPhamService from "../../services/SanPhamService.js";
```

Đổi:
```js
onMounted(() => {
  ensureProducts();
});
```
Thành:
```js
const history = ref([]);
const historyLoading = ref(false);
const loadHistory = async () => {
  historyLoading.value = true;
  try {
    history.value = await SanPhamService.getLichSu(props.sanPhamId);
  } catch (e) {
    history.value = [];
  } finally {
    historyLoading.value = false;
  }
};

onMounted(() => {
  ensureProducts();
  loadHistory();
});
```

- [ ] **Step 2: Điền nội dung section "history"**

Thay:
```vue
    <section v-show="activeTab === 'history'"></section>
```
Thành:
```vue
    <section v-show="activeTab === 'history'">
      <div v-if="historyLoading" class="text-secondary small">{{ t("admin.productDetail.loading") }}</div>
      <div v-else class="table-responsive">
        <table
          class="table table-hover table-sm align-middle"
          style="--bs-table-bg: var(--bg-card); --bs-table-color: var(--text-primary); --bs-table-border-color: var(--border-color-soft)"
        >
          <thead>
            <tr>
              <th>{{ t("admin.productDetail.historyColTime") }}</th>
              <th>{{ t("admin.productDetail.historyColUser") }}</th>
              <th>{{ t("admin.productDetail.historyColTarget") }}</th>
              <th>{{ t("admin.productDetail.historyColField") }}</th>
              <th>{{ t("admin.productDetail.historyColOld") }}</th>
              <th>{{ t("admin.productDetail.historyColNew") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="h in history" :key="h.lichSuId">
              <td class="text-secondary" style="font-size: 0.78rem">{{ formatDateTime(h.thoiGian) }}</td>
              <td>{{ h.tenNhanVien || t("admin.productDetail.historyUnknownUser") }}</td>
              <td>
                <span v-if="h.doiTuong === 'bien_the'">{{ t("admin.productDetail.historyTargetVariant") }} ({{ h.maSku }})</span>
                <span v-else>{{ t("admin.productDetail.historyTargetProduct") }}</span>
              </td>
              <td>{{ t(`admin.productDetail.fields.${h.tenTruong}`) }}</td>
              <td class="text-secondary">{{ h.giaTriCu ?? "—" }}</td>
              <td class="text-primary">{{ h.giaTriMoi ?? "—" }}</td>
            </tr>
            <tr v-if="history.length === 0">
              <td colspan="6" class="text-center text-secondary">{{ t("admin.productDetail.historyEmpty") }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
```

- [ ] **Step 3: Thêm i18n keys — `vi.js`**

Trong khối `productDetail: { ... }` đã tạo ở Task 9, thêm các key sau vào cuối (trước dấu `}` đóng khối):
```js
      historyColTime: "Thời gian",
      historyColUser: "Người sửa",
      historyColTarget: "Đối tượng",
      historyColField: "Trường",
      historyColOld: "Giá trị cũ",
      historyColNew: "Giá trị mới",
      historyTargetProduct: "Sản phẩm",
      historyTargetVariant: "Biến thể",
      historyEmpty: "Chưa có thay đổi nào được ghi nhận",
      historyUnknownUser: "—",
      fields: {
        tenSanPham: "Tên sản phẩm",
        thuongHieuId: "Thương hiệu",
        danhMucId: "Danh mục",
        nhaCungCapId: "Nhà cung cấp",
        loaiSanPham: "Loại sản phẩm",
        moTa: "Mô tả",
        hinhAnhChinh: "Hình ảnh chính",
        trangThai: "Trạng thái",
        maSku: "Mã SKU",
        giaNhap: "Giá nhập",
        giaBan: "Giá bán",
        baoHanhThang: "Bảo hành (tháng)",
        hinhAnhBienThe: "Hình ảnh biến thể",
        mauSac: "Màu sắc",
        cpuId: "CPU",
        ramId: "RAM",
        oCungId: "Ổ cứng",
        gpuId: "GPU",
        kichThuocManHinh: "Màn hình",
        heDieuHanh: "Hệ điều hành",
        pin: "Pin",
        trongLuongKg: "Trọng lượng (kg)",
      },
```

- [ ] **Step 4: Thêm i18n keys — `en.js`**

Cùng vị trí tương ứng trong khối `productDetail: { ... }` của `en.js`:
```js
      historyColTime: "Time",
      historyColUser: "Changed by",
      historyColTarget: "Target",
      historyColField: "Field",
      historyColOld: "Old value",
      historyColNew: "New value",
      historyTargetProduct: "Product",
      historyTargetVariant: "Variant",
      historyEmpty: "No changes recorded yet",
      historyUnknownUser: "—",
      fields: {
        tenSanPham: "Product name",
        thuongHieuId: "Brand",
        danhMucId: "Category",
        nhaCungCapId: "Supplier",
        loaiSanPham: "Product type",
        moTa: "Description",
        hinhAnhChinh: "Main image",
        trangThai: "Status",
        maSku: "SKU",
        giaNhap: "Cost price",
        giaBan: "Sell price",
        baoHanhThang: "Warranty (months)",
        hinhAnhBienThe: "Variant image",
        mauSac: "Color",
        cpuId: "CPU",
        ramId: "RAM",
        oCungId: "Storage",
        gpuId: "GPU",
        kichThuocManHinh: "Screen size",
        heDieuHanh: "OS",
        pin: "Battery",
        trongLuongKg: "Weight (kg)",
      },
```

- [ ] **Step 5: Build frontend**

Run: `cd FrontEnd/QLBanMayTinh && npm run build`
Expected: build thành công.

- [ ] **Step 6: Kiểm tra thủ công đầu-cuối**

1. Sửa 1 sản phẩm (đổi tên, trạng thái) qua nút "Chỉnh sửa" ở trang chi tiết.
2. Sửa 1 biến thể của sản phẩm đó qua tab "Biến thể sản phẩm".
3. Chuyển sang tab "Lịch sử thay đổi" → thấy đủ dòng log cho từng trường đã đổi, đúng cũ/mới, đúng tên người sửa, đúng nhãn "Sản phẩm"/"Biến thể (SKU)".

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/SanPhamDetailPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js
git commit -m "feat(frontend): tab Lịch sử thay đổi trong trang chi tiết sản phẩm"
```
