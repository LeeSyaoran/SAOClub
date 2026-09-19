# Kế hoạch tính năng Chat + AI Chatbot — SAOClub (v2)

## 1. Tổng quan luồng chat

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         KHÁCH HÀNG                                     │
│   ┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐ │
│   │  Khách lẻ    │     │ Khách có tài khoản│     │  Khách có tài khoản│ │
│   │  (ẩn danh)   │     │                  │     │                  │ │
│   │              │     │                  │     │                  │ │
│   └──────┬───────┘     └────────┬─────────┘     └────────┬─────────┘ │
│          │                      │                          │           │
│          └──────────────────────┼──────────────────────────┘           │
│                                 ▼                                      │
│                    ┌────────────────────────┐                           │
│                    │    AI CHATBOT         │ ←─── RAG: sản phẩm, FAQ  │
│                    │  (Spring AI)          │                           │
│                    │                       │                           │
│                    │  • Trả lời câu hỏi   │                           │
│                    │  • Gợi ý sản phẩm   │                           │
│                    │  • Hướng dẫn mua hàng│                           │
│                    │                       │                           │
│                    │  [Yêu cầu chat NV]  │                           │
│                    └──────────┬───────────┘                           │
│                               │ khách yêu cầu                          │
│                               ▼                                        │
│                    ┌────────────────────────┐                          │
│                    │  NHÂN VIÊN / ADMIN    │                          │
│                    │  (AdminChatPanel)     │                          │
│                    │                       │                           │
│                    │  [Quay lại AI]       │ ← khách yêu cầu           │
│                    └───────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Luồng trạng thái cuộc trò chuyện

```
                    ┌──────────────┐
                    │  CHAO_DONG   │ ← tạo mới
                    └──────┬───────┘
                           │
                           ▼
               ┌───────────────────────────┐
               │       HOI_DAP_AI          │  ← AI trả lời
               │  (trạng thái mặc định)  │
               └───────────┬───────────────┘
                           │ khách gõ "chat với nhân viên" / nút
                           │ hoặc NV nhận tiếp
                           ▼
               ┌───────────────────────────┐
               │    CHAT_NHAN_VIEN         │  ← NV/Admin trả lời
               │    (AI im lặng)          │    AI không tự trả lời
               └───────────┬───────────────┘
                           │ khách gõ "chat với AI" / nút
                           │ hoặc timeout 30 phút không ai trả lời
                           ▼
               ┌───────────────────────────┐
               │      HOI_DAP_AI          │  ← quay lại AI
               └───────────────────────────┘
```

### Badge phân biệt người gửi (trong chat)

| Người gửi | Badge | Màu |
|-----------|-------|------|
| AI Chatbot | 🤖 Bot | Tím |
| Khách hàng (ẩn danh) | 👤 Ẩn danh | Xám |
| Khách hàng (có tài khoản) | 👤 [Tên KH] | Xanh lá |
| Nhân viên | 🧑‍💻 [Tên NV] | Cam |
| Admin | 👑 Admin | Đỏ |

---

## 2. Database Schema

```sql
-- Bảng 1: Cuộc trò chuyện
CREATE TABLE cuoc_tro_chuyen (
    id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
    loai_khach             NVARCHAR(20) NOT NULL,       -- 'HE_THONG' | 'ANONYMOUS'
    khach_hang_id          INT NULL,
    session_id             NVARCHAR(100) NULL,
    ho_ten_khach           NVARCHAR(150) NULL,
    trang_thai             NVARCHAR(30) DEFAULT 'HOI_DAP_AI',  -- HOI_DAP_AI | CHAT_NHAN_VIEN | DA_DONG
    nhan_vien_phu_trach    INT NULL,
    so_lan_escalate        INT DEFAULT 0,             -- đếm số lần khách yêu cầu NV
    created_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ctc_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(khach_hang_id),
    CONSTRAINT fk_ctc_nv FOREIGN KEY (nhan_vien_phu_trach) REFERENCES nhan_vien(nhan_vien_id)
);

-- Bảng 2: Tin nhắn
CREATE TABLE tin_nhan (
    id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
    cuoc_tro_chuyen_id     BIGINT NOT NULL,
    nguoi_gui              NVARCHAR(20) NOT NULL,     -- 'KHACH' | 'NHAN_VIEN' | 'ADMIN' | 'AI'
    nhan_vien_id           INT NULL,
    noi_dung               NVARCHAR(MAX) NOT NULL,
    loai_nguoi_gui        NVARCHAR(20) DEFAULT 'ANONYMOUS', -- ANONYMOUS | KHACH_HANG | NHAN_VIEN | ADMIN | AI
    da_doc                 BIT DEFAULT 0,
    la_câu_hỏi_của_ai      BIT DEFAULT 0,           -- đánh dấu tin nhắn AI gợi ý (context)
    created_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tn_ctc FOREIGN KEY (cuoc_tro_chuyen_id) REFERENCES cuoc_tro_chuyen(id),
    CONSTRAINT fk_tn_nv FOREIGN KEY (nhan_vien_id) REFERENCES nhan_vien(nhan_vien_id)
);

-- Bảng 3: Cơ sở kiến thức cho AI (RAG)
CREATE TABLE ai_kien_thuc (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    loai            NVARCHAR(30) NOT NULL,           -- 'SAN_PHAM' | 'CHINH_SACH' | 'FAQ' | 'KHAC'
    tieu_de         NVARCHAR(255) NOT NULL,
    noi_dung        NVARCHAR(MAX) NOT NULL,
    san_pham_id     INT NULL,                         -- NULL nếu không phải về sản phẩm
    embedding       VARBINARY(MAX) NULL,              -- vector embedding (optional, nếu dùng vector DB)
    active          BIT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_kt_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham(san_pham_id)
);

-- Index cho performance
CREATE INDEX idx_ctc_loai ON cuoc_tro_chuyen(loai_khach);
CREATE INDEX idx_ctc_kh ON cuoc_tro_chuyen(khach_hang_id);
CREATE INDEX idx_ctc_trang_thai ON cuoc_tro_chuyen(trang_thai);
CREATE INDEX idx_tn_ctc ON tin_nhan(cuoc_tro_chuyen_id);
CREATE INDEX idx_ai_kien_thuc_loai ON ai_kien_thuc(loai);
```

---

## 3. Backend

### 3.1 Dependencies (pom.xml)

```xml
<!-- Spring AI OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 3.2 Cấu hình (application.properties)

```properties
# Spring AI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.chat.role-indicator.enabled=true

# WebSocket
spring.websocket.path=/ws/chat
```

### 3.3 AI System Prompt (có thể điều chỉnh)

```text
Bạn là trợ lý AI của cửa hàng laptop SAOClub.
Nhiệm vụ:
- Trả lời câu hỏi về sản phẩm, giá cả, chính sách
- Gợi ý sản phẩm phù hợp
- Hướng dẫn mua hàng, thanh toán, vận chuyển
- Nếu không biết → xin lỗi và gợi ý chat với nhân viên

QUAN TRỌNG: Khi khách yêu cầu chat với nhân viên ("chuyển nhân viên", "cần người tư vấn", ...)
→ phải nói rõ: "Tôi sẽ kết nối bạn với nhân viên. Vui lòng đợi trong giây lát."
→ KHÔNG tự trả lời câu hỏi nữa sau khi khách yêu cầu chuyển.

Không bình luận về chính trị, tôn giáo hay các vấn đề nhạy cảm.
```

### 3.4 API Endpoints

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| POST | `/api/chat/tao-phien` | Public | Tạo cuộc trò chuyện mới |
| GET | `/api/chat/phien/{sessionId}` | Public | Lấy cuộc trò chuyện |
| GET | `/api/chat/{id}/tin-nhan` | JWT | Lấy tin nhắn (phân trang) |
| POST | `/api/chat/{id}/tin-nhan` | JWT/Public | Gửi tin nhắn (AI/NV) |
| POST | `/api/chat/{id}/escalate` | Public | Khách yêu cầu chat NV |
| POST | `/api/chat/{id}/back-to-ai` | JWT | Quay lại AI |
| GET | `/api/chat/danh-sach` | Staff | DS cuộc trò chuyện |
| PUT | `/api/chat/{id}/trang-thai` | Staff | Cập nhật trạng thái |
| PUT | `/api/chat/{id}/nhan-vien` | Staff | Nhận tiếp cuộc trò chuyện |
| GET | `/api/chat/khach/{khId}` | Staff | Lịch sử chat của KH |
| GET | `/api/chat/ai-kien-thuc` | Staff | DS kiến thức AI |
| POST | `/api/chat/ai-kien-thuc` | Admin | Thêm kiến thức AI |
| PUT | `/api/chat/ai-kien-thuc/{id}` | Admin | Cập nhật kiến thức AI |
| DELETE | `/api/chat/ai-kien-thuc/{id}` | Admin | Xóa kiến thức AI |

### 3.5 Backend Files

```
entity/
  CuocTroChuyen.java
  TinNhan.java
  AiKienThuc.java

repository/
  CuocTroChuyenRepository.java
  TinNhanRepository.java
  AiKienThucRepository.java

service/
  ChatService.java          — quản lý cuộc trò chuyện
  AiChatService.java        — AI chatbot (Spring AI)
  AiKienThucService.java    — quản lý cơ sở kiến thức

config/
  WebSocketConfig.java      — STOMP endpoints
  SecurityConfig.java       — cập nhật cho /ws/chat

websocket/
  ChatWebSocketHandler.java — real-time messaging

controller/
  ChatController.java       — REST API
```

---

## 4. Frontend

### 4.1 ChatWidget.vue (Client)

```
┌──────────────────────────────────┐
│ 🤖 SAOClub Bot         [─][×]   │  ← Header với icon Bot + badge AI
├──────────────────────────────────┤
│                                  │
│  🤖 Bot: Chào bạn! Mình là trợ  │
│         lý của SAOClub. Bạn cần  │
│         hỗ trợ gì hôm nay?       │
│                        10:30 AM   │
│                                  │
│  👤 Ẩn danh: Cho mình hỏi laptop│
│            nào giá dưới 15 triệu?│
│                        10:31 AM   │
│                                  │
│  🤖 Bot: Mình gợi ý bạn dòng... │
│         [ASUS VivoBook 15]       │
│         Giá: 14.990.000đ         │
│         [Xem chi tiết →]         │
│                        10:31 AM   │
│                                  │
├──────────────────────────────────┤
│  [Nhắn tin cho Bot...] [Gửi]     │
│                                  │
│  ┌──────────────────────────────┐│
│  │ Hoặc [💬 Chat với nhân viên] ││  ← Nút chuyển sang NV
│  └──────────────────────────────┘│
└──────────────────────────────────┘
```

**Sau khi chuyển sang nhân viên:**
```
┌──────────────────────────────────┐
│ 👑 Admin CSKH         [─][×]   │  ← Badge đỏ "Admin"
├──────────────────────────────────┤
│  🤖 Bot: Tôi đã kết nối bạn    │
│         với nhân viên. Vui lòng  │
│         đợi trong giây lát.      │
│                        10:32 AM   │
│                                  │
│  🧑‍💻 Admin: Xin chào! Mình là     │
│            Admin, sẵn sàng hỗ trợ │
│            bạn. Bạn cần hỏi gì?   │
│                        10:32 AM   │
│                                  │
├──────────────────────────────────┤
│  [Nhắn tin...] [Gửi]            │
│                                  │
│  ┌──────────────────────────────┐│
│  │ [🤖 Quay lại chat Bot]       ││  ← Quay lại AI
│  └──────────────────────────────┘│
└──────────────────────────────────┘
```

### 4.2 AdminChatPanel.vue

```
┌────────────────────────────────────────────────────────────────────┐
│ Chat quản lý khách hàng                                [🔔 3]    │
├─────────────────┬──────────────────────────────────────────────────┤
│ Tabs:           │                                                  │
│  [●] Hệ thống  │  👤 Nguyễn Văn A (khách hàng)                   │
│  [ ] Khách lẻ   │  Session: abc123 • Trạng thái: HOI_DAP_AI       │
│                 ├──────────────────────────────────────────────────┤
│ ─────────────── │                                                  │
│ 🔴 Khách mới    │  🤖 Bot: Chào bạn! Mình có thể giúp gì?         │
│  [A] A (1)     │                        AI • 10:30                │
│  [B] B (3)     │                                                  │
│                 │  👤 A: cho mình hỏi laptop gaming               │
│ ─────────────── │                        Ẩn danh • 10:31          │
│ 🟡 Đang xử lý  │                                                  │
│  [C] C         │  🤖 Bot: Bạn tham khảo dòng ASUS ROG...          │
│                 │                        AI • 10:31                 │
│ ─────────────── │                                                  │
│ 🟢 Hoàn thành  │  👤 A: chuyển nhân viên đi                       │
│  [D] D         │                        Ẩn danh • 10:32          │
│                 │                                                  │
│                 │  🤖 Bot: Tôi đã kết nối bạn với nhân viên...    │
│                 │                        AI • 10:32                 │
│                 │                                                  │
│                 │  👑 Admin: Xin chào! Mình sẵn sàng hỗ trợ bạn! │
│                 │                        Admin • 10:32              │
│                 ├──────────────────────────────────────────────────┤
│                 │  [Nhập tin nhắn...           ] [Gửi]            │
│                 │  [🤖 Quay lại AI] [📋 Xem lịch sử] [Đóng]     │
└─────────────────┴──────────────────────────────────────────────────┘
```

### 4.3 Badge phân biệt (Frontend)

```vue
<!-- Badge Component -->
<span class="chat-badge" :class="badgeClass">
  {{ badgeIcon }} {{ badgeLabel }}
</span>

<!-- CSS classes -->
.ai-badge   { background: #8b5cf6; color: white; }  /* Tím */
.nv-badge   { background: #f97316; color: white; }  /* Cam */
.admin-badge { background: #dc2626; color: white; }  /* Đỏ */
.kh-badge   { background: #22c55e; color: white; }   /* Xanh lá */
.anonymous-badge { background: #6b7280; color: white; } /* Xám */
```

---

## 5. AI Chatbot — Chi tiết

### 5.1 RAG (Retrieval-Augmented Generation)

```
User query → Embedding → Vector search → Top-k relevant docs → LLM → Response
                              ↑
                        ai_kien_thuc table
                        (hoặc Elasticsearch/Pinecone nếu cần)
```

**Cơ sở kiến thức mặc định:**

| Loại | Nguồn | Ví dụ |
|------|--------|--------|
| `SAN_PHAM` | DB sản phẩm | tên, giá, mô tả, thông số |
| `FAQ` | AI manual | Chính sách đổi trả, bảo hành |
| `CHINH_SACH` | Admin nhập | Phương thức thanh toán, vận chuyển |

### 5.2 Keywords trigger chuyển sang NV

```javascript
const ESCALATE_KEYWORDS = [
  'nhân viên', 'tư vấn', 'người thật', 'người thật',
  'chat với người', 'chuyển', 'kết nối nhân viên',
  'không hiểu', 'cần người', 'agent', 'staff',
  'đặt hàng', 'mua', 'thanh toán',
  // có thể mở rộng
];
```

### 5.3 Keywords quay lại AI

```javascript
const BACK_TO_AI_KEYWORDS = [
  'quay lại bot', 'chat với bot', 'bot', 'AI',
  'tự trả lời', 'không cần nhân viên',
];
```

---

## 6. Security

### 6.1 WebSocket authentication

- Client gửi JWT token khi connect STOMP
- Server validate token, lưu username + role vào session
- Subscribe topic theo quyền:
  - `/topic/chat.{id}` — tất cả người trong cuộc trò chuyện
  - `/topic/staff.notifications` — tất cả staff
  - `/user/queue/personal` — notification riêng

### 6.2 Anonymous handling

- Session ID được tạo server-side (UUID), gửi về client qua cookie hoặc localStorage
- Client không thể tự tạo session ID
- Server validate sessionId trước khi xử lý

---

## 7. Thứ tự Implementation

### Phase 1: Database + Backend core (1-2 ngày)
- [ ] Thêm bảng SQL
- [ ] Entity + Repository
- [ ] ChatService cơ bản
- [ ] REST API endpoints

### Phase 2: AI Chatbot (2-3 ngày) ⚠️ Quan trọng nhất
- [ ] Thêm Spring AI dependency
- [ ] Cấu hình OpenAI API key
- [ ] AiChatService — RAG + LLM
- [ ] AiKienThucService — quản lý cơ sở kiến thức
- [ ] Sync sản phẩm vào bảng ai_kien_thuc
- [ ] Detect escalation keywords
- [ ] Detect back-to-AI keywords

### Phase 3: WebSocket real-time (1 ngày)
- [ ] WebSocketConfig (STOMP)
- [ ] Update SecurityConfig
- [ ] ChatWebSocketHandler
- [ ] Frontend STOMP client

### Phase 4: Frontend — ChatWidget (1-2 ngày)
- [ ] Cải tiện ChatWidget.vue
- [ ] Badge phân biệt AI/NV/KH
- [ ] Nút chuyển / quay lại AI
- [ ] Polling fallback

### Phase 5: Frontend — Admin Panel (1-2 ngày)
- [ ] ChatManagementPanel.vue (2 tab)
- [ ] Danh sách cuộc trò chuyện
- [ ] Chat window với badge
- [ ] Action buttons (nhận, chuyển, đóng)
- [ ] Badge đếm thông báo

### Phase 6: Polish & Test (1 ngày)
- [ ] Lưu lịch sử chat (xem lại)
- [ ] Browser notification
- [ ] Test multi-user
- [ ] Fix edge cases

---

## 8. Đánh đổi quan trọng

### AI Provider

| Lựa chọn | Chi phí | Độ thông minh | Độ khó |
|----------|---------|---------------|--------|
| **OpenAI GPT-4o-mini** | ~$0.15/1M tokens | Rất cao | Dễ |
| **Anthropic Claude** | ~$3/1M input | Rất cao | Dễ |
| **Ollama (local)** | Miễn phí | Trung bình | Trung bình |
| **Rule-based (keyword)** | Miễn phí | Thấp | Rất dễ |

**Khuyến nghị:** Bắt đầu với **GPT-4o-mini** (OpenAI) vì:
- Spring AI hỗ trợ native
- Giá rẻ, free tier đủ cho development
- Chất lượng tốt cho use case FAQ/sản phẩm

### Embedding cho RAG

| Lựa chọn | Chi phí | Độ chính xác |
|----------|---------|-------------|
| OpenAI `text-embedding-3-small` | $0.02/1M | Cao |
| Ollama local | Miễn phí | Trung bình |
| **Simple keyword match (no vector)** | Miễn phí | Thấp-trung |

**Khuyến nghị:** Bắt đầu với **simple keyword match** (LIKE query trên `noi_dung`), sau nâng cấp lên vector search nếu cần.

---

## 9. Câu hỏi cần xác nhận

1. **AI Provider:** Bạn có OpenAI API key chưa? Hay muốn dùng Ollama local (miễn phí, cài trên máy)?

2. **Cơ sở kiến thức ban đầu:** Admin nhập tay FAQ + chính sách, hay tự động sync từ:
   - Mô tả sản phẩm (DB)?
   - Bài viết/chính sách có sẵn?

3. **Số lần escalate:** Có giới hạn không (ví dụ: chỉ chuyển NV tối đa 3 lần/cuộc)?

Trả lời hoặc "cứ làm theo mặc định", tôi bắt đầu implementation.
