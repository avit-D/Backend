package com.fairpilot.tracking.congestion.service;

import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 박람회별 SSE Emitter 레지스트리 + 로컬 브로드캐스트.
 * 같은 인스턴스에 연결된 구독자에게 혼잡 이벤트를 푸시한다.
 * (인스턴스 간 전파는 CongestionMessageRouter 의 Redis Pub/Sub 가 담당)
 */
@Slf4j
@Service
public class CongestionSseService {

    @Value("${fairpilot.congestion.sse-timeout-ms:1800000}")
    private long sseTimeoutMs;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long exhibitionId, List<CongestionEvent> initialSnapshot) {
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        emitters.computeIfAbsent(exhibitionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(exhibitionId, emitter));
        emitter.onTimeout(() -> remove(exhibitionId, emitter));
        emitter.onError(e -> remove(exhibitionId, emitter));

        try {
            emitter.send(SseEmitter.event().name("init").data(initialSnapshot));
        } catch (IOException e) {
            remove(exhibitionId, emitter);
        }
        return emitter;
    }

    /** 로컬 구독자에게 혼잡 이벤트 전송. */
    public void broadcast(CongestionEvent event) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(event.exhibitionId());
        if (list == null) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("congestion")
                        .data(event));
            } catch (IOException e) {
                remove(event.exhibitionId(), emitter);
            }
        }
    }

    /** 연결 유지용 heartbeat. */
    @Scheduled(fixedDelayString = "${fairpilot.congestion.heartbeat-ms:15000}")
    public void heartbeat() {
        emitters.forEach((exhId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException e) {
                    remove(exhId, emitter);
                }
            }
        });
    }

    private void remove(Long exhibitionId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(exhibitionId);
        if (list != null) list.remove(emitter);
    }
}
