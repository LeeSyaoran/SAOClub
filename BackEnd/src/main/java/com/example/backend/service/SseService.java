package com.example.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @Async
    public void notifyNewOrder(Integer orderId) {
        broadcast("new-order", orderId);
    }

    @Async
    public void notifyOrderUpdate(Integer orderId) {
        broadcast("order-updated", orderId);
    }

    // Broadcast serial lock event — chi gui data nho (chiTietId + lockedByTen)
    @Async
    public void notifySerialLocked(Integer chiTietId, String soSerial, Integer lockedBy, String lockedByTen) {
        Map<String, Object> payload = Map.of(
            "chiTietId", chiTietId,
            "soSerial", soSerial != null ? soSerial : "",
            "lockedBy", lockedBy != null ? lockedBy : 0,
            "lockedByTen", lockedByTen != null ? lockedByTen : ""
        );
        broadcast("serial-locked", payload);
    }

    // Broadcast serial unlock event
    @Async
    public void notifySerialUnlocked(Integer chiTietId, String soSerial) {
        Map<String, Object> payload = Map.of(
            "chiTietId", chiTietId,
            "soSerial", soSerial != null ? soSerial : ""
        );
        broadcast("serial-unlocked", payload);
    }

    private void broadcast(String eventName, Object data) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
