package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.DisasterEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisasterEventRepository extends JpaRepository<DisasterEvent, Long> {
    List<DisasterEvent> findByActiveTrueOrderByStartDateDesc();

    Optional<DisasterEvent> findFirstByActiveTrueOrderByStartDateDesc();

    boolean existsByNameIgnoreCase(String name);
}
