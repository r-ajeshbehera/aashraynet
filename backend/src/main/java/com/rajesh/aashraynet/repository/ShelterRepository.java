package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    @Query("select s from Shelter s join fetch s.district where s.id = :id")
    Optional<Shelter> findWithDistrict(@Param("id") Long id);

    /** Scoped list: districtId null = all districts (STATE_ADMIN). */
    @Query("""
           select s from Shelter s join fetch s.district d
           where (:districtId is null or d.id = :districtId)
             and (:activeOnly = false or s.active = true)
           order by d.name, s.name
           """)
    List<Shelter> findScoped(@Param("districtId") Long districtId, @Param("activeOnly") boolean activeOnly);

    List<Shelter> findByInchargeIdOrderByName(Long inchargeId);

    /**
     * Nearest-shelter candidates: filtered in SQL (active, enough free places, inside a bounding box, not the
     * current shelter) so only a small list is sorted by exact Haversine distance in Java.
     */
    @Query("""
           select s from Shelter s join fetch s.district
           where s.active = true
             and s.id <> :excludeId
             and (s.capacity - s.occupancy) >= :people
             and s.latitude between :minLat and :maxLat
             and s.longitude between :minLng and :maxLng
           """)
    List<Shelter> findCandidates(@Param("excludeId") Long excludeId, @Param("people") int people,
                                 @Param("minLat") BigDecimal minLat, @Param("maxLat") BigDecimal maxLat,
                                 @Param("minLng") BigDecimal minLng, @Param("maxLng") BigDecimal maxLng);

    List<Shelter> findByActiveTrue();
}
