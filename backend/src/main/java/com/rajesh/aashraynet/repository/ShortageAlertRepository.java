package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.ShortageAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortageAlertRepository extends JpaRepository<ShortageAlert, Long> {
    List<ShortageAlert> findByResolvedAtIsNull();

    List<ShortageAlert> findByShelterIdInAndResolvedAtIsNullOrderByDaysRemainingAsc(java.util.Collection<Long> shelterIds);

    List<ShortageAlert> findByShelterIdInOrderByDetectedAtDesc(java.util.Collection<Long> shelterIds);
}
