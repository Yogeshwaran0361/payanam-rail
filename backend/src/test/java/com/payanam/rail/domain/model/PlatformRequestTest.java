package com.payanam.rail.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static com.payanam.rail.domain.Fixtures.at;
import static com.payanam.rail.domain.Fixtures.train;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformRequestTest {

    @Test
    @DisplayName("OOP: each PlatformRequest subclass overrides priorityRank()")
    void polymorphicRanks() {
        TrainService t = train("11111", "07:00", "07:10", 300, Priority.NORMAL);
        PlatformRequest e = PlatformRequest.create(ArrivalType.EMERGENCY, t, at("06:00"));
        PlatformRequest c = PlatformRequest.create(ArrivalType.CONNECTION, t, at("06:00"));
        PlatformRequest s = PlatformRequest.create(ArrivalType.SCHEDULED, t, at("06:00"));
        assertThat(e).isInstanceOf(EmergencyArrival.class);
        assertThat(c).isInstanceOf(ConnectionArrival.class);
        assertThat(s).isInstanceOf(ScheduledArrival.class);
        assertThat(List.of(e.priorityRank(), c.priorityRank(), s.priorityRank())).containsExactly(0, 1, 2);
        assertThat(s.copyFor(t).getType()).isEqualTo(ArrivalType.SCHEDULED);
        assertThat(ArrivalType.fromPriority(Priority.CONNECTION)).isEqualTo(ArrivalType.CONNECTION);
    }

    @Test
    @DisplayName("P4: queue order is priority rank, then arrival time, then trainNo")
    void queueOrder() {
        PriorityQueue<PlatformRequest> q = new PriorityQueue<>(PlatformRequest.QUEUE_ORDER);
        q.add(new ScheduledArrival(train("10001", "07:00", "07:10", 300, Priority.NORMAL), at("06:00")));
        q.add(new ConnectionArrival(train("10002", "07:30", "07:40", 300, Priority.NORMAL), at("06:00")));
        q.add(new EmergencyArrival(train("10003", "08:00", "08:10", 300, Priority.NORMAL), at("06:00")));
        q.add(new ScheduledArrival(train("10004", "06:50", "07:10", 300, Priority.NORMAL), at("06:00")));
        List<String> order = new ArrayList<>();
        while (!q.isEmpty()) {
            order.add(q.poll().getTrainNo());
        }
        assertThat(order).containsExactly("10003", "10002", "10004", "10001");
    }

    @Test
    @DisplayName("T6: a full tie (same rank and arrival) is broken by trainNo, deterministically")
    void tieBreak() {
        for (int run = 0; run < 5; run++) {
            PriorityQueue<PlatformRequest> q = new PriorityQueue<>(PlatformRequest.QUEUE_ORDER);
            q.add(new ScheduledArrival(train("22222", "07:00", "07:10", 300, Priority.NORMAL), at("06:00")));
            q.add(new ScheduledArrival(train("11111", "07:00", "07:10", 300, Priority.NORMAL), at("06:00")));
            assertThat(q.poll().getTrainNo()).isEqualTo("11111");
        }
    }

    @Test
    @DisplayName("P7: a request's arrival time is read live, so a delay changes its key")
    void liveKey() {
        TrainService t = train("11111", "07:00", "07:10", 300, Priority.NORMAL);
        PlatformRequest r = new ScheduledArrival(t, at("06:00"));
        t.applyDelay(15, "x");
        assertThat(r.getArrivalTime()).isEqualTo(at("07:15"));
        assertThat(r.getRequestedAt()).isEqualTo(at("06:00"));
    }
}
