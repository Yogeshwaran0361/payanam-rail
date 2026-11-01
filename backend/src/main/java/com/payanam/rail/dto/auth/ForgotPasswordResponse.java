package com.payanam.rail.dto.auth;

/**
 * SIMULATED e-mail: because the project has no e-mail service, the OTP is returned here and shown on
 * screen (and written to the server log). It is null when the account does not exist.
 */
public record ForgotPasswordResponse(String message, String simulatedOtp, int expiresInMinutes) {
}
