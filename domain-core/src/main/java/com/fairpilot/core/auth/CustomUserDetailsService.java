package com.fairpilot.core.auth;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
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
        Object[] row = (Object[]) em.createNativeQuery(
                        "SELECT password_hash, role FROM users WHERE id = :id AND deleted_at IS NULL"
                )
                .setParameter("id", Long.valueOf(userId))
                .getSingleResult();

        if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "사용자 없음");

        String passwordHash = (String) row[0];
        String role = (String) row[1];

        return new User(
                userId,
                passwordHash,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}