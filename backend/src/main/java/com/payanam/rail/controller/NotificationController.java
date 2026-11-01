package com.payanam.rail.controller;

import com.payanam.rail.dto.MessageResponse;
import com.payanam.rail.dto.booking.NotificationView;
import com.payanam.rail.security.AuthUser;
import com.payanam.rail.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** In-app notifications of the signed-in passenger. */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "bookings")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    @Operation(summary = "My notifications, newest first")
    public List<NotificationView> mine(@AuthenticationPrincipal AuthUser user) {
        return notifications.forUser(user.id());
    }

    @PostMapping("/{id}/read")
    public MessageResponse read(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        notifications.markRead(user.id(), id);
        return new MessageResponse("Marked as read");
    }

    @PostMapping("/read-all")
    public MessageResponse readAll(@AuthenticationPrincipal AuthUser user) {
        notifications.markAllRead(user.id());
        return new MessageResponse("All marked as read");
    }
}
