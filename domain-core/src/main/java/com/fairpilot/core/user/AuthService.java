package com.fairpilot.core.user;

import com.fairpilot.core.auth.JwtProvider;
import com.fairpilot.core.auth.RefreshTokenService;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void signUp(String email, String password, String name, String phone) {
        // is_deleted = 0 인 활성 유저만 중복 체크
        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .phone(phone)
                .role(Role.VISITOR)
                .build();
        userRepository.save(user);
    }

    /** 로그인 → AccessToken + RefreshToken 반환 */
    @Transactional(readOnly = true)
    public Map<String, String> login(String email, String password) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않습니다."));

        // 소셜 로그인/초대 미완료 유저는 passwordHash가 NULL일 수 있음 — NPE 방어
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "소셜 로그인 계정입니다. 구글 로그인을 이용해주세요.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtProvider.generate(user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    /** Access Token 재발급 */
    public Map<String, String> refresh(String refreshToken) {
        Long userId = refreshTokenService.validate(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 없음"));

        String newAccessToken = jwtProvider.generate(user.getId(), user.getRole().name());
        return Map.of("accessToken", newAccessToken);
    }

    /** 로그아웃 */
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}