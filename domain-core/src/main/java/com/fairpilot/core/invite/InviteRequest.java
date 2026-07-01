package com.fairpilot.core.invite;

import com.fairpilot.core.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        /**
         * 초대 가능 역할: EXPO_ADMIN, STAFF, ACCOUNTANT, EXHIBITOR
         * PLATFORM_ADMIN, VISITOR 는 초대 불가 (서비스에서 검증)
         */
        @NotNull(message = "역할은 필수입니다.")
        Role role,

        /** 소속 박람회 ID */
        Long exhibitionId
) {}