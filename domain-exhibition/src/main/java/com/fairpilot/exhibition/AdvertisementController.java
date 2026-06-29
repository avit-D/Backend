package com.fairpilot.exhibition;

import com.fairpilot.core.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    /** 광고 슬롯 목록 조회 */
    @GetMapping("/slots")
    public ApiResponse<List<AdSlotResponse>> slots(
            @RequestParam(required = false) Long exhibitionId) {
        return ApiResponse.ok(advertisementService.findSlots(exhibitionId));
    }

    /** 광고 목록 조회 */
    @GetMapping
    public ApiResponse<List<AdvertisementResponse>> list(@RequestParam Long adSlotId) {
        return ApiResponse.ok(advertisementService.findAds(adSlotId));
    }

    /** 광고 생성 (PLATFORM_ADMIN / EXPO_ADMIN 전용) */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<AdvertisementResponse> create(@Valid @RequestBody AdRequest req) {
        return ApiResponse.ok(advertisementService.create(req));
    }

    /** 노출 카운트 증가 */
    @PostMapping("/{adId}/impression")
    public ApiResponse<Void> impression(@PathVariable Long adId) {
        advertisementService.incrementImpressions(adId);
        return ApiResponse.ok(null);
    }

    /** 클릭 카운트 증가 */
    @PostMapping("/{adId}/click")
    public ApiResponse<Void> click(@PathVariable Long adId) {
        advertisementService.incrementClicks(adId);
        return ApiResponse.ok(null);
    }
}