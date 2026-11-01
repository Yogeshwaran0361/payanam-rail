package com.payanam.rail.repository;

import com.payanam.rail.domain.audit.StationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Saved station snapshots. */
public interface StationSnapshotRepository extends JpaRepository<StationSnapshot, Long> {

    List<StationSnapshot> findTop20ByOrderByCreatedAtDesc();
}
