package com.rajesh.aashraynet.entity;

import java.util.EnumSet;
import java.util.Set;

/** Supply request workflow: REQUESTED -> APPROVED -> DISPATCHED -> RECEIVED, or REQUESTED -> REJECTED. */
public enum RequestStatus {
    REQUESTED, APPROVED, REJECTED, DISPATCHED, RECEIVED;

    public Set<RequestStatus> allowedNext() {
        return switch (this) {
            case REQUESTED -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(DISPATCHED);
            case DISPATCHED -> EnumSet.of(RECEIVED);
            case REJECTED, RECEIVED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    public boolean canMoveTo(RequestStatus next) {
        return allowedNext().contains(next);
    }
}
