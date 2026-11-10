package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.response.DistrictDashboardResponse;
import com.rajesh.aashraynet.dto.response.StateDashboardResponse;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboards")
public class DashboardController {
    private final DashboardService dashboards;

    public DashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/district/{id}")
    @PreAuthorize("hasAnyRole('DISTRICT_ADMIN','STATE_ADMIN')")
    public DistrictDashboardResponse district(@PathVariable Long id,
                                              @RequestParam(name = "event", required = false) Long eventId,
                                              @RequestParam(name = "shelter", required = false) Long shelterId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboards.district(id, eventId, shelterId, from, to, CurrentUser.get());
    }

    @GetMapping("/state")
    @PreAuthorize("hasRole('STATE_ADMIN')")
    public StateDashboardResponse state(@RequestParam(name = "event", required = false) Long eventId) {
        return dashboards.state(eventId);
    }
}
