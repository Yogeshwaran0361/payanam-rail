package com.payanam.rail.dto.auth;

import com.payanam.rail.dto.validation.PasswordPair;
import com.payanam.rail.dto.validation.PasswordsMatch;
import com.payanam.rail.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/** Change password from the profile page (the current password is required). */
@PasswordsMatch
public record ChangePasswordRequest(
        @NotBlank(message = "Enter your current password") String currentPassword,
        @NotBlank(message = "Password is required") @StrongPassword String password,
        @NotBlank(message = "Please confirm the password") String confirmPassword) implements PasswordPair {
}
