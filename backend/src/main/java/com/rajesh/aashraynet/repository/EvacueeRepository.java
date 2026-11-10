package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.Evacuee;
import com.rajesh.aashraynet.entity.SpecialNeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EvacueeRepository extends JpaRepository<Evacuee, Long> {

    @Query("select e from Evacuee e join fetch e.shelter s join fetch s.district where e.id = :id")
    Optional<Evacuee> findWithShelter(@Param("id") Long id);

    @Query("""
           select e from Evacuee e
           where e.shelter.id = :shelterId
             and (:activeOnly = false or e.checkedOutAt is null)
             and (:q is null or e.nameNormalized like concat('%', :q, '%'))
           """)
    Page<Evacuee> findForShelter(@Param("shelterId") Long shelterId, @Param("activeOnly") boolean activeOnly,
                                 @Param("q") String q, Pageable pageable);

    long countByShelterIdAndCheckedOutAtIsNull(Long shelterId);

    /** Public finder: only people currently inside a shelter of an active event. Returns shelter + district names only. */
    @Query("""
           select distinct new com.rajesh.aashraynet.dto.response.PublicMatch(s.name, d.name)
           from Evacuee e join e.shelter s join s.district d join e.event ev
           where e.nameNormalized = :name and e.villageNormalized = :village
             and e.checkedOutAt is null and ev.active = true
           """)
    List<com.rajesh.aashraynet.dto.response.PublicMatch> publicFind(@Param("name") String name,
                                                                   @Param("village") String village,
                                                                   Pageable pageable);

    default List<com.rajesh.aashraynet.dto.response.PublicMatch> publicFind(String name, String village, int max) {
        return publicFind(name, village, PageRequest.of(0, max));
    }

    // ---- dashboard aggregates (event / date filters optional) ----------------------------

    @Query("""
           select count(e) from Evacuee e join e.shelter s
           where (:districtId is null or s.district.id = :districtId)
             and (:eventId is null or e.event.id = :eventId)
             and (:shelterId is null or s.id = :shelterId)
             and (:from is null or e.checkedInAt >= :from)
             and (:to is null or e.checkedInAt < :to)
             and e.checkedOutAt is null
           """)
    long countActive(@Param("districtId") Long districtId, @Param("eventId") Long eventId,
                     @Param("shelterId") Long shelterId, @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

    @Query("""
           select e.specialNeed, count(e) from Evacuee e join e.shelter s
           where (:districtId is null or s.district.id = :districtId)
             and (:eventId is null or e.event.id = :eventId)
             and (:shelterId is null or s.id = :shelterId)
             and (:from is null or e.checkedInAt >= :from)
             and (:to is null or e.checkedInAt < :to)
             and e.checkedOutAt is null
           group by e.specialNeed
           """)
    List<Object[]> countBySpecialNeed(@Param("districtId") Long districtId, @Param("eventId") Long eventId,
                                      @Param("shelterId") Long shelterId, @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("""
           select s.district.id, count(e) from Evacuee e join e.shelter s
           where (:eventId is null or e.event.id = :eventId) and e.checkedOutAt is null
           group by s.district.id
           """)
    List<Object[]> countActiveByDistrict(@Param("eventId") Long eventId);

    @Query("select count(e) from Evacuee e where e.shelter.id = :shelterId and e.checkedOutAt is null and e.specialNeed <> :none")
    long countSpecialNeedsInShelter(@Param("shelterId") Long shelterId, @Param("none") SpecialNeed none);
}
