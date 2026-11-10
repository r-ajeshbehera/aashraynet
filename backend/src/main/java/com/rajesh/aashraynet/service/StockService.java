package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.StockAdjustmentRequest;
import com.rajesh.aashraynet.dto.response.MovementResponse;
import com.rajesh.aashraynet.dto.response.StockResponse;
import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.exception.BusinessRuleException;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.ShelterStockRepository;
import com.rajesh.aashraynet.repository.StockMovementRepository;
import com.rajesh.aashraynet.repository.SupplyItemRepository;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every stock change goes through here: update shelter_stock (optimistic lock) AND write a stock_movements row,
 * in the same transaction. That keeps the current quantity and the history consistent.
 */
@Service
public class StockService {
    private final ShelterStockRepository stocks;
    private final StockMovementRepository movements;
    private final SupplyItemRepository items;
    private final ShelterService shelters;
    private final AccessPolicy access;

    public StockService(ShelterStockRepository stocks, StockMovementRepository movements, SupplyItemRepository items,
                        ShelterService shelters, AccessPolicy access) {
        this.stocks = stocks;
        this.movements = movements;
        this.items = items;
        this.shelters = shelters;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<StockResponse> stockFor(Long shelterId, AppUserPrincipal actor) {
        Shelter shelter = shelters.load(shelterId);
        access.assertCanOperate(actor, shelter);
        Map<Long, ShelterStock> byItem = stocks.findForShelter(shelterId).stream()
                .collect(Collectors.toMap(st -> st.getItem().getId(), Function.identity()));
        return items.findByActiveTrueOrderByName().stream().map(item -> {
            BigDecimal qty = byItem.containsKey(item.getId()) ? byItem.get(item.getId()).getQuantity() : BigDecimal.ZERO;
            BigDecimal days = ShortageCalculator.daysRemaining(qty, shelter.getOccupancy(), item.getPerPersonPerDay()).orElse(null);
            return new StockResponse(item.getId(), item.getName(), item.getUnit(), qty, item.getPerPersonPerDay(), days,
                    ShortageCalculator.isShortage(days));
        }).toList();
    }

    @Transactional
    public MovementResponse adjust(Long shelterId, StockAdjustmentRequest req, AppUserPrincipal actor) {
        Shelter shelter = shelters.load(shelterId);
        access.assertCanOperate(actor, shelter);
        BigDecimal delta = switch (req.type()) {
            case INITIAL, ADJUSTMENT_IN -> req.quantity();
            case CONSUMPTION, ADJUSTMENT_OUT -> req.quantity().negate();
            case RECEIPT -> throw new BusinessRuleException("USE_SUPPLY_REQUEST",
                    "Receipts are recorded by confirming a dispatched supply request");
        };
        SupplyItem item = items.findById(req.itemId()).orElseThrow(() -> new ResourceNotFoundException("Supply item", req.itemId()));
        StockMovement m = apply(shelter, item, delta, req.type(), req.eventId(), null, req.reason(), actor.id());
        return MovementResponse.from(m);
    }

    /** Called by SupplyService inside the receipt transaction. */
    @Transactional
    public StockMovement addReceipt(Shelter shelter, SupplyItem item, BigDecimal quantity, Long eventId, Long requestId,
                                    Long userId) {
        return apply(shelter, item, quantity, MovementType.RECEIPT, eventId, requestId,
                "Supply request #" + requestId + " received", userId);
    }

    private StockMovement apply(Shelter shelter, SupplyItem item, BigDecimal delta, MovementType type, Long eventId,
                                Long requestId, String reason, Long userId) {
        ShelterStock stock = stocks.findById(new ShelterStockId(shelter.getId(), item.getId()))
                .orElseGet(() -> new ShelterStock(shelter, item));
        BigDecimal balance;
        try {
            balance = stock.apply(delta);
        } catch (IllegalArgumentException negative) {
            throw new BusinessRuleException("NEGATIVE_STOCK", "Not enough " + item.getName() + " in stock for this change");
        }
        stocks.saveAndFlush(stock);
        return movements.save(new StockMovement(shelter.getId(), item.getId(), eventId, type, delta, balance,
                requestId, reason, userId));
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> movements(Long shelterId, Pageable pageable, AppUserPrincipal actor) {
        access.assertCanOperate(actor, shelters.load(shelterId));
        return movements.findByShelterIdOrderByCreatedAtDesc(shelterId, pageable).map(MovementResponse::from);
    }
}
