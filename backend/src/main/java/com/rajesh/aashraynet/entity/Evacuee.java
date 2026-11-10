package com.rajesh.aashraynet.entity;

import com.rajesh.aashraynet.service.TextNormalizer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evacuees")
public class Evacuee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private DisasterEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id", nullable = false)
    private Shelter shelter;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "name_normalized", nullable = false, length = 120)
    private String nameNormalized;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(nullable = false, length = 120)
    private String village;

    @Column(name = "village_normalized", nullable = false, length = 120)
    private String villageNormalized;

    @Enumerated(EnumType.STRING)
    @Column(name = "special_need", nullable = false, length = 10)
    private SpecialNeed specialNeed = SpecialNeed.NONE;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by", nullable = false)
    private User registeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_out_by")
    private User checkedOutBy;

    protected Evacuee() { }

    public Evacuee(DisasterEvent event, Shelter shelter, String fullName, int age, Gender gender, String village,
                   SpecialNeed specialNeed, LocalDateTime checkedInAt, User registeredBy) {
        this.event = event;
        this.shelter = shelter;
        this.fullName = fullName.trim();
        this.age = age;
        this.gender = gender;
        this.village = village.trim();
        this.specialNeed = specialNeed == null ? SpecialNeed.NONE : specialNeed;
        this.checkedInAt = checkedInAt;
        this.registeredBy = registeredBy;
    }

    /** Keeps the search columns in sync with the display columns. */
    @PrePersist
    @PreUpdate
    void normalize() {
        this.nameNormalized = TextNormalizer.normalize(fullName);
        this.villageNormalized = TextNormalizer.normalize(village);
    }

    public boolean isCheckedOut() { return checkedOutAt != null; }

    public void checkOut(LocalDateTime when, User by) {
        if (isCheckedOut()) throw new IllegalStateException("already checked out");
        this.checkedOutAt = when;
        this.checkedOutBy = by;
    }

    public Long getId() { return id; }
    public DisasterEvent getEvent() { return event; }
    public Shelter getShelter() { return shelter; }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public Gender getGender() { return gender; }
    public String getVillage() { return village; }
    public SpecialNeed getSpecialNeed() { return specialNeed; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public User getRegisteredBy() { return registeredBy; }
}
