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
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",                          // 로그인·회원가입
                                "/api/exhibitions",                      // 박람회 목록 (비로그인)
                                "/api/exhibitions/*",                    // 박람회 단건 (비로그인)
                                "/api/exhibitions/*/booths",             // 부스 목록 (비로그인)
                                "/api/exhibitions/*/booths/*",           // 부스 단건 (비로그인)
                                "/api/exhibitions/*/sessions",           // 세션 목록 (비로그인)
                                "/api/exhibitions/*/sessions/*",         // 세션 단건 (비로그인)
                                "/api/exhibitions/*/ticket-types",       // 티켓타입 목록 (비로그인)
                                "/api/payments/webhook"                  // 포트원 webhook (인증 불필요)
                        ).permitAll()
                        .requestMatchers(
                                "/api/exhibitions/*/reservations",       // 예약 현황
                                "/api/exhibitions/*/reservations/export" // 예약 엑셀
                        ).hasAnyRole("EXPO_ADMIN", "ACCOUNTANT", "PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

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