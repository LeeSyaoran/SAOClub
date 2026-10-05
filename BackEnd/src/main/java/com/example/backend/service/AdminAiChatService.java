package com.example.backend.service;

import com.example.backend.entity.AiKienThuc;
import com.example.backend.entity.CaiDatHeThong;
import com.example.backend.entity.DonHang;
import com.example.backend.entity.KhachHang;
import com.example.backend.entity.KhuyenMai;
import com.example.backend.repository.*;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.RevenueByDayResponse;
import com.example.backend.response.TonKhoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
public class AdminAiChatService {

    private final String model;
    private final boolean enabled;
    private final WebClient webClient;
    private final DonHangRepository donHangRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamRepository sanPhamRepository;
    private final TonKhoRepository tonKhoRepository;
    private final NhanVienRepository nhanVienRepository;
    private final AiKienThucRepository aiKienThucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final CaiDatHeThongRepository caiDatHeThongRepository;
    private final KhuyenMaiRepository khuyenMaiRepository;
    private final PhieuBaoHanhRepository phieuBaoHanhRepository;

    public AdminAiChatService(
            @Value("${ai.ollama.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ai.ollama.model:llama3.2}") String model,
            @Value("${ai.ollama.enabled:true}") boolean enabled,
            DonHangRepository donHangRepository,
            KhachHangRepository khachHangRepository,
            SanPhamRepository sanPhamRepository,
            TonKhoRepository tonKhoRepository,
            NhanVienRepository nhanVienRepository,
            AiKienThucRepository aiKienThucRepository,
            ThuongHieuRepository thuongHieuRepository,
            DanhMucRepository danhMucRepository,
            CaiDatHeThongRepository caiDatHeThongRepository,
            KhuyenMaiRepository khuyenMaiRepository,
            PhieuBaoHanhRepository phieuBaoHanhRepository) {
        this.model = model;
        this.enabled = enabled;
        this.donHangRepository = donHangRepository;
        this.khachHangRepository = khachHangRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.tonKhoRepository = tonKhoRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.aiKienThucRepository = aiKienThucRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.danhMucRepository = danhMucRepository;
        this.caiDatHeThongRepository = caiDatHeThongRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.phieuBaoHanhRepository = phieuBaoHanhRepository;
        this.webClient = WebClient.builder().baseUrl(ollamaUrl).build();
    }

    // ─── Lời chào của Admin Bot ────────────────────────────────────────────────
    public String getWelcomeMessage() {
        return "Xin chào Admin! 👑 Tôi là **SAOClub AI Analytics & Operations Assistant**.\n\n" +
                "Tôi có toàn quyền truy xuất dữ liệu hệ thống để hỗ trợ bạn:\n\n" +
                "📊 **Doanh thu** — tổng hợp, theo ngày/tuần/tháng/năm\n" +
                "📦 **Đơn hàng** — phân loại 7 trạng thái, tra cứu đơn theo mã, đơn mới nhất\n" +
                "⚠️ **Tồn kho** — danh sách chi tiết các máy sắp hết hàng và ngưỡng cảnh báo\n" +
                "💻 **Sản phẩm & Giá** — tìm kiếm cấu hình, giá bán, giá vốn, top bán chạy\n" +
                "👥 **Khách hàng** — thông tin khách hàng, số đơn, top chi tiêu cao\n" +
                "🎁 **Khuyến mãi** — danh sách voucher đang áp dụng\n" +
                "🛡️ **Bảo hành** — tiến độ xử lý phiếu bảo hành\n" +
                "🏪 **Cửa hàng** — chính sách đổi trả, giao hàng, thông tin hotline/địa chỉ\n\n" +
                "Hãy đặt bất kỳ câu hỏi nào về hoạt động của cửa hàng!";
    }

    // ─── Xử lý câu hỏi từ admin ───────────────────────────────────────────────
    public String traLoi(String cauHoi, List<Map<String, String>> lichSuChat) {
        if (!enabled) {
            return fallbackResponse();
        }
        try {
            String dataContext = layDuLieuToanDien(cauHoi);
            String prompt = buildAdminPrompt(cauHoi, dataContext);
            return goiOllama(prompt, lichSuChat);
        } catch (Exception e) {
            return fallbackResponse();
        }
    }

    // ─── Truy vấn dữ liệu toàn diện theo câu hỏi ──────────────────────────────
    private String layDuLieuToanDien(String cauHoi) {
        String lower = cauHoi.toLowerCase();
        StringBuilder data = new StringBuilder();

        LocalDateTime[] range = extractDateRange(lower);
        LocalDateTime tuNgay = range[0];
        LocalDateTime denNgay = range[1];

        // 1. TRA CỨU MÃ ĐƠN HÀNG CỤ THỂ (NẾU CÓ)
        Pattern orderPattern = Pattern.compile("(?i)\\b([0-9a-f]{12}|dh[0-9a-z]+|don[0-9a-z]+)\\b");
        Matcher orderMatcher = orderPattern.matcher(cauHoi);
        if (orderMatcher.find()) {
            String maDon = orderMatcher.group(1);
            Optional<DonHang> donOpt = donHangRepository.findByMaDonHang(maDon);
            if (donOpt.isPresent()) {
                DonHang d = donOpt.get();
                data.append("=== THÔNG TIN CHI TIẾT ĐƠN HÀNG: ").append(d.getMaDonHang()).append(" ===\n");
                data.append("• Người nhận: ").append(d.getNguoiNhan() != null ? d.getNguoiNhan() : "Chưa có").append("\n");
                data.append("• SĐT nhận: ").append(d.getSdtNguoiNhan() != null ? d.getSdtNguoiNhan() : "Chưa có").append("\n");
                data.append("• Địa chỉ: ").append(d.getDiaChiGiaoHangText() != null ? d.getDiaChiGiaoHangText() : "Tại quầy").append("\n");
                data.append("• Kênh bán: ").append(d.getKenhBan()).append("\n");
                data.append("• Trạng thái đơn: ").append(translateOrderStatus(d.getTrangThaiDonHang())).append("\n");
                data.append("• Thanh toán: ").append(d.getTrangThaiThanhToan()).append(" (").append(d.getPhuongThucThanhToan() != null ? d.getPhuongThucThanhToan() : "Chưa chọn").append(")\n");
                data.append("• Tổng tiền: ").append(formatCurrency(d.getTongTien())).append("\n");
                data.append("• Giảm giá: ").append(formatCurrency(d.getGiamGia())).append("\n");
                data.append("• Phí vận chuyển: ").append(formatCurrency(d.getPhiVanChuyen())).append("\n");
                data.append("• Thành tiền: ").append(formatCurrency(d.getThanhTien())).append("\n");
                data.append("• Ngày đặt: ").append(formatDateTime(d.getNgayDat())).append("\n");
                if (d.getMaVanDon() != null) data.append("• Mã vận đơn: ").append(d.getMaVanDon()).append("\n");
                data.append("\n");
            }
        }

        // 2. TRA CỨU SỐ ĐIỆN THOẠI KHÁCH HÀNG (NẾU CÓ)
        Pattern phonePattern = Pattern.compile("\\b(0[35789][0-9]{8})\\b");
        Matcher phoneMatcher = phonePattern.matcher(cauHoi);
        if (phoneMatcher.find()) {
            String sdt = phoneMatcher.group(1);
            Optional<KhachHang> khOpt = khachHangRepository.findBySoDienThoai(sdt);
            if (khOpt.isPresent()) {
                KhachHang kh = khOpt.get();
                data.append("=== THÔNG TIN KHÁCH HÀNG SĐT ").append(sdt).append(" ===\n");
                data.append("• Họ tên: ").append(kh.getHoTen()).append("\n");
                data.append("• Email: ").append(kh.getEmail() != null ? kh.getEmail() : "Chưa có").append("\n");
                data.append("• Địa chỉ: ").append(kh.getDiaChi() != null ? kh.getDiaChi() : "Chưa có").append("\n");
                data.append("• Điểm tích lũy: ").append(kh.getDiemTichLuy()).append(" điểm\n");

                List<DonHang> ordersOfCust = donHangRepository.findByKhachHang_SoDienThoai(sdt);
                data.append("• Tổng số đơn đã đặt: ").append(ordersOfCust.size()).append(" đơn\n");
                if (!ordersOfCust.isEmpty()) {
                    data.append("Danh sách đơn hàng:\n");
                    for (DonHang od : ordersOfCust.stream().limit(5).toList()) {
                        data.append("  - Đơn ").append(od.getMaDonHang())
                                .append(" | ").append(formatCurrency(od.getThanhTien()))
                                .append(" | ").append(translateOrderStatus(od.getTrangThaiDonHang()))
                                .append(" | ").append(formatDateTime(od.getNgayDat())).append("\n");
                    }
                }
                data.append("\n");
            }
        }

        // 3. THÔNG TIN CỬA HÀNG & CÀI ĐẶT HỆ THỐNG
        if (matchesAny(lower, "cửa hàng", "shop", "địa chỉ", "hotline", "số điện thoại", "email", "liên hệ", "giờ mở cửa", "giờ làm việc")) {
            try {
                var caiDatList = caiDatHeThongRepository.findAll();
                if (!caiDatList.isEmpty()) {
                    CaiDatHeThong c = caiDatList.get(0);
                    data.append("=== THÔNG TIN CỬA HÀNG SAOCLUB ===\n");
                    data.append("• Tên cửa hàng: ").append(c.getTenCuaHang() != null ? c.getTenCuaHang() : "SAOClub").append("\n");
                    if (c.getDiaChi() != null) data.append("• Địa chỉ: ").append(c.getDiaChi()).append("\n");
                    if (c.getSoDienThoai() != null) data.append("• Hotline: ").append(c.getSoDienThoai()).append("\n");
                    if (c.getEmail() != null) data.append("• Email liên hệ: ").append(c.getEmail()).append("\n");
                    if (c.getNguongTonKhoMacDinh() != null) data.append("• Ngưỡng tồn kho mặc định: ").append(c.getNguongTonKhoMacDinh()).append(" chiếc\n");
                    data.append("• Giờ mở cửa: 8h00 - 21h30 (tất cả các ngày trong tuần)\n\n");
                }
            } catch (Exception ignored) {}
        }

        // 4. KIẾN THỨC CỬA HÀNG (CHÍNH SÁCH, BẢO HÀNH, ĐỔI TRẢ, THANH TOÁN, VẬN CHUYỂN)
        try {
            List<AiKienThuc> matchedKienThuc = aiKienThucRepository.searchByKeyword(cauHoi);
            if (matchedKienThuc.isEmpty() && matchesAny(lower, "chính sách", "bảo hành", "đổi trả", "thanh toán", "vận chuyển", "giao hàng", "faq", "quy định", "tích điểm", "hướng dẫn")) {
                matchedKienThuc = aiKienThucRepository.findGeneralKnowledge();
            }
            if (!matchedKienThuc.isEmpty()) {
                data.append("=== CHÍNH SÁCH & HƯỚNG DẪN CỬA HÀNG ===\n");
                for (AiKienThuc kt : matchedKienThuc.stream().limit(3).toList()) {
                    data.append("• ").append(kt.getTieuDe()).append(":\n  ").append(kt.getNoiDung().trim()).append("\n");
                }
                data.append("\n");
            }
        } catch (Exception ignored) {}

        // 5. KHUYẾN MÃI & GIẢM GIÁ
        if (matchesAny(lower, "khuyến mãi", "khuyến mại", "giảm giá", "voucher", "mã giảm", "ưu đãi", "discount", "sale")) {
            try {
                List<KhuyenMai> kmList = khuyenMaiRepository.findAll();
                List<KhuyenMai> activeKm = kmList.stream()
                        .filter(k -> "active".equalsIgnoreCase(k.getTrangThai()))
                        .toList();

                data.append("=== CHƯƠNG TRÌNH KHUYẾN MÃI & VOUCHER ===\n");
                if (!activeKm.isEmpty()) {
                    data.append("Các khuyến mãi đang áp dụng:\n");
                    for (KhuyenMai k : activeKm) {
                        String giam = "percent".equalsIgnoreCase(k.getLoai())
                                ? (k.getGiaTri() + "% (tối đa " + formatCurrency(k.getGiaTriToiDa()) + ")")
                                : formatCurrency(k.getGiaTri());
                        data.append("• Mã: [").append(k.getMaKhuyenMai()).append("] - ").append(k.getTenKhuyenMai())
                                .append(" (Giảm: ").append(giam)
                                .append(", Đơn tối thiểu: ").append(formatCurrency(k.getDonHangToiThieu()))
                                .append(")\n");
                    }
                } else {
                    data.append("Hiện tại không có chương trình khuyến mãi nào đang kích hoạt.\n");
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 6. PHIẾU BẢO HÀNH & KỸ THUẬT
        if (matchesAny(lower, "phiếu bảo hành", "tình trạng bảo hành", "bảo hành xử lý", "sửa chữa", "tiếp nhận bảo hành")) {
            try {
                var allBh = phieuBaoHanhRepository.findAll();
                long dangXuLy = allBh.stream().filter(b -> "dang_xu_ly".equalsIgnoreCase(b.getTrangThai())).count();
                long conBh = allBh.stream().filter(b -> "con_bao_hanh".equalsIgnoreCase(b.getTrangThai())).count();
                long daXuLy = allBh.stream().filter(b -> "da_xu_ly".equalsIgnoreCase(b.getTrangThai())).count();

                data.append("=== THỐNG KÊ PHIẾU BẢO HÀNH ===\n");
                data.append("• Tổng số phiếu bảo hành ghi nhận: ").append(allBh.size()).append(" phiếu\n");
                data.append("• Đang trong quá trình xử lý/sửa chữa: ").append(dangXuLy).append(" phiếu\n");
                data.append("• Máy còn hạn bảo hành: ").append(conBh).append(" phiếu\n");
                data.append("• Đã xử lý & hoàn trả: ").append(daXuLy).append(" phiếu\n\n");
            } catch (Exception ignored) {}
        }

        // 7. TỒN KHO & CẢNH BÁO SẮP HẾT HÀNG
        if (matchesAny(lower, "tồn kho", "hết hàng", "sắp hết", "kho", "inventory", "tồn", "cảnh báo kho")) {
            try {
                List<TonKhoResponse> allStock = tonKhoRepository.findAllAsResponse();
                long lowStockCount = tonKhoRepository.countLowStock();
                List<TonKhoResponse> lowStockList = allStock.stream()
                        .filter(t -> t.getTonKhoToiThieu() != null && t.getSoLuongTon() != null && t.getSoLuongTon() <= t.getTonKhoToiThieu())
                        .sorted(Comparator.comparingInt(TonKhoResponse::getSoLuongTon))
                        .toList();

                data.append("=== TỒN KHO & CẢNH BÁO SẮP HẾT HÀNG ===\n");
                data.append("Tổng số biến thể quản lý trong kho: ").append(allStock.size()).append(" dòng\n");
                data.append("Số sản phẩm sắp hết hàng (tồn ≤ ngưỡng cảnh báo): ").append(lowStockCount).append(" dòng máy\n");

                if (!lowStockList.isEmpty()) {
                    data.append("DANH SÁCH CHI TIẾT SẢN PHẨM CHẠM NGƯỠNG CẦN NHẬP BỔ SUNG:\n");
                    for (TonKhoResponse tk : lowStockList) {
                        data.append("• ").append(tk.getTenSanPham())
                                .append(" (Màu: ").append(tk.getMauSac() != null ? tk.getMauSac() : "Tiêu chuẩn")
                                .append(" | SKU: ").append(tk.getMaSku()).append(")")
                                .append(" - Tồn thực tế: ").append(tk.getSoLuongTon()).append(" chiếc")
                                .append(" (Ngưỡng: ").append(tk.getTonKhoToiThieu()).append(" chiếc")
                                .append(", Đã bán: ").append(tk.getSoLuongDaBan() != null ? tk.getSoLuongDaBan() : 0).append(" chiếc)\n");
                    }
                } else {
                    data.append("Tất cả sản phẩm hiện đang ở mức tồn kho an toàn (> ngưỡng tối thiểu).\n");
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 8. ĐƠN HÀNG CHI TIẾT
        if (matchesAny(lower, "đơn hàng", "đơn", "order", "hóa đơn", "chờ xử lý", "chờ duyệt", "đang giao", "đã giao", "hủy đơn", "hoàn trả")) {
            try {
                long tongDon = donHangRepository.count();
                long pending = donHangRepository.countByTrangThaiDonHang("pending");
                long confirmed = donHangRepository.countByTrangThaiDonHang("confirmed");
                long processing = donHangRepository.countByTrangThaiDonHang("processing");
                long shipping = donHangRepository.countByTrangThaiDonHang("shipping");
                long delivered = donHangRepository.countByTrangThaiDonHang("delivered");
                long cancelled = donHangRepository.countByTrangThaiDonHang("cancelled");
                long returned = donHangRepository.countByTrangThaiDonHang("returned");

                data.append("=== THỐNG KÊ ĐƠN HÀNG HỆ THỐNG ===\n");
                data.append("Tổng số đơn hàng: ").append(tongDon).append(" đơn\n");
                data.append("Phân loại trạng thái:\n");
                data.append("• Chờ xử lý (pending): ").append(pending).append(" đơn (Cần Admin/Nhân viên duyệt)\n");
                data.append("• Đã lên đơn (confirmed): ").append(confirmed).append(" đơn\n");
                data.append("• Đang đóng gói (processing): ").append(processing).append(" đơn\n");
                data.append("• Đang giao hàng (shipping): ").append(shipping).append(" đơn\n");
                data.append("• Đã giao thành công (delivered): ").append(delivered).append(" đơn\n");
                data.append("• Đã hủy (cancelled): ").append(cancelled).append(" đơn\n");
                data.append("• Đã hoàn trả (returned): ").append(returned).append(" đơn\n");

                var recentPage = donHangRepository.hienThiDonHang(null, PageRequest.of(0, 5));
                if (recentPage.hasContent()) {
                    data.append("5 ĐƠN HÀNG MỚI NHẤT:\n");
                    for (var dh : recentPage.getContent()) {
                        data.append("• Đơn ").append(dh.getMaDonHang())
                                .append(" - Khách: ").append(dh.getNguoiNhan())
                                .append(" | Giá trị: ").append(formatCurrency(dh.getThanhTien()))
                                .append(" | Trạng thái: ").append(translateOrderStatus(dh.getTrangThaiDonHang()))
                                .append(" | Ngày: ").append(formatDateTime(dh.getNgayDat())).append("\n");
                    }
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 9. SẢN PHẨM & TÌM KIẾM THEO HÃNG / CẤU HÌNH / DÒNG MÁY
        if (matchesAny(lower, "sản phẩm", "laptop", "máy tính", "hàng", "bán chạy", "bán chậm", "top", "giá", "cấu hình", "acer", "asus", "dell", "hp", "lenovo", "msi", "apple", "nitro", "legion", "xps", "vivobook", "loq", "tuf", "zenbook", "thinkpad", "ideapad")) {
            try {
                long tongSanPham = sanPhamRepository.count();
                data.append("=== THỐNG KÊ SẢN PHẨM ===\n");
                data.append("Tổng số dòng sản phẩm: ").append(tongSanPham).append(" mẫu máy\n");

                // Bán chạy
                if (matchesAny(lower, "bán chạy", "top", "nhiều nhất", "phổ biến", "best seller")) {
                    LocalDateTime from = tuNgay != null ? tuNgay : LocalDate.now().minusMonths(1).atStartOfDay();
                    LocalDateTime to = denNgay != null ? denNgay : LocalDate.now().atTime(LocalTime.MAX);
                    var topSelling = sanPhamRepository.topSelling(from, to, PageRequest.of(0, 5));
                    if (!topSelling.isEmpty()) {
                        data.append("Top 5 sản phẩm bán chạy nhất:\n");
                        for (int i = 0; i < topSelling.size(); i++) {
                            ProductSalesResponse sp = topSelling.get(i);
                            data.append(i + 1).append(". ").append(sp.getTenSanPham())
                                    .append(" — ").append(sp.getSoLuongDaBan()).append(" chiếc\n");
                        }
                    }
                }

                // Bán chậm
                if (matchesAny(lower, "bán chậm", "ít bán", "tồn đọng", "ế", "ít mua")) {
                    var slow = sanPhamRepository.slowSelling(LocalDate.now().minusMonths(3).atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX), PageRequest.of(0, 5));
                    if (!slow.isEmpty()) {
                        data.append("Top 5 sản phẩm bán chậm (3 tháng gần đây):\n");
                        for (int i = 0; i < slow.size(); i++) {
                            data.append(i + 1).append(". ").append(slow.get(i).getTenSanPham())
                                    .append(" — ").append(slow.get(i).getSoLuongDaBan()).append(" chiếc\n");
                        }
                    }
                }

                // Tìm kiếm sản phẩm theo từ khóa
                String searchKw = extractSearchKeyword(lower);
                if (searchKw != null && !searchKw.isBlank()) {
                    var searchResults = sanPhamRepository.hienThiSanPham(searchKw, null, null, null, PageRequest.of(0, 6));
                    if (searchResults.hasContent()) {
                        data.append("DANH SÁCH SẢN PHẨM PHÙ HỢP ('").append(searchKw).append("'):\n");
                        for (var sp : searchResults.getContent()) {
                            data.append("• ").append(sp.getTenSanPham())
                                    .append(" [Hãng: ").append(sp.getTenThuongHieu()).append(" | SKU: ").append(sp.getMaSku()).append("]\n")
                                    .append("  - Cấu hình: ").append(sp.getCpu() != null ? sp.getCpu() : "").append(" / ")
                                    .append(sp.getRam() != null ? sp.getRam() : "").append(" / ")
                                    .append(sp.getOCung() != null ? sp.getOCung() : "").append(" / ")
                                    .append(sp.getGpu() != null ? sp.getGpu() : "").append("\n")
                                    .append("  - Giá bán: ").append(formatCurrency(sp.getGiaBan()))
                                    .append(" (Giá vốn: ").append(formatCurrency(sp.getGiaNhap())).append(")")
                                    .append(" | Tồn kho: ").append(sp.getSoLuongTon() != null ? sp.getSoLuongTon() : 0).append(" chiếc\n");
                        }
                    }
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 10. DOANH THU
        if (matchesAny(lower, "doanh thu", "doanh số", "thu nhập", "revenue", "tiền bán")) {
            try {
                if (tuNgay != null && denNgay != null) {
                    List<RevenueByDayResponse> rev = donHangRepository.doanhThuTheoNgay(tuNgay, denNgay);
                    BigDecimal tong = rev.stream().map(RevenueByDayResponse::getDoanhThu)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    data.append("=== DOANH THU (").append(formatDateRange(tuNgay, denNgay)).append(") ===\n");
                    data.append("Tổng doanh thu: ").append(formatCurrency(tong)).append("\n");
                    data.append("Số ngày có giao dịch: ").append(rev.size()).append(" ngày\n");
                } else {
                    BigDecimal tongDoanhThu = donHangRepository.sumDoanhThu();
                    data.append("=== DOANH THU HỆ THỐNG ===\n");
                    data.append("Tổng doanh thu toàn thời gian: ").append(formatCurrency(tongDoanhThu)).append("\n");

                    LocalDateTime s = LocalDate.now().atStartOfDay();
                    LocalDateTime e = LocalDate.now().atTime(LocalTime.MAX);
                    List<RevenueByDayResponse> todayList = donHangRepository.doanhThuTheoNgay(s, e);
                    BigDecimal homNay = todayList.stream().map(RevenueByDayResponse::getDoanhThu)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    data.append("Doanh thu hôm nay: ").append(formatCurrency(homNay)).append("\n");

                    LocalDateTime startMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                    List<RevenueByDayResponse> thangNay = donHangRepository.doanhThuTheoNgay(startMonth, e);
                    BigDecimal tongThang = thangNay.stream().map(RevenueByDayResponse::getDoanhThu)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    data.append("Doanh thu tháng này: ").append(formatCurrency(tongThang)).append("\n");
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 11. KHÁCH HÀNG & NHÂN SỰ
        if (matchesAny(lower, "khách hàng", "khách", "customer", "thành viên", "chi tiêu")) {
            try {
                long tongKhach = khachHangRepository.count();
                data.append("=== THỐNG KÊ KHÁCH HÀNG ===\n");
                data.append("Tổng số khách hàng đăng ký: ").append(tongKhach).append(" người\n");

                var topKhach = khachHangRepository.chiTieuTheoKhachHang(null, null, PageRequest.of(0, 5));
                if (!topKhach.isEmpty()) {
                    data.append("Top khách hàng chi tiêu nhiều nhất:\n");
                    for (int i = 0; i < topKhach.size(); i++) {
                        var k = topKhach.get(i);
                        data.append(i + 1).append(". ").append(k.getHoTen())
                                .append(" — ").append(k.getSoDonHang()).append(" đơn")
                                .append(" | Tổng chi: ").append(formatCurrency(k.getTongChiTieu())).append("\n");
                    }
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 12. NHÂN SỰ & NHÂN VIÊN
        if (matchesAny(lower, "nhân viên", "nhân sự", "staff", "lương", "tài khoản nhân viên")) {
            try {
                long tongNv = nhanVienRepository.count();
                var dsNv = nhanVienRepository.hienThiNhanVien();
                data.append("=== THỐNG KÊ NHÂN SỰ CỬA HÀNG ===\n");
                data.append("• Tổng số nhân sự: ").append(tongNv).append(" người\n");
                if (!dsNv.isEmpty()) {
                    data.append("Danh sách nhân viên:\n");
                    for (var nv : dsNv) {
                        data.append("• ").append(nv.getHoTen())
                                .append(" - SĐT: ").append(nv.getSoDienThoai() != null ? nv.getSoDienThoai() : "Chưa có")
                                .append(" | Tài khoản: ").append(nv.getUsername() != null ? nv.getUsername() : "Chưa liên kết")
                                .append(" | Trạng thái: ").append(nv.getTrangThai() != null ? nv.getTrangThai() : "Hoạt động")
                                .append(" | Lương cơ bản: ").append(formatCurrency(nv.getLuongCoBan())).append("\n");
                    }
                }
                data.append("\n");
            } catch (Exception ignored) {}
        }

        // 13. DANH MỤC & THƯƠNG HIỆU
        if (matchesAny(lower, "danh mục", "thương hiệu", "hãng", "category", "brand")) {
            try {
                var brands = thuongHieuRepository.hienThiThuongHieu();
                var categories = danhMucRepository.hienThiDanhMuc();
                data.append("=== DANH MỤC & THƯƠNG HIỆU KINH DOANH ===\n");
                data.append("• Các thương hiệu: ");
                data.append(brands.stream().map(com.example.backend.response.ThuongHieuResponse::getTenThuongHieu).collect(java.util.stream.Collectors.joining(", "))).append("\n");
                data.append("• Các danh mục: ");
                data.append(categories.stream().map(com.example.backend.response.DanhMucResponse::getTenDanhMuc).collect(java.util.stream.Collectors.joining(", "))).append("\n\n");
            } catch (Exception ignored) {}
        }

        // 14. LỢI NHUẬN & BIÊN LỢI NHUẬN
        if (matchesAny(lower, "lợi nhuận", "lãi", "biên lợi nhuận", "margin", "chênh lệch giá")) {
            try {
                var productsPage = sanPhamRepository.hienThiSanPham(null, null, null, null, PageRequest.of(0, 50));
                BigDecimal tongGiaTriBan = BigDecimal.ZERO;
                BigDecimal tongGiaTriNhap = BigDecimal.ZERO;
                for (var item : productsPage.getContent()) {
                    if (item.getGiaBan() != null && item.getGiaNhap() != null && item.getSoLuongTon() != null && item.getSoLuongTon() > 0) {
                        BigDecimal ton = BigDecimal.valueOf(item.getSoLuongTon());
                        tongGiaTriBan = tongGiaTriBan.add(item.getGiaBan().multiply(ton));
                        tongGiaTriNhap = tongGiaTriNhap.add(item.getGiaNhap().multiply(ton));
                    }
                }
                BigDecimal chenhLech = tongGiaTriBan.subtract(tongGiaTriNhap);
                data.append("=== PHÂN TÍCH GIÁ TRỊ & BIÊN LỢI NHUẬN TỒN KHO ===\n");
                data.append("• Tổng giá trị kho theo giá bán dự kiến: ").append(formatCurrency(tongGiaTriBan)).append("\n");
                data.append("• Tổng giá trị kho theo vốn giá nhập: ").append(formatCurrency(tongGiaTriNhap)).append("\n");
                data.append("• Chênh lệch lợi nhuận gộp ước tính trong kho: ").append(formatCurrency(chenhLech)).append("\n\n");
            } catch (Exception ignored) {}
        }

        // 15. TỔNG QUAN HỆ THỐNG (FALLBACK NẾU CHƯA CÓ NỘI DUNG)
        if (data.length() == 0) {
            try {
                long tongDon = donHangRepository.count();
                long tongKhach = khachHangRepository.count();
                long tongSanPham = sanPhamRepository.count();
                long lowStock = tonKhoRepository.countLowStock();
                BigDecimal tongDoanhThu = donHangRepository.sumDoanhThu();

                data.append("=== TỔNG QUAN HỆ THỐNG SAOCLUB ===\n");
                data.append("• Tổng doanh thu toàn thời gian: ").append(formatCurrency(tongDoanhThu)).append("\n");
                data.append("• Tổng đơn hàng: ").append(tongDon).append(" đơn (Đơn chờ duyệt: ").append(donHangRepository.countByTrangThaiDonHang("pending")).append(")\n");
                data.append("• Tổng khách hàng: ").append(tongKhach).append(" người\n");
                data.append("• Tổng dòng sản phẩm: ").append(tongSanPham).append(" mẫu\n");
                data.append("• Sản phẩm sắp hết hàng (tồn ≤ ngưỡng): ").append(lowStock).append(" sản phẩm\n");
            } catch (Exception ignored) {}
        }

        return data.toString();
    }

    // ─── Trích xuất từ khóa tìm kiếm sản phẩm ────────────────────────────────
    private String extractSearchKeyword(String lower) {
        for (String brand : List.of("acer", "asus", "dell", "hp", "lenovo", "msi", "apple")) {
            if (lower.contains(brand)) return brand;
        }
        for (String modelName : List.of("nitro", "legion", "vivobook", "xps", "aspire", "loq", "tuf", "zenbook", "stealth", "thinkpad", "ideapad", "victus", "pavilion", "envy", "vostro", "inspiron", "macbook")) {
            if (lower.contains(modelName)) return modelName;
        }
        if (lower.contains("gaming")) return "gaming";
        if (lower.contains("văn phòng")) return "văn phòng";

        Pattern p = Pattern.compile("(?:tìm|giá|về|xem|mua|thông tin)\\s+(?:laptop|máy tính|sản phẩm)?\\s*([a-zA-Z0-9\\s]{2,20})");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            String kw = m.group(1).trim();
            if (!kw.isBlank() && !matchesAny(kw, "nào", "gì", "bao nhiêu", "đâu", "hôm nay", "tháng này", "bán chạy", "sắp hết", "tồn kho")) {
                return kw;
            }
        }
        return null;
    }

    // ─── Trích xuất khoảng thời gian ──────────────────────────────────────────
    private LocalDateTime[] extractDateRange(String lower) {
        LocalDate today = LocalDate.now();
        LocalDateTime tuNgay = null;
        LocalDateTime denNgay = null;

        if (lower.contains("hôm nay")) {
            tuNgay = today.atStartOfDay();
            denNgay = today.atTime(LocalTime.MAX);
        } else if (lower.contains("hôm qua")) {
            tuNgay = today.minusDays(1).atStartOfDay();
            denNgay = today.minusDays(1).atTime(LocalTime.MAX);
        } else if (matchesAny(lower, "tuần này", "tuần hiện tại")) {
            tuNgay = today.with(DayOfWeek.MONDAY).atStartOfDay();
            denNgay = today.atTime(LocalTime.MAX);
        } else if (matchesAny(lower, "tuần trước")) {
            tuNgay = today.minusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay();
            denNgay = today.minusWeeks(1).with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        } else if (matchesAny(lower, "tháng này", "tháng hiện tại")) {
            tuNgay = today.withDayOfMonth(1).atStartOfDay();
            denNgay = today.atTime(LocalTime.MAX);
        } else if (lower.contains("tháng trước")) {
            tuNgay = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
            denNgay = today.withDayOfMonth(1).minusDays(1).atTime(LocalTime.MAX);
        } else if (matchesAny(lower, "năm nay", "năm này")) {
            tuNgay = today.withDayOfYear(1).atStartOfDay();
            denNgay = today.atTime(LocalTime.MAX);
        } else if (matchesAny(lower, "7 ngày", "tuần qua")) {
            tuNgay = today.minusDays(7).atStartOfDay();
            denNgay = today.atTime(LocalTime.MAX);
        } else if (lower.contains("30 ngày")) {
            tuNgay = today.minusDays(30).atStartOfDay();
            denNgay = today.atTime(LocalTime.MAX);
        }

        Matcher m = Pattern.compile("tháng (\\d{1,2})").matcher(lower);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int year = today.getYear();
            if (month > today.getMonthValue()) year--;
            try {
                tuNgay = LocalDate.of(year, month, 1).atStartOfDay();
                denNgay = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).atTime(LocalTime.MAX);
            } catch (Exception ignored) {}
        }

        return new LocalDateTime[]{tuNgay, denNgay};
    }

    // ─── Build prompt chuẩn xác cho Admin ──────────────────────────────────────
    private String buildAdminPrompt(String cauHoi, String dataContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là Trợ lý AI Quản trị (Admin AI Assistant) của cửa hàng máy tính SAOClub.\n");
        prompt.append("Bạn đang hỗ trợ trực tiếp cho Admin (Quản trị viên / Chủ cửa hàng).\n\n");

        prompt.append("QUY TẮC BẮT BUỘC:\n");
        prompt.append("1. TRẢ LỜI TRỰC DIỆN, ĐI THẲNG VÀO SỐ LIỆU THỰC TẾ. TUYỆT ĐỐI KHÔNG mở đầu bằng câu chào dài dòng, không triết lý thừa thãi, không viết văn sáo rỗng kiểu 'Là AI Assistant tôi nhận thức được tầm quan trọng...'.\n");
        prompt.append("2. DỰA 100% VÀO DỮ LIỆU THỰC TẾ ĐƯỢC CUNG CẤP DƯỚI ĐÂY. Nếu hỏi về sản phẩm/tồn kho/đơn hàng/chính sách/cửa hàng, hãy nêu rõ thông tin cụ thể (tên máy, SKU, số lượng, giá tiền, trạng thái, địa chỉ, hotline).\n");
        prompt.append("3. Trình bày rõ ràng bằng Markdown (danh sách gạch đầu dòng *, in đậm **, bảng Table nếu liệt kê sản phẩm/đơn hàng) để Admin nắm bắt nhanh nhất.\n");
        prompt.append("4. Tiền tệ định dạng chuẩn VNĐ (ví dụ: 25.000.000 VNĐ).\n");
        prompt.append("5. Đưa ra 1-2 dòng gợi ý hành động thiết thực ở cuối khi phù hợp.\n\n");

        if (!dataContext.isBlank()) {
            prompt.append("--- DỮ LIỆU THỰC TẾ TỪ CƠ SỞ DỮ LIỆU CỬA HÀNG ---\n");
            prompt.append(dataContext);
            prompt.append("--------------------------------------------------\n\n");
        }

        prompt.append("Câu hỏi của Admin: ").append(cauHoi).append("\n");
        prompt.append("Câu trả lời trực diện:");

        return prompt.toString();
    }

    // ─── Gọi Ollama API ───────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String goiOllama(String prompt, List<Map<String, String>> lichSuChat) {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "Bạn là Trợ lý AI Quản trị cho SAOClub. Luôn trả lời bằng tiếng Việt, đi thẳng vào số liệu thực tế, súc tích, chuyên nghiệp.");
        messages.add(systemMsg);

        if (lichSuChat != null && !lichSuChat.isEmpty()) {
            int start = Math.max(0, lichSuChat.size() - 6);
            for (int i = start; i < lichSuChat.size(); i++) {
                Map<String, String> turn = lichSuChat.get(i);
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", turn.getOrDefault("role", "user"));
                msg.put("content", turn.getOrDefault("content", ""));
                messages.add(msg);
            }
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        Map<String, Object> response = webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("message")) {
            Map<String, Object> msg = (Map<String, Object>) response.get("message");
            String content = (String) msg.get("content");
            return content != null ? content.trim() : fallbackResponse();
        }

        return fallbackResponse();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return String.format("%,.0f VNĐ", amount).replace(",", ".");
    }

    private String formatDateRange(LocalDateTime from, LocalDateTime to) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return from.format(fmt) + " – " + to.format(fmt);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String translateOrderStatus(String status) {
        if (status == null) return "Chưa xác định";
        return switch (status.toLowerCase()) {
            case "pending" -> "Chờ xử lý";
            case "confirmed" -> "Đã lên đơn";
            case "processing" -> "Đang đóng gói";
            case "shipping" -> "Đang giao hàng";
            case "delivered" -> "Đã giao thành công";
            case "cancelled" -> "Đã hủy";
            case "returned" -> "Đã hoàn trả";
            default -> status;
        };
    }

    private String fallbackResponse() {
        return "⚠️ Xin lỗi Admin, hiện tại hệ thống AI đang bận hoặc chưa nhận đủ dữ liệu phản hồi.\n\n" +
                "Bạn có thể thử lại với các câu lệnh như:\n" +
                "• 'Sản phẩm sắp hết hàng'\n" +
                "• 'Thống kê đơn hàng'\n" +
                "• 'Tìm laptop Asus / Acer / Dell'\n" +
                "• 'Khuyến mãi đang áp dụng'\n" +
                "• 'Chính sách bảo hành / đổi trả'";
    }
}
