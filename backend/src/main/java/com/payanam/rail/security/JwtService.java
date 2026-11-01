package com.payanam.rail.security;

import com.payanam.rail.config.PayanamProperties;
import com.payanam.rail.domain.user.Permission;
import com.payanam.rail.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates and verifies JSON Web Tokens (HMAC-SHA; HS384 with the demo key). A token holds the user id, e-mail, name, role,
 * designation and permissions, so each request is authorised without a user lookup; its "jti" links
 * it to an AuthSession row for revocation.
 */
@Service
public class JwtService {
    private final SecretKey key;

    public JwtService(PayanamProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.security().jwtSecret()));
    }

    /** Signs a token for {@code user} that expires at {@code expiresAt}. */
    public String createToken(AuthUser user, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .id(user.sessionId())
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("name", user.fullName())
                .claim("role", user.role().name())
                .claim("designation", user.designation())
                .claim("permissions", user.permissions().stream().map(Enum::name).toList())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /** Verifies the signature and expiry; returns empty for any invalid or expired token. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Set<Permission> permissions = EnumSet.noneOf(Permission.class);
            Object raw = c.get("permissions");
            if (raw instanceof List<?> list) {
                for (Object p : list) {
                    permissions.add(Permission.valueOf(String.valueOf(p)));
                }
            }
            return Optional.of(new AuthUser(Long.valueOf(c.getSubject()), c.get("email", String.class),
                    c.get("name", String.class), Role.valueOf(c.get("role", String.class)),
                    c.get("designation", String.class), permissions, c.getId()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
