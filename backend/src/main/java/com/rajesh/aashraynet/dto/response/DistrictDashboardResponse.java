package com.rajesh.aashraynet.dto.response;

import java.util.List;
import java.util.Map;

public record DistrictDashboardResponse(
        Long districtId, String districtName,
        long totalEvacuees, int activeShelters, int totalCapacity, int availableCapacity,
        Map<String, Long> sheltersByStatus,
        List<ShelterOccupancy> occupancyByShelter,
        Map<String, Long> specialNeeds,          // null for roles that may not see it
        long pendingSupplyRequests,
        Map<String, Long> supplyRequestsByStatus,
        List<ShortageResponse> shortages) {

    public record ShelterOccupancy(Long shelterId, String name, int capacity, int occupancy, double percent, String status) { }
}
