package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.RequestStatus;
import com.rajesh.aashraynet.entity.SupplyRequest;
import com.rajesh.aashraynet.entity.SupplyRequestHistory;
import com.rajesh.aashraynet.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SupplyRequestResponse(Long id, Long eventId, Long shelterId, String shelterName, Long districtId,
                                    RequestStatus status, String note, String rejectionReason,
                                    String requestedBy, String approvedBy, String dispatchedBy, String receivedBy,
                                    LocalDateTime createdAt, LocalDateTime decidedAt, LocalDateTime dispatchedAt,
                                    LocalDateTime receivedAt, List<Line> lines, List<History> history) {

    public record Line(Long itemId, String itemName, String unit, BigDecimal quantity) { }

    public record History(RequestStatus fromStatus, RequestStatus toStatus, Long changedBy, String note,
                          LocalDateTime changedAt) { }

    public static SupplyRequestResponse from(SupplyRequest r, List<SupplyRequestHistory> history) {
        return new SupplyRequestResponse(r.getId(), r.getEvent().getId(), r.getShelter().getId(), r.getShelter().getName(),
                r.getShelter().getDistrictId(), r.getStatus(), r.getNote(), r.getRejectionReason(),
                name(r.getRequestedBy()), name(r.getApprovedBy()), name(r.getDispatchedBy()), name(r.getReceivedBy()),
                r.getCreatedAt(), r.getDecidedAt(), r.getDispatchedAt(), r.getReceivedAt(),
                r.getLines().stream().map(l -> new Line(l.getItem().getId(), l.getItem().getName(), l.getItem().getUnit(),
                        l.getQuantity())).toList(),
                history.stream().map(h -> new History(h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getNote(),
                        h.getChangedAt())).toList());
    }

    private static String name(User u) { return u == null ? null : u.getFullName(); }
}
