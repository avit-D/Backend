package com.fairpilot.exhibition;

import com.fairpilot.core.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exhibitions/{exhibitionId}/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    /** 티켓타입 목록 조회 */
    @GetMapping
    public ApiResponse<List<TicketType>> list(@PathVariable Long exhibitionId) {
        return ApiResponse.ok(ticketTypeService.findAll(exhibitionId));
    }

    /** 티켓타입 단건 조회 */
    @GetMapping("/{ticketTypeId}")
    public ApiResponse<TicketType> get(@PathVariable Long exhibitionId,
                                       @PathVariable Long ticketTypeId) {
        return ApiResponse.ok(ticketTypeService.findById(exhibitionId, ticketTypeId));
    }

    /** 티켓타입 생성 (EXPO_ADMIN 전용) */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<TicketType> create(@PathVariable Long exhibitionId,
                                          @Valid @RequestBody TicketTypeRequest req) {
        return ApiResponse.ok(ticketTypeService.create(exhibitionId, req));
    }

    /** 티켓타입 삭제 (EXPO_ADMIN 전용) */
    @DeleteMapping("/{ticketTypeId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'EXPO_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long exhibitionId,
                                    @PathVariable Long ticketTypeId) {
        ticketTypeService.delete(exhibitionId, ticketTypeId);
        return ApiResponse.ok(null);
    }
}