package com.payanam.rail.repository;

import com.payanam.rail.entity.PlatformAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PlatformAllocationEntity}.
 */
@Repository
public interface PlatformAllocationRepository extends JpaRepository<PlatformAllocationEntity, Long> {

    List<PlatformAllocationEntity> findByPlatform_PlatformIdOrderByOccupationStartAsc(String platformId);

    List<PlatformAllocationEntity> findByTrain_TrainNoOrderByOccupationStartAsc(String trainNo);

    Optional<PlatformAllocationEntity> findTopByTrain_TrainNoOrderByCreatedAtDesc(String trainNo);
}

