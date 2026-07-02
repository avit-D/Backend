package com.fairpilot.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 결제 요청 발급 DTO
 * 클라이언트가 reservationId + pgProvider + amount 를 함께 전송
 * 서버는 orderId 생성 + READY Payment 레코드 저장 후 반환
 *
 * amount를 클라이언트에서 받는 이유:
 *   domain-payment는 domain-exhibition에 의존하지 않아
 *   TicketType.price를 직접 조회할 수 없음.
 *   실제 결제 금액은 webhook(DONE/PAID) 수신 시 PG사 응답값으로 2차 검증됨.
 */
public record PaymentInitiateRequest(

        @NotNull(message = "reservationId는 필수입니다.")
        Long reservationId,

        @NotNull(message = "exhibitionId는 필수입니다.")
        Long exhibitionId,

        @NotNull(message = "amount는 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = true, message = "amount는 0 이상이어야 합니다.")
        BigDecimal amount,

        /**
         * "TOSS" | "PORTONE"
         * 그 외 값 입력 시 서비스에서 BusinessException 처리
         */
        @NotBlank(message = "pgProvider는 필수입니다.")
        String pgProvider
) {}