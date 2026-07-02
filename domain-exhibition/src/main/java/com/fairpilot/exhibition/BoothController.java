package com.fairpilot.exhibition;

import com.fairpilot.core.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/booths")
@RequiredArgsConstructor
public class BoothController {

    private final BoothService boothService;

    /** 부스 목록 조회 (전체 공개) */
    @GetMapping
    public ApiResponse<List<Booth>> list(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(boothService.findAll(exhibitionId));
    }

    /** 부스 단건 조회 */
    @GetMapping("/{boothId}")
    public ApiResponse<Booth> get(@PathVariable Long exhibitionId,
                                  @PathVariable Long boothId) {
        return ApiResponse.ok(boothService.findById(exhibitionId, boothId));
    }

    /** 부스 생성 (EXPO_ADMIN 전용) */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<Booth> create(@PathVariable Long exhibitionId,
                                     @Valid @RequestBody BoothRequest req) {
        return ApiResponse.ok(boothService.create(exhibitionId, req));
    }

    /** 부스 삭제 (EXPO_ADMIN 전용) */
    @DeleteMapping("/{boothId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long exhibitionId,
                                    @PathVariable Long boothId) {
        boothService.delete(exhibitionId, boothId);
        return ApiResponse.ok(null);
    }
}