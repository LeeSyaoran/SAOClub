<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { AuthStore } from "../../stores/index.js";
import * as ChatService from "../../services/ChatService.js";
import { useChatWebSocket } from "../../composables/useChatWebSocket.js";

const auth = AuthStore;
const open = ref(false);
const messages = ref([]);
const input = ref("");
const chatBodyRef = ref(null);
const loading = ref(false);
const sessionId = ref(null);
const cuocTroChuyenId = ref(null);
const trangThai = ref("HOI_DAP_AI");
const isStaff = computed(() => auth.isAdmin);

// ─── WebSocket ────────────────────────────────────────────────────────────────
const { connected, connect, subscribeChat, subscribeStaffNotifications, disconnect } =
  useChatWebSocket();

function setupWebSocket() {
  connect(
    // onMessage
    (msg) => {
      messages.value.push(formatMessage(msg));
      nextTick(() => scrollToBottom());
    },
    // onStatusChange
    (status) => {
      trangThai.value = status.trangThai;
    }
  );
}

// ─── Init session ─────────────────────────────────────────────────────────────
async function initSession() {
  // Khôi phục session cũ từ localStorage
  const saved = localStorage.getItem("saoclub_chat_session");
  if (saved) {
    try {
      const data = JSON.parse(saved);
      sessionId.value = data.sessionId;
      cuocTroChuyenId.value = data.cuocTroChuyenId;
      trangThai.value = data.trangThai || "HOI_DAP_AI";

      // Kiểm tra cuộc trò chuyện còn hoạt động không
      try {
        const ctc = await ChatService.layPhienChat(sessionId.value);
        cuocTroChuyenId.value = ctc.id;
        trangThai.value = ctc.trangThai;
        await loadMessages();
      } catch {
        // Session hết hạn → tạo mới
        sessionId.value = null;
        cuocTroChuyenId.value = null;
      }
    } catch {
      localStorage.removeItem("saoclub_chat_session");
    }
  }

  // Tạo session mới nếu chưa có
  if (!sessionId.value) {
    try {
      const khachHangId = auth.user?.khachHangId || null;
      const ctc = await ChatService.taoPhienChat({
        sessionId: null,
        khachHangId,
        hoTen: auth.user?.hoTen || null,
      });
      sessionId.value = ctc.sessionId || ctc.id?.toString();
      cuocTroChuyenId.value = ctc.id;
      trangThai.value = ctc.trangThai;
      saveSession();

      // Load tin nhắn ban đầu
      await loadMessages();
    } catch (e) {
      console.error("Lỗi khởi tạo chat session:", e);
    }
  }

  // Subscribe WebSocket sau khi có cuocTroChuyenId
  if (cuocTroChuyenId.value) {
    subscribeChat(
      cuocTroChuyenId.value,
      // onMessage
      (msg) => {
        // Tránh duplicate khi tự gửi
        if (!messages.value.find((m) => m.id === msg.id)) {
          messages.value.push(formatMessage(msg));
          nextTick(() => scrollToBottom());
        }
      },
      // onStatusChange
      (status) => {
        trangThai.value = status.trangThai;
      }
    );
  }
}

async function loadMessages() {
  if (!cuocTroChuyenId.value) return;
  loading.value = true;
  try {
    const page = await ChatService.layTinNhan(cuocTroChuyenId.value, 0, 100);
    messages.value = (page.content || []).map(formatMessage).reverse();
    nextTick(() => scrollToBottom());
  } catch (e) {
    console.error("Lỗi load tin nhắn:", e);
  } finally {
    loading.value = false;
  }
}

function saveSession() {
  if (!cuocTroChuyenId.value) return;
  localStorage.setItem(
    "saoclub_chat_session",
    JSON.stringify({
      sessionId: sessionId.value,
      cuocTroChuyenId: cuocTroChuyenId.value,
      trangThai: trangThai.value,
    })
  );
}

// ─── Send message ─────────────────────────────────────────────────────────────
async function send() {
  const text = input.value.trim();
  if (!text || loading.value) return;
  input.value = "";

  // Thêm ngay vào UI (optimistic)
  const tempId = `temp-${Date.now()}`;
  messages.value.push({
    id: tempId,
    nguoiGui: "KHACH",
    loaiNguoiGui: auth.user?.khachHangId ? "KHACH_HANG" : "ANONYMOUS",
    tenNguoiGui: auth.user?.hoTen || "Ẩn danh",
    noiDung: text,
    createdAt: new Date().toISOString(),
    daDoc: true,
  });
  nextTick(() => scrollToBottom());

  loading.value = true;
  try {
    const reply = await ChatService.guiTinNhan(cuocTroChuyenId.value, text, sessionId.value);
    trangThai.value = reply.trangThai || trangThai.value;

    // Xóa temp message và thêm real messages (AI reply)
    messages.value = messages.value.filter((m) => m.id !== tempId);
    const existing = messages.value.find((m) => m.id === reply.id);
    if (!existing) {
      messages.value.push(formatMessage(reply));
      nextTick(() => scrollToBottom());
    }
  } catch (e) {
    // Remove optimistic message on error
    messages.value = messages.value.filter((m) => m.id !== tempId);
    console.error("Lỗi gửi tin nhắn:", e);
  } finally {
    loading.value = false;
  }
}

// ─── Format helpers ────────────────────────────────────────────────────────────
function formatMessage(msg) {
  const time = msg.createdAt
    ? new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    : "";

  let senderLabel = msg.tenNguoiGui || "Khách";
  let avatarIcon = "👤";
  let avatarBg = "#6b7280";

  switch (msg.loaiNguoiGui) {
    case "AI":
      senderLabel = "SAOClub Bot";
      avatarIcon = "🤖";
      avatarBg = "#8b5cf6";
      break;
    case "ADMIN":
      senderLabel = `${msg.tenNguoiGui || "Admin"}`;
      avatarIcon = "👑";
      avatarBg = "#dc2626";
      break;
    case "NHAN_VIEN":
      senderLabel = `${msg.tenNguoiGui || "NV"}`;
      avatarIcon = "🧑‍💻";
      avatarBg = "#f97316";
      break;
    case "KHACH_HANG":
      avatarIcon = "👤";
      avatarBg = "#22c55e";
      break;
    case "ANONYMOUS":
    default:
      avatarIcon = "👤";
      avatarBg = "#6b7280";
      senderLabel = msg.tenNguoiGui || "Ẩn danh";
      break;
  }

  return {
    ...msg,
    time,
    senderLabel,
    avatarIcon,
    avatarBg,
    isMe: ["KHACH_HANG", "ANONYMOUS"].includes(msg.loaiNguoiGui) &&
      msg.nguoiGui === "KHACH",
    isBot: msg.loaiNguoiGui === "AI",
    isStaff: ["NHAN_VIEN", "ADMIN"].includes(msg.loaiNguoiGui),
  };
}

// ─── Status helpers ────────────────────────────────────────────────────────────
const statusLabel = computed(() => {
  if (trangThai.value === "CHAT_NHAN_VIEN") return "Nhân viên đang trả lời";
  if (trangThai.value === "DA_DONG") return "Cuộc trò chuyện đã đóng";
  return "Bot đang trả lời";
});

const statusColor = computed(() => {
  if (trangThai.value === "CHAT_NHAN_VIEN") return "#f97316";
  if (trangThai.value === "DA_DONG") return "#6b7280";
  return "#10b981";
});

const showEscalateButton = computed(() => {
  return trangThai.value === "HOI_DAP_AI";
});

// ─── Scroll ──────────────────────────────────────────────────────────────────
function scrollToBottom() {
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
  }
}

watch(open, (v) => {
  if (v) {
    nextTick(() => scrollToBottom());
    if (!sessionId.value) {
      initSession();
    }
    setupWebSocket();
  }
});

onMounted(() => {
  // Khởi tạo session sớm để bot đã reply khi mở chat
  initSession();
  setupWebSocket();
});

onUnmounted(() => {
  disconnect();
});
</script>

<template>
  <div class="chat-widget">
    <!-- FAB -->
    <button
      class="chat-fab"
      :class="{ 'is-open': open }"
      :aria-label="open ? 'Đóng chat' : 'Mở chat'"
      @click="open = !open"
    >
      <i class="fa" :class="open ? 'fa-times' : 'fa-comments'"></i>
      <span v-if="!open" class="chat-fab-pulse"></span>
    </button>

    <!-- Popup -->
    <div v-if="open" class="chat-popup">
      <!-- Header -->
      <div class="chat-popup-header">
        <div class="chat-popup-avatar">
          <span style="font-size: 18px">🤖</span>
        </div>
        <div class="chat-popup-meta">
          <div class="chat-popup-title">SAOClub Bot</div>
          <div class="chat-popup-sub">
            <span
              class="status-dot"
              :style="{ background: statusColor }"
            ></span>
            {{ statusLabel }}
          </div>
        </div>
        <button
          class="chat-popup-close"
          :aria-label="'Đóng'"
          @click="open = false"
        >
          <i class="fa fa-chevron-down"></i>
        </button>
      </div>

      <!-- Body -->
      <div ref="chatBodyRef" class="chat-popup-body">
        <div v-if="loading && messages.length === 0" class="chat-loading">
          <div class="chat-loading-dots">
            <span></span><span></span><span></span>
          </div>
        </div>

        <div
          v-for="m in messages"
          :key="m.id"
          class="chat-popup-msg"
          :class="{
            right: m.isMe,
            bot: m.isBot,
            staff: m.isStaff,
          }"
        >
          <!-- Avatar -->
          <div
            v-if="!m.isMe"
            class="chat-avatar"
            :style="{ background: m.avatarBg }"
          >
            {{ m.avatarIcon }}
          </div>

          <div class="chat-bubble-wrap">
            <!-- Sender name -->
            <div
              v-if="!m.isMe && !m.isBot"
              class="chat-sender-label"
              :class="{ staff: m.isStaff }"
            >
              {{ m.senderLabel }}
            </div>
            <div
              v-else-if="!m.isMe && m.isBot"
              class="chat-sender-label bot-label"
            >
              🤖 {{ m.senderLabel }}
            </div>

            <!-- Bubble -->
            <div class="chat-popup-bubble" :class="{ 'bubble-bot': m.isBot, 'bubble-staff': m.isStaff }">
              <span class="chat-popup-bubble-text">{{ m.noiDung }}</span>
              <span class="chat-popup-bubble-time">{{ m.time }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Action buttons -->
      <div v-if="trangThai === 'DA_DONG'" class="chat-ended-bar">
        <i class="fa fa-check-circle"></i> Cuộc trò chuyện đã kết thúc
      </div>

      <!-- Composer -->
      <div class="chat-popup-composer" v-if="trangThai !== 'DA_DONG'">
        <textarea
          v-model="input"
          rows="1"
          placeholder="Nhắn tin cho Bot..."
          :disabled="loading"
          @keyup.enter.exact.prevent="send"
        ></textarea>
        <button
          class="chat-popup-send"
          :disabled="!input.trim() || loading"
          @click="send"
        >
          <i class="fa" :class="loading ? 'fa-spinner fa-spin' : 'fa-paper-plane'"></i>
        </button>
      </div>

      <!-- Escalate button -->
      <div v-if="showEscalateButton && trangThai !== 'DA_DONG'" class="chat-escalate-bar">
        <span>Bạn cần hỗ trợ từ nhân viên?</span>
        <button
          class="btn-escalate"
          @click="send"
        >
          💬 Chat với nhân viên
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ═══ Wrapper — fixed bottom-right ═══ */
.chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 999;
}

/* ═══ FAB ═══ */
.chat-fab {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(180deg, var(--pink-500, #db2777) 0%, var(--pink-700, #a81b5d) 100%);
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 0 var(--pink-700, #a81b5d), 0 6px 16px rgba(168, 27, 93, 0.4);
  border-bottom: 4px solid var(--pink-700, #a81b5d);
  transition: all 0.15s ease;
}
.chat-fab:hover {
  transform: translateY(-2px);
  background: linear-gradient(180deg, var(--pink-400, #ec4899) 0%, var(--pink-600, #db2777) 100%);
  box-shadow: 0 6px 0 var(--pink-700, #a81b5d), 0 10px 24px rgba(168, 27, 93, 0.45);
}
.chat-fab:active {
  transform: translateY(2px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  border-bottom-width: 0;
}
.chat-fab.is-open {
  background: linear-gradient(180deg, #6b7280 0%, #4b5563 100%);
  box-shadow: 0 4px 0 #374151, 0 6px 16px rgba(0, 0, 0, 0.3);
  border-bottom-color: #374151;
}
.chat-fab-pulse {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 10px;
  height: 10px;
  background: #10b981;
  border: 2px solid #fff;
  border-radius: 50%;
  animation: pulse 1.6s infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.6); }
  50%      { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
}

/* ═══ Popup ═══ */
.chat-popup {
  position: absolute;
  right: 0;
  bottom: 72px;
  width: 340px;
  height: 480px;
  max-height: calc(100vh - 120px);
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(168, 27, 93, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ═══ Header ═══ */
.chat-popup-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: linear-gradient(135deg, var(--pink-400, #ec4899), var(--pink-600, #db2777));
  color: #fff;
}
.chat-popup-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  border: 2px solid rgba(255,255,255,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-popup-meta { flex: 1; }
.chat-popup-title { font-size: 14px; font-weight: 700; }
.chat-popup-sub {
  font-size: 11px;
  opacity: 0.9;
  display: flex;
  align-items: center;
  gap: 4px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
.chat-popup-close {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  opacity: 0.85;
  transition: all 0.15s ease;
}
.chat-popup-close:hover { background: rgba(255,255,255,0.2); opacity: 1; }

/* ═══ Body ═══ */
.chat-popup-body {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--pink-50, #fff5f9);
}

.chat-loading {
  display: flex;
  justify-content: center;
  padding: 20px;
}
.chat-loading-dots {
  display: flex;
  gap: 4px;
}
.chat-loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--pink-400, #ec4899);
  border-radius: 50%;
  animation: bounce 1.2s infinite;
}
.chat-loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.chat-loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-popup-msg {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}
.chat-popup-msg.right {
  flex-direction: row-reverse;
}

.chat-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.chat-bubble-wrap {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.chat-popup-msg.right .chat-bubble-wrap {
  align-items: flex-end;
}

.chat-sender-label {
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  padding: 0 4px;
}
.chat-sender-label.staff { color: #f97316; }
.chat-sender-label.bot-label { color: #8b5cf6; }

.chat-popup-bubble {
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  padding: 8px 12px;
  border-radius: 14px;
  max-width: 100%;
}
.chat-popup-bubble.bubble-bot {
  background: linear-gradient(135deg, #f3e8ff, #ede9fe);
  border-color: #c4b5fd;
}
.chat-popup-bubble.bubble-staff {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-color: #fed7aa;
}
.chat-popup-msg.right .chat-popup-bubble {
  background: linear-gradient(135deg, var(--pink-400, #ec4899), var(--pink-600, #db2777));
  color: #fff;
  border-color: transparent;
}
.chat-popup-msg.right .chat-popup-bubble-time { color: rgba(255,255,255,0.8); }

.chat-popup-bubble-text {
  display: block;
  font-size: 13px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-popup-bubble-time {
  display: block;
  margin-top: 4px;
  font-size: 10px;
  color: var(--muted, #9ca3af);
  text-align: right;
}

/* ═══ Composer ═══ */
.chat-popup-composer {
  display: flex;
  gap: 8px;
  padding: 10px;
  background: #fff;
  border-top: 1px solid var(--border-color-soft, #f1dbe6);
  align-items: flex-end;
}
.chat-popup-composer textarea {
  flex: 1;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 12px;
  background: var(--bg-input, #fff);
  color: var(--text-primary);
  padding: 8px 12px;
  font-size: 13px;
  resize: none;
  font-family: inherit;
  line-height: 1.4;
  max-height: 100px;
}
.chat-popup-composer textarea:focus {
  outline: none;
  border-color: var(--pink-400, #ec4899);
}
.chat-popup-composer textarea:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}
.chat-popup-send {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, var(--pink-500, #db2777) 0%, var(--pink-600, #db2777) 100%);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 0 var(--pink-700, #a81b5d), 0 3px 6px rgba(168, 27, 93, 0.3);
  border-bottom: 2px solid var(--pink-700, #a81b5d);
  transition: all 0.12s ease;
}
.chat-popup-send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 3px 0 var(--pink-700, #a81b5d), 0 4px 8px rgba(168, 27, 93, 0.4);
}
.chat-popup-send:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  border-bottom-width: 0;
}
.chat-popup-send:disabled {
  background: #d1d5db;
  box-shadow: none;
  border-bottom-color: #9ca3af;
  cursor: not-allowed;
}

/* ═══ Escalate bar ═══ */
.chat-escalate-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: #fef3c7;
  border-top: 1px solid #fcd34d;
  font-size: 12px;
  color: #92400e;
}
.chat-escalate-bar span { flex: 1; }
.btn-escalate {
  flex-shrink: 0;
  padding: 4px 10px;
  border: none;
  border-radius: 8px;
  background: #f97316;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.12s;
}
.btn-escalate:hover { background: #ea580c; }

/* ═══ Ended bar ═══ */
.chat-ended-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  background: #f3f4f6;
  border-top: 1px solid #e5e7eb;
  font-size: 12px;
  color: #6b7280;
}

/* ═══ Responsive ═══ */
@media (max-width: 480px) {
  .chat-widget { right: 16px; bottom: 16px; }
  .chat-popup { width: calc(100vw - 32px); height: 70vh; }
}
</style>
