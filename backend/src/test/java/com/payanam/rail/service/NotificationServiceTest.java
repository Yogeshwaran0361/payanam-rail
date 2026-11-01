package com.payanam.rail.service;

import com.payanam.rail.domain.engine.PromotionNotice;
import com.payanam.rail.domain.engine.TrainUpdateNotice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    @Test
    @DisplayName("R3: a promotion becomes an unread in-app notification 'WL/1 → CNF/S7' for the PNR owner")
    void promotionNotification() {
        NotificationService service = new NotificationService();
        service.onPromotion(new PromotionNotice(7L, "4000824633", "12635", "Vaigai Express", "Arun", "WL/1", "CNF/S7",
                LocalDateTime.of(2026, 1, 5, 6, 40)));
        assertThat(service.forUser(7L)).singleElement().satisfies(n -> {
            assertThat(n.title()).isEqualTo("Ticket confirmed: WL/1 → CNF/S7");
            assertThat(n.read()).isFalse();
        });
        assertThat(service.unreadCount(7L)).isEqualTo(1);
        service.markRead(7L, service.forUser(7L).get(0).id());
        assertThat(service.unreadCount(7L)).isZero();
        service.add(7L, "BOOKING", "t", "m", LocalDateTime.now(), null);
        service.markAllRead(7L);
        assertThat(service.unreadCount(7L)).isZero();
        assertThat(service.forUser(8L)).isEmpty();
        service.clear();
        assertThat(service.forUser(7L)).isEmpty();
    }

    @Test
    @DisplayName("Notifications keep the newest 50 per user")
    void capped() {
        NotificationService service = new NotificationService();
        for (int i = 0; i < 60; i++) {
            service.add(1L, "BOOKING", "n" + i, "m", LocalDateTime.now(), null);
        }
        assertThat(service.forUser(1L)).hasSize(50);
        assertThat(service.forUser(1L).get(0).title()).isEqualTo("n59");
    }

    @Test
    @DisplayName("Notifications: a delay or platform change on a train becomes a TRAIN notification for the PNR owner")
    void trainUpdateNotification() {
        NotificationService service = new NotificationService();
        service.onTrainUpdate(new TrainUpdateNotice(3L, "4000824633", "12635", "Vaigai Express", "Delay",
                "12635 delayed by 20 min", LocalDateTime.of(2026, 1, 5, 6, 45)));
        assertThat(service.forUser(3L)).singleElement().satisfies(n -> {
            assertThat(n.type()).isEqualTo("TRAIN");
            assertThat(n.title()).isEqualTo("Delay: 12635 Vaigai Express");
            assertThat(n.pnr()).isEqualTo("4000824633");
        });
    }
}
