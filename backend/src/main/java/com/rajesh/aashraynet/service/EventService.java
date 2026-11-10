package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.EventRequest;
import com.rajesh.aashraynet.dto.response.EventResponse;
import com.rajesh.aashraynet.entity.DisasterEvent;
import com.rajesh.aashraynet.exception.BusinessRuleException;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.DisasterEventRepository;
import com.rajesh.aashraynet.repository.UserRepository;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {
    private final DisasterEventRepository events;
    private final UserRepository users;
    private final AuditService audit;

    public EventService(DisasterEventRepository events, UserRepository users, AuditService audit) {
        this.events = events;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public EventResponse create(EventRequest req, AppUserPrincipal actor) {
        validateDates(req);
        if (events.existsByNameIgnoreCase(req.name().trim())) {
            throw new BusinessRuleException("EVENT_EXISTS", "An event with this name already exists");
        }
        DisasterEvent e = new DisasterEvent(req.name().trim(), req.startDate(), req.endDate(), users.getReferenceById(actor.id()));
        if (req.active() != null) e.setActive(req.active());
        e = events.save(e);
        audit.log(actor.id(), "EVENT_CREATED", "EVENT", e.getId(), null);
        return EventResponse.from(e);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(boolean activeOnly) {
        List<DisasterEvent> list = activeOnly ? events.findByActiveTrueOrderByStartDateDesc()
                : events.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
        return list.stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) {
        return EventResponse.from(find(id));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req, AppUserPrincipal actor) {
        validateDates(req);
        DisasterEvent e = find(id);
        e.setName(req.name().trim());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        if (req.active() != null) e.setActive(req.active());
        audit.log(actor.id(), "EVENT_UPDATED", "EVENT", id, "active=" + e.isActive());
        return EventResponse.from(e);
    }

    /** Used by check-in and supply requests: operational records may only be attached to an active event. */
    @Transactional(readOnly = true)
    public DisasterEvent requireActive(Long id) {
        DisasterEvent e = find(id);
        if (!e.isActive()) throw new BusinessRuleException("EVENT_INACTIVE", "Event " + id + " is not active");
        return e;
    }

    private DisasterEvent find(Long id) {
        return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    private static void validateDates(EventRequest req) {
        if (req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("INVALID_DATES", "End date cannot be before start date");
        }
    }
}
