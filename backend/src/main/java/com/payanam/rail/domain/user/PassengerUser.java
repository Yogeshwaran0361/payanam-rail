package com.payanam.rail.domain.user;

import com.payanam.rail.domain.model.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** A member of the public who books tickets. Has no staff permissions at all. */
@Entity
@DiscriminatorValue("PASSENGER")
public class PassengerUser extends UserAccount {

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 60)
    private String city;

    protected PassengerUser() {
        // for JPA
    }

    public PassengerUser(String fullName, String email, String mobile, String passwordHash, LocalDate dateOfBirth,
                         Gender gender, String city, LocalDateTime createdAt) {
        super(fullName, email, mobile, passwordHash, createdAt);
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.city = city.trim();
    }

    @Override
    public Role getRole() {
        return Role.PASSENGER;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.noneOf(Permission.class);
    }

    @Override
    public String getDisplayTitle() {
        return "Passenger";
    }

    public void updateCity(String newCity) {
        this.city = newCity.trim();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public String getCity() {
        return city;
    }
}
