package com.payanam.rail.repository;

import com.payanam.rail.domain.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Accounts of both kinds (single-table inheritance). */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByMobile(String mobile);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);
}
