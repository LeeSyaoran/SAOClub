# Chọn serial trước khi đóng gói — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Đơn hàng online không còn tự động gán + chốt bán serial ngay lúc đặt hàng — thay vào đó chỉ giữ chỗ (`giu_hang`); admin phải chọn/xác nhận serial cụ thể cho từng dòng sản phẩm trước khi đóng gói (`confirmed` → `processing`). Đơn tại quầy (`in_store`) giữ nguyên hành vi cũ 100%.

**Architecture:** Tận dụng trạng thái `giu_hang` đã có sẵn trong schema nhưng chưa được set ở đâu. Thêm bảng join `chi_tiet_don_hang_serial` để 1 dòng đơn hàng có thể gắn nhiều serial (hỗ trợ `so_luong > 1`), dùng cho cả 2 kênh bán (để làm nguồn dữ liệu đầy đủ, dù chỉ đơn online thực sự cần luồng giữ chỗ → chọn lại). 1 endpoint `PATCH /api/don-hang/{id}/dong-goi` gộp "chọn serial cho từng dòng + chốt da_ban + chuyển trạng thái processing" thành 1 transaction.

**Tech Stack:** Spring Boot 4.0.6 (Java 17, JPA/Hibernate, SQL Server), Vue 3 `<script setup>` (Vite), JUnit 5 + Mockito (đã có sẵn qua `spring-boot-starter-webmvc-test`).

## Global Constraints

- Chỉ đơn `kenhBan == "online"` đi qua luồng giữ chỗ → chọn serial → đóng gói. Đơn `kenhBan == "in_store"` giữ nguyên hành vi cũ (gán `da_ban` ngay lúc tạo dòng đơn).
- `spring.jpa.hibernate.ddl-auto=none` — bảng mới phải tạo bằng tay qua SQL, JPA sẽ không tự tạo.
- Lỗi nghiệp vụ ném `IllegalArgumentException` với message tiếng Việt rõ ràng — `GlobalExceptionHandler` tự chuyển thành HTTP 400 kèm message làm body (xem `BackEnd/src/main/java/com/example/backend/exception/GlobalExceptionHandler.java:34-37`).
- Máy dev cục bộ có `JAVA_HOME` hệ thống bị set sai (kèm dấu ngoặc kép trong giá trị, làm `mvnw.cmd` lỗi). Mọi lệnh Maven trong plan này đặt lại biến cho đúng trong phiên PowerShell trước khi gọi: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'`.
- Toàn bộ code mới viết theo đúng style hiện có trong file cùng loại: Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Getter @Setter`, comment tiếng Việt giải thích "vì sao" (không giải thích "cái gì"), field injection `@Autowired` (không dùng constructor injection dù đó là best practice hiện đại — để nhất quán với toàn bộ codebase).

---

### Task 1: Bảng `chi_tiet_don_hang_serial` + entity + repository + response DTO

**Files:**
- Modify: `Database/QLBanMayTinh.sql` (chèn ngay sau khối `CREATE TABLE chi_tiet_don_hang` kết thúc ở dòng 354, trước `CREATE TABLE lich_su_ton_kho` ở dòng 356)
- Create: `BackEnd/src/main/java/com/example/backend/entity/ChiTietDonHangSerial.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/ChiTietDonHangSerialResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/ChiTietDonHangSerialRepository.java`

**Interfaces:**
- Produces: entity `ChiTietDonHangSerial` (getters/setters: `getId()`, `getChiTietDonHang()`/`setChiTietDonHang(ChiTietDonHang)`, `getChiTietSanPham()`/`setChiTietSanPham(ChiTietSanPham)`); `ChiTietDonHangSerialRepository` với `findByChiTietDonHang_Id(Integer)`, `deleteByChiTietDonHang_Id(Integer)`, `findByDonHangId(Integer)` trả `List<ChiTietDonHangSerialResponse>`; DTO `ChiTietDonHangSerialResponse(Integer chiTietDonHangId, Integer chiTietId, String soSerial)`. Task 2, 3, 4 dùng trực tiếp các tên này.

- [ ] **Step 1: Thêm bảng vào schema SQL**

Mở `Database/QLBanMayTinh.sql`, chèn đoạn sau ngay sau dòng 354 (`);` đóng `chi_tiet_don_hang`, trước dòng 356 `CREATE TABLE lich_su_ton_kho`):

```sql
-- Gắn nhiều serial cho 1 dòng đơn hàng — chi_tiet_don_hang.chi_tiet_id (FK đơn) chỉ giữ
-- được 1 serial đại diện, bảng này là nguồn đầy đủ khi so_luong > 1. Dùng cho cả 2 kênh
-- bán, nhưng chỉ đơn online thực sự cần luồng giữ chỗ ("giu_hang") -> chọn lại -> đóng gói.
CREATE TABLE chi_tiet_don_hang_serial (
    chi_tiet_don_hang_serial_id INT IDENTITY(1,1) PRIMARY KEY,
    chi_tiet_don_hang_id        INT NOT NULL,
    chi_tiet_id                 INT NOT NULL,
    CONSTRAINT FK_ctdhs_ctdh FOREIGN KEY (chi_tiet_don_hang_id) REFERENCES chi_tiet_don_hang(chi_tiet_don_hang_id) ON DELETE CASCADE,
    CONSTRAINT FK_ctdhs_ctsp FOREIGN KEY (chi_tiet_id)          REFERENCES chi_tiet_san_pham(chi_tiet_id),
    CONSTRAINT UX_ctdhs_pair UNIQUE (chi_tiet_don_hang_id, chi_tiet_id)
);
GO
```

- [ ] **Step 2: Chạy script này vào database `QLBanMayTinh`**

Đây là thao tác thủ công (cần thông tin đăng nhập DB không có sẵn trong plan) — dùng SSMS, Azure Data Studio, hoặc `sqlcmd` để chạy đúng đoạn `CREATE TABLE chi_tiet_don_hang_serial ...` ở trên vào database `QLBanMayTinh` tại `localhost:1433` (xem `BackEnd/src/main/resources/application.properties:3`). Bỏ qua bước này thì mọi thao tác JPA ở các task sau sẽ lỗi "Invalid object name".

- [ ] **Step 3: Tạo entity**

```java
package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "chi_tiet_don_hang_serial")
public class ChiTietDonHangSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_don_hang_serial_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chi_tiet_don_hang_id")
    private ChiTietDonHang chiTietDonHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chi_tiet_id")
    private ChiTietSanPham chiTietSanPham;
}
```

- [ ] **Step 4: Tạo response DTO**

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
public class ChiTietDonHangSerialResponse {
    private Integer chiTietDonHangId;
    private Integer chiTietId;
    private String soSerial;
}
```

- [ ] **Step 5: Tạo repository**

```java
package com.example.backend.repository;

import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.response.ChiTietDonHangSerialResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDonHangSerialRepository extends JpaRepository<ChiTietDonHangSerial, Integer> {

    // Toàn bộ serial đang gắn với 1 dòng đơn hàng — dùng để hiện lại lựa chọn đã giữ chỗ
    // khi mở modal chọn serial, và để giải phóng khi admin đổi lựa chọn hoặc hủy đơn.
    List<ChiTietDonHangSerial> findByChiTietDonHang_Id(Integer chiTietDonHangId);

    void deleteByChiTietDonHang_Id(Integer chiTietDonHangId);

    // Toàn bộ serial đã gắn cho mọi dòng của 1 đơn hàng, gộp theo chiTietDonHangId ở phía
    // gọi (FE) — dùng để load lại modal "Chọn serial trước khi đóng gói".
    @Query("SELECT new com.example.backend.response.ChiTietDonHangSerialResponse(s.chiTietDonHang.id, s.chiTietSanPham.chiTietId, s.chiTietSanPham.soSerial) " +
           "FROM ChiTietDonHangSerial s WHERE s.chiTietDonHang.donHang.id = :donHangId")
    List<ChiTietDonHangSerialResponse> findByDonHangId(@Param("donHangId") Integer donHangId);
}
```

- [ ] **Step 6: Biên dịch để chắc chắn không lỗi cú pháp/import**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -q -o compile
```
Expected: không có output (build thành công), exit code 0.

- [ ] **Step 7: Commit**

```bash
git add "Database/QLBanMayTinh.sql" \
  BackEnd/src/main/java/com/example/backend/entity/ChiTietDonHangSerial.java \
  BackEnd/src/main/java/com/example/backend/response/ChiTietDonHangSerialResponse.java \
  BackEnd/src/main/java/com/example/backend/repository/ChiTietDonHangSerialRepository.java
git commit -m "feat: add chi_tiet_don_hang_serial table for multi-serial order lines"
```

---

### Task 2: Giữ chỗ serial theo kênh bán + endpoint đọc serial theo đơn

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/service/ChiTietDonHangService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ChiTietDonHangController.java`
- Create: `BackEnd/src/test/java/com/example/backend/service/ChiTietDonHangServiceTest.java`

**Interfaces:**
- Consumes: `ChiTietDonHangSerialRepository` (Task 1) — `save()`, `findByDonHangId(Integer)`.
- Produces: `ChiTietDonHangService.getSerialsByDonHangId(Integer)` → `List<ChiTietDonHangSerialResponse>`, dùng trực tiếp trong controller. `GET /api/chi-tiet-don-hang/don-hang/{donHangId}/serials` — dùng ở Task 7 (frontend modal).

- [ ] **Step 1: Đọc lại code hiện tại của `create()`**

Xem `BackEnd/src/main/java/com/example/backend/service/ChiTietDonHangService.java:48-100` — logic hiện tại: nếu `request.chiTietId` có giá trị thì dùng đúng serial đó; nếu không, FIFO chọn đủ `soLuong` serial `trong_kho`. Cả 2 nhánh đều set `trangThai = "da_ban"` ngay và chỉ gắn serial đầu tiên vào FK đơn `entity.chiTietSanPham` (vì FK này là 1-1).

- [ ] **Step 2: Sửa `create()` — rẽ nhánh theo kênh bán + ghi bảng join cho mọi serial**

Thay toàn bộ method `create()` (dòng 48-100) bằng:

```java
    @Transactional
    public ChiTietDonHang create(ChiTietDonHangRequest request) {
        ChiTietDonHang entity = new ChiTietDonHang();
        // BeanUtils copies: soLuong, donGia, giamGiaDong, ghiChu
        // Bỏ qua: donHangId, bienTheId, chiTietId (khác tên với entity)
        BeanUtils.copyProperties(request, entity, "donHangId", "bienTheId", "chiTietId");

        DonHang donHang = donHangRepository.getReferenceById(request.getDonHangId());
        entity.setDonHang(donHang);
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));

        // Gán serial cụ thể cho dòng đơn hàng này + trừ tồn kho.
        // ChiTietSanPham.trangThai chuyển khỏi "trong_kho" sẽ tự kích hoạt trigger
        // trg_CapNhatTonKhoThucTe trừ ton_kho.so_luong_ton_thuc_te tương ứng.
        List<ChiTietSanPham> assignedSerials;
        if (request.getChiTietId() != null) {
            // Đã chỉ định seri cụ thể (vd: nhân viên chọn tay tại quầy)
            ChiTietSanPham chosen = chiTietSanPhamRepository.findById(request.getChiTietId())
                    .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + request.getChiTietId()));
            entity.setChiTietSanPham(chosen);
            assignedSerials = List.of(chosen);
        } else {
            // Tự động gán seri còn trong kho theo thứ tự nhập trước (FIFO)
            int soLuong = request.getSoLuong() != null ? request.getSoLuong() : 1;
            List<ChiTietSanPham> available = chiTietSanPhamRepository
                    .findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(request.getBienTheId(), "trong_kho");
            if (available.size() < soLuong)
                throw new IllegalArgumentException(
                        "Không đủ hàng trong kho: cần " + soLuong + ", còn " + available.size());
            assignedSerials = available.subList(0, soLuong);
            // Chỉ gắn 1 seri đại diện lên dòng đơn hàng (FK chi_tiet_id là 1-1) — bảng join
            // chi_tiet_don_hang_serial bên dưới mới là nguồn đầy đủ khi so_luong > 1.
            entity.setChiTietSanPham(assignedSerials.get(0));
        }

        ChiTietDonHang saved = chiTietDonHangRepository.save(entity);

        // Đơn online: chỉ giữ chỗ ("giu_hang") — admin xác nhận/đổi serial ở bước đóng gói
        // (xem DonHangService.dongGoi) mới chốt "da_ban". Đơn tại quầy (in_store): chốt bán
        // ngay như trước, không qua bước xác nhận/đóng gói (nhân viên đã cầm máy trên tay).
        boolean online = "online".equals(donHang.getKenhBan());
        String trangThaiMoi = online ? "giu_hang" : "da_ban";

        for (ChiTietSanPham serial : assignedSerials) {
            serial.setTrangThai(trangThaiMoi);
            chiTietSanPhamRepository.save(serial);
            // Ghi vào bảng join cho MỌI serial (kể cả đơn tại quầy) — đây là nguồn duy nhất
            // biết đủ mọi serial của 1 dòng khi so_luong > 1, FK đơn chỉ giữ 1 đại diện.
            ChiTietDonHangSerial link = new ChiTietDonHangSerial();
            link.setChiTietDonHang(saved);
            link.setChiTietSanPham(serial);
            chiTietDonHangSerialRepository.save(link);
        }

        LichSuTonKho lichSu = new LichSuTonKho();
        lichSu.setBienThe(entity.getBienThe());
        lichSu.setChiTietSanPham(assignedSerials.isEmpty() ? null : assignedSerials.get(0));
        lichSu.setLoaiBienDong(online ? "giu_hang" : "xuat_ban");
        lichSu.setSoLuongThayDoi(-assignedSerials.size());
        lichSu.setDonHang(entity.getDonHang());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSu.setGhiChu(online
                ? "Giữ chỗ — đơn #" + request.getDonHangId()
                : "Bán hàng — đơn #" + request.getDonHangId());
        lichSuTonKhoRepository.save(lichSu);

        return saved;
    }
```

Thêm field + import mới ở đầu file (cạnh các `@Autowired` field khác, dòng 24-33):

```java
    @Autowired
    private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;
```

```java
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.DonHang;
import com.example.backend.repository.ChiTietDonHangSerialRepository;
import com.example.backend.response.ChiTietDonHangSerialResponse;
```

- [ ] **Step 3: Thêm method đọc serial theo đơn hàng (cho modal đóng gói ở Task 7)**

Thêm vào cuối class `ChiTietDonHangService` (trước dấu `}` đóng class):

```java
    // Toàn bộ serial đang giữ chỗ/đã gán cho từng dòng của 1 đơn — dùng cho modal "Chọn
    // serial" trước khi đóng gói (đơn online có thể có nhiều serial/dòng nên không đủ nếu
    // chỉ lấy serial đại diện từ ChiTietDonHangResponse).
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHangId(Integer donHangId) {
        return chiTietDonHangSerialRepository.findByDonHangId(donHangId);
    }
```

- [ ] **Step 4: Thêm endpoint GET trong controller**

Trong `BackEnd/src/main/java/com/example/backend/controller/ChiTietDonHangController.java`, thêm ngay sau method `getByDonHang` (sau dòng 35):

```java

    @GetMapping("/don-hang/{donHangId}/serials")
    public List<ChiTietDonHangSerialResponse> getSerialsByDonHang(@PathVariable Integer donHangId) {
        return chiTietDonHangService.getSerialsByDonHangId(donHangId);
    }
```

Thêm import: `import com.example.backend.response.ChiTietDonHangSerialResponse;`

- [ ] **Step 5: Viết test**

Tạo `BackEnd/src/test/java/com/example/backend/service/ChiTietDonHangServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietDonHangSerial;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.repository.*;
import com.example.backend.request.ChiTietDonHangRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChiTietDonHangServiceTest {

    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private BienTheSanPhamRepository bienTheSanPhamRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;

    @InjectMocks
    private ChiTietDonHangService service;

    private ChiTietSanPham serialTrongKho(Integer id, BienTheSanPham bienThe) {
        ChiTietSanPham s = new ChiTietSanPham();
        s.setChiTietId(id);
        s.setBienThe(bienThe);
        s.setSoSerial("SN-" + id);
        s.setTrangThai("trong_kho");
        return s;
    }

    @Test
    void create_donOnline_giuChoKhongDanhDauDaBan() {
        DonHang donHang = new DonHang();
        donHang.setId(1);
        donHang.setKenhBan("online");
        when(donHangRepository.getReferenceById(1)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        when(bienTheSanPhamRepository.getReferenceById(10)).thenReturn(bienThe);

        ChiTietSanPham s1 = serialTrongKho(100, bienThe);
        ChiTietSanPham s2 = serialTrongKho(101, bienThe);
        when(chiTietSanPhamRepository.findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(10, "trong_kho"))
                .thenReturn(List.of(s1, s2));
        when(chiTietDonHangRepository.save(any(ChiTietDonHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(1, 10, null, 2, BigDecimal.TEN, BigDecimal.ZERO, null);

        service.create(request);

        assertThat(s1.getTrangThai()).isEqualTo("giu_hang");
        assertThat(s2.getTrangThai()).isEqualTo("giu_hang");
        verify(chiTietDonHangSerialRepository, times(2)).save(any(ChiTietDonHangSerial.class));
    }

    @Test
    void create_donTaiQuay_danhDauDaBanNgay() {
        DonHang donHang = new DonHang();
        donHang.setId(2);
        donHang.setKenhBan("in_store");
        when(donHangRepository.getReferenceById(2)).thenReturn(donHang);

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(20);
        when(bienTheSanPhamRepository.getReferenceById(20)).thenReturn(bienThe);

        ChiTietSanPham s1 = serialTrongKho(200, bienThe);
        when(chiTietSanPhamRepository.findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(20, "trong_kho"))
                .thenReturn(List.of(s1));
        when(chiTietDonHangRepository.save(any(ChiTietDonHang.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChiTietDonHangRequest request = new ChiTietDonHangRequest(2, 20, null, 1, BigDecimal.TEN, BigDecimal.ZERO, null);

        service.create(request);

        assertThat(s1.getTrangThai()).isEqualTo("da_ban");
        verify(chiTietDonHangSerialRepository, times(1)).save(any(ChiTietDonHangSerial.class));
    }
}
```

- [ ] **Step 6: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -q -o test "-Dtest=ChiTietDonHangServiceTest"
```
Expected: `BUILD SUCCESS`, 2/2 test passed.

- [ ] **Step 7: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/ChiTietDonHangService.java \
  BackEnd/src/main/java/com/example/backend/controller/ChiTietDonHangController.java \
  BackEnd/src/test/java/com/example/backend/service/ChiTietDonHangServiceTest.java
git commit -m "feat: reserve (giu_hang) instead of sell serials for online order lines"
```

---

### Task 3: Endpoint `PATCH /api/don-hang/{id}/dong-goi`

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/DongGoiRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/DongGoiLineRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/DonHangService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DonHangController.java`
- Create: `BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java`

**Interfaces:**
- Consumes: `ChiTietDonHangSerialRepository` (Task 1), `ChiTietSanPhamRepository.findById` (đã có).
- Produces: `DonHangService.dongGoi(Integer donHangId, DongGoiRequest request)` — dùng ở controller. Request JSON dùng ở Task 5/7 (frontend): `{ "lines": [{ "chiTietDonHangId": 123, "serialIds": [45, 46] }] }`.

- [ ] **Step 1: Tạo request DTO**

`BackEnd/src/main/java/com/example/backend/request/DongGoiLineRequest.java`:

```java
package com.example.backend.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DongGoiLineRequest {
    @NotNull(message = "Dòng đơn hàng không được để trống")
    private Integer chiTietDonHangId;

    @NotEmpty(message = "Phải chọn ít nhất 1 serial")
    private List<Integer> serialIds;
}
```

`BackEnd/src/main/java/com/example/backend/request/DongGoiRequest.java`:

```java
package com.example.backend.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DongGoiRequest {
    @NotEmpty(message = "Đơn hàng chưa có dòng sản phẩm nào")
    @Valid
    private List<DongGoiLineRequest> lines;
}
```

- [ ] **Step 2: Thêm method `dongGoi()` vào `DonHangService`**

Thêm field mới cạnh các field `@Autowired` khác (sau dòng 48):

```java
    @Autowired
    private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;
```

Thêm import: `import com.example.backend.entity.ChiTietDonHangSerial;`, `import com.example.backend.entity.ChiTietSanPham;`, `import com.example.backend.repository.ChiTietDonHangSerialRepository;`, `import com.example.backend.request.DongGoiLineRequest;`, `import com.example.backend.request.DongGoiRequest;`, `import java.util.ArrayList;`, `import java.util.HashSet;`, `import java.util.Set;`, `import java.util.stream.Collectors;`.

Thêm method mới (trước method `recalculateTongTien`, sau `delete()`):

```java
    // Chọn serial cho từng dòng + chốt "da_ban" + chuyển trạng thái "processing" trong 1
    // transaction — chỉ áp dụng đơn online (đơn tại quầy đã chốt serial ngay lúc tạo dòng
    // đơn, không qua bước xác nhận/đóng gói nên không cần gọi endpoint này).
    @Transactional
    public void dongGoi(Integer donHangId, DongGoiRequest request) {
        DonHang donHang = getById(donHangId);
        if (!"online".equals(donHang.getKenhBan()))
            throw new IllegalArgumentException("Chỉ đơn hàng online mới cần chọn serial trước khi đóng gói");
        if (!"confirmed".equals(donHang.getTrangThaiDonHang()))
            throw new IllegalArgumentException("Đơn hàng phải ở trạng thái 'Đã xác nhận' mới đóng gói được");

        for (DongGoiLineRequest line : request.getLines()) {
            ChiTietDonHang item = chiTietDonHangRepository.findById(line.getChiTietDonHangId())
                    .orElseThrow(() -> new IllegalArgumentException("Dòng đơn hàng không tồn tại với id: " + line.getChiTietDonHangId()));
            if (!item.getDonHang().getId().equals(donHangId))
                throw new IllegalArgumentException("Dòng #" + item.getId() + " không thuộc đơn hàng này");

            List<Integer> serialIds = line.getSerialIds();
            if (new HashSet<>(serialIds).size() != serialIds.size())
                throw new IllegalArgumentException("Dòng #" + item.getId() + " chọn trùng serial");
            if (serialIds.size() != item.getSoLuong())
                throw new IllegalArgumentException(
                        "Dòng #" + item.getId() + " cần đúng " + item.getSoLuong() + " serial, đã chọn " + serialIds.size());

            List<ChiTietDonHangSerial> existingLinks = chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId());
            Set<Integer> reservedForThisLine = existingLinks.stream()
                    .map(l -> l.getChiTietSanPham().getChiTietId())
                    .collect(Collectors.toSet());

            List<ChiTietSanPham> finalSerials = new ArrayList<>();
            for (Integer serialId : serialIds) {
                ChiTietSanPham serial = chiTietSanPhamRepository.findById(serialId)
                        .orElseThrow(() -> new IllegalArgumentException("Serial không tồn tại với id: " + serialId));
                if (!serial.getBienThe().getBienTheId().equals(item.getBienThe().getBienTheId()))
                    throw new IllegalArgumentException("Serial " + serial.getSoSerial() + " không thuộc đúng sản phẩm của dòng #" + item.getId());
                boolean daGiuChoDongNay = reservedForThisLine.contains(serialId);
                if (!"trong_kho".equals(serial.getTrangThai()) && !daGiuChoDongNay)
                    throw new IllegalArgumentException("Serial " + serial.getSoSerial() + " không còn khả dụng, vui lòng chọn lại");
                finalSerials.add(serial);
            }

            // Trả các serial đã giữ chỗ trước đó nhưng bị bỏ chọn (admin đổi ý) về lại kho
            for (ChiTietDonHangSerial link : existingLinks) {
                if (!serialIds.contains(link.getChiTietSanPham().getChiTietId())) {
                    link.getChiTietSanPham().setTrangThai("trong_kho");
                    chiTietSanPhamRepository.save(link.getChiTietSanPham());
                }
            }
            chiTietDonHangSerialRepository.deleteByChiTietDonHang_Id(item.getId());

            for (ChiTietSanPham serial : finalSerials) {
                serial.setTrangThai("da_ban");
                chiTietSanPhamRepository.save(serial);
                ChiTietDonHangSerial link = new ChiTietDonHangSerial();
                link.setChiTietDonHang(item);
                link.setChiTietSanPham(serial);
                chiTietDonHangSerialRepository.save(link);
            }

            // Đồng bộ serial đại diện trên FK đơn — nếu admin đổi serial khác lúc đóng gói,
            // các nơi hiển thị dựa trên FK này (chi tiết đơn, bảo hành...) vẫn đúng.
            item.setChiTietSanPham(finalSerials.get(0));
            chiTietDonHangRepository.save(item);
        }

        donHang.setTrangThaiDonHang("processing");
        donHangRepository.save(donHang);
        sseService.notifyOrderUpdate(donHangId);
    }
```

- [ ] **Step 3: Thêm endpoint trong controller**

Trong `BackEnd/src/main/java/com/example/backend/controller/DonHangController.java`, thêm ngay sau method `recalculate` (sau dòng 75):

```java

    // Chọn serial cho từng dòng + chốt bán + chuyển sang "processing" (đóng gói) — chỉ
    // đơn online (đơn tại quầy đã chốt serial ngay lúc tạo, không qua bước này).
    @PatchMapping("{id}/dong-goi")
    public ResponseEntity<Void> dongGoi(@PathVariable Integer id, @Valid @RequestBody DongGoiRequest request) {
        donHangService.dongGoi(id, request);
        return ResponseEntity.ok().build();
    }
```

Thêm import: `import com.example.backend.request.DongGoiRequest;`

- [ ] **Step 4: Viết test**

Tạo `BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.entity.DonHang;
import com.example.backend.repository.*;
import com.example.backend.request.DongGoiLineRequest;
import com.example.backend.request.DongGoiRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonHangServiceTest {

    @Mock private DonHangRepository donHangRepository;
    @Mock private SseService sseService;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private NhanVienRepository nhanVienRepository;
    @Mock private KhuyenMaiRepository khuyenMaiRepository;
    @Mock private DiaChiGiaoHangRepository diaChiGiaoHangRepository;
    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private LichSuTonKhoRepository lichSuTonKhoRepository;
    @Mock private PhieuTraHangRepository phieuTraHangRepository;
    @Mock private PhieuBaoHanhRepository phieuBaoHanhRepository;
    @Mock private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock private ChiTietDonHangSerialRepository chiTietDonHangSerialRepository;

    @InjectMocks
    private DonHangService service;

    private DonHang donHangOnlineConfirmed() {
        DonHang d = new DonHang();
        d.setId(1);
        d.setKenhBan("online");
        d.setTrangThaiDonHang("confirmed");
        return d;
    }

    @Test
    void dongGoi_khongPhaiDonOnline_biChan() {
        DonHang d = donHangOnlineConfirmed();
        d.setKenhBan("in_store");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        DongGoiRequest req = new DongGoiRequest(List.of());

        assertThatThrownBy(() -> service.dongGoi(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("online");
    }

    @Test
    void dongGoi_saiSoLuongSerial_biChan() {
        DonHang d = donHangOnlineConfirmed();
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setDonHang(d);
        item.setBienThe(bienThe);
        item.setSoLuong(2);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(item));

        DongGoiRequest req = new DongGoiRequest(List.of(new DongGoiLineRequest(5, List.of(100))));

        assertThatThrownBy(() -> service.dongGoi(1, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 serial");
    }

    @Test
    void dongGoi_hopLe_chotDaBanVaChuyenProcessing() {
        DonHang d = donHangOnlineConfirmed();
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));

        BienTheSanPham bienThe = new BienTheSanPham();
        bienThe.setBienTheId(10);
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setDonHang(d);
        item.setBienThe(bienThe);
        item.setSoLuong(1);
        when(chiTietDonHangRepository.findById(5)).thenReturn(Optional.of(item));
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of());

        ChiTietSanPham serial = new ChiTietSanPham();
        serial.setChiTietId(100);
        serial.setBienThe(bienThe);
        serial.setSoSerial("SN-100");
        serial.setTrangThai("trong_kho");
        when(chiTietSanPhamRepository.findById(100)).thenReturn(Optional.of(serial));

        DongGoiRequest req = new DongGoiRequest(List.of(new DongGoiLineRequest(5, List.of(100))));

        service.dongGoi(1, req);

        assertThat(serial.getTrangThai()).isEqualTo("da_ban");
        assertThat(d.getTrangThaiDonHang()).isEqualTo("processing");
    }
}
```

- [ ] **Step 5: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -q -o test "-Dtest=DonHangServiceTest"
```
Expected: `BUILD SUCCESS`, 3/3 test passed.

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/request/DongGoiRequest.java \
  BackEnd/src/main/java/com/example/backend/request/DongGoiLineRequest.java \
  BackEnd/src/main/java/com/example/backend/service/DonHangService.java \
  BackEnd/src/main/java/com/example/backend/controller/DonHangController.java \
  BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java
git commit -m "feat: add PATCH /don-hang/{id}/dong-goi to finalize serials and pack the order"
```

---

### Task 4: Hủy/xóa đơn giải phóng luôn serial trong bảng join

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/service/DonHangService.java`
- Modify: `BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java`

**Interfaces:**
- Consumes: `ChiTietDonHangSerialRepository.findByChiTietDonHang_Id` (đã thêm ở Task 1, đã autowired ở Task 3).

- [ ] **Step 1: Sửa `releaseSerialsToStock`**

Thay method `releaseSerialsToStock` (dòng 115-123 hiện tại) bằng:

```java
    // Trả toàn bộ serial đã gắn với đơn hàng về lại "trong_kho" — dùng chung khi xóa đơn
    // (delete) hoặc khi đơn chuyển sang trạng thái "cancelled" (update).
    private void releaseSerialsToStock(Integer donHangId) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findEntityByDonHangId(donHangId);
        for (ChiTietDonHang item : items) {
            // Serial đại diện trên FK đơn (chi_tiet_id) — luôn có nếu dòng đã gán serial.
            if (item.getChiTietSanPham() != null) {
                item.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(item.getChiTietSanPham());
            }
            // Đơn có so_luong > 1: các serial còn lại chỉ nằm trong bảng join, không nằm
            // trên FK đại diện — phải trả riêng, nếu không sẽ kẹt vĩnh viễn ở "giu_hang"/
            // "da_ban" dù đơn đã hủy, làm lệch tồn kho thật.
            for (ChiTietDonHangSerial link : chiTietDonHangSerialRepository.findByChiTietDonHang_Id(item.getId())) {
                link.getChiTietSanPham().setTrangThai("trong_kho");
                chiTietSanPhamRepository.save(link.getChiTietSanPham());
            }
        }
    }
```

- [ ] **Step 2: Thêm test**

Thêm vào cuối `DonHangServiceTest.java` (trước dấu `}` đóng class):

```java

    @Test
    void update_chuyenCancelled_giaiPhongCaSerialTrongBangJoin() {
        DonHang d = new DonHang();
        d.setId(1);
        d.setTrangThaiDonHang("processing");
        when(donHangRepository.findById(1)).thenReturn(Optional.of(d));
        when(khachHangRepository.getReferenceById(1)).thenReturn(new com.example.backend.entity.KhachHang());
        when(donHangRepository.save(d)).thenReturn(d);

        ChiTietSanPham repSerial = new ChiTietSanPham();
        repSerial.setChiTietId(100);
        repSerial.setTrangThai("da_ban");
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(5);
        item.setChiTietSanPham(repSerial);
        when(chiTietDonHangRepository.findEntityByDonHangId(1)).thenReturn(List.of(item));

        ChiTietSanPham extraSerial = new ChiTietSanPham();
        extraSerial.setChiTietId(101);
        extraSerial.setTrangThai("da_ban");
        com.example.backend.entity.ChiTietDonHangSerial link = new com.example.backend.entity.ChiTietDonHangSerial();
        link.setChiTietSanPham(extraSerial);
        when(chiTietDonHangSerialRepository.findByChiTietDonHang_Id(5)).thenReturn(List.of(link));

        com.example.backend.request.DonHangRequest request = new com.example.backend.request.DonHangRequest();
        request.setKhachHangId(1);
        request.setTrangThaiDonHang("cancelled");

        service.update(1, request);

        assertThat(repSerial.getTrangThai()).isEqualTo("trong_kho");
        assertThat(extraSerial.getTrangThai()).isEqualTo("trong_kho");
    }
```

- [ ] **Step 3: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -q -o test "-Dtest=DonHangServiceTest"
```
Expected: `BUILD SUCCESS`, 4/4 test passed.

- [ ] **Step 4: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/service/DonHangService.java \
  BackEnd/src/test/java/com/example/backend/service/DonHangServiceTest.java
git commit -m "fix: release join-table serials too when an order is cancelled/deleted"
```

---

### Task 5: Frontend — service functions

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/Service/api.js`
- Create: `FrontEnd/QLBanMayTinh/src/Service/ChiTietDonHangSerialService.js`
- Modify: `FrontEnd/QLBanMayTinh/src/Service/DonHangService.js`

**Interfaces:**
- Produces: `patch(url, body)` trong `api.js`; `ChiTietDonHangSerialService.getByDonHang(donHangId)` → `Promise<ChiTietDonHangSerialResponse[]>`; `DonHangService.dongGoi(donHangId, body)` → `Promise<Response>`. Task 7 (modal) gọi trực tiếp các hàm này.

- [ ] **Step 1: Thêm helper `patch` vào `api.js`**

Thêm vào `FrontEnd/QLBanMayTinh/src/Service/api.js`, ngay sau `export const put` (sau dòng 31):

```js
export const patch = (url, body) =>
  fetch(url, { method: 'PATCH', headers: headers(), body: JSON.stringify(body) });
```

- [ ] **Step 2: Tạo `ChiTietDonHangSerialService.js`**

```js
import { get } from './api.js';

// Toàn bộ serial đang giữ chỗ/đã gán cho từng dòng của 1 đơn — dùng để load lại lựa chọn
// đã có sẵn khi mở modal "Chọn serial trước khi đóng gói".
export const getByDonHang = (donHangId) => get(`/api/chi-tiet-don-hang/don-hang/${donHangId}/serials`);
```

- [ ] **Step 3: Thêm `dongGoi` vào `DonHangService.js`**

Sửa import ở dòng 1 của `FrontEnd/QLBanMayTinh/src/Service/DonHangService.js`:

```js
import { get, post, put, patch, del, authHeaders } from './api.js';
```

Thêm vào cuối file (sau dòng 34, `export const addChiTiet`):

```js

// Chọn serial cho từng dòng + chốt bán + chuyển đơn sang "processing" (đóng gói) — chỉ
// dùng cho đơn online. body: { lines: [{ chiTietDonHangId, serialIds: [...] }] }
export const dongGoi = (donHangId, body) => patch(`/api/don-hang/${donHangId}/dong-goi`, body);
```

- [ ] **Step 4: Kiểm tra cú pháp**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/Service/api.js; node --check src/Service/ChiTietDonHangSerialService.js; node --check src/Service/DonHangService.js
```
Expected: không có output/lỗi (cả 3 lệnh exit code 0).

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/api.js \
  FrontEnd/QLBanMayTinh/src/Service/ChiTietDonHangSerialService.js \
  FrontEnd/QLBanMayTinh/src/Service/DonHangService.js
git commit -m "feat(frontend): add dongGoi + serial-by-order service calls"
```

---

### Task 6: Frontend — i18n keys (5 ngôn ngữ)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: namespace `admin.packModal.*` (`title`, `loading`, `selectedCount`, `noSerialAvailable`, `cancel`, `confirm`) — dùng ở Task 7.

- [ ] **Step 1: Thêm khối `packModal` vào cả 5 file**

Trong mỗi file, tìm dòng chứa `needCustomerFirst: "..."` (dòng 902 trong cả 5 file, ngay trước dòng `    },` đóng khối `pos` ở dòng 903, và trước khối `placeholder: {` ở dòng 905) — chèn khối mới ngay sau dòng 903 (`    },`) và trước dòng 905, giữ dòng trống 904 giữa 2 khối như style hiện có:

`vi.js`:
```js
    packModal: {
      title: "Chọn serial trước khi đóng gói",
      loading: "Đang tải...",
      selectedCount: "{selected}/{count} đã chọn",
      noSerialAvailable: "Không còn serial khả dụng cho sản phẩm này",
      cancel: "Hủy",
      confirm: "Xác nhận đóng gói",
    },
```

`en.js`:
```js
    packModal: {
      title: "Choose serial before packing",
      loading: "Loading...",
      selectedCount: "{selected}/{count} selected",
      noSerialAvailable: "No serial available for this product",
      cancel: "Cancel",
      confirm: "Confirm packing",
    },
```

`zh.js`:
```js
    packModal: {
      title: "打包前选择序列号",
      loading: "加载中...",
      selectedCount: "已选 {selected}/{count}",
      noSerialAvailable: "该商品没有可用的序列号",
      cancel: "取消",
      confirm: "确认打包",
    },
```

`ko.js`:
```js
    packModal: {
      title: "포장 전 시리얼 선택",
      loading: "불러오는 중...",
      selectedCount: "{selected}/{count} 선택됨",
      noSerialAvailable: "이 제품에 사용 가능한 시리얼이 없습니다",
      cancel: "취소",
      confirm: "포장 확인",
    },
```

`ja.js`:
```js
    packModal: {
      title: "梱包前にシリアルを選択",
      loading: "読み込み中...",
      selectedCount: "{selected}/{count} 選択済み",
      noSerialAvailable: "この製品には利用可能なシリアルがありません",
      cancel: "キャンセル",
      confirm: "梱包を確定",
    },
```

- [ ] **Step 2: Kiểm tra cú pháp cả 5 file**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/i18n/locales/vi.js; node --check src/i18n/locales/en.js; node --check src/i18n/locales/zh.js; node --check src/i18n/locales/ko.js; node --check src/i18n/locales/ja.js
```
Expected: không có output/lỗi (5 lệnh đều exit code 0).

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
  FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "i18n: add packModal translation keys (vi, en, zh, ko, ja)"
```

---

### Task 7: Frontend — modal "Chọn serial trước khi đóng gói" + gate nút đóng gói

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `ChiTietDonHangSerialService.getByDonHang` (Task 5), `DonHangService.dongGoi` (Task 5), `ChiTietDonHangService.getByDonHang` (đã có), `fetchSerialMap` (đã có, dòng 968), `productByBienThe` (đã có, dòng 1587), `t('admin.packModal.*')` (Task 6).

- [ ] **Step 1: Import service mới**

Thêm sau dòng 17 (`import * as ChiTietDonHangService  from "../Service/ChiTietDonHangService.js";`):

```js
import * as ChiTietDonHangSerialService from "../Service/ChiTietDonHangSerialService.js";
```

- [ ] **Step 2: Thêm state + logic modal**

Thêm ngay sau method `advanceOrderStatus` (sau dòng 1874, trước comment `// ── Inventory stock edit`):

```js

// ── Modal "Chọn serial trước khi đóng gói" (chỉ đơn online) ──────────────────────
// Đơn online chỉ giữ chỗ serial ("giu_hang") lúc đặt hàng — admin phải xem lại/đổi rồi
// xác nhận ở đây trước khi đơn được đóng gói (chuyển "processing"). Serial đã giữ chỗ sẵn
// từ lúc đặt hàng được tick trước, admin chỉ cần xác nhận hoặc đổi sang serial khác.
const showPackModal = ref(false);
const packOrder     = ref(null);
const packLines     = ref([]);   // [{ ...ChiTietDonHangResponse, chosenSerialIds: Set<number> }]
const packSerialMap = ref({});   // bienTheId -> ChiTietSanPhamResponse[]
const packLoading   = ref(false);
const packError     = ref('');

const openPackModal = async (o) => {
  packOrder.value = o;
  packLines.value = [];
  packError.value = '';
  showPackModal.value = true;
  packLoading.value = true;
  try {
    const [items, reserved] = await Promise.all([
      ChiTietDonHangService.getByDonHang(o.donHangId),
      ChiTietDonHangSerialService.getByDonHang(o.donHangId),
    ]);
    const reservedByLine = {};
    reserved.forEach((r) => {
      (reservedByLine[r.chiTietDonHangId] ??= []).push(r.chiTietId);
    });
    packSerialMap.value = await fetchSerialMap(items.map((i) => i.bienTheId));
    packLines.value = items.map((item) => ({
      ...item,
      chosenSerialIds: new Set(reservedByLine[item.id] ?? []),
    }));
  } catch (e) {
    packError.value = e.message;
  } finally {
    packLoading.value = false;
  }
};

// Serial khả dụng để chọn cho 1 dòng: đang "trong_kho", hoặc đang "giu_hang" nhưng đã
// giữ sẵn cho chính dòng này (FIFO lúc đặt hàng) — không hiện serial đang giữ cho đơn khác.
const packAvailableSerials = (line) => {
  const all = packSerialMap.value[line.bienTheId] ?? [];
  return all.filter((s) => s.trangThai === 'trong_kho' || line.chosenSerialIds.has(s.chiTietId));
};

const packToggleSerial = (line, serialId) => {
  if (line.chosenSerialIds.has(serialId)) line.chosenSerialIds.delete(serialId);
  else if (line.chosenSerialIds.size < line.soLuong) line.chosenSerialIds.add(serialId);
};

const packAllLinesComplete = computed(() =>
  packLines.value.length > 0 && packLines.value.every((l) => l.chosenSerialIds.size === l.soLuong)
);

const confirmPack = async () => {
  if (!packAllLinesComplete.value) return;
  packError.value = '';
  packLoading.value = true;
  try {
    const res = await DonHangService.dongGoi(packOrder.value.donHangId, {
      lines: packLines.value.map((l) => ({
        chiTietDonHangId: l.id,
        serialIds: [...l.chosenSerialIds],
      })),
    });
    if (!res.ok) {
      packError.value = await res.text().catch(() => t('admin.errors.updateFailed', { status: res.status }));
      return;
    }
    showPackModal.value = false;
    orders.value = await DonHangService.getAll().catch(() => orders.value);
  } catch (e) {
    packError.value = e.message;
  } finally {
    packLoading.value = false;
  }
};
```

- [ ] **Step 3: Chặn nút "Đóng gói nhanh" (`advanceOrderStatus`) cho đơn online**

Sửa `advanceOrderStatus` (dòng 1857-1874) — thêm đoạn chặn ngay đầu, sau dòng `if (!next) return;`:

```js
const advanceOrderStatus = async (o) => {
  const next = NEXT_ORDER_STATUS[o.trangThaiDonHang];
  if (!next) return;
  // Đơn online chuyển sang "processing" (đóng gói) phải chọn serial trước — mở modal thay
  // vì đổi trạng thái thẳng. Đơn tại quầy đã chốt serial từ lúc tạo, không qua đây.
  if (next === 'processing' && o.kenhBan === 'online') {
    await openPackModal(o);
    return;
  }
  const body = buildOrderUpdateBody(o, {
```

(Giữ nguyên toàn bộ phần thân còn lại của hàm y hệt bên dưới dòng `const body = buildOrderUpdateBody(o, {`.)

- [ ] **Step 4: Chặn modal "Cập nhật trạng thái" sửa tay (`saveOrderStatus`) cho cùng trường hợp**

Sửa đầu `saveOrderStatus` (dòng 1824-1826) — thêm đoạn chặn ngay sau dòng `const o = editingOrder.value;`:

```js
const saveOrderStatus = async () => {
  orderStatusError.value = "";
  const o = editingOrder.value;
  if (orderStatusForm.trangThaiDonHang === 'processing' && o.trangThaiDonHang !== 'processing' && o.kenhBan === 'online') {
    showOrderModal.value = false;
    await openPackModal(o);
    return;
  }
  const body = buildOrderUpdateBody(o, {
```

- [ ] **Step 5: Thêm markup modal**

Thêm vào template, ngay sau khối `<!-- ══ MODAL CHON SERIAL (POS) ══ -->` kết thúc (sau dòng 3489, trước dòng trống tiếp theo) — tìm bằng cách grep `MODAL CHON SERIAL (POS)` rồi đọc tới thẻ `</div>` đóng ngoài cùng của khối đó để chèn ngay sau:

```html

    <!-- ══ MODAL CHỌN SERIAL TRƯỚC KHI ĐÓNG GÓI ══ -->
    <div v-if="showPackModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showPackModal=false">
      <div class="rounded-3 p-3" style="background:var(--bg-card);width:520px;max-height:85vh;overflow-y:auto;">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <div>
            <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.packModal.title') }}</div>
            <div class="text-secondary" style="font-size:0.75rem;">{{ packOrder?.maDonHang }}</div>
          </div>
          <button class="btn-close btn-sm" @click="showPackModal=false"></button>
        </div>

        <div v-if="packLoading" class="text-secondary small text-center py-4">{{ t('admin.packModal.loading') }}</div>
        <div v-else>
          <div v-if="packError" class="alert alert-danger py-2 small">{{ packError }}</div>
          <div v-for="line in packLines" :key="line.id" class="mb-3 p-2 rounded-2" style="background:var(--bg-card-inset);">
            <div class="d-flex justify-content-between mb-1">
              <span class="text-light">{{ productByBienThe(line.bienTheId)?.tenSanPham || line.maSku }}</span>
              <span class="text-secondary" style="font-size:0.75rem;">{{ t('admin.packModal.selectedCount', { selected: line.chosenSerialIds.size, count: line.soLuong }) }}</span>
            </div>
            <div v-if="packAvailableSerials(line).length === 0" class="text-danger small">{{ t('admin.packModal.noSerialAvailable') }}</div>
            <div v-else class="d-flex flex-wrap gap-2">
              <button v-for="s in packAvailableSerials(line)" :key="s.chiTietId"
                      class="btn btn-sm"
                      :class="line.chosenSerialIds.has(s.chiTietId) ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                      style="font-family:monospace;font-size:0.75rem;"
                      @click="packToggleSerial(line, s.chiTietId)">
                {{ s.soSerial }}
              </button>
            </div>
          </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-3">
          <button class="btn btn-sm btn-outline-secondary" @click="showPackModal=false">{{ t('admin.packModal.cancel') }}</button>
          <button class="btn btn-sm btn-success" :disabled="!packAllLinesComplete || packLoading" @click="confirmPack">{{ t('admin.packModal.confirm') }}</button>
        </div>
      </div>
    </div>
```

- [ ] **Step 6: Build để bắt lỗi cú pháp template/script**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không có lỗi Vue compiler/JS.

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add serial-picker modal gating the pack action for online orders"
```

---

### Task 8: Kiểm thử thủ công end-to-end

**Files:** không có file thay đổi — chỉ chạy và quan sát.

- [ ] **Step 1: Chạy backend**

Run (terminal riêng, để chạy nền):
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd spring-boot:run
```
Expected: log hiện `Started BackEndApplication` và không có exception lúc khởi động (đặc biệt là lỗi JPA mapping cho entity mới).

- [ ] **Step 2: Chạy frontend**

Run (terminal riêng):
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev
```

- [ ] **Step 3: Kịch bản chính — đơn online**

1. Ở trang khách hàng, đặt 1 đơn online với 1 sản phẩm có serial còn `trong_kho`.
2. Vào trang Admin → Đơn hàng, xác nhận serial vừa đặt đã chuyển sang `giu_hang` (kiểm tra ở modal "Chi tiết serial" của sản phẩm đó, hoặc bảng `chi_tiet_san_pham`).
3. Bấm "Xác nhận" cho đơn → trạng thái chuyển `confirmed`.
4. Bấm "📦 Đóng gói" → modal "Chọn serial trước khi đóng gói" hiện ra, serial đã giữ chỗ được tick sẵn.
5. Bấm "Xác nhận đóng gói" → đơn chuyển `processing`, serial chuyển `da_ban` (kiểm tra lại modal "Chi tiết serial").
6. Hủy 1 đơn khác đang ở trạng thái `giu_hang`/`da_ban` → xác nhận serial quay lại `trong_kho`.

- [ ] **Step 4: Kịch bản đối chứng — đơn tại quầy (POS) không đổi hành vi**

1. Vào tab "Bán tại quầy" (POS), tạo 1 đơn với 1 sản phẩm.
2. Xác nhận đơn được tạo với `trangThaiDonHang = "confirmed"` và serial đã `da_ban` ngay (không qua `giu_hang`, không có nút "Đóng gói" cần mở modal chọn serial).

- [ ] **Step 5: Dừng cả 2 server**

Nếu chạy bằng `run_in_background`, dừng qua công cụ quản lý tiến trình tương ứng; nếu chạy trực tiếp trong terminal, `Ctrl+C`.
