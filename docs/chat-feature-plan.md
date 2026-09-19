# Kế hoạch nâng cấp tính năng Chat — SAOClub

## 1. Mục tiêu & Scope

- Real-time chat giữa **Admin/NV bán hàng ↔ Khách hàng** (có tài khoản)
- Real-time chat giữa **Admin/NV bán hàng ↔ Khách lẻ** (không có tài khoản, dùng session ID)
- Admin panel: **2 tab** — "Hệ thống" (khách có tài khoản) và "Khách lẻ" (anonymous)
- Browser notification khi có tin nhắn mới
- Lưu lịch sử chat để restore khi khách lẻ đăng ký

## 2. Kiến trúc

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Vue 3)                            │
│  ChatWidget.vue (global)  ── WebSocket ──  AdminChatPanel.vue       │
│  (KH + KH lẻ)                                   (2 tab)            │
└──────────────────────┬──────────────────────────┬──────────────────┘
                       │ STOMP over WebSocket       │
                       ▼                          ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot 3.4)                        │
│  WebSocketConfig (STOMP)                                            │
│  CuocTroChuyenController  ──  ChatService                           │
│  TinNhanController          ──  ChatRepository (JPA)                │
└──────────────────────┬──────────────────────────┬──────────────────┘
                       │                          │
                       ▼                          ▼
              ┌─────────────────┐      ┌─────────────────┐
              │   MySQL         │      │   Notification   │
              │ cuoc_tro_chuyen │      │   Service       │
              │     tin_nhan    │      │  (Browser Push) │
              └─────────────────┘      └─────────────────┘
```

## 3. Database Schema

```sql
-- Bảng 1: Cuộc trò chuyện
CREATE TABLE cuoc_tro_chuyen (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    loai_khach        NVARCHAR(20) NOT NULL,        -- 'HE_THONG' | 'ANONYMOUS'
    khach_hang_id     INT NULL,
    session_id        NVARCHAR(100) NULL,             -- cho khách lẻ
    ho_ten_khach      NVARCHAR(150) NULL,            -- lưu tên khi khách lẻ
    so_dien_thoai     NVARCHAR(20) NULL,             -- SĐT khách lẻ
    trang_thai        NVARCHAR(20) DEFAULT 'CHO_XU_LY', -- CHO_XU_LY | DANG_XU_LY | DA_DONG
    nhan_vien_phu_trach INT NULL,
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ctc_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
    CONSTRAINT fk_ctc_nv FOREIGN KEY (nhan_vien_phu_trach) REFERENCES nhan_vien(nhan_vien_id)
);

-- Bảng 2: Tin nhắn
CREATE TABLE tin_nhan (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    cuoc_tro_chuyen_id    BIGINT NOT NULL,
    nguoi_gui             NVARCHAR(20) NOT NULL,    -- 'KHACH' | 'NHAN_VIEN' | 'HE_THONG'
    nhan_vien_id          INT NULL,                  -- NULL nếu người gửi là khách
    noi_dung              NVARCHAR(MAX) NOT NULL,
    da_doc                 BIT DEFAULT 0,
    created_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tn_ctc FOREIGN KEY (cuoc_tro_chuyen_id) REFERENCES cuoc_tro_chuyen(id),
    CONSTRAINT fk_tn_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
);

-- Index cho performance
CREATE INDEX idx_ctc_loai ON cuoc_tro_chuyen(loai_khach);
CREATE INDEX idx_ctc_kh ON cuoc_tro_chuyen(khach_hang_id);
CREATE INDEX idx_tn_ctc ON tin_nhan(cuoc_tro_chuyen_id);
CREATE INDEX idx_tn_chua_doc ON tin_nhan(cuoc_tro_chuyen_id, da_doc) WHERE da_doc = 0;
```

## 4. Backend

### 4.1 Thêm dependency (pom.xml)

```xml
<!-- WebSocket + STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 4.2 Cấu hình WebSocket

```
Endpoint: /ws/chat
STOMP destinations:
  - /topic/conversation.{id}       — tin nhắn mới trong cuộc trò chuyện
  - /topic/conversations.{type}    — danh sách cập nhật (hệ thống / anonymous)
  - /user/queue/notifications      — notification riêng cho từng user
```

### 4.3 API Endpoints

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| POST | `/api/chat/phien` | Public | Tạo/kiểm tra phiên chat (anonymous) |
| GET | `/api/chat/phien/{sessionId}` | Public | Lấy cuộc trò chuyện theo sessionId |
| GET | `/api/chat/{id}/tin-nhan` | JWT | Lấy tin nhắn (phân trang) |
| POST | `/api/chat/{id}/tin-nhan` | JWT | Gửi tin nhắn |
| GET | `/api/chat/danh-sach` | Staff | DS cuộc trò chuyện (theo loại) |
| PUT | `/api/chat/{id}/trang-thai` | Staff | Cập nhật trạng thái |
| PUT | `/api/chat/{id}/nhan-vien` | Staff | Nhận / chuyển cuộc trò chuyện |
| GET | `/api/chat/khach/{khId}` | Staff | Lấy lịch sử chat của 1 KH |
| POST | `/api/chat/link-tai-khoan` | JWT | Link session anonymous → tài khoản mới đăng ký |

### 4.4 Backend Files (thứ tự tạo)

```
entity/CuocTroChuyen.java
entity/TinNhan.java
repository/CuocTroChuyenRepository.java
repository/TinNhanRepository.java
service/ChatService.java
service/NotificationService.java
config/WebSocketConfig.java
config/WebSecurityConfig.java   (cập nhật — cho phép /ws endpoint)
controller/CuocTroChuyenController.java
controller/TinNhanController.java
websocket/ChatWebSocketHandler.java
```

## 5. Frontend

### 5.1 Files mới

| File | Mô tả |
|------|--------|
| `services/ChatService.js` | Gọi API + WebSocket STOMP client |
| `stores/chat.js` | Pinia store — danh sách cuộc trò chuyện, tin nhắn |
| `components/account/ChatWidget.vue` | **Cải tiến** — kết nối real-time, hỗ trợ anonymous |
| `components/admin/ChatManagementPanel.vue` | Panel chat cho Admin/NV |
| `components/common/ChatConversation.vue` | Component hiển thị tin nhắn (reusable) |
| `components/common/ChatComposer.vue` | Component nhập tin nhắn (reusable) |

### 5.2 Cải tiến ChatWidget.vue

```javascript
// Logic chính:
1. Kiểm tra auth.user:
   - Có tài khoản → gửi tin nhắn với khachHangId
   - Không có → tạo/lấy sessionId từ localStorage
2. WebSocket STOMP:
   - Kết nối khi widget mở
   - Subscribe /topic/conversation.{id}
   - Subscribe /user/queue/notifications
3. Browser Notification:
   - Xin quyền khi widget mount
   - Hiện notification khi có tin nhắn mới từ NV (không phải mình gửi)
4. Polling fallback: setInterval 5s khi WebSocket disconnect
```

### 5.3 AdminChatPanel layout

```
┌─────────────────────────────────────────────────────────────┐
│  Chat quản lý khách hàng                                   │
├────────────────┬────────────────────────────────────────────┤
│ Tabs:          │                                            │
│  ○ Hệ thống   │  Danh sách tin nhắn                        │
│  ○ Khách lẻ    │  ──────────────────────────────────────── │
│                │  KH: Nguyễn Văn A                          │
│ ─────────────  │  Tin nhắn 1: Xin chào                      │
│ Danh sách KH   │  Tin nhắn 2: Tôi muốn hỏi về laptop...    │
│ ─────────────  │                                            │
│ ○ A (mới 2)   │  ────────────────────────────────────────  │
│ ○ B           │  [Nhập tin nhắn...        ] [Gửi]          │
│ ○ C           │                                            │
│                │  [Nhận tiếp]  [Chuyển NV khác] [Đóng]     │
└────────────────┴────────────────────────────────────────────┘
```

## 6. Thứ tự implementation

### Phase 1: Database + Backend core (1-2 ngày)
- [ ] Thêm bảng `cuoc_tro_chuyen`, `tin_nhan` vào SQL (idempotent)
- [ ] Thêm dependency WebSocket vào pom.xml
- [ ] Tạo entity, repository, service
- [ ] Cấu hình WebSocket (STOMP) + update Security config
- [ ] Implement REST API endpoints
- [ ] WebSocket handler cho real-time

### Phase 2: Frontend — ChatWidget nâng cấp (1-2 ngày)
- [ ] Tạo ChatService.js (API + STOMP client)
- [ ] Tạo Pinia store cho chat
- [ ] Cải tiến ChatWidget.vue (anonymous + real-time)
- [ ] Xử lý browser notification
- [ ] Fallback polling khi WebSocket disconnect

### Phase 3: Frontend — Admin Panel (1-2 ngày)
- [ ] Tạo ChatManagementPanel.vue
- [ ] Tab "Hệ thống" — danh sách KH có tài khoản
- [ ] Tab "Khách lẻ" — danh sách anonymous
- [ ] Chat window với KH
- [ ] Action: Nhận tiếp, chuyển NV, đóng cuộc trò chuyện
- [ ] Badge đếm tin nhắn chưa đọc trên tab

### Phase 4: Integration & Polish (0.5-1 ngày)
- [ ] Link anonymous session → tài khoản khi khách đăng ký
- [ ] Toast notification khi có tin nhắn mới
- [ ] Kiểm thử multi-user (4 tab như workflow hiện tại)
- [ ] Fix edge cases

## 7. Đánh đổi & Constraints

| Quyết định | Lý do |
|------------|-------|
| STOMP over WebSocket | Spring hỗ trợ native, đơn giản hơn raw WS, có subscription routing |
| Session ID cho khách lẻ | Không cần đăng ký, nhanh, đủ cho use case |
| link-tai-khoan sau đăng ký | Khách lẻ → KH có thể thấy lại lịch sử chat |
| Polling fallback 5s | WebSocket có thể drop trên mạng yếu |

## 8. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| WebSocket reconnect | Exponential backoff + polling fallback |
| Duplicate tin nhắn | Deduplicate ở frontend bằng message ID |
| Anonymous impersonation | Validate sessionId server-side, không trust client |
| Browser notification denied | Graceful fallback — chỉ hiện toast |
