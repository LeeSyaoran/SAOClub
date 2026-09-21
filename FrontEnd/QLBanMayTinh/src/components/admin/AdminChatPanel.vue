<script setup>
/* Bảng điều khiển chat hỗ trợ khách hàng */
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { AuthStore } from "../../stores/index.js";
import * as ChatService from "../../services/ChatService.js";
import { useChatWebSocket } from "../../composables/useChatWebSocket.js";
import { showToast } from "../../stores/toast.js";

const auth = AuthStore;

// ─── Tab state ────────────────────────────────────────────────────────────────
const activeTab = ref("ANONYMOUS"); // ANONYMOUS | HE_THONG
const activeChat = ref(null); // cuộc trò chuyện đang mở
const conversations = ref([]); // danh sách cuộc trò chuyện
const messages = ref([]);
const input = ref("");
const chatBodyRef = ref(null);
const loading = ref(false);
const loadingMessages = ref(false);
const page = ref(0);
const totalPages = ref(0);

// ─── Notification badge ──────────────────────────────────────────────────────
const unreadCount = ref(0);
const unreadMap = ref({}); // cuocTroChuyenId → count

// ─── WebSocket ────────────────────────────────────────────────────────────────
const { connected, connect, subscribeChat, subscribeStaffNotifications, subscribeConversationsList, disconnect } =
  useChatWebSocket();

function setupWebSocket() {
  connect(
    null, // onMessage — xử lý riêng per chat
    null, // onStatusChange — xử lý riêng per chat
    null  // onStaffNotification — xử lý ở đây
  );

  // Thông báo mới cho staff
  subscribeStaffNotifications((notif) => {
    if (notif.type === "new_message") {
      unreadCount.value++;
      // Cập nhật map
      if (!unreadMap.value[notif.cuocTroChuyenId]) {
        unreadMap.value[notif.cuocTroChuyenId] = 0;
      }
      unreadMap.value[notif.cuocTroChuyenId]++;
    } else if (notif.type === "escalate") {
      unreadCount.value++;
      unreadMap.value[notif.cuocTroChuyenId] = (unreadMap.value[notif.cuocTroChuyenId] || 0) + 1;
    }
  });

  // Cập nhật danh sách khi có thay đổi
  subscribeConversationsList(() => {
    loadConversations();
  });
}

// ─── Load conversations ───────────────────────────────────────────────────
async function loadConversations() {
  loading.value = true;
  try {
    const res = await ChatService.layDanhSachChat(activeTab.value, null, 0, 50);
    conversations.value = res.content || [];
    totalPages.value = res.totalPages || 0;
    page.value = 0;
  } catch (e) {
    showToast("Không tải được danh sách cuộc trò chuyện", "error");
  } finally {
    loading.value = false;
  }
}

async function loadMore() {
  if (page.value >= totalPages.value - 1) return;
  loading.value = true;
  try {
    const res = await ChatService.layDanhSachChat(activeTab.value, null, page.value + 1, 50);
    conversations.value.push(...(res.content || []));
    page.value++;
  } catch (e) {
    // ignore
  } finally {
    loading.value = false;
  }
}

// ─── Select conversation ────────────────────────────────────────────────────
let currentSub = null;

async function selectChat(chat) {
  if (activeChat.value?.id === chat.id) return;
  activeChat.value = chat;

  // Unsubscribe chat cũ
  if (currentSub) {
    try { currentSub.sub1?.unsubscribe(); } catch {}
    try { currentSub.sub2?.unsubscribe(); } catch {}
  }

  // Reset unread
  unreadCount.value = Math.max(0, unreadCount.value - (unreadMap.value[chat.id] || 0));
  unreadMap.value[chat.id] = 0;

  // Load tin nhắn
  await loadMessages(chat.id);

  // Subscribe WebSocket
  currentSub = subscribeChat(chat.id,
    (msg) => {
      if (!messages.value.find((m) => m.id === msg.id)) {
        messages.value.push(formatMessage(msg));
        nextTick(() => scrollToBottom());
      }
    },
    (status) => {
      if (activeChat.value?.id === chat.id) {
        activeChat.value = { ...activeChat.value, trangThai: status.trangThai };
        // Cập nhật trong danh sách
        const idx = conversations.value.findIndex((c) => c.id === chat.id);
        if (idx >= 0) conversations.value[idx].trangThai = status.trangThai;
      }
    }
  );
}

async function loadMessages(cuocTroChuyenId) {
  loadingMessages.value = true;
  try {
    const res = await ChatService.layTinNhan(cuocTroChuyenId, 0, 100);
    messages.value = (res.content || []).map(formatMessage).reverse();
    nextTick(() => scrollToBottom());
  } catch (e) {
    showToast("Không tải được tin nhắn", "error");
  } finally {
    loadingMessages.value = false;
  }
}

// ─── Actions ────────────────────────────────────────────────────────────────
async function nhanTiep() {
  if (!activeChat.value) return;
  try {
    const updated = await ChatService.nhanTiepChat(activeChat.value.id);
    activeChat.value = updated;
    showToast("Đã nhận tiếp cuộc trò chuyện", "success");
  } catch (e) {
    showToast("Lỗi: " + e.message, "error");
  }
}

async function guiTinNhan() {
  const text = input.value.trim();
  if (!text || !activeChat.value || loading.value) return;
  input.value = "";

  const tempId = `temp-${Date.now()}`;
  messages.value.push({
    id: tempId,
    nguoiGui: auth.user?.role === "admin" ? "ADMIN" : "NHAN_VIEN",
    loaiNguoiGui: auth.user?.role === "admin" ? "ADMIN" : "NHAN_VIEN",
    tenNguoiGui: auth.user?.hoTen || auth.user?.tenDangNhap,
    noiDung: text,
    createdAt: new Date().toISOString(),
  });
  nextTick(() => scrollToBottom());

  loading.value = true;
  try {
    await ChatService.guiTinNhanTuNhanVien(activeChat.value.id, text);
    // Reply từ WebSocket sẽ tự thêm vào messages
    messages.value = messages.value.filter((m) => m.id !== tempId);
  } catch (e) {
    messages.value = messages.value.filter((m) => m.id !== tempId);
    showToast("Lỗi gửi tin nhắn: " + e.message, "error");
  } finally {
    loading.value = false;
  }
}

async function quayLaiAI() {
  if (!activeChat.value) return;
  try {
    await ChatService.quayLaiAI(activeChat.value.id);
    showToast("Đã chuyển về AI", "success");
    activeChat.value = null;
    messages.value = [];
  } catch (e) {
    showToast("Lỗi: " + e.message, "error");
  }
}

async function dongChat() {
  if (!activeChat.value) return;
  try {
    await ChatService.dongChat(activeChat.value.id);
    showToast("Đã đóng cuộc trò chuyện", "success");
    activeChat.value = null;
    messages.value = [];
    loadConversations();
  } catch (e) {
    showToast("Lỗi: " + e.message, "error");
  }
}

async function huyNhanTiep() {
  await quayLaiAI();
}

// ─── Format helpers ──────────────────────────────────────────────────────────
function formatMessage(msg) {
  const time = msg.createdAt
    ? new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    : "";
  let avatarIcon = "👤";
  let avatarBg = "#6b7280";
  if (msg.loaiNguoiGui === "AI") { avatarIcon = "🤖"; avatarBg = "#8b5cf6"; }
  if (msg.loaiNguoiGui === "ADMIN") { avatarIcon = "👑"; avatarBg = "#dc2626"; }
  if (msg.loaiNguoiGui === "NHAN_VIEN") { avatarIcon = "🧑‍💻"; avatarBg = "#f97316"; }
  if (msg.loaiNguoiGui === "KHACH_HANG") { avatarIcon = "👤"; avatarBg = "#22c55e"; }

  return {
    ...msg,
    time,
    avatarIcon,
    avatarBg,
    isMe: ["ADMIN", "NHAN_VIEN"].includes(msg.loaiNguoiGui) && msg.nguoiGui === "ADMIN" || msg.nguoiGui === "NHAN_VIEN",
    isBot: msg.loaiNguoiGui === "AI",
    isStaff: ["NHAN_VIEN", "ADMIN"].includes(msg.loaiNguoiGui),
  };
}

function formatTrangThai(tt) {
  const map = {
    HOI_DAP_AI: { label: "AI", color: "#10b981" },
    CHAT_NHAN_VIEN: { label: "NV đang trả lời", color: "#f97316" },
    DA_DONG: { label: "Đã đóng", color: "#6b7280" },
  };
  return map[tt] || { label: tt, color: "#6b7280" };
}

function scrollToBottom() {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
}

// ─── Lifecycle ───────────────────────────────────────────────────────────────
watch(activeTab, () => {
  activeChat.value = null;
  messages.value = [];
  loadConversations();
});

onMounted(() => {
  loadConversations();
  setupWebSocket();
  // Poll định kỳ nếu WS không khả dụng
  pollTimer = setInterval(() => {
    if (!connected.value && activeChat.value) {
      loadMessages(activeChat.value.id);
    }
  }, 15000);
});

let pollTimer = null;
onUnmounted(() => {
  clearInterval(pollTimer);
  if (currentSub) {
    try { currentSub.sub1?.unsubscribe(); } catch {}
    try { currentSub.sub2?.unsubscribe(); } catch {}
  }
  disconnect();
});

defineExpose({ unreadCount });
</script>

<template>
  <div class="admin-chat-panel">
    <!-- ── Sidebar: danh sách cuộc trò chuyện ── -->
    <div class="chat-sidebar">
      <!-- Tabs -->
      <div class="chat-tabs">
        <button
          class="chat-tab"
          :class="{ active: activeTab === 'ANONYMOUS' }"
          @click="activeTab = 'ANONYMOUS'"
        >
          Khách lẻ
        </button>
        <button
          class="chat-tab"
          :class="{ active: activeTab === 'HE_THONG' }"
          @click="activeTab = 'HE_THONG'"
        >
          Khách hàng
        </button>
      </div>

      <!-- List -->
      <div class="chat-list">
        <div v-if="loading && conversations.length === 0" class="chat-list-loading">
          <span class="fa fa-spinner fa-spin"></span>
        </div>
        <div v-else-if="conversations.length === 0" class="chat-list-empty">
          <span class="fa fa-comments" style="font-size:2rem;color:#d1d5db;"></span>
          <p>Chưa có cuộc trò chuyện nào</p>
        </div>
        <div
          v-for="chat in conversations"
          :key="chat.id"
          class="chat-list-item"
          :class="{
            active: activeChat?.id === chat.id,
            unread: (unreadMap[chat.id] || 0) > 0,
            escalated: chat.soLanEscalate > 0,
          }"
          @click="selectChat(chat)"
        >
          <div class="chat-item-avatar">
            {{ chat.loaiKhach === 'HE_THONG' ? '👤' : '🕵️' }}
          </div>
          <div class="chat-item-body">
            <div class="chat-item-top">
              <span class="chat-item-name">{{ chat.hoTenKhach }}</span>
              <span
                class="chat-item-badge"
                :style="{ background: formatTrangThai(chat.trangThai).color }"
              >
                {{ formatTrangThai(chat.trangThai).label }}
              </span>
            </div>
            <div class="chat-item-sub">
              <span class="chat-item-preview">{{ chat.tinNhanCuoi || "..." }}</span>
              <span v-if="(unreadMap[chat.id] || 0) > 0" class="chat-unread-badge">
                {{ unreadMap[chat.id] }}
              </span>
            </div>
          </div>
          <div v-if="chat.soLanEscalate > 0" class="chat-escalate-flag" title="Khách yêu cầu nhân viên">
            🚨
          </div>
        </div>
        <div v-if="loading && conversations.length > 0" class="chat-list-more">
          <span class="fa fa-spinner fa-spin"></span>
        </div>
      </div>
    </div>

    <!-- ── Main chat area ── -->
    <div class="chat-main">
      <template v-if="activeChat">
        <!-- Chat header -->
        <div class="chat-header">
          <div class="chat-header-info">
            <div class="chat-header-avatar">
              {{ activeChat.loaiKhach === 'HE_THONG' ? '👤' : '🕵️' }}
            </div>
            <div>
              <div class="chat-header-name">{{ activeChat.hoTenKhach }}</div>
              <div class="chat-header-sub">
                <span
                  class="status-pill"
                  :style="{ background: formatTrangThai(activeChat.trangThai).color + '22', color: formatTrangThai(activeChat.trangThai).color }"
                >
                  {{ formatTrangThai(activeChat.trangThai).label }}
                </span>
                <span v-if="activeChat.nhanVienPhuTrachTen" style="font-size:11px;color:#6b7280;">
                  • NV: {{ activeChat.nhanVienPhuTrachTen }}
                </span>
              </div>
            </div>
          </div>
          <div class="chat-header-actions">
            <!-- Chưa nhận tiếp -->
            <template v-if="!activeChat.nhanVienPhuTrachId && activeChat.trangThai === 'CHAT_NHAN_VIEN'">
              <button class="btn btn-primary btn-sm" @click="nhanTiep">
                <span class="fa fa-headset"></span> Nhận tiếp
              </button>
            </template>

            <!-- Đã nhận tiếp hoặc đang chat -->
            <template v-if="activeChat.nhanVienPhuTrachId && activeChat.trangThai === 'CHAT_NHAN_VIEN'">
              <button class="btn btn-outline-secondary btn-sm" @click="huyNhanTiep">
                <span class="fa fa-robot"></span> Chuyển về AI
              </button>
            </template>

            <button
              v-if="activeChat.trangThai !== 'DA_DONG'"
              class="btn btn-outline-danger btn-sm"
              @click="dongChat"
            >
              <span class="fa fa-times"></span> Đóng
            </button>
          </div>
        </div>

        <!-- Messages -->
        <div ref="chatBodyRef" class="chat-messages">
          <div v-if="loadingMessages" class="chat-loading-center">
            <span class="fa fa-spinner fa-spin"></span>
          </div>
          <div
            v-for="m in messages"
            :key="m.id"
            class="chat-msg"
            :class="{ right: m.isMe || m.isStaff }"
          >
            <div
              v-if="!m.isMe"
              class="chat-msg-avatar"
              :style="{ background: m.avatarBg }"
            >{{ m.avatarIcon }}</div>
            <div class="chat-msg-bubble" :class="{ 'bubble-bot': m.isBot, 'bubble-staff': m.isStaff }">
              <div v-if="!m.isMe" class="chat-msg-sender">{{ m.tenNguoiGui || 'Khách' }}</div>
              <div class="chat-msg-text">{{ m.noiDung }}</div>
              <div class="chat-msg-time">{{ m.time }}</div>
            </div>
          </div>
        </div>

        <!-- Composer -->
        <div v-if="activeChat.trangThai !== 'DA_DONG'" class="chat-composer">
          <textarea
            v-model="input"
            rows="1"
            placeholder="Nhập tin nhắn..."
            :disabled="loading"
            @keyup.enter.exact.prevent="guiTinNhan"
          ></textarea>
          <button
            class="btn-send"
            :disabled="!input.trim() || loading"
            @click="guiTinNhan"
          >
            <span class="fa" :class="loading ? 'fa-spinner fa-spin' : 'fa-paper-plane'"></span>
          </button>
        </div>
        <div v-else class="chat-composer-disabled">
          Cuộc trò chuyện đã đóng
        </div>
      </template>

      <!-- Empty state -->
      <div v-else class="chat-empty">
        <span class="fa fa-comments" style="font-size:3rem;color:#d1d5db;"></span>
        <p>Chọn một cuộc trò chuyện để xem</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-chat-panel {
  display: flex;
  height: calc(100vh - 120px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  background: var(--bg-card);
}

/* ── Sidebar ── */
.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  background: var(--bg-card-inset);
}

.chat-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
}
.chat-tab {
  flex: 1;
  padding: 10px 8px;
  border: none;
  background: transparent;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.15s;
}
.chat-tab.active {
  color: var(--pink-500, #db2777);
  border-bottom-color: var(--pink-500, #db2777);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}
.chat-list-loading,
.chat-list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 16px;
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
}
.chat-list-more {
  display: flex;
  justify-content: center;
  padding: 8px;
  color: var(--text-muted);
}

.chat-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color-soft, #f0f0f0);
  transition: background 0.1s;
  position: relative;
}
.chat-list-item:hover { background: var(--bg-hover); }
.chat-list-item.active { background: rgba(219, 39, 119, 0.08); }
.chat-list-item.unread { background: rgba(219, 39, 119, 0.04); }

.chat-item-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.chat-item-body { flex: 1; min-width: 0; }
.chat-item-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 3px;
}
.chat-item-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chat-item-badge {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  color: #fff;
  flex-shrink: 0;
}
.chat-item-sub {
  display: flex;
  align-items: center;
  gap: 6px;
}
.chat-item-preview {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.chat-unread-badge {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--pink-500, #db2777);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chat-escalate-flag {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 12px;
}

/* ── Main ── */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
  gap: 12px;
}
.chat-header-info { display: flex; align-items: center; gap: 10px; }
.chat-header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.chat-header-name { font-size: 14px; font-weight: 700; }
.chat-header-sub { display: flex; align-items: center; gap: 6px; margin-top: 2px; }
.chat-header-actions { display: flex; gap: 6px; flex-shrink: 0; }

.status-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 600;
}

.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 6px; }
.btn-outline-danger { border: 1px solid #ef4444; color: #ef4444; background: transparent; cursor: pointer; }
.btn-outline-danger:hover { background: #fef2f2; }
.btn-outline-secondary { border: 1px solid #d1d5db; color: #6b7280; background: transparent; cursor: pointer; }
.btn-outline-secondary:hover { background: #f9fafb; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--pink-50, #fff5f9);
}
.chat-loading-center {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: var(--text-muted);
}

.chat-msg {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.chat-msg.right {
  flex-direction: row-reverse;
}
.chat-msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}
.chat-msg-bubble {
  max-width: 65%;
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  border-radius: 12px;
  padding: 8px 12px;
}
.chat-msg-bubble.bubble-bot {
  background: linear-gradient(135deg, #f3e8ff, #ede9fe);
  border-color: #c4b5fd;
}
.chat-msg-bubble.bubble-staff {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-color: #fed7aa;
}
.chat-msg.right .chat-msg-bubble {
  background: linear-gradient(135deg, var(--pink-400, #ec4899), var(--pink-600, #db2777));
  color: #fff;
  border-color: transparent;
}
.chat-msg.right .chat-msg-time { color: rgba(255,255,255,0.8); }
.chat-msg-sender {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 2px;
}
.chat-msg.right .chat-msg-sender { color: rgba(255,255,255,0.8); }
.chat-msg-text {
  font-size: 13px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-msg-time {
  font-size: 10px;
  color: var(--text-muted);
  text-align: right;
  margin-top: 4px;
}

.chat-composer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background: #fff;
  align-items: flex-end;
}
.chat-composer-disabled {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background: #f9fafb;
  color: var(--text-muted);
  font-size: 13px;
}
.chat-composer textarea {
  flex: 1;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  font-family: inherit;
  resize: none;
  max-height: 100px;
  background: var(--bg-input);
  color: var(--text-primary);
}
.chat-composer textarea:focus { outline: none; border-color: var(--pink-400, #ec4899); }
.btn-send {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 8px;
  background: var(--pink-500, #db2777);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.12s;
}
.btn-send:hover:not(:disabled) { background: var(--pink-600, #a81b5d); }
.btn-send:disabled { background: #d1d5db; cursor: not-allowed; }

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
}
</style>
