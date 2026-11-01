package com.payanam.rail.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * The ONE JSON error format of the API, e.g.
 * {"timestamp": "...", "status": 409, "error": "Conflict", "code": "INVALID_CANCELLATION",
 *  "message": "Ticket 4000824633/1 is already cancelled", "path": "/api/v1/bookings/...", "fieldErrors": []}
 */
public record ApiError(Instant timestamp, int status, String error, String code, String message, String path,
                       List<FieldErrorView> fieldErrors) {

    /** One invalid form field, shown next to that field in the UI. */
    public record FieldErrorView(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path, List.of());
    }

    public static ApiError withFields(HttpStatus status, String code, String message, String path,
                                      List<FieldErrorView> fields) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path, fields);
    }
}
