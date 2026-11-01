package com.payanam.rail.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingApiTest extends ApiTestSupport {

    private Map<String, Object> booking(String trainNo, int people) {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int i = 1; i <= people; i++) {
            list.add(Map.of("name", "Traveller " + i, "age", 30 + i, "gender", "MALE"));
        }
        return Map.of("trainNo", trainNo, "passengers", list);
    }

    @Test
    @DisplayName("SEED: 6 trains, 32 PNRs, Vaigai and Pearl City full with waiting lists")
    void seed() throws Exception {
        JsonNode trains = read(getAs(null, "/api/v1/trains").andExpect(status().isOk()));
        assertThat(trains).hasSize(6);
        assertThat(trains.findValuesAsText("trainNo")).containsExactly("12635", "16127", "06055", "12693", "22671", "12637");
        getAs(null, "/api/v1/trains/stats").andExpect(jsonPath("$.bookings").value(32));
        JsonNode vaigai = read(getAs(null, "/api/v1/trains/12635/live"));
        assertThat(vaigai.get("seatsLeft").asInt()).isZero();
        assertThat(vaigai.get("waiting").asInt()).isEqualTo(4);
        assertThat(read(getAs(null, "/api/v1/trains/12693/live")).get("waiting").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("R1/R2 API: booking 2 passengers on a train with seats gives CNF seats; on a full train WL")
    void book() throws Exception {
        String t = token(PASSENGER, PASSENGER_PW);
        JsonNode ok = read(postAs(t, "/api/v1/bookings", booking("22671", 2)).andExpect(status().isCreated()));
        assertThat(ok.at("/passengers/0/label").asText()).startsWith("CNF/S");
        assertThat(ok.at("/passengers/1/label").asText()).startsWith("CNF/S");
        JsonNode wl = read(postAs(t, "/api/v1/bookings", booking("12635", 1)).andExpect(status().isCreated()));
        assertThat(wl.at("/passengers/0/label").asText()).isEqualTo("WL/5");
        getAs(t, "/api/v1/bookings").andExpect(jsonPath("$.length()").value(6));
        getAs(t, "/api/v1/notifications").andExpect(jsonPath("$[0].type").value("BOOKING"));
    }

    @Test
    @DisplayName("R2 API: 5 passengers or an invalid passenger are rejected")
    void bookingValidation() throws Exception {
        String t = token(PASSENGER, PASSENGER_PW);
        postAs(t, "/api/v1/bookings", booking("22671", 5)).andExpect(status().isBadRequest());
        postAs(t, "/api/v1/bookings", Map.of("trainNo", "22671", "passengers",
                List.of(Map.of("name", "", "age", 0, "gender", "MALE")))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F3 API: booking an unknown train returns 404 TRAIN_NOT_FOUND")
    void unknownTrain() throws Exception {
        postAs(token(PASSENGER, PASSENGER_PW), "/api/v1/bookings", booking("99999", 1))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TRAIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("T1 / R3 API: a cancellation promotes the oldest WL passenger and notifies them")
    void cancelPromotes() throws Exception {
        String priya = token("priya.raman@example.in", PASSENGER_PW);
        JsonNode mine = read(getAs(priya, "/api/v1/bookings"));
        String vaigaiPnr = null;
        for (JsonNode b : mine) {
            if (b.get("trainNo").asText().equals("12635")) {
                vaigaiPnr = b.get("pnr").asText();
            }
        }
        assertThat(vaigaiPnr).isNotBlank();
        JsonNode result = read(postAs(priya, "/api/v1/bookings/" + vaigaiPnr + "/passengers/1/cancel", null)
                .andExpect(status().isOk()));
        assertThat(result.at("/promotions/0/from").asText()).isEqualTo("WL/1");
        assertThat(result.at("/promotions/0/to").asText()).isEqualTo("CNF/S1");

        String arun = token(PASSENGER, PASSENGER_PW);
        JsonNode notes = read(getAs(arun, "/api/v1/notifications"));
        assertThat(notes.get(0).get("type").asText()).isEqualTo("PROMOTION");
        assertThat(notes.get(0).get("title").asText()).contains("WL/1 → CNF/S1");
        JsonNode dash = read(getAs(arun, "/api/v1/bookings/dashboard"));
        assertThat(dash.get("unreadNotifications").asInt()).isPositive();
    }

    @Test
    @DisplayName("R5 / F2 API: double cancellation returns 409 INVALID_CANCELLATION")
    void doubleCancel() throws Exception {
        String t = token(PASSENGER, PASSENGER_PW);
        String pnr = read(postAs(t, "/api/v1/bookings", booking("22671", 1))).get("pnr").asText();
        postAs(t, "/api/v1/bookings/" + pnr + "/cancel", null).andExpect(status().isOk());
        postAs(t, "/api/v1/bookings/" + pnr + "/cancel", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_CANCELLATION"));
    }

    @Test
    @DisplayName("R5 API: someone else's PNR cannot be opened or cancelled (403)")
    void someoneElsesPnr() throws Exception {
        String t = token(PASSENGER, PASSENGER_PW);
        String pnr = read(postAs(t, "/api/v1/bookings", booking("22671", 1))).get("pnr").asText();
        String other = token("priya.raman@example.in", PASSENGER_PW);
        getAs(other, "/api/v1/bookings/" + pnr).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ACTION"));
        postAs(other, "/api/v1/bookings/" + pnr + "/cancel", null).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F1 API: an unknown PNR returns 404; the public lookup hides passenger names")
    void pnrLookup() throws Exception {
        getAs(null, "/api/v1/pnr/0000000000").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PNR_NOT_FOUND"));
        String t = token(PASSENGER, PASSENGER_PW);
        String pnr = read(postAs(t, "/api/v1/bookings", booking("22671", 2))).get("pnr").asText();
        JsonNode pub = read(getAs(null, "/api/v1/pnr/" + pnr).andExpect(status().isOk()));
        assertThat(pub.at("/passengers/0/name").isNull()).isTrue();
        assertThat(pub.at("/passengers/0/timeline").size()).isGreaterThanOrEqualTo(2);
        assertThat(read(getAs(t, "/api/v1/bookings/" + pnr)).at("/passengers/0/name").asText()).isEqualTo("Traveller 1");
    }

    @Test
    @DisplayName("Seat map: no names, my own seats are marked")
    void seatMap() throws Exception {
        String t = token(PASSENGER, PASSENGER_PW);
        JsonNode map = read(getAs(t, "/api/v1/trains/12635/seat-map").andExpect(status().isOk()));
        assertThat(map.get("seats")).hasSize(10);
        assertThat(map.toString()).doesNotContain("Priya");
        assertThat(map.at("/seats/3/mine").asBoolean()).isTrue();          // Arun holds S4
        assertThat(map.get("myWaitingPositions").get(0).asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("R6 API: booking after arrival is refused with 409 SEAT_UNAVAILABLE")
    void bookingClosedAfterArrival() throws Exception {
        String master = token(MASTER, STAFF_PW);
        postAs(master, "/api/v1/simulation/advance", Map.of("minutes", 30)).andExpect(status().isOk());
        postAs(master, "/api/v1/operations/trains/12635/arrival", null).andExpect(status().isOk());
        postAs(token(PASSENGER, PASSENGER_PW), "/api/v1/bookings", booking("12635", 1))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SEAT_UNAVAILABLE"));
    }
}
