# Nâng cấp trang Báo cáo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nâng cấp tab "Báo cáo" trong `AdminPage.vue` từ chỗ tự tính bằng JS trên dữ liệu tải sẵn thành: sửa 2 chỗ đang sai (top sản phẩm theo giá → theo số lượng bán; trạng thái đơn dùng nhãn/màu có sẵn), thêm bộ lọc khoảng thời gian, biểu đồ doanh thu theo ngày, và báo cáo khách hàng nổi bật (top chi tiêu + tỷ lệ mua lại).

**Architecture:** 3 endpoint backend mới/mở rộng dưới `/api/dashboard/*` (đã có sẵn, không đổi route base), dùng optional-date-range param pattern đã có trong dự án (`:x IS NULL OR ...`) để không phá tab Dashboard hiện tại (vẫn gọi các API cũ không kèm ngày). Biểu đồ vẽ SVG thuần theo đúng phong cách `DonutChart.vue`, không thêm thư viện.

**Tech Stack:** Spring Boot 4.0.6 (JPA/Hibernate), Vue 3 `<script setup>`.

## Global Constraints

- Tab **Dashboard** hiện tại (khác tab Báo cáo) gọi `DashboardService.getTopSelling(limit)`/`getSlowSelling(limit)` không kèm ngày — phải tiếp tục chạy y hệt sau khi thêm tham số ngày (tham số mới phải nullable, mặc định null = không lọc).
- 4 thẻ KPI trên cùng của tab Báo cáo (tổng doanh thu, sản phẩm đang bán, khuyến mãi đang chạy, biến thể sắp hết hàng) **không lọc theo ngày** — giữ nguyên logic hiện tại.
- Biểu đồ doanh thu luôn gộp theo **ngày** (không có chế độ gộp tháng riêng), kể cả khi chọn khoảng "Tháng này".
- Không thêm thư viện biểu đồ ngoài — vẽ SVG thuần theo đúng phong cách `DonutChart.vue` (`FrontEnd/QLBanMayTinh/src/components/common/DonutChart.vue`).
- Style code theo đúng file đang sửa: Lombok `@AllArgsConstructor @Getter @Setter` cho response DTO (không `@Data`, không `@NoArgsConstructor` — đúng theo `ProductSalesResponse.java`/`DashboardKpiResponse.java` hiện có), field injection `@Autowired`, comment tiếng Việt giải thích "vì sao".

---

### Task 1: Backend — DTO mới + lọc ngày cho top-selling/slow-selling

**Files:**
- Create: `BackEnd/src/main/java/com/example/backend/response/RevenueByDayResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/CustomerSpendingResponse.java`
- Create: `BackEnd/src/main/java/com/example/backend/response/CustomerReportResponse.java`
- Modify: `BackEnd/src/main/java/com/example/backend/repository/SanPhamRepository.java`
- Modify: `BackEnd/src/main/java/com/example/backend/service/DashboardService.java`
- Modify: `BackEnd/src/main/java/com/example/backend/controller/DashboardController.java`
- Test: `BackEnd/src/test/java/com/example/backend/service/DashboardServiceTest.java`

**Interfaces:**
- Produces: `DashboardService.getTopSelling(int limit, LocalDate tuNgay, LocalDate denNgay)` và `getSlowSelling(...)` cùng chữ ký (cả hai nhận `tuNgay`/`denNgay` nullable) — Task 4 (frontend service) gọi endpoint `GET /api/dashboard/top-selling?limit=&tuNgay=&denNgay=`. `RevenueByDayResponse{LocalDate ngay; BigDecimal doanhThu;}`, `CustomerSpendingResponse{Integer khachHangId; String hoTen; Long soDonHang; BigDecimal tongChiTieu;}`, `CustomerReportResponse{List<CustomerSpendingResponse> topKhach; double tyLeMuaLai; int tongSoKhach;}` — dùng ở Task 2, 3.

- [ ] **Step 1: Tạo 3 DTO mới**

`BackEnd/src/main/java/com/example/backend/response/RevenueByDayResponse.java`:
```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
public class RevenueByDayResponse {
    private LocalDate ngay;
    private BigDecimal doanhThu;
}
```

`BackEnd/src/main/java/com/example/backend/response/CustomerSpendingResponse.java`:
```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class CustomerSpendingResponse {
    private Integer khachHangId;
    private String hoTen;
    private Long soDonHang;
    private BigDecimal tongChiTieu;
}
```

`BackEnd/src/main/java/com/example/backend/response/CustomerReportResponse.java`:
```java
package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class CustomerReportResponse {
    private List<CustomerSpendingResponse> topKhach;
    private double tyLeMuaLai;
    private int tongSoKhach;
}
```

- [ ] **Step 2: Thêm tham số ngày cho `topSelling`/`slowSelling`**

Trong `BackEnd/src/main/java/com/example/backend/repository/SanPhamRepository.java`, thay 2 method cuối (dòng 89-110) bằng:

```java
    // Xếp hạng sản phẩm bán chạy/bán chậm — SUM ở SQL thay vì kéo hết chi_tiet_don_hang về
    // JS cộng dồn. LEFT JOIN để sản phẩm chưa từng bán vẫn xuất hiện với soLuongDaBan = 0
    // (cần cho "bán chậm"). tuNgay/denNgay null = không lọc (tab Dashboard gọi không kèm
    // ngày, giữ nguyên hành vi cũ); có giá trị = chỉ tính đơn đặt trong khoảng đó (tab Báo
    // cáo) — dùng cho "top bán chạy", nên sản phẩm 0 đơn trong khoảng có thể bị lọc khỏi
    // kết quả thay vì hiện 0 (chấp nhận được, không ảnh hưởng vì chỉ lấy top N bán chạy).
    @Query("""
    SELECT new com.example.backend.response.ProductSalesResponse(sp.tenSanPham, COALESCE(SUM(ct.soLuong), 0))
    FROM SanPham sp
    LEFT JOIN BienTheSanPham bt ON bt.sanPham = sp
    LEFT JOIN ChiTietDonHang ct ON ct.bienThe = bt
    LEFT JOIN ct.donHang d
    WHERE (:tuNgay IS NULL OR d.ngayDat >= :tuNgay) AND (:denNgay IS NULL OR d.ngayDat <= :denNgay)
    GROUP BY sp.sanPhamId, sp.tenSanPham
    ORDER BY COALESCE(SUM(ct.soLuong), 0) DESC
    """)
    List<ProductSalesResponse> topSelling(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay, Pageable pageable);

    @Query("""
    SELECT new com.example.backend.response.ProductSalesResponse(sp.tenSanPham, COALESCE(SUM(ct.soLuong), 0))
    FROM SanPham sp
    LEFT JOIN BienTheSanPham bt ON bt.sanPham = sp
    LEFT JOIN ChiTietDonHang ct ON ct.bienThe = bt
    LEFT JOIN ct.donHang d
    WHERE (:tuNgay IS NULL OR d.ngayDat >= :tuNgay) AND (:denNgay IS NULL OR d.ngayDat <= :denNgay)
    GROUP BY sp.sanPhamId, sp.tenSanPham
    ORDER BY COALESCE(SUM(ct.soLuong), 0) ASC
    """)
    List<ProductSalesResponse> slowSelling(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay, Pageable pageable);
}
```

Thêm import ở đầu file: `import java.time.LocalDateTime;`

- [ ] **Step 3: Cập nhật `DashboardService`**

Thay toàn bộ nội dung `BackEnd/src/main/java/com/example/backend/service/DashboardService.java` bằng:

```java
package com.example.backend.service;

import com.example.backend.repository.DonHangRepository;
import com.example.backend.repository.KhachHangRepository;
import com.example.backend.repository.SanPhamRepository;
import com.example.backend.repository.TonKhoRepository;
import com.example.backend.response.CustomerReportResponse;
import com.example.backend.response.CustomerSpendingResponse;
import com.example.backend.response.DashboardKpiResponse;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.RevenueByDayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private TonKhoRepository tonKhoRepository;

    public DashboardKpiResponse getKpi() {
        return new DashboardKpiResponse(
                sanPhamRepository.count(),
                donHangRepository.count(),
                khachHangRepository.count(),
                donHangRepository.sumDoanhThu(),
                tonKhoRepository.countLowStock());
    }

    public List<ProductSalesResponse> getTopSelling(int limit, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return sanPhamRepository.topSelling(tuNgay, denNgay, PageRequest.of(0, limit));
    }

    public List<ProductSalesResponse> getSlowSelling(int limit, LocalDateTime tuNgay, LocalDateTime denNgay) {
        return sanPhamRepository.slowSelling(tuNgay, denNgay, PageRequest.of(0, limit));
    }

    public List<RevenueByDayResponse> getRevenueByDay(LocalDateTime tuNgay, LocalDateTime denNgay) {
        return donHangRepository.doanhThuTheoNgay(tuNgay, denNgay);
    }

    // Top khách chi tiêu nhiều nhất + tỷ lệ khách mua từ 2 đơn trở lên trong khoảng ngày —
    // lấy hết (Pageable.unpaged()) để đếm đúng tỷ lệ mua lại trên toàn bộ khách có đơn
    // trong khoảng, rồi mới cắt ra top N để hiển thị bảng.
    public CustomerReportResponse getCustomerReport(LocalDateTime tuNgay, LocalDateTime denNgay, int limit) {
        List<CustomerSpendingResponse> all = khachHangRepository.chiTieuTheoKhachHang(tuNgay, denNgay, Pageable.unpaged());
        long soKhachMuaLai = all.stream().filter(c -> c.getSoDonHang() >= 2).count();
        double tyLeMuaLai = all.isEmpty() ? 0 : (double) soKhachMuaLai / all.size();
        List<CustomerSpendingResponse> top = all.stream().limit(limit).toList();
        return new CustomerReportResponse(top, tyLeMuaLai, all.size());
    }
}
```

- [ ] **Step 4: Cập nhật `DashboardController`**

Thay `BackEnd/src/main/java/com/example/backend/controller/DashboardController.java` bằng:

```java
package com.example.backend.controller;

import com.example.backend.response.CustomerReportResponse;
import com.example.backend.response.DashboardKpiResponse;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.RevenueByDayResponse;
import com.example.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Endpoint tổng hợp cho Dashboard + Báo cáo admin — SUM/COUNT/GROUP BY chạy ở SQL, thay vì
// tải toàn bộ san_pham/don_hang/chi_tiet_don_hang về trình duyệt rồi cộng dồn bằng JS.
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("kpi")
    public DashboardKpiResponse getKpi() {
        return dashboardService.getKpi();
    }

    @GetMapping("top-selling")
    public List<ProductSalesResponse> getTopSelling(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return dashboardService.getTopSelling(limit,
                tuNgay == null ? null : tuNgay.atStartOfDay(),
                denNgay == null ? null : LocalDateTime.of(denNgay, LocalTime.MAX));
    }

    @GetMapping("slow-selling")
    public List<ProductSalesResponse> getSlowSelling(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return dashboardService.getSlowSelling(limit,
                tuNgay == null ? null : tuNgay.atStartOfDay(),
                denNgay == null ? null : LocalDateTime.of(denNgay, LocalTime.MAX));
    }

    @GetMapping("doanh-thu-theo-ngay")
    public List<RevenueByDayResponse> getRevenueByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return dashboardService.getRevenueByDay(tuNgay.atStartOfDay(), LocalDateTime.of(denNgay, LocalTime.MAX));
    }

    @GetMapping("khach-hang-noi-bat")
    public CustomerReportResponse getCustomerReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getCustomerReport(tuNgay.atStartOfDay(), LocalDateTime.of(denNgay, LocalTime.MAX), limit);
    }
}
```

- [ ] **Step 5: Viết test cho phần lọc ngày + tương thích ngược**

Tạo `BackEnd/src/test/java/com/example/backend/service/DashboardServiceTest.java`:

```java
package com.example.backend.service;

import com.example.backend.repository.*;
import com.example.backend.response.CustomerSpendingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SanPhamRepository sanPhamRepository;
    @Mock private DonHangRepository donHangRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private TonKhoRepository tonKhoRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    void getTopSelling_khongTruyenNgay_goiRepoVoiNullGiuHanhViCu() {
        service.getTopSelling(5, null, null);
        verify(sanPhamRepository).topSelling(isNull(), isNull(), any());
    }

    @Test
    void getTopSelling_coTruyenNgay_goiRepoDungThamSo() {
        LocalDateTime tu = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime den = LocalDateTime.of(2026, 1, 31, 23, 59, 59);
        service.getTopSelling(5, tu, den);
        verify(sanPhamRepository).topSelling(eq(tu), eq(den), any());
    }

    @Test
    void getCustomerReport_danhSachRong_tyLeMuaLaiBang0KhongChiaCho0() {
        when(khachHangRepository.chiTieuTheoKhachHang(any(), any(), any())).thenReturn(List.of());

        var result = service.getCustomerReport(LocalDateTime.now().minusDays(7), LocalDateTime.now(), 5);

        assertThat(result.getTyLeMuaLai()).isEqualTo(0.0);
        assertThat(result.getTongSoKhach()).isEqualTo(0);
        assertThat(result.getTopKhach()).isEmpty();
    }

    @Test
    void getCustomerReport_tinhDungTyLeKhachMuaTuHaiDonTroLen() {
        List<CustomerSpendingResponse> all = List.of(
                new CustomerSpendingResponse(1, "A", 3L, BigDecimal.valueOf(1000)),
                new CustomerSpendingResponse(2, "B", 1L, BigDecimal.valueOf(500)),
                new CustomerSpendingResponse(3, "C", 2L, BigDecimal.valueOf(800)),
                new CustomerSpendingResponse(4, "D", 1L, BigDecimal.valueOf(200))
        );
        when(khachHangRepository.chiTieuTheoKhachHang(any(), any(), any())).thenReturn(all);

        var result = service.getCustomerReport(LocalDateTime.now().minusDays(7), LocalDateTime.now(), 2);

        // 2/4 khách (A, C) có >= 2 đơn
        assertThat(result.getTyLeMuaLai()).isEqualTo(0.5);
        assertThat(result.getTongSoKhach()).isEqualTo(4);
        assertThat(result.getTopKhach()).hasSize(2);
        assertThat(result.getTopKhach().get(0).getHoTen()).isEqualTo("A");
    }
}
```

- [ ] **Step 6: Chạy test**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o test "-Dtest=DashboardServiceTest"
```
Expected: `BUILD SUCCESS`, 4/4 test passed.

- [ ] **Step 7: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/response/RevenueByDayResponse.java \
  BackEnd/src/main/java/com/example/backend/response/CustomerSpendingResponse.java \
  BackEnd/src/main/java/com/example/backend/response/CustomerReportResponse.java \
  BackEnd/src/main/java/com/example/backend/repository/SanPhamRepository.java \
  BackEnd/src/main/java/com/example/backend/service/DashboardService.java \
  BackEnd/src/main/java/com/example/backend/controller/DashboardController.java \
  BackEnd/src/test/java/com/example/backend/service/DashboardServiceTest.java
git commit -m "feat: add optional date-range filter to top/slow-selling + new DTOs"
```

---

### Task 2: Backend — doanh thu theo ngày

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/repository/DonHangRepository.java`

**Interfaces:**
- Consumes: `RevenueByDayResponse` (Task 1).
- Produces: `DonHangRepository.doanhThuTheoNgay(LocalDateTime, LocalDateTime)` — đã được `DashboardService.getRevenueByDay` (Task 1, Step 3) gọi sẵn.

- [ ] **Step 1: Thêm query**

Thêm vào cuối `BackEnd/src/main/java/com/example/backend/repository/DonHangRepository.java` (trước dấu `}` đóng interface):

```java

    // Doanh thu gộp theo ngày trong khoảng — dùng cho biểu đồ cột "Doanh thu theo thời
    // gian" ở tab Báo cáo. CAST sang LocalDate để gộp đúng theo ngày (ngayDat là
    // LocalDateTime, có giờ phút giây khác nhau).
    @Query("""
    SELECT new com.example.backend.response.RevenueByDayResponse(CAST(d.ngayDat AS java.time.LocalDate), SUM(d.thanhTien))
    FROM DonHang d
    WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay
    GROUP BY CAST(d.ngayDat AS java.time.LocalDate)
    ORDER BY CAST(d.ngayDat AS java.time.LocalDate)
    """)
    List<RevenueByDayResponse> doanhThuTheoNgay(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay);
```

Thêm import: `import com.example.backend.response.RevenueByDayResponse;`, `import java.time.LocalDateTime;`, `import java.util.List;` (nếu `List` chưa có sẵn — file hiện tại chưa import `List`, cần thêm).

- [ ] **Step 2: Biên dịch để chắc JPQL `CAST ... AS java.time.LocalDate` hợp lệ**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
```
Expected: `BUILD SUCCESS`. Đây chỉ kiểm tra biên dịch Java — JPQL chỉ được Hibernate xác thực lúc khởi động ứng dụng thật (kiểm tra kỹ hơn ở Task 9, chạy thử thực tế). Nếu `CAST(... AS java.time.LocalDate)` không được Hibernate 7 chấp nhận lúc chạy thử, đổi sang `CAST(d.ngayDat AS LocalDate)` (không cần full qualified name — cách này cũng hợp lệ ở hầu hết bản Hibernate 6/7).

- [ ] **Step 3: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/repository/DonHangRepository.java
git commit -m "feat: add doanh-thu-theo-ngay query for revenue-over-time chart"
```

---

### Task 3: Backend — khách hàng nổi bật

**Files:**
- Modify: `BackEnd/src/main/java/com/example/backend/repository/KhachHangRepository.java`

**Interfaces:**
- Consumes: `CustomerSpendingResponse` (Task 1).
- Produces: `KhachHangRepository.chiTieuTheoKhachHang(LocalDateTime, LocalDateTime, Pageable)` — đã được `DashboardService.getCustomerReport` (Task 1, Step 3) gọi sẵn.

- [ ] **Step 1: Thêm query**

`KhachHangRepository.java` hiện dùng tab để thụt lề (khác 4-space ở các file khác) — giữ nguyên phong cách file này. Thêm vào cuối interface (trước dấu `}`):

```java

	// Chi tiêu từng khách trong khoảng ngày — dùng cho báo cáo "Khách hàng nổi bật" (top
	// chi tiêu + tỷ lệ mua lại). Không phân trang ở DB (Pageable ở service truyền
	// Pageable.unpaged() khi cần đếm tỷ lệ mua lại trên toàn bộ, hoặc PageRequest khi
	// service tự giới hạn) — JOIN thường vì 1 đơn luôn có khách hàng (NOT NULL).
	@Query("""
	SELECT new com.example.backend.response.CustomerSpendingResponse(kh.khachHangId, kh.hoTen, COUNT(d), SUM(d.thanhTien))
	FROM DonHang d JOIN d.khachHang kh
	WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay
	GROUP BY kh.khachHangId, kh.hoTen
	ORDER BY SUM(d.thanhTien) DESC
	""")
	java.util.List<com.example.backend.response.CustomerSpendingResponse> chiTieuTheoKhachHang(
			@org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
			@org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay,
			org.springframework.data.domain.Pageable pageable);
```

(Dùng fully-qualified name thay vì thêm dòng `import` mới, đúng phong cách field/method còn lại trong chính file này — ví dụ `java.util.List<KhachHangResponse>` ở dòng 12 đã làm vậy.)

- [ ] **Step 2: Biên dịch**

Run:
```powershell
cd "d:\project code\SAOClub\BackEnd"; $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd -o compile
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add BackEnd/src/main/java/com/example/backend/repository/KhachHangRepository.java
git commit -m "feat: add chi-tieu-theo-khach-hang query for top-customers report"
```

---

### Task 4: Frontend — service layer

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/Service/DashboardService.js`

**Interfaces:**
- Produces: `getTopSelling(limit, tuNgay, denNgay)`, `getSlowSelling(limit, tuNgay, denNgay)` (tuNgay/denNgay optional, string `YYYY-MM-DD` hoặc `undefined`), `getRevenueByDay(tuNgay, denNgay)`, `getCustomerReport(tuNgay, denNgay, limit)` — dùng ở Task 6, 7, 8.

- [ ] **Step 1: Cập nhật file**

Thay toàn bộ `FrontEnd/QLBanMayTinh/src/Service/DashboardService.js` bằng:

```js
import { get } from './api.js';

// KPI + xếp hạng bán chạy/bán chậm — tính bằng SQL (SUM/COUNT/GROUP BY) ở backend,
// thay vì tải toàn bộ san-pham/don-hang/chi-tiet-don-hang về rồi cộng dồn bằng JS.
export const getKpi = () => get('/api/dashboard/kpi');

// tuNgay/denNgay (chuỗi 'YYYY-MM-DD') optional — không truyền = không lọc theo ngày,
// dùng cho tab Dashboard (không đổi hành vi cũ). Tab Báo cáo truyền kèm ngày để lọc.
const dateParams = (tuNgay, denNgay) => {
  const p = new URLSearchParams();
  if (tuNgay) p.set('tuNgay', tuNgay);
  if (denNgay) p.set('denNgay', denNgay);
  return p.toString();
};

export const getTopSelling = (limit = 5, tuNgay, denNgay) =>
  get(`/api/dashboard/top-selling?limit=${limit}&${dateParams(tuNgay, denNgay)}`);

export const getSlowSelling = (limit = 5, tuNgay, denNgay) =>
  get(`/api/dashboard/slow-selling?limit=${limit}&${dateParams(tuNgay, denNgay)}`);

// Doanh thu theo ngày trong khoảng — cho biểu đồ cột ở tab Báo cáo. tuNgay/denNgay bắt buộc.
export const getRevenueByDay = (tuNgay, denNgay) =>
  get(`/api/dashboard/doanh-thu-theo-ngay?tuNgay=${tuNgay}&denNgay=${denNgay}`);

// Top khách chi tiêu nhiều nhất + tỷ lệ mua lại trong khoảng. tuNgay/denNgay bắt buộc.
export const getCustomerReport = (tuNgay, denNgay, limit = 5) =>
  get(`/api/dashboard/khach-hang-noi-bat?tuNgay=${tuNgay}&denNgay=${denNgay}&limit=${limit}`);
```

- [ ] **Step 2: Kiểm tra cú pháp**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; node --check src/Service/DashboardService.js
```
Expected: không có output/lỗi.

- [ ] **Step 3: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/Service/DashboardService.js
git commit -m "feat(frontend): add date-range params to dashboard service calls"
```

---

### Task 5: Frontend — i18n keys cho toàn bộ phần mới (5 ngôn ngữ)

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/vi.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/en.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/zh.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ko.js`
- Modify: `FrontEnd/QLBanMayTinh/src/i18n/locales/ja.js`

**Interfaces:**
- Produces: các key mới dưới `admin.reports.*` — dùng ở Task 6, 7, 8.

- [ ] **Step 1: Sửa key có sẵn + thêm key mới trong khối `reports` của cả 5 file**

Trong mỗi file, khối `reports: { ... }` hiện có (vi.js dòng 942-957) — sửa giá trị `topProducts` và thêm các key mới ngay trước dấu `},` đóng khối:

`vi.js` — thay `topProducts: "Top 5 sản phẩm giá cao nhất",` bằng, và thêm các dòng sau nó:
```js
      topProducts: "Top 5 sản phẩm bán chạy nhất",
      colQuantitySold: "Số lượng đã bán",
      dateRangeToday: "Hôm nay",
      dateRangeWeek: "Tuần này",
      dateRangeMonth: "Tháng này",
      dateRangeCustom: "Tùy chọn",
      dateFrom: "Từ ngày",
      dateTo: "Đến ngày",
      revenueChartTitle: "Doanh thu theo thời gian",
      revenueChartEmpty: "Chưa có doanh thu trong khoảng này",
      customersTitle: "Khách hàng nổi bật",
      colCustomerName: "Khách hàng",
      colOrderCount: "Số đơn",
      colTotalSpent: "Tổng chi tiêu",
      repeatRateLabel: "{repeat}/{total} khách ({pct}%) mua từ 2 đơn trở lên",
      customersEmpty: "Chưa có dữ liệu",
```

`en.js`:
```js
      topProducts: "Top 5 best-selling products",
      colQuantitySold: "Units sold",
      dateRangeToday: "Today",
      dateRangeWeek: "This week",
      dateRangeMonth: "This month",
      dateRangeCustom: "Custom",
      dateFrom: "From",
      dateTo: "To",
      revenueChartTitle: "Revenue over time",
      revenueChartEmpty: "No revenue in this range",
      customersTitle: "Top customers",
      colCustomerName: "Customer",
      colOrderCount: "Orders",
      colTotalSpent: "Total spent",
      repeatRateLabel: "{repeat}/{total} customers ({pct}%) ordered 2+ times",
      customersEmpty: "No data yet",
```

`zh.js`:
```js
      topProducts: "热销商品 Top 5",
      colQuantitySold: "销售数量",
      dateRangeToday: "今天",
      dateRangeWeek: "本周",
      dateRangeMonth: "本月",
      dateRangeCustom: "自定义",
      dateFrom: "开始日期",
      dateTo: "结束日期",
      revenueChartTitle: "营收趋势",
      revenueChartEmpty: "该时间段暂无营收",
      customersTitle: "重点客户",
      colCustomerName: "客户",
      colOrderCount: "订单数",
      colTotalSpent: "消费总额",
      repeatRateLabel: "{repeat}/{total} 位客户（{pct}%）下单 2 次以上",
      customersEmpty: "暂无数据",
```

`ko.js`:
```js
      topProducts: "베스트셀러 Top 5",
      colQuantitySold: "판매 수량",
      dateRangeToday: "오늘",
      dateRangeWeek: "이번 주",
      dateRangeMonth: "이번 달",
      dateRangeCustom: "사용자 지정",
      dateFrom: "시작일",
      dateTo: "종료일",
      revenueChartTitle: "기간별 매출",
      revenueChartEmpty: "해당 기간에 매출이 없습니다",
      customersTitle: "우수 고객",
      colCustomerName: "고객",
      colOrderCount: "주문 수",
      colTotalSpent: "총 구매액",
      repeatRateLabel: "{repeat}/{total}명 ({pct}%) 고객이 2회 이상 주문",
      customersEmpty: "아직 데이터가 없습니다",
```

`ja.js`:
```js
      topProducts: "売れ筋商品 Top 5",
      colQuantitySold: "販売数",
      dateRangeToday: "今日",
      dateRangeWeek: "今週",
      dateRangeMonth: "今月",
      dateRangeCustom: "カスタム",
      dateFrom: "開始日",
      dateTo: "終了日",
      revenueChartTitle: "期間別売上",
      revenueChartEmpty: "この期間の売上はありません",
      customersTitle: "優良顧客",
      colCustomerName: "顧客",
      colOrderCount: "注文数",
      colTotalSpent: "合計購入額",
      repeatRateLabel: "{repeat}/{total}人（{pct}%）が2回以上注文",
      customersEmpty: "データがありません",
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
git commit -m "i18n: add reports upgrade translation keys (vi, en, zh, ko, ja)"
```

---

### Task 6: Frontend — bộ lọc khoảng thời gian + sửa 2 chỗ đang sai

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `DashboardService.getTopSelling` (Task 4), i18n keys (Task 5), `orderStatusLabel`/`orderStatusColor` (đã có, `utils/orderStatus.js`, đã import ở dòng 5).
- Produces: `reportsDateRange` (ref: `'today'|'week'|'month'|'custom'`), `reportsDateFrom`/`reportsDateTo` (computed string `YYYY-MM-DD`), `reportsTopSelling` (ref, dùng ở template Task 6), `reportsOrdersByStatus` (computed, dùng ở template Task 6) — Task 7, 8 dùng lại `reportsDateFrom`/`reportsDateTo` để gọi API của chúng.

- [ ] **Step 1: Thêm state bộ lọc ngày + fetch top-selling theo khoảng**

Thêm ngay sau khối `ordersByStatus` hiện có (`AdminPage.vue:246-252`, giữ nguyên không xoá — vẫn có thể dùng chỗ khác), chèn:

```js

// ── Báo cáo: bộ lọc khoảng thời gian (Hôm nay/Tuần này/Tháng này/Tùy chọn) ────────────
// Dùng lại đúng quy ước ngày-tháng đã có ở tab Dashboard (toDateInputValue, so sánh
// string 'YYYY-MM-DD') — xem weekChartFrom/weekChartTo cùng file để đối chiếu.
const reportsDateRange = ref('week'); // 'today' | 'week' | 'month' | 'custom'
const reportsCustomFrom = ref(toDateInputValue(new Date()));
const reportsCustomTo   = ref(toDateInputValue(new Date()));

const reportsDateFrom = computed(() => {
  const now = new Date();
  if (reportsDateRange.value === 'today') return toDateInputValue(now);
  if (reportsDateRange.value === 'week') return toDateInputValue(startOfWeek(now));
  if (reportsDateRange.value === 'month') return toDateInputValue(new Date(now.getFullYear(), now.getMonth(), 1));
  return reportsCustomFrom.value;
});
const reportsDateTo = computed(() => {
  const now = new Date();
  if (reportsDateRange.value === 'today') return toDateInputValue(now);
  if (reportsDateRange.value === 'week') return toDateInputValue(endOfWeek(now));
  if (reportsDateRange.value === 'month') return toDateInputValue(new Date(now.getFullYear(), now.getMonth() + 1, 0));
  return reportsCustomTo.value;
});

// Top sản phẩm bán chạy trong khoảng đã chọn — tải lại mỗi khi khoảng đổi.
const reportsTopSelling = ref([]); // [{ tenSanPham, soLuongDaBan }]
const loadReportsTopSelling = async () => {
  reportsTopSelling.value = await DashboardService
    .getTopSelling(5, reportsDateFrom.value, reportsDateTo.value)
    .catch(() => []);
};
watch([reportsDateFrom, reportsDateTo], loadReportsTopSelling, { immediate: true });

// Đơn hàng theo trạng thái trong khoảng đã chọn — vẫn tính từ orders đã tải sẵn (đủ
// nhanh, không cần thêm endpoint riêng vì đây chỉ là group-by theo status, không phải
// SUM/COUNT nặng), nhưng nay có lọc theo ngày + dùng đúng nhãn/màu trạng thái đã chốt.
const reportsOrdersByStatus = computed(() => {
  const map = {};
  orders.value.forEach((o) => {
    if (!o.ngayDat) return;
    const d = toDateInputValue(new Date(o.ngayDat));
    if (d < reportsDateFrom.value || d > reportsDateTo.value) return;
    map[o.trangThaiDonHang] = (map[o.trangThaiDonHang] || 0) + 1;
  });
  return Object.entries(map).map(([status, count]) => ({
    status, count,
    label: orderStatusLabel(status),
    color: orderStatusColor(status),
  }));
});
```

- [ ] **Step 2: Thêm UI bộ lọc ngày + sửa 2 bảng trong template**

Trong `AdminPage.vue`, ngay sau `</div>` đóng khối 4 thẻ KPI (dòng 3390, trước dòng `<div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.ordersByStatus') }}</div>` ở dòng 3391), chèn bộ chọn khoảng ngày:

```html

          <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
            <button v-for="opt in ['today','week','month','custom']" :key="opt"
                    class="btn btn-sm"
                    :class="reportsDateRange===opt ? 'btn-warning text-dark' : 'btn-outline-secondary'"
                    @click="reportsDateRange=opt">
              {{ t(`admin.reports.dateRange${opt.charAt(0).toUpperCase()}${opt.slice(1)}`) }}
            </button>
            <template v-if="reportsDateRange==='custom'">
              <span class="text-secondary small">{{ t('admin.reports.dateFrom') }}</span>
              <input type="date" v-model="reportsCustomFrom" class="form-control form-control-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
              <span class="text-secondary small">{{ t('admin.reports.dateTo') }}</span>
              <input type="date" v-model="reportsCustomTo" class="form-control form-control-sm" style="width:auto;background:var(--bg-input);color:var(--text-primary);border-color:var(--border-color-strong);" />
            </template>
          </div>
```

Thay toàn bộ bảng "Đơn hàng theo trạng thái" (dòng 3391-3403 gốc) bằng:

```html
          <div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.ordersByStatus') }}</div>
          <div class="table-responsive mb-4">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th>{{ t('admin.reports.colStatus') }}</th><th>{{ t('admin.reports.colQuantity') }}</th></tr></thead>
              <tbody>
                <tr v-for="row in reportsOrdersByStatus" :key="row.status">
                  <td><span class="badge" :style="{ background: row.color.bg, color: row.color.text }">{{ row.label }}</span></td>
                  <td><strong>{{ row.count }}</strong></td>
                </tr>
                <tr v-if="reportsOrdersByStatus.length===0"><td colspan="2" class="text-center text-secondary">{{ t('admin.reports.emptyOrders') }}</td></tr>
              </tbody>
            </table>
          </div>
```

Thay toàn bộ bảng "Top 5" (dòng 3404-3414 gốc) bằng:

```html
          <div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.topProducts') }}</div>
          <div class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th>{{ t('admin.reports.colIndex') }}</th><th>{{ t('admin.reports.colName') }}</th><th>{{ t('admin.reports.colQuantitySold') }}</th></tr></thead>
              <tbody>
                <tr v-for="(p,i) in reportsTopSelling" :key="p.tenSanPham">
                  <td class="text-secondary">{{ i+1 }}</td><td>{{ p.tenSanPham }}</td><td>{{ p.soLuongDaBan }}</td>
                </tr>
                <tr v-if="reportsTopSelling.length===0"><td colspan="3" class="text-center text-secondary">{{ t('admin.reports.emptyOrders') }}</td></tr>
              </tbody>
            </table>
          </div>
```

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add date-range filter, fix top-products and status labels in Reports"
```

---

### Task 7: Frontend — biểu đồ doanh thu theo thời gian

**Files:**
- Create: `FrontEnd/QLBanMayTinh/src/components/common/RevenueBarChart.vue`
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `DashboardService.getRevenueByDay` (Task 4), `reportsDateFrom`/`reportsDateTo` (Task 6), i18n `admin.reports.revenueChartTitle`/`revenueChartEmpty` (Task 5).
- Produces: component `RevenueBarChart` với prop `data: [{ ngay: string, doanhThu: number }]` — chỉ dùng trong task này.

- [ ] **Step 1: Tạo component biểu đồ cột**

Theo skill dataviz: 1 chuỗi số liệu (doanh thu) theo thời gian → cột dọc, 1 màu (`var(--accent)`), không cần chú giải, có tooltip hover từng cột, trục X thưa nhãn nếu nhiều cột.

`FrontEnd/QLBanMayTinh/src/components/common/RevenueBarChart.vue`:
```vue
<template>
  <!-- Biểu đồ cột doanh thu theo ngày — vẽ bằng SVG thuần, không cần thư viện ngoài -->
  <div>
    <svg :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" style="width:100%;height:auto;">
      <g v-for="(bar, i) in bars" :key="i">
        <rect :x="bar.x" :y="bar.y" :width="barWidth" :height="bar.barHeight"
              :fill="hoverIndex === i ? 'var(--accent-fg)' : 'var(--accent)'"
              rx="2"
              style="cursor:pointer;transition:fill .15s ease;"
              @mouseenter="hoverIndex = i" @mouseleave="hoverIndex = null" />
        <text v-if="i % labelStep === 0"
              :x="bar.x + barWidth / 2" :y="height - 4"
              text-anchor="middle" style="font-size:9px;fill:var(--text-secondary);">{{ bar.label }}</text>
      </g>
    </svg>
    <div v-if="hoverIndex !== null" class="small mt-1" style="color:var(--text-secondary);">
      {{ bars[hoverIndex].label }}: <strong style="color:var(--text-heading);">{{ formatPrice(data[hoverIndex].doanhThu) }}</strong>
    </div>
    <div v-if="data.length === 0" class="text-secondary small text-center py-4">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  data:      { type: Array, required: true }, // [{ ngay: 'YYYY-MM-DD', doanhThu: number }]
  width:     { type: Number, default: 600 },
  height:    { type: Number, default: 160 },
  emptyText: { type: String, default: '' },
});

const hoverIndex = ref(null);
const barGap = 2;
const barWidth = computed(() => props.data.length ? Math.max(4, props.width / props.data.length - barGap) : 0);
// Nhãn thưa dần khi nhiều cột — tối đa ~12 nhãn hiện trên trục X để khỏi chồng chữ.
const labelStep = computed(() => Math.max(1, Math.ceil(props.data.length / 12)));

const maxValue = computed(() => Math.max(1, ...props.data.map(d => Number(d.doanhThu) || 0)));
const chartHeight = computed(() => props.height - 16); // chừa chỗ nhãn trục X

const bars = computed(() => props.data.map((d, i) => {
  const value = Number(d.doanhThu) || 0;
  const barHeight = (value / maxValue.value) * (chartHeight.value - 4);
  const [, m, day] = d.ngay.split('-');
  return {
    x: i * (props.width / props.data.length),
    y: chartHeight.value - barHeight,
    barHeight,
    label: `${day}/${m}`,
  };
}));

const formatPrice = (v) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
</script>
```

- [ ] **Step 2: Wire vào `AdminPage.vue`**

Import component (thêm sau dòng import `DonutChart` hiện có):
```js
import RevenueBarChart from "../components/common/RevenueBarChart.vue";
```

Thêm state + fetch, ngay sau `loadReportsTopSelling`/`watch` đã thêm ở Task 6:
```js

// Doanh thu theo ngày trong khoảng đã chọn — cho biểu đồ cột.
const reportsRevenueByDay = ref([]); // [{ ngay, doanhThu }]
const loadReportsRevenueByDay = async () => {
  reportsRevenueByDay.value = await DashboardService
    .getRevenueByDay(reportsDateFrom.value, reportsDateTo.value)
    .catch(() => []);
};
watch([reportsDateFrom, reportsDateTo], loadReportsRevenueByDay, { immediate: true });
```

Trong template, chèn ngay sau khối bộ lọc ngày (Task 6, Step 2) và trước bảng "Đơn hàng theo trạng thái":
```html
          <div class="small fw-semibold text-secondary mb-2">{{ t('admin.reports.revenueChartTitle') }}</div>
          <div class="card border-secondary mb-4" style="background:var(--bg-hover);"><div class="card-body">
            <RevenueBarChart :data="reportsRevenueByDay" :empty-text="t('admin.reports.revenueChartEmpty')" />
          </div></div>
```

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/components/common/RevenueBarChart.vue FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add revenue-over-time bar chart to Reports"
```

---

### Task 8: Frontend — khách hàng nổi bật

**Files:**
- Modify: `FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue`

**Interfaces:**
- Consumes: `DashboardService.getCustomerReport` (Task 4), `reportsDateFrom`/`reportsDateTo` (Task 6), i18n keys (Task 5).

- [ ] **Step 1: Thêm state + fetch**

Ngay sau `loadReportsRevenueByDay`/`watch` (Task 7, Step 2):
```js

// Khách hàng nổi bật (top chi tiêu + tỷ lệ mua lại) trong khoảng đã chọn.
const reportsCustomerReport = ref({ topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 });
const loadReportsCustomerReport = async () => {
  reportsCustomerReport.value = await DashboardService
    .getCustomerReport(reportsDateFrom.value, reportsDateTo.value, 5)
    .catch(() => ({ topKhach: [], tyLeMuaLai: 0, tongSoKhach: 0 }));
};
watch([reportsDateFrom, reportsDateTo], loadReportsCustomerReport, { immediate: true });

const reportsRepeatRateText = computed(() => {
  const c = reportsCustomerReport.value;
  const repeat = Math.round(c.tyLeMuaLai * c.tongSoKhach);
  const pct = Math.round(c.tyLeMuaLai * 100);
  return t('admin.reports.repeatRateLabel', { repeat, total: c.tongSoKhach, pct });
});
```

- [ ] **Step 2: Thêm bảng vào template**

Chèn vào cuối section Báo cáo, ngay trước `</section>` đóng (dòng 3415 gốc, đã bị dịch xuống do Task 6/7 chèn thêm nội dung phía trên — tìm đúng vị trí bằng cách grep `</section>` ngay sau bảng "Top 5"):

```html
          <div class="small fw-semibold text-secondary mb-2 mt-4">{{ t('admin.reports.customersTitle') }}</div>
          <div class="text-secondary small mb-2">{{ reportsRepeatRateText }}</div>
          <div class="table-responsive">
            <table class="table table-hover table-sm align-middle" style="--bs-table-bg:var(--bg-card); --bs-table-color:var(--text-primary); --bs-table-hover-bg:var(--bg-hover); --bs-table-hover-color:var(--text-primary); --bs-table-border-color:var(--border-color-soft)">
              <thead><tr><th>{{ t('admin.reports.colIndex') }}</th><th>{{ t('admin.reports.colCustomerName') }}</th><th>{{ t('admin.reports.colOrderCount') }}</th><th>{{ t('admin.reports.colTotalSpent') }}</th></tr></thead>
              <tbody>
                <tr v-for="(c,i) in reportsCustomerReport.topKhach" :key="c.khachHangId">
                  <td class="text-secondary">{{ i+1 }}</td><td>{{ c.hoTen }}</td><td>{{ c.soDonHang }}</td><td>{{ formatPrice(c.tongChiTieu) }}</td>
                </tr>
                <tr v-if="reportsCustomerReport.topKhach.length===0"><td colspan="4" class="text-center text-secondary">{{ t('admin.reports.customersEmpty') }}</td></tr>
              </tbody>
            </table>
          </div>
```

- [ ] **Step 3: Build kiểm tra**

Run:
```powershell
cd "d:\project code\SAOClub\FrontEnd\QLBanMayTinh"; npm run build
```
Expected: `✓ built in ...`, không lỗi.

- [ ] **Step 4: Commit**

```bash
git add FrontEnd/QLBanMayTinh/src/pages/AdminPage.vue
git commit -m "feat(frontend): add top-customers + repeat-rate report to Reports"
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

- [ ] **Step 2: Kiểm tra JPQL `CAST` chạy thật đúng (rủi ro lớn nhất trong plan này)**

Vào tab Báo cáo — nếu biểu đồ doanh thu không hiện/lỗi 500, xem log backend: nếu có lỗi liên quan `CAST`/`java.time.LocalDate` trong JPQL, sửa `DonHangRepository.doanhThuTheoNgay` theo hướng dẫn dự phòng ở Task 2 Step 2 (bỏ `java.time.` khỏi `CAST(... AS LocalDate)`), chạy lại.

- [ ] **Step 3: Kịch bản chính**

1. Vào tab Báo cáo — xác nhận bảng "Đơn hàng theo trạng thái" hiện nhãn tiếng Việt có màu (không còn string thô như `confirmed`).
2. Xác nhận "Top 5" hiện đúng cột "Số lượng đã bán" (không phải giá).
3. Bấm qua các nút Hôm nay/Tuần này/Tháng này/Tùy chọn — xác nhận biểu đồ doanh thu, bảng trạng thái, top sản phẩm, khách hàng nổi bật đều đổi theo đúng khoảng đã chọn.
4. Xác nhận 4 thẻ KPI trên cùng (tổng doanh thu...) KHÔNG đổi khi bấm các nút khoảng thời gian (đúng theo thiết kế — số liệu hiện tại, không lọc theo ngày).
5. Chọn khoảng "Tùy chọn" không có đơn nào (vd 1 ngày xa trong quá khứ) — xác nhận biểu đồ/bảng hiện đúng trạng thái rỗng, không lỗi.
6. Qua tab Dashboard (khác tab Báo cáo) — xác nhận "Top sản phẩm bán chạy/bán chậm" vẫn hiện y hệt như trước khi sửa (không bị ảnh hưởng bởi tham số ngày mới).

- [ ] **Step 4: Dừng server**

`Ctrl+C` ở cả 2 terminal (hoặc dừng qua công cụ quản lý tiến trình nếu chạy nền).
