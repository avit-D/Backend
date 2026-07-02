package com.fairpilot.exhibition;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long exhibitionId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column
    private Integer quota;

    @Builder
    public TicketType(Long exhibitionId, String name, BigDecimal price, Integer quota) {
        this.exhibitionId = exhibitionId;
        this.name = name;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.quota = quota;
    }
}