package com.payanam.rail.domain.model;

import com.payanam.rail.exception.PlatformConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.payanam.rail.domain.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformTest {

    @Test
    @DisplayName("P1: occupyPlatform keeps intervals sorted and refuses an overlap")
    void refusesOverlap() {
        Platform p = new Platform("P2", "Platform 2", 450);
        p.occupyPlatform("B", at("08:00"), at("08:30"), 400);
        p.occupyPlatform("A", at("07:00"), at("07:30"), 400);
        assertThat(p.getOccupations()).extracting(OccupationInterval::trainNo).containsExactly("A", "B");
        assertThatThrownBy(() -> p.occupyPlatform("C", at("07:20"), at("07:50"), 400))
                .isInstanceOf(PlatformConflictException.class).hasMessageContaining("busy");
        assertThat(p.isFree(at("07:30"), at("08:00"))).isTrue();
        assertThat(p.isFree(at("07:29"), at("08:00"))).isFalse();
        assertThat(p.isFree(at("08:29"), at("09:00"))).isFalse();
    }

    @Test
    @DisplayName("P1: occupyPlatform refuses a train longer than the platform")
    void refusesTooLong() {
        Platform p = new Platform("P3", null, 300);
        assertThat(p.getName()).isEqualTo("Platform P3");
        assertThatThrownBy(() -> p.occupyPlatform("X", at("07:00"), at("07:30"), 301))
                .isInstanceOf(PlatformConflictException.class).hasMessageContaining("too short");
        assertThat(p.canFit(300)).isTrue();
    }

    @Test
    @DisplayName("P1: the same train cannot hold two intervals on one platform")
    void refusesDuplicateTrain() {
        Platform p = new Platform("P1", "Platform 1", 600);
        p.occupyPlatform("X", at("07:00"), at("07:30"), 300);
        assertThatThrownBy(() -> p.occupyPlatform("X", at("09:00"), at("09:30"), 300))
                .isInstanceOf(PlatformConflictException.class);
    }

    @Test
    @DisplayName("P9: a platform in maintenance accepts no new occupation")
    void maintenanceRefuses() {
        Platform p = new Platform("P1", "Platform 1", 600);
        p.setUnderMaintenance(true);
        assertThat(p.isAvailableForAllocation()).isFalse();
        assertThatThrownBy(() -> p.occupyPlatform("X", at("07:00"), at("07:30"), 300))
                .isInstanceOf(PlatformConflictException.class).hasMessageContaining("maintenance");
    }

    @Test
    @DisplayName("Platform: next free time skips back-to-back intervals")
    void nextFreeTime() {
        Platform p = new Platform("P1", "Platform 1", 600);
        p.occupyPlatform("A", at("07:00"), at("07:30"), 300);
        p.occupyPlatform("B", at("07:30"), at("08:00"), 300);
        assertThat(p.nextFreeTime(at("07:10"))).isEqualTo(at("08:00"));
        assertThat(p.nextFreeTime(at("06:00"))).isEqualTo(at("06:00"));
        assertThat(p.occupationAt(at("07:45"))).get().extracting(OccupationInterval::trainNo).isEqualTo("B");
        assertThat(p.findOverlapping(at("07:20"), at("07:40"))).hasSize(2);
    }

    @Test
    @DisplayName("Platform: replace/reoccupy/remove keep the timeline consistent")
    void replaceAndRemove() {
        Platform p = new Platform("P1", "Platform 1", 600);
        p.occupyPlatform("A", at("07:00"), at("07:30"), 300);
        p.occupyPlatform("B", at("08:00"), at("08:30"), 300);
        assertThatThrownBy(() -> p.replaceOccupation("A", at("07:00"), at("08:10")))
                .isInstanceOf(PlatformConflictException.class);
        assertThat(p.findOccupation("A")).get().extracting(OccupationInterval::end).isEqualTo(at("07:30"));
        p.replaceOccupation("A", at("07:00"), at("07:20"));
        assertThat(p.findOccupation("A")).get().extracting(OccupationInterval::end).isEqualTo(at("07:20"));
        assertThatThrownBy(() -> p.replaceOccupation("Z", at("07:00"), at("07:20")))
                .isInstanceOf(PlatformConflictException.class);
        p.removeOccupation("A");
        assertThatThrownBy(() -> p.reoccupy("C", at("08:10"), at("08:20"))).isInstanceOf(PlatformConflictException.class);
        p.reoccupy("A", at("07:00"), at("07:40"));
        assertThat(p.getOccupations()).hasSize(2);
        assertThat(p.removeOccupation("nope")).isEmpty();
    }

    @Test
    @DisplayName("Platform management: length cannot drop below a planned train")
    void changeLength() {
        Platform p = new Platform("P2", "Platform 2", 450);
        assertThatThrownBy(() -> p.changeLength(350, 420)).isInstanceOf(PlatformConflictException.class);
        p.changeLength(500, 420);
        assertThat(p.getLengthMeters()).isEqualTo(500);
        assertThatThrownBy(() -> p.changeLength(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Platform(" ", "x", 100)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Platform: ids sort naturally (P2 before P10) and copy is independent")
    void idsAndCopy() {
        assertThat(Platform.compareIds("P2", "P10")).isNegative();
        assertThat(Platform.compareIds("P1", "P2")).isNegative();
        Platform p = new Platform("P1", "Platform 1", 600);
        Platform c = p.copy();
        c.occupyPlatform("A", at("07:00"), at("07:30"), 300);
        assertThat(p.getOccupations()).isEmpty();
    }
}
