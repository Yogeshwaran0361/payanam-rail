package com.payanam.rail.service;

import com.payanam.rail.config.PayanamProperties;
import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.domain.policy.AllocationPolicyType;
import com.payanam.rail.domain.user.PassengerUser;
import com.payanam.rail.dto.auth.SignInRequest;
import com.payanam.rail.exception.AccountLockedException;
import com.payanam.rail.exception.AuthenticationFailedException;
import com.payanam.rail.repository.AuthSessionRepository;
import com.payanam.rail.repository.PasswordResetRequestRepository;
import com.payanam.rail.repository.StationStaffUserRepository;
import com.payanam.rail.repository.UserAccountRepository;
import com.payanam.rail.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Sign-in rules tested in isolation: repositories, encoder, JWT and audit are Mockito mocks. */
@ExtendWith(MockitoExtension.class)
class AuthServiceMockitoTest {
    @Mock
    UserAccountRepository users;
    @Mock
    StationStaffUserRepository staff;
    @Mock
    AuthSessionRepository sessions;
    @Mock
    PasswordResetRequestRepository resets;
    @Mock
    PasswordEncoder encoder;
    @Mock
    JwtService jwt;
    @Mock
    AuditService audit;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-05T06:00:00Z"), ZoneId.of("UTC"));
    private AuthService service;
    private PassengerUser user;

    @BeforeEach
    void setUp() {
        PayanamProperties props = new PayanamProperties(
                new PayanamProperties.Station("Payanam Junction", "PAYANAM-2026", 10, AllocationPolicyType.BEST_FIT,
                        LocalTime.of(6, 30), 2, 1),
                new PayanamProperties.Security("c2VjcmV0", 60, 7, 5, 15, 10, "payanam.in"));
        service = new AuthService(users, staff, sessions, resets, encoder, jwt, audit, props, clock);
        user = new PassengerUser("Asha", "asha@example.in", "9000000001", "HASH", LocalDate.of(1990, 1, 1),
                Gender.FEMALE, "Chennai", LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("AUTH (Mockito): the fifth wrong password locks the account and is audited")
    void fifthFailureLocks() {
        when(users.findByEmailIgnoreCase("asha@example.in")).thenReturn(Optional.of(user));
        when(encoder.matches(anyString(), eq("HASH"))).thenReturn(false);
        SignInRequest wrong = new SignInRequest("asha@example.in", "bad", false);
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.signIn(wrong, "junit")).isInstanceOf(AuthenticationFailedException.class);
        }
        assertThatThrownBy(() -> service.signIn(wrong, "junit")).isInstanceOf(AccountLockedException.class);
        assertThat(user.isLocked(LocalDateTime.now(clock))).isTrue();
        assertThat(user.isLocked(LocalDateTime.now(clock).plusMinutes(16))).isFalse();
        verify(audit).recordAs(eq("asha@example.in"), eq("PASSENGER"), eq("ACCOUNT_LOCKED"), anyString(), eq(false));
        verify(jwt, never()).createToken(any(), any(), any());
    }

    @Test
    @DisplayName("AUTH (Mockito): sign-in by mobile uses findByMobile and issues a token")
    void signInByMobile() {
        when(users.findByMobile("9000000001")).thenReturn(Optional.of(user));
        when(encoder.matches("Right@123", "HASH")).thenReturn(true);
        when(jwt.createToken(any(), any(), any())).thenReturn("jwt-token");
        assertThat(service.signIn(new SignInRequest("9000000001", "Right@123", true), "junit").token())
                .isEqualTo("jwt-token");
        verify(sessions).save(any());
        verify(audit).recordAs(eq("asha@example.in"), eq("PASSENGER"), eq("SIGN_IN"), contains("remember me"), eq(true));
    }

    @Test
    @DisplayName("AUTH (Mockito): an unknown account gives the same generic error")
    void unknownAccount() {
        when(users.findByEmailIgnoreCase("ghost@example.in")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.signIn(new SignInRequest("ghost@example.in", "x", false), "junit"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email/mobile or password");
    }
}
