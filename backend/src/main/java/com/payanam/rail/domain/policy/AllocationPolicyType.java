package com.payanam.rail.domain.policy;

/** The allocation policies that staff can choose in Station Settings. */
public enum AllocationPolicyType {
    BEST_FIT,
    FIRST_FIT;

    /** Factory returning the policy object for this type. */
    public PlatformAllocationPolicy create() {
        return this == BEST_FIT ? new BestFitAllocationPolicy() : new FirstFitAllocationPolicy();
    }
}
