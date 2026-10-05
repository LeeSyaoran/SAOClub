import { reactive } from "vue";

// Quản lý sự kiện cập nhật trạng thái serial realtime
export const serialEvents = reactive({ count: 0 });

export const bumpSerialEvent = () => { serialEvents.count++; };

// Kết nối SSE lắng nghe sự kiện thay đổi serial
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

  serialEventSource.addEventListener('new-order', () => {
    bumpSerialEvent();
  });

  serialEventSource.addEventListener('order-updated', () => {
    bumpSerialEvent();
  });
};

export const disconnectSerialEvents = () => {
  serialSubscriberCount = Math.max(0, serialSubscriberCount - 1);
  if (serialSubscriberCount === 0 && serialEventSource) {
    serialEventSource.close();
    serialEventSource = null;
  }
};
