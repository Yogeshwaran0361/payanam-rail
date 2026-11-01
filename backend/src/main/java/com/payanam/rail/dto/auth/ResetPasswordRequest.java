package com.payanam.rail.dto.auth;

import com.payanam.rail.dto.validation.PasswordPair;
import com.payanam.rail.dto.validation.PasswordsMatch;
import com.payanam.rail.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Step 2 of the simulated reset: the 6-digit OTP and the new password. */
@PasswordsMatch
public record ResetPasswordRequest(
        @NotBlank(message = "Enter your email or mobile number") String identifier,
        @NotBlank(message = "Enter the OTP") @Pattern(regexp = "^\\d{6}$", message = "The OTP has 6 digits") String otp,
        @NotBlank(message = "Password is required") @StrongPassword String password,
        @NotBlank(message = "Please confirm the password") String confirmPassword) implements PasswordPair {
}
