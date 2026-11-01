package com.payanam.rail.repository;

import com.payanam.rail.domain.audit.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Sign-in sessions, used to revoke JWTs on logout and password change. */
public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    List<AuthSession> findByUserIdAndRevokedAtIsNull(Long userId);
}
