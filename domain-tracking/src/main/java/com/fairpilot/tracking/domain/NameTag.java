package com.fairpilot.tracking.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 네임태그(읽기 모델). 사전 인쇄 풀 + 입구 바인딩의 쓰기는 2번(체크인) 담당.
 * 4번 스캔 엔진은 token→attendee 해석과 상태 가드(AVAILABLE/REVOKED 거부)에만 사용한다.
 */
@Entity
@Table(name = "name_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NameTag {

    @Id
    private Long id;

    @Column(name = "exhibition_id")
    private Long exhibitionId;

    @Column(name = "attendee_id")
    private Long attendeeId;

    @Column(name = "token")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NameTagStatus status;
}
