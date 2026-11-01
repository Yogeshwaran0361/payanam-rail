package com.payanam.rail.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    private Map<String, Object> passengerForm(String email, String mobile) {
        Map<String, Object> form = new HashMap<>();
        form.put("fullName", "Test Passenger");
        form.put("email", email);
        form.put("mobile", mobile);
        form.put("dateOfBirth", LocalDate.now().minusYears(25).toString());
        form.put("gender", "FEMALE");
        form.put("city", "Madurai");
        form.put("password", "Secret@123");
        form.put("confirmPassword", "Secret@123");
        form.put("acceptTerms", true);
        return form;
    }

    private Map<String, Object> staffForm(String email, String employeeId, String code) {
        Map<String, Object> form = new HashMap<>();
        form.put("fullName", "New Clerk");
        form.put("employeeId", employeeId);
        form.put("designation", "BOOKING_CLERK");
        form.put("email", email);
        form.put("mobile", "9" + (100000000 + Math.abs(email.hashCode() % 899999999)));
        form.put("password", "Secret@123");
        form.put("confirmPassword", "Secret@123");
        form.put("accessCode", code);
        return form;
    }

    @Test
    @DisplayName("AUTH: a valid passenger sign-up returns a JWT and the profile")
    void signUpPassenger() throws Exception {
        JsonNode body = read(postAs(null, "/api/v1/auth/signup/passenger", passengerForm("new.one@example.in", "9123400001"))
                .andExpect(status().isCreated()));
        assertThat(body.get("token").asText()).isNotBlank();
        assertThat(body.at("/user/role").asText()).isEqualTo("PASSENGER");
        assertThat(body.at("/user/city").asText()).isEqualTo("Madurai");
    }

    @Test
    @DisplayName("AUTH: sign-up validation – weak password, under 18, bad mobile, mismatch, terms")
    void signUpValidation() throws Exception {
        Map<String, Object> form = passengerForm("bad@example.in", "12345");
        form.put("password", "password");
        form.put("confirmPassword", "different");
        form.put("dateOfBirth", LocalDate.now().minusYears(17).toString());
        form.put("acceptTerms", false);
        JsonNode body = read(postAs(null, "/api/v1/auth/signup/passenger", form).andExpect(status().isBadRequest()));
        assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.get("fieldErrors").findValuesAsText("field"))
                .contains("password", "confirmPassword", "dateOfBirth", "mobile", "acceptTerms");
    }

    @Test
    @DisplayName("AUTH: a duplicate email or mobile is refused")
    void duplicate() throws Exception {
        postAs(null, "/api/v1/auth/signup/passenger", passengerForm(PASSENGER, "9123400002"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        postAs(null, "/api/v1/auth/signup/passenger", passengerForm("x1@example.in", "9876543210"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("mobile"));
    }

    @Test
    @DisplayName("AUTH: staff registration needs the access code, PJ-EMP-XXXX id and an official email")
    void staffSignUp() throws Exception {
        postAs(null, "/api/v1/auth/signup/staff", staffForm("c1@payanam.in", "PJ-EMP-2001", "WRONG"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("accessCode"));
        postAs(null, "/api/v1/auth/signup/staff", staffForm("c2@payanam.in", "EMP-2002", "PAYANAM-2026"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("employeeId"));
        postAs(null, "/api/v1/auth/signup/staff", staffForm("c3@gmail.com", "PJ-EMP-2003", "PAYANAM-2026"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        postAs(null, "/api/v1/auth/signup/staff", staffForm("c4@payanam.in", "PJ-EMP-1001", "PAYANAM-2026"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("employeeId"));
        JsonNode ok = read(postAs(null, "/api/v1/auth/signup/staff", staffForm("c5@payanam.in", "PJ-EMP-2005", "PAYANAM-2026"))
                .andExpect(status().isCreated()));
        assertThat(ok.at("/user/designation").asText()).isEqualTo("BOOKING_CLERK");
        assertThat(ok.at("/user/permissions").toString()).contains("MANAGE_RESERVATIONS").doesNotContain("MANAGE_PLATFORMS");
    }

    @Test
    @DisplayName("AUTH: sign in with email or with mobile")
    void signInEmailOrMobile() throws Exception {
        assertThat(token(PASSENGER, PASSENGER_PW)).isNotBlank();
        assertThat(token("9876543210", PASSENGER_PW)).isNotBlank();
        getAs(token(MASTER, STAFF_PW), "/api/v1/auth/me").andExpect(jsonPath("$.displayTitle").value("Station Master"));
    }

    @Test
    @DisplayName("AUTH: a wrong password is refused with the attempts left")
    void wrongPassword() throws Exception {
        postAs(null, "/api/v1/auth/signin", Map.of("identifier", CLERK, "password", "Nope@1234", "rememberMe", false))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("4 attempts left")));
        postAs(null, "/api/v1/auth/signin", Map.of("identifier", "nobody@example.in", "password", "x", "rememberMe", false))
                .andExpect(status().isUnauthorized());
        token(CLERK, STAFF_PW);                // a success resets the counter
    }

    @Test
    @DisplayName("AUTH: 5 wrong passwords lock the account for 15 minutes (even the right password is refused)")
    void lockout() throws Exception {
        postAs(null, "/api/v1/auth/signup/passenger", passengerForm("lock.me@example.in", "9123400003"))
                .andExpect(status().isCreated());
        for (int i = 0; i < 4; i++) {
            postAs(null, "/api/v1/auth/signin", Map.of("identifier", "lock.me@example.in", "password", "Wrong@123",
                    "rememberMe", false)).andExpect(status().isUnauthorized());
        }
        postAs(null, "/api/v1/auth/signin", Map.of("identifier", "lock.me@example.in", "password", "Wrong@123",
                "rememberMe", false)).andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        postAs(null, "/api/v1/auth/signin", Map.of("identifier", "lock.me@example.in", "password", "Secret@123",
                "rememberMe", false)).andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("15 more minutes")));
    }

    @Test
    @DisplayName("F4 / ROLE: a passenger gets 403 on staff APIs and staff get 403 on passenger APIs")
    void roleSeparation() throws Exception {
        String passenger = token(PASSENGER, PASSENGER_PW);
        String master = token(MASTER, STAFF_PW);
        getAs(passenger, "/api/v1/platforms").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        getAs(passenger, "/api/v1/operations/dashboard").andExpect(status().isForbidden());
        getAs(passenger, "/api/v1/audit").andExpect(status().isForbidden());
        getAs(master, "/api/v1/bookings").andExpect(status().isForbidden());
        getAs(null, "/api/v1/bookings").andExpect(status().isUnauthorized());
        getAs("not-a-jwt", "/api/v1/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE: designations limit staff – a clerk cannot manage platforms, a controller cannot read the audit log")
    void designationPermissions() throws Exception {
        String clerk = token(CLERK, STAFF_PW);
        String controller = token(CONTROLLER, STAFF_PW);
        getAs(clerk, "/api/v1/platforms/board").andExpect(status().isForbidden());
        getAs(clerk, "/api/v1/operations/reservations/trains/12635").andExpect(status().isOk());
        getAs(controller, "/api/v1/platforms/board").andExpect(status().isOk());
        getAs(controller, "/api/v1/audit").andExpect(status().isForbidden());
        getAs(controller, "/api/v1/operations/reservations/trains/12635").andExpect(status().isForbidden());
        getAs(token(MASTER, STAFF_PW), "/api/v1/audit").andExpect(status().isOk());
    }

    @Test
    @DisplayName("AUTH: logout revokes the token immediately")
    void logout() throws Exception {
        String t = token(PASSENGER, PASSENGER_PW);
        getAs(t, "/api/v1/auth/me").andExpect(status().isOk());
        postAs(t, "/api/v1/auth/logout", null).andExpect(status().isOk());
        getAs(t, "/api/v1/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH: simulated forgot password – OTP shown, wrong OTP refused, reset works")
    void forgotPassword() throws Exception {
        postAs(null, "/api/v1/auth/signup/passenger", passengerForm("forgot.me@example.in", "9123400004"))
                .andExpect(status().isCreated());
        JsonNode otp = read(postAs(null, "/api/v1/auth/forgot-password", Map.of("identifier", "forgot.me@example.in"))
                .andExpect(status().isOk()));
        String code = otp.get("simulatedOtp").asText();
        assertThat(code).matches("\\d{6}");
        String wrong = code.equals("000000") ? "111111" : "000000";
        postAs(null, "/api/v1/auth/reset-password", Map.of("identifier", "forgot.me@example.in", "otp", wrong,
                "password", "Fresh@1234", "confirmPassword", "Fresh@1234")).andExpect(status().isBadRequest());
        postAs(null, "/api/v1/auth/reset-password", Map.of("identifier", "forgot.me@example.in", "otp", code,
                "password", "Fresh@1234", "confirmPassword", "Fresh@1234")).andExpect(status().isOk());
        assertThat(token("forgot.me@example.in", "Fresh@1234")).isNotBlank();
        JsonNode unknown = read(postAs(null, "/api/v1/auth/forgot-password", Map.of("identifier", "ghost@example.in")));
        assertThat(unknown.get("simulatedOtp").isNull()).isTrue();
    }

    @Test
    @DisplayName("AUTH: profile edit and change password")
    void profileAndPassword() throws Exception {
        postAs(null, "/api/v1/auth/signup/passenger", passengerForm("profile.me@example.in", "9123400005"))
                .andExpect(status().isCreated());
        String t = token("profile.me@example.in", "Secret@123");
        putAs(t, "/api/v1/auth/profile", Map.of("fullName", "Renamed Person", "mobile", "9123400006", "city", "Salem"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Salem"));
        putAs(t, "/api/v1/auth/profile", Map.of("fullName", "Renamed Person", "mobile", "9876543210", "city", "Salem"))
                .andExpect(status().isBadRequest());
        putAs(t, "/api/v1/auth/password", Map.of("currentPassword", "Wrong@123", "password", "Next@12345",
                "confirmPassword", "Next@12345")).andExpect(status().isBadRequest());
        putAs(t, "/api/v1/auth/password", Map.of("currentPassword", "Secret@123", "password", "Next@12345",
                "confirmPassword", "Next@12345")).andExpect(status().isOk());
        assertThat(token("9123400006", "Next@12345")).isNotBlank();
    }

    @Test
    @DisplayName("AUTH: remember me gives a longer-lived token")
    void rememberMe() throws Exception {
        JsonNode shortLived = read(mvc.perform(post("/api/v1/auth/signin").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("identifier", PASSENGER, "password", PASSENGER_PW, "rememberMe", false)))));
        JsonNode longLived = read(mvc.perform(post("/api/v1/auth/signin").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("identifier", PASSENGER, "password", PASSENGER_PW, "rememberMe", true)))));
        assertThat(java.time.Instant.parse(longLived.get("expiresAt").asText()))
                .isAfter(java.time.Instant.parse(shortLived.get("expiresAt").asText()).plusSeconds(3600 * 24));
    }
}
