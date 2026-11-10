package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.CheckInRequest;
import com.rajesh.aashraynet.dto.response.CheckInResponse;
import com.rajesh.aashraynet.dto.response.EvacueeResponse;
import com.rajesh.aashraynet.dto.response.NearestShelterResponse;
import com.rajesh.aashraynet.dto.response.ShelterResponse;
import com.rajesh.aashraynet.entity.DisasterEvent;
import com.rajesh.aashraynet.entity.Evacuee;
import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.exception.BusinessRuleException;
import com.rajesh.aashraynet.exception.CapacityExceededException;
import com.rajesh.aashraynet.exception.ConcurrentUpdateException;
import com.rajesh.aashraynet.exception.InvalidStateException;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.EvacueeRepository;
import com.rajesh.aashraynet.repository.ShelterRepository;
import com.rajesh.aashraynet.repository.UserRepository;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * Check-in / check-out with concurrency-safe capacity.
 *
 * Three layers stop overbooking:
 *  1. Business check: shelter.admit() refuses when free places < 1.
 *  2. Optimistic lock: Shelter has @Version, so "UPDATE ... WHERE id=? AND version=?" updates 0 rows if another
 *     check-in committed first -> ObjectOptimisticLockingFailureException -> we retry with fresh data.
 *  3. Database: CHECK (occupancy <= capacity) is the final safety net.
 *
 * Each attempt runs in its own transaction (TransactionTemplate), so a retry really re-reads the row.
 */
@Service
public class CapacityService {
    private static final Logger log = LoggerFactory.getLogger(CapacityService.class);
    static final int MAX_ATTEMPTS = 3;

    private final ShelterRepository shelters;
    private final EvacueeRepository evacuees;
    private final UserRepository users;
    private final EventService events;
    private final NearestShelterService nearest;
    private final AccessPolicy access;
    private final AuditService audit;
    private final TransactionTemplate tx;

    public CapacityService(ShelterRepository shelters, EvacueeRepository evacuees, UserRepository users,
                           EventService events, NearestShelterService nearest, AccessPolicy access, AuditService audit,
                           PlatformTransactionManager txManager) {
        this.shelters = shelters;
        this.evacuees = evacuees;
        this.users = users;
        this.events = events;
        this.nearest = nearest;
        this.access = access;
        this.audit = audit;
        this.tx = new TransactionTemplate(txManager);
    }

    public CheckInResponse checkIn(Long shelterId, CheckInRequest req, AppUserPrincipal actor) {
        try {
            return withRetry(() -> doCheckIn(shelterId, req, actor));
        } catch (CapacityExceededException full) {
            // Attach alternatives so the volunteer immediately knows where to send the family.
            List<NearestShelterResponse> alternatives = tx.execute(status ->
                    nearest.nearestFrom(shelters.findWithDistrict(shelterId).orElseThrow(), 1, 3, 50));
            throw full.withAlternatives(alternatives);
        }
    }

    private CheckInResponse doCheckIn(Long shelterId, CheckInRequest req, AppUserPrincipal actor) {
        Shelter shelter = shelters.findWithDistrict(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter", shelterId));
        access.assertCanCheckIn(actor, shelter);
        if (!shelter.isActive()) throw new BusinessRuleException("SHELTER_INACTIVE", "Shelter is not active");
        DisasterEvent event = events.requireActive(req.eventId());

        shelter.admit(1);   // throws CapacityExceededException when full
        // Update the shelter FIRST: the version-checked UPDATE takes the row lock immediately, so a competing
        // check-in waits and then fails cleanly with an optimistic-lock error.
        // (Inserting the evacuee first caused a real deadlock in ConcurrentCheckInIT: both transactions held a
        // shared lock on the shelter row from the foreign-key check and then both wanted the exclusive lock.)
        shelters.saveAndFlush(shelter);
        Evacuee evacuee = evacuees.save(new Evacuee(event, shelter, req.fullName(), req.age(), req.gender(),
                req.village(), req.specialNeed(), LocalDateTime.now(), users.getReferenceById(actor.id())));
        audit.log(actor.id(), "EVACUEE_CHECKED_IN", "EVACUEE", evacuee.getId(), "shelter=" + shelterId);
        return new CheckInResponse(EvacueeResponse.from(evacuee, access.canSeeSensitive(actor)), ShelterResponse.from(shelter));
    }

    public CheckInResponse checkOut(Long evacueeId, AppUserPrincipal actor) {
        return withRetry(() -> {
            Evacuee evacuee = evacuees.findWithShelter(evacueeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Evacuee", evacueeId));
            Shelter shelter = evacuee.getShelter();
            access.assertCanOperate(actor, shelter);
            if (evacuee.isCheckedOut()) throw new InvalidStateException("Evacuee is already checked out");
            shelter.release(1);
            shelters.saveAndFlush(shelter);          // shelter row first, same lock order as check-in
            evacuee.checkOut(LocalDateTime.now(), users.getReferenceById(actor.id()));
            audit.log(actor.id(), "EVACUEE_CHECKED_OUT", "EVACUEE", evacueeId, "shelter=" + shelter.getId());
            return new CheckInResponse(EvacueeResponse.from(evacuee, access.canSeeSensitive(actor)), ShelterResponse.from(shelter));
        });
    }

    private <T> T withRetry(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return tx.execute(status -> work.get());
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException | PessimisticLockingFailureException e) {
                // optimistic conflict, or a database deadlock/lock timeout: retry with fresh data
                log.debug("Optimistic lock conflict, attempt {}/{}", attempt, MAX_ATTEMPTS);
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ConcurrentUpdateException("The shelter was updated by someone else. Please try again.");
                }
            }
        }
    }
}
