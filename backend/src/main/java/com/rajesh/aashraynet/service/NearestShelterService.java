package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.config.AppProperties;
import com.rajesh.aashraynet.dto.response.NearestShelterResponse;
import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.ShelterRepository;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Recommends the nearest shelters that can take N more people. It only RECOMMENDS; it never moves anyone.
 * Step 1 (DB): active, enough free places, not the current shelter, inside a bounding box.
 * Step 2 (Java): exact Haversine distance, keep those within the radius, sort nearest first, take "count".
 */
@Service
public class NearestShelterService {
    public static final int MAX_RESULTS = 20;

    private final ShelterRepository shelters;
    private final AccessPolicy access;
    private final double defaultRadiusKm;

    public NearestShelterService(ShelterRepository shelters, AccessPolicy access, AppProperties props) {
        this.shelters = shelters;
        this.access = access;
        this.defaultRadiusKm = props.nearest().defaultRadiusKm();
    }

    @Transactional(readOnly = true)
    public List<NearestShelterResponse> nearestTo(Long shelterId, int people, int count, Double radiusKm,
                                                  AppUserPrincipal actor) {
        Shelter origin = shelters.findWithDistrict(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter", shelterId));
        access.assertCanView(actor, origin);
        return nearestFrom(origin, people, count, radiusKm == null ? defaultRadiusKm : radiusKm);
    }

    /** Also used internally when a check-in fails because the shelter is full. */
    @Transactional(readOnly = true)
    public List<NearestShelterResponse> nearestFrom(Shelter origin, int people, int count, double radiusKm) {
        validate(people, count, radiusKm);
        double lat = origin.getLatitude().doubleValue();
        double lng = origin.getLongitude().doubleValue();
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(lat, lng, radiusKm);
        List<Shelter> candidates = shelters.findCandidates(origin.getId(), people,
                box.minLat(), box.maxLat(), box.minLng(), box.maxLng());
        return rank(origin.getId(), lat, lng, candidates, people, count, radiusKm);
    }

    /**
     * Pure ranking step (unit-tested without a database). Re-checks every rule in Java as defence in depth,
     * so it is also correct if the candidate list was not pre-filtered.
     */
    public static List<NearestShelterResponse> rank(Long excludeId, double lat, double lng, List<Shelter> candidates,
                                                    int people, int count, double radiusKm) {
        record Scored(Shelter shelter, double km) { }
        return candidates.stream()
                .filter(s -> !Objects.equals(s.getId(), excludeId))
                .filter(Shelter::isActive)
                .filter(s -> s.freePlaces() >= people)
                .map(s -> new Scored(s, GeoUtils.haversineKm(lat, lng, s.getLatitude().doubleValue(), s.getLongitude().doubleValue())))
                .filter(sc -> sc.km() <= radiusKm)
                .sorted(Comparator.comparingDouble(Scored::km).thenComparing(sc -> sc.shelter().getName()))
                .limit(count)
                .map(sc -> new NearestShelterResponse(sc.shelter().getId(), sc.shelter().getName(),
                        sc.shelter().getDistrict().getName(), sc.shelter().getVillage(), GeoUtils.round2(sc.km()),
                        sc.shelter().getCapacity(), sc.shelter().getOccupancy(), sc.shelter().freePlaces(),
                        sc.shelter().status(), sc.shelter().isHasMedical(), sc.shelter().isHasRamp(),
                        sc.shelter().isHasGenerator()))
                .toList();
    }

    private static void validate(int people, int count, double radiusKm) {
        if (people < 1 || people > 10_000) throw new IllegalArgumentException("people must be between 1 and 10000");
        if (count < 1 || count > MAX_RESULTS) throw new IllegalArgumentException("count must be between 1 and " + MAX_RESULTS);
        if (radiusKm <= 0 || radiusKm > 500) throw new IllegalArgumentException("radiusKm must be between 0 and 500");
    }
}
