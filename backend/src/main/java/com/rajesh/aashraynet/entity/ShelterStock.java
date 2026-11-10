package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shelter_stock")
public class ShelterStock {
    @EmbeddedId
    private ShelterStockId id;

    @MapsId("shelterId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private SupplyItem item;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Version
    private Long version;

    protected ShelterStock() { }

    public ShelterStock(Shelter shelter, SupplyItem item) {
        this.id = new ShelterStockId(shelter.getId(), item.getId());
        this.shelter = shelter;
        this.item = item;
    }

    /** Applies a signed change and returns the new balance. Never allows negative stock. */
    public BigDecimal apply(BigDecimal delta) {
        BigDecimal next = quantity.add(delta);
        if (next.signum() < 0) {
            throw new IllegalArgumentException("Stock cannot become negative (current " + quantity + ", change " + delta + ")");
        }
        quantity = next;
        return quantity;
    }

    public ShelterStockId getId() { return id; }
    public Shelter getShelter() { return shelter; }
    public SupplyItem getItem() { return item; }
    public BigDecimal getQuantity() { return quantity; }
}
