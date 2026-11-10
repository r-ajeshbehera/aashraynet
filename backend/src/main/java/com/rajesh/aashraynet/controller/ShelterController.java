package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.request.AssignInchargeRequest;
import com.rajesh.aashraynet.dto.request.FacilitiesRequest;
import com.rajesh.aashraynet.dto.request.ShelterRequest;
import com.rajesh.aashraynet.dto.response.NearestShelterResponse;
import com.rajesh.aashraynet.dto.response.ShelterResponse;
import com.rajesh.aashraynet.entity.OccupancyStatus;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.service.NearestShelterService;
import com.rajesh.aashraynet.service.ShelterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@Tag(name = "Shelters")
public class ShelterController {
    private final ShelterService shelters;
    private final NearestShelterService nearest;

    public ShelterController(ShelterService shelters, NearestShelterService nearest) {
        this.shelters = shelters;
        this.nearest = nearest;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STATE_ADMIN','DISTRICT_ADMIN')")
    public ShelterResponse create(@Valid @RequestBody ShelterRequest req) {
        return shelters.create(req, CurrentUser.get());
    }

    @GetMapping
    @Operation(summary = "List shelters in the caller's scope, e.g. ?district=1&status=GREEN")
    public List<ShelterResponse> list(@RequestParam(name = "district", required = false) Long districtId,
                                      @RequestParam(required = false) OccupancyStatus status,
                                      @RequestParam(defaultValue = "true") boolean activeOnly) {
        return shelters.list(districtId, status, activeOnly, CurrentUser.get());
    }

    @GetMapping("/{id}")
    public ShelterResponse get(@PathVariable Long id) {
        return shelters.get(id, CurrentUser.get());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STATE_ADMIN','DISTRICT_ADMIN')")
    public ShelterResponse update(@PathVariable Long id, @Valid @RequestBody ShelterRequest req) {
        return shelters.update(id, req, CurrentUser.get());
    }

    @PatchMapping("/{id}/facilities")
    @PreAuthorize("hasAnyRole('STATE_ADMIN','DISTRICT_ADMIN','SHELTER_INCHARGE')")
    public ShelterResponse facilities(@PathVariable Long id, @Valid @RequestBody FacilitiesRequest req) {
        return shelters.updateFacilities(id, req, CurrentUser.get());
    }

    @PatchMapping("/{id}/incharge")
    @PreAuthorize("hasAnyRole('STATE_ADMIN','DISTRICT_ADMIN')")
    public ShelterResponse incharge(@PathVariable Long id, @RequestBody AssignInchargeRequest req) {
        return shelters.assignIncharge(id, req, CurrentUser.get());
    }

    @GetMapping("/{id}/nearest-available")
    @Operation(summary = "Nearest active shelters with at least 'people' free places (Haversine distance)")
    public List<NearestShelterResponse> nearestAvailable(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "3") int count,
                                                         @RequestParam(defaultValue = "1") int people,
                                                         @RequestParam(required = false) Double radiusKm) {
        return nearest.nearestTo(id, people, count, radiusKm, CurrentUser.get());
    }
}
