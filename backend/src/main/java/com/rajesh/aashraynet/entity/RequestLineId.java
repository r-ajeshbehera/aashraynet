package com.rajesh.aashraynet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RequestLineId implements Serializable {
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "item_id")
    private Long itemId;

    protected RequestLineId() { }

    public RequestLineId(Long requestId, Long itemId) {
        this.requestId = requestId;
        this.itemId = itemId;
    }

    public Long getRequestId() { return requestId; }
    public Long getItemId() { return itemId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestLineId that)) return false;
        return Objects.equals(requestId, that.requestId) && Objects.equals(itemId, that.itemId);
    }

    @Override public int hashCode() { return Objects.hash(requestId, itemId); }
}
