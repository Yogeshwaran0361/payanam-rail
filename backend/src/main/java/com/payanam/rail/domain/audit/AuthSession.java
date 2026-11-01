package com.payanam.rail.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One sign-in. The JWT carries this row's id as its "jti" claim. A JWT is accepted only while its
 * session is not revoked and not expired, so logout and password changes take effect immediately
 * even though JWTs are otherwise stateless.
 */
@Entity
@Table(name = "auth_session", indexes = @Index(columnList = "userId"))
public class AuthSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;
    private boolean rememberMe;

    @Column(length = 200)
    private String userAgent;

    protected AuthSession() {
        // for JPA
    }

    public AuthSession(String id, Long userId, LocalDateTime issuedAt, LocalDateTime expiresAt, boolean rememberMe,
                       String userAgent) {
        this.id = id;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.rememberMe = rememberMe;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(200, userAgent.length()));
    }

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(LocalDateTime now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public String getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
