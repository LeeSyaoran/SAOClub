# Danh mục linh kiện (CPU/RAM/GPU/Ổ cứng) + Quản lý Serial Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khóa quyền 5 controller backend đang mở hoàn toàn (`DmCpu`/`DmRam`/`DmGpu`/`DmOcung`/`ChiTietSanPham`), bổ sung field `ghiChu` còn thiếu trên chuỗi entity→request→response→repository của `ChiTietSanPham`, rồi xây UI: 1 component dùng chung `DmCategoryTable.vue` cho 4 bảng danh mục linh kiện (Admin-only) + 1 component `SerialManager.vue` quản lý toàn bộ serial (Admin + Kho), gắn vào sub-tab mới trong trang Sản phẩm (`AdminPage.vue`) và mục nav mới trong `WarehouseManagementPage.vue`.

**Architecture:** Backend đã có đủ CRUD cho cả 5 entity — chỉ thiếu `@PreAuthorize` + field `ghiChu`. Frontend: `ChiTietSanPhamService.js` đã có sẵn `create/update/remove`, chỉ `DmService.js` cần thêm CRUD. 1 component Vue dùng chung (props-driven) thay vì 4 file gần giống nhau cho CPU/RAM/GPU/Ổ cứng. `SerialManager.vue` là component riêng dùng lại `ProductsStore` có sẵn để đổ dropdown chọn biến thể.

**Tech Stack:** Spring Boot / JPA (Hibernate) / SQL Server backend, Vue 3 `<script setup>` frontend, JUnit 5 + AssertJ cho backend test.

## Global Constraints

- `@PreAuthorize` dùng đúng 2 khuôn đã có trong dự án: `hasRole('ADMIN')` (như `CaiDatController`) và `hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')` (như `PhieuTraHangController`/`PhieuBaoHanhController`).
- Không đổi luồng "nhập 1 serial lúc tạo sản phẩm mới" đã có sẵn trong `ProductsTable.vue`.
- Không thêm cột mới vào 4 bảng `dm_cpu`/`dm_ram`/`dm_gpu`/`dm_o_cung` — schema hiện tại tối giản, giữ nguyên.
- Không dùng lại `DmXxxRequest`/`DmXxxResponse` DTO đang tồn tại nhưng chưa được controller dùng — controller tiếp tục thao tác thẳng entity.
- 5 file i18n locale (`vi.js`, `en.js`, `zh.js`, `ko.js`, `ja.js`) đều phải nhận key mới; tái dùng key đã có bất cứ khi nào trùng nghĩa (`admin.confirm.deleteSerial`, `admin.errors.saveFailed`, `admin.errors.deleteFailed`, `admin.statusLabel.*`, `admin.common.stt` — đã xác nhận tồn tại sẵn, KHÔNG thêm lại).
- Mọi `LocalDateTime` gửi lên backend dùng `nowLocalIso()` (từ `utils/datetime.js`), không dùng `new Date().toISOString()`.
- Windows/PowerShell: khi build backend, `$env:JAVA_HOME = $env:JAVA_HOME.Trim('"')` phải chạy CÙNG lệnh với `mvnw.cmd`; không dùng `2>&1` với `mvnw.cmd` trên PowerShell. Test frontend dev-server (nếu cần) dùng PowerShell, không dùng Git Bash.

---

### Task 1: Khóa quyền 5 controller + test

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DmCpuController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DmRamController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DmGpuController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DmOcungController.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/ChiTietSanPhamController.java`
- Test: `BackEnd/src/test/java/com/example/backend/controller/DmAndSerialAuthorizationTest.java`

**Interfaces:**
- Produces: 5 controllers khóa quyền, không đổi endpoint signature nào — Task 3 (frontend `DmService.js`) và luồng frontend hiện tại (`ChiTietSanPhamService.js`) không cần đổi gì để vẫn hoạt động (đều gọi từ trang admin đã có JWT hợp lệ).

- [ ] **Step 1: Thêm `@PreAuthorize` vào `DmCpuController`**

Trong `DmCpuController.java`, thêm import và annotation trước class:

```java
package com.example.backend.controller;

import com.example.backend.entity.DmCpu;
import com.example.backend.repository.DmCpuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Danh mục CPU — dữ liệu tham chiếu đơn giản, không cần service riêng
@RestController
@RequestMapping("/api/dm-cpu")
@PreAuthorize("hasRole('ADMIN')")
public class DmCpuController {
```

(Phần còn lại của file giữ nguyên không đổi.)

- [ ] **Step 2: Thêm `@PreAuthorize` vào `DmRamController`, `DmGpuController`, `DmOcungController`**

Áp dụng đúng thay đổi tương tự Step 1 cho 3 file còn lại — chỉ thêm dòng `import org.springframework.security.access.prepost.PreAuthorize;` và `@PreAuthorize("hasRole('ADMIN')")` ngay trên khai báo class, không đổi gì khác.

`DmRamController.java`:
```java
import org.springframework.security.access.prepost.PreAuthorize;
...
@RestController
@RequestMapping("/api/dm-ram")
@PreAuthorize("hasRole('ADMIN')")
public class DmRamController {
```

`DmGpuController.java`:
```java
import org.springframework.security.access.prepost.PreAuthorize;
...
@RestController
@RequestMapping("/api/dm-gpu")
@PreAuthorize("hasRole('ADMIN')")
public class DmGpuController {
```

`DmOcungController.java`:
```java
import org.springframework.security.access.prepost.PreAuthorize;
...
@RestController
@RequestMapping("/api/dm-o-cung")
@PreAuthorize("hasRole('ADMIN')")
public class DmOcungController {
```

- [ ] **Step 3: Thêm `@PreAuthorize` vào `ChiTietSanPhamController`**

```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.request.ChiTietSanPhamRequest;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.WarrantyStatusResponse;
import com.example.backend.service.ChiTietSanPhamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-san-pham")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietSanPhamController {
```

(Phần còn lại của file giữ nguyên.)

- [ ] **Step 4: Viết test xác nhận annotation**

Tạo file mới `BackEnd/src/test/java/com/example/backend/controller/DmAndSerialAuthorizationTest.java`:

```java
package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// 5 controller nay truoc task nay KHONG co bat ky @PreAuthorize nao (mo cho moi role da
// dang nhap, ke ca khach hang). Da xac nhan (grep) chi ChiTietSanPhamService.js va
// DmService.js tung goi toi cac endpoint nay, ca 2 deu chi dung o phia admin — khoa an toan.
class DmAndSerialAuthorizationTest {

    @Test
    void dmCpuController_khoaChoAdmin() {
        PreAuthorize pa = DmCpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void dmRamController_khoaChoAdmin() {
        PreAuthorize pa = DmRamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void dmGpuController_khoaChoAdmin() {
        PreAuthorize pa = DmGpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void dmOcungController_khoaChoAdmin() {
        PreAuthorize pa = DmOcungController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void chiTietSanPhamController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietSanPhamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
```

- [ ] **Step 5: Chạy test**

Run (PowerShell, từ thư mục `BackEnd`):
```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); .\mvnw.cmd test "-Dtest=DmAndSerialAuthorizationTest"
```
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/controller/DmCpuController.java BackEnd/src/main/java/com/example/backend/controller/DmRamController.java BackEnd/src/main/java/com/example/backend/controller/DmGpuController.java BackEnd/src/main/java/com/example/backend/controller/DmOcungController.java BackEnd/src/main/java/com/example/backend/controller/ChiTietSanPhamController.java BackEnd/src/test/java/com/example/backend/controller/DmAndSerialAuthorizationTest.java
git commit -m "feat(security): lock dm-cpu/ram/gpu/o-cung + chi-tiet-san-pham controllers"
```

---

### Task 2: Bổ sung field `ghiChu` cho chuỗi entity/request/response/repository của `ChiTietSanPham`

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/entity/ChiTietSanPham.java`
- Modify: `BackEnd/src/main/java/com/example/backend/request/ChiTietSanPhamRequest.java`
- Modify: `BackEnd/src/main/java/com/example/backend/response/ChiTietSanPhamResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/ChiTietSanPhamService.java`

**Lưu ý quan trọng (khác spec ban đầu):** Cột DB `chi_tiet_san_pham.ghi_chu NVARCHAR(255) NULL` đã tồn tại (đã xác nhận đọc trực tiếp `Database/QLBanMayTinh.sql` dòng 273), NHƯNG entity `ChiTietSanPham.java` **cũng chưa có field `ghiChu`** (không chỉ Request/Response như spec giả định) — phải thêm cả 3 tầng: entity, request, response.

**Interfaces:**
- Consumes: không phụ thuộc task nào trước.
- Produces: `ChiTietSanPhamRequest.getGhiChu()/setGhiChu(String)`, `ChiTietSanPhamResponse.getGhiChu()/setGhiChu(String)` — Task 6 (`SerialManager.vue`) đọc/ghi field `ghiChu` trên JSON body/response dựa vào các getter/setter này.

- [ ] **Step 1: Thêm field vào entity**

Trong `ChiTietSanPham.java`, thêm field mới sau `ngayNhapKho`:

```java
    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
```

- [ ] **Step 2: Thêm field vào Request DTO**

Trong `ChiTietSanPhamRequest.java`:

```java
package com.example.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChiTietSanPhamRequest {
    @NotNull(message = "Phiên bản sản phẩm không được để trống")
    private Integer bienTheId;

    @NotBlank(message = "Số serial không được để trống")
    private String soSerial;

    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

- [ ] **Step 3: Thêm field vào Response DTO**

Trong `ChiTietSanPhamResponse.java`:

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
public class ChiTietSanPhamResponse {
    private Integer chiTietId;
    private Integer bienTheId;
    private String maSku;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

- [ ] **Step 4: Cập nhật JPQL constructor-expression trong repository**

Trong `ChiTietSanPhamRepository.java`, thêm `c.ghiChu` làm tham số cuối trong CẢ HAI query (thứ tự tham số phải khớp đúng thứ tự field vừa khai báo ở Step 3):

```java
    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietSanPham c")
    List<ChiTietSanPhamResponse> hienThiChiTietSanPham();

    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietSanPham c WHERE c.bienThe.bienTheId = :bienTheId")
    List<ChiTietSanPhamResponse> findByBienTheId(@Param("bienTheId") Integer bienTheId);
```

(Phần còn lại của file — 2 query khác không dùng `ChiTietSanPhamResponse` — giữ nguyên không đổi.)

- [ ] **Step 5: Sửa comment cũ sai trong service**

`BeanUtils.copyProperties` tự copy field mới theo tên (reflection), không cần đổi logic — chỉ sửa comment đang nhắc tới field không tồn tại `soImei`:

Trong `ChiTietSanPhamService.java`, sửa dòng comment:

```java
    public ChiTietSanPham create(ChiTietSanPhamRequest request) {
        ChiTietSanPham entity = new ChiTietSanPham();
        // BeanUtils copies: soSerial, trangThai, ngayNhapKho, ghiChu
        BeanUtils.copyProperties(request, entity, "bienTheId");
        entity.setBienThe(bienTheSanPhamRepository.getReferenceById(request.getBienTheId()));
        return chiTietSanPhamRepository.save(entity);
    }
```

- [ ] **Step 6: Build để xác nhận compile thành công**

Run (PowerShell, từ thư mục `BackEnd`):
```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); .\mvnw.cmd compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/ChiTietSanPham.java BackEnd/src/main/java/com/example/backend/request/ChiTietSanPhamRequest.java BackEnd/src/main/java/com/example/backend/response/ChiTietSanPhamResponse.java BackEnd/src/main/java/com/example/backend/repository/ChiTietSanPhamRepository.java BackEnd/src/main/java/com/example/backend/service/ChiTietSanPhamService.java
git commit -m "feat: add missing ghiChu field to ChiTietSanPham entity/request/response"
```

---

### Task 3: `DmService.js` — thêm CRUD cho 4 danh mục linh kiện

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/Service/DmService.js`

**Interfaces:**
- Consumes: Task 1's 4 khóa-quyền-ADMIN controllers (không đổi endpoint path/shape, chỉ cần JWT admin hợp lệ — trang admin đã có sẵn).
- Produces: `DmCpuService`/`DmRamService`/`DmGpuService`/`DmOCungService`, mỗi cái có `{ getAll(), save(id, body), remove(id) }` — Task 5 (`DmCategoryTable.vue`) nhận 1 trong 4 object này qua prop `service`. `save()` trả về `Response` thô (không auto-parse JSON) — caller phải tự `.ok`-check, đúng quy ước `post/put/del` trong `api.js`.

- [ ] **Step 1: Đọc file hiện tại để xác nhận vị trí chèn**

File hiện tại:
```javascript
import { get } from './api.js';
export const getThuongHieu = () => get('/api/thuong-hieu');
export const getNhaCungCap = () => get('/api/nha-cung-cap');
export const getChucVu     = () => get('/api/chuc-vu');
export const getCpu        = () => get('/api/dm-cpu');
export const getRam        = () => get('/api/dm-ram');
export const getOCung      = () => get('/api/dm-o-cung');
export const getGpu        = () => get('/api/dm-gpu');
```

- [ ] **Step 2: Thêm `crud()` factory + 4 export mới**

Thay toàn bộ nội dung file bằng:

```javascript
import { get, post, put, del } from './api.js';
export const getThuongHieu = () => get('/api/thuong-hieu');
export const getNhaCungCap = () => get('/api/nha-cung-cap');
export const getChucVu     = () => get('/api/chuc-vu');
export const getCpu        = () => get('/api/dm-cpu');
export const getRam        = () => get('/api/dm-ram');
export const getOCung      = () => get('/api/dm-o-cung');
export const getGpu        = () => get('/api/dm-gpu');

const crud = (path) => ({
  getAll: () => get(`/api/${path}`),
  save: (id, body) => id ? put(`/api/${path}/update/${id}`, body) : post(`/api/${path}`, body),
  remove: (id) => del(`/api/${path}/delete/${id}`),
});

export const DmCpuService = crud('dm-cpu');
export const DmRamService = crud('dm-ram');
export const DmGpuService = crud('dm-gpu');
export const DmOCungService = crud('dm-o-cung');
```

- [ ] **Step 3: Kiểm tra thủ công**

Không có test framework frontend trong repo này. Xác nhận bằng cách grep không còn tham chiếu vỡ:
```bash
grep -rn "getCpu\|getRam\|getOCung\|getGpu" "FrontEnd/QLBanMayTinh/src/components/admin/ProductsTable.vue"
```
Expected: các dòng gọi `DmService.getCpu()` v.v. vẫn còn nguyên (không đổi), chứng tỏ 4 hàm GET cũ chưa bị động tới.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/DmService.js
git commit -m "feat: add CRUD functions to DmService for CPU/RAM/GPU/O-cung categories"
```

---

### Task 4: 5 file i18n — thêm key mới cho productsTabs / dmCategory / serialManager

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Đã xác nhận các key sau ĐÃ TỒN TẠI ở cả 5 file, KHÔNG thêm lại:** `admin.confirm.deleteSerial`, `admin.errors.saveFailed`, `admin.errors.deleteFailed`, `admin.statusLabel.trong_kho/giu_hang/da_ban/loi_bao_hanh/da_tra_hang`, `admin.common.stt`.

**Interfaces:**
- Produces: key namespace `admin.productsTabs.*`, `admin.dmCategory.*`, `admin.serialManager.*`, `admin.confirm.deleteDmItem`, `admin.sidebar.serial`, `admin.pageMeta.serial.{title,sub}` — dùng bởi Task 5 (`DmCategoryTable.vue`), Task 6 (`SerialManager.vue`), Task 7 (`AdminPage.vue`), Task 8 (`WarehouseManagementPage.vue`).

- [ ] **Step 1: `vi.js` — thêm `serial` vào `sidebar` và `pageMeta`**

Tìm dòng:
```javascript
      warrantyClaims: "Phiếu bảo hành",
    },
```
(đây là cuối block `sidebar:`) — thay bằng:
```javascript
      warrantyClaims: "Phiếu bảo hành",
      serial: "Serial",
    },
```

Tìm dòng:
```javascript
      warrantyClaims:    { title: "Phiếu bảo hành", sub: "Tiếp nhận và xử lý bảo hành" },
    },
```
(đây là cuối block `pageMeta:`) — thay bằng:
```javascript
      warrantyClaims:    { title: "Phiếu bảo hành", sub: "Tiếp nhận và xử lý bảo hành" },
      serial:            { title: "Quản lý Serial", sub: "Xem, thêm, sửa, xóa serial máy" },
    },
```

- [ ] **Step 2: `vi.js` — thêm block `productsTabs`, `dmCategory`, `serialManager`**

Tìm dòng cuối block `inventory:` (ngay trước block `inventoryHistory:`):
```javascript
      empty: "Không có dữ liệu",
    },

    inventoryHistory: {
```
Thay bằng:
```javascript
      empty: "Không có dữ liệu",
    },

    productsTabs: {
      sanPham: "Sản phẩm",
      cpu: "CPU",
      ram: "RAM",
      gpu: "GPU",
      oCung: "Ổ cứng",
      serial: "Serial",
    },

    dmCategory: {
      countSuffix: "{label}",
      searchPlaceholder: "Tìm {label}...",
      add: "+ Thêm {label}",
      loading: "Đang tải...",
      colAction: "Thao tác",
      edit: "Sửa",
      delete: "Xóa",
      empty: "Chưa có {label} nào",
      titleAdd: "Thêm {label}",
      titleEdit: "Sửa {label}",
      cancel: "Hủy",
      save: "Lưu",
      nameRequired: "Vui lòng nhập {label}",
    },

    serialManager: {
      searchPlaceholder: "Tìm serial, SKU, tên sản phẩm...",
      add: "+ Thêm serial",
      loading: "Đang tải...",
      colVariant: "Sản phẩm / SKU",
      colSerial: "Số serial",
      colStatus: "Trạng thái",
      colDate: "Ngày nhập kho",
      colNote: "Ghi chú",
      colAction: "Thao tác",
      edit: "Sửa",
      delete: "Xóa",
      empty: "Chưa có serial nào",
      titleAdd: "Thêm serial",
      titleEdit: "Sửa serial",
      variantLabel: "Sản phẩm / biến thể",
      variantPlaceholder: "Chọn sản phẩm...",
      serialLabel: "Số serial",
      statusLabel: "Trạng thái",
      dateLabel: "Ngày nhập kho",
      noteLabel: "Ghi chú",
      cancel: "Hủy",
      save: "Lưu",
      serialRequired: "Vui lòng nhập số serial",
      variantRequired: "Vui lòng chọn sản phẩm/biến thể",
    },

    inventoryHistory: {
```

- [ ] **Step 3: `vi.js` — thêm `deleteDmItem` vào block `confirm`**

Tìm dòng:
```javascript
      deleteWarrantyClaim: "Xóa phiếu bảo hành này?",
    },
```
Thay bằng:
```javascript
      deleteWarrantyClaim: "Xóa phiếu bảo hành này?",
      deleteDmItem: "Xóa {label} này?",
    },
```

- [ ] **Step 4: Lặp lại Step 1-3 cho `en.js` với nội dung dịch tiếng Anh**

`sidebar`/`pageMeta` (tìm anchor `warrantyClaims: "Warranty claims",` / `warrantyClaims:    { title: "Warranty claims", sub: "Receive and process warranty claims" },`):
```javascript
      warrantyClaims: "Warranty claims",
      serial: "Serial",
    },
```
```javascript
      warrantyClaims:    { title: "Warranty claims", sub: "Receive and process warranty claims" },
      serial:            { title: "Serial management", sub: "View, add, edit, delete device serials" },
    },
```

`productsTabs`/`dmCategory`/`serialManager` (chèn trước block `inventoryHistory: {` ngay sau block `inventory:`):
```javascript
    productsTabs: {
      sanPham: "Products",
      cpu: "CPU",
      ram: "RAM",
      gpu: "GPU",
      oCung: "Storage",
      serial: "Serial",
    },

    dmCategory: {
      countSuffix: "{label}",
      searchPlaceholder: "Search {label}...",
      add: "+ Add {label}",
      loading: "Loading...",
      colAction: "Actions",
      edit: "Edit",
      delete: "Delete",
      empty: "No {label} yet",
      titleAdd: "Add {label}",
      titleEdit: "Edit {label}",
      cancel: "Cancel",
      save: "Save",
      nameRequired: "Please enter {label}",
    },

    serialManager: {
      searchPlaceholder: "Search serial, SKU, product name...",
      add: "+ Add serial",
      loading: "Loading...",
      colVariant: "Product / SKU",
      colSerial: "Serial number",
      colStatus: "Status",
      colDate: "Stock-in date",
      colNote: "Note",
      colAction: "Actions",
      edit: "Edit",
      delete: "Delete",
      empty: "No serial yet",
      titleAdd: "Add serial",
      titleEdit: "Edit serial",
      variantLabel: "Product / variant",
      variantPlaceholder: "Select product...",
      serialLabel: "Serial number",
      statusLabel: "Status",
      dateLabel: "Stock-in date",
      noteLabel: "Note",
      cancel: "Cancel",
      save: "Save",
      serialRequired: "Please enter the serial number",
      variantRequired: "Please select a product/variant",
    },
```

`confirm` (tìm anchor `deleteWarrantyClaim: "Delete this warranty claim?",`):
```javascript
      deleteWarrantyClaim: "Delete this warranty claim?",
      deleteDmItem: "Delete this {label}?",
    },
```

- [ ] **Step 5: Lặp lại cho `zh.js` với nội dung dịch tiếng Trung**

`sidebar`/`pageMeta` (anchor `warrantyClaims: "保修单",` / `warrantyClaims:    { title: "保修单", sub: "接收并处理保修申请" },`):
```javascript
      warrantyClaims: "保修单",
      serial: "序列号",
    },
```
```javascript
      warrantyClaims:    { title: "保修单", sub: "接收并处理保修申请" },
      serial:            { title: "序列号管理", sub: "查看、添加、编辑、删除设备序列号" },
    },
```

`productsTabs`/`dmCategory`/`serialManager`:
```javascript
    productsTabs: {
      sanPham: "产品",
      cpu: "CPU",
      ram: "内存",
      gpu: "显卡",
      oCung: "硬盘",
      serial: "序列号",
    },

    dmCategory: {
      countSuffix: "{label}",
      searchPlaceholder: "搜索{label}...",
      add: "+ 添加{label}",
      loading: "加载中...",
      colAction: "操作",
      edit: "编辑",
      delete: "删除",
      empty: "暂无{label}",
      titleAdd: "添加{label}",
      titleEdit: "编辑{label}",
      cancel: "取消",
      save: "保存",
      nameRequired: "请输入{label}",
    },

    serialManager: {
      searchPlaceholder: "搜索序列号、SKU、产品名称...",
      add: "+ 添加序列号",
      loading: "加载中...",
      colVariant: "产品 / SKU",
      colSerial: "序列号",
      colStatus: "状态",
      colDate: "入库日期",
      colNote: "备注",
      colAction: "操作",
      edit: "编辑",
      delete: "删除",
      empty: "暂无序列号",
      titleAdd: "添加序列号",
      titleEdit: "编辑序列号",
      variantLabel: "产品 / 型号",
      variantPlaceholder: "选择产品...",
      serialLabel: "序列号",
      statusLabel: "状态",
      dateLabel: "入库日期",
      noteLabel: "备注",
      cancel: "取消",
      save: "保存",
      serialRequired: "请输入序列号",
      variantRequired: "请选择产品/型号",
    },
```

`confirm` (anchor `deleteWarrantyClaim: "确定删除此保修单？",`):
```javascript
      deleteWarrantyClaim: "确定删除此保修单？",
      deleteDmItem: "确定删除此{label}？",
    },
```

- [ ] **Step 6: Lặp lại cho `ko.js` với nội dung dịch tiếng Hàn**

`sidebar`/`pageMeta` (anchor `warrantyClaims: "보증 신청서",` / `warrantyClaims:    { title: "보증 신청서", sub: "보증 접수 및 처리" },`):
```javascript
      warrantyClaims: "보증 신청서",
      serial: "시리얼",
    },
```
```javascript
      warrantyClaims:    { title: "보증 신청서", sub: "보증 접수 및 처리" },
      serial:            { title: "시리얼 관리", sub: "기기 시리얼 조회/추가/수정/삭제" },
    },
```

`productsTabs`/`dmCategory`/`serialManager`:
```javascript
    productsTabs: {
      sanPham: "제품",
      cpu: "CPU",
      ram: "RAM",
      gpu: "GPU",
      oCung: "저장장치",
      serial: "시리얼",
    },

    dmCategory: {
      countSuffix: "{label}",
      searchPlaceholder: "{label} 검색...",
      add: "+ {label} 추가",
      loading: "불러오는 중...",
      colAction: "작업",
      edit: "수정",
      delete: "삭제",
      empty: "등록된 {label}이(가) 없습니다",
      titleAdd: "{label} 추가",
      titleEdit: "{label} 수정",
      cancel: "취소",
      save: "저장",
      nameRequired: "{label}을(를) 입력해 주세요",
    },

    serialManager: {
      searchPlaceholder: "시리얼, SKU, 제품명 검색...",
      add: "+ 시리얼 추가",
      loading: "불러오는 중...",
      colVariant: "제품 / SKU",
      colSerial: "시리얼 번호",
      colStatus: "상태",
      colDate: "입고일",
      colNote: "메모",
      colAction: "작업",
      edit: "수정",
      delete: "삭제",
      empty: "등록된 시리얼이 없습니다",
      titleAdd: "시리얼 추가",
      titleEdit: "시리얼 수정",
      variantLabel: "제품 / 옵션",
      variantPlaceholder: "제품 선택...",
      serialLabel: "시리얼 번호",
      statusLabel: "상태",
      dateLabel: "입고일",
      noteLabel: "메모",
      cancel: "취소",
      save: "저장",
      serialRequired: "시리얼 번호를 입력해 주세요",
      variantRequired: "제품/옵션을 선택해 주세요",
    },
```

`confirm` (anchor `deleteWarrantyClaim: "이 보증 신청서를 삭제할까요?",`):
```javascript
      deleteWarrantyClaim: "이 보증 신청서를 삭제할까요?",
      deleteDmItem: "이 {label}을(를) 삭제할까요?",
    },
```

- [ ] **Step 7: Lặp lại cho `ja.js` với nội dung dịch tiếng Nhật**

`sidebar`/`pageMeta` (anchor `warrantyClaims: "保証申請",` / `warrantyClaims:    { title: "保証申請", sub: "保証の受付と処理" },`):
```javascript
      warrantyClaims: "保証申請",
      serial: "シリアル",
    },
```
```javascript
      warrantyClaims:    { title: "保証申請", sub: "保証の受付と処理" },
      serial:            { title: "シリアル管理", sub: "機器シリアルの閲覧・追加・編集・削除" },
    },
```

`productsTabs`/`dmCategory`/`serialManager`:
```javascript
    productsTabs: {
      sanPham: "商品",
      cpu: "CPU",
      ram: "RAM",
      gpu: "GPU",
      oCung: "ストレージ",
      serial: "シリアル",
    },

    dmCategory: {
      countSuffix: "{label}",
      searchPlaceholder: "{label}を検索...",
      add: "+ {label}を追加",
      loading: "読み込み中...",
      colAction: "操作",
      edit: "編集",
      delete: "削除",
      empty: "{label}はまだありません",
      titleAdd: "{label}を追加",
      titleEdit: "{label}を編集",
      cancel: "キャンセル",
      save: "保存",
      nameRequired: "{label}を入力してください",
    },

    serialManager: {
      searchPlaceholder: "シリアル、SKU、商品名で検索...",
      add: "+ シリアルを追加",
      loading: "読み込み中...",
      colVariant: "商品 / SKU",
      colSerial: "シリアル番号",
      colStatus: "状態",
      colDate: "入庫日",
      colNote: "備考",
      colAction: "操作",
      edit: "編集",
      delete: "削除",
      empty: "シリアルはまだありません",
      titleAdd: "シリアルを追加",
      titleEdit: "シリアルを編集",
      variantLabel: "商品 / バリエーション",
      variantPlaceholder: "商品を選択...",
      serialLabel: "シリアル番号",
      statusLabel: "状態",
      dateLabel: "入庫日",
      noteLabel: "備考",
      cancel: "キャンセル",
      save: "保存",
      serialRequired: "シリアル番号を入力してください",
      variantRequired: "商品/バリエーションを選択してください",
    },
```

`confirm` (anchor `deleteWarrantyClaim: "この保証申請を削除しますか？",`):
```javascript
      deleteWarrantyClaim: "この保証申請を削除しますか？",
      deleteDmItem: "この{label}を削除しますか？",
    },
```

- [ ] **Step 8: Kiểm tra cú pháp JS hợp lệ**

Run (PowerShell, từ thư mục `FrontEnd/QLBanMayTinh`):
```powershell
node -e "require('./src/i18n/locales/vi.js')" 2>$null; node --input-type=module -e "import('./src/i18n/locales/vi.js').then(()=>console.log('OK vi'))"
node --input-type=module -e "import('./src/i18n/locales/en.js').then(()=>console.log('OK en'))"
node --input-type=module -e "import('./src/i18n/locales/zh.js').then(()=>console.log('OK zh'))"
node --input-type=module -e "import('./src/i18n/locales/ko.js').then(()=>console.log('OK ko'))"
node --input-type=module -e "import('./src/i18n/locales/ja.js').then(()=>console.log('OK ja'))"
```
Expected: mỗi lệnh in ra `OK <locale>` không có lỗi cú pháp.

- [ ] **Step 9: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat(i18n): add keys for products sub-tabs, Dm category table, serial manager"
```

---

### Task 5: `DmCategoryTable.vue` — component dùng chung cho 4 bảng danh mục linh kiện

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue`

**Interfaces:**
- Consumes: prop `service` = 1 trong 4 object từ Task 3 (`{ getAll(), save(id, body), remove(id) }`), `t()` key namespace `admin.dmCategory.*` + `admin.confirm.deleteDmItem` + `admin.common.stt` từ Task 4, `askConfirm` (`stores/confirm.js`), `showToast` (`stores/toast.js`).
- Produces: component nhận props `service: Object` (required), `idField: String` (required, vd `'cpuId'`), `nameField: String` (required, vd `'tenCpu'`), `label: String` (required, vd `'CPU'`), `nameLabel: String` (required, vd `'Tên CPU'`) — Task 7 gắn 4 lần với props khác nhau.

- [ ] **Step 1: Viết component**

```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";

const props = defineProps({
  service: { type: Object, required: true },
  idField: { type: String, required: true },
  nameField: { type: String, required: true },
  label: { type: String, required: true },
  nameLabel: { type: String, required: true },
});

const items = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    items.value = await props.service.getAll().catch(() => []);
  } finally {
    loading.value = false;
  }
};
onMounted(load);

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((i) => (i[props.nameField] || '').toLowerCase().includes(q));
});

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const formValue = ref("");

const openAdd = () => {
  editingId.value = null;
  formValue.value = "";
  formError.value = "";
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item[props.idField];
  formValue.value = item[props.nameField];
  formError.value = "";
  showModal.value = true;
};

const saveItem = async () => {
  formError.value = "";
  if (!formValue.value.trim()) {
    formError.value = t('admin.dmCategory.nameRequired', { label: props.nameLabel });
    return;
  }
  try {
    const body = { [props.nameField]: formValue.value.trim() };
    const res = await props.service.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteItem = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteDmItem', { label: props.label })))) return;
  const res = await props.service.remove(id);
  if (!res.ok) {
    showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status })));
    return;
  }
  await load();
};
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredItems.length }}/{{ items.length }} {{ t('admin.dmCategory.countSuffix', { label }) }}</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="search" class="form-control form-control-sm" style="width:220px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.dmCategory.searchPlaceholder', { label })" />
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.dmCategory.add', { label }) }}</button>
    </div>
  </div>

  <div v-if="loading" class="text-secondary small">{{ t('admin.dmCategory.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead>
        <tr>
          <th style="width:40px;">{{ t('admin.common.stt') }}</th>
          <th>{{ nameLabel }}</th>
          <th style="width:140px;">{{ t('admin.dmCategory.colAction') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in filteredItems" :key="item[idField]">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td>{{ item[nameField] }}</td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(item)">{{ t('admin.dmCategory.edit') }}</button>
              <button class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteItem(item[idField])">{{ t('admin.dmCategory.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="3" class="text-center text-secondary">{{ t('admin.dmCategory.empty', { label }) }}</td></tr>
      </tbody>
    </table>
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:400px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.dmCategory.titleEdit', { label }) : t('admin.dmCategory.titleAdd', { label }) }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ nameLabel }}</label>
        <input v-model="formValue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" @keyup.enter="saveItem" />
      </div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.dmCategory.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveItem">{{ t('admin.dmCategory.save') }}</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Kiểm tra thủ công (không có test framework frontend)**

Sau khi Task 7 gắn component vào `AdminPage.vue`, kiểm tra bằng cách chạy dev server (xem Step kiểm tra ở Task 7) và thử thêm/sửa/xóa 1 CPU thật. Task này tự nó không thể test độc lập vì cần điểm gắn (Task 7) — bỏ qua bước test riêng, review code diff bằng mắt là đủ (component nhỏ, logic giống hệt `SupplierManager.vue` đã có, tối giản hơn).

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue
git commit -m "feat: add shared DmCategoryTable component for CPU/RAM/GPU/O-cung CRUD"
```

---

### Task 6: `SerialManager.vue` — component quản lý toàn bộ serial

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/admin/SerialManager.vue`

**Interfaces:**
- Consumes: `ChiTietSanPhamService.{getAll, create, update, remove}` (đã có sẵn, không đổi ở task này), `ProductsStore`/`ensureProducts` (`stores/products.js`, đã có sẵn — items có field `bienTheId`/`tenSanPham`/`maSku`), `SearchSelect.vue` (đã có sẵn, props `modelValue`/`options: [{value,label}]`/`placeholder`/`disabled`), `nowLocalIso()` (`utils/datetime.js`), `formatDate()` (`utils/adminFormat.js`), `t()` key namespace `admin.serialManager.*` + `admin.statusLabel.*` + `admin.confirm.deleteSerial` + `admin.errors.saveFailed/deleteFailed` + `admin.common.stt` (đã có từ trước hoặc Task 4).
- Produces: component không nhận prop nào — Task 7 và Task 8 đều gắn `<SerialManager />` full CRUD giống hệt nhau.

- [ ] **Step 1: Viết component**

```vue
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import { formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import SearchSelect from "../common/SearchSelect.vue";

const items = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    items.value = await ChiTietSanPhamService.getAll().catch(() => []);
  } finally {
    loading.value = false;
  }
};
onMounted(() => { load(); ensureProducts(); });

const variantOptions = computed(() =>
  ProductsStore.items.map((p) => ({ value: p.bienTheId, label: `${p.tenSanPham} — ${p.maSku}` }))
);
const variantLabel = (bienTheId) => variantOptions.value.find((o) => o.value === bienTheId)?.label ?? '';

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((i) =>
    [i.soSerial, i.maSku, variantLabel(i.bienTheId)].some((v) => (v || '').toLowerCase().includes(q))
  );
});

const STATUS_COLOR = {
  trong_kho: '#22c55e',
  giu_hang: '#facc15',
  da_ban: '#94a3b8',
  loi_bao_hanh: '#fb923c',
  da_tra_hang: '#38bdf8',
};
const statusColor = (s) => STATUS_COLOR[s] ?? '#6b7280';
const statusLabel = (s) => t(`admin.statusLabel.${s}`);

const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const emptyForm = () => ({
  bienTheId: '',
  soSerial: '',
  trangThai: 'trong_kho',
  ngayNhapKho: nowLocalIso().slice(0, 16),
  ghiChu: '',
});
const form = ref(emptyForm());

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item.chiTietId;
  form.value = {
    bienTheId: item.bienTheId,
    soSerial: item.soSerial,
    trangThai: item.trangThai,
    ngayNhapKho: (item.ngayNhapKho || '').slice(0, 16),
    ghiChu: item.ghiChu || '',
  };
  formError.value = "";
  showModal.value = true;
};

const saveSerial = async () => {
  formError.value = "";
  if (!form.value.bienTheId) { formError.value = t('admin.serialManager.variantRequired'); return; }
  if (!form.value.soSerial.trim()) { formError.value = t('admin.serialManager.serialRequired'); return; }
  try {
    const body = {
      bienTheId: Number(form.value.bienTheId),
      soSerial: form.value.soSerial.trim(),
      trangThai: form.value.trangThai,
      ngayNhapKho: nowLocalIso(new Date(form.value.ngayNhapKho)),
      ghiChu: form.value.ghiChu || null,
    };
    const res = editingId.value
      ? await ChiTietSanPhamService.update(editingId.value, body)
      : await ChiTietSanPhamService.create(body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  }
};

const deleteSerial = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const res = await ChiTietSanPhamService.remove(id);
  if (!res.ok) {
    showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status })));
    return;
  }
  await load();
};
</script>

<template>
  <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
    <span class="text-secondary small">{{ filteredItems.length }}/{{ items.length }} serial</span>
    <div class="d-flex gap-2 flex-wrap">
      <input v-model="search" class="form-control form-control-sm" style="width:260px;background:var(--bg-input);border-color:var(--border-color-strong);color:var(--text-primary);" :placeholder="t('admin.serialManager.searchPlaceholder')" />
      <button class="btn btn-sm btn-warning text-dark fw-bold" @click="openAdd">{{ t('admin.serialManager.add') }}</button>
    </div>
  </div>

  <div v-if="loading" class="text-secondary small">{{ t('admin.serialManager.loading') }}</div>
  <div v-else class="table-responsive">
    <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
      <thead>
        <tr>
          <th style="width:40px;">{{ t('admin.common.stt') }}</th>
          <th>{{ t('admin.serialManager.colVariant') }}</th>
          <th>{{ t('admin.serialManager.colSerial') }}</th>
          <th>{{ t('admin.serialManager.colStatus') }}</th>
          <th>{{ t('admin.serialManager.colDate') }}</th>
          <th>{{ t('admin.serialManager.colNote') }}</th>
          <th style="width:140px;">{{ t('admin.serialManager.colAction') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in filteredItems" :key="item.chiTietId">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td>{{ variantLabel(item.bienTheId) || item.maSku }}</td>
          <td>{{ item.soSerial }}</td>
          <td>
            <span style="display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:6px;" :style="{ background: statusColor(item.trangThai) }"></span>
            {{ statusLabel(item.trangThai) }}
          </td>
          <td class="text-secondary">{{ formatDate(item.ngayNhapKho) }}</td>
          <td class="text-secondary">{{ item.ghiChu }}</td>
          <td>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-warning" style="font-size:0.78rem;padding:2px 8px;" @click="openEdit(item)">{{ t('admin.serialManager.edit') }}</button>
              <button v-if="item.trangThai === 'trong_kho'" class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteSerial(item.chiTietId)">{{ t('admin.serialManager.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="7" class="text-center text-secondary">{{ t('admin.serialManager.empty') }}</td></tr>
      </tbody>
    </table>
  </div>

  <div v-if="showModal" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:var(--bg-overlay);z-index:1000;" @click.self="showModal=false">
    <div class="rounded-3 p-3" style="background:var(--bg-card);width:420px;max-width:94vw;">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <div class="fw-bold" style="color:var(--text-heading);">{{ editingId ? t('admin.serialManager.titleEdit') : t('admin.serialManager.titleAdd') }}</div>
        <button class="btn-close btn-close-white btn-sm" @click="showModal=false"></button>
      </div>
      <div v-if="formError" class="alert alert-danger small py-2 mb-2">{{ formError }}</div>

      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.variantLabel') }}</label>
        <SearchSelect v-model="form.bienTheId" :options="variantOptions" :placeholder="t('admin.serialManager.variantPlaceholder')" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.serialLabel') }}</label>
        <input v-model="form.soSerial" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="trong_kho">{{ t('admin.statusLabel.trong_kho') }}</option>
          <option value="giu_hang">{{ t('admin.statusLabel.giu_hang') }}</option>
          <option value="da_ban">{{ t('admin.statusLabel.da_ban') }}</option>
          <option value="loi_bao_hanh">{{ t('admin.statusLabel.loi_bao_hanh') }}</option>
          <option value="da_tra_hang">{{ t('admin.statusLabel.da_tra_hang') }}</option>
        </select>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.dateLabel') }}</label>
        <input v-model="form.ngayNhapKho" type="datetime-local" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.noteLabel') }}</label>
        <input v-model="form.ghiChu" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-sm btn-outline-secondary" @click="showModal=false">{{ t('admin.serialManager.cancel') }}</button>
        <button class="btn btn-sm btn-warning text-dark fw-bold" @click="saveSerial">{{ t('admin.serialManager.save') }}</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Kiểm tra thủ công**

Không có test framework frontend. Kiểm tra bằng dev server sau khi Task 7/8 gắn điểm mount (xem Step kiểm tra ở Task 7).

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/SerialManager.vue
git commit -m "feat: add SerialManager component for full serial CRUD"
```

---

### Task 7: Gắn vào `AdminPage.vue` — sub-tab mới trong trang Sản phẩm

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `DmCategoryTable.vue` (Task 5), `SerialManager.vue` (Task 6), `DmService.DmCpuService/DmRamService/DmGpuService/DmOCungService` (Task 3, đã import sẵn `import * as DmService from "../Service/DmService.js"` ở dòng 9 — không cần import mới), key i18n `admin.productsTabs.*` (Task 4).

- [ ] **Step 1: Thêm import 2 component mới**

Sau dòng `import WarrantyPanel from "../components/admin/WarrantyPanel.vue";` (dòng 31), thêm:

```javascript
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
import DmCategoryTable from "../components/admin/DmCategoryTable.vue";
import SerialManager from "../components/admin/SerialManager.vue";
```

- [ ] **Step 2: Thêm ref `productsMainTab`**

Sau dòng `const inventoryMainTab = ref('kho');` (dòng 436), thêm:

```javascript
const inventoryMainTab = ref('kho');
const productsMainTab = ref('sanPham');
```

- [ ] **Step 3: Thay section "San pham" bằng sub-tab switcher**

Tìm block hiện tại:
```html
        <!-- ── San pham ── -->
        <section v-show="currentPage === 'products'">
          <ProductsTable />
        </section>
```

Thay bằng:
```html
        <!-- ── San pham ── -->
        <section v-show="currentPage === 'products'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='sanPham'}" @click="productsMainTab='sanPham'">{{ t('admin.productsTabs.sanPham') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='cpu'}" @click="productsMainTab='cpu'">{{ t('admin.productsTabs.cpu') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='ram'}" @click="productsMainTab='ram'">{{ t('admin.productsTabs.ram') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='gpu'}" @click="productsMainTab='gpu'">{{ t('admin.productsTabs.gpu') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='oCung'}" @click="productsMainTab='oCung'">{{ t('admin.productsTabs.oCung') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='serial'}" @click="productsMainTab='serial'">{{ t('admin.productsTabs.serial') }}</button></li>
          </ul>

          <div v-show="productsMainTab==='sanPham'">
            <ProductsTable />
          </div>
          <div v-show="productsMainTab==='cpu'">
            <DmCategoryTable :service="DmService.DmCpuService" id-field="cpuId" name-field="tenCpu" :label="t('admin.productsTabs.cpu')" :name-label="t('admin.productsTabs.cpu')" />
          </div>
          <div v-show="productsMainTab==='ram'">
            <DmCategoryTable :service="DmService.DmRamService" id-field="ramId" name-field="dungLuong" :label="t('admin.productsTabs.ram')" :name-label="t('admin.productsTabs.ram')" />
          </div>
          <div v-show="productsMainTab==='gpu'">
            <DmCategoryTable :service="DmService.DmGpuService" id-field="gpuId" name-field="tenGpu" :label="t('admin.productsTabs.gpu')" :name-label="t('admin.productsTabs.gpu')" />
          </div>
          <div v-show="productsMainTab==='oCung'">
            <DmCategoryTable :service="DmService.DmOCungService" id-field="oCungId" name-field="loaiOcung" :label="t('admin.productsTabs.oCung')" :name-label="t('admin.productsTabs.oCung')" />
          </div>
          <div v-show="productsMainTab==='serial'">
            <SerialManager />
          </div>
        </section>
```

- [ ] **Step 4: Kiểm tra bằng dev server**

Run (PowerShell, từ thư mục `FrontEnd/QLBanMayTinh`):
```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Expected: log hiện `VITE ... ready in ...ms` không có lỗi biên dịch Vue/JS.

Sau đó mở trình duyệt thật tới URL dev server, đăng nhập tài khoản admin, vào "Sản phẩm" → xác nhận 6 sub-tab hiện ra, tab CPU/RAM/GPU/Ổ cứng cho thêm/sửa/xóa 1 mục, tab Serial cho thêm/sửa/xóa 1 serial (chọn biến thể qua ô tìm kiếm).

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat: add products sub-tabs for CPU/RAM/GPU/O-cung catalog + serial management"
```

---

### Task 8: Gắn vào `WarehouseManagementPage.vue` — mục nav mới "Serial"

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue`

**Interfaces:**
- Consumes: `SerialManager.vue` (Task 6), key i18n `admin.sidebar.serial` + `admin.pageMeta.serial.{title,sub}` (Task 4).

- [ ] **Step 1: Thêm import**

Sau dòng `import WarrantyPanel from "../components/admin/WarrantyPanel.vue";` (dòng 12), thêm:

```javascript
import WarrantyPanel from "../components/admin/WarrantyPanel.vue";
import SerialManager from "../components/admin/SerialManager.vue";
```

- [ ] **Step 2: Thêm entry vào `PAGE_META`**

Thay:
```javascript
const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  warrantyClaims: { titleKey: "admin.pageMeta.warrantyClaims.title", subKey: "admin.pageMeta.warrantyClaims.sub", icon: "🛡️" },
};
```
bằng:
```javascript
const PAGE_META = {
  inventory: { titleKey: "admin.pageMeta.inventory.title", subKey: "admin.pageMeta.inventory.sub", icon: "📦" },
  suppliers: { titleKey: "admin.pageMeta.suppliers.title", subKey: "admin.pageMeta.suppliers.sub", icon: "🚚" },
  inventoryHistory: { titleKey: "admin.pageMeta.inventoryHistory.title", subKey: "admin.pageMeta.inventoryHistory.sub", icon: "📜" },
  traHang: { titleKey: "admin.pageMeta.traHang.title", subKey: "admin.pageMeta.traHang.sub", icon: "↩️" },
  warrantyClaims: { titleKey: "admin.pageMeta.warrantyClaims.title", subKey: "admin.pageMeta.warrantyClaims.sub", icon: "🛡️" },
  serial: { titleKey: "admin.pageMeta.serial.title", subKey: "admin.pageMeta.serial.sub", icon: "🔢" },
};
```

- [ ] **Step 3: Thêm nav item**

Sau block:
```html
        <div class="adm-nav" :class="{active: currentPage==='warrantyClaims'}" @click="navigate('warrantyClaims')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.warrantyClaims') }}
        </div>
      </nav>
```

thay bằng:
```html
        <div class="adm-nav" :class="{active: currentPage==='warrantyClaims'}" @click="navigate('warrantyClaims')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.warrantyClaims') }}
        </div>
        <div class="adm-nav" :class="{active: currentPage==='serial'}" @click="navigate('serial')">
          <svg class="adm-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 6a1 1 0 011-1h12a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zm3 2a1 1 0 100 2h.01a1 1 0 100-2H6zm3 0a1 1 0 100 2h.01a1 1 0 100-2H9z" clip-rule="evenodd"/></svg>
          {{ t('admin.sidebar.serial') }}
        </div>
      </nav>
```

- [ ] **Step 4: Thêm section mount**

Thay:
```html
        <section v-show="currentPage === 'warrantyClaims'"><WarrantyPanel /></section>
      </div>
```
bằng:
```html
        <section v-show="currentPage === 'warrantyClaims'"><WarrantyPanel /></section>
        <section v-show="currentPage === 'serial'"><SerialManager /></section>
      </div>
```

- [ ] **Step 5: Kiểm tra bằng dev server**

Run (PowerShell, từ thư mục `FrontEnd/QLBanMayTinh`):
```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Expected: không có lỗi biên dịch.

Mở trình duyệt, đăng nhập tài khoản Kho (`QUAN_KHO`), vào trang Kho hàng → xác nhận mục nav "Serial" mới xuất hiện (không có 4 tab Dm* — đúng theo quyết định phân quyền), click vào → `SerialManager` hiện đầy đủ, thêm/sửa/xóa 1 serial thành công.

- [ ] **Step 6: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/WarehouseManagementPage.vue
git commit -m "feat: add Serial nav item to warehouse management page"
```

---

## Self-Review

**1. Spec coverage:**
- Khóa quyền 5 controller → Task 1 ✅
- Bổ sung `ghiChu` (kèm phát hiện thêm: entity cũng thiếu field, không chỉ Request/Response như spec giả định) → Task 2 ✅
- `DmService.js` CRUD → Task 3 ✅
- i18n 5 locale → Task 4 ✅
- `DmCategoryTable.vue` dùng chung → Task 5 ✅
- `SerialManager.vue` → Task 6 ✅
- Gắn `AdminPage.vue` (sub-tab Products) → Task 7 ✅
- Gắn `WarehouseManagementPage.vue` (nav Serial, không có Dm*) → Task 8 ✅

**2. Placeholder scan:** Không còn "TBD"/"tương tự Task N không code lại" — mọi step đều có code đầy đủ, kể cả 4 lần lặp lại nội dung dịch cho i18n (Task 4 Step 4-7) thay vì tham chiếu chéo.

**3. Type/field consistency:**
- `idField`/`nameField` dùng ở Task 7 (`cpuId`/`tenCpu`, `ramId`/`dungLuong`, `gpuId`/`tenGpu`, `oCungId`/`loaiOcung`) khớp chính xác với field thật trên entity (xác nhận qua grep `ProductsTable.vue` — code đang chạy thật dùng đúng casing này, đặc biệt `oCungId`/`loaiOcung` — không phải `OCungId`/`loaiOCung`).
- `ChiTietSanPhamResponse`/`Request` field `ghiChu` (Task 2) khớp với `form.ghiChu`/`item.ghiChu` dùng trong `SerialManager.vue` (Task 6).
- `props.service.save(id, body)` (Task 5) khớp đúng chữ ký `crud()` factory (Task 3: `save: (id, body) => ...`).
- `ChiTietSanPhamService.create/update` (2 hàm riêng, không phải `save()` gộp — đã xác nhận đọc file thật) — `SerialManager.vue` (Task 6) gọi đúng 2 hàm riêng này, không gọi nhầm `.save()` không tồn tại.
- `variantOptions`/`SearchSelect` props (`modelValue`/`options: [{value,label}]`) khớp đúng props thật của `SearchSelect.vue` đã đọc.

**4. Idempotency:** Không có thay đổi SQL/schema nào trong plan này — chỉ Java/Vue, không cần section mới trong `QLBanMayTinh.sql`.
