package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByShelterIdOrderByCreatedAtDesc(Long shelterId, Pageable pageable);

    long countByRequestId(Long requestId);
}
