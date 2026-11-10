package com.rajesh.aashraynet.dto.response;

import java.math.BigDecimal;

public record StockResponse(Long itemId, String itemName, String unit, BigDecimal quantity, BigDecimal perPersonPerDay,
                            BigDecimal daysRemaining, boolean shortage) { }
