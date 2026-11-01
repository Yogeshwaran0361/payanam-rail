package com.payanam.rail.exception;

import com.payanam.rail.dto.ApiError;
import com.payanam.rail.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Turns every exception into the single JSON error format ({@link ApiError}) with a fitting HTTP status.
 * Failed state-changing requests are also written to the audit log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditService audit;

    public GlobalExceptionHandler(AuditService audit) {
        this.audit = audit;
    }

    @ExceptionHandler(RailwayException.class)
    public ResponseEntity<ApiError> railway(RailwayException e, HttpServletRequest request) {
        HttpStatus status = statusOf(e);
        log.info("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(), status.value(), e.getCode(),
                e.getMessage());
        if (e instanceof ValidationException) {
            return validation((ValidationException) e, request);
        }
        auditFailure(request, e.getCode(), e.getMessage(), e instanceof AuthenticationFailedException
                || e instanceof AccountLockedException);
        return ResponseEntity.status(status).body(ApiError.of(status, e.getCode(), e.getMessage(), request.getRequestURI()));
    }

    /** "field: message" business validation errors are shown next to the form field. */
    private ResponseEntity<ApiError> validation(ValidationException e, HttpServletRequest request) {
        String raw = e.getMessage();
        int colon = raw.indexOf(": ");
        List<ApiError.FieldErrorView> fields = colon > 0
                ? List.of(new ApiError.FieldErrorView(raw.substring(0, colon), raw.substring(colon + 2)))
                : List.of();
        String message = colon > 0 ? raw.substring(colon + 2) : raw;
        return ResponseEntity.badRequest().body(ApiError.withFields(HttpStatus.BAD_REQUEST, e.getCode(), message,
                request.getRequestURI(), fields));
    }

    /** Bean Validation errors: one entry per invalid field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ApiError.FieldErrorView> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldErrorView(f.getField(), f.getDefaultMessage())).toList();
        String message = fields.isEmpty() ? "The request is not valid"
                : fields.size() == 1 ? fields.get(0).message() : "Please correct the highlighted fields";
        return ResponseEntity.badRequest().body(ApiError.withFields(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message,
                request.getRequestURI(), fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable(Exception e, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "The request body or a parameter has the wrong format", request.getRequestURI()));
    }

    /** Invalid arguments from domain constructors (e.g. a passenger age of 0). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        auditFailure(request, "BAD_REQUEST", e.getMessage(), false);
        return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(),
                request.getRequestURI()));
    }

    /** @PreAuthorize failures (wrong designation) end up here. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest request) {
        audit.recordDenied(request.getMethod() + " " + request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Your account is not allowed to use this feature", request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> notFound(NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "No such endpoint", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong on the server", request.getRequestURI()));
    }

    /** HTTP status for each business exception. */
    static HttpStatus statusOf(RailwayException e) {
        if (e instanceof TrainNotFoundException || e instanceof ReservationNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof UnauthorizedActionException) {
            return HttpStatus.FORBIDDEN;
        }
        if (e instanceof AuthenticationFailedException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (e instanceof AccountLockedException) {
            return HttpStatus.LOCKED;
        }
        if (e instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.CONFLICT;   // seat unavailable, invalid cancellation, platform conflict, invalid operation
    }

    private void auditFailure(HttpServletRequest request, String code, String message, boolean alreadyAudited) {
        if (!alreadyAudited && !"GET".equals(request.getMethod())) {
            audit.recordFailure("FAILED_" + code, request.getMethod() + " " + request.getRequestURI() + ": " + message);
        }
    }
}
