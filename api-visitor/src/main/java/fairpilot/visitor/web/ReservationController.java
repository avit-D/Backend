package fairpilot.visitor.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.reservation.domain.Reservation;
import com.fairpilot.reservation.dto.ReservationResponse;
import com.fairpilot.reservation.dto.ReserveRequest;
import com.fairpilot.reservation.repository.ReservationAttendeeRepository;
import com.fairpilot.reservation.repository.ReservationRepository;
import com.fairpilot.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 동시성 API (개발자 4번, v2.4). 인증/테넌트는 2번 모듈, 여기서는 X-User-Id 로 대체.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final ReservationAttendeeRepository attendeeRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> reserve(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReserveRequest req) {
        Reservation r = reservationService.reserve(userId, req);
        ReservationResponse body = ReservationResponse.of(r, attendeeRepository.findByReservationId(r.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    @GetMapping("/me")
    public ApiResponse<Page<ReservationResponse>> myReservations(@RequestHeader("X-User-Id") Long userId,
                                                                 Pageable pageable) {
        Page<ReservationResponse> page = reservationRepository.findByUserId(userId, pageable)
                .map(r -> ReservationResponse.of(r, attendeeRepository.findByReservationId(r.getId())));
        return ApiResponse.ok(page);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        reservationService.cancel(id, userId);
        return ApiResponse.ok(null);
    }

    /** 참석자 단위 부분 취소(INDIVIDUAL, 체크인 전). */
    @PostMapping("/{id}/attendees/{attendeeId}/cancel")
    public ApiResponse<Void> cancelAttendee(@RequestHeader("X-User-Id") Long userId,
                                            @PathVariable Long id, @PathVariable Long attendeeId) {
        reservationService.cancelAttendee(id, attendeeId, userId);
        return ApiResponse.ok(null);
    }

    /** 결제 webhook(2번/PG) 훅: 임시점유 확정 + PAID. */
    @PostMapping("/{id}/confirm-paid")
    public ApiResponse<Void> confirmPaid(@PathVariable Long id) {
        reservationService.confirmPaid(id);
        return ApiResponse.ok(null);
    }
}
