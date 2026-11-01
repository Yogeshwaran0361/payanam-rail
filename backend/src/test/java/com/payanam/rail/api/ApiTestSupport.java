package com.payanam.rail.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payanam.rail.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the whole application (in-memory H2) and reloads the seed station before every test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestSupport {
    static final String PASSENGER = "passenger@payanam.in";
    static final String PASSENGER_PW = "Passenger@123";
    static final String MASTER = "stationmaster@payanam.in";
    static final String CONTROLLER = "controller@payanam.in";
    static final String CLERK = "clerk@payanam.in";
    static final String STAFF_PW = "Station@123";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    StationService station;

    @BeforeEach
    void freshStation() {
        station.loadSeed();
    }

    String token(String identifier, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", identifier, "password", password, "rememberMe", false))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    ResultActions getAs(String token, String url) throws Exception {
        return mvc.perform(auth(get(url), token));
    }

    ResultActions postAs(String token, String url, Object body) throws Exception {
        MockHttpServletRequestBuilder req = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            req.content(json(body));
        }
        return mvc.perform(auth(req, token));
    }

    ResultActions putAs(String token, String url, Object body) throws Exception {
        return mvc.perform(auth(put(url).contentType(MediaType.APPLICATION_JSON).content(json(body)), token));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
        return token == null ? req : req.header("Authorization", "Bearer " + token);
    }

    String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    JsonNode read(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
