<script setup>
import { ref, nextTick, onMounted } from "vue";
import * as AdminAiChatService from "../../services/AdminAiChatService.js";

const open = ref(false);
const messages = ref([]);
const input = ref("");
const chatBodyRef = ref(null);
const thinking = ref(false);
const chatHistory = ref([]);

function renderMarkdown(text) {
  if (!text) return "";
  let out = text;

  // Render Markdown tables
  const tableRegex = /((?:\|.+?\|\r?\n)+)/g;
  out = out.replace(tableRegex, (match) => {
    const lines = match.trim().split(/\r?\n/).filter(l => l.includes("|"));
    if (lines.length < 2) return match;
    let html = '<div class="ai-table-wrap"><table>';
    let isHeader = true;
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (/^\|[\s\-:|]+\|$/.test(line)) {
        isHeader = false;
        continue;
      }
      const cells = line.split("|").slice(1, -1).map(c => c.trim());
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
  });

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

async function init() {
  if (messages.value.length > 0) return;
  try {
    const data = await AdminAiChatService.getWelcomeMessage();
    if (data?.message) {
      pushMessage("assistant", data.message);
      return;
    }
  } catch (err) {
    console.warn("Chưa tải được lời chào từ server AI:", err);
  }
  pushMessage(
    "assistant",
    "👑 Xin chào Admin! Tôi là AI Analytics Assistant của SAOClub.\n\nHãy hỏi tôi về doanh thu, đơn hàng, khách hàng, tồn kho..."
  );
}

async function send() {
  const text = input.value.trim();
  if (!text || thinking.value) return;
  input.value = "";
  pushMessage("user", text);
  thinking.value = true;
  scrollToBottom();
  chatHistory.value.push({ role: "user", content: text });
  try {
    const data = await AdminAiChatService.sendAdminAiChat(
      text,
      chatHistory.value.slice(-16)
    );
    const reply = data?.reply || "Xin lỗi, tôi không thể xử lý câu hỏi này lúc này.";
    pushMessage("assistant", reply);
    chatHistory.value.push({ role: "assistant", content: reply });
    if (chatHistory.value.length > 40) chatHistory.value = chatHistory.value.slice(-40);
  } catch (err) {
    console.error("Lỗi khi chat với AI:", err);
    pushMessage("assistant", "Lỗi kết nối hoặc AI đang xử lý dữ liệu. Vui lòng thử lại sau.");
  } finally {
    thinking.value = false;
    nextTick(() => scrollToBottom());
  }
}

function pushMessage(role, content) {
  messages.value.push({
    id: Date.now() + Math.random(),
    role,
    content,
    time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
  });
  nextTick(() => scrollToBottom());
}

function scrollToBottom() {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
}

function handleKeydown(e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    send();
  }
}

function clearChat() {
  messages.value = [];
  chatHistory.value = [];
  init();
}

const quickPrompts = [
  "Doanh thu hôm nay",
  "Thống kê đơn hàng",
  "Top sản phẩm bán chạy",
  "Sản phẩm sắp hết hàng",
  "Tổng số khách hàng",
  "Tổng quan hệ thống",
];

function useQuickPrompt(p) {
  input.value = p;
  send();
}

onMounted(() => init());
</script>

<template>
  <div class="admin-ai-widget">
    <button class="admin-ai-fab" :class="{ open }" title="AI Analytics (Admin Only)" @click="open = !open">
      <span v-if="!open">🤖<br><small>AI</small></span>
      <span v-else>✕</span>
      <span v-if="!open" class="fab-crown">👑</span>
    </button>
    <Transition name="slide-up">
      <div v-if="open" class="admin-ai-panel">
        <div class="panel-header">
          <div class="header-left">
            <div class="header-avatar">🤖</div>
            <div>
              <div class="header-title">AI Analytics</div>
              <div class="header-sub"><span class="status-dot"></span> Admin Only</div>
            </div>
          </div>
          <div class="header-actions">
            <button class="btn-icon" title="Xoa lich su" @click="clearChat">🗑</button>
            <button class="btn-icon" @click="open = false">✕</button>
          </div>
        </div>
        <div v-if="messages.length <= 1" class="quick-prompts">
          <div class="qp-label">Gợi ý câu hỏi:</div>
          <div class="qp-list">
            <button v-for="p in quickPrompts" :key="p" class="qp-btn" @click="useQuickPrompt(p)">{{ p }}</button>
          </div>
        </div>
        <div ref="chatBodyRef" class="panel-body">
          <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ 'msg-user': msg.role === 'user', 'msg-ai': msg.role === 'assistant' }">
            <div v-if="msg.role === 'assistant'" class="msg-avatar">🤖</div>
            <div class="msg-content">
              <div class="msg-bubble" :class="{ 'bubble-ai': msg.role === 'assistant', 'bubble-user': msg.role === 'user' }">
                <span class="msg-text" v-html="renderMarkdown(msg.content)"></span>
              </div>
              <div class="msg-time">{{ msg.time }}</div>
            </div>
          </div>
          <div v-if="thinking" class="msg-row msg-ai">
            <div class="msg-avatar">🤖</div>
            <div class="msg-content">
              <div class="msg-bubble bubble-ai thinking-bubble">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                <span class="thinking-label">Đang phân tích dữ liệu...</span>
              </div>
            </div>
          </div>
        </div>
        <div class="panel-composer">
          <textarea v-model="input" placeholder="Hỏi về doanh thu, đơn hàng, tồn kho..." :disabled="thinking" rows="1" @keydown="handleKeydown"></textarea>
          <button class="btn-send" :disabled="!input.trim() || thinking" @click="send">
            <span v-if="!thinking">➤</span><span v-else class="spinner"></span>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.admin-ai-widget { position: fixed; right: 24px; bottom: 24px; z-index: 998; }
.admin-ai-fab {
  position: relative; width: 56px; height: 56px; border-radius: 50%; border: none;
  background: linear-gradient(145deg, #7c3aed, #4f46e5); color: #fff; font-size: 11px; font-weight: 800;
  cursor: pointer; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px;
  box-shadow: 0 4px 0 #3730a3, 0 6px 20px rgba(79,70,229,0.45); border-bottom: 4px solid #3730a3;
  transition: all 0.15s; line-height: 1.2;
}
.admin-ai-fab:hover { transform: translateY(-2px); box-shadow: 0 6px 0 #3730a3, 0 10px 28px rgba(79,70,229,0.5); }
.admin-ai-fab:active { transform: translateY(2px); box-shadow: inset 0 2px 4px rgba(0,0,0,0.25); border-bottom-width: 0; }
.admin-ai-fab.open { background: linear-gradient(145deg, #6b7280, #4b5563); box-shadow: 0 4px 0 #374151; border-bottom-color: #374151; }
.fab-crown { position: absolute; top: 1px; right: 1px; font-size: 11px; }
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.25s cubic-bezier(0.175,0.885,0.32,1.275); }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(14px) scale(0.97); }
.admin-ai-panel {
  position: absolute; right: 0; bottom: 70px; width: 360px; height: 520px;
  max-height: calc(100vh - 120px); background: var(--bg-card, #fff);
  border: 1px solid rgba(124,58,237,0.2); border-radius: 16px;
  box-shadow: 0 12px 48px rgba(79,70,229,0.22), 0 4px 16px rgba(0,0,0,0.08);
  display: flex; flex-direction: column; overflow: hidden;
}
.panel-header {
  display: flex; align-items: center; justify-content: space-between; padding: 12px 14px;
  background: linear-gradient(135deg, #7c3aed, #4f46e5); color: #fff; flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-avatar { width: 36px; height: 36px; border-radius: 50%; background: rgba(255,255,255,0.2); border: 2px solid rgba(255,255,255,0.4); display: flex; align-items: center; justify-content: center; font-size: 18px; }
.header-title { font-size: 14px; font-weight: 700; }
.header-sub { font-size: 11px; opacity: 0.85; display: flex; align-items: center; gap: 4px; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #4ade80; animation: pulse-dot 2s infinite; }
@keyframes pulse-dot { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
.header-actions { display: flex; gap: 4px; }
.btn-icon { width: 28px; height: 28px; border: none; background: rgba(255,255,255,0.15); color: #fff; border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 12px; transition: background 0.15s; }
.btn-icon:hover { background: rgba(255,255,255,0.25); }
.quick-prompts { padding: 8px 12px 6px; background: rgba(124,58,237,0.04); border-bottom: 1px solid rgba(124,58,237,0.1); flex-shrink: 0; }
.qp-label { font-size: 10px; font-weight: 600; color: #7c3aed; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 5px; }
.qp-list { display: flex; flex-wrap: wrap; gap: 4px; }
.qp-btn { padding: 3px 8px; background: rgba(124,58,237,0.08); border: 1px solid rgba(124,58,237,0.2); border-radius: 20px; color: #7c3aed; font-size: 11px; cursor: pointer; transition: all 0.15s; white-space: nowrap; }
.qp-btn:hover { background: rgba(124,58,237,0.15); border-color: #7c3aed; }
.panel-body { flex: 1; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 10px; background: #faf9ff; }
.msg-row { display: flex; align-items: flex-end; gap: 7px; }
.msg-user { flex-direction: row-reverse; }
.msg-avatar { width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg,#ede9fe,#ddd6fe); display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; border: 1px solid #c4b5fd; }
.msg-content { max-width: 80%; display: flex; flex-direction: column; gap: 2px; }
.msg-user .msg-content { align-items: flex-end; }
.msg-bubble { padding: 9px 13px; border-radius: 14px; font-size: 13px; line-height: 1.5; max-width: 100%; word-break: break-word; }
.bubble-ai { background: linear-gradient(135deg,#f3e8ff,#ede9fe); border: 1px solid #c4b5fd; color: #3b0764; }
.bubble-user { background: linear-gradient(135deg,#4f46e5,#7c3aed); color: #fff; }
.msg-text { display: block; white-space: pre-wrap; }
.msg-text :deep(strong) { font-weight: 700; }
.msg-text :deep(code) { background: rgba(0,0,0,0.08); padding: 1px 4px; border-radius: 3px; font-size: 12px; font-family: monospace; }
.msg-text :deep(h2), .msg-text :deep(h3), .msg-text :deep(h4) { margin: 4px 0 2px; font-weight: 700; color: #4f46e5; }
.msg-text :deep(li) { margin-left: 14px; margin-bottom: 2px; }
.msg-text :deep(.ai-table-wrap) { width: 100%; overflow-x: auto; margin: 6px 0; border-radius: 8px; border: 1px solid rgba(124,58,237,0.25); }
.msg-text :deep(table) { width: 100%; border-collapse: collapse; font-size: 11px; background: #fff; }
.msg-text :deep(th) { background: #7c3aed; color: #fff; padding: 5px 7px; font-weight: 600; text-align: left; white-space: nowrap; }
.msg-text :deep(td) { padding: 4px 7px; border-bottom: 1px solid #ede9fe; color: #374151; white-space: nowrap; }
.msg-text :deep(tr:nth-child(even) td) { background: #faf9ff; }
.msg-time { font-size: 10px; color: #9ca3af; padding: 0 4px; }
.thinking-bubble { display: flex; align-items: center; gap: 5px; padding: 10px 14px !important; }
.dot { width: 7px; height: 7px; background: #7c3aed; border-radius: 50%; animation: bounce-dot 1.2s infinite ease-in-out; flex-shrink: 0; }
.dot:nth-child(1) { animation-delay: 0s; } .dot:nth-child(2) { animation-delay: 0.2s; } .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce-dot { 0%,60%,100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-6px); opacity: 1; } }
.thinking-label { font-size: 11px; color: #7c3aed; font-style: italic; white-space: nowrap; }
.panel-composer { display: flex; gap: 8px; padding: 10px 12px; background: var(--bg-card,#fff); border-top: 1px solid rgba(124,58,237,0.15); align-items: flex-end; flex-shrink: 0; }
.panel-composer textarea { flex: 1; border: 1px solid rgba(124,58,237,0.25); border-radius: 12px; background: #faf9ff; color: var(--text-primary,#111); padding: 8px 12px; font-size: 13px; resize: none; font-family: inherit; line-height: 1.4; max-height: 80px; transition: border-color 0.15s; }
.panel-composer textarea:focus { outline: none; border-color: #7c3aed; box-shadow: 0 0 0 3px rgba(124,58,237,0.1); }
.panel-composer textarea:disabled { background: #f9fafb; cursor: not-allowed; }
.btn-send { width: 38px; height: 38px; border: none; border-radius: 12px; background: linear-gradient(145deg,#7c3aed,#4f46e5); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 16px; box-shadow: 0 2px 0 #3730a3,0 3px 8px rgba(79,70,229,0.35); border-bottom: 2px solid #3730a3; transition: all 0.12s; flex-shrink: 0; }
.btn-send:hover:not(:disabled) { transform: translateY(-1px); }
.btn-send:active:not(:disabled) { transform: translateY(1px); border-bottom-width: 0; }
.btn-send:disabled { background: #d1d5db; box-shadow: none; border-bottom-color: #9ca3af; cursor: not-allowed; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.4); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
