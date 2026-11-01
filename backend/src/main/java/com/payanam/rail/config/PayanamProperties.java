package com.payanam.rail.config;

import com.payanam.rail.domain.policy.AllocationPolicyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

/**
 * Typed view of the {@code payanam.*} settings in application.yml.
 *
 * @param station  station and simulation settings
 * @param security authentication settings
 */
@ConfigurationProperties(prefix = "payanam")
public record PayanamProperties(Station station, Security security) {

    /**
     * @param accessCode             code that staff must enter to register (e.g. PAYANAM-2026)
     * @param clearanceBufferMinutes minutes added after each departure before the platform is free
     * @param clockStart             simulation start time on the service day
     * @param tickSeconds            real seconds between automatic clock steps while "playing"
     * @param playStepMinutes        simulated minutes added per automatic step
     */
    public record Station(String name, String accessCode, int clearanceBufferMinutes,
                          AllocationPolicyType allocationPolicy, LocalTime clockStart, int tickSeconds,
                          int playStepMinutes) {
    }

    /**
     * @param jwtSecret         Base64 HMAC key (at least 256 bits)
     * @param staffEmailDomain  official e-mail domain required for staff accounts
     */
    public record Security(String jwtSecret, int jwtExpiryMinutes, int rememberMeDays, int maxFailedAttempts,
                           int lockoutMinutes, int otpExpiryMinutes, String staffEmailDomain) {
    }
}
