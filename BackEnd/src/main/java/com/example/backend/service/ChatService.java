package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.request.ChatMessageRequest;
import com.example.backend.response.ChatCuocTroChuyenResponse;
import com.example.backend.response.ChatTinNhanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private CuocTroChuyenRepository cuocTroChuyenRepository;

    @Autowired
    private TinNhanRepository tinNhanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AiChatService aiChatService;

    // Keywords trigger escalate (khách yêu cầu chat với nhân viên)
    private static final List<String> ESCALATE_KEYWORDS = Arrays.asList(
            "nhân viên", "tư vấn", "người thật", "chuyển", "kết nối nhân viên",
            "cần người", "agent", "staff", "đặt hàng", "mua hàng", "thanh toán",
            "đặt", "order", "chủ shop", "admin", "quản lý", "giám đốc",
            "nói chuyện với", "chat với người", "tôi muốn mua", "giao hàng"
    );

    // Keywords trigger back to AI
    private static final List<String> BACK_TO_AI_KEYWORDS = Arrays.asList(
            "quay lại bot", "chat với bot", "bot", "tự trả lời", "không cần nhân viên",
            "hỏi bot", "tự động", "auto"
    );

    // ─── Tạo phiên chat mới (public — không cần auth) ───────────────────────
    @Transactional
    public ChatCuocTroChuyenResponse taoPhienChat(String sessionId, Integer khachHangId, String hoTen) {
        CuocTroChuyen ctc;

        if (khachHangId != null) {
            // Khách có tài khoản
            Optional<KhachHang> kh = khachHangRepository.findById(khachHangId);
            if (kh.isEmpty()) throw new IllegalArgumentException("Khách hàng không tồn tại");

            ctc = new CuocTroChuyen();
            ctc.setLoaiKhach(CuocTroChuyen.LOAI_HE_THONG);
            ctc.setKhachHang(kh.get());
            ctc.setHoTenKhach(kh.get().getHoTen());
            ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
        } else {
            // Khách lẻ — kiểm tra session đã tồn tại chưa
            if (sessionId != null && !sessionId.isBlank()) {
                Optional<CuocTroChuyen> existing = cuocTroChuyenRepository.findBySessionId(sessionId);
                if (existing.isPresent()) {
                    return toResponse(existing.get());
                }
            }

            ctc = new CuocTroChuyen();
            ctc.setLoaiKhach(CuocTroChuyen.LOAI_ANONYMOUS);
            ctc.setSessionId(sessionId != null ? sessionId : UUID.randomUUID().toString());
            ctc.setHoTenKhach(hoTen != null ? hoTen : "Ẩn danh");
            ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
        }

        ctc = cuocTroChuyenRepository.save(ctc);

        // Gửi tin nhắn chào từ AI
        String loiChao = aiChatService.getWelcomeMessage();
        luuTinNhanBot(ctc.getId(), loiChao);

        return toResponse(ctc);
    }

    // ─── Lấy cuộc trò chuyện theo session ID ──────────────────────────────
    public ChatCuocTroChuyenResponse layPhienChat(String sessionId) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Phiên chat không tồn tại"));
        return toResponse(ctc);
    }

    // ─── Lấy danh sách cuộc trò chuyện (cho staff) ────────────────────────
    public Page<ChatCuocTroChuyenResponse> layDanhSachChat(String loaiKhach, String trangThai, Pageable pageable) {
        Page<CuocTroChuyen> page;
        if (trangThai != null && !trangThai.isBlank()) {
            page = cuocTroChuyenRepository.findByLoaiKhachAndTrangThaiOrderByUpdatedAtDesc(loaiKhach, trangThai, pageable);
        } else {
            page = cuocTroChuyenRepository.findByLoaiKhachWithPriority(loaiKhach, pageable);
        }
        return page.map(this::toResponse);
    }

    // ─── Lấy tin nhắn ───────────────────────────────────────────────────────
    public Page<ChatTinNhanResponse> layTinNhan(Long cuocTroChuyenId, Pageable pageable) {
        return tinNhanRepository.findByCuocTroChuyenIdOrderByCreatedAtAsc(cuocTroChuyenId, pageable)
                .map(this::toTinNhanResponse);
    }

    // ─── Gửi tin nhắn ────────────────────────────────────────────────────────
    @Transactional
    public ChatTinNhanResponse guiTinNhan(Long cuocTroChuyenId, ChatMessageRequest request) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findById(cuocTroChuyenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        String noiDung = request.getNoiDung().trim();
        if (noiDung.isEmpty()) throw new IllegalArgumentException("Nội dung không được trống");

        // Lưu tin nhắn khách
        TinNhan tinNhan = new TinNhan();
        tinNhan.setCuocTroChuyen(ctc);
        tinNhan.setNguoiGui(TinNhan.NGUOI_GUI_KHACH);
        tinNhan.setLoaiNguoiGui(
                CuocTroChuyen.LOAI_ANONYMOUS.equals(ctc.getLoaiKhach())
                        ? TinNhan.LOAI_ANONYMOUS
                        : TinNhan.LOAI_KHACH_HANG
        );
        tinNhan.setNoiDung(noiDung);
        tinNhan.setDaDoc(false);
        tinNhan = tinNhanRepository.save(tinNhan);

        // Broadcast cho khách thấy
        broadcastTinNhan(ctc.getId(), toTinNhanResponse(tinNhan));

        // Xử lý tiếp theo trạng thái
        if (CuocTroChuyen.TRANG_THAI_HOI_DAP_AI.equals(ctc.getTrangThai())) {
            // Đang ở chế độ AI — kiểm tra escalation
            if (containsEscalateKeyword(noiDung)) {
                return xuLyEscalate(ctc, tinNhan);
            }
            // AI trả lời
            return xuLyTraLoiAI(ctc, tinNhan);
        } else {
            // Đang ở chế độ chat NV — chỉ lưu, NV sẽ reply
            // Notify staff có tin nhắn mới
            notifyStaffNewMessage(ctc);
            return toTinNhanResponse(tinNhan);
        }
    }

    // ─── Gửi tin nhắn từ NV/Admin ────────────────────────────────────────────
    @Transactional
    public ChatTinNhanResponse guiTinNhanTuNhanVien(Long cuocTroChuyenId, ChatMessageRequest request) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findById(cuocTroChuyenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        TaiKhoan tk = currentAccount();
        if (tk == null || tk.getNhanVien() == null) {
            throw new AccessDeniedException("Chỉ nhân viên mới gửi được tin nhắn");
        }

        NhanVien nv = tk.getNhanVien();
        String nguoiGui = "admin".equals(tk.getChucVu().getMaChucVu())
                ? TinNhan.NGUOI_GUI_ADMIN
                : TinNhan.NGUOI_GUI_NHAN_VIEN;

        TinNhan tinNhan = new TinNhan();
        tinNhan.setCuocTroChuyen(ctc);
        tinNhan.setNguoiGui(nguoiGui);
        tinNhan.setLoaiNguoiGui(
                "admin".equals(tk.getChucVu().getMaChucVu())
                        ? TinNhan.LOAI_ADMIN
                        : TinNhan.LOAI_NHAN_VIEN
        );
        tinNhan.setNhanVien(nv);
        tinNhan.setNoiDung(request.getNoiDung().trim());
        tinNhan.setDaDoc(true); // NV gửi = đã đọc
        tinNhan = tinNhanRepository.save(tinNhan);

        // Đánh dấu tất cả tin nhắn khách là đã đọc
        tinNhanRepository.markAllAsReadByCuocTroChuyenId(cuocTroChuyenId);

        // Broadcast
        broadcastTinNhan(ctc.getId(), toTinNhanResponse(tinNhan));
        notifyCustomer(ctc);

        return toTinNhanResponse(tinNhan);
    }

    // ─── Khách yêu cầu chat với NV ────────────────────────────────────────
    @Transactional
    public ChatTinNhanResponse xuLyEscalate(CuocTroChuyen ctc, TinNhan tinNhanKhach) {
        // Chuyển trạng thái
        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_CHAT_NV);
        ctc.setSoLanEscalate(ctc.getSoLanEscalate() + 1);
        cuocTroChuyenRepository.save(ctc);

        // Tin nhắn từ bot thông báo
        String noiDung = "Tôi đã kết nối bạn với nhân viên. Vui lòng đợi trong giây lát. Nhân viên sẽ phản hồi sớm nhất có thể!";
        TinNhan botMsg = luuTinNhanBot(ctc.getId(), noiDung);

        // Notify staff
        notifyStaffEscalate(ctc);

        // Broadcast cập nhật trạng thái
        broadcastTrangThaiChange(ctc);

        return toTinNhanResponse(botMsg);
    }

    // ─── Quay lại AI (từ NV/Admin) ──────────────────────────────────────────
    @Transactional
    public ChatCuocTroChuyenResponse quayLaiAI(Long cuocTroChuyenId) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findById(cuocTroChuyenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
        ctc.setNhanVienPhuTrach(null);
        ctc = cuocTroChuyenRepository.save(ctc);

        // Tin nhắn thông báo
        String noiDung = "Cuộc trò chuyện đã được chuyển về chế độ tự động. Bạn có thể tiếp tục hỏi Bot hoặc gõ \"chuyển nhân viên\" để được hỗ trợ bởi nhân viên.";
        TinNhan botMsg = luuTinNhanBot(ctc.getId(), noiDung);

        broadcastTinNhan(ctc.getId(), toTinNhanResponse(botMsg));
        broadcastTrangThaiChange(ctc);

        return toResponse(ctc);
    }

    // ─── Nhân viên nhận tiếp cuộc trò chuyện ─────────────────────────────
    @Transactional
    public ChatCuocTroChuyenResponse nhanTiepChat(Long cuocTroChuyenId) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findById(cuocTroChuyenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        TaiKhoan tk = currentAccount();
        if (tk == null || tk.getNhanVien() == null) {
            throw new AccessDeniedException("Chỉ nhân viên mới nhận tiếp được");
        }

        ctc.setNhanVienPhuTrach(tk.getNhanVien());
        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_CHAT_NV);
        ctc = cuocTroChuyenRepository.save(ctc);

        // Tin nhắn chào từ NV
        String loaiNguoiGui = "admin".equals(tk.getChucVu().getMaChucVu())
                ? TinNhan.LOAI_ADMIN : TinNhan.LOAI_NHAN_VIEN;
        String tenNguoiGui = tk.getNhanVien().getHoTen();

        TinNhan chaoTuNV = new TinNhan();
        chaoTuNV.setCuocTroChuyen(ctc);
        chaoTuNV.setNguoiGui("admin".equals(tk.getChucVu().getMaChucVu())
                ? TinNhan.NGUOI_GUI_ADMIN : TinNhan.NGUOI_GUI_NHAN_VIEN);
        chaoTuNV.setLoaiNguoiGui(loaiNguoiGui);
        chaoTuNV.setNhanVien(tk.getNhanVien());
        chaoTuNV.setNoiDung("Xin chào! Mình là " + tenNguoiGui + ", sẵn sàng hỗ trợ bạn. Bạn cần hỏi gì?");
        chaoTuNV.setDaDoc(true);
        tinNhanRepository.save(chaoTuNV);

        // Đánh dấu đã đọc
        tinNhanRepository.markAllAsReadByCuocTroChuyenId(cuocTroChuyenId);

        broadcastTrangThaiChange(ctc);
        notifyCustomer(ctc);

        return toResponse(ctc);
    }

    // ─── Đóng cuộc trò chuyện ──────────────────────────────────────────────
    @Transactional
    public ChatCuocTroChuyenResponse dongChat(Long cuocTroChuyenId) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findById(cuocTroChuyenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_DA_DONG);
        ctc = cuocTroChuyenRepository.save(ctc);

        String noiDung = "Cảm ơn bạn đã chat với SAOClub. Nếu cần hỗ trợ thêm, bạn có thể quay lại bất cứ lúc nào. Chúc bạn một ngày tốt lành!";
        luuTinNhanBot(ctc.getId(), noiDung);

        broadcastTrangThaiChange(ctc);

        return toResponse(ctc);
    }

    // ─── Lấy lịch sử chat của khách hàng ──────────────────────────────────
    public Page<ChatCuocTroChuyenResponse> layLichSuChatKhach(Integer khachHangId, Pageable pageable) {
        return cuocTroChuyenRepository.findByLoaiKhachOrderByUpdatedAtDesc(
                CuocTroChuyen.LOAI_HE_THONG, pageable
        ).map(this::toResponse);
    }

    // ─── AI trả lời ───────────────────────────────────────────────────────
    private ChatTinNhanResponse xuLyTraLoiAI(CuocTroChuyen ctc, TinNhan tinNhanKhach) {
        String cauTraLoi = aiChatService.traLoi(tinNhanKhach.getNoiDung(), ctc.getId());
        TinNhan botMsg = luuTinNhanBot(ctc.getId(), cauTraLoi);

        // Broadcast
        broadcastTinNhan(ctc.getId(), toTinNhanResponse(botMsg));

        return toTinNhanResponse(botMsg);
    }

    // ─── Lưu tin nhắn từ bot ───────────────────────────────────────────────
    private TinNhan luuTinNhanBot(Long cuocTroChuyenId, String noiDung) {
        CuocTroChuyen ctc = cuocTroChuyenRepository.findById(cuocTroChuyenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại"));

        TinNhan msg = new TinNhan();
        msg.setCuocTroChuyen(ctc);
        msg.setNguoiGui(TinNhan.NGUOI_GUI_AI);
        msg.setLoaiNguoiGui(TinNhan.LOAI_AI);
        msg.setNoiDung(noiDung);
        msg.setDaDoc(false);
        return tinNhanRepository.save(msg);
    }

    // ─── Broadcast helpers ─────────────────────────────────────────────────
    private void broadcastTinNhan(Long cuocTroChuyenId, ChatTinNhanResponse msg) {
        messagingTemplate.convertAndSend("/topic/chat/" + cuocTroChuyenId, msg);
    }

    private void broadcastTrangThaiChange(CuocTroChuyen ctc) {
        Map<String, Object> payload = Map.of(
                "cuocTroChuyenId", ctc.getId(),
                "trangThai", ctc.getTrangThai(),
                "nhanVienPhuTrachId", ctc.getNhanVienPhuTrach() != null ? ctc.getNhanVienPhuTrach().getNhanVienId() : null,
                "nhanVienPhuTrachTen", ctc.getNhanVienPhuTrach() != null ? ctc.getNhanVienPhuTrach().getHoTen() : null
        );
        messagingTemplate.convertAndSend("/topic/chat/" + ctc.getId() + "/status", payload);
        // Notify staff list update
        messagingTemplate.convertAndSend("/topic/staff/conversations", payload);
    }

    private void notifyStaffNewMessage(CuocTroChuyen ctc) {
        Map<String, Object> payload = Map.of(
                "type", "new_message",
                "cuocTroChuyenId", ctc.getId(),
                "hoTenKhach", ctc.getHoTenKhach(),
                "loaiKhach", ctc.getLoaiKhach()
        );
        messagingTemplate.convertAndSend("/topic/staff/notifications", payload);
    }

    private void notifyStaffEscalate(CuocTroChuyen ctc) {
        Map<String, Object> payload = Map.of(
                "type", "escalate",
                "cuocTroChuyenId", ctc.getId(),
                "hoTenKhach", ctc.getHoTenKhach(),
                "loaiKhach", ctc.getLoaiKhach(),
                "soLanEscalate", ctc.getSoLanEscalate()
        );
        messagingTemplate.convertAndSend("/topic/staff/notifications", payload);
    }

    private void notifyCustomer(CuocTroChuyen ctc) {
        Map<String, Object> payload = Map.of(
                "cuocTroChuyenId", ctc.getId(),
                "trangThai", ctc.getTrangThai()
        );
        messagingTemplate.convertAndSend("/topic/chat/" + ctc.getId() + "/status", payload);
    }

    // ─── Keyword detection ─────────────────────────────────────────────────
    private boolean containsEscalateKeyword(String text) {
        String lower = text.toLowerCase();
        for (String kw : ESCALATE_KEYWORDS) {
            if (Pattern.compile("\\b" + Pattern.quote(kw.toLowerCase()) + "\\b").matcher(lower).find()) {
                return true;
            }
        }
        return false;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────
    private TaiKhoan currentAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(username)) return null;
        return taiKhoanRepository.findByUsername(username).orElse(null);
    }

    private boolean isStaff() {
        TaiKhoan tk = currentAccount();
        if (tk == null) return false;
        String role = tk.getChucVu().getMaChucVu();
        return "admin".equals(role) || "nhan_vien".equals(role) || "quan_kho".equals(role);
    }

    private ChatCuocTroChuyenResponse toResponse(CuocTroChuyen ctc) {
        ChatCuocTroChuyenResponse r = new ChatCuocTroChuyenResponse();
        r.setId(ctc.getId());
        r.setLoaiKhach(ctc.getLoaiKhach());
        r.setTrangThai(ctc.getTrangThai());
        r.setSoLanEscalate(ctc.getSoLanEscalate());
        r.setCreatedAt(ctc.getCreatedAt());
        r.setUpdatedAt(ctc.getUpdatedAt());

        if (ctc.getKhachHang() != null) {
            r.setKhachHangId(ctc.getKhachHang().getKhachHangId());
        }
        r.setHoTenKhach(ctc.getHoTenKhach());

        if (ctc.getNhanVienPhuTrach() != null) {
            r.setNhanVienPhuTrachId(ctc.getNhanVienPhuTrach().getNhanVienId());
            r.setNhanVienPhuTrachTen(ctc.getNhanVienPhuTrach().getHoTen());
        }

        // Đếm tin nhắn chưa đọc
        long chuaDoc = tinNhanRepository.countByCuocTroChuyenIdAndDaDocFalseAndNguoiGuiNot(ctc.getId(), "KHACH");
        r.setTinNhanChuaDoc((int) chuaDoc);

        // Tin nhắn cuối
        List<TinNhan> latest = tinNhanRepository.findLatestByCuocTroChuyenId(ctc.getId(), PageRequest.of(0, 1));
        if (!latest.isEmpty()) {
            r.setTinNhanCuoi(latest.get(0).getNoiDung());
        }

        return r;
    }

    private ChatTinNhanResponse toTinNhanResponse(TinNhan tn) {
        ChatTinNhanResponse r = new ChatTinNhanResponse();
        r.setId(tn.getId());
        r.setCuocTroChuyenId(tn.getCuocTroChuyen().getId());
        r.setNguoiGui(tn.getNguoiGui());
        r.setLoaiNguoiGui(tn.getLoaiNguoiGui());
        r.setNoiDung(tn.getNoiDung());
        r.setDaDoc(tn.getDaDoc());
        r.setLaCauHoiCuaAi(tn.getLaCauHoiCuaAi());
        r.setCreatedAt(tn.getCreatedAt());

        // Tên người gửi
        if (TinNhan.NGUOI_GUI_AI.equals(tn.getNguoiGui())) {
            r.setTenNguoiGui("SAOClub Bot");
        } else if (TinNhan.NGUOI_GUI_ADMIN.equals(tn.getNguoiGui()) || TinNhan.NGUOI_GUI_NHAN_VIEN.equals(tn.getNguoiGui())) {
            if (tn.getNhanVien() != null) {
                r.setTenNguoiGui(tn.getNhanVien().getHoTen());
            } else {
                r.setTenNguoiGui("Nhân viên");
            }
        } else {
            r.setTenNguoiGui(tn.getCuocTroChuyen().getHoTenKhach());
        }

        return r;
    }

    // ─── Đếm thông báo cho staff dashboard ─────────────────────────────────
    public Map<String, Long> demThongBao() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("heThongChuaDoc", cuocTroChuyenRepository.countByLoaiKhachAndTrangThaiIn(
                CuocTroChuyen.LOAI_HE_THONG,
                Arrays.asList(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI, CuocTroChuyen.TRANG_THAI_CHAT_NV)
        ));
        counts.put("anonymousChuaDoc", cuocTroChuyenRepository.countByLoaiKhachAndTrangThaiIn(
                CuocTroChuyen.LOAI_ANONYMOUS,
                Arrays.asList(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI, CuocTroChuyen.TRANG_THAI_CHAT_NV)
        ));
        return counts;
    }
}
