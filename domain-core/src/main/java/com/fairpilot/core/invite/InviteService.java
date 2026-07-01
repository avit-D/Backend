package com.fairpilot.core.invite;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import com.fairpilot.core.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${invite.base-url}")
    private String baseUrl;

    @Value("${invite.expiry-hours:72}")
    private int expiryHours;

    /** 초대 불가 역할 */
    private static final List<Role> UNINVITABLE_ROLES =
            List.of(Role.PLATFORM_ADMIN, Role.VISITOR);

    /**
     * 관리자 초대 발급
     * 1. 역할 유효성 검증
     * 2. 이미 ACTIVE 계정이면 예외
     * 3. INVITED 상태면 토큰 재발급 (재초대)
     * 4. 신규면 User 생성
     * 5. 이메일 발송
     */
    @Transactional
    public void invite(InviteRequest req) {
        if (UNINVITABLE_ROLES.contains(req.role())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "초대할 수 없는 역할입니다: " + req.role());
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);

        userRepository.findByEmailAndIsDeletedFalse(req.email())
                .ifPresentOrElse(existingUser -> {
                    if (existingUser.getAccountStatus() == AccountStatus.ACTIVE) {
                        throw new BusinessException(ErrorCode.CONFLICT,
                                "이미 활성화된 계정입니다: " + req.email());
                    }
                    // INVITED 상태 → 토큰 재발급
                    existingUser.reissueInviteToken(token, expiresAt);
                    log.info("초대 토큰 재발급: email={}", req.email());
                }, () -> {
                    // 신규 유저 생성
                    User user = User.builder()
                            .email(req.email())
                            .name(req.name())
                            .role(req.role())
                            .accountStatus(AccountStatus.INVITED)
                            .inviteToken(token)
                            .inviteExpiresAt(expiresAt)
                            .build();
                    userRepository.save(user);
                    log.info("초대 유저 생성: email={}, role={}", req.email(), req.role());
                });

        sendInviteMail(req.email(), req.name(), token);
    }

    /**
     * 초대 수락 — 비밀번호 설정 + 계정 활성화
     * 1. 토큰으로 유저 조회
     * 2. 만료 여부 확인
     * 3. acceptInvite() → passwordHash 저장, ACTIVE 전환, 토큰 소멸
     */
    @Transactional
    public void accept(AcceptInviteRequest req) {
        User user = userRepository.findByInviteToken(req.token())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "유효하지 않은 초대 토큰입니다."));

        if (user.getInviteExpiresAt() == null ||
                user.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "만료된 초대 토큰입니다. 관리자에게 재초대를 요청하세요.");
        }

        user.acceptInvite(passwordEncoder.encode(req.password()));
        log.info("초대 수락 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    private void sendInviteMail(String to, String name, String token) {
        String link = baseUrl + "/invite/accept?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[FairPilot] 관리자 초대");
        message.setText(
                name + "님 안녕하세요.\n\n" +
                        "FairPilot 관리자로 초대되었습니다.\n" +
                        "아래 링크에서 " + expiryHours + "시간 이내에 비밀번호를 설정해주세요.\n\n" +
                        link + "\n\n" +
                        "본 메일은 발신 전용입니다."
        );

        try {
            mailSender.send(message);
            log.info("초대 메일 발송 완료: to={}", to);
        } catch (Exception e) {
            log.error("초대 메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new BusinessException(ErrorCode.UPSTREAM_ERROR,
                    "초대 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}