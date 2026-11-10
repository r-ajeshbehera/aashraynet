package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.response.*;
import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.*;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard numbers come only from database queries.
 * Date filters apply to evacuee counts (check-in time); shelter status is always the current state.
 */
@Service
public class DashboardService {
    private final ShelterRepository shelters;
    private final EvacueeRepository evacuees;
    private final SupplyRequestRepository requests;
    private final DistrictRepository districts;
    private final DisasterEventRepository events;
    private final ShortageService shortages;
    private final AccessPolicy access;

    public DashboardService(ShelterRepository shelters, EvacueeRepository evacuees, SupplyRequestRepository requests,
                            DistrictRepository districts, DisasterEventRepository events, ShortageService shortages,
                            AccessPolicy access) {
        this.shelters = shelters;
        this.evacuees = evacuees;
        this.requests = requests;
        this.districts = districts;
        this.events = events;
        this.shortages = shortages;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public DistrictDashboardResponse district(Long districtId, Long eventId, Long shelterId, LocalDate from, LocalDate to,
                                              AppUserPrincipal actor) {
        access.assertDistrict(actor, districtId);
        District district = districts.findById(districtId).orElseThrow(() -> new ResourceNotFoundException("District", districtId));
        List<Shelter> list = shelters.findScoped(districtId, true).stream()
                .filter(s -> shelterId == null || s.getId().equals(shelterId)).toList();
        LocalDateTime fromDt = from == null ? null : from.atStartOfDay();
        LocalDateTime toDt = to == null ? null : to.plusDays(1).atStartOfDay();

        long totalEvacuees = evacuees.countActive(districtId, eventId, shelterId, fromDt, toDt);
        Map<String, Long> byStatus = statusCounts(list);
        List<DistrictDashboardResponse.ShelterOccupancy> occupancy = list.stream()
                .map(s -> new DistrictDashboardResponse.ShelterOccupancy(s.getId(), s.getName(), s.getCapacity(),
                        s.getOccupancy(), s.occupancyPercent(), s.status().name()))
                .sorted(Comparator.comparingDouble(DistrictDashboardResponse.ShelterOccupancy::percent).reversed())
                .toList();

        Map<String, Long> needs = null;
        if (access.canSeeSensitive(actor)) {
            needs = new LinkedHashMap<>();
            for (SpecialNeed n : SpecialNeed.values()) needs.put(n.name(), 0L);
            for (Object[] row : evacuees.countBySpecialNeed(districtId, eventId, shelterId, fromDt, toDt)) {
                needs.put(((SpecialNeed) row[0]).name(), (Long) row[1]);
            }
        }
        Map<String, Long> requestsByStatus = requestCounts(requests.countByStatus(districtId, eventId, shelterId));
        List<ShortageResponse> shortageList = shortages.currentShortages(districtId).stream()
                .filter(s -> shelterId == null || s.shelterId().equals(shelterId)).toList();

        return new DistrictDashboardResponse(districtId, district.getName(), totalEvacuees, list.size(),
                list.stream().mapToInt(Shelter::getCapacity).sum(), list.stream().mapToInt(Shelter::freePlaces).sum(),
                byStatus, occupancy, needs, requestsByStatus.get(RequestStatus.REQUESTED.name()), requestsByStatus,
                shortageList);
    }

    @Transactional(readOnly = true)
    public StateDashboardResponse state(Long eventId) {
        List<Shelter> all = shelters.findScoped(null, true);
        Map<Long, Long> evacueesByDistrict = new HashMap<>();
        for (Object[] row : evacuees.countActiveByDistrict(eventId)) evacueesByDistrict.put((Long) row[0], (Long) row[1]);
        List<ShortageResponse> shortageList = shortages.currentShortages(null);
        Map<Long, List<Shelter>> byDistrict = all.stream().collect(Collectors.groupingBy(Shelter::getDistrictId));

        List<StateDashboardResponse.DistrictSummary> summaries = districts.findAll().stream()
                .sorted(Comparator.comparing(District::getName))
                .map(d -> {
                    List<Shelter> ds = byDistrict.getOrDefault(d.getId(), List.of());
                    long pending = requestCounts(requests.countByStatus(d.getId(), eventId, null))
                            .get(RequestStatus.REQUESTED.name());
                    return new StateDashboardResponse.DistrictSummary(d.getId(), d.getName(), ds.size(),
                            ds.stream().mapToInt(Shelter::getCapacity).sum(),
                            ds.stream().mapToInt(Shelter::getOccupancy).sum(),
                            evacueesByDistrict.getOrDefault(d.getId(), 0L),
                            (int) ds.stream().filter(s -> s.status() == OccupancyStatus.FULL).count(),
                            (int) shortageList.stream().filter(s -> s.districtId().equals(d.getId())).count(),
                            pending);
                }).toList();

        return new StateDashboardResponse(
                events.findByActiveTrueOrderByStartDateDesc().stream().map(EventResponse::from).toList(),
                evacueesByDistrict.values().stream().mapToLong(Long::longValue).sum(),
                all.stream().mapToInt(Shelter::getCapacity).sum(),
                all.stream().mapToInt(Shelter::freePlaces).sum(),
                statusCounts(all),
                requestCounts(requests.countByStatus(null, eventId, null)),
                shortageList.size(), summaries);
    }

    private static Map<String, Long> statusCounts(List<Shelter> list) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (OccupancyStatus st : OccupancyStatus.values()) m.put(st.name(), 0L);
        list.forEach(s -> m.merge(s.status().name(), 1L, Long::sum));
        return m;
    }

    private static Map<String, Long> requestCounts(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (RequestStatus st : RequestStatus.values()) m.put(st.name(), 0L);
        for (Object[] row : rows) m.put(((RequestStatus) row[0]).name(), (Long) row[1]);
        return m;
    }
}
