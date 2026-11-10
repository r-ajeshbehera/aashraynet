package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.DisasterEvent;

import java.time.LocalDate;

public record EventResponse(Long id, String name, LocalDate startDate, LocalDate endDate, boolean active) {
    public static EventResponse from(DisasterEvent e) {
        return new EventResponse(e.getId(), e.getName(), e.getStartDate(), e.getEndDate(), e.isActive());
    }
}
