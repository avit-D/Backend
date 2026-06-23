package com.fairpilot.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** POST /api/checkin/nametag — QR 티켓 + 네임태그 바인딩 */
public record BindNametagRequest(
        @NotNull Long exhibitionId,
        @NotBlank String ticketQrToken,
        @NotBlank String nametagToken
) {}
