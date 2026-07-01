package com.fairpilot.core.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // ── 인증 ──────────────────────────────
                                "/api/auth/**",

                                // ── 초대 수락 (JWT 없음) ───────────────
                                "/api/invite/accept",

                                // ── 박람회 공개 조회 (비로그인) ──────────
                                "/api/exhibitions",
                                "/api/exhibitions/*",
                                "/api/exhibitions/*/booths",
                                "/api/exhibitions/*/booths/*",
                                "/api/exhibitions/*/sessions",
                                "/api/exhibitions/*/sessions/*",
                                "/api/exhibitions/*/ticket-types",

                                // ── Webhook (외부 서버 호출, JWT 없음) ──
                                "/api/payments/webhook",
                                "/api/payments/webhook/toss",

                                // ── 광고 공개 (비로그인) ─────────────────
                                "/api/advertisements/slots",
                                "/api/advertisements",
                                "/api/advertisements/*/impression",
                                "/api/advertisements/*/click"
                        ).permitAll()
                        .requestMatchers(
                                "/api/exhibitions/*/reservations",
                                "/api/exhibitions/*/reservations/export"
                        ).hasAnyRole("EXPO_ADMIN", "ACCOUNTANT", "PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}