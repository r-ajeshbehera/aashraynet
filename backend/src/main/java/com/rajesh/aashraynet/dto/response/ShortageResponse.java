package com.rajesh.aashraynet.dto.response;

import java.math.BigDecimal;

public record ShortageResponse(Long shelterId, String shelterName, Long districtId, String districtName, Long itemId,
                               String itemName, String unit, BigDecimal quantity, int occupancy,
                               BigDecimal perPersonPerDay, BigDecimal daysRemaining) { }
