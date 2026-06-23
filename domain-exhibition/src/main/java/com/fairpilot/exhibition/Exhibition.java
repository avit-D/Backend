package com.fairpilot.exhibition;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "exhibition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE exhibition SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Exhibition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120, unique = true)
    private String slug;

    @Column(length = 200)
    private String venue;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExhibitionStatus status;

    @Column(nullable = false)
    private boolean enforceStaffQualification;

    @Column(nullable = false)
    private Long createdBy;

    @Column
    private java.time.LocalDateTime deletedAt;

    @Builder
    public Exhibition(String title, String slug, String venue, String address,
                      LocalDate startDate, LocalDate endDate, Long createdBy) {
        this.title = title;
        this.slug = slug;
        this.venue = venue;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ExhibitionStatus.DRAFT;
        this.enforceStaffQualification = false;
        this.createdBy = createdBy;
    }
}