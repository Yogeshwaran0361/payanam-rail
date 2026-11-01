package com.payanam.rail.config;

import com.payanam.rail.security.JsonSecurityHandlers;
import com.payanam.rail.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security. Two layers protect every API:
 * <ol>
 *   <li>URL rules here: passenger APIs need ROLE_PASSENGER, station APIs need ROLE_STATION_STAFF
 *       (so a passenger gets 403 on staff APIs and the reverse, test F4);</li>
 *   <li>{@code @PreAuthorize("hasAuthority('PERM_...')")} on each station endpoint for the designation's
 *       permissions (e.g. a Booking Clerk cannot touch platforms).</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String STAFF = "STATION_STAFF";
    private static final String PASSENGER = "PASSENGER";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                           JsonSecurityHandlers handlers) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)          // no cookies: the JWT travels in a header
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/signup/**", "/api/v1/auth/signin",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/pnr/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/trains", "/api/v1/trains/stats",
                                "/api/v1/trains/*/live").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/trains/**").authenticated()
                        .requestMatchers("/api/v1/trains/**").hasRole(STAFF)
                        .requestMatchers("/api/v1/bookings/**", "/api/v1/notifications/**").hasRole(PASSENGER)
                        .requestMatchers("/api/v1/platforms/**", "/api/v1/operations/**", "/api/v1/simulation/**",
                                "/api/v1/audit/**", "/api/v1/tests/**", "/api/v1/admin/**").hasRole(STAFF)
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())          // the React app, Swagger UI, static files
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt with strength 10: slow on purpose, salted, the industry default for passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
