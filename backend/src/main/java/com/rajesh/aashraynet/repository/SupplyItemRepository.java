package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.SupplyItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplyItemRepository extends JpaRepository<SupplyItem, Long> {
    List<SupplyItem> findByActiveTrueOrderByName();

    boolean existsByNameIgnoreCase(String name);
}
