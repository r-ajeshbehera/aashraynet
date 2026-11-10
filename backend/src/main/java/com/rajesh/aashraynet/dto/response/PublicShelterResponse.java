package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.OccupancyStatus;

import java.time.LocalDateTime;

public record PublicShelterResponse(String shelterName, String district, String village, OccupancyStatus status,
                                    int freePlaces, boolean hasMedical, boolean hasRamp, LocalDateTime lastUpdated) { }
