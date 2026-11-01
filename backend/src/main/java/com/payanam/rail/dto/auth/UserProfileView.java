package com.payanam.rail.dto.auth;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The signed-in account as the UI needs it. Passenger-only fields (dateOfBirth, gender, city) and
 * staff-only fields (employeeId, designation) are null for the other role.
 */
public record UserProfileView(Long id, String fullName, String email, String mobile, String role,
                              String displayTitle, List<String> permissions, LocalDate dateOfBirth, String gender,
                              String city, String employeeId, String designation, LocalDateTime createdAt,
                              LocalDateTime lastLoginAt) {
}
