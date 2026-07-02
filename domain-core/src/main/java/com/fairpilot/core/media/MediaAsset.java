package com.fairpilot.core.media;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE media_asset SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnerType ownerType;

    @Column(nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false, length = 500)
    private String s3Key;

    @Column(nullable = false, length = 120)
    private String s3Bucket;

    @Column(length = 100)
    private String contentType;

    @Column
    private Long fileSize;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean isThumbnail = false;

    @Column
    private Long uploadedBy;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder
    public MediaAsset(OwnerType ownerType, Long ownerId, MediaType mediaType,
                      String s3Key, String s3Bucket, String contentType,
                      Long fileSize, int displayOrder, boolean isThumbnail,
                      Long uploadedBy) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.mediaType = mediaType;
        this.s3Key = s3Key;
        this.s3Bucket = s3Bucket;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.displayOrder = displayOrder;
        this.isThumbnail = isThumbnail;
        this.uploadedBy = uploadedBy;
        this.isDeleted = false;
    }
}