package com.payanam.rail.service;

import com.payanam.rail.config.PayanamProperties;
import com.payanam.rail.domain.audit.AuthSession;
import com.payanam.rail.domain.audit.PasswordResetRequest;
import com.payanam.rail.domain.user.PassengerUser;
import com.payanam.rail.domain.user.StationStaffUser;
import com.payanam.rail.domain.user.UserAccount;
import com.payanam.rail.dto.auth.AuthResponse;
import com.payanam.rail.dto.auth.ChangePasswordRequest;
import com.payanam.rail.dto.auth.ForgotPasswordRequest;
import com.payanam.rail.dto.auth.ForgotPasswordResponse;
import com.payanam.rail.dto.auth.PassengerSignUpRequest;
import com.payanam.rail.dto.auth.ResetPasswordRequest;
import com.payanam.rail.dto.auth.SignInRequest;
import com.payanam.rail.dto.auth.StaffSignUpRequest;
import com.payanam.rail.dto.auth.UpdateProfileRequest;
import com.payanam.rail.dto.auth.UserProfileView;
import com.payanam.rail.exception.AccountLockedException;
import com.payanam.rail.exception.AuthenticationFailedException;
import com.payanam.rail.exception.ValidationException;
import com.payanam.rail.repository.AuthSessionRepository;
import com.payanam.rail.repository.PasswordResetRequestRepository;
import com.payanam.rail.repository.StationStaffUserRepository;
import com.payanam.rail.repository.UserAccountRepository;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sign up, sign in (with lockout), logout, profile and the SIMULATED forgot-password flow.
 * Passwords are hashed with BCrypt; sign-in returns a JWT linked to an {@link AuthSession}.
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final StationStaffUserRepository staffUsers;
    private final AuthSessionRepository sessions;
    private final PasswordResetRequestRepository resets;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final PayanamProperties properties;
    private final Clock clock;

    public AuthService(UserAccountRepository users, StationStaffUserRepository staffUsers,
                       AuthSessionRepository sessions, PasswordResetRequestRepository resets, PasswordEncoder encoder,
                       JwtService jwt, AuditService audit, PayanamProperties properties, Clock clock) {
        this.users = users;
        this.staffUsers = staffUsers;
        this.sessions = sessions;
        this.resets = resets;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    // ============================================================ sign up

    @Transactional
    public AuthResponse signUpPassenger(PassengerSignUpRequest r, String userAgent) {
        requireUniqueContact(r.email(), r.mobile());
        PassengerUser user = users.save(new PassengerUser(r.fullName(), r.email(), r.mobile(),
                encoder.encode(r.password()), r.dateOfBirth(), r.gender(), r.city(), now()));
        audit.recordAs(user.getEmail(), "PASSENGER", "SIGN_UP", "Passenger account created", true);
        return issue(user, false, userAgent);
    }

    /** Staff registration is refused without the station access code (application.yml). */
    @Transactional
    public AuthResponse signUpStaff(StaffSignUpRequest r, String userAgent) {
        if (!properties.station().accessCode().equals(r.accessCode().trim())) {
            audit.recordAs(r.email(), "STATION_STAFF", "SIGN_UP", "Refused: wrong station access code", false);
            throw new ValidationException("accessCode: The station access code is not valid");
        }
        String domain = properties.security().staffEmailDomain();
        if (domain != null && !domain.isBlank() && !r.email().trim().toLowerCase().endsWith("@" + domain)) {
            throw new ValidationException("email: Use your official @" + domain + " email address");
        }
        if (staffUsers.existsByEmployeeIdIgnoreCase(r.employeeId().trim())) {
            throw new ValidationException("employeeId: This employee ID is already registered");
        }
        requireUniqueContact(r.email(), r.mobile());
        StationStaffUser user = users.save(new StationStaffUser(r.fullName(), r.email(), r.mobile(),
                encoder.encode(r.password()), r.employeeId(), r.designation(), now()));
        audit.recordAs(user.getEmail(), user.getDesignation().name(), "SIGN_UP", "Staff account created ("
                + user.getEmployeeId() + ")", true);
        return issue(user, false, userAgent);
    }

    private void requireUniqueContact(String email, String mobile) {
        if (users.existsByEmailIgnoreCase(email.trim())) {
            throw new ValidationException("email: This email is already registered");
        }
        if (users.existsByMobile(mobile.trim())) {
            throw new ValidationException("mobile: This mobile number is already registered");
        }
    }

    // ============================================================ sign in

    /**
     * Sign in with email OR mobile. A wrong password counts towards the lockout: after 5 failures the
     * account is locked for 15 minutes (both configurable). The error never says whether the account exists.
     */
    @Transactional(noRollbackFor = {AuthenticationFailedException.class, AccountLockedException.class})
    public AuthResponse signIn(SignInRequest r, String userAgent) {
        LocalDateTime now = now();
        Optional<UserAccount> found = findByIdentifier(r.identifier());
        if (found.isEmpty()) {
            audit.recordAs(r.identifier(), "PUBLIC", "SIGN_IN", "Unknown account", false);
            throw new AuthenticationFailedException("Invalid email/mobile or password");
        }
        UserAccount user = found.get();
        if (user.isLocked(now)) {
            throw locked(user, now);
        }
        if (!encoder.matches(r.password(), user.getPasswordHash())) {
            PayanamProperties.Security sec = properties.security();
            int left = user.registerFailedLogin(now, sec.maxFailedAttempts(), sec.lockoutMinutes());
            users.save(user);
            audit.recordAs(user.getEmail(), user.getRole().name(), "SIGN_IN", "Wrong password (" + left
                    + " attempts left)", false);
            if (left == 0) {
                throw locked(user, now);
            }
            throw new AuthenticationFailedException("Invalid email/mobile or password. " + left
                    + (left == 1 ? " attempt" : " attempts") + " left before the account is locked.");
        }
        user.registerSuccessfulLogin(now);
        users.save(user);
        audit.recordAs(user.getEmail(), roleOf(user), "SIGN_IN", "Signed in" + (r.rememberMe() ? " (remember me)" : ""),
                true);
        return issue(user, r.rememberMe(), userAgent);
    }

    private AccountLockedException locked(UserAccount user, LocalDateTime now) {
        long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
        audit.recordAs(user.getEmail(), user.getRole().name(), "ACCOUNT_LOCKED", "Locked until " + user.getLockedUntil(),
                false);
        return new AccountLockedException("Too many failed attempts. The account is locked for " + minutes
                + " more minute" + (minutes == 1 ? "" : "s") + ".");
    }

    private Optional<UserAccount> findByIdentifier(String identifier) {
        String id = identifier == null ? "" : identifier.trim();
        return id.contains("@") ? users.findByEmailIgnoreCase(id) : users.findByMobile(id);
    }

    /** Creates the session row and signs a JWT for it. */
    private AuthResponse issue(UserAccount user, boolean rememberMe, String userAgent) {
        PayanamProperties.Security sec = properties.security();
        Instant issued = clock.instant();
        Instant expires = rememberMe ? issued.plus(Duration.ofDays(sec.rememberMeDays()))
                : issued.plus(Duration.ofMinutes(sec.jwtExpiryMinutes()));
        String sessionId = UUID.randomUUID().toString();
        sessions.save(new AuthSession(sessionId, user.getId(), toLocal(issued), toLocal(expires), rememberMe, userAgent));
        String token = jwt.createToken(AuthUser.of(user, sessionId), issued, expires);
        return new AuthResponse(token, expires, profile(user));
    }

    /** Logout: the session is revoked, so the same JWT is refused from now on. */
    @Transactional
    public void logout(AuthUser user) {
        sessions.findById(user.sessionId()).ifPresent(s -> {
            s.revoke(now());
            sessions.save(s);
        });
        audit.record("SIGN_OUT", "Signed out");
    }

    /** Issues a short-lived token for an account without a password (used by the Test Lab for F4). */
    @Transactional
    public String issueInternalToken(UserAccount user) {
        return issue(user, false, "test-lab").token();
    }

    /** Revokes one session by id (used after an internal token is no longer needed). */
    @Transactional
    public void revokeSession(String sessionId) {
        sessions.findById(sessionId).ifPresent(s -> {
            s.revoke(now());
            sessions.save(s);
        });
    }

    // ============================================================ forgot password (simulated)

    /**
     * Generates a 6-digit OTP valid for 10 minutes. With no e-mail service, the OTP is returned to the
     * screen and written to the server log as a "simulated e-mail".
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest r) {
        int minutes = properties.security().otpExpiryMinutes();
        Optional<UserAccount> found = findByIdentifier(r.identifier());
        String generic = "If the account exists, a one-time password has been sent (simulated: shown below).";
        if (found.isEmpty()) {
            return new ForgotPasswordResponse(generic, null, minutes);
        }
        UserAccount user = found.get();
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        resets.save(new PasswordResetRequest(user.getId(), encoder.encode(otp), now().plusMinutes(minutes)));
        log.warn("SIMULATED EMAIL to {}: your Payanam Rail password reset OTP is {} (valid {} min)", user.getEmail(),
                otp, minutes);
        audit.recordAs(user.getEmail(), user.getRole().name(), "PASSWORD_RESET_REQUESTED", "OTP generated", true);
        return new ForgotPasswordResponse(generic, otp, minutes);
    }

    @Transactional(noRollbackFor = ValidationException.class)
    public void resetPassword(ResetPasswordRequest r) {
        UserAccount user = findByIdentifier(r.identifier())
                .orElseThrow(() -> new ValidationException("otp: The OTP is invalid or has expired"));
        PasswordResetRequest request = resets.findFirstByUserIdOrderByIdDesc(user.getId())
                .filter(req -> req.isUsable(now()))
                .orElseThrow(() -> new ValidationException("otp: The OTP is invalid or has expired"));
        if (!encoder.matches(r.otp(), request.getOtpHash())) {
            request.registerWrongAttempt();
            resets.save(request);
            throw new ValidationException("otp: The OTP is not correct");
        }
        request.markUsed();
        resets.save(request);
        user.changePasswordHash(encoder.encode(r.password()));
        users.save(user);
        revokeAllSessions(user.getId());
        audit.recordAs(user.getEmail(), user.getRole().name(), "PASSWORD_RESET", "Password reset with OTP", true);
    }

    // ============================================================ profile

    public UserProfileView me(AuthUser user) {
        return profile(load(user));
    }

    @Transactional
    public UserProfileView updateProfile(AuthUser current, UpdateProfileRequest r) {
        UserAccount user = load(current);
        if (!user.getMobile().equals(r.mobile().trim()) && users.existsByMobile(r.mobile().trim())) {
            throw new ValidationException("mobile: This mobile number is already registered");
        }
        user.updateContact(r.fullName(), r.mobile());
        if (user instanceof PassengerUser passenger) {
            if (r.city() == null || r.city().isBlank()) {
                throw new ValidationException("city: City is required");
            }
            passenger.updateCity(r.city());
        }
        users.save(user);
        audit.record("PROFILE_UPDATED", "Name/mobile/city updated");
        return profile(user);
    }

    /** Changes the password and signs out every OTHER session of the account. */
    @Transactional
    public void changePassword(AuthUser current, ChangePasswordRequest r) {
        UserAccount user = load(current);
        if (!encoder.matches(r.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("currentPassword: The current password is not correct");
        }
        if (encoder.matches(r.password(), user.getPasswordHash())) {
            throw new ValidationException("password: The new password must be different from the current one");
        }
        user.changePasswordHash(encoder.encode(r.password()));
        users.save(user);
        for (AuthSession s : sessions.findByUserIdAndRevokedAtIsNull(user.getId())) {
            if (!s.getId().equals(current.sessionId())) {
                s.revoke(now());
                sessions.save(s);
            }
        }
        audit.record("PASSWORD_CHANGED", "Password changed; other sessions signed out");
    }

    private void revokeAllSessions(Long userId) {
        for (AuthSession s : sessions.findByUserIdAndRevokedAtIsNull(userId)) {
            s.revoke(now());
            sessions.save(s);
        }
    }

    private UserAccount load(AuthUser user) {
        return users.findById(user.id()).orElseThrow(() -> new AuthenticationFailedException("Account not found"));
    }

    public Optional<UserAccount> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email);
    }

    /** The profile DTO, with the subclass-specific fields filled in (polymorphism via pattern matching). */
    public static UserProfileView profile(UserAccount u) {
        List<String> perms = u.getPermissions().stream().map(Enum::name).sorted().toList();
        if (u instanceof PassengerUser p) {
            return new UserProfileView(u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getRole().name(),
                    u.getDisplayTitle(), perms, p.getDateOfBirth(), p.getGender().name(), p.getCity(), null, null,
                    u.getCreatedAt(), u.getLastLoginAt());
        }
        StationStaffUser s = (StationStaffUser) u;
        return new UserProfileView(u.getId(), u.getFullName(), u.getEmail(), u.getMobile(), u.getRole().name(),
                u.getDisplayTitle(), perms, null, null, null, s.getEmployeeId(), s.getDesignation().name(),
                u.getCreatedAt(), u.getLastLoginAt());
    }

    private static String roleOf(UserAccount u) {
        return u instanceof StationStaffUser s ? s.getDesignation().name() : u.getRole().name();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
