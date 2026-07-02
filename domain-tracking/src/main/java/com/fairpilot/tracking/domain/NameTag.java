package com.fairpilot.tracking.domain;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 네임태그. 사전 인쇄 풀 + 입구 바인딩 쓰기는 체크인 담당.
 * 스캔 엔진은 token→attendee 해석과 상태 가드(AVAILABLE/REVOKED 거부)에만 사용.
 */
@Entity
@Table(name = "name_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NameTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exhibition_id", nullable = false)
    private Long exhibitionId;

    @Column(name = "attendee_id")
    private Long attendeeId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NameTagStatus status;

    @Column(name = "issued_by_user_id")
    private Long issuedByUserId;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    // -------------------------------------------------------
    // 도메인 메서드
    // -------------------------------------------------------

    /** AVAILABLE → ISSUED. */
    public void bind(Long attendeeId, Long staffUserId) {
        if (this.status != NameTagStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "바인딩할 수 없는 상태입니다: " + this.status);
        }
        this.attendeeId = attendeeId;
        this.status = NameTagStatus.ISSUED;
        this.issuedByUserId = staffUserId;
        this.issuedAt = LocalDateTime.now();
    }

    /** 어떤 상태든 → REVOKED. attendeeId 초기화. */
    public void revoke() {
        this.status = NameTagStatus.REVOKED;
        this.attendeeId = null;
        this.issuedByUserId = null;
        this.issuedAt = null;
    }

    // -------------------------------------------------------
    // 팩토리
    // -------------------------------------------------------

    /** 전시회용 AVAILABLE 태그 N개 일괄 생성. */
    public static List<NameTag> batchCreate(Long exhibitionId, int count) {
        List<NameTag> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            NameTag tag = new NameTag();
            tag.exhibitionId = exhibitionId;
            tag.token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            tag.status = NameTagStatus.AVAILABLE;
            list.add(tag);
        }
        return list;
    }
}
