package com.rajesh.aashraynet.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "districts")
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    protected District() { }

    public District(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}
