package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.MovementType;
import com.rajesh.aashraynet.entity.StockMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementResponse(Long id, Long itemId, MovementType type, BigDecimal quantityDelta, BigDecimal balanceAfter,
                               Long requestId, String reason, Long performedBy, LocalDateTime createdAt) {
    public static MovementResponse from(StockMovement m) {
        return new MovementResponse(m.getId(), m.getItemId(), m.getMovementType(), m.getQuantityDelta(),
                m.getBalanceAfter(), m.getRequestId(), m.getReason(), m.getPerformedBy(), m.getCreatedAt());
    }
}
