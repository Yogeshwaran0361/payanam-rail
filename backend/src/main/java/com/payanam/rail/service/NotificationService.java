package com.payanam.rail.service;

import com.payanam.rail.domain.engine.PromotionNotice;
import com.payanam.rail.domain.engine.StationListener;
import com.payanam.rail.domain.engine.TrainUpdateNotice;
import com.payanam.rail.dto.booking.NotificationView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-app notifications (no SMS / e-mail). Kept in memory, like the reservations they describe:
 * HashMap account id → ArrayDeque, newest first (addFirst is O(1)). Also the domain's
 * {@link StationListener}: a promotion WL/1 → CNF/S7, a platform change or a delay becomes a notification
 * for the PNR's owner.
 */
@Service
public class NotificationService implements StationListener {
    private static final int MAX_PER_USER = 50;

    private final Map<Long, ArrayDeque<Notification>> byUser = new HashMap<>();
    private long sequence;

    /** One notification (internal, mutable "read" flag). */
    private static final class Notification {
        private final long id;
        private final String type;
        private final String title;
        private final String message;
        private final LocalDateTime createdAt;
        private final String pnr;
        private boolean read;

        private Notification(long id, String type, String title, String message, LocalDateTime createdAt, String pnr) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.message = message;
            this.createdAt = createdAt;
            this.pnr = pnr;
        }

        private NotificationView view() {
            return new NotificationView(id, type, title, message, createdAt, read, pnr);
        }
    }

    @Override
    public synchronized void onPromotion(PromotionNotice n) {
        add(n.ownerUserId(), "PROMOTION", "Ticket confirmed: " + n.fromLabel() + " → " + n.toLabel(),
                n.passengerName() + " on " + n.trainNo() + " " + n.trainName() + " moved from " + n.fromLabel()
                        + " to " + n.toLabel() + " after a cancellation.", n.at(), n.pnr());
    }

    @Override
    public synchronized void onTrainUpdate(TrainUpdateNotice n) {
        add(n.ownerUserId(), "TRAIN", n.kind() + ": " + n.trainNo() + " " + n.trainName(), n.message(), n.at(), n.pnr());
    }

    /** Adds a notification for one account. */
    public synchronized void add(Long userId, String type, String title, String message, LocalDateTime at, String pnr) {
        ArrayDeque<Notification> list = byUser.computeIfAbsent(userId, id -> new ArrayDeque<>());
        list.addFirst(new Notification(++sequence, type, title, message, at, pnr));
        while (list.size() > MAX_PER_USER) {
            list.removeLast();
        }
    }

    public synchronized List<NotificationView> forUser(Long userId) {
        List<NotificationView> views = new ArrayList<>();
        for (Notification n : byUser.getOrDefault(userId, new ArrayDeque<>())) {
            views.add(n.view());
        }
        return views;
    }

    public synchronized long unreadCount(Long userId) {
        return byUser.getOrDefault(userId, new ArrayDeque<>()).stream().filter(n -> !n.read).count();
    }

    public synchronized void markRead(Long userId, long notificationId) {
        for (Notification n : byUser.getOrDefault(userId, new ArrayDeque<>())) {
            if (n.id == notificationId) {
                n.read = true;
            }
        }
    }

    public synchronized void markAllRead(Long userId) {
        byUser.getOrDefault(userId, new ArrayDeque<>()).forEach(n -> n.read = true);
    }

    /** Called when the station is reloaded: old notifications refer to PNRs that no longer exist. */
    public synchronized void clear() {
        byUser.clear();
    }
}
