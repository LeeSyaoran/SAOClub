package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.response.DanhMucResponse;
import com.example.backend.response.ProductSalesResponse;
import com.example.backend.response.SanPhamResponse;
import com.example.backend.response.ThuongHieuResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final String ollamaUrl;
    private final String model;
    private final boolean enabled;
    private final WebClient webClient;

    private final AiKienThucRepository aiKienThucRepository;
    private final SanPhamRepository sanPhamRepository;
    private final CaiDatHeThongRepository caiDatHeThongRepository;
    private final KhuyenMaiRepository khuyenMaiRepository;
    private final DonHangRepository donHangRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;
    private final KhachHangRepository khachHangRepository;

    public AiChatService(
            @Value("${ai.ollama.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ai.ollama.model:llama3.2}") String model,
            @Value("${ai.ollama.enabled:true}") boolean enabled,
            AiKienThucRepository aiKienThucRepository,
            SanPhamRepository sanPhamRepository,
            CaiDatHeThongRepository caiDatHeThongRepository,
            KhuyenMaiRepository khuyenMaiRepository,
            DonHangRepository donHangRepository,
            ThuongHieuRepository thuongHieuRepository,
            DanhMucRepository danhMucRepository,
            KhachHangRepository khachHangRepository) {
        this.ollamaUrl = ollamaUrl;
        this.model = model;
        this.enabled = enabled;
        this.aiKienThucRepository = aiKienThucRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.caiDatHeThongRepository = caiDatHeThongRepository;
        this.khuyenMaiRepository = khuyenMaiRepository;
        this.donHangRepository = donHangRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.danhMucRepository = danhMucRepository;
        this.khachHangRepository = khachHangRepository;
        this.webClient = WebClient.builder().baseUrl(ollamaUrl).build();
    }

    // Lấy tin nhắn chào mừng
    public String getWelcomeMessage() {
        return "Xin chào! 👋 Mình là trợ lý AI chính thức của **SAOClub**.\n\n" +
                "Mình có thể hỗ trợ bạn mọi thông tin liên quan đến cửa hàng:\n\n" +
                "💻 **Tư vấn laptop** — tìm máy theo nhu cầu (Gaming, Văn phòng, Đồ họa) & ngân sách\n" +
                "💰 **Báo giá & Cấu hình** — giá bán chính xác, cấu hình chi tiết & tình trạng còn hàng\n" +
                "🎁 **Khuyến mãi** — cập nhật mã giảm giá & voucher đang áp dụng\n" +
                "📦 **Tra cứu đơn hàng** — kiểm tra tiến độ đơn qua mã đơn hoặc số điện thoại\n" +
                "🛡️ **Chính sách** — bảo hành chính hãng, đổi trả, trả góp 0%\n" +
                "🏪 **Cửa hàng** — địa chỉ, hotline & giờ mở cửa\n\n" +
                "Bạn cần hỗ trợ điều gì hôm nay? *(Nếu cần nói chuyện với nhân viên người thật, bạn có thể gõ **\"chuyển nhân viên\"** bất cứ lúc nào nhé!)*";
    }

    // Trả lời câu hỏi của khách hàng
    public String traLoi(String cauHoi, Long cuocTroChuyenId) {
        if (!enabled) {
            return fallbackResponse();
        }

        try {
            String context = layContext(cauHoi);
            String prompt = buildPrompt(cauHoi, context);
            return goiOllama(prompt);
        } catch (Exception e) {
            return fallbackResponse();
        }
    }

    // Lấy ngữ cảnh toàn diện từ cơ sở dữ liệu hệ thống
    private String layContext(String cauHoi) {
        String lower = cauHoi.toLowerCase();
        StringBuilder context = new StringBuilder();

        // 1. TRA CỨU ĐƠN HÀNG (MÃ ĐƠN HOẶC SỐ ĐIỆN THOẠI)
        Pattern orderPattern = Pattern.compile("(?i)\\b([0-9a-f]{12}|dh[0-9a-z]+|don[0-9a-z]+)\\b");
        Matcher orderMatcher = orderPattern.matcher(cauHoi);
        boolean hasOrderCode = orderMatcher.find();
        if (hasOrderCode) {
            String maDon = orderMatcher.group(1);
            Optional<DonHang> donOpt = donHangRepository.findByMaDonHang(maDon);
            if (donOpt.isPresent()) {
                DonHang d = donOpt.get();
                context.append("=== THÔNG TIN ĐƠN HÀNG TRA CỨU: ").append(d.getMaDonHang()).append(" ===\n");
                context.append("• Người nhận: ").append(d.getNguoiNhan() != null ? d.getNguoiNhan() : "Khách hàng").append("\n");
                context.append("• SĐT nhận hàng: ").append(d.getSdtNguoiNhan() != null ? d.getSdtNguoiNhan() : "").append("\n");
                context.append("• Địa chỉ giao: ").append(d.getDiaChiGiaoHangText() != null ? d.getDiaChiGiaoHangText() : "Nhận tại quầy").append("\n");
                context.append("• Trạng thái đơn: ").append(translateOrderStatus(d.getTrangThaiDonHang())).append("\n");
                context.append("• Thanh toán: ").append(d.getTrangThaiThanhToan()).append(" (").append(d.getPhuongThucThanhToan() != null ? d.getPhuongThucThanhToan() : "Chưa chọn").append(")\n");
                context.append("• Thành tiền: ").append(formatCurrency(d.getThanhTien())).append("\n");
                context.append("• Ngày đặt: ").append(formatDateTime(d.getNgayDat())).append("\n");
                if (d.getMaVanDon() != null) context.append("• Mã vận đơn: ").append(d.getMaVanDon()).append("\n");
                context.append("\n");
            }
        }

        Pattern phonePattern = Pattern.compile("\\b(0[35789][0-9]{8})\\b");
        Matcher phoneMatcher = phonePattern.matcher(cauHoi);
        if (phoneMatcher.find() && !hasOrderCode) {
            String sdt = phoneMatcher.group(1);
            List<DonHang> orders = donHangRepository.findByKhachHang_SoDienThoai(sdt);
            if (!orders.isEmpty()) {
                context.append("=== DANH SÁCH ĐƠN HÀNG THEO SĐT ").append(sdt).append(" ===\n");
                for (DonHang od : orders.stream().limit(3).toList()) {
                    context.append("• Đơn [").append(od.getMaDonHang()).append("]")
                            .append(" - Trạng thái: ").append(translateOrderStatus(od.getTrangThaiDonHang()))
                            .append(" | Thành tiền: ").append(formatCurrency(od.getThanhTien()))
                            .append(" | Ngày: ").append(formatDateTime(od.getNgayDat())).append("\n");
                }
                context.append("\n");
            }
        }

        // 2. KHUYẾN MÃI & MÃ GIẢM GIÁ
        if (matchesAny(lower, "khuyến mãi", "khuyến mại", "giảm giá", "voucher", "mã giảm", "ưu đãi", "sale", "quà tặng", "coupon", "code")) {
            try {
                List<KhuyenMai> activeKm = khuyenMaiRepository.findActiveKhaDung();
                if (activeKm.isEmpty()) {
                    activeKm = khuyenMaiRepository.findAll().stream()
                            .filter(k -> "active".equalsIgnoreCase(k.getTrangThai()))
                            .toList();
                }

                context.append("=== CHƯƠNG TRÌNH KHUYẾN MÃI & VOUCHER ĐANG ÁP DỤNG ===\n");
                if (!activeKm.isEmpty()) {
                    for (KhuyenMai k : activeKm) {
                        String giam = "percent".equalsIgnoreCase(k.getLoai())
                                ? (k.getGiaTri() + "% (tối đa " + formatCurrency(k.getGiaTriToiDa()) + ")")
                                : formatCurrency(k.getGiaTri());
                        context.append("• Mã: **").append(k.getMaKhuyenMai()).append("** - ").append(k.getTenKhuyenMai())
                                .append(" (Giảm: ").append(giam)
                                .append(", Đơn tối thiểu: ").append(formatCurrency(k.getDonHangToiThieu()))
                                .append(")\n");
                    }
                } else {
                    context.append("Hiện tại các chương trình khuyến mãi tự động được áp dụng trực tiếp vào giá bán sản phẩm trên website.\n");
                }
                context.append("\n");
            } catch (Exception ignored) {}
        }

        // 3. THÔNG TIN CỬA HÀNG, ĐỊA CHỈ, HOTLINE, GIỜ LÀM VIỆC
        if (matchesAny(lower, "cửa hàng", "shop", "địa chỉ", "ở đâu", "hotline", "số điện thoại", "sđt", "liên hệ", "email", "giờ mở cửa", "giờ làm việc", "mấy giờ", "chi nhánh")) {
            try {
                var caiDatList = caiDatHeThongRepository.findAll();
                String ten = "SAOClub";
                String diaChi = "Tòa nhà FPT Polytechnic, Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội";
                String sdt = "0988 888 888 / 1900 6868";
                String email = "hotro@saoclub.vn";

                if (!caiDatList.isEmpty()) {
                    CaiDatHeThong c = caiDatList.get(0);
                    if (c.getTenCuaHang() != null && !c.getTenCuaHang().isBlank()) ten = c.getTenCuaHang();
                    if (c.getDiaChi() != null && !c.getDiaChi().isBlank()) diaChi = c.getDiaChi();
                    if (c.getSoDienThoai() != null && !c.getSoDienThoai().isBlank()) sdt = c.getSoDienThoai();
                    if (c.getEmail() != null && !c.getEmail().isBlank()) email = c.getEmail();
                }

                context.append("=== THÔNG TIN LIÊN HỆ CỬA HÀNG ").append(ten.toUpperCase()).append(" ===\n");
                context.append("• Hệ thống: ").append(ten).append(" - Chuyên Laptop & Máy tính chính hãng\n");
                context.append("• Địa chỉ showroom: ").append(diaChi).append("\n");
                context.append("• Hotline hỗ trợ 24/7: ").append(sdt).append("\n");
                context.append("• Email tiếp nhận CSKH: ").append(email).append("\n");
                context.append("• Giờ mở cửa: 08:00 - 21:30 (Mở cửa tất cả các ngày trong tuần, kể cả Thứ Bảy, Chủ Nhật và ngày Lễ)\n\n");
            } catch (Exception ignored) {}
        }

        // 4. DANH MỤC & THƯƠNG HIỆU
        if (matchesAny(lower, "hãng", "thương hiệu", "danh mục", "bán những gì", "các dòng máy")) {
            try {
                var brands = thuongHieuRepository.hienThiThuongHieu();
                var categories = danhMucRepository.findActiveForPos();
                context.append("=== DANH MỤC & THƯƠNG HIỆU KINH DOANH TẠI SAOCLUB ===\n");
                context.append("• Các thương hiệu chính hãng: ");
                context.append(brands.stream().map(ThuongHieuResponse::getTenThuongHieu).collect(Collectors.joining(", "))).append("\n");
                context.append("• Các dòng sản phẩm: ");
                context.append(categories.stream().map(DanhMucResponse::getTenDanhMuc).collect(Collectors.joining(", "))).append("\n\n");
            } catch (Exception ignored) {}
        }

        // 5. TRA CỨU & TƯ VẤN SẢN PHẨM (GIÁ, CẤU HÌNH, TỒN KHO)
        if (matchesAny(lower, "laptop", "máy tính", "giá", "mua", "tư vấn", "gaming", "văn phòng", "đồ họa", "cấu hình", "còn hàng", "hết hàng", "asus", "acer", "dell", "hp", "lenovo", "msi", "apple", "macbook", "nitro", "legion", "vivobook", "loq", "tuf", "zenbook", "thinkpad", "ideapad", "victus", "pavilion", "envy", "vostro", "inspiron", "bán chạy", "triệu")) {
            try {
                String searchKw = extractSearchKeyword(lower);
                var searchResults = sanPhamRepository.hienThiSanPham(searchKw, null, null, "active", PageRequest.of(0, 5));

                if (searchResults.hasContent()) {
                    context.append("=== SẢN PHẨM PHÙ HỢP TẠI CỬA HÀNG ===\n");
                    for (SanPhamResponse sp : searchResults.getContent()) {
                        long ton = sp.getSoLuongTon() != null ? sp.getSoLuongTon() : 0;
                        String tinhTrangKho = ton > 0 ? "Còn hàng (" + ton + " chiếc sẵn sàng giao)" : "Tạm hết hàng (Có thể liên hệ đặt trước)";
                        context.append("• **").append(sp.getTenSanPham()).append("** [Hãng: ").append(sp.getTenThuongHieu()).append(" | SKU: ").append(sp.getMaSku()).append("]\n")
                                .append("  - Cấu hình: ").append(sp.getCpu() != null ? sp.getCpu() : "").append(" | ")
                                .append(sp.getRam() != null ? sp.getRam() : "").append(" | ")
                                .append(sp.getOCung() != null ? sp.getOCung() : "").append(" | ")
                                .append(sp.getGpu() != null ? sp.getGpu() : "").append("\n")
                                .append("  - Màn hình: ").append(sp.getKichThuocManHinh() != null ? sp.getKichThuocManHinh() : "Tiêu chuẩn")
                                .append(" | Màu sắc: ").append(sp.getMauSac() != null ? sp.getMauSac() : "Tiêu chuẩn").append("\n")
                                .append("  - Giá bán niêm yết: ").append(formatCurrency(sp.getGiaBan())).append("\n")
                                .append("  - Tình trạng: ").append(tinhTrangKho).append("\n")
                                .append("  - Bảo hành: ").append(sp.getBaoHanhThang() != null ? sp.getBaoHanhThang() + " tháng chính hãng" : "12 tháng chính hãng").append("\n\n");
                    }
                } else {
                    // Nếu tìm từ khóa cụ thể không có, gợi ý top 5 bán chạy
                    LocalDateTime from = LocalDate.now().minusMonths(3).atStartOfDay();
                    LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);
                    List<ProductSalesResponse> topList = sanPhamRepository.topSelling(from, to, PageRequest.of(0, 5));
                    if (!topList.isEmpty()) {
                        context.append("=== CÁC DÒNG LAPTOP NỔI BẬT & BÁN CHẠY NHẤT TẠI CỬA HÀNG ===\n");
                        for (int i = 0; i < topList.size(); i++) {
                            context.append(i + 1).append(". ").append(topList.get(i).getTenSanPham())
                                    .append(" (Đã bán: ").append(topList.get(i).getSoLuongDaBan()).append(" chiếc)\n");
                        }
                        context.append("\n");
                    }
                }
            } catch (Exception ignored) {}
        }

        // 6. KIẾN THỨC CỬA HÀNG & CHÍNH SÁCH (BẢO HÀNH, ĐỔI TRẢ, TRẢ GÓP, VẬN CHUYỂN, TÍCH ĐIỂM)
        try {
            List<AiKienThuc> matchedKienThuc = aiKienThucRepository.searchByKeyword(cauHoi);
            if (matchedKienThuc.isEmpty() && matchesAny(lower, "chính sách", "bảo hành", "đổi trả", "thanh toán", "vận chuyển", "giao hàng", "faq", "quy định", "tích điểm", "trả góp", "hướng dẫn")) {
                matchedKienThuc = aiKienThucRepository.findGeneralKnowledge();
            }
            if (!matchedKienThuc.isEmpty()) {
                context.append("=== CHÍNH SÁCH & HƯỚNG DẪN CỦA SAOCLUB ===\n");
                for (AiKienThuc kt : matchedKienThuc.stream().limit(3).toList()) {
                    context.append("• ").append(kt.getTieuDe()).append(":\n  ").append(kt.getNoiDung().trim()).append("\n");
                }
                context.append("\n");
            }
        } catch (Exception ignored) {}

        // 7. KIẾN THỨC MẶC ĐỊNH NẾU CONTEXT VẪN TRỐNG
        if (context.length() == 0) {
            List<AiKienThuc> faq = aiKienThucRepository.findGeneralKnowledge();
            if (!faq.isEmpty()) {
                context.append("=== THÔNG TIN THƯỜNG GẶP TẠI SAOCLUB ===\n");
                for (AiKienThuc k : faq.stream().limit(3).toList()) {
                    context.append("• ").append(k.getTieuDe()).append(":\n  ").append(k.getNoiDung().trim()).append("\n");
                }
            }
        }

        return context.toString();
    }

    // Trích xuất từ khóa tìm kiếm sản phẩm từ câu hỏi khách
    private String extractSearchKeyword(String lower) {
        for (String brand : List.of("acer", "asus", "dell", "hp", "lenovo", "msi", "apple", "macbook")) {
            if (lower.contains(brand)) return brand;
        }
        for (String modelName : List.of("nitro", "legion", "vivobook", "xps", "aspire", "loq", "tuf", "zenbook", "stealth", "thinkpad", "ideapad", "victus", "pavilion", "envy", "vostro", "inspiron", "predator", "alienware", "rog")) {
            if (lower.contains(modelName)) return modelName;
        }
        if (lower.contains("gaming")) return "gaming";
        if (lower.contains("văn phòng")) return "văn phòng";
        if (lower.contains("đồ họa")) return "đồ họa";
        if (lower.contains("mỏng nhẹ")) return "mỏng nhẹ";

        Pattern p = Pattern.compile("(?:tìm|giá|về|xem|mua|thông tin|tư vấn)\\s+(?:laptop|máy tính|sản phẩm)?\\s*([a-zA-Z0-9\\s]{2,20})");
        Matcher m = p.matcher(lower);
        if (m.find()) {
            String kw = m.group(1).trim();
            if (!kw.isBlank() && !matchesAny(kw, "nào", "gì", "bao nhiêu", "đâu", "hôm nay", "tháng này", "bán chạy", "sắp hết", "tồn kho")) {
                return kw;
            }
        }
        return null;
    }

    // Tạo prompt gửi cho mô hình AI
    private String buildPrompt(String cauHoi, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là Trợ lý AI Bán hàng & Chăm sóc Khách hàng của hệ thống cửa hàng máy tính SAOClub.\n");
        prompt.append("QUY TẮC BẮT BUỘC:\n");
        prompt.append("1. TRẢ LỜI NHIỆT TÌNH, THÂN THIỆN, LỊCH SỰ, CHÍNH XÁC 100% DỰA TRÊN DỮ LIỆU CỬA HÀNG ĐƯỢC CUNG CẤP DƯỚI ĐÂY.\n");
        prompt.append("2. Nếu khách hỏi về sản phẩm/giá cả: Nêu đúng tên model, hãng, cấu hình (CPU, RAM, Ổ cứng, GPU), giá niêm yết chuẩn VNĐ và tình trạng còn hàng. Tuyệt đối không bịa đặt sản phẩm hoặc mức giá không có trong dữ liệu.\n");
        prompt.append("3. Nếu khách hỏi về đơn hàng: Đọc chính xác trạng thái đơn hàng, người nhận, tổng tiền từ dữ liệu.\n");
        prompt.append("4. Nếu khách hỏi về khuyến mãi/voucher: Nêu rõ mã voucher và mức giảm.\n");
        prompt.append("5. Nếu khách hỏi về địa chỉ, hotline, giờ mở cửa, chính sách: Nêu thông tin chính xác của SAOClub.\n");
        prompt.append("6. Trình bày bằng Markdown đẹp mắt (in đậm **, gạch đầu dòng *, danh sách) để khách dễ xem.\n");
        prompt.append("7. Nếu không có dữ liệu phù hợp, hãy xin lỗi một cách lịch sự và gợi ý khách liên hệ nhân viên bằng cách gõ 'chuyển nhân viên' hoặc gọi hotline.\n\n");

        if (context != null && !context.isBlank()) {
            prompt.append("--- DỮ LIỆU THỰC TẾ TỪ HỆ THỐNG CỬA HÀNG SAOCLUB ---\n");
            prompt.append(context);
            prompt.append("--------------------------------------------------\n\n");
        }

        prompt.append("Câu hỏi của khách hàng: ").append(cauHoi).append("\n");
        prompt.append("Câu trả lời hỗ trợ khách hàng:");

        return prompt.toString();
    }

    // Gửi yêu cầu sinh phản hồi đến Ollama API
    private String goiOllama(String prompt) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "Bạn là Trợ lý AI Bán hàng & CSKH của SAOClub. Luôn trả lời bằng tiếng Việt, thân thiện, chính xác theo dữ liệu cửa hàng.");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(systemMsg, message));
        requestBody.put("stream", false);

        Map<String, Object> response = webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("message")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = (Map<String, Object>) response.get("message");
            return formatResponse((String) msg.get("content"));
        }

        return fallbackResponse();
    }

    // Chuẩn hóa định dạng phản hồi
    private String formatResponse(String raw) {
        if (raw == null) return fallbackResponse();

        raw = raw.trim();
        if (raw.length() > 2500) {
            raw = raw.substring(0, 2500) + "...";
        }
        return raw;
    }

    // Phản hồi mặc định khi dịch vụ AI không khả dụng
    private String fallbackResponse() {
        return "Xin lỗi bạn, hiện tại hệ thống AI đang nâng cấp dữ liệu một chút. 😅\n\n" +
                "Bạn có thể:\n" +
                "📞 Gọi Hotline SAOClub: **0988 888 888** hoặc **1900 6868** (8h00 - 21h30)\n" +
                "💬 Gõ **\"chuyển nhân viên\"** để được nhân viên tư vấn hỗ trợ trực tiếp ngay\n" +
                "📧 Email: hotro@saoclub.vn\n" +
                "🏢 Địa chỉ: Tòa nhà FPT Polytechnic, Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội";
    }

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

    // Kiểm tra trạng thái kết nối Ollama
    public boolean isOllamaAvailable() {
        if (!enabled) return false;
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Lấy danh sách các mô hình AI khả dụng
    public List<String> getAvailableModels() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("models")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
                return models.stream()
                        .map(m -> (String) m.get("name"))
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }
}
