package com.payanam.rail.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Sign in with an email OR a mobile number, plus the password. */
public record SignInRequest(
        @NotBlank(message = "Enter your email or mobile number") String identifier,
        @NotBlank(message = "Enter your password") String password,
        boolean rememberMe) {
}
