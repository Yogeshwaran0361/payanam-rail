package com.payanam.rail.domain.model;

import java.time.LocalDateTime;

/**
 * Entry of the release-time MIN-HEAP: "platform X becomes free at time T (after train Y)".
 * Natural order = earliest release time first, platform id as the tie-breaker, so
 * {@code PriorityQueue<PlatformRelease>.peek()} is always the next platform to be released.
 */
public record PlatformRelease(String platformId, String trainNo, LocalDateTime releaseTime)
        implements Comparable<PlatformRelease> {

    @Override
    public int compareTo(PlatformRelease other) {
        int byTime = releaseTime.compareTo(other.releaseTime);
        return byTime != 0 ? byTime : Platform.compareIds(platformId, other.platformId);
    }
}
