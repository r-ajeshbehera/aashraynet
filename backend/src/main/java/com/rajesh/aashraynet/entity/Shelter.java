package com.rajesh.aashraynet.entity;

import com.rajesh.aashraynet.exception.CapacityExceededException;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shelters")
public class Shelter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String village;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int occupancy;

    @Column(name = "has_medical", nullable = false)
    private boolean hasMedical;

    @Column(name = "has_ramp", nullable = false)
    private boolean hasRamp;

    @Column(name = "has_generator", nullable = false)
    private boolean hasGenerator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incharge_id")
    private User incharge;

    @Column(nullable = false)
    private boolean active = true;

    /** Optimistic lock: Hibernate adds "AND version = ?" to every UPDATE of this row. */
    @Version
    private Long version;

    /** Maintained by MySQL (ON UPDATE CURRENT_TIMESTAMP); read-only here. Shown as "last updated" on public pages. */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;

    protected Shelter() { }

    public Shelter(District district, String name, String village, BigDecimal latitude, BigDecimal longitude, int capacity) {
        this.district = district;
        this.name = name;
        this.village = village;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
    }

    // ---- business behaviour -------------------------------------------------

    public int freePlaces() { return capacity - occupancy; }

    public OccupancyStatus status() { return OccupancyStatus.of(occupancy, capacity); }

    /** occupancy / capacity x 100, one decimal place. */
    public double occupancyPercent() { return Math.round(occupancy * 1000.0 / capacity) / 10.0; }

    public void admit(int people) {
        if (people <= 0) throw new IllegalArgumentException("people must be positive");
        if (freePlaces() < people) throw new CapacityExceededException(id, freePlaces(), people);
        occupancy += people;
    }

    public void release(int people) {
        if (people <= 0 || people > occupancy) throw new IllegalStateException("cannot release " + people + " from occupancy " + occupancy);
        occupancy -= people;
    }

    public boolean isInchargeUser(Long userId) { return incharge != null && incharge.getId().equals(userId); }

    // ---- getters / setters --------------------------------------------------

    public Long getId() { return id; }
    public District getDistrict() { return district; }
    public Long getDistrictId() { return district.getId(); }
    public String getName() { return name; }
    public String getVillage() { return village; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public int getCapacity() { return capacity; }
    public int getOccupancy() { return occupancy; }
    public boolean isHasMedical() { return hasMedical; }
    public boolean isHasRamp() { return hasRamp; }
    public boolean isHasGenerator() { return hasGenerator; }
    public User getIncharge() { return incharge; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setVillage(String village) { this.village = village; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setCapacity(int capacity) {
        if (capacity <= 0 || capacity < occupancy) throw new IllegalArgumentException("capacity must be > 0 and >= current occupancy");
        this.capacity = capacity;
    }
    public void setHasMedical(boolean hasMedical) { this.hasMedical = hasMedical; }
    public void setHasRamp(boolean hasRamp) { this.hasRamp = hasRamp; }
    public void setHasGenerator(boolean hasGenerator) { this.hasGenerator = hasGenerator; }
    public void setIncharge(User incharge) { this.incharge = incharge; }
    public void setActive(boolean active) { this.active = active; }
    /** Test/demo helper only. */
    public void setOccupancyForTest(int occupancy) { this.occupancy = occupancy; }
    public void setIdForTest(Long id) { this.id = id; }
}
