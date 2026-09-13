<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { AuthStore } from "../../stores/index.js";

const auth = AuthStore;
const open = ref(false);
const messages = ref([
  { side: 'left',  name: 'CSKH', text: 'Xin chào! Mình có thể hỗ trợ gì cho bạn?', time: '09:00' },
]);
const input = ref('');
const chatBodyRef = ref(null);

const me = computed(() => auth.user?.hoTen || auth.user?.tenDangNhap || 'Khách');

const send = () => {
  const text = input.value.trim();
  if (!text) return;
  messages.value.push({
    side: 'right',
    name: me.value,
    text,
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  });
  input.value = '';
  // Mock: CSKH tự reply sau 1s
  setTimeout(() => {
    messages.value.push({
      side: 'left',
      name: 'CSKH',
      text: 'Cảm ơn bạn đã liên hệ. Chúng tôi sẽ phản hồi sớm nhất có thể!',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    });
    nextTick(() => scrollToBottom());
  }, 1000);
  nextTick(() => scrollToBottom());
};

const scrollToBottom = () => {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
};

watch(open, (v) => {
  if (v) nextTick(() => scrollToBottom());
});
onMounted(() => {});
onUnmounted(() => {});
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
      <div class="chat-popup-header">
        <div class="chat-popup-avatar">
          <i class="fa fa-headset"></i>
        </div>
        <div class="chat-popup-meta">
          <div class="chat-popup-title">Hỗ trợ trực tuyến</div>
          <div class="chat-popup-sub">CSKH đang online</div>
        </div>
        <button class="chat-popup-close" :aria-label="'Đóng'" @click="open = false">
          <i class="fa fa-chevron-down"></i>
        </button>
      </div>

      <div ref="chatBodyRef" class="chat-popup-body">
        <div v-for="(m, idx) in messages" :key="idx" class="chat-popup-msg" :class="m.side">
          <div class="chat-popup-bubble">
            <span class="chat-popup-bubble-name">{{ m.name }}</span>
            <span class="chat-popup-bubble-text">{{ m.text }}</span>
            <span class="chat-popup-bubble-time">{{ m.time }}</span>
          </div>
        </div>
      </div>

      <div class="chat-popup-composer">
        <textarea
          v-model="input"
          rows="1"
          placeholder="Nhắn tin cho CSKH..."
          @keyup.enter.exact.prevent="send"
        ></textarea>
        <button class="chat-popup-send" :disabled="!input.trim()" @click="send">
          <i class="fa fa-paper-plane"></i>
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
  font-size: 16px;
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
.chat-popup-sub::before {
  content: '';
  width: 6px;
  height: 6px;
  background: #10b981;
  border: 1px solid #fff;
  border-radius: 50%;
  display: inline-block;
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
.chat-popup-msg { display: flex; }
.chat-popup-msg.right { justify-content: flex-end; }
.chat-popup-msg.left  { justify-content: flex-start; }
.chat-popup-bubble {
  max-width: 75%;
  background: #fff;
  border: 1px solid var(--border-color-soft, #f1dbe6);
  padding: 8px 12px;
  border-radius: 14px;
}
.chat-popup-msg.right .chat-popup-bubble {
  background: linear-gradient(135deg, var(--pink-400, #ec4899), var(--pink-600, #db2777));
  color: #fff;
  border-color: transparent;
}
.chat-popup-msg.right .chat-popup-bubble-name,
.chat-popup-msg.right .chat-popup-bubble-time { color: rgba(255,255,255,0.85); }
.chat-popup-bubble-name {
  display: block;
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 2px;
  color: var(--muted, #6b7280);
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
  color: var(--muted, #6b7280);
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

/* ═══ Responsive ═══ */
@media (max-width: 480px) {
  .chat-widget { right: 16px; bottom: 16px; }
  .chat-popup { width: calc(100vw - 32px); height: 70vh; }
}
</style>
