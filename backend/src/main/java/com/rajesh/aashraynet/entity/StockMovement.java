package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ledger row: every stock change is recorded here. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shelter_id", nullable = false)
    private Long shelterId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "event_id")
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 16)
    private MovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityDelta;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    /** Set only for RECEIPT. UNIQUE(request_id, item_id) in the DB = a request line can be received once. */
    @Column(name = "request_id")
    private Long requestId;

    @Column(length = 255)
    private String reason;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected StockMovement() { }

    public StockMovement(Long shelterId, Long itemId, Long eventId, MovementType type, BigDecimal delta,
                         BigDecimal balanceAfter, Long requestId, String reason, Long performedBy) {
        this.shelterId = shelterId;
        this.itemId = itemId;
        this.eventId = eventId;
        this.movementType = type;
        this.quantityDelta = delta;
        this.balanceAfter = balanceAfter;
        this.requestId = requestId;
        this.reason = reason;
        this.performedBy = performedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getShelterId() { return shelterId; }
    public Long getItemId() { return itemId; }
    public Long getEventId() { return eventId; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getQuantityDelta() { return quantityDelta; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Long getRequestId() { return requestId; }
    public String getReason() { return reason; }
    public Long getPerformedBy() { return performedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
