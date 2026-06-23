package com.fairpilot.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 멱등성 처리 — pg_tx_id로 중복 webhook 방어 */
    Optional<Payment> findByPgTxId(String pgTxId);

    /** 예약별 활성 결제 조회 */
    Optional<Payment> findByReservationIdAndStatusIn(
            Long reservationId, java.util.List<PaymentStatus> statuses);
}