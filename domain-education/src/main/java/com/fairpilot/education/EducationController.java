package com.fairpilot.education;

import com.fairpilot.core.auth.CurrentUser;
import com.fairpilot.core.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/education")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    /** 가이드 목록 조회 (STAFF / EXHIBITOR 전용) */
    @GetMapping("/guides")
    @PreAuthorize("hasAnyRole('STAFF', 'EXHIBITOR', 'EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<List<EducationGuide>> list(
            @RequestParam TargetRole role) {
        return ApiResponse.ok(educationService.findGuides(role));
    }

    /** 가이드 단건 조회 */
    @GetMapping("/guides/{guideId}")
    @PreAuthorize("hasAnyRole('STAFF', 'EXHIBITOR', 'EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<EducationGuide> get(@PathVariable Long guideId) {
        return ApiResponse.ok(educationService.findGuide(guideId));
    }

    /** 영상 시청 완료 처리 */
    @PostMapping("/guides/{guideId}/video-complete")
    @PreAuthorize("hasAnyRole('STAFF', 'EXHIBITOR')")
    public ApiResponse<Void> videoComplete(
            @PathVariable Long guideId,
            @CurrentUser Long userId) {
        educationService.markVideoCompleted(guideId, userId);
        return ApiResponse.ok(null);
    }

    /** 텍스트 가이드 확인 완료 */
    @PostMapping("/guides/{guideId}/confirm")
    @PreAuthorize("hasAnyRole('STAFF', 'EXHIBITOR')")
    public ApiResponse<Void> confirm(
            @PathVariable Long guideId,
            @CurrentUser Long userId) {
        educationService.markPassed(guideId, userId);
        return ApiResponse.ok(null);
    }

    /** LMS 자격 판정 (하드 게이트용) */
    @GetMapping("/qualified")
    @PreAuthorize("hasAnyRole('STAFF', 'EXHIBITOR', 'EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<Boolean> isQualified(
            @RequestParam Long exhibitionId,
            @RequestParam TargetRole role,
            @CurrentUser Long userId) {
        return ApiResponse.ok(educationService.isQualified(userId, exhibitionId, role));
    }
}