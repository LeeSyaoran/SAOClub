# Menu hồ sơ admin (dropdown ở chân sidebar) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Biến ô "Quản trị viên" (avatar+tên+role) ở chân sidebar admin từ `<div>` tĩnh thành nút mở dropdown 3 mục: Chỉnh sửa hồ sơ, Đổi mật khẩu, Cài đặt — kèm 2 modal thao tác tại chỗ, không cần rời trang.

**Architecture:** 1 endpoint backend mới `PUT /api/cai-dat/ho-so` (tự phục vụ, mọi vai trò, đặt cạnh `doiMatKhau` đã có trong `CaiDatController` vì cùng lý do né `/api/auth/**` đang `permitAll()`), chỉ sửa đúng `hoTen`/`soDienThoai`/`email` trên `NhanVien` của người gọi — không dùng lại `NhanVienService.update()` (nhận full request, dễ vô tình cho tự đổi chức vụ/lương). Frontend: dropdown theo đúng pattern đã có ở `NavBar.vue` (position-relative + v-if panel + đóng khi mất focus/Esc), 2 modal theo đúng pattern overlay tự viết đã dùng xuyên suốt `AdminPage.vue` (không dùng `Modal.vue` — file này có convention riêng).

**Tech Stack:** Spring Boot 4.0.6 (JPA/Hibernate), Vue 3 `<script setup>`.

## Global Constraints

- Endpoint mới đặt trong `CaiDatController` (`/api/cai-dat/ho-so`), KHÔNG đặt dưới `/api/auth/**` (đang `permitAll()` trong `SecurityConfig.java`).
- KHÔNG thêm `@PreAuthorize` cho endpoint này — cố ý mở cho mọi vai trò tự sửa hồ sơ của chính mình, giống hệt `doiMatKhau`.
- Style DTO response: Lombok `@AllArgsConstructor @Getter @Setter` — đúng theo `CaiDatHeThongResponse.java`/`HoSoResponse` mới.
- Style DTO request: `@NoArgsConstructor @Getter @Setter` + Bean Validation — đúng theo `DoiMatKhauRequest.java`.
- Frontend: `AdminPage.vue` dùng pattern modal tự viết (`<div v-if="showX" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:...;" @click.self="showX=false">`), KHÔNG dùng component `Modal.vue` (file này chưa từng dùng nó ở đâu).
- i18n: tái dùng key đã có ở `admin.settings.*` cho phần đổi mật khẩu (`changePasswordTitle`, `currentPassword`, `newPassword`, `confirmNewPassword`, `changePasswordButton`, `passwordMismatch`, `passwordChanged`, `saveButton`) và `admin.sidebar.settings` cho mục "Cài đặt" — KHÔNG tạo key trùng lặp. Chỉ thêm key mới cho phần thật sự mới (form hồ sơ) dưới khối `admin.profileMenu.*`, đủ cả 5 file `vi.js`/`en.js`/`zh.js`/`ko.js`/`ja.js`.

---

### Task 1: Backend — endpoint tự cập nhật hồ sơ

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/request/HoSoRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/HoSoResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/AuthService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/AuthServiceTest.java`

**Interfaces:**
- Produces: `AuthService.capNhatHoSo(String username, HoSoRequest req) → HoSoResponse` (ném `IllegalStateException` nếu tài khoản không có `NhanVien` liên kết). `PUT /api/cai-dat/ho-so` — Task 2 (frontend service) gọi endpoint này.

- [ ] **Step 1: Tạo `HoSoRequest`**

`BackEnd/src/main/java/com/example/backend/request/HoSoRequest.java`:
```java
package com.example.backend.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class HoSoRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String soDienThoai;

    @Email(message = "Email không hợp lệ")
    private String email;
}
```

- [ ] **Step 2: Tạo `HoSoResponse`**

`BackEnd/src/main/java/com/example/backend/response/HoSoResponse.java`:
```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class HoSoResponse {
    private String hoTen;
    private String soDienThoai;
    private String email;
}
```

- [ ] **Step 3: Thêm `capNhatHoSo` vào `AuthService`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/service/AuthService.java` bằng:
```java
package com.example.backend.service;

import com.example.backend.entity.KhachHang;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.response.HoSoResponse;
import com.example.backend.response.LoginResponse;
import com.example.backend.request.HoSoRequest;
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
    private NhanVienRepository nhanVienRepository;

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

    // Tự sửa hồ sơ (tên/SĐT/email) — chỉ đụng đúng 3 trường này trên NhanVien của người
    // gọi, KHÔNG dùng NhanVienService.update() (nhận full request, có thể vô tình cho tự
    // đổi chức vụ/lương/trạng thái nếu request thiếu field). Trang AdminPage.vue chỉ vào
    // được khi auth.isAdmin (admin/nhân viên/quản kho) nên luôn có NhanVien liên kết —
    // vẫn kiểm tra null để không lộ NPE nếu có trường hợp lạ.
    public HoSoResponse capNhatHoSo(String username, HoSoRequest req) {
        TaiKhoan tk = taiKhoanRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
        NhanVien nv = tk.getNhanVien();
        if (nv == null) {
            throw new IllegalStateException("Tài khoản này không có hồ sơ nhân viên để chỉnh sửa");
        }
        nv.setHoTen(req.getHoTen());
        nv.setSoDienThoai(req.getSoDienThoai());
        nv.setEmail(req.getEmail());
        nhanVienRepository.save(nv);
        return new HoSoResponse(nv.getHoTen(), nv.getSoDienThoai(), nv.getEmail());
    }
}
```

- [ ] **Step 4: Thêm endpoint vào `CaiDatController`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java` bằng:
```java
package com.example.backend.controller;

import com.example.backend.request.CaiDatHeThongRequest;
import com.example.backend.request.DoiMatKhauRequest;
import com.example.backend.request.HoSoRequest;
import com.example.backend.response.CaiDatHeThongResponse;
import com.example.backend.response.HoSoResponse;
import com.example.backend.service.AuthService;
import com.example.backend.service.CaiDatHeThongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// /api/cai-dat/** — KHÔNG nằm trong permitAll() của SecurityConfig, nên mọi endpoint ở đây
// tự động yêu cầu JWT hợp lệ qua .anyRequest().authenticated() (xem SecurityConfig.java).
// get/update/ap-dung-nguong-ton-kho chỉ admin (xem cấu hình cửa hàng, đổi ngưỡng tồn kho hàng loạt).
// doi-mat-khau và ho-so đặt ở đây (không phải AuthController) vì /api/auth/** đang permitAll()
// toàn bộ, và CỐ Ý không giới hạn role — mọi tài khoản (admin, nhân viên, quản kho, khách hàng)
// đều cần tự đổi mật khẩu/sửa hồ sơ của chính mình.
@RestController
@RequestMapping("/api/cai-dat")
public class CaiDatController {

    @Autowired
    private CaiDatHeThongService caiDatHeThongService;

    @Autowired
    private AuthService authService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public CaiDatHeThongResponse get() {
        return caiDatHeThongService.get();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public CaiDatHeThongResponse update(@Valid @RequestBody CaiDatHeThongRequest req) {
        return caiDatHeThongService.update(req);
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PutMapping("/ho-so")
    public HoSoResponse capNhatHoSo(@Valid @RequestBody HoSoRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return authService.capNhatHoSo(username, req);
    }
}
```

- [ ] **Step 5: Viết test cho `capNhatHoSo`**

Thay toàn bộ nội dung `BackEnd/src/test/java/com/example/backend/service/AuthServiceTest.java` bằng:
```java
package com.example.backend.service;

import com.example.backend.entity.ChucVu;
import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.repository.NhanVienRepository;
import com.example.backend.repository.TaiKhoanRepository;
import com.example.backend.request.HoSoRequest;
import com.example.backend.response.HoSoResponse;
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
    @Mock private NhanVienRepository nhanVienRepository;
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

    @Test
    void capNhatHoSo_coNhanVien_chiSuaDungBaTruong() {
        NhanVien nv = new NhanVien();
        nv.setNhanVienId(7);
        nv.setHoTen("Tên cũ");
        nv.setSoDienThoai("0900000000");
        nv.setEmail("cu@example.com");
        nv.setChucVu(new ChucVu()); // giữ nguyên, không được đụng tới
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin");
        tk.setNhanVien(nv);
        when(taiKhoanRepository.findByUsername("admin")).thenReturn(Optional.of(tk));

        HoSoRequest req = new HoSoRequest();
        req.setHoTen("Tên mới");
        req.setSoDienThoai("0911111111");
        req.setEmail("moi@example.com");

        HoSoResponse res = authService.capNhatHoSo("admin", req);

        assertThat(res.getHoTen()).isEqualTo("Tên mới");
        assertThat(res.getSoDienThoai()).isEqualTo("0911111111");
        assertThat(res.getEmail()).isEqualTo("moi@example.com");
        assertThat(nv.getChucVu()).isNotNull(); // không bị đụng tới
        verify(nhanVienRepository).save(nv);
    }

    @Test
    void capNhatHoSo_taiKhoanKhongCoNhanVien_nemLoi() {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("khachle");
        tk.setNhanVien(null);
        when(taiKhoanRepository.findByUsername("khachle")).thenReturn(Optional.of(tk));

        assertThatThrownBy(() -> authService.capNhatHoSo("khachle", new HoSoRequest()))
                .isInstanceOf(IllegalStateException.class);

        verify(nhanVienRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
```

- [ ] **Step 6: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o test "-Dtest=AuthServiceTest"
```
Expected: `BUILD SUCCESS`, 4/4 test passed.

- [ ] **Step 7: Biên dịch toàn bộ backend**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/request/HoSoRequest.java \
  BackEnd/src/main/java/com/example/backend/response/HoSoResponse.java \
  BackEnd/src/main/java/com/example/backend/service/AuthService.java \
  BackEnd/src/main/java/com/example/backend/controller/CaiDatController.java \
  BackEnd/src/test/java/com/example/backend/service/AuthServiceTest.java
git commit -m "feat: add self-service profile-edit endpoint (PUT /api/cai-dat/ho-so)"
```

---

### Task 2: Frontend — service + i18n

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/Service/CaiDatService.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: `CaiDatService.capNhatHoSo(data) → Promise<{hoTen, soDienThoai, email}>` — Task 3 dùng. i18n key mới `admin.profileMenu.{editProfile,fullName,phone,email,profileSaved}` — Task 3 dùng, cùng với key đã có sẵn `admin.settings.{changePasswordTitle,currentPassword,newPassword,confirmNewPassword,changePasswordButton,passwordMismatch,passwordChanged,saveButton}` và `admin.sidebar.settings`.

- [ ] **Step 1: Thêm `capNhatHoSo` vào `CaiDatService.js`**

Thêm vào cuối `FrontEnd/QLBanMayTinh/src/Service/CaiDatService.js`:
```js
export const capNhatHoSo = (data) => put('/api/cai-dat/ho-so', data).then(parseOrThrow);
```

- [ ] **Step 2: Kiểm tra cú pháp**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/Service/CaiDatService.js
```
Expected: không có output/lỗi.

- [ ] **Step 3: Thêm khối i18n `admin.profileMenu` vào cả 5 file**

Trong mỗi file, tìm khối `settings: { ... }` dưới `admin` (kết thúc bằng dòng chứa key `numberFormatEn`, rồi `},`, rồi dòng trống, rồi `pos: {`). Chèn khối `profileMenu` MỚI ngay sau dấu `},` đóng khối `settings` và trước dòng trống + `pos: {`:

`vi.js`:
```js

    profileMenu: {
      editProfile: "Chỉnh sửa hồ sơ",
      fullName: "Họ tên",
      phone: "Số điện thoại",
      email: "Email",
      profileSaved: "Đã cập nhật hồ sơ",
    },
```

`en.js`:
```js

    profileMenu: {
      editProfile: "Edit profile",
      fullName: "Full name",
      phone: "Phone number",
      email: "Email",
      profileSaved: "Profile updated",
    },
```

`zh.js`:
```js

    profileMenu: {
      editProfile: "编辑个人资料",
      fullName: "姓名",
      phone: "电话号码",
      email: "邮箱",
      profileSaved: "个人资料已更新",
    },
```

`ko.js`:
```js

    profileMenu: {
      editProfile: "프로필 수정",
      fullName: "이름",
      phone: "전화번호",
      email: "이메일",
      profileSaved: "프로필이 업데이트되었습니다",
    },
```

`ja.js`:
```js

    profileMenu: {
      editProfile: "プロフィール編集",
      fullName: "氏名",
      phone: "電話番号",
      email: "メール",
      profileSaved: "プロフィールを更新しました",
    },
```

- [ ] **Step 4: Kiểm tra cú pháp cả 5 file**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/i18n/locales/vi.js; node --check src/i18n/locales/en.js; node --check src/i18n/locales/zh.js; node --check src/i18n/locales/ko.js; node --check src/i18n/locales/ja.js
```
Expected: không có output/lỗi ở cả 5 lệnh.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/CaiDatService.js FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat(frontend): add capNhatHoSo service call + profileMenu i18n keys"
```

---

### Task 3: Frontend — dropdown menu + 2 modal

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `CaiDatService.capNhatHoSo`, `CaiDatService.doiMatKhau` (đã có từ trước), `setSession` (`stores/index.js`, chưa import trong file này — cần thêm), i18n key từ Task 2.

- [ ] **Step 1: Thêm `setSession` vào import**

Sửa dòng import (dòng 3):
```js
import { AuthStore, clearSession } from "../stores/index.js";
```
thành:
```js
import { AuthStore, clearSession, setSession } from "../stores/index.js";
```

- [ ] **Step 2: Thêm state + hàm xử lý cho dropdown và 2 modal**

Tìm đoạn khai báo `userDisplayName`/`userAvatar`/`userDisplayRole` (gần đầu file, ngay sau `const logout = ...`), thêm ngay sau khối đó:
```js

// ── Menu hồ sơ (dropdown ở chân sidebar) ─────────────────────────────────────
const showUserMenu = ref(false);
const userMenuTriggerRef = ref(null);
const closeUserMenu = () => {
  showUserMenu.value = false;
  userMenuTriggerRef.value?.focus();
};
const onUserMenuFocusOut = (e) => {
  if (!e.currentTarget.contains(e.relatedTarget)) showUserMenu.value = false;
};

// ── Modal: Chỉnh sửa hồ sơ ────────────────────────────────────────────────────
const showEditProfileModal = ref(false);
const profileForm = ref({ hoTen: '', soDienThoai: '', email: '' });
const profileSaving = ref(false);
const profileError = ref('');
const profileSaved = ref(false);

const openEditProfileModal = () => {
  showUserMenu.value = false;
  profileForm.value = {
    hoTen: AuthStore.user?.hoTen ?? '',
    soDienThoai: AuthStore.user?.soDienThoai ?? '',
    email: AuthStore.user?.email ?? '',
  };
  profileError.value = '';
  profileSaved.value = false;
  showEditProfileModal.value = true;
};

const saveProfile = async () => {
  profileSaving.value = true;
  profileError.value = '';
  profileSaved.value = false;
  try {
    const res = await CaiDatService.capNhatHoSo(profileForm.value);
    setSession({ ...AuthStore.user, hoTen: res.hoTen, soDienThoai: res.soDienThoai, email: res.email });
    profileSaved.value = true;
  } catch (e) {
    profileError.value = e.message || String(e);
  } finally {
    profileSaving.value = false;
  }
};

// ── Modal: Đổi mật khẩu (link nhanh từ menu hồ sơ — cùng API đã có ở trang Cài đặt) ──
const showQuickPasswordModal = ref(false);
const qpMatKhauCu = ref('');
const qpMatKhauMoi = ref('');
const qpMatKhauXacNhan = ref('');
const qpError = ref('');
const qpSuccess = ref('');
const qpLoading = ref(false);

const openQuickPasswordModal = () => {
  showUserMenu.value = false;
  qpMatKhauCu.value = '';
  qpMatKhauMoi.value = '';
  qpMatKhauXacNhan.value = '';
  qpError.value = '';
  qpSuccess.value = '';
  showQuickPasswordModal.value = true;
};

const quickChangePassword = async () => {
  qpError.value = '';
  qpSuccess.value = '';
  if (qpMatKhauMoi.value !== qpMatKhauXacNhan.value) {
    qpError.value = t('admin.settings.passwordMismatch');
    return;
  }
  qpLoading.value = true;
  try {
    await CaiDatService.doiMatKhau(qpMatKhauCu.value, qpMatKhauMoi.value);
    qpSuccess.value = t('admin.settings.passwordChanged');
    qpMatKhauCu.value = '';
    qpMatKhauMoi.value = '';
    qpMatKhauXacNhan.value = '';
  } catch (e) {
    qpError.value = e.message || String(e);
  } finally {
    qpLoading.value = false;
  }
};

const goToSettingsFromMenu = () => {
  showUserMenu.value = false;
  navigate('settings');
};
```

**Lưu ý cho người triển khai:** đoạn code trên dùng `t`, `navigate`, `CaiDatService`, `AuthStore` — đều đã có sẵn trong file, không cần import thêm (trừ `setSession` ở Step 1).

- [ ] **Step 3: Thay khối "Footer sidebar" thành nút mở dropdown**

Thay:
```html
      <!-- Footer sidebar: thong tin user + logout -->
      <div class="p-3 border-top" style="border-color:var(--border-color-soft)!important;">
        <div class="d-flex align-items-center gap-2 mb-2">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
               style="width:34px;height:34px;background:var(--accent);color:var(--accent-text);font-size:0.9rem;">{{ userAvatar }}</div>
          <div class="flex-grow-1" style="min-width:0;">
            <div class="fw-semibold text-truncate" style="font-size:0.85rem;">{{ userDisplayName }}</div>
            <div style="font-size:0.72rem;color:var(--text-muted);">{{ userDisplayRole }}</div>
          </div>
        </div>
        <button class="btn btn-sm w-100 fw-semibold"
                style="background:var(--bg-card); border:1px solid #7f1d1d; border-radius:8px; color:#f87171; font-size:0.78rem;"
                @click="logout">
          {{ t('admin.sidebar.logout') }}
        </button>
      </div>
```
bằng:
```html
      <!-- Footer sidebar: thong tin user (bam mo dropdown ho so) + logout -->
      <div class="p-3 border-top position-relative" style="border-color:var(--border-color-soft)!important;"
           @keydown.esc="closeUserMenu" @focusout="onUserMenuFocusOut">
        <!-- Dropdown menu ho so — mo LEN tren (bottom:100%) vi dang o cuoi sidebar -->
        <div v-if="showUserMenu" class="position-absolute rounded-3 shadow-lg overflow-hidden"
             style="left:12px; right:12px; bottom:100%; margin-bottom:8px; background:var(--bg-card); border:1px solid var(--border-color); z-index:50;">
          <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="openEditProfileModal">
            {{ t('admin.profileMenu.editProfile') }}
          </button>
          <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="openQuickPasswordModal">
            {{ t('admin.settings.changePasswordTitle') }}
          </button>
          <button class="btn btn-sm w-100 text-start rounded-0 border-0" style="color:var(--text-primary);" @click="goToSettingsFromMenu">
            {{ t('admin.sidebar.settings') }}
          </button>
        </div>

        <button ref="userMenuTriggerRef" type="button"
                class="btn d-flex align-items-center gap-2 mb-2 w-100 text-start p-0 border-0"
                style="background:transparent;"
                aria-haspopup="true" :aria-expanded="showUserMenu"
                @click="showUserMenu = !showUserMenu">
          <div class="rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0"
               style="width:34px;height:34px;background:var(--accent);color:var(--accent-text);font-size:0.9rem;">{{ userAvatar }}</div>
          <div class="flex-grow-1" style="min-width:0;">
            <div class="fw-semibold text-truncate" style="font-size:0.85rem;">{{ userDisplayName }}</div>
            <div style="font-size:0.72rem;color:var(--text-muted);">{{ userDisplayRole }}</div>
          </div>
        </button>
        <button class="btn btn-sm w-100 fw-semibold"
                style="background:var(--bg-card); border:1px solid #7f1d1d; border-radius:8px; color:#f87171; font-size:0.78rem;"
                @click="logout">
          {{ t('admin.sidebar.logout') }}
        </button>
      </div>
```

- [ ] **Step 4: Thêm 2 modal — theo đúng pattern overlay tự viết đã dùng trong file này**

Tìm khối modal `showHeldOrders` (bắt đầu bằng comment `<!-- ══ MODAL DON DANG GIU (POS) ══ -->`), chèn 2 modal mới ngay TRƯỚC comment đó:
```html
        <!-- ══ MODAL CHINH SUA HO SO ══ -->
        <div v-if="showEditProfileModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showEditProfileModal=false">
          <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.profileMenu.editProfile') }}</div>
              <button class="btn-close btn-close-white btn-sm" @click="showEditProfileModal=false"></button>
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.fullName') }}</label>
              <input v-model="profileForm.hoTen" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.phone') }}</label>
              <input v-model="profileForm.soDienThoai" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.profileMenu.email') }}</label>
              <input v-model="profileForm.email" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div v-if="profileError" class="text-danger small mb-2">{{ profileError }}</div>
            <div v-if="profileSaved" class="text-success small mb-2">{{ t('admin.profileMenu.profileSaved') }}</div>
            <div class="d-flex justify-content-end gap-2">
              <button class="btn btn-sm btn-outline-secondary" @click="showEditProfileModal=false">{{ t('admin.productModal.cancel') }}</button>
              <button class="btn btn-sm btn-warning" :disabled="profileSaving" @click="saveProfile">{{ t('admin.settings.saveButton') }}</button>
            </div>
          </div>
        </div>

        <!-- ══ MODAL DOI MAT KHAU NHANH (tu menu ho so) ══ -->
        <div v-if="showQuickPasswordModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1070;" @click.self="showQuickPasswordModal=false">
          <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div class="fw-bold" style="color:var(--text-heading);">{{ t('admin.settings.changePasswordTitle') }}</div>
              <button class="btn-close btn-close-white btn-sm" @click="showQuickPasswordModal=false"></button>
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.currentPassword') }}</label>
              <input type="password" v-model="qpMatKhauCu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-2">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.newPassword') }}</label>
              <input type="password" v-model="qpMatKhauMoi" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div class="mb-3">
              <label class="form-label small text-secondary mb-1">{{ t('admin.settings.confirmNewPassword') }}</label>
              <input type="password" v-model="qpMatKhauXacNhan" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </div>
            <div v-if="qpError" class="text-danger small mb-2">{{ qpError }}</div>
            <div v-if="qpSuccess" class="text-success small mb-2">{{ qpSuccess }}</div>
            <div class="d-flex justify-content-end gap-2">
              <button class="btn btn-sm btn-outline-secondary" @click="showQuickPasswordModal=false">{{ t('admin.productModal.cancel') }}</button>
              <button class="btn btn-sm btn-warning" :disabled="qpLoading || !qpMatKhauCu || !qpMatKhauMoi" @click="quickChangePassword">{{ t('admin.settings.changePasswordButton') }}</button>
            </div>
          </div>
        </div>

```

**Lưu ý cho người triển khai:** `t('admin.productModal.cancel')` đã có sẵn trong dự án (dùng ở nhiều modal khác trong cùng file) — tái dùng cho nút Hủy, không tạo key mới.

- [ ] **Step 5: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add profile-menu dropdown with edit-profile and quick-password modals"
```

---

### Task 4: Kiểm thử thủ công end-to-end

**Files:** không có file thay đổi — chỉ chạy và quan sát.

- [ ] **Step 1: Chạy backend + frontend**

```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd spring-boot:run
```
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev
```

- [ ] **Step 2: Kịch bản chính**

1. Bấm vào ô "Quản trị viên" ở chân sidebar — dropdown mở LÊN TRÊN (không tràn khỏi màn hình), gồm đúng 3 mục: Chỉnh sửa hồ sơ / Đổi mật khẩu / Cài đặt.
2. Bấm ra ngoài dropdown (hoặc Esc) — dropdown đóng, không mở modal nào.
3. Bấm "Chỉnh sửa hồ sơ" — modal mở, 3 ô đã điền sẵn đúng tên/SĐT/email hiện tại. Sửa họ tên → Lưu → thấy "Đã cập nhật hồ sơ", tên trên sidebar đổi NGAY (không cần F5/đăng nhập lại).
4. Thử lưu với SĐT trùng 1 nhân viên khác đã có trong hệ thống (tra ở tab Nhân viên) → thấy lỗi rõ ràng trong modal, modal không tự đóng.
5. Bấm "Đổi mật khẩu" (từ menu, không phải trang Cài đặt) — modal riêng mở, nhập sai mật khẩu hiện tại → lỗi "Mật khẩu hiện tại không đúng". Nhập đúng + mật khẩu mới hợp lệ → "Đổi mật khẩu thành công". Đăng xuất, đăng nhập lại bằng mật khẩu MỚI để xác nhận đã đổi thật.
6. Bấm "Cài đặt" từ menu — chuyển sang tab Cài đặt đầy đủ, xác nhận đúng đường link cũ (sidebar nav item "Cài đặt") cũng dẫn tới cùng 1 trang, không xung đột.

- [ ] **Step 3: Dừng server**

`Ctrl+C` ở cả 2 terminal.

---

## Tự rà soát (self-review)

**1. Phủ đủ spec:**
- Dropdown 3 mục ở footer sidebar → Task 3. ✅
- Endpoint tự cập nhật hồ sơ, chỉ đúng 3 trường, không đụng chức vụ/lương → Task 1. ✅
- Modal đổi mật khẩu tái dùng đúng API/i18n key đã có, không viết lại logic → Task 1 (API), Task 3 (UI). ✅
- `AuthStore` cập nhật ngay sau khi lưu hồ sơ → Task 3 (`setSession`). ✅
- i18n đủ 5 ngôn ngữ, tái dùng key cũ thay vì tạo trùng → Task 2. ✅

**2. Không còn placeholder/thiếu code** — đã rà lại, mọi step đều có code đầy đủ.

**3. Nhất quán tên hàm/field xuyên suốt task:**
- `AuthService.capNhatHoSo(username, HoSoRequest) → HoSoResponse` (Task 1) ↔ `CaiDatController.capNhatHoSo()` gọi đúng chữ ký này (Task 1). ✅
- `CaiDatService.capNhatHoSo(data)` (Task 2) ↔ gọi đúng trong `saveProfile()` (Task 3). ✅
- `admin.profileMenu.{editProfile,fullName,phone,email,profileSaved}` (Task 2) ↔ dùng đúng trong template Task 3. ✅
- `HoSoResponse{hoTen,soDienThoai,email}` (Task 1) ↔ `setSession({...AuthStore.user, hoTen: res.hoTen, soDienThoai: res.soDienThoai, email: res.email})` (Task 3) đọc đúng field. ✅

## Ngoài phạm vi

- Không thêm upload avatar (không có cột lưu, không được yêu cầu).
- Không đổi trang tự sửa hồ sơ của khách hàng (`AccountPage.vue`) — đã có sẵn, độc lập.
- Không thêm bước xác nhận email/SĐT mới qua OTP/link — đổi có hiệu lực ngay, giống `NhanVienService.update()` hiện tại.
