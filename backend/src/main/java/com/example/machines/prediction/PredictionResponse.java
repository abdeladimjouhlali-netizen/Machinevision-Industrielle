package com.example.machines.prediction;

import com.example.machines.equipment.EquipmentView;
import java.time.Instant;

public record PredictionResponse(
        Long id,
        String assetTag,
        String category,
        double confidence,
        Instant createdAt,
        String disclaimer,
        EquipmentView equipment
) {}
