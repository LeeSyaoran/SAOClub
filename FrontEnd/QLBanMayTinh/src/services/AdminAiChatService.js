import { get, post } from './api.js';

// Lấy tin nhắn chào mừng từ AI Analytics Assistant
export const getWelcomeMessage = async () => {
  return get('/api/admin-ai/welcome');
};

// Gửi câu hỏi và nhận phân tích số liệu từ AI
export const sendAdminAiChat = async (cauHoi, lichSuChat = []) => {
  const res = await post('/api/admin-ai/chat', { cauHoi, lichSuChat });
  if (!res.ok) {
    const errorText = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status}${errorText ? ': ' + errorText : ''}`);
  }
  return res.json();
};
