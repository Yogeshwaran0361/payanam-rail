package com.payanam.rail.security;

import com.payanam.rail.repository.AuthSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Reads "Authorization: Bearer &lt;jwt&gt;", verifies it and checks that its session is still active
 * (not logged out / revoked). If everything is valid the request runs as that user; otherwise it runs
 * anonymously and protected endpoints answer 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthSessionRepository sessions;
    private final Clock clock;

    public JwtAuthenticationFilter(JwtService jwtService, AuthSessionRepository sessions, Clock clock) {
        this.jwtService = jwtService;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Optional<AuthUser> user = jwtService.parse(header.substring(7).trim());
            LocalDateTime now = LocalDateTime.now(clock);
            if (user.isPresent() && sessions.findById(user.get().sessionId()).map(s -> s.isActive(now)).orElse(false)) {
                AuthUser u = user.get();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(u, null, u.authorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
