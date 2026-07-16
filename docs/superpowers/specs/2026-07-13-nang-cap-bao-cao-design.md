# Thiết kế: Nâng cấp trang Báo cáo (admin)

## Bối cảnh

Trang "Báo cáo" hiện tại (`AdminPage.vue:3363-3415`) không gọi API riêng nào — toàn bộ số liệu tính bằng `computed()` trên JS từ dữ liệu `orders`/`products` đã tải sẵn cho các trang khác:
- `totalRevenue` (dòng 241-243): cộng dồn `thanhTien` của TOÀN BỘ đơn đã tải.
- `ordersByStatus` (dòng 246-252): group-by JS trên toàn bộ đơn, hiện thẳng string `trangThaiDonHang` thô (`confirmed`, `delivered`...), không dịch/không màu.
- "Top 5" (dòng 3409): sắp theo `maxPrice` cao nhất, không phải bán chạy.

Backend đã có sẵn `DashboardController`/`DashboardService` (`/api/dashboard/kpi`, `/top-selling`, `/slow-selling`) dùng cho tab **Dashboard** riêng (không phải tab Báo cáo) — trong đó `topSelling`/`slowSelling` (`SanPhamRepository.java:100-110`) đã tính đúng SUM số lượng bán qua SQL, nhưng KHÔNG lọc theo ngày.

## Phạm vi

Nâng cấp tab **Báo cáo** (không đụng tab Dashboard, dùng chung backend nhưng mở rộng có tham số ngày):

1. Sửa "Top 5" → bán chạy nhất theo số lượng (gọi API có sẵn).
2. Sửa bảng "Đơn hàng theo trạng thái" → dùng `orderStatusLabel()`/`orderStatusColor()` (đã có, `utils/orderStatus.js`, đang dùng ở tab Dashboard).
3. Thêm bộ lọc khoảng thời gian (Hôm nay / Tuần này / Tháng này / Tùy chọn) áp dụng cho các mục "theo thời gian" — 4 thẻ KPI trên cùng (tổng doanh thu, sản phẩm đang bán, khuyến mãi đang chạy, biến thể sắp hết hàng) **giữ nguyên không lọc** (số liệu hiện tại/snapshot, không phải lịch sử).
4. Thêm biểu đồ cột "Doanh thu theo thời gian" trong khoảng đã chọn.
5. Thêm bảng "Khách hàng nổi bật": top khách chi tiêu nhiều nhất + tỷ lệ khách mua lại, trong khoảng đã chọn.
6. "Top sản phẩm bán chạy" và "Đơn hàng theo trạng thái" cũng lọc theo cùng khoảng thời gian.

## Backend

### Mở rộng `topSelling`/`slowSelling` — thêm khoảng ngày tùy chọn (không phá tab Dashboard)

`SanPhamRepository.java` — thêm 2 tham số nullable `tuNgay`/`denNgay` theo đúng pattern optional-param đã dùng ở `DonHangRepository.hienThiDonHang` (dòng 45: `(:khachHangId IS NULL OR ...)`). Khi `null` → không lọc (giữ nguyên hành vi hiện tại cho tab Dashboard, vốn gọi không truyền ngày).

```java
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
```

`slowSelling` tương tự (đổi `ASC`). `DashboardService.getTopSelling`/`getSlowSelling` nhận thêm `LocalDateTime tuNgay, LocalDateTime denNgay` (nullable), truyền xuống. `DashboardController` thêm `@RequestParam(required = false) String tuNgay/denNgay` (ISO date string `yyyy-MM-dd`, parse ở service — không bắt buộc, tab Dashboard hiện tại không gửi vẫn chạy y hệt cũ).

### Endpoint mới: doanh thu theo ngày

`DonHangRepository` — thêm:
```java
@Query("""
SELECT new com.example.backend.response.RevenueByDayResponse(CAST(d.ngayDat AS LocalDate), SUM(d.thanhTien))
FROM DonHang d
WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay
GROUP BY CAST(d.ngayDat AS LocalDate)
ORDER BY CAST(d.ngayDat AS LocalDate)
""")
List<RevenueByDayResponse> doanhThuTheoNgay(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay);
```
DTO mới `RevenueByDayResponse { LocalDate ngay; BigDecimal doanhThu; }`.

`DashboardController` thêm `GET /api/dashboard/doanh-thu-theo-ngay?tuNgay=&denNgay=` (bắt buộc — khác `top-selling`, mục này chỉ dùng cho Báo cáo nên không cần optional).

### Endpoint mới: khách hàng nổi bật

`KhachHangRepository` — thêm:
```java
@Query("""
SELECT new com.example.backend.response.CustomerSpendingResponse(kh.khachHangId, kh.hoTen, COUNT(d), SUM(d.thanhTien))
FROM DonHang d JOIN d.khachHang kh
WHERE d.ngayDat >= :tuNgay AND d.ngayDat <= :denNgay
GROUP BY kh.khachHangId, kh.hoTen
ORDER BY SUM(d.thanhTien) DESC
""")
List<CustomerSpendingResponse> chiTieuTheoKhachHang(@Param("tuNgay") LocalDateTime tuNgay, @Param("denNgay") LocalDateTime denNgay, Pageable pageable);
```
DTO mới `CustomerSpendingResponse { Integer khachHangId; String hoTen; Long soDonHang; BigDecimal tongChiTieu; }`.

Tỷ lệ khách mua lại: tính ở service từ CÙNG 1 query không phân trang (lấy hết, không giới hạn `limit`) — đếm số khách có `soDonHang >= 2` / tổng số khách có đơn trong khoảng. Trả kèm trong 1 response tổng hợp:

```java
// DashboardService
public CustomerReportResponse getCustomerReport(LocalDateTime tuNgay, LocalDateTime denNgay, int limit) {
    List<CustomerSpendingResponse> all = khachHangRepository.chiTieuTheoKhachHang(tuNgay, denNgay, Pageable.unpaged());
    long soKhachMuaLai = all.stream().filter(c -> c.getSoDonHang() >= 2).count();
    double tyLeMuaLai = all.isEmpty() ? 0 : (double) soKhachMuaLai / all.size();
    List<CustomerSpendingResponse> top = all.stream().limit(limit).toList();
    return new CustomerReportResponse(top, tyLeMuaLai, all.size());
}
```
DTO mới `CustomerReportResponse { List<CustomerSpendingResponse> topKhach; double tyLeMuaLai; int tongSoKhach; }`.

`DashboardController` thêm `GET /api/dashboard/khach-hang-noi-bat?tuNgay=&denNgay=&limit=5`.

## Frontend

### Bộ lọc khoảng thời gian (component mới, dùng chung cho cả 3 phần lọc-được)

Theo đúng convention ngày-tháng đã có ở tab Dashboard (`toDateInputValue`, so sánh string `YYYY-MM-DD`) — thêm 1 `ref` cho preset (`today|week|month|custom`) + 2 `ref` cho `tuNgay`/`denNgay` (input `type="date"`), tính sẵn theo preset (giống cách `weekChartFrom`/`weekChartTo` đã tính từ 1 anchor). Đặt trực tiếp trong `AdminPage.vue` (không tách component riêng — chỉ dùng ở đúng 1 chỗ, tách ra là abstraction thừa).

### Biểu đồ "Doanh thu theo thời gian" — component mới `RevenueBarChart.vue`

Theo skill dataviz: đây là biểu đồ 1 chuỗi số liệu (doanh thu) theo thời gian → cột dọc, 1 màu (`var(--accent)`, không cần bảng màu phân loại vì chỉ 1 series), không cần chú giải (legend chỉ bắt buộc khi ≥2 series). Vẽ SVG thuần theo đúng phong cách `DonutChart.vue` (không thêm thư viện). Có tooltip hover từng cột (ngày + số tiền), trục X thưa nhãn nếu khoảng dài (vd mỗi 5 cột 1 nhãn), có trạng thái rỗng.

**Luôn gộp theo ngày** (không có chế độ gộp theo tháng riêng) — kể cả khi chọn "Tháng này" (~30 cột), vẫn đủ đọc được và đơn giản hơn nhiều so với thêm logic chuyển đổi ngày/tháng. Nếu sau này khoảng chọn tùy ý quá dài (vd cả năm) làm biểu đồ quá rối, để sau, không làm trước khi có nhu cầu thật.

### Bảng "Khách hàng nổi bật"

Bảng đơn giản (top khách + số đơn + tổng chi tiêu) + 1 dòng tóm tắt tỷ lệ mua lại (`{soKhachMuaLai}/{tongSoKhach} khách ({tyLeMuaLai}%) mua từ 2 đơn trở lên`).

### Service mới

`FrontEnd/QLBanMayTinh/src/Service/DashboardService.js` — thêm `getRevenueByDay(tuNgay, denNgay)`, `getCustomerReport(tuNgay, denNgay, limit)`, cập nhật `getTopSelling`/`getSlowSelling` nhận thêm `tuNgay`/`denNgay` optional (không truyền = hành vi cũ, tab Dashboard không cần đổi gì).

## Xử lý lỗi / trường hợp biên

- Khoảng thời gian rỗng (không có đơn nào) → biểu đồ hiện trạng thái rỗng, bảng khách hàng hiện "Chưa có dữ liệu", tỷ lệ mua lại hiện `0/0 (—)`.
- `tuNgay > denNgay` (nhập tay khoảng tùy chọn sai) → chặn ở frontend trước khi gọi API (disable nút hoặc auto-swap), không cần validate thêm ở backend cho trường hợp nội bộ admin.
- Toàn bộ endpoint mới chỉ admin gọi (không public), không cần thêm rule bảo mật mới — dùng chung `SecurityConfig` hiện tại.

## Kiểm thử

- Test tầng service (Mockito) cho `DashboardService.getCustomerReport()`: tính đúng tỷ lệ mua lại khi có khách nhiều đơn, khi danh sách rỗng (chia 0).
- Không cần test cho các query JPQL mới (đã có pattern optional-param tương tự được tin dùng trong dự án, ví dụ `hienThiDonHang`).

## Ngoài phạm vi

- Không đổi gì ở tab Dashboard hiện tại (chỉ mở rộng optional param không phá hành vi cũ).
- Không thêm export Excel/PDF cho báo cáo (không được yêu cầu).
- Không phân trang cho bảng "Khách hàng nổi bật" (chỉ hiện top N cố định).
