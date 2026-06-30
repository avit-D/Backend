package com.fairpilot.core.user;

import com.fairpilot.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET is_deleted = 1 WHERE id = ?")
@SQLRestriction("is_deleted = 0")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash; // 소셜 로그인/초대 유저는 NULL 가능

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
    private SocialProvider socialProvider;

    @Column(name = "social_provider_id")
    private String socialProviderId;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Builder
    public User(String email, String passwordHash, String name, String phone, Role role,
                SocialProvider socialProvider, String socialProviderId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.role = role != null ? role : Role.VISITOR;
        this.socialProvider = socialProvider != null ? socialProvider : SocialProvider.NONE;
        this.socialProviderId = socialProviderId;
        this.isDeleted = false;
    }

    /** 기존 이메일/비밀번호 계정에 소셜 로그인 계정을 연동 */
    public void linkSocialAccount(SocialProvider provider, String providerId) {
        this.socialProvider = provider;
        this.socialProviderId = providerId;
    }
}