package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.OccupancySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface OccupancySnapshotRepository extends JpaRepository<OccupancySnapshot, Long> {
    boolean existsByEventIdAndShelterIdAndTakenAt(Long eventId, Long shelterId, LocalDateTime takenAt);
}
