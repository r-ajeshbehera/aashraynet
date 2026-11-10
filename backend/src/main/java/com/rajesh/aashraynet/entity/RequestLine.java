package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "request_lines")
public class RequestLine {
    @EmbeddedId
    private RequestLineId id = new RequestLineId();

    @MapsId("requestId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private SupplyRequest request;

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private SupplyItem item;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    protected RequestLine() { }

    RequestLine(SupplyRequest request, SupplyItem item, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("quantity must be positive");
        this.request = request;
        this.item = item;
        this.quantity = quantity;
    }

    public SupplyItem getItem() { return item; }
    public BigDecimal getQuantity() { return quantity; }
}
