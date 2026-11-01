package com.payanam.rail.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payanam.rail.dto.ApiError;
import com.payanam.rail.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes 401 (not signed in / token expired) and 403 (wrong role or permission) in the same JSON
 * error format as every other error. Denied access is also written to the audit log.
 */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final AuditService audit;

    public JsonSecurityHandlers(ObjectMapper mapper, AuditService audit) {
        this.mapper = mapper;
        this.audit = audit;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                "Please sign in again: your session is missing or has expired", request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        audit.recordDenied(request.getMethod() + " " + request.getRequestURI());
        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Your account is not allowed to use this feature", request.getRequestURI());
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiError.of(status, code, message, path));
    }
}
