package com.payanam.rail.security;

import com.payanam.rail.domain.user.Permission;
import com.payanam.rail.domain.user.Role;
import com.payanam.rail.domain.user.StationStaffUser;
import com.payanam.rail.domain.user.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The signed-in user, rebuilt from the JWT claims on every request (no user lookup; the filter only checks that the session row is active).
 *
 * @param sessionId the JWT "jti" = id of the AuthSession row
 */
public record AuthUser(Long id, String email, String fullName, Role role, String designation,
                       Set<Permission> permissions, String sessionId) {

    public static AuthUser of(UserAccount account, String sessionId) {
        String designation = account instanceof StationStaffUser staff ? staff.getDesignation().name() : null;
        return new AuthUser(account.getId(), account.getEmail(), account.getFullName(), account.getRole(), designation,
                account.getPermissions(), sessionId);
    }

    /** ROLE_PASSENGER / ROLE_STATION_STAFF plus one PERM_xxx authority per permission. */
    public List<GrantedAuthority> authorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        for (Permission p : permissions) {
            list.add(new SimpleGrantedAuthority("PERM_" + p.name()));
        }
        return list;
    }

    public boolean isStaff() {
        return role == Role.STATION_STAFF;
    }
}
