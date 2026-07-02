package com.fairpilot.reservation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

/**
 * 입장 시간대 슬롯. reserved_count 가 동시성 제어 핵심 자원.
 * v2.4: 정원 차감 단위는 reservation.group_size. 원자 조건부 UPDATE 단일 전략(version 미사용).
 * DB 레벨 이중 방어: reserved_count <= capacity CHECK constraint.
 */
@Entity
@Table(name = "time_slot")
@Check(constraints = "reserved_count <= capacity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "reserved_count", nullable = false)
    private int reservedCount;

    public int getAvailableCount() {
        return Math.max(0, capacity - reservedCount);
    }
}
