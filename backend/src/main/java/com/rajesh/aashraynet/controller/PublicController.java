package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.response.PublicFindResponse;
import com.rajesh.aashraynet.dto.response.PublicShelterList;
import com.rajesh.aashraynet.service.PublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** No login required. Rate-limited per IP by RateLimitFilter. */
@RestController
@RequestMapping("/api/public")
@Validated
@Tag(name = "Public")
public class PublicController {
    private final PublicService publicService;

    public PublicController(PublicService publicService) {
        this.publicService = publicService;
    }

    @GetMapping("/find")
    @Operation(summary = "Find a relative by exact name + village. Returns only shelter name and district.")
    public PublicFindResponse find(@RequestParam @NotBlank @Size(max = 120) String name,
                                   @RequestParam @NotBlank @Size(max = 120) String village) {
        return publicService.find(name, village);
    }

    @GetMapping("/shelters/available")
    @Operation(summary = "Shelters with free places (demonstration data)")
    public PublicShelterList available(@RequestParam(name = "district", required = false) Long districtId) {
        return publicService.availableShelters(districtId);
    }
}
