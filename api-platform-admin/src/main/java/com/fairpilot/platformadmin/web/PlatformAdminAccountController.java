package com.fairpilot.platformadmin.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.core.user.Role;
import com.fairpilot.core.user.User;
import com.fairpilot.core.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 운영 계정 발급 (PLATFORM_ADMIN 전용).
 *
 * POST   /api/admin/accounts            — EXPO_ADMIN·ACCOUNTANT·STAFF 계정 발급
 * GET    /api/admin/accounts            — 운영 계정 목록 (VISITOR 제외)
 * DELETE /api/admin/accounts/{id}       — 계정 비활성화 (soft delete)
 */
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class PlatformAdminAccountController {

    /** PLATFORM_ADMIN이 발급 가능한 역할 — VISITOR 셀프 가입은 AuthController 담당 */
    private static final Set<Role> ISSUABLE_ROLES = Set.of(
            Role.EXPO_ADMIN, Role.ACCOUNTANT, Role.STAFF, Role.EXHIBITOR
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 운영 계정 발급 요청 DTO */
    public record IssueAccountRequest(
            @NotBlank @Email String email,
            @NotBlank String name,
            String phone,
            @NotBlank String tempPassword,
            @NotNull Role role
    ) {}

    /** 계정 응답 DTO */
    public record AccountResponse(Long id, String email, String name, String phone, String role) {
        public static AccountResponse from(User u) {
            return new AccountResponse(u.getId(), u.getEmail(), u.getName(), u.getPhone(), u.getRole().name());
        }
    }

    /** 운영 계정 발급 — EXPO_ADMIN / ACCOUNTANT / STAFF / EXHIBITOR 역할만 허용 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> issueAccount(@Valid @RequestBody IssueAccountRequest req) {
        if (!ISSUABLE_ROLES.contains(req.role())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "발급 가능한 역할: " + ISSUABLE_ROLES);
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.tempPassword()))
                .name(req.name())
                .phone(req.phone())
                .role(req.role())
                .build();
        return ApiResponse.ok(AccountResponse.from(userRepository.save(user)));
    }

    /** 운영 계정 목록 조회 — VISITOR 제외 전체 */
    @GetMapping
    public ApiResponse<List<AccountResponse>> listAccounts() {
        List<AccountResponse> accounts = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.VISITOR)
                .map(AccountResponse::from)
                .toList();
        return ApiResponse.ok(accounts);
    }

    /** 계정 비활성화 (soft delete) */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계정을 찾을 수 없습니다."));
        if (user.getRole() == Role.PLATFORM_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "PLATFORM_ADMIN 계정은 삭제할 수 없습니다.");
        }
        userRepository.delete(user);
        return ApiResponse.ok(null);
    }
}
