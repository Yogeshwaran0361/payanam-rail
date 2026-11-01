package com.payanam.rail.dto.auth;

import com.payanam.rail.domain.model.Gender;
import com.payanam.rail.dto.validation.Adult;
import com.payanam.rail.dto.validation.PasswordPair;
import com.payanam.rail.dto.validation.PasswordsMatch;
import com.payanam.rail.dto.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Passenger registration form. Every rule here is also shown inline in the Sign Up page. */
@PasswordsMatch
public record PassengerSignUpRequest(
        @NotBlank(message = "Full name is required") @Size(min = 2, max = 80, message = "Name must be 2-80 characters")
        @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$", message = "Name can contain letters, spaces, . ' and - only")
        String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 120)
        String email,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a 10-digit Indian mobile number starting with 6-9")
        String mobile,
        @NotNull(message = "Date of birth is required") @Adult
        LocalDate dateOfBirth,
        @NotNull(message = "Gender is required")
        Gender gender,
        @NotBlank(message = "City is required") @Size(max = 60)
        String city,
        @NotBlank(message = "Password is required") @StrongPassword
        String password,
        @NotBlank(message = "Please confirm the password")
        String confirmPassword,
        @AssertTrue(message = "You must accept the terms of use")
        boolean acceptTerms) implements PasswordPair {
}
