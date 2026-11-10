package com.rajesh.aashraynet.dto.response;

import java.util.List;
import java.util.Map;

public record StateDashboardResponse(List<EventResponse> activeEvents, long totalEvacuees, int totalCapacity,
                                     int availableCapacity, Map<String, Long> sheltersByStatus,
                                     Map<String, Long> supplyRequestsByStatus, int openShortages,
                                     List<DistrictSummary> districts) {

    public record DistrictSummary(Long districtId, String districtName, int shelters, int capacity, int occupancy,
                                  long evacuees, int fullShelters, int shortages, long pendingRequests) { }
}
