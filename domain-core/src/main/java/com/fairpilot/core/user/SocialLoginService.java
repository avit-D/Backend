package com.fairpilot.core.user;

import com.fairpilot.core.auth.JwtProvider;
import com.fairpilot.core.auth.RefreshTokenService;
import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    /** 구글 idToken 검증 후 로그인/자동연동/신규가입 → AccessToken + RefreshToken 반환 */
    @Transactional
    public Map<String, String> loginWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(idTokenString);

        String email = payload.getEmail();
        String googleSub = payload.getSubject();
        String name = (String) payload.get("name");

        User user = userRepository.findBySocialProviderAndSocialProviderId(
                        SocialProvider.GOOGLE, googleSub)
                .orElseGet(() -> linkOrCreateUser(email, googleSub, name));

        String accessToken = jwtProvider.generate(user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    /** 같은 이메일의 기존 계정이 있으면 연동, 없으면 신규 가입 */
    private User linkOrCreateUser(String email, String googleSub, String name) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .map(existing -> {
                    existing.linkSocialAccount(SocialProvider.GOOGLE, googleSub);
                    return existing;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .passwordHash(null)
                            .name(name != null ? name : "구글사용자")
                            .role(Role.VISITOR)
                            .socialProvider(SocialProvider.GOOGLE)
                            .socialProviderId(googleSub)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 구글 토큰입니다.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "구글 토큰 검증에 실패했습니다.");
        }
    }
}