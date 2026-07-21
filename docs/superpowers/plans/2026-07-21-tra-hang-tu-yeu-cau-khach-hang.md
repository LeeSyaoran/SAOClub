# Trả hàng tự yêu cầu (khách hàng) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cho khách hàng tự gửi yêu cầu trả hàng (chọn sản phẩm + số lượng) cho đơn đã giao trong vòng 7 ngày, đơn có yêu cầu active chuyển từ tab "Hoàn tất" sang "Đã hủy/Trả hàng".

**Architecture:** Backend thêm 2 endpoint hẹp, riêng biệt khỏi CRUD staff hiện có (`PhieuTraHangController` giữ nguyên khóa staff-only ở class level, 2 method mới override bằng `@PreAuthorize("isAuthenticated()")`) — service tự suy khách hàng từ `SecurityContextHolder`, không tin input từ client. Frontend thêm modal chọn sản phẩm + 1 lượt fetch song song trong `fetchData()` của `AccountPage.vue`, đổi logic gom tab từ thuần `trangThaiDonHang` sang kết hợp thêm "có phiếu trả hàng active hay không".

**Tech Stack:** Spring Boot + JPA (Java), Vue 3 `<script setup>`.

## Global Constraints

- Không nới lỏng/đổi `@PreAuthorize` class-level hiện có của `PhieuTraHangController`/`ChiTietTraHangController` — chỉ thêm method mới với `@PreAuthorize` riêng.
- Endpoint tạo yêu cầu luôn tự suy khách hàng qua `SecurityContextHolder` — không nhận `khachHangId` từ request body.
- Luôn ép `trangThai='cho_xu_ly'`, `hinhThucHoan='vi'`, `nhanVienId=null` khi khách tự tạo — không cho client set các trường này.
- Chỉ cho tạo khi đơn `trangThaiDonHang='delivered'`, còn trong 7 ngày kể từ `ngayGiaoThucTe`, và đơn chưa có phiếu nào đang `cho_xu_ly`/`da_xu_ly`.

Spec đầy đủ: `docs/superpowers/specs/2026-07-21-tra-hang-tu-yeu-cau-khach-hang-design.md`.

---

### Task 1: Backend — endpoint tự yêu cầu trả hàng cho khách hàng

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/DongTraRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/YeuCauTraHangRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/PhieuTraHangRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/PhieuTraHangService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/PhieuTraHangController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/PhieuTraHangServiceTest.java`

**Interfaces:**
- Produces: `POST /api/phieu-tra-hang/tu-yeu-cau` (body `YeuCauTraHangRequest`) → `201` + `PhieuTraHang`; `GET /api/phieu-tra-hang/don-hang/{donHangId}` → `List<PhieuTraHangResponse>`.

- [ ] **Step 1: Sửa bug JOIN ngầm trên FK nullable `nhan_vien` trong `PhieuTraHangRepository`**

Bug có sẵn từ trước (cùng dạng bug đã fix ở `DonHangRepository` — path navigation `p.nhanVien.nhanVienId` trên `@ManyToOne` nullable tạo INNER JOIN ngầm, âm thầm loại bỏ mọi phiếu có `nhan_vien_id IS NULL`). Phiếu do khách tự tạo ở Task này LUÔN có `nhanVien=null` — nếu không sửa, `ReturnsPanel.vue` (nhân viên) sẽ không bao giờ thấy các phiếu khách gửi.

Trong `PhieuTraHangRepository.java`, sửa:

```java
    @Query("SELECT new com.example.backend.response.PhieuTraHangResponse(p.phieuTraId, p.donHang.id, nv.nhanVienId, p.lyDo, p.ngayTra, p.trangThai, p.soTienHoan, p.hinhThucHoan, p.ghiChu) " +
           "FROM PhieuTraHang p LEFT JOIN p.nhanVien nv")
    List<PhieuTraHangResponse> hienThiPhieuTraHang();

    List<PhieuTraHang> findByDonHang_Id(Integer donHangId);
```

(`p.donHang.id` giữ nguyên — FK này `nullable=false` nên không có bug.)

- [ ] **Step 2: Build backend, xác nhận không lỗi compile**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Tạo `DongTraRequest`**

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
public class DongTraRequest {
    @NotNull(message = "Dòng đơn hàng không được để trống")
    private Integer chiTietDonHangId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;
}
```

- [ ] **Step 4: Tạo `YeuCauTraHangRequest`**

```java
package com.example.backend.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class YeuCauTraHangRequest {
    @NotNull(message = "Đơn hàng không được để trống")
    private Integer donHangId;

    @NotBlank(message = "Lý do không được để trống")
    private String lyDo;

    @NotEmpty(message = "Phải chọn ít nhất 1 sản phẩm để trả")
    @Valid
    private List<DongTraRequest> dongTra;
}
```

- [ ] **Step 5: Viết test trước cho `taoYeuCauTuKhachHang` — case hợp lệ**

Trong `PhieuTraHangServiceTest.java`, thêm imports:

```java
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.entity.ChucVu;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.DongTraRequest;
import com.example.backend.request.YeuCauTraHangRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
```

Thêm static imports: `assertThatThrownBy`, `lenient`, `mock` (cạnh `when` đã có trong `import static org.mockito.Mockito.*;` — không cần thêm nếu file đã dùng wildcard `Mockito.*`; kiểm tra lại import hiện tại của file trước khi thêm để tránh trùng).

Thêm mock field mới:

```java
    @Mock private ChiTietDonHangRepository chiTietDonHangRepository;
    @Mock private ChiTietTraHangRepository chiTietTraHangRepository;
    @Mock private TaiKhoanRepository taiKhoanRepository;
```

Thêm helper (giống hệt pattern đã dùng ở `LichSuDonHangServiceTest.java`):

```java
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
        lenient().when(auth.getName()).thenReturn(username);
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(auth);
    }

    private TaiKhoan taiKhoanKhachHang(String username, Integer khachHangId) {
        ChucVu chucVu = new ChucVu();
        chucVu.setMaChucVu("khach_hang");
        KhachHang kh = new KhachHang();
        kh.setKhachHangId(khachHangId);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(username);
        tk.setChucVu(chucVu);
        tk.setKhachHang(kh);
        return tk;
    }
```

Thêm test:

```java
    @Test
    void taoYeuCau_hopLe_taoPhieuChoXuLyHoanVi() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(2));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());

        ChiTietDonHang dong = new ChiTietDonHang();
        dong.setId(100);
        dong.setDonHang(donHang);
        dong.setSoLuong(2);
        dong.setDonGia(BigDecimal.valueOf(500_000));
        when(chiTietDonHangRepository.findById(100)).thenReturn(Optional.of(dong));

        when(phieuTraHangRepository.save(any(PhieuTraHang.class))).thenAnswer(inv -> inv.getArgument(0));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        PhieuTraHang saved = service.taoYeuCauTuKhachHang(req);

        assertThat(saved.getTrangThai()).isEqualTo("cho_xu_ly");
        assertThat(saved.getHinhThucHoan()).isEqualTo("vi");
        assertThat(saved.getNhanVien()).isNull();
        assertThat(saved.getSoTienHoan()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
        verify(chiTietTraHangRepository).save(any(ChiTietTraHang.class));
    }

    @Test
    void taoYeuCau_donKhongPhaiCuaMinh_biTuChoi() {
        loginAs("khach2");
        when(taiKhoanRepository.findByUsername("khach2")).thenReturn(Optional.of(taiKhoanKhachHang("khach2", 43)));

        KhachHang chuDon = new KhachHang();
        chuDon.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(chuDon);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(2));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(AccessDeniedException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_quaHan7Ngay_biChan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(8));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 ngày");
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_donCoPhieuChoXuLyRoi_biChanTaoTrung() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(1));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));

        PhieuTraHang phieuCu = new PhieuTraHang();
        phieuCu.setTrangThai("cho_xu_ly");
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of(phieuCu));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 1)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }

    @Test
    void taoYeuCau_soLuongVuotSoDaMua_biChan() {
        loginAs("khach1");
        when(taiKhoanRepository.findByUsername("khach1")).thenReturn(Optional.of(taiKhoanKhachHang("khach1", 42)));

        KhachHang kh = new KhachHang();
        kh.setKhachHangId(42);
        DonHang donHang = new DonHang();
        donHang.setId(9);
        donHang.setKhachHang(kh);
        donHang.setTrangThaiDonHang("delivered");
        donHang.setNgayGiaoThucTe(LocalDateTime.now().minusDays(1));
        when(donHangRepository.findById(9)).thenReturn(Optional.of(donHang));
        when(phieuTraHangRepository.findByDonHang_Id(9)).thenReturn(List.of());

        ChiTietDonHang dong = new ChiTietDonHang();
        dong.setId(100);
        dong.setDonHang(donHang);
        dong.setSoLuong(1);
        dong.setDonGia(BigDecimal.valueOf(500_000));
        when(chiTietDonHangRepository.findById(100)).thenReturn(Optional.of(dong));

        YeuCauTraHangRequest req = new YeuCauTraHangRequest(9, "Không vừa ý", List.of(new DongTraRequest(100, 2)));

        assertThatThrownBy(() -> service.taoYeuCauTuKhachHang(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(phieuTraHangRepository, never()).save(any());
    }
```

- [ ] **Step 6: Chạy test, xác nhận FAIL (chưa có method/field mới)**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test -Dtest=PhieuTraHangServiceTest`
Expected: FAIL biên dịch — `taoYeuCauTuKhachHang` chưa tồn tại trong `PhieuTraHangService`.

- [ ] **Step 7: Thêm dependency + method `taoYeuCauTuKhachHang`/`getByDonHang` vào `PhieuTraHangService`**

Thêm import:

```java
import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietTraHang;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.ChiTietDonHangRepository;
import com.example.backend.repository.ChiTietTraHangRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.DongTraRequest;
import com.example.backend.request.YeuCauTraHangRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
```

Thêm field:

```java
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private ChiTietTraHangRepository chiTietTraHangRepository;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
```

Thêm method (cuối class, trước `delete()` hoặc sau — vị trí không quan trọng):

```java
    // ── Khách hàng tự gửi yêu cầu trả hàng (tách biệt hoàn toàn CRUD staff ở trên) ──────

    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    // Chỉ đúng chủ đơn mới được tự tạo yêu cầu — không cho staff bypass qua endpoint này
    // (staff có luồng riêng: PhieuTraHangController CRUD + ReturnsPanel.vue).
    private void assertIsOwner(DonHang donHang) {
        TaiKhoan tk = currentAccount();
        boolean laChuDon = tk != null && tk.getKhachHang() != null && donHang.getKhachHang() != null
                && donHang.getKhachHang().getKhachHangId().equals(tk.getKhachHang().getKhachHangId());
        if (!laChuDon)
            throw new AccessDeniedException("Không có quyền tạo yêu cầu trả hàng cho đơn này");
    }

    // Staff xem được mọi đơn, khách chỉ xem đơn của chính mình — dùng cho getByDonHang().
    private boolean isStaffOrOwner(DonHang donHang) {
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        if (!"khach_hang".equals(tk.getChucVu().getMaChucVu())) return true;
        return tk.getKhachHang() != null && donHang.getKhachHang() != null
                && donHang.getKhachHang().getKhachHangId().equals(tk.getKhachHang().getKhachHangId());
    }

    @Transactional
    public PhieuTraHang taoYeuCauTuKhachHang(YeuCauTraHangRequest request) {
        DonHang donHang = donHangRepository.findById(request.getDonHangId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + request.getDonHangId()));
        assertIsOwner(donHang);

        if (!"delivered".equals(donHang.getTrangThaiDonHang()))
            throw new IllegalArgumentException("Chỉ có thể yêu cầu trả hàng khi đơn đã giao");
        if (donHang.getNgayGiaoThucTe() == null
                || LocalDateTime.now().isAfter(donHang.getNgayGiaoThucTe().plusDays(7)))
            throw new IllegalArgumentException("Đã quá hạn 7 ngày trả hàng kể từ khi nhận hàng");

        boolean coPhieuActive = phieuTraHangRepository.findByDonHang_Id(donHang.getId()).stream()
                .anyMatch(p -> "cho_xu_ly".equals(p.getTrangThai()) || "da_xu_ly".equals(p.getTrangThai()));
        if (coPhieuActive)
            throw new IllegalArgumentException("Đơn này đã có yêu cầu trả hàng đang xử lý");

        List<ChiTietTraHang> dongTraHang = new ArrayList<>();
        BigDecimal tongTienHoan = BigDecimal.ZERO;
        for (DongTraRequest d : request.getDongTra()) {
            ChiTietDonHang item = chiTietDonHangRepository.findById(d.getChiTietDonHangId())
                    .orElseThrow(() -> new IllegalArgumentException("Dòng đơn hàng không tồn tại với id: " + d.getChiTietDonHangId()));
            if (!item.getDonHang().getId().equals(donHang.getId()))
                throw new IllegalArgumentException("Dòng #" + item.getId() + " không thuộc đơn hàng này");
            if (d.getSoLuong() > item.getSoLuong())
                throw new IllegalArgumentException(
                        "Dòng #" + item.getId() + " chỉ mua " + item.getSoLuong() + ", không thể trả " + d.getSoLuong());

            ChiTietTraHang dong = new ChiTietTraHang();
            dong.setBienThe(item.getBienThe());
            dong.setChiTietSanPham(item.getChiTietSanPham());
            dong.setSoLuong(d.getSoLuong());
            dong.setDonGiaHoan(item.getDonGia());
            dongTraHang.add(dong);
            tongTienHoan = tongTienHoan.add(item.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())));
        }

        PhieuTraHang phieu = new PhieuTraHang();
        phieu.setDonHang(donHang);
        phieu.setNhanVien(null);
        phieu.setLyDo(request.getLyDo());
        phieu.setNgayTra(LocalDateTime.now());
        phieu.setTrangThai("cho_xu_ly");
        phieu.setSoTienHoan(tongTienHoan);
        phieu.setHinhThucHoan("vi");
        PhieuTraHang saved = phieuTraHangRepository.save(phieu);

        for (ChiTietTraHang dong : dongTraHang) {
            dong.setPhieuTraHang(saved);
            chiTietTraHangRepository.save(dong);
        }
        return saved;
    }

    public List<PhieuTraHangResponse> getByDonHang(Integer donHangId) {
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại với id: " + donHangId));
        if (!isStaffOrOwner(donHang))
            throw new AccessDeniedException("Không có quyền xem yêu cầu trả hàng của đơn này");
        return phieuTraHangRepository.findByDonHang_Id(donHangId).stream()
                .map(p -> new PhieuTraHangResponse(
                        p.getPhieuTraId(), p.getDonHang().getId(),
                        p.getNhanVien() != null ? p.getNhanVien().getNhanVienId() : null,
                        p.getLyDo(), p.getNgayTra(), p.getTrangThai(), p.getSoTienHoan(), p.getHinhThucHoan(), p.getGhiChu()))
                .toList();
    }
```

- [ ] **Step 8: Chạy lại test, xác nhận PASS**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test -Dtest=PhieuTraHangServiceTest`
Expected: PASS toàn bộ (5 test mới + các test cũ trong file).

- [ ] **Step 9: Thêm 2 endpoint vào `PhieuTraHangController`**

Thêm import: `import com.example.backend.request.YeuCauTraHangRequest;` và `java.util.List` (nếu chưa có).

Thêm 2 method vào class (class-level `@PreAuthorize` hiện có khóa staff — 2 method dưới đây PHẢI có `@PreAuthorize` riêng để override, nếu không sẽ vẫn bị khóa staff-only, khách gọi sẽ nhận 403):

```java
    // Giữ mở cho MỌI người dùng đã đăng nhập (override @PreAuthorize class-level ở trên) —
    // khách hàng tự gửi yêu cầu trả hàng cho đơn của chính mình. Service tự suy khách hàng
    // qua SecurityContextHolder, không tin donHangId/khách hàng từ client.
    @PreAuthorize("isAuthenticated()")
    @PostMapping("tu-yeu-cau")
    public ResponseEntity<PhieuTraHang> taoYeuCauTuKhachHang(@Valid @RequestBody YeuCauTraHangRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phieuTraHangService.taoYeuCauTuKhachHang(request));
    }

    // Giữ mở cho MỌI người dùng đã đăng nhập — khách xem yêu cầu trả hàng của đơn mình,
    // nhân viên xem được của bất kỳ đơn nào (service tự kiểm tra quyền theo vai trò).
    @PreAuthorize("isAuthenticated()")
    @GetMapping("don-hang/{donHangId}")
    public List<PhieuTraHangResponse> getByDonHang(@PathVariable Integer donHangId) {
        return phieuTraHangService.getByDonHang(donHangId);
    }
```

- [ ] **Step 10: Chạy toàn bộ test suite backend**

Run: `cd BackEnd && JAVA_HOME="/c/Program Files/Java/jdk-21" ./mvnw -q -o test`
Expected: BUILD SUCCESS, không regressions.

- [ ] **Step 11: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/repository/PhieuTraHangRepository.java \
        BackEnd/src/main/java/com/example/backend/request/DongTraRequest.java \
        BackEnd/src/main/java/com/example/backend/request/YeuCauTraHangRequest.java \
        BackEnd/src/main/java/com/example/backend/service/PhieuTraHangService.java \
        BackEnd/src/main/java/com/example/backend/controller/PhieuTraHangController.java \
        BackEnd/src/test/java/com/example/backend/service/PhieuTraHangServiceTest.java
git commit -m "feat: let customers self-request returns for delivered orders"
```

---

### Task 2: Frontend — form chọn sản phẩm trả hàng + tách tab "Đã hủy/Trả hàng"

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/Service/PhieuTraHangService.js`
- Create: `FrontEnd/QLBanMayTinh/src/components/order/ReturnRequestModal.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/{vi,en,ja,ko,zh}.js`

**Interfaces:**
- Consumes: `POST /api/phieu-tra-hang/tu-yeu-cau`, `GET /api/phieu-tra-hang/don-hang/{donHangId}` (Task 1).
- Produces: component `ReturnRequestModal` — props `{ order: Object, items: Array }`, emits `close`, `submitted`.

- [ ] **Step 1: Thêm 2 hàm vào `Service/PhieuTraHangService.js`**

```js
// Khách hàng tự gửi yêu cầu trả hàng — body: { donHangId, lyDo, dongTra: [{ chiTietDonHangId, soLuong }] }
export const taoYeuCau = (body) => post('/api/phieu-tra-hang/tu-yeu-cau', body);

// Lấy các yêu cầu trả hàng của 1 đơn — dùng cho AccountPage hiện trạng thái xử lý.
export const getByDonHang = (donHangId) => get(`/api/phieu-tra-hang/don-hang/${donHangId}`);
```

(File đã có sẵn `import { get, post, put, del } from './api.js';` ở đầu — không cần sửa dòng import.)

- [ ] **Step 2: Thêm i18n keys vào cả 5 file locale**

`vi.js` (trong khối `account`, sau `trackingCodeCopied:` đã có từ tính năng trước):

```js
    requestReturn: "Trả hàng",
    returnDeadlinePassed: "Đã quá hạn trả hàng",
    returnModalTitle: "Yêu cầu trả hàng",
    returnModalSelectItems: "Chọn sản phẩm muốn trả",
    returnModalReasonLabel: "Lý do trả hàng",
    returnModalReasonPlaceholder: "VD: Sản phẩm lỗi, không đúng mô tả, đổi ý...",
    returnModalSubmit: "Gửi yêu cầu",
    returnModalCancel: "Hủy",
    returnModalErrorNoItems: "Vui lòng chọn ít nhất 1 sản phẩm",
    returnModalErrorNoReason: "Vui lòng nhập lý do trả hàng",
    returnStatusPending: "Chờ xử lý",
    returnStatusDone: "Đã xử lý",
    returnStatusRejected: "Từ chối",
```

`en.js`:

```js
    requestReturn: "Return",
    returnDeadlinePassed: "Return window expired",
    returnModalTitle: "Return request",
    returnModalSelectItems: "Select items to return",
    returnModalReasonLabel: "Return reason",
    returnModalReasonPlaceholder: "e.g. Defective item, not as described, changed my mind...",
    returnModalSubmit: "Submit request",
    returnModalCancel: "Cancel",
    returnModalErrorNoItems: "Please select at least 1 item",
    returnModalErrorNoReason: "Please enter a return reason",
    returnStatusPending: "Pending",
    returnStatusDone: "Processed",
    returnStatusRejected: "Rejected",
```

`ja.js`:

```js
    requestReturn: "返品",
    returnDeadlinePassed: "返品期限切れ",
    returnModalTitle: "返品リクエスト",
    returnModalSelectItems: "返品する商品を選択",
    returnModalReasonLabel: "返品理由",
    returnModalReasonPlaceholder: "例：不良品、説明と違う、気が変わった...",
    returnModalSubmit: "リクエストを送信",
    returnModalCancel: "キャンセル",
    returnModalErrorNoItems: "少なくとも1つの商品を選択してください",
    returnModalErrorNoReason: "返品理由を入力してください",
    returnStatusPending: "処理待ち",
    returnStatusDone: "処理済み",
    returnStatusRejected: "却下",
```

`ko.js`:

```js
    requestReturn: "반품",
    returnDeadlinePassed: "반품 기한 만료",
    returnModalTitle: "반품 요청",
    returnModalSelectItems: "반품할 상품 선택",
    returnModalReasonLabel: "반품 사유",
    returnModalReasonPlaceholder: "예: 불량품, 설명과 다름, 마음이 바뀜...",
    returnModalSubmit: "요청 보내기",
    returnModalCancel: "취소",
    returnModalErrorNoItems: "최소 1개 상품을 선택해 주세요",
    returnModalErrorNoReason: "반품 사유를 입력해 주세요",
    returnStatusPending: "처리 대기",
    returnStatusDone: "처리 완료",
    returnStatusRejected: "거절됨",
```

`zh.js`:

```js
    requestReturn: "退货",
    returnDeadlinePassed: "已超过退货期限",
    returnModalTitle: "退货申请",
    returnModalSelectItems: "选择要退的商品",
    returnModalReasonLabel: "退货原因",
    returnModalReasonPlaceholder: "例如：商品有问题、与描述不符、改变主意...",
    returnModalSubmit: "提交申请",
    returnModalCancel: "取消",
    returnModalErrorNoItems: "请至少选择1件商品",
    returnModalErrorNoReason: "请填写退货原因",
    returnStatusPending: "待处理",
    returnStatusDone: "已处理",
    returnStatusRejected: "已拒绝",
```

(Chèn khối trên vào đúng object `account: { ... }` của mỗi file, cạnh các key `trackingCode*`/`buyAgain*` đã có từ trước — đọc file trước khi chèn để lấy đúng vị trí, không tạo trùng key.)

- [ ] **Step 3: Tạo component `ReturnRequestModal.vue`**

```vue
<template>
  <div class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
       style="background:var(--bg-overlay); z-index:1080;" @click.self="$emit('close')">
    <div class="rounded-4 d-flex flex-column" style="background:var(--bg-card); border:1px solid var(--border-color-strong); width:480px; max-width:95vw; max-height:90vh;">
      <div class="d-flex justify-content-between align-items-center p-3 border-bottom border-secondary fw-bold">
        <span>{{ t('account.returnModalTitle') }}</span>
        <button class="btn-close btn-close-white btn-sm" @click="$emit('close')"></button>
      </div>
      <div class="overflow-y-auto p-4">
        <div v-if="error" class="alert alert-danger small py-2 mb-3">{{ error }}</div>
        <div class="mb-2 small text-secondary">{{ t('account.returnModalSelectItems') }}</div>
        <div class="d-flex flex-column gap-2 mb-3">
          <div v-for="line in lines" :key="line.chiTietDonHangId"
               class="d-flex align-items-center gap-2 p-2 rounded-3" style="background:var(--bg-card-inset);">
            <input type="checkbox" v-model="line.checked" class="form-check-input mt-0" />
            <span class="flex-grow-1" style="font-size:12.5px; color:var(--text-primary);">{{ line.tenSanPham }}</span>
            <input type="number" min="1" :max="line.soLuongMua" v-model.number="line.soLuong"
                   :disabled="!line.checked"
                   class="form-control form-control-sm" style="width:64px; background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong);" />
            <span class="text-secondary" style="font-size:11px;">/ {{ line.soLuongMua }}</span>
          </div>
        </div>
        <label class="form-label small text-secondary">{{ t('account.returnModalReasonLabel') }}</label>
        <textarea v-model="lyDo" rows="3" class="form-control form-control-sm"
                  :placeholder="t('account.returnModalReasonPlaceholder')"
                  style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong);"></textarea>
      </div>
      <div class="d-flex justify-content-end gap-2 p-3 border-top border-secondary">
        <button class="btn btn-sm btn-outline-secondary" @click="$emit('close')">{{ t('account.returnModalCancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" :disabled="submitting" @click="submit">{{ t('account.returnModalSubmit') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { t } from '../../i18n/index.js';
import * as PhieuTraHangService from '../../Service/PhieuTraHangService.js';

const props = defineProps({
  order: { type: Object, required: true },
  items: { type: Array, default: () => [] },
});
const emit = defineEmits(['close', 'submitted']);

const lines = ref(props.items.map(item => ({
  chiTietDonHangId: item.id,
  tenSanPham: item.tenSanPham || item.maSku,
  soLuongMua: item.soLuong,
  soLuong: item.soLuong,
  checked: false,
})));
const lyDo = ref('');
const error = ref('');
const submitting = ref(false);

const submit = async () => {
  error.value = '';
  const chosen = lines.value.filter(l => l.checked);
  if (chosen.length === 0) { error.value = t('account.returnModalErrorNoItems'); return; }
  if (!lyDo.value.trim()) { error.value = t('account.returnModalErrorNoReason'); return; }

  submitting.value = true;
  try {
    const res = await PhieuTraHangService.taoYeuCau({
      donHangId: props.order.donHangId,
      lyDo: lyDo.value.trim(),
      dongTra: chosen.map(l => ({ chiTietDonHangId: l.chiTietDonHangId, soLuong: l.soLuong })),
    });
    if (!res.ok) { error.value = await res.text().catch(() => res.statusText); return; }
    emit('submitted');
  } finally {
    submitting.value = false;
  }
};
</script>
```

- [ ] **Step 4: Wiring trong `AccountPage.vue` — import + state + fetch song song**

Thêm import:

```js
import * as PhieuTraHangService from "../Service/PhieuTraHangService.js";
import ReturnRequestModal from "../components/order/ReturnRequestModal.vue";
```

Thêm state cạnh `historyByOrder`:

```js
const returnsByOrder = ref({});  // { [donHangId]: PhieuTraHangResponse[] }
const returnModalOrder = ref(null);  // đơn đang mở modal trả hàng (null = đóng)
```

Trong `fetchData()`, thêm 1 lượt fetch song song ngay sau khối `historyEntries`/`historyByOrder.value = ...`:

```js
    const returnEntries = await Promise.all(
      orders.value.map(async (o) => [
        o.donHangId,
        await PhieuTraHangService.getByDonHang(o.donHangId).catch(() => []),
      ])
    );
    returnsByOrder.value = Object.fromEntries(returnEntries);
```

- [ ] **Step 5: Helper phân loại tab dựa trên yêu cầu trả hàng active**

Thêm ngay dưới `TAB_STATUS_GROUPS`:

```js
// Đơn có phiếu trả hàng đang active (chờ xử lý/đã xử lý) — luôn hiện ở tab "Đã hủy/Trả
// hàng" bất kể trangThaiDonHang thực tế (thường vẫn là "delivered", vì phiếu trả hàng
// không tự đổi trạng thái đơn — nhân viên tự bấm đổi riêng nếu muốn).
const hasActiveReturn = (donHangId) =>
  (returnsByOrder.value[donHangId] || []).some(r => r.trangThai === 'cho_xu_ly' || r.trangThai === 'da_xu_ly');

const orderTabId = (o) => {
  if (hasActiveReturn(o.donHangId)) return 'cancelled';
  for (const [tabId, statuses] of Object.entries(TAB_STATUS_GROUPS)) {
    if (statuses.includes(o.trangThaiDonHang)) return tabId;
  }
  return null;
};

// Còn trong hạn 7 ngày kể từ khi nhận hàng, và đơn chưa có phiếu trả hàng active.
const canRequestReturn = (o) => {
  if (o.trangThaiDonHang !== 'delivered' || hasActiveReturn(o.donHangId) || !o.ngayGiaoThucTe) return false;
  return Date.now() <= new Date(o.ngayGiaoThucTe).getTime() + 7 * 24 * 60 * 60 * 1000;
};

const RETURN_STATUS_LABEL = { cho_xu_ly: 'account.returnStatusPending', da_xu_ly: 'account.returnStatusDone', tu_choi: 'account.returnStatusRejected' };
const RETURN_STATUS_COLOR = {
  cho_xu_ly: { bg: 'rgba(250,204,21,0.15)', text: '#facc15' },
  da_xu_ly:  { bg: 'rgba(34,197,94,0.15)',  text: '#22c55e' },
  tu_choi:   { bg: 'rgba(239,68,68,0.15)',  text: '#f87171' },
};
```

- [ ] **Step 6: Sửa `tabOrderCounts`/`currentOrders`/`historyOrders` dùng `orderTabId()` thay vì check trực tiếp `trangThaiDonHang`**

```js
const tabOrderCounts = computed(() => {
  const counts = { pending: 0, shipping: 0, completed: 0, cancelled: 0 };
  orders.value.forEach(o => {
    const tabId = orderTabId(o);
    if (tabId && tabId in counts) counts[tabId]++;
  });
  return counts;
});

const currentOrders = computed(() => {
  return (activeTab.value === 'pending' || activeTab.value === 'shipping')
    ? orders.value.filter(o => orderTabId(o) === activeTab.value)
    : [];
});

const historyOrders = computed(() => {
  return (activeTab.value === 'completed' || activeTab.value === 'cancelled')
    ? orders.value.filter(o => orderTabId(o) === activeTab.value)
    : [];
});
```

- [ ] **Step 7: Thêm nút "Trả hàng" cạnh nút "Mua lại" trong dòng đơn của `historyOrders`**

Ngay sau khối `<button v-if="o.trangThaiDonHang === 'delivered'" ... Mua lại ...>` (trong `historyOrders`/tab Hoàn tất-Trả hàng), thêm:

```html
                <button v-if="canRequestReturn(o)"
                        class="btn btn-sm fw-bold rounded-pill px-3"
                        style="background:var(--bg-input); border:1px solid var(--border-color-strong); color:var(--text-primary); font-size:11.5px;"
                        @click.stop="returnModalOrder = o">
                  ↩️ {{ t('account.requestReturn') }}
                </button>
```

- [ ] **Step 8: Hiện badge trạng thái + lý do khi đơn có phiếu trả hàng, trong phần mở rộng**

Ngay sau `<OrderTrackingLog ... />` đã có trong khối `v-if="expandedHistoryOrders.has(o.donHangId)"`, thêm:

```html
              <div v-if="(returnsByOrder[o.donHangId] || []).length" class="rounded-3 mt-2 p-2 d-flex flex-column gap-2" style="background:var(--bg-card-alt);">
                <div v-for="r in returnsByOrder[o.donHangId]" :key="r.phieuTraId" class="d-flex flex-column gap-1">
                  <div class="d-flex align-items-center gap-2">
                    <span class="badge px-2 py-1 rounded-pill fw-semibold"
                          :style="{ background: RETURN_STATUS_COLOR[r.trangThai]?.bg, color: RETURN_STATUS_COLOR[r.trangThai]?.text }">
                      {{ t(RETURN_STATUS_LABEL[r.trangThai]) }}
                    </span>
                    <span style="font-size:11px; color:var(--text-secondary);">{{ formatPrice(r.soTienHoan) }}</span>
                  </div>
                  <div style="font-size:12px; color:var(--text-primary);">{{ r.lyDo }}</div>
                </div>
              </div>
```

- [ ] **Step 9: Gắn `<ReturnRequestModal>` ở cuối template (cạnh modal `ProductDetail` đã có)**

```html
    <ReturnRequestModal v-if="returnModalOrder"
                         :order="returnModalOrder"
                         :items="itemsByOrder[returnModalOrder.donHangId] || []"
                         @close="returnModalOrder = null"
                         @submitted="returnModalOrder = null; fetchData();" />
```

- [ ] **Step 10: Verify bằng tay trên trình duyệt**

Run: `cd FrontEnd/QLBanMayTinh && npm run dev`

1. Đăng nhập khách hàng có đơn "delivered" trong vòng 7 ngày gần đây → tab "Hoàn tất".
2. Expected: đơn hiện cả 2 nút "Mua lại" và "Trả hàng".
3. Bấm "Trả hàng" → modal mở, tick 1 sản phẩm, nhập lý do, bấm "Gửi yêu cầu".
4. Expected: modal đóng, đơn đó BIẾN MẤT khỏi tab "Hoàn tất", XUẤT HIỆN ở tab "Đã hủy/Trả hàng" với badge "Chờ xử lý".
5. Mở rộng đơn đó → thấy lý do đã nhập.
6. Thử bấm lại "Trả hàng" cho đơn khác đã quá 7 ngày (nếu có dữ liệu) → nút không hiện.

- [ ] **Step 11: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/PhieuTraHangService.js \
        FrontEnd/QLBanMayTinh/src/components/order/ReturnRequestModal.vue \
        FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue \
        FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/en.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js \
        FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js
git commit -m "feat: let customers request returns from the account page"
```

---

## Self-Review

**1. Spec coverage:**
- Endpoint riêng, không đụng CRUD staff hiện có → Task 1 ✅
- Chọn từng sản phẩm + số lượng, hạn 7 ngày, chặn trùng phiếu active → Task 1 (validate) + Task 2 (UI) ✅
- Đơn có phiếu trả hàng chuyển hẳn sang tab "Đã hủy/Trả hàng" → Task 2 ✅
- Bug JOIN ngầm trên `nhan_vien` (phát hiện lúc lập plan — nếu không sửa, phiếu khách tạo sẽ vô hình với `ReturnsPanel.vue` của nhân viên, feature coi như hỏng nửa đường) → Task 1 Step 1 ✅

**2. Placeholder scan:** không còn "TBD" — mọi code đều đầy đủ, copy đúng field/kiểu dữ liệu từ entity/request thực tế đã đọc.

**3. Type/tên nhất quán:** `chiTietDonHangId` giữ nguyên tên từ request → entity → frontend; `RETURN_STATUS_LABEL`/`RETURN_STATUS_COLOR` dùng đúng 3 giá trị enum `cho_xu_ly`/`da_xu_ly`/`tu_choi` đã xác nhận từ CHECK constraint DB.
