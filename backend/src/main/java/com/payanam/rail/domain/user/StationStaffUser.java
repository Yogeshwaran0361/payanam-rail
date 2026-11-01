package com.payanam.rail.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;
import java.util.Set;

/** A Payanam Junction employee. Permissions depend on the designation (role-based behaviour). */
@Entity
@DiscriminatorValue("STAFF")
public class StationStaffUser extends UserAccount {

    @Column(unique = true, length = 11)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StaffDesignation designation;

    protected StationStaffUser() {
        // for JPA
    }

    public StationStaffUser(String fullName, String email, String mobile, String passwordHash, String employeeId,
                            StaffDesignation designation, LocalDateTime createdAt) {
        super(fullName, email, mobile, passwordHash, createdAt);
        this.employeeId = employeeId.trim().toUpperCase();
        this.designation = designation;
    }

    @Override
    public Role getRole() {
        return Role.STATION_STAFF;
    }

    @Override
    public Set<Permission> getPermissions() {
        return designation.permissions();
    }

    @Override
    public String getDisplayTitle() {
        return designation.title();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public StaffDesignation getDesignation() {
        return designation;
    }
}
