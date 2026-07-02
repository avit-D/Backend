package com.fairpilot.platformadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.exhibition.Exhibition;
import com.fairpilot.exhibition.ExhibitionRequest;
import com.fairpilot.exhibition.ExhibitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 박람회 마스터 관리 (PLATFORM_ADMIN 전용).
 *
 * GET    /api/admin/exhibitions          — 전체 목록
 * POST   /api/admin/exhibitions          — 박람회 생성
 * DELETE /api/admin/exhibitions/{id}     — 박람회 삭제 (soft delete)
 */
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RestController
@RequestMapping("/api/admin/exhibitions")
@RequiredArgsConstructor
public class PlatformAdminExhibitionController {

    private final ExhibitionService exhibitionService;

    @GetMapping
    public ApiResponse<List<Exhibition>> list() {
        return ApiResponse.ok(exhibitionService.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Exhibition> create(
            @Valid @RequestBody ExhibitionRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        return ApiResponse.ok(exhibitionService.create(request, adminUserId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        exhibitionService.delete(id);
        return ApiResponse.ok(null);
    }
}
