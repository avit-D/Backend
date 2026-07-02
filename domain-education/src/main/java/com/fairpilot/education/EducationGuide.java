package com.fairpilot.education;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "education_guide")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EducationGuide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long exhibitionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetRole targetRole;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 255)
    private String videoUrl;

    @Column(nullable = false)
    private boolean isRequired;

    @Column(nullable = false)
    private int sortOrder;

    @Column(columnDefinition = "JSON")
    private String quizQuestions;

    @Column
    private Integer quizPassScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuideStatus status;

    @Builder
    public EducationGuide(Long exhibitionId, TargetRole targetRole, String category,
                          String title, String content, String videoUrl,
                          boolean isRequired, int sortOrder,
                          String quizQuestions, Integer quizPassScore) {
        this.exhibitionId = exhibitionId;
        this.targetRole = targetRole;
        this.category = category;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.isRequired = isRequired;
        this.sortOrder = sortOrder;
        this.quizQuestions = quizQuestions;
        this.quizPassScore = quizPassScore;
        this.status = GuideStatus.ACTIVE;
    }
}