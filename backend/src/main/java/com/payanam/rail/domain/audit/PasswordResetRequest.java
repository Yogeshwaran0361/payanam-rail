package com.payanam.rail.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A SIMULATED "forgot password" request. There is no e-mail service, so the 6-digit OTP is shown
 * on screen and written to the log; only its BCrypt hash is stored here.
 */
@Entity
@Table(name = "password_reset_request")
public class PasswordResetRequest {
    /** Wrong OTP guesses allowed before the request is burnt. */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean used;
    private int attempts;

    protected PasswordResetRequest() {
        // for JPA
    }

    public PasswordResetRequest(Long userId, String otpHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(LocalDateTime now) {
        return !used && attempts < MAX_ATTEMPTS && now.isBefore(expiresAt);
    }

    public void registerWrongAttempt() {
        attempts++;
    }

    public void markUsed() {
        used = true;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
