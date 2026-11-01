package com.payanam.rail.service.testlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payanam.rail.domain.user.UserAccount;
import com.payanam.rail.repository.UserAccountRepository;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.security.JwtService;
import com.payanam.rail.service.AuthService;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lab scenario F4 makes a REAL HTTP call, so the whole security chain (JWT filter, URL rules,
 * JSON 403 handler) is exercised exactly as a browser would hit it. It signs the demo passenger in
 * with a temporary session, calls a staff-only endpoint, and revokes the session afterwards.
 */
@Component
public class HttpAccessProbe {
    static final String STAFF_ENDPOINT = "/api/v1/operations/dashboard";
    private static final String DEMO_PASSENGER = "passenger@payanam.in";

    private final UserAccountRepository users;
    private final AuthService auth;
    private final JwtService jwt;
    private final ObjectMapper json;
    private volatile int port;

    public HttpAccessProbe(UserAccountRepository users, AuthService auth, JwtService jwt, ObjectMapper json) {
        this.users = users;
        this.auth = auth;
        this.jwt = jwt;
        this.json = json;
    }

    /** Remembers the port the embedded server listens on (random in tests, 8080 normally). */
    @EventListener
    public void onServerStarted(WebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
    }

    /** Status and error code returned by the staff API, with the passenger used. */
    public record Result(String passengerEmail, int status, String code) {
    }

    public Result passengerCallsStaffApi() {
        if (port <= 0) {
            throw new IllegalStateException("The web server is not listening, so the HTTP check cannot run");
        }
        UserAccount passenger = users.findByEmailIgnoreCase(DEMO_PASSENGER)
                .orElseThrow(() -> new IllegalStateException("Demo passenger account not found"));
        String token = auth.issueInternalToken(passenger);
        String sessionId = jwt.parse(token).map(AuthUser::sessionId).orElseThrow();
        try {
            HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY)
                    .connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + STAFF_ENDPOINT))
                    .header("Authorization", "Bearer " + token).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = json.readTree(response.body().isBlank() ? "{}" : response.body());
            return new Result(passenger.getEmail(), response.statusCode(), body.path("code").asText(""));
        } catch (IOException e) {
            throw new IllegalStateException("HTTP call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP call interrupted", e);
        } finally {
            auth.revokeSession(sessionId);
        }
    }
}
