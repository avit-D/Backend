package com.fairpilot.exhibition;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "booth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long exhibitionId;

    @Column(nullable = false)
    private Long exhibitorId;

    @Column
    private Long categoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String tags;

    @Column
    private Integer posX;

    @Column
    private Integer posY;

    @Column
    private Integer floor;

    @Builder
    public Booth(Long exhibitionId, Long exhibitorId, Long categoryId,
                 String name, String description, String tags,
                 Integer posX, Integer posY, Integer floor) {
        this.exhibitionId = exhibitionId;
        this.exhibitorId = exhibitorId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.posX = posX;
        this.posY = posY;
        this.floor = floor;
    }
}