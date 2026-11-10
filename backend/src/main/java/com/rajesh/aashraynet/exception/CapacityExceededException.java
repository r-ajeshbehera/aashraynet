package com.rajesh.aashraynet.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

/** 422 SHELTER_FULL. The service attaches nearest available alternatives before it reaches the client. */
public class CapacityExceededException extends ApiException {
    private final Long shelterId;
    private final int freePlaces;
    private final int requested;
    private final List<?> alternatives;

    public CapacityExceededException(Long shelterId, int freePlaces, int requested) {
        this(shelterId, freePlaces, requested, List.of());
    }

    private CapacityExceededException(Long shelterId, int freePlaces, int requested, List<?> alternatives) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "SHELTER_FULL",
                "Shelter has " + freePlaces + " free place(s); " + requested + " needed");
        this.shelterId = shelterId;
        this.freePlaces = freePlaces;
        this.requested = requested;
        this.alternatives = alternatives;
    }

    public CapacityExceededException withAlternatives(List<?> alternatives) {
        return new CapacityExceededException(shelterId, freePlaces, requested, alternatives);
    }

    public Long getShelterId() { return shelterId; }
    public int getFreePlaces() { return freePlaces; }
    public int getRequested() { return requested; }
    public List<?> getAlternatives() { return alternatives; }
}
