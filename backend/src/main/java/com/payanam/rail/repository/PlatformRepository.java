package com.payanam.rail.repository;

import com.payanam.rail.entity.PlatformEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link PlatformEntity}.
 */
@Repository
public interface PlatformRepository extends JpaRepository<PlatformEntity, String> {
}

