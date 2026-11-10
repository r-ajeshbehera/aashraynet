package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** At most one OPEN alert per shelter+item (DB unique key on a generated column). */
@Entity
@Table(name = "shortage_alerts")
public class ShortageAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "shelter_id", nullable = false)
    private Long shelterId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "days_remaining", nullable = false, precision = 8, scale = 2)
    private BigDecimal daysRemaining;

    @Column(name = "quantity_at_detection", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityAtDetection;

    @Column(name = "occupancy_at_detection", nullable = false)
    private int occupancyAtDetection;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "last_checked_at", nullable = false)
    private LocalDateTime lastCheckedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected ShortageAlert() { }

    public ShortageAlert(Long eventId, Long shelterId, Long itemId, BigDecimal daysRemaining, BigDecimal quantity, int occupancy) {
        LocalDateTime now = LocalDateTime.now();
        this.eventId = eventId;
        this.shelterId = shelterId;
        this.itemId = itemId;
        this.daysRemaining = daysRemaining;
        this.quantityAtDetection = quantity;
        this.occupancyAtDetection = occupancy;
        this.detectedAt = now;
        this.lastCheckedAt = now;
    }

    public void refresh(BigDecimal daysRemaining) {
        this.daysRemaining = daysRemaining;
        this.lastCheckedAt = LocalDateTime.now();
    }

    public void resolve() { this.resolvedAt = LocalDateTime.now(); }

    public boolean isOpen() { return resolvedAt == null; }

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public Long getShelterId() { return shelterId; }
    public Long getItemId() { return itemId; }
    public BigDecimal getDaysRemaining() { return daysRemaining; }
    public BigDecimal getQuantityAtDetection() { return quantityAtDetection; }
    public int getOccupancyAtDetection() { return occupancyAtDetection; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
