package com.example.machines.equipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "equipment")
public class EquipmentEntity {
    @Id
    @Column(name = "asset_tag", length = 100)
    private String assetTag;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 160)
    private String manufacturer;

    @Column(name = "model_name", nullable = false, length = 160)
    private String model;

    @Column(name = "technical_sheet_filename", length = 255)
    private String technicalSheetFilename;

    @Column(name = "manual_filename", length = 255)
    private String manualFilename;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EquipmentEntity() {}

    public String getAssetTag() { return assetTag; }
    public String getCategory() { return category; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public String getTechnicalSheetFilename() { return technicalSheetFilename; }
    public String getManualFilename() { return manualFilename; }
    public Instant getCreatedAt() { return createdAt; }
}
