package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.OccupancyStatus;
import com.rajesh.aashraynet.entity.Shelter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShelterResponse(Long id, Long districtId, String districtName, String name, String village,
                              BigDecimal latitude, BigDecimal longitude, int capacity, int occupancy, int freePlaces,
                              double occupancyPercent, OccupancyStatus status, boolean hasMedical, boolean hasRamp,
                              boolean hasGenerator, Long inchargeId, String inchargeName, boolean active,
                              LocalDateTime updatedAt) {
    public static ShelterResponse from(Shelter s) {
        return new ShelterResponse(s.getId(), s.getDistrictId(), s.getDistrict().getName(), s.getName(), s.getVillage(),
                s.getLatitude(), s.getLongitude(), s.getCapacity(), s.getOccupancy(), s.freePlaces(),
                s.occupancyPercent(), s.status(), s.isHasMedical(), s.isHasRamp(), s.isHasGenerator(),
                s.getIncharge() == null ? null : s.getIncharge().getId(),
                s.getIncharge() == null ? null : s.getIncharge().getFullName(), s.isActive(), s.getUpdatedAt());
    }
}
