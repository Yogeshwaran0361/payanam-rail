package com.payanam.rail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity representing a platform at Payanam Junction.
 * Maps to MySQL table 'platforms'.
 */
@Entity
@Table(name = "platforms")
public class PlatformEntity {

    @Id
    @Column(name = "platform_id", length = 10, nullable = false)
    private String platformId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "length_meters", nullable = false)
    private int lengthMeters;

    @Column(name = "under_maintenance", nullable = false)
    private boolean underMaintenance = false;

    public PlatformEntity() {
    }

    public PlatformEntity(String platformId, String name, int lengthMeters, boolean underMaintenance) {
        this.platformId = platformId;
        this.name = name;
        this.lengthMeters = lengthMeters;
        this.underMaintenance = underMaintenance;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLengthMeters() {
        return lengthMeters;
    }

    public void setLengthMeters(int lengthMeters) {
        this.lengthMeters = lengthMeters;
    }

    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
    }
}

