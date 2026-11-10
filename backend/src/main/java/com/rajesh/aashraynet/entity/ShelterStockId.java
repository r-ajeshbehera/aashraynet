package com.rajesh.aashraynet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Composite primary key (shelter_id, item_id). */
@Embeddable
public class ShelterStockId implements Serializable {
    @Column(name = "shelter_id")
    private Long shelterId;

    @Column(name = "item_id")
    private Long itemId;

    protected ShelterStockId() { }

    public ShelterStockId(Long shelterId, Long itemId) {
        this.shelterId = shelterId;
        this.itemId = itemId;
    }

    public Long getShelterId() { return shelterId; }
    public Long getItemId() { return itemId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShelterStockId that)) return false;
        return Objects.equals(shelterId, that.shelterId) && Objects.equals(itemId, that.itemId);
    }

    @Override public int hashCode() { return Objects.hash(shelterId, itemId); }
}
