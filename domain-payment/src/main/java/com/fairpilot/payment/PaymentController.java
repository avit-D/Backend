package com.fairpilot.payment;

import com.fairpilot.core.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 포트원 V2 webhook 수신
     * 포트원이 결제 결과를 이 엔드포인트로 보내줌
     * 멱등성 처리로 중복 호출 방어
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(
            @RequestBody PortOneWebhookPayload payload) {
        log.info("webhook 수신: type={}, paymentId={}",
                payload.type(), payload.data().paymentId());
        paymentService.handleWebhook(payload);
        return ApiResponse.ok(null);
    }

    /**
     * ONSITE 현장결제 등록
     * EXPO_ADMIN / STAFF 전용
     */
    @PostMapping("/onsite")
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'STAFF')")
    public ApiResponse<Void> onsite(
            @RequestBody @Valid OnsitePaymentRequest req) {
        paymentService.handleOnsite(req);
        return ApiResponse.ok(null);
    }
}