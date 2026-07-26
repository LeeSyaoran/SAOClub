# Vòng quay may mắn — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khách hàng tốn điểm tích lũy để quay 1 vòng quay may mắn, trúng thì nhận 1 phiếu giảm giá cá nhân mới (giá trị lấy từ 1 khuyến mãi đang active), admin cấu hình được điểm/lượt quay + % trượt.

**Architecture:** Backend thêm 1 service/controller mới (`VongQuayService`/`VongQuayController`) tái dùng nguyên cơ chế khóa điểm + sinh `PhieuGiamGiaCaNhan` đã có ở `PhieuGiamGiaCaNhanService`, chỉ đổi nguồn "phần thưởng" từ `dm_doi_thuong` sang `khuyen_mai`. Frontend thêm 1 component vẽ vòng quay bằng CSS `conic-gradient` (không cần thư viện ngoài), nhúng vào tab mới trong `AccountPage.vue`; admin cấu hình chèn vào section Khuyến mãi có sẵn trong `AdminPage.vue`.

**Tech Stack:** Spring Boot (Java 17) + Hibernate/JPA + SQL Server, Vue 3 `<script setup>`, không thêm dependency mới ở cả 2 phía.

## Global Constraints

- Toàn bộ thay đổi DB phải idempotent trong `Database/QLBanMayTinh.sql` (`IF NOT EXISTS ... BEGIN CREATE TABLE ... END`) — người dùng luôn chạy lại NGUYÊN file này mỗi lần, không được viết migration rời.
- Phiếu giảm giá do vòng quay tạo ra dùng `doiThuong = null` — KHÔNG được đổi entity/bảng `phieu_giam_gia_ca_nhan` hiện có.
- Random hóa (miss-rate, chọn khuyến mãi trúng) phải quyết định ở SERVER, không tin client.
- Không thêm bảng/UI "catalog khuyến mãi riêng cho vòng quay" — luôn lấy trực tiếp, động, từ `khuyen_mai WHERE trang_thai='active'` còn hiệu lực ngày.
- Không thêm dependency frontend mới cho việc vẽ vòng quay (CSS `conic-gradient` thuần).
- Spec đầy đủ: `docs/superpowers/specs/2026-07-26-vong-quay-may-man-design.md`.

---

## Task 1: Bảng dữ liệu mới trong `Database/QLBanMayTinh.sql`

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (thêm khối mới, đặt ngay sau khối tạo bảng `phieu_giam_gia_ca_nhan`, dòng ~2326)

**Interfaces:**
- Produces: bảng `cau_hinh_vong_quay(id, diem_moi_luot, ty_le_truot, ngay_cap_nhat)` và `lich_su_quay(id, khach_hang_id, ngay_quay, ket_qua, khuyen_mai_id, phieu_giam_gia_ca_nhan_id, diem_da_tru)` — Task 2 map trực tiếp 2 bảng này thành entity.

- [ ] **Step 1: Tìm điểm chèn**

Mở `Database/QLBanMayTinh.sql`, tìm khối:
```sql
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'phieu_giam_gia_ca_nhan')
BEGIN
    CREATE TABLE phieu_giam_gia_ca_nhan (
        ...
    );
END
GO
```
(khoảng dòng 2308-2327). Chèn khối mới NGAY SAU dòng `GO` kết thúc khối này.

- [ ] **Step 2: Thêm 2 bảng mới**

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

- [ ] **Step 3: Chạy lại toàn bộ file trên SQL Server (SSMS hoặc sqlcmd), xác nhận không lỗi**

Không tạo dòng mặc định cho `cau_hinh_vong_quay` ở đây (Task 3 tự tạo lúc `GET` đầu tiên) — chạy xong file, chạy thử:
```sql
SELECT * FROM cau_hinh_vong_quay;  -- rỗng, không lỗi
SELECT * FROM lich_su_quay;        -- rỗng, không lỗi
```
Chạy lại LẦN NỮA toàn bộ file — xác nhận vẫn không lỗi (đúng tinh thần idempotent).

- [ ] **Step 4: Commit**

```bash
git add "Database/QLBanMayTinh.sql"
git commit -m "feat(db): thêm bảng cau_hinh_vong_quay và lich_su_quay cho vòng quay may mắn"
```

---

## Task 2: Backend — Entity, Repository, DTO

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/entity/CauHinhVongQuay.java`
- Create: `BackEnd/src/main/java/com/example/backend/entity/LichSuQuay.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/CauHinhVongQuayRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/LichSuQuayRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/KhuyenMaiRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/CauHinhVongQuayResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/KetQuaQuayResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/LichSuQuayResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/CauHinhVongQuayRequest.java`

**Interfaces:**
- Consumes: bảng `cau_hinh_vong_quay`/`lich_su_quay` (Task 1), entity `KhachHang`, `KhuyenMai`, `PhieuGiamGiaCaNhan` đã có sẵn.
- Produces: `CauHinhVongQuayRepository.findById(1)`/`.save(...)`; `LichSuQuayRepository.save(...)`/`.findResponsesByKhachHangId(Integer)`; `KhuyenMaiRepository.findActiveKhaDung()`; 3 Response DTO + 1 Request DTO với constructor/field chính xác như dưới — Task 3 (`VongQuayService`) dùng trực tiếp các type này.

- [ ] **Step 1: Tạo `entity/CauHinhVongQuay.java`**

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

- [ ] **Step 2: Tạo `entity/LichSuQuay.java`**

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
@Table(name = "lich_su_quay")
public class LichSuQuay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

- [ ] **Step 3: Tạo `response/CauHinhVongQuayResponse.java`**

```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CauHinhVongQuayResponse {
    private Integer diemMoiLuot;
    private Integer tyLeTruot;
    private List<KhuyenMaiResponse> khuyenMaiKhaDung;
}
```

- [ ] **Step 4: Tạo `response/KetQuaQuayResponse.java`**

```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class KetQuaQuayResponse {
    private String ketQua; // "trung" | "truot"
    private KhuyenMaiResponse khuyenMai;             // null nếu trượt
    private PhieuGiamGiaCaNhanResponse phieuGiamGia; // null nếu trượt
    private Integer diemConLai;
}
```

- [ ] **Step 5: Tạo `response/LichSuQuayResponse.java`**

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
public class LichSuQuayResponse {
    private Integer id;
    private LocalDateTime ngayQuay;
    private String ketQua;
    private String tenKhuyenMai; // null nếu trượt
    private Integer diemDaTru;
}
```

- [ ] **Step 6: Tạo `request/CauHinhVongQuayRequest.java`**

```java
package com.example.backend.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CauHinhVongQuayRequest {

    @NotNull(message = "Điểm mỗi lượt không được để trống")
    @Min(value = 1, message = "Điểm mỗi lượt phải lớn hơn 0")
    private Integer diemMoiLuot;

    @NotNull(message = "Tỷ lệ trượt không được để trống")
    @Min(value = 0, message = "Tỷ lệ trượt phải từ 0 đến 100")
    @Max(value = 100, message = "Tỷ lệ trượt phải từ 0 đến 100")
    private Integer tyLeTruot;
}
```

- [ ] **Step 7: Tạo `repository/CauHinhVongQuayRepository.java`**

```java
package com.example.backend.repository;

import com.example.backend.entity.CauHinhVongQuay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CauHinhVongQuayRepository extends JpaRepository<CauHinhVongQuay, Integer> {
}
```

- [ ] **Step 8: Tạo `repository/LichSuQuayRepository.java`**

```java
package com.example.backend.repository;

import com.example.backend.entity.LichSuQuay;
import com.example.backend.response.LichSuQuayResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuQuayRepository extends JpaRepository<LichSuQuay, Integer> {

    // Projection thẳng ra Response (đúng pattern ChiTietDonHangSerialRepository.findByDonHangId)
    // — tránh N+1 lazy-load lichSu.khuyenMai.tenKhuyenMai riêng cho mỗi dòng.
    @Query("SELECT new com.example.backend.response.LichSuQuayResponse(" +
           "l.id, l.ngayQuay, l.ketQua, k.tenKhuyenMai, l.diemDaTru) " +
           "FROM LichSuQuay l LEFT JOIN l.khuyenMai k " +
           "WHERE l.khachHang.khachHangId = :khachHangId ORDER BY l.ngayQuay DESC")
    List<LichSuQuayResponse> findResponsesByKhachHangId(@Param("khachHangId") Integer khachHangId);
}
```

- [ ] **Step 9: Thêm query vào `repository/KhuyenMaiRepository.java`**

File hiện tại (`@Query`/`List` đã import sẵn):
```java
package com.example.backend.repository;

import com.example.backend.entity.KhuyenMai;
import com.example.backend.response.KhuyenMaiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
    @Query("SELECT new com.example.backend.response.KhuyenMaiResponse(k.khuyenMaiId, k.maKhuyenMai, k.tenKhuyenMai, k.loai, k.giaTri, k.giaTriToiDa, k.donHangToiThieu, k.ngayBatDau, k.ngayKetThuc, k.soLuongToiDa, k.soLanDaDung, k.trangThai, k.ngayTao) FROM KhuyenMai k")
    List<KhuyenMaiResponse> hienThiKhuyenMai();
}
```
Thêm method mới NGAY TRƯỚC dấu `}` đóng interface:
```java
    // Khuyến mãi đang thực sự dùng được — active + trong khoảng ngày hiệu lực. Dùng chung
    // cho cả việc vẽ vòng quay (GET cau-hinh) lẫn random chọn thưởng (POST quay), tránh 2 nơi
    // lặp lại điều kiện lọc.
    @Query("SELECT k FROM KhuyenMai k WHERE k.trangThai = 'active' " +
           "AND k.ngayBatDau <= CURRENT_TIMESTAMP AND k.ngayKetThuc >= CURRENT_TIMESTAMP")
    List<KhuyenMai> findActiveKhaDung();
```

- [ ] **Step 10: Build để xác nhận không lỗi compile**

```bash
cd BackEnd && ./mvnw.cmd compile -q
```
Expected: build thành công, không lỗi. (Chưa có test tự động cho bước này — thuần mapping dữ liệu, được kiểm chứng gián tiếp qua test của Task 3 và bước verify sống ở Task 9.)

- [ ] **Step 11: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/CauHinhVongQuay.java \
        BackEnd/src/main/java/com/example/backend/entity/LichSuQuay.java \
        BackEnd/src/main/java/com/example/backend/repository/CauHinhVongQuayRepository.java \
        BackEnd/src/main/java/com/example/backend/repository/LichSuQuayRepository.java \
        BackEnd/src/main/java/com/example/backend/repository/KhuyenMaiRepository.java \
        BackEnd/src/main/java/com/example/backend/response/CauHinhVongQuayResponse.java \
        BackEnd/src/main/java/com/example/backend/response/KetQuaQuayResponse.java \
        BackEnd/src/main/java/com/example/backend/response/LichSuQuayResponse.java \
        BackEnd/src/main/java/com/example/backend/request/CauHinhVongQuayRequest.java
git commit -m "feat(backend): entity/repository/DTO cho vòng quay may mắn"
```

---

## Task 3: Backend — `VongQuayService` (TDD)

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/service/VongQuayService.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/VongQuayServiceTest.java`

**Interfaces:**
- Consumes: mọi type từ Task 2, cộng `KhachHangRepository.findWithLockByKhachHangId(Integer)` và `TaiKhoanRepository.findByUsername(String)` đã có sẵn trong codebase.
- Produces: `VongQuayService.getCauHinhChoKhachHang()`, `.capNhatCauHinh(CauHinhVongQuayRequest)`, `.quay()`, `.getLichSuCuaToi()` — Task 4 (`VongQuayController`) gọi trực tiếp 4 method này.

**Mẹo test không cần mock `Random`:** dùng giá trị biên `tyLeTruot=0` (không bao giờ trượt) hoặc `tyLeTruot=100` (luôn trượt), và danh sách khuyến mãi chỉ có ĐÚNG 1 phần tử khi cần trúng chắc chắn (`Random.nextInt(1)` luôn trả về 0) — loại bỏ hoàn toàn tính ngẫu nhiên khỏi test mà không cần đổi code service để "test được".

- [ ] **Step 1: Viết test file (sẽ fail vì `VongQuayService` chưa tồn tại)**

```java
package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.KhuyenMai;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.CauHinhVongQuay;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.CauHinhVongQuayRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.KhuyenMaiRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.KetQuaQuayResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VongQuayServiceTest {

    @Mock private CauHinhVongQuayRepository cauHinhRepository;
    @Mock private LichSuQuayRepository lichSuQuayRepository;
    @Mock private KhuyenMaiRepository khuyenMaiRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;

    @InjectMocks
    private VongQuayService service;

    @BeforeEach
    void setUp() {
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsKhachHang(String username, Integer khachHangId, int diemTichLuy) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        kh.setDiemTichLuy(diemTichLuy);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        when(taiKhoanRepository.findByUsername(username)).thenReturn(Optional.of(tk));
        lenient().when(khachHangRepository.findWithLockByKhachHangId(khachHangId)).thenReturn(Optional.of(kh));
    }

    private void loginAsNhanVien(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("nhan_vien");
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(null);
        when(taiKhoanRepository.findByUsername(username)).thenReturn(Optional.of(tk));
    }

    private CauHinhVongQuay cauHinh(int diemMoiLuot, int tyLeTruot) {
        return new CauHinhVongQuay(1, diemMoiLuot, tyLeTruot, LocalDateTime.now());
    }

    private KhuyenMai khuyenMaiActive(Integer id, String loai, long giaTri) {
        KhuyenMai k = new KhuyenMai();
        k.setKhuyenMaiId(id);
        k.setMaKhuyenMai("KM" + id);
        k.setTenKhuyenMai("Khuyến mãi " + id);
        k.setLoai(loai);
        k.setGiaTri(BigDecimal.valueOf(giaTri));
        k.setTrangThai("active");
        k.setNgayTao(LocalDateTime.now());
        return k;
    }

    @Test
    void quay_duDiemVaCoKhuyenMaiActive_tyLeTruot0_trungThuong() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0)));
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of(khuyenMaiActive(7, "percent", 20)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phieuGiamGiaCaNhanRepository.save(any(PhieuGiamGiaCaNhan.class))).thenAnswer(inv -> inv.getArgument(0));

        KetQuaQuayResponse res = service.quay();

        assertThat(res.getKetQua()).isEqualTo("trung");
        assertThat(res.getKhuyenMai().getKhuyenMaiId()).isEqualTo(7);
        assertThat(res.getDiemConLai()).isEqualTo(900); // 1000 - 100
        verify(khachHangRepository).save(argThat(kh -> kh.getDiemTichLuy() == 900));
        verify(phieuGiamGiaCaNhanRepository).save(argThat(p ->
                p.getDoiThuong() == null && "percent".equals(p.getLoai())
                        && p.getGiaTri().compareTo(BigDecimal.valueOf(20)) == 0));
        verify(lichSuQuayRepository).save(argThat(l -> "trung".equals(l.getKetQua()) && l.getDiemDaTru() == 100));
    }

    @Test
    void quay_khongDuDiem_biChanKhongTruDiem() {
        loginAsKhachHang("khach1", 42, 50);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0)));

        assertThatThrownBy(() -> service.quay())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đủ điểm");
        verify(khachHangRepository, never()).save(any());
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
        verify(lichSuQuayRepository, never()).save(any());
    }

    @Test
    void quay_tyLeTruot100_luonTruotNhungVanTruDiem() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 100)));
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of(khuyenMaiActive(7, "percent", 20)));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        KetQuaQuayResponse res = service.quay();

        assertThat(res.getKetQua()).isEqualTo("truot");
        assertThat(res.getKhuyenMai()).isNull();
        assertThat(res.getDiemConLai()).isEqualTo(900);
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
        verify(lichSuQuayRepository).save(argThat(l -> "truot".equals(l.getKetQua())));
    }

    @Test
    void quay_danhSachKhuyenMaiRong_luonTruotBatKeTyLeTruot() {
        loginAsKhachHang("khach1", 42, 1000);
        when(cauHinhRepository.findById(1)).thenReturn(Optional.of(cauHinh(100, 0))); // 0% trượt
        when(khuyenMaiRepository.findActiveKhaDung()).thenReturn(List.of()); // nhưng rỗng
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        KetQuaQuayResponse res = service.quay();

        assertThat(res.getKetQua()).isEqualTo("truot");
        verify(phieuGiamGiaCaNhanRepository, never()).save(any());
    }

    @Test
    void quay_taiKhoanKhongPhaiKhachHang_nemAccessDenied() {
        loginAsNhanVien("nhanvien1");

        assertThatThrownBy(() -> service.quay())
                .isInstanceOf(AccessDeniedException.class);
        verify(khachHangRepository, never()).findWithLockByKhachHangId(any());
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail vì thiếu class `VongQuayService`**

```bash
cd BackEnd && ./mvnw.cmd test -Dtest=VongQuayServiceTest -q
```
Expected: FAIL biên dịch — "cannot find symbol: class VongQuayService".

- [ ] **Step 3: Viết `service/VongQuayService.java`**

```java
package com.example.backend.service;

import com.example.backend.entity.CauHinhVongQuay;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.KhuyenMai;
import com.example.backend.entity.LichSuQuay;
import com.example.backend.entity.PhieuGiamGiaCaNhan;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.CauHinhVongQuayRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.KhuyenMaiRepository;
import com.example.backend.repository.LichSuQuayRepository;
import com.example.backend.repository.PhieuGiamGiaCaNhanRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.CauHinhVongQuayRequest;
import com.example.backend.response.CauHinhVongQuayResponse;
import com.example.backend.response.KetQuaQuayResponse;
import com.example.backend.response.KhuyenMaiResponse;
import com.example.backend.response.LichSuQuayResponse;
import com.example.backend.response.PhieuGiamGiaCaNhanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

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

    // KhuyenMaiResponse chỉ có @AllArgsConstructor (13 field) — không có constructor nhận
    // thẳng KhuyenMai, nên map tường minh, dùng lại ở cả getCauHinhChoKhachHang() lẫn quay().
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
        // gia_tri của khuyen_mai là DECIMAL(18,2), của phieu_giam_gia_ca_nhan là DECIMAL(18,0)
        // — làm tròn khi clone để tránh lệch giữa entity Java và cột DB.
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

- [ ] **Step 4: Chạy test, xác nhận PASS**

```bash
cd BackEnd && ./mvnw.cmd test -Dtest=VongQuayServiceTest -q
```
Expected: 5/5 test PASS.

- [ ] **Step 5: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/VongQuayService.java \
        BackEnd/src/test/java/com/example/backend/service/VongQuayServiceTest.java
git commit -m "feat(backend): VongQuayService — logic quay thưởng, khoá điểm, sinh phiếu giảm giá"
```

---

## Task 4: Backend — `VongQuayController`

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/controller/VongQuayController.java`
- Test: `BackEnd/src/test/java/com/example/backend/controller/VongQuayAuthorizationTest.java`

**Interfaces:**
- Consumes: `VongQuayService` (Task 3).
- Produces: `GET /api/vong-quay/cau-hinh`, `PUT /api/vong-quay/cau-hinh`, `POST /api/vong-quay/quay`, `GET /api/vong-quay/lich-su/cua-toi` — Task 5 (`Service/VongQuayService.js` phía frontend) gọi trực tiếp 4 endpoint này.

- [ ] **Step 1: Viết test phân quyền (theo đúng pattern `PhieuTraHangAuthorizationTest.java` — kiểm tra annotation bằng reflection, không cần MockMvc/context thật)**

```java
package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

// Xác nhận VongQuayController phân quyền đúng: GET/POST mở cho mọi role đã đăng nhập (không
// có @PreAuthorize class-level, giống KhuyenMaiController), riêng PUT cấu hình chỉ staff.
class VongQuayAuthorizationTest {

    @Test
    void controller_khongCoPreAuthorizeCapClass() {
        assertThat(VongQuayController.class.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void capNhatCauHinh_chiChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        Method m = VongQuayController.class.getMethod("capNhatCauHinh",
                com.example.backend.request.CauHinhVongQuayRequest.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void quay_khongCoPreAuthorizeRieng() throws NoSuchMethodException {
        Method m = VongQuayController.class.getMethod("quay");
        assertThat(m.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void getCauHinh_khongCoPreAuthorizeRieng() throws NoSuchMethodException {
        Method m = VongQuayController.class.getMethod("getCauHinh");
        assertThat(m.getAnnotation(PreAuthorize.class)).isNull();
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail vì thiếu class `VongQuayController`**

```bash
cd BackEnd && ./mvnw.cmd test -Dtest=VongQuayAuthorizationTest -q
```
Expected: FAIL biên dịch.

- [ ] **Step 3: Viết `controller/VongQuayController.java`**

```java
package com.example.backend.controller;

import com.example.backend.entity.CauHinhVongQuay;
import com.example.backend.request.CauHinhVongQuayRequest;
import com.example.backend.response.CauHinhVongQuayResponse;
import com.example.backend.response.KetQuaQuayResponse;
import com.example.backend.response.LichSuQuayResponse;
import com.example.backend.service.VongQuayService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Không @PreAuthorize cấp class — GET mở cho mọi role đã đăng nhập (đúng pattern
// KhuyenMaiController), PUT chặn riêng ở method vì chỉ staff được đổi cấu hình. POST /quay
// không cần @PreAuthorize role vì VongQuayService tự chặn qua currentKhachHangId() (đúng
// pattern PhieuGiamGiaCaNhanController — chỉ khách hàng mới có KhachHang liên kết).
@RestController
@RequestMapping("/api/vong-quay")
public class VongQuayController {

    @Autowired
    private VongQuayService vongQuayService;

    @GetMapping("cau-hinh")
    public CauHinhVongQuayResponse getCauHinh() {
        return vongQuayService.getCauHinhChoKhachHang();
    }

    @PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
    @PutMapping("cau-hinh")
    public CauHinhVongQuay capNhatCauHinh(@Valid @RequestBody CauHinhVongQuayRequest req) {
        return vongQuayService.capNhatCauHinh(req);
    }

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

- [ ] **Step 4: Chạy test, xác nhận PASS**

```bash
cd BackEnd && ./mvnw.cmd test -Dtest=VongQuayAuthorizationTest -q
```
Expected: 4/4 test PASS.

- [ ] **Step 5: Chạy TOÀN BỘ test suite backend, xác nhận không phá vỡ gì có sẵn**

```bash
cd BackEnd && ./mvnw.cmd test -q
```
Expected: tất cả test (kể cả các test có từ trước) đều PASS.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/VongQuayController.java \
        BackEnd/src/test/java/com/example/backend/controller/VongQuayAuthorizationTest.java
git commit -m "feat(backend): VongQuayController — 4 endpoint REST cho vòng quay may mắn"
```

---

## Task 5: Frontend — Service + component `LuckyWheelPanel.vue`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/VongQuayService.js`
- Create: `FrontEnd/QLBanMayTinh/src/components/account/LuckyWheelPanel.vue`

**Interfaces:**
- Consumes: `get/post/put` từ `Service/api.js` (đã có sẵn); `Modal.vue`, `formatPrice.js`, `t()` từ `i18n/index.js` (đã có sẵn); i18n key `wheel.*` (Task 8 mới thêm — component gọi `t('wheel.xxx')`, key CHƯA tồn tại tới hết Task 8, chấp nhận hiện text key thô lúc test riêng lẻ component này).
- Produces: `<LuckyWheelPanel :points="Number" @spun="(diemConLai: Number) => void" />` — Task 6 (`AccountPage.vue`) dùng trực tiếp component + props/emit này.

- [ ] **Step 1: Tạo `Service/VongQuayService.js`**

```js
import { get, post, put } from './api.js';

export const getCauHinh = () => get('/api/vong-quay/cau-hinh');
export const capNhatCauHinh = (body) => put('/api/vong-quay/cau-hinh', body);
export const quay = () => post('/api/vong-quay/quay');
export const getLichSuCuaToi = () => get('/api/vong-quay/lich-su/cua-toi');
```

- [ ] **Step 2: Tạo `components/account/LuckyWheelPanel.vue`**

```vue
<script setup>
import { ref, computed, onMounted } from 'vue';
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';
import * as VongQuayService from '../../Service/VongQuayService.js';
import Modal from '../common/Modal.vue';

// points: điểm tích lũy hiện tại của khách — nhận từ AccountPage.vue (đã load sẵn cho
// badge điểm ở header), không tự fetch profile riêng trong component này.
const props = defineProps({
  points: { type: Number, default: 0 },
});
// spun: báo cho AccountPage.vue biết vừa quay xong (kèm điểm còn lại) để cập nhật lại badge.
const emit = defineEmits(['spun']);

const loading = ref(true);
const loadError = ref('');
const diemMoiLuot = ref(0);
// Cố định thứ tự ô sau khi load — không refetch giữa các lượt quay, tránh lệch chỉ số ô
// so với animate lúc component đã render.
const khuyenMaiKhaDung = ref([]);
const spinning = ref(false);
const rotation = ref(0);
const showResultModal = ref(false);
const lastResult = ref(null);
const spinError = ref('');

const sliceCount = computed(() => khuyenMaiKhaDung.value.length + 1); // +1 ô "Chúc may mắn lần sau"
const anglePerSlice = computed(() => 360 / sliceCount.value);

const SLICE_COLORS = ['#f43f5e', '#f59e0b', '#22c55e', '#3b82f6', '#a855f7', '#ec4899'];

const sliceLabel = (index) => {
  if (index === khuyenMaiKhaDung.value.length) return t('wheel.missSlice');
  const km = khuyenMaiKhaDung.value[index];
  return km.loai === 'percent' ? `-${km.giaTri}%` : `-${formatPrice(km.giaTri)}`;
};

const wheelBackground = computed(() => {
  const n = sliceCount.value;
  const stops = [];
  for (let i = 0; i < n; i++) {
    const color = SLICE_COLORS[i % SLICE_COLORS.length];
    stops.push(`${color} ${i * anglePerSlice.value}deg ${(i + 1) * anglePerSlice.value}deg`);
  }
  return `conic-gradient(${stops.join(', ')})`;
});

const canSpin = computed(() => !loading.value && !spinning.value && props.points >= diemMoiLuot.value);

const loadConfig = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const res = await VongQuayService.getCauHinh();
    diemMoiLuot.value = res.diemMoiLuot;
    khuyenMaiKhaDung.value = res.khuyenMaiKhaDung;
  } catch (e) {
    loadError.value = e.message || t('wheel.loadError');
  } finally {
    loading.value = false;
  }
};

onMounted(loadConfig);

const onSpin = async () => {
  if (!canSpin.value) return;
  spinning.value = true;
  spinError.value = '';
  try {
    const res = await VongQuayService.quay();
    const targetIndex = res.ketQua === 'truot'
      ? khuyenMaiKhaDung.value.length
      : khuyenMaiKhaDung.value.findIndex(k => k.khuyenMaiId === res.khuyenMai.khuyenMaiId);
    const slice = anglePerSlice.value;
    const targetAngleInCircle = 360 - (targetIndex * slice + slice / 2);
    // Quay thêm 5 vòng trọn rồi dừng đúng giữa ô targetIndex — trừ phần dư hiện tại để luôn
    // quay THEO CHIỀU THUẬN, không giật ngược khi rotation hiện tại lệch pha.
    rotation.value += 5 * 360 + targetAngleInCircle - (rotation.value % 360);
    lastResult.value = res;
    setTimeout(() => {
      spinning.value = false;
      showResultModal.value = true;
      emit('spun', res.diemConLai);
    }, 4000); // khớp đúng transition 4s ở CSS bên dưới
  } catch (e) {
    spinning.value = false;
    spinError.value = e.message || t('wheel.spinError');
  }
};
</script>

<template>
  <div class="d-flex flex-column align-items-center gap-4 py-4">
    <div v-if="loadError" class="alert alert-danger small">{{ loadError }}</div>
    <template v-else>
      <div class="position-relative" style="width:280px; height:280px;">
        <div class="position-absolute top-0 start-50 translate-middle-x" style="z-index:2; font-size:28px; margin-top:-14px;">🔻</div>
        <div class="rounded-circle position-relative"
             style="width:100%; height:100%; transition:transform 4s cubic-bezier(0.17,0.67,0.12,0.99);"
             :style="{ background: wheelBackground, transform: `rotate(${rotation}deg)` }">
          <div v-for="(_, i) in sliceCount" :key="i"
               class="position-absolute top-50 start-50 fw-bold text-white text-center"
               style="width:120px; margin-left:-60px; margin-top:-10px; font-size:12px; text-shadow:0 1px 3px rgba(0,0,0,0.5);"
               :style="{ transform: `rotate(${i * anglePerSlice + anglePerSlice / 2}deg) translateY(-100px)` }">
            {{ sliceLabel(i) }}
          </div>
        </div>
      </div>

      <div class="text-center">
        <div class="small" style="color:var(--text-secondary);">{{ t('wheel.costLabel', { points: diemMoiLuot }) }}</div>
        <button class="btn btn-warning fw-bold rounded-pill px-4 mt-2"
                :disabled="!canSpin"
                @click="onSpin">
          {{ spinning ? t('wheel.spinning') : t('wheel.spinButton') }}
        </button>
        <div v-if="spinError" class="alert alert-danger small mt-2 mb-0">{{ spinError }}</div>
        <div v-if="!loading && points < diemMoiLuot" class="small mt-2" style="color:var(--text-secondary);">
          {{ t('wheel.notEnoughPoints') }}
        </div>
      </div>
    </template>

    <Modal v-model="showResultModal" width="380px">
      <div v-if="lastResult" class="text-center">
        <template v-if="lastResult.ketQua === 'trung'">
          <div style="font-size:2.4rem;">🎉</div>
          <h5 class="fw-black mt-2" style="color:var(--text-heading);">{{ t('wheel.winTitle') }}</h5>
          <p class="mb-1" style="color:var(--text-primary);">
            {{ lastResult.khuyenMai.loai === 'percent'
                ? t('wheel.winPercent', { value: lastResult.khuyenMai.giaTri })
                : t('wheel.winFixed', { value: formatPrice(lastResult.khuyenMai.giaTri) }) }}
          </p>
          <div class="small" style="color:var(--text-secondary);">{{ t('wheel.winCode', { code: lastResult.phieuGiamGia.maPhieu }) }}</div>
        </template>
        <template v-else>
          <div style="font-size:2.4rem;">🍀</div>
          <h5 class="fw-black mt-2" style="color:var(--text-heading);">{{ t('wheel.missTitle') }}</h5>
          <p class="small" style="color:var(--text-secondary);">{{ t('wheel.missDesc') }}</p>
        </template>
        <button class="btn btn-sm btn-outline-secondary rounded-pill px-4 mt-3" @click="showResultModal = false">
          {{ t('common.close') }}
        </button>
      </div>
    </Modal>
  </div>
</template>
```

- [ ] **Step 3: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/Service/VongQuayService.js" \
        "FrontEnd/QLBanMayTinh/src/components/account/LuckyWheelPanel.vue"
git commit -m "feat(frontend): component LuckyWheelPanel + VongQuayService.js"
```

(Không verify sống ở bước này — text key `wheel.*` chưa có tới Task 8, và component chưa được nhúng vào trang nào tới Task 6. Verify tổng thể ở Task 9.)

---

## Task 6: Frontend — Nhúng vào `AccountPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`

**Interfaces:**
- Consumes: `LuckyWheelPanel.vue` (Task 5), `profile.diemTichLuy`/`fetchProfile()` đã có sẵn trong file (dòng 199, 212).

- [ ] **Step 1: Import component (đầu file, cạnh các import component khác, dòng ~25)**

Tìm:
```js
import Skeleton from "../components/common/Skeleton.vue";
```
Thêm ngay dưới:
```js
import LuckyWheelPanel from "../components/account/LuckyWheelPanel.vue";
```

- [ ] **Step 2: Thêm tab "wheel" vào mảng `TABS` (dòng 71-77)**

Tìm:
```js
const TABS = computed(() => [
  { id: "pending",   icon: "🕐", label: t("account.tabPending") },
  { id: "shipping",  icon: "🚚", label: t("account.tabShipping") },
  { id: "completed", icon: "✅", label: t("account.tabCompleted") },
  { id: "cancelled", icon: "❌", label: t("account.tabCancelled") },
  { id: "settings",  icon: "⚙️", label: t("account.tabSettings") },
]);
```
Sửa thành (thêm dòng "wheel" trước "settings"):
```js
const TABS = computed(() => [
  { id: "pending",   icon: "🕐", label: t("account.tabPending") },
  { id: "shipping",  icon: "🚚", label: t("account.tabShipping") },
  { id: "completed", icon: "✅", label: t("account.tabCompleted") },
  { id: "cancelled", icon: "❌", label: t("account.tabCancelled") },
  { id: "wheel",     icon: "🎡", label: t("account.tabWheel") },
  { id: "settings",  icon: "⚙️", label: t("account.tabSettings") },
]);
```

- [ ] **Step 3: Thêm block hiển thị — CHÈN GIỮA chuỗi `v-if`/`v-else-if`/`v-else` hiện có, KHÔNG được để lọt vào nhánh `v-else` của tab settings**

Cấu trúc hiện tại (dòng 349, 441, 540):
```
v-if="activeTab === 'pending' || activeTab === 'shipping'"        (dòng 349)
v-else-if="activeTab === 'completed' || activeTab === 'cancelled'" (dòng 441)
v-else  (dòng 540 — đây là nhánh MẶC ĐỊNH cho mọi tab còn lại, tức "settings")
```
Vì `v-else` ở dòng 540 khớp với BẤT KỲ giá trị `activeTab` nào chưa được nhánh trên khớp — nếu thêm tab `"wheel"` mà không chèn 1 nhánh `v-else-if` MỚI vào giữa, tab wheel sẽ vô tình hiện luôn nội dung tab Cài đặt. Tìm đoạn đóng của block `completed/cancelled` (dòng ~536-539):
```html
        </div>
      </div>

      <!-- ══ Tab: Cài đặt tài khoản ══ -->
      <div v-else class="d-flex flex-column gap-3 mx-auto" style="max-width:640px;">
```
Sửa thành (chèn 1 block `v-else-if` mới cho "wheel" NGAY TRƯỚC dòng `v-else` của settings):
```html
        </div>
      </div>

      <!-- ══ Tab: Vòng quay may mắn ══ -->
      <div v-else-if="activeTab === 'wheel'" class="d-flex flex-column mx-auto" style="max-width:640px;">
        <LuckyWheelPanel :points="profile?.diemTichLuy ?? 0" @spun="fetchProfile" />
      </div>

      <!-- ══ Tab: Cài đặt tài khoản ══ -->
      <div v-else class="d-flex flex-column gap-3 mx-auto" style="max-width:640px;">
```
(`@spun="fetchProfile"` — sau khi quay xong, gọi lại đúng hàm `fetchProfile()` đã có sẵn ở dòng 212 để cập nhật badge điểm ở header, giống hệt cách `redeemReward()` đang làm ở dòng 240. `fetchProfile` không nhận tham số nên bỏ qua giá trị `diemConLai` mà event `spun` phát ra — chấp nhận 1 lần gọi API dư để lấy đúng nguồn dữ liệu, không tự đồng bộ tay số điểm.)

- [ ] **Step 4: Xác nhận không còn chỗ nào khác dùng chuỗi literal `"settings"` cho logic điều hướng bị ảnh hưởng**

```bash
grep -n "'settings'" "FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue"
```
Expected: chỉ còn xuất hiện trong comment dòng 31 (`// 'pending' | 'shipping' | ... | 'settings'`) — không có logic nào khác cần sửa.

- [ ] **Step 5: Verify sống — khởi động frontend, đăng nhập `khachhang`/`123456`, vào trang Tài khoản**

```bash
cd "FrontEnd/QLBanMayTinh" && npm run dev
```
Mở trình duyệt, đăng nhập khách hàng, xác nhận: tab "🎡" xuất hiện giữa "Đã hủy/Trả hàng" và "Cài đặt tài khoản"; bấm vào tab đó hiện đúng `LuckyWheelPanel` (dù nhãn text còn hiện thô kiểu `wheel.spinButton` vì Task 8 chưa làm — CHẤP NHẬN Ở BƯỚC NÀY, chỉ cần xác nhận đúng component render, không lẫn sang tab Cài đặt); bấm lại tab "⚙️ Cài đặt" xác nhận vẫn hiện đúng nội dung cũ, không bị vỡ.

- [ ] **Step 6: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue"
git commit -m "feat(frontend): thêm tab Vòng quay may mắn vào AccountPage.vue"
```

---

## Task 7: Frontend — Cấu hình vòng quay trong `AdminPage.vue`

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `Service/VongQuayService.js` (Task 5).

- [ ] **Step 1: Import service (đầu file, cạnh import `KhuyenMaiService`, dòng 8)**

Tìm dòng import `KhuyenMaiService` và thêm ngay dưới:
```js
import * as VongQuayService from "../Service/VongQuayService.js";
```

- [ ] **Step 2: Thêm state + hàm load/save (cạnh khai báo `promotions`, dòng ~88)**

Tìm:
```js
const promotions = computed(() => PromotionsStore.items);
```
Thêm ngay dưới:
```js
// Cấu hình vòng quay may mắn — không dùng store riêng vì chỉ 1 dòng dữ liệu phẳng, chỉ
// dùng ở đúng section này (khác các store khác dùng chung nhiều nơi).
const wheelConfig = ref({ diemMoiLuot: 0, tyLeTruot: 0 });
const wheelConfigSaving = ref(false);
const wheelConfigError = ref("");
const loadWheelConfig = async () => {
  try {
    const res = await VongQuayService.getCauHinh();
    wheelConfig.value = { diemMoiLuot: res.diemMoiLuot, tyLeTruot: res.tyLeTruot };
  } catch (e) {
    wheelConfigError.value = e.message || t("admin.wheelConfig.loadError");
  }
};
const saveWheelConfig = async () => {
  wheelConfigSaving.value = true;
  wheelConfigError.value = "";
  try {
    // capNhatCauHinh() dùng put() (Service/api.js) — trả về Response THÔ, không tự parse
    // JSON và không tự throw khi !ok (khác get()). Phải tự kiểm tra res.ok, nếu không lỗi
    // lưu (vd validate 400 do nhập điểm/lượt <=0) sẽ bị nuốt im lặng, admin tưởng đã lưu.
    const res = await VongQuayService.capNhatCauHinh(wheelConfig.value);
    if (!res.ok) throw new Error(await res.text().catch(() => res.statusText));
  } catch (e) {
    wheelConfigError.value = e.message || t("admin.wheelConfig.saveError");
  } finally {
    wheelConfigSaving.value = false;
  }
};
```

- [ ] **Step 3: Gọi `loadWheelConfig()` cùng lúc với `fetchAll()` (dòng ~910)**

Tìm:
```js
  try {
    await fetchAll();
    await fetchProductSales();
  } catch (e) {
```
Sửa thành:
```js
  try {
    await fetchAll();
    await fetchProductSales();
    await loadWheelConfig();
  } catch (e) {
```

- [ ] **Step 4: Chèn khối UI vào đầu section khuyến mãi (trước dòng ~1320, khối "count + nút Thêm")**

Tìm:
```html
        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <div class="d-flex justify-content-between align-items-center mb-3">
```
Sửa thành (chèn khối cấu hình NGAY SAU thẻ `<section>`, TRƯỚC dòng count/nút Thêm):
```html
        <!-- ── Khuyen mai ── -->
        <section v-show="currentPage === 'promotions'">
          <div class="d-flex align-items-center flex-wrap gap-3 p-3 mb-3 rounded-3"
               style="background:var(--bg-card-inset); border:1px solid var(--border-color);">
            <span class="fw-bold small">{{ t('admin.wheelConfig.title') }}</span>
            <label class="small text-secondary mb-0">{{ t('admin.wheelConfig.pointsPerSpin') }}</label>
            <input v-model.number="wheelConfig.diemMoiLuot" type="number" min="1"
                   class="form-control form-control-sm" style="width:90px;" />
            <label class="small text-secondary mb-0">{{ t('admin.wheelConfig.missRate') }}</label>
            <input v-model.number="wheelConfig.tyLeTruot" type="number" min="0" max="100"
                   class="form-control form-control-sm" style="width:70px;" />
            <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="wheelConfigSaving" @click="saveWheelConfig">
              {{ t('admin.wheelConfig.save') }}
            </button>
            <span v-if="wheelConfigError" class="text-danger small">{{ wheelConfigError }}</span>
          </div>
          <div class="d-flex justify-content-between align-items-center mb-3">
```

- [ ] **Step 5: Verify sống — đăng nhập `admin`/`123456`, vào trang Khuyến mãi**

Xác nhận: khối cấu hình hiện phía trên bảng khuyến mãi với giá trị mặc định (100 / 30, do `VongQuayService.getOrCreateCauHinh()` tự tạo lúc `GET` đầu tiên); sửa 2 ô số rồi bấm nút Lưu (nhãn còn hiện thô `admin.wheelConfig.save` — CHẤP NHẬN, Task 8 mới thêm text); tải lại trang, xác nhận giá trị vừa lưu vẫn còn (không bị reset về mặc định).

- [ ] **Step 6: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue"
git commit -m "feat(frontend): khối cấu hình vòng quay trong trang Khuyến mãi (admin)"
```

---

## Task 8: i18n cho 5 ngôn ngữ

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`

**Interfaces:**
- Consumes: không có (chỉ text).
- Produces: mọi key `t('account.tabWheel')`, `t('wheel.*')`, `t('admin.wheelConfig.*')` mà Task 5-7 đã gọi.

- [ ] **Step 1: `vi.js` — thêm `tabWheel` (cạnh `tabSettings`, dòng 377)**

Tìm:
```js
    tabCancelled: "Đã hủy/Trả hàng",
    tabSettings: "Cài đặt tài khoản",
```
Sửa thành:
```js
    tabCancelled: "Đã hủy/Trả hàng",
    tabWheel: "Vòng quay may mắn",
    tabSettings: "Cài đặt tài khoản",
```

- [ ] **Step 2: `vi.js` — thêm khối `wheel` (cạnh khối `rewards` trong `account`, dòng ~419-432, chèn SAU dấu đóng `}` của `rewards`)**

Tìm:
```js
      redeemError: "Không thể kết nối để đổi thưởng, vui lòng thử lại.",
    },
  },
```
Sửa thành:
```js
      redeemError: "Không thể kết nối để đổi thưởng, vui lòng thử lại.",
    },
    wheel: {
      costLabel: "Mỗi lượt quay tốn {points} điểm",
      spinButton: "Quay ngay",
      spinning: "Đang quay...",
      missSlice: "Chúc may mắn lần sau",
      notEnoughPoints: "Bạn chưa đủ điểm để quay.",
      loadError: "Không tải được thông tin vòng quay, vui lòng thử lại.",
      spinError: "Không thể quay lúc này, vui lòng thử lại.",
      winTitle: "Chúc mừng bạn đã trúng thưởng!",
      winPercent: "Giảm {value}% cho đơn hàng tiếp theo",
      winFixed: "Giảm {value} cho đơn hàng tiếp theo",
      winCode: "Mã voucher: {code}",
      missTitle: "Chúc may mắn lần sau!",
      missDesc: "Bạn chưa trúng khuyến mãi nào lần này, hãy thử lại ở lượt sau nhé.",
    },
  },
```

- [ ] **Step 3: `vi.js` — thêm khối `admin.wheelConfig` (cạnh khối `admin.promotions`, chèn NGAY TRƯỚC nó)**

Tìm:
```js
    rewards: {
      countSuffix: "phần thưởng",
```
(khối `admin.rewards`, dòng 1222) — chèn khối `wheelConfig` mới ngay TRƯỚC dòng này (cùng cấp với `rewards`, trong object `admin`):
```js
    wheelConfig: {
      title: "Cấu hình vòng quay may mắn",
      pointsPerSpin: "Điểm/lượt quay",
      missRate: "% Trượt",
      save: "Lưu",
      loadError: "Không tải được cấu hình vòng quay",
      saveError: "Lưu cấu hình thất bại, vui lòng thử lại",
    },
    rewards: {
      countSuffix: "phần thưởng",
```

- [ ] **Step 4: Lặp lại Step 1-3 cho `en.js`, `ja.js`, `ko.js`, `zh.js`**

Cùng vị trí chèn (tìm đúng `tabCancelled`/`tabSettings`, `redeemError` cuối khối `rewards`, và điểm ngay trước khối `rewards` trong `admin`), nội dung dịch:

**`en.js`:**
```js
    tabWheel: "Lucky Wheel",
```
```js
    wheel: {
      costLabel: "Each spin costs {points} points",
      spinButton: "Spin now",
      spinning: "Spinning...",
      missSlice: "Better luck next time",
      notEnoughPoints: "You don't have enough points to spin.",
      loadError: "Couldn't load the wheel, please try again.",
      spinError: "Couldn't spin right now, please try again.",
      winTitle: "Congratulations, you won!",
      winPercent: "{value}% off your next order",
      winFixed: "{value} off your next order",
      winCode: "Voucher code: {code}",
      missTitle: "Better luck next time!",
      missDesc: "No prize this time — try again on your next spin.",
    },
```
```js
    wheelConfig: {
      title: "Lucky wheel settings",
      pointsPerSpin: "Points per spin",
      missRate: "Miss rate %",
      save: "Save",
      loadError: "Couldn't load wheel settings",
      saveError: "Couldn't save settings, please try again",
    },
```

**`ja.js`:**
```js
    tabWheel: "ラッキーホイール",
```
```js
    wheel: {
      costLabel: "1回のスピンに{points}ポイント必要です",
      spinButton: "今すぐスピン",
      spinning: "スピン中...",
      missSlice: "残念、また次回",
      notEnoughPoints: "スピンに必要なポイントが不足しています。",
      loadError: "ホイール情報の読み込みに失敗しました。",
      spinError: "現在スピンできません。もう一度お試しください。",
      winTitle: "おめでとうございます！当選しました",
      winPercent: "次回のご注文が{value}%オフ",
      winFixed: "次回のご注文が{value}オフ",
      winCode: "クーポンコード：{code}",
      missTitle: "残念、また次回！",
      missDesc: "今回は当選しませんでした。次のスピンをお試しください。",
    },
```
```js
    wheelConfig: {
      title: "ラッキーホイール設定",
      pointsPerSpin: "1回あたりのポイント",
      missRate: "はずれ率 %",
      save: "保存",
      loadError: "設定の読み込みに失敗しました",
      saveError: "保存に失敗しました。もう一度お試しください",
    },
```

**`ko.js`:**
```js
    tabWheel: "행운의 룰렛",
```
```js
    wheel: {
      costLabel: "1회 스핀에 {points} 포인트가 필요합니다",
      spinButton: "지금 돌리기",
      spinning: "돌리는 중...",
      missSlice: "다음 기회에",
      notEnoughPoints: "스핀에 필요한 포인트가 부족합니다.",
      loadError: "룰렛 정보를 불러오지 못했습니다.",
      spinError: "지금은 돌릴 수 없습니다. 다시 시도해 주세요.",
      winTitle: "축하합니다, 당첨되었습니다!",
      winPercent: "다음 주문 시 {value}% 할인",
      winFixed: "다음 주문 시 {value} 할인",
      winCode: "쿠폰 코드: {code}",
      missTitle: "다음 기회에!",
      missDesc: "이번엔 당첨되지 않았습니다. 다음 스핀에 다시 도전해 보세요.",
    },
```
```js
    wheelConfig: {
      title: "행운의 룰렛 설정",
      pointsPerSpin: "스핀당 포인트",
      missRate: "꽝 확률 %",
      save: "저장",
      loadError: "설정을 불러오지 못했습니다",
      saveError: "저장에 실패했습니다. 다시 시도해 주세요",
    },
```

**`zh.js`:**
```js
    tabWheel: "幸运转盘",
```
```js
    wheel: {
      costLabel: "每次抽奖需消耗{points}积分",
      spinButton: "立即抽奖",
      spinning: "抽奖中...",
      missSlice: "祝下次好运",
      notEnoughPoints: "积分不足，无法抽奖。",
      loadError: "转盘信息加载失败，请重试。",
      spinError: "暂时无法抽奖，请重试。",
      winTitle: "恭喜您中奖了！",
      winPercent: "下次订单立减{value}%",
      winFixed: "下次订单立减{value}",
      winCode: "优惠券代码：{code}",
      missTitle: "祝下次好运！",
      missDesc: "这次没有中奖，欢迎下次再来试试。",
    },
```
```js
    wheelConfig: {
      title: "幸运转盘设置",
      pointsPerSpin: "每次抽奖所需积分",
      missRate: "未中奖概率 %",
      save: "保存",
      loadError: "设置加载失败",
      saveError: "保存失败，请重试",
    },
```

- [ ] **Step 5: Verify sống — đổi ngôn ngữ ở NavBar (combobox 🇻🇳/🇬🇧/...) trên tab khách hàng, xác nhận tab "Vòng quay may mắn" và toàn bộ text trong `LuckyWheelPanel` đổi ngôn ngữ đúng theo từng lựa chọn; tương tự đổi ngôn ngữ ở tab admin, xác nhận khối cấu hình đổi đúng.**

- [ ] **Step 6: Commit**

```bash
git add "FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/en.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js" \
        "FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js"
git commit -m "feat(i18n): text vòng quay may mắn cho 5 ngôn ngữ"
```

---

## Task 9: Verify sống toàn bộ luồng (đúng "Kiểm thử" trong spec)

**Files:** không tạo/sửa file — chỉ vận hành thật qua trình duyệt (Playwright hoặc thủ công), đúng phương pháp QA đã dùng suốt dự án này (xem `docs/qa-notes-2026-07-26.md`).

- [ ] **Step 1: Đăng nhập `khachhang`/`123456`, vào tab "🎡 Vòng quay may mắn"**

Xác nhận số ô hiển thị = số khuyến mãi `active` hiện có (kiểm tra chéo qua `GET /api/khuyen-mai` hoặc trang admin) + đúng 1 ô "Chúc may mắn lần sau".

- [ ] **Step 2: Quay nhiều lần liên tiếp khi đủ điểm**

Mỗi lần quay: xác nhận điểm hiển thị (badge header) giảm đúng bằng `diemMoiLuot`; nếu trúng, mở tab "⚙️ Cài đặt" xác nhận voucher mới xuất hiện ngay trong "Voucher đã đổi" với mã/loại/giá trị khớp đúng khuyến mãi vừa trúng.

- [ ] **Step 3: Dùng thử 1 voucher vừa trúng ở checkout**

Thêm sản phẩm vào giỏ, vào `CheckoutModal.vue`, áp voucher vừa trúng — xác nhận giảm giá tính đúng, đặt hàng thành công, không cần sửa gì thêm ở `CheckoutModal.vue`/`DonHangService` (đúng dự đoán trong spec vì `doiThuong=null` không ảnh hưởng luồng validate).

- [ ] **Step 4: Admin đổi cấu hình, xác nhận áp dụng ngay không cần đăng xuất/vào lại**

Đăng nhập `admin`/`123456`, vào trang Khuyến mãi, đổi `diemMoiLuot` (vd 100 → 50), bấm Lưu. Quay lại tab khách hàng (không tải lại trang), vào lại tab vòng quay (component gọi lại `getCauHinh()` mỗi lần `onMounted` — chuyển tab đi rồi quay lại sẽ tự fetch mới) — xác nhận nhãn "Mỗi lượt quay tốn 50 điểm" cập nhật đúng.

- [ ] **Step 5: Hạ điểm khách hàng xuống dưới mức tối thiểu**

Ở admin, sửa tay điểm tích lũy của khách hàng test xuống dưới `diemMoiLuot` hiện tại. Tải lại tab vòng quay bên khách hàng — xác nhận nút "Quay ngay" tự động bị disable + hiện đúng thông báo "Bạn chưa đủ điểm để quay."

- [ ] **Step 6: Test race-condition thật (tuỳ chọn, không bắt buộc)**

Nếu có thể, mở 2 tab cùng đăng nhập 1 tài khoản khách hàng chỉ đủ điểm cho ĐÚNG 1 lượt, bấm quay gần như đồng thời ở cả 2 tab — xác nhận chỉ 1 lượt thành công, lượt kia nhận lỗi "Không đủ điểm" (không có trường hợp cả 2 đều thành công / điểm bị âm).

- [ ] **Step 7: Cập nhật `docs/qa-notes-2026-07-26.md` (hoặc tạo note mới nếu đã sang ngày khác) ghi lại kết quả verify**

Theo đúng format đã dùng trong file đó — mục "Bug đã tìm thấy và ĐÃ SỬA" hoặc mục riêng "Tính năng mới đã triển khai", tuỳ tình huống thực tế lúc verify (nếu phát hiện bug trong lúc verify, note rõ đã sửa hay còn tồn đọng).
