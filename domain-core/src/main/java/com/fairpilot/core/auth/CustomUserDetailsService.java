package com.fairpilot.core.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @PersistenceContext
    private EntityManager em;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            Object[] row = (Object[]) em.createNativeQuery(
                            "SELECT password_hash, role FROM users WHERE id = :id AND deleted_at IS NULL"
                    )
                    .setParameter("id", Long.valueOf(userId))
                    .getSingleResult();

            String passwordHash = (String) row[0];
            String role = (String) row[1];

            return new User(
                    userId,
                    passwordHash,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        } catch (Exception e) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
    }
}