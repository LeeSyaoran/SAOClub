package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Timeout huu han rat lon thay vi 0L (0 = "khong timeout" theo spec, nhung mot so
        // container/proxy xu ly gia tri 0 khong nhat quan) — an toan hon, van coi nhu vo han
        // trong thuc te (~24h), va se duoc gia han khi emitter con song nho onCompletion/onTimeout.
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        try {
            // Gui ngay 1 event "connected" de force flush header/response ngay khi client
            // subscribe, thay vi doi den lan notifyNewOrder() dau tien moi co byte nao duoc gui.
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void notifyNewOrder(Integer orderId) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("new-order").data(orderId));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
