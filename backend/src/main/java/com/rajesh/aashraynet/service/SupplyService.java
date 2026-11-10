package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.CreateSupplyRequest;
import com.rajesh.aashraynet.dto.request.SupplyItemRequest;
import com.rajesh.aashraynet.entity.*;
import com.rajesh.aashraynet.dto.response.SupplyRequestResponse;
import com.rajesh.aashraynet.exception.BusinessRuleException;
import com.rajesh.aashraynet.exception.InvalidStateException;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.*;
import com.rajesh.aashraynet.security.AccessPolicy;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * REQUESTED -> APPROVED -> DISPATCHED -> RECEIVED (or REQUESTED -> REJECTED).
 * Receipt updates stock exactly once, guaranteed three ways:
 *  - state machine: RECEIVED cannot be entered twice,
 *  - @Version on supply_requests: two simultaneous "receive" clicks -> one fails with 409,
 *  - UNIQUE(request_id, item_id) on stock_movements: the database refuses a second receipt row.
 */
@Service
public class SupplyService {
    private final SupplyRequestRepository requests;
    private final SupplyRequestHistoryRepository history;
    private final SupplyItemRepository items;
    private final UserRepository users;
    private final ShelterService shelters;
    private final EventService events;
    private final StockService stock;
    private final AccessPolicy access;
    private final AuditService audit;

    public SupplyService(SupplyRequestRepository requests, SupplyRequestHistoryRepository history,
                         SupplyItemRepository items, UserRepository users, ShelterService shelters, EventService events,
                         StockService stock, AccessPolicy access, AuditService audit) {
        this.requests = requests;
        this.history = history;
        this.items = items;
        this.users = users;
        this.shelters = shelters;
        this.events = events;
        this.stock = stock;
        this.access = access;
        this.audit = audit;
    }

    // ---- catalogue -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SupplyItem> catalogue() {
        return items.findByActiveTrueOrderByName();
    }

    @Transactional
    public SupplyItem createItem(SupplyItemRequest req, AppUserPrincipal actor) {
        if (items.existsByNameIgnoreCase(req.name().trim())) {
            throw new BusinessRuleException("ITEM_EXISTS", "Item already exists");
        }
        SupplyItem item = items.save(new SupplyItem(req.name().trim(), req.unit().trim(), req.perPersonPerDay()));
        audit.log(actor.id(), "ITEM_CREATED", "SUPPLY_ITEM", item.getId(), null);
        return item;
    }

    // ---- workflow ------------------------------------------------------------------

    @Transactional
    public SupplyRequestResponse create(Long shelterId, CreateSupplyRequest req, AppUserPrincipal actor) {
        Shelter shelter = shelters.load(shelterId);
        access.assertIncharge(actor, shelter);
        DisasterEvent event = events.requireActive(req.eventId());
        SupplyRequest r = new SupplyRequest(event, shelter, users.getReferenceById(actor.id()), req.note());
        for (CreateSupplyRequest.Line line : req.lines()) {
            SupplyItem item = items.findById(line.itemId())
                    .filter(SupplyItem::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Supply item", line.itemId()));
            r.addLine(item, line.quantity());
        }
        r = requests.save(r);
        record(r, null, RequestStatus.REQUESTED, actor, req.note());
        return toResponse(r);
    }

    @Transactional
    public SupplyRequestResponse approve(Long id, AppUserPrincipal actor) {
        return adminStep(id, actor, (r, u) -> r.approve(u), null);
    }

    @Transactional
    public SupplyRequestResponse reject(Long id, String reason, AppUserPrincipal actor) {
        return adminStep(id, actor, (r, u) -> r.reject(u, reason), reason);
    }

    @Transactional
    public SupplyRequestResponse dispatch(Long id, AppUserPrincipal actor) {
        return adminStep(id, actor, (r, u) -> r.dispatch(u), null);
    }

    @Transactional
    public SupplyRequestResponse receive(Long id, AppUserPrincipal actor) {
        SupplyRequest r = load(id);
        access.assertIncharge(actor, r.getShelter());
        if (r.getStatus() == RequestStatus.RECEIVED) {
            throw new InvalidStateException("Supply request " + id + " was already received");
        }
        RequestStatus from = r.getStatus();
        User me = users.getReferenceById(actor.id());
        r.receive(me);                      // throws if not DISPATCHED
        requests.saveAndFlush(r);           // version check before touching stock
        for (RequestLine line : r.getLines()) {
            stock.addReceipt(r.getShelter(), line.getItem(), line.getQuantity(), r.getEvent().getId(), r.getId(), actor.id());
        }
        record(r, from, RequestStatus.RECEIVED, actor, null);
        return toResponse(r);
    }

    private SupplyRequestResponse adminStep(Long id, AppUserPrincipal actor, BiConsumer<SupplyRequest, User> step, String note) {
        SupplyRequest r = load(id);
        access.assertDistrictAdmin(actor, r.getShelter());
        RequestStatus from = r.getStatus();
        step.accept(r, users.getReferenceById(actor.id()));
        requests.saveAndFlush(r);
        record(r, from, r.getStatus(), actor, note);
        return toResponse(r);
    }

    // ---- queries -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<SupplyRequestResponse> list(Long districtId, Long shelterId, RequestStatus status, Pageable pageable,
                                            AppUserPrincipal actor) {
        Long scopeDistrict = actor.isStateAdmin() ? districtId : actor.districtId();
        Long scopeShelter = shelterId;
        if (actor.role() == Role.SHELTER_INCHARGE) {
            if (shelterId == null) {
                throw new BusinessRuleException("SHELTER_REQUIRED", "shelterId is required for shelter in-charges");
            }
            access.assertCanView(actor, shelters.load(shelterId));
        }
        return requests.search(scopeDistrict, scopeShelter, status, pageable)
                .map(r -> SupplyRequestResponse.from(r, List.of()));
    }

    @Transactional(readOnly = true)
    public SupplyRequestResponse get(Long id, AppUserPrincipal actor) {
        SupplyRequest r = load(id);
        access.assertCanOperate(actor, r.getShelter());
        return toResponse(r);
    }

    private SupplyRequest load(Long id) {
        return requests.findWithShelter(id).orElseThrow(() -> new ResourceNotFoundException("Supply request", id));
    }

    private void record(SupplyRequest r, RequestStatus from, RequestStatus to, AppUserPrincipal actor, String note) {
        history.save(new SupplyRequestHistory(r.getId(), from, to, actor.id(), note));
        audit.log(actor.id(), "SUPPLY_" + to, "SUPPLY_REQUEST", r.getId(), from == null ? null : from + "->" + to);
    }

    private SupplyRequestResponse toResponse(SupplyRequest r) {
        return SupplyRequestResponse.from(r, history.findByRequestIdOrderByChangedAtAsc(r.getId()));
    }
}
