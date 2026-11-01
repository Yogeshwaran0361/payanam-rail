package com.payanam.rail.dto.booking;

import java.time.LocalDateTime;

/** An in-app notification for a passenger (promotion, booking, cancellation). */
public record NotificationView(long id, String type, String title, String message, LocalDateTime createdAt,
                               boolean read, String pnr) {
}
