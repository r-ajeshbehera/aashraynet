package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.request.CheckInRequest;
import com.rajesh.aashraynet.dto.response.CheckInResponse;
import com.rajesh.aashraynet.dto.response.EvacueeResponse;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.service.CapacityService;
import com.rajesh.aashraynet.service.EvacueeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Evacuees")
public class EvacueeController {
    private final CapacityService capacity;
    private final EvacueeService evacuees;

    public EvacueeController(CapacityService capacity, EvacueeService evacuees) {
        this.capacity = capacity;
        this.evacuees = evacuees;
    }

    @PostMapping("/shelters/{id}/evacuees")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VOLUNTEER','SHELTER_INCHARGE')")
    @Operation(summary = "Check in an evacuee. 422 SHELTER_FULL includes nearest alternatives.")
    public CheckInResponse checkIn(@PathVariable Long id, @Valid @RequestBody CheckInRequest req) {
        return capacity.checkIn(id, req, CurrentUser.get());
    }

    @GetMapping("/shelters/{id}/evacuees")
    public Page<EvacueeResponse> list(@PathVariable Long id,
                                      @RequestParam(defaultValue = "true") boolean active,
                                      @RequestParam(required = false) String q,
                                      @PageableDefault(size = 20, sort = "checkedInAt") Pageable pageable) {
        return evacuees.listForShelter(id, active, q, Pages.capped(pageable), CurrentUser.get());
    }

    @GetMapping("/evacuees/{id}")
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
    public EvacueeResponse get(@PathVariable Long id) {
        return evacuees.get(id, CurrentUser.get());
    }

    @PostMapping("/evacuees/{id}/checkout")
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
    public CheckInResponse checkOut(@PathVariable Long id) {
        return capacity.checkOut(id, CurrentUser.get());
    }
}
