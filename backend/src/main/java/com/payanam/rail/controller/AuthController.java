package com.payanam.rail.controller;

import com.payanam.rail.dto.MessageResponse;
import com.payanam.rail.dto.auth.AuthResponse;
import com.payanam.rail.dto.auth.ChangePasswordRequest;
import com.payanam.rail.dto.auth.ForgotPasswordRequest;
import com.payanam.rail.dto.auth.ForgotPasswordResponse;
import com.payanam.rail.dto.auth.PassengerSignUpRequest;
import com.payanam.rail.dto.auth.ResetPasswordRequest;
import com.payanam.rail.dto.auth.SignInRequest;
import com.payanam.rail.dto.auth.StaffSignUpRequest;
import com.payanam.rail.dto.auth.UpdateProfileRequest;
import com.payanam.rail.dto.auth.UserProfileView;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sign up, sign in, logout, profile and the simulated password reset. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Accounts, JWT sign-in, profile")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/signup/passenger")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a passenger (18+, unique email and mobile, strong password)")
    public AuthResponse signUpPassenger(@Valid @RequestBody PassengerSignUpRequest request,
                                        @RequestHeader(value = "User-Agent", required = false) String agent) {
        return auth.signUpPassenger(request, agent);
    }

    @PostMapping("/signup/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register station staff (needs the station access code)")
    public AuthResponse signUpStaff(@Valid @RequestBody StaffSignUpRequest request,
                                    @RequestHeader(value = "User-Agent", required = false) String agent) {
        return auth.signUpStaff(request, agent);
    }

    @PostMapping("/signin")
    @Operation(summary = "Sign in with email or mobile; 5 wrong passwords lock the account for 15 minutes")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest request,
                               @RequestHeader(value = "User-Agent", required = false) String agent) {
        return auth.signIn(request, agent);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session (the token stops working)")
    public MessageResponse logout(@AuthenticationPrincipal AuthUser user) {
        auth.logout(user);
        return new MessageResponse("Signed out");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "SIMULATED: generates a 6-digit OTP and returns it (no e-mail service)")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return auth.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset the password with the OTP")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        auth.resetPassword(request);
        return new MessageResponse("Password updated. Please sign in with the new password.");
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in account")
    public UserProfileView me(@AuthenticationPrincipal AuthUser user) {
        return auth.me(user);
    }

    @PutMapping("/profile")
    @Operation(summary = "Edit name, mobile and (passengers) city")
    public UserProfileView updateProfile(@AuthenticationPrincipal AuthUser user,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return auth.updateProfile(user, request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change the password (signs out other sessions)")
    public MessageResponse changePassword(@AuthenticationPrincipal AuthUser user,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(user, request);
        return new MessageResponse("Password changed");
    }
}
