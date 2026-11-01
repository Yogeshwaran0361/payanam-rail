package com.payanam.rail.repository;

import com.payanam.rail.domain.user.StationStaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

/** Staff-only queries (employee id uniqueness). */
public interface StationStaffUserRepository extends JpaRepository<StationStaffUser, Long> {

    boolean existsByEmployeeIdIgnoreCase(String employeeId);
}
