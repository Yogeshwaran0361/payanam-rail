package com.payanam.rail.repository;

import com.payanam.rail.domain.audit.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Simulated password-reset OTPs. */
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    Optional<PasswordResetRequest> findFirstByUserIdOrderByIdDesc(Long userId);
}
