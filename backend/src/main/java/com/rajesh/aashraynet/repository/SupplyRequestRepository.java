package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.RequestStatus;
import com.rajesh.aashraynet.entity.SupplyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplyRequestRepository extends JpaRepository<SupplyRequest, Long> {

    @Query("""
           select r from SupplyRequest r join fetch r.shelter s join fetch s.district
           where r.id = :id
           """)
    Optional<SupplyRequest> findWithShelter(@Param("id") Long id);

    @Query(value = """
           select r from SupplyRequest r join r.shelter s
           where (:districtId is null or s.district.id = :districtId)
             and (:shelterId is null or s.id = :shelterId)
             and (:status is null or r.status = :status)
           """,
           countQuery = """
           select count(r) from SupplyRequest r join r.shelter s
           where (:districtId is null or s.district.id = :districtId)
             and (:shelterId is null or s.id = :shelterId)
             and (:status is null or r.status = :status)
           """)
    Page<SupplyRequest> search(@Param("districtId") Long districtId, @Param("shelterId") Long shelterId,
                               @Param("status") RequestStatus status, Pageable pageable);

    @Query("""
           select r.status, count(r) from SupplyRequest r join r.shelter s
           where (:districtId is null or s.district.id = :districtId)
             and (:eventId is null or r.event.id = :eventId)
             and (:shelterId is null or s.id = :shelterId)
           group by r.status
           """)
    List<Object[]> countByStatus(@Param("districtId") Long districtId, @Param("eventId") Long eventId,
                                 @Param("shelterId") Long shelterId);
}
