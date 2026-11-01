package com.payanam.rail.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test Scenarios Lab through the API. RANDOM_PORT starts a real server so F4 can make its HTTP call. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestLabApiTest extends ApiTestSupport {

    @ParameterizedTest(name = "{0} passes on its own isolated dataset")
    @ValueSource(strings = {"T1", "T2", "T3", "T4", "T5", "T6", "T7", "F1", "F2", "F3", "F4"})
    @DisplayName("T1–T7, F1–F4: every lab scenario passes")
    void scenarioPasses(String id) throws Exception {
        String t = token(MASTER, STAFF_PW);
        JsonNode result = read(postAs(t, "/api/v1/tests/" + id + "/run", null).andExpect(status().isOk()));
        assertThat(result.get("passed").asBoolean()).as(result.toString()).isTrue();
        assertThat(result.get("steps")).isNotEmpty();
        assertThat(result.get("actual").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Lab: run-all reports 11 PASS and never changes the live station")
    void runAllIsIsolated() throws Exception {
        String t = token(MASTER, STAFF_PW);
        getAs(t, "/api/v1/tests").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(11));
        JsonNode all = read(postAs(t, "/api/v1/tests/run-all", null).andExpect(status().isOk()));
        assertThat(all).hasSize(11).allMatch(r -> r.get("passed").asBoolean());
        assertThat(read(getAs(null, "/api/v1/trains/stats")).get("bookings").asInt()).isEqualTo(32);
        postAs(t, "/api/v1/tests/T9/run", null).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Lab: only staff with RUN_TESTS may use it (Booking Clerk and passengers get 403)")
    void labIsRestricted() throws Exception {
        getAs(token(CLERK, STAFF_PW), "/api/v1/tests").andExpect(status().isForbidden());
        getAs(token(PASSENGER, PASSENGER_PW), "/api/v1/tests").andExpect(status().isForbidden());
    }
}
