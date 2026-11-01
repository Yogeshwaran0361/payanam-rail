package com.payanam.rail.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Profile edit: name, mobile and (passengers only) city. */
public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required") @Size(min = 2, max = 80)
        @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$", message = "Name can contain letters, spaces, . ' and - only")
        String fullName,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a 10-digit Indian mobile number starting with 6-9")
        String mobile,
        @Size(max = 60) String city) {
}
