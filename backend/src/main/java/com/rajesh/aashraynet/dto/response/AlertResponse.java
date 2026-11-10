package com.rajesh.aashraynet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertResponse(Long id, Long eventId, Long shelterId, String shelterName, Long itemId, String itemName,
                            BigDecimal daysRemaining, BigDecimal quantityAtDetection, int occupancyAtDetection,
                            LocalDateTime detectedAt, LocalDateTime lastCheckedAt, LocalDateTime resolvedAt,
                            boolean open) { }
