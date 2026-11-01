package com.payanam.rail.repository;

import com.payanam.rail.entity.WaitingListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WaitingListEntity}.
 */
@Repository
public interface WaitingListRepository extends JpaRepository<WaitingListEntity, Long> {

    List<WaitingListEntity> findByTrain_TrainNoAndQueueStatusOrderByWaitingPositionAsc(String trainNo, String queueStatus);

    Optional<WaitingListEntity> findByTicketId(String ticketId);

    List<WaitingListEntity> findByPnr(String pnr);
}

