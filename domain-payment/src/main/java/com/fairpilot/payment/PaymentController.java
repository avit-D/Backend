package com.fairpilot.payment;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    /**
     * 결제 요청 발급
     * 프론트엔드가 reservationId + pgProvider + amount 를 전송
     * 서버는 orderId 생성 + READY Payment 저장 후 반환
     * VISITOR 전용
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('VISITOR')")
    public ApiResponse<PaymentInitiateResponse> initiate(
            @RequestBody @Valid PaymentInitiateRequest req) {
        return ApiResponse.ok(paymentService.initiate(req));
    }

    /**
     * 포트원 V2 webhook 수신
     * SecurityConfig에서 permitAll 처리 — JWT 불필요
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(
            @RequestBody PortOneWebhookPayload payload) {
        log.info("포트원 webhook 수신: type={}, paymentId={}",
                payload.type(), payload.data().paymentId());
        paymentService.handleWebhook(payload);
        return ApiResponse.ok(null);
    }

    /**
     * 토스페이먼츠 webhook 수신
     * SecurityConfig에서 permitAll 처리 — JWT 불필요
     * Authorization: Basic {Base64(secret:)} 헤더 검증으로 위변조 방어
     */
    @PostMapping("/webhook/toss")
    public ApiResponse<Void> tossWebhook(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TossWebhookPayload payload) {

        verifyTossAuthorization(authorization);

        log.info("토스 webhook 수신: eventType={}, paymentKey={}",
                payload.eventType(), payload.data().paymentKey());
        paymentService.handleTossWebhook(payload);
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

    /**
     * 토스 Authorization 헤더 검증
     * [보안] MessageDigest.isEqual() 상수 시간 비교 → 타이밍 어택 방지
     * String.equals()는 첫 불일치 문자에서 즉시 반환하므로
     * 응답 시간 측정으로 secret key를 역추산할 수 있음
     */
    private void verifyTossAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "토스 Authorization 헤더가 없습니다.");
        }
        String expected = "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        boolean matched = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authorization.getBytes(StandardCharsets.UTF_8)
        );

        if (!matched) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "토스 Authorization 헤더가 유효하지 않습니다.");
        }
    }
}