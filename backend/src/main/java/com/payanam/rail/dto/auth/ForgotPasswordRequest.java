package com.payanam.rail.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Step 1 of the simulated reset: which account. */
public record ForgotPasswordRequest(@NotBlank(message = "Enter your email or mobile number") String identifier) {
}
