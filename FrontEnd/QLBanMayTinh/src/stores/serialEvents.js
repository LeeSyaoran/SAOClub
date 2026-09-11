import { reactive } from "vue";

// Dùng làm event bus: PosPanel gọi bump() khi thay đổi serial,
// SerialManager watch bump.count để reload. Đồng thời nhận SSE events
// từ backend để đồng bộ khi POS tab khác thay đổi serial.
export const serialEvents = reactive({ count: 0 });

export const bumpSerialEvent = () => { serialEvents.count++; };

// SSE connection for serial events — kết nối 1 lần, dùng chung toàn app
let serialEventSource = null;
let serialSubscriberCount = 0;

export const connectSerialEvents = (token) => {
  serialSubscriberCount += 1;
  if (serialEventSource) return;

  document.cookie = `sse_token=${encodeURIComponent(token ?? '')}; path=/api/don-hang; SameSite=Strict`;
  serialEventSource = new EventSource('/api/don-hang/events');

  serialEventSource.onerror = (e) => console.error('[SerialSSE] Lỗi kết nối:', e);

  serialEventSource.addEventListener('serial-locked', (e) => {
    try {
      const data = JSON.parse(e.data);
      console.info('[SerialSSE] Serial locked:', data.soSerial, 'by', data.lockedByTen);
      bumpSerialEvent();
    } catch {}
  });

  serialEventSource.addEventListener('serial-unlocked', (e) => {
    try {
      const data = JSON.parse(e.data);
      console.info('[SerialSSE] Serial unlocked:', data.soSerial);
      bumpSerialEvent();
    } catch {}
  });
};

export const disconnectSerialEvents = () => {
  serialSubscriberCount = Math.max(0, serialSubscriberCount - 1);
  if (serialSubscriberCount === 0 && serialEventSource) {
    serialEventSource.close();
    serialEventSource = null;
  }
};
