package com.example.machines.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "predictions")
public class PredictionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_tag", length = 100)
    private String assetTag;

    @Column(name = "predicted_category", nullable = false, length = 40)
    private String predictedCategory;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PredictionEntity() {}

    public PredictionEntity(String assetTag, String predictedCategory, double confidence) {
        this.assetTag = assetTag;
        this.predictedCategory = predictedCategory;
        this.confidence = confidence;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public String getPredictedCategory() { return predictedCategory; }
    public double getConfidence() { return confidence; }
    public Instant getCreatedAt() { return createdAt; }
}
