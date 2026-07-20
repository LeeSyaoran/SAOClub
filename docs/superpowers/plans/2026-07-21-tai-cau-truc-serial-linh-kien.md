# Tái cấu trúc Quản lý Serial + Serial linh kiện CPU/RAM/GPU/Ổ cứng Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm CRUD serial nội bộ cho CPU/RAM/GPU/Ổ cứng (bắt buộc nhập serial lúc tạo mới spec, đơn/nhiều/import Excel), mở rộng `SerialManager.vue` thành nơi thêm serial cho CẢ sản phẩm lẫn linh kiện qua 1 bộ chọn "Loại", và di chuyển sub-tab "Serial" từ trang Sản phẩm sang trang Kho hàng (cạnh Bảo hành).

**Architecture:** 4 bảng DB mới (`chi_tiet_cpu`/`ram`/`gpu`/`o_cung`, đã áp dụng vào `Database/QLBanMayTinh.sql`) theo đúng khuôn `chi_tiet_san_pham` nhưng trạng thái rút gọn 3 mức (không bán rời). Backend: 4 entity/request/response/repository/service/controller mới, đúng khuôn `ChiTietSanPham*`. Frontend: 1 service file mới gộp CRUD 4 loại linh kiện, `DmCategoryTable.vue` thêm phần nhập serial bắt buộc lúc tạo (tái dùng UI có sẵn của `InventoryPanel.vue`), `SerialManager.vue` thêm dropdown "Loại" + gộp dữ liệu 5 nguồn (sản phẩm + 4 linh kiện) ở tầng frontend.

**Tech Stack:** Spring Boot / JPA (Hibernate) / SQL Server backend, Vue 3 `<script setup>` frontend, thư viện `xlsx` (đã có sẵn trong `package.json`) để đọc file Excel, JUnit 5 + AssertJ cho backend test.

## Global Constraints

- **Schema DB đã áp dụng thật** vào `Database/QLBanMayTinh.sql` (không cần task SQL trong plan này) — 4 bảng: `chi_tiet_cpu` (PK `chi_tiet_cpu_id`, FK `cpu_id` → `dm_cpu`), `chi_tiet_ram` (PK `chi_tiet_ram_id`, FK `ram_id` → `dm_ram`), `chi_tiet_gpu` (PK `chi_tiet_gpu_id`, FK `gpu_id` → `dm_gpu`), `chi_tiet_o_cung` (PK `chi_tiet_o_cung_id`, FK `o_cung_id` → `dm_o_cung`) — mỗi bảng có `so_serial VARCHAR(100) NOT NULL` (UNIQUE INDEX riêng từng bảng), `trang_thai NVARCHAR(30) NOT NULL DEFAULT N'trong_kho' CHECK IN (N'trong_kho', N'da_su_dung', N'loi_bao_hanh')`, `ngay_nhap_kho DATETIME NOT NULL DEFAULT GETDATE()`, `ghi_chu NVARCHAR(255) NULL`.
- Trạng thái linh kiện CHỈ có 3 mức: `trong_kho`/`da_su_dung`/`loi_bao_hanh` — KHÔNG dùng `giu_hang`/`da_ban`/`da_tra_hang` (không bán rời linh kiện).
- Quyền backend cho 4 controller mới: `@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")` — đúng khuôn `ChiTietSanPhamController`, KHÔNG phải Admin-only như `Dm*Controller`.
- Serial linh kiện chỉ bắt buộc nhập lúc TẠO MỚI spec (modal Thêm của `DmCategoryTable.vue`) — modal Sửa không đụng tới serial.
- Bảng danh sách CPU/RAM/GPU/Ổ cứng (`DmCategoryTable.vue`) KHÔNG hiện serial/tồn kho — giữ nguyên chỉ 1 cột tên.
- Không liên kết serial linh kiện với 1 serial sản phẩm cụ thể (không FK, không truy vết "lắp vào máy nào").
- `WarehouseManagementPage.vue` KHÔNG đổi — Serial vẫn là nav top-level riêng.
- Mọi `LocalDateTime`/`ngayNhapKho` gửi lên backend dùng `nowLocalIso()` (từ `utils/datetime.js`), không dùng `new Date().toISOString()`.
- Windows/PowerShell: khi build backend, `$env:JAVA_HOME = $env:JAVA_HOME.Trim('"')` phải chạy CÙNG lệnh với `mvnw.cmd`; không dùng `2>&1` với `mvnw.cmd` trên PowerShell. Test frontend dev-server dùng PowerShell, không dùng Git Bash.

---

### Task 1: Backend — 4 slice CRUD serial linh kiện (ChiTietCpu/Ram/Gpu/Ocung)

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/entity/ChiTietCpu.java`
- Create: `BackEnd/src/main/java/com/example/backend/entity/ChiTietRam.java`
- Create: `BackEnd/src/main/java/com/example/backend/entity/ChiTietGpu.java`
- Create: `BackEnd/src/main/java/com/example/backend/entity/ChiTietOcung.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/ChiTietCpuRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/ChiTietRamRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/ChiTietGpuRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/request/ChiTietOcungRequest.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/ChiTietCpuResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/ChiTietRamResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/ChiTietGpuResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/ChiTietOcungResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/ChiTietCpuRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/ChiTietRamRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/ChiTietGpuRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/repository/ChiTietOcungRepository.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/ChiTietCpuService.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/ChiTietRamService.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/ChiTietGpuService.java`
- Create: `BackEnd/src/main/java/com/example/backend/service/ChiTietOcungService.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/ChiTietCpuController.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/ChiTietRamController.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/ChiTietGpuController.java`
- Create: `BackEnd/src/main/java/com/example/backend/controller/ChiTietOcungController.java`
- Test: `BackEnd/src/test/java/com/example/backend/controller/ChiTietLinhKienAuthorizationTest.java`

**Interfaces:**
- Consumes: `DmCpuRepository`/`DmRamRepository`/`DmGpuRepository`/`DmOcungRepository` (đã có sẵn, `JpaRepository<DmCpu,Integer>` v.v., dùng `getReferenceById(id)`).
- Produces: 4 endpoint REST — `GET/POST /api/chi-tiet-cpu`, `PUT /api/chi-tiet-cpu/update/{id}`, `DELETE /api/chi-tiet-cpu/delete/{id}` (và tương tự `chi-tiet-ram`/`chi-tiet-gpu`/`chi-tiet-o-cung`) — Task 2 (frontend service) gọi các endpoint này. Response JSON mỗi loại có field phẳng: CPU→`{chiTietCpuId, cpuId, tenCpu, soSerial, trangThai, ngayNhapKho, ghiChu}`, RAM→`{chiTietRamId, ramId, dungLuong, ...}`, GPU→`{chiTietGpuId, gpuId, tenGpu, ...}`, Ổ cứng→`{chiTietOCungId, oCungId, loaiOcung, ...}` — Task 5 (`SerialManager.vue`) đọc đúng các field name này để hiển thị/gộp bảng.

- [ ] **Step 1: Entity `ChiTietCpu`**

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
@Table(name = "chi_tiet_cpu")
public class ChiTietCpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_cpu_id")
    private Integer chiTietCpuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id", nullable = false)
    private DmCpu cpu;

    @Column(name = "so_serial", length = 100, unique = true)
    private String soSerial;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
```

- [ ] **Step 2: Entity `ChiTietRam`, `ChiTietGpu`, `ChiTietOcung`**

`ChiTietRam.java`:
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
@Table(name = "chi_tiet_ram")
public class ChiTietRam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_ram_id")
    private Integer chiTietRamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ram_id", nullable = false)
    private DmRam ram;

    @Column(name = "so_serial", length = 100, unique = true)
    private String soSerial;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
```

`ChiTietGpu.java`:
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
@Table(name = "chi_tiet_gpu")
public class ChiTietGpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_gpu_id")
    private Integer chiTietGpuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id", nullable = false)
    private DmGpu gpu;

    @Column(name = "so_serial", length = 100, unique = true)
    private String soSerial;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
```

`ChiTietOcung.java`:
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
@Table(name = "chi_tiet_o_cung")
public class ChiTietOcung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chi_tiet_o_cung_id")
    private Integer chiTietOCungId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "o_cung_id", nullable = false)
    private DmOcung oCung;

    @Column(name = "so_serial", length = 100, unique = true)
    private String soSerial;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "ngay_nhap_kho")
    private LocalDateTime ngayNhapKho;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
```

- [ ] **Step 3: Request DTO — 4 file**

`ChiTietCpuRequest.java`:
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
public class ChiTietCpuRequest {
    @NotNull(message = "CPU không được để trống")
    private Integer cpuId;

    @NotBlank(message = "Số serial không được để trống")
    private String soSerial;

    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

`ChiTietRamRequest.java`:
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
public class ChiTietRamRequest {
    @NotNull(message = "RAM không được để trống")
    private Integer ramId;

    @NotBlank(message = "Số serial không được để trống")
    private String soSerial;

    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

`ChiTietGpuRequest.java`:
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
public class ChiTietGpuRequest {
    @NotNull(message = "GPU không được để trống")
    private Integer gpuId;

    @NotBlank(message = "Số serial không được để trống")
    private String soSerial;

    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

`ChiTietOcungRequest.java`:
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
public class ChiTietOcungRequest {
    @NotNull(message = "Ổ cứng không được để trống")
    private Integer oCungId;

    @NotBlank(message = "Số serial không được để trống")
    private String soSerial;

    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

- [ ] **Step 4: Response DTO — 4 file**

`ChiTietCpuResponse.java`:
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
public class ChiTietCpuResponse {
    private Integer chiTietCpuId;
    private Integer cpuId;
    private String tenCpu;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

`ChiTietRamResponse.java`:
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
public class ChiTietRamResponse {
    private Integer chiTietRamId;
    private Integer ramId;
    private String dungLuong;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

`ChiTietGpuResponse.java`:
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
public class ChiTietGpuResponse {
    private Integer chiTietGpuId;
    private Integer gpuId;
    private String tenGpu;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

`ChiTietOcungResponse.java`:
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
public class ChiTietOcungResponse {
    private Integer chiTietOCungId;
    private Integer oCungId;
    private String loaiOcung;
    private String soSerial;
    private String trangThai;
    private LocalDateTime ngayNhapKho;
    private String ghiChu;
}
```

- [ ] **Step 5: Repository — 4 file**

`ChiTietCpuRepository.java`:
```java
package com.example.backend.repository;

import com.example.backend.entity.ChiTietCpu;
import com.example.backend.response.ChiTietCpuResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietCpuRepository extends JpaRepository<ChiTietCpu, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietCpuResponse(c.chiTietCpuId, c.cpu.cpuId, c.cpu.tenCpu, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietCpu c")
    List<ChiTietCpuResponse> hienThiChiTietCpu();
}
```

`ChiTietRamRepository.java`:
```java
package com.example.backend.repository;

import com.example.backend.entity.ChiTietRam;
import com.example.backend.response.ChiTietRamResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietRamRepository extends JpaRepository<ChiTietRam, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietRamResponse(c.chiTietRamId, c.ram.ramId, c.ram.dungLuong, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietRam c")
    List<ChiTietRamResponse> hienThiChiTietRam();
}
```

`ChiTietGpuRepository.java`:
```java
package com.example.backend.repository;

import com.example.backend.entity.ChiTietGpu;
import com.example.backend.response.ChiTietGpuResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietGpuRepository extends JpaRepository<ChiTietGpu, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietGpuResponse(c.chiTietGpuId, c.gpu.gpuId, c.gpu.tenGpu, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietGpu c")
    List<ChiTietGpuResponse> hienThiChiTietGpu();
}
```

`ChiTietOcungRepository.java`:
```java
package com.example.backend.repository;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.response.ChiTietOcungResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietOcungRepository extends JpaRepository<ChiTietOcung, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietOcungResponse(c.chiTietOCungId, c.oCung.oCungId, c.oCung.loaiOcung, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietOcung c")
    List<ChiTietOcungResponse> hienThiChiTietOcung();
}
```

- [ ] **Step 6: Service — 4 file**

`ChiTietCpuService.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.ChiTietCpu;
import com.example.backend.repository.ChiTietCpuRepository;
import com.example.backend.repository.DmCpuRepository;
import com.example.backend.request.ChiTietCpuRequest;
import com.example.backend.response.ChiTietCpuResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietCpuService {

    @Autowired
    private ChiTietCpuRepository chiTietCpuRepository;
    @Autowired
    private DmCpuRepository dmCpuRepository;

    public List<ChiTietCpuResponse> hienThiChiTietCpu() {
        return chiTietCpuRepository.hienThiChiTietCpu();
    }

    public ChiTietCpu getById(Integer id) {
        return chiTietCpuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial CPU không tồn tại với id: " + id));
    }

    public ChiTietCpu create(ChiTietCpuRequest request) {
        ChiTietCpu entity = new ChiTietCpu();
        BeanUtils.copyProperties(request, entity, "cpuId");
        entity.setCpu(dmCpuRepository.getReferenceById(request.getCpuId()));
        return chiTietCpuRepository.save(entity);
    }

    public ChiTietCpu update(Integer id, ChiTietCpuRequest request) {
        ChiTietCpu entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietCpuId", "cpuId");
        entity.setCpu(dmCpuRepository.getReferenceById(request.getCpuId()));
        return chiTietCpuRepository.save(entity);
    }

    // Chỉ cho xóa serial đang "trong_kho" (thêm nhầm) — đã dùng/lỗi bảo hành mà xóa sẽ
    // làm sai lịch sử nhập kho đã ghi nhận.
    public void delete(Integer id) {
        ChiTietCpu entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietCpuRepository.deleteById(id);
    }
}
```

`ChiTietRamService.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.ChiTietRam;
import com.example.backend.repository.ChiTietRamRepository;
import com.example.backend.repository.DmRamRepository;
import com.example.backend.request.ChiTietRamRequest;
import com.example.backend.response.ChiTietRamResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietRamService {

    @Autowired
    private ChiTietRamRepository chiTietRamRepository;
    @Autowired
    private DmRamRepository dmRamRepository;

    public List<ChiTietRamResponse> hienThiChiTietRam() {
        return chiTietRamRepository.hienThiChiTietRam();
    }

    public ChiTietRam getById(Integer id) {
        return chiTietRamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial RAM không tồn tại với id: " + id));
    }

    public ChiTietRam create(ChiTietRamRequest request) {
        ChiTietRam entity = new ChiTietRam();
        BeanUtils.copyProperties(request, entity, "ramId");
        entity.setRam(dmRamRepository.getReferenceById(request.getRamId()));
        return chiTietRamRepository.save(entity);
    }

    public ChiTietRam update(Integer id, ChiTietRamRequest request) {
        ChiTietRam entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietRamId", "ramId");
        entity.setRam(dmRamRepository.getReferenceById(request.getRamId()));
        return chiTietRamRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietRam entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietRamRepository.deleteById(id);
    }
}
```

`ChiTietGpuService.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.ChiTietGpu;
import com.example.backend.repository.ChiTietGpuRepository;
import com.example.backend.repository.DmGpuRepository;
import com.example.backend.request.ChiTietGpuRequest;
import com.example.backend.response.ChiTietGpuResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietGpuService {

    @Autowired
    private ChiTietGpuRepository chiTietGpuRepository;
    @Autowired
    private DmGpuRepository dmGpuRepository;

    public List<ChiTietGpuResponse> hienThiChiTietGpu() {
        return chiTietGpuRepository.hienThiChiTietGpu();
    }

    public ChiTietGpu getById(Integer id) {
        return chiTietGpuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial GPU không tồn tại với id: " + id));
    }

    public ChiTietGpu create(ChiTietGpuRequest request) {
        ChiTietGpu entity = new ChiTietGpu();
        BeanUtils.copyProperties(request, entity, "gpuId");
        entity.setGpu(dmGpuRepository.getReferenceById(request.getGpuId()));
        return chiTietGpuRepository.save(entity);
    }

    public ChiTietGpu update(Integer id, ChiTietGpuRequest request) {
        ChiTietGpu entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietGpuId", "gpuId");
        entity.setGpu(dmGpuRepository.getReferenceById(request.getGpuId()));
        return chiTietGpuRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietGpu entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietGpuRepository.deleteById(id);
    }
}
```

`ChiTietOcungService.java`:
```java
package com.example.backend.service;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.repository.ChiTietOcungRepository;
import com.example.backend.repository.DmOcungRepository;
import com.example.backend.request.ChiTietOcungRequest;
import com.example.backend.response.ChiTietOcungResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChiTietOcungService {

    @Autowired
    private ChiTietOcungRepository chiTietOcungRepository;
    @Autowired
    private DmOcungRepository dmOcungRepository;

    public List<ChiTietOcungResponse> hienThiChiTietOcung() {
        return chiTietOcungRepository.hienThiChiTietOcung();
    }

    public ChiTietOcung getById(Integer id) {
        return chiTietOcungRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serial ổ cứng không tồn tại với id: " + id));
    }

    public ChiTietOcung create(ChiTietOcungRequest request) {
        ChiTietOcung entity = new ChiTietOcung();
        BeanUtils.copyProperties(request, entity, "oCungId");
        entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        return chiTietOcungRepository.save(entity);
    }

    public ChiTietOcung update(Integer id, ChiTietOcungRequest request) {
        ChiTietOcung entity = getById(id);
        BeanUtils.copyProperties(request, entity, "chiTietOCungId", "oCungId");
        entity.setOCung(dmOcungRepository.getReferenceById(request.getOCungId()));
        return chiTietOcungRepository.save(entity);
    }

    public void delete(Integer id) {
        ChiTietOcung entity = getById(id);
        if (!"trong_kho".equals(entity.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ được xóa serial đang ở trạng thái \"Trong kho\" (chưa dùng)");
        }
        chiTietOcungRepository.deleteById(id);
    }
}
```

**Lưu ý quan trọng cho `ChiTietOcungService`:** Lombok `@Data`/`@Setter` trên entity `ChiTietOcung` sinh setter theo tên field Java — field là `oCung` (chữ O hoa, chữ C hoa) nên setter sinh ra là `setOCung(...)` (Lombok viết hoa ký tự đầu field, giữ nguyên phần còn lại: `oCung` → `OCung` → `setOCung`). Đã dùng đúng `entity.setOCung(...)` ở trên — nếu build lỗi "cannot find symbol setOCung", kiểm tra lại field entity có đúng tên `oCung` không.

- [ ] **Step 7: Controller — 4 file**

`ChiTietCpuController.java`:
```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietCpu;
import com.example.backend.request.ChiTietCpuRequest;
import com.example.backend.response.ChiTietCpuResponse;
import com.example.backend.service.ChiTietCpuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-cpu")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietCpuController {

    @Autowired
    private ChiTietCpuService chiTietCpuService;

    @GetMapping
    public List<ChiTietCpuResponse> getAll() {
        return chiTietCpuService.hienThiChiTietCpu();
    }

    @GetMapping("/{id}")
    public ChiTietCpu getById(@PathVariable Integer id) {
        return chiTietCpuService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietCpuRequest request) {
        chiTietCpuService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietCpuRequest request) {
        chiTietCpuService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietCpuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

`ChiTietRamController.java`:
```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietRam;
import com.example.backend.request.ChiTietRamRequest;
import com.example.backend.response.ChiTietRamResponse;
import com.example.backend.service.ChiTietRamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-ram")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietRamController {

    @Autowired
    private ChiTietRamService chiTietRamService;

    @GetMapping
    public List<ChiTietRamResponse> getAll() {
        return chiTietRamService.hienThiChiTietRam();
    }

    @GetMapping("/{id}")
    public ChiTietRam getById(@PathVariable Integer id) {
        return chiTietRamService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietRamRequest request) {
        chiTietRamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietRamRequest request) {
        chiTietRamService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietRamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

`ChiTietGpuController.java`:
```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietGpu;
import com.example.backend.request.ChiTietGpuRequest;
import com.example.backend.response.ChiTietGpuResponse;
import com.example.backend.service.ChiTietGpuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-gpu")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietGpuController {

    @Autowired
    private ChiTietGpuService chiTietGpuService;

    @GetMapping
    public List<ChiTietGpuResponse> getAll() {
        return chiTietGpuService.hienThiChiTietGpu();
    }

    @GetMapping("/{id}")
    public ChiTietGpu getById(@PathVariable Integer id) {
        return chiTietGpuService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietGpuRequest request) {
        chiTietGpuService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietGpuRequest request) {
        chiTietGpuService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietGpuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

`ChiTietOcungController.java`:
```java
package com.example.backend.controller;

import com.example.backend.entity.ChiTietOcung;
import com.example.backend.request.ChiTietOcungRequest;
import com.example.backend.response.ChiTietOcungResponse;
import com.example.backend.service.ChiTietOcungService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chi-tiet-o-cung")
@PreAuthorize("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')")
public class ChiTietOcungController {

    @Autowired
    private ChiTietOcungService chiTietOcungService;

    @GetMapping
    public List<ChiTietOcungResponse> getAll() {
        return chiTietOcungService.hienThiChiTietOcung();
    }

    @GetMapping("/{id}")
    public ChiTietOcung getById(@PathVariable Integer id) {
        return chiTietOcungService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ChiTietOcungRequest request) {
        chiTietOcungService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody ChiTietOcungRequest request) {
        chiTietOcungService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        chiTietOcungService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 8: Test quyền — xác nhận `@PreAuthorize` trên 4 controller**

Tạo `BackEnd/src/test/java/com/example/backend/controller/ChiTietLinhKienAuthorizationTest.java`:

```java
package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// 4 controller nay la moi hoan toan (Task 1 cua plan tai cau truc serial linh kien) —
// khoa dung khuon ChiTietSanPhamController vi Kho can them serial linh kien qua
// SerialManager cho spec da co san.
class ChiTietLinhKienAuthorizationTest {

    @Test
    void chiTietCpuController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietCpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietRamController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietRamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietGpuController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietGpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietOcungController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietOcungController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
```

- [ ] **Step 9: Build + chạy test**

Run (PowerShell, từ thư mục `BackEnd`):
```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); .\mvnw.cmd test "-Dtest=ChiTietLinhKienAuthorizationTest"
```
Expected: `Tests run: 4, Failures: 0, Errors: 0`

Run thêm để chắc chắn toàn bộ project vẫn compile (Response DTO/JPQL sai cú pháp sẽ lộ ra ở đây, khác lỗi runtime):
```powershell
$env:JAVA_HOME = $env:JAVA_HOME.Trim('"'); .\mvnw.cmd compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 10: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/entity/ChiTietCpu.java BackEnd/src/main/java/com/example/backend/entity/ChiTietRam.java BackEnd/src/main/java/com/example/backend/entity/ChiTietGpu.java BackEnd/src/main/java/com/example/backend/entity/ChiTietOcung.java BackEnd/src/main/java/com/example/backend/request/ChiTietCpuRequest.java BackEnd/src/main/java/com/example/backend/request/ChiTietRamRequest.java BackEnd/src/main/java/com/example/backend/request/ChiTietGpuRequest.java BackEnd/src/main/java/com/example/backend/request/ChiTietOcungRequest.java BackEnd/src/main/java/com/example/backend/response/ChiTietCpuResponse.java BackEnd/src/main/java/com/example/backend/response/ChiTietRamResponse.java BackEnd/src/main/java/com/example/backend/response/ChiTietGpuResponse.java BackEnd/src/main/java/com/example/backend/response/ChiTietOcungResponse.java BackEnd/src/main/java/com/example/backend/repository/ChiTietCpuRepository.java BackEnd/src/main/java/com/example/backend/repository/ChiTietRamRepository.java BackEnd/src/main/java/com/example/backend/repository/ChiTietGpuRepository.java BackEnd/src/main/java/com/example/backend/repository/ChiTietOcungRepository.java BackEnd/src/main/java/com/example/backend/service/ChiTietCpuService.java BackEnd/src/main/java/com/example/backend/service/ChiTietRamService.java BackEnd/src/main/java/com/example/backend/service/ChiTietGpuService.java BackEnd/src/main/java/com/example/backend/service/ChiTietOcungService.java BackEnd/src/main/java/com/example/backend/controller/ChiTietCpuController.java BackEnd/src/main/java/com/example/backend/controller/ChiTietRamController.java BackEnd/src/main/java/com/example/backend/controller/ChiTietGpuController.java BackEnd/src/main/java/com/example/backend/controller/ChiTietOcungController.java BackEnd/src/test/java/com/example/backend/controller/ChiTietLinhKienAuthorizationTest.java
git commit -m "feat: add CRUD backend for CPU/RAM/GPU/O-cung component serials"
```

---

### Task 2: Frontend — `Service/ChiTietLinhKienService.js`

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/Service/ChiTietLinhKienService.js`

**Interfaces:**
- Consumes: Task 1's 4 endpoint set (`/api/chi-tiet-cpu`, `/api/chi-tiet-ram`, `/api/chi-tiet-gpu`, `/api/chi-tiet-o-cung`), `get/post/put/del` từ `Service/api.js` (đã có sẵn).
- Produces: `ChiTietCpuService`/`ChiTietRamService`/`ChiTietGpuService`/`ChiTietOCungService`, mỗi cái có `{ getAll(), create(body), update(id, body), remove(id) }` — Task 4 (`DmCategoryTable.vue`) dùng `.create()`, Task 5 (`SerialManager.vue`) dùng cả 4 hàm. Khớp đúng shape của `ChiTietSanPhamService.js` (KHÔNG dùng `save(id,body)` gộp như `DmService.js`'s `crud()`).

- [ ] **Step 1: Viết file**

```javascript
import { get, post, put, del } from './api.js';

const chiTietLinhKien = (path) => ({
  getAll: () => get(`/api/${path}`),
  create: (body) => post(`/api/${path}`, body),
  update: (id, body) => put(`/api/${path}/update/${id}`, body),
  remove: (id) => del(`/api/${path}/delete/${id}`),
});

export const ChiTietCpuService = chiTietLinhKien('chi-tiet-cpu');
export const ChiTietRamService = chiTietLinhKien('chi-tiet-ram');
export const ChiTietGpuService = chiTietLinhKien('chi-tiet-gpu');
export const ChiTietOCungService = chiTietLinhKien('chi-tiet-o-cung');
```

- [ ] **Step 2: Kiểm tra cú pháp**

Run (PowerShell, từ thư mục `FrontEnd/QLBanMayTinh`):
```powershell
node --input-type=module -e "import('./src/Service/ChiTietLinhKienService.js').then(()=>console.log('OK'))"
```
Expected: in ra `OK`.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/ChiTietLinhKienService.js
git commit -m "feat: add frontend service for component serial CRUD"
```

---

### Task 3: i18n — key mới cho 5 locale

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Đã xác nhận ĐÃ TỒN TẠI ở vi.js (dùng lại, KHÔNG thêm mới):** `admin.stockModal.newSerialsLabel`/`serialPlaceholder`/`addSerialRow`/`importFromFile`/`importHint` (dòng 1044-1048), `admin.statusLabel.trong_kho`/`loi_bao_hanh`, `admin.productsTabs.sanPham`/`cpu`/`ram`/`gpu`/`oCung`/`serial`.

**Interfaces:**
- Produces: `admin.statusLabel.da_su_dung`, `admin.serialManager.colLoai`, `admin.serialManager.specPlaceholder`, `admin.serialManager.specRequired`, `admin.dmCategory.serialRequired`, `admin.inventory.tabSerial` — dùng bởi Task 4 (`DmCategoryTable.vue`) và Task 5 (`SerialManager.vue`) và Task 6 (`AdminPage.vue`).

- [ ] **Step 1: `vi.js` — thêm `da_su_dung` vào `statusLabel`**

Tìm dòng:
```javascript
      loi_bao_hanh: "Bảo hành",
      da_tra_hang: "Đã trả hàng",
```
Thay bằng:
```javascript
      loi_bao_hanh: "Bảo hành",
      da_tra_hang: "Đã trả hàng",
      da_su_dung: "Đã sử dụng",
```

- [ ] **Step 2: `vi.js` — thêm `tabSerial` vào `inventory`**

Tìm dòng (cuối block `inventory:`):
```javascript
      tabWarranty: "Bảo hành",
```
Thay bằng:
```javascript
      tabWarranty: "Bảo hành",
      tabSerial: "Serial",
```

- [ ] **Step 3: `vi.js` — thêm key mới vào `serialManager`**

Tìm dòng:
```javascript
      serialRequired: "Vui lòng nhập số serial",
      variantRequired: "Vui lòng chọn sản phẩm/biến thể",
    },
```
Thay bằng:
```javascript
      serialRequired: "Vui lòng nhập số serial",
      variantRequired: "Vui lòng chọn sản phẩm/biến thể",
      colLoai: "Loại",
      specPlaceholder: "Chọn linh kiện...",
      specRequired: "Vui lòng chọn linh kiện",
    },
```

- [ ] **Step 4: `vi.js` — thêm `serialRequired` vào `dmCategory`**

Tìm dòng:
```javascript
      nameRequired: "Vui lòng nhập {label}",
    },
```
Thay bằng:
```javascript
      nameRequired: "Vui lòng nhập {label}",
      serialRequired: "Vui lòng nhập ít nhất 1 số serial cho {label} mới",
    },
```

- [ ] **Step 5: Lặp lại Step 1-4 cho `en.js` với nội dung dịch tiếng Anh**

`statusLabel` (anchor `da_tra_hang: "Returned",` hoặc giá trị tiếng Anh tương ứng đã có — tìm đúng dòng `da_tra_hang:` trong block `statusLabel` của `en.js` và thêm ngay sau):
```javascript
      da_su_dung: "Used",
```

`inventory` (anchor `tabWarranty: "Warranty",` hoặc giá trị đã có — thêm ngay sau):
```javascript
      tabSerial: "Serial",
```

`serialManager` (anchor `variantRequired: "Please select a product/variant",` hoặc giá trị đã có — thêm ngay sau):
```javascript
      colLoai: "Type",
      specPlaceholder: "Select component...",
      specRequired: "Please select a component",
```

`dmCategory` (anchor `nameRequired: "Please enter {label}",` — thêm ngay sau):
```javascript
      serialRequired: "Please enter at least 1 serial number for the new {label}",
```

- [ ] **Step 6: Lặp lại cho `zh.js` với nội dung dịch tiếng Trung**

`statusLabel`:
```javascript
      da_su_dung: "已使用",
```
`inventory`:
```javascript
      tabSerial: "序列号",
```
`serialManager`:
```javascript
      colLoai: "类型",
      specPlaceholder: "选择配件...",
      specRequired: "请选择配件",
```
`dmCategory`:
```javascript
      serialRequired: "请为新{label}至少输入1个序列号",
```

- [ ] **Step 7: Lặp lại cho `ko.js` với nội dung dịch tiếng Hàn**

`statusLabel`:
```javascript
      da_su_dung: "사용됨",
```
`inventory`:
```javascript
      tabSerial: "시리얼",
```
`serialManager`:
```javascript
      colLoai: "종류",
      specPlaceholder: "부품 선택...",
      specRequired: "부품을 선택해 주세요",
```
`dmCategory`:
```javascript
      serialRequired: "새 {label}에 시리얼 번호를 1개 이상 입력해 주세요",
```

- [ ] **Step 8: Lặp lại cho `ja.js` với nội dung dịch tiếng Nhật**

`statusLabel`:
```javascript
      da_su_dung: "使用済み",
```
`inventory`:
```javascript
      tabSerial: "シリアル",
```
`serialManager`:
```javascript
      colLoai: "種類",
      specPlaceholder: "パーツを選択...",
      specRequired: "パーツを選択してください",
```
`dmCategory`:
```javascript
      serialRequired: "新しい{label}にシリアル番号を1つ以上入力してください",
```

- [ ] **Step 9: Kiểm tra cú pháp JS hợp lệ cho cả 5 file**

Run (PowerShell, từ thư mục `FrontEnd/QLBanMayTinh`):
```powershell
node --input-type=module -e "import('./src/i18n/locales/vi.js').then(()=>console.log('OK vi'))"
node --input-type=module -e "import('./src/i18n/locales/en.js').then(()=>console.log('OK en'))"
node --input-type=module -e "import('./src/i18n/locales/zh.js').then(()=>console.log('OK zh'))"
node --input-type=module -e "import('./src/i18n/locales/ko.js').then(()=>console.log('OK ko'))"
node --input-type=module -e "import('./src/i18n/locales/ja.js').then(()=>console.log('OK ja'))"
```
Expected: mỗi lệnh in ra `OK <locale>`.

- [ ] **Step 10: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js FrontEnd/QLBanMayTinh/src/i18n/locales/en.js FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js
git commit -m "feat(i18n): add keys for component serial status, type selector, inventory serial tab"
```

---

### Task 4: `DmCategoryTable.vue` — bắt buộc nhập serial lúc tạo mới

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue`

**Interfaces:**
- Consumes: prop mới `serialService` (1 trong 4 object từ Task 2: `{create(body)}`), prop mới `serialFieldName` (String, tên field FK gửi vào body serial, vd `'cpuId'`), `XLSX` (npm package `xlsx`, đã có sẵn trong `package.json`), `nowLocalIso()` (`utils/datetime.js`).
- Produces: component nhận thêm 2 prop mới bên cạnh 5 prop cũ (`service`/`idField`/`nameField`/`label`/`nameLabel`) — Task 6 (`AdminPage.vue`) truyền đủ 7 prop khi gắn 4 lần cho CPU/RAM/GPU/Ổ cứng.

- [ ] **Step 1: Thêm import + 2 prop mới**

Thay:
```javascript
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
```
bằng:
```javascript
<script setup>
import { ref, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { nowLocalIso } from "../../utils/datetime.js";
import * as XLSX from "xlsx";

const props = defineProps({
  service: { type: Object, required: true },
  idField: { type: String, required: true },
  nameField: { type: String, required: true },
  label: { type: String, required: true },
  nameLabel: { type: String, required: true },
  serialService: { type: Object, required: true },
  serialFieldName: { type: String, required: true },
});
```

- [ ] **Step 2: Thêm state + hàm quản lý danh sách serial nhập mới**

Sau khối `const formValue = ref("");` (trước `const openAdd = ...`), thêm:

```javascript
const newSerials = ref(['']);
const addSerialRow = () => newSerials.value.push('');
const removeSerialRow = (idx) => {
  if (newSerials.value.length > 1) newSerials.value.splice(idx, 1);
  else newSerials.value[idx] = '';
};
// Nhập hàng loạt từ file — .xlsx/.xls đọc qua thư viện xlsx, .csv/.txt đọc thẳng dạng
// text (mỗi serial 1 dòng hoặc cách nhau bằng dấu phẩy) — tái dùng đúng cơ chế của
// InventoryPanel.vue's importSerialsFromFile.
const importSerialsFromFile = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  const ext = file.name.split('.').pop()?.toLowerCase();
  let parsed;
  if (ext === 'xlsx' || ext === 'xls') {
    const buf = await file.arrayBuffer();
    const wb = XLSX.read(buf, { type: 'array' });
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 });
    parsed = rows.flat().map((v) => String(v ?? '').trim()).filter(Boolean);
  } else {
    const text = await file.text();
    parsed = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
  }
  const existing = newSerials.value.filter(Boolean);
  newSerials.value = [...existing, ...parsed].length ? [...existing, ...parsed] : [''];
  e.target.value = '';
};
```

- [ ] **Step 3: Reset `newSerials` trong `openAdd`, không đụng trong `openEdit`**

Thay:
```javascript
const openAdd = () => {
  editingId.value = null;
  formValue.value = "";
  formError.value = "";
  showModal.value = true;
};
```
bằng:
```javascript
const openAdd = () => {
  editingId.value = null;
  formValue.value = "";
  formError.value = "";
  newSerials.value = [''];
  showModal.value = true;
};
```

(`openEdit` giữ nguyên không đổi — không có phần serial khi sửa.)

- [ ] **Step 4: Sửa `saveItem` — bắt buộc serial lúc tạo mới, tạo xong spec rồi tạo từng serial**

Thay toàn bộ hàm `saveItem`:
```javascript
const saveItem = async () => {
  formError.value = "";
  if (!formValue.value.trim()) {
    formError.value = t('admin.dmCategory.nameRequired', { label: props.nameLabel });
    return;
  }
  const serials = newSerials.value.map((s) => s.trim()).filter(Boolean);
  if (!editingId.value && serials.length === 0) {
    formError.value = t('admin.dmCategory.serialRequired', { label: props.nameLabel });
    return;
  }
  try {
    const body = { [props.nameField]: formValue.value.trim() };
    const res = await props.service.save(editingId.value, body);
    if (!res.ok) {
      formError.value = t('admin.errors.saveFailed', { status: res.status, text: await res.text() });
      return;
    }
    if (!editingId.value) {
      const created = await res.json();
      const newId = created[props.idField];
      for (const soSerial of serials) {
        const sres = await props.serialService.create({
          [props.serialFieldName]: newId,
          soSerial,
          trangThai: 'trong_kho',
          ngayNhapKho: nowLocalIso(),
        });
        if (!sres.ok) {
          showToast(await sres.text().catch(() => t('admin.errors.addSerialError')));
          return;
        }
      }
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.message;
  }
};
```

- [ ] **Step 5: Thêm phần UI nhập serial vào modal (chỉ hiện lúc tạo mới)**

Tìm block:
```html
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ nameLabel }}</label>
        <input v-model="formValue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" @keyup.enter="saveItem" />
      </div>
      <div class="d-flex justify-content-end gap-2">
```
Thay bằng:
```html
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ nameLabel }}</label>
        <input v-model="formValue" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" @keyup.enter="saveItem" />
      </div>
      <div v-if="!editingId" class="mb-3">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <label class="form-label small text-secondary mb-0">{{ t('admin.stockModal.newSerialsLabel') }}</label>
          <label class="btn btn-sm btn-outline-info" style="padding:2px 10px;font-size:0.72rem;cursor:pointer;">
            📂 {{ t('admin.stockModal.importFromFile') }}
            <input type="file" accept=".csv,.txt,.xlsx,.xls" class="d-none" @change="importSerialsFromFile" />
          </label>
        </div>
        <div class="d-flex flex-column gap-2">
          <div v-for="(s, idx) in newSerials" :key="idx" class="d-flex gap-2 align-items-center">
            <input v-model="newSerials[idx]" class="form-control form-control-sm" style="background:var(--bg-input); color:var(--text-primary); border-color:var(--border-color-strong)" :placeholder="t('admin.stockModal.serialPlaceholder')" />
            <button class="btn btn-sm btn-outline-danger" style="padding:2px 8px;" @click="removeSerialRow(idx)">✕</button>
          </div>
        </div>
        <button class="btn btn-sm btn-outline-warning mt-2" @click="addSerialRow">{{ t('admin.stockModal.addSerialRow') }}</button>
        <div class="text-secondary mt-1" style="font-size:0.72rem;">{{ t('admin.stockModal.importHint') }}</div>
      </div>
      <div class="d-flex justify-content-end gap-2">
```

- [ ] **Step 6: Kiểm tra thủ công**

Không có test framework frontend. Component không thể test độc lập tới khi Task 6 truyền đủ prop mới — review code diff bằng mắt, xác nhận `newSerials`/`addSerialRow`/`removeSerialRow`/`importSerialsFromFile` không xung đột tên biến nào khác trong file (grep nhanh trong chính file để chắc chắn).

- [ ] **Step 7: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue
git commit -m "feat: require serial entry (single/bulk/Excel import) when creating a new component"
```

---

### Task 5: `SerialManager.vue` — thêm bộ chọn "Loại" + gộp dữ liệu 5 nguồn

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/components/admin/SerialManager.vue`

**Interfaces:**
- Consumes: `ChiTietLinhKienService.{ChiTietCpuService,ChiTietRamService,ChiTietGpuService,ChiTietOCungService}` (Task 2), `DmService.{getCpu,getRam,getGpu,getOCung}` (đã có sẵn), key i18n `admin.serialManager.colLoai/specPlaceholder/specRequired` + `admin.statusLabel.da_su_dung` + `admin.productsTabs.*` (Task 3).
- Produces: không đổi interface bên ngoài (vẫn không props) — Task 6 tiếp tục gắn `<SerialManager />` không đổi cách gọi.

- [ ] **Step 1: Thêm import mới**

Thay:
```javascript
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
```
bằng:
```javascript
<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { t } from "../../i18n/index.js";
import * as ChiTietSanPhamService from "../../Service/ChiTietSanPhamService.js";
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../../Service/ChiTietLinhKienService.js";
import * as DmService from "../../Service/DmService.js";
import { formatDate } from "../../utils/adminFormat.js";
import { nowLocalIso } from "../../utils/datetime.js";
import { showToast } from "../../stores/toast.js";
import { askConfirm } from "../../stores/confirm.js";
import { ProductsStore, ensureProducts } from "../../stores/products.js";
import SearchSelect from "../common/SearchSelect.vue";
```

- [ ] **Step 2: Thêm cấu hình 4 loại linh kiện + state danh sách spec**

Sau dòng `import SearchSelect from "../common/SearchSelect.vue";` (đã sửa ở Step 1), trước `const items = ref([]);`, thêm:

```javascript
const specLists = reactive({ cpu: [], ram: [], gpu: [], oCung: [] });
const LINH_KIEN_META = {
  cpu:   { idField: 'cpuId',   nameField: 'tenCpu',    itemIdField: 'chiTietCpuId',   service: ChiTietCpuService },
  ram:   { idField: 'ramId',   nameField: 'dungLuong', itemIdField: 'chiTietRamId',   service: ChiTietRamService },
  gpu:   { idField: 'gpuId',   nameField: 'tenGpu',    itemIdField: 'chiTietGpuId',   service: ChiTietGpuService },
  oCung: { idField: 'oCungId', nameField: 'loaiOcung', itemIdField: 'chiTietOCungId', service: ChiTietOCungService },
};
```

- [ ] **Step 3: Sửa `load()` — gộp 5 nguồn dữ liệu**

Thay:
```javascript
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
```
bằng:
```javascript
const items = ref([]);
const loading = ref(false);
const search = ref("");

const load = async () => {
  loading.value = true;
  try {
    const [sp, cpu, ram, gpu, oCung] = await Promise.all([
      ChiTietSanPhamService.getAll().catch(() => []),
      ChiTietCpuService.getAll().catch(() => []),
      ChiTietRamService.getAll().catch(() => []),
      ChiTietGpuService.getAll().catch(() => []),
      ChiTietOCungService.getAll().catch(() => []),
    ]);
    items.value = [
      ...sp.map((i) => ({ ...i, loai: 'sanPham', rowId: i.chiTietId })),
      ...cpu.map((i) => ({ ...i, loai: 'cpu', rowId: i.chiTietCpuId })),
      ...ram.map((i) => ({ ...i, loai: 'ram', rowId: i.chiTietRamId })),
      ...gpu.map((i) => ({ ...i, loai: 'gpu', rowId: i.chiTietGpuId })),
      ...oCung.map((i) => ({ ...i, loai: 'oCung', rowId: i.chiTietOCungId })),
    ];
  } finally {
    loading.value = false;
  }
};
onMounted(() => {
  load();
  ensureProducts();
  DmService.getCpu().then((l) => { specLists.cpu = l; }).catch(() => {});
  DmService.getRam().then((l) => { specLists.ram = l; }).catch(() => {});
  DmService.getGpu().then((l) => { specLists.gpu = l; }).catch(() => {});
  DmService.getOCung().then((l) => { specLists.oCung = l; }).catch(() => {});
});
```

- [ ] **Step 4: Thêm hàm hiển thị tên dòng theo loại + sửa `filteredItems`**

Thay:
```javascript
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
```
bằng:
```javascript
const variantOptions = computed(() =>
  ProductsStore.items.map((p) => ({ value: p.bienTheId, label: `${p.tenSanPham} — ${p.maSku}` }))
);
const variantLabel = (bienTheId) => variantOptions.value.find((o) => o.value === bienTheId)?.label ?? '';

// Nhãn hiển thị cột "Sản phẩm/SKU" cho MỌI loại dòng (sản phẩm lẫn linh kiện) —
// linh kiện đã có sẵn tên spec (tenCpu/dungLuong/...) ngay trong response, không cần
// tra cứu thêm.
const rowSpecLabel = (item) => {
  if (item.loai === 'sanPham') return variantLabel(item.bienTheId) || item.maSku;
  const meta = LINH_KIEN_META[item.loai];
  return meta ? item[meta.nameField] : '';
};

const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return items.value;
  return items.value.filter((i) =>
    [i.soSerial, rowSpecLabel(i)].some((v) => (v || '').toLowerCase().includes(q))
  );
});
```

- [ ] **Step 5: Sửa `emptyForm`/`openAdd`/`openEdit` — dùng `specId` chung + `loai`, thêm `statusOptions`/`specOptions`**

Thay:
```javascript
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
```
bằng:
```javascript
const showModal = ref(false);
const editingId = ref(null);
const formError = ref("");
const emptyForm = () => ({
  loai: 'sanPham',
  specId: '',
  soSerial: '',
  trangThai: 'trong_kho',
  ngayNhapKho: nowLocalIso().slice(0, 16),
  ghiChu: '',
});
const form = ref(emptyForm());
const STATUS_OPTIONS_SAN_PHAM = ['trong_kho', 'giu_hang', 'da_ban', 'loi_bao_hanh', 'da_tra_hang'];
const STATUS_OPTIONS_LINH_KIEN = ['trong_kho', 'da_su_dung', 'loi_bao_hanh'];
const statusOptions = computed(() =>
  form.value.loai === 'sanPham' ? STATUS_OPTIONS_SAN_PHAM : STATUS_OPTIONS_LINH_KIEN
);
// Đổi Loại (người dùng bấm chọn trong modal) → trạng thái/spec cũ có thể không hợp lệ
// với loại mới, reset về mặc định. Gắn vào @change của <select> (xem Step 8), KHÔNG
// dùng watch(() => form.value.loai) — watch sẽ fire cả lúc openEdit() gán nguyên object
// form mới (loai đổi từ giá trị cũ sang item.loai), xoá mất specId/trangThai vừa set.
const onLoaiChange = () => {
  form.value.trangThai = 'trong_kho';
  form.value.specId = '';
};

const specOptions = computed(() => {
  if (form.value.loai === 'sanPham') return variantOptions.value;
  const meta = LINH_KIEN_META[form.value.loai];
  if (!meta) return [];
  return specLists[form.value.loai].map((s) => ({ value: s[meta.idField], label: s[meta.nameField] }));
});

const openAdd = () => {
  editingId.value = null;
  form.value = emptyForm();
  formError.value = "";
  showModal.value = true;
};
const openEdit = (item) => {
  editingId.value = item.rowId;
  const specId = item.loai === 'sanPham' ? item.bienTheId : item[LINH_KIEN_META[item.loai].idField];
  form.value = {
    loai: item.loai,
    specId,
    soSerial: item.soSerial,
    trangThai: item.trangThai,
    ngayNhapKho: (item.ngayNhapKho || '').slice(0, 16),
    ghiChu: item.ghiChu || '',
  };
  formError.value = "";
  showModal.value = true;
};
```

- [ ] **Step 6: Sửa `saveSerial` — rẽ nhánh theo `loai`**

Thay toàn bộ hàm `saveSerial`:
```javascript
const saveSerial = async () => {
  formError.value = "";
  if (!form.value.specId) {
    formError.value = t(form.value.loai === 'sanPham' ? 'admin.serialManager.variantRequired' : 'admin.serialManager.specRequired');
    return;
  }
  if (!form.value.soSerial.trim()) { formError.value = t('admin.serialManager.serialRequired'); return; }
  try {
    const common = {
      soSerial: form.value.soSerial.trim(),
      trangThai: form.value.trangThai,
      ngayNhapKho: nowLocalIso(new Date(form.value.ngayNhapKho)),
      ghiChu: form.value.ghiChu || null,
    };
    let res;
    if (form.value.loai === 'sanPham') {
      const body = { bienTheId: Number(form.value.specId), ...common };
      res = editingId.value
        ? await ChiTietSanPhamService.update(editingId.value, body)
        : await ChiTietSanPhamService.create(body);
    } else {
      const meta = LINH_KIEN_META[form.value.loai];
      const body = { [meta.idField]: Number(form.value.specId), ...common };
      res = editingId.value
        ? await meta.service.update(editingId.value, body)
        : await meta.service.create(body);
    }
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
```

- [ ] **Step 7: Sửa `deleteSerial` — rẽ nhánh theo `loai`**

Thay:
```javascript
const deleteSerial = async (id) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const res = await ChiTietSanPhamService.remove(id);
  if (!res.ok) {
    showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status })));
    return;
  }
  await load();
};
```
bằng:
```javascript
const deleteSerial = async (item) => {
  if (!(await askConfirm(t('admin.confirm.deleteSerial')))) return;
  const service = item.loai === 'sanPham' ? ChiTietSanPhamService : LINH_KIEN_META[item.loai].service;
  const res = await service.remove(item.rowId);
  if (!res.ok) {
    showToast(await res.text().catch(() => t('admin.errors.deleteFailed', { status: res.status })));
    return;
  }
  await load();
};
```

- [ ] **Step 8: Sửa template — cột "Loại", dùng `rowSpecLabel`/`rowId`, dropdown "Loại" + chọn spec động, dropdown trạng thái động**

Thay toàn bộ `<template>`:
```html
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
          <th>{{ t('admin.serialManager.colLoai') }}</th>
          <th>{{ t('admin.serialManager.colVariant') }}</th>
          <th>{{ t('admin.serialManager.colSerial') }}</th>
          <th>{{ t('admin.serialManager.colStatus') }}</th>
          <th>{{ t('admin.serialManager.colDate') }}</th>
          <th>{{ t('admin.serialManager.colNote') }}</th>
          <th style="width:140px;">{{ t('admin.serialManager.colAction') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in filteredItems" :key="`${item.loai}-${item.rowId}`">
          <td class="text-secondary">{{ idx + 1 }}</td>
          <td>{{ t(`admin.productsTabs.${item.loai}`) }}</td>
          <td>{{ rowSpecLabel(item) }}</td>
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
              <button v-if="item.trangThai === 'trong_kho'" class="btn btn-sm btn-outline-danger" style="font-size:0.78rem;padding:2px 8px;" @click="deleteSerial(item)">{{ t('admin.serialManager.delete') }}</button>
            </div>
          </td>
        </tr>
        <tr v-if="filteredItems.length===0"><td colspan="8" class="text-center text-secondary">{{ t('admin.serialManager.empty') }}</td></tr>
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
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.colLoai') }}</label>
        <select v-model="form.loai" class="form-select form-select-sm" :disabled="!!editingId" @change="onLoaiChange" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option value="sanPham">{{ t('admin.productsTabs.sanPham') }}</option>
          <option value="cpu">{{ t('admin.productsTabs.cpu') }}</option>
          <option value="ram">{{ t('admin.productsTabs.ram') }}</option>
          <option value="gpu">{{ t('admin.productsTabs.gpu') }}</option>
          <option value="oCung">{{ t('admin.productsTabs.oCung') }}</option>
        </select>
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ form.loai === 'sanPham' ? t('admin.serialManager.variantLabel') : t(`admin.productsTabs.${form.loai}`) }}</label>
        <SearchSelect v-model="form.specId" :options="specOptions" :placeholder="form.loai === 'sanPham' ? t('admin.serialManager.variantPlaceholder') : t('admin.serialManager.specPlaceholder')" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.serialLabel') }}</label>
        <input v-model="form.soSerial" class="form-control form-control-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
      </div>
      <div class="mb-3">
        <label class="form-label small text-secondary mb-1">{{ t('admin.serialManager.statusLabel') }}</label>
        <select v-model="form.trangThai" class="form-select form-select-sm" style="background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);">
          <option v-for="s in statusOptions" :key="s" :value="s">{{ t(`admin.statusLabel.${s}`) }}</option>
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

- [ ] **Step 9: Kiểm tra thủ công**

Không có test framework frontend. Review lại toàn file sau khi sửa: xác nhận không còn tham chiếu `item.chiTietId`/`item.bienTheId` (cũ) sót lại ngoài những chỗ đặc thù cho `loai==='sanPham'`, xác nhận đổi "Loại" trong modal Thêm (không phải Sửa, vì lúc Sửa dropdown bị `disabled`) reset đúng `specId`/`trangThai` về mặc định qua `onLoaiChange`, xác nhận `openEdit()` không bị `onLoaiChange` can thiệp (chỉ gắn qua `@change` của `<select>`, không phải `watch()`, nên gán `form.value` nguyên object trong `openEdit()` không kích hoạt nó).

- [ ] **Step 10: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/admin/SerialManager.vue
git commit -m "feat: add component type selector to SerialManager, merge product + component serials"
```

---

### Task 6: Wiring — di chuyển Serial từ trang Sản phẩm sang trang Kho hàng

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `DmCategoryTable.vue` (Task 4, giờ cần thêm 2 prop `serial-service`/`serial-field-name`), `SerialManager.vue` (Task 5, không đổi cách gọi), `ChiTietCpuService`/`ChiTietRamService`/`ChiTietGpuService`/`ChiTietOCungService` (Task 2, cần import mới), key i18n `admin.inventory.tabSerial` (Task 3).

- [ ] **Step 1: Thêm import service linh kiện**

Sau dòng `import * as DmService from "../Service/DmService.js";` (dòng 9), thêm:

```javascript
import { ChiTietCpuService, ChiTietRamService, ChiTietGpuService, ChiTietOCungService } from "../Service/ChiTietLinhKienService.js";
```

- [ ] **Step 2: Sửa trang Sản phẩm — bỏ sub-tab Serial (quay về 5 tab), thêm 2 prop mới cho 4 `DmCategoryTable`**

Thay toàn bộ block:
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
bằng:
```html
        <!-- ── San pham ── -->
        <section v-show="currentPage === 'products'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='sanPham'}" @click="productsMainTab='sanPham'">{{ t('admin.productsTabs.sanPham') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='cpu'}" @click="productsMainTab='cpu'">{{ t('admin.productsTabs.cpu') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='ram'}" @click="productsMainTab='ram'">{{ t('admin.productsTabs.ram') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='gpu'}" @click="productsMainTab='gpu'">{{ t('admin.productsTabs.gpu') }}</button></li>
            <li class="nav-item"><button class="nav-link" :class="{active: productsMainTab==='oCung'}" @click="productsMainTab='oCung'">{{ t('admin.productsTabs.oCung') }}</button></li>
          </ul>

          <div v-show="productsMainTab==='sanPham'">
            <ProductsTable />
          </div>
          <div v-show="productsMainTab==='cpu'">
            <DmCategoryTable :service="DmService.DmCpuService" id-field="cpuId" name-field="tenCpu" :label="t('admin.productsTabs.cpu')" :name-label="t('admin.productsTabs.cpu')" :serial-service="ChiTietCpuService" serial-field-name="cpuId" />
          </div>
          <div v-show="productsMainTab==='ram'">
            <DmCategoryTable :service="DmService.DmRamService" id-field="ramId" name-field="dungLuong" :label="t('admin.productsTabs.ram')" :name-label="t('admin.productsTabs.ram')" :serial-service="ChiTietRamService" serial-field-name="ramId" />
          </div>
          <div v-show="productsMainTab==='gpu'">
            <DmCategoryTable :service="DmService.DmGpuService" id-field="gpuId" name-field="tenGpu" :label="t('admin.productsTabs.gpu')" :name-label="t('admin.productsTabs.gpu')" :serial-service="ChiTietGpuService" serial-field-name="gpuId" />
          </div>
          <div v-show="productsMainTab==='oCung'">
            <DmCategoryTable :service="DmService.DmOCungService" id-field="oCungId" name-field="loaiOcung" :label="t('admin.productsTabs.oCung')" :name-label="t('admin.productsTabs.oCung')" :serial-service="ChiTietOCungService" serial-field-name="oCungId" />
          </div>
        </section>
```

- [ ] **Step 3: Sửa trang Kho hàng — thêm sub-tab thứ 3 "Serial"**

Thay toàn bộ block:
```html
        <!-- ── Kho hang ── -->
        <section v-show="currentPage === 'inventory'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='kho'}" @click="inventoryMainTab='kho'">📦 {{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
            </li>
          </ul>

          <div v-show="inventoryMainTab==='kho'">
            <InventoryPanel />
          </div>

          <!-- ══ TAB: BAO HANH ══ -->
          <div v-show="inventoryMainTab==='bao-hanh'">
            <WarrantyPanel />
          </div>
        </section>
```
bằng:
```html
        <!-- ── Kho hang ── -->
        <section v-show="currentPage === 'inventory'">
          <ul class="nav nav-tabs mb-3">
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='kho'}" @click="inventoryMainTab='kho'">📦 {{ t('admin.inventory.tabStock') }} / {{ t('admin.inventory.tabReceipts') }}</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='bao-hanh'}" @click="inventoryMainTab='bao-hanh'">🛡️ {{ t('admin.inventory.tabWarranty') }}</button>
            </li>
            <li class="nav-item">
              <button class="nav-link" :class="{active: inventoryMainTab==='serial'}" @click="inventoryMainTab='serial'">🔢 {{ t('admin.inventory.tabSerial') }}</button>
            </li>
          </ul>

          <div v-show="inventoryMainTab==='kho'">
            <InventoryPanel />
          </div>

          <!-- ══ TAB: BAO HANH ══ -->
          <div v-show="inventoryMainTab==='bao-hanh'">
            <WarrantyPanel />
          </div>

          <!-- ══ TAB: SERIAL ══ -->
          <div v-show="inventoryMainTab==='serial'">
            <SerialManager />
          </div>
        </section>
```

- [ ] **Step 4: Kiểm tra bằng dev server**

Run (PowerShell, từ thư mục `FrontEnd/QLBanMayTinh`):
```powershell
Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; $job = Start-Job { Set-Location "D:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run dev }; Start-Sleep -Seconds 8; Receive-Job $job; Stop-Job $job; Remove-Job $job -Force
```
Expected: `VITE ... ready in ...ms`, không có lỗi biên dịch.

Mở trình duyệt, đăng nhập admin, kiểm tra:
- Trang "Sản phẩm" chỉ còn 5 sub-tab (không còn "Serial").
- Trang "Kho hàng" có 3 sub-tab (Tồn kho/Phiếu nhập, Bảo hành, Serial mới).
- Tab CPU: thử "Thêm" → phải nhập tên + ít nhất 1 serial (thử nhập tay, thử import file Excial mẫu) mới lưu được.
- Tab Serial (ở Kho hàng): thử "Thêm serial", chọn Loại = CPU → dropdown đổi sang chọn CPU, trạng thái chỉ còn 3 lựa chọn, lưu thành công, dòng mới hiện đúng cột "Loại" = CPU.

- [ ] **Step 5: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat: move Serial tab from Products page to Inventory page"
```

---

## Self-Review

**1. Spec coverage:**
- Giải quyết trùng lặp (giữ SerialManager làm trang tổng hợp) → Task 5 ✅
- Di chuyển Serial từ trang Sản phẩm sang Kho hàng → Task 6 ✅
- Backend 4 bảng serial linh kiện (đã áp dụng SQL trước plan này) + entity/DTO/repo/service/controller → Task 1 ✅
- Trạng thái riêng cho linh kiện (3 mức) → Task 1 (CHECK constraint đã áp dụng) + Task 5 (UI) ✅
- Serial bắt buộc lúc tạo mới, không bắt buộc lúc sửa → Task 4 ✅
- Đơn/nhiều/import Excel → Task 4 (tái dùng đúng cơ chế `InventoryPanel.vue`) ✅
- Không hiện serial/tồn kho trên bảng danh sách linh kiện → Task 4 (không đổi phần bảng danh sách, chỉ đổi modal) ✅
- `SerialManager` chọn Loại, gộp 5 nguồn → Task 5 ✅
- `WarehouseManagementPage.vue` không đổi → không có task nào đụng tới file này ✅

**2. Placeholder scan:** Đã viết đầy đủ code cho cả 4 loại linh kiện ở Task 1 (không dùng "lặp lại tương tự") — riêng Task 3 (i18n) dùng "anchor" (tìm dòng gần đúng) cho 4 locale còn lại vì các file đó chưa đọc trực tiếp trong phiên làm việc này, nhưng đã cho đúng nội dung dịch đầy đủ + anchor rõ ràng (dòng cuối mỗi block, không mơ hồ).

**3. Type consistency:**
- Field JSON response linh kiện (`chiTietCpuId`/`cpuId`/`tenCpu`, `chiTietOCungId`/`oCungId`/`loaiOcung`, v.v.) khớp giữa Task 1 (Response DTO) và Task 5 (`SerialManager.vue` đọc `item[meta.idField]`/`item[meta.nameField]`, `item.chiTietCpuId` v.v.).
- `serialFieldName` prop (Task 4, vd `'cpuId'`) khớp đúng field FK request Task 1 mong đợi (`ChiTietCpuRequest.cpuId`).
- `LINH_KIEN_META` (Task 5) dùng đúng field name của Response DTO Task 1 (`idField`/`nameField`) và đúng export name Task 2 (`service`).
- `props.service.save()` (Task 4, `DmCpuService.save`) không đổi — vẫn dùng `crud()` factory cũ, tách biệt hoàn toàn với `props.serialService.create()` (Task 2, factory mới).

**4. Idempotency:** Backend/frontend đều là code Java/Vue, không lặp lại migration SQL nào (đã áp dụng thật ở phiên brainstorming trước task này).
