package com.rajesh.aashraynet.entity;

import com.rajesh.aashraynet.exception.InvalidStateException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Header of a supply request; lines hold the items. The state machine lives here so it can be unit tested. */
@Entity
@Table(name = "supply_requests")
public class SupplyRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private DisasterEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id", nullable = false)
    private Shelter shelter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RequestStatus status = RequestStatus.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatched_by")
    private User dispatchedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestLine> lines = new ArrayList<>();

    protected SupplyRequest() { }

    public SupplyRequest(DisasterEvent event, Shelter shelter, User requestedBy, String note) {
        this.event = event;
        this.shelter = shelter;
        this.requestedBy = requestedBy;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public void addLine(SupplyItem item, BigDecimal quantity) {
        boolean duplicate = lines.stream().anyMatch(l -> l.getItem().getId().equals(item.getId()));
        if (duplicate) throw new IllegalArgumentException("Item " + item.getName() + " appears twice in the request");
        lines.add(new RequestLine(this, item, quantity));
    }

    // ---- state machine --------------------------------------------------------

    public void approve(User admin) {
        moveTo(RequestStatus.APPROVED);
        this.approvedBy = admin;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject(User admin, String reason) {
        moveTo(RequestStatus.REJECTED);
        this.approvedBy = admin;          // the deciding admin (DB check requires it)
        this.rejectionReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    public void dispatch(User admin) {
        moveTo(RequestStatus.DISPATCHED);
        this.dispatchedBy = admin;
        this.dispatchedAt = LocalDateTime.now();
    }

    public void receive(User incharge) {
        moveTo(RequestStatus.RECEIVED);
        this.receivedBy = incharge;
        this.receivedAt = LocalDateTime.now();
    }

    private void moveTo(RequestStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStateException("Supply request " + id + " cannot move from " + status + " to " + next);
        }
        this.status = next;
    }

    public Long getId() { return id; }
    public DisasterEvent getEvent() { return event; }
    public Shelter getShelter() { return shelter; }
    public RequestStatus getStatus() { return status; }
    public User getRequestedBy() { return requestedBy; }
    public User getApprovedBy() { return approvedBy; }
    public User getDispatchedBy() { return dispatchedBy; }
    public User getReceivedBy() { return receivedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public List<RequestLine> getLines() { return Collections.unmodifiableList(lines); }
}
