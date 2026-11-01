package com.payanam.rail.repository;

import com.payanam.rail.entity.TrainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link TrainEntity}.
 */
@Repository
public interface TrainRepository extends JpaRepository<TrainEntity, String> {
}

