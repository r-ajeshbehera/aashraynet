package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "supply_items")
public class SupplyItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String unit;

    /** Daily need per person (0 = not consumption-tracked, e.g. blankets). Stored in DB, never hard-coded. */
    @Column(name = "per_person_per_day", nullable = false, precision = 8, scale = 3)
    private BigDecimal perPersonPerDay = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    protected SupplyItem() { }

    public SupplyItem(String name, String unit, BigDecimal perPersonPerDay) {
        this.name = name;
        this.unit = unit;
        this.perPersonPerDay = perPersonPerDay;
    }

    public boolean isConsumptionTracked() { return perPersonPerDay.signum() > 0; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public BigDecimal getPerPersonPerDay() { return perPersonPerDay; }
    public boolean isActive() { return active; }
    public void setIdForTest(Long id) { this.id = id; }
}
