package com.payanam.rail.repository;

import com.payanam.rail.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link BookingEntity}.
 */
@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, String> {

    List<BookingEntity> findByOwner_IdOrderByBookedAtDesc(Long ownerUserId);

    List<BookingEntity> findByTrain_TrainNoOrderByBookedAtAsc(String trainNo);
}

