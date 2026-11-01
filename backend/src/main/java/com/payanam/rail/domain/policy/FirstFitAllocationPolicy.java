package com.payanam.rail.domain.policy;

import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.TrainService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * FIRST FIT: the valid platform with the LOWEST id wins, whatever its length.
 * Simpler and faster to explain, but it may waste a long platform on a short train.
 * Exists to demonstrate polymorphism: the scheduler calls the same method on either policy.
 */
public class FirstFitAllocationPolicy implements PlatformAllocationPolicy {

    @Override
    public Optional<Platform> selectPlatform(TrainService train, LocalDateTime start, LocalDateTime end,
                                             Collection<Platform> platforms) {
        Platform first = null;
        for (Platform candidate : platforms) {
            if (PlatformAllocationPolicy.isValid(candidate, train, start, end)
                    && (first == null || Platform.compareIds(candidate.getId(), first.getId()) < 0)) {
                first = candidate;
            }
        }
        return Optional.ofNullable(first);
    }

    @Override
    public AllocationPolicyType type() {
        return AllocationPolicyType.FIRST_FIT;
    }
}
