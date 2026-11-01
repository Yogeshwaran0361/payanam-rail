package com.payanam.rail.domain.user;

/**
 * Fine-grained staff permissions. They become Spring Security authorities "PERM_xxx" and are
 * checked with {@code @PreAuthorize} on every station API, and used by the UI to hide menu items.
 */
public enum Permission {
    VIEW_DASHBOARD,
    MANAGE_PLATFORMS,
    MANAGE_TRAINS,
    MANAGE_RESERVATIONS,
    CONTROL_SIMULATION,
    VIEW_AUDIT,
    RUN_TESTS,
    MANAGE_SETTINGS
}
