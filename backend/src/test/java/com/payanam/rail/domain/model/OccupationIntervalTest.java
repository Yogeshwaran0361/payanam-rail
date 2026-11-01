package com.payanam.rail.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.payanam.rail.domain.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccupationIntervalTest {

    @Test
    @DisplayName("P1: half-open intervals [a,b) and [b,c) do not overlap")
    void backToBackDoesNotOverlap() {
        OccupationInterval i = new OccupationInterval("12635", at("07:00"), at("07:25"));
        assertThat(i.overlaps(at("07:25"), at("07:40"))).isFalse();
        assertThat(i.overlaps(at("06:40"), at("07:00"))).isFalse();
        assertThat(i.overlaps(at("07:24"), at("07:40"))).isTrue();
        assertThat(i.overlaps(at("06:00"), at("08:00"))).isTrue();
    }

    @Test
    @DisplayName("P1: contains() includes the start and excludes the end")
    void contains() {
        OccupationInterval i = new OccupationInterval("12635", at("07:00"), at("07:25"));
        assertThat(i.contains(at("07:00"))).isTrue();
        assertThat(i.contains(at("07:25"))).isFalse();
    }

    @Test
    @DisplayName("P1: an interval must end after it starts")
    void invalid() {
        assertThatThrownBy(() -> new OccupationInterval("1", at("07:00"), at("07:00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
