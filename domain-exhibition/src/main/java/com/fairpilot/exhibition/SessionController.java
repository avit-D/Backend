package com.fairpilot.exhibition;

import com.fairpilot.core.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /** 세션 목록 조회 (전체 공개) */
    @GetMapping
    public ApiResponse<List<Session>> list(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(sessionService.findAll(exhibitionId));
    }

    /** 세션 단건 조회 */
    @GetMapping("/{sessionId}")
    public ApiResponse<Session> get(@PathVariable Long exhibitionId,
                                    @PathVariable Long sessionId) {
        return ApiResponse.ok(sessionService.findById(exhibitionId, sessionId));
    }

    /** 세션 생성 (EXPO_ADMIN 전용) */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<Session> create(@PathVariable Long exhibitionId,
                                       @Valid @RequestBody SessionRequest req) {
        return ApiResponse.ok(sessionService.create(exhibitionId, req));
    }

    /** 세션 삭제 (EXPO_ADMIN 전용) */
    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long exhibitionId,
                                    @PathVariable Long sessionId) {
        sessionService.delete(exhibitionId, sessionId);
        return ApiResponse.ok(null);
    }
}