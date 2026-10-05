// Quản lý kết nối STOMP WebSocket cho tính năng chat
import { ref, onUnmounted } from "vue";

export function useChatWebSocket() {
  const connected = ref(false);
  const stompConnected = ref(false);

  // Per-instance state (not module-level, so multiple components don't share one socket)
  let socket = null;
  let reconnectTimer = null;

  // Saved callbacks + subscribed chat IDs for re-subscription after reconnect
  let savedOnMessage = null;
  let savedOnStatusChange = null;
  let savedOnStaffNotification = null;
  let subscribedChatIds = []; // [{ chatId, onMessage, onStatusChange }]

  const getWsUrl = () => {
    const rawUrl = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || "";
    if (rawUrl && rawUrl.startsWith("http")) {
      return rawUrl.replace(/\/api\/?$/, "").replace(/^http/, "ws") + "/ws/chat";
    }
    const proto = typeof window !== "undefined" && window.location.protocol === "https:" ? "wss:" : "ws:";
    const host = typeof window !== "undefined" ? window.location.host : "localhost:8080";
    return `${proto}//${host}/ws/chat`;
  };

  const WS_URL = getWsUrl();

  function connect(onMessage, onStatusChange, onStaffNotification) {
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      if (onMessage) savedOnMessage = onMessage;
      if (onStatusChange) savedOnStatusChange = onStatusChange;
      if (onStaffNotification) savedOnStaffNotification = onStaffNotification;
      return;
    }

    // Save callbacks for reconnect
    if (onMessage) savedOnMessage = onMessage;
    if (onStatusChange) savedOnStatusChange = onStatusChange;
    if (onStaffNotification) savedOnStaffNotification = onStaffNotification;

    try {
      socket = new WebSocket(WS_URL);
    } catch (err) {
      console.error("[ChatWS] WebSocket create error:", err);
      scheduleReconnect();
      return;
    }

    socket.onopen = () => {
      console.log("[ChatWS] WebSocket transport opened to", WS_URL);
      // STOMP 1.1 / 1.2 CONNECT frame
      sendFrame("CONNECT", {
        "accept-version": "1.1,1.2",
        "heart-beat": "10000,10000",
      });
    };

    socket.onmessage = (event) => {
      const frame = parseFrame(event.data);
      if (!frame) return;

      const { command, headers, body } = frame;

      if (command === "CONNECTED") {
        connected.value = true;
        stompConnected.value = true;
        console.log("[ChatWS] STOMP connected, session:", headers["session"]);

        // Re-subscribe to all previously subscribed topics after connect / reconnect
        for (const sub of subscribedChatIds) {
          sendSubscribeStomp(`/topic/chat/${sub.chatId}`);
          sendSubscribeStomp(`/topic/chat/${sub.chatId}/status`);
        }
        if (savedOnStaffNotification) {
          sendSubscribeStomp("/topic/staff/notifications");
        }
      } else if (command === "MESSAGE") {
        const dest = headers["destination"];
        if (dest && dest.includes("/topic/chat/") && !dest.includes("/status")) {
          try {
            const data = JSON.parse(body);
            // Route to the correct subscriber's callback
            const chatId = dest.replace("/topic/chat/", "");
            const sub = subscribedChatIds.find((s) => String(s.chatId) === String(chatId));
            if (sub && sub.onMessage) sub.onMessage(data);
            else if (savedOnMessage) savedOnMessage(data);
          } catch (e) {
            console.error("[ChatWS] Error parsing message:", e);
          }
        } else if (dest && dest.includes("/status")) {
          try {
            const data = JSON.parse(body);
            const chatIdStr = dest.replace("/topic/chat/", "").replace("/status", "");
            const sub = subscribedChatIds.find((s) => String(s.chatId) === String(chatIdStr));
            if (sub && sub.onStatusChange) sub.onStatusChange(data);
            else if (savedOnStatusChange) savedOnStatusChange(data);
          } catch (e) {
            console.error("[ChatWS] Error parsing status:", e);
          }
        } else if (dest === "/topic/staff/notifications" || dest === "/topic/staff/conversations") {
          try {
            const data = JSON.parse(body);
            if (savedOnStaffNotification) savedOnStaffNotification(data);
          } catch (e) {
            console.error("[ChatWS] Error parsing staff notification:", e);
          }
        }
      }
    };

    socket.onerror = (err) => {
      console.error("[ChatWS] WebSocket error:", err);
    };

    socket.onclose = (event) => {
      connected.value = false;
      stompConnected.value = false;
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
    if (typeof data !== "string") return null;
    const nullIndex = data.indexOf("\x00");
    const content = nullIndex >= 0 ? data.substring(0, nullIndex) : data;
    const lines = content.replace(/\r\n/g, "\n").split("\n");
    if (lines.length < 1) return null;

    const command = lines[0].trim();
    if (!command) return null;
    const headers = {};
    let body = "";

    let i = 1;
    for (; i < lines.length; i++) {
      const line = lines[i];
      if (line === "") {
        i++;
        break;
      }
      const colonIdx = line.indexOf(":");
      if (colonIdx > 0) {
        headers[line.substring(0, colonIdx).trim()] = line.substring(colonIdx + 1).trim();
      }
    }

    if (i <= lines.length) {
      body = lines.slice(i).join("\n");
    }

    return { command, headers, body };
  }

  function sendSubscribeStomp(destination) {
    const subId = "sub-" + Math.random().toString(36).substr(2, 9);
    sendFrame("SUBSCRIBE", { id: subId, destination });
    return subId;
  }

  function subscribeChat(chatId, onMessage, onStatusChange) {
    if (!chatId) return { sub1: null, sub2: null };

    // Track subscription for reconnect
    const existingIndex = subscribedChatIds.findIndex((s) => String(s.chatId) === String(chatId));
    if (existingIndex >= 0) {
      subscribedChatIds[existingIndex] = { chatId, onMessage, onStatusChange };
    } else {
      subscribedChatIds.push({ chatId, onMessage, onStatusChange });
    }

    if (stompConnected.value) {
      const sub1 = sendSubscribeStomp(`/topic/chat/${chatId}`);
      const sub2 = sendSubscribeStomp(`/topic/chat/${chatId}/status`);
      return { sub1, sub2 };
    }
    return { sub1: null, sub2: null };
  }

  function subscribeStaffNotifications(onNotification) {
    savedOnStaffNotification = onNotification;
    if (stompConnected.value) {
      return sendSubscribeStomp("/topic/staff/notifications");
    }
    return null;
  }

  function subscribeConversationsList(onUpdate) {
    savedOnStaffNotification = onUpdate;
    if (stompConnected.value) {
      return sendSubscribeStomp("/topic/staff/conversations");
    }
    return null;
  }

  function unsubscribeAll() {
    subscribedChatIds = [];
  }

  function disconnect() {
    clearTimeout(reconnectTimer);
    unsubscribeAll();
    if (socket) {
      try { socket.close(1000, "Client disconnect"); } catch {}
      socket = null;
    }
    connected.value = false;
    stompConnected.value = false;
  }

  function scheduleReconnect() {
    clearTimeout(reconnectTimer);
    reconnectTimer = setTimeout(() => {
      console.log("[ChatWS] Attempting reconnect...");
      connect(); // reuses saved callbacks
    }, 4000);
  }

  onUnmounted(() => {
    disconnect();
  });

  return {
    connected,
    stompConnected,
    connect,
    disconnect,
    subscribeChat,
    subscribeStaffNotifications,
    subscribeConversationsList,
    unsubscribeAll,
  };
}
