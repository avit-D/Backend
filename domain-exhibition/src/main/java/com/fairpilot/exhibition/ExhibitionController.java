package com.fairpilot.exhibition;

import com.fairpilot.core.auth.CurrentUser;
import com.fairpilot.core.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    /** 박람회 목록 조회 (전체 공개) */
    @GetMapping
    public ApiResponse<List<Exhibition>> list() {
        return ApiResponse.ok(exhibitionService.findAll());
    }

    /** 박람회 단건 조회 */
    @GetMapping("/{id}")
    public ApiResponse<Exhibition> get(@PathVariable Long id) {
        return ApiResponse.ok(exhibitionService.findById(id));
    }

    /** 박람회 생성 (PLATFORM_ADMIN 전용) */
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Exhibition> create(
            @Valid @RequestBody ExhibitionRequest req,
            @CurrentUser Long userId) {
        return ApiResponse.ok(exhibitionService.create(req, userId));
    }

    /** 박람회 삭제 (PLATFORM_ADMIN 전용) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        exhibitionService.delete(id);
        return ApiResponse.ok(null);
    }
}