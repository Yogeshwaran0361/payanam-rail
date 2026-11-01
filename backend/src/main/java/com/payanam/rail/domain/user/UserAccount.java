package com.payanam.rail.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * ABSTRACT login account. INHERITANCE: {@link PassengerUser} and {@link StationStaffUser} extend it
 * and override {@link #getRole()}, {@link #getPermissions()} and {@link #getDisplayTitle()}
 * (role-based behaviour through POLYMORPHISM). Stored in one table with a discriminator column.
 *
 * <p>ENCAPSULATION: the lockout counters can only change through {@link #registerFailedLogin} and
 * {@link #registerSuccessfulLogin}; the password hash only through {@link #changePasswordHash}.</p>
 */
@Entity
@Table(name = "user_account")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type")
public abstract class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String fullName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 10)
    private String mobile;

    /** BCrypt hash, never the password itself. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;

    protected UserAccount() {
        // for JPA
    }

    protected UserAccount(String fullName, String email, String mobile, String passwordHash, LocalDateTime createdAt) {
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.mobile = mobile.trim();
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /** PASSENGER or STATION_STAFF. */
    public abstract Role getRole();

    /** What this account may do. Passengers have none of the staff permissions. */
    public abstract Set<Permission> getPermissions();

    /** Title shown in the profile menu, e.g. "Passenger" or "Station Master". */
    public abstract String getDisplayTitle();

    public boolean hasPermission(Permission permission) {
        return getPermissions().contains(permission);
    }

    /** True while a lockout is active. */
    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Counts a wrong password. After {@code maxAttempts} failures in a row the account is locked for
     * {@code lockMinutes}. Returns the number of attempts left before the lock (0 = now locked).
     */
    public int registerFailedLogin(LocalDateTime now, int maxAttempts, int lockMinutes) {
        if (lockedUntil != null && !now.isBefore(lockedUntil)) {
            failedLoginAttempts = 0;          // the previous lock has expired
            lockedUntil = null;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plusMinutes(lockMinutes);
            return 0;
        }
        return maxAttempts - failedLoginAttempts;
    }

    public void registerSuccessfulLogin(LocalDateTime now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    /** Sets a new BCrypt hash and clears any lockout (used by change and reset password). */
    public void changePasswordHash(String newHash) {
        this.passwordHash = newHash;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /** Profile edit: name and mobile (the email is the permanent login id). */
    public void updateContact(String newFullName, String newMobile) {
        this.fullName = newFullName.trim();
        this.mobile = newMobile.trim();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
