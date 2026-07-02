package com.fairpilot.education;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "education_completion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"guide_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EducationCompletion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long guideId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private boolean videoCompleted;

    @Column
    private Integer quizScore;

    @Column(nullable = false)
    private boolean passed;

    @Column
    private LocalDateTime confirmedAt;

    @Builder
    public EducationCompletion(Long guideId, Long userId,
                               boolean videoCompleted, Integer quizScore) {
        this.guideId = guideId;
        this.userId = userId;
        this.videoCompleted = videoCompleted;
        this.quizScore = quizScore;
        this.passed = false;
    }

    /** 이수 완료 처리 */
    public void markPassed() {
        this.passed = true;
        this.confirmedAt = LocalDateTime.now();
    }

    /** 영상 시청 완료 */
    public void markVideoCompleted() {
        this.videoCompleted = true;
    }
}