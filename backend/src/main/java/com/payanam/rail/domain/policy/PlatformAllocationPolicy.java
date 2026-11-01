package com.payanam.rail.domain.policy;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import com.payanam.rail.domain.model.Platform;
import com.payanam.rail.domain.model.TrainService;

/**
 * ABSTRACTION: "which platform do we choose?" is a separate decision from "how do we keep the queue
 * and the timelines?". The scheduler depends only on this interface, so a policy can be swapped at
 * runtime (Station Settings) without changing the scheduler: that is polymorphism in action.
 */
public interface PlatformAllocationPolicy {

    /**
     * Chooses a platform for {@code train} during the half-open interval [start, end), or returns
     * empty when no platform is valid (available, long enough AND free for the whole interval).
     */
    Optional<Platform> selectPlatform(TrainService train, LocalDateTime start, LocalDateTime end,
                                      Collection<Platform> platforms);

    /** The policy's name shown in Settings, e.g. BEST_FIT. */
    AllocationPolicyType type();

    /** P1 check shared by every policy: not in maintenance, long enough, no overlap. */
    static boolean isValid(Platform platform, TrainService train, LocalDateTime start, LocalDateTime end) {
        return platform.isAvailableForAllocation()
                && platform.canFit(train.getLengthMeters())
                && platform.isFree(start, end);
    }
}