package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.response.AlertResponse;
import com.rajesh.aashraynet.dto.response.ShortageResponse;
import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shortage = stock covers less than one day at current occupancy.
 * A consumption-tracked item with no stock row at an occupied shelter counts as 0 stock (decision D5).
 */
@Service
public class ShortageService {
    private static final Logger log = LoggerFactory.getLogger(ShortageService.class);

    private final ShelterRepository shelters;
    private final SupplyItemRepository items;
    private final ShelterStockRepository stocks;
    private final ShortageAlertRepository alerts;
    private final DisasterEventRepository events;

    public ShortageService(ShelterRepository shelters, SupplyItemRepository items, ShelterStockRepository stocks,
                           ShortageAlertRepository alerts, DisasterEventRepository events) {
        this.shelters = shelters;
        this.items = items;
        this.stocks = stocks;
        this.alerts = alerts;
        this.events = events;
    }

    /** Live calculation from current stock and occupancy. districtId null = whole state. */
    @Transactional(readOnly = true)
    public List<ShortageResponse> currentShortages(Long districtId) {
        List<Shelter> occupied = shelters.findScoped(districtId, true).stream()
                .filter(s -> s.getOccupancy() > 0).toList();
        if (occupied.isEmpty()) return List.of();
        List<SupplyItem> tracked = items.findByActiveTrueOrderByName().stream()
                .filter(SupplyItem::isConsumptionTracked).toList();
        Map<ShelterStockId, ShelterStock> stockById = stocks.findForShelters(
                occupied.stream().map(Shelter::getId).toList()).stream()
                .collect(Collectors.toMap(ShelterStock::getId, Function.identity()));

        List<ShortageResponse> result = new ArrayList<>();
        for (Shelter s : occupied) {
            for (SupplyItem item : tracked) {
                ShelterStock st = stockById.get(new ShelterStockId(s.getId(), item.getId()));
                BigDecimal qty = st == null ? BigDecimal.ZERO : st.getQuantity();
                ShortageCalculator.daysRemaining(qty, s.getOccupancy(), item.getPerPersonPerDay())
                        .filter(ShortageCalculator::isShortage)
                        .ifPresent(days -> result.add(new ShortageResponse(s.getId(), s.getName(), s.getDistrictId(),
                                s.getDistrict().getName(), item.getId(), item.getName(), item.getUnit(), qty,
                                s.getOccupancy(), item.getPerPersonPerDay(), days)));
            }
        }
        result.sort(Comparator.comparing(ShortageResponse::daysRemaining).thenComparing(ShortageResponse::shelterName));
        return result;
    }

    public record DetectionResult(int opened, int refreshed, int resolved) { }

    /**
     * Idempotent detection used by the hourly job: running it twice in a row changes nothing the second time
     * (except last_checked_at). The DB unique key also prevents duplicate open alerts if two runs overlap.
     */
    @Transactional
    public DetectionResult detect() {
        Optional<DisasterEvent> event = events.findFirstByActiveTrueOrderByStartDateDesc();
        if (event.isEmpty()) {
            log.info("Shortage check skipped: no active event");
            return new DetectionResult(0, 0, 0);
        }
        Map<String, ShortageResponse> current = currentShortages(null).stream()
                .collect(Collectors.toMap(s -> key(s.shelterId(), s.itemId()), Function.identity(), (a, b) -> a));
        Map<String, ShortageAlert> open = alerts.findByResolvedAtIsNull().stream()
                .collect(Collectors.toMap(a -> key(a.getShelterId(), a.getItemId()), Function.identity(), (a, b) -> a));

        int opened = 0, refreshed = 0, resolved = 0;
        for (var entry : current.entrySet()) {
            ShortageResponse s = entry.getValue();
            ShortageAlert existing = open.get(entry.getKey());
            if (existing != null) {
                existing.refresh(s.daysRemaining());
                refreshed++;
            } else {
                alerts.save(new ShortageAlert(event.get().getId(), s.shelterId(), s.itemId(), s.daysRemaining(),
                        s.quantity(), s.occupancy()));
                opened++;
            }
        }
        for (var entry : open.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                entry.getValue().resolve();
                resolved++;
            }
        }
        log.info("Shortage check: {} opened, {} refreshed, {} resolved", opened, refreshed, resolved);
        return new DetectionResult(opened, refreshed, resolved);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> alerts(Long districtId, Long shelterId, boolean openOnly) {
        List<Shelter> scope = shelters.findScoped(districtId, false).stream()
                .filter(s -> shelterId == null || s.getId().equals(shelterId)).toList();
        if (scope.isEmpty()) return List.of();
        Map<Long, String> shelterNames = scope.stream().collect(Collectors.toMap(Shelter::getId, Shelter::getName));
        Map<Long, String> itemNames = items.findAll().stream().collect(Collectors.toMap(SupplyItem::getId, SupplyItem::getName));
        List<ShortageAlert> list = openOnly
                ? alerts.findByShelterIdInAndResolvedAtIsNullOrderByDaysRemainingAsc(shelterNames.keySet())
                : alerts.findByShelterIdInOrderByDetectedAtDesc(shelterNames.keySet());
        return list.stream().map(a -> new AlertResponse(a.getId(), a.getEventId(), a.getShelterId(),
                shelterNames.get(a.getShelterId()), a.getItemId(), itemNames.get(a.getItemId()), a.getDaysRemaining(),
                a.getQuantityAtDetection(), a.getOccupancyAtDetection(), a.getDetectedAt(), a.getLastCheckedAt(),
                a.getResolvedAt(), a.isOpen())).toList();
    }

    private static String key(Long shelterId, Long itemId) {
        return shelterId + ":" + itemId;
    }
}
