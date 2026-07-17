# Trang Cài đặt (admin) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Biến trang "Cài đặt" từ 1 card tĩnh thành 5 card có chức năng thật: đổi mật khẩu, thông tin cửa hàng (+ logo), ngưỡng cảnh báo tồn kho (áp dụng hàng loạt), và giao diện/ngôn ngữ/định dạng số — áp dụng thật cho toàn app bằng cách gộp 9 nơi định nghĩa `formatPrice` về 1 hàm dùng chung.

**Architecture:** 1 bảng cấu hình singleton `cai_dat_he_thong` (luôn đúng 1 dòng id=1) + `CaiDatController` (`/api/cai-dat`) cho CRUD cấu hình và áp dụng ngưỡng tồn kho hàng loạt; đổi mật khẩu đặt logic ở `AuthService` (để test được không cần mock `SecurityContextHolder`) nhưng endpoint vẫn nằm ở `CaiDatController` vì `/api/auth/**` đang `permitAll()` toàn bộ trong `SecurityConfig`. Frontend thêm `stores/settings.js` (load 1 lần lúc khởi động, giống `stores/theme.js`) và `utils/formatPrice.js` dùng chung.

**Tech Stack:** Spring Boot 4.0.6 (JPA/Hibernate, SQL Server), Vue 3 `<script setup>`, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- Toàn bộ endpoint mới nằm dưới `/api/cai-dat/**` — KHÔNG được đặt dưới `/api/auth/**` (đang `permitAll()` trong `SecurityConfig.java`, đặt nhầm sẽ thành public không cần đăng nhập).
- Style DTO response: Lombok `@AllArgsConstructor @Getter @Setter` (không `@Data`, không `@NoArgsConstructor`) — đúng theo `DashboardKpiResponse.java` hiện có.
- Style DTO request: `@NoArgsConstructor @Getter @Setter` + Bean Validation annotation (`@NotBlank`...) — đúng theo `LoginRequest.java` hiện có.
- Style entity: `@Data @NoArgsConstructor @AllArgsConstructor @Getter @Setter @Entity` — đúng theo `DanhMuc.java`/`TaiKhoan.java` hiện có, field injection `@Autowired` (không constructor injection) ở service/controller.
- SQL migration idempotent theo đúng khuôn `IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'x') BEGIN ... END` đã dùng xuyên suốt `Database/QLBanMayTinh.sql` — user luôn chạy lại TOÀN BỘ file.
- Frontend dùng import tương đối (`../`, `../../`) theo đúng quy ước hiện có trong dự án — KHÔNG dùng alias `@/` (khai báo sẵn trong `vite.config.js` nhưng không nơi nào trong codebase thực sự dùng nó).
- `api.js`: `get()` trả `Promise<JSON đã parse>`; `post()`/`put()` trả `Promise<Response>` CHƯA parse — caller phải tự `.json()` + tự kiểm tra `res.ok`.
- i18n: thêm key cho đủ cả 5 file `vi.js`, `en.js`, `zh.js`, `ko.js`, `ja.js` — không được thiếu ngôn ngữ nào (đúng quy ước dự án).

---

### Task 1: Backend — Bảng `cai_dat_he_thong`, Entity, Repository

**Files:**
- Modify: `Database/QLBanMayTinh.sql`
- Create: `BackEnd/src/main/java/com/example/backend/entity/CaiDatHeThong.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/CaiDatHeThongRepository.java`

**Interfaces:**
- Produces: entity `CaiDatHeThong` (field `caiDatId`, `tenCuaHang`, `diaChi`, `soDienThoai`, `email`, `maSoThue`, `logoUrl`, `nguongTonKhoMacDinh`, `ngonNguMacDinh`, `dinhDangSo`) và `CaiDatHeThongRepository extends JpaRepository<CaiDatHeThong, Integer>` — Task 2 dùng trực tiếp.

- [ ] **Step 1: Thêm bảng vào cuối `Database/QLBanMayTinh.sql`**

Thêm vào cuối file (sau dòng `GO` cuối cùng, dòng 9320):

```sql

-- ============================================================
--  CÀI ĐẶT HỆ THỐNG (singleton — luôn đúng 1 dòng, cai_dat_id = 1)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cai_dat_he_thong')
BEGIN
    CREATE TABLE cai_dat_he_thong (
        cai_dat_id                INT            PRIMARY KEY,
        ten_cua_hang              NVARCHAR(200)  NOT NULL DEFAULT N'SAOPhone',
        dia_chi                   NVARCHAR(300)  NOT NULL DEFAULT N'',
        so_dien_thoai             NVARCHAR(20)   NOT NULL DEFAULT N'',
        email                     NVARCHAR(100)  NOT NULL DEFAULT N'',
        ma_so_thue                NVARCHAR(20)   NOT NULL DEFAULT N'',
        logo_url                  NVARCHAR(300)  NULL,
        nguong_ton_kho_mac_dinh   INT            NOT NULL DEFAULT 5,
        ngon_ngu_mac_dinh         VARCHAR(5)     NOT NULL DEFAULT 'vi'
            CONSTRAINT CK_cai_dat_ngonngu CHECK (ngon_ngu_mac_dinh IN ('vi','en','zh','ja','ko')),
        dinh_dang_so              VARCHAR(5)     NOT NULL DEFAULT 'vi'
            CONSTRAINT CK_cai_dat_dinhdangso CHECK (dinh_dang_so IN ('vi','en'))
    );
END

IF NOT EXISTS (SELECT 1 FROM cai_dat_he_thong WHERE cai_dat_id = 1)
BEGIN
    INSERT INTO cai_dat_he_thong (cai_dat_id) VALUES (1);
END
GO
```

- [ ] **Step 2: Chạy lại toàn bộ file SQL**

Chạy file `Database/QLBanMayTinh.sql` trong SSMS/Azure Data Studio (hoặc `sqlcmd`) như user vẫn làm. Expected: chạy xong không lỗi, `SELECT * FROM cai_dat_he_thong;` trả về đúng 1 dòng với `cai_dat_id = 1`.

- [ ] **Step 3: Tạo entity**

`BackEnd/src/main/java/com/example/backend/entity/CaiDatHeThong.java`:
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
@Table(name = "cai_dat_he_thong")
public class CaiDatHeThong {

    @Id
    @Column(name = "cai_dat_id")
    private Integer caiDatId;

    @Column(name = "ten_cua_hang", length = 200)
    private String tenCuaHang;

    @Column(name = "dia_chi", length = 300)
    private String diaChi;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ma_so_thue", length = 20)
    private String maSoThue;

    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(name = "nguong_ton_kho_mac_dinh")
    private Integer nguongTonKhoMacDinh;

    @Column(name = "ngon_ngu_mac_dinh", length = 5)
    private String ngonNguMacDinh;

    @Column(name = "dinh_dang_so", length = 5)
    private String dinhDangSo;
}
```

- [ ] **Step 4: Tạo repository**

`BackEnd/src/main/java/com/example/backend/repository/CaiDatHeThongRepository.java`:
```java
package com.example.backend.repository;

import com.example.backend.entity.CaiDatHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaiDatHeThongRepository extends JpaRepository<CaiDatHeThong, Integer> {
}
```

- [ ] **Step 5: Biên dịch**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add Database/QLBanMayTinh.sql BackEnd/src/main/java/com/example/backend/entity/CaiDatHeThong.java BackEnd/src/main/java/com/example/backend/repository/CaiDatHeThongRepository.java
git commit -m "feat: add cai_dat_he_thong singleton table + entity + repository"
```

---

### Task 2: Backend — DTO, Service, Controller cho cấu hình + ngưỡng tồn kho hàng loạt

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/CaiDatHeThongRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/CaiDatHeThongResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/CaiDatHeThongService.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/TonKhoRepository.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/CaiDatHeThongServiceTest.java`

**Interfaces:**
- Consumes: `CaiDatHeThong`, `CaiDatHeThongRepository` (Task 1); `TonKhoRepository` (đã có sẵn).
- Produces: `CaiDatHeThongService.get()`, `.update(CaiDatHeThongRequest)`, `.apDungNguongTonKhoChoTatCa(int)` — Task 3 (Controller) gọi thêm phương thức từ Task 3. `GET/PUT /api/cai-dat`, `POST /api/cai-dat/ap-dung-nguong-ton-kho` — Task 7/8 (frontend) gọi các endpoint này.

- [ ] **Step 1: Thêm bulk-update query vào `TonKhoRepository`**

Thêm vào cuối interface `BackEnd/src/main/java/com/example/backend/repository/TonKhoRepository.java` (trước dấu `}` đóng), và thêm import:

```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
```

```java

    // Ghi đè ngưỡng tồn kho tối thiểu lên TOÀN BỘ biến thể — dùng cho nút "Áp dụng cho tất
    // cả biến thể" ở trang Cài đặt. Trả về số dòng bị ảnh hưởng để FE hiện xác nhận.
    @Modifying
    @Transactional
    @Query("UPDATE TonKho t SET t.tonKhoToiThieu = :nguong")
    int capNhatNguongChoTatCa(@Param("nguong") int nguong);
```

- [ ] **Step 2: Tạo `CaiDatHeThongRequest`**

`BackEnd/src/main/java/com/example/backend/request/CaiDatHeThongRequest.java`:
```java
package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CaiDatHeThongRequest {
    @NotBlank(message = "Tên cửa hàng không được để trống")
    private String tenCuaHang;
    private String diaChi;
    private String soDienThoai;
    private String email;
    private String maSoThue;
    private String logoUrl;
    private String ngonNguMacDinh;
    private String dinhDangSo;
}
```

- [ ] **Step 3: Tạo `CaiDatHeThongResponse`**

`BackEnd/src/main/java/com/example/backend/response/CaiDatHeThongResponse.java`:
```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CaiDatHeThongResponse {
    private String tenCuaHang;
    private String diaChi;
    private String soDienThoai;
    private String email;
    private String maSoThue;
    private String logoUrl;
    private Integer nguongTonKhoMacDinh;
    private String ngonNguMacDinh;
    private String dinhDangSo;
}
```

- [ ] **Step 4: Tạo `CaiDatHeThongService`**

`BackEnd/src/main/java/com/example/backend/service/CaiDatHeThongService.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.CaiDatHeThong;
import com.example.backend.repository.CaiDatHeThongRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaiDatHeThongService {

    private static final int ID = 1;

    @Autowired
    private CaiDatHeThongRepository caiDatHeThongRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;

    private CaiDatHeThong getEntity() {
        return caiDatHeThongRepository.findById(ID)
                .orElseThrow(() -> new IllegalStateException("Chưa khởi tạo cài đặt hệ thống"));
    }

    public CaiDatHeThongResponse get() {
        CaiDatHeThong c = getEntity();
        return new CaiDatHeThongResponse(c.getTenCuaHang(), c.getDiaChi(), c.getSoDienThoai(),
                c.getEmail(), c.getMaSoThue(), c.getLogoUrl(), c.getNguongTonKhoMacDinh(),
                c.getNgonNguMacDinh(), c.getDinhDangSo());
    }

    public CaiDatHeThongResponse update(CaiDatHeThongRequest req) {
        CaiDatHeThong c = getEntity();
        c.setTenCuaHang(req.getTenCuaHang());
        c.setDiaChi(req.getDiaChi());
        c.setSoDienThoai(req.getSoDienThoai());
        c.setEmail(req.getEmail());
        c.setMaSoThue(req.getMaSoThue());
        c.setLogoUrl(req.getLogoUrl());
        c.setNgonNguMacDinh(req.getNgonNguMacDinh());
        c.setDinhDangSo(req.getDinhDangSo());
        caiDatHeThongRepository.save(c);
        return get();
    }

    // Lưu lại ngưỡng đã gõ (tiện gõ lại lần sau) VÀ ghi đè lên toàn bộ biến thể trong 1 hành
    // động — đây KHÔNG phải giá trị mặc định cho biến thể mới, chỉ là bulk-update thủ công.
    @Transactional
    public int apDungNguongTonKhoChoTatCa(int nguong) {
        CaiDatHeThong c = getEntity();
        c.setNguongTonKhoMacDinh(nguong);
        caiDatHeThongRepository.save(c);
        return tonKhoRepository.capNhatNguongChoTatCa(nguong);
    }
}
```

- [ ] **Step 5: Tạo `CaiDatController`**

`BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java`:
```java
package com.example.backend.controller;

import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import com.example.backend.service.CaiDatHeThongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// /api/cai-dat/** — KHÔNG nằm trong permitAll() của SecurityConfig, nên mọi endpoint ở đây
// tự động yêu cầu JWT hợp lệ qua .anyRequest().authenticated() (xem SecurityConfig.java).
@RestController
@RequestMapping("/api/cai-dat")
public class CaiDatController {

    @Autowired
    private CaiDatHeThongService caiDatHeThongService;

    @GetMapping
    public CaiDatHeThongResponse get() {
        return caiDatHeThongService.get();
    }

    @PutMapping
    public CaiDatHeThongResponse update(@Valid @RequestBody CaiDatHeThongRequest req) {
        return caiDatHeThongService.update(req);
    }

    @PostMapping("/ap-dung-nguong-ton-kho")
    public Map<String, Integer> apDungNguongTonKho(@RequestBody Map<String, Integer> body) {
        int nguong = body.getOrDefault("nguong", 0);
        int soBienTheDaCapNhat = caiDatHeThongService.apDungNguongTonKhoChoTatCa(nguong);
        return Map.of("soBienTheDaCapNhat", soBienTheDaCapNhat);
    }
}
```

- [ ] **Step 6: Viết test cho `apDungNguongTonKhoChoTatCa`**

Tạo `BackEnd/src/test/java/com/example/backend/service/CaiDatHeThongServiceTest.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.CaiDatHeThong;
import com.example.backend.repository.CaiDatHeThongRepository;
import com.example.backend.repository.TonKhoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaiDatHeThongServiceTest {

    @Mock private CaiDatHeThongRepository caiDatHeThongRepository;
    @Mock private TonKhoRepository tonKhoRepository;

    @InjectMocks
    private CaiDatHeThongService service;

    @Test
    void apDungNguongTonKhoChoTatCa_luuNguongMoiVaGoiBulkUpdate() {
        CaiDatHeThong c = new CaiDatHeThong();
        c.setCaiDatId(1);
        when(caiDatHeThongRepository.findById(1)).thenReturn(Optional.of(c));
        when(tonKhoRepository.capNhatNguongChoTatCa(10)).thenReturn(7);

        int result = service.apDungNguongTonKhoChoTatCa(10);

        assertThat(result).isEqualTo(7);
        assertThat(c.getNguongTonKhoMacDinh()).isEqualTo(10);
        verify(caiDatHeThongRepository).save(c);
        verify(tonKhoRepository).capNhatNguongChoTatCa(10);
    }
}
```

- [ ] **Step 7: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o test "-Dtest=CaiDatHeThongServiceTest"
```
Expected: `BUILD SUCCESS`, 1/1 test passed.

- [ ] **Step 8: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/repository/TonKhoRepository.java \
  BackEnd/src/main/java/com/example/backend/request/CaiDatHeThongRequest.java \
  BackEnd/src/main/java/com/example/backend/response/CaiDatHeThongResponse.java \
  BackEnd/src/main/java/com/example/backend/service/CaiDatHeThongService.java \
  BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java \
  BackEnd/src/test/java/com/example/backend/service/CaiDatHeThongServiceTest.java
git commit -m "feat: add cai-dat CRUD + bulk low-stock-threshold apply endpoint"
```

---

### Task 3: Backend — Đổi mật khẩu

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/DoiMatKhauRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/AuthService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: `TaiKhoanRepository.findByUsername` (đã có), `PasswordEncoder` bean (đã có ở `SecurityConfig`).
- Produces: `AuthService.doiMatKhau(String username, String matKhauCu, String matKhauMoi)` (ném `BadCredentialsException` nếu sai mật khẩu cũ) — `POST /api/cai-dat/doi-mat-khau` dùng ở Task 7 (frontend).

- [ ] **Step 1: Tạo `DoiMatKhauRequest`**

`BackEnd/src/main/java/com/example/backend/request/DoiMatKhauRequest.java`:
```java
package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class DoiMatKhauRequest {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String matKhauCu;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String matKhauMoi;
}
```

- [ ] **Step 2: Thêm `doiMatKhau()` vào `AuthService`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/service/AuthService.java` bằng:
```java
package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.LoginResponse;
import com.example.backend.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponse buildLoginResponse(String username) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        String role = tk.getChucVu().getMaChucVu(); // "admin", "nhan_vien", "quan_kho", "khach_hang"
        String token = jwtUtil.generateToken(tk.getUsername(), role);

        if (tk.getNhanVien() != null) {
            NhanVien nv = tk.getNhanVien();
            return new LoginResponse(nv.getNhanVienId(), nv.getHoTen(), tk.getUsername(),
                    nv.getSoDienThoai(), nv.getEmail(), role, token);
        }

        if (tk.getKhachHang() != null) {
            KhachHang kh = tk.getKhachHang();
            return new LoginResponse(kh.getKhachHangId(), kh.getHoTen(), tk.getUsername(),
                    kh.getSoDienThoai(), kh.getEmail(), role, token);
        }

        throw new UsernameNotFoundException("Tài khoản không liên kết với người dùng: " + username);
    }

    // Đổi mật khẩu tự phục vụ — dùng chung cho MỌI vai trò vì tất cả đều đăng nhập qua
    // cùng 1 bảng tai_khoan. Nhận username (không phải id số) vì JWT chỉ mang username
    // (xem JwtUtil) — controller lấy username từ SecurityContextHolder rồi truyền vào đây,
    // giữ hàm này test được mà không cần mock SecurityContextHolder.
    public void doiMatKhau(String username, String matKhauCu, String matKhauMoi) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        if (!passwordEncoder.matches(matKhauCu, tk.getMatKhauHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng");
        }
        tk.setMatKhauHash(passwordEncoder.encode(matKhauMoi));
        taiKhoanRepository.save(tk);
    }
}
```

- [ ] **Step 3: Wire endpoint vào `CaiDatController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.request.DoiMatKhauRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import com.example.backend.service.AuthService;
import com.example.backend.service.CaiDatHeThongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// /api/cai-dat/** — KHÔNG nằm trong permitAll() của SecurityConfig, nên mọi endpoint ở đây
// tự động yêu cầu JWT hợp lệ qua .anyRequest().authenticated() (xem SecurityConfig.java).
// doi-mat-khau đặt ở đây (không phải AuthController) vì /api/auth/** đang permitAll() toàn bộ.
@RestController
@RequestMapping("/api/cai-dat")
public class CaiDatController {

    @Autowired
    private CaiDatHeThongService caiDatHeThongService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public CaiDatHeThongResponse get() {
        return caiDatHeThongService.get();
    }

    @PutMapping
    public CaiDatHeThongResponse update(@Valid @RequestBody CaiDatHeThongRequest req) {
        return caiDatHeThongService.update(req);
    }

    @PostMapping("/ap-dung-nguong-ton-kho")
    public Map<String, Integer> apDungNguongTonKho(@RequestBody Map<String, Integer> body) {
        int nguong = body.getOrDefault("nguong", 0);
        int soBienTheDaCapNhat = caiDatHeThongService.apDungNguongTonKhoChoTatCa(nguong);
        return Map.of("soBienTheDaCapNhat", soBienTheDaCapNhat);
    }

    @PostMapping("/doi-mat-khau")
    public ResponseEntity<?> doiMatKhau(@Valid @RequestBody DoiMatKhauRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            authService.doiMatKhau(username, req.getMatKhauCu(), req.getMatKhauMoi());
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
```

- [ ] **Step 4: Viết test cho `AuthService.doiMatKhau`**

Tạo `BackEnd/src/test/java/com/example/backend/service/AuthServiceTest.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TaiKhoanRepository taiKhoanRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void doiMatKhau_saiMatKhauCu_nemLoiKhongLuu() {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin");
        tk.setMatKhauHash("hash-cu");
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(Optional.of(tk));
        when(passwordEncoder.matches("sai", "hash-cu")).thenReturn(false);

        assertThatThrownBy(() -> authService.doiMatKhau("admin", "sai", "moimoi123"))
                .isInstanceOf(BadCredentialsException.class);

        verify(taiKhoanRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doiMatKhau_dungMatKhauCu_luuHashMoi() {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin");
        tk.setMatKhauHash("hash-cu");
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(Optional.of(tk));
        when(passwordEncoder.matches("dung", "hash-cu")).thenReturn(true);
        when(passwordEncoder.encode("moimoi123")).thenReturn("hash-moi");

        authService.doiMatKhau("admin", "dung", "moimoi123");

        assertThat(tk.getMatKhauHash()).isEqualTo("hash-moi");
        verify(taiKhoanRepository).save(tk);
    }
}
```

- [ ] **Step 5: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o test "-Dtest=AuthServiceTest"
```
Expected: `BUILD SUCCESS`, 2/2 test passed.

- [ ] **Step 6: Biên dịch toàn bộ backend**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/request/DoiMatKhauRequest.java \
  BackEnd/src/main/java/com/example/backend/service/AuthService.java \
  BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java \
  BackEnd/src/test/java/com/example/backend/service/AuthServiceTest.java
git commit -m "feat: add self-service change-password (all roles, via CaiDatController)"
```

---

### Task 4: Frontend — Service, Settings store, `formatPrice` dùng chung

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/CaiDatService.js`
- Create: `FrontEnd/QLBanMayTinh/src/stores/settings.js`
- Create: `FrontEnd/QLBanMayTinh/src/utils/formatPrice.js`
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue`

**Interfaces:**
- Produces: `getCaiDat()`, `updateCaiDat(data)`, `apDungNguongTonKho(nguong)`, `doiMatKhau(matKhauCu, matKhauMoi)` (Task 7/8 dùng); `SettingsStore` reactive `{ tenCuaHang, diaChi, soDienThoai, email, maSoThue, logoUrl, nguongTonKhoMacDinh, ngonNguMacDinh, dinhDangSo, loaded }` + `loadSettings()` (Task 5, 7, 8 dùng); `formatPrice(v)` (Task 5 dùng để thay 9 file).

- [ ] **Step 1: Tạo `Service/CaiDatService.js`**

`FrontEnd/QLBanMayTinh/src/Service/CaiDatService.js`:
```js
import { get, put, post } from './api.js';

// post()/put() ở api.js trả Promise<Response> CHƯA parse (khác get()) — tự parse ở đây.
const parseOrThrow = async (res) => {
  if (!res.ok) throw new Error((await res.text().catch(() => '')) || `HTTP ${res.status}`);
  return res.json();
};

export const getCaiDat = () => get('/api/cai-dat');
export const updateCaiDat = (data) => put('/api/cai-dat', data).then(parseOrThrow);
export const apDungNguongTonKho = (nguong) =>
  post('/api/cai-dat/ap-dung-nguong-ton-kho', { nguong }).then(parseOrThrow);
export const doiMatKhau = (matKhauCu, matKhauMoi) =>
  post('/api/cai-dat/doi-mat-khau', { matKhauCu, matKhauMoi }).then(parseOrThrow);
```

- [ ] **Step 2: Kiểm tra cú pháp**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/Service/CaiDatService.js
```
Expected: không có output/lỗi.

- [ ] **Step 3: Tạo `stores/settings.js`**

`FrontEnd/QLBanMayTinh/src/stores/settings.js`:
```js
import { reactive } from "vue";
import { getCaiDat } from "../Service/CaiDatService.js";

// ── Settings Store — cấu hình hệ thống tải từ backend lúc khởi động ─────────
// Giá trị mặc định dưới đây dùng ngay trước khi tải xong (không chặn render);
// nếu tải lỗi (mất mạng...) thì giữ nguyên mặc định, không chặn app.
export const SettingsStore = reactive({
  tenCuaHang: "SAOPhone",
  diaChi: "",
  soDienThoai: "",
  email: "",
  maSoThue: "",
  logoUrl: "",
  nguongTonKhoMacDinh: 5,
  ngonNguMacDinh: "vi",
  dinhDangSo: "vi",
  loaded: false,
});

export const loadSettings = async () => {
  try {
    const data = await getCaiDat();
    Object.assign(SettingsStore, data);
  } catch {
    // giữ mặc định — không chặn app khi tải lỗi
  }
  SettingsStore.loaded = true;
};
```

- [ ] **Step 4: Kiểm tra cú pháp**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/stores/settings.js
```
Expected: không có output/lỗi.

- [ ] **Step 5: Tạo `utils/formatPrice.js`**

`FrontEnd/QLBanMayTinh/src/utils/formatPrice.js`:
```js
import { SettingsStore } from "../stores/settings.js";

// Định dạng tiền VNĐ dùng chung toàn app — đọc SettingsStore.dinhDangSo ('vi'|'en') để đổi
// kiểu nhóm chữ số (1.234.567 đ kiểu Việt / 1,234,567 đ kiểu Anh), đơn vị tiền tệ luôn là
// VNĐ (không đổi currency thật, chỉ đổi cách hiển thị số — xem spec "Ngoài phạm vi").
export const formatPrice = (v) =>
  new Intl.NumberFormat(SettingsStore.dinhDangSo === "en" ? "en-US" : "vi-VN",
    { style: "currency", currency: "VND" }).format(v ?? 0);
```

- [ ] **Step 6: Kiểm tra cú pháp**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/utils/formatPrice.js
```
Expected: không có output/lỗi.

- [ ] **Step 7: Gọi `loadSettings()` lúc app khởi động**

Trong `FrontEnd/QLBanMayTinh/src/App.vue`, sửa import (dòng 13):
```js
import { AuthStore, setSession, clearSession } from "./stores/index.js";
```
thành:
```js
import { AuthStore, setSession, clearSession } from "./stores/index.js";
import { loadSettings } from "./stores/settings.js";
```

Và sửa `onMounted` (dòng 544-550):
```js
onMounted(() => {
  window.addEventListener("hashchange", onHashChange);
  window.addEventListener("popstate", onPopState);
  loadCart(); // Khôi phục giỏ hàng đã lưu (theo tài khoản đang đăng nhập, hoặc khách vãng lai)
  fetchProducts();
  fetchApiCats();
});
```
thành:
```js
onMounted(() => {
  window.addEventListener("hashchange", onHashChange);
  window.addEventListener("popstate", onPopState);
  loadCart(); // Khôi phục giỏ hàng đã lưu (theo tài khoản đang đăng nhập, hoặc khách vãng lai)
  fetchProducts();
  fetchApiCats();
  loadSettings();
});
```

- [ ] **Step 8: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 9: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/CaiDatService.js \
  FrontEnd/QLBanMayTinh/src/stores/settings.js \
  FrontEnd/QLBanMayTinh/src/utils/formatPrice.js \
  FrontEnd/QLBanMayTinh/src/App.vue
git commit -m "feat(frontend): add CaiDatService, SettingsStore, shared formatPrice util"
```

---

### Task 5: Frontend — Gộp 9 nơi định nghĩa `formatPrice` về dùng chung

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/checkout/CheckoutModal.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/product/ProductDetail.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/product/ProductCard.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/cart/CartItem.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/cart/CartSummary.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/components/common/RevenueBarChart.vue`

**Interfaces:**
- Consumes: `formatPrice` từ `utils/formatPrice.js` (Task 4).

5 file dưới đây có xử lý riêng khi giá trị `null` (hiện "Liên hệ" hoặc "—") — GIỮ NGUYÊN hành vi null đó, chỉ đổi phần tính số sang dùng hàm chung (import với alias `formatPriceRaw` để không trùng tên biến cục bộ):

- [ ] **Step 1: `AdminPage.vue` — thêm import**

Thêm sau dòng `import { nowLocalIso } from "../utils/datetime.js";` (dòng 6):
```js
import { nowLocalIso } from "../utils/datetime.js";
import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";
```

- [ ] **Step 2: `AdminPage.vue` — thay định nghĩa `formatPrice`**

Thay:
```js
const formatPrice = (v) =>
  v == null
    ? "—"
    : new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
      }).format(v);
```
bằng:
```js
const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));
```

- [ ] **Step 3: `AccountPage.vue` — thêm import**

Thêm sau dòng `import { orderStatusLabel, orderStatusColor, orderStatusIcon } from "../utils/orderStatus.js";` (dòng 11):
```js
import { orderStatusLabel, orderStatusColor, orderStatusIcon } from "../utils/orderStatus.js";
import { formatPrice as formatPriceRaw } from "../utils/formatPrice.js";
```

- [ ] **Step 4: `AccountPage.vue` — thay định nghĩa `formatPrice`**

Thay:
```js
const formatPrice = (v) =>
  v == null ? "—" : new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v);
```
bằng:
```js
const formatPrice = (v) => (v == null ? "—" : formatPriceRaw(v));
```

- [ ] **Step 5: `App.vue` — thêm import**

Thêm sau dòng `import { AuthStore, setSession, clearSession } from "./stores/index.js"; import { loadSettings } from "./stores/settings.js";` (đã thêm ở Task 4 Step 7):
```js
import { loadSettings } from "./stores/settings.js";
import { formatPrice as formatPriceRaw } from "./utils/formatPrice.js";
```

- [ ] **Step 6: `App.vue` — thay định nghĩa `formatPrice`**

Thay:
```js
// Định dạng tiền tệ VND
const formatPrice = (value) => {
  if (value == null) return t("productDetail.contact");
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
};
```
bằng:
```js
// Định dạng tiền tệ VND
const formatPrice = (value) => (value == null ? t("productDetail.contact") : formatPriceRaw(value));
```

- [ ] **Step 7: `CheckoutModal.vue` — thêm import**

Thêm sau dòng `import { nowLocalIso } from '../../utils/datetime.js';` (dòng 341):
```js
import { nowLocalIso } from '../../utils/datetime.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
```

- [ ] **Step 8: `CheckoutModal.vue` — thay định nghĩa `formatPrice`**

Thay:
```js
const formatPrice = (value) => {
  if (value == null) return t('productDetail.contact');
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
};
```
bằng:
```js
const formatPrice = (value) => (value == null ? t('productDetail.contact') : formatPriceRaw(value));
```

- [ ] **Step 9: `ProductDetail.vue` — thêm import**

Thêm sau dòng `import { t } from '../../i18n/index.js';` (dòng 265):
```js
import { t } from '../../i18n/index.js';
import { formatPrice as formatPriceRaw } from '../../utils/formatPrice.js';
```

- [ ] **Step 10: `ProductDetail.vue` — thay định nghĩa `formatPrice`**

Thay:
```js
const formatPrice = (v) =>
  v == null ? t('productDetail.contact')
  : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v);
```
bằng:
```js
const formatPrice = (v) => (v == null ? t('productDetail.contact') : formatPriceRaw(v));
```

4 file còn lại KHÔNG có xử lý null riêng (đã tự nhiên fallback về 0 giống hàm chung) — import trực tiếp `formatPrice`, xoá hẳn định nghĩa cục bộ:

- [ ] **Step 11: `ProductCard.vue`**

Thay:
```js
import { t } from '../../i18n/index.js';

defineProps({
  // Sản phẩm từ API /api/san-pham/hien-thi
  product:      { type: Object,  required: true },
  // Số biến thể của sản phẩm này — > 1 thì hiện nhãn "từ giá"
  variantCount: { type: Number,  default: 0 },
});

// Emits: click (xem chi tiết), add-to-cart (thêm nhanh — cha tự quyết định có mở trang chi tiết trước hay không)
defineEmits(['click', 'add-to-cart']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
```
bằng:
```js
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';

defineProps({
  // Sản phẩm từ API /api/san-pham/hien-thi
  product:      { type: Object,  required: true },
  // Số biến thể của sản phẩm này — > 1 thì hiện nhãn "từ giá"
  variantCount: { type: Number,  default: 0 },
});

// Emits: click (xem chi tiết), add-to-cart (thêm nhanh — cha tự quyết định có mở trang chi tiết trước hay không)
defineEmits(['click', 'add-to-cart']);
```

- [ ] **Step 12: `CartItem.vue`**

Thay:
```js
import { t } from '../../i18n/index.js';

defineProps({ item: { type: Object, required: true } });
defineEmits(['increase', 'decrease']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
```
bằng:
```js
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';

defineProps({ item: { type: Object, required: true } });
defineEmits(['increase', 'decrease']);
```

- [ ] **Step 13: `CartSummary.vue`**

Thay:
```js
import { t } from '../../i18n/index.js';

defineProps({
  cartCount: { type: Number, default: 0 },
  cartTotal: { type: Number, default: 0 },
});
defineEmits(['checkout']);

const formatPrice = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v ?? 0);
```
bằng:
```js
import { t } from '../../i18n/index.js';
import { formatPrice } from '../../utils/formatPrice.js';

defineProps({
  cartCount: { type: Number, default: 0 },
  cartTotal: { type: Number, default: 0 },
});
defineEmits(['checkout']);
```

- [ ] **Step 14: `RevenueBarChart.vue`**

Thay:
```js
import { ref, computed, useId } from 'vue';
```
bằng:
```js
import { ref, computed, useId } from 'vue';
import { formatPrice } from '../../utils/formatPrice.js';
```

Và xoá dòng định nghĩa cục bộ:
```js
const formatPrice = (v) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
```

- [ ] **Step 15: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi (đặc biệt chú ý không còn cảnh báo `formatPrice` khai báo nhưng không dùng, hoặc trùng tên biến).

- [ ] **Step 16: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue \
  FrontEnd/QLBanMayTinh/src/pages/AccountPage.vue \
  FrontEnd/QLBanMayTinh/src/App.vue \
  FrontEnd/QLBanMayTinh/src/components/checkout/CheckoutModal.vue \
  FrontEnd/QLBanMayTinh/src/components/product/ProductDetail.vue \
  FrontEnd/QLBanMayTinh/src/components/product/ProductCard.vue \
  FrontEnd/QLBanMayTinh/src/components/cart/CartItem.vue \
  FrontEnd/QLBanMayTinh/src/components/cart/CartSummary.vue \
  FrontEnd/QLBanMayTinh/src/components/common/RevenueBarChart.vue
git commit -m "refactor(frontend): consolidate 9 duplicated formatPrice definitions into shared util"
```

---

### Task 6: Frontend — i18n cho 4 card mới (5 ngôn ngữ)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: các key mới dưới `admin.settings.*` — Task 7, 8 dùng.

- [ ] **Step 1: Thêm key vào khối `settings` của cả 5 file**

Khối `settings: { ... }` hiện có (vi.js dòng 979-987) — thêm các key sau ngay trước dấu `},` đóng khối, KHÔNG xoá key cũ (`systemInfo`, `systemName`, `version`, `backendApi`, `database`, `status`, `active`).

`vi.js`:
```js
      changePasswordTitle: "Đổi mật khẩu",
      currentPassword: "Mật khẩu hiện tại",
      newPassword: "Mật khẩu mới",
      confirmNewPassword: "Xác nhận mật khẩu mới",
      changePasswordButton: "Đổi mật khẩu",
      passwordMismatch: "Mật khẩu xác nhận không khớp",
      passwordChanged: "Đổi mật khẩu thành công",
      storeInfoTitle: "Thông tin cửa hàng",
      storeName: "Tên cửa hàng",
      storeAddress: "Địa chỉ",
      storePhone: "Số điện thoại",
      storeEmail: "Email",
      storeTaxCode: "Mã số thuế",
      storeLogo: "Logo",
      saveButton: "Lưu",
      saved: "Đã lưu",
      lowStockThresholdTitle: "Ngưỡng cảnh báo tồn kho",
      lowStockThresholdLabel: "Số lượng tồn tối thiểu",
      applyToAllButton: "Áp dụng cho tất cả biến thể",
      applyToAllConfirm: "Áp dụng ngưỡng {nguong} cho toàn bộ {count} biến thể đang có trong kho? Hành động này không thể hoàn tác.",
      applyToAllDone: "Đã cập nhật {count} biến thể",
      appearanceTitle: "Giao diện & ngôn ngữ",
      themeLabel: "Giao diện",
      languageLabel: "Ngôn ngữ hiện tại",
      defaultLanguageLabel: "Ngôn ngữ mặc định khi đăng nhập",
      numberFormatLabel: "Định dạng số",
      numberFormatVi: "Kiểu Việt Nam (1.234.567 ₫)",
      numberFormatEn: "Kiểu Anh (1,234,567 ₫)",
```

`en.js`:
```js
      changePasswordTitle: "Change password",
      currentPassword: "Current password",
      newPassword: "New password",
      confirmNewPassword: "Confirm new password",
      changePasswordButton: "Change password",
      passwordMismatch: "Passwords do not match",
      passwordChanged: "Password changed successfully",
      storeInfoTitle: "Store information",
      storeName: "Store name",
      storeAddress: "Address",
      storePhone: "Phone",
      storeEmail: "Email",
      storeTaxCode: "Tax code",
      storeLogo: "Logo",
      saveButton: "Save",
      saved: "Saved",
      lowStockThresholdTitle: "Low-stock alert threshold",
      lowStockThresholdLabel: "Minimum stock quantity",
      applyToAllButton: "Apply to all variants",
      applyToAllConfirm: "Apply threshold {nguong} to all {count} variants currently in stock? This cannot be undone.",
      applyToAllDone: "Updated {count} variants",
      appearanceTitle: "Appearance & language",
      themeLabel: "Theme",
      languageLabel: "Current language",
      defaultLanguageLabel: "Default language on login",
      numberFormatLabel: "Number format",
      numberFormatVi: "Vietnamese style (1,234,567 ₫)",
      numberFormatEn: "English style (1,234,567 ₫)",
```

`zh.js`:
```js
      changePasswordTitle: "修改密码",
      currentPassword: "当前密码",
      newPassword: "新密码",
      confirmNewPassword: "确认新密码",
      changePasswordButton: "修改密码",
      passwordMismatch: "两次输入的密码不一致",
      passwordChanged: "密码修改成功",
      storeInfoTitle: "店铺信息",
      storeName: "店铺名称",
      storeAddress: "地址",
      storePhone: "电话",
      storeEmail: "邮箱",
      storeTaxCode: "税号",
      storeLogo: "店铺Logo",
      saveButton: "保存",
      saved: "已保存",
      lowStockThresholdTitle: "低库存预警阈值",
      lowStockThresholdLabel: "最低库存数量",
      applyToAllButton: "应用到所有变体",
      applyToAllConfirm: "将阈值 {nguong} 应用到当前全部 {count} 个库存变体？此操作无法撤销。",
      applyToAllDone: "已更新 {count} 个变体",
      appearanceTitle: "外观与语言",
      themeLabel: "主题",
      languageLabel: "当前语言",
      defaultLanguageLabel: "登录默认语言",
      numberFormatLabel: "数字格式",
      numberFormatVi: "越南式（1.234.567 ₫）",
      numberFormatEn: "英文式（1,234,567 ₫）",
```

`ko.js`:
```js
      changePasswordTitle: "비밀번호 변경",
      currentPassword: "현재 비밀번호",
      newPassword: "새 비밀번호",
      confirmNewPassword: "새 비밀번호 확인",
      changePasswordButton: "비밀번호 변경",
      passwordMismatch: "비밀번호가 일치하지 않습니다",
      passwordChanged: "비밀번호가 변경되었습니다",
      storeInfoTitle: "매장 정보",
      storeName: "매장명",
      storeAddress: "주소",
      storePhone: "전화번호",
      storeEmail: "이메일",
      storeTaxCode: "사업자 등록번호",
      storeLogo: "로고",
      saveButton: "저장",
      saved: "저장되었습니다",
      lowStockThresholdTitle: "재고 부족 알림 기준",
      lowStockThresholdLabel: "최소 재고 수량",
      applyToAllButton: "전체 옵션에 적용",
      applyToAllConfirm: "기준값 {nguong}을(를) 현재 재고 전체 {count}개 옵션에 적용할까요? 되돌릴 수 없습니다.",
      applyToAllDone: "{count}개 옵션이 업데이트되었습니다",
      appearanceTitle: "화면 & 언어",
      themeLabel: "테마",
      languageLabel: "현재 언어",
      defaultLanguageLabel: "로그인 시 기본 언어",
      numberFormatLabel: "숫자 형식",
      numberFormatVi: "베트남식 (1.234.567 ₫)",
      numberFormatEn: "영어식 (1,234,567 ₫)",
```

`ja.js`:
```js
      changePasswordTitle: "パスワード変更",
      currentPassword: "現在のパスワード",
      newPassword: "新しいパスワード",
      confirmNewPassword: "新しいパスワード（確認）",
      changePasswordButton: "パスワードを変更",
      passwordMismatch: "パスワードが一致しません",
      passwordChanged: "パスワードを変更しました",
      storeInfoTitle: "店舗情報",
      storeName: "店舗名",
      storeAddress: "住所",
      storePhone: "電話番号",
      storeEmail: "メール",
      storeTaxCode: "税番号",
      storeLogo: "ロゴ",
      saveButton: "保存",
      saved: "保存しました",
      lowStockThresholdTitle: "在庫アラートしきい値",
      lowStockThresholdLabel: "最低在庫数",
      applyToAllButton: "全バリエーションに適用",
      applyToAllConfirm: "しきい値 {nguong} を現在庫の全 {count} バリエーションに適用しますか？元に戻せません。",
      applyToAllDone: "{count} 件のバリエーションを更新しました",
      appearanceTitle: "外観・言語",
      themeLabel: "テーマ",
      languageLabel: "現在の言語",
      defaultLanguageLabel: "ログイン時のデフォルト言語",
      numberFormatLabel: "数値の書式",
      numberFormatVi: "ベトナム式（1.234.567 ₫）",
      numberFormatEn: "英語式（1,234,567 ₫）",
```

- [ ] **Step 2: Kiểm tra cú pháp cả 5 file**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/i18n/locales/vi.js; node --check src/i18n/locales/en.js; node --check src/i18n/locales/zh.js; node --check src/i18n/locales/ko.js; node --check src/i18n/locales/ja.js
```
Expected: không có output/lỗi ở cả 5 lệnh.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "i18n: add settings-page translation keys (vi, en, zh, ko, ja)"
```

---

### Task 7: Frontend — UI card "Đổi mật khẩu" + "Thông tin cửa hàng"

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `CaiDatService.doiMatKhau`, `.updateCaiDat` (Task 4); `SettingsStore` (Task 4); i18n keys (Task 6); `authHeaders` (đã import sẵn, dùng lại đúng flow upload ảnh ở `AdminPage.vue:1339`).

- [ ] **Step 1: Thêm state + hàm xử lý, ngay trước section Cài đặt hiện có**

Tìm đoạn (đã xác định qua Task 4/5, các dòng xung quanh có thể lệch nhẹ do các task trước — tìm bằng nội dung, chèn state ngay TRƯỚC dòng `<!-- ── Cai dat ── -->`):

```js

// ── Cài đặt: đổi mật khẩu ──────────────────────────────────────────────────────
const cdMatKhauCu = ref('');
const cdMatKhauMoi = ref('');
const cdMatKhauXacNhan = ref('');
const cdMatKhauError = ref('');
const cdMatKhauSuccess = ref('');
const cdMatKhauLoading = ref(false);

const doiMatKhauSubmit = async () => {
  cdMatKhauError.value = '';
  cdMatKhauSuccess.value = '';
  if (cdMatKhauMoi.value !== cdMatKhauXacNhan.value) {
    cdMatKhauError.value = t('admin.settings.passwordMismatch');
    return;
  }
  cdMatKhauLoading.value = true;
  try {
    await CaiDatService.doiMatKhau(cdMatKhauCu.value, cdMatKhauMoi.value);
    cdMatKhauSuccess.value = t('admin.settings.passwordChanged');
    cdMatKhauCu.value = '';
    cdMatKhauMoi.value = '';
    cdMatKhauXacNhan.value = '';
  } catch (e) {
    cdMatKhauError.value = e.message || String(e);
  } finally {
    cdMatKhauLoading.value = false;
  }
};

// ── Cài đặt: thông tin cửa hàng ─────────────────────────────────────────────────
const cdForm = reactive({
  tenCuaHang: '', diaChi: '', soDienThoai: '', email: '', maSoThue: '', logoUrl: '',
});
const cdLogoPreview = ref('');
const cdLogoFilePending = ref(null);
const cdStoreSaving = ref(false);
const cdStoreSaved = ref(false);

watch(() => SettingsStore.loaded, (loaded) => {
  if (!loaded) return;
  cdForm.tenCuaHang = SettingsStore.tenCuaHang;
  cdForm.diaChi = SettingsStore.diaChi;
  cdForm.soDienThoai = SettingsStore.soDienThoai;
  cdForm.email = SettingsStore.email;
  cdForm.maSoThue = SettingsStore.maSoThue;
  cdForm.logoUrl = SettingsStore.logoUrl;
  cdLogoPreview.value = SettingsStore.logoUrl || '';
}, { immediate: true });

const handleLogoFile = (e) => {
  const file = e.target.files[0];
  if (!file) return;
  cdLogoFilePending.value = file;
  cdLogoPreview.value = URL.createObjectURL(file);
};

const saveStoreInfo = async () => {
  cdStoreSaving.value = true;
  cdStoreSaved.value = false;
  try {
    if (cdLogoFilePending.value) {
      const fd = new FormData();
      fd.append('file', cdLogoFilePending.value);
      const upRes = await fetch('/api/upload/image', { method: 'POST', headers: authHeaders(), body: fd });
      if (upRes.ok) {
        const upData = await upRes.json();
        cdForm.logoUrl = upData.url;
      }
    }
    const updated = await CaiDatService.updateCaiDat({
      tenCuaHang: cdForm.tenCuaHang, diaChi: cdForm.diaChi, soDienThoai: cdForm.soDienThoai,
      email: cdForm.email, maSoThue: cdForm.maSoThue, logoUrl: cdForm.logoUrl,
      ngonNguMacDinh: SettingsStore.ngonNguMacDinh, dinhDangSo: SettingsStore.dinhDangSo,
    });
    Object.assign(SettingsStore, updated);
    cdLogoFilePending.value = null;
    cdStoreSaved.value = true;
  } finally {
    cdStoreSaving.value = false;
  }
};
```

Thêm 2 import cần dùng ở đầu file (`<script setup>`), ngay sau dòng `import RevenueBarChart from "../components/common/RevenueBarChart.vue";` (dòng 24):
```js
import RevenueBarChart from "../components/common/RevenueBarChart.vue";
import * as CaiDatService from "../Service/CaiDatService.js";
import { SettingsStore } from "../stores/settings.js";
```

- [ ] **Step 2: Thay template section "Cài đặt"**

Thay toàn bộ nội dung hiện có:
```html
        <!-- ── Cai dat ── -->
        <section v-show="currentPage === 'settings'">
          <div class="card border-secondary" style="background:var(--bg-hover); max-width:520px;">
            <div class="card-body">
              <div class="fw-bold mb-3">⚙️ {{ t('admin.settings.systemInfo') }}</div>
              <div v-for="row in [
                {label:t('admin.settings.systemName'), value:'SAOPhone Admin'},
                {label:t('admin.settings.version'), value:'1.0.0'},
                {label:t('admin.settings.backendApi'), value:'http://localhost:8080'},
                {label:t('admin.settings.database'), value:'SQL Server — QLBanMayTinh'},
              ]" :key="row.label"
                   class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                <span class="text-secondary">{{ row.label }}</span>
                <span>{{ row.value }}</span>
              </div>
              <div class="d-flex justify-content-between align-items-center py-2 small">
                <span class="text-secondary">{{ t('admin.settings.status') }}</span>
                <span class="badge bg-success">{{ t('admin.settings.active') }}</span>
              </div>
            </div>
          </div>
        </section>
```
bằng:
```html
        <!-- ── Cai dat ── -->
        <section v-show="currentPage === 'settings'">
          <div class="row g-3">
            <!-- Đổi mật khẩu -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">🔑 {{ t('admin.settings.changePasswordTitle') }}</div>
                  <div class="mb-2">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
                    <input type="password" v-model="cdMatKhauCu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <div class="mb-2">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
                    <input type="password" v-model="cdMatKhauMoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <div class="mb-3">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
                    <input type="password" v-model="cdMatKhauXacNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <div v-if="cdMatKhauError" class="text-danger small mb-2">{{ cdMatKhauError }}</div>
                  <div v-if="cdMatKhauSuccess" class="text-success small mb-2">{{ cdMatKhauSuccess }}</div>
                  <button class="btn btn-warning btn-sm" :disabled="cdMatKhauLoading || !cdMatKhauCu || !cdMatKhauMoi" @click="doiMatKhauSubmit">
                    {{ t('admin.settings.changePasswordButton') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Thông tin cửa hàng -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">🏪 {{ t('admin.settings.storeInfoTitle') }}</div>
                  <div class="d-flex align-items-center gap-3 mb-3">
                    <label class="d-flex flex-column align-items-center justify-content-center rounded-3 border border-secondary text-secondary" style="width:88px;height:70px;cursor:pointer;flex-shrink:0;overflow:hidden;background:var(--bg-card-inset);">
                      <img v-if="cdLogoPreview" :src="cdLogoPreview" style="width:88px;height:70px;object-fit:contain;" />
                      <span v-else style="font-size:1.3rem;">🖼️</span>
                      <input type="file" accept="image/*" class="d-none" @change="handleLogoFile" />
                    </label>
                    <span class="text-secondary small">{{ t('admin.settings.storeLogo') }}</span>
                  </div>
                  <div class="row g-2 mb-3">
                    <div class="col-12">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeName') }}</label>
                      <input v-model="cdForm.tenCuaHang" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-12">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeAddress') }}</label>
                      <input v-model="cdForm.diaChi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-6">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storePhone') }}</label>
                      <input v-model="cdForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-6">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeEmail') }}</label>
                      <input v-model="cdForm.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                    <div class="col-12">
                      <label class="form-label small text-secondary mb-1">{{ t('admin.settings.storeTaxCode') }}</label>
                      <input v-model="cdForm.maSoThue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                    </div>
                  </div>
                  <div v-if="cdStoreSaved" class="text-success small mb-2">{{ t('admin.settings.saved') }}</div>
                  <button class="btn btn-warning btn-sm" :disabled="cdStoreSaving" @click="saveStoreInfo">
                    {{ t('admin.settings.saveButton') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>
```

(2 card mới còn lại — "Ngưỡng cảnh báo tồn kho" và "Giao diện & ngôn ngữ" — thêm ở Task 8, chèn tiếp vào cùng `<div class="row g-3">` này. Card "Thông tin hệ thống" cũ giữ nguyên, thêm lại ở Task 8 Step 4.)

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add change-password and store-info cards to Settings page"
```

---

### Task 8: Frontend — Card "Ngưỡng tồn kho" + "Giao diện & ngôn ngữ", ngôn ngữ mặc định khi đăng nhập

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/index.js`
- Modify: `FrontEnd/QLBanMayTinh/src/App.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `CaiDatService.apDungNguongTonKho` (Task 4); `SettingsStore` (Task 4); `askConfirm` (đã có sẵn, `stores/confirm.js`); `ThemeStore`/`toggleTheme` (đã có sẵn, đang import ở `AdminPage.vue:31`); `I18nStore`/`LOCALES`/`setLocale` (đã có ở `i18n/index.js`, chưa import vào `AdminPage.vue`).
- Produces: `applySystemDefaultLocale(code)` (export mới ở `i18n/index.js`) — Task 8 Step 2 gọi ngay trong task này, không có task nào khác dùng.

- [ ] **Step 1: Thêm `applySystemDefaultLocale` vào `i18n/index.js`**

Thêm vào cuối `FrontEnd/QLBanMayTinh/src/i18n/index.js`:
```js

// Áp dụng ngôn ngữ mặc định hệ thống (Cài đặt) CHỈ khi người dùng chưa từng tự chọn ngôn
// ngữ ở trình duyệt này (chưa có key trong localStorage) — không ghi đè lựa chọn đã có.
export const applySystemDefaultLocale = (code) => {
  if (!localStorage.getItem(STORAGE_KEY) && MESSAGES[code]) {
    setLocale(code);
  }
};
```

- [ ] **Step 2: Gọi sau khi `loadSettings()` tải xong, trong `App.vue`**

Sửa import (đã thêm `loadSettings` ở Task 4 Step 7):
```js
import { loadSettings } from "./stores/settings.js";
```
thành (gộp luôn `SettingsStore` cần dùng ở `onMounted` bên dưới):
```js
import { loadSettings, SettingsStore } from "./stores/settings.js";
import { applySystemDefaultLocale } from "./i18n/index.js";
```

Sửa `onMounted` (đã thêm `loadSettings();` ở Task 4 Step 7):
```js
onMounted(() => {
  window.addEventListener("hashchange", onHashChange);
  window.addEventListener("popstate", onPopState);
  loadCart(); // Khôi phục giỏ hàng đã lưu (theo tài khoản đang đăng nhập, hoặc khách vãng lai)
  fetchProducts();
  fetchApiCats();
  loadSettings();
});
```
thành:
```js
onMounted(async () => {
  window.addEventListener("hashchange", onHashChange);
  window.addEventListener("popstate", onPopState);
  loadCart(); // Khôi phục giỏ hàng đã lưu (theo tài khoản đang đăng nhập, hoặc khách vãng lai)
  fetchProducts();
  fetchApiCats();
  await loadSettings();
  applySystemDefaultLocale(SettingsStore.ngonNguMacDinh);
});
```

- [ ] **Step 3: Thêm state cho ngưỡng tồn kho + import ThemeStore/I18n vào `AdminPage.vue`**

Thêm import (ngay sau dòng đã thêm ở Task 7 Step 1: `import { SettingsStore } from "../stores/settings.js";`):
```js
import { SettingsStore } from "../stores/settings.js";
import { I18nStore, LOCALES, setLocale } from "../i18n/index.js";
```

Thêm state, ngay sau khối `saveStoreInfo` đã thêm ở Task 7 Step 1:
```js

// ── Cài đặt: ngưỡng cảnh báo tồn kho ─────────────────────────────────────────────
const cdNguongTonKho = ref(5);
watch(() => SettingsStore.loaded, (loaded) => {
  if (loaded) cdNguongTonKho.value = SettingsStore.nguongTonKhoMacDinh;
}, { immediate: true });
const cdApplyingThreshold = ref(false);

const apDungNguongTonKhoSubmit = async () => {
  const count = inventory.value.length;
  const ok = await askConfirm(t('admin.settings.applyToAllConfirm', { nguong: cdNguongTonKho.value, count }));
  if (!ok) return;
  cdApplyingThreshold.value = true;
  try {
    const res = await CaiDatService.apDungNguongTonKho(cdNguongTonKho.value);
    SettingsStore.nguongTonKhoMacDinh = cdNguongTonKho.value;
    // Không có loader tồn-kho-riêng-lẻ trong file này — inventory chỉ được tải lại cùng
    // 1 lượt với products/orders/customers/promotions qua fetchAll() (AdminPage.vue:1001-1019),
    // dùng lại đúng hàm đó để bảng/cảnh báo hết hàng cập nhật ngay.
    await fetchAll();
    // showToast(msg, type) đã có sẵn (AdminPage.vue:35-45), dùng lại thay vì alert() —
    // toàn bộ thông báo thành công/lỗi khác trong trang admin đều qua đường này.
    showToast(t('admin.settings.applyToAllDone', { count: res.soBienTheDaCapNhat }), 'success');
  } finally {
    cdApplyingThreshold.value = false;
  }
};

// Lưu ngôn ngữ mặc định / định dạng số ngay khi đổi dropdown — đọc field từ SettingsStore
// (không phải cdForm) vì 2 lý do: (1) cdForm chỉ được điền sau khi SettingsStore.loaded,
// đổi dropdown trước lúc đó sẽ gửi chuỗi rỗng đè lên dữ liệu thật; (2) đổi ngôn ngữ không
// nên vô tình lưu luôn các trường thông tin cửa hàng đang gõ dở nhưng chưa bấm Lưu.
const saveAppearancePrefs = async () => {
  const updated = await CaiDatService.updateCaiDat({
    tenCuaHang: SettingsStore.tenCuaHang, diaChi: SettingsStore.diaChi,
    soDienThoai: SettingsStore.soDienThoai, email: SettingsStore.email,
    maSoThue: SettingsStore.maSoThue, logoUrl: SettingsStore.logoUrl,
    ngonNguMacDinh: SettingsStore.ngonNguMacDinh, dinhDangSo: SettingsStore.dinhDangSo,
  });
  Object.assign(SettingsStore, updated);
};
```

- [ ] **Step 4: Thêm template 2 card còn lại, sau card "Thông tin cửa hàng" (chèn tiếp trong cùng `<div class="row g-3">` ở Task 7 Step 2), và card "Thông tin hệ thống" cũ**

Thêm ngay sau khối đóng `</div>` của card "Thông tin cửa hàng" (trước dòng `</div>` đóng `row g-3`, xem Task 7 Step 2):

```html
            <!-- Ngưỡng cảnh báo tồn kho -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">📦 {{ t('admin.settings.lowStockThresholdTitle') }}</div>
                  <div class="mb-3">
                    <label class="form-label small text-secondary mb-1">{{ t('admin.settings.lowStockThresholdLabel') }}</label>
                    <input type="number" min="0" v-model.number="cdNguongTonKho" class="form-control form-control-sm" style="width:120px;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
                  </div>
                  <button class="btn btn-outline-warning btn-sm" :disabled="cdApplyingThreshold" @click="apDungNguongTonKhoSubmit">
                    {{ t('admin.settings.applyToAllButton') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Giao diện & ngôn ngữ -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">🎨 {{ t('admin.settings.appearanceTitle') }}</div>
                  <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ t('admin.settings.themeLabel') }}</span>
                    <button type="button" class="btn btn-sm btn-outline-secondary" @click="toggleTheme">
                      {{ ThemeStore.mode === 'dark' ? '🌙' : '☀️' }}
                    </button>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ t('admin.settings.languageLabel') }}</span>
                    <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                            :value="I18nStore.locale" @change="setLocale($event.target.value)">
                      <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code">{{ loc.flag }} {{ loc.label }}</option>
                    </select>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ t('admin.settings.defaultLanguageLabel') }}</span>
                    <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                            v-model="SettingsStore.ngonNguMacDinh"
                            @change="saveAppearancePrefs">
                      <option v-for="loc in LOCALES" :key="loc.code" :value="loc.code">{{ loc.flag }} {{ loc.label }}</option>
                    </select>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 small">
                    <span class="text-secondary">{{ t('admin.settings.numberFormatLabel') }}</span>
                    <select class="form-select form-select-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);"
                            v-model="SettingsStore.dinhDangSo"
                            @change="saveAppearancePrefs">
                      <option value="vi">{{ t('admin.settings.numberFormatVi') }}</option>
                      <option value="en">{{ t('admin.settings.numberFormatEn') }}</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>

            <!-- Thông tin hệ thống (giữ nguyên, tĩnh) -->
            <div class="col-12 col-xl-6">
              <div class="card border-secondary h-100" style="background:var(--bg-hover);">
                <div class="card-body">
                  <div class="fw-bold mb-3">⚙️ {{ t('admin.settings.systemInfo') }}</div>
                  <div v-for="row in [
                    {label:t('admin.settings.systemName'), value:'SAOPhone Admin'},
                    {label:t('admin.settings.version'), value:'1.0.0'},
                    {label:t('admin.settings.backendApi'), value:'http://localhost:8080'},
                    {label:t('admin.settings.database'), value:'SQL Server — QLBanMayTinh'},
                  ]" :key="row.label"
                       class="d-flex justify-content-between align-items-center py-2 border-bottom border-secondary small">
                    <span class="text-secondary">{{ row.label }}</span>
                    <span>{{ row.value }}</span>
                  </div>
                  <div class="d-flex justify-content-between align-items-center py-2 small">
                    <span class="text-secondary">{{ t('admin.settings.status') }}</span>
                    <span class="badge bg-success">{{ t('admin.settings.active') }}</span>
                  </div>
                </div>
              </div>
            </div>
```

- [ ] **Step 5: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/index.js FrontEnd/QLBanMayTinh/src/App.vue FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add low-stock-threshold and appearance/language cards to Settings"
```

---

### Task 9: Kiểm thử thủ công end-to-end

**Files:** không có file thay đổi — chỉ chạy và quan sát.

- [ ] **Step 1: Chạy backend + frontend**

```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd spring-boot:run
```
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev
```

- [ ] **Step 2: Kịch bản chính**

1. Vào trang Cài đặt (admin) — xác nhận thấy đủ 5 card: Đổi mật khẩu, Thông tin cửa hàng, Ngưỡng cảnh báo tồn kho, Giao diện & ngôn ngữ, Thông tin hệ thống.
2. **Đổi mật khẩu**: nhập sai mật khẩu hiện tại → thấy lỗi "Mật khẩu hiện tại không đúng", không bị đăng xuất. Nhập đúng mật khẩu hiện tại + mật khẩu mới hợp lệ → thấy thông báo thành công. Đăng xuất, đăng nhập lại bằng mật khẩu MỚI → vào được. (Đổi lại về mật khẩu cũ sau khi test xong nếu cần dùng tiếp.)
3. **Thông tin cửa hàng**: sửa tên cửa hàng, chọn 1 ảnh logo → Lưu → F5 lại trang → xác nhận tên và logo vẫn còn (đã lưu DB, không phải chỉ state tạm).
4. **Ngưỡng tồn kho**: nhập 1 số (vd 8) → bấm "Áp dụng cho tất cả biến thể" → xác nhận hộp thoại xác nhận hiện đúng số biến thể → đồng ý → thấy thông báo đã cập nhật N biến thể. Vào tab Kho hàng → xác nhận cột "tồn kho tối thiểu" của TẤT CẢ biến thể đã đổi thành 8.
5. **Giao diện & ngôn ngữ**: bấm nút sáng/tối → giao diện đổi ngay (giống nút ở topbar). Đổi "Ngôn ngữ hiện tại" → toàn bộ UI đổi ngôn ngữ ngay. Đổi "Định dạng số" sang kiểu Anh → xác nhận các số tiền trên trang Báo cáo/Dashboard đổi từ dấu chấm sang dấu phẩy phân cách hàng nghìn.
6. **Ngôn ngữ mặc định khi đăng nhập** (test riêng, cẩn thận): mở cửa sổ ẩn danh (chưa có `localStorage`), vào thẳng app → xác nhận ngôn ngữ hiển thị đúng theo giá trị đã đặt ở "Ngôn ngữ mặc định khi đăng nhập" (không phải luôn là tiếng Việt).
7. Vào lại trang bán hàng (POS)/trang khách hàng/giỏ hàng — xác nhận giá tiền vẫn hiển thị đúng như trước (không có nơi nào hiện `NaN`, `undefined`, hay lỗi định dạng do gộp `formatPrice`).

- [ ] **Step 3: Dừng server**

`Ctrl+C` ở cả 2 terminal.

---

## Tự rà soát (self-review)

**1. Phủ đủ spec:**
- Đổi mật khẩu (bắt buộc mật khẩu cũ) → Task 3, 7. ✅
- Thông tin cửa hàng (tên/địa chỉ/SĐT/email/MST/logo) → Task 2, 7. ✅
- Ngưỡng tồn kho (áp dụng hàng loạt, không phải mặc định cho biến thể mới) → Task 2, 8. ✅
- Giao diện & ngôn ngữ (theme, ngôn ngữ hiện tại, ngôn ngữ mặc định, định dạng số) → Task 8. ✅
- Gộp 9 file `formatPrice` → Task 5. ✅
- Bảo mật (`doi-mat-khau` không nằm dưới `/api/auth/**`) → Task 3. ✅
- Idempotent SQL migration → Task 1. ✅

**2. Không còn chỗ nào ghi "TODO"/"tương tự task N"/thiếu code** — đã rà lại toàn bộ; chỗ ban đầu định đoán tên hàm tải lại tồn kho đã được xác minh trực tiếp trong `AdminPage.vue:1001-1019` (không có loader riêng — dùng `fetchAll()`) và sửa lại trong plan, không còn phần đoán mò nào.

**3. Nhất quán tên hàm/field xuyên suốt task:**
- `CaiDatHeThongService.get()/update()/apDungNguongTonKhoChoTatCa()` (Task 2) ↔ `CaiDatController` gọi đúng 3 hàm này (Task 2, 3). ✅
- `CaiDatService.getCaiDat/updateCaiDat/apDungNguongTonKho/doiMatKhau` (Task 4) ↔ dùng đúng tên này ở Task 7, 8. ✅
- `SettingsStore.{tenCuaHang,diaChi,soDienThoai,email,maSoThue,logoUrl,nguongTonKhoMacDinh,ngonNguMacDinh,dinhDangSo,loaded}` (Task 4) ↔ dùng đúng field xuyên suốt Task 7, 8. ✅
- `formatPrice(v)` từ `utils/formatPrice.js` (Task 4) ↔ import đúng chữ ký ở cả 9 file Task 5. ✅

## Ngoài phạm vi (nhắc lại từ spec)

- Không đổi đơn vị tiền tệ thật (luôn VNĐ) — "định dạng số" chỉ đổi cách nhóm chữ số.
- Không thêm 2FA/quản lý phiên đăng nhập.
- Không tự động dùng ngưỡng tồn kho làm mặc định khi tạo biến thể mới.
- Không thêm xuất/nhập cấu hình (backup/restore settings).

