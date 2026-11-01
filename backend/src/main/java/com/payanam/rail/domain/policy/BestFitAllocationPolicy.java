package com.payanam.rail.domain.policy;

import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.TrainService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * Rule P6 (default) – BEST FIT: among the valid platforms choose the SHORTEST one that is long
 * enough; on a tie choose the lowest platform id.
 * WHY: it keeps the long platforms free for the long trains that cannot use anything else.
 */
public class BestFitAllocationPolicy implements PlatformAllocationPolicy {

    @Override
    public Optional<Platform> selectPlatform(TrainService train, LocalDateTime start, LocalDateTime end,
                                             Collection<Platform> platforms) {
        Platform best = null;
        for (Platform candidate : platforms) {
            if (PlatformAllocationPolicy.isValid(candidate, train, start, end)
                    && (best == null || isBetter(candidate, best))) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isBetter(Platform candidate, Platform best) {
        if (candidate.getLengthMeters() != best.getLengthMeters()) {
            return candidate.getLengthMeters() < best.getLengthMeters();
        }
        return Platform.compareIds(candidate.getId(), best.getId()) < 0;
    }

    @Override
    public AllocationPolicyType type() {
        return AllocationPolicyType.BEST_FIT;
    }
}
