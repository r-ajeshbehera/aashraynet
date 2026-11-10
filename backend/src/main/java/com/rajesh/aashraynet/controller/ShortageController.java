package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.response.AlertResponse;
import com.rajesh.aashraynet.dto.response.ShortageResponse;
import com.rajesh.aashraynet.entity.Role;
import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.repository.ShelterRepository;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.service.ShortageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Shortages")
@PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
public class ShortageController {
    private final ShortageService shortages;
    private final ShelterRepository shelters;

    public ShortageController(ShortageService shortages, ShelterRepository shelters) {
        this.shortages = shortages;
        this.shelters = shelters;
    }

    @GetMapping("/shortages")
    public List<ShortageResponse> shortages(@RequestParam(name = "district", required = false) Long districtId) {
        AppUserPrincipal me = CurrentUser.get();
        List<ShortageResponse> list = shortages.currentShortages(me.isStateAdmin() ? districtId : me.districtId());
        if (me.role() == Role.SHELTER_INCHARGE) {
            Set<Long> mine = myShelters(me);
            list = list.stream().filter(s -> mine.contains(s.shelterId())).toList();
        }
        return list;
    }

    @GetMapping("/alerts")
    public List<AlertResponse> alerts(@RequestParam(defaultValue = "OPEN") String status,
                                      @RequestParam(name = "district", required = false) Long districtId) {
        AppUserPrincipal me = CurrentUser.get();
        boolean openOnly = !"ALL".equalsIgnoreCase(status);
        List<AlertResponse> list = shortages.alerts(me.isStateAdmin() ? districtId : me.districtId(), null, openOnly);
        if (me.role() == Role.SHELTER_INCHARGE) {
            Set<Long> mine = myShelters(me);
            list = list.stream().filter(a -> mine.contains(a.shelterId())).toList();
        }
        return list;
    }

    private Set<Long> myShelters(AppUserPrincipal me) {
        return shelters.findByInchargeIdOrderByName(me.id()).stream().map(Shelter::getId).collect(Collectors.toSet());
    }
}
