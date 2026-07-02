package com.fairpilot.reservation.scheduler;

import com.fairpilot.reservation.service.ReservationService;
import com.fairpilot.reservation.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 임시 점유(Hold) 만료 처리 스케줄러.
 * Redis 만료 인덱스(ZSET)에서 만료된 예약을 주기적으로 꺼내 좌석을 자동 반납한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

    private final SeatHoldService seatHoldService;
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${fairpilot.reservation.hold-sweep-ms:5000}")
    public void sweepExpiredHolds() {
        Set<String> expired = seatHoldService.popExpiredHolds(500);
        if (expired == null || expired.isEmpty()) {
            return;
        }
        for (String reservationId : expired) {
            try {
                reservationService.releaseExpired(Long.valueOf(reservationId));
            } catch (Exception e) {
                log.error("failed to release expired hold reservation={}", reservationId, e);
            }
        }
        log.info("hold sweep released {} expired reservations", expired.size());
    }
}
