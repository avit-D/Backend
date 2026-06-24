package com.fairpilot.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 멱등성 처리 — pg_tx_id로 중복 webhook 방어 */
    Optional<Payment> findByPgTxId(String pgTxId);

    /** 예약별 활성 결제 조회 */
    Optional<Payment> findByReservationIdAndStatusIn(
            Long reservationId, java.util.List<PaymentStatus> statuses);

    /** 온라인 매출 합산 (기간 필터) */
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.exhibitionId = :exhibitionId " +
           "AND p.status = :status " +
           "AND p.pgProvider != :onsiteProvider " +
           "AND p.paidAt BETWEEN :from AND :to")
    BigDecimal sumOnlineAmount(@Param("exhibitionId") Long exhibitionId,
                               @Param("status") PaymentStatus status,
                               @Param("onsiteProvider") String onsiteProvider,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    /** ONSITE 현장 매출 합산 (기간 필터) */
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.exhibitionId = :exhibitionId " +
           "AND p.status = :status " +
           "AND p.pgProvider = :onsiteProvider " +
           "AND p.paidAt BETWEEN :from AND :to")
    BigDecimal sumOnsiteAmount(@Param("exhibitionId") Long exhibitionId,
                               @Param("status") PaymentStatus status,
                               @Param("onsiteProvider") String onsiteProvider,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);
}