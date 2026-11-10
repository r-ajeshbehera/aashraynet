package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.SupplyRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplyRequestHistoryRepository extends JpaRepository<SupplyRequestHistory, Long> {
    List<SupplyRequestHistory> findByRequestIdOrderByChangedAtAsc(Long requestId);
}
