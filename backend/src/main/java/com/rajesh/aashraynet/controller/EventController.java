package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.request.EventRequest;
import com.rajesh.aashraynet.dto.response.DistrictResponse;
import com.rajesh.aashraynet.dto.response.EventResponse;
import com.rajesh.aashraynet.repository.DistrictRepository;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Events and districts")
public class EventController {
    private final EventService events;
    private final DistrictRepository districts;

    public EventController(EventService events, DistrictRepository districts) {
        this.events = events;
        this.districts = districts;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STATE_ADMIN')")
    public EventResponse create(@Valid @RequestBody EventRequest req) {
        return events.create(req, CurrentUser.get());
    }

    @GetMapping("/events")
    public List<EventResponse> list(@RequestParam(defaultValue = "false") boolean active) {
        return events.list(active);
    }

    @GetMapping("/events/{id}")
    public EventResponse get(@PathVariable Long id) {
        return events.get(id);
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasRole('STATE_ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest req) {
        return events.update(id, req, CurrentUser.get());
    }

    /** Public: district names are needed by the public shelter page. */
    @GetMapping("/districts")
    public List<DistrictResponse> districts() {
        return districts.findAll(Sort.by("name")).stream().map(d -> new DistrictResponse(d.getId(), d.getName())).toList();
    }
}
