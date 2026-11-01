package com.payanam.rail.dto.auth;

import java.time.Instant;

/** Returned after sign-up / sign-in: the JWT, when it expires, and the profile. */
public record AuthResponse(String token, Instant expiresAt, UserProfileView user) {
}
