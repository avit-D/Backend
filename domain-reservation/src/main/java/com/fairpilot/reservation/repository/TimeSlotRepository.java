package com.fairpilot.reservation.repository;

import com.fairpilot.reservation.domain.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * v2.4 동시성 핵심: group_size 단위 원자 조건부 UPDATE.
     * reserved_count + amount <= capacity 일 때만 증가 → 초과 예약 원천 방지.
     */
    @Modifying(clearAutomatically = true)
    @Query("update TimeSlot t set t.reservedCount = t.reservedCount + :amount " +
           "where t.id = :id and t.reservedCount + :amount <= t.capacity")
    int increaseReserved(@Param("id") Long id, @Param("amount") int amount);

    /** 취소/만료 시 group_size(또는 부분 취소 인원)만큼 원자 복원. 0 미만 방지. */
    @Modifying(clearAutomatically = true)
    @Query("update TimeSlot t set t.reservedCount = t.reservedCount - :amount " +
           "where t.id = :id and t.reservedCount - :amount >= 0")
    int decreaseReserved(@Param("id") Long id, @Param("amount") int amount);
}
