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
const botThinking = ref(false); // hiển thị indicator khi bot đang suy nghĩ
const sessionId = ref(null);
const cuocTroChuyenId = ref(null);
const trangThai = ref("HOI_DAP_AI");
const isStaff = computed(() => auth.isAdmin);
const currentKhachHangId = computed(() => {
  if (isStaff.value) return null;
  return auth.user?.khachHangId || auth.user?.id || null;
});

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
      saveSession();
    },
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
    } catch {
      localStorage.removeItem("saoclub_chat_session");
    }
  }

  const khachHangId = currentKhachHangId.value;

  if (khachHangId) {
    // Khách hàng đã đăng nhập → luôn gọi taoPhienChat với khachHangId để gắn/lấy đúng phiên của khách hàng
    try {
      const ctc = await ChatService.taoPhienChat({
        sessionId: sessionId.value || null,
        khachHangId,
        hoTen: auth.user?.hoTen || null,
      });
      sessionId.value = ctc.sessionId || ctc.id?.toString();
      cuocTroChuyenId.value = ctc.id;
      trangThai.value = ctc.trangThai;
      saveSession();
      await loadMessages();
    } catch (e) {
      console.error("Lỗi khởi tạo chat session cho khách hàng:", e);
    }
  } else {
    // Khách lẻ (ẩn danh)
    if (sessionId.value) {
      try {
        const ctc = await ChatService.layPhienChat(sessionId.value);
        cuocTroChuyenId.value = ctc.id;
        trangThai.value = ctc.trangThai;
        await loadMessages();
      } catch {
        sessionId.value = null;
        cuocTroChuyenId.value = null;
      }
    }

    if (!sessionId.value) {
      try {
        const ctc = await ChatService.taoPhienChat({
          sessionId: null,
          khachHangId: null,
          hoTen: null,
        });
        sessionId.value = ctc.sessionId || ctc.id?.toString();
        cuocTroChuyenId.value = ctc.id;
        trangThai.value = ctc.trangThai;
        saveSession();
        await loadMessages();
      } catch (e) {
        console.error("Lỗi khởi tạo chat session:", e);
      }
    }
  }

  // Subscribe WebSocket sau khi có cuocTroChuyenId
  if (cuocTroChuyenId.value) {
    subscribeChat(
      cuocTroChuyenId.value,
      // onMessage
      (msg) => {
        if (msg.nguoiGui === "KHACH") {
          // Tin nhắn của chính khách: cập nhật id của optimistic message thay vì thêm mới
          const tempMsg = messages.value.find(
            (m) => String(m.id).startsWith("temp-") && m.noiDung === msg.noiDung,
          );
          if (tempMsg) {
            tempMsg.id = msg.id; // gán id thật, giữ nguyên bubble
          } else if (!messages.value.find((m) => m.id === msg.id)) {
            messages.value.push(formatMessage(msg));
            nextTick(() => scrollToBottom());
          }
          return;
        }

        // Tin nhắn từ bot/nhân viên
        botThinking.value = false; // bot đã trả lời xong
        if (msg.trangThai) {
          trangThai.value = msg.trangThai;
          saveSession();
        }
        if (!messages.value.find((m) => m.id === msg.id)) {
          messages.value.push(formatMessage(msg));
          nextTick(() => scrollToBottom());
        }
      },
      // onStatusChange
      (status) => {
        trangThai.value = status.trangThai;
        saveSession();
      },
    );
  }
}

async function loadMessages() {
  if (!cuocTroChuyenId.value) return;
  loading.value = true;
  try {
    const page = await ChatService.layTinNhan(cuocTroChuyenId.value, 0, 100);
    messages.value = (page.content || []).map(formatMessage);
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
      khachHangId: currentKhachHangId.value,
    }),
  );
}

// ─── Send message ─────────────────────────────────────────────────────────────
async function send() {
  const text = input.value.trim();
  if (!text || loading.value) return;

  // Chưa có phiên chat → thử khởi tạo lại
  if (!cuocTroChuyenId.value) {
    console.warn("[Chat] cuocTroChuyenId chưa có, đang khởi tạo session...");
    await initSession();
    if (!cuocTroChuyenId.value) {
      console.error("[Chat] Không thể khởi tạo phiên chat.");
      return;
    }
  }

  input.value = "";

  // Thêm ngay vào UI (optimistic)
  const tempId = `temp-${Date.now()}`;
  messages.value.push(
    formatMessage({
      id: tempId,
      nguoiGui: "KHACH",
      loaiNguoiGui: currentKhachHangId.value ? "KHACH_HANG" : "ANONYMOUS",
      tenNguoiGui: auth.user?.hoTen || "Ẩn danh",
      noiDung: text,
      createdAt: new Date().toISOString(),
      daDoc: true,
    }),
  );
  nextTick(() => scrollToBottom());

  loading.value = true;
  // Hiện thinking indicator nếu đang ở chế độ AI
  if (trangThai.value === "HOI_DAP_AI") {
    botThinking.value = true;
  }

  try {
    const reply = await ChatService.guiTinNhan(cuocTroChuyenId.value, text, sessionId.value);
    if (reply.trangThai) {
      trangThai.value = reply.trangThai;
      saveSession();
    }

    if (reply.nguoiGui === "KHACH") {
      // Server trả về xác nhận tin nhắn khách (ví dụ chat với nhân viên)
      const optMsg = messages.value.find(
        (m) => m.id === tempId || (String(m.id).startsWith("temp-") && m.noiDung === reply.noiDung),
      );
      if (optMsg) {
        optMsg.id = reply.id;
        optMsg.createdAt = reply.createdAt || optMsg.createdAt;
      } else if (!messages.value.find((m) => m.id === reply.id)) {
        messages.value.push(formatMessage(reply));
      }
    } else {
      // Server trả về câu trả lời của Bot/AI/Escalate/Chuyển chế độ
      botThinking.value = false;
      if (!messages.value.find((m) => m.id === reply.id)) {
        messages.value.push(formatMessage(reply));
      }
    }
    nextTick(() => scrollToBottom());
  } catch (e) {
    // Remove optimistic message on error
    messages.value = messages.value.filter((m) => m.id !== tempId);
    botThinking.value = false;
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
    isMe: ["KHACH_HANG", "ANONYMOUS"].includes(msg.loaiNguoiGui) && msg.nguoiGui === "KHACH",
    isBot: msg.loaiNguoiGui === "AI",
    isStaff: ["NHAN_VIEN", "ADMIN"].includes(msg.loaiNguoiGui),
  };
}

// ─── Quick suggestions & Markdown rendering ──────────────────────────────────
const quickSuggestions = [
  "💻 Tư vấn laptop",
  "🎁 Khuyến mãi",
  "📦 Tra cứu đơn",
  "🛡️ Bảo hành",
  "🏪 Cửa hàng ở đâu?",
];

function sendQuick(chip) {
  input.value = chip;
  send();
}

function renderMarkdown(text) {
  if (!text) return "";
  let out = text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  // Format markdown tables
  out = out.replace(
    /(?:(?:^|\n)\|[^\n]+\|\r?\n(?:\|[-: |]+\|\r?\n)(?:\|[^\n]+\|\r?\n?)+)/g,
    (match) => {
      const lines = match
        .trim()
        .split(/\r?\n/)
        .filter((l) => l.trim().startsWith("|"));
      if (lines.length < 2) return match;
      let html = '<div class="chat-md-table-wrap"><table>';
      let isHeader = true;
      for (let i = 0; i < lines.length; i++) {
        if (i === 1) continue;
        const cells = lines[i]
          .split("|")
          .slice(1, -1)
          .map((c) => c.trim());
        html += "<tr>";
        for (const c of cells) {
          const tag = isHeader ? "th" : "td";
          html += `<${tag}>${c}</${tag}>`;
        }
        html += "</tr>";
        if (i === 0) isHeader = false;
      }
      html += "</table></div>";
      return html;
    },
  );

  out = out
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.*?)\*/g, "<em>$1</em>")
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/^### (.+)$/gm, "<h4>$1</h4>")
    .replace(/^## (.+)$/gm, "<h3>$1</h3>")
    .replace(/^# (.+)$/gm, "<h2>$1</h2>")
    .replace(/^[•\-\*]\s+(.+)$/gm, "<li>$1</li>")
    .replace(/\n/g, "<br>");

  return out;
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

watch(currentKhachHangId, (newId, oldId) => {
  if (newId !== oldId) {
    if (!newId) {
      localStorage.removeItem("saoclub_chat_session");
      sessionId.value = null;
      cuocTroChuyenId.value = null;
      messages.value = [];
    }
    initSession();
  }
});

watch(open, (v) => {
  if (v) {
    nextTick(() => scrollToBottom());
    // initSession và setupWebSocket đã được gọi trong onMounted
    // Chỉ cần init lại nếu session bị mất (ví dụ reload trang giữa chừng)
    if (!cuocTroChuyenId.value) {
      initSession();
    }
  }
});

let pollTimer = null;

function startPolling() {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    if (open.value && cuocTroChuyenId.value && trangThai.value === "CHAT_NHAN_VIEN") {
      try {
        const page = await ChatService.layTinNhan(cuocTroChuyenId.value, 0, 100);
        const fetched = (page.content || []).map(formatMessage);
        if (
          fetched.length !== messages.value.length ||
          (fetched.length > 0 && messages.value.length > 0 && fetched[fetched.length - 1].id !== messages.value[messages.value.length - 1].id)
        ) {
          messages.value = fetched;
          nextTick(() => scrollToBottom());
        }
      } catch {}
    }
  }, 5000);
}

onMounted(() => {
  // Khởi tạo session và kết nối WebSocket một lần duy nhất
  initSession();
  setupWebSocket();
  startPolling();
});

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer);
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
          <span style="font-size: 18px">{{ trangThai === "CHAT_NHAN_VIEN" ? "🧑‍💻" : "🤖" }}</span>
        </div>
        <div class="chat-popup-meta">
          <div class="chat-popup-title">
            {{ trangThai === "CHAT_NHAN_VIEN" ? "Hỗ trợ SAOClub" : "SAOClub Bot" }}
          </div>
          <div class="chat-popup-sub">
            <span class="status-dot" :style="{ background: statusColor }"></span>
            {{ statusLabel }}
          </div>
        </div>
        <button class="chat-popup-close" :aria-label="'Đóng'" @click="open = false">
          <i class="fa fa-chevron-down"></i>
        </button>
      </div>

      <!-- Body -->
      <div ref="chatBodyRef" class="chat-popup-body">
        <div v-if="loading && messages.length === 0" class="chat-loading">
          <div class="chat-loading-dots"><span></span><span></span><span></span></div>
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
          <div v-if="!m.isMe" class="chat-avatar" :style="{ background: m.avatarBg }">
            {{ m.avatarIcon }}
          </div>

          <div class="chat-bubble-wrap">
            <!-- Sender name -->
            <div v-if="!m.isMe && !m.isBot" class="chat-sender-label" :class="{ staff: m.isStaff }">
              {{ m.senderLabel }}
            </div>
            <div v-else-if="!m.isMe && m.isBot" class="chat-sender-label bot-label">
              🤖 {{ m.senderLabel }}
            </div>

            <!-- Bubble -->
            <div
              class="chat-popup-bubble"
              :class="{ 'bubble-bot': m.isBot, 'bubble-staff': m.isStaff }"
            >
              <span class="chat-popup-bubble-text" v-html="renderMarkdown(m.noiDung)"></span>
              <span class="chat-popup-bubble-time">{{ m.time }}</span>
            </div>
          </div>
        </div>

        <!-- Bot thinking indicator -->
        <div v-if="botThinking" class="chat-popup-msg bot thinking-msg">
          <div class="chat-avatar" style="background: #8b5cf6">🤖</div>
          <div class="chat-bubble-wrap">
            <div class="chat-sender-label bot-label">🤖 SAOClub Bot</div>
            <div class="chat-popup-bubble bubble-bot thinking-bubble">
              <span class="thinking-dot"></span>
              <span class="thinking-dot"></span>
              <span class="thinking-dot"></span>
              <span class="thinking-text">Đang phân tích...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick suggestion chips -->
      <div v-if="trangThai === 'HOI_DAP_AI'" class="chat-quick-suggestions">
        <button
          v-for="chip in quickSuggestions"
          :key="chip"
          type="button"
          class="chat-quick-chip"
          :disabled="loading"
          @click="sendQuick(chip)"
        >
          {{ chip }}
        </button>
      </div>

      <!-- Action buttons -->
      <div v-if="trangThai === 'DA_DONG'" class="chat-ended-bar">
        <i class="fa fa-check-circle"></i> Cuộc trò chuyện đã kết thúc
      </div>

      <!-- Composer -->
      <div v-if="trangThai !== 'DA_DONG'" class="chat-popup-composer">
        <textarea
          v-model="input"
          rows="1"
          :placeholder="
            trangThai === 'CHAT_NHAN_VIEN'
              ? 'Nhắn nhân viên (hoặc gõ: chuyển chat bot)...'
              : 'Nhắn tin cho Bot (hoặc gõ: chuyển nhân viên)...'
          "
          :disabled="loading"
          @keyup.enter.exact.prevent="send"
        ></textarea>
        <button class="chat-popup-send" :disabled="!input.trim() || loading" @click="send">
          <i class="fa" :class="loading ? 'fa-spinner fa-spin' : 'fa-paper-plane'"></i>
        </button>
      </div>

      <!-- Mode switch bar (chuyển nhân viên / chuyển chat bot) -->
      <div v-if="trangThai === 'HOI_DAP_AI'" class="chat-escalate-bar">
        <span>Cần tư vấn trực tiếp?</span>
        <button
          type="button"
          class="btn-escalate"
          :disabled="loading"
          @click="sendQuick('chuyển nhân viên')"
        >
          💬 Chuyển nhân viên
        </button>
      </div>
      <div v-else-if="trangThai === 'CHAT_NHAN_VIEN'" class="chat-escalate-bar chat-bot-bar">
        <span>Quay lại hỏi tự động?</span>
        <button
          type="button"
          class="btn-escalate btn-back-bot"
          :disabled="loading"
          @click="sendQuick('chuyển chat bot')"
        >
          🤖 Chuyển chat bot
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
  box-shadow:
    0 4px 0 var(--pink-700, #a81b5d),
    0 6px 16px rgba(168, 27, 93, 0.4);
  border-bottom: 4px solid var(--pink-700, #a81b5d);
  transition: all 0.15s ease;
}
.chat-fab:hover {
  transform: translateY(-2px);
  background: linear-gradient(180deg, var(--pink-400, #ec4899) 0%, var(--pink-600, #db2777) 100%);
  box-shadow:
    0 6px 0 var(--pink-700, #a81b5d),
    0 10px 24px rgba(168, 27, 93, 0.45);
}
.chat-fab:active {
  transform: translateY(2px);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
  border-bottom-width: 0;
}
.chat-fab.is-open {
  background: linear-gradient(180deg, #6b7280 0%, #4b5563 100%);
  box-shadow:
    0 4px 0 #374151,
    0 6px 16px rgba(0, 0, 0, 0.3);
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
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.6);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
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
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-popup-meta {
  flex: 1;
}
.chat-popup-title {
  font-size: 14px;
  font-weight: 700;
}
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
.chat-popup-close:hover {
  background: rgba(255, 255, 255, 0.2);
  opacity: 1;
}

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
.chat-loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.chat-loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-popup-msg {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}
.chat-popup-msg.right {
  flex-direction: row-reverse;
}

/* ─── Bot thinking indicator ─── */
.thinking-msg {
  animation: fadeInUp 0.3s ease;
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 14px !important;
  min-width: 100px;
}
.thinking-dot {
  width: 7px;
  height: 7px;
  background: #8b5cf6;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
  animation: thinkBounce 1.2s infinite ease-in-out;
}
.thinking-dot:nth-child(1) {
  animation-delay: 0s;
}
.thinking-dot:nth-child(2) {
  animation-delay: 0.2s;
}
.thinking-dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes thinkBounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}
.thinking-text {
  font-size: 11px;
  color: #7c3aed;
  font-style: italic;
  margin-left: 2px;
  white-space: nowrap;
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
.chat-sender-label.staff {
  color: #f97316;
}
.chat-sender-label.bot-label {
  color: #8b5cf6;
}

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
.chat-popup-msg.right .chat-popup-bubble-time {
  color: rgba(255, 255, 255, 0.8);
}

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
  box-shadow:
    0 2px 0 var(--pink-700, #a81b5d),
    0 3px 6px rgba(168, 27, 93, 0.3);
  border-bottom: 2px solid var(--pink-700, #a81b5d);
  transition: all 0.12s ease;
}
.chat-popup-send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 3px 0 var(--pink-700, #a81b5d),
    0 4px 8px rgba(168, 27, 93, 0.4);
}
.chat-popup-send:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
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
.chat-escalate-bar span {
  flex: 1;
}
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
.btn-escalate:hover {
  background: #ea580c;
}
.btn-escalate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.chat-bot-bar {
  background: #ede9fe;
  border-top-color: #c4b5fd;
  color: #5b21b6;
}
.btn-back-bot {
  background: #8b5cf6;
}
.btn-back-bot:hover {
  background: #7c3aed;
}

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

/* ═══ Quick suggestions chips ═══ */
.chat-quick-suggestions {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 6px 10px;
  background: rgba(244, 114, 182, 0.08);
  border-top: 1px solid var(--border-color-soft, #f1dbe6);
  scrollbar-width: none;
}
.chat-quick-suggestions::-webkit-scrollbar {
  display: none;
}
.chat-quick-chip {
  flex-shrink: 0;
  padding: 4px 10px;
  background: #fff;
  border: 1px solid var(--pink-300, #f472b6);
  border-radius: 16px;
  color: var(--pink-600, #db2777);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.chat-quick-chip:hover:not(:disabled) {
  background: var(--pink-500, #ec4899);
  color: #fff;
  border-color: var(--pink-500, #ec4899);
}
.chat-quick-chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ═══ Markdown table & formatting in bubbles ═══ */
.chat-popup-bubble-text strong {
  color: inherit;
  font-weight: 700;
}
.chat-popup-bubble-text code {
  background: rgba(0, 0, 0, 0.07);
  padding: 1px 4px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.9em;
}
.chat-popup-bubble-text li {
  margin-left: 14px;
  margin-bottom: 2px;
}
.chat-md-table-wrap {
  overflow-x: auto;
  margin: 6px 0;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.12);
}
.chat-md-table-wrap table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}
.chat-md-table-wrap th,
.chat-md-table-wrap td {
  padding: 4px 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  text-align: left;
}
.chat-md-table-wrap th {
  background: rgba(0, 0, 0, 0.05);
  font-weight: 700;
}

/* ═══ Responsive ═══ */
@media (max-width: 480px) {
  .chat-widget {
    right: 16px;
    bottom: 16px;
  }
  .chat-popup {
    width: calc(100vw - 32px);
    height: 70vh;
  }
}
</style>
