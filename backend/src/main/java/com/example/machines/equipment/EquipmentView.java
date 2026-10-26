package com.example.machines.equipment;

public record EquipmentView(
        String assetTag,
        String category,
        String manufacturer,
        String model,
        String technicalSheetUrl,
        String manualUrl
) {}
