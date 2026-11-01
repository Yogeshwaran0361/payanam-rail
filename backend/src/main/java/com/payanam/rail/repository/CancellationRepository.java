package com.payanam.rail.repository;

import com.payanam.rail.entity.CancellationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CancellationEntity}.
 */
@Repository
public interface CancellationRepository extends JpaRepository<CancellationEntity, Long> {

    List<CancellationEntity> findByBooking_PnrOrderByCancelledAtDesc(String pnr);

    List<CancellationEntity> findByTrain_TrainNoOrderByCancelledAtDesc(String trainNo);
}

