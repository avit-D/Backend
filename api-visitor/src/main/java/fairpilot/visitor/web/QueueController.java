package fairpilot.visitor.web;
// TODO: QueueService/QueueController는 기획안 §4.5.3 미기재. 팀 PM 확인 필요.

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.reservation.domain.TimeSlot;
import com.fairpilot.reservation.dto.QueueStatusResponse;
import com.fairpilot.reservation.repository.TimeSlotRepository;
import com.fairpilot.reservation.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 대기열 API (트래픽 폭증 완화). 활성 슬롯 = 잔여 정원 * buffer.
 */
@RestController
@RequestMapping("/api/time-slots/{slotId}/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final TimeSlotRepository timeSlotRepository;

    @Value("${fairpilot.reservation.queue.active-buffer:1.2}")
    private double activeBuffer;

    @PostMapping
    public ApiResponse<QueueStatusResponse> enter(@RequestHeader("X-User-Id") Long userId,
                                                  @PathVariable Long slotId) {
        var pos = queueService.enter(slotId, userId, activeSlots(slotId));
        return ApiResponse.ok(QueueStatusResponse.of(pos.position(), pos.allowed()));
    }

    @GetMapping("/status")
    public ApiResponse<QueueStatusResponse> status(@RequestHeader("X-User-Id") Long userId,
                                                   @PathVariable Long slotId) {
        var pos = queueService.status(slotId, userId, activeSlots(slotId));
        return ApiResponse.ok(QueueStatusResponse.of(pos.position(), pos.allowed()));
    }

    private int activeSlots(Long slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        int remaining = Math.max(0, slot.getCapacity() - slot.getReservedCount());
        return (int) Math.ceil(remaining * activeBuffer);
    }
}
