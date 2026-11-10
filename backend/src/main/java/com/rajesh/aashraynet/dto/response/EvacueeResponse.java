package com.rajesh.aashraynet.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajesh.aashraynet.entity.Evacuee;
import com.rajesh.aashraynet.entity.Gender;
import com.rajesh.aashraynet.entity.SpecialNeed;

import java.time.LocalDateTime;

/** specialNeed is null (and omitted from JSON) for roles that may not see it. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvacueeResponse(Long id, Long eventId, Long shelterId, String shelterName, String fullName, int age,
                              Gender gender, String village, SpecialNeed specialNeed, LocalDateTime checkedInAt,
                              LocalDateTime checkedOutAt) {
    public static EvacueeResponse from(Evacuee e, boolean includeSensitive) {
        return new EvacueeResponse(e.getId(), e.getEvent().getId(), e.getShelter().getId(), e.getShelter().getName(),
                e.getFullName(), e.getAge(), e.getGender(), e.getVillage(),
                includeSensitive ? e.getSpecialNeed() : null, e.getCheckedInAt(), e.getCheckedOutAt());
    }
}
