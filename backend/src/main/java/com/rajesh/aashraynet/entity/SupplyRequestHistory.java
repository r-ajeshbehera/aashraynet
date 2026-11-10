package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_request_history")
public class SupplyRequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 12)
    private RequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 12)
    private RequestStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected SupplyRequestHistory() { }

    public SupplyRequestHistory(Long requestId, RequestStatus from, RequestStatus to, Long changedBy, String note) {
        this.requestId = requestId;
        this.fromStatus = from;
        this.toStatus = to;
        this.changedBy = changedBy;
        this.note = note;
        this.changedAt = LocalDateTime.now();
    }

    public Long getRequestId() { return requestId; }
    public RequestStatus getFromStatus() { return fromStatus; }
    public RequestStatus getToStatus() { return toStatus; }
    public Long getChangedBy() { return changedBy; }
    public String getNote() { return note; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
