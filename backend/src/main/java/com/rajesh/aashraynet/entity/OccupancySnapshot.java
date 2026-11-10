package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_snapshots")
public class OccupancySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "shelter_id", nullable = false)
    private Long shelterId;

    @Column(nullable = false)
    private int occupancy;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    protected OccupancySnapshot() { }

    public OccupancySnapshot(Long eventId, Long shelterId, int occupancy, int capacity, LocalDateTime takenAt) {
        this.eventId = eventId;
        this.shelterId = shelterId;
        this.occupancy = occupancy;
        this.capacity = capacity;
        this.takenAt = takenAt;
    }

    public Long getShelterId() { return shelterId; }
    public int getOccupancy() { return occupancy; }
    public LocalDateTime getTakenAt() { return takenAt; }
}
