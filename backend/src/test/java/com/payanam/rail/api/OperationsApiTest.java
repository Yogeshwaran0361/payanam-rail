package com.payanam.rail.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationsApiTest extends ApiTestSupport {

    @Test
    @DisplayName("Board: seed allocations – Vaigai P2, Guruvayur P1, Tejas P3, Relief Special queued")
    void board() throws Exception {
        JsonNode board = read(getAs(token(CONTROLLER, STAFF_PW), "/api/v1/platforms/board").andExpect(status().isOk()));
        assertThat(board.get("platforms")).hasSize(3);
        assertThat(board.at("/platforms/0/blocks/0/trainNo").asText()).isEqualTo("16127");
        assertThat(board.at("/platforms/1/blocks/0/trainNo").asText()).isEqualTo("12635");
        assertThat(board.at("/platforms/2/blocks/0/trainNo").asText()).isEqualTo("22671");
        assertThat(board.at("/waiting/0/trainNo").asText()).isEqualTo("06055");
        assertThat(board.at("/waiting/0/type").asText()).isEqualTo("EMERGENCY");
    }

    @Test
    @DisplayName("P3/P4 API: Pearl City queues behind the emergency; Pandian is rejected as overlong")
    void arrivalRequests() throws Exception {
        String t = token(CONTROLLER, STAFF_PW);
        JsonNode pearl = read(postAs(t, "/api/v1/operations/arrival-requests", Map.of("trainNo", "12693"))
                .andExpect(status().isOk()));
        assertThat(pearl.get("outcome").asText()).isEqualTo("QUEUED");
        assertThat(pearl.get("queuePosition").asInt()).isEqualTo(2);
        JsonNode pandian = read(postAs(t, "/api/v1/operations/arrival-requests", Map.of("trainNo", "12637")));
        assertThat(pandian.get("outcome").asText()).isEqualTo("REJECTED");
        assertThat(pandian.get("message").asText()).contains("Overlong train – exceeds all platform lengths");
        getAs(t, "/api/v1/platforms/queue").andExpect(jsonPath("$[0].trainNo").value("06055"))
                .andExpect(jsonPath("$[1].trainNo").value("12693"));
        postAs(t, "/api/v1/operations/arrival-requests", Map.of("trainNo", "12693")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("P7 API: the delay preview changes nothing; the real delay bumps the later train and notifies passengers")
    void delayPreviewAndApply() throws Exception {
        String t = token(CONTROLLER, STAFF_PW);
        String arun = token(PASSENGER, PASSENGER_PW);
        JsonNode preview = read(postAs(t, "/api/v1/operations/trains/12635/delay/preview",
                Map.of("minutes", 20, "reason", "Signal failure")).andExpect(status().isOk()));
        assertThat(preview.get("preview").asBoolean()).isTrue();
        assertThat(preview.get("occupationExtended").asBoolean()).isTrue();
        assertThat(read(getAs(null, "/api/v1/trains/12635/live")).get("delayMinutes").asInt()).isZero();
        assertThat(read(getAs(arun, "/api/v1/notifications")).findValuesAsText("type")).doesNotContain("TRAIN");

        JsonNode applied = read(postAs(t, "/api/v1/operations/trains/12635/delay",
                Map.of("minutes", 20, "reason", "Signal failure")).andExpect(status().isOk()));
        assertThat(applied.get("preview").asBoolean()).isFalse();
        assertThat(read(getAs(null, "/api/v1/trains/12635/live")).get("delayMinutes").asInt()).isEqualTo(20);
        assertThat(read(getAs(arun, "/api/v1/notifications")).findValuesAsText("title")).contains("Delay: 12635 Vaigai Express");
        postAs(t, "/api/v1/operations/trains/12635/delay", Map.of("minutes", 0)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("P8 API: arrival, departure preview and departure release the platform")
    void arrivalAndDeparture() throws Exception {
        String t = token(MASTER, STAFF_PW);
        postAs(t, "/api/v1/operations/trains/12635/arrival", null).andExpect(status().isConflict());   // not due yet
        postAs(t, "/api/v1/simulation/advance", Map.of("minutes", 30)).andExpect(jsonPath("$.now").exists());
        JsonNode arrival = read(postAs(t, "/api/v1/operations/trains/12635/arrival", null).andExpect(status().isOk()));
        assertThat(arrival.get("message").asText()).contains("arrived on P2");
        JsonNode preview = read(postAs(t, "/api/v1/operations/trains/12635/departure/preview", null));
        assertThat(preview.get("preview").asBoolean()).isTrue();
        assertThat(read(getAs(null, "/api/v1/trains/12635/live")).get("status").asText()).isEqualTo("ARRIVED");
        JsonNode dep = read(postAs(t, "/api/v1/operations/trains/12635/departure", null).andExpect(status().isOk()));
        assertThat(dep.get("message").asText()).contains("is released at");
        assertThat(dep.get("events").size()).isPositive();
    }

    @Test
    @DisplayName("P9 API: maintenance on P1 re-allocates Guruvayur; platform length is validated")
    void maintenanceAndLength() throws Exception {
        String t = token(CONTROLLER, STAFF_PW);
        JsonNode r = read(postAs(t, "/api/v1/platforms/P1/maintenance", Map.of("maintenance", true)).andExpect(status().isOk()));
        assertThat(r.get("alerts").get(0).asText()).contains("16127");
        getAs(t, "/api/v1/platforms").andExpect(jsonPath("$[0].status").value("MAINTENANCE"));
        putAs(t, "/api/v1/platforms/P2", Map.of("lengthMeters", 300)).andExpect(status().isConflict());
        putAs(t, "/api/v1/platforms/P3", Map.of("lengthMeters", 350)).andExpect(status().isOk());
        postAs(t, "/api/v1/platforms/P1/maintenance", Map.of("maintenance", false)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dashboard, events, settings, simulation and snapshots for the Station Master")
    void stationMasterTools() throws Exception {
        String t = token(MASTER, STAFF_PW);
        JsonNode dash = read(getAs(t, "/api/v1/operations/dashboard").andExpect(status().isOk()));
        assertThat(dash.at("/kpis/trainsToday").asInt()).isEqualTo(6);
        assertThat(dash.at("/kpis/totalBookings").asInt()).isEqualTo(32);
        assertThat(dash.get("seatOccupancy")).hasSize(6);
        assertThat(dash.get("bookingsOverTime")).hasSize(7);
        getAs(t, "/api/v1/operations/events?limit=5").andExpect(jsonPath("$.length()").value(5));

        putAs(t, "/api/v1/admin/settings", Map.of("allocationPolicy", "FIRST_FIT", "clearanceBufferMinutes", 5))
                .andExpect(jsonPath("$.allocationPolicy").value("FIRST_FIT"));
        putAs(t, "/api/v1/admin/settings", Map.of("allocationPolicy", "BEST_FIT", "clearanceBufferMinutes", 10))
                .andExpect(status().isOk());

        postAs(t, "/api/v1/simulation/play", null).andExpect(jsonPath("$.running").value(true));
        postAs(t, "/api/v1/simulation/pause", null).andExpect(jsonPath("$.running").value(false));
        postAs(t, "/api/v1/simulation/snapshots", Map.of("label", "Before demo")).andExpect(status().isOk());
        getAs(t, "/api/v1/simulation/snapshots").andExpect(jsonPath("$[0].label").value("Before demo"));
        postAs(t, "/api/v1/simulation/reset-system", null).andExpect(status().isOk());
        getAs(null, "/api/v1/trains").andExpect(jsonPath("$.length()").value(0));
        postAs(t, "/api/v1/simulation/reset-clock", null).andExpect(status().isOk());
        getAs(null, "/api/v1/trains").andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("Timetable management: edit a SCHEDULED train; a 7th train is refused")
    void timetableManagement() throws Exception {
        String t = token(CONTROLLER, STAFF_PW);
        putAs(t, "/api/v1/trains/12693", Map.of("arrival", "08:10", "departure", "08:25", "priority", "CONNECTION"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("CONNECTION"));
        putAs(t, "/api/v1/trains/12635", Map.of("arrival", "08:10", "departure", "08:25", "priority", "NORMAL"))
                .andExpect(status().isConflict());
        postAs(t, "/api/v1/trains", Map.of("trainNo", "16101", "name", "Rameswaram Express", "origin", "Chennai Egmore",
                "destination", "Rameswaram", "arrival", "09:00", "departure", "09:15", "lengthMeters", 400,
                "priority", "NORMAL", "fare", 400)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Reservations management: clerk sees the FIFO list and cancels at the counter")
    void clerkReservations() throws Exception {
        String clerk = token(CLERK, STAFF_PW);
        JsonNode view = read(getAs(clerk, "/api/v1/operations/reservations/trains/12635").andExpect(status().isOk()));
        assertThat(view.get("confirmed")).hasSize(10);
        assertThat(view.at("/waiting/0/passengerName").asText()).isEqualTo("Arun Kumar");
        String pnr = view.at("/confirmed/0/pnr").asText();
        getAs(clerk, "/api/v1/operations/reservations/pnr/" + pnr).andExpect(status().isOk());
        JsonNode cancel = read(postAs(clerk, "/api/v1/operations/reservations/pnr/" + pnr + "/passengers/1/cancel", null)
                .andExpect(status().isOk()));
        assertThat(cancel.at("/promotions/0/to").asText()).isEqualTo("CNF/S1");
        JsonNode after = read(getAs(clerk, "/api/v1/operations/reservations/trains/12635"));
        assertThat(after.get("promotions")).hasSize(1);
        postAs(clerk, "/api/v1/operations/reservations/pnr/" + pnr + "/cancel", null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Audit log: actions are recorded, filterable and exportable as CSV")
    void auditLog() throws Exception {
        String t = token(MASTER, STAFF_PW);
        postAs(t, "/api/v1/simulation/advance", Map.of("minutes", 5)).andExpect(status().isOk());
        getAs(t, "/api/v1/audit?action=CLOCK_ADVANCED").andExpect(jsonPath("$.content[0].action").value("CLOCK_ADVANCED"));
        getAs(t, "/api/v1/audit?q=stationmaster").andExpect(jsonPath("$.totalElements").isNumber());
        getAs(t, "/api/v1/audit/actions").andExpect(status().isOk());
        getAs(t, "/api/v1/audit/export").andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("id,timestamp")));
    }

    @Test
    @DisplayName("Swagger: the OpenAPI document lists the versioned API")
    void openApi() throws Exception {
        JsonNode doc = read(getAs(null, "/v3/api-docs/all").andExpect(status().isOk()));
        assertThat(doc.get("paths").has("/api/v1/bookings")).isTrue();
        getAs(null, "/swagger-ui/index.html").andExpect(status().isOk());
    }
}
