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

    // Keywords trigger escalate (chỉ chuyển khi khách thực sự muốn gặp nhân viên người thật)
    private static final List<String> ESCALATE_KEYWORDS = Arrays.asList(
            "chuyển nhân viên", "chuyên nhân viên", "chuyen nhan vien",
            "chuyển nv", "chuyên nv", "chuyen nv",
            "gặp nhân viên", "kết nối nhân viên", "nói chuyện với nhân viên",
            "chat với nhân viên", "chat vs nhân viên", "chat nhan vien",
            "người thật", "chuyển người thật", "gặp người thật",
            "chat với người thật", "gặp tư vấn viên", "chuyển qua nhân viên",
            "chuyển sang nhân viên", "yêu cầu nhân viên", "gặp hỗ trợ viên",
            "chuyển người", "nói chuyện với người", "chat với người", "human agent", "talk to human"
    );

    // Keywords trigger back to AI
    private static final List<String> BACK_TO_AI_KEYWORDS = Arrays.asList(
            "chuyển chat bot", "chuyên chat bot", "chuyen chat bot",
            "chuyển chatbot", "chuyên chatbot", "chuyen chatbot",
            "chuyển bot", "chuyên bot", "chuyen bot",
            "chuyển sang bot", "chuyển qua bot", "chuyển về bot",
            "chuyển sang chat bot", "chuyển qua chat bot", "chuyển về chat bot",
            "quay lại bot", "quay lai bot", "quay lại chat bot", "quay lại ai", "quay lai ai",
            "chat với bot", "chat vs bot", "chat với chat bot", "chat vs chat bot",
            "tự trả lời", "không cần nhân viên"
    );

    // ─── Tạo phiên chat mới (public — không cần auth) ───────────────────────
    @Transactional
    public ChatCuocTroChuyenResponse taoPhienChat(String sessionId, Integer khachHangId, String hoTen) {
        if (khachHangId == null) {
            TaiKhoan tk = currentAccount();
            if (tk != null && tk.getKhachHang() != null) {
                khachHangId = tk.getKhachHang().getKhachHangId();
            }
        }

        if (khachHangId != null) {
            Optional<KhachHang> khOpt = khachHangRepository.findById(khachHangId);
            if (khOpt.isEmpty()) throw new IllegalArgumentException("Khách hàng không tồn tại");
            KhachHang kh = khOpt.get();

            // Nếu có sessionId từ phiên ẩn danh trước khi đăng nhập, kiểm tra để gộp/nâng cấp
            if (sessionId != null && !sessionId.isBlank()) {
                Optional<CuocTroChuyen> bySession = cuocTroChuyenRepository.findBySessionId(sessionId);
                if (bySession.isPresent()) {
                    CuocTroChuyen sCtc = bySession.get();
                    if (sCtc.getKhachHang() == null) {
                        sCtc.setLoaiKhach(CuocTroChuyen.LOAI_HE_THONG);
                        sCtc.setKhachHang(kh);
                        sCtc.setHoTenKhach(kh.getHoTen());
                        sCtc = cuocTroChuyenRepository.save(sCtc);
                        return toResponse(sCtc);
                    } else if (kh.getKhachHangId().equals(sCtc.getKhachHang().getKhachHangId())) {
                        return toResponse(sCtc);
                    }
                }
            }

            // Kiểm tra khách hàng đã có cuộc trò chuyện nào chưa — ưu tiên tái sử dụng cuộc trò chuyện gần nhất
            List<CuocTroChuyen> existingList = cuocTroChuyenRepository.findByKhachHang_KhachHangIdOrderByUpdatedAtDesc(khachHangId);
            if (!existingList.isEmpty()) {
                CuocTroChuyen existing = existingList.stream()
                        .filter(c -> !CuocTroChuyen.TRANG_THAI_DA_DONG.equals(c.getTrangThai()))
                        .findFirst()
                        .orElse(existingList.get(0));
                boolean changed = false;
                if (existing.getSessionId() == null || existing.getSessionId().isBlank()) {
                    existing.setSessionId(UUID.randomUUID().toString());
                    changed = true;
                }
                if (!CuocTroChuyen.LOAI_HE_THONG.equals(existing.getLoaiKhach())) {
                    existing.setLoaiKhach(CuocTroChuyen.LOAI_HE_THONG);
                    changed = true;
                }
                if (existing.getHoTenKhach() == null || "Ẩn danh".equals(existing.getHoTenKhach())) {
                    existing.setHoTenKhach(kh.getHoTen());
                    changed = true;
                }
                if (changed) {
                    existing = cuocTroChuyenRepository.save(existing);
                }
                return toResponse(existing);
            }

            CuocTroChuyen ctc = new CuocTroChuyen();
            ctc.setLoaiKhach(CuocTroChuyen.LOAI_HE_THONG);
            ctc.setKhachHang(kh);
            ctc.setHoTenKhach(kh.getHoTen());
            ctc.setSessionId(sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString());
            ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
            ctc = cuocTroChuyenRepository.save(ctc);

            String loiChao = aiChatService.getWelcomeMessage();
            luuTinNhanBot(ctc.getId(), loiChao);

            return toResponse(ctc);
        }

        // Khách lẻ — kiểm tra session đã tồn tại chưa
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<CuocTroChuyen> existing = cuocTroChuyenRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        CuocTroChuyen ctc = new CuocTroChuyen();
        ctc.setLoaiKhach(CuocTroChuyen.LOAI_ANONYMOUS);
        ctc.setSessionId(sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString());
        ctc.setHoTenKhach(hoTen != null ? hoTen : "Ẩn danh");
        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
        ctc = cuocTroChuyenRepository.save(ctc);

        // Gửi tin nhắn chào từ AI
        String loiChao = aiChatService.getWelcomeMessage();
        luuTinNhanBot(ctc.getId(), loiChao);

        return toResponse(ctc);
    }

    // ─── Lấy cuộc trò chuyện theo session ID ──────────────────────────────
    @Transactional
    public ChatCuocTroChuyenResponse layPhienChat(String sessionId) {
        Optional<CuocTroChuyen> opt = cuocTroChuyenRepository.findBySessionId(sessionId);
        if (opt.isEmpty()) {
            try {
                Long id = Long.parseLong(sessionId);
                opt = cuocTroChuyenRepository.findById(id);
            } catch (NumberFormatException ignored) {
            }
        }
        CuocTroChuyen ctc = opt.orElseThrow(() -> new IllegalArgumentException("Phiên chat không tồn tại"));

        // Nếu khách đang đăng nhập mà phiên chat vẫn là ANONYMOUS, tự động gắn vào khách hàng
        TaiKhoan tk = currentAccount();
        if (tk != null && tk.getKhachHang() != null && ctc.getKhachHang() == null) {
            ctc.setKhachHang(tk.getKhachHang());
            ctc.setLoaiKhach(CuocTroChuyen.LOAI_HE_THONG);
            ctc.setHoTenKhach(tk.getKhachHang().getHoTen());
            ctc = cuocTroChuyenRepository.save(ctc);
        }

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

        // Nếu khách đã đăng nhập nhưng cuộc trò chuyện chưa gắn khách hàng -> gắn luôn
        TaiKhoan tk = currentAccount();
        if (tk != null && tk.getKhachHang() != null && ctc.getKhachHang() == null) {
            ctc.setKhachHang(tk.getKhachHang());
            ctc.setLoaiKhach(CuocTroChuyen.LOAI_HE_THONG);
            ctc.setHoTenKhach(tk.getKhachHang().getHoTen());
            ctc = cuocTroChuyenRepository.save(ctc);
        }

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

        // Cập nhật thời gian cuộc trò chuyện
        ctc.setUpdatedAt(LocalDateTime.now());
        ctc = cuocTroChuyenRepository.save(ctc);

        // Broadcast cho khách và admin thấy tin nhắn khách vừa gửi
        broadcastTinNhan(ctc.getId(), toTinNhanResponse(tinNhan));

        // 1. Kiểm tra lệnh chuyển về Chat Bot (hoạt động ở mọi trạng thái)
        if (containsBackToAiKeyword(noiDung)) {
            return xuLyQuayLaiAITuKhach(ctc);
        }

        // 2. Kiểm tra lệnh chuyển sang Nhân viên (hoạt động ở mọi trạng thái)
        if (containsEscalateKeyword(noiDung)) {
            return xuLyEscalate(ctc, tinNhan);
        }

        // 3. Xử lý tiếp theo trạng thái hiện tại
        if (CuocTroChuyen.TRANG_THAI_HOI_DAP_AI.equals(ctc.getTrangThai())
                || CuocTroChuyen.TRANG_THAI_DA_DONG.equals(ctc.getTrangThai())) {
            if (CuocTroChuyen.TRANG_THAI_DA_DONG.equals(ctc.getTrangThai())) {
                ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
                ctc = cuocTroChuyenRepository.save(ctc);
                broadcastTrangThaiChange(ctc);
            }
            // AI trả lời
            return xuLyTraLoiAI(ctc, tinNhan);
        } else {
            // Đang ở chế độ CHAT_NHAN_VIEN -> thông báo cho nhân viên khi có tin nhắn mới
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

        // Khi nhân viên nhắn tin trực tiếp, đảm bảo cuộc trò chuyện ở chế độ CHAT_NHAN_VIEN
        boolean statusChanged = false;
        if (!CuocTroChuyen.TRANG_THAI_CHAT_NV.equals(ctc.getTrangThai())) {
            ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_CHAT_NV);
            statusChanged = true;
        }
        if (ctc.getNhanVienPhuTrach() == null) {
            ctc.setNhanVienPhuTrach(nv);
            statusChanged = true;
        }
        ctc.setUpdatedAt(LocalDateTime.now());
        ctc = cuocTroChuyenRepository.save(ctc);

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
        ChatTinNhanResponse response = toTinNhanResponse(tinNhan);
        broadcastTinNhan(ctc.getId(), response);
        if (statusChanged) {
            broadcastTrangThaiChange(ctc);
        } else {
            notifyCustomer(ctc);
        }

        return response;
    }

    // ─── Khách yêu cầu chat với NV ────────────────────────────────────────
    @Transactional
    public ChatTinNhanResponse xuLyEscalate(CuocTroChuyen ctc, TinNhan tinNhanKhach) {
        // Chuyển trạng thái
        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_CHAT_NV);
        ctc.setSoLanEscalate((ctc.getSoLanEscalate() != null ? ctc.getSoLanEscalate() : 0) + 1);
        ctc = cuocTroChuyenRepository.save(ctc);

        // Tin nhắn từ bot thông báo
        String noiDung = "Tôi đã kết nối bạn với nhân viên tư vấn 🧑‍💻. Vui lòng đợi trong giây lát, nhân viên sẽ phản hồi sớm nhất có thể! (Gõ \"chuyển chat bot\" nếu bạn muốn quay lại chat với Bot)";
        TinNhan botMsg = luuTinNhanBot(ctc.getId(), noiDung);
        ChatTinNhanResponse botResp = toTinNhanResponse(botMsg);

        // Broadcast tin nhắn thông báo và trạng thái
        broadcastTinNhan(ctc.getId(), botResp);
        notifyStaffEscalate(ctc);
        broadcastTrangThaiChange(ctc);

        return botResp;
    }

    // ─── Khách chủ động chuyển về AI bằng lệnh ────────────────────────────
    @Transactional
    public ChatTinNhanResponse xuLyQuayLaiAITuKhach(CuocTroChuyen ctc) {
        ctc.setTrangThai(CuocTroChuyen.TRANG_THAI_HOI_DAP_AI);
        ctc.setNhanVienPhuTrach(null);
        ctc = cuocTroChuyenRepository.save(ctc);

        String noiDung = "Đã chuyển về chế độ chat với SAOClub Bot 🤖. Mình có thể giúp gì cho bạn? (Gõ \"chuyển nhân viên\" nếu bạn cần gặp nhân viên tư vấn nhé!)";
        TinNhan botMsg = luuTinNhanBot(ctc.getId(), noiDung);
        ChatTinNhanResponse botResp = toTinNhanResponse(botMsg);

        broadcastTinNhan(ctc.getId(), botResp);
        broadcastTrangThaiChange(ctc);

        return botResp;
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
        chaoTuNV = tinNhanRepository.save(chaoTuNV);

        // Đánh dấu đã đọc
        tinNhanRepository.markAllAsReadByCuocTroChuyenId(cuocTroChuyenId);

        broadcastTinNhan(ctc.getId(), toTinNhanResponse(chaoTuNV));
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
        TinNhan botMsg = luuTinNhanBot(ctc.getId(), noiDung);

        broadcastTinNhan(ctc.getId(), toTinNhanResponse(botMsg));
        broadcastTrangThaiChange(ctc);

        return toResponse(ctc);
    }

    // ─── Lấy lịch sử chat của khách hàng ──────────────────────────────────
    public Page<ChatCuocTroChuyenResponse> layLichSuChatKhach(Integer khachHangId, Pageable pageable) {
        return cuocTroChuyenRepository.findByKhachHang_KhachHangIdOrderByUpdatedAtDesc(
                khachHangId, pageable
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
        Map<String, Object> payload = new HashMap<>();
        payload.put("cuocTroChuyenId", ctc.getId());
        payload.put("trangThai", ctc.getTrangThai());
        payload.put("nhanVienPhuTrachId",
                ctc.getNhanVienPhuTrach() != null ? ctc.getNhanVienPhuTrach().getNhanVienId() : null);
        payload.put("nhanVienPhuTrachTen",
                ctc.getNhanVienPhuTrach() != null ? ctc.getNhanVienPhuTrach().getHoTen() : null);
        messagingTemplate.convertAndSend("/topic/chat/" + ctc.getId() + "/status", payload);
        // Notify staff list update
        messagingTemplate.convertAndSend("/topic/staff/conversations", payload);
    }

    private void notifyStaffNewMessage(CuocTroChuyen ctc) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "new_message");
        payload.put("cuocTroChuyenId", ctc.getId());
        payload.put("hoTenKhach", ctc.getHoTenKhach());
        payload.put("loaiKhach", ctc.getLoaiKhach());
        messagingTemplate.convertAndSend("/topic/staff/notifications", payload);
    }

    private void notifyStaffEscalate(CuocTroChuyen ctc) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "escalate");
        payload.put("cuocTroChuyenId", ctc.getId());
        payload.put("hoTenKhach", ctc.getHoTenKhach());
        payload.put("loaiKhach", ctc.getLoaiKhach());
        payload.put("soLanEscalate", ctc.getSoLanEscalate());
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
        if (text == null) return false;
        String lower = text.trim().toLowerCase();
        for (String kw : ESCALATE_KEYWORDS) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBackToAiKeyword(String text) {
        if (text == null) return false;
        String lower = text.trim().toLowerCase();
        if (lower.equals("bot") || lower.equals("chat bot") || lower.equals("chatbot")) {
            return true;
        }
        for (String kw : BACK_TO_AI_KEYWORDS) {
            if (lower.contains(kw)) {
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
        r.setSessionId(ctc.getSessionId());
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
        r.setTrangThai(tn.getCuocTroChuyen().getTrangThai());

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
