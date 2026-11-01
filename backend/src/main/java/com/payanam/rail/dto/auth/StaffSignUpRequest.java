package com.payanam.rail.dto.auth;

import com.payanam.rail.domain.user.StaffDesignation;
import com.payanam.rail.dto.validation.PasswordPair;
import com.payanam.rail.dto.validation.PasswordsMatch;
import com.payanam.rail.dto.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Station staff registration. Refused without the station access code from application.yml. */
@PasswordsMatch
public record StaffSignUpRequest(
        @NotBlank(message = "Full name is required") @Size(min = 2, max = 80, message = "Name must be 2-80 characters")
        @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$", message = "Name can contain letters, spaces, . ' and - only")
        String fullName,
        @NotBlank(message = "Employee ID is required")
        @Pattern(regexp = "^PJ-EMP-\\d{4}$", message = "Employee ID must look like PJ-EMP-1234")
        String employeeId,
        @NotNull(message = "Designation is required")
        StaffDesignation designation,
        @NotBlank(message = "Official email is required") @Email(message = "Enter a valid email address") @Size(max = 120)
        String email,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a 10-digit Indian mobile number starting with 6-9")
        String mobile,
        @NotBlank(message = "Password is required") @StrongPassword
        String password,
        @NotBlank(message = "Please confirm the password")
        String confirmPassword,
        @NotBlank(message = "Station access code is required")
        String accessCode) implements PasswordPair {
}
