// Quản lý kết nối STOMP WebSocket cho tính năng chat
import { ref, onUnmounted } from "vue";

let socket = null;
let reconnectTimer = null;

export function useChatWebSocket() {
  const connected = ref(false);
  const subscriptions = ref([]);

  const rawUrl = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  // Resolve relative paths (e.g. "/api") against current origin
  let baseUrl = rawUrl;
  if (!rawUrl.match(/^https?:\/\//)) {
    baseUrl = window.location.origin + (rawUrl.startsWith("/") ? rawUrl : "/" + rawUrl);
  }

  // Build WebSocket URL — strip /api suffix, convert http→ws
  const wsBase = baseUrl.replace(/\/api\/?$/, "");
  const WS_URL = wsBase.replace(/^http/, "ws") + "/ws/chat";

  function connect(onMessage, onStatusChange, onStaffNotification) {
    if (socket && socket.readyState === WebSocket.OPEN) {
      console.warn("[ChatWS] Already connected");
      return;
    }

    try {
      socket = new WebSocket(WS_URL);
    } catch (err) {
      console.error("[ChatWS] WebSocket create error:", err);
      scheduleReconnect();
      return;
    }

    socket.onopen = () => {
      connected.value = true;
      console.log("[ChatWS] Connected to", WS_URL);

      // STOMP CONNECT frame
      sendFrame("CONNECT", { accept: "application/json, text/plain, */*" });
    };

    socket.onmessage = (event) => {
      const frame = parseFrame(event.data);
      if (!frame) return;

      const { command, headers, body } = frame;

      if (command === "CONNECTED") {
        console.log("[ChatWS] STOMP connected, session:", headers["session"]);
      } else if (command === "MESSAGE") {
        const dest = headers["destination"];
        if (dest && dest.includes("/topic/chat/")) {
          try {
            const data = JSON.parse(body);
            if (onMessage) onMessage(data);
          } catch {}
        } else if (dest && dest.includes("/status")) {
          try {
            const data = JSON.parse(body);
            if (onStatusChange) onStatusChange(data);
          } catch {}
        } else if (dest === "/topic/staff/notifications") {
          try {
            const data = JSON.parse(body);
            if (onStaffNotification) onStaffNotification(data);
          } catch {}
        }
      }
    };

    socket.onerror = (err) => {
      console.error("[ChatWS] WebSocket error:", err);
    };

    socket.onclose = (event) => {
      connected.value = false;
      console.log("[ChatWS] Disconnected, code:", event.code);
      if (!event.wasClean) {
        scheduleReconnect();
      }
    };
  }

  function sendFrame(command, headers = {}, body = "") {
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    let frame = command + "\n";
    for (const [k, v] of Object.entries(headers)) {
      frame += k + ":" + v + "\n";
    }
    frame += "\n" + body + "\x00";
    socket.send(frame);
  }

  function parseFrame(data) {
    const nullIndex = data.indexOf("\x00");
    const content = nullIndex >= 0 ? data.substring(0, nullIndex) : data;
    const lines = content.split("\n");
    if (lines.length < 1) return null;

    const command = lines[0].trim();
    const headers = {};
    let body = "";

    let i = 1;
    for (; i < lines.length; i++) {
      const line = lines[i];
      if (line === "") break;
      const colonIdx = line.indexOf(":");
      if (colonIdx > 0) {
        headers[line.substring(0, colonIdx)] = line.substring(colonIdx + 1);
      }
    }

    if (i < lines.length) {
      body = lines.slice(i).join("\n");
    }

    return { command, headers, body };
  }

  function subscribeChat(chatId, onMessage, onStatusChange) {
    const sub1 = sendSubscribe(`/topic/chat/${chatId}`);
    const sub2 = sendSubscribe(`/topic/chat/${chatId}/status`);

    // Store subscription info for potential unsubscribe
    subscriptions.value.push({ id: chatId, sub1, sub2 });
    return { sub1, sub2 };
  }

  function sendSubscribe(destination) {
    const subId = "sub-" + Math.random().toString(36).substr(2, 9);
    sendFrame("SUBSCRIBE", {
      id: subId,
      destination,
    });
    return subId;
  }

  function subscribeStaffNotifications(onNotification) {
    const subId = sendSubscribe("/topic/staff/notifications");
    subscriptions.value.push({ id: "staff-notif", subId });
    return subId;
  }

  function subscribeConversationsList(onUpdate) {
    const subId = sendSubscribe("/topic/staff/conversations");
    subscriptions.value.push({ id: "conversations", subId });
    return subId;
  }

  function unsubscribeAll() {
    for (const sub of subscriptions.value) {
      try {
        sendFrame("UNSUBSCRIBE", { id: sub.sub1 || sub.id });
      } catch {}
    }
    subscriptions.value = [];
  }

  function disconnect() {
    clearTimeout(reconnectTimer);
    unsubscribeAll();
    if (socket) {
      try { socket.close(1000, "Client disconnect"); } catch {}
      socket = null;
    }
    connected.value = false;
  }

  function scheduleReconnect() {
    clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(() => {
      console.log("[ChatWS] Attempting reconnect...");
      connect(() => {}, () => {}, () => {});
    }, 5000);
  }

  onUnmounted(() => {
    disconnect();
  });

  return {
    connected,
    connect,
    disconnect,
    subscribeChat,
    subscribeStaffNotifications,
    subscribeConversationsList,
    unsubscribeAll,
  };
}
