package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.OccupancyStatus;

public record NearestShelterResponse(Long shelterId, String name, String district, String village, double distanceKm,
                                     int capacity, int occupancy, int freePlaces, OccupancyStatus status,
                                     boolean hasMedical, boolean hasRamp, boolean hasGenerator) { }
