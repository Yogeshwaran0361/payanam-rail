package com.payanam.rail.domain.model;

/**
 * Live status of a platform. FREE and OCCUPIED are computed from the trains on it;
 * MAINTENANCE is set by staff and excludes the platform from allocation (P9).
 */
public enum PlatformStatus {
    FREE,
    OCCUPIED,
    MAINTENANCE
}
