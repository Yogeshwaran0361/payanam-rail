package com.payanam.rail.domain.policy;

import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.Priority;
import com.payanam.rail.domain.model.TrainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.payanam.rail.domain.Fixtures.at;
import static com.payanam.rail.domain.Fixtures.train;
import static org.assertj.core.api.Assertions.assertThat;

class AllocationPolicyTest {
    private final Platform p1 = new Platform("P1", "Platform 1", 600);
    private final Platform p2 = new Platform("P2", "Platform 2", 450);
    private final Platform p3 = new Platform("P3", "Platform 3", 300);
    private final List<Platform> all = List.of(p1, p2, p3);

    @Test
    @DisplayName("P6: best fit chooses the SHORTEST platform that is long enough")
    void bestFitShortest() {
        TrainService t = train("10001", "07:00", "07:10", 280, Priority.NORMAL);
        assertThat(new BestFitAllocationPolicy().selectPlatform(t, at("07:00"), at("07:20"), all))
                .get().extracting(Platform::getId).isEqualTo("P3");
        TrainService t2 = train("10002", "07:00", "07:10", 420, Priority.NORMAL);
        assertThat(new BestFitAllocationPolicy().selectPlatform(t2, at("07:00"), at("07:20"), all))
                .get().extracting(Platform::getId).isEqualTo("P2");
    }

    @Test
    @DisplayName("P6: best fit tie on length goes to the lowest platform id")
    void bestFitTie() {
        Platform a = new Platform("P5", "Platform 5", 450);
        TrainService t = train("10001", "07:00", "07:10", 400, Priority.NORMAL);
        assertThat(new BestFitAllocationPolicy().selectPlatform(t, at("07:00"), at("07:20"), List.of(a, p2)))
                .get().extracting(Platform::getId).isEqualTo("P2");
    }

    @Test
    @DisplayName("P6: first fit chooses the lowest-id valid platform (polymorphism)")
    void firstFit() {
        PlatformAllocationPolicy policy = AllocationPolicyType.FIRST_FIT.create();
        TrainService t = train("10001", "07:00", "07:10", 280, Priority.NORMAL);
        assertThat(policy.selectPlatform(t, at("07:00"), at("07:20"), List.of(p3, p2, p1)))
                .get().extracting(Platform::getId).isEqualTo("P1");
        assertThat(policy.type()).isEqualTo(AllocationPolicyType.FIRST_FIT);
        assertThat(AllocationPolicyType.BEST_FIT.create().type()).isEqualTo(AllocationPolicyType.BEST_FIT);
    }

    @Test
    @DisplayName("P1: busy platforms are skipped by both policies")
    void skipsBusy() {
        p3.occupyPlatform("X", at("07:00"), at("07:30"), 200);
        TrainService t = train("10001", "07:10", "07:20", 280, Priority.NORMAL);
        assertThat(new BestFitAllocationPolicy().selectPlatform(t, at("07:10"), at("07:30"), all))
                .get().extracting(Platform::getId).isEqualTo("P2");
        p1.occupyPlatform("Y", at("07:00"), at("07:30"), 200);
        assertThat(new FirstFitAllocationPolicy().selectPlatform(t, at("07:10"), at("07:30"), all))
                .get().extracting(Platform::getId).isEqualTo("P2");
    }

    @Test
    @DisplayName("P9: platforms in maintenance are excluded")
    void skipsMaintenance() {
        p3.setUnderMaintenance(true);
        TrainService t = train("10001", "07:00", "07:10", 280, Priority.NORMAL);
        assertThat(new BestFitAllocationPolicy().selectPlatform(t, at("07:00"), at("07:20"), all))
                .get().extracting(Platform::getId).isEqualTo("P2");
    }

    @Test
    @DisplayName("P2: no valid platform gives an empty result")
    void none() {
        TrainService t = train("10001", "07:00", "07:10", 650, Priority.NORMAL);
        assertThat(new BestFitAllocationPolicy().selectPlatform(t, at("07:00"), at("07:20"), all)).isEmpty();
        assertThat(new FirstFitAllocationPolicy().selectPlatform(t, at("07:00"), at("07:20"), all)).isEmpty();
    }
}
