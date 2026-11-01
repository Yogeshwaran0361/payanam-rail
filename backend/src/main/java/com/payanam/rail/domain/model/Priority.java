package com.payanam.rail.domain.model;

/**
 * Declared priority of a train. A lower {@link #rank()} is served first by the platform
 * waiting PriorityQueue (rule P4): EMERGENCY = 0 &lt; CONNECTION = 1 &lt; NORMAL = 2.
 */
public enum Priority {
    EMERGENCY(0),
    CONNECTION(1),
    NORMAL(2);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
